package com.suning.fab.loan.utils;

import java.math.BigDecimal;
import java.util.*;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.*;
import scala.collection.immutable.Stream;

/**
 * 根据预还款日期和预还款金额试算贷款各期账单，包括已结清、未结清账单
 *
 *
 * @return 返回账单明细
 */
public class LoanTrailCalculationProvider {
	private LoanTrailCalculationProvider() {
		// nothing to do
	}
	//预判是否为空
	public static LnsBillStatistics genLoanBillDetail(
			LoanAgreement loanAgreement, String repayDate, TranCtx ctx,LnsBillStatistics lnsBillStatistics)
			throws FabException {
		if(VarChecker.isEmpty(lnsBillStatistics)){
			if (loanAgreement.getContract().getContractStartDate().equals(repayDate) && 
					"3".equals(loanAgreement.getInterestAgreement().getAdvanceFeeType())){
				repayDate = CalendarUtil.nDaysAfter(repayDate, 1).toString("yyyy-MM-dd");
			}
			return  genLoanBillDetail(loanAgreement,repayDate,ctx);
		}
		return lnsBillStatistics;
	}
	public static LnsBillStatistics  genLoanBillDetail(
			LoanAgreement loanAgreement, String repayDate, TranCtx ctx)
			throws FabException {
		long start  = System.currentTimeMillis();
		LoggerUtil.info("试算开始：" );
		LnsBillStatistics billStatistics = new LnsBillStatistics();

		billStatistics.setIntTotalPeriod(0);
		
		billStatistics.setPrinTotalPeriod(0);
		
		
		//SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd")
		//new Date(format.parse(ctx.getTranDate()).getTime())
		LoanAgreement la = getLoanAgreement(loanAgreement, repayDate, ctx, billStatistics);


		// 添加历史账单
		LoanBillHistorySupporter.getLoanHisBillDetail(la,
				billStatistics, repayDate, ctx);

		return getLnsBillStatistics(loanAgreement, repayDate, ctx, start, billStatistics, la);

	}

	public static LoanAgreement getLoanAgreement(LoanAgreement loanAgreement, String repayDate, TranCtx ctx, LnsBillStatistics billStatistics) throws FabException {
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", ctx.getTranDate());
		param.put("serSeqNo", ctx.getSerSeqNo());
		Integer subno = DbAccessUtil.queryForObject("CUSTOMIZE.query_txseq", param, Integer.class);

		billStatistics.setBillNo(subno);
		//预约还款查询会出现这种情况
		if(!repayDate.equals(ctx.getTranDate())){
			//重新计算封顶值
			DynamicCapUtil.CapInterestCalculation(loanAgreement,ctx, repayDate);
		}
		if(ConstantDeclare.REPAYWAY.REPAYWAY_DEBXF.equals(loanAgreement.getWithdrawAgreement().getRepayWay())){
			transferFeeCalculate( loanAgreement);
		}
		LoanAgreement la = null;
		try {
			 la = (LoanAgreement) loanAgreement.deepClone();
		} catch (Exception e) {
			//协议复制失败
			throw new FabException(e,"SPS105");
		}
		return la;
	}

	public static LnsBillStatistics getLnsBillStatistics(LoanAgreement loanAgreement, String repayDate, TranCtx ctx, long start, LnsBillStatistics billStatistics, LoanAgreement la) throws FabException {
		//房抵贷封顶利率
		capRate(loanAgreement,ctx);
		// 添加当前期账单
		//房抵贷
//		!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
//		//房抵贷的罚息 只算第一期-----20200121
//		|| "2412615".equals(loanAgreement.getPrdId())) 
		if(  VarChecker.asList("2412615","4010002").contains(loanAgreement.getPrdId())
				|| !VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
				){
			LoanFDDBillFefureSupporter.genLoanFutureBillDetail(la,
					billStatistics, repayDate);
		}
		//普通
		else{
			LoanBillFefureSupporter.genLoanFutureBillDetail(la,
					billStatistics, repayDate);
		}
		//试算呆滞呆账罚息金额  不封顶
		la.getBasicExtension().setBadDebt(new FabAmount(-1.0));
		//试算 呆滞呆账罚息
		//获取呆滞呆账期间新罚息复利账单list
		billStatistics.setCdbillList(LoanPlanCintDint.genCintDintListC(la.getContract().getReceiptNo(), ctx,billStatistics,la,repayDate));

//		List<TblLnsbill> cdbillList = LoanPlanCintDint.genCintDintListC(la.getContract().getReceiptNo(), ctx,billStatistics,la,repayDate);
//		//将呆滞呆账罚息加入试算
//		for( TblLnsbill tblLnsbill:cdbillList)
//		{
//			billStatistics.getCdbillList().add(BillTransformHelper.convertToLnsBill(tblLnsbill));
//		}
		//动态封顶计息
		if(la.getBasicExtension().getDynamicCapAmt().isPositive()) {

			DynamicCapUtil.dynamCapTotal(billStatistics, la,repayDate,ctx);

			//将差值赋值给loanAgreement 留给后面算 手续费
			loanAgreement.getBasicExtension().setDynamicCapDiff(la.getBasicExtension().getDynamicCapDiff());
		}
		loanAgreement.getBasicExtension().setBadDebt(la.getBasicExtension().getBadDebt());
		//免息实际还款日
		loanAgreement.getBasicExtension().setDropNints(la.getBasicExtension().getDropNints());

		//房抵贷罚息信息落表 在合同到期日之前  进行封顶计算
		if("2412615".equals(loanAgreement.getPrdId())&&CalendarUtil.beforeAlsoEqual(ctx.getTranDate(),la.getContract().getContractEndDate())){
			LoanTrailCalculationProvider.getCapAmt(billStatistics,loanAgreement);
		}

		LoggerUtil.debug("贷款统计信息：" + billStatistics.toString());
		LoggerUtil.info("试算结束："+( System.currentTimeMillis()-start)+"ms");
		return billStatistics;
	}


	//呆滞呆账重新试算
	public static LnsBillStatistics reLoanBillDetail(LoanAgreement loanAgreement,LnsBillStatistics billStatistics, TranCtx ctx)throws FabException {
		LoanAgreement la ;
		try {
			la = (LoanAgreement) loanAgreement.deepClone();
		} catch (Exception e) {
			//协议复制失败
			throw new FabException(e,"SPS105");
		}
		//试算呆滞呆账罚息金额  不封顶
		la.getBasicExtension().setBadDebt(new FabAmount(-1.0));

		billStatistics.setCdbillList(new ArrayList<>());

		//试算 呆滞呆账罚息
		//获取呆滞呆账期间新罚息复利账单list
		billStatistics.setCdbillList(LoanPlanCintDint.genCintDintListC(la.getContract().getReceiptNo(), ctx,billStatistics,la,ctx.getTranDate()));
//		List<TblLnsbill> cdbillList = LoanPlanCintDint.genCintDintListC(la.getContract().getReceiptNo(), ctx,billStatistics,la,ctx.getTranDate());
//		//将呆滞呆账罚息加入试算
//		for( TblLnsbill tblLnsbill:cdbillList)
//		{
//			billStatistics.getCdbillList().add(BillTransformHelper.convertToLnsBill(tblLnsbill));
//		}
		//动态封顶计息  判断是否是日利率封顶
		if(la.getBasicExtension().getDynamicCapAmt().isPositive()) {
			//罚息落表用 repaydate = trandate
			DynamicCapUtil.dynamCapTotal(billStatistics, la,ctx.getTranDate(),ctx);
			//将差值赋值给loanAgreement 留给后面算 手续费
			loanAgreement.getBasicExtension().setDynamicCapDiff(la.getBasicExtension().getDynamicCapDiff());
		}
		loanAgreement.getBasicExtension().setBadDebt(la.getBasicExtension().getBadDebt());

		return billStatistics;
	}
	/**
	 * 历史未结清账单结息后修改原历史账单信息：积数、利息记至日期、形态
	 *
	 * @param lnsBill
	 * @throws FabException
	 */

	public static void modifyBillInfo(LnsBill lnsBill, LoanAgreement la,
			TranCtx ctx) throws FabException {
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", lnsBill.getHisBill().getTranDate());
		param.put("serseqno", lnsBill.getHisBill().getSerSeqno());
		param.put("txseq", lnsBill.getHisBill().getTxSeq());
		param.put("intedate", lnsBill.getHisBill().getIntendDate());
		param.put("intedate1", lnsBill.getIntendDate());
		param.put("accumulate", lnsBill.getHisBill().getAccumulate().getVal());
		
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_hisbill", param);
		} catch (FabException e) {
			throw new FabException(e, "SPS102", "lnsbills");
		}
	}

	/**
	 * 利率封顶
	 * @param loanAgreement 贷款协议
	 */
	public static void capRate(LoanAgreement loanAgreement,TranCtx ctx){
		//房抵贷产品进行利率封顶  在合同结束日期之后
		if("2412615".equals(loanAgreement.getPrdId())&&CalendarUtil.after(ctx.getTranDate(),loanAgreement.getContract().getContractEndDate())){
			//逾期利率
			FabRate overdueRate  =loanAgreement.getRateAgreement().getOverdueRate();
			Double rate = Double.valueOf(GlobalScmConfUtil.getProperty("dayCapRate", "24")) / 100.0D;
			rate = AmountUtil.round(rate, 6);
			//封顶利率
			FabRate capRate=new FabRate(rate);
			//罚息利率和封顶利率之间的差值运算
			FabRate overdueRateSpread=overdueRate.sub(capRate);
			//罚息大于利差
			if(overdueRateSpread.isPositive()){
			loanAgreement.getRateAgreement().setOverdueRate(capRate);
			}
		}
	}


	/**
	 * 根据账本 获取本次封顶金额
	 *
	 *  LnsBillStatistics  已试算信息
	 * @return
	 */
	public static void getCapAmt(LnsBillStatistics lnsBillStatistics,LoanAgreement loanAgreement)throws FabException{
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());
		//获取呆滞呆账期间新罚息复利账单list
		//宽限期罚息落表的不获取呆滞呆账期间的新罚息复利
		billList.addAll(lnsBillStatistics.getCdbillList());
		for (LnsBill bill : billList){
			//宽限期落表只落宽限期的
			if (bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)){
				if(bill.getBillAmt().isPositive())
				{
					FabAmount oldAmt=bill.getBillAmt();
					BigDecimal sumDint= getPeriodDint(lnsBillStatistics,bill,loanAgreement);
					FabAmount newDint=new FabAmount(sumDint.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
					bill.setBillAmt(newDint);
					bill.setBillBal((FabAmount) bill.getBillBal().add(newDint).sub(oldAmt));
				}
			}
		}
	}

	/**
	 * 获取时间段内的利息金额
	 *
	 * @param lnsBillStatistics
	 * @return
	 */
	public static BigDecimal getPeriodDint(LnsBillStatistics lnsBillStatistics,LnsBill bill,LoanAgreement loanAgreement) {
		List<LnsBill> selectedbills = new ArrayList<LnsBill>();
		selectedbills.addAll(lnsBillStatistics.getHisBillList());
		selectedbills.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前期和未来期
		selectedbills.addAll(lnsBillStatistics.getBillInfoList());
		selectedbills.addAll(lnsBillStatistics.getFutureBillInfoList());
		selectedbills.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		List<LnsBill> nintList = new ArrayList<LnsBill>();
		//过滤利息账单
		for(LnsBill lnsbill:selectedbills){
			//获取利息账单
			if("NINT".equals(lnsbill.getBillType())){
				nintList.add(lnsbill);
			}
		}
		BigDecimal sumDint=BigDecimal.valueOf(0.00);
		//利息账单进行金额汇总
		for(LnsBill nintBill:nintList){
			sumDint=sumDint.add(getSubsectionDintAmt(nintBill,lnsBillStatistics,bill,loanAgreement));
		}
		return sumDint;
	}

	/**
	 * 计算分段时间内  封顶的罚息金额
	 * @param nintBill 分期利息账本
	 * @return
	 */
	public static BigDecimal getSubsectionDintAmt(LnsBill nintBill,LnsBillStatistics lnsBillStatistics,LnsBill dintBill,LoanAgreement loanAgreement){
		String billStartDate =dintBill.getStartDate();
		String billEnddate=dintBill.getEndDate();
		Double rate = Double.valueOf(GlobalScmConfUtil.getProperty("dayCapRate", "24")) / 100.0D;
		rate = AmountUtil.round(rate, 6);
		//封顶利率
		FabRate capRate=new FabRate(rate);
		BigDecimal nintSum=BigDecimal.valueOf(0.00);
		String nintStartDate=nintBill.getStartDate();
		String nintEndDate= nintBill.getEndDate();
		//判断时间段的交集
		if(billEnddate.compareTo(nintStartDate)<=0||billStartDate.compareTo(nintEndDate)>=0){
			return nintSum;
		}
		String [] dates=new String[]{billStartDate,billEnddate,nintStartDate,nintEndDate};
		Arrays.sort(dates);
		//分段所占的实际天数
		int betweenDays=CalendarUtil.actualDaysBetween(dates[1],dates[2]);
		//封顶金额
		BigDecimal capAmt=BigDecimal.valueOf(lnsBillStatistics.getUnrepayPrin().getVal()).multiply(capRate.getDayRate()).setScale(2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(Integer.toString(betweenDays)));
		//罚息金额
		BigDecimal dintAmt=BigDecimal.valueOf(lnsBillStatistics.getUnrepayPrin().getVal()).multiply(dintBill.getBillRate().getDayRate()).setScale(2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(Integer.toString(betweenDays)));
		//利息金额
		//若天数占用一整期 则取所有利息金额
		BigDecimal nintAmt;
		//占满一整段
		if(nintStartDate.equals(dates[1])&&nintEndDate.equals(dates[2])){
			nintAmt=BigDecimal.valueOf(nintBill.getBillAmt().getVal());
		}else{
			//在当期计息前本期已经计提的利息
			int beforeDayNum=CalendarUtil.actualDaysBetween(nintStartDate,dates[1]);
			//在计提本次计提前已经计提的金额
		    BigDecimal beforeNintAmt=BigDecimal.valueOf(nintBill.getPrinBal().getVal()).multiply(nintBill.getBillRate().getDayRate()).setScale(2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(Integer.toString(beforeDayNum)));
			//本次需要计提的金额
		    BigDecimal betweenNintAmt=BigDecimal.valueOf(nintBill.getPrinBal().getVal()).multiply(nintBill.getBillRate().getDayRate()).setScale(2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(Integer.toString(betweenDays)));
            //本周期所有利息金额
			BigDecimal allNintAmt=BigDecimal.valueOf(nintBill.getBillAmt().getVal());
		    if(allNintAmt.subtract(beforeNintAmt).subtract(betweenNintAmt).signum()>=0){
		    	//增加期末判断
				if(dates[2].equals(nintEndDate)){
					nintAmt=allNintAmt.subtract(beforeNintAmt);
				}else{
					nintAmt=betweenNintAmt;
				}
			}else{
		    	if(allNintAmt.subtract(beforeNintAmt).signum()>0){
					nintAmt=allNintAmt.subtract(beforeNintAmt);
				}else{
					nintAmt=BigDecimal.valueOf(0.00);
				}
			}
		}
		//封顶金额-利息金额  和 罚息金额比较  大于 取罚息金额
		if(capAmt.subtract(nintAmt).subtract(dintAmt).signum()>0){
			return dintAmt;
		}else{
			if(capAmt.subtract(nintAmt).signum()>0){
				return capAmt.subtract(nintAmt);
			}else{
				return   BigDecimal.valueOf(0.00);
			}
		}
	}

	/**
	 * 等额本息费的计费方式 DTM转换为 DAY
	 * @param la
	 */
	public static void transferFeeCalculate(LoanAgreement la){
		if(la.getFeeAgreement().getLnsfeeinfos()!=null && la.getFeeAgreement().getLnsfeeinfos().size()>0){
			for(TblLnsfeeinfo lnsfeeinfo:la.getFeeAgreement().getLnsfeeinfos()){
				if(ConstantDeclare.CALCULATRULE.BYDAYTERM.equals(lnsfeeinfo.getCalculatrule())){
					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYDAY);
				}
			}
		}

	}

}

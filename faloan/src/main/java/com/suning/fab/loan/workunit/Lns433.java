package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsrentreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	SC
 *
 * @version V1.0.1
 *
 * @see 	通讯还款试算
 *
 * @param	acctNo			（账号）借据号
 * 			endDate			预约还款日期
 *
 * @return	cleanPrin		结清整笔金额
 * 			cleanAmt		结清尾款金额
 *			prinAmt			正常还款金额
 *			totalAmt		正常还尾款金额
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns433 extends WorkUnit {

	String acctNo;
	String endDate;

	FabAmount cleanPrin = new FabAmount();
	FabAmount cleanTotal = new FabAmount();
	FabAmount prinAmt = new FabAmount();
	FabAmount totalAmt = new FabAmount();

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();

		//参数校验
		if(VarChecker.isEmpty(endDate)){
			throw new FabException("LNS005");
		}
		//预约日期大于交易日
		if(CalendarUtil.before(endDate, ctx.getTranDate())){
			throw new FabException("LNS034");
		}

		//读取借据主文件
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());

		TblLnsbasicinfo lnsbasicinfo = null;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if (null == lnsbasicinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		//结息日为空就取起息日
		if(VarChecker.isEmpty(lnsbasicinfo.getLastintdate())){
			lnsbasicinfo.setLastintdate(lnsbasicinfo.getBeginintdate());
		}
		
//		if(CalendarUtil.equalDate(lnsbasicinfo.getOpendate(), lnsbasicinfo.getBeginintdate())){
//			//开户日当天还款还到最后一期本金，但是没还清
//			if(!CalendarUtil.equalDate(lnsbasicinfo.getLastintdate(), lnsbasicinfo.getContduedate())
//					&& CalendarUtil.equalDate(endDate, lnsbasicinfo.getOpendate())){
//				if(new FabAmount(lnsbasicinfo.getContractamt())
//						.sub(new FabAmount(lnsbasicinfo.getContractbal())).isZero()){
//					cleanPrin = new FabAmount(lnsbasicinfo.getContractbal());
//					return;  //如果是倒起息开户 当天预约那就有利息不能直接返回 此段放子交易最后 重置本金
//				}
//			}
//		}

		//根据账号生成协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		//生成还款计划
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, endDate, ctx);
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史账单
		billList.addAll(lnsBillStatistics.getHisBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
		billList.addAll(lnsBillStatistics.getBillInfoList());
		//未来期账单：从还款日到合同到期日之间的本金和利息账单
		billList.addAll(lnsBillStatistics.getFutureBillInfoList());

//		String repayDate = endDate;(保留)
		//预约日期大于合同结束日期取合同结束日
		if(CalendarUtil.after(endDate, lnsbasicinfo.getContduedate())){
			endDate = lnsbasicinfo.getContduedate();
		}
		
		//尾款计算
		//查询尾款
		//读取借据主文件
		Map<String,Object> finalaram = new HashMap<String,Object>();
		finalaram.put("acctno", acctNo);
		finalaram.put("brc", ctx.getBrc());

		TblLnsrentreg lnsrentreg;
		try {
			lnsrentreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrentreg", finalaram, TblLnsrentreg.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsrentreg");
		}

		if (null == lnsrentreg){
			throw new FabException("SPS104", "lnsrentreg");
		}
		
		//实际天数
		int nDays = CalendarUtil.actualDaysBetween(la.getContract().getContractStartDate(), endDate);
		//总天数
		int totalDays = CalendarUtil.actualDaysBetween(la.getContract().getContractStartDate(), la.getContract().getContractEndDate());
		//是否超过总天数
		if(nDays>totalDays){
			nDays = totalDays;
		}
		//计算尾款本息（尾款本金*（年利率*100/36000）*实际天数+尾款本金）
		//尾款本金
		BigDecimal finalPrin = new BigDecimal(lnsrentreg.getRsqmap().split("\\|")[1].split(":")[1]);
		//尾款总本息
		BigDecimal finalAmt = new BigDecimal(lnsrentreg.getRsqmap().split("\\|")[0].split(":")[1]);
		//尾款利率
		FabRate finalRate;
		if(VarChecker.isEmpty(lnsrentreg.getReqmap())){
			throw new FabException("SPS104", "lnsrentreg");
		}
		if(lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("finalRate"))==-1){
			//根据字符串截取尾款利率的值
			finalRate = new FabRate(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("finalRate")).split(":")[1])/100);
		}else{
			finalRate = new FabRate(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("finalRate"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("finalRate"))).split(":")[1])/100);
		}
		
		//计算尾款本息（尾款本金*（年利率*100/36000）*实际天数+尾款本金）
//		cleanTotal = new FabAmount(finalAmt.multiply(new BigDecimal(nDays)).divide(new BigDecimal(totalDays),2,BigDecimal.ROUND_HALF_UP).doubleValue());
		//获取日利率加1
		BigDecimal finalPrinRate = finalRate.getDayRate().multiply(BigDecimal.valueOf(nDays)).add(BigDecimal.valueOf(1));
		//计算得到尾款本息
		BigDecimal finalAmtDec = finalPrin.multiply(finalPrinRate).setScale(2,BigDecimal.ROUND_HALF_UP);
		
		cleanTotal = new FabAmount(finalAmtDec.compareTo(finalAmt)<0?finalAmtDec.doubleValue():finalAmt.doubleValue());
		//遍历list算出租金
		for(LnsBill lnsBill : billList){
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(lnsBill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE).contains(lnsBill.getSettleFlag())){
				continue;
			}else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())){
					cleanPrin.selfAdd(lnsBill.getBillBal());
				}
			}
			if((CalendarUtil.after(endDate, lnsBill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, lnsBill.getEndDate()))||CalendarUtil.before(lnsBill.getEndDate(),endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())){
					prinAmt.selfAdd(lnsBill.getBillBal());
				}
			}
		}
		if(cleanPrin.isZero()){
			cleanTotal = new FabAmount(0.0);
			finalAmt = BigDecimal.valueOf(0.0);
		}else{
			cleanPrin.selfAdd(cleanTotal);
			cleanPrin.selfSub(new FabAmount(finalAmt.doubleValue()));
		}
		//取当前账单列表最后一期，验证入参（预约还款日期）是否是最后一期（待验证list的最后一个对象是否是最后一期）
		if(CalendarUtil.after(endDate, billList.get(billList.size()-1).getStartDate())
				&& CalendarUtil.beforeAlsoEqual(endDate, billList.get(billList.size()-1).getEndDate())){
			prinAmt.selfSub(new FabAmount(finalAmt.doubleValue()));
//			if(cleanPrin.isZero()){
//				totalAmt = new FabAmount(0.0);
//			}else{
//				totalAmt =new FabAmount(finalAmt.doubleValue());
		}
//		}else{
		if(CalendarUtil.equalDate(endDate, billList.get(0).getStartDate())){
			prinAmt = billList.get(0).getBillBal();
		}
		prinAmt.selfAdd(cleanTotal);
		
//		}
		
	}

	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	/**
	 * @return the endDate
	 */
	public String getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the cleanPrin
	 */
	public FabAmount getCleanPrin() {
		return cleanPrin;
	}

	/**
	 * @param cleanPrin the cleanPrin to set
	 */
	public void setCleanPrin(FabAmount cleanPrin) {
		this.cleanPrin = cleanPrin;
	}

	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}

	/**
	 * @param prinAmt the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}

	/**
	 * @return the cleanTotal
	 */
	public FabAmount getCleanTotal() {
		return cleanTotal;
	}

	/**
	 * @param cleanTotal the cleanTotal to set
	 */
	public void setCleanTotal(FabAmount cleanTotal) {
		this.cleanTotal = cleanTotal;
	}

	/**
	 * @return the totalAmt
	 */
	public FabAmount getTotalAmt() {
		return totalAmt;
	}

	/**
	 * @param totalAmt the totalAmt to set
	 */
	public void setTotalAmt(FabAmount totalAmt) {
		this.totalAmt = totalAmt;
	}


	


}

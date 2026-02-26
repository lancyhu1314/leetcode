package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;

import com.suning.fab.loan.domain.TblLnsamortizeplan;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.RepayFormulaSupporter;
import com.suning.fab.loan.supporter.RepayFormulaSupporterProducter;
import com.suning.fab.loan.supporter.RepayWaySupporter;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsRepayPlanCalculate;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import scala.collection.immutable.Stream;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：按揭贷款还款计划试算
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype")
@Repository
public class Lns461 extends WorkUnit {

	FabAmount		contractAmt;	//本金
	Integer 		periodNum;		//期限
	Integer 		paymentTimes;	//期数
	String			periodType;		//期限类型
	String 			repayWay;		//还款方式
	String 			normalRateType;	//利率类型
	Double 			normalRate;		//利率
	String 			intPerUnit;		//扣息周期
	String 			loanType;		//贷款种类
	String 		repayDate;		//还款日
	String 			openDate;		//起始日期
	String 			endDate;		//到期日期
	Integer 		currentPage;	//页数
	Integer 		pageSize;		//每页条数
	Integer 		expandPeriod;	//膨胀期数
	LoanAgreement 	loanAgreement;

	Integer freeDays;//免息天数
	Integer freefeeDays=0;//免费天数
	Double feeDiscountRatio;//费用折扣

	String			productCode;

	Integer 		totalLine;
	String 			pkgList;
	String 			salesType;		//利息促销类型
	String 			feeSalesType;		//费用促销类型
	Double 			freeRate;		//免息比例	


	FabAmount 		freeInterest = new FabAmount(0.00);	//免利息合计
	FabAmount       freefeeamt = new FabAmount(0.00);		//免融担费合计
	FabAmount       totalamt = new FabAmount(0.00);		//合计金额



	private String enddateRule;//到期日按月对日规则
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();



		//产品配置可用还款方式后校验
		if( !"NO".equals( loanAgreement.getWithdrawAgreement().getUseRepayWay() ) )
		{
			String [] repayWayArray= loanAgreement.getWithdrawAgreement().getUseRepayWay().split("/");
			if( !Arrays.asList(repayWayArray).contains(repayWay) )
				throw new FabException("LNS152", loanAgreement.getPrdId(), repayWay);
		}

		if("1".equals(salesType) &&  null==freeDays){
			throw new FabException("LNS250");
		}
		
		if("2".equals(salesType) &&  null==freeRate){
			throw new FabException("LNS251");
		}

		//增加期数后限制
		if( null == periodNum )  //初始化空值
			periodNum = 0;
		if( null == paymentTimes )  //初始化空值
			paymentTimes = 0;

		if( StringUtil.isNull(ctx.getRequestDict("endDate"))){
			if(  periodNum > 0  && paymentTimes > 0 )
			{
				throw new FabException("LNS206");
			}
		}


		if( paymentTimes > 0  )
		{
			if( !VarChecker.isEmpty(ctx.getRequestDict("endDate")) )
				throw new FabException("LNS215");	//期数传值时到期日不能传值
			if( VarChecker.isEmpty(ctx.getRequestDict("enddateRule"))  ||
					!"2".equals(ctx.getRequestDict("enddateRule")) )
				throw new FabException("LNS216");   //到期日按月对日规则必须是固定还款日
		}


		//还款方式必输
		if( VarChecker.isEmpty(repayWay))
			throw new FabException("LNS051");
		//等额本息，不允许传零利率
		if ( "1".equals(repayWay) && !(new FabAmount(normalRate)).isPositive() )
			throw new FabException("LNS040");
		//随借随还，期限期数不能大于1
		if ( "8".equals(repayWay) && paymentTimes > 0 )
			throw new FabException("LNS218");




		if(!VarChecker.isEmpty(enddateRule))
		{
			//到期日按月对日规则校验
			if( !VarChecker.asList(ConstantDeclare.DTAEAGAINST.STARTINTDATE,
					ConstantDeclare.DTAEAGAINST.DATE_BEFORE_STARTINT,
					ConstantDeclare.DTAEAGAINST.REPAYDATE).contains(enddateRule)) {
				throw new FabException("LNS169", "到期日按月对日规则",enddateRule);
			}

			//不支持同时有到期日按月对日规则和到期日
			if(!VarChecker.isEmpty(ctx.getRequestDict("endDate")))
				throw new FabException("LNS195");
		}

		//2019-09-26 气球贷膨胀期数
		if( "9".equals(repayWay) )
		{
			if(	!VarChecker.isEmpty(expandPeriod) && expandPeriod > 0 )
				loanAgreement.getBasicExtension().setExpandPeriod(expandPeriod);
			else
				throw new FabException("LNS184");
		}

		//按揭贷款试算接口增加到期日enddate 2018-01-10
		if( null != ctx.getRequestDict("endDate") && !VarChecker.isEmpty(ctx.getRequestDict("endDate")) )
		{
			loanAgreement.getContract().setContractEndDate( endDate );
			setLoanAgreement(LoanAgreementProvider.genLoanAgreementFromRepayWay(loanAgreement, ctx)); //给loanAgreement赋值
			loanAgreement.getContract().setBalance(loanAgreement.getContract().getContractAmt());
		}
		else
		{
			if( periodNum == 0 )
				periodNum = paymentTimes;
			//当期限和期数同时为0时
			if(  periodNum == 0 &&  paymentTimes == 0 )
			{
				endDate = CalendarUtil.nDaysAfter(loanAgreement.getContract().getStartIntDate(), freeDays).toString("yyyy-MM-dd");
				//按揭贷款试算接口增加到期日enddate 2018-01-10
				if( null != endDate &&
						!VarChecker.isEmpty(endDate) )
				{
					loanAgreement.getContract().setContractEndDate( endDate );
					setLoanAgreement(LoanAgreementProvider.genLoanAgreementFromRepayWay(loanAgreement, ctx));//给loanAgreement赋值
					loanAgreement.getContract().setBalance(loanAgreement.getContract().getContractAmt());
				}
				String periodUnit = "M";
				//默认还款方式8
				RepayWaySupporter loanRepayWaySupporter =LoanSupporterUtil.getRepayWaySupporter("8");
				loanAgreement.getInterestAgreement().setIntFormula(loanRepayWaySupporter.getIntFormula());// 利息公式 :1-等额本息公式   2-其他
				loanAgreement.getWithdrawAgreement().setRepayAmtFormula(loanRepayWaySupporter.getRepayAmtFormula()); // 还款金额公式:1-等额本息  2-其他
				loanAgreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(periodNum, periodUnit, repayDate, loanAgreement.getContract().getStartIntDate(), endDate));
				loanAgreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(periodNum, periodUnit, repayDate,loanAgreement.getContract().getContractStartDate(), endDate));
				loanAgreement.getWithdrawAgreement().setRepayWay("8");     //还款方式
				setLoanAgreement(loanAgreement);
			}else{
				//根据扣息周期和期限计算到期日nDaysAfter
				if ("D".equals(intPerUnit)) {// 日
					loanAgreement.getContract().setContractEndDate(
							CalendarUtil.nDaysAfter(loanAgreement.getContract().getContractStartDate(), periodNum)
									.toString("yyyy-MM-dd"));
				} else if ("M".equals(intPerUnit)) {// 月
					loanAgreement.getContract().setContractEndDate(
							CalendarUtil.nMonthsAfter(loanAgreement.getContract().getContractStartDate(), periodNum)
									.toString("yyyy-MM-dd"));
				} else if ("Q".equals(intPerUnit)) {// 季
					loanAgreement.getContract().setContractEndDate(
							CalendarUtil.nMonthsAfter(loanAgreement.getContract().getContractStartDate(), periodNum * 3)
									.toString("yyyy-MM-dd"));
				} else if ("H".equals(intPerUnit)) {// 半年
					loanAgreement.getContract().setContractEndDate(
							CalendarUtil.nMonthsAfter(loanAgreement.getContract().getContractStartDate(), periodNum * 6)
									.toString("yyyy-MM-dd"));
				} else if ("Y".equals(intPerUnit)) {// 年
					loanAgreement.getContract().setContractEndDate(
							CalendarUtil.nYearsAfter(loanAgreement.getContract().getContractStartDate(), periodNum)
									.toString("yyyy-MM-dd"));
				}
				//指定到期日的DayOfMonth
				if(ConstantDeclare.DTAEAGAINST.REPAYDATE.equals(enddateRule)){
					loanAgreement.getContract().setContractEndDate(CalendarUtil.optDay(new DateTime(loanAgreement.getContract().getContractEndDate()),Integer.parseInt(loanAgreement.getContract().getRepayDate())).toString("yyyy-MM-dd"));
				}else if(ConstantDeclare.DTAEAGAINST.DATE_BEFORE_STARTINT.equals(enddateRule)){
					loanAgreement.getContract().setContractEndDate(new DateTime(loanAgreement.getContract().getContractEndDate()).minusDays(1).toString("yyyy-MM-dd"));

				}
				//先确定到期日  再算 周期公式
				setLoanAgreement(LoanAgreementProvider.genLoanAgreementForCalculation(loanAgreement, ctx));//给loanAgreement赋值
			}
		}





		if( paymentTimes > 0 )
		{
//				if(ConstantDeclare.DTAEAGAINST.REPAYDATE.equals(enddateRule)) {
//					LoanAgreement la = null;
//					try {
//						 la = (LoanAgreement) loanAgreement.deepClone();
//					} catch (Exception e) {
//						//协议复制失败
//						throw new FabException(e,"SPS105");
//					}
//					//试算一下总期数
//					RepayFormulaSupporter loanSupporter = RepayFormulaSupporterProducter.producter(loanAgreement);
//					//保证试算期数
//					loanAgreement.getContract().setContractEndDate(loanSupporter.calculateEndDateByPeriodNum(periodNum));
//
//
//				}

			//固定还款日对日  需要对期数做动态处理  -----------periodNum用信期限
			if(ConstantDeclare.DTAEAGAINST.REPAYDATE.equals(enddateRule))
			{
				if (VarChecker.isEmpty(loanAgreement.getContract().getStartIntDate())) {
					loanAgreement.getContract().setStartIntDate(loanAgreement.getContract().getContractStartDate());
				}
				if (VarChecker.isEmpty(loanAgreement.getContract().getRepayIntDate())) {
					loanAgreement.getContract().setRepayIntDate(loanAgreement.getContract().getStartIntDate());
				}
				if (VarChecker.isEmpty(loanAgreement.getContract().getRepayPrinDate())) {
					loanAgreement.getContract().setRepayPrinDate(loanAgreement.getContract().getContractStartDate());
				}
				//试算一下总期数
				RepayFormulaSupporter loanSupporter = RepayFormulaSupporterProducter.producter(loanAgreement);
				//保证试算期数
				loanAgreement.getContract().setContractEndDate(loanSupporter.calculateEndDateByPeriodNum(paymentTimes));
				setLoanAgreement(LoanAgreementProvider.genLoanAgreementForCalculation(loanAgreement, ctx));
			}
		}


		//房抵贷封顶计息 2019-10-15
		if( "2412615".equals(loanAgreement.getPrdId()))
		{
//			loanAgreement.getBasicExtension().setCapAmt(DynamicCapUtil.calculateCappingVal(loanAgreement));
			loanAgreement.getInterestAgreement().setCapRate(null);

//			loanAgreement.getBasicExtension().setCapAmt(new FabAmount(BigDecimal.valueOf(loanAgreement.getInterestAgreement().getCapRate())
//					.divide(new BigDecimal(366), 20 ,BigDecimal.ROUND_HALF_UP)
//					.multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()))
//					.multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(loanAgreement.getContract().getStartIntDate(), loanAgreement.getContract().getContractEndDate())))
//					.setScale(2,BigDecimal.ROUND_HALF_UP ).doubleValue()));
		}


		//补费用信息
		ListMap pkgList1 = ctx.getRequestDict("pkgList3");
		if( !VarChecker.isEmpty(pkgList1) ){
			loanAgreement.getFeeAgreement().setLnsfeeinfos(new ArrayList<>());
			for(PubDict pkg:pkgList1.getLoopmsg()){
				freefeeDays=PubDict.getRequestDict(pkg, "freefeeDays");//免费天数
				TblLnsfeeinfo lnsfeeinfo = new TblLnsfeeinfo();
				checkFeeInfo(pkg);
//				FaChecker.checkFeeList(productCode,pkgList1);
				lnsfeeinfo.setRepayway(PubDict.getRequestDict(pkg, "feerepayWay"));//费用交取模式
				if("A".equals(lnsfeeinfo.getRepayway()) && null!=PubDict.getRequestDict(pkg, "feeDiscountRatio"))
					feeDiscountRatio=Double.valueOf(PubDict.getRequestDict(pkg, "feeDiscountRatio").toString());
				if(null!=feeDiscountRatio && feeDiscountRatio>0.00  && null!=freefeeDays && freefeeDays>0  ){
					throw new FabException("LNS252");
				}
				lnsfeeinfo.setFeetype(PubDict.getRequestDict(pkg, "feeType"));//费用类型
				if( "1".equals(PubDict.getRequestDict(pkg, "calCulatrule").toString()) )
					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYDAY);
				else if( "2".equals(PubDict.getRequestDict(pkg, "calCulatrule")) )
					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYTERM);
				lnsfeeinfo.setFeebase(PubDict.getRequestDict(pkg, "feeBase"));//计费方式
				lnsfeeinfo.setFeerate(Double.valueOf(PubDict.getRequestDict(pkg, "feeRate").toString()));//费率
				lnsfeeinfo.setLastfeedate(loanAgreement.getContract().getContractStartDate());//上次结费日
				lnsfeeinfo.setFeeformula(loanAgreement.getInterestAgreement().getPeriodFormula());//上次结费日
				if(ConstantDeclare.FEEREPAYWAY.ADVDEDUCT.equals(lnsfeeinfo.getRepayway()))
					lnsfeeinfo.setFeestat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);

				//一次性费率默认值 2020-07-02
				if( ConstantDeclare.FEEREPAYWAY.ONETIME.equals( lnsfeeinfo.getRepayway()) )
				{
//					lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYTERM);
					lnsfeeinfo.setFeebase(ConstantDeclare.FEEBASE.ALL);//计费方式
				}

//				lnsfeeinfo.setDeducetionamt(PubDict.getRequestDict(pkg, "deducetionAmt"));//扣费金额
//				lnsfeeinfo.setOverrate(PubDict.getRequestDict(pkg, "overRate"));//违约费率
//				lnsfeeinfo.setAdvancesettle(PubDict.getRequestDict(pkg, "advanceSettle"));//提前结清收取方式
				loanAgreement.getFeeAgreement().getLnsfeeinfos().add(lnsfeeinfo);
			}
		}
		if(null!=feeDiscountRatio && feeDiscountRatio>0.00){
			feeSalesType="2";
		}
		loanAgreement.getContract().setGraceDays(0);
		if(null!=freeDays && !"2".equals(salesType)){
			//计算利息时还款日期
			String endDate_Nint = CalendarUtil.nDaysAfter(loanAgreement.getContract().getStartIntDate(), freeDays).toString("yyyy-MM-dd");
			if(CalendarUtil.after(endDate_Nint,loanAgreement.getContract().getContractEndDate())){
				throw new FabException("LNS238");
			}
			LnsBillStatistics billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,
					endDate_Nint, ctx);//将还款日期设置成合同起始日期
			//免息试算
			accumulateBillAmt(billStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,freeInterest,endDate_Nint,salesType,0.00);
		}
		if("2".equals(salesType) &&  null!=freeRate && freeRate>0.00){

			
			LnsBillStatistics billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,
					loanAgreement.getContract().getContractStartDate(), ctx);//合同起始日期
			//免息试算
			accumulateBillAmt(billStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,freeInterest,loanAgreement.getContract().getContractStartDate(),salesType,freeRate);
		}
		if("2".equals(feeSalesType)){
			LnsBillStatistics billStatistics_fee = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,
					loanAgreement.getContract().getContractStartDate(), ctx);//合同起始日期
			//免费试算
			accumulateBillAmt(billStatistics_fee,ConstantDeclare.BILLTYPE.BILLTYPE_AFEE,freefeeamt,loanAgreement.getContract().getContractStartDate(),feeSalesType,feeDiscountRatio);

		}else{
			if(null!=freefeeDays){
				String endDate_Fee = CalendarUtil.nDaysAfter(loanAgreement.getContract().getStartIntDate(), freefeeDays).toString("yyyy-MM-dd");
				if(CalendarUtil.after(endDate_Fee,loanAgreement.getContract().getContractEndDate())){
					throw new FabException("LNS238");
				}
				LnsBillStatistics billStatistics_fee = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,
						endDate_Fee, ctx);//将还款日期设置成合同起始日期
				//免费试算
				accumulateBillAmt(billStatistics_fee,ConstantDeclare.BILLTYPE.BILLTYPE_AFEE,freefeeamt,endDate_Fee,"1",0.00);
			}
		}
		totalamt.selfAdd(freeInterest).selfAdd(freefeeamt);
	}

	/**
	 * 累加免息、免费金额
	 * @param billStatistics
	 * @param billType
	 * @param billAmt
	 */
	public void accumulateBillAmt(LnsBillStatistics billStatistics,String billType,FabAmount billAmt,String repayDate,String salesType,double freeRate)throws FabException {
		if (null == billStatistics || (null == billStatistics.getFutureBillInfoList()&&null==billStatistics.getBillInfoList())) {
			throw new FabException("LNS021");//"lns401获取还款计划失败!"
		}
		List<LnsBill> trailList=new ArrayList<LnsBill>();
		//根据还款日期的不同，试算的计划可能存在历史期或者未来期
		if(billStatistics.getFutureBillInfoList()!=null){
			trailList.addAll(billStatistics.getFutureBillInfoList());
		}
		if(billStatistics.getBillInfoList()!=null){
			trailList.addAll(billStatistics.getBillInfoList());
		}
		Iterator<LnsBill> iterator = trailList.iterator();
		while (iterator.hasNext()) {
			//获取每条记录的peroid值，如果相同则合并，否则创建一个新key
			LnsBill bill = iterator.next();
			if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(billType)){
				if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())) {//如果是利息，则增加利息的信息
					if("2".equals(salesType)){//折扣比例免息
						billAmt.selfAdd(BigDecimal.valueOf(bill.getBillAmt().getVal()).multiply(BigDecimal.valueOf(freeRate).divide(BigDecimal.valueOf(100.00))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());//折扣利息
					}else{
						if(CalendarUtil.afterAlsoEqual(repayDate,bill.getEndDate())){
							billAmt.selfAdd(bill.getBillAmt());//累加利息
						}else if(CalendarUtil.afterAlsoEqual(repayDate,bill.getStartDate())){
							//等本等息按占比计算
							if(ConstantDeclare.REPAYWAY.REPAYWAY_DBDX.equals(bill.getRepayWay())){
								//按占比计算
								int billAllDay=CalendarUtil.actualDaysBetween(bill.getStartDate(),bill.getEndDate());
								int billHasDay=CalendarUtil.actualDaysBetween(bill.getStartDate(),repayDate);
								billAmt.selfAdd(new BigDecimal(bill.getBillAmt().getVal()).multiply(new BigDecimal(billHasDay)).divide(new BigDecimal(billAllDay),2, BigDecimal.ROUND_HALF_UP).doubleValue());
							}else{
								if(bill.getRepayDateInt()!=null){
									billAmt.selfAdd(bill.getRepayDateInt());//累加利息
								}
							}
						}
					}
				}
			} else if(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE.equals(billType)){
				if(LoanFeeUtils.isFeeType(bill.getBillType())){
					if("2".equals(salesType)){//折扣比例免费
						billAmt.selfAdd(BigDecimal.valueOf(bill.getBillAmt().getVal()).multiply(BigDecimal.valueOf(freeRate)).setScale(2, BigDecimal.ROUND_HALF_UP).divide(BigDecimal.valueOf(100.00),2, BigDecimal.ROUND_HALF_UP).doubleValue());//折扣费用
					}else{
						if(CalendarUtil.afterAlsoEqual(repayDate,bill.getEndDate())){
							billAmt.selfAdd(bill.getBillAmt());//累加费用
						}else if(CalendarUtil.afterAlsoEqual(repayDate,bill.getStartDate())){
							if(bill.getRepayDateInt()!=null){
								billAmt.selfAdd(bill.getRepayDateInt());//累加费用
							}
						}
					}
				}
			}
		}
	}


	//校验费用
	private void checkFeeInfo(PubDict pkg) throws FabException {
		if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feerepayWay"))){
			throw new FabException("LNS055","费用交取模式");
		}
		if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeType"))){
			throw new FabException("LNS055","费用类型");
		}
		if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "calCulatrule"))){
			throw new FabException("LNS055","计费方式");
		}
		if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeBase"))){
			throw new FabException("LNS055","计费基数");
		}
		if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeRate"))){
			throw new FabException("LNS055","费率");
		}
	}

	private String calculateRepayDate(Integer repayDate){
		if(repayDate<10){
			return "0"+Integer.toString(repayDate);
		}
		return Integer.toString(repayDate);
	}
	/**
	 * Gets the value of enddateRule.
	 *
	 * @return the value of enddateRule
	 */
	public String getEnddateRule() {
		return enddateRule;
	}

	/**
	 * Sets the enddateRule.
	 *
	 * @param enddateRule enddateRule
	 */
	public void setEnddateRule(String enddateRule) {
		this.enddateRule = enddateRule;

	}

	/**
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}

	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	/**
	 * @return the periodNum
	 */
	public Integer getPeriodNum() {
		return periodNum;
	}

	/**
	 * @param periodNum the periodNum to set
	 */
	public void setPeriodNum(Integer periodNum) {
		this.periodNum = periodNum;
	}

	/**
	 * @return the periodType
	 */
	public String getPeriodType() {
		return periodType;
	}

	/**
	 * @param periodType the periodType to set
	 */
	public void setPeriodType(String periodType) {
		this.periodType = periodType;
	}

	/**
	 * @return the repayWay
	 */
	public String getRepayWay() {
		return repayWay;
	}

	/**
	 * @param repayWay the repayWay to set
	 */
	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
	}

	/**
	 * @return the normalRateType
	 */
	public String getNormalRateType() {
		return normalRateType;
	}

	/**
	 * @param normalRateType the normalRateType to set
	 */
	public void setNormalRateType(String normalRateType) {
		this.normalRateType = normalRateType;
	}

	/**
	 * @return the normalRate
	 */
	public Double getNormalRate() {
		return normalRate;
	}

	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(Double normalRate) {
		this.normalRate = normalRate;
	}

	/**
	 * @return the intPerUnit
	 */
	public String getIntPerUnit() {
		return intPerUnit;
	}

	/**
	 * @param intPerUnit the intPerUnit to set
	 */
	public void setIntPerUnit(String intPerUnit) {
		this.intPerUnit = intPerUnit;
	}

	/**
	 * @return the loanType
	 */
	public String getLoanType() {
		return loanType;
	}

	/**
	 * @param loanType the loanType to set
	 */
	public void setLoanType(String loanType) {
		this.loanType = loanType;
	}

	/**
	 * @return the repayDate
	 */
	public String getRepayDate() {
		return repayDate;
	}

	/**
	 * @param repayDate the repayDate to set
	 */
	public void setRepayDate(String repayDate) {
		this.repayDate = repayDate;
	}

	/**
	 * @return the openDate
	 */
	public String getOpenDate() {
		return openDate;
	}

	/**
	 * @param openDate the openDate to set
	 */
	public void setOpenDate(String openDate) {
		this.openDate = openDate;
	}



	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	/**
	 * @param loanAgreement the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}

	/**
	 * @return the totalLine
	 */
	public Integer getTotalLine() {
		return totalLine;
	}

	/**
	 * @param totalLine the totalLine to set
	 */
	public void setTotalLine(Integer totalLine) {
		this.totalLine = totalLine;
	}

	/**
	 * @return the pkgList
	 */
	public String getPkgList() {
		return pkgList;
	}

	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
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
	 * @return the expandPeriod
	 */
	public Integer getExpandPeriod() {
		return expandPeriod;
	}

	/**
	 * @param expandPeriod the expandPeriod to set
	 */
	public void setExpandPeriod(Integer expandPeriod) {
		this.expandPeriod = expandPeriod;
	}
	/**
	 * @return the paymentTimes
	 */
	public Integer getPaymentTimes() {
		return paymentTimes;
	}
	/**
	 * @param paymentTimes the paymentTimes to set
	 */
	public void setPaymentTimes(Integer paymentTimes) {
		this.paymentTimes = paymentTimes;
	}


	/**
	 * @return the productCode
	 */
	public String getProductCode() {
		return productCode;
	}
	/**
	 * @param productCode the productCode to set
	 */
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}


	public FabAmount getFreeInterest() {
		return freeInterest;
	}

	public void setFreeInterest(FabAmount freeInterest) {
		this.freeInterest = freeInterest;
	}

	public FabAmount getFreefeeamt() {
		return freefeeamt;
	}

	public void setFreefeeamt(FabAmount freefeeamt) {
		this.freefeeamt = freefeeamt;
	}

	
	public FabAmount getTotalamt() {
		return totalamt;
	}

	public void setTotalamt(FabAmount totalamt) {
		this.totalamt = totalamt;
	}

	public Integer getFreeDays() {
		return freeDays;
	}

	public void setFreeDays(Integer freeDays) {
		this.freeDays = freeDays;
	}

	public Integer getFreefeeDays() {
		return freefeeDays;
	}

	public void setFreefeeDays(Integer freefeeDays) {
		this.freefeeDays = freefeeDays;
	}

	public String getSalesType() {
		return salesType;
	}

	public void setSalesType(String salesType) {
		this.salesType = salesType;
	}

	public Double getFreeRate() {
		return freeRate;
	}

	public void setFreeRate(Double freeRate) {
		this.freeRate = freeRate;
	}

	
}

package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsRepayPlanCalculate;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.RepayFormulaSupporter;
import com.suning.fab.loan.supporter.RepayFormulaSupporterProducter;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.*;

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
public class Lns401 extends WorkUnit {

	FabAmount		contractAmt;	//本金
	Integer 		periodNum;		//期限
	Integer 		paymentTimes;	//期数
	String			periodType;		//期限类型
	String 			repayWay;		//还款方式
	String 			normalRateType;	//利率类型
	Double 			normalRate;		//利率
	String 			intPerUnit;		//扣息周期
	String 			loanType;		//贷款种类
	Integer 		repayDate;		//还款日
	String 			openDate;		//起始日期
	String 			endDate;		//到期日期
	Integer 		currentPage;	//页数
	Integer 		pageSize;		//每页条数
	Integer 		expandPeriod;	//膨胀期数
	LoanAgreement 	loanAgreement;
	
	String			productCode;

	Integer 		totalLine;
	String 			pkgList;
	FabAmount 		prinAmt;		//应还本金合计
	FabAmount 		intAmt;			//应还利息合计
	FabAmount       feeAmt;			//应还费用合计
	FabAmount       singleAmt = new FabAmount();		//趸交融担费（绿地小贷）
	List<LnsRepayPlanCalculate> lnsRepayPlanCalList = new ArrayList<LnsRepayPlanCalculate>();
	
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
		//增加期数后限制
		if( null == periodNum )  //初始化空值
  			periodNum = 0;
		if( null == paymentTimes )  //初始化空值
			paymentTimes = 0;
		if(  periodNum > 0  && paymentTimes > 0 )
		{
			throw new FabException("LNS206");
		}
		if(  periodNum == 0 &&  paymentTimes == 0 )
		{
			throw new FabException("LNS206");
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
				//首期还款日存放扩展表
				if(!VarChecker.isEmpty(ctx.getRequestDict("firstRepayDate"))){
					PubDictUtils.checkFirstRepayDate(ctx.getRequestDict("firstRepayDate"),tranctx.getRequestDict("repayDate"),loanAgreement.getContract().getContractEndDate(),loanAgreement.getContract().getStartIntDate(),loanAgreement.getWithdrawAgreement().getRepayWay());
					loanAgreement.getBasicExtension().setFirstRepayDate(ctx.getRequestDict("firstRepayDate"));
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
				TblLnsfeeinfo lnsfeeinfo = new TblLnsfeeinfo();
				checkFeeInfo(pkg);
//				FaChecker.checkFeeList(productCode,pkgList1);
				lnsfeeinfo.setRepayway(PubDict.getRequestDict(pkg, "feerepayWay"));//费用交取模式
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

		//首期还款日存放扩展表
		if(!VarChecker.isEmpty(ctx.getRequestDict("firstRepayDate"))){
			PubDictUtils.checkFirstRepayDate(ctx.getRequestDict("firstRepayDate"),tranctx.getRequestDict("repayDate"),loanAgreement.getContract().getContractEndDate(),loanAgreement.getContract().getStartIntDate(),loanAgreement.getWithdrawAgreement().getRepayWay());
			loanAgreement.getBasicExtension().setFirstRepayDate(ctx.getRequestDict("firstRepayDate"));
		}
		LnsBillStatistics billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, 
				loanAgreement.getContract().getContractStartDate(), ctx);//将还款日期设置成合同起始日期
		if (null == billStatistics || null == billStatistics.getFutureBillInfoList()) {
			throw new FabException("LNS021");//"lns401获取还款计划失败!"
		}
		
		List<LnsBill> lnsBillTemp = billStatistics.getFutureBillInfoList();
//		List<LnsRepayPlanCalculate> lnsRepayPlanCalList = new ArrayList<LnsRepayPlanCalculate>();
	
		Iterator<LnsBill> iterator = lnsBillTemp.iterator();
		intAmt = new FabAmount(0.00);
		feeAmt = new FabAmount();
		Map<Integer, LnsRepayPlanCalculate> repayPlanMap = new HashMap<Integer, LnsRepayPlanCalculate>();
		while(iterator.hasNext()) {
			//获取每条记录的peroid值，如果相同则合并，否则创建一个新key
			LnsBill bill = iterator.next();
			LnsRepayPlanCalculate lnsRepayPlanCalculate = new LnsRepayPlanCalculate();
			
			if (null != repayPlanMap.get(bill.getPeriod())) {
				lnsRepayPlanCalculate = repayPlanMap.get(bill.getPeriod());
			} else {
				lnsRepayPlanCalculate.setRepayterm(bill.getPeriod());//还款期数
				lnsRepayPlanCalculate.setCcy(loanAgreement.getContract().getCcy().getCcy());//币种
				lnsRepayPlanCalculate.setRepayintbdate(bill.getStartDate());//本期起日
				lnsRepayPlanCalculate.setRepayintedate(bill.getEndDate());//本期止日
				lnsRepayPlanCalculate.setRepayownbdate(bill.getEndDate());//还款起日为本期止日
				lnsRepayPlanCalculate.setRepayownedate(bill.getEndDate());//还款止日

			}
			if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())) {//如果是利息，则增加利息的信息
				lnsRepayPlanCalculate.setNoretint(bill.getBillAmt());
				setIntAmt((FabAmount) intAmt.selfAdd(bill.getBillAmt()));//累加利息
			}else if(LoanFeeUtils.isFeeType(bill.getBillType())){

				lnsRepayPlanCalculate.getNoretfee().selfAdd(bill.getBillAmt());
				feeAmt.selfAdd(bill.getBillAmt());
				
				//绿地小贷要求返回趸交融担费,扣除首期趸交融担费
				if( ("2512630".equals(productCode) || "2512636".equals(productCode) || "2512637".equals(productCode) || "2512633".equals(productCode))
						&& "B".equals(bill.getRepayWay()) )
				{
					singleAmt.selfAdd(bill.getBillAmt());
					if(lnsRepayPlanCalculate.getRepayterm() == 1)
						lnsRepayPlanCalculate.getNoretfee().selfSub(singleAmt);
				}
			} else {//如果是本金，则增加本金的信息
				lnsRepayPlanCalculate.setNoretamt(bill.getBillAmt());//本期应还本金
			}

			lnsRepayPlanCalculate.setSumamt((FabAmount)lnsRepayPlanCalculate.getNoretamt().add(
					lnsRepayPlanCalculate.getNoretint()));//本息合计
			repayPlanMap.put(bill.getPeriod(), lnsRepayPlanCalculate);
		}
		
		Iterator<Map.Entry<Integer, LnsRepayPlanCalculate>> iterMap = repayPlanMap.entrySet().iterator();
		while (iterMap.hasNext()) {//将Map中的value放入list中
			lnsRepayPlanCalList.add(iterMap.next().getValue());
		}
		if(!VarChecker.isEmpty(expandPeriod) &&
				expandPeriod > 0 && lnsRepayPlanCalList.size()>=expandPeriod)
			throw new FabException("LNS186");
		Collections.sort(lnsRepayPlanCalList);//将list中的内容按repayterm进行排序
		
		setPrinAmt(loanAgreement.getContract().getContractAmt());
		setPkgList(JsonTransfer.ToJson(lnsRepayPlanCalList));
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
	public Integer getRepayDate() {
		return repayDate;
	}

	/**
	 * @param repayDate the repayDate to set
	 */
	public void setRepayDate(Integer repayDate) {
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
	 * @return the currentPage
	 */
	public Integer getCurrentPage() {
		return currentPage;
	}

	/**
	 * @param currentPage the currentPage to set
	 */
	public void setCurrentPage(Integer currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
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
	 * @return the intAmt
	 */
	public FabAmount getIntAmt() {
		return intAmt;
	}

	/**
	 * @param intAmt the intAmt to set
	 */
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}

	public List<LnsRepayPlanCalculate> getLnsRepayPlanCalList() {
		return lnsRepayPlanCalList;
	}

	public void setLnsRepayPlanCalList(
			List<LnsRepayPlanCalculate> lnsRepayPlanCalList) {
		this.lnsRepayPlanCalList = lnsRepayPlanCalList;
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
	 * Gets the value of feeAmt.
	 *
	 * @return the value of feeAmt
	 */
	public FabAmount getFeeAmt() {
		return feeAmt;
	}

	/**
	 * Sets the feeAmt.
	 *
	 * @param feeAmt feeAmt
	 */
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;

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

}

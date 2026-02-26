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
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
public class Lns600 extends WorkUnit {

	FabAmount contractAmt;	//本金
	Integer periodNum;		//期限
	Integer paymentTimes;	//期数
	String periodType;		//期限类型
	String repayWay;		//还款方式
	String normalRateType;	//利率类型
	Double normalRate;		//利率
	String intPerUnit;		//扣息周期
	String loanType;		//贷款种类
	Integer repayDate;		//还款日
	String openDate;		//起始日期
	String endDate;			//到期日期
	Integer currentPage;	//页数
	Integer pageSize;		//每页条数
	Integer expandPeriod;	//膨胀期数
	LoanAgreement loanAgreement;

	Integer totalLine;
	String pkgList;
	List<LnsRepayPlanCalculate> lnsRepayPlanCalList = new ArrayList<LnsRepayPlanCalculate>();
	FabAmount prinAmt;	//应还本金合计
	FabAmount intAmt;	//应还利息合计
	private String enddateRule;//到期日按月对日规则
	
	
	
	
	
	
	
	FabRate yearFeeRate;	//年费率
	List<LnsRepayPlanCalculate> calculates = new ArrayList<LnsRepayPlanCalculate>();
	FabAmount feeAmt;	//应还费用合计
	String productCode; // 产品代码  2512617任性贷搭保险（平台开户）---新增   2412619个贷-外部平台贷（资产方开户）
	String flag;  //1.一次性收取2.分期收取(默认)3.分期+一次性
	FabRate	 onetimeRate;//一次性费率

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
				
				
		//费用相关校验
		
		feeAmt = new FabAmount();
		FaChecker.checkFeeInput(productCode,flag,onetimeRate, yearFeeRate);
		

		
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
				throw new FabException("LNS215");   //期数传值时到期日不能传值
			if( VarChecker.isEmpty(ctx.getRequestDict("enddateRule"))  ||
				!"2".equals(ctx.getRequestDict("enddateRule")) )
				throw new FabException("LNS216");    //到期日按月对日规则必须是固定还款日
		}
				
		
		//还款方式必输
		if( VarChecker.isEmpty(repayWay))
			throw new FabException("LNS051");
		//等额本息，不允许传零利率
		if ("1".equals(repayWay) && !(new FabAmount(normalRate)).isPositive()) {//等额本息，不允许传零利率
			throw new FabException("LNS040");
		}
		//随借随还，期限期数不能大于1
		if ( "8".equals(repayWay) && paymentTimes > 0 )
			throw new FabException("LNS218");
		
		//到期日按月对日规则校验
		//不支持同时有到期日按月对日规则和到期日
		if(!VarChecker.isEmpty(enddateRule)){
			if( !VarChecker.asList(ConstantDeclare.DTAEAGAINST.STARTINTDATE,
					ConstantDeclare.DTAEAGAINST.DATE_BEFORE_STARTINT,
					ConstantDeclare.DTAEAGAINST.REPAYDATE).contains(enddateRule)) {
				throw new FabException("LNS169", "到期日按月对日规则",enddateRule);
			}
			if(!VarChecker.isEmpty(ctx.getRequestDict("endDate")))
				throw new FabException("LNS195");

		}

		//2019-09-26 气球贷膨胀期数
		if( "9".equals(repayWay) )
		{
			if(	null != expandPeriod &&
				!VarChecker.isEmpty(expandPeriod) &&
				expandPeriod > 0 )
					loanAgreement.getBasicExtension().setExpandPeriod(expandPeriod);
			else
				throw new FabException("LNS184");
		}
				
		//根据扣息周期和期限计算到期日nDaysAfter
		if ("1".equals(repayWay) && !(new FabAmount(normalRate)).isPositive()) {//等额本息，不允许传零利率
			throw new FabException("LNS040");
		}
		
		//按揭贷款试算接口增加到期日enddate 2018-01-10
		if( null != ctx.getRequestDict("endDate") &&
			!VarChecker.isEmpty(ctx.getRequestDict("endDate")) )
		{
			loanAgreement.getContract().setContractEndDate( endDate );
			setLoanAgreement(LoanAgreementProvider.genLoanAgreementFromRepayWay(loanAgreement, ctx));//给loanAgreement赋值
			loanAgreement.getContract().setBalance(loanAgreement.getContract().getContractAmt());
		}
		else
		{
			if( periodNum == 0 )
				periodNum = paymentTimes;
			
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

		
//		//放到气球贷处理之后
		if( paymentTimes > 0 )
		{
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
	            setLoanAgreement(LoanAgreementProvider.genLoanAgreementForCalculation(loanAgreement, ctx));//给loanAgreement赋值

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
		
		
		//费用相关信息登记
		List<TblLnsfeeinfo> lnsfeeinfos = new ArrayList<TblLnsfeeinfo>();
		
		if(LoanFeeUtils.rateIsPositive(ctx.getRequestDict("onetimeRate")))
			lnsfeeinfos.add(LoanFeeUtils.prepareFeeinfo(loanAgreement,ctx,ConstantDeclare.FEEREPAYWAY.ONETIME,ctx.getRequestDict("onetimeRate")));
    	if(LoanFeeUtils.rateIsPositive(ctx.getRequestDict("yearFeeRate")))
    		lnsfeeinfos.add(LoanFeeUtils.prepareFeeinfo(loanAgreement,ctx,ConstantDeclare.FEEREPAYWAY.STAGING,ctx.getRequestDict("yearFeeRate")));
    	loanAgreement.getFeeAgreement().setLnsfeeinfos(lnsfeeinfos);
				
		LnsBillStatistics billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, 
				loanAgreement.getContract().getContractStartDate(), ctx);//将还款日期设置成合同起始日期
		if (null == billStatistics || null == billStatistics.getFutureBillInfoList()) {
			throw new FabException("LNS021");//"lns401获取还款计划失败!"
		}
		
		List<LnsBill> lnsBillTemp = billStatistics.getFutureBillInfoList();
//		List<LnsRepayPlanCalculate> lnsRepayPlanCalList = new ArrayList<LnsRepayPlanCalculate>();
	
		Iterator<LnsBill> iterator = lnsBillTemp.iterator();
		intAmt = new FabAmount(0.00);
		Map<Integer, LnsRepayPlanCalculate> repayPlanMap = new HashMap<Integer, LnsRepayPlanCalculate>();
		FabAmount oneTimeAmt = new FabAmount(0.00);
		//增加预扣费的
		if(!loanAgreement.getFeeAgreement().isEmpty()){
			for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
				if (lnsfeeinfo.getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ADVDEDUCT))
				oneTimeAmt.selfAdd(lnsfeeinfo.getDeducetionamt());
			}
		}
		while(iterator.hasNext()) {
			//获取每条记录的peroid值，如果相同则合并，否则创建一个新key
			LnsBill bill = iterator.next();
			if( !VarChecker.isEmpty(bill.getLnsfeeinfo()) &&
				ConstantDeclare.FEEREPAYWAY.ONETIME.equals(bill.getLnsfeeinfo().getRepayway()))
			{
//				oneTimeAmt =	bill.getBillAmt();
				oneTimeAmt.selfAdd(bill.getBillAmt());
				continue;
			}

				
			LnsRepayPlanCalculate lnsRepayPlanCalculate = new LnsRepayPlanCalculate();
			
			if (null != repayPlanMap.get(bill.getPeriod())) {
				
				lnsRepayPlanCalculate = repayPlanMap.get(bill.getPeriod());
				if ( LoanFeeUtils.isFeeType(bill.getBillType())    ) {
					lnsRepayPlanCalculate.setNoretfee(bill.getBillAmt());
//					if( !VarChecker.isEmpty(bill.getLnsfeeinfo()) &&
//						ConstantDeclare.FEEREPAYWAY.STAGING.equals(bill.getLnsfeeinfo().getRepayway()) &&
//						1 == bill.getPeriod())
//					{
//						lnsRepayPlanCalculate.getNoretfee().selfAdd(oneTimeAmt);
//						setFeeAmt((FabAmount)feeAmt.selfSub(oneTimeAmt));
//
//					}
					
					setFeeAmt((FabAmount)feeAmt.selfAdd(bill.getBillAmt()));//累加费用
				} 
				else if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())) {//如果是利息，则增加利息的信息
					lnsRepayPlanCalculate.setNoretint(bill.getBillAmt());
					setIntAmt((FabAmount)intAmt.selfAdd(bill.getBillAmt()));//累加利息
				} else {//如果是本金，则增加本金的信息
					lnsRepayPlanCalculate.setNoretamt(bill.getBillAmt());//本期应还本金
				}
				
			} else {
				
				lnsRepayPlanCalculate.setRepayterm(bill.getPeriod());//还款期数
				lnsRepayPlanCalculate.setCcy(loanAgreement.getContract().getCcy().getCcy());//币种
				lnsRepayPlanCalculate.setRepayintbdate(bill.getStartDate());//本期起日
				lnsRepayPlanCalculate.setRepayintedate(bill.getEndDate());//本期止日
				lnsRepayPlanCalculate.setRepayownbdate(bill.getEndDate());//还款起日为本期止日
				lnsRepayPlanCalculate.setRepayownedate(bill.getEndDate());//还款止日
				
				if (  LoanFeeUtils.isFeeType( bill.getBillType() ) ) {
					lnsRepayPlanCalculate.setNoretfee(bill.getBillAmt());
//					if( !VarChecker.isEmpty(bill.getLnsfeeinfo()) &&
//						ConstantDeclare.FEEREPAYWAY.STAGING.equals(bill.getLnsfeeinfo().getRepayway()) &&
//						1 == bill.getPeriod())
//					{
//						lnsRepayPlanCalculate.getNoretfee().selfAdd(oneTimeAmt);
//						setFeeAmt((FabAmount)feeAmt.selfSub(oneTimeAmt));
//					}
					setFeeAmt((FabAmount)feeAmt.selfAdd(bill.getBillAmt()));//累加费用
				}else if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())) {//利息
					lnsRepayPlanCalculate.setNoretint(bill.getBillAmt());//本期应还利息
					setIntAmt((FabAmount)intAmt.selfAdd(bill.getBillAmt()));//累加利息
				} else {//本金
					lnsRepayPlanCalculate.setNoretamt(bill.getBillAmt());//本期应还本金
				}
			}
			
			lnsRepayPlanCalculate.setSumamt((FabAmount)lnsRepayPlanCalculate.getNoretamt().add(
					lnsRepayPlanCalculate.getNoretint()));//本息合计
			repayPlanMap.put(bill.getPeriod(), lnsRepayPlanCalculate);
		}
		
		//预扣费两个产品展示试算费用
		//防止空指针报错
		if( !repayPlanMap.isEmpty()&&oneTimeAmt.isPositive()
//				&&VarChecker.asList("2412626","2412624").contains( productCode )
				)
		{
			repayPlanMap.get(1).getNoretfee().selfAdd(oneTimeAmt);
			setFeeAmt((FabAmount)feeAmt.selfAdd(oneTimeAmt));
		}
		
		Iterator<Map.Entry<Integer, LnsRepayPlanCalculate>> iterMap = repayPlanMap.entrySet().iterator();
		while (iterMap.hasNext()) {//将Map中的value放入list中
			lnsRepayPlanCalList.add(iterMap.next().getValue());
		}
		Collections.sort(lnsRepayPlanCalList);//将list中的内容按repayterm进行排序
		
       

		setPrinAmt(loanAgreement.getContract().getContractAmt());
		setPkgList(JsonTransfer.ToJson(lnsRepayPlanCalList));
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
	 * @return the yearFeeRate
	 */
	public FabRate getYearFeeRate() {
		return yearFeeRate;
	}
	/**
	 * @param yearFeeRate the yearFeeRate to set
	 */
	public void setYearFeeRate(FabRate yearFeeRate) {
		this.yearFeeRate = yearFeeRate;
	}
	/**
	 * @return the calculates
	 */
	public List<LnsRepayPlanCalculate> getCalculates() {
		return calculates;
	}
	/**
	 * @param calculates the calculates to set
	 */
	public void setCalculates(List<LnsRepayPlanCalculate> calculates) {
		this.calculates = calculates;
	}
	/**
	 * @return the feeAmt
	 */
	public FabAmount getFeeAmt() {
		return feeAmt;
	}
	/**
	 * @param feeAmt the feeAmt to set
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
	/**
	 * @return the flag
	 */
	public String getFlag() {
		return flag;
	}
	/**
	 * @param flag the flag to set
	 */
	public void setFlag(String flag) {
		this.flag = flag;
	}
	/**
	 * @return the onetimeRate
	 */
	public FabRate getOnetimeRate() {
		return onetimeRate;
	}
	/**
	 * @param onetimeRate the onetimeRate to set
	 */
	public void setOnetimeRate(FabRate onetimeRate) {
		this.onetimeRate = onetimeRate;
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

}

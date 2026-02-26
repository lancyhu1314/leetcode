package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsRepayPlanCalculate;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;

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
public class Lns437 extends WorkUnit {

	FabAmount contractAmt;	//本金
	Integer periodNum;		//期限
	String periodType;		//期限类型
	String repayWay;		//还款方式
	String normalRateType;	//利率类型
	Double normalRate;		//利率
	FabRate serviceFeeRate;		//利率
	String intPerUnit;		//扣息周期
	String loanType;		//贷款种类
	Integer repayDate;		//还款日
	String openDate;		//起始日期
	Integer currentPage;	//页数
	Integer pageSize;		//每页条数
	LoanAgreement loanAgreement;
	
	Integer totalLine;
	String pkgList;
	List<LnsRepayPlanCalculate> lnsRepayPlanCalList = new ArrayList<LnsRepayPlanCalculate>();
	FabAmount prinAmt = new FabAmount(0.0);	//应还本金合计
	FabAmount intAmt = new FabAmount(0.0);	//应还利息合计
	FabAmount feeAmt = new FabAmount(0.0);	//应还信息服务费合计
	String endDate;	//应还信息服务费合计
	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		this.setIntPerUnit("M");
		this.setNormalRateType("Y");
		//根据扣息周期和期限计算到期日nDaysAfter
		if ("1".equals(repayWay) && !(new FabAmount(normalRate)).isPositive()) {//等额本息，不允许传零利率
			throw new FabException("LNS040");
		}
		if("8".equals(repayWay)){
			this.setLoanType("2");
			this.setIntPerUnit("D");
		}else{
			this.setLoanType("1");
		}
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
		
		setLoanAgreement(LoanAgreementProvider.genLoanAgreementForCalculation(loanAgreement, ctx));//给loanAgreement赋值
		loanAgreement.getContract().setCcy(new FabCurrency("01"));
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
		while(iterator.hasNext()) {
			//获取每条记录的peroid值，如果相同则合并，否则创建一个新key
			LnsBill bill = iterator.next();
			LnsRepayPlanCalculate lnsRepayPlanCalculate = new LnsRepayPlanCalculate();
			
			if (null != repayPlanMap.get(bill.getPeriod())) {
				
				lnsRepayPlanCalculate = repayPlanMap.get(bill.getPeriod());
				if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())) {//如果是利息，则增加利息的信息
					lnsRepayPlanCalculate.setNoretint(bill.getBillAmt());
					setIntAmt((FabAmount)intAmt.selfAdd(bill.getBillAmt()));//累加利息
				} else if(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE.equals(bill.getBillType())){
					lnsRepayPlanCalculate.setNoretfee(bill.getBillAmt());
					setIntAmt((FabAmount)feeAmt.selfAdd(bill.getBillAmt()));//累加费用
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
				
				if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())) {//利息
					lnsRepayPlanCalculate.setNoretint(bill.getBillAmt());//本期应还利息
					setIntAmt((FabAmount)intAmt.selfAdd(bill.getBillAmt()));//累加利息
				} else if(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE.equals(bill.getBillType())){
					lnsRepayPlanCalculate.setNoretfee(bill.getBillAmt());
					setIntAmt((FabAmount)feeAmt.selfAdd(bill.getBillAmt()));//累加费用
				} else {//本金
					lnsRepayPlanCalculate.setNoretamt(bill.getBillAmt());//本期应还本金
				}
			}
			
			lnsRepayPlanCalculate.setSumamt((FabAmount)lnsRepayPlanCalculate.getNoretamt().add(
					lnsRepayPlanCalculate.getNoretint()));//本息合计
			repayPlanMap.put(bill.getPeriod(), lnsRepayPlanCalculate);
		}
		
		Iterator<Map.Entry<Integer, LnsRepayPlanCalculate>> iterMap = repayPlanMap.entrySet().iterator();
		while (iterMap.hasNext()) {//将Map中的value放入list中
			lnsRepayPlanCalList.add(iterMap.next().getValue());
		}
		Collections.sort(lnsRepayPlanCalList);//将list中的内容按repayterm进行排序
		
		setPrinAmt(loanAgreement.getContract().getContractAmt());
		setPkgList(JsonTransfer.ToJson(lnsRepayPlanCalList));
		if(!VarChecker.isEmpty(lnsRepayPlanCalList) && lnsRepayPlanCalList.size()>0){
			setEndDate(lnsRepayPlanCalList.get(lnsRepayPlanCalList.size()-1).getRepayintedate());
		}
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

	/**
	 * @return the serviceFeeRate
	 */
	public FabRate getServiceFeeRate() {
		return serviceFeeRate;
	}

	/**
	 * @param serviceFeeRate the serviceFeeRate to set
	 */
	public void setServiceFeeRate(FabRate serviceFeeRate) {
		this.serviceFeeRate = serviceFeeRate;
	}

	/**
	 * @return the lnsRepayPlanCalList
	 */
	public List<LnsRepayPlanCalculate> getLnsRepayPlanCalList() {
		return lnsRepayPlanCalList;
	}

	/**
	 * @param lnsRepayPlanCalList the lnsRepayPlanCalList to set
	 */
	public void setLnsRepayPlanCalList(List<LnsRepayPlanCalculate> lnsRepayPlanCalList) {
		this.lnsRepayPlanCalList = lnsRepayPlanCalList;
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


}

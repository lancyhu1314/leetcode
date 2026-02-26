package com.suning.fab.loan.bo;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;

public class BillInfo {

	private Integer currPeriods;		/* 当前期数*/
	private String  planType;			/* 类型:1-本金   2-利息 */ 
	private FabAmount repayAmt;			/* 还款金额 */
	private FabRate  normalRate;		/* 执行利率 */
	private String startDate;   		/* 还款起日 */
	private String endDate;				/* 还款止日 */
	private Integer intDays;			/* 记息天数 */
	private FabAmount balance;			/* 贷款剩余本金 */

	
	@Override
	public String toString() {
		return "RepaymentPlan [currPeriods=" + currPeriods + ", planType=" + planType
				+ ", repayAmt=" + repayAmt + ", normalRate=" + normalRate
				+ ", startDate=" + startDate + ", endDate=" + endDate
				+ ", intDays=" + intDays + ", balance=" + balance + "]";
	}


	/**
	 * @return the currPeriods
	 */
	public Integer getCurrPeriods() {
		return currPeriods;
	}


	/**
	 * @param currPeriods the currPeriods to set
	 */
	public void setCurrPeriods(Integer currPeriods) {
		this.currPeriods = currPeriods;
	}


	/**
	 * @return the planType
	 */
	public String getPlanType() {
		return planType;
	}


	/**
	 * @param planType the planType to set
	 */
	public void setPlanType(String planType) {
		this.planType = planType;
	}


	/**
	 * @return the repayAmt
	 */
	public FabAmount getRepayAmt() {
		return repayAmt;
	}


	/**
	 * @param repayAmt the repayAmt to set
	 */
	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}


	/**
	 * @return the normalRate
	 */
	public FabRate getNormalRate() {
		return normalRate;
	}


	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}


	/**
	 * @return the startDate
	 */
	public String getStartDate() {
		return startDate;
	}


	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(String startDate) {
		this.startDate = startDate;
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
	 * @return the intDays
	 */
	public Integer getIntDays() {
		return intDays;
	}


	/**
	 * @param intDays the intDays to set
	 */
	public void setIntDays(Integer intDays) {
		this.intDays = intDays;
	}


	/**
	 * @return the balance
	 */
	public FabAmount getBalance() {
		return balance;
	}


	/**
	 * @param balance the balance to set
	 */
	public void setBalance(FabAmount balance) {
		this.balance = balance;
	}
	


}

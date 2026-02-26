package com.suning.fab.loan.la;

import java.io.Serializable;

import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.amount.FabAmount;

public class Contract  implements Serializable{ 
	/**
	 * 
	 */
	private static final long serialVersionUID = -3190775680050824151L;
	private  FabAmount contractAmt;	//合同金额
	private  FabAmount rentAmt;		//租金本息
	private  String contractNo;		//合同编号
	private  String receiptNo;		//借据号
	private  FabCurrency ccy;		//币 种 号
	private  String contractStartDate;//合同开始日期
	private  String contractEndDate;//合同到期日期
	private  FabAmount balance;	//剩余本金
	private  String repayPrinDate;		//上次结本日
	private  String repayIntDate;		//上次结息日
	private  Integer currPrinPeriod;    //本金当前期数
	private  Integer currIntPeriod;     //利息当前期数
	private  Integer graceDays;		//宽限期
	private  String startIntDate;	//起息日期
	private  FabAmount discountAmt;		//扣息金额
	private  String repayDate;		//扣款日期      格式：DD
	private  String flag1;		//是否改过还款日期      格式：DD
	private  String settleDate;		//结清日期
	private  String loanStat;		//贷款状态

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
	 * @return the contractNo
	 */
	public String getContractNo() {
		return contractNo;
	}
	/**
	 * @param contractNo the contractNo to set
	 */
	public void setContractNo(String contractNo) {
		this.contractNo = contractNo;
	}
	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}
	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}
	/**
	 * @return the ccy
	 */
	public FabCurrency getCcy() {
		return ccy;
	}
	/**
	 * @param ccy the ccy to set
	 */
	public void setCcy(FabCurrency ccy) {
		this.ccy = ccy;
	}
	/**
	 * @return the contractStartDate
	 */
	public String getContractStartDate() {
		return contractStartDate;
	}
	/**
	 * @param contractStartDate the contractStartDate to set
	 */
	public void setContractStartDate(String contractStartDate) {
		this.contractStartDate = contractStartDate;
	}
	/**
	 * @return the contractEndDate
	 */
	public String getContractEndDate() {
		return contractEndDate;
	}
	/**
	 * @param contractEndDate the contractEndDate to set
	 */
	public void setContractEndDate(String contractEndDate) {
		this.contractEndDate = contractEndDate;
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
	/**
	 * @return the repayPrinDate
	 */
	public String getRepayPrinDate() {
		return repayPrinDate;
	}
	/**
	 * @param repayPrinDate the repayPrinDate to set
	 */
	public void setRepayPrinDate(String repayPrinDate) {
		this.repayPrinDate = repayPrinDate;
	}
	/**
	 * @return the repayIntDate
	 */
	public String getRepayIntDate() {
		return repayIntDate;
	}
	/**
	 * @param repayIntDate the repayIntDate to set
	 */
	public void setRepayIntDate(String repayIntDate) {
		this.repayIntDate = repayIntDate;
	}
	/**
	 * @return the currPrinPeriod
	 */
	public Integer getCurrPrinPeriod() {
		return currPrinPeriod;
	}
	/**
	 * @param currPrinPeriod the currPrinPeriod to set
	 */
	public void setCurrPrinPeriod(Integer currPrinPeriod) {
		this.currPrinPeriod = currPrinPeriod;
	}
	/**
	 * @return the currIntPeriod
	 */
	public Integer getCurrIntPeriod() {
		return currIntPeriod;
	}
	/**
	 * @param currIntPeriod the currIntPeriod to set
	 */
	public void setCurrIntPeriod(Integer currIntPeriod) {
		this.currIntPeriod = currIntPeriod;
	}
	/**
	 * @return the graceDays
	 */
	public Integer getGraceDays() {
		return graceDays;
	}
	/**
	 * @param graceDays the graceDays to set
	 */
	public void setGraceDays(Integer graceDays) {
		this.graceDays = graceDays;
	}
	/**
	 * @return the startIntDate
	 */
	public String getStartIntDate() {
		return startIntDate;
	}
	/**
	 * @param startIntDate the startIntDate to set
	 */
	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
	}
	/**
	 * @return the discountAmt
	 */
	public FabAmount getDiscountAmt() {
		return discountAmt;
	}
	/**
	 * @param discountAmt the discountAmt to set
	 */
	public void setDiscountAmt(FabAmount discountAmt) {
		this.discountAmt = discountAmt;
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
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	public FabAmount getRentAmt() {
		return rentAmt;
	}
	public void setRentAmt(FabAmount rentAmt) {
		this.rentAmt = rentAmt;
	}
	public String getFlag1() {
		return flag1;
	}
	public void setFlag1(String flag1) {
		this.flag1 = flag1;
	}
	/**
	 * @return the settleDate
	 */
	public String getSettleDate() {
		return settleDate;
	}
	/**
	 * @param settleDate the settleDate to set
	 */
	public void setSettleDate(String settleDate) {
		this.settleDate = settleDate;
	}
	/**
	 * @return the loanStat
	 */
	public String getLoanStat() {
		return loanStat;
	}
	/**
	 * @param loanStat the loanStat to set
	 */
	public void setLoanStat(String loanStat) {
		this.loanStat = loanStat;
	}

}

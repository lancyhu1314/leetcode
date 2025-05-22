package com.suning.fab.loan.bo;

public class LoanFormInfo {

	private Integer order;
	private String  loanForm;
	private Integer	currStatusDays;
	private String  statusEndDate;
	private LoanFormInfo preNode;
	/**
	 * @return the order
	 */
	public Integer getOrder() {
		return order;
	}
	/**
	 * @param order the order to set
	 */
	public void setOrder(Integer order) {
		this.order = order;
	}
	/**
	 * @return the loanForm
	 */
	public String getLoanForm() {
		return loanForm;
	}
	/**
	 * @param loanForm the loanForm to set
	 */
	public void setLoanForm(String loanForm) {
		this.loanForm = loanForm;
	}
	/**
	 * @return the currStatusDays
	 */
	public Integer getCurrStatusDays() {
		return currStatusDays;
	}
	/**
	 * @param currStatusDays the currStatusDays to set
	 */
	public void setCurrStatusDays(Integer currStatusDays) {
		this.currStatusDays = currStatusDays;
	}
	/**
	 * @return the statusEndDate
	 */
	public String getStatusEndDate() {
		return statusEndDate;
	}
	/**
	 * @param statusEndDate the statusEndDate to set
	 */
	public void setStatusEndDate(String statusEndDate) {
		this.statusEndDate = statusEndDate;
	}
	/**
	 * @return the preNode
	 */
	public LoanFormInfo getPreNode() {
		return preNode;
	}
	/**
	 * @param preNode the preNode to set
	 */
	public void setPreNode(LoanFormInfo preNode) {
		this.preNode = preNode;
	}
}

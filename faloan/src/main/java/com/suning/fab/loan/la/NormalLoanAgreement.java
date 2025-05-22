package com.suning.fab.loan.la;

public class NormalLoanAgreement {
	private Integer graceDays;
	private String  repayChannel;
	private Boolean isPrepay;
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
	 * @return the repayChannel
	 */
	public String getRepayChannel() {
		return repayChannel;
	}
	/**
	 * @param repayChannel the repayChannel to set
	 */
	public void setRepayChannel(String repayChannel) {
		this.repayChannel = repayChannel;
	}
	/**
	 * @return the isPrepay
	 */
	public Boolean getIsPrepay() {
		return isPrepay;
	}
	/**
	 * @param isPrepay the isPrepay to set
	 */
	public void setIsPrepay(Boolean isPrepay) {
		this.isPrepay = isPrepay;
	}
}

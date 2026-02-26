package com.suning.fab.loan.la;

import java.io.Serializable;
import java.util.List;


public class Product implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1177864179398109103L;
	private String prdId;
	private String prdName;
	private Boolean available;
	private RateAgreement rateAgreement;
	private FeeAgreement feeAgreement;
	private RangeLimit rangeLimit;
	private GrantAgreement grantAgreement;
	private OpenAgreement openAgreement;
	private InterestAgreement interestAgreement;
	private WithdrawAgreement withdrawAgreement;
	
	private List<AbstractUserDefineAgreement> userDefineAgreement;
	/**
	 * @return the prdId
	 */
	public String getPrdId() {
		return prdId;
	}
	/**
	 * @param prdId the prdId to set
	 */
	public void setPrdId(String prdId) {
		this.prdId = prdId;
	}
	/**
	 * @return the prdName
	 */
	public String getPrdName() {
		return prdName;
	}
	/**
	 * @param prdName the prdName to set
	 */
	public void setPrdName(String prdName) {
		this.prdName = prdName;
	}
	/**
	 * @return the available
	 */
	public Boolean getAvailable() {
		return available;
	}
	/**
	 * @param available the available to set
	 */
	public void setAvailable(Boolean available) {
		this.available = available;
	}
	/**
	 * @return the rateAgreement
	 */
	public RateAgreement getRateAgreement() {
		return rateAgreement;
	}
	/**
	 * @param rateAgreement the rateAgreement to set
	 */
	public void setRateAgreement(RateAgreement rateAgreement) {
		this.rateAgreement = rateAgreement;
	}
	/**
	 * @return the feeAgreement
	 */
	public FeeAgreement getFeeAgreement() {
		return feeAgreement;
	}
	/**
	 * @param feeAgreement the feeAgreement to set
	 */
	public void setFeeAgreement(FeeAgreement feeAgreement) {
		this.feeAgreement = feeAgreement;
	}
	/**
	 * @return the rangeLimit
	 */
	public RangeLimit getRangeLimit() {
		return rangeLimit;
	}
	/**
	 * @param rangeLimit the rangeLimit to set
	 */
	public void setRangeLimit(RangeLimit rangeLimit) {
		this.rangeLimit = rangeLimit;
	}
	/**
	 * @return the grantAgreement
	 */
	public GrantAgreement getGrantAgreement() {
		return grantAgreement;
	}
	/**
	 * @param grantAgreement the grantAgreement to set
	 */
	public void setGrantAgreement(GrantAgreement grantAgreement) {
		this.grantAgreement = grantAgreement;
	}
	/**
	 * @return the openAgreement
	 */
	public OpenAgreement getOpenAgreement() {
		return openAgreement;
	}
	/**
	 * @param openAgreement the openAgreement to set
	 */
	public void setOpenAgreement(OpenAgreement openAgreement) {
		this.openAgreement = openAgreement;
	}
	/**
	 * @return the interestAgreement
	 */
	public InterestAgreement getInterestAgreement() {
		return interestAgreement;
	}
	/**
	 * @param interestAgreement the interestAgreement to set
	 */
	public void setInterestAgreement(InterestAgreement interestAgreement) {
		this.interestAgreement = interestAgreement;
	}
	/**
	 * @return the withdrawAgreement
	 */
	public WithdrawAgreement getWithdrawAgreement() {
		return withdrawAgreement;
	}
	/**
	 * @param withdrawAgreement the withdrawAgreement to set
	 */
	public void setWithdrawAgreement(WithdrawAgreement withdrawAgreement) {
		this.withdrawAgreement = withdrawAgreement;
	}
	/**
	 * @return the userDefineAgreement
	 */
	public List<AbstractUserDefineAgreement> getUserDefineAgreement() {
		return userDefineAgreement;
	}
	/**
	 * @param userDefineAgreement the userDefineAgreement to set
	 */
	public void setUserDefineAgreement(
			List<AbstractUserDefineAgreement> userDefineAgreement) {
		this.userDefineAgreement = userDefineAgreement;
	}
	
}

package com.suning.fab.loan.la;

import java.io.Serializable;

public class WithdrawAgreement implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7814431305682500355L;

	/*
	 * 还款渠道
	 */
	private String repayChannel; //还款渠道
	/*
	 * 是否提前还款 
	 */
	private Boolean isPrepay;
	/*
	 * 周期公式：3Y2YA2Y1M0410
	 */
	private String periodFormula;	
	/*
	 * 周期最小天数
	 */
	private Integer periodMinDays;
	

	/*
	 * 还款金额公式:1-等额本息  2-其他
	 */
	private String repayAmtFormula;

	/*
	 * 是否同意部分还款
	 */
	private String isAgreePartRepay;     //是否同意部分还款
	
	private String genCurrRepayPlanOpt;  //生成还款计划时机
	private String repayWay;             //还款方式
	private String intIssueS;		//扣息周期		定义到产品层
	private String issueType;		//期限类型		定义到产品层
	
	/*
	 * 允许的还款方式
	 */
	private String useRepayWay;
	
	/*
	 * 首期是否需要加上周期月数 
	 */
	private Boolean firstTermMonth;
	
	/*
	 * 中间期是否需要合并
	 */
	private Boolean middleTermMonth;
	
	/*
	 * 最后一期是否需要合并
	 */
	private Boolean lastTermMerge;
	/*
	 * 最后两期是否需要合并
	 */
	private Boolean lastTowTermMerge = false;
	/*
	 * 到期日按工作日延期
	 */
	private Boolean endDateDelay;

	/**
	 * Gets the value of endDateDelay.
	 *
	 * @return the value of endDateDelay
	 */
	public Boolean getEndDateDelay() {
		return endDateDelay;
	}

	/**
	 * Sets the endDateDelay.
	 *
	 * @param endDateDelay endDateDelay
	 */
	public void setEndDateDelay(Boolean endDateDelay) {
		this.endDateDelay = endDateDelay;

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
	/**
	 * @return the periodFormula
	 */
	public String getPeriodFormula() {
		return periodFormula;
	}
	/**
	 * @param periodFormula the periodFormula to set
	 */
	public void setPeriodFormula(String periodFormula) {
		this.periodFormula = periodFormula;
	}
	/**
	 * @return the periodMinDays
	 */
	public Integer getPeriodMinDays() {
		return periodMinDays;
	}
	/**
	 * @param periodMinDays the periodMinDays to set
	 */
	public void setPeriodMinDays(Integer periodMinDays) {
		this.periodMinDays = periodMinDays;
	}
	/**
	 * @return the repayAmtFormula
	 */
	public String getRepayAmtFormula() {
		return repayAmtFormula;
	}
	/**
	 * @param repayAmtFormula the repayAmtFormula to set
	 */
	public void setRepayAmtFormula(String repayAmtFormula) {
		this.repayAmtFormula = repayAmtFormula;
	}
	/**
	 * @return the isAgreePartRepay
	 */
	public String getIsAgreePartRepay() {
		return isAgreePartRepay;
	}
	/**
	 * @param isAgreePartRepay the isAgreePartRepay to set
	 */
	public void setIsAgreePartRepay(String isAgreePartRepay) {
		this.isAgreePartRepay = isAgreePartRepay;
	}
	/**
	 * @return the genCurrRepayPlanOpt
	 */
	public String getGenCurrRepayPlanOpt() {
		return genCurrRepayPlanOpt;
	}
	/**
	 * @param genCurrRepayPlanOpt the genCurrRepayPlanOpt to set
	 */
	public void setGenCurrRepayPlanOpt(String genCurrRepayPlanOpt) {
		this.genCurrRepayPlanOpt = genCurrRepayPlanOpt;
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
	 * @return the intIssueS
	 */
	public String getIntIssueS() {
		return intIssueS;
	}
	/**
	 * @param intIssueS the intIssueS to set
	 */
	public void setIntIssueS(String intIssueS) {
		this.intIssueS = intIssueS;
	}
	/**
	 * @return the issueType
	 */
	public String getIssueType() {
		return issueType;
	}
	/**
	 * @param issueType the issueType to set
	 */
	public void setIssueType(String issueType) {
		this.issueType = issueType;
	}
	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Boolean getFirstTermMonth() {
		return firstTermMonth;
	}
	public void setFirstTermMonth(Boolean firstTermMonth) {
		this.firstTermMonth = firstTermMonth;
	}
	public Boolean getMiddleTermMonth() {
		return middleTermMonth;
	}
	public void setMiddleTermMonth(Boolean middleTermMonth) {
		this.middleTermMonth = middleTermMonth;
	}
	public Boolean getLastTermMerge() {
		return lastTermMerge;
	}
	public void setLastTermMerge(Boolean lastTermMerge) {
		this.lastTermMerge = lastTermMerge;
	}
	/**
	 * @return the useRepayWay
	 */
	public String getUseRepayWay() {
		return useRepayWay;
	}
	/**
	 * @param useRepayWay the useRepayWay to set
	 */
	public void setUseRepayWay(String useRepayWay) {
		this.useRepayWay = useRepayWay;
	}

	/**
	 * @return the lastTowTermMerge
	 */
	public Boolean getLastTowTermMerge() {
		return lastTowTermMerge;
	}

	/**
	 * @param lastTowTermMerge the lastTowTermMerge to set
	 */
	public void setLastTowTermMerge(Boolean lastTowTermMerge) {
		this.lastTowTermMerge = lastTowTermMerge;
	}
	
}

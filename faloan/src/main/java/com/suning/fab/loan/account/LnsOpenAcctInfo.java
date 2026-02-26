/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: LnsOpenAcctInfo.java
 * Author:   15032049
 * Date:     2017年6月9日 下午7:28:23
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.account;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:贷款信息查询类，用于返回报文使用
 *
 * @author 15032049
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsOpenAcctInfo {
	
	private String openBrc;		//公司代码
	private String prdCode;		//产品代码
	private String customId;	//客户代码
	private String customName;	//客户名称
	private String receiptNo;	//借据号
	private String acctNo;		//贷款帐号
	private String loanStat;	//贷款状态
	private String openDate;	//贷款发放日
	private String contdueDate;	//合同到期日
	private String beginintDate;//起息日期
	private String prinTerms;	//本金期数
	private String intTerms;	//利息期数
	private String graceDays;	//宽限期
	private String repayWay;	//还款方式
	private String normalRate;	//正常年利率
	private String contractAmt;	//放款金额
	private String deductionAmt;//扣息金额
	private String contractBal;	//未还本金
	private String channelType;	//渠道详情
	
	
	private String prdName;//产品名称


	/**
	 * @return the openBrc
	 */
	public String getOpenBrc() {
		return openBrc;
	}


	/**
	 * @param openBrc the openBrc to set
	 */
	public void setOpenBrc(String openBrc) {
		this.openBrc = openBrc;
	}


	/**
	 * @return the prdCode
	 */
	public String getPrdCode() {
		return prdCode;
	}


	/**
	 * @param prdCode the prdCode to set
	 */
	public void setPrdCode(String prdCode) {
		this.prdCode = prdCode;
	}


	/**
	 * @return the customId
	 */
	public String getCustomId() {
		return customId;
	}


	/**
	 * @param customId the customId to set
	 */
	public void setCustomId(String customId) {
		this.customId = customId;
	}


	/**
	 * @return the customName
	 */
	public String getCustomName() {
		return customName;
	}


	/**
	 * @param customName the customName to set
	 */
	public void setCustomName(String customName) {
		this.customName = customName;
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
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}


	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
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
	 * @return the contdueDate
	 */
	public String getContdueDate() {
		return contdueDate;
	}


	/**
	 * @param contdueDate the contdueDate to set
	 */
	public void setContdueDate(String contdueDate) {
		this.contdueDate = contdueDate;
	}


	/**
	 * @return the beginintDate
	 */
	public String getBeginintDate() {
		return beginintDate;
	}


	/**
	 * @param beginintDate the beginintDate to set
	 */
	public void setBeginintDate(String beginintDate) {
		this.beginintDate = beginintDate;
	}


	/**
	 * @return the prinTerms
	 */
	public String getPrinTerms() {
		return prinTerms;
	}


	/**
	 * @param prinTerms the prinTerms to set
	 */
	public void setPrinTerms(String prinTerms) {
		this.prinTerms = prinTerms;
	}


	/**
	 * @return the intTerms
	 */
	public String getIntTerms() {
		return intTerms;
	}


	/**
	 * @param intTerms the intTerms to set
	 */
	public void setIntTerms(String intTerms) {
		this.intTerms = intTerms;
	}


	/**
	 * @return the graceDays
	 */
	public String getGraceDays() {
		return graceDays;
	}


	/**
	 * @param graceDays the graceDays to set
	 */
	public void setGraceDays(String graceDays) {
		this.graceDays = graceDays;
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
	 * @return the normalRate
	 */
	public String getNormalRate() {
		return normalRate;
	}


	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(String normalRate) {
		this.normalRate = normalRate;
	}


	/**
	 * @return the contractAmt
	 */
	public String getContractAmt() {
		return contractAmt;
	}


	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(String contractAmt) {
		this.contractAmt = contractAmt;
	}


	/**
	 * @return the deductionAmt
	 */
	public String getDeductionAmt() {
		return deductionAmt;
	}


	/**
	 * @param deductionAmt the deductionAmt to set
	 */
	public void setDeductionAmt(String deductionAmt) {
		this.deductionAmt = deductionAmt;
	}


	/**
	 * @return the contractBal
	 */
	public String getContractBal() {
		return contractBal;
	}


	/**
	 * @param contractBal the contractBal to set
	 */
	public void setContractBal(String contractBal) {
		this.contractBal = contractBal;
	}


	/**
	 * @return the channelType
	 */
	public String getChannelType() {
		return channelType;
	}


	/**
	 * @param channelType the channelType to set
	 */
	public void setChannelType(String channelType) {
		this.channelType = channelType;
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


	
}

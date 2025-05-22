/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: LnsAcctInfoQuery.java
 * Author:   16071579
 * Date:     2017年6月6日 下午7:28:23
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.account;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:账户信息查询类，中间过渡类
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsAcctInfoQueryM {
	
	private String acctNo;		//贷款帐号
	private String receiptNo;	//贷款借据号
	private String openDate;	//开户日期
	private String acctStat;	//账户状态
	private Double currBal;		//当前余额
	private Double lastBal;		//昨日余额
	private String preTranDate;	//最后交易日期
	private String customId;	//客户代码
	private String customName;	//客户名称
	private String prdCode;		//产品代码
	
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
	 * @return the acctStat
	 */
	public String getAcctStat() {
		return acctStat;
	}
	/**
	 * @param acctStat the acctStat to set
	 */
	public void setAcctStat(String acctStat) {
		this.acctStat = acctStat;
	}
	/**
	 * @return the currBal
	 */
	public Double getCurrBal() {
		return currBal;
	}
	/**
	 * @param currBal the currBal to set
	 */
	public void setCurrBal(Double currBal) {
		this.currBal = currBal;
	}
	/**
	 * @return the lastBal
	 */
	public Double getLastBal() {
		return lastBal;
	}
	/**
	 * @param lastBal the lastBal to set
	 */
	public void setLastBal(Double lastBal) {
		this.lastBal = lastBal;
	}
	/**
	 * @return the preTranDate
	 */
	public String getPreTranDate() {
		return preTranDate;
	}
	/**
	 * @param preTranDate the preTranDate to set
	 */
	public void setPreTranDate(String preTranDate) {
		this.preTranDate = preTranDate;
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
	
}

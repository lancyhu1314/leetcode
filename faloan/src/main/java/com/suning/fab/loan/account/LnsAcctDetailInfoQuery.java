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

import com.suning.fab.tup4j.amount.FabAmount;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:账户明细查询类，用于返回报文使用
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsAcctDetailInfoQuery {

	private String customId;	//客户代码
	private String customName;	//客户名称
	private String receiptNo;	//借据号
	private String openBrc;		//公司代码
	private String tranDate;	//账务日期
	private String acctNo;		//贷款帐号
	private String acctStat;	//账户类型
	private String cdFlag;		//收支标志
	private FabAmount tranAmt;	//发生额
	private FabAmount bal;		//余额
	private String memoName;	//描述
	private String oppAcctNo;	//交易对方帐号
	private String prdName;		//产品名名称
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
	 * @return the tranDate
	 */
	public String getTranDate() {
		return tranDate;
	}
	/**
	 * @param tranDate the tranDate to set
	 */
	public void setTranDate(String tranDate) {
		this.tranDate = tranDate;
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
	 * @return the cdFlag
	 */
	public String getCdFlag() {
		return cdFlag;
	}
	/**
	 * @param cdFlag the cdFlag to set
	 */
	public void setCdFlag(String cdFlag) {
		this.cdFlag = cdFlag;
	}
	/**
	 * @return the tranAmt
	 */
	public FabAmount getTranAmt() {
		return tranAmt;
	}
	/**
	 * @param tranAmt the tranAmt to set
	 */
	public void setTranAmt(FabAmount tranAmt) {
		this.tranAmt = tranAmt;
	}
	/**
	 * @return the bal
	 */
	public FabAmount getBal() {
		return bal;
	}
	/**
	 * @param bal the bal to set
	 */
	public void setBal(FabAmount bal) {
		this.bal = bal;
	}
	/**
	 * @return the memoName
	 */
	public String getMemoName() {
		return memoName;
	}
	/**
	 * @param memoName the memoName to set
	 */
	public void setMemoName(String memoName) {
		this.memoName = memoName;
	}
	/**
	 * @return the oppAcctNo
	 */
	public String getOppAcctNo() {
		return oppAcctNo;
	}
	/**
	 * @param oppAcctNo the oppAcctNo to set
	 */
	public void setOppAcctNo(String oppAcctNo) {
		this.oppAcctNo = oppAcctNo;
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

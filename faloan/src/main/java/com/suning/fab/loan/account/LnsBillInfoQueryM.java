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
 * 〈功能详细描述〉:账单表查询类，用于返回报文使用
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsBillInfoQueryM {
	
	private String tranDate; 		// 账务日期
	private String txSeq; 			// 子序号
	private String acctNo;			// 账号
	private String billType;		// 账单类型 PRIN本金NINT利息DINT罚息CINT复利
	private String period;			// 期数
	private Double billAmt;			// 账单金额
	private Double billBal;			// 账单余额
	private Double lastBal;			// 上日余额
	private String lastDate;		// 上笔日期
	private Double prinBal;			// 账单对应本金余额
	private String billRate;		// 账单执行利率
	private String beginDate;		// 账单起始日期
	private String endDate;			// 账单结束日期
	private String curendDate;		// 当期到期日
	private String repayeDate;		// 账单应还款止日
	private String settleDate;		// 账单结清日期
	private String inteDate;		// 利息计止日期
	private String finteDate;		// 罚息计止日期
	private String cinteDate;		// 复利计至日期
	private String repayWay;		// 计息方式0等本等息1等额本息2等额本金
	private String billStatus;		// 账单状态 N正常G宽限期O逾期L呆滞B呆账
	private String statusBDate;		// 账单状态开始日期
	private String billProperty;	// 账单属性 INTSET正常结息 REPAY还款
	private String intRecordFlag;	// 利息入账标志 NO未入 YES已入
	private String cancelFlag;		// 账单作废标志 NORMAL正常 CANCEL作废
	private String settleFlag;		// 结清标志 RUNNING未结 CLOSE已结
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
	 * @return the txSeq
	 */
	public String getTxSeq() {
		return txSeq;
	}
	/**
	 * @param txSeq the txSeq to set
	 */
	public void setTxSeq(String txSeq) {
		this.txSeq = txSeq;
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
	 * @return the billType
	 */
	public String getBillType() {
		return billType;
	}
	/**
	 * @param billType the billType to set
	 */
	public void setBillType(String billType) {
		this.billType = billType;
	}
	/**
	 * @return the period
	 */
	public String getPeriod() {
		return period;
	}
	/**
	 * @param period the period to set
	 */
	public void setPeriod(String period) {
		this.period = period;
	}
	/**
	 * @return the billAmt
	 */
	public Double getBillAmt() {
		return billAmt;
	}
	/**
	 * @param billAmt the billAmt to set
	 */
	public void setBillAmt(Double billAmt) {
		this.billAmt = billAmt;
	}
	/**
	 * @return the billBal
	 */
	public Double getBillBal() {
		return billBal;
	}
	/**
	 * @param billBal the billBal to set
	 */
	public void setBillBal(Double billBal) {
		this.billBal = billBal;
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
	 * @return the lastDate
	 */
	public String getLastDate() {
		return lastDate;
	}
	/**
	 * @param lastDate the lastDate to set
	 */
	public void setLastDate(String lastDate) {
		this.lastDate = lastDate;
	}
	/**
	 * @return the prinBal
	 */
	public Double getPrinBal() {
		return prinBal;
	}
	/**
	 * @param prinBal the prinBal to set
	 */
	public void setPrinBal(Double prinBal) {
		this.prinBal = prinBal;
	}
	/**
	 * @return the billRate
	 */
	public String getBillRate() {
		return billRate;
	}
	/**
	 * @param billRate the billRate to set
	 */
	public void setBillRate(String billRate) {
		this.billRate = billRate;
	}
	/**
	 * @return the beginDate
	 */
	public String getBeginDate() {
		return beginDate;
	}
	/**
	 * @param beginDate the beginDate to set
	 */
	public void setBeginDate(String beginDate) {
		this.beginDate = beginDate;
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
	 * @return the curendDate
	 */
	public String getCurendDate() {
		return curendDate;
	}
	/**
	 * @param curendDate the curendDate to set
	 */
	public void setCurendDate(String curendDate) {
		this.curendDate = curendDate;
	}
	/**
	 * @return the repayeDate
	 */
	public String getRepayeDate() {
		return repayeDate;
	}
	/**
	 * @param repayeDate the repayeDate to set
	 */
	public void setRepayeDate(String repayeDate) {
		this.repayeDate = repayeDate;
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
	 * @return the inteDate
	 */
	public String getInteDate() {
		return inteDate;
	}
	/**
	 * @param inteDate the inteDate to set
	 */
	public void setInteDate(String inteDate) {
		this.inteDate = inteDate;
	}
	/**
	 * @return the finteDate
	 */
	public String getFinteDate() {
		return finteDate;
	}
	/**
	 * @param finteDate the finteDate to set
	 */
	public void setFinteDate(String finteDate) {
		this.finteDate = finteDate;
	}
	/**
	 * @return the cinteDate
	 */
	public String getCinteDate() {
		return cinteDate;
	}
	/**
	 * @param cinteDate the cinteDate to set
	 */
	public void setCinteDate(String cinteDate) {
		this.cinteDate = cinteDate;
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
	 * @return the billStatus
	 */
	public String getBillStatus() {
		return billStatus;
	}
	/**
	 * @param billStatus the billStatus to set
	 */
	public void setBillStatus(String billStatus) {
		this.billStatus = billStatus;
	}
	/**
	 * @return the statusBDate
	 */
	public String getStatusBDate() {
		return statusBDate;
	}
	/**
	 * @param statusBDate the statusBDate to set
	 */
	public void setStatusBDate(String statusBDate) {
		this.statusBDate = statusBDate;
	}
	/**
	 * @return the billProperty
	 */
	public String getBillProperty() {
		return billProperty;
	}
	/**
	 * @param billProperty the billProperty to set
	 */
	public void setBillProperty(String billProperty) {
		this.billProperty = billProperty;
	}
	/**
	 * @return the intRecordFlag
	 */
	public String getIntRecordFlag() {
		return intRecordFlag;
	}
	/**
	 * @param intRecordFlag the intRecordFlag to set
	 */
	public void setIntRecordFlag(String intRecordFlag) {
		this.intRecordFlag = intRecordFlag;
	}
	/**
	 * @return the cancelFlag
	 */
	public String getCancelFlag() {
		return cancelFlag;
	}
	/**
	 * @param cancelFlag the cancelFlag to set
	 */
	public void setCancelFlag(String cancelFlag) {
		this.cancelFlag = cancelFlag;
	}
	/**
	 * @return the settleFlag
	 */
	public String getSettleFlag() {
		return settleFlag;
	}
	/**
	 * @param settleFlag the settleFlag to set
	 */
	public void setSettleFlag(String settleFlag) {
		this.settleFlag = settleFlag;
	}

}

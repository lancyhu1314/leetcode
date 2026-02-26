package com.suning.fab.loan.workunit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 还款--任性贷退货渠道
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns210 extends WorkUnit { 

	LoanAgreement loanAgreement;
	String		fundChannel;
	String		acctNo;
	FabAmount	refundAmt;
	FabAmount	offsetAmt;
	String		serialNo;
	String		termDate;
	String		tranCode;
	String		repayAcctNo;
	String		brc;
	String		outSerialNo;
	
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
	
		FundInvest fundInvest = null;
		//getRefundAmt任性贷退款金额 在还款渠道子交易lns212还有一步记账
		if(getRefundAmt() != null && getRefundAmt().isPositive())
			eventProvider.createEvent("GOODRETURN", getRefundAmt(), null, null, fundInvest, "THQD", ctx);
		
		//getOffsetAmt任性贷客户用已还的本金用来单独还罚息和利息
		//该场景业务上要求repayAmt等于罚息利息和   getOffsetAmt小于等于repayAmt
		//getOffsetAmt小于repayAmt时候 lns212交易的渠道金额等于 totalRepayAmt-getOffsetAmt 理论上repayAmt=totalRepayAmt
		//getOffsetAmt等于repayAmt时候 lns212交易空跑
		if(getOffsetAmt() != null && getOffsetAmt().isPositive())
			eventProvider.createEvent("GOODRETURN", getOffsetAmt(), null, null, fundInvest, "YHHX", ctx);
	}
	/**
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}
	/**
	 * @param loanAgreement the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}
	/**
	 * @return the fundChannel
	 */
	public String getFundChannel() {
		return fundChannel;
	}
	/**
	 * @param fundChannel the fundChannel to set
	 */
	public void setFundChannel(String fundChannel) {
		this.fundChannel = fundChannel;
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
	 * @return the refundAmt
	 */
	public FabAmount getRefundAmt() {
		return refundAmt;
	}
	/**
	 * @param refundAmt the refundAmt to set
	 */
	public void setRefundAmt(FabAmount refundAmt) {
		this.refundAmt = refundAmt;
	}
	/**
	 * @return the offsetAmt
	 */
	public FabAmount getOffsetAmt() {
		return offsetAmt;
	}
	/**
	 * @param offsetAmt the offsetAmt to set
	 */
	public void setOffsetAmt(FabAmount offsetAmt) {
		this.offsetAmt = offsetAmt;
	}
	/**
	 * @return the serialNo
	 */
	public String getSerialNo() {
		return serialNo;
	}
	/**
	 * @param serialNo the serialNo to set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	/**
	 * @return the termDate
	 */
	public String getTermDate() {
		return termDate;
	}
	/**
	 * @param termDate the termDate to set
	 */
	public void setTermDate(String termDate) {
		this.termDate = termDate;
	}
	/**
	 * @return the tranCode
	 */
	public String getTranCode() {
		return tranCode;
	}
	/**
	 * @param tranCode the tranCode to set
	 */
	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}
	/**
	 * @return the repayAcctNo
	 */
	public String getRepayAcctNo() {
		return repayAcctNo;
	}
	/**
	 * @param repayAcctNo the repayAcctNo to set
	 */
	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
	}
	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}
	/**
	 * @param brc the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}
	/**
	 * @return the outSerialNo
	 */
	public String getOutSerialNo() {
		return outSerialNo;
	}
	/**
	 * @param outSerialNo the outSerialNo to set
	 */
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}
	/**
	 * @return the eventProvider
	 */
	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}
	/**
	 * @param eventProvider the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}


}

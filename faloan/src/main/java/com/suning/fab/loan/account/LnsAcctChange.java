package com.suning.fab.loan.account;

import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.account.AcctChange;
import com.suning.fab.tup4j.account.AcctOperationType;
import com.suning.fab.tup4j.amount.Amount;

public class LnsAcctChange extends AcctChange {

	FundInvest fundInvest;
	String billTranDate;
	Integer billSerSeqno;
	Integer billTxnSeq;

	public LnsAcctChange(AcctOperationType aot, Amount tranAmt, String briefCode, Integer ctxnSeq, String ctxnCode,
			LnsAcctInfo oppAcctInfo, FundInvest fundInvest, String billTranDate, Integer billSerSeqno,
			Integer billTxnSeq) {
		super(aot, tranAmt, briefCode, ctxnSeq, ctxnCode, oppAcctInfo);
		this.fundInvest = fundInvest;
		this.billTranDate = billTranDate;
		this.billSerSeqno = billSerSeqno;
		this.billTxnSeq = billTxnSeq;
	}

	/**
	 * @return the fundInvest
	 */
	public FundInvest getFundInvest() {
		return fundInvest;
	}

	/**
	 * @param fundInvest the fundInvest to set
	 */
	public void setFundInvest(FundInvest fundInvest) {
		this.fundInvest = fundInvest;
	}

	/**
	 * @return the billTranDate
	 */
	public String getBillTranDate() {
		return billTranDate;
	}

	/**
	 * @param billTranDate the billTranDate to set
	 */
	public void setBillTranDate(String billTranDate) {
		this.billTranDate = billTranDate;
	}

	/**
	 * @return the billSerSeqno
	 */
	public Integer getBillSerSeqno() {
		return billSerSeqno;
	}

	/**
	 * @param billSerSeqno the billSerSeqno to set
	 */
	public void setBillSerSeqno(Integer billSerSeqno) {
		this.billSerSeqno = billSerSeqno;
	}

	/**
	 * @return the billTxnSeq
	 */
	public Integer getBillTxnSeq() {
		return billTxnSeq;
	}

	/**
	 * @param billTxnSeq the billTxnSeq to set
	 */
	public void setBillTxnSeq(Integer billTxnSeq) {
		this.billTxnSeq = billTxnSeq;
	}


}

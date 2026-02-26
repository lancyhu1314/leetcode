package com.suning.fab.loan.bo;

import com.suning.fab.tup4j.amount.FabAmount;

public class RepayAdvance {
	
	FabAmount	billAmt;
	String		billTrandate;
	Integer		billSerseqno;
	Integer		billTxseq;
	Integer		repayterm;



	String      billStatus;//账本状态
	
	
	public RepayAdvance() {
		super();
	}


	/**
	 * @return the billAmt
	 */
	public FabAmount getBillAmt() {
		return billAmt;
	}


	/**
	 * @param billAmt the billAmt to set
	 */
	public void setBillAmt(FabAmount billAmt) {
		this.billAmt = billAmt;
	}


	/**
	 * @return the billTrandate
	 */
	public String getBillTrandate() {
		return billTrandate;
	}


	/**
	 * @param billTrandate the billTrandate to set
	 */
	public void setBillTrandate(String billTrandate) {
		this.billTrandate = billTrandate;
	}


	/**
	 * @return the billSerseqno
	 */
	public Integer getBillSerseqno() {
		return billSerseqno;
	}


	/**
	 * @param billSerseqno the billSerseqno to set
	 */
	public void setBillSerseqno(Integer billSerseqno) {
		this.billSerseqno = billSerseqno;
	}


	/**
	 * @return the billTxseq
	 */
	public Integer getBillTxseq() {
		return billTxseq;
	}


	/**
	 * @param billTxseq the billTxseq to set
	 */
	public void setBillTxseq(Integer billTxseq) {
		this.billTxseq = billTxseq;
	}


	/**
	 * @return the repayterm
	 */
	public Integer getRepayterm() {
		return repayterm;
	}


	/**
	 * @param repayterm the repayterm to set
	 */
	public void setRepayterm(Integer repayterm) {
		this.repayterm = repayterm;
	}

	public String getBillStatus() {
		return billStatus;
	}

	public void setBillStatus(String billStatus) {
		this.billStatus = billStatus;
	}


}

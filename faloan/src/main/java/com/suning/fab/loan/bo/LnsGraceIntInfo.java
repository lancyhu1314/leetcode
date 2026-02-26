package com.suning.fab.loan.bo;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class LnsGraceIntInfo {

	private Date tranDate;
	private Integer serSeqno;
	private Integer txSeq ;
	private List<LnsBill> prinBill = new ArrayList<LnsBill>();
	private List<LnsBill> graceIntBill = new ArrayList<LnsBill>();
	/**
	 * @return the tranDate
	 */
	public Date getTranDate() {
		return tranDate;
	}
	/**
	 * @param tranDate the tranDate to set
	 */
	public void setTranDate(Date tranDate) {
		this.tranDate = tranDate;
	}
	/**
	 * @return the serSeqno
	 */
	public Integer getSerSeqno() {
		return serSeqno;
	}
	/**
	 * @param serSeqno the serSeqno to set
	 */
	public void setSerSeqno(Integer serSeqno) {
		this.serSeqno = serSeqno;
	}
	/**
	 * @return the txSeq
	 */
	public Integer getTxSeq() {
		return txSeq;
	}
	/**
	 * @param txSeq the txSeq to set
	 */
	public void setTxSeq(Integer txSeq) {
		this.txSeq = txSeq;
	}
	/**
	 * @return the prinBill
	 */
	public List<LnsBill> getPrinBill() {
		return prinBill;
	}
	/**
	 * @param prinBill the prinBill to set
	 */
	public void setPrinBill(List<LnsBill> prinBill) {
		this.prinBill = prinBill;
	}
	/**
	 * @return the graceIntBill
	 */
	public List<LnsBill> getGraceIntBill() {
		return graceIntBill;
	}
	/**
	 * @param graceIntBill the graceIntBill to set
	 */
	public void setGraceIntBill(List<LnsBill> graceIntBill) {
		this.graceIntBill = graceIntBill;
	}

}

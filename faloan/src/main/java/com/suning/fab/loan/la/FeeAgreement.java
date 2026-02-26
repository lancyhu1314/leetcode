package com.suning.fab.loan.la;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.tup4j.amount.FabRate;

public class FeeAgreement implements Serializable{

	private static final long serialVersionUID = 1858009436148248099L;

	private String feeType;
	private List<TblLnsfeeinfo> lnsfeeinfos;

	/**
	 * Gets the value of lnsfeeinfos.
	 *
	 * @return the value of lnsfeeinfos
	 */
	public List<TblLnsfeeinfo> getLnsfeeinfos() {
		return lnsfeeinfos;
	}
	public boolean isEmpty() {
		if(lnsfeeinfos==null) return true;
		return lnsfeeinfos.isEmpty();
	}
	/**
	 * Sets the lnsfeeinfos.
	 *
	 * @param lnsfeeinfos lnsfeeinfos
	 */
	public void setLnsfeeinfos(List<TblLnsfeeinfo> lnsfeeinfos) {
		this.lnsfeeinfos = lnsfeeinfos;

	}

	/**
	 * Gets the value of feeType.
	 *
	 * @return the value of feeType
	 */
	public String getFeeType() {
		return feeType;
	}

	/**
	 * Sets the feeType.
	 *
	 * @param feeType feeType
	 */
	public void setFeeType(String feeType) {
		this.feeType = feeType;

	}
}

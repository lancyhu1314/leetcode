package com.suning.fab.loan.bo;

import com.suning.fab.loan.domain.TblLnsbill;

public class LnsBillExtend {

	
	TblLnsbill lnsBill;
	String endDate;
	

	public LnsBillExtend(TblLnsbill lnsBill) {
		super();
		this.lnsBill = lnsBill;
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
	 * @return the lnsBill
	 */
	public TblLnsbill getLnsBill() {
		return lnsBill;
	}

	/**
	 * @param lnsBill the lnsBill to set
	 */
	public void setLnsBill(TblLnsbill lnsBill) {
		this.lnsBill = lnsBill;
	}
	
}

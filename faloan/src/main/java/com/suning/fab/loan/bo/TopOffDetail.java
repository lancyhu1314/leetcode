package com.suning.fab.loan.bo;

import com.suning.fab.tup4j.amount.FabAmount;

public class TopOffDetail {
	private String begindate;
	private String enddate;

	private String dintbdate;
	private String dintedate;
	private FabAmount noretprin;
	private Double dealrate;
	private FabAmount calDint;
	private FabAmount sumrfint;
	/**
	 * @return the begindate
	 */
	public String getBegindate() {
		return begindate;
	}

	/**
	 * @param begindate to set
	 */
	public void setBegindate(String begindate) {
		this.begindate = begindate;
	}

	/**
	 * @return the enddate
	 */
	public String getEnddate() {
		return enddate;
	}

	/**
	 * @param enddate to set
	 */
	public void setEnddate(String enddate) {
		this.enddate = enddate;
	}

	/**
	 * @return the dintbdate
	 */
	public String getDintbdate() {
		return dintbdate;
	}

	/**
	 * @param dintbdate to set
	 */
	public void setDintbdate(String dintbdate) {
		this.dintbdate = dintbdate;
	}

	/**
	 * @return the dintedate
	 */
	public String getDintedate() {
		return dintedate;
	}

	/**
	 * @param dintedate to set
	 */
	public void setDintedate(String dintedate) {
		this.dintedate = dintedate;
	}

	/**
	 * @return the noretprin
	 */
	public FabAmount getNoretprin() {
		return noretprin;
	}

	/**
	 * @param noretprin to set
	 */
	public void setNoretprin(FabAmount noretprin) {
		this.noretprin = noretprin;
	}

	/**
	 * @return the dealrate
	 */
	public Double getDealrate() {
		return dealrate;
	}

	/**
	 * @param dealrate to set
	 */
	public void setDealrate(Double dealrate) {
		this.dealrate = dealrate;
	}

	/**
	 * @return the calDint
	 */
	public FabAmount getCalDint() {
		return calDint;
	}

	/**
	 * @param calDint to set
	 */
	public void setCalDint(FabAmount calDint) {

		this.calDint = calDint;
	}

	/**
	 * @return the sumrfint
	 */
	public FabAmount getSumrfint() {
		return sumrfint;
	}

	/**
	 * @param sumrfint to set
	 */
	public void setSumrfint(FabAmount sumrfint) {
		this.sumrfint = sumrfint;
	}
}

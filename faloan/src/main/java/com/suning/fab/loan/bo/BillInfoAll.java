package com.suning.fab.loan.bo;

import java.util.List;

public class BillInfoAll {

	Integer intTotalPeriod;
	Integer prinTotalPeriod;
	String  firstRepayPrinDate;
	String  firstRepayIntDate;
	List<BillInfo> BillInfoList ;
	/**
	 * @return the intTotalPeriod
	 */
	public Integer getIntTotalPeriod() {
		return intTotalPeriod;
	}
	/**
	 * @param intTotalPeriod the intTotalPeriod to set
	 */
	public void setIntTotalPeriod(Integer intTotalPeriod) {
		this.intTotalPeriod = intTotalPeriod;
	}
	/**
	 * @return the prinTotalPeriod
	 */
	public Integer getPrinTotalPeriod() {
		return prinTotalPeriod;
	}
	/**
	 * @param prinTotalPeriod the prinTotalPeriod to set
	 */
	public void setPrinTotalPeriod(Integer prinTotalPeriod) {
		this.prinTotalPeriod = prinTotalPeriod;
	}
	/**
	 * @return the firstRepayPrinDate
	 */
	public String getFirstRepayPrinDate() {
		return firstRepayPrinDate;
	}
	/**
	 * @param firstRepayPrinDate the firstRepayPrinDate to set
	 */
	public void setFirstRepayPrinDate(String firstRepayPrinDate) {
		this.firstRepayPrinDate = firstRepayPrinDate;
	}
	/**
	 * @return the firstRepayIntDate
	 */
	public String getFirstRepayIntDate() {
		return firstRepayIntDate;
	}
	/**
	 * @param firstRepayIntDate the firstRepayIntDate to set
	 */
	public void setFirstRepayIntDate(String firstRepayIntDate) {
		this.firstRepayIntDate = firstRepayIntDate;
	}
	/**
	 * @return the billInfoList
	 */
	public List<BillInfo> getBillInfoList() {
		return BillInfoList;
	}
	/**
	 * @param billInfoList the billInfoList to set
	 */
	public void setBillInfoList(List<BillInfo> billInfoList) {
		BillInfoList = billInfoList;
	}


}

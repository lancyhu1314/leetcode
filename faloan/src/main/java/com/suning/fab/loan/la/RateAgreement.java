package com.suning.fab.loan.la;

import java.io.Serializable;

import com.suning.fab.tup4j.amount.FabRate;

public class RateAgreement implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6760118493778154L;
	private  FabRate normalRate;	//贷款正常利率	
	private  FabRate overdueRate;	//贷款逾期利率
	private  FabRate compoundRate;	//贷款复利利率
	/**
	 * @return the normalRate
	 */
	public FabRate getNormalRate() {
		return normalRate;
	}
	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}
	/**
	 * @return the overdueRate
	 */
	public FabRate getOverdueRate() {
		return overdueRate;
	}
	/**
	 * @param overdueRate the overdueRate to set
	 */
	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
	}
	/**
	 * @return the compoundRate
	 */
	public FabRate getCompoundRate() {
		return compoundRate;
	}
	/**
	 * @param compoundRate the compoundRate to set
	 */
	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
	}
	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	

}

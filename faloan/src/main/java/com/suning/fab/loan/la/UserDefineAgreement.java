package com.suning.fab.loan.la;

import java.io.Serializable;
import java.math.BigDecimal;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;

public class UserDefineAgreement extends AbstractUserDefineAgreement{

	private Integer repayTerm;   //当前期数
	private String termbDate;	//本期起日
	private String termeDate;   //本期止日
	private FabAmount termPrin;	//本期应还本金
	private FabAmount termInt;	//本期应还利息
	private Integer days;		//宽限期天数
	private FabRate normalRate;	//正常利率
	private FabRate overdueRate;	//正常利率
	private FabRate compoundRate;	//正常利率
	/**
	 * @return the repayTerm
	 */
	public Integer getRepayTerm() {
		return repayTerm;
	}
	/**
	 * @param repayTerm the repayTerm to set
	 */
	public void setRepayTerm(Integer repayTerm) {
		this.repayTerm = repayTerm;
	}
	/**
	 * @return the termeDate
	 */
	public String getTermeDate() {
		return termeDate;
	}
	/**
	 * @param termeDate the termeDate to set
	 */
	public void setTermeDate(String termeDate) {
		this.termeDate = termeDate;
	}
	/**
	 * @return the termPrin
	 */
	public FabAmount getTermPrin() {
		return termPrin;
	}
	/**
	 * @param termPrin the termPrin to set
	 */
	public void setTermPrin(FabAmount termPrin) {
		this.termPrin = termPrin;
	}
	/**
	 * @return the days
	 */
	public Integer getDays() {
		return days;
	}
	/**
	 * @param days the days to set
	 */
	public void setDays(Integer days) {
		this.days = days;
	}
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
	 * @return the termbDate
	 */
	public String getTermbDate() {
		return termbDate;
	}
	/**
	 * @param termbDate the termbDate to set
	 */
	public void setTermbDate(String termbDate) {
		this.termbDate = termbDate;
	}
	/**
	 * @return the termInt
	 */
	public FabAmount getTermInt() {
		return termInt;
	}
	/**
	 * @param termInt the termInt to set
	 */
	public void setTermInt(FabAmount termInt) {
		this.termInt = termInt;
	}
	
}

package com.suning.fab.loan.bo;

import com.suning.fab.tup4j.amount.FabAmount;

public class RpyList {
	
	Integer repayterm = 0;
	FabAmount prinAmt = new FabAmount();
	FabAmount intAmt = new FabAmount();
	FabAmount forfeitAmt = new FabAmount();
	FabAmount forfeetAmt = new FabAmount();
	FabAmount reduceIntAmt = new FabAmount();
	FabAmount reduceFintAmt = new FabAmount();
	FabAmount subPrinAmt = new FabAmount();
	FabAmount subIntAmt = new FabAmount();
	FabAmount subFintAmt = new FabAmount();
	FabAmount subFeeAmt = new FabAmount();
	String discountfeeFlag="N";//贴费标志
	String discountIntFlag="N";//贴息标志
	FabAmount couponFeeAmt=new FabAmount();//减免费用金额

	String nintBillStatus; //区分利息账本状态
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
	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}
	/**
	 * @param prinAmt the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}
	/**
	 * @return the intAmt
	 */
	public FabAmount getIntAmt() {
		return intAmt;
	}
	/**
	 * @param intAmt the intAmt to set
	 */
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}
	/**
	 * @return the forfeitAmt
	 */
	public FabAmount getForfeitAmt() {
		return forfeitAmt;
	}
	/**
	 * @param forfeitAmt the forfeitAmt to set
	 */
	public void setForfeitAmt(FabAmount forfeitAmt) {
		this.forfeitAmt = forfeitAmt;
	}
	/**
	 * @return the forfeetAmt
	 */
	public FabAmount getForfeetAmt() {
		return forfeetAmt;
	}
	/**
	 * @param forfeetAmt the forfeetAmt to set
	 */
	public void setForfeetAmt(FabAmount forfeetAmt) {
		this.forfeetAmt = forfeetAmt;
	}
	/**
	 * @return the reduceIntAmt
	 */
	public FabAmount getReduceIntAmt() {
		return reduceIntAmt;
	}
	/**
	 * @param reduceIntAmt the reduceIntAmt to set
	 */
	public void setReduceIntAmt(FabAmount reduceIntAmt) {
		this.reduceIntAmt = reduceIntAmt;
	}
	/**
	 * @return the reduceFintAmt
	 */
	public FabAmount getReduceFintAmt() {
		return reduceFintAmt;
	}
	/**
	 * @param reduceFintAmt the reduceFintAmt to set
	 */
	public void setReduceFintAmt(FabAmount reduceFintAmt) {
		this.reduceFintAmt = reduceFintAmt;
	}

	public String getNintBillStatus() {
		return nintBillStatus;
	}

	public void setNintBillStatus(String nintBillStatus) {
		this.nintBillStatus = nintBillStatus;
	}

	public FabAmount getCouponFeeAmt() {
		return couponFeeAmt;
	}

	public void setCouponFeeAmt(FabAmount couponFeeAmt) {
		this.couponFeeAmt = couponFeeAmt;
	}

	public String getDiscountfeeFlag() {
		return discountfeeFlag;
	}

	public void setDiscountfeeFlag(String discountfeeFlag) {
		this.discountfeeFlag = discountfeeFlag;
	}

	public String getDiscountIntFlag() {
		return discountIntFlag;
	}

	public void setDiscountIntFlag(String discountIntFlag) {
		this.discountIntFlag = discountIntFlag;
	}
	public FabAmount getSubFeeAmt() {
		return subFeeAmt;
	}
	public void setSubFeeAmt(FabAmount subFeeAmt) {
		this.subFeeAmt = subFeeAmt;
	}
	public FabAmount getSubPrinAmt() {
		return subPrinAmt;
	}
	public void setSubPrinAmt(FabAmount subPrinAmt) {
		this.subPrinAmt = subPrinAmt;
	}
	public FabAmount getSubIntAmt() {
		return subIntAmt;
	}
	public void setSubIntAmt(FabAmount subIntAmt) {
		this.subIntAmt = subIntAmt;
	}
	public FabAmount getSubFintAmt() {
		return subFintAmt;
	}
	public void setSubFintAmt(FabAmount subFintAmt) {
		this.subFintAmt = subFintAmt;
	}


}

package com.suning.fab.loan.bo;

import com.suning.fab.tup4j.amount.FabAmount;

public class PreRepay {
	String	  acctNo;
	String	  brc;
	FabAmount cleanPrin;
	FabAmount prinAmt;
	FabAmount cleanInt;
	FabAmount intAmt;
	FabAmount cleanForfeit;
	FabAmount forfeitAmt;
	FabAmount cleanForfee;
	FabAmount forfeetAmt;
	FabAmount cleanDamages;
	FabAmount damagesAmt;
	FabAmount overPrin;
	FabAmount overInt;
	FabAmount overDint;
	FabAmount overFee;
	FabAmount overDamages;
	FabAmount cleanFee;



	//未结本金
	FabAmount totalPrin;
	//未结利息
	FabAmount totalNint;
	//动态收益封顶
	FabAmount dynamicCapAmt;
	//动态收益封顶差值
	FabAmount dynamicCapDiff;
	//提前结清手续费收取方式
	String advanceFeeType;



	FabAmount exceedPrin;//逾期本金
	FabAmount exceedInt;//逾期利息

	String	  timestamp;
	
//	FabAmount totalClean;
//	FabAmount totalCurrent;
//	FabAmount totalOver;
//	FabAmount totalFeeClean;
//	FabAmount totalFeeCurrent;
//	FabAmount totalFeeOver;
	
	public PreRepay() {
		super();
		
		acctNo ="";
		brc ="";
		cleanPrin = new FabAmount();
		prinAmt = new FabAmount();
		cleanInt = new FabAmount();
		intAmt = new FabAmount();
		cleanForfeit = new FabAmount();
		forfeitAmt = new FabAmount();
		cleanForfee = new FabAmount();
		forfeetAmt = new FabAmount();
		cleanDamages = new FabAmount();
		damagesAmt = new FabAmount();
		overPrin = new FabAmount();
		overInt = new FabAmount();
		overDint = new FabAmount();
		overFee = new FabAmount();
		overDamages = new FabAmount();
		cleanFee = new FabAmount();
		exceedPrin=new FabAmount();
		exceedInt=new FabAmount();
		timestamp="";
		
		
		
//		totalClean = new FabAmount();
//		totalCurrent = new FabAmount();
//		totalOver = new FabAmount();
//		totalFeeClean = new FabAmount();
//		totalFeeCurrent = new FabAmount();
//		totalFeeOver = new FabAmount();
	}



	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}



	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}



	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}



	/**
	 * @param brc the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}



	/**
	 * @return the cleanPrin
	 */
	public FabAmount getCleanPrin() {
		return cleanPrin;
	}



	/**
	 * @param cleanPrin the cleanPrin to set
	 */
	public void setCleanPrin(FabAmount cleanPrin) {
		this.cleanPrin = cleanPrin;
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
	 * @return the cleanInt
	 */
	public FabAmount getCleanInt() {
		return cleanInt;
	}



	/**
	 * @param cleanInt the cleanInt to set
	 */
	public void setCleanInt(FabAmount cleanInt) {
		this.cleanInt = cleanInt;
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
	 * @return the cleanForfeit
	 */
	public FabAmount getCleanForfeit() {
		return cleanForfeit;
	}



	/**
	 * @param cleanForfeit the cleanForfeit to set
	 */
	public void setCleanForfeit(FabAmount cleanForfeit) {
		this.cleanForfeit = cleanForfeit;
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
	 * @return the overPrin
	 */
	public FabAmount getOverPrin() {
		return overPrin;
	}



	/**
	 * @param overPrin the overPrin to set
	 */
	public void setOverPrin(FabAmount overPrin) {
		this.overPrin = overPrin;
	}



	/**
	 * @return the overInt
	 */
	public FabAmount getOverInt() {
		return overInt;
	}



	/**
	 * @param overInt the overInt to set
	 */
	public void setOverInt(FabAmount overInt) {
		this.overInt = overInt;
	}



	/**
	 * @return the overDint
	 */
	public FabAmount getOverDint() {
		return overDint;
	}



	/**
	 * @param overDint the overDint to set
	 */
	public void setOverDint(FabAmount overDint) {
		this.overDint = overDint;
	}



	/**
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}



	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}



	/**
	 * @return the cleanFee
	 */
	public FabAmount getCleanFee() {
		return cleanFee;
	}



	/**
	 * @param cleanFee the cleanFee to set
	 */
	public void setCleanFee(FabAmount cleanFee) {
		this.cleanFee = cleanFee;
	}



	/**
	 * @return the cleanForfee
	 */
	public FabAmount getCleanForfee() {
		return cleanForfee;
	}



	/**
	 * @param cleanForfee the cleanForfee to set
	 */
	public void setCleanForfee(FabAmount cleanForfee) {
		this.cleanForfee = cleanForfee;
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
	 * @return the cleanDamages
	 */
	public FabAmount getCleanDamages() {
		return cleanDamages;
	}



	/**
	 * @param cleanDamages the cleanDamages to set
	 */
	public void setCleanDamages(FabAmount cleanDamages) {
		this.cleanDamages = cleanDamages;
	}



	/**
	 * @return the damagesAmt
	 */
	public FabAmount getDamagesAmt() {
		return damagesAmt;
	}



	/**
	 * @param damagesAmt the damagesAmt to set
	 */
	public void setDamagesAmt(FabAmount damagesAmt) {
		this.damagesAmt = damagesAmt;
	}



	/**
	 * @return the overFee
	 */
	public FabAmount getOverFee() {
		return overFee;
	}



	/**
	 * @param overFee the overFee to set
	 */
	public void setOverFee(FabAmount overFee) {
		this.overFee = overFee;
	}



	/**
	 * @return the overDamages
	 */
	public FabAmount getOverDamages() {
		return overDamages;
	}



	/**
	 * @param overDamages the overDamages to set
	 */
	public void setOverDamages(FabAmount overDamages) {
		this.overDamages = overDamages;
	}



//	/**
//	 * @return the totalClean
//	 */
//	public FabAmount getTotalClean() {
//		return totalClean;
//	}
//
//
//
//	/**
//	 * @param totalClean the totalClean to set
//	 */
//	public void setTotalClean(FabAmount totalClean) {
//		this.totalClean = totalClean;
//	}
//
//
//
//	/**
//	 * @return the totalCurrent
//	 */
//	public FabAmount getTotalCurrent() {
//		return totalCurrent;
//	}
//
//
//
//	/**
//	 * @param totalCurrent the totalCurrent to set
//	 */
//	public void setTotalCurrent(FabAmount totalCurrent) {
//		this.totalCurrent = totalCurrent;
//	}
//
//
//
//	/**
//	 * @return the totalOver
//	 */
//	public FabAmount getTotalOver() {
//		return totalOver;
//	}
//
//
//
//	/**
//	 * @param totalOver the totalOver to set
//	 */
//	public void setTotalOver(FabAmount totalOver) {
//		this.totalOver = totalOver;
//	}
//
//
//
//	/**
//	 * @return the totalFeeClean
//	 */
//	public FabAmount getTotalFeeClean() {
//		return totalFeeClean;
//	}
//
//
//
//	/**
//	 * @param totalFeeClean the totalFeeClean to set
//	 */
//	public void setTotalFeeClean(FabAmount totalFeeClean) {
//		this.totalFeeClean = totalFeeClean;
//	}
//
//
//
//	/**
//	 * @return the totalFeeCurrent
//	 */
//	public FabAmount getTotalFeeCurrent() {
//		return totalFeeCurrent;
//	}
//
//
//
//	/**
//	 * @param totalFeeCurrent the totalFeeCurrent to set
//	 */
//	public void setTotalFeeCurrent(FabAmount totalFeeCurrent) {
//		this.totalFeeCurrent = totalFeeCurrent;
//	}
//
//
//
//	/**
//	 * @return the totalFeeOver
//	 */
//	public FabAmount getTotalFeeOver() {
//		return totalFeeOver;
//	}
//
//
//
//	/**
//	 * @param totalFeeOver the totalFeeOver to set
//	 */
//	public void setTotalFeeOver(FabAmount totalFeeOver) {
//		this.totalFeeOver = totalFeeOver;
//	}

	public FabAmount getExceedPrin() {
		return exceedPrin;
	}

	public FabAmount getExceedInt() {
		return exceedInt;
	}

	public void setExceedPrin(FabAmount exceedPrin) {
		this.exceedPrin = exceedPrin;
	}

	public void setExceedInt(FabAmount exceedInt) {
		this.exceedInt = exceedInt;
	}

	public FabAmount getTotalPrin() {
		return totalPrin;
	}

	public void setTotalPrin(FabAmount totalPrin) {
		this.totalPrin = totalPrin;
	}

	public FabAmount getTotalNint() {
		return totalNint;
	}

	public void setTotalNint(FabAmount totalNint) {
		this.totalNint = totalNint;
	}

	public FabAmount getDynamicCapAmt() {
		return dynamicCapAmt;
	}

	public void setDynamicCapAmt(FabAmount dynamicCapAmt) {
		this.dynamicCapAmt = dynamicCapAmt;
	}

	public FabAmount getDynamicCapDiff() {
		return dynamicCapDiff;
	}

	public void setDynamicCapDiff(FabAmount dynamicCapDiff) {
		this.dynamicCapDiff = dynamicCapDiff;
	}

	public String getAdvanceFeeType() {
		return advanceFeeType;
	}

	public void setAdvanceFeeType(String advanceFeeType) {
		this.advanceFeeType = advanceFeeType;
	}

	

}

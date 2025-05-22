package com.suning.fab.loan.bo;

import com.suning.fab.tup4j.amount.FabAmount;
/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 *
 */
public class InvestInfo {

	private String investeeId;		//资金方
	private String newInvestee;		//转换后资金方
	private String investeeNo;		//资金方流水号
	private String newInvesteeNo;	//转换后资金方流水
	private FabAmount investeePrin;		//资金方本金
	private FabAmount investeeInt;		//资金方利息
	private FabAmount investeeDint;		//资金方罚息
	private FabAmount investeeFee;		//资金方手续费
	private String investeeFlag;	//资金方标识	01:转贷退货标识	02:平台现金贷线下还款"
	private String reserv;//辅助字段 存放资金方占比
	
	
	public InvestInfo(){
		this.investeeId = "";
		this.investeeNo = "";
		this.newInvesteeNo = "";
		this.investeePrin = new FabAmount(0.00);
		this.investeeInt = new FabAmount(0.00);
		this.investeeDint = new FabAmount(0.00);
		this.investeeFee = new FabAmount(0.00);
		this.investeeFlag = "";
		this.reserv="";
	}
	/**
	 * @return the investeeId
	 */
	public String getInvesteeId() {
		return investeeId;
	}
	/**
	 * @param investeeId the investeeId to set
	 */
	public void setInvesteeId(String investeeId) {
		this.investeeId = investeeId;
	}
	/**
	 * @return the newInvestee
	 */
	public String getNewInvestee() {
		return newInvestee;
	}
	/**
	 * @param newInvestee the newInvestee to set
	 */
	public void setNewInvestee(String newInvestee) {
		this.newInvestee = newInvestee;
	}
	/**
	 * @return the investeeNo
	 */
	public String getInvesteeNo() {
		return investeeNo;
	}
	/**
	 * @param investeeNo the investeeNo to set
	 */
	public void setInvesteeNo(String investeeNo) {
		this.investeeNo = investeeNo;
	}
	/**
	 * @return the newInvesteeNo
	 */
	public String getNewInvesteeNo() {
		return newInvesteeNo;
	}
	/**
	 * @param newInvesteeNo the newInvesteeNo to set
	 */
	public void setNewInvesteeNo(String newInvesteeNo) {
		this.newInvesteeNo = newInvesteeNo;
	}
	/**
	 * @return the investeePrin
	 */
	public FabAmount getInvesteePrin() {
		return investeePrin;
	}
	/**
	 * @param investeePrin the investeePrin to set
	 */
	public void setInvesteePrin(FabAmount investeePrin) {
		this.investeePrin = investeePrin;
	}
	/**
	 * @return the investeeInt
	 */
	public FabAmount getInvesteeInt() {
		return investeeInt;
	}
	/**
	 * @param investeeInt the investeeInt to set
	 */
	public void setInvesteeInt(FabAmount investeeInt) {
		this.investeeInt = investeeInt;
	}
	/**
	 * @return the investeeDint
	 */
	public FabAmount getInvesteeDint() {
		return investeeDint;
	}
	/**
	 * @param investeeDint the investeeDint to set
	 */
	public void setInvesteeDint(FabAmount investeeDint) {
		this.investeeDint = investeeDint;
	}
	/**
	 * @return the investeeFee
	 */
	public FabAmount getInvesteeFee() {
		return investeeFee;
	}
	/**
	 * @param investeeFee the investeeFee to set
	 */
	public void setInvesteeFee(FabAmount investeeFee) {
		this.investeeFee = investeeFee;
	}
	/**
	 * @return the investeeFlag
	 */
	public String getInvesteeFlag() {
		return investeeFlag;
	}
	/**
	 * @param investeeFlag the investeeFlag to set
	 */
	public void setInvesteeFlag(String investeeFlag) {
		this.investeeFlag = investeeFlag;
	}

	public String getReserv() {
		return reserv;
	}

	public void setReserv(String reserv) {
		this.reserv = reserv;
	}
}

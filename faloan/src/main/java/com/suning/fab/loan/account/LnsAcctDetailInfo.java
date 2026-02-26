/**
* @author 14050269 Howard
* @version 创建时间：2016年6月11日 下午3:03:29
* 类说明
*/
package com.suning.fab.loan.account;

import com.suning.fab.tup4j.amount.FabAmount;

public class LnsAcctDetailInfo  {

	String prdCode;
	String accStat;
	FabAmount bal;
	FabAmount lastBal;
	String lastTranDate;
	String custType;
	String acctNo;
	String merchantNo;
	
	public LnsAcctDetailInfo(){
		//nothing to do
	}

	/**
	 * @return the prdCode
	 */
	public String getPrdCode() {
		return prdCode;
	}

	/**
	 * @param prdCode the prdCode to set
	 */
	public void setPrdCode(String prdCode) {
		this.prdCode = prdCode;
	}

	/**
	 * @return the accStat
	 */
	public String getAccStat() {
		return accStat;
	}

	/**
	 * @param accStat the accStat to set
	 */
	public void setAccStat(String accStat) {
		this.accStat = accStat;
	}

	/**
	 * @return the bal
	 */
	public FabAmount getBal() {
		return bal;
	}

	/**
	 * @param bal the bal to set
	 */
	public void setBal(FabAmount bal) {
		this.bal = bal;
	}

	/**
	 * @return the lastBal
	 */
	public FabAmount getLastBal() {
		return lastBal;
	}

	/**
	 * @param lastBal the lastBal to set
	 */
	public void setLastBal(FabAmount lastBal) {
		this.lastBal = lastBal;
	}

	/**
	 * @return the lastTranDate
	 */
	public String getLastTranDate() {
		return lastTranDate;
	}

	/**
	 * @param lastTranDate the lastTranDate to set
	 */
	public void setLastTranDate(String lastTranDate) {
		this.lastTranDate = lastTranDate;
	}

	/**
	 * @return the custType
	 */
	public String getCustType() {
		return custType;
	}

	/**
	 * @param custType the custType to set
	 */
	public void setCustType(String custType) {
		this.custType = custType;
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
	 * @return the merchantNo
	 */
	public String getMerchantNo() {
		return merchantNo;
	}

	/**
	 * @param merchantNo the merchantNo to set
	 */
	public void setMerchantNo(String merchantNo) {
		this.merchantNo = merchantNo;
	}


}

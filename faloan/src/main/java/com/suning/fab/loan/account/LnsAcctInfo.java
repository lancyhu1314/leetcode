/**
* @author 14050269 Howard
* @version 创建时间：2016年6月11日 下午3:03:29
* 类说明
*/
package com.suning.fab.loan.account;

import com.suning.fab.tup4j.currency.Currency;
import com.suning.fab.tup4j.account.AcctInfo;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

public class LnsAcctInfo extends AcctInfo {

	String merchantNo="";
	String custType="";
	String custName="";
	String prdCode="";
	String billType="";
	String loanForm="";
	String receiptNo ="";
	String childBrc = "";
	String cancelFlag = "";
	public LnsAcctInfo(String receiptNo, String billType,String loanForm, Currency ccy) {
		super(receiptNo,PropertyUtil.getPropertyOrDefault("billtype."+billType, billType)+"."+loanForm,0, ccy);
		this.billType = PropertyUtil.getPropertyOrDefault("billtype."+billType, billType);
		this.loanForm = loanForm;
	}
	public LnsAcctInfo(String receiptNo, String billType,String loanForm, Currency ccy,String childBrc) {
		super(receiptNo,PropertyUtil.getPropertyOrDefault("billtype."+billType, billType)+"."+loanForm,VarChecker.isEmpty(childBrc)?0:Integer.valueOf(childBrc), ccy);
		this.billType = PropertyUtil.getPropertyOrDefault("billtype."+billType, billType);
		this.loanForm = loanForm;
		this.childBrc = childBrc;
	}
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
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
	 * @return the custName
	 */
	public String getCustName() {
		return custName;
	}


	/**
	 * @param custName the custName to set
	 */
	public void setCustName(String custName) {
		this.custName = custName;
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
	 * @return the billType
	 */
	public String getBillType() {
		return billType;
	}


	/**
	 * @param billType the billType to set
	 */
	public void setBillType(String billType) {
		this.billType = billType;
	}


	/**
	 * @return the loanForm
	 */
	public String getLoanForm() {
		return loanForm;
	}


	/**
	 * @param loanForm the loanForm to set
	 */
	public void setLoanForm(String loanForm) {
		this.loanForm = loanForm;
	}


	public String getReceiptNo() {
		return receiptNo;
	}


	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	/**
	 * Gets the value of childBrc.
	 *
	 * @return the value of childBrc
	 */
	public String getChildBrc() {
		return childBrc;
	}

	/**
	 * Sets the childBrc.
	 *
	 * @param childBrc childBrc
	 */
	public void setChildBrc(String childBrc) {
		this.childBrc = childBrc;

	}

	public String getCancelFlag() {
		return cancelFlag;
	}

	public void setCancelFlag(String cancelFlag) {
		this.cancelFlag = cancelFlag;
	}
}

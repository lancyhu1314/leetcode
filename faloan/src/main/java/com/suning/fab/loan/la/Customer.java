package com.suning.fab.loan.la;

import java.io.Serializable;

public class Customer  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2498737165603812849L;
	private  String customId;		//客户号
	private  String merchantNo;		//商户号		!!!
	private  String customName;		//客户名称		!!!
	private  String customType;		//客户类别		!!!
	/**
	 * @return the customId
	 */
	public String getCustomId() {
		return customId;
	}
	/**
	 * @param customId the customId to set
	 */
	public void setCustomId(String customId) {
		this.customId = customId;
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
	 * @return the customName
	 */
	public String getCustomName() {
		return customName;
	}
	/**
	 * @param customName the customName to set
	 */
	public void setCustomName(String customName) {
		this.customName = customName;
	}
	/**
	 * @return the customType
	 */
	public String getCustomType() {
		return customType;
	}
	/**
	 * @param customType the customType to set
	 */
	public void setCustomType(String customType) {
		this.customType = customType;
	}
	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
}

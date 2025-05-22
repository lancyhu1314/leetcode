package com.suning.fab.loan.la;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.tup4j.base.FabException;


import java.io.Serializable;

public class InterestAgreement implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4585152161351898423L;

	/**
	 * 是否计正常利息息标志  YES-计息  NO-不计息
	 */
	private String isCalInt;
	
	/**
	 * 是否收取罚息标识
	 */
	private Boolean collectDefaultInterestFlag;
	
	/**
	 * 本金是否收取罚息标识
	 */
	private Boolean collectDefaultInterestPrin;
	
	/**
	 * 利息是否收取复利标识
	 */
	private Boolean collectDefaultInterestInt;
	
	
	/**
	 * 是否转列标识
	 */
	private Boolean needRisksClassificationFlag; 
	
	/**
	 * 本金是否转列标识
	 */
	private Boolean needRisksClassificationPrin; 
	
	/**
	 * 利息是否转列标识
	 */
	private Boolean needRisksClassificationInt; 
	
	
	/**
	 * 计息基础   取值：ACT/360、ACT/365、30/360、30/365、ACT/ACT、30/ACT
	 */
	private String intBase; 
	

	/**
	 * 周期公式：3Y3YA2Y1M0410   A左边为周期，A右边为指定指定还款
	 */
	private String periodFormula;
	
	/**
	 * 利息公式 :1-等额本息公式   2-其他
	 */
	private String intFormula;

	/**
	 * 周期最小天数
	 */
	private Integer periodMinDays;

	/**
	 * 罚息来源
	 */
	private String dintSource;
	
	/**
	 * 封顶计息
	 */
	private Double capRate;
	/**
	 * 新封顶计息
	 */
	private JSONObject dynamicCapRate;
	/**
	 * 是否允许提前还款
	 */
	private String isAdvanceRepay;
	
	/**
	 *  日期是否算尾
	 */
	private String isCalTail;
	
	/**
	 *  是否代偿开户
	 */
	private String isCompensatory;
	/**
	 *  宽限期是否计息
	 */
	private String isCalGrace;



	/**
	 *  是否记违约金
	 */
	private String isPenalty;
	/**
	 *  是否展示还款明细
	 */
	private String showRepayList;
	/**
	 *  是否有到期日
	 */
	private String needDueDate;

	/**
	 * 费用收取方式 author:chenchao
	 */
	private String advanceFeeType;

	public String getIgnoreOffDint() {
		return ignoreOffDint;
	}

	public void setIgnoreOffDint(String ignoreOffDint) {
		this.ignoreOffDint = ignoreOffDint;
	}

	/**
	 * 核销后罚息是否通知核销
	 */
	private String ignoreOffDint;

	/**
	 * 最低还款额
	 */
	private String minimumPayment;

	/**
	 * Gets the value of needDueDate.
	 *
	 * @return the value of needDueDate
	 */
	public String getNeedDueDate() {
		return needDueDate;
	}

	/**
	 * Gets the value of serialVersionUID.
	 *
	 * @return the value of serialVersionUID
	 */
	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	/**
	 * Sets the needDueDate.
	 *
	 * @param needDueDate needDueDate
	 */
	public  void setNeedDueDate(String needDueDate) {
		this.needDueDate = needDueDate;

	}

	/**
	 * @return the intBase
	 */
	public String getIntBase() {
		return intBase;
	}

	/**
	 * @param intBase the intBase to set
	 */
	public void setIntBase(String intBase) {
		this.intBase = intBase;
	}

	/**
	 * @return the periodFormula
	 */
	public String getPeriodFormula() {
		return periodFormula;
	}

	/**
	 * @param periodFormula the periodFormula to set
	 */
	public void setPeriodFormula(String periodFormula) {
		this.periodFormula = periodFormula;
	}

	/**
	 * @return the intFormula
	 */
	public String getIntFormula() {
		return intFormula;
	}

	/**
	 * @param intFormula the intFormula to set
	 */
	public void setIntFormula(String intFormula) {
		this.intFormula = intFormula;
	}

	/**
	 * @return the periodMinDays
	 */
	public Integer getPeriodMinDays() {
		return periodMinDays;
	}

	/**
	 * @param periodMinDays the periodMinDays to set
	 */
	public void setPeriodMinDays(Integer periodMinDays) {
		this.periodMinDays = periodMinDays;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Boolean getCollectDefaultInterestFlag() {
		return collectDefaultInterestFlag;
	}

	public void setCollectDefaultInterestFlag(Boolean collectDefaultInterestFlag) {
		this.collectDefaultInterestFlag = collectDefaultInterestFlag;
	}

	public Boolean getNeedRisksClassificationFlag() {
		return needRisksClassificationFlag;
	}

	public void setNeedRisksClassificationFlag(Boolean needRisksClassificationFlag) {
		this.needRisksClassificationFlag = needRisksClassificationFlag;
	}

	public String getIsCalInt() {
		return isCalInt;
	}

	public void setIsCalInt(String isCalInt) {
		this.isCalInt = isCalInt;
	}

	/**
	 * @return the dintSource
	 */
	public String getDintSource() {
		return dintSource;
	}

	/**
	 * @param dintSource the dintSource to set
	 */
	public void setDintSource(String dintSource) {
		this.dintSource = dintSource;
	}

	/**
	 * @return the isAdvanceRepay
	 */
	public String getIsAdvanceRepay() {
		return isAdvanceRepay;
	}

	/**
	 * @param isAdvanceRepay the isAdvanceRepay to set
	 */
	public void setIsAdvanceRepay(String isAdvanceRepay) {
		this.isAdvanceRepay = isAdvanceRepay;
	}

	/**
	 * @return the isCalTail
	 */
	public String getIsCalTail() {
		return isCalTail;
	}

	/**
	 * @param isCalTail the isCalTail to set
	 */
	public void setIsCalTail(String isCalTail) {
		this.isCalTail = isCalTail;
	}

	/**
	 * @return the isCalGrace
	 */
	public String getIsCalGrace() {
		return isCalGrace;
	}

	/**
	 * @param isCalGrace the isCalGrace to set
	 */
	public void setIsCalGrace(String isCalGrace) {
		this.isCalGrace = isCalGrace;
	}

	public Double getCapRate() {
		return capRate;
	}

	public void setCapRate(Double capRate) {
		this.capRate = capRate;
	}

	/**
	 * @return the collectDefaultInterestPrin
	 */
	public Boolean getCollectDefaultInterestPrin() {
		return collectDefaultInterestPrin;
	}

	/**
	 * @param collectDefaultInterestPrin the collectDefaultInterestPrin to set
	 */
	public void setCollectDefaultInterestPrin(Boolean collectDefaultInterestPrin) {
		this.collectDefaultInterestPrin = collectDefaultInterestPrin;
	}

	/**
	 * @return the collectDefaultInterestInt
	 */
	public Boolean getCollectDefaultInterestInt() {
		return collectDefaultInterestInt;
	}

	/**
	 * @param collectDefaultInterestInt the collectDefaultInterestInt to set
	 */
	public void setCollectDefaultInterestInt(Boolean collectDefaultInterestInt) {
		this.collectDefaultInterestInt = collectDefaultInterestInt;
	}

	/**
	 * @return the needRisksClassificationPrin
	 */
	public Boolean getNeedRisksClassificationPrin() {
		return needRisksClassificationPrin;
	}

	/**
	 * @param needRisksClassificationPrin the needRisksClassificationPrin to set
	 */
	public void setNeedRisksClassificationPrin(Boolean needRisksClassificationPrin) {
		this.needRisksClassificationPrin = needRisksClassificationPrin;
	}

	/**
	 * @return the needRisksClassificationInt
	 */
	public Boolean getNeedRisksClassificationInt() {
		return needRisksClassificationInt;
	}

	/**
	 * @param needRisksClassificationInt the needRisksClassificationInt to set
	 */
	public void setNeedRisksClassificationInt(Boolean needRisksClassificationInt) {
		this.needRisksClassificationInt = needRisksClassificationInt;
	}

	/**
	 * Gets the value of dynamicCapRate.
	 *
	 * @return the value of dynamicCapRate
	 */
	public Double queryDynamicCapRate(String key) throws  FabException {
			if(dynamicCapRate.containsKey(key))
				return dynamicCapRate.getDouble(key);
			else
				return dynamicCapRate.getDouble("default");
	}

	/**
	 * Gets the value of dynamicCapRate.
	 *
	 * @return the value of dynamicCapRate
	 */
	public JSONObject getDynamicCapRate() {
		return dynamicCapRate;
	}

	/**
	 * Sets the dynamicCapRate.
	 *
	 * @param dynamicCapRate dynamicCapRate
	 */
	public void setDynamicCapRate(JSONObject dynamicCapRate) {
		this.dynamicCapRate = dynamicCapRate;

	}

	/**
	 *
	 * @return 费用收取方式
	 */
	public String getAdvanceFeeType() {
		return advanceFeeType;
	}

	/**
	 *
	 * @param advanceFeeType 费用收取方式
	 */
	public void setAdvanceFeeType(String advanceFeeType) {
		this.advanceFeeType = advanceFeeType;
	}

	/**
	 * @return the showRepayList
	 */
	public String getShowRepayList() {
		return showRepayList;
	}

	/**
	 * @param showRepayList the showRepayList to set
	 */
	public void setShowRepayList(String showRepayList) {
		this.showRepayList = showRepayList;
	}

	/**
	 * @return the isPenalty
	 */
	public String getIsPenalty() {
		return isPenalty;
	}

	/**
	 * @param isPenalty the isPenalty to set
	 */
	public void setIsPenalty(String isPenalty) {
		this.isPenalty = isPenalty;
	}

	/**
	 * @return the isCompensatory
	 */
	public String getIsCompensatory() {
		return isCompensatory;
	}

	/**
	 * @param isCompensatory the isCompensatory to set
	 */
	public void setIsCompensatory(String isCompensatory) {
		this.isCompensatory = isCompensatory;
	}

	/**
	 * @return the minimumPayment
	 */
	public String getMinimumPayment() {
		return minimumPayment;
	}

	/**
	 * @param minimumPayment the minimumPayment to set
	 */
	public void setMinimumPayment(String minimumPayment) {
		this.minimumPayment = minimumPayment;
	}
}

/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: LnsAcctInfoQuery.java
 * Author:   16071579
 * Date:     2017年6月6日 下午7:28:23
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.bo;

import com.suning.fab.tup4j.amount.FabAmount;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:账单表查询类，用于返回报文使用
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsBasicInfo {
	
	private String	lastPrinDate;
	private Integer	prinTerm;
	private FabAmount	tranAmt;
	private String	lastIntDate;
	private Integer	intTerm;
	private String loanStat;
	/**
	 * @return the lastPrinDate
	 */
	public String getLastPrinDate() {
		return lastPrinDate;
	}
	/**
	 * @param lastPrinDate the lastPrinDate to set
	 */
	public void setLastPrinDate(String lastPrinDate) {
		this.lastPrinDate = lastPrinDate;
	}
	/**
	 * @return the prinTerm
	 */
	public Integer getPrinTerm() {
		return prinTerm;
	}
	/**
	 * @param prinTerm the prinTerm to set
	 */
	public void setPrinTerm(Integer prinTerm) {
		this.prinTerm = prinTerm;
	}
	/**
	 * @return the tranAmt
	 */
	public FabAmount getTranAmt() {
		return tranAmt;
	}
	/**
	 * @param tranAmt the tranAmt to set
	 */
	public void setTranAmt(FabAmount tranAmt) {
		this.tranAmt = tranAmt;
	}
	/**
	 * @return the lastIntDate
	 */
	public String getLastIntDate() {
		return lastIntDate;
	}
	/**
	 * @param lastIntDate the lastIntDate to set
	 */
	public void setLastIntDate(String lastIntDate) {
		this.lastIntDate = lastIntDate;
	}
	/**
	 * @return the intTerm
	 */
	public Integer getIntTerm() {
		return intTerm;
	}
	/**
	 * @param intTerm the intTerm to set
	 */
	public void setIntTerm(Integer intTerm) {
		this.intTerm = intTerm;
	}
	/**
	 * @return the loanStat
	 */
	public String getLoanStat() {
		return loanStat;
	}
	/**
	 * @param loanStat the loanStat to set
	 */
	public void setLoanStat(String loanStat) {
		this.loanStat = loanStat;
	}

}

/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: LnsAmortizeplan.java
 * Author:   16071579
 * Date:     2017年11月7日 上午10:27:52
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.account;

/**
 * 〈一句话功能简述〉<br> 
 * 用于保存查询摊销计划表和主文件表联合查询的数据
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsAmortizeplan {
	
	private Double taxrate = 0.00;		// 摊销税率
	private Double totalamt = 0.00;		// 摊销总金额
	private Double amortizeamt = 0.00;	// 已摊销金额
	private Double totaltaxamt = 0.00;	// 摊销总税金
	private Double amortizetax = 0.00;	// 已摊销税金
	private Double contractbal = 0.00;	// 合同余额
	private Double contractamt = 0.00;   //购机款
	private Double rentamt = 0.00;       //租金本息
	private Double monrate = 0.00;       //月利率
	private String lastdate = "";		// 上次摊销日期
	private String begindate = "";		// 合同开始日期
	private String enddate = "";		// 合同结束日期
	private Integer period = 0;			// 期数
	private String status = "";			// 摊销状态
	private String loanstat = "";		// 贷款状态
	private String amortizeformula = "";// 累计结费金额

	/**
	 * @return the taxrate
	 */
	public Double getTaxrate() {
		return taxrate;
	}
	/**
	 * @param taxrate the taxrate to set
	 */
	public void setTaxrate(Double taxrate) {
		this.taxrate = taxrate;
	}
	/**
	 * @return the totalamt
	 */
	public Double getTotalamt() {
		return totalamt;
	}
	/**
	 * @param totalamt the totalamt to set
	 */
	public void setTotalamt(Double totalamt) {
		this.totalamt = totalamt;
	}
	/**
	 * @return the amortizeamt
	 */
	public Double getAmortizeamt() {
		return amortizeamt;
	}
	/**
	 * @param amortizeamt the amortizeamt to set
	 */
	public void setAmortizeamt(Double amortizeamt) {
		this.amortizeamt = amortizeamt;
	}
	/**
	 * @return the totaltaxamt
	 */
	public Double getTotaltaxamt() {
		return totaltaxamt;
	}
	/**
	 * @param totaltaxamt the totaltaxamt to set
	 */
	public void setTotaltaxamt(Double totaltaxamt) {
		this.totaltaxamt = totaltaxamt;
	}
	/**
	 * @return the amortizetax
	 */
	public Double getAmortizetax() {
		return amortizetax;
	}
	/**
	 * @param amortizetax the amortizetax to set
	 */
	public void setAmortizetax(Double amortizetax) {
		this.amortizetax = amortizetax;
	}
	/**
	 * @return the contractbal
	 */
	public Double getContractbal() {
		return contractbal;
	}
	/**
	 * @param contractbal the contractbal to set
	 */
	public void setContractbal(Double contractbal) {
		this.contractbal = contractbal;
	}
	/**
	 * @return the lastdate
	 */
	public String getLastdate() {
		return lastdate;
	}
	/**
	 * @param lastdate the lastdate to set
	 */
	public void setLastdate(String lastdate) {
		this.lastdate = lastdate;
	}
	/**
	 * @return the begindate
	 */
	public String getBegindate() {
		return begindate;
	}
	/**
	 * @param begindate the begindate to set
	 */
	public void setBegindate(String begindate) {
		this.begindate = begindate;
	}
	/**
	 * @return the enddate
	 */
	public String getEnddate() {
		return enddate;
	}
	/**
	 * @param enddate the enddate to set
	 */
	public void setEnddate(String enddate) {
		this.enddate = enddate;
	}
	/**
	 * @return the period
	 */
	public Integer getPeriod() {
		return period;
	}
	/**
	 * @param period the period to set
	 */
	public void setPeriod(Integer period) {
		this.period = period;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	/**
	 * @return the loanstat
	 */
	public String getLoanstat() {
		return loanstat;
	}
	/**
	 * @param loanstat the loanstat to set
	 */
	public void setLoanstat(String loanstat) {
		this.loanstat = loanstat;
	}
	/**
	 * @return the contractamt
	 */
	public Double getContractamt() {
		return contractamt;
	}
	/**
	 * @param contractamt the contractamt to set
	 */
	public void setContractamt(Double contractamt) {
		this.contractamt = contractamt;
	}
	/**
	 * @return the rentamt
	 */
	public Double getRentamt() {
		return rentamt;
	}
	/**
	 * @param rentamt the rentamt to set
	 */
	public void setRentamt(Double rentamt) {
		this.rentamt = rentamt;
	}
	/**
	 * @return the monrate
	 */
	public Double getMonrate() {
		return monrate;
	}
	/**
	 * @param monrate the monrate to set
	 */
	public void setMonrate(Double monrate) {
		this.monrate = monrate;
	}
	/**
	 * @return the amortizeformula
	 */
	public String getAmortizeformula() {
		return amortizeformula;
	}
	/**
	 * @param amortizeformula the amortizeformula to set
	 */
	public void setAmortizeformula(String amortizeformula) {
		this.amortizeformula = amortizeformula;
	}
	

}

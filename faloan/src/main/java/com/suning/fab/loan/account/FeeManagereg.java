package com.suning.fab.loan.account;

import com.suning.fab.tup4j.amount.FabAmount;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:管理费登记薄类，用于返回报文使用
 *
 * @author 18049687
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class FeeManagereg {

	private String brc;				//公司代码
	private String acctno;			//账号
	private Integer repayterm;		//还款期数
	private String repayownbdate;	//还款起日
	private String repayownedate;	//还款止日
	private String repayintbdate;	//本期起日
	private String repayintedate;	//本期止日
	private FabAmount termretfee;	//本期应还费用
	private FabAmount feeamt;		//已还费用
	private FabAmount noretfee;		//未还费用
	private String currentstat;		//逾期状态
	private String settleflag;			//结清状态
	private Integer days;			//天数
	private String termretdate;		//本期还款日期
	private String modifydate;		//修改日期
	private String modifytime;		//修改时间
	private String reserve1;		//备用字段1
	private FabAmount reserve2;		//备用字段2
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
	 * @return the acctno
	 */
	public String getAcctno() {
		return acctno;
	}
	/**
	 * @param acctno the acctno to set
	 */
	public void setAcctno(String acctno) {
		this.acctno = acctno;
	}
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
	 * @return the repayownbdate
	 */
	public String getRepayownbdate() {
		return repayownbdate;
	}
	/**
	 * @param repayownbdate the repayownbdate to set
	 */
	public void setRepayownbdate(String repayownbdate) {
		this.repayownbdate = repayownbdate;
	}
	/**
	 * @return the repayownedate
	 */
	public String getRepayownedate() {
		return repayownedate;
	}
	/**
	 * @param repayownedate the repayownedate to set
	 */
	public void setRepayownedate(String repayownedate) {
		this.repayownedate = repayownedate;
	}
	/**
	 * @return the repayintbdate
	 */
	public String getRepayintbdate() {
		return repayintbdate;
	}
	/**
	 * @param repayintbdate the repayintbdate to set
	 */
	public void setRepayintbdate(String repayintbdate) {
		this.repayintbdate = repayintbdate;
	}
	/**
	 * @return the repayintedate
	 */
	public String getRepayintedate() {
		return repayintedate;
	}
	/**
	 * @param repayintedate the repayintedate to set
	 */
	public void setRepayintedate(String repayintedate) {
		this.repayintedate = repayintedate;
	}
	/**
	 * @return the termretfee
	 */
	public FabAmount getTermretfee() {
		return termretfee;
	}
	/**
	 * @param termretfee the termretfee to set
	 */
	public void setTermretfee(FabAmount termretfee) {
		this.termretfee = termretfee;
	}
	/**
	 * @return the feeamt
	 */
	public FabAmount getFeeamt() {
		return feeamt;
	}
	/**
	 * @param feeamt the feeamt to set
	 */
	public void setFeeamt(FabAmount feeamt) {
		this.feeamt = feeamt;
	}
	/**
	 * @return the noretfee
	 */
	public FabAmount getNoretfee() {
		return noretfee;
	}
	/**
	 * @param noretfee the noretfee to set
	 */
	public void setNoretfee(FabAmount noretfee) {
		this.noretfee = noretfee;
	}
	/**
	 * @return the currentstat
	 */
	public String getCurrentstat() {
		return currentstat;
	}
	/**
	 * @param currentstat the currentstat to set
	 */
	public void setCurrentstat(String currentstat) {
		this.currentstat = currentstat;
	}
	
	/**
	 * 
	 * @return the settleflag
	 */
	public String getSettleflag() {
		return settleflag;
	}
	/**
	 * @param settleflag the settleflag to set
	 */
	public void setSettleflag(String settleflag) {
		this.settleflag = settleflag;
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
	 * @return the termretdate
	 */
	public String getTermretdate() {
		return termretdate;
	}
	/**
	 * @param termretdate the termretdate to set
	 */
	public void setTermretdate(String termretdate) {
		this.termretdate = termretdate;
	}
	/**
	 * @return the modifydate
	 */
	public String getModifydate() {
		return modifydate;
	}
	/**
	 * @param modifydate the modifydate to set
	 */
	public void setModifydate(String modifydate) {
		this.modifydate = modifydate;
	}
	/**
	 * @return the modifytime
	 */
	public String getModifytime() {
		return modifytime;
	}
	/**
	 * @param modifytime the modifytime to set
	 */
	public void setModifytime(String modifytime) {
		this.modifytime = modifytime;
	}
	/**
	 * @return the reserve1
	 */
	public String getReserve1() {
		return reserve1;
	}
	/**
	 * @param reserve1 the reserve1 to set
	 */
	public void setReserve1(String reserve1) {
		this.reserve1 = reserve1;
	}
	/**
	 * @return the reserve2
	 */
	public FabAmount getReserve2() {
		return reserve2;
	}
	/**
	 * @param reserve2 the reserve2 to set
	 */
	public void setReserve2(FabAmount reserve2) {
		this.reserve2 = reserve2;
	}
	
}

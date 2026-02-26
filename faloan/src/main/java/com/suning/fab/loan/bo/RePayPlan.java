package com.suning.fab.loan.bo;

import com.suning.fab.tup4j.amount.FabAmount;

public class RePayPlan {
	private String brc;				//公司代码
	private String acctno;			//账号
	private Integer repayterm = 0;	//还款期数
	private String repayownbdate;	//还款起日
	private String repayownedate;	//还款止日
	private String repayintbdate;	//本期起日
	private String repayintedate;	//本期止日
	private FabAmount termretprin;	//本期应还本金
	private FabAmount termretint;	//本期应还利息
	private FabAmount deductionamt;	//扣息金额
	private FabAmount prinAmt;		//已还本金
	private FabAmount intAmt;		//已还利息
	private FabAmount noretamt;		//未还本金
	private FabAmount noretint;		//未还利息
	private FabAmount termfint;		//未还罚息
	private FabAmount termcint;		//未还复利
	private FabAmount sumfint;		//累计罚息
	private FabAmount sumcint;		//累计复利
	private Integer days = 0;		//记息天数
	private FabAmount termcdint;	//剩余结息利息
	private FabAmount balance;		//贷款剩余本金
	private FabAmount sumrfint;		//已还罚息
	private FabAmount sumrcint;		//已还复利
	private String termretdate;		//本期结清日
	private String termstatus;		//计划状态
	private String settleflag;		//结清标志
	private String settledate;		//结清日期
	String fundsource;
	
	public RePayPlan() {
		super();
		termretprin = new FabAmount();
		termretint = new FabAmount();
		prinAmt = new FabAmount();
		intAmt = new FabAmount();
		noretamt = new FabAmount();
		noretint = new FabAmount();
		termfint = new FabAmount();
		termcint = new FabAmount();
		sumfint = new FabAmount();
		sumcint = new FabAmount();
		deductionamt = new FabAmount();
		termcdint = new FabAmount();
		balance = new FabAmount();
		sumrfint = new FabAmount();
		sumrcint = new FabAmount();
		termretdate ="";
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
	 * @return the termretprin
	 */
	public FabAmount getTermretprin() {
		return termretprin;
	}

	/**
	 * @param termretprin the termretprin to set
	 */
	public void setTermretprin(FabAmount termretprin) {
		this.termretprin = termretprin;
	}

	/**
	 * @return the termretint
	 */
	public FabAmount getTermretint() {
		return termretint;
	}

	/**
	 * @param termretint the termretint to set
	 */
	public void setTermretint(FabAmount termretint) {
		this.termretint = termretint;
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
	 * @return the noretamt
	 */
	public FabAmount getNoretamt() {
		return noretamt;
	}

	/**
	 * @param noretamt the noretamt to set
	 */
	public void setNoretamt(FabAmount noretamt) {
		this.noretamt = noretamt;
	}

	/**
	 * @return the noretint
	 */
	public FabAmount getNoretint() {
		return noretint;
	}

	/**
	 * @param noretint the noretint to set
	 */
	public void setNoretint(FabAmount noretint) {
		this.noretint = noretint;
	}

	/**
	 * @return the termfint
	 */
	public FabAmount getTermfint() {
		return termfint;
	}

	/**
	 * @param termfint the termfint to set
	 */
	public void setTermfint(FabAmount termfint) {
		this.termfint = termfint;
	}

	/**
	 * @return the termcint
	 */
	public FabAmount getTermcint() {
		return termcint;
	}

	/**
	 * @param termcint the termcint to set
	 */
	public void setTermcint(FabAmount termcint) {
		this.termcint = termcint;
	}

	/**
	 * @return the sumfint
	 */
	public FabAmount getSumfint() {
		return sumfint;
	}

	/**
	 * @param sumfint the sumfint to set
	 */
	public void setSumfint(FabAmount sumfint) {
		this.sumfint = sumfint;
	}

	/**
	 * @return the sumcint
	 */
	public FabAmount getSumcint() {
		return sumcint;
	}

	/**
	 * @param sumcint the sumcint to set
	 */
	public void setSumcint(FabAmount sumcint) {
		this.sumcint = sumcint;
	}

	/**
	 * @return the deductionamt
	 */
	public FabAmount getDeductionamt() {
		return deductionamt;
	}

	/**
	 * @param deductionamt the deductionamt to set
	 */
	public void setDeductionamt(FabAmount deductionamt) {
		this.deductionamt = deductionamt;
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
	 * @return the termcdint
	 */
	public FabAmount getTermcdint() {
		return termcdint;
	}

	/**
	 * @param termcdint the termcdint to set
	 */
	public void setTermcdint(FabAmount termcdint) {
		this.termcdint = termcdint;
	}

	/**
	 * @return the balance
	 */
	public FabAmount getBalance() {
		return balance;
	}

	/**
	 * @param balance the balance to set
	 */
	public void setBalance(FabAmount balance) {
		this.balance = balance;
	}

	/**
	 * @return the sumrfint
	 */
	public FabAmount getSumrfint() {
		return sumrfint;
	}

	/**
	 * @param sumrfint the sumrfint to set
	 */
	public void setSumrfint(FabAmount sumrfint) {
		this.sumrfint = sumrfint;
	}

	/**
	 * @return the sumrcint
	 */
	public FabAmount getSumrcint() {
		return sumrcint;
	}

	/**
	 * @param sumrcint the sumrcint to set
	 */
	public void setSumrcint(FabAmount sumrcint) {
		this.sumrcint = sumrcint;
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
	 * @return the termstatus
	 */
	public String getTermstatus() {
		return termstatus;
	}

	/**
	 * @param termstatus the termstatus to set
	 */
	public void setTermstatus(String termstatus) {
		this.termstatus = termstatus;
	}

	/**
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
	 * @return the settledate
	 */
	public String getSettledate() {
		return settledate;
	}
	/**
	 * @param settledate the settledate to set
	 */
	public void setSettledate(String settledate) {
		this.settledate = settledate;
	}

	public String getFundsource() {
		return fundsource;
	}

	public void setFundsource(String fundsource) {
		this.fundsource = fundsource;
	}

}

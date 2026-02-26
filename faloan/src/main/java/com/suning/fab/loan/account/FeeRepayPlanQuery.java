package com.suning.fab.loan.account;

import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.tup4j.amount.FabAmount;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:房抵贷还款计划查询类，用于返回报文使用
 *
 * @author 18049687
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class FeeRepayPlanQuery extends RePayPlan {


	private FabAmount termretfee = new FabAmount();	//本期应还费用
	private FabAmount feeamt = new FabAmount();		//已还费用
	private FabAmount noretfee = new FabAmount();		//未还费用
//	private FabAmount termretdamages = new FabAmount();		//应还违约金
//    private FabAmount damagesamt = new FabAmount();		//已还违约金
//    private FabAmount noretdamages = new FabAmount();		//未还违约金
	private FabAmount termcdfee  = new FabAmount();		//本期剩余已计费
	private String currentstat = "01";		//费用逾期状态
	private String feeflag = "02";			//费用结清状态

	private FabAmount termretpenalty =new FabAmount() ; //本期应还违约金
	private FabAmount penaltyamt    = new  FabAmount() ;  //已还违约金
	private FabAmount noretpenalty  = new  FabAmount() ; //未还违约金

	public FabAmount getTermretpenalty() {
		return termretpenalty;
	}

	public void setTermretpenalty(FabAmount termretpenalty) {
		this.termretpenalty = termretpenalty;
	}

	public FabAmount getPenaltyamt() {
		return penaltyamt;
	}

	public void setPenaltyamt(FabAmount penaltyamt) {
		this.penaltyamt = penaltyamt;
	}

	public FabAmount getNoretpenalty() {
		return noretpenalty;
	}

	public void setNoretpenalty(FabAmount noretpenalty) {
		this.noretpenalty = noretpenalty;
	}



	public FeeRepayPlanQuery() {
		super();
	}

	/**
	 * Gets the value of termretfee.
	 *
	 * @return the value of termretfee
	 */
	public FabAmount getTermretfee() {
		return termretfee;
	}

	/**
	 * Sets the termretfee.
	 *
	 * @param termretfee termretfee
	 */
	public void setTermretfee(FabAmount termretfee) {
		this.termretfee = termretfee;

	}

	/**
	 * Gets the value of feeamt.
	 *
	 * @return the value of feeamt
	 */
	public FabAmount getFeeamt() {
		return feeamt;
	}

	/**
	 * Sets the feeamt.
	 *
	 * @param feeamt feeamt
	 */
	public void setFeeamt(FabAmount feeamt) {
		this.feeamt = feeamt;

	}

	/**
	 * Gets the value of noretfee.
	 *
	 * @return the value of noretfee
	 */
	public FabAmount getNoretfee() {
		return noretfee;
	}

	/**
	 * Sets the noretfee.
	 *
	 * @param noretfee noretfee
	 */
	public void setNoretfee(FabAmount noretfee) {
		this.noretfee = noretfee;

	}

	/**
	 * Gets the value of currentstat.
	 *
	 * @return the value of currentstat
	 */
	public String getCurrentstat() {
		return currentstat;
	}

	/**
	 * Sets the currentstat.
	 *
	 * @param currentstat currentstat
	 */
	public void setCurrentstat(String currentstat) {
		this.currentstat = currentstat;

	}

	/**
	 * Gets the value of feeflag.
	 *
	 * @return the value of feeflag
	 */
	public String getFeeflag() {
		return feeflag;
	}

	/**
	 * Sets the feeflag.
	 *
	 * @param feeflag feeflag
	 */
	public void setFeeflag(String feeflag) {
		this.feeflag = feeflag;

	}

	/**
	 * Gets the value of termcdfee.
	 *
	 * @return the value of termcdfee
	 */
	public FabAmount getTermcdfee() {
		return termcdfee;
	}

	/**
	 * Sets the termcdfee.
	 *
	 * @param termcdfee termcdfee
	 */
	public void setTermcdfee(FabAmount termcdfee) {
		this.termcdfee = termcdfee;

	}
}

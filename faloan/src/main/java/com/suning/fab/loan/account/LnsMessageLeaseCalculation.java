package com.suning.fab.loan.account;

import com.suning.fab.tup4j.amount.FabAmount;

public class LnsMessageLeaseCalculation implements Comparable<LnsMessageLeaseCalculation>{
	private Integer repayTerm; // 还款期数
	private String beginDate; // 本期起日
	private String endDate; // 本期止日
	private FabAmount termRent; // 本期应还租金
	private FabAmount termTotal; // 本期应还尾款
	private FabAmount termPrin; // 本期应还总额      
	
	/**
	 * 构造函数，用于对自定义变量创建空间
	 */
	public LnsMessageLeaseCalculation() {
		termRent = new FabAmount(0.00);
		termTotal = new FabAmount(0.00);
		termPrin = new FabAmount(0.00);
		//balance = new FabAmount(0.00) //目前不需要返回剩余本金
	}
	
	public Integer getRepayTerm() {
		return repayTerm;
	}
	public void setRepayTerm(Integer repayTerm) {
		this.repayTerm = repayTerm;
	}
	public String getBeginDate() {
		return beginDate;
	}
	public void setBeginDate(String beginDate) {
		this.beginDate = beginDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public FabAmount getTermRent() {
		return termRent;
	}
	public void setTermRent(FabAmount termRent) {
		this.termRent = termRent;
	}
	public FabAmount getTermTotal() {
		return termTotal;
	}
	public void setTermTotal(FabAmount termTotal) {
		this.termTotal = termTotal;
	}
	public FabAmount getTermPrin() {
		return termPrin;
	}
	public void setTermPrin(FabAmount termPrin) {
		this.termPrin = termPrin;
	}

	@Override
	public int compareTo(LnsMessageLeaseCalculation o) {
		return this.getRepayTerm().compareTo(o.getRepayTerm());
	}
	
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof LnsMessageLeaseCalculation) {
        	LnsMessageLeaseCalculation lnsMessageLeaseCalculation = (LnsMessageLeaseCalculation) obj;
            return this.repayTerm.equals(lnsMessageLeaseCalculation.repayTerm);
        }
        return super.equals(obj);
    }
	
	@Override
    public int hashCode() {
        return repayTerm.hashCode();
    }
}

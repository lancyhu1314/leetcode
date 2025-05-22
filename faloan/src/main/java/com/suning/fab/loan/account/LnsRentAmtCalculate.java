/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: LnsRepayPlanCalculate.java
 * Author:   16071579
 * Date:     2017年6月12日 下午5:13:01
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.account;

import com.suning.fab.tup4j.amount.FabAmount;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:按揭贷款还款试算返回报文类
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsRentAmtCalculate implements Comparable<LnsRentAmtCalculate> {
	
	private Integer repayterm; // 还款期数
	private String ccy; // 币种
	private String repayintbdate; // 本期起日
	private String repayintedate; // 本期止日
	private FabAmount termRent; // 本期应还本金
	private FabAmount termTotal; // 本期应还利息
	private FabAmount termPrin; // 本息合计      

	
	/**
	 * 构造函数，用于对自定义变量创建空间
	 */
	public LnsRentAmtCalculate() {
		setTermRent(new FabAmount(0.00));
		setTermTotal(new FabAmount(0.00));
		setTermPrin(new FabAmount(0.00));
		//balance = new FabAmount(0.00) //目前不需要返回剩余本金
	}
	
	
	@Override
	public int compareTo(LnsRentAmtCalculate arg0) {
		return this.getRepayterm().compareTo(arg0.getRepayterm());
    }
	
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof LnsRentAmtCalculate) {
        	LnsRentAmtCalculate lnsRepayPlanCalculate = (LnsRentAmtCalculate) obj;
            return this.repayterm.equals(lnsRepayPlanCalculate.repayterm);
        }
        return super.equals(obj);
    }
        
	@Override
    public int hashCode() {
        return repayterm.hashCode();
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
	 * @return the ccy
	 */
	public String getCcy() {
		return ccy;
	}


	/**
	 * @param ccy the ccy to set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
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


	

}

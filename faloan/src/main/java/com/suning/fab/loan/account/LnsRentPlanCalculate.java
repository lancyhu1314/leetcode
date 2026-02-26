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
import com.suning.fab.tup4j.amount.FabRate;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉:按揭贷款还款试算返回报文类
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsRentPlanCalculate implements Comparable<LnsRentPlanCalculate> {
	
	private Integer repayterm; // 还款期数
	private String termeDate; // 本期止日
	private String intbdate; // 计息起日
	private String intedate; // 计息止日
	private FabAmount termPrin; // 本期应还本金      
	private FabAmount termInt; // 本期应还利息      
	private Integer days; // 宽限期天数
	private String calcIntFlag1; // 计息本金标志
	private String calcIntFlag2; // 起息日期标志
	private FabRate normalRate; // 正常利率
	private FabRate overdueRate; // 逾期利率
	private FabRate compoundRate; // 复利利率
	private String normalRateType; // 利率类型

	
	/**
	 * 构造函数，用于对自定义变量创建空间
	 */
	public LnsRentPlanCalculate() {
		setTermPrin(new FabAmount(0.00));
		setTermInt(new FabAmount(0.00));
		//balance = new FabAmount(0.00) //目前不需要返回剩余本金
	}
	
	
	@Override
	public int compareTo(LnsRentPlanCalculate arg0) {
		return this.getRepayterm().compareTo(arg0.getRepayterm());
    }
	
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof LnsRentPlanCalculate) {
        	LnsRentPlanCalculate lnsRepayPlanCalculate = (LnsRentPlanCalculate) obj;
            return this.repayterm.equals(lnsRepayPlanCalculate.repayterm);
        }
        return super.equals(obj);
    }
        
	@Override
    public int hashCode() {
        return repayterm.hashCode();
    }


	public Integer getRepayterm() {
		return repayterm;
	}


	public void setRepayterm(Integer repayterm) {
		this.repayterm = repayterm;
	}


	public String getTermeDate() {
		return termeDate;
	}


	public void setTermeDate(String termeDate) {
		this.termeDate = termeDate;
	}


	public String getIntbdate() {
		return intbdate;
	}


	public void setIntbdate(String intbdate) {
		this.intbdate = intbdate;
	}


	public String getIntedate() {
		return intedate;
	}


	public void setIntedate(String intedate) {
		this.intedate = intedate;
	}


	public FabAmount getTermPrin() {
		return termPrin;
	}


	public void setTermPrin(FabAmount termPrin) {
		this.termPrin = termPrin;
	}


	public FabAmount getTermInt() {
		return termInt;
	}


	public void setTermInt(FabAmount termInt) {
		this.termInt = termInt;
	}


	public Integer getDays() {
		return days;
	}


	public void setDays(Integer days) {
		this.days = days;
	}


	public String getCalcIntFlag1() {
		return calcIntFlag1;
	}


	public void setCalcIntFlag1(String calcIntFlag1) {
		this.calcIntFlag1 = calcIntFlag1;
	}


	public String getCalcIntFlag2() {
		return calcIntFlag2;
	}


	public void setCalcIntFlag2(String calcIntFlag2) {
		this.calcIntFlag2 = calcIntFlag2;
	}



	public String getNormalRateType() {
		return normalRateType;
	}


	public void setNormalRateType(String normalRateType) {
		this.normalRateType = normalRateType;
	}


	/**
	 * @return the normalRate
	 */
	public FabRate getNormalRate() {
		return normalRate;
	}


	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}


	/**
	 * @return the overdueRate
	 */
	public FabRate getOverdueRate() {
		return overdueRate;
	}


	/**
	 * @param overdueRate the overdueRate to set
	 */
	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
	}


	/**
	 * @return the compoundRate
	 */
	public FabRate getCompoundRate() {
		return compoundRate;
	}


	/**
	 * @param compoundRate the compoundRate to set
	 */
	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
	}






	

}

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
 * 〈功能详细描述〉:非标准还款试算请求报文
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsNonstdPlanCalculateIn implements Comparable<LnsNonstdPlanCalculateIn> {
	
	private Integer repayTerm; // 还款期数
	private String termeDate; // 本期止日
	private String intbDate; // 还款起日
	private String inteDate; // 还款止日
	private FabAmount termPrin; // 本期应还本金
	private String	calcIntFlag1; //计息本金标志
	private String	calcIntFlag2; //计息天数标志
	private FabRate	normalRate; //正常年利率
	/**
	 * 构造函数，用于对自定义变量创建空间
	 */
	public LnsNonstdPlanCalculateIn() {
		termPrin = new FabAmount(0.00);
		normalRate = new FabRate(0.00);
	}
	
	
	@Override
	public int compareTo(LnsNonstdPlanCalculateIn arg0) {
		return this.getRepayTerm().compareTo(arg0.getRepayTerm());
    }
	
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof LnsNonstdPlanCalculateIn) {
        	LnsNonstdPlanCalculateIn lnsRepayPlanCalculateIn = (LnsNonstdPlanCalculateIn) obj;
            return this.repayTerm.equals(lnsRepayPlanCalculateIn.repayTerm);
        }
        return super.equals(obj);
    }
        
	@Override
    public int hashCode() {
        return repayTerm.hashCode();
    }


	public Integer getRepayTerm() {
		return repayTerm;
	}


	public void setRepayTerm(Integer repayTerm) {
		this.repayTerm = repayTerm;
	}


	public String getTermeDate() {
		return termeDate;
	}


	public void setTermeDate(String termeDate) {
		this.termeDate = termeDate;
	}


	public String getIntbDate() {
		return intbDate;
	}


	public void setIntbDate(String intbDate) {
		this.intbDate = intbDate;
	}


	public String getInteDate() {
		return inteDate;
	}


	public void setInteDate(String inteDate) {
		this.inteDate = inteDate;
	}


	public FabAmount getTermPrin() {
		return termPrin;
	}


	public void setTermPrin(FabAmount termPrin) {
		this.termPrin = termPrin;
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


	public FabRate getNormalRate() {
		return normalRate;
	}


	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}


}

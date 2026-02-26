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
 * 〈功能详细描述〉:非标准还款试算返回报文类
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class LnsNonstdPlanCalculate implements Comparable<LnsNonstdPlanCalculate> {
	
	private Integer repayterm; // 还款期数
	private String repayintbdate; // 本期起日
	private String repayintedate; // 本期止日
	private String repayownbdate; // 还款起日
	private String repayownedate; // 还款止日
	private FabAmount noretamt; // 本期应还本金
	private FabAmount noretint; // 本期应还利息
	private FabAmount sumamt; // 本息合计      
	
	/**
	 * 构造函数，用于对自定义变量创建空间
	 */
	public LnsNonstdPlanCalculate() {
		noretamt = new FabAmount(0.00);
		noretint = new FabAmount(0.00);
		sumamt = new FabAmount(0.00);
		//balance = new FabAmount(0.00) //目前不需要返回剩余本金
	}
	
	
	@Override
	public int compareTo(LnsNonstdPlanCalculate arg0) {
		return this.getRepayterm().compareTo(arg0.getRepayterm());
    }
	
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof LnsNonstdPlanCalculate) {
        	LnsNonstdPlanCalculate lnsRepayPlanCalculate = (LnsNonstdPlanCalculate) obj;
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
	 * @return the sumamt
	 */
	public FabAmount getSumamt() {
		return sumamt;
	}


	/**
	 * @param sumamt the sumamt to set
	 */
	public void setSumamt(FabAmount sumamt) {
		this.sumamt = sumamt;
	}

}

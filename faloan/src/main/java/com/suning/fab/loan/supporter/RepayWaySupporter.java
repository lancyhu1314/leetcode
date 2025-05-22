package com.suning.fab.loan.supporter;

import com.suning.fab.tup4j.base.FabException;
/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 根据repayWay映射本金、利息相关公式
 *
 * @param getIntFormula()
 * @param getIntPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate)
 * @param getPrinPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate)
 * @param getRepayAmtFormula()
 * 
 * @return
 *
 * @exception
 */
public interface RepayWaySupporter {  
	
	/**
	 * 计算利息公式
	 * @return 0-等本等息 
	 * @return 1-等额本息
	 * @return 2-其他
	 * */
	String getIntFormula() throws FabException;
	/**
	 * 计算还息周期公式
	 * @param periodNum  周期数量
	 * @param periodUnit 周期单位(Y-年    M-月    D-日    H-半年    X-旬)
	 * @param repayDate  指定还款日(1...31)
	 * @param beginDate  计息起日或者开户日期
	 * @param endDate    合同到期日
	 * 
	 * @return String 1MA10:每月10号还息
	 */
	String getIntPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException;
	/**
	 * 计算还本周期公式
	 * @param beginDate  开户日期
	 * @param endDate    合同到期日
	 * 
	 * @return String 1MA10:每月10号还本
	 * */
	String getPrinPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException;
	/**
	 * 计算还款金额公式
	 * */
	String getRepayAmtFormula() throws FabException;
}  
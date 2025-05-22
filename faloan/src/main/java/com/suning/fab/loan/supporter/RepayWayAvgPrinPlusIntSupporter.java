package com.suning.fab.loan.supporter;

import java.lang.reflect.Method;

import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.RepayPeriod;
import com.suning.fab.loan.utils.MethodUtil;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.base.FabException;
/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 等额本息(对应repayWay:1)映射本金、利息相关公式
 *
 */
//@Scope("prototype")
@Repository
public class RepayWayAvgPrinPlusIntSupporter implements RepayWaySupporter {
	
	public RepayWayAvgPrinPlusIntSupporter() {
		super();
		// nothing to do
	}
	
	@Override
	//计算还息周期公式
	public String getIntPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException{
		RepayPeriod repayPeriod = new RepayPeriod();
		Method msetp = MethodUtil.getMethod(repayPeriod.getClass(), "setPeriod"+periodUnit, Integer.class);
		Method msetod = MethodUtil.getMethod(repayPeriod.getClass(), "setOptDay", Integer.class);
		MethodUtil.methodInvoke(repayPeriod, msetp, periodNum);
		MethodUtil.methodInvoke(repayPeriod, msetod, Integer.valueOf(repayDate));
		return RepayPeriodSupporter.combinationPeriodFormula(repayPeriod);
	}
	
	//计算利息公式
	@Override
	public String getIntFormula() throws FabException{
		return "1";
	}
	
	@Override
	//计算还本周期公式
	public String getPrinPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException{
		RepayPeriod repayPeriod = new RepayPeriod();
		Method msetp = MethodUtil.getMethod(repayPeriod.getClass(), "setPeriod"+periodUnit, Integer.class);
		Method msetod = MethodUtil.getMethod(repayPeriod.getClass(), "setOptDay", Integer.class);
		MethodUtil.methodInvoke(repayPeriod, msetp, periodNum);
		MethodUtil.methodInvoke(repayPeriod, msetod, Integer.valueOf(repayDate));
		return RepayPeriodSupporter.combinationPeriodFormula(repayPeriod);
	}
	
	//计算还款金额公式
	@Override
	public String getRepayAmtFormula() throws FabException{
		return "1";
	}
}

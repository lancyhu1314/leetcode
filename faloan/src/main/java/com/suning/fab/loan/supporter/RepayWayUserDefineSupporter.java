package com.suning.fab.loan.supporter;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.RepayPeriod;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.base.FabException;
/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 每期都要还本付息(对应repayWay:13)映射本金、利息相关公式
 *
 */
//@Scope("prototype")
@Repository
public class RepayWayUserDefineSupporter implements RepayWaySupporter {
	
	public RepayWayUserDefineSupporter() {
		super();
		//nothing to do
	}
	
	@Override
	//计算还息周期公式
	public String getIntPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException{
		DateTime b = new DateTime(beginDate);  
		DateTime e = new DateTime(endDate);  
		Period p = new Period(b, e, PeriodType.yearMonthDay());
		RepayPeriod repayPeriod = new RepayPeriod();
		repayPeriod.setPeriodY(p.getYears());
		repayPeriod.setPeriodM(p.getMonths());
		repayPeriod.setPeriodD(p.getDays());
		return RepayPeriodSupporter.combinationPeriodFormula(repayPeriod);
	}
	
	//计算利息公式
	@Override
	public String getIntFormula() throws FabException{
		return "3";
	}
	
	@Override
	//计算还本周期公式
	public String getPrinPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException{
		DateTime b = new DateTime(beginDate);  
		DateTime e = new DateTime(endDate);  
		Period p = new Period(b, e, PeriodType.yearMonthDay());
		RepayPeriod repayPeriod = new RepayPeriod();
		repayPeriod.setPeriodY(p.getYears());
		repayPeriod.setPeriodM(p.getMonths());
		repayPeriod.setPeriodD(p.getDays());
		return RepayPeriodSupporter.combinationPeriodFormula(repayPeriod);
	}
	
	//计算还款金额公式
	@Override
	public String getRepayAmtFormula() throws FabException{
		return "3";
	}
}

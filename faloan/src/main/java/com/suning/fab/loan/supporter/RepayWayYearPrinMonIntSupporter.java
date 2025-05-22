package com.suning.fab.loan.supporter;

import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.RepayPeriod;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.base.FabException;
/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 年还本,月还息(对应repayWay:5)映射本金、利息相关公式
 *
 */
//@Scope("prototype")
@Repository
public class RepayWayYearPrinMonIntSupporter implements RepayWaySupporter {
	
	public RepayWayYearPrinMonIntSupporter() {
		super();
		//nothing to do
	}
	
	@Override
	//计算还息周期公式
	public String getIntPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException{
		RepayPeriod repayPeriod = new  RepayPeriod();
		repayPeriod.setPeriodM(Integer.valueOf(1));
		repayPeriod.setOptDay(Integer.valueOf(repayDate));
		return RepayPeriodSupporter.combinationPeriodFormula(repayPeriod);
	}
	
	//计算利息公式
	@Override
	public String getIntFormula() throws FabException{
		return "2";
	}
	
	@Override
	//计算还本周期公式
	public String getPrinPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException{
		RepayPeriod repayPeriod = new RepayPeriod();
		repayPeriod.setPeriodY(Integer.valueOf(1));
		//repayPeriod.setTimesY(Integer.valueOf(1));
		//repayPeriod.setOptMonth(Integer.valueOf(12));
		repayPeriod.setOptDay(Integer.valueOf(repayDate));
		return RepayPeriodSupporter.combinationPeriodFormula(repayPeriod);
	}
	
	//计算还款金额公式
	@Override
	public String getRepayAmtFormula() throws FabException{
		return "2";
	}
}

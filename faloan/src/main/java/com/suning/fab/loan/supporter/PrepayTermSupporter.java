package com.suning.fab.loan.supporter;

import java.util.List;

import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;

public abstract class  PrepayTermSupporter {
	
	
	private String val;
	protected LoanAgreement loanAgreement;
	
	public abstract String genMoment(String openDate,String setIntDate ,String setPrinDate,String repayDate) throws FabException;
	public abstract String genThing(FabAmount intAmt,FabAmount repayAmt);
	
	public abstract void genVal(String a,String  b);
	
		
	public Integer  genUseTerm(Integer term){
		
		if ("P".equals(val.split(":")[0]))
		{
			return term-1<=0?1:term-1;
		}
		return term;
	}
	public boolean isAccumterm()
	{
		return "Y".equals(val.split(":")[1]);
	}
	public boolean  isInsertPrinBillPlan()
	{
		return "Y".equals(val.split(":")[2]);
	}
	public boolean  isInsertIntBillPlan()
	{
		return "Y".equals(val.split(":")[3]);
	}

	public boolean isSettleInterestDate(String repayDate) throws FabException
	{
		List<String> dateList = RepayPeriodSupporter.getIntPeriodList(loanAgreement);
		
		for (String dt:dateList)
		{
			//2018-12-05 断层问题 
			if (dt.equals(repayDate) && 
				CalendarUtil.fixedDaysBetween(loanAgreement.getContract().getRepayPrinDate(), dt)> 15)
			{
				return true;
			}
			
			//当前循环日期晚于还款日期提前跳出循环，提高执行效率
			if (CalendarUtil.after(dt,repayDate ))
			{
				return false;
			}
		}
		return false;
	}
	
	public boolean isSettlePrinDate(String repayDate) throws FabException
	{
		List<String> dateList = RepayPeriodSupporter.genPrinPeriodList(loanAgreement);
		
		for (String dt:dateList)
		{
			if (dt.equals(repayDate))
			{
				return true;
			}
			
			if (CalendarUtil.after(repayDate, dt))
			{
				return false;
			}
		}
		return false;
	}
	

	public String getVal() {
		return val;
	}


	public void setVal(String val) {
		this.val = val;
	}
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}
	
	
}

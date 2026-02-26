package com.suning.fab.loan.supporter;

import java.math.BigDecimal;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.utils.VarChecker;

public  class SettleInterestIntAmltSupporter extends SettleInterestSupporter{  
	@Override
	public  LnsBill settleInterest(LoanAgreement loanAgreement,LnsBill hisBill,String repayDate)
	{
		if (CalendarUtil.after(repayDate, hisBill.getRepayendDate()))
		{
			return null;
		}
		LnsBill lnsBill = createLnsBill(hisBill, repayDate);
		FabRate rate = loanAgreement.getRateAgreement().getCompoundRate();
		lnsBill.setBillRate(rate);//账单执行利率
		//账单类型为累计积数，不写账单表，只更新原利息账单积数及利息记至日期，利息记至日期改到还款日
		lnsBill.setBillType("AMLT");
		//核销优化登记cancelflag，“3”
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
			lnsBill.setCancelFlag("3");
		}
		BigDecimal accumulate  = BigDecimal.valueOf(0.00);
		if (!VarChecker.isEmpty(hisBill.getAccumulate()))
		{
			accumulate = new BigDecimal(hisBill.getAccumulate().getVal());
		}
		accumulate  = accumulate.add(BigDecimal.valueOf(hisBill.getBillBal().getVal()*CalendarUtil.actualDaysBetween(hisBill.getIntendDate(), repayDate)));

		hisBill.setAccumulate(new FabAmount(accumulate.doubleValue()));
		lnsBill.setStatusbDate(lnsBill.getStartDate());
		FabAmount  interest = new FabAmount();
		lnsBill.setCcy(interest.getCurrency().getCcy());
		lnsBill.setBillAmt(interest);//账单金额
		lnsBill.setBillBal(interest);//账单余额
		
		return lnsBill;
	}
	
}  
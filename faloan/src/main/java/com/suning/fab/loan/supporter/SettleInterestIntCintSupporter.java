package com.suning.fab.loan.supporter;

import java.math.BigDecimal;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.VarChecker;

public class  SettleInterestIntCintSupporter extends SettleInterestSupporter{  
	@Override
	public  LnsBill settleInterest(LoanAgreement loanAgreement,LnsBill hisBill,String repayDate)
	{
		if (!CalendarUtil.after(repayDate, hisBill.getRepayendDate()))
		{
			return null;
		}
		
		LnsBill lnsBill = createLnsBill(hisBill, repayDate);
		lnsBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_CINT);
		//核销优化登记cancelflag，“3”
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
			lnsBill.setCancelFlag("3");
		}
		//复利记至账单逾期状态结束，之后不再计息
		if (CalendarUtil.afterAlsoEqual(repayDate,getLoanFormInfo().getStatusEndDate()) )
		{
			lnsBill.setEndDate(getLoanFormInfo().getStatusEndDate());
		}
		lnsBill.setCurendDate(lnsBill.getEndDate());
		
		//日利息
		BigDecimal dayInt = loanAgreement.getRateAgreement().getCompoundRate().getDayRate()
				            .multiply(new BigDecimal(hisBill.getBillBal().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
		//天数
		Integer intDays = CalendarUtil.actualDaysBetween(lnsBill.getStartDate(), lnsBill.getEndDate());
		//总利息
		BigDecimal intDec = new BigDecimal(intDays).multiply(dayInt);

		
		BigDecimal intDec1 = BigDecimal.valueOf(0.00);
		//加上积数产生的复利
		if (!VarChecker.isEmpty(hisBill.getAccumulate()))
		{
			if("yes".equals(GlobalScmConfUtil.getProperty("newKXQ", "no"))) {
				intDec1 = BigDecimal.valueOf(0.00);
			}
			else{
				intDec1 = new BigDecimal(hisBill.getAccumulate().getVal());
			}
		}

		intDec = intDec.add(intDec1.multiply(loanAgreement.getRateAgreement().getCompoundRate().getDayRate()))
				.setScale(2,BigDecimal.ROUND_HALF_UP );
		FabAmount interest = new FabAmount();
		interest.selfAdd(intDec.doubleValue());
		hisBill.setAccumulate(new FabAmount(0.00));
		
		lnsBill.setStatusbDate(lnsBill.getStartDate());
		lnsBill.setCcy(interest.getCurrency().getCcy());
		lnsBill.setBillAmt(interest);//账单金额
		lnsBill.setBillBal(interest);//账单余额
		lnsBill.setRepayendDate(lnsBill.getEndDate());
		lnsBill.setIntendDate(lnsBill.getEndDate());
		lnsBill.setBillRate(loanAgreement.getRateAgreement().getCompoundRate());
		return lnsBill;
		
	}
	
}  
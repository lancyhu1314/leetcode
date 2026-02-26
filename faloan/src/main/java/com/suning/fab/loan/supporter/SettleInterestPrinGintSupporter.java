package com.suning.fab.loan.supporter;

import java.math.BigDecimal;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.utils.VarChecker;

public  class SettleInterestPrinGintSupporter extends SettleInterestSupporter{  
	@Override
	public  LnsBill settleInterest(LoanAgreement loanAgreement,LnsBill hisBill,String repayDate)
	{
		//出宽限期之后直接记罚息
		if (CalendarUtil.after(repayDate, hisBill.getRepayendDate()))
		{
			return null;
		}
		
		if(CalendarUtil.afterAlsoEqual(hisBill.getIntendDate(), repayDate))
		{
			return null;
		}
		if("false".equals(loanAgreement.getInterestAgreement().getIsCalGrace())){
			return null;
		}
		
		LnsBill lnsBill = createLnsBill(hisBill, repayDate);
		FabAmount interest = new FabAmount();
		//账单类型
		lnsBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_GINT);
		//核销优化登记cancelflag，“3”
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
			lnsBill.setCancelFlag("3");
		}
		//利率为正常利率
		FabRate rate = loanAgreement.getRateAgreement().getNormalRate();
		
		//计算利息
		//日利息
		BigDecimal dayInt = BigDecimal.valueOf(rate.getDayRate().doubleValue())
				            .multiply(BigDecimal.valueOf(hisBill.getBillBal().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
		//天数
		BigDecimal intDec = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(lnsBill.getStartDate(), lnsBill.getEndDate()));
		//总利息
		intDec = intDec.multiply(dayInt);
		
		interest.selfAdd(intDec.doubleValue());
		//账单开始日期
		lnsBill.setStatusbDate(lnsBill.getStartDate());
		//币种
		lnsBill.setCcy(interest.getCurrency().getCcy());
		//账单金额
		lnsBill.setBillAmt(interest);//账单金额
		//账单余额
		lnsBill.setBillBal(interest);//账单余额
		
		//
		lnsBill.setCurendDate(lnsBill.getEndDate());
		
		lnsBill.setBillRate(rate);
		return lnsBill;
		
	}
	
}  
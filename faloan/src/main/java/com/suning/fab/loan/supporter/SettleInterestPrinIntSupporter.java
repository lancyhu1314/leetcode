package com.suning.fab.loan.supporter;

import java.math.BigDecimal;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.VarChecker;

public  class SettleInterestPrinIntSupporter extends SettleInterestSupporter{  
	@Override
	public  LnsBill settleInterest(LoanAgreement loanAgreement,LnsBill hisBill,String repayDate) throws FabException
	{

		//还款方式是非标还款方式时退出
		if (!(ConstantDeclare.REPAYWAY.REPAYWAY_XXHB).equals(loanAgreement.getWithdrawAgreement().getRepayWay())  &&
			!(ConstantDeclare.REPAYWAY.REPAYWAY_XBHX).equals(loanAgreement.getWithdrawAgreement().getRepayWay())  &&
			!(ConstantDeclare.REPAYWAY.REPAYWAY_YBYX).equals(loanAgreement.getWithdrawAgreement().getRepayWay()) )
		{
			return null;
		}
		
		//补息补到当期到期日
		if (CalendarUtil.after(repayDate, hisBill.getCurendDate()))
		{
			return null;
		}
		
		LnsBill lnsBill = createLnsBill(hisBill, repayDate);
		
		//账单类型
		lnsBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
			lnsBill.setCancelFlag("3");
		}
		lnsBill.setStartDate(hisBill.getIntendDate());
//		//利息记至账单到期日，之后不再计息
//		if (CalendarUtil.afterAlsoEqual(repayDate,hisBill.getEndDate()) )
//		{
//			lnsBill.setEndDate(hisBill.getEndDate());
//		}
		lnsBill.setEndDate(hisBill.getEndDate());

		lnsBill.setCurendDate(hisBill.getEndDate());
		
		
		if( ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(loanAgreement.getWithdrawAgreement().getRepayWay()))
		{
			//先本后息的利息应还款日记到合同到期日
			lnsBill.setCurendDate(loanAgreement.getContract().getContractEndDate());
			//期数放在最后一期
			lnsBill.setPeriod(loanAgreement.getUserDefineAgreement().size());
		}
		
		//利率为正常利率
		FabRate rate = loanAgreement.getRateAgreement().getNormalRate();
		//计算利息
		BigDecimal intDec = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(lnsBill.getStartDate(), hisBill.getEndDate()));
		intDec = intDec.multiply(BigDecimal.valueOf(rate.getDayRate().doubleValue()));
		intDec = intDec.multiply(BigDecimal.valueOf(hisBill.getBillBal().getVal())).setScale(2,BigDecimal.ROUND_HALF_UP );
		FabAmount interest = new FabAmount(); 
		interest.selfAdd(intDec.doubleValue());
		//账单开始日期
		lnsBill.setStatusbDate(lnsBill.getStartDate());
		//币种
		lnsBill.setCcy(interest.getCurrency().getCcy());
		//账单金额
		lnsBill.setBillAmt(interest);
		//账单余额
		lnsBill.setBillBal(interest);
		//本期应还款日
		lnsBill.setRepayendDate(CalendarUtil.nDaysAfter(lnsBill.getCurendDate(), loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
		//利息计止日期
		lnsBill.setIntendDate(lnsBill.getCurendDate());
		//账单利息
		lnsBill.setBillRate(rate);
			
		
		
		//计算计提利息 2018-01-24
		BigDecimal repayIntDec = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(lnsBill.getStartDate(), repayDate));
		repayIntDec = repayIntDec.multiply(BigDecimal.valueOf(rate.getDayRate().doubleValue()));
		repayIntDec = repayIntDec.multiply(BigDecimal.valueOf(hisBill.getBillBal().getVal())).setScale(2,BigDecimal.ROUND_HALF_UP );
		FabAmount repayInterest = new FabAmount(); 
		repayInterest.selfAdd(repayIntDec.doubleValue());
		lnsBill.setRepayDateInt(repayInterest);
		
		return lnsBill;
		
	}


}  
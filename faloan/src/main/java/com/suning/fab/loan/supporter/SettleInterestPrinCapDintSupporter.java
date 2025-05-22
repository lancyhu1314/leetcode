package com.suning.fab.loan.supporter;

import java.math.BigDecimal;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.utils.VarChecker;

public  class SettleInterestPrinCapDintSupporter extends SettleInterestSupporter{  
	@Override
	public  LnsBill settleInterest(LoanAgreement loanAgreement,LnsBill hisBill,String repayDate)
	{
		if (!CalendarUtil.after(repayDate, hisBill.getRepayendDate()))
		{
			return null;
		}
		
		if(CalendarUtil.afterAlsoEqual(hisBill.getIntendDate(), repayDate))
		{
			return null;
		}
		
		LnsBill lnsBill = createLnsBill(hisBill, repayDate);
		//账单类型
		lnsBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
		//核销优化登记cancelflag，“3”
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
			lnsBill.setCancelFlag("3");
		}
		//罚息记至账单逾期状态结束，之后不再计息
		if (CalendarUtil.afterAlsoEqual(repayDate,getLoanFormInfo().getStatusEndDate()) )
		{
			lnsBill.setEndDate(getLoanFormInfo().getStatusEndDate());
		}
		lnsBill.setCurendDate(lnsBill.getEndDate());
		//利率为逾期利率
		FabRate rate = loanAgreement.getRateAgreement().getOverdueRate();
		
		//计算罚息
		//天数
		BigDecimal intDec = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(lnsBill.getStartDate(), lnsBill.getEndDate()));
		//日利息
		BigDecimal dayInt = BigDecimal.valueOf(rate.getDayRate().doubleValue());

		//2-合同余额
		if("2".equals(loanAgreement.getInterestAgreement().getDintSource()))
		{
			dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getBalance().getVal()));
		}
		//3-合同金额
		else if("3".equals(loanAgreement.getInterestAgreement().getDintSource()))
		{
			dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()));
		}
		//4-未还本金 仅房抵贷封顶计息用
        else if("4".equals(loanAgreement.getInterestAgreement().getDintSource())){
            dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getBasicExtension().getSumPrin().getVal()));
            lnsBill.setPrinBal(new FabAmount(loanAgreement.getBasicExtension().getSumPrin().getVal()));
		}
		else
		{
			dayInt = dayInt.multiply(BigDecimal.valueOf(hisBill.getBillBal().getVal()));
		}

		//封顶计息20190117
		//上次更新日期与当天之间天数
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
			//上次更新日到试算日之间天数
			BigDecimal capIntDays = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(CalendarUtil.after(loanAgreement.getBasicExtension().getComeinDate(), lnsBill.getStartDate())?loanAgreement.getBasicExtension().getComeinDate():lnsBill.getStartDate()
					, CalendarUtil.beforeAlsoEqual(repayDate, getLoanFormInfo().getStatusEndDate())?repayDate:getLoanFormInfo().getStatusEndDate()));
			if (CalendarUtil.afterAlsoEqual(repayDate,getLoanFormInfo().getStatusEndDate()) && CalendarUtil.afterAlsoEqual(loanAgreement.getBasicExtension().getComeinDate(), getLoanFormInfo().getStatusEndDate()))
			{
				capIntDays = BigDecimal.valueOf(0.0);
			}


			//上次更新日到试算日之间金额
			BigDecimal capDintDec = capIntDays.multiply(dayInt.setScale(2, BigDecimal.ROUND_HALF_UP));
			//判断新增金额是否超过封顶金额
			FabAmount capDint  = getCapInterest(new FabAmount(capDintDec.doubleValue()),loanAgreement);
			//计算已计未结金额
			//账本开始日到更新日天数*日利息
			BigDecimal beforeCapIntDec = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(lnsBill.getStartDate()
					,CalendarUtil.beforeAlsoEqual(loanAgreement.getBasicExtension().getComeinDate(), getLoanFormInfo().getStatusEndDate())?loanAgreement.getBasicExtension().getComeinDate():getLoanFormInfo().getStatusEndDate())).multiply(dayInt.setScale(2, BigDecimal.ROUND_HALF_UP));
			if(beforeCapIntDec.compareTo(BigDecimal.ZERO)<0){
				beforeCapIntDec = BigDecimal.valueOf(0.0);
			}
			//小老弟遗留bug  201910-11
			if(beforeCapIntDec.compareTo(BigDecimal.valueOf(loanAgreement.getBasicExtension().getHisDintComein().getVal()))>0){
				beforeCapIntDec = BigDecimal.valueOf(loanAgreement.getBasicExtension().getHisDintComein().getVal());
			}
			BigDecimal finalCapDint;
			if(!VarChecker.isEmpty(capDint)){
				//计算账本开始日到试算日的总金额
				finalCapDint =  beforeCapIntDec.add(BigDecimal.valueOf(capDint.getVal()));
				//新增金额累加进收益
				loanAgreement.getBasicExtension().getCalDintComein().selfAdd(capDint);
			}else{
				finalCapDint =  beforeCapIntDec.add(capDintDec);
				loanAgreement.getBasicExtension().getCalDintComein().selfAdd(new FabAmount(capDintDec.doubleValue()));
			}
			intDec = finalCapDint;
		}else{
			//总利息
			intDec = intDec.multiply(dayInt.setScale(2, BigDecimal.ROUND_HALF_UP));
		}
		
				
		//封顶计息判断
//		FabAmount dintAmount = getCapInterest(new FabAmount(intDec.doubleValue()),loanAgreement);
//		if(!VarChecker.isEmpty(dintAmount)){
//			intDec = BigDecimal.valueOf(dintAmount.getVal());
//		}
			

		
//		loanAgreement.getBasicExtension().getCalDintComein().selfAdd(new FabAmount(intDec.doubleValue()));
		FabAmount interest = new FabAmount(); 
		interest.selfAdd(intDec.doubleValue());


		//账单开始日期
		lnsBill.setStatusbDate(lnsBill.getStartDate());
		//币种
		lnsBill.setCcy(interest.getCurrency().getCcy());
		//账单金额
		lnsBill.setBillAmt(interest);//账单金额
		//账单余额
		lnsBill.setBillBal(interest);//账单余额
		lnsBill.setRepayendDate(lnsBill.getEndDate());
		lnsBill.setIntendDate(lnsBill.getEndDate());
		lnsBill.setBillRate(rate);
		return lnsBill;
		
	}
	
	public FabAmount getCapInterest(FabAmount currInterest,LoanAgreement loanAgreement) {
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
			if(!currInterest.add(loanAgreement.getBasicExtension().getCalDintComein()).
					sub(new FabAmount(loanAgreement.getBasicExtension().getCapAmt().getVal())).isNegative()){
				FabAmount result = new FabAmount((loanAgreement.getBasicExtension().getCapAmt().getVal())
						-loanAgreement.getBasicExtension().getCalDintComein().getVal());
				if(result.isNegative()){
					return new FabAmount(0.0);
				}
				return result;
			}else{
				return null;
			}
		}
		return null;
	}
	
}  
package com.suning.fab.loan.utils;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 
该类包含一个公共方法genCintDintList,用于生成呆滞呆账期间的罚息和复利账单列表
genCintDintList入参:贷款客户账号(借据号),CTX
genCintDintList出参：List<lnsBill>

**/


public class LoanPlanCintDint {
	private LoanPlanCintDint(){
		//nothing to do
	}

//	public static List<lnsBill> genCintDintList(String acctNo, TranCtx ctx) throws FabException{
//
//		//获取贷款协议信息
//		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
//
//		//生成还款计划
//		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
//		//历史期账单（账单表）
//		List<LnsBill> hisLnsbill = new ArrayList<>();
//		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
//		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
//		hisLnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
//
////		hisLnsbill.addAll(lnsBillStatistics.getBillInfoList());
//
//		return getTblLnsbills(acctNo, ctx, la, lnsBillStatistics, hisLnsbill,ctx.getTranDate());
//
//	}
    //20191022 更改为根据repayDate 试算
	//20191118 更改返回为 LnsBill
	private static List<LnsBill> getTblLnsbills(String acctNo, TranCtx ctx, LoanAgreement la, LnsBillStatistics lnsBillStatistics, List<LnsBill> hisLnsbill,String repayDate) {
		//定义返回呆滞呆账罚息复利账单list
		List<LnsBill> cdBillList = new ArrayList<>();
		FabAmount sumlbcdint = new FabAmount(0.0);
		FabAmount sumPrin = new FabAmount(0.0);
		//20191104 当前期的账本加入了历史账本
		//封顶计息判断----20190124
		if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
				//房抵贷罚息在首期
				||VarChecker.asList("2412615","4010002").contains(la.getPrdId())){
			for(LnsBill lnsHisbill : hisLnsbill){
				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.BILLTYPE.BILLTYPE_DINT).contains(lnsHisbill.getBillType()) ) {
					sumlbcdint.selfAdd(lnsHisbill.getBillAmt());
				}
				if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
					sumPrin.selfAdd(lnsHisbill.getBillBal());
				}
			}
			for(LnsBill lnsbill : lnsBillStatistics.getFutureBillInfoList()){
				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsbill.getBillType()) && !VarChecker.isEmpty(lnsbill.getRepayDateInt())) {
					sumlbcdint.selfAdd(lnsbill.getRepayDateInt());
				}
				//加上未来期的本金
				if( lnsbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
					sumPrin.selfAdd(lnsbill.getBillBal());
				}
			}
			//本金直接从所有的账本里面算
//			sumPrin.selfAdd(la.getContract().getBalance());
//			sumlbcdint.selfAdd(la.getBasicExtension().getHisDintComein());
			la.getBasicExtension().setCalDintComein(sumlbcdint);
		}
		//设置所有的未还本金
		la.getBasicExtension().setSumPrin(sumPrin);
		boolean breakCap = false;
		//循环处理呆滞呆账状态的本金和利息账单
		for (LnsBill lnsHisbill : hisLnsbill) {
			//首先处理呆滞呆账本金产生 罚息账单

			if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
					|| (lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
							&& (!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())|| VarChecker.asList("2412615","4010002").contains(la.getPrdId())   )
							&& la.getContract().getFlag1().contains("C"))) cap:{
				if((breakCap && (!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())||VarChecker.asList("2412615","4010002").contains(la.getPrdId()))) || (sumPrin.isZero() && !VarChecker.isEmpty(la.getInterestAgreement().getCapRate()))){
					if(la.getContract().getFlag1().contains("C")){
						break cap;
					}

				}
				if( VarChecker.asList("3","4").contains(la.getInterestAgreement().getDintSource()) &&
				lnsHisbill.getSettleFlag().equals("CLOSE")	)
				{
					continue;
				}

				if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
						&& la.getContract().getFlag1().contains("C")
						&& lnsHisbill.getSettleFlag().equals("CLOSE")){
					continue;
				}
				String repayDate1 = repayDate;
				//房抵贷跑批计提特殊处理
				if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate()) && la.getContract().getFlag1().contains("C")){


					//封顶计息特殊处理20190127
					//计算整笔呆滞呆账逾期开始日期
					String loanform = PropertyUtil.getPropertyOrDefault("transfer."+String.valueOf(3),null);

					if (loanform == null)
						return null;
					String tmp[] = loanform.split(":");
					//逾期天数
					Integer days = Integer.parseInt(tmp[1]);


					String baddebtsDate = CalendarUtil.nDaysAfter(la.getContract().getContractEndDate(), days).toString("yyyy-MM-dd");
					if(VarChecker.asList( "479001").contains(ctx.getTranCode())){
						repayDate1 = CalendarUtil.after(repayDate,baddebtsDate)?baddebtsDate:repayDate;
						if(CalendarUtil.after(lnsHisbill.getIntendDate(), repayDate1)){
							repayDate1 = lnsHisbill.getIntendDate();
						}
					}

				}
				FabAmount famt;
				if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
						&& (!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())||VarChecker.asList("2412615","4010002").contains(la.getPrdId()))) {
					famt = LoanBillSettleInterestSupporter.calculateCapBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), repayDate1);
				}else{
					if((!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())||VarChecker.asList("2412615","4010002").contains(la.getPrdId())) && la.getContract().getFlag1().contains("C")) {
						famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), repayDate1);
					}else{
						famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), repayDate);
					}

				}
				breakCap = true;
				if(famt == null || famt.isZero())
					continue;

				LnsBill lnsBill = new LnsBill();
				lnsBill.setTranDate(Date.valueOf(ctx.getTranDate()));
				lnsBill.setSerSeqno(ctx.getSerSeqNo());
				lnsBill.setTxSeq(0);
//				lnsBill.setAcctno(acctNo);
//				lnsBill.setBrc(ctx.getBrc());
				lnsBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				lnsBill.setPeriod(lnsHisbill.getPeriod());
				lnsBill.setBillAmt(famt);
				lnsBill.setBillBal(famt);
				lnsBill.setPrinBal(lnsHisbill.getBillBal());
				lnsBill.setBillRate(la.getRateAgreement().getOverdueRate());
				lnsBill.setStartDate(lnsHisbill.getIntendDate());
				lnsBill.setEndDate(ctx.getTranDate());
				lnsBill.setCurendDate(ctx.getTranDate());
				lnsBill.setRepayendDate(ctx.getTranDate());
				lnsBill.setIntendDate(ctx.getTranDate());
				lnsBill.setRepayWay(lnsHisbill.getRepayWay());
				lnsBill.setCcy(lnsHisbill.getCcy());
				lnsBill.setDerTranDate(lnsHisbill.getTranDate());
				lnsBill.setDerSerseqno(lnsHisbill.getSerSeqno());
				lnsBill.setDerTxSeq(lnsHisbill.getTxSeq());
				lnsBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				lnsBill.setStatusbDate(lnsHisbill.getIntendDate());
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				lnsBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				lnsBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				//核销优化登记cancelflag，“3”
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					lnsBill.setCancelFlag("3");
				}
				lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				lnsBill.setHisBill(lnsHisbill);
				cdBillList.add(lnsBill);
			}

			//处理呆滞利息产生呆滞复利账单
			if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) ) {

				if( VarChecker.asList("3","4").contains(la.getInterestAgreement().getDintSource()) &&
				lnsHisbill.getSettleFlag().equals("CLOSE")	)
				{
					continue;
				}

				//当前工作日期减复利记至日期得到计复利天数
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getCompoundRate(), repayDate);
				if(famt == null || famt.isZero())
					continue;

				LnsBill lnsBill = new LnsBill();
				lnsBill.setTranDate(Date.valueOf(ctx.getTranDate()));
				lnsBill.setSerSeqno(ctx.getSerSeqNo());
				lnsBill.setTxSeq(0);
//				lnsBill.setAcctno(acctNo);
//				lnsBill.setBrc(ctx.getBrc());
				lnsBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_CINT);
				lnsBill.setPeriod(lnsHisbill.getPeriod());
				lnsBill.setBillAmt(famt);
				lnsBill.setBillBal(famt);
				lnsBill.setPrinBal(lnsHisbill.getBillBal());
				lnsBill.setBillRate(la.getRateAgreement().getCompoundRate());
				lnsBill.setStartDate(lnsHisbill.getIntendDate());
				lnsBill.setEndDate(ctx.getTranDate());
				lnsBill.setCurendDate(ctx.getTranDate());
				lnsBill.setRepayendDate(ctx.getTranDate());
				lnsBill.setIntendDate(ctx.getTranDate());
				lnsBill.setRepayWay(lnsHisbill.getRepayWay());
				lnsBill.setCcy(lnsHisbill.getCcy());
				lnsBill.setDerTranDate(lnsHisbill.getTranDate());
				lnsBill.setDerSerseqno(lnsHisbill.getSerSeqno());
				lnsBill.setDerTxSeq(lnsHisbill.getTxSeq());
				lnsBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				lnsBill.setStatusbDate(lnsHisbill.getIntendDate());
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				lnsBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				lnsBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				//核销优化登记cancelflag，“3”
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					lnsBill.setCancelFlag("3");
				}
				lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				lnsBill.setHisBill(lnsHisbill);
				cdBillList.add(lnsBill);
			}


			//呆滞呆账的违约金
//			if( LoanFeeUtils.isFeeType(lnsHisbill.getBillType())) {
//				//结清的费用
//				if(lnsHisbill.getSettleFlag().equals(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE)
//						//没有逾期费率
//						||lnsHisbill.getLnsfeeinfo().getOverrate().compareTo(0.00)<=0)
//				{
//					//不用计算
//					continue;
//				}
//				//计算逾期开始日期  逾期90天  没从配置文件取
//				String overStartDate = CalendarUtil.nDaysAfter(lnsHisbill.getCurendDate(), 90).toString("yyyy-MM-dd");
//
//				//未呆滞呆账不计罚息
//				if (CalendarUtil.before(repayDate, overStartDate))
//				{
//					continue;
//				}
//				if (CalendarUtil.before(overStartDate, lnsHisbill.getIntendDate()))
//				{
//					overStartDate = lnsHisbill.getIntendDate();
//				}
//				LnsBill lnsBill = new LnsBill();
//				lnsBill.setPeriod(lnsHisbill.getPeriod());//期数
//				lnsBill.setStartDate(overStartDate);
//
//				lnsBill.setEndDate(repayDate);
//				lnsBill.setCurendDate(ctx.getTranDate());
//				lnsBill.setDerTranDate(lnsHisbill.getTranDate());//账务日期
//				lnsBill.setDerSerseqno(lnsHisbill.getSerSeqno());//流水号
//				lnsBill.setDerTxSeq(lnsHisbill.getTxSeq());//子序号
//				lnsBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);//账单状态N正常G宽限期O逾期L呆滞B呆账
//				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET);//账单属性INTSET正常结息REPAY还款
//				lnsBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);//利息入账标志NO未入YES已入
//				lnsBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);//账单作废标志NORMAL正常CANCEL作废
//				lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);//结清标志RUNNING未结CLOSE已结
//				lnsBill.setRepayendDate(lnsBill.getEndDate());
//				lnsBill.setIntendDate(lnsBill.getEndDate());
//				lnsBill.setPrinBal(new FabAmount(lnsHisbill.getBillBal().getVal()));//账单对应本金余额
//				//违约金类型
//				lnsBill.setBillType(LoanFeeUtils.feePenaltyType(lnsHisbill.getBillType()));
//
//				//日违约金
//				BigDecimal dayInt = new FabRate(Double.toString(lnsHisbill.getLnsfeeinfo().getOverrate())).getDayRate()
//						.multiply(new BigDecimal(lnsHisbill.getBillBal().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
//				//天数
//				Integer intDays = CalendarUtil.actualDaysBetween(lnsBill.getStartDate(), lnsBill.getEndDate());
//				//总利息
//				BigDecimal intDec = new BigDecimal(intDays).multiply(dayInt);
//
//
//
//
//				FabAmount interest = new FabAmount();
//				interest.selfAdd(intDec.doubleValue());
//
//				lnsBill.setStatusbDate(lnsBill.getStartDate());
//				lnsBill.setCcy(interest.getCurrency().getCcy());
//				lnsBill.setBillAmt(interest);//账单金额
//				lnsBill.setBillBal(interest);//账单余额
//				lnsBill.setRepayendDate(lnsBill.getEndDate());
//				lnsBill.setIntendDate(lnsBill.getEndDate());
//				lnsBill.setBillRate(la.getRateAgreement().getCompoundRate());
//				cdBillList.add(lnsBill);
//
//			}


		}
		return cdBillList;
	}

	public static List<LnsBill> genCintDintListC(String acctNo, TranCtx ctx,LnsBillStatistics lnsBillStatistics,LoanAgreement la,String repayDate) throws FabException{

		//生成还款计划
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<>();
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		hisLnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		hisLnsbill.addAll(lnsBillStatistics.getBillInfoList());


		return getTblLnsbills(acctNo, ctx, la, lnsBillStatistics, hisLnsbill,repayDate);


	}

}

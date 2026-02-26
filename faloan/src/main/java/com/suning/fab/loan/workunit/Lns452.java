package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanBillSettleInterestSupporter;
import com.suning.fab.loan.utils.LoanPlanCintDint;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	AY
 *
 * @version V1.0.1
 *
 * @see 	预约还款查询
 *
 * @param	acctNo			借据号
 * 			endDate			预约还款日期
 *
 * @return	cleanPrin		结清本金
 *			prinAmt			正常还款本金
 *			cleanInt		结清利息
 *			intAmt			正常还款利息
 *			cleanForfeit	结清罚息
 *			forfeitAmt		正常还款罚息
 *			overPrin		逾期本金
 *			overInt			逾期利息
 *			overDint		逾期罚息
 *			overFee			逾期管理费
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns452 extends WorkUnit {

	String acctNo;
	String endDate;

	FabAmount cleanPrin = new FabAmount();
	FabAmount prinAmt = new FabAmount();
	FabAmount cleanInt = new FabAmount();
	FabAmount intAmt = new FabAmount();
	FabAmount cleanForfeit = new FabAmount();
	FabAmount forfeitAmt = new FabAmount();
	FabAmount overPrin = new FabAmount();
	FabAmount overInt = new FabAmount();
	FabAmount overDint = new FabAmount();
	LnsBillStatistics lnsBillStatistics;
	TranCtx ctx;
	TblLnsbasicinfo lnsbasicinfo ;
	LoanAgreement la;
	@Override
	public void run() throws Exception {
		ctx = getTranctx();

		if(VarChecker.isEmpty(endDate)){
			throw new FabException("LNS005");
		}
		//预约日期大于交易日
		if(CalendarUtil.before(endDate, ctx.getTranDate())){
			throw new FabException("LNS034");
		}

		//读取借据主文件
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());

		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if (null == lnsbasicinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		//结息日为空就取起息日
		if(VarChecker.isEmpty(lnsbasicinfo.getLastintdate())){
			lnsbasicinfo.setLastintdate(lnsbasicinfo.getBeginintdate());
		}
		
		//根据账号生成协议\
		la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,la);
				
		if (la.getContract().getContractStartDate().equals(endDate) && 
				"3".equals(la.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
		}
		if(CalendarUtil.equalDate(lnsbasicinfo.getOpendate(), lnsbasicinfo.getBeginintdate())){
			//开户日当天还款还到最后一期本金，但是没还清
			if(!CalendarUtil.equalDate(lnsbasicinfo.getLastintdate(), lnsbasicinfo.getContduedate())
					&& CalendarUtil.equalDate(endDate, lnsbasicinfo.getOpendate())){
				if(new FabAmount(lnsbasicinfo.getContractamt())
						.sub(new FabAmount(lnsbasicinfo.getContractbal())).isZero()){
					cleanPrin = new FabAmount(lnsbasicinfo.getContractbal());
					return;  //如果是倒起息开户 当天预约那就有利息不能直接返回 此段放子交易最后 重置本金
				}
			}
		}
		
		//生成还款计划
		 lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, endDate, ctx);
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		List<LnsBill> lnsbill = new ArrayList<LnsBill>();
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前期和未来期
		lnsbill.addAll(lnsBillStatistics.getBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());

		//获取呆滞呆账期间新罚息复利账单list
		List<TblLnsbill> cdbillList = genCintDintList(getAcctNo(), ctx,endDate);
		for( TblLnsbill tblLnsbill:cdbillList)
		{
			hisLnsbill.add(BillTransformHelper.convertToLnsBill(tblLnsbill));
		}
		
		String repayDate = endDate;
		//预约日期大于合同结束日期取合同结束日
		if(CalendarUtil.after(endDate, lnsbasicinfo.getContduedate())){
			endDate = lnsbasicinfo.getContduedate();
		}
		//用于记录提前还款还了部分账单的期数标识
		int period = 0;
		//遍历历史账单
		for(LnsBill bill:hisLnsbill){
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(bill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE).contains(bill.getSettleFlag())){
				continue;
			}

			if(CalendarUtil.after(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){

					if( VarChecker.asList("11", "12", "13").contains(lnsbasicinfo.getRepayway()) && !VarChecker.isEmpty(bill.getHisBill()) )
					{
						cleanInt.selfAdd(bill.getRepayDateInt());
						intAmt.selfAdd(bill.getRepayDateInt());
					}		
					else
					{	
						cleanInt.selfAdd(bill.getBillBal());
						intAmt.selfAdd(bill.getBillBal());
						FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
						if (fint != null)
						{
							cleanForfeit.selfAdd(fint);
							forfeitAmt.selfAdd(fint);
							overDint.selfAdd(fint);
						}
					}
					if(period < bill.getPeriod()){
						period = bill.getPeriod();
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
				continue;
			}
			//历史期
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(), endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overPrin.selfAdd(bill.getBillBal());
					}
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					cleanInt.selfAdd(bill.getBillBal());
					intAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overInt.selfAdd(bill.getBillBal());
					}
					//计算呆滞呆账罚息复利
					FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
					if (fint != null)
					{
						cleanForfeit.selfAdd(fint);
						forfeitAmt.selfAdd(fint);
						overDint.selfAdd(fint);
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
			}
			//未来期
			else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					if( VarChecker.asList("11", "12", "13").contains(lnsbasicinfo.getRepayway()) && !VarChecker.isEmpty(bill.getHisBill()) )
					{
						cleanInt.selfAdd(bill.getRepayDateInt());
					}		
					else
					{	
						cleanInt.selfAdd(bill.getBillBal());
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
			}
		}
		String termStartDate = "";
		int termperiod = 0;
		//遍历当前期账单和未来期账单
		for(LnsBill bill:lnsbill){
			
			if ("".equals(termStartDate) || CalendarUtil.after(bill.getStartDate(), termStartDate)){
				termStartDate = bill.getStartDate();
			}
			
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(bill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE ).contains(bill.getSettleFlag())){
				continue;
			}
			if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
				FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
				if (fint != null)
				{
					cleanForfeit.selfAdd(fint);
					forfeitAmt.selfAdd(fint);
					overDint.selfAdd(fint);
				}
			}

			//当前期
			if((CalendarUtil.after(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())
					&& VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, 
							ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())
					&& (period == 0 || period >= bill.getPeriod())) 
					|| ("true".equals(la.getInterestAgreement().getIsCalTail())
							&& CalendarUtil.afterAlsoEqual(endDate, bill.getStartDate())
							&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())
							&& VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, 
									ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())
							&& (period == 0 || period >= bill.getPeriod()))){//还款方式为12（先本后息）时，会先还利息，第一期的本金会在未来期出现
				
				if(termperiod == 0){
					termperiod = bill.getPeriod();
				}
				
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					if(termperiod == bill.getPeriod()){
						prinAmt.selfAdd(bill.getBillBal());
					}
				} 
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					//等本等息
					if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
						intAmt.selfAdd(bill.getBillBal());
						cleanInt.selfAdd(bill.getBillBal());
					}
					//非等本等息
					else {
						intAmt.selfAdd(bill.getRepayDateInt());
						cleanInt.selfAdd(bill.getRepayDateInt());
					}
				}
				continue;
			}
			//试算出的历史期
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(), endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overPrin.selfAdd(bill.getBillBal());
					}
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					cleanInt.selfAdd(bill.getBillBal());
					intAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overInt.selfAdd(bill.getBillBal());
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
			}
			//未来期
			else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					//还款方式为8（随借随还）时，还多少本金插多少本金，当天之后的本金全部属于未来期
					if("8".equals(lnsbasicinfo.getRepayway())){
						prinAmt.selfAdd(bill.getBillBal());
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
			}
		}
		//开户日当天还款还到最后一期本金，但是没还清
		/*if(!CalendarUtil.equalDate(lnsbasicinfo.getLastintdate(), lnsbasicinfo.getContduedate())
				&& CalendarUtil.equalDate(endDate, lnsbasicinfo.getOpendate())){
			cleanPrin = new FabAmount(lnsbasicinfo.getContractbal());
			//return;  如果是倒起息开户 当天预约那就有利息不能直接返回 此段放子交易最后 重置本金
		}*/
		if("12".equals(lnsbasicinfo.getRepayway()) && 
				endDate.compareTo(termStartDate) <= 0)
		{
			intAmt.setVal(0.00);
		}	
		
	}

	public List<TblLnsbill> genCintDintList(String acctNo, TranCtx ctx,String bookDate) throws FabException{
		String repayDate = null;
		//定义返回呆滞呆账罚息复利账单list
		List<TblLnsbill> cdBillList = new ArrayList<TblLnsbill>();	
		//获取贷款协议信息

		//生成还款计划
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, bookDate, ctx);
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		hisLnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		hisLnsbill.addAll(lnsBillStatistics.getBillInfoList());

//		//定义未来期lns账单list（用于生成未来期还款计划）
//		List<LnsBill> listFutureBillInfo = new ArrayList<LnsBill>();
//		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
//		listFutureBillInfo.addAll(lnsBillStatistics.getBillInfoList());
//		//未来期账单：从还款日到合同到期日之间的本金和利息账单
//		listFutureBillInfo.addAll(lnsBillStatistics.getFutureBillInfoList());
		FabAmount sumlbcdint = new FabAmount(0.0);
		FabAmount sumPrin = new FabAmount(0.0);

		//20191104 当前期的账本加入了历史账本
		//封顶计息判断----20190124
		if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
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
				//本金直接从所有的账本里面算
				//加上未来期的本金
				if( lnsbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
					sumPrin.selfAdd(lnsbill.getBillBal());
				}
			}
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
							&& (!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
					//房抵贷动态封顶
					||VarChecker.asList("2412615","4010002").contains(la.getPrdId()))
							&& la.getContract().getFlag1().contains("C"))) cap:{
				if((breakCap && (!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())||VarChecker.asList("2412615","4010002").contains(la.getPrdId()))) || (sumPrin.isZero() && !VarChecker.isEmpty(la.getInterestAgreement().getCapRate()))){
					if(la.getContract().getFlag1().contains("C")){
						break cap;
					}
					
				}
				if(lnsHisbill.getSettleFlag().equals("CLOSE")	)
				{
					continue;
				}

				//房抵贷跑批计提特殊处理
				if((!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())||VarChecker.asList("2412615","4010002").contains(la.getPrdId())) && la.getContract().getFlag1().contains("C")){
					
				
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
						repayDate = CalendarUtil.after(bookDate,baddebtsDate)?baddebtsDate:bookDate; 
						if(CalendarUtil.after(lnsHisbill.getIntendDate(), repayDate)){
							repayDate = lnsHisbill.getIntendDate();
						}
					}else{
						repayDate = bookDate;
					}
					
				}
				FabAmount famt;
				if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) 
						&& (!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())||VarChecker.asList("2412615","4010002").contains(la.getPrdId()))) {
					famt = LoanBillSettleInterestSupporter.calculateCapBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), repayDate);
				}else{
					if((!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())||VarChecker.asList("2412615","4010002").contains(la.getPrdId()) )&& la.getContract().getFlag1().contains("C")) {
						famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), repayDate);
					}else{
						famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), bookDate);
					}
					
				}
				breakCap = true;
				if(famt == null || famt.isZero())
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				tblLnsbill.setPeriod(lnsHisbill.getPeriod());
				tblLnsbill.setBillamt(famt.getVal());
				tblLnsbill.setBillbal(famt.getVal());
				tblLnsbill.setPrinbal(lnsHisbill.getBillBal().getVal());
				tblLnsbill.setBillrate(la.getRateAgreement().getOverdueRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsHisbill.getIntendDate());
				tblLnsbill.setEnddate(bookDate);
				tblLnsbill.setCurenddate(bookDate);
				tblLnsbill.setRepayedate(bookDate);
				tblLnsbill.setIntedate(bookDate);
				tblLnsbill.setRepayway(lnsHisbill.getRepayWay());
				tblLnsbill.setCcy(lnsHisbill.getCcy());
				tblLnsbill.setDertrandate(lnsHisbill.getTranDate());
				tblLnsbill.setDerserseqno(lnsHisbill.getSerSeqno());
				tblLnsbill.setDertxseq(lnsHisbill.getTxSeq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsHisbill.getIntendDate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}

				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				cdBillList.add(tblLnsbill);
			}
			
		}	
		
		return cdBillList;
	}
	
	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	/**
	 * @return the endDate
	 */
	public String getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the cleanPrin
	 */
	public FabAmount getCleanPrin() {
		return cleanPrin;
	}

	/**
	 * @param cleanPrin the cleanPrin to set
	 */
	public void setCleanPrin(FabAmount cleanPrin) {
		this.cleanPrin = cleanPrin;
	}

	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}

	/**
	 * @param prinAmt the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}

	/**
	 * @return the cleanInt
	 */
	public FabAmount getCleanInt() {
		return cleanInt;
	}

	/**
	 * @param cleanInt the cleanInt to set
	 */
	public void setCleanInt(FabAmount cleanInt) {
		this.cleanInt = cleanInt;
	}

	/**
	 * @return the intAmt
	 */
	public FabAmount getIntAmt() {
		return intAmt;
	}

	/**
	 * @param intAmt the intAmt to set
	 */
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}

	/**
	 * @return the cleanForfeit
	 */
	public FabAmount getCleanForfeit() {
		return cleanForfeit;
	}

	/**
	 * @param cleanForfeit the cleanForfeit to set
	 */
	public void setCleanForfeit(FabAmount cleanForfeit) {
		this.cleanForfeit = cleanForfeit;
	}

	/**
	 * @return the forfeitAmt
	 */
	public FabAmount getForfeitAmt() {
		return forfeitAmt;
	}

	/**
	 * @param forfeitAmt the forfeitAmt to set
	 */
	public void setForfeitAmt(FabAmount forfeitAmt) {
		this.forfeitAmt = forfeitAmt;
	}

	public FabAmount getOverPrin() {
		return overPrin;
	}

	public void setOverPrin(FabAmount overPrin) {
		this.overPrin = overPrin;
	}

	public FabAmount getOverInt() {
		return overInt;
	}

	public void setOverInt(FabAmount overInt) {
		this.overInt = overInt;
	}

	public FabAmount getOverDint() {
		return overDint;
	}

	public void setOverDint(FabAmount overDint) {
		this.overDint = overDint;
	}

	/**
	 * 
	 * @return the lnsBillStatistics
	 */
	public LnsBillStatistics getLnsBillStatistics() {
		return lnsBillStatistics;
	}

	/**
	 * @param lnsBillStatistics the lnsBillStatistics to set
	 */
	public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
		this.lnsBillStatistics = lnsBillStatistics;
	}

	/**
	 * @return the ctx
	 */
	public TranCtx getCtx() {
		return ctx;
	}

	/**
	 * @param ctx the ctx to set
	 */
	public void setCtx(TranCtx ctx) {
		this.ctx = ctx;
	}

	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}

	/**
	 * Gets the value of la.
	 *
	 * @return the value of la
	 */
	public LoanAgreement getLa() {
		return la;
	}

	/**
	 * Sets the la.
	 *
	 * @param la la
	 */
	public void setLa(LoanAgreement la) {
		this.la = la;

	}
}

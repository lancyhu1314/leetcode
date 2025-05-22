package com.suning.fab.loan.utils;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 
该类包含一个公共方法genCintDintList,用于生成呆滞呆账期间的罚息和复利账单列表
 genCintDintList入参:贷款客户账号(借据号),CTX
 genCintDintList出参：List<TblLnsbill>

**/


public class LoanLanBadCintDint {
	private LoanLanBadCintDint(){
		//nothing to do
	}

	public static List<TblLnsbill> genCintDintList(String acctNo, TranCtx ctx) throws FabException{
		//定义返回呆滞呆账罚息复利账单list
		List<TblLnsbill> cdBillList = new ArrayList<TblLnsbill>();	
		//获取贷款协议信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		
		// 读取贷款账单
		Map<String, Object> parambill = new HashMap<String, Object>();
		parambill.put("acctno", acctNo);
		parambill.put("brc", ctx.getBrc());
		List<TblLnsbill> billList;
		try {
			billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", parambill, TblLnsbill.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}
		
		//循环处理呆滞呆账状态的本金和利息账单
		for (TblLnsbill lnsbill : billList) {
			//首先处理呆滞呆账本金产生 罚息账单
			if( (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbill.getBillstatus()) || 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbill.getBillstatus())) && 
					lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
				//当前工作日期减罚息记至日期得到计罚息天数
				int nDays = CalendarUtil.actualDaysBetween(lnsbill.getIntedate(),ctx.getTranDate());
				if(nDays <= 0) //罚息已记至当日
					continue;
				
				BigDecimal intDec = new BigDecimal(nDays);
				//取账单余额 从la中取得利率
				intDec = (intDec.multiply(la.getRateAgreement().getOverdueRate().getDayRate())
						.multiply(new BigDecimal(lnsbill.getBillbal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
				FabAmount cBillBal = new FabAmount(intDec.doubleValue());
				
				if(cBillBal.isZero()) //计算罚息金额为零
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				tblLnsbill.setPeriod(lnsbill.getPeriod());
				tblLnsbill.setBillamt(cBillBal.getVal());
				tblLnsbill.setBillbal(cBillBal.getVal());
				tblLnsbill.setPrinbal(lnsbill.getBillbal());
				tblLnsbill.setBillrate(la.getRateAgreement().getCompoundRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsbill.getIntedate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsbill.getRepayway());
				tblLnsbill.setCcy(lnsbill.getCcy());
				tblLnsbill.setDertrandate(lnsbill.getTrandate());
				tblLnsbill.setDerserseqno(lnsbill.getSerseqno());
				tblLnsbill.setDertxseq(lnsbill.getTxseq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsbill.getIntedate());
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
			
			//处理呆滞利息产生呆滞复利账单
			if( (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbill.getBillstatus()) || 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbill.getBillstatus())) && 
					lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) ) {
				//当前工作日期减复利记至日期得到计复利天数
				int nDays = CalendarUtil.actualDaysBetween(lnsbill.getIntedate(),ctx.getTranDate());
				if(nDays <= 0) //复利已记至当日
					continue;
				
				BigDecimal intDec = new BigDecimal(nDays);
				//取账单余额  从la中取得利率
				intDec = (intDec.multiply(la.getRateAgreement().getCompoundRate().getDayRate())
						.multiply(new BigDecimal(lnsbill.getBillbal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
				
				FabAmount cBillBal = new FabAmount(intDec.doubleValue());
				if(cBillBal.isZero()) //计算复利金额为零
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_CINT);
				tblLnsbill.setPeriod(lnsbill.getPeriod());
				tblLnsbill.setBillamt(cBillBal.getVal());
				tblLnsbill.setBillbal(cBillBal.getVal());
				tblLnsbill.setPrinbal(lnsbill.getBillbal());
				tblLnsbill.setBillrate(la.getRateAgreement().getCompoundRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsbill.getIntedate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsbill.getRepayway());
				tblLnsbill.setCcy(lnsbill.getCcy());
				tblLnsbill.setDertrandate(lnsbill.getTrandate());
				tblLnsbill.setDerserseqno(lnsbill.getSerseqno());
				tblLnsbill.setDertxseq(lnsbill.getTxseq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsbill.getIntedate());
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
	
}

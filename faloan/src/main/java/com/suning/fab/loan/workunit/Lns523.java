/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Lns502.java
 * Author:   16071579
 * Date:     2017年5月24日 上午11:02:07
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsAmortizeplan;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnsprovision;
import com.suning.fab.loan.domain.TblLnsprovisiondtl;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：费用摊销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns523 extends WorkUnit {

	String acctNo;			// 本金账号
	String repayDate;		// 摊销日期
	String amortizeType;	// 摊销类型
	FabAmount amortizAmt;	// 本次摊销金额
	FabAmount taxAmt;		// 本次摊销税金
	FabAmount dAmortizAmt;	// 已摊销金额--用于更新摊销计划表以及利息计提登记薄
	FabAmount dTaxAmt;		// 已摊销税金--用于更新摊销计划表以及利息计提登记薄
	String status;			// 摊销状态 "RUNNING"，"CLOSE", "CANCEL"
	
	@Autowired 
	LoanEventOperateProvider eventProvider;
	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		if (VarChecker.isEmpty(repayDate)) {
			throw new FabException("LNS005");
		}
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("brc", ctx.getBrc());
		param.put("acctno", acctNo);
		param.put("amortizetype", amortizeType);
		LnsAmortizeplan lnsamortizeplan;
		
		try {
			lnsamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsamortizeplan", param, 
					LnsAmortizeplan.class);
		} catch(FabSqlException e) {
			throw new FabException(e, "SPS102", "CUSTOMIZE.query_lnsamortizeplan");
		}
		
		if (null == lnsamortizeplan) {
			throw new FabException("LNS021");
		}

		/** 核销的贷款还款时，允许摊销  **/
		if (!VarChecker.asList("471007","471008","471009","472006").contains(ctx.getTranCode())&&
				!VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING).contains(lnsamortizeplan.getStatus())) {
			throw new FabException("LNS030");
		}

		FabAmount totalAmt = new FabAmount(lnsamortizeplan.getTotalamt());			// 摊销总金额
		FabAmount totalTaxAmt = new FabAmount(lnsamortizeplan.getTotaltaxamt());	// 摊销总税金
		FabAmount amortizedAmt = new FabAmount(lnsamortizeplan.getAmortizeamt());	// 上次已摊销金额
		FabAmount taxedAmt = new FabAmount(lnsamortizeplan.getAmortizetax());		// 上次已摊销税金
		// 还款与核销的时候   摊销结束不报错  用摊销总金额 = 上次已摊销金额判断是否摊销结束
		if(amortizedAmt.equals(totalAmt))
			return;
		if (!CalendarUtil.before(repayDate, lnsamortizeplan.getEnddate()) || 
				!new FabAmount(lnsamortizeplan.getContractbal()).isPositive()){
			//如果摊销当前日期大于合同结束日期，则 摊销金额 = 摊销总金额 - 已摊销金额，摊销税金 = 摊销总税金 - 已摊销税金
			setAmortizAmt((FabAmount)totalAmt.sub(amortizedAmt));
			setTaxAmt((FabAmount)totalTaxAmt.sub(taxedAmt));
			//setdAmortizAmt(totalAmt);	//将已摊销金额设为摊销总金额
			//setdTaxAmt(totalTaxAmt);	//将已摊销税金设为摊销总税金
			setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);//摊销结束
		} else {
			//本次摊销天数: 当前日期 - 合同起始日期）
			int nAmortizDays = CalendarUtil.actualDaysBetween(lnsamortizeplan.getBegindate(), repayDate);
			//合同总天数：合同结束日期-合同起始日期
			int nTotalDays = CalendarUtil.actualDaysBetween(lnsamortizeplan.getBegindate(), lnsamortizeplan.getEnddate());
			
			if (nAmortizDays < 0 || nTotalDays < 0) {
				return ;//日期有误,不做摊销处理，直接返回
			}
			
			//当前日期到合同起始日期期间的摊销金额,赋值给已摊销金额
			//Double dAmortizAmt = (Double.valueOf(nAmortizDays)/nTotalDays)*lnsamortizeplan.getTotalamt()
			setdAmortizAmt(new FabAmount((Double.valueOf(nAmortizDays)/nTotalDays)*lnsamortizeplan.getTotalamt()));
			//当前日期到和容起始日期期间的摊销税金，赋值给已摊销税金
			//Double dTaxAmt = dAmortizAmt*lnsamortizeplan.getTaxrate()
			
			//因为孙总给的摊销总金额是含税的，所以需要将摊销总金额除以（1+利率）再乘以利率才是税金
			BigDecimal rate = new BigDecimal(lnsamortizeplan.getTaxrate());
			BigDecimal ratefactor = rate.add(new BigDecimal(1));
			setdTaxAmt(new FabAmount((BigDecimal.valueOf(dAmortizAmt.getVal()).divide(ratefactor, 9, BigDecimal.ROUND_HALF_UP)
					.multiply(rate)).doubleValue()));
			/*BigDecimal dTaxAmt = BigDecimal.valueOf(dAmortizAmt.getVal()).divide(ratefactor, 9, BigDecimal.ROUND_HALF_UP)
					.multiply(rate)*/
			
			//本此摊销金额(已摊销金额-上次摊销金额)， 本次摊销税金(已摊销税金-上次摊销税金)
			setAmortizAmt((FabAmount)(dAmortizAmt.sub(amortizedAmt)));
			setTaxAmt((FabAmount)(dTaxAmt.sub(taxedAmt)));
			setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);//摊销中
		}
		
		//判断摊销金额如果为零或负数，则不抛事件直接返回
		if (!amortizAmt.isPositive()) {
			return ;
		}
		
		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsamortizeplan.getLoanstat().trim())) {
			setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);//摊销结束
		}

		//更新表中已摊销金额，已摊销税金，最后摊销日期
		param.clear();
		param.put("brc", ctx.getBrc());
		param.put("acctno", acctNo);
		param.put("amortizetype", amortizeType);
		param.put("lastdate", repayDate);
		param.put("amortizamt", amortizAmt.getVal());
		param.put("taxamt", taxAmt.getVal());
		param.put("status", status);
		
		LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		//子机构号
		String childBrc = null;
		for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
			if(Arrays.asList(ConstantDeclare.FEEREPAYWAY.ADVDEDUCT).contains(lnsfeeinfo.getRepayway()) ){
				if(   (ConstantDeclare.BILLTYPE.BILLTYPE_RBBF.equals(lnsfeeinfo.getFeetype()) && 
					   ConstantDeclare.AMORTIZETYPE.AMORTIZERBBF.equals(amortizeType)) 
					 || 
					  (ConstantDeclare.BILLTYPE.BILLTYPE_GDBF.equals(lnsfeeinfo.getFeetype()) && 
					   ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF.equals(amortizeType)) 
					 ||
					  (      !VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_RBBF,ConstantDeclare.BILLTYPE.BILLTYPE_GDBF).contains(lnsfeeinfo.getFeetype())   && 
					   ConstantDeclare.AMORTIZETYPE.AMORTIZEFEE.equals(amortizeType)))
					childBrc = lnsfeeinfo.getFeebrc();
			}
		}
		//登记利息计提摊销登记薄 lnsprovisionreg 
		TblLnsprovisiondtl lnsprovisionreg = new TblLnsprovisiondtl();
		lnsprovisionreg.setTrandate(Date.valueOf(repayDate));
		lnsprovisionreg.setSerseqno(ctx.getSerSeqNo());
		lnsprovisionreg.setTxnseq(1);
		lnsprovisionreg.setBrc(ctx.getBrc());
		lnsprovisionreg.setAcctno(acctNo);
		lnsprovisionreg.setPeriod(loanAgreement.getContract().getCurrPrinPeriod());
		lnsprovisionreg.setListno(lnsamortizeplan.getPeriod()+1);
		if( ConstantDeclare.AMORTIZETYPE.AMORTIZERBBF.equals(amortizeType) ){
			lnsprovisionreg.setBilltype( ConstantDeclare.BILLTYPE.BILLTYPE_RBBF );//人保保费
			lnsprovisionreg.setIntertype( ConstantDeclare.INTERTYPE.RBBFAMOR );//RBBFAMOR

		}
		else if( ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF.equals(amortizeType) ){
			lnsprovisionreg.setBilltype( ConstantDeclare.BILLTYPE.BILLTYPE_GDBF );//光大保费
			lnsprovisionreg.setIntertype( ConstantDeclare.INTERTYPE.GDBFAMOR );//GDBFAMOR

		}
		else{
			lnsprovisionreg.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_FEEA);//预扣担保费
			lnsprovisionreg.setIntertype(ConstantDeclare.INTERTYPE.AMORTIZE);//"AMORTIZE");//摊销
		}
		lnsprovisionreg.setCcy(new FabCurrency().getCcy());
		lnsprovisionreg.setTotalinterest((amortizedAmt.add(amortizAmt)).getVal());//已摊销金额+本次摊销金额
		lnsprovisionreg.setTotaltax((taxedAmt.add(taxAmt)).getVal());//已摊销税金+本次摊销税金
		lnsprovisionreg.setTaxrate(lnsamortizeplan.getTaxrate());//税率
		lnsprovisionreg.setInterest(amortizAmt.getVal());	//本次摊销金额
		lnsprovisionreg.setTax(taxAmt.getVal());	//本次摊销税金

		lnsprovisionreg.setBegindate(Date.valueOf(lnsamortizeplan.getLastdate()));//摊销表中最后摊销日期为本表起始日期
		lnsprovisionreg.setEnddate(Date.valueOf(repayDate));//本次调用的日期为结束日期
		lnsprovisionreg.setInterflag(ConstantDeclare.INTERFLAG.POSITIVE);//"POSITIVE");//正向
		lnsprovisionreg.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
		lnsprovisionreg.setChildbrc(childBrc);
		lnsprovisionreg.setReserv1(null);
		lnsprovisionreg.setReserv2(null);
		LoanProvisionProvider.exchangeProvision(lnsprovisionreg);
		try {
			DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisionreg);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "lnsprovisionreg");
		}
		//摊销
		// 所有的计提，摊销  都不记税金表
		//AccountingModeChange.saveProvisionTax(ctx, acctNo, lnsprovisionreg.getInterest(), lnsprovisionreg.getTax(), "TX",
		//		lnsprovisionreg.getInterflag(), ConstantDeclare.BILLTYPE.BILLTYPE_NINT);

		LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
											ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

		List<FabAmount> amtList = new ArrayList<FabAmount>();
		amtList.add(taxAmt);

		//累计结转费用
		String amortizeformula = lnsamortizeplan.getAmortizeformula();
		
		//预扣融担费计提DBJT，人保保费不计提，光大保费计提GDJT
		if( ConstantDeclare.AMORTIZETYPE.AMORTIZEFEE.equals(amortizeType) )
			eventProvider.createEvent(ConstantDeclare.EVENT.ACCRUEDFEE, amortizAmt, acctinfo, null,
					loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DBJT, ctx, amtList,
					repayDate, ctx.getSerSeqNo(), 1, childBrc);
		//光大项目担保费结转:每个结息日之间，将该时间区间的融担费汇总金额
		else if( ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF.equals(amortizeType) )
		{
			eventProvider.createEvent(ConstantDeclare.EVENT.ACCRUEDFEE, amortizAmt, acctinfo, null,
					loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDJT, ctx, amtList,
					repayDate, ctx.getSerSeqNo(), 1, childBrc);

			LnsBillStatistics lnsBillStatistics = null;
			lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, repayDate, ctx,lnsBillStatistics);

			List<LnsBill> billList = new ArrayList<LnsBill>();
			//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
			billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
			//历史账单
			billList.addAll(lnsBillStatistics.getHisBillList());
			//历史未结清账单结息账单
			billList.addAll(lnsBillStatistics.getHisSetIntBillList());
			//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
			billList.addAll(lnsBillStatistics.getBillInfoList());
			//未来期账单：从还款日到合同到期日之间的本金和利息账单
			billList.addAll(lnsBillStatistics.getFutureBillInfoList());
			//获取呆滞呆账期间新罚息复利账单list
			billList.addAll(lnsBillStatistics.getCdbillList());

			FabAmount gdjzAmt = new FabAmount(0.00);
			for(LnsBill bill : billList){
				if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
					ctx.getTranDate().equals( bill.getEndDate())  )
				{

					Map<String, Object> bparam = new HashMap<String, Object>();
					bparam.put("receiptno", acctNo);
					bparam.put("brc", ctx.getBrc());
					List<TblLnsprovisiondtl> lnsprovisionregs;
					try {
						lnsprovisionregs = DbAccessUtil.queryForList("Lnsprovisiondtl.select_all", bparam, TblLnsprovisiondtl.class);
					} catch (FabSqlException e) {
						throw new FabException(e, "SPS103", "lnsprovisionreg");
					}
					if (null != lnsprovisionregs) {
						for( TblLnsprovisiondtl provisionreg : lnsprovisionregs )
						{
							if( ConstantDeclare.BILLTYPE.BILLTYPE_GDBF.equals(provisionreg.getBilltype())  &&
								CalendarUtil.after(provisionreg.getTrandate().toString(), bill.getStartDate())  &&
								CalendarUtil.beforeAlsoEqual(provisionreg.getTrandate().toString(), bill.getEndDate()))
							{
								gdjzAmt.selfAdd(provisionreg.getInterest());
							}

						}
					}
				}
			}

			if( gdjzAmt.isPositive() )
				eventProvider.createEvent(ConstantDeclare.EVENT.FEESETLEMT, gdjzAmt, acctinfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDJZ, ctx, amtList,
						repayDate, ctx.getSerSeqNo(), 1, childBrc);
			
			//每次结转累计
			if(VarChecker.isEmpty(amortizeformula.trim()))
				amortizeformula = "0.00";
			amortizeformula = new FabAmount(Double.valueOf(amortizeformula)).selfAdd(gdjzAmt).toString();
			param.put("amortizeformula", amortizeformula);
		}
		
		
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsamortizeplan");
		}
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
	 * @return the repayDate
	 */
	public String getRepayDate() {
		return repayDate;
	}

	/**
	 * @param repayDate the repayDate to set
	 */
	public void setRepayDate(String repayDate) {
		this.repayDate = repayDate;
	}

	/**
	 * @return the amortizAmt
	 */
	public FabAmount getAmortizAmt() {
		return amortizAmt;
	}

	/**
	 * @param amortizAmt the amortizAmt to set
	 */
	public void setAmortizAmt(FabAmount amortizAmt) {
		this.amortizAmt = amortizAmt;
	}

	/**
	 * @return the taxAmt
	 */
	public FabAmount getTaxAmt() {
		return taxAmt;
	}

	/**
	 * @param taxAmt the taxAmt to set
	 */
	public void setTaxAmt(FabAmount taxAmt) {
		this.taxAmt = taxAmt;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the dAmortizAmt
	 */
	public FabAmount getdAmortizAmt() {
		return dAmortizAmt;
	}

	/**
	 * @param dAmortizAmt the dAmortizAmt to set
	 */
	public void setdAmortizAmt(FabAmount dAmortizAmt) {
		this.dAmortizAmt = dAmortizAmt;
	}

	/**
	 * @return the dTaxAmt
	 */
	public FabAmount getdTaxAmt() {
		return dTaxAmt;
	}

	/**
	 * @param dTaxAmt the dTaxAmt to set
	 */
	public void setdTaxAmt(FabAmount dTaxAmt) {
		this.dTaxAmt = dTaxAmt;
	}

	/**
	 * @return the amortizeType
	 */
	public String getAmortizeType() {
		return amortizeType;
	}

	/**
	 * @param amortizeType the amortizeType to set
	 */
	public void setAmortizeType(String amortizeType) {
		this.amortizeType = amortizeType;
	}

	/**
	 * @return the eventProvider
	 */
	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}

	/**
	 * @param eventProvider the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}
	
}

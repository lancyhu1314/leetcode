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

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsAmortizeplan;
import com.suning.fab.loan.domain.TblLnsprovisiondtl;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：摊销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns502 extends WorkUnit {

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
		param.put("amortizetype", ConstantDeclare.AMORTIZETYPE.AMORTIZEINT);
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
		if (!VarChecker.asList("471007","471008","471009","472006","479003").contains(ctx.getTranCode())&&
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
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsamortizeplan");
		}
		
		//登记利息计提摊销登记薄 lnsprovisionreg 
		TblLnsprovisiondtl lnsprovisiondtl = new TblLnsprovisiondtl();
		lnsprovisiondtl.setTrandate(Date.valueOf(repayDate));
		lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
		lnsprovisiondtl.setBrc(ctx.getBrc());
		lnsprovisiondtl.setAcctno(acctNo);
		lnsprovisiondtl.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
		lnsprovisiondtl.setListno(lnsamortizeplan.getPeriod()+1);
		lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);//设置为利息
		lnsprovisiondtl.setCcy(new FabCurrency().getCcy());
		lnsprovisiondtl.setTotalinterest((amortizedAmt.add(amortizAmt)).getVal());//已摊销金额+本次摊销金额
		lnsprovisiondtl.setTotaltax((taxedAmt.add(taxAmt)).getVal());//已摊销税金+本次摊销税金
		lnsprovisiondtl.setTaxrate(lnsamortizeplan.getTaxrate());//税率
		lnsprovisiondtl.setInterest(amortizAmt.getVal());	//本次摊销金额
		lnsprovisiondtl.setTax(taxAmt.getVal());	//本次摊销税金
		lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.AMORTIZE);//"AMORTIZE");//摊销
		lnsprovisiondtl.setBegindate(Date.valueOf(lnsamortizeplan.getLastdate()));//摊销表中最后摊销日期为本表起始日期
		lnsprovisiondtl.setEnddate(Date.valueOf(repayDate));//本次调用的日期为结束日期
		lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.POSITIVE);//"POSITIVE");//正向
		lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
		lnsprovisiondtl.setReserv1(null);
		lnsprovisiondtl.setReserv2(null);
		LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
		try {
			DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "lnsprovisiondtl");
		}
		//摊销
		// 所有的计提，摊销  都不记税金表
		//AccountingModeChange.saveProvisionTax(ctx, acctNo, lnsprovisionreg.getInterest(), lnsprovisionreg.getTax(), "TX",
		//		lnsprovisionreg.getInterflag(), ConstantDeclare.BILLTYPE.BILLTYPE_NINT);

		LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
											ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());


		List<FabAmount> amtList = new ArrayList<FabAmount>();
		amtList.add(taxAmt);


		//写事件
		eventProvider.createEvent(ConstantDeclare.EVENT.LNAMORTIZE, amortizAmt, acctinfo, null, 
							loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.LXTX, ctx, amtList,
							repayDate, ctx.getSerSeqNo(), 1);
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

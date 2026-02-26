/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Lns205.java
 * Author:   16071579
 * Date:     2017年5月25日 下午4:00:57
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

import com.suning.fab.loan.domain.TblLnsprovision;
import com.suning.fab.loan.domain.TblLnsprovisiondtl;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.MapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnseventreg;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：非标利息计提冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns232 extends WorkUnit {
	
	String errDate;		// 错误日期
	String errSerSeq;	// 错误流水号
	String acctNo;
	String receiptNo;

	@Autowired 
	LoanEventOperateProvider eventProvider;
	LoanAgreement loanAgreement;
	private Integer txseqno = 0;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		Map<String, Object> param = new HashMap<>();

		//此处可以拿到借据号，难道借据号后查询利息计提摊销登记簿，看看有没有记录，有这说明利息计提过，没有则无需操作，直接return
//		TblLnsprovisionreg lnsprovisionreg;
		TblLnsprovision lnsprovision;
		param.put("acctno", getAcctNo());
		param.put("brc", ctx.getBrc());
		if(loanAgreement==null){
			loanAgreement=LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
		}
		try {
//			lnsprovisionreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_provision", param,
//					TblLnsprovisionreg.class);
			lnsprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,getAcctNo(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.PROVISION, loanAgreement);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsprovision");
		}
		
		if (!lnsprovision.isExist()) {//没有利息计提数据，则不用处理该子交易，直接返回即可
			return ;
		}
		
		//写利息计提摊销登记簿(lnsprovisionreg)
//		TblLnsprovisionreg lnsprovisionregOff = new TblLnsprovisionreg();
		TblLnsprovisiondtl lnsprovisiondtl =  new TblLnsprovisiondtl();
		lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
		lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
		lnsprovisiondtl.setTxnseq(1);
		lnsprovisiondtl.setBrc(ctx.getBrc());
//		lnsprovisionregOff.setTeller(ctx.getTeller());
//		lnsprovisionregOff.setReceiptno(getAcctNo());
		lnsprovisiondtl.setAcctno(getAcctNo());
//		lnsprovisionregOff.setPeriod(lnsprovisionreg.getPeriod() + 1);//期数
		lnsprovisiondtl.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
		lnsprovisiondtl.setListno(lnsprovision.getTotallist()+1);
		lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
		lnsprovisiondtl.setCcy(lnsprovision.getCcy());
		lnsprovisiondtl.setTotalinterest(lnsprovision.getTotalinterest());
		lnsprovisiondtl.setTotaltax(lnsprovision.getTotaltax());
		lnsprovisiondtl.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
		lnsprovisiondtl.setInterest(lnsprovision.getTotalinterest());	//本次利息计提金额为 已计提总额
		lnsprovisiondtl.setTax(lnsprovision.getTotaltax());	//本次计提税金为已计提税金总额，冲销掉原来的发生额
		lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.PROVISION);
		lnsprovisiondtl.setBegindate(lnsprovision.getLastenddate());
		lnsprovisiondtl.setEnddate(Date.valueOf(ctx.getTranDate()));
		lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.NEGATIVE);//反向
//		lnsprovisionregOff.setSendflag(ConstantDeclare.SENDFLAG.PENDIND);
//		lnsprovisionregOff.setSendnum(0);
		lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
		//类型转换
		LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
		try {
			DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "lnsprovisiondtl");
		}
		//更新总表
		MapperUtil.map(lnsprovisiondtl, lnsprovision, "map500_01");
		try {
			if(lnsprovision.isSaveFlag()){
				DbAccessUtil.execute("Lnsprovision.updateByUk", lnsprovision);
			}else{
				if(!"".equals(lnsprovisiondtl.getInterflag())){
					DbAccessUtil.execute("Lnsprovision.insert", lnsprovision);
				}
			}
		}catch (FabSqlException e){
			LoggerUtil.info("插入计提明细总表异常：",e);
			throw new FabException(e, "SPS102", "Lnsprovision");
		}
		LnsAcctInfo acctinfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		//LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(lnseventreg.getReceiptno(), ctx);
		if(loanAgreement == null||null==loanAgreement.getContract()||null==loanAgreement.getContract().getReceiptNo())
			loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		List<FabAmount> amtList = new ArrayList<FabAmount>();
		amtList.add(new FabAmount(lnsprovision.getTotaltax()));
		FabAmount theInt = new FabAmount();
		FabAmount theTax = new FabAmount();
		//将trandate 修改为lastenddate
		if(CalendarUtil.equalDate(lnsprovision.getLastenddate().toString() ,ctx.getTranDate()) ){
			if(ConstantDeclare.INTERFLAG.POSITIVE.equals(lnsprovision.getInterflag())){
//				theInt.selfAdd(lnsprovisionreg.getInterest());
//				theTax.selfAdd(lnsprovisionreg.getTax());
				theInt.selfAdd(lnsprovision.getLastinterest());
 				theTax.selfAdd(lnsprovision.getLasttax());
			}else{
//				theInt.selfSub(lnsprovisionreg.getInterest());
//				theTax.selfSub(lnsprovisionreg.getTax());
				theInt.selfSub(lnsprovision.getLastinterest());
 				theTax.selfSub(lnsprovision.getLasttax());
			}

		}
		//FundInvest fundInvest = new FundInvest();
		//写利息计提冲销事件
		eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFINT, new FabAmount(lnsprovision.getTotalinterest()),
				acctinfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.LXCX, ctx, amtList);
		AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, receiptNo, ConstantDeclare.EVENT.WRITOFFINT, ConstantDeclare.BRIEFCODE.LXCX,
				lnsprovision.getTotalinterest() , "", loanAgreement.getFundInvest(),lnsprovision.getTotaltax(),theInt.getVal() , theTax.getVal());

	}

	/**
	 * @return the errDate
	 */
	public String getErrDate() {
		return errDate;
	}

	/**
	 * @param errDate the errDate to set
	 */
	public void setErrDate(String errDate) {
		this.errDate = errDate;
	}

	/**
	 * @return the errSerSeq
	 */
	public String getErrSerSeq() {
		return errSerSeq;
	}

	/**
	 * @param errSerSeq the errSerSeq to set
	 */
	public void setErrSerSeq(String errSerSeq) {
		this.errSerSeq = errSerSeq;
	}

	public String getAcctNo() {
		return acctNo;
	}

	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}

	public Integer getTxseqno() {
		return txseqno;
	}

	public void setTxseqno(Integer txseqno) {
		this.txseqno = txseqno;
	}

	public String getReceiptNo() {
		return receiptNo;
	}

	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}
}

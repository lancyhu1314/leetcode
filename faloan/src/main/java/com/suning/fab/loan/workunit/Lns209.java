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

import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.MapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：利息计提冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns209 extends WorkUnit {
	
	String errDate;		// 错误日期
	String errSerSeq;	// 错误流水号
	
	@Autowired 
	LoanEventOperateProvider eventProvider;
	private Integer txseqno = 0;
	private LoanAgreement loanAgreement;
	private TblLnsinterface lnsinterface;
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator suber;
	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		//查询幂等表
		if(lnsinterface == null)
		{
			try {
				lnsinterface = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsinterface_208", param, TblLnsinterface.class);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS103", "lnsinterface");
			}
		}
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(lnsinterface.getAcctno(), ctx,loanAgreement);

		//此处可以拿到借据号，难道借据号后查询利息计提摊销登记簿，看看有没有记录，有这说明利息计提过，没有则无需操作，直接return
//		TblLnsprovisionreg lnsprovisionreg;
		TblLnsprovision lnsprovision;
		param.clear();
		param.put("acctno", lnsinterface.getAcctno());
		param.put("brc", ctx.getBrc());
		try {
//			lnsprovisionreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_provision", param,
//					TblLnsprovisionreg.class);
			lnsprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,lnsinterface.getAcctno(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.PROVISION, loanAgreement);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsprovision");
		}
		
		if (lnsprovision.isExist()) {//没有利息计提数据，则不用处理该子交易，直接返回即可
			//写利息计提摊销登记簿(lnsprovisionreg)
//			TblLnsprovisionreg lnsprovisionregOff = new TblLnsprovisionreg();
			TblLnsprovisiondtl lnsprovisiondtl = new TblLnsprovisiondtl();
			lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
			lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
			lnsprovisiondtl.setTxnseq(1);
			lnsprovisiondtl.setBrc(ctx.getBrc());
//			lnsprovisionregOff.setTeller(ctx.getTeller());
//			lnsprovisionregOff.setReceiptno(lnsprovisionreg.getReceiptno());
			lnsprovisiondtl.setAcctno(lnsprovision.getAcctno());
//			lnsprovisionregOff.setPeriod(lnsprovisionreg.getPeriod() + 1);//期数
			lnsprovisiondtl.setListno(lnsprovision.getTotallist()+1);
			lnsprovisiondtl.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
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
			lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);

//			try {
//				DbAccessUtil.execute("Lnsprovisionreg.insert", lnsprovisionregOff);
//			} catch (FabSqlException e) {
//				throw new FabException(e, "SPS100", "lnsprovisionreg");
//			}
			LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
			try {
				DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS100", "lnsprovisiondtl");
			}

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

			LnsAcctInfo acctinfo = new LnsAcctInfo(lnsinterface.getAcctno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

			List<FabAmount> amtList = new ArrayList<>();
			amtList.add(new FabAmount(lnsprovision.getTotaltax()));

			//写利息计提冲销事件
			eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFINT, new FabAmount(lnsprovision.getTotalinterest()),
					acctinfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.LXCX, ctx, amtList, loanAgreement.getCustomer().getMerchantNo(), loanAgreement.getBasicExtension().getDebtCompany());
			FabAmount theInt = new FabAmount();
			FabAmount theTax = new FabAmount();
			if(CalendarUtil.equalDate(lnsprovision.getLastenddate().toString() ,ctx.getTranDate()) ){
				if(ConstantDeclare.INTERFLAG.POSITIVE.equals(lnsprovision.getInterflag())){
//					theInt.selfAdd(lnsprovisionreg.getInterest());
//					theTax.selfAdd(lnsprovisionreg.getTax());
			        theInt.selfAdd(lnsprovision.getLastinterest());
					theTax.selfAdd(lnsprovision.getLasttax());
				}else{
//					theInt.selfSub(lnsprovisionreg.getInterest());
//					theTax.selfSub(lnsprovisionreg.getTax());
				    theInt.selfSub(lnsprovision.getLastinterest());
					theTax.selfSub(lnsprovision.getLasttax());
				}

			}

			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, lnsinterface.getAcctno(), ConstantDeclare.EVENT.WRITOFFINT, ConstantDeclare.BRIEFCODE.LXCX,
					lnsprovision.getTotalinterest() , "", loanAgreement.getFundInvest(),lnsprovision.getTotaltax(),theInt.getVal() , theTax.getVal());
		}

		//罚息计提冲销
		//取计提登记簿信息
		TblLnspenintprovreg penintprovisionDint = null;
		param.put("receiptno", lnsinterface.getAcctno());
		param.put("billtype", "DINT");
		try {
			penintprovisionDint = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "罚息计提登记簿Lnspenintprovreg");
		}
		LnsAcctInfo dintAcctInfo = new LnsAcctInfo(lnsinterface.getAcctno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_DINT,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		if(penintprovisionDint != null && new FabAmount(penintprovisionDint.getTotalinterest().doubleValue()).isPositive()) {
			List<FabAmount> taxs = new ArrayList<>();
			taxs.add(new FabAmount(penintprovisionDint.getTotaltax().doubleValue()));

			suber.operate(dintAcctInfo, null, new FabAmount(penintprovisionDint.getTotalinterest().doubleValue()), loanAgreement.getFundInvest(),
					"", ctx);

			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, lnsinterface.getAcctno(), ConstantDeclare.EVENT.REDUDEFINT, ConstantDeclare.BRIEFCODE.FXCX,
					penintprovisionDint.getTotalinterest().doubleValue(), "", loanAgreement.getFundInvest(), penintprovisionDint.getTotaltax().doubleValue(), 0.00, 0.00);
			eventProvider.createEvent(ConstantDeclare.EVENT.REDUDEFINT, new FabAmount(penintprovisionDint.getTotalinterest().doubleValue()), dintAcctInfo, null, loanAgreement.getFundInvest(),
					ConstantDeclare.BRIEFCODE.FXCX, ctx, taxs, loanAgreement.getCustomer().getMerchantNo(), loanAgreement.getBasicExtension().getDebtCompany());
		}
		//取计提登记簿信息
		TblLnspenintprovreg penintprovisionCint = null;
		param.put("billtype", "CINT");
		try {
			penintprovisionCint = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
		}catch (FabSqlException e){
			throw new FabException(e, "LNS103", "罚息计提登记簿Lnspenintprovreg");
		}
		if(penintprovisionCint != null && new FabAmount(penintprovisionCint.getTotalinterest().doubleValue()).isPositive())  {
			List<FabAmount> taxs = new ArrayList<>();
			taxs.add(new FabAmount(penintprovisionCint.getTotaltax().doubleValue()));


			suber.operate(dintAcctInfo, null, new FabAmount(penintprovisionCint.getTotalinterest().doubleValue()), loanAgreement.getFundInvest(),
					"", ctx);
			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, lnsinterface.getAcctno(), ConstantDeclare.EVENT.REDUDEFINT, ConstantDeclare.BRIEFCODE.FXCX,
					penintprovisionCint.getTotalinterest().doubleValue(), "", loanAgreement.getFundInvest(), penintprovisionCint.getTotaltax().doubleValue(), 0.00, 0.00);
			eventProvider.createEvent(ConstantDeclare.EVENT.REDUDEFINT, new FabAmount(penintprovisionCint.getTotalinterest().doubleValue()), dintAcctInfo, null, loanAgreement.getFundInvest(),
					ConstantDeclare.BRIEFCODE.FXCX, ctx, taxs, loanAgreement.getCustomer().getMerchantNo(), loanAgreement.getBasicExtension().getDebtCompany());
		}
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
	/**
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}
	/**
	 * @param loanAgreement the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}/**
	 * @return the txseqno
	 */
	public Integer getTxseqno() {
		return txseqno;
	}
	/**
	 * @param txseqno the txseqno to set
	 */
	public void setTxseqno(Integer txseqno) {
		this.txseqno = txseqno;
	}
	/**
	 * @return the lnsinterface
	 */
	public TblLnsinterface getLnsinterface() {
		return lnsinterface;
	}
	/**
	 * @param lnsinterface the lnsinterface to set
	 */
	public void setLnsinterface(TblLnsinterface lnsinterface) {
		this.lnsinterface = lnsinterface;
	}
}

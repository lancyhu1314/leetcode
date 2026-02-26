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

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.utils.MapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：摊销冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns219 extends WorkUnit {
	
	String errDate;		// 错误日期
	String errSerSeq;	// 错误流水号
	
	@Autowired 
	LoanEventOperateProvider eventProvider;
	private Integer txseqno = 0;
	private LoanAgreement loanAgreement;
	private TblLnsinterface tblLnsinterface;

	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		//判断是否满足冲销条件，如果不满足则直接return
		checkCondition();
		
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("brc", ctx.getBrc());
		
		List<TblLnsamortizeplan> lnsamortizeplans;
		try {//根据核心流水号取出符合条件的记录，摊销计划表中的核心流水号和开户放款的是一致的
			/**
			 * SELECT * FROM LNSAMORTIZEPLAN 
			 *		   WHERE ACCTNO = :acctno 
			 *			<#if brc?exists && brc != "">
			 *			 AND BRC = :brc
			 *			</#if>
			 * 20190529|14050183
			 */
			lnsamortizeplans = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsamortizeplan_off", param, 
						TblLnsamortizeplan.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsamortizeplan");
		}
		
		/*如果摊销计划表中无此数据，则意味着该帐号不需要摊销，直接返回，进行下一步*/
		if (null == lnsamortizeplans || 0 == lnsamortizeplans.size()) {
			return ;
		}
		
		for( TblLnsamortizeplan lnsamortizeplan : lnsamortizeplans )
		{
			//更新摊销计划表中的摊销金额， 登记摊销计提登记薄，登记事件
			FabAmount amortizedAmt = new FabAmount(lnsamortizeplan.getAmortizeamt());	//已摊销金额
			FabAmount taxedAmt = new FabAmount(lnsamortizeplan.getAmortizetax());		//已摊销税金
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnsamortizeplan.getAcctno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			if(loanAgreement==null)
			 	loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(lnsamortizeplan.getAcctno(), ctx);
			
			if (amortizedAmt.isPositive()) {//如果已摊销金额不为零，则需要处理利息计提摊销登记薄以及事件登记薄，
										//并将摊销登记表中的已摊销金额，已摊销税金，最后摊销日期进行更新
				FabAmount theInt = new FabAmount();
				FabAmount theTax = new FabAmount();
				//存储当天摊销的金额和税金
				if(CalendarUtil.equalDate(lnsamortizeplan.getLastdate(), ctx.getTranDate())){
					param.put("receiptno", lnsamortizeplan.getAcctno());
					if( ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF.equals(lnsamortizeplan.getAmortizetype()) )
						param.put("intertype", ConstantDeclare.INTERTYPE.GDBFAMOR);
					else
						param.put("intertype", ConstantDeclare.INTERTYPE.AMORTIZE);

				//	TblLnsprovisionreg lnsprovisionreg;
					TblLnsprovision lnsprovision;
					try {
//						lnsprovisionreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", param,
//								TblLnsprovisionreg.class);
						//判断账本类型
						String billtype="";
						
						if( ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF.equals(lnsamortizeplan.getAmortizetype()) ){
							billtype=ConstantDeclare.BILLTYPE.BILLTYPE_GDBF;
						}
						else if( ConstantDeclare.AMORTIZETYPE.AMORTIZEINT.equals(lnsamortizeplan.getAmortizetype()) )
						{
							billtype=ConstantDeclare.BILLTYPE.BILLTYPE_NINT;
						}
						else if( ConstantDeclare.AMORTIZETYPE.AMORTIZEFEE.equals(lnsamortizeplan.getAmortizetype()) )
						{
							billtype=ConstantDeclare.BILLTYPE.BILLTYPE_SQFE;
						}
						lnsprovision= LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,lnsamortizeplan.getAcctno(),billtype,param.get("intertype")==null?"":param.get("intertype").toString(), loanAgreement);

					} catch (FabSqlException e) {
						throw new FabException(e, "SPS103", "lnsprovision");
					}
					
//					if( null == lnsprovision)
//						continue;
					if(!lnsprovision.isExist()){
						continue;
					}
					
					if(ConstantDeclare.INTERFLAG.POSITIVE.equals(lnsprovision.getInterflag())){
//						theInt.selfAdd(lnsprovisionreg.getInterest());
//						theTax.selfAdd(lnsprovisionreg.getTax());
					 	theInt.selfAdd(lnsprovision.getLastinterest());
 						theTax.selfAdd(lnsprovision.getLasttax());
					}else{
//						theInt.selfSub(lnsprovisionreg.getInterest());
//						theTax.selfSub(lnsprovisionreg.getTax());
						theInt.selfSub(lnsprovision.getLastinterest());
						theTax.selfSub(lnsprovision.getLasttax());
					}
				}


				//登记利息计提摊销登记薄，登记正反标志为反向TblLnsprovisionreg
//				TblLnsprovisionreg lnsprovisionreg = new TblLnsprovisionreg();
				TblLnsprovisiondtl lnsprovisiondtl = new TblLnsprovisiondtl();
				lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
				lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
				lnsprovisiondtl.setTxnseq(1);
				lnsprovisiondtl.setBrc(ctx.getBrc());
//				lnsprovisionreg.setTeller(ctx.getTeller());
//				lnsprovisionreg.setReceiptno(lnsamortizeplan.getAcctno());
				lnsprovisiondtl.setAcctno(lnsamortizeplan.getAcctno());
//				lnsprovisionreg.setPeriod(lnsamortizeplan.getPeriod()+1);
				lnsprovisiondtl.setListno(lnsamortizeplan.getPeriod()+1);
				lnsprovisiondtl.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
				String memoCode="";
				if( ConstantDeclare.AMORTIZETYPE.AMORTIZERBBF.equals(lnsamortizeplan.getAmortizetype()) ){
					lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_RBBF);//人保保费
					lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.RBBFAMOR);
					memoCode = ConstantDeclare.BRIEFCODE.GDCX;

				}
				else if( ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF.equals(lnsamortizeplan.getAmortizetype()) ){
					lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_GDBF);//光大保费
					lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.GDBFAMOR);
					memoCode = ConstantDeclare.BRIEFCODE.GDCX;
				}
				else if( ConstantDeclare.AMORTIZETYPE.AMORTIZEFEE.equals(lnsamortizeplan.getAmortizetype()) ){
					lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_SQFE);//摊销担保费
					lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.AMORTIZE);
					memoCode = ConstantDeclare.BRIEFCODE.DBCX;
				}
				
				lnsprovisiondtl.setCcy(new FabCurrency().getCcy());
				lnsprovisiondtl.setTotalinterest(amortizedAmt.getVal());	//已摊销总金额
				lnsprovisiondtl.setTotaltax(taxedAmt.getVal()); 			//已摊销总税金
				lnsprovisiondtl.setTaxrate(lnsamortizeplan.getTaxrate());//税率
				lnsprovisiondtl.setInterest(amortizedAmt.getVal());	//本次摊销金额为已摊销金额
				lnsprovisiondtl.setTax(taxedAmt.getVal());	//本次摊销税金为已摊销税金

				lnsprovisiondtl.setBegindate(Date.valueOf(lnsamortizeplan.getLastdate()));//摊销表中最后摊销日期为本表起始日期
				lnsprovisiondtl.setEnddate(Date.valueOf(ctx.getTranDate()));//本次调用的账务日期为结束日期
				lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.NEGATIVE);//反向
//				lnsprovisionreg.setSendflag(ConstantDeclare.SENDFLAG.PENDIND);
//				lnsprovisionreg.setSendnum(0);
				lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				lnsprovisiondtl.setReserv1(null);
				lnsprovisiondtl.setReserv2(null);
				LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
				try {
					DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsprovisiondtl");
				}



				List<FabAmount> amtList = new ArrayList<FabAmount>();
				amtList.add(taxedAmt);
				//登记摊销冲销事件， 将已摊销金额及已摊销税金进行反向处理
				
				if( "1".equals(lnsamortizeplan.getAmortizetype()))
				{
					eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZOFF, amortizedAmt, lnsAcctInfo, null, 
							loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.TXCX, ctx, amtList);
					
					AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, lnsamortizeplan.getAcctno(), ConstantDeclare.EVENT.AMORTIZOFF, ConstantDeclare.BRIEFCODE.TXCX,
							amortizedAmt.getVal() , "", loanAgreement.getFundInvest(),taxedAmt.getVal(),theInt.getVal() , theTax.getVal());

				}
					
				else
				{
					String childBrc = loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getFeebrc();

					eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFFEE, amortizedAmt, lnsAcctInfo, null, 
							loanAgreement.getFundInvest(), memoCode, ctx, amtList,childBrc);
					AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, lnsamortizeplan.getAcctno(), ConstantDeclare.EVENT.WRITOFFFEE, memoCode,
							amortizedAmt.getVal() , "", loanAgreement.getFundInvest(),taxedAmt.getVal(),theInt.getVal() , theTax.getVal());
					
					//光大结费冲销
					if(  !VarChecker.isEmpty(lnsamortizeplan.getAmortizeformula())  &&  new FabAmount(Double.valueOf(lnsamortizeplan.getAmortizeformula())).isPositive())
						eventProvider.createEvent(ConstantDeclare.EVENT.WFFEESETLE, new FabAmount(Double.valueOf(lnsamortizeplan.getAmortizeformula())), lnsAcctInfo, null, 
								loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDJC, ctx, childBrc);

				}
			}
			//如果摊销金额为零，则还未发生摊销，直接将摊销计划表中的status更改为CLOSE
			
			try {//更新摊销表，只要有数据，则都应该更新摊销计划表中的status为CLOSE,有金额产生时，则还应登记事件登记薄
				param.put("lastdate", ctx.getTranDate());
				DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan_off", param);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsamortizeplan");
			}
			
			
		}
		
		
		
	}
	
	public void checkCondition() throws FabException {

		TranCtx ctx = getTranctx();
		/**
		 * 1、登记幂等登记薄，根据serialno以及 trancode来判断是否幂等，
		 * 2、如果幂等，则去幂等登记薄中查询customName,acctNo,contractAmt三个字段对应的值，并返回给外围
		 * 3、如果不幂等，判断是否满足冲销条件(无逾期记录，且未还款)，满足则进行冲销，最后删除数据的借据号相关记录之后，
		 * 	     更新幂等登记薄中刚插入的记录的customName,acctNo,contractAmt三个字段的值，否则直接返回
		 **/
		TblLnsinterface lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setTrancode(ctx.getTranCode());
		lnsinterface.setSerialno(ctx.getSerialNo());
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());

		try {
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
		} catch (FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				LoggerUtil.info("交易幂等:serialNo[" + ctx.getSerialNo() + "]" + ", tranCode[" + ctx.getTranCode() + "]");
				throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
			} else {
				throw new FabException(e, "SPS100", "lnsinterface");
			}
		}

		Map<String, Object> param = new HashMap<String, Object>();
		//查询幂等表
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		if(tblLnsinterface == null)
		{
			try {
				/**
				 * SELECT * FROM LNSINTERFACE WHERE	SERSEQNO = :serseqno
			  	 *	   		 AND ACCDATE = :trandate 
				 *			 AND TRANCODE in ('473004','479000','473005','473007')
				 * 473004	开户放款
				 * 473005	非标开户放款
				 * 473007	房抵贷开户
				 * 479000	非标迁移
				 * 20190529|14050183
				 */
				tblLnsinterface = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsinterface_208", param, TblLnsinterface.class);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS103", "lnsinterface");
			}
		}

		if (null == tblLnsinterface) {
			throw new FabException("LNS003");
		}
		
		
		param.put("acctno", tblLnsinterface.getAcctno());
		param.put("brc", tblLnsinterface.getBrc());
		param.put("trancode", "470023");
		TblLnsinterface interfaceTZ = null;
		try {
			/**
			 * SELECT * FROM LNSINTERFACE WHERE	ACCTNO = :acctno
		  	 *	   		 AND BRC = :brc 
			 *			 AND TRANCODE in ('470023')
			 * 470023	还款结果通知
			 */
			interfaceTZ = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsinterface", param, TblLnsinterface.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsinterface");
		}
		if( null != interfaceTZ )
			throw new FabException("LNS001");

		// 查看还款明细表，查看待冲销用户有没有还款记录，有则直接终止放款冲销交易，
		param.clear();
		param.put("acctno", tblLnsinterface.getAcctno());
		param.put("brc", ctx.getBrc());
		Map<String, Object> retMap;
		try {
			/**
			 * SELECT COUNT(1) AS NUM FROM LNSRPYINFO 
			 *				WHERE ACCTNO = :acctno  
			 *				<#if brc?exists && brc != "">
			 *				  AND BRC = :brc
			 *				</#if>
			 * 20190529|14050183
			 */
			retMap = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsrpyinfo_count", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsrpyinfo");
		}

		if (Integer.parseInt(retMap.get("NUM").toString()) > 0) {// 如果还款明细表中有数据，则不能冲销，直接返回，结束
			throw new FabException("LNS001");
		}

		// 查看用户有没有出现过逾期的记录，有则直接终止放款冲销交易，查看账单表
		List<Map<String, Object>> retList;
		try {
			/**
			 * SELECT BILLSTATUS FROM LNSBILL 
			 *					WHERE ACCTNO = :acctno 
			 *					<#if brc?exists && brc != "">
			 *				     AND BRC = :brc
			 *					</#if>
			 * 20190529|14050183
			 */
			retList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_info", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}



		TblLnsinterfaceex interfaceex = null;
		if (null != retList && !retList.isEmpty()) {
			for (Map<String, Object> map : retList) {
				// {// 如果出现逾期情况，则直接退出，结束程序
				if (VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(map.get("billstatus")))
					throw new FabException("LNS002");
				if (!new FabAmount(Double.valueOf(map.get("billamt").toString()))
						.sub(Double.valueOf(map.get("billbal").toString())).isZero())
					throw new FabException("LNS174", map.get("billamt"), map.get("billbal"));
				if (CalendarUtil.after(ctx.getTranDate(), map.get("enddate").toString())) {

					//校验一下融担代偿的
					if(interfaceex==null) {
						param.put("acctno", tblLnsinterface.getAcctno());
						param.put("brc", ctx.getBrc());
						param.put("key", ConstantDeclare.KEYNAME.DC);//代偿
						try {
							interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", param, TblLnsinterfaceex.class);
						} catch (FabSqlException e) {
							throw new FabException(e, "SPS103", "lnsbasicinfo");
						}
						if(interfaceex==null)
							throw new FabException("LNS175");
					}
				}
			}
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
	}
	/**
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
	 * @return the tblLnsinterface
	 */
	public TblLnsinterface getTblLnsinterface() {
		return tblLnsinterface;
	}
	/**
	 * @param tblLnsinterface the tblLnsinterface to set
	 */
	public void setTblLnsinterface(TblLnsinterface tblLnsinterface) {
		this.tblLnsinterface = tblLnsinterface;
	}

}

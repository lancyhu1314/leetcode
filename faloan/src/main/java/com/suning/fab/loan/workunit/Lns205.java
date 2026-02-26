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

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.domain.TblLnstaxdetail;
import com.suning.fab.loan.utils.AccountingModeChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnseventreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：扣息税金冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns205 extends WorkUnit {
	
	String errDate;		// 错误日期
	String errSerSeq;	// 错误流水号
	
	@Autowired 
	LoanEventOperateProvider eventProvider;
	private Integer txseqno = 0;
	LoanAgreement loanAgreement;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("eventcode", ConstantDeclare.EVENT.DISCONTTAX);
		param.put("brc", ctx.getBrc());
		TblLnseventreg lnseventreg; 
		
		try {
			lnseventreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnseventreg", param, TblLnseventreg.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}
		
//		if (null == lnseventreg) {
//			return ;
//		}
		
		//账速融非标迁移冲销补充 2019-03-06
		if (null == lnseventreg || VarChecker.isEmpty(lnseventreg)) {
			
			Map<String, Object> param1 = new HashMap<String, Object>();
			param1.put("trandate", errDate);
			param1.put("serseqno", errSerSeq);
			param1.put("eventcode", ConstantDeclare.EVENT.LNDAINTTAX);
			param1.put("brc", ctx.getBrc());
			
			try {
				lnseventreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnseventreg", param1, TblLnseventreg.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnseventreg");
			}
			
			if (null == lnseventreg) {
				return ;
			}
			
			
//							return ;
		}
				
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnseventreg.getReceiptno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

		if(loanAgreement  == null)
			loanAgreement= LoanAgreementProvider.genLoanAgreementFromDB(lnseventreg.getReceiptno(), ctx);
		//FundInvest fundInvest = null
		
		//登记扣息税金冲销事件
		eventProvider.createEvent(ConstantDeclare.EVENT.DISCTAXOFF, new FabAmount(lnseventreg.getTranamt()), 
				lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.KSCX, ctx);
		//查询  扣息税金数据
		/*
		select * from lnstaxdetail where trandate = :trandate and serseqno = :serseqno and taxtype = 'KX'
		 */
		TblLnstaxdetail  lnstaxdetail;
		try {
			lnstaxdetail = DbAccessUtil.queryForObject("AccountingMode.query_lnstaxdetailKX", param, TblLnstaxdetail.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}
		if(null==lnstaxdetail){
			return;
		}
		AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, loanAgreement.getContract().getReceiptNo(), ConstantDeclare.EVENT.DISCTAXOFF, ConstantDeclare.BRIEFCODE.KSCX,
				lnstaxdetail.getTax().doubleValue() , "", loanAgreement.getFundInvest(),0.00,0.00 , 0.00);


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

	public Integer getTxseqno() {
		return txseqno;
	}

	public void setTxseqno(Integer txseqno) {
		this.txseqno = txseqno;
	}

	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}
}

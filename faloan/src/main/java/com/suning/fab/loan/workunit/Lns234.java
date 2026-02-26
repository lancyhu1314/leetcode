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

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
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

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：放款渠道冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns234 extends WorkUnit {
	
	String errDate;		// 错误日期
	String errSerSeq;	// 错误流水号
	String acctNo;
	String receiptNo;
	private TblLnsbasicinfo lnsbasicinfo;
	private Integer txseqno = 0;
	private LoanAgreement loanAgreement;
	private TblLnsinterface lnsinterface;

	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("eventcode", ConstantDeclare.EVENT.LOANCHANEL);
		param.put("brc", ctx.getBrc());
		TblLnseventreg lnseventreg; 
		
		try {
			lnseventreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnseventreg", param, TblLnseventreg.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}
		
		if (null == lnseventreg) {
			return ;
		}
		if(loanAgreement == null||null==loanAgreement.getContract()||null==loanAgreement.getContract().getReceiptNo())
			loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		
		//登记放款渠道冲销事件
		eventProvider.createEvent(ConstantDeclare.EVENT.LOANCNLOFF, new FabAmount(lnseventreg.getTranamt()), 
				lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FDCX, ctx);

		//查询主文件
		if(lnsbasicinfo==null){
			param.clear();
			param.put("acctno", acctNo);
			param.put("openbrc", ctx.getBrc());
			try{
				lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}
		}
		//单渠道  事件金额等于 合同金额减去扣息放款金额
		AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, getReceiptNo(), ConstantDeclare.EVENT.LOANCNLOFF, ConstantDeclare.BRIEFCODE.FDCX,
					lnsinterface.getTranamt() , "", loanAgreement.getFundInvest(),0.00,0.00 , 0.00);
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

	public String getReceiptNo() {
		return receiptNo;
	}

	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
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

	public TblLnsinterface getLnsinterface() {
		return lnsinterface;
	}

	public void setLnsinterface(TblLnsinterface lnsinterface) {
		this.lnsinterface = lnsinterface;
	}
}

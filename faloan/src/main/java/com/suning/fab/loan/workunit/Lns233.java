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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.AccountingModeChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnseventreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：非标放款冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns233 extends WorkUnit {
	
	String errDate;			// 错误日期
	String errSerSeq;		// 错误流水号
	String acctNo;
	Integer curKey;
	private Integer txseqno = 0;
	private LoanAgreement loanAgreement;
	String receiptNo;

	@Autowired
	@Qualifier("accountSuber")
	AccountOperator suber;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		int	i = 0;
		TranCtx ctx = getTranctx();
		
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("eventcode", ConstantDeclare.EVENT.LOANGRANTA);
		param.put("brc", ctx.getBrc());
		//当有债务公司的时候，可能有多个放款事件，此处需要循环遍历
		List<TblLnseventreg> lnseventregList;
		try {
			lnseventregList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnseventreg", param, TblLnseventreg.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}
		
		if (null == lnseventregList || lnseventregList.isEmpty()) {
			throw new FabException("LNS003");
		}
		if(loanAgreement == null||null==loanAgreement.getContract()||null==loanAgreement.getContract().getReceiptNo())
		 	loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
		Iterator<TblLnseventreg> iterator = lnseventregList.iterator();
		while(iterator.hasNext()) {
			TblLnseventreg lnseventreg = iterator.next();
			i++;
			if(i == getCurKey())
			{	
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());


				/*减本金户，放款冲销--将账务处理删掉*/
				suber.operate(lnsAcctInfo, null, new FabAmount(lnseventreg.getTranamt()), loanAgreement.getFundInvest(), 
						ConstantDeclare.BRIEFCODE.FKCX, ctx);
				//登记放款冲销事件
				eventProvider.createEvent(ConstantDeclare.EVENT.LOANWRTOFF, new FabAmount(lnseventreg.getTranamt()), 
						lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FKCX, ctx);
			}
			//else 多次放款是债务公司的案例 非标不存在
		}

		//只有一个放款事件 幂等表里的tranAmt即为合同金额
		AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, receiptNo, ConstantDeclare.EVENT.LOANWRTOFF, ConstantDeclare.BRIEFCODE.FKCX,
				loanAgreement.getContract().getContractAmt().getVal() , "", loanAgreement.getFundInvest(),0.00,0.00 , 0.00);

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

	public Integer getCurKey() {
		return curKey;
	}

	public void setCurKey(Integer curKey) {
		this.curKey = curKey;
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

	public String getReceiptNo() {
		return receiptNo;
	}

	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}


}

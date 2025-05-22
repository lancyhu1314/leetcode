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

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnseventreg;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.domain.TblLnsprefundsch;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AccountingModeChange;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：本金户销户
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns103 extends WorkUnit {
	
	String errDate;			// 错误日期
	String errSerSeq;		// 错误流水号
	String acctNo;			// 借据号，用于在原交易中特殊处理流程中使用
	FabAmount contractAmt;	// 放款金额
	String customName;		// 户名
	String memo;			// 银行流水号/易付宝单号

	@Autowired 
	LoanEventOperateProvider eventProvider;
	private Integer txseqno = 0;
	private LoanAgreement loanAgreement;
	private TblLnsinterface lnsinterface;
	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName="";
		
		TranCtx ctx = getTranctx();
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("eventcode", ConstantDeclare.EVENT.LOANMEBRGT);
		param.put("brc", ctx.getBrc());
		TblLnseventreg lnseventreg; 
		
		try {
			lnseventreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnseventreg", param, TblLnseventreg.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}
		
		if (null != lnseventreg) {//当有债务公司的时候可能没有本金户事件
			LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(lnseventreg.getReceiptno(), ctx);
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnseventreg.getReceiptno(), 
					ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			
			//登记本金户销户事件
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANMEBCAL, new FabAmount(lnseventreg.getTranamt()), 
					lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.BJXH, ctx);
			//删除账户信息只能放在special中操作，如果在此处删除，执行AccountUpdate()时会报错，找不到该账户
		} else {//如果没有本金户冲销事件，则查询下该冲销数据的事件登记薄信息
			param.clear();
			param.put("trandate", errDate);
			param.put("serseqno", errSerSeq);
			try {
				lnseventreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnseventreg", param, TblLnseventreg.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnseventreg");
			}
			if (null == lnseventreg) {
				throw new FabException("LNS003");
			}
		}

		/*
		 * 销本金户
		 * 需要金额  （开本金户时金额默认为0）
		 */
		/*
		select * from lnsprefundsch where trandate = :trandate and serseqno = :serseqno and  accsrccode ='D';
		 */
		Map<String, Object> queryparam = new HashMap<>();
		queryparam.put("trandate", errDate);
		queryparam.put("serseqno", errSerSeq);
		List<TblLnsprefundsch> lnsprefundschs;
		//查询幂等表
		if(lnsinterface == null) {
			try {
				lnsinterface = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsinterface_208", queryparam, TblLnsinterface.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsinterface");
			}
		}
		try {
			lnsprefundschs = DbAccessUtil.queryForList("AccountingMode.query_lnsprefundschD", queryparam, TblLnsprefundsch.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsprefundsch");
		}
		//给返回报文字段赋值
		setAcctNo(lnsinterface.getAcctno());//给借据号赋值，用于给原交易特殊流程进行删除操作
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		//没有债务公司的时候抛此事件 （开本金户时金额默认为0）
		if(null==lnsprefundschs || lnsprefundschs.size()==0)
			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, lnsinterface.getAcctno(), ConstantDeclare.EVENT.LOANMEBCAL, ConstantDeclare.BRIEFCODE.BJXH,
					0.00 , "", loanAgreement.getFundInvest(),0.00,0.00 , 0.00);


		setContractAmt(loanAgreement.getContract().getContractAmt());
		setCustomName("");
		
		//冲销完成后，忘lnsbasicinfocal中插入该冲销记录，同时更新幂等登记薄中acctname,acctno,tranamt
		try{
			param.clear();
			param.put("acctno", acctNo);
			param.put("brc", ctx.getBrc());
			param.put("loanstat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
			
			/*删除账单表*/
			DbAccessUtil.execute("CUSTOMIZE.delete_lnsbill_off", param);
			
			/*更新主文件表中该数据的贷款状态CA,以及利息计提标志CLOSE*/
			DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstatEx", param);
			
			/*冲销时将该条记录插入到lnsbasicinfocal表中*/
			DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfocal", param);
			
			/*更新幂等登记薄中的acctname(customName), acctNo, tranamt(contractAmt)三个字段的值*/
			param.put("serialno", ctx.getSerialNo());
			param.put("trancode", ctx.getTranCode());
			param.put("acctname", customName);
			param.put("tranamt", contractAmt.getVal());
			param.put("reserv5", loanAgreement.getFundInvest().getChannelType());
			param.put("memo", memo);
			DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_off", param);
		
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS101", "update_lnsinterface_off");
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
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}

	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	/**
	 * @return the customName
	 */
	public String getCustomName() {
		return customName;
	}

	/**
	 * @param customName the customName to set
	 */
	public void setCustomName(String customName) {
		this.customName = customName;
	}

	/**
	 * @return the memo
	 */
	public String getMemo() {
		return memo;
	}

	/**
	 * @param memo the memo to set
	 */
	public void setMemo(String memo) {
		this.memo = memo;
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
}

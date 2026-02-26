package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnseventreg;
import com.suning.fab.loan.domain.TblLnstaxdetail;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 16071579
 *
 * @version V1.0.1
 *
 * @see -放款冲销-结息税金冲销
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns224 extends WorkUnit {

	String acctNo;	//帐号
	FabAmount tranAmt;	//结息税金
	String orgId;
	String channelType;
	String outSerialNo;
	String receiptNo;//借据号
	
	@Autowired
	LoanEventOperateProvider eventProvider;
	private Integer txseqno = 0;
	private LoanAgreement loanAgreement;
	private TblLnstaxdetail lnstaxdetail;
	@Override
	public void run() throws Exception {

//		TranCtx ctx = getTranctx();
//		setTranAmt(new FabAmount(0.00));//将结息税金清零
//
//		Map<String, Object> param = new HashMap<>();
//		param.put("acctno", acctNo);
//		param.put("brc", ctx.getBrc());
//		//查询税金相关表   select * from lnstaxdetail where acctno = :acctno  and brc = :brc and taxtype = 'JX'
//
//		try {
//			lnstaxdetail = DbAccessUtil.queryForObject("AccountingMode.query_lnstaxdetailJX", param, TblLnstaxdetail.class);
//		} catch (FabSqlException e) {
//			throw new FabException(e, "SPS103", "lnsaccountdyninfo");
//		}


//		LnsAcctInfo lnsAcctInfo;
//		if (null == lnstaxdetail) {//如果没有结息的数据，可能是未存税金表的老数据  查询事件登记簿
//			//并行期
//			param.put("acctno", receiptNo);
//			param.put("eventcode", ConstantDeclare.EVENT.INTSETMTAX);
//			param.put("brc", ctx.getBrc());
//
//			TblLnseventreg lnseventreg;
//			try {
//				lnseventreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_JXOFF", param, TblLnseventreg.class);
//			} catch (FabSqlException e) {
//				throw new FabException(e, "SPS103", "lnseventreg");
//			}
//
//			if (null == lnseventreg) //如果没有结息的数据，则直接返回，不处理
//				return;
//			LoggerUtil.info("未找到lnstaxdetail数据，从事件表获取到了 ");
//			setTranAmt(new FabAmount(lnseventreg.getTranamt()));
//			String acctstat = lnseventreg.getAcctstat().trim();//获取该账户的形态
//			String[] temp = acctstat.split("\\.");
//			lnsAcctInfo = new LnsAcctInfo(acctNo, temp[0],
//					temp[1], new FabCurrency());
//		}else{
//			setTranAmt(new FabAmount(lnstaxdetail.getTax()));
//			lnsAcctInfo = new LnsAcctInfo(acctNo, lnstaxdetail.getBilltype(), "N", new FabCurrency());
//		}

//		if(loanAgreement == null||null==loanAgreement.getContract()||null==loanAgreement.getContract().getReceiptNo())
//		 	loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
//		// 如果orgid,channeltype,outserialno为空，则不进行设值,老系统的数据，此处有值，新系统的这个字段应该为空
//		if (!VarChecker.isEmpty(orgId)) {
//			loanAgreement.getFundInvest().setInvestee(orgId);
//		}
//
//		if (!VarChecker.isEmpty(channelType)) {
//			loanAgreement.getFundInvest().setChannelType(channelType);
//		}
//
//		if (!VarChecker.isEmpty(outSerialNo)) {
//			loanAgreement.getFundInvest().setOutSerialNo(outSerialNo);
//		}
//		// 登记结息冲销事件
//		eventProvider.createEvent(ConstantDeclare.EVENT.INTSTAXOFF, tranAmt, lnsAcctInfo, null,
//						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.JSCX, ctx, loanAgreement.getCustomer().getMerchantNo(), loanAgreement.getBasicExtension().getDebtCompany());
//		Double amt = 0.00;
//		if(CalendarUtil.equalDate(lnstaxdetail.getTrandate(), ctx.getTranDate())){
//			amt = tranAmt.getVal();
//		}
//		if(receiptNo!=null)
//
//			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, receiptNo, ConstantDeclare.EVENT.INTSTAXOFF, ConstantDeclare.BRIEFCODE.JSCX,
//				tranAmt.getVal() , "", loanAgreement.getFundInvest(),0.00,0.00 , amt);
//		else
//			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, acctNo, ConstantDeclare.EVENT.INTSTAXOFF, ConstantDeclare.BRIEFCODE.JSCX,
//					tranAmt.getVal() , "", loanAgreement.getFundInvest(),0.00,0.00 , amt);

	}

	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo
	 *            the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	/**
	 * @return the tranAmt
	 */
	public FabAmount getTranAmt() {
		return tranAmt;
	}

	/**
	 * @param tranAmt the tranAmt to set
	 */
	public void setTranAmt(FabAmount tranAmt) {
		this.tranAmt = tranAmt;
	}

	/**
	 * @return the orgId
	 */
	public String getOrgId() {
		return orgId;
	}

	/**
	 * @param orgId the orgId to set
	 */
	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	/**
	 * @return the channelType
	 */
	public String getChannelType() {
		return channelType;
	}

	/**
	 * @param channelType the channelType to set
	 */
	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	/**
	 * @return the outSerialNo
	 */
	public String getOutSerialNo() {
		return outSerialNo;
	}

	/**
	 * @param outSerialNo the outSerialNo to set
	 */
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}

	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}

	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
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

	public TblLnstaxdetail getLnstaxdetail() {
		return lnstaxdetail;
	}

	public void setLnstaxdetail(TblLnstaxdetail lnstaxdetail) {
		this.lnstaxdetail = lnstaxdetail;
	}
}

package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 16071579
 *
 * @version V1.0.1
 *
 * @see -放款冲销-结息冲销
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns223 extends WorkUnit {

	String acctNo;	//帐号
	FabAmount tranAmt;	//结息金额
	String orgId;
	String channelType;
	String outSerialNo;
	String receiptNo;//借据号

	@Autowired
	@Qualifier("accountSuber")
	AccountOperator suber;
	@Autowired
	LoanEventOperateProvider eventProvider;
	private Integer txseqno = 0;
	private LoanAgreement loanAgreement;
	private TblLnstaxdetail lnstaxdetail;
	private TblLnsbasicinfo lnsbasicinfo;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		setTranAmt(new FabAmount(0.00));//将结息金额清零

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());
		//查询税金相关表   select * from lnstaxdetail where acctno = :acctno  and brc = :brc and taxtype = 'JX'
		try {
			lnstaxdetail = DbAccessUtil.queryForObject("AccountingMode.query_lnstaxdetailJX", param, TblLnstaxdetail.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsaccountdyninfo");
		}

		param.put("openbrc", ctx.getBrc());

		if(lnsbasicinfo == null) {
			try {
				lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}
		}

		LnsAcctInfo lnsAcctInfo;
		if (null == lnstaxdetail) {//如果没有结息的数据，可能是未存税金表的老数据  查询事件登记簿
			//并行期
			param.put("acctno", acctNo);
			param.put("eventcode", ConstantDeclare.EVENT.INTSETLEMT);
			param.put("brc", ctx.getBrc());

			TblLnseventreg lnseventreg;
			try {
				/**
				 * SELECT * FROM LNSEVENTREG 
				 * 		   WHERE RECEIPTNO = :acctno 
				 * 			 AND EVENTCODE = :eventcode 
				 * 			 AND TRANCODE = '479002' 
				 * 			 AND BRC = :brc 
				 *		   ORDER BY TIMESTAMP DESC FETCH FIRST 1 ROWS ONLY
				 */
				lnseventreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_JXOFF", param, TblLnseventreg.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnseventreg");
			}
			if(lnseventreg == null)
				return ;
			LoggerUtil.info("未找到lnstaxdetail数据，从事件表获取到了 ");
			setTranAmt(new FabAmount(lnseventreg.getTranamt()));
			String acctstat = lnseventreg.getAcctstat().trim();//获取该账户的形态
			String[] temp = acctstat.split("\\.");
			lnsAcctInfo = new LnsAcctInfo(acctNo, temp[0],
					temp[1], new FabCurrency());
		}else{
			setTranAmt(new FabAmount(lnstaxdetail.getTranamt()));
			lnsAcctInfo = new LnsAcctInfo(acctNo, lnstaxdetail.getBilltype(),
					//这里有问题
					"N", new FabCurrency());
		}



		if(loanAgreement == null||null==loanAgreement.getContract()||null==loanAgreement.getContract().getReceiptNo())
			loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);

		//核销的贷款不能被冲销
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lnsbasicinfo.getLoanstat()))
			throw new FabException("LNS124");

		//如果orgid,channeltype,outserialno为空，则不进行设值,老系统的数据，此处有值，新系统的这个字段应该为空
		if (!VarChecker.isEmpty(orgId)) {
			loanAgreement.getFundInvest().setInvestee(orgId);
		}

		if (!VarChecker.isEmpty(channelType)) {
			loanAgreement.getFundInvest().setChannelType(channelType);
		}

		if (!VarChecker.isEmpty(outSerialNo)) {
			loanAgreement.getFundInvest().setOutSerialNo(outSerialNo);
		}

		/*减利息户(NINT.N)，结息冲销--将账务处理删掉，否则会账不平*/
		suber.operate(lnsAcctInfo, null, tranAmt, loanAgreement.getFundInvest(),
				ConstantDeclare.BRIEFCODE.JXCX, ctx);
		List<FabAmount> amtList = new ArrayList<>();
		amtList.add(new FabAmount(lnstaxdetail.getTax()));
		// 登记结息冲销事件
		eventProvider.createEvent(ConstantDeclare.EVENT.INTSETMOFF, tranAmt, lnsAcctInfo, null,
				loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.JXCX, ctx,amtList, loanAgreement.getCustomer().getMerchantNo(), loanAgreement.getBasicExtension().getDebtCompany());
		Double amt = 0.00;
		if(CalendarUtil.equalDate(lnstaxdetail.getTrandate(), ctx.getTranDate())){
			amt = tranAmt.getVal();
		}
		if(receiptNo!=null)
			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, receiptNo, ConstantDeclare.EVENT.INTSETMOFF, ConstantDeclare.BRIEFCODE.JXCX,
					tranAmt.getVal() , "", loanAgreement.getFundInvest(),lnstaxdetail.getTax(),amt , 0.00);
		else
			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, acctNo, ConstantDeclare.EVENT.INTSETMOFF, ConstantDeclare.BRIEFCODE.JXCX,
					tranAmt.getVal() , "", loanAgreement.getFundInvest(),lnstaxdetail.getTax(),amt , 0.00);

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

	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	public TblLnstaxdetail getLnstaxdetail() {
		return lnstaxdetail;
	}

	public void setLnstaxdetail(TblLnstaxdetail lnstaxdetail) {
		this.lnstaxdetail = lnstaxdetail;
	}

	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}
}

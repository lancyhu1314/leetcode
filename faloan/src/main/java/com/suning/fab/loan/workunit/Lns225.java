package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsadjustinforeg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 罚息还本
 *
 * @param serialNo	公文号
 * @param briefCode	摘要码：罚息还本
 * @param acctType	转出账户形态
 * @param acctNo	转出账号
 * @param tranAmt	调账金额
 * @param tellerCode	操作人工号
 * @param receiptNo	借据号
 * @param tunnelData	隧道字段：包括错账日期、调账原因等
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns225 extends WorkUnit { 

	String serialNo;
	String briefCode;
	String acctType;
	String acctNo;
	FabAmount tranAmt;
	String tellerCode;
	String receiptNo;
	String tunnelData;
	String debtCompanyin;
	String debtCompanyout;
	
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		if(VarChecker.isEmpty(tranAmt) || !tranAmt.isPositive()){
			throw new FabException("LNS042");
		}
		if(VarChecker.isEmpty(acctType) || (acctType.length()<6)){
			throw new FabException("LNS043");
		}
		if(VarChecker.isEmpty(acctNo)){
			throw new FabException("ACC103");
		}
		//插调账信息登记簿
		TblLnsadjustinforeg lnsadjustinforeg = new TblLnsadjustinforeg();
		lnsadjustinforeg.setTrandate(Date.valueOf(ctx.getTermDate()));
		lnsadjustinforeg.setSerialno(serialNo);
		lnsadjustinforeg.setChannelid(ctx.getChannelId());
		lnsadjustinforeg.setAccdate(Date.valueOf(ctx.getTranDate()));
		lnsadjustinforeg.setSerseqno(ctx.getSerSeqNo());
		lnsadjustinforeg.setTxnseq(1);
		lnsadjustinforeg.setTransource("1");
		lnsadjustinforeg.setBrc(ctx.getBrc());
		lnsadjustinforeg.setCcy(tranAmt.getCurrency().getCcy());
		lnsadjustinforeg.setTranamt(tranAmt.getVal());
		lnsadjustinforeg.setTrancode(ctx.getTranCode());
		lnsadjustinforeg.setTeller(tellerCode);
		lnsadjustinforeg.setBriefcode(ConstantDeclare.BRIEFCODE.FXHB);
		lnsadjustinforeg.setReceiptno(receiptNo);
		lnsadjustinforeg.setAccstat(acctType);
		lnsadjustinforeg.setAcctno(acctNo);
		lnsadjustinforeg.setTunneldata(tunnelData);
		lnsadjustinforeg.setReserv3(debtCompanyin);
		lnsadjustinforeg.setReserv4(debtCompanyout);
		try{
			DbAccessUtil.execute("Lnsadjustinforeg.insert", lnsadjustinforeg);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS100", "lnsadjustinforeg");
		}
		
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		la.getFundInvest().setOutSerialNo(serialNo);
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, acctType.substring(0, 4), acctType.substring(5, 6), new FabCurrency());

		List<FabAmount> amtList = new ArrayList<FabAmount>();
		amtList.add(TaxUtil.calcVAT(tranAmt));
		sub.operate(lnsAcctInfo, null, tranAmt, la.getFundInvest(), ConstantDeclare.BRIEFCODE.FXHB, ctx);
		eventProvider.createEvent(ConstantDeclare.EVENT.DINTADJUST, tranAmt, lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.FXHB, ctx, amtList,debtCompanyin,debtCompanyout);

	}
	/**
	 * @return the serialNo
	 */
	public String getSerialNo() {
		return serialNo;
	}
	/**
	 * @param serialNo the serialNo to set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	/**
	 * @return the briefCode
	 */
	public String getBriefCode() {
		return briefCode;
	}
	/**
	 * @param briefCode the briefCode to set
	 */
	public void setBriefCode(String briefCode) {
		this.briefCode = briefCode;
	}
	/**
	 * @return the acctType
	 */
	public String getAcctType() {
		return acctType;
	}
	/**
	 * @param acctType the acctType to set
	 */
	public void setAcctType(String acctType) {
		this.acctType = acctType;
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
	 * @return the tellerCode
	 */
	public String getTellerCode() {
		return tellerCode;
	}
	/**
	 * @param tellerCode the tellerCode to set
	 */
	public void setTellerCode(String tellerCode) {
		this.tellerCode = tellerCode;
	}
	/**
	 * @return the tunnelData
	 */
	public String getTunnelData() {
		return tunnelData;
	}
	/**
	 * @param tunnelData the tunnelData to set
	 */
	public void setTunnelData(String tunnelData) {
		this.tunnelData = tunnelData;
	}
	/**
	 * @return the sub
	 */
	public AccountOperator getSub() {
		return sub;
	}
	/**
	 * @param sub the sub to set
	 */
	public void setSub(AccountOperator sub) {
		this.sub = sub;
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
	 * @return the debtCompanyin
	 */
	public String getDebtCompanyin() {
		return debtCompanyin;
	}
	/**
	 * @param debtCompanyin the debtCompanyin to set
	 */
	public void setDebtCompanyin(String debtCompanyin) {
		this.debtCompanyin = debtCompanyin;
	}
	/**
	 * @return the debtCompanyout
	 */
	public String getDebtCompanyout() {
		return debtCompanyout;
	}
	/**
	 * @param debtCompanyout the debtCompanyout to set
	 */
	public void setDebtCompanyout(String debtCompanyout) {
		this.debtCompanyout = debtCompanyout;
	}
}


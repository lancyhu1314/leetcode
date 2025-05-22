package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsadjustinforeg;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
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
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 客户账调账
 *
 * @param serialNo	公文号
 * @param briefCode	摘要码：罚息还本
 * @param acctType	转出账户形态
 * @param toAcctType	转入账户形态
 * @param acctNo	转出账号
 * @param toAcctNo	转入账号
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
public class Lns227 extends WorkUnit { 

	String serialNo;
	String briefCode;
	String acctType;
	String toAcctType;
	String acctNo;
	String toAcctNo;
	FabAmount tranAmt;
	String tellerCode;
	String receiptNo;
	String tunnelData;
	String debtCompanyin;
	String debtCompanyout;
	
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		if(VarChecker.isEmpty(tranAmt) || !tranAmt.isPositive()){
			throw new FabException("LNS042");
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
		lnsadjustinforeg.setBriefcode(briefCode);
		lnsadjustinforeg.setReceiptno(receiptNo);
		lnsadjustinforeg.setAccstat(acctType);
		lnsadjustinforeg.setAcctno(acctNo);
		lnsadjustinforeg.setToaccstat(toAcctType);
		lnsadjustinforeg.setToaccountno(toAcctNo);
		lnsadjustinforeg.setTunneldata(tunnelData);
		lnsadjustinforeg.setReserv3(debtCompanyin);
		lnsadjustinforeg.setReserv4(debtCompanyout);
		try{
			DbAccessUtil.execute("Lnsadjustinforeg.insert", lnsadjustinforeg);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS100", "lnsadjustinforeg");
		}
		
		//费用子机构
		String feeBrc = null;

		if( (null != toAcctType && !VarChecker.isEmpty(toAcctType) && VarChecker.asList("FEEA","FEED").contains(toAcctType.substring(0, 4)) ) ||
			(null != acctType   && !VarChecker.isEmpty(acctType) && VarChecker.asList("FEEA","FEED").contains(acctType.substring(0, 4)) )  )
		{
			Map<String,Object> param = new HashMap<String,Object>();
			param.put("openbrc", ctx.getBrc());
			
			List<TblLnsfeeinfo> accFeeInfo=null;
			List<TblLnsfeeinfo> toAccFeeInfo=null;

			if( null != acctNo && !VarChecker.isEmpty(acctNo) )
			{
				param.put("acctno",acctNo );
				try{
					accFeeInfo = DbAccessUtil.queryForList("Lnsfeeinfo.select", param, TblLnsfeeinfo.class);
				}catch (FabSqlException e){
					throw  new FabException(e,"SPS103","Lnsfeeinfo");
				}
			}
			if( null != toAcctNo && !VarChecker.isEmpty(toAcctNo) )
			{
				param.put("acctno",toAcctNo );
				try{
					toAccFeeInfo = DbAccessUtil.queryForList("Lnsfeeinfo.select", param, TblLnsfeeinfo.class);
				}catch (FabSqlException e){
					throw  new FabException(e,"SPS103","Lnsfeeinfo");
				}
			}
			
			if( !VarChecker.isEmpty(accFeeInfo) && accFeeInfo.size() > 0  )
				feeBrc = accFeeInfo.get(0).getFeebrc();
			if( !VarChecker.isEmpty(toAccFeeInfo) && toAccFeeInfo.size() > 0  )
				feeBrc = toAccFeeInfo.get(0).getFeebrc();
		}
		
		if(VarChecker.asList(ConstantDeclare.BRIEFCODE.KHZZ).contains(ctx.getRequestDict("briefCode"))){
			if(VarChecker.isEmpty(acctNo) || VarChecker.isEmpty(toAcctNo)){
				throw new FabException("ACC103");
			}
			if(VarChecker.isEmpty(acctType) || VarChecker.isEmpty(toAcctType)){
				throw new FabException("LNS043");
			}
			if((acctType.length()<6) || (toAcctType.length()<6)){
				throw new FabException("LNS043");
			}
			if(!acctNo.trim().equals(toAcctNo.trim())){
				throw new FabException("LNS044");
			}
			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
			la.getFundInvest().setOutSerialNo(serialNo);


			if( null == feeBrc )
			{
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, acctType.substring(0, 4), acctType.substring(5, 6), new FabCurrency());
				LnsAcctInfo lnsToAcctInfo = new LnsAcctInfo(toAcctNo, toAcctType.substring(0, 4), toAcctType.substring(5, 6), new FabCurrency());

				sub.operate(lnsAcctInfo, null, tranAmt, la.getFundInvest(), briefCode, ctx);
				add.operate(lnsToAcctInfo, null, tranAmt, la.getFundInvest(), briefCode, ctx);
				
				eventProvider.createEvent(ConstantDeclare.EVENT.CACTADJUST, tranAmt, lnsAcctInfo, lnsToAcctInfo, la.getFundInvest(), briefCode, ctx,debtCompanyin,debtCompanyout);
			}
			else
			{
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, acctType.substring(0, 4), acctType.substring(5, 6), new FabCurrency(), feeBrc);
				LnsAcctInfo lnsToAcctInfo = new LnsAcctInfo(toAcctNo, toAcctType.substring(0, 4), toAcctType.substring(5, 6), new FabCurrency(),feeBrc );

				sub.operate(lnsAcctInfo, null, tranAmt, la.getFundInvest(), briefCode, ctx);
				add.operate(lnsToAcctInfo, null, tranAmt, la.getFundInvest(), briefCode, ctx);
				
				eventProvider.createEvent(ConstantDeclare.EVENT.CACTADJUST, tranAmt, lnsAcctInfo, lnsToAcctInfo, la.getFundInvest(), briefCode, ctx, feeBrc);
			}

		}else if(VarChecker.asList(ConstantDeclare.BRIEFCODE.KHZK).contains(ctx.getRequestDict("briefCode"))){
			if(VarChecker.isEmpty(acctNo)){
				throw new FabException("ACC103");
			}
			if(VarChecker.isEmpty(acctType) || (acctType.length()<6)){
				throw new FabException("LNS043");
			}
			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
			la.getFundInvest().setOutSerialNo(serialNo);

			if( null == feeBrc )
			{
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, acctType.substring(0, 4), acctType.substring(5, 6), new FabCurrency());
				sub.operate(lnsAcctInfo, null, tranAmt, la.getFundInvest(), briefCode, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CACTADJUST, tranAmt, lnsAcctInfo, null, la.getFundInvest(), briefCode, ctx,debtCompanyin,debtCompanyout);
			}
			else
			{
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, acctType.substring(0, 4), acctType.substring(5, 6), new FabCurrency(),feeBrc);
				sub.operate(lnsAcctInfo, null, tranAmt, la.getFundInvest(), briefCode, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CACTADJUST, tranAmt, lnsAcctInfo, null, la.getFundInvest(), briefCode, ctx,feeBrc);
			}
			
			
		}else if(VarChecker.asList(ConstantDeclare.BRIEFCODE.KHZJ).contains(ctx.getRequestDict("briefCode"))){
			if(VarChecker.isEmpty(toAcctNo)){
				throw new FabException("ACC103");
			}
			if(VarChecker.isEmpty(toAcctType) || (toAcctType.length()<6)){
				throw new FabException("LNS043");
			}
			LoanAgreement laopp = LoanAgreementProvider.genLoanAgreementFromDB(toAcctNo, ctx);
			laopp.getFundInvest().setOutSerialNo(serialNo);

			if( null == feeBrc )
			{
				LnsAcctInfo lnsToAcctInfo = new LnsAcctInfo(toAcctNo, toAcctType.substring(0, 4), toAcctType.substring(5, 6), new FabCurrency());
				add.operate(lnsToAcctInfo, null, tranAmt, laopp.getFundInvest(), briefCode, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CACTADJUST, tranAmt, lnsToAcctInfo, null, laopp.getFundInvest(), briefCode, ctx,debtCompanyin,debtCompanyout);
			}
			else
			{
				LnsAcctInfo lnsToAcctInfo = new LnsAcctInfo(toAcctNo, toAcctType.substring(0, 4), toAcctType.substring(5, 6), new FabCurrency(),feeBrc);
				add.operate(lnsToAcctInfo, null, tranAmt, laopp.getFundInvest(), briefCode, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CACTADJUST, tranAmt, lnsToAcctInfo, null, laopp.getFundInvest(), briefCode, ctx,feeBrc);
			}
		}
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
	 * @return the toAcctType
	 */
	public String getToAcctType() {
		return toAcctType;
	}
	/**
	 * @param toAcctType the toAcctType to set
	 */
	public void setToAcctType(String toAcctType) {
		this.toAcctType = toAcctType;
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
	 * @return the toAcctNo
	 */
	public String getToAcctNo() {
		return toAcctNo;
	}
	/**
	 * @param toAcctNo the toAcctNo to set
	 */
	public void setToAcctNo(String toAcctNo) {
		this.toAcctNo = toAcctNo;
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
	/**
	 * @return the add
	 */
	public AccountOperator getAdd() {
		return add;
	}
	/**
	 * @param add the add to set
	 */
	public void setAdd(AccountOperator add) {
		this.add = add;
	}
}


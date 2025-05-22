package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsadjustinforeg;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 *  预收户调账
 *
 *  serialNo	公文号
 *  briefCode	摘要码：罚息还本
 *  acctType	转出账户形态
 *  toAcctType	转入账户形态
 *  acctNo	转出账号
 *  toAcctNo	转入账号
 *  tranAmt	调账金额
 *  tellerCode	操作人工号
 *  receiptNo	借据号
 *  tunnelData	隧道字段：包括错账日期、调账原因等
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns228 extends WorkUnit { 

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

	
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		String feeType = "";  	//账户类型
		LnsAcctInfo oppAcctInfo = null;
		TranCtx ctx = getTranctx();
		if(VarChecker.isEmpty(tranAmt)||!tranAmt.isPositive()){
			throw new FabException("LNS042");
		}

		//插调账登记簿
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
		try{
			DbAccessUtil.execute("Lnsadjustinforeg.insert", lnsadjustinforeg);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS100", "lnsadjustinforeg");
		}
		
//		Map<String,Object> param = new HashMap<String, Object>();
//		param.put("brc", ctx.getBrc());
//		param.put("accsrccode", "N");
		String cdflag = "add";
		String acctno = acctNo;
		if(VarChecker.asList(ConstantDeclare.BRIEFCODE.YSZJ).contains(briefCode)){
			if(VarChecker.isEmpty(toAcctNo)){
				throw new FabException("LNS010");
			}else{
				acctno = toAcctNo;
				feeType = ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT;
				oppAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, "", new FabCurrency());
//				param.put("acctno", toAcctNo);
//				param.put("balance", tranAmt.getVal());
			}
		}else if(VarChecker.asList(ConstantDeclare.BRIEFCODE.YSJS).contains(briefCode)){
			if(VarChecker.isEmpty(acctNo)){
				throw new FabException("LNS010");
			}else{
//				param.put("acctno", acctNo);
//				param.put("balance", new FabAmount(0.00).selfSub(tranAmt).getVal());
				acctno = acctNo;
				feeType = ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT;
				oppAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, "", new FabCurrency());
				cdflag = "sub";
			}
		}else if(VarChecker.asList(ConstantDeclare.BRIEFCODE.CKZJ).contains(briefCode)){
			if(VarChecker.isEmpty(toAcctNo)){
				throw new FabException("LNS010");
			}else{
				acctno = toAcctNo;
				feeType = ConstantDeclare.ASSISTACCOUNT.SURPLUSACCT;
				oppAcctInfo = new LnsAcctInfo(receiptNo, "", ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT, new FabCurrency());
//				param.put("acctno", toAcctNo);
//				param.put("balance", tranAmt.getVal());
			}
		}else if(VarChecker.asList(ConstantDeclare.BRIEFCODE.CKJS).contains(briefCode)){
			if(VarChecker.isEmpty(acctNo)){
				throw new FabException("LNS010");
			}else{
//				param.put("acctno", acctNo);
//				param.put("balance", new FabAmount(0.00).selfSub(tranAmt).getVal());
				acctno = acctNo;
				feeType = ConstantDeclare.ASSISTACCOUNT.SURPLUSACCT;
				oppAcctInfo = new LnsAcctInfo(receiptNo, "", ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT, new FabCurrency());
				cdflag = "sub";
			}
		}

//		TblLnsprefundaccount lnsprefundaccount;
//		try {
//			lnsprefundaccount = DbAccessUtil.queryForObject("CUSTOMIZE.update_lnsprefundaccount_add", param, TblLnsprefundaccount.class);
//		}
//		catch (FabSqlException e)
//		{
//			throw new FabException(e, "SPS102", "lnsprefundaccount");
//		}
//		if(null == lnsprefundaccount){
//			throw new FabException("LNS013");
//		}
//
//		if (new FabAmount(lnsprefundaccount.getBalance()).isNegative()) {
//			throw new FabException("LNS019");
//		}
//		//预收户 差错账调整
//		AccountingModeChange.saveLnsprefundsch(ctx, 1, lnsprefundaccount.getAcctno(), lnsprefundaccount.getCustomid(), "N",lnsprefundaccount.getCusttype() ,
//				lnsprefundaccount.getName() ,tranAmt.getVal() ,cdflag );


		// 更新辅助账户动态表并插入一条明细
		TblLnsassistdyninfo dyninfo = LoanAssistDynInfoUtil.updatePreaccountInfo(ctx, ctx.getBrc(),acctno, feeType, tranAmt, cdflag,"");

		if (new FabAmount(dyninfo.getCurrbal()).isNegative()) {
			throw new FabException("LNS019");
		}

		LnsAcctInfo lnsAcctInfo = null;
		FundInvest	fundInvest = new FundInvest("", "", "", "", "");
		if(!VarChecker.isEmpty(receiptNo)){
			//处理老数据acctno与acctno1不一致
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("acctno1", receiptNo);
			paramMap.put("brc", ctx.getBrc());
			
			Map<String, Object> custominfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_acctno", paramMap);
			if (custominfo != null)
			{
				String accttmp = custominfo.get("acctno").toString();
				lnsAcctInfo = new LnsAcctInfo(accttmp, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(accttmp, ctx);
				fundInvest = la.getFundInvest();
			}
		}
		fundInvest.setOutSerialNo(serialNo);
		
		

		//写事件
		eventProvider.createEvent(ConstantDeclare.EVENT.PACTADJUST, tranAmt, lnsAcctInfo, oppAcctInfo, 
								fundInvest, briefCode, ctx,"","",dyninfo.getCusttype());
        	
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
}


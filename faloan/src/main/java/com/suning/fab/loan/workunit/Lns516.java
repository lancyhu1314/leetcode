
package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author 	
 *
 * @version V1.0.1
 *
// * @see 	调账登记幂等登记簿
 *
 * @param	
 * 			
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns516 extends WorkUnit {

	String serialNo;
	String briefCode;
	String acctType;
	String toAcctType;
	String acctNo;
	String toAcctNo;
	String receiptNo;
	FabAmount tranAmt;
	String tellerCode;
	String tunnelData;

	@Override
	public void run() throws Exception{
		
		TranCtx ctx = getTranctx();
		
		
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setSerialno(serialNo);
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode(ctx.getTranCode());
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setAcctname(briefCode);
		lnsinterface.setUserno(receiptNo);
		lnsinterface.setAcctno("");
		lnsinterface.setTranamt(tranAmt.getVal());
		lnsinterface.setMagacct("");
		lnsinterface.setOrgid("");
		lnsinterface.setBankno("");
		lnsinterface.setReserv3(acctType);
		lnsinterface.setReserv4(toAcctType);
		lnsinterface.setReserv5(acctNo);
		lnsinterface.setReserv6(toAcctNo);

		
		try{
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
		} catch(FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				LoggerUtil.info("交易幂等ACCTNO["+receiptNo+"]");
				throw new FabException(e,TRAN.IDEMPOTENCY);
			}
			else
				throw new FabException(e, "SPS100", "lnsinterface");
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

	
	
}

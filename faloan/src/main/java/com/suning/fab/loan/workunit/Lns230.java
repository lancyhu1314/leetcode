package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 非标利息减免
 *
 * @param serialNo 业务流水号
 * @param acctNo 贷款账号
 * @param intAmt 利息减免金额
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns230 extends WorkUnit { 

	String serialNo;
	String	acctNo;
	FabAmount intAmt;
	FabAmount forfeitAmt;
	LnsBillStatistics billStatistics;
	Integer subNo;
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		if(VarChecker.isEmpty(intAmt)&&VarChecker.isEmpty(forfeitAmt)){
			throw new FabException("LNS035");
		}
		//插幂等登记簿
		
		//利息减免开始
		if(VarChecker.isEmpty(intAmt)||intAmt.isZero()){
			return;
		}
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		TblLnsbasicinfo lnsbasicinfo;
		Map<String, Object> basicParam = new HashMap<String, Object>();
		basicParam.put("acctno", acctNo);
		basicParam.put("openbrc", ctx.getBrc());
		try{
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", basicParam, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if(null == lnsbasicinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		
		Map<String, Object> billParam = new HashMap<String, Object>();
		billParam.put("acctno", acctNo);
		billParam.put("brc", ctx.getBrc());
		List<TblLnsbill> billList;
		try{
			billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_nint", billParam, TblLnsbill.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbill");
		}
		
		if(null == billList){
			throw new FabException("LNS031");
		}
		
		// 小本小息
		billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		Map<String,Object> upbillmap = new HashMap<String, Object>();
		for(TblLnsbill lnsbill : billList)
		{

            LoggerUtil.debug("LNSBILL:"+lnsbill.getAcctno()+"|"+lnsbill.getPeriod()+"|"+lnsbill.getBillstatus()+"."+lnsbill.getBilltype()+"|"+lnsbill.getBillbal());

            upbillmap.put("actrandate", ctx.getTranDate());
        	upbillmap.put("tranamt", intAmt.getVal());
        	
        	upbillmap.put("trandate", lnsbill.getTrandate());
    		upbillmap.put("serseqno", lnsbill.getSerseqno());
    		upbillmap.put("txseq", lnsbill.getTxseq());
			
    		Map<String, Object> repaymap;
    		try{
    			repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_repay", upbillmap);
    		}catch(FabSqlException e){
    			throw new FabException(e, "SPS103", "lnsbill");
    		}
			if(repaymap == null)
			{
				throw new FabException("SPS104", "lnsbill");
			}
			FabAmount minAmt = new FabAmount(Double.valueOf(repaymap.get("minamt").toString()));
			LoggerUtil.debug("minAmt:" + minAmt.getVal());
			if(minAmt.isZero())
			{
				LoggerUtil.debug("该账单金额已经为零");
				continue;
			}
    		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),lnsbill.getBilltype(), lnsbill.getBillstatus(), new FabCurrency());
			lnsAcctInfo.setCancelFlag(lnsbill.getCancelflag());
    		List<FabAmount> amtList = new ArrayList<FabAmount>();
			amtList.add(TaxUtil.calcVAT(minAmt));
    		sub.operate(lnsAcctInfo, null, minAmt, la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXJM, lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), ctx);
   			
   			
   			if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_NINT, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
				lnsOpAcctInfo.setCancelFlag(lnsbill.getCancelflag());
				eventProvider.createEvent(ConstantDeclare.EVENT.NINTRETURN, minAmt, lnsAcctInfo, lnsOpAcctInfo,
						la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXZH, ctx, null, lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
	   			eventProvider.createEvent(ConstantDeclare.EVENT.REDUCENINT, minAmt, lnsOpAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXJM, ctx, amtList, lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());

			}else {
				eventProvider.createEvent(ConstantDeclare.EVENT.REDUCENINT, minAmt, lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXJM, ctx, amtList, lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
			}
			//利息减免
			AccountingModeChange.saveReduceTax(ctx,acctNo , minAmt ,lnsbill);
   			intAmt.selfSub(minAmt);
        	
        	if(!intAmt.isPositive())
	        {	
	        	break;
	        }    
		}
		if(intAmt.isPositive()){
			List<RepayAdvance> nintAdvance = new ArrayList<RepayAdvance>();
			List<RepayAdvance> prinAdvance = new ArrayList<RepayAdvance>();
			FabAmount newRemainAmt;
			//prePayFlag标志用于处理每期从合同起息日计息的情况 辅助表的repayterm等于0时候赋值1
			if( ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway()) )
			{
				newRemainAmt = LoanInterestSettlementUtil.nonStdIntDeduction(la, ctx,  lnsbasicinfo, intAmt, prinAdvance, nintAdvance, billStatistics);
			}
			else
			{
				newRemainAmt = LoanInterestSettlementUtil.nonStdIntDedu(la, ctx, subNo, lnsbasicinfo, intAmt, prinAdvance, nintAdvance, billStatistics, "ONLYINT");
			}
				
			if( !newRemainAmt.isZero())
			{
				throw new FabException("LNS033");
			}	
			//等本等息可以减免未来期利息，nintAdvance可能存在多条
			/*if(nintAdvance.size()>1){
				throw new FabException("LNS033");
			}*/
			for(RepayAdvance nint:nintAdvance){
				List<FabAmount> amtList = new ArrayList<FabAmount>();
				amtList.add(TaxUtil.calcVAT(nint.getBillAmt()));
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_NINT, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				lnsAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat()) ? "3" : "1");
	    		sub.operate(lnsAcctInfo, null, nint.getBillAmt(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXJM, nint.getBillTrandate(), nint.getBillSerseqno(), nint.getBillTxseq(), ctx);
	   			eventProvider.createEvent(ConstantDeclare.EVENT.REDUCENINT, nint.getBillAmt(), lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXJM, ctx, amtList, nint.getBillTrandate(), nint.getBillSerseqno(), nint.getBillTxseq(), la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
				//利息减免
				AccountingModeChange.saveReduceTax(ctx,acctNo , ConstantDeclare.BILLTYPE.BILLTYPE_NINT,nint);
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
	 * @return the intAmt
	 */
	public FabAmount getIntAmt() {
		return intAmt;
	}
	/**
	 * @param intAmt the intAmt to set
	 */
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}
	/**
	 * @return the forfeitAmt
	 */
	public FabAmount getForfeitAmt() {
		return forfeitAmt;
	}
	/**
	 * @param forfeitAmt the forfeitAmt to set
	 */
	public void setForfeitAmt(FabAmount forfeitAmt) {
		this.forfeitAmt = forfeitAmt;
	}
	/**
	 * @return the billStatistics
	 */
	public LnsBillStatistics getBillStatistics() {
		return billStatistics;
	}
	/**
	 * @param billStatistics the billStatistics to set
	 */
	public void setBillStatistics(LnsBillStatistics billStatistics) {
		this.billStatistics = billStatistics;
	}
	/**
	 * @return the subNo
	 */
	public Integer getSubNo() {
		return subNo;
	}
	/**
	 * @param subNo the subNo to set
	 */
	public void setSubNo(Integer subNo) {
		this.subNo = subNo;
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


}


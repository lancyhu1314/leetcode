package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
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
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see -利息减免
 *
 * @param -serialNo 业务流水号
 * @param -acctNo 贷款账号
 * @param -reduceIntAmt 利息减免金额
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns247 extends WorkUnit { 

	String serialNo;
	String	acctNo;
	FabAmount reduceIntAmt;
	FabAmount reduceFintAmt;
	LnsBillStatistics billStatistics;
	Integer subNo;
	FabAmount sumdelint;
	String settleFlag;
	FabAmount fnintRed = new FabAmount();
	FabAmount autoNintRed = new FabAmount();
	LoanAgreement loanAgreement;
	String  intReduceList;  //利息减免明细
	
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired LoanEventOperateProvider eventProvider;
	private boolean ifOverdue = false;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
//		if(VarChecker.isEmpty(reduceIntAmt)&&VarChecker.isEmpty(reduceFintAmt)){
//			throw new FabException("LNS035");
//		}
		Map<String, Map<String,FabAmount>> repayAmtList = new HashMap<String, Map<String,FabAmount>>();
		if( !VarChecker.isEmpty(reduceIntAmt))
			sumdelint = new FabAmount(reduceIntAmt.getVal()) ;
		
		if( autoNintRed.isPositive() )
		{
			sumdelint=autoNintRed;
			reduceIntAmt=sumdelint;  
		}
		//利息减免开始
		if(VarChecker.isEmpty(reduceIntAmt)||reduceIntAmt.isZero()){
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
		
		Integer repayterm = 0;
		// 小本小息
		/**
		 * 6	本金	利息	罚息	复利
		 * 7	本金	利息	罚息	复利
		 * 8	本金	利息	罚息	复利
		 * 小本小息:先横后竖	期次->金额->时间
		 * 大本大息:先竖后横	金额->期次->时间
		 * 20190517|14050183
		 */
		billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		Map<String,Object> upbillmap = new HashMap<String, Object>();
		Map<String, Object> rpyPlanMap = new HashMap<String, Object>();

		for(TblLnsbill lnsbill : billList)
		{
			//如果有逾期或者罚息账本 则还款为逾期还款  add at 2019-03-25
			if(!ifOverdue &&lnsbill.isOverdue())
				ifOverdue = true;
			//add end

            LoggerUtil.debug("LNSBILL:"+lnsbill.getAcctno()+"|"+lnsbill.getPeriod()+"|"+lnsbill.getBillstatus()+"."+lnsbill.getBilltype()+"|"+lnsbill.getBillbal());

            upbillmap.put("actrandate", ctx.getTranDate());
        	upbillmap.put("tranamt", reduceIntAmt.getVal());
        	
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
			LoanRpyInfoUtil.addRpyList(repayAmtList, lnsbill.getBilltype(), lnsbill.getPeriod().toString(), minAmt,lnsbill.getBillstatus());
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
   			repayterm = repayterm>lnsbill.getPeriod()?repayterm:lnsbill.getPeriod();
   			//更新lnsRpyPlan
			rpyPlanMap.put("repayterm", lnsbill.getPeriod());//期数
			rpyPlanMap.put("acctno", acctNo);//期数
			rpyPlanMap.put("intamt", minAmt.getVal());//罚息减免金额
			try {
				DbAccessUtil.execute(
						"CUSTOMIZE.update_lnsrpyplan_intamt", rpyPlanMap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "update_lnsrpyplan_intamt");
			}
   			
   			
   			
   			reduceIntAmt.selfSub(minAmt);
        	
        	if(!reduceIntAmt.isPositive())
	        {	
	        	break;
	        }    
		}
		
		//未来期的利息减免 在未来期减免
		reduceIntAmt.selfAdd(fnintRed);
		if(reduceIntAmt.isPositive()){
			List<RepayAdvance> nintAdvance = new ArrayList<RepayAdvance>();
			List<RepayAdvance> prinAdvance = new ArrayList<RepayAdvance>();
			List<RepayAdvance> feeAdvance = new ArrayList<RepayAdvance>();
			//等本等息 利息减免
			if(ConstantDeclare.REPAYWAY.isEqualInterest(la.getWithdrawAgreement().getRepayWay()))
				LoanInterestSettlementUtil.dbdxInterestDeduction(la, ctx, lnsbasicinfo, reduceIntAmt, prinAdvance, nintAdvance, billStatistics);
			else
			{
				//房抵贷利息减免
				/**
				 * settleFlag:1-结清
				 * prdId:4010001-P2P
				 * 20190517|14050183
				 */
				if( "4010001".equals(la.getPrdId()) && !"1".equals(settleFlag))
					throw new FabException("LNS064");
				if("4010001".equals(la.getPrdId()) )	
				{
					LoanInterestSettlementUtil.feeRepaymentBill(la, ctx, subNo, lnsbasicinfo, reduceIntAmt, prinAdvance, nintAdvance, feeAdvance, billStatistics, "ONLYINT");
				}
				//非标的利息减免
				else if("471009".equals(ctx.getTranCode())) {
					FabAmount newRemainAmt  = LoanInterestSettlementUtil.nonStdIntDedu(la, ctx, subNo, lnsbasicinfo, reduceIntAmt, prinAdvance, nintAdvance, billStatistics, "ONLYINT");
					if( !newRemainAmt.isZero())
						throw new FabException("LNS033");

				}else{
					//正常贷款 非等本等息
					LoanInterestSettlementUtil.interestRepaymentBill(la, ctx, subNo, lnsbasicinfo, reduceIntAmt, prinAdvance, nintAdvance, billStatistics, "ONLYINT","1","");
				}
			}
			
			//等本等息可以减免未来期利息，nintAdvance可能存在多条
			/*if(nintAdvance.size()>1){
				throw new FabException("LNS033");
			}*/
			for(RepayAdvance nint:nintAdvance){
				List<FabAmount> amtList = new ArrayList<FabAmount>();
				amtList.add(TaxUtil.calcVAT(nint.getBillAmt()));
				LoanRpyInfoUtil.addRpyList(repayAmtList, "NINT", nint.getRepayterm().toString(), nint.getBillAmt(),nint.getBillStatus());
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_NINT, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				lnsAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat()) ? "3" : "1");
	    		sub.operate(lnsAcctInfo, null, nint.getBillAmt(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXJM, nint.getBillTrandate(), nint.getBillSerseqno(), nint.getBillTxseq(), ctx);
	   			eventProvider.createEvent(ConstantDeclare.EVENT.REDUCENINT, nint.getBillAmt(), lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXJM, ctx, amtList, nint.getBillTrandate(), nint.getBillSerseqno(), nint.getBillTxseq(), la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
				//利息减免
				AccountingModeChange.saveReduceTax(ctx,acctNo , ConstantDeclare.BILLTYPE.BILLTYPE_NINT,nint);
	   			//更新lnsRpyPlan
	   			repayterm++;
	   			rpyPlanMap.put("repayterm", repayterm);//期数
				rpyPlanMap.put("acctno", acctNo);//期数
				rpyPlanMap.put("intamt", nint.getBillAmt().getVal());//罚息减免金额
				try {
					DbAccessUtil.execute(
							"CUSTOMIZE.update_lnsrpyplan_intamt", rpyPlanMap);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS103", "update_lnsrpyplan_intamt");
				}
			}			
			
		}
		intReduceList = LoanRpyInfoUtil.getRepayList(acctNo, tranctx, repayAmtList,la);
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
	 * @return the reduceIntAmt
	 */
	public FabAmount getReduceIntAmt() {
		return reduceIntAmt;
	}
	/**
	 * @param reduceIntAmt the reduceIntAmt to set
	 */
	public void setReduceIntAmt(FabAmount reduceIntAmt) {
		this.reduceIntAmt = reduceIntAmt;
	}
	/**
	 * @return the reduceFintAmt
	 */
	public FabAmount getReduceFintAmt() {
		return reduceFintAmt;
	}
	/**
	 * @param reduceFintAmt the reduceFintAmt to set
	 */
	public void setReduceFintAmt(FabAmount reduceFintAmt) {
		this.reduceFintAmt = reduceFintAmt;
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
	/**
	 * 
	 * @return the sumdelint
	 */
	public FabAmount getSumdelint() {
		return sumdelint;
	}
	/**
	 * @param sumdelint the sumdelint to set
	 */
	public void setSumdelint(FabAmount sumdelint) {
		this.sumdelint = sumdelint;
	}
	/**
	 * 
	 * @return the settleFlag
	 */
	public String getSettleFlag() {
		return settleFlag;
	}
	/**
	 * @param settleFlag the settleFlag to set
	 */
	public void setSettleFlag(String settleFlag) {
		this.settleFlag = settleFlag;
	}
	/**
	 * 
	 * @return the fnintRed
	 */
	public FabAmount getFnintRed() {
		return fnintRed;
	}
	/**
	 * @param fnintRed the fnintRed to set
	 */
	public void setFnintRed(FabAmount fnintRed) {
		this.fnintRed = fnintRed;
	}
	/**
	 * @return  ifOverdue
	 */
	public boolean getIfOverdue() {
		return ifOverdue;
	}
	/**
	 * @param ifOverdue the ifOverdue to set
	 */
	public void setIfOverdue(boolean ifOverdue) {
		this.ifOverdue = ifOverdue;
	}
	/**
	 * @return the autoNintRed
	 */
	public FabAmount getAutoNintRed() {
		return autoNintRed;
	}
	/**
	 * @param autoNintRed the autoNintRed to set
	 */
	public void setAutoNintRed(FabAmount autoNintRed) {
		this.autoNintRed = autoNintRed;
	}
	public String getIntReduceList() {
		return intReduceList;
	}
	public void setIntReduceList(String intReduceList) {
		this.intReduceList = intReduceList;
	}
	
}


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
 * @see 罚息减免
 *
 * @param serialNo
 *            业务流水号
 * @param acctNo
 *            贷款账号
 * @param reduceFintAmt
 *            罚息减免金额
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns248 extends WorkUnit {

	String serialNo;
	String acctNo;
	FabAmount reduceFintAmt;
	FabAmount sumdelfint;
	String  fintReduceList;  //罚息减免明细
	
	FabAmount autoFNintRed = new FabAmount();
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	LoanEventOperateProvider eventProvider;
	//是否逾期
	private boolean  ifOverdue;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		Map<String, Map<String,FabAmount>> repayAmtList = new HashMap<String, Map<String,FabAmount>>();
		if( !VarChecker.isEmpty(reduceFintAmt))
			sumdelfint = new FabAmount(reduceFintAmt.getVal());
		
		if( autoFNintRed.isPositive()){
			sumdelfint=autoFNintRed;
			reduceFintAmt=sumdelfint;
		}
		

		if (VarChecker.isEmpty(reduceFintAmt) || reduceFintAmt.isZero()) {
			return;
		}
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo,
				ctx);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());
		List<TblLnsbill> billList;
		try {
			billList = DbAccessUtil.queryForList(
					"CUSTOMIZE.query_lnsbill_dint", param, TblLnsbill.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}

		if (null == billList) {
			throw new FabException("LNS032");
		}

		// 小本小息
		billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		Map<String, Object> upbillmap = new HashMap<String, Object>();
		Map<String, Object> rpyPlanMap = new HashMap<String, Object>();
		for (TblLnsbill lnsbill : billList) {
			LoggerUtil.debug("LNSBILL:" + lnsbill.getAcctno() + "|"
					+ lnsbill.getPeriod() + "|" + lnsbill.getBillstatus() + "."
					+ lnsbill.getBilltype() + "|" + lnsbill.getBillbal());
			//如果有逾期或者罚息账本 则还款为逾期还款  add at 2019-03-25
			if(!ifOverdue &&lnsbill.isOverdue())
				ifOverdue = true;
			//add end
			upbillmap.put("actrandate", ctx.getTranDate());
			upbillmap.put("tranamt", reduceFintAmt.getVal());

			upbillmap.put("trandate", lnsbill.getTrandate());
			upbillmap.put("serseqno", lnsbill.getSerseqno());
			upbillmap.put("txseq", lnsbill.getTxseq());

			Map<String, Object> repaymap;
			try {
				repaymap = DbAccessUtil.queryForMap(
						"CUSTOMIZE.update_lnsbill_repay", upbillmap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsbill");
			}
			if (repaymap == null) {
				throw new FabException("SPS104", "lnsbill");
			}
			FabAmount minAmt = new FabAmount(Double.valueOf(repaymap.get(
					"minamt").toString()));
			LoggerUtil.debug("minAmt:" + minAmt.getVal());
			if (minAmt.isZero()) {
				LoggerUtil.debug("该账单金额已经为零");
				continue;
			}
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),
					lnsbill.getBilltype(), lnsbill.getBillstatus(),
					new FabCurrency());
			lnsAcctInfo.setCancelFlag(lnsbill.getCancelflag());
			List<FabAmount> amtList = new ArrayList<FabAmount>();
			amtList.add(TaxUtil.calcVAT(minAmt));
			LoanRpyInfoUtil.addRpyList(repayAmtList, lnsbill.getBilltype(), lnsbill.getPeriod().toString(), minAmt,lnsbill.getBillstatus());

			sub.operate(lnsAcctInfo, null, minAmt, la.getFundInvest(),
					ConstantDeclare.BRIEFCODE.FXJM, lnsbill.getTrandate()
							.toString(), lnsbill.getSerseqno(), lnsbill
							.getTxseq(), ctx);
			eventProvider.createEvent(ConstantDeclare.EVENT.REDUDEFINT, minAmt,
					lnsAcctInfo, null, la.getFundInvest(),
					ConstantDeclare.BRIEFCODE.FXJM, ctx, amtList, lnsbill
							.getTrandate().toString(), lnsbill.getSerseqno(),
					lnsbill.getTxseq(), la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
			//罚息减免
			AccountingModeChange.saveReduceTax(ctx,acctNo , minAmt ,lnsbill);
			reduceFintAmt.selfSub(minAmt);

			//更新lnsRpyPlan
			rpyPlanMap.put("repayterm", lnsbill.getPeriod());//期数
			rpyPlanMap.put("acctno", acctNo);//期数
			rpyPlanMap.put("sumrfint", minAmt.getVal());//罚息减免金额
			try {
				 DbAccessUtil.execute(
						"CUSTOMIZE.update_lnsrpyplan_sumrfint", rpyPlanMap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "update_lnsrpyplan_sumrfint");
			}
			
			
			if (!reduceFintAmt.isPositive()) {
				break;
			}
		}
		if (reduceFintAmt.isPositive()) {
			throw new FabException("LNS032");
		}
		fintReduceList = LoanRpyInfoUtil.getRepayList(acctNo, tranctx, repayAmtList,la);
		
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
	 * @return the reduceFintAmt
	 */
	public FabAmount getReduceFintAmt() {
		return reduceFintAmt;
	}

	/**
	 * @param reduceFintAmt
	 *            the reduceFintAmt to set
	 */
	public void setReduceFintAmt(FabAmount reduceFintAmt) {
		this.reduceFintAmt = reduceFintAmt;
	}

	/**
	 * @return the sub
	 */
	public AccountOperator getSub() {
		return sub;
	}

	/**
	 * @param sub
	 *            the sub to set
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
	 * @param eventProvider
	 *            the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}

	/**
	 * 
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
	 * 
	 * @return the sumdelfint
	 */
	public FabAmount getSumdelfint() {
		return sumdelfint;
	}

	/**
	 * @param sumdelfint the sumdelfint to set
	 */
	public void setSumdelfint(FabAmount sumdelfint) {
		this.sumdelfint = sumdelfint;
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
	 * @return the autoFNintRed
	 */
	public FabAmount getAutoFNintRed() {
		return autoFNintRed;
	}

	/**
	 * @param autoFNintRed the autoFNintRed to set
	 */
	public void setAutoFNintRed(FabAmount autoFNintRed) {
		this.autoFNintRed = autoFNintRed;
	}

	public String getFintReduceList() {
		return fintReduceList;
	}

	public void setFintReduceList(String fintReduceList) {
		this.fintReduceList = fintReduceList;
	}
	
	
}

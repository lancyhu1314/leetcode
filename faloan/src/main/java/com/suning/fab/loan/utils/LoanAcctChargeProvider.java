package com.suning.fab.loan.utils;
/**
 * @author TT.Y
 * @version V1.1.0
 * 提供utils调用的账务操作
 *
 * 账户加钱+登记事件
 * @param add(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String eventCode, String briefCode, List<FabAmount> amtList)
 *
 * 登记事件
 * @param event(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String eventCode, String briefCode, FabAmount tranAmt)
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.ServiceFactory;
import com.suning.fab.tup4j.utils.VarChecker;

@Component
public class LoanAcctChargeProvider {

	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;

	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;

	@Autowired
	LoanEventOperateProvider eventProvider;

	/**
	 * 账户加钱+登记事件操作
	 * 账单类型为宽限期或者复利时，强转为罚息(与C版本保持一致)
	 * @param lnsBill    账单数据结构
	 * @param ctx    交易信息上下文
	 * @param loanAgreement    贷款协议信息
	 * @param eventCode    事件代码
	 * @param briefCode    摘要代码
	 *
	 * @throws FabException
	 */
	//动态表加余额
	public static void add(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String eventCode, String briefCode) throws FabException{
		String billType;
		String reserv1 = loanAgreement.getCustomer().getMerchantNo();
		String childBrc ="";
		if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
				ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(lnsBill.getBillType()))
		{
			billType = ConstantDeclare.BILLTYPE.BILLTYPE_DINT;
		}
		else if( LoanFeeUtils.isFeeType(lnsBill.getBillType()) )
		{
			billType = ConstantDeclare.BILLTYPE.BILLTYPE_FEEA;
			reserv1 = 	LoanFeeUtils.matchFeeInfo(loanAgreement, lnsBill.getBillType(),lnsBill.getRepayWay() ).getFeebrc();
			childBrc = reserv1;
		}
		else
		{
			//当类型是担保费违约金时
			if(Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DBWY).contains(lnsBill.getBillType())&&Arrays.asList("2512643","2512634","2512641","2412638","2412641","2412640","2412639").contains(loanAgreement.getPrdId())){
				reserv1 = 	LoanFeeUtils.matchFeeInfo(loanAgreement, ConstantDeclare.BILLTYPE.BILLTYPE_SQFE,lnsBill.getRepayWay() ).getFeebrc();
				childBrc = reserv1;
				billType=ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEED;
			}else{
				billType =lnsBill.getBillType();
			}

		}
		List<FabAmount> amtList = new ArrayList<>();

		amtList.add(TaxUtil.calcVAT(lnsBill.getBillAmt()));
		FundInvest fundInvest = new FundInvest(loanAgreement.getFundInvest().getInvestee(),
				loanAgreement.getFundInvest().getInvestMode(),
				loanAgreement.getFundInvest().getChannelType(),
				loanAgreement.getFundInvest().getFundChannel(),
				loanAgreement.getFundInvest().getOutSerialNo());
		LnsAcctInfo acctinfo = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(),
				billType,
				lnsBill.getBillStatus(),
				new FabCurrency(),
				childBrc);
		acctinfo.setCancelFlag(lnsBill.getCancelFlag());
		ServiceFactory.getBean(LoanAcctChargeProvider.class).add.operate(acctinfo, null, lnsBill.getBillAmt(), fundInvest, briefCode, lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ctx);
		ServiceFactory.getBean(LoanAcctChargeProvider.class).eventProvider.createEvent(eventCode,
				lnsBill.getBillAmt(),
				acctinfo,
				null,
				fundInvest,
				briefCode,
				ctx,
				amtList,
				lnsBill.getTranDate().toString(),
				lnsBill.getSerSeqno(),
				lnsBill.getTxSeq(), reserv1,
				loanAgreement.getBasicExtension().getDebtCompany());
	}

	public static void sub(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String eventCode, String briefCode, List<FabAmount> amtList) throws FabException{
		String billType;
		if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
				ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(lnsBill.getBillType()))
		{
			billType = ConstantDeclare.BILLTYPE.BILLTYPE_DINT;
		}
		else if(LoanFeeUtils.isFeeType(lnsBill.getBillType()) )
		{
			billType = ConstantDeclare.BILLTYPE.BILLTYPE_FEEA;
		}
		else
		{
			billType =lnsBill.getBillType();
		}

		FundInvest fundInvest = new FundInvest(loanAgreement.getFundInvest().getInvestee(),
				loanAgreement.getFundInvest().getInvestMode(),
				loanAgreement.getFundInvest().getChannelType(),
				loanAgreement.getFundInvest().getFundChannel(),
				loanAgreement.getFundInvest().getOutSerialNo());
		LnsAcctInfo acctinfo = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(),
				billType,
				lnsBill.getBillStatus(),
				new FabCurrency());
		acctinfo.setCancelFlag(lnsBill.getCancelFlag());
		ServiceFactory.getBean(LoanAcctChargeProvider.class).sub.operate(acctinfo, null, lnsBill.getBillAmt(), fundInvest, briefCode, lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ctx);
		ServiceFactory.getBean(LoanAcctChargeProvider.class).eventProvider.createEvent(eventCode,
				lnsBill.getBillAmt(),
				acctinfo,
				null,
				fundInvest,
				briefCode,
				ctx,
				amtList,
				lnsBill.getTranDate().toString(),
				lnsBill.getSerSeqno(),
				lnsBill.getTxSeq());
	}


	/**
	 * 登记事件信息
	 *
	 * @param lnsBill    账单数据结构
	 * @param ctx    交易信息上下文
	 * @param loanAgreement    贷款协议信息
	 * @param eventCode    事件代码
	 * @param briefCode    摘要代码
	 * @param tranAmt    发生额
	 * @throws FabException
	 */
	public static void event(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String eventCode, String briefCode, FabAmount tranAmt) throws FabException{
		String billType;
		if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
				ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
				.contains(lnsBill.getBillType())) {
			billType = ConstantDeclare.BILLTYPE.BILLTYPE_DINT;
		}
		else if( LoanFeeUtils.isFeeType(lnsBill.getBillType()) )
		{
			billType = ConstantDeclare.BILLTYPE.BILLTYPE_FEEA;
		}
		else {
			billType = lnsBill.getBillType();
		}

		FundInvest fundInvest = new FundInvest(loanAgreement.getFundInvest().getInvestee(),
				loanAgreement.getFundInvest().getInvestMode(),
				loanAgreement.getFundInvest().getChannelType(),
				loanAgreement.getFundInvest().getFundChannel(),
				loanAgreement.getFundInvest().getOutSerialNo());
		LnsAcctInfo acctinfo = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(),
				billType,
				lnsBill.getBillStatus(),
				new FabCurrency());
		acctinfo.setCancelFlag(lnsBill.getCancelFlag());
		ServiceFactory.getBean(LoanAcctChargeProvider.class).eventProvider.createEvent(eventCode,
				tranAmt,
				acctinfo,
				null,
				fundInvest,
				briefCode,
				ctx,
				null,
				lnsBill.getTranDate().toString(),
				lnsBill.getSerSeqno(),
				lnsBill.getTxSeq(),
				loanAgreement.getCustomer().getMerchantNo(),
				loanAgreement.getBasicExtension().getDebtCompany());
	}
}


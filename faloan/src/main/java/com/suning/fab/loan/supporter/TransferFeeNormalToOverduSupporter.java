package com.suning.fab.loan.supporter;

import com.suning.fab.loan.utils.LoanFeeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 正常费用转逾期费用
 *
 * @return
 *
 * @exception
 */
public  class TransferFeeNormalToOverduSupporter extends TransferSupporter{
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void transfer(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String statusbDate) throws FabException{
		if( lnsBill.getBillBal().isPositive()) {
			String childBrc = LoanFeeUtils.matchFeeInfo(loanAgreement, lnsBill.getBillType(), lnsBill.getRepayWay()).getFeebrc();
			FundInvest fundInvest = fundInvestInit(loanAgreement);
			LnsAcctInfo normal = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(),
					ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
					new FabCurrency(), childBrc);
			LnsAcctInfo overdue = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(),
					ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,
					new FabCurrency(), childBrc);
			sub.operate(normal, null, lnsBill.getBillBal(), fundInvest, LoanFeeUtils.feeTransferBrief(lnsBill), lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ctx);
			add.operate(overdue, null, lnsBill.getBillBal(), fundInvest, LoanFeeUtils.feeTransferBrief(lnsBill), lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ctx);
			eventProvider.createEvent(ConstantDeclare.EVENT.FEETRNSFER,
					lnsBill.getBillBal(),
					normal,
					overdue,
					fundInvest,
					LoanFeeUtils.feeTransferBrief(lnsBill),
					ctx,
					null,
					lnsBill.getTranDate().toString(),
					lnsBill.getSerSeqno(),
					lnsBill.getTxSeq(),
					childBrc,
					loanAgreement.getBasicExtension().getDebtCompany());

			billStatusUpdate(lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, lnsBill.getCurendDate());
		}
	}

}  
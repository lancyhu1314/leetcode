package com.suning.fab.loan.supporter;

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
 * @see 逾期本金转呆滞本金
 * 
 * @return
 *
 * @exception
 */
public  class TransferPrinOverduToLanguishmentSupporter extends TransferSupporter{  
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void transfer(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String statusbDate) throws FabException{
		FundInvest fundInvest = fundInvestInit(loanAgreement);
		LnsAcctInfo overdu = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(),
											 lnsBill.getBillType(), 
											 ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, 
											 new FabCurrency());
		LnsAcctInfo languishment = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(),
											  lnsBill.getBillType(), 
											  ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT, 
											  new FabCurrency());
		sub.operate(overdu, null, lnsBill.getBillBal(), fundInvest, ConstantDeclare.BRIEFCODE.BJZL, lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ctx);
		add.operate(languishment, null, lnsBill.getBillBal(), fundInvest, ConstantDeclare.BRIEFCODE.BJZL, lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ctx);
		eventProvider.createEvent(ConstantDeclare.EVENT.LNTRANSFER, 
											   lnsBill.getBillBal(),
											   overdu, 
											   languishment, 
											   fundInvest,
											   ConstantDeclare.BRIEFCODE.BJZL, 
											   ctx,
											   null,
											   lnsBill.getTranDate().toString(), 
											   lnsBill.getSerSeqno(), 
											   lnsBill.getTxSeq(),
											   loanAgreement.getCustomer().getMerchantNo(),
											   loanAgreement.getBasicExtension().getDebtCompany());
		billStatusUpdate(lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT, statusbDate);
		
	}
	
}  
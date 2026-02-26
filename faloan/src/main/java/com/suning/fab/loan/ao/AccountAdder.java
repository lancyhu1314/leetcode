package com.suning.fab.loan.ao;

import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAcctOperateProvider;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.stereotype.Component;

import com.suning.fab.loan.account.LnsAcctChange;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.account.AcctOperationType;
import com.suning.fab.tup4j.amount.Amount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;

/**
 * @author 14060269
 * 加款操作
 */
@Component("accountAdder")
public class AccountAdder implements AccountOperator {

	AccountAdder() {
	}
	@Override
	public void operate(LnsAcctInfo acctInfo, LnsAcctInfo oppAcctInfo, Amount amt, FundInvest fundInvest, String briefCode,   TranCtx ctx) throws FabException 
	{
		if ( !("1".equals(acctInfo.getCancelFlag()) &&
				VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat","") ))) {
			LoanAcctOperateProvider.changeLoanStat(acctInfo);
			LoanAcctOperateProvider.changeLoanStat(oppAcctInfo);
		}
		ctx.appendOperation(acctInfo, 
				new LnsAcctChange(AcctOperationType.PLUS, amt, briefCode, 
						ctx.getSerSeqNo(), ctx.getTranCode(), oppAcctInfo, 
						fundInvest,null,null,null));
		
	}
	@Override
	public void operate(LnsAcctInfo acctInfo, LnsAcctInfo oppAcctInfo, Amount amt, FundInvest fundInvest, String briefCode,  String billTranDate,Integer billSerSeqno,Integer billTxnSeq,TranCtx ctx) throws FabException 
	{
		if ( !("1".equals(acctInfo.getCancelFlag()) &&
				VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat","") ))) {
			LoanAcctOperateProvider.changeLoanStat(acctInfo);
			LoanAcctOperateProvider.changeLoanStat(oppAcctInfo);
		}
		ctx.appendOperation(acctInfo, 
				new LnsAcctChange(AcctOperationType.PLUS, amt, briefCode, 
						ctx.getSerSeqNo(), ctx.getTranCode(), oppAcctInfo, 
						fundInvest, billTranDate, billSerSeqno, billTxnSeq));
		
	}
}

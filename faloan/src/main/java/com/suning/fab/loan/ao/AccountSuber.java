package com.suning.fab.loan.ao;

import com.suning.fab.loan.account.LnsAcctChange;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAcctOperateProvider;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.tup4j.account.AcctOperationType;
import com.suning.fab.tup4j.amount.Amount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.stereotype.Component;

/**
 * @author 14050269 Howard
 * 减款操作
 */
@Component("accountSuber")
public class AccountSuber implements AccountOperator {

    public AccountSuber() {
        //nothing to do
    }

    @Override
    public void operate(LnsAcctInfo acctInfo, LnsAcctInfo oppAcctInfo, Amount amt, FundInvest fundInvest, String briefCode, TranCtx ctx) throws FabException {
        if ( !("1".equals(acctInfo.getCancelFlag()) &&
                VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat","") ))) {
            LoanAcctOperateProvider.changeLoanStat(acctInfo);
            LoanAcctOperateProvider.changeLoanStat(oppAcctInfo);
        }
        ctx.appendOperation(acctInfo,
                new LnsAcctChange(AcctOperationType.MINUS, amt, briefCode,
                        ctx.getSerSeqNo(), ctx.getTranCode(), oppAcctInfo,
                        fundInvest, null, null, null));
    }

    @Override
    public void operate(LnsAcctInfo acctInfo, LnsAcctInfo oppAcctInfo, Amount amt, FundInvest fundInvest, String briefCode, String billTranDate, Integer billSerSeqno, Integer billTxSeq, TranCtx ctx) throws FabException {
        if ( !("1".equals(acctInfo.getCancelFlag()) &&
                VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get("loanStat","") ))) {
            LoanAcctOperateProvider.changeLoanStat(acctInfo);
            LoanAcctOperateProvider.changeLoanStat(oppAcctInfo);
        }
        ctx.appendOperation(acctInfo,
                new LnsAcctChange(AcctOperationType.MINUS, amt, briefCode,
                        ctx.getSerSeqNo(), ctx.getTranCode(), oppAcctInfo,
                        fundInvest, billTranDate, billSerSeqno, billTxSeq));
    }
}

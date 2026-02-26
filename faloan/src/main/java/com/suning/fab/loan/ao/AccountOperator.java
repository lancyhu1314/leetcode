package com.suning.fab.loan.ao;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.amount.Amount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
public interface AccountOperator {
	void operate(LnsAcctInfo acctNo,LnsAcctInfo oppAcctNo,Amount amt,FundInvest fundInvest,String briefCode, TranCtx ctx) throws FabException;
	void operate(LnsAcctInfo acctNo,LnsAcctInfo oppAcctNo,Amount amt,FundInvest fundInvest,String briefCode, String billTranDate,Integer billSerSeqno,Integer billTxSeq,TranCtx ctx) throws FabException;

}

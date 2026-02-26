package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.domain.TblLnsinterfaceex;
import com.suning.fab.loan.domain.TblLnsprefundaccount;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LH
 * @version V1.0.1
 * <p>
 * 跨库预收账户充退
 * @return
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns263 extends WorkUnit {

    String serialNo;
    String repayAcctNo;
    FabAmount amt;
    String memo;
    String receiptNo;
    String customType;
    String operation;

    ListMap pkgList;

    LoanAgreement loanAgreement;
    FabAmount debtTotalAmt = new FabAmount(0.00);


    @Autowired
    LoanEventOperateProvider eventProvider;

    @Override
    public void run() throws Exception {
        TranCtx ctx = getTranctx();
        String eventCode = ConstantDeclare.EVENT.NRECPREACT;
        ListMap pkgList = ctx.getRequestDict("pkgList");

        if (amt.isNegative() || amt.isZero()) {
            throw new FabException("LNS009");
        }

        if ((VarChecker.isEmpty(repayAcctNo) && VarChecker.isEmpty(pkgList)) ||
                (!VarChecker.isEmpty(repayAcctNo) && !VarChecker.isEmpty(pkgList))) {
            throw new FabException("LNS010");
        }

        // 如果是回滚交易，查询正向交易的幂等表
        if (VarChecker.asList("BAK").contains(operation)) {
            try {
                Map<String, Object> param = new HashMap<>();
                param.put("trancode", "176012");
                param.put("serialno", serialNo);
                TblLnsinterface lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", param, TblLnsinterface.class);
                if (null == lnsinterface) {
                    // 未查询到正向交易的幂等信息，说明，正向交易没有成功，故，不需要做冲销交易，直接返回
                    return;
                }
            } catch (FabSqlException se) {
                throw new FabException(se, "SPS100", "lnsinterface");
            }
        }


        //写幂等登记薄
        TblLnsinterface lnsinterface = new TblLnsinterface();
        //账务日期
        lnsinterface.setTrandate(ctx.getTermDate());
        //幂等流水号
        lnsinterface.setSerialno(ctx.getSerialNo());
        //交易码
        lnsinterface.setTrancode(ctx.getTranCode());
        //自然日期
        lnsinterface.setAccdate(ctx.getTranDate());
        //系统流水号
        lnsinterface.setSerseqno(ctx.getSerSeqNo());
        //网点
        lnsinterface.setBrc(ctx.getBrc());
        //预收账号
        lnsinterface.setUserno(repayAcctNo);
        //开户金额
        lnsinterface.setTranamt(amt.getVal());
        //时间戳
        lnsinterface.setTimestamp(ctx.getTranTime());
        //借据号
        lnsinterface.setMagacct(receiptNo);
        try {
            if (serialNo.getBytes().length > 50) {
                Map<String, Object> param = new HashMap<>();
                param.put("acctno", repayAcctNo);
                param.put("brc", getTranctx().getBrc());
                param.put("key", ConstantDeclare.KEYNAME.MDLS);
                TblLnsinterfaceex interfaceex;
                try {
                    interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", param, TblLnsinterfaceex.class);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS103", "query_lnsinterfaceex");
                }
                if (null != interfaceex && interfaceex.getSerseqno().toString().equals(serialNo))
                    throw new FabException( ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
                else{
                    AccountingModeChange.saveInterfaceEx(ctx, repayAcctNo, ConstantDeclare.KEYNAME.MDLS, "幂等流水", serialNo);
                }
            }
            else
                DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
        } catch (FabSqlException e) {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            } else {
                throw new FabException(e, "SPS100", "lnsinterface");
            }
        }

        //将借据号通过tunneldate字段传递给辅助账户明细表
        ctx.setTunnelData(receiptNo);
        LnsAcctInfo lnsAcctInfo = null;
        FundInvest fundInvest = new FundInvest("", "", "", "", "");
        LnsAcctInfo oppAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT, new FabCurrency());
        List<FabAmount> amtList = new ArrayList<FabAmount>();
        amtList.add(amt);
        //预收户减款
        TblLnsassistdyninfo lnsassistdyninfo = LoanAssistDynInfoUtil.updatePreaccountInfo(ctx, ctx.getBrc(), repayAcctNo, ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT, amt, "sub", customType);
        //预收户余额不足
        if(new FabAmount(lnsassistdyninfo.getCurrbal()).isNegative())
            throw new FabException("LNS019");

        //预收还款事件
        eventProvider.createEvent("LNDKHKYSJK", amt, lnsAcctInfo, oppAcctInfo,
                fundInvest, "YSJK", ctx, amtList,
                //预收和保理客户类型统一放至备用3方便发送事件时转换处理
                lnsassistdyninfo.getCustomid(), "", lnsassistdyninfo.getCusttype(), "", "");
    }

    /**
     * @return the serialNo
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * @param serialNo to set
     */
    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    /**
     * @return the repayAcctNo
     */
    public String getRepayAcctNo() {
        return repayAcctNo;
    }

    /**
     * @param repayAcctNo to set
     */
    public void setRepayAcctNo(String repayAcctNo) {
        this.repayAcctNo = repayAcctNo;
    }

    /**
     * @return the amt
     */
    public FabAmount getAmt() {
        return amt;
    }

    /**
     * @param amt to set
     */
    public void setAmt(FabAmount amt) {
        this.amt = amt;
    }

    /**
     * @return the memo
     */
    public String getMemo() {
        return memo;
    }

    /**
     * @param memo to set
     */
    public void setMemo(String memo) {
        this.memo = memo;
    }

    /**
     * @return the receiptNo
     */
    public String getReceiptNo() {
        return receiptNo;
    }

    /**
     * @param receiptNo to set
     */
    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    /**
     * @return the customType
     */
    public String getCustomType() {
        return customType;
    }

    /**
     * @param customType to set
     */
    public void setCustomType(String customType) {
        this.customType = customType;
    }

    /**
     * @return the pkgList
     */
    public ListMap getPkgList() {
        return pkgList;
    }

    /**
     * @param pkgList to set
     */
    public void setPkgList(ListMap pkgList) {
        this.pkgList = pkgList;
    }

    /**
     * @return the loanAgreement
     */
    public LoanAgreement getLoanAgreement() {
        return loanAgreement;
    }

    /**
     * @param loanAgreement to set
     */
    public void setLoanAgreement(LoanAgreement loanAgreement) {
        this.loanAgreement = loanAgreement;
    }

    /**
     * @return the eventProvider
     */
    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }

    /**
     * @param eventProvider to set
     */
    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }
}
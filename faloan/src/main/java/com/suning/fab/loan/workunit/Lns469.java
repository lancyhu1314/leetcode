package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：保费减免
 *
 * @Author 18049705 MYP
 * @Date Created in 14:46 2019/10/22
 * @see
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns469 extends WorkUnit {
    String serialNo;
    String feeType;
    String acctNo;

    FabAmount reduceDamage;//减免违约金

    FabAmount reduceDamageResult;//减免违约金返回
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;

    //B期缴担保费
    private static String FEETYPEB = "B";

    @Autowired
    LoanEventOperateProvider eventProvider;

    @Override
    public void run() throws Exception {
        reduceDamageResult=new FabAmount(reduceDamage.getVal());
        LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);
        String childBrc = "";
        for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos())
        {
            if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsfeeinfo.getFeestat())){
                childBrc = lnsfeeinfo.getFeebrc();
            }
        }
        LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, tranctx.getTranDate(),tranctx);
        //期缴担保费减免
        if(FEETYPEB.equals(feeType)){
            reduce(loanAgreement, lnsBillStatistics,ConstantDeclare.FEETYPE.SQFE,ConstantDeclare.BRIEFCODE.SFJM,childBrc);
        }
    }

    private void reduce(LoanAgreement loanAgreement, LnsBillStatistics lnsBillStatistics, String feeType,String briefCode,String childBrc) throws FabException {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", acctNo);
        param.put("brc", tranctx.getBrc());
        Map<String,Object> tmp = new HashMap<>();
        List<LnsBill> hisbillinfos = new ArrayList<>();
        hisbillinfos.addAll(lnsBillStatistics.getHisBillList());
        hisbillinfos.addAll(lnsBillStatistics.getHisSetIntBillList());
        hisbillinfos.addAll(lnsBillStatistics.getFutureBillInfoList());

        for(LnsBill lnsBill : hisbillinfos){
            if(LoanFeeUtils.isPenalty(lnsBill.getBillType().trim())){
                Double minAmt = LoanFeeUtils.repaysql(reduceDamage,tmp, lnsBill,tranctx);
                reduceDamage.selfSub(minAmt);
                //还款事件
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEED, lnsBill.getBillStatus(),
                        new FabCurrency(),childBrc);
                lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minAmt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), briefCode, tranctx, childBrc);
                sub.operate(lnsAcctInfo, null, new FabAmount(minAmt), loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DBHK,
                        lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), tranctx);
                if (!reduceDamage.isPositive())
                    break;
            }
        }
        //减免费用 大于所有未还费用
        if (reduceDamage.isPositive()) {
            throw new FabException("LNS058", reduceDamageResult.getVal(),reduceDamageResult.sub(reduceDamage).getVal());
        }
    }


    /**
     * Gets the value of serialNo.
     *
     * @return the value of serialNo
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * Sets the serialNo.
     *
     * @param serialNo serialNo
     */
    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;

    }

    /**
     * Gets the value of feeType.
     *
     * @return the value of feeType
     */
    public String getFeeType() {
        return feeType;
    }

    /**
     * Sets the feeType.
     *
     * @param feeType feeType
     */
    public void setFeeType(String feeType) {
        this.feeType = feeType;

    }

    /**
     * Gets the value of acctNo.
     *
     * @return the value of acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }

    /**
     * Sets the acctNo.
     *
     * @param acctNo acctNo
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;

    }

    public FabAmount getReduceDamage() {
        return reduceDamage;
    }

    public void setReduceDamage(FabAmount reduceDamage) {
        this.reduceDamage = reduceDamage;
    }

    public FabAmount getReduceDamageResult() {
        return reduceDamageResult;
    }

    public void setReduceDamageResult(FabAmount reduceDamageResult) {
        this.reduceDamageResult = reduceDamageResult;
    }

}

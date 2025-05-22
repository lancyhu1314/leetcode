package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.JsonTransfer;
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
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：费用减免
 *
 * @Author 18049705 MYP
 * @Date Created in 10:01 2020/5/11
 * @see
 */
@Scope("prototype")
@Repository
public class Lns533 extends WorkUnit {


    private FabAmount adjustDamage = new FabAmount();//减免违约金
    private FabAmount adjustFee = new FabAmount();
    private String acctNo; //贷款账号
    private Map<String, FabAmount> damageReduceAmts;
    private Map<String, FabAmount> termReduceAmts;//按期计费减免金额
    private Map<String, FabAmount>  dayReduceAmts;//按天计费减免金额
    //实际还款日
    private String  realDate;
    private String repayAcctNo;
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;
    @Autowired
    LoanEventOperateProvider eventProvider;
    @Override
    public void run() throws Exception {

        for (Map.Entry<String, FabAmount> entry : termReduceAmts.entrySet()) {
            adjustFee.selfAdd(entry.getValue());
        }
        for (Map.Entry<String, FabAmount> entry : dayReduceAmts.entrySet()) {
            adjustFee.selfAdd(entry.getValue());
        }
        for (Map.Entry<String, FabAmount> entry : damageReduceAmts.entrySet()) {
            adjustDamage.selfAdd(entry.getValue());
        }
        if (adjustFee.isPositive()||adjustDamage.isPositive()) {
            Map<String, Object> json = new HashMap<>();
            json.put("realDate", realDate);
            if (!dayReduceAmts.isEmpty())
                json.put("dayReduce", dayReduceAmts);
            if (!termReduceAmts.isEmpty())
                json.put("termReduce", termReduceAmts);
            if (!damageReduceAmts.isEmpty())
                json.put("damageReduce", damageReduceAmts);
            //存储减免的费用金额
            AccountingModeChange.saveInterfaceEx(tranctx, acctNo, ConstantDeclare.KEYNAME.ZDJF, "自动减费", JsonTransfer.ToJson(json));
        }else{
            return;
        }




        Map<String, Object> tmp = new HashMap<>();
        LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);

        //查询历史期账本
        // 读取贷款账单
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", acctNo);
        param.put("brc", tranctx.getBrc());
        List<TblLnsbill> billList = LoanDbUtil.queryForList("Lnsbill.query_feebill_repay", param, TblLnsbill.class);

        List<LnsBill> billinfos = new ArrayList<>();
        LnsBillStatistics lnsBillStatistics = null;


        //最后一步  减免
        //分期按期计费的
        for (Map.Entry<String, FabAmount> entry : termReduceAmts.entrySet()) {
            FabAmount reduceFee = new FabAmount(entry.getValue().getVal());
            for (TblLnsbill lnsBill : billList) {

                if (!reduceFee.isPositive())
                    break;

                if (ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleflag())
                        || !entry.getKey().equals(lnsBill.getBilltype())
                        || !ConstantDeclare.FEEREPAYWAY.STAGING.equals(lnsBill.getRepayway())) {
                    continue;
                }

                Double minAmt = LoanFeeUtils.repaysql(reduceFee, tmp, lnsBill, tranctx);

                reduceFee.selfSub(minAmt);
                String feeBrc = LoanFeeUtils.matchFeeInfo(loanAgreement, lnsBill.getBilltype(), lnsBill.getRepayway()).getFeebrc();
                LoanFeeUtils.accountsub(sub, tranctx, lnsBill, minAmt, loanAgreement, "", acctBriefCode(lnsBill.getBilltype()),feeBrc);
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillstatus(),
                        new FabCurrency(), feeBrc);
                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minAmt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), acctBriefCode(lnsBill.getBilltype()), tranctx, feeBrc);
            }

            //减免到未来期再试算
            lnsBillStatistics = getfutureBills(loanAgreement, billinfos, lnsBillStatistics, reduceFee);

            //未来期的
            for (LnsBill lnsBill : billinfos) {
                if (!reduceFee.isPositive())
                    break;
                if (ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleFlag())
                        || !entry.getKey().equals(lnsBill.getBillType())
                        || !ConstantDeclare.FEEREPAYWAY.STAGING.equals(lnsBill.getRepayWay())) {
                    continue;
                }
                LoanFeeUtils.settleFee(tranctx, lnsBill, loanAgreement);
                Double minAmt = LoanFeeUtils.repaysql(reduceFee, tmp, lnsBill, tranctx);
                reduceFee.selfSub(minAmt);
                LoanFeeUtils.accountsub(sub, tranctx, lnsBill, minAmt, loanAgreement, "", acctBriefCode(lnsBill.getBillType()));
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                        new FabCurrency(), lnsBill.getLnsfeeinfo().getFeebrc());
                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minAmt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), acctBriefCode(lnsBill.getBillType()), tranctx, lnsBill.getLnsfeeinfo().getFeebrc());
            }

            if(reduceFee.isPositive()){
                throw new FabException("");
            }
        }

        //分期按天计费的
        for (Map.Entry<String, FabAmount> entry : dayReduceAmts.entrySet()) {

            FabAmount reduceFee = new FabAmount(entry.getValue().getVal());
            for (TblLnsbill lnsBill : billList) {

                if (!reduceFee.isPositive())
                    break;

                if (ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleflag())
                        || !entry.getKey().equals(lnsBill.getBilltype())) {
                    continue;
                }
                Double minAmt = LoanFeeUtils.repaysql(reduceFee, tmp, lnsBill, tranctx);
                reduceFee.selfSub(minAmt);
                String feeBrc = LoanFeeUtils.matchFeeInfo(loanAgreement, lnsBill.getBilltype(), lnsBill.getRepayway()).getFeebrc();

                LoanFeeUtils.accountsub(sub, tranctx, lnsBill, minAmt, loanAgreement, "", acctBriefCode(lnsBill.getBilltype()),feeBrc);
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillstatus(),
                        new FabCurrency(), feeBrc);
                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minAmt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), acctBriefCode(lnsBill.getBilltype()), tranctx, feeBrc);
            }
            //减免到未来期再试算
            lnsBillStatistics = getfutureBills(loanAgreement, billinfos, lnsBillStatistics, reduceFee);

            //未来期的
            for (LnsBill lnsBill : billinfos) {
                if (!reduceFee.isPositive())
                    break;
                if (ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleFlag())
                        || !entry.getKey().equals(lnsBill.getBillType())) {
                    continue;
                }
                boolean isPeriod = false;
                //当期的特殊处理
                if (!VarChecker.isEmpty(lnsBill.getRepayDateInt())
                        && !lnsBill.getBillBal().equals(lnsBill.getRepayDateInt())) {
                    lnsBill.setEndDate(tranctx.getTranDate());
                    lnsBill.setBillAmt(lnsBill.getRepayDateInt());
                    lnsBill.setBillBal(lnsBill.getRepayDateInt());
                    isPeriod = true;
                }

                LoanFeeUtils.settleFee(tranctx, lnsBill, loanAgreement);
                //提前还费不更改期数
                if (isPeriod)
                    LoanFeeUtils.updatePeriod(lnsBill.getLnsfeeinfo(), lnsBill.getPeriod());
                Double minAmt = LoanFeeUtils.repaysql(reduceFee, tmp, lnsBill, tranctx);
                reduceFee.selfSub(minAmt);
                LoanFeeUtils.accountsub(sub, tranctx, lnsBill, minAmt, loanAgreement, "", acctBriefCode(lnsBill.getBillType()));
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                        new FabCurrency(), lnsBill.getLnsfeeinfo().getFeebrc());
                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minAmt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), acctBriefCode(lnsBill.getBillType()), tranctx, lnsBill.getLnsfeeinfo().getFeebrc());


            }
            if(reduceFee.isPositive()){
                throw new FabException("");
            }
        }
        //减免违约金
        for (Map.Entry<String, FabAmount> entry : damageReduceAmts.entrySet()) {

            FabAmount reduceFee = new FabAmount(entry.getValue().getVal());
            for (TblLnsbill lnsBill : billList) {
                String feebrc = "";

                if (!reduceFee.isPositive())
                    break;
                if (ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleflag())||
                        !entry.getKey().equals(lnsBill.getBilltype())) {
                    continue;
                }
                for (TblLnsbill bill :billList) {

                    //三编码匹配上 对应的费用账本
                    if (lnsBill.getDertrandate().equals(bill.getTrandate())
                            && lnsBill.getDertxseq().equals(bill.getTxseq())
                            && lnsBill.getDerserseqno().equals(bill.getSerseqno())) {
                        //费用账本匹配上对应的 费用信息  得到 费用机构号
                        feebrc = LoanFeeUtils.matchFeeInfo(loanAgreement, bill.getBilltype(), bill.getRepayway()).getFeebrc();
                        break;
                    }
                }



                Double minAmt = LoanFeeUtils.repaysql(reduceFee, tmp, lnsBill, tranctx);
                reduceFee.selfSub(minAmt);


                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEED, lnsBill.getBillstatus(),
                        new FabCurrency(), feebrc);
                lnsAcctInfo.setMerchantNo(repayAcctNo);
                lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minAmt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(),ConstantDeclare.BRIEFCODE.SFJM, tranctx, feebrc);
                sub.operate(lnsAcctInfo, null, new FabAmount(minAmt), loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.SFJM,
                        lnsBill.getTrandate().toString(), lnsBill.getSerseqno(), lnsBill.getTxseq(), tranctx);
            }
            //不会有未落表的违约金 违约金减免钱违约金都落表了
//            List<LnsBill> damageBills = new ArrayList<>();
//            damageBills.addAll(lnsBillStatistics.getHisSetIntBillList());
//            damageBills.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());

            if(reduceFee.isPositive()){
                throw new FabException("");
            }
        }


    }

    private LnsBillStatistics getfutureBills(LoanAgreement loanAgreement, List<LnsBill> billinfos, LnsBillStatistics lnsBillStatistics, FabAmount reduceFee) throws FabException {
        if(lnsBillStatistics==null && reduceFee.isPositive()){
            lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, tranctx.getTranDate(),tranctx);
            billinfos.addAll(lnsBillStatistics.getBillInfoList());
            billinfos.addAll(lnsBillStatistics.getFutureBillInfoList());
        }
        return lnsBillStatistics;
    }

    //交易明细摘要码
    private String acctBriefCode(String billType){

        if(ConstantDeclare.FEETYPE.RMFE.equals(billType))
            return ConstantDeclare.BRIEFCODE.RFJM;
        else if(ConstantDeclare.FEETYPE.SQFE.equals(billType))
            return ConstantDeclare.BRIEFCODE.SFJM;
        else if( LoanFeeUtils.isOtherFee(billType)   )
            return ConstantDeclare.BRIEFCODE.FYJM;
        return "";
    }

    /**
     * Gets the value of adjustFee.
     *
     * @return the value of adjustFee
     */
    public FabAmount getAdjustFee() {
        return adjustFee;
    }

    /**
     * Sets the adjustFee.
     *
     * @param adjustFee adjustFee
     */
    public void setAdjustFee(FabAmount adjustFee) {
        this.adjustFee = adjustFee;

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

    /**
     * Gets the value of damageReduceAmts.
     *
     * @return the value of damageReduceAmts
     */
    public Map<String, FabAmount> getDamageReduceAmts() {
        return damageReduceAmts;
    }

    /**
     * Sets the damageReduceAmts.
     *
     * @param damageReduceAmts damageReduceAmts
     */
    public void setDamageReduceAmts(Map<String, FabAmount> damageReduceAmts) {
        this.damageReduceAmts = damageReduceAmts;

    }

    /**
     * Gets the value of termReduceAmts.
     *
     * @return the value of termReduceAmts
     */
    public Map<String, FabAmount> getTermReduceAmts() {
        return termReduceAmts;
    }

    /**
     * Sets the termReduceAmts.
     *
     * @param termReduceAmts termReduceAmts
     */
    public void setTermReduceAmts(Map<String, FabAmount> termReduceAmts) {
        this.termReduceAmts = termReduceAmts;

    }

    /**
     * Gets the value of dayReduceAmts.
     *
     * @return the value of dayReduceAmts
     */
    public Map<String, FabAmount> getDayReduceAmts() {
        return dayReduceAmts;
    }

    /**
     * Sets the dayReduceAmts.
     *
     * @param dayReduceAmts dayReduceAmts
     */
    public void setDayReduceAmts(Map<String, FabAmount> dayReduceAmts) {
        this.dayReduceAmts = dayReduceAmts;

    }

    /**
     * Gets the value of realDate.
     *
     * @return the value of realDate
     */
    public String getRealDate() {
        return realDate;
    }

    /**
     * Sets the realDate.
     *
     * @param realDate realDate
     */
    public void setRealDate(String realDate) {
        this.realDate = realDate;

    }

    /**
     * Gets the value of repayAcctNo.
     *
     * @return the value of repayAcctNo
     */
    public String getRepayAcctNo() {
        return repayAcctNo;
    }

    /**
     * Sets the repayAcctNo.
     *
     * @param repayAcctNo repayAcctNo
     */
    public void setRepayAcctNo(String repayAcctNo) {
        this.repayAcctNo = repayAcctNo;

    }

    /**
     * Gets the value of adjustDamage.
     *
     * @return the value of adjustDamage
     */
    public FabAmount getAdjustDamage() {
        return adjustDamage;
    }

    /**
     * Sets the adjustDamage.
     *
     * @param adjustDamage adjustDamage
     */
    public void setAdjustDamage(FabAmount adjustDamage) {
        this.adjustDamage = adjustDamage;

    }
}

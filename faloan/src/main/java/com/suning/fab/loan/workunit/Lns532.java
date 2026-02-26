package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
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

import java.util.*;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：计算实际还款日 费用的减免金额
 *
 * @Author 18049705 MYP
 * @Date Created in 16:56 2020/4/1
 * @see
 */
@Scope("prototype")
@Repository
public class Lns532 extends WorkUnit {

    private String acctNo; //贷款账号
    private String settleFlag;//结清标志 0-不结清  1-结清


    private LoanAgreement loanAgreement;
    private LnsBillStatistics lnsBillStatistics;
    //实际还款日
    private String  realDate;
    private Map<String, FabAmount> damageReduceAmts = new HashMap<>();
    private Map<String, FabAmount> termReduceAmts = new HashMap<>();//按期计费减免金额
    private Map<String, FabAmount>  dayReduceAmts = new HashMap<>();//按天计费减免金额

    @Override
    public void run() throws Exception {
        //实际还款日
        if(VarChecker.isEmpty(realDate)||realDate.equals(tranctx.getTranDate()))
            return;


        //还款
        loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx,loanAgreement);

        if(loanAgreement.getFeeAgreement().isEmpty())
            throw new FabException("LNS037");

        //实际扣款日不结清  暂不报错
        if(VarChecker.isEmpty(settleFlag)||"0".equals(settleFlag))
           return ;


        //分期费用的 计费类型 分期还是 按天计费
        Map<String,Map<String,List<LnsBill>>> ruleBills = new HashMap<>();//Key计费类型-key账本类型-value账本
        Map<String,Map<String,List<LnsBill>>> realDateRuleBills = new HashMap<>();//Key计费类型-key账本类型-value账本
        for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
            //提前结清全收费用  无需减免
            if(ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(lnsfeeinfo.getAdvancesettle()))
                continue;
            //费用减免 新增还款方式D
            if(Arrays.asList(ConstantDeclare.FEEREPAYWAY.STAGING,ConstantDeclare.FEEREPAYWAY.NONESTATIC).contains(lnsfeeinfo.getRepayway())
                   /* //过滤提前结清全收的
                    &&!ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(lnsfeeinfo.getAdvancesettle())*/){
                ruleBills.put(lnsfeeinfo.getCalculatrule(), new HashMap<>());
                realDateRuleBills.put(lnsfeeinfo.getCalculatrule(), new HashMap<>());
            }
        }

        //没有按期的费用 暂时只有按期
        if(ruleBills.isEmpty())
            return ;
        lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, tranctx.getTranDate(),tranctx,lnsBillStatistics);

        //违约金list
        Map<String,List<LnsBill>>  damages = new HashMap<>();

        //统计所有费用  按照计费方式区分
        groupFee(ruleBills, damages, lnsBillStatistics);
        //按期计费-减免金额
//        Map<String,FabAmount> termReduceAmts = new HashMap<>();//key费用类型-value减免金额
        //按期的计费的 统计
        if (ruleBills.get(ConstantDeclare.CALCULATRULE.BYTERM)!=null){
            for(Map.Entry<String,List<LnsBill>> bills :ruleBills.get(ConstantDeclare.CALCULATRULE.BYTERM).entrySet()){
                for (LnsBill lnsBill :bills.getValue()){
                    //实际还款日的未来期  trandate的当前期和历史期  需要减免    && CalendarUtil.beforeAlsoEqual(tranctx.getTranDate(),lnsBill.getCurendDate() )  去掉日期限制
                    if(CalendarUtil.beforeAlsoEqual(realDate, lnsBill.getStartDate())
                            &&CalendarUtil.after(tranctx.getTranDate(),lnsBill.getStartDate() )
                           ){
                        if(termReduceAmts.get(lnsBill.getBillType()) == null){
                            termReduceAmts.put(lnsBill.getBillType(), new FabAmount());
                        }
                        termReduceAmts.get(lnsBill.getBillType()).selfAdd(lnsBill.getBillBal());
                    }
                }
            }
        }
        //违约金list
        Map<String,List<LnsBill>>  realDateDamages = new HashMap<>();
        //按期计费-减免金额
//        Map<String,FabAmount> dayReduceAmts = new HashMap<>();//key费用类型-value减免金额
        //按照实际还款日试算的
        LnsBillStatistics realDateBillStatistics = null;
        if(ruleBills.get(ConstantDeclare.CALCULATRULE.BYDAY)!=null){
            //只会试算一遍

            realDateBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, realDate, tranctx, realDateBillStatistics);
            groupFee(realDateRuleBills, realDateDamages, realDateBillStatistics);


            for(Map.Entry<String,List<LnsBill>> bills :ruleBills.get(ConstantDeclare.CALCULATRULE.BYDAY).entrySet()){
                FabAmount reduceAmt = new FabAmount();
                for(LnsBill lnsBill:bills.getValue())
                {
                    if(!VarChecker.isEmpty(lnsBill.getRepayDateInt())
                            &&lnsBill.getBillAmt().sub(lnsBill.getRepayDateInt()).isPositive())

                        reduceAmt.selfAdd(lnsBill.getRepayDateInt());
                    else
                        reduceAmt.selfAdd(lnsBill.getBillBal());
                }
                if(realDateRuleBills.get(ConstantDeclare.CALCULATRULE.BYDAY).get(bills.getKey())!=null){
                    //匹配费用类型
                    for(LnsBill lnsBill:realDateRuleBills.get(ConstantDeclare.CALCULATRULE.BYDAY).get(bills.getKey())){
                        if(!VarChecker.isEmpty(lnsBill.getRepayDateInt())
                                &&lnsBill.getBillAmt().sub(lnsBill.getRepayDateInt()).isPositive())
                            reduceAmt.selfSub(lnsBill.getRepayDateInt());
                        else
                            reduceAmt.selfSub(lnsBill.getBillBal());
                    }
                }


                dayReduceAmts.put(bills.getKey(), reduceAmt);
            }
        }
//        Map<String,FabAmount> damageReduceAmts = new HashMap<>();

        //违约金
        if(!damages.isEmpty()){
            if(VarChecker.isEmpty(realDateBillStatistics)) {
                realDateBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, realDate, tranctx, realDateBillStatistics);
                groupFee(realDateRuleBills, realDateDamages, realDateBillStatistics);
            }
            for(Map.Entry<String,List<LnsBill>> entry:damages.entrySet())
            {
                FabAmount reduceAmt = new FabAmount();
                for(LnsBill lnsBill:entry.getValue())
                {
                    reduceAmt.selfAdd(lnsBill.getBillBal());
                }
                if(realDateDamages.get(entry.getKey())!=null){
                    for(LnsBill lnsBill:realDateDamages.get(entry.getKey())){
                        reduceAmt.selfSub(lnsBill.getBillBal());
                    }
                }

                damageReduceAmts.put(entry.getKey(), reduceAmt);
            }

        }






    }

    //事件摘要码
//    private String eventBriefCode(String billType){
//
//        if(ConstantDeclare.FEETYPE.RMFE.equals(billType))
//              return ConstantDeclare.BRIEFCODE.FXGL;
//        else if(ConstantDeclare.FEETYPE.SQFE.equals(billType))
//              return ConstantDeclare.BRIEFCODE.QJDB;
//        return "";
//    }


    private void groupFee(Map<String,Map<String, List<LnsBill>>> ruleBills, Map<String,List<LnsBill>> damages,LnsBillStatistics lnsBillStatistics) {
        List<LnsBill> hisbillinfos = new ArrayList<>();
        hisbillinfos.addAll(lnsBillStatistics.getHisBillList());
        hisbillinfos.addAll(lnsBillStatistics.getHisSetIntBillList());
        for(LnsBill lnsBill:hisbillinfos){
            //过滤非费用的账本 过滤
            if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleFlag())
                    ||(!LoanFeeUtils.isFeeType(lnsBill.getBillType())&&!lnsBill.isPenalty())){
                continue;
            }
            //违约金
            if(lnsBill.isPenalty()){
                if(damages.get(lnsBill.getBillType()) == null){
                    damages.put(lnsBill.getBillType(), new ArrayList<>());
                }
                damages.get(lnsBill.getBillType()).add(lnsBill);
            }else
            //分期的  未来期提前结清不全收的
                // 费用减免 新增还款方式D
            if(Arrays.asList(ConstantDeclare.FEEREPAYWAY.STAGING,ConstantDeclare.FEEREPAYWAY.NONESTATIC).contains(lnsBill.getLnsfeeinfo().getRepayway())
                    //过滤提前结清全收的
                    &&!ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(lnsBill.getLnsfeeinfo().getAdvancesettle())){

                if(ruleBills.get(lnsBill.getLnsfeeinfo().getCalculatrule()).get(lnsBill.getBillType()) == null){
                    ruleBills.get(lnsBill.getLnsfeeinfo().getCalculatrule()).put(lnsBill.getBillType(), new ArrayList<>());
                }
                ruleBills.get(lnsBill.getLnsfeeinfo().getCalculatrule()).get(lnsBill.getBillType()).add(lnsBill);
            }

        }
        List<LnsBill> billinfos = new ArrayList<>();
        billinfos.addAll(lnsBillStatistics.getBillInfoList());
        billinfos.addAll(lnsBillStatistics.getFutureBillInfoList());

        for(LnsBill lnsBill:billinfos){
            //不是费用且或者是未来期的过滤

            if(!LoanFeeUtils.isFeeType(lnsBill.getBillType())
                    ||VarChecker.isEmpty(lnsBill.getRepayDateInt())){
                    continue;
            }
            //分期的  未来期提前结清不全收的
            //费用减免 新增还款方式D
            if(Arrays.asList(ConstantDeclare.FEEREPAYWAY.STAGING,ConstantDeclare.FEEREPAYWAY.NONESTATIC).contains(lnsBill.getLnsfeeinfo().getRepayway())
                    //过滤提前结清全收的
                    &&!ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(lnsBill.getLnsfeeinfo().getAdvancesettle())){
                if(ruleBills.get(lnsBill.getLnsfeeinfo().getCalculatrule()).get(lnsBill.getBillType()) == null){
                    ruleBills.get(lnsBill.getLnsfeeinfo().getCalculatrule()).put(lnsBill.getBillType(), new ArrayList<>());
                }
                ruleBills.get(lnsBill.getLnsfeeinfo().getCalculatrule()).get(lnsBill.getBillType()).add(lnsBill);
            }

        }
            for(LnsBill lnsBill:lnsBillStatistics.getFutureOverDuePrinIntBillList()){
                if(lnsBill.isPenalty())
                {
                    if(damages.get(lnsBill.getBillType()) == null){
                        damages.put(lnsBill.getBillType(), new ArrayList<>());
                    }
                    damages.get(lnsBill.getBillType()).add(lnsBill);
                }
            }
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
     * Gets the value of settleFlag.
     *
     * @return the value of settleFlag
     */
    public String getSettleFlag() {
        return settleFlag;
    }

    /**
     * Sets the settleFlag.
     *
     * @param settleFlag settleFlag
     */
    public void setSettleFlag(String settleFlag) {
        this.settleFlag = settleFlag;

    }

    /**
     * Gets the value of loanAgreement.
     *
     * @return the value of loanAgreement
     */
    public LoanAgreement getLoanAgreement() {
        return loanAgreement;
    }

    /**
     * Sets the loanAgreement.
     *
     * @param loanAgreement loanAgreement
     */
    public void setLoanAgreement(LoanAgreement loanAgreement) {
        this.loanAgreement = loanAgreement;

    }

    /**
     * Gets the value of lnsBillStatistics.
     *
     * @return the value of lnsBillStatistics
     */
    public LnsBillStatistics getLnsBillStatistics() {
        return lnsBillStatistics;
    }

    /**
     * Sets the lnsBillStatistics.
     *
     * @param lnsBillStatistics lnsBillStatistics
     */
    public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
        this.lnsBillStatistics = lnsBillStatistics;

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
}

package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.LnsAmortizeplan;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.RepayWaySupporter;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：延期还款 非等本等息
 *
 * @Author 18049705 MYP
 * @Date Created in 10:39 2020/2/26
 * @see
 */
@Scope("prototype")
@Repository
public class Lns607 extends WorkUnit {

    private String acctNo;
    private String serviceType;//	服务类型		"1.合同到期日展期（默认）2.延期还款（展期结束日期不不输）3.账单分期（预留，本期不上）"
    private String intervalTime; //	延期还款间隔		"M：按月 D：按天（预留，本期不支持） Q：按季（预留，本期不支持）"
    private Integer delayTime;//	延期还款时间	"1：一个月 2：两个月 3：三个月"
    private String serialNo; //业务流水号
    private String afterExpandDate = ""; //展期后到期日
    private String beforeExpandDate = "";
    @Override
    public void run() throws Exception{

        //查询
        //读取账户基本信息
        LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);

        if(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()) ){
            return;
        }

        //查询所有的历史账本
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", loanAgreement.getContract().getReceiptNo());
        param.put("brc", tranctx.getBrc());
        List<TblLnsbill> tblLnsBills = LoanDbUtil.queryForList("CUSTOMIZE.query_hisbills", param, TblLnsbill.class);

        if(VarChecker.isEmpty(serviceType))
            serviceType = "1";
        //校验
        if("1".equals(serviceType)){
            //只更改合同到期日，不更改当期的起止日期
            intervalTime = "M";
            delayTime = 0;

        }else{
            //延期的校验
            FaChecker.delayRpyInputCheck(intervalTime,delayTime);
        }
        //普通展期和延期+展期  都校验beforeExpandDate
        if(VarChecker.asList("4","1") .contains(serviceType)&&
                !loanAgreement.getContract().getContractEndDate().equals(beforeExpandDate))
            throw new FabException("LNS119","合同到期日");
        FaChecker.delayRpyRuleCheck(tblLnsBills, loanAgreement,tranctx);

        //备份主文件
        Map<String,Object> backParam = new HashMap<String,Object>();
        backParam.put("acctno", acctNo);
        backParam.put("brc", tranctx.getBrc());
        backParam.put("memo", tranctx.getTranCode());
        /*展期时将该条记录插入到lnsbasicinfoback表中*/
        LoanDbUtil.insert("CUSTOMIZE.insert_lnsbasicinfoback", backParam);


        //幂等
        TblLnsinterface lnsinterface = new TblLnsinterface();
        lnsinterface.setTrandate(tranctx.getTranDate());
        lnsinterface.setSerialno(serialNo);
        lnsinterface.setAccdate(tranctx.getTranDate());
        lnsinterface.setSerseqno(tranctx.getSerSeqNo());
        lnsinterface.setTrancode("470017");
        lnsinterface.setBrc(tranctx.getBrc());
        lnsinterface.setAcctname(loanAgreement.getCustomer().getCustomName());
        lnsinterface.setUserno(loanAgreement.getCustomer().getCustomId());
        lnsinterface.setAcctno(acctNo);
        lnsinterface.setTranamt(0.0);
        lnsinterface.setMagacct(acctNo);
        lnsinterface.setReserv3(beforeExpandDate);
        lnsinterface.setReserv4(afterExpandDate);

        try{
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
        } catch(FabSqlException e) {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            }
            else
                throw new FabException(e, "SPS100", "lnsinterface");
        }

        Map<String,String> stringJson = new HashMap<>();
        stringJson.put("serviceType", serviceType);//代偿原账号
        stringJson.put("intervalTime", intervalTime); //延期还款间隔
        stringJson.put("delayTime",Integer.toString(delayTime));//延期还款时间

        if ("2".equals(serviceType)){
            afterExpandDate = CalendarUtil.nTimesAfter(loanAgreement.getContract().getContractEndDate(),intervalTime,delayTime);
        }else if(VarChecker.isEmpty(afterExpandDate)){
            throw new FabException("LNS055","合同结束日");
        }


        if(CalendarUtil.before(afterExpandDate, CalendarUtil.nTimesAfter(loanAgreement.getContract().getContractEndDate(),intervalTime,delayTime)))
            throw new FabException("LNS214");
        //幂等拓展表
        AccountingModeChange.saveInterfaceEx(tranctx, acctNo, ConstantDeclare.KEYNAME.ZQ, "展期", JsonTransfer.ToJson(stringJson));


        //查询还款计划辅助表

        List<TblLnsrpyplan> rpyplanlist = LoanDbUtil.queryForList("CUSTOMIZE.query_lnsrpyplan", param, TblLnsrpyplan.class);
        if (null == rpyplanlist){
            rpyplanlist = new ArrayList<>();
        }
        String repayDate = tranctx.getTranDate();
        repayDate = FaChecker.getRepayDate(loanAgreement, tblLnsBills, rpyplanlist, repayDate);


        String oldContractEndDate =loanAgreement.getContract().getContractEndDate();



        String oldStartDate = null;
        String termEndDate = null;
        String oldEndDate = null;
        //查找被延长的到期日
        for(TblLnsrpyplan lnsrpyplan:rpyplanlist){
            if(CalendarUtil.after(repayDate,lnsrpyplan.getRepayintbdate().trim())
                    &&CalendarUtil.beforeAlsoEqual(repayDate,lnsrpyplan.getActrepaydate().trim()))
            {
//                //提前还款的往后延一期(当天提前还款到本金)
//                if(!lnsrpyplan.getActrepaydate().trim().equals(lnsrpyplan.getRepayintedate().trim())){
//                    repayDate = CalendarUtil.nDaysAfter(repayDate,1 ).toString("yyyy-MM-dd");
//                    continue;
//                }

//                //当期结清 选后期
//                if(curSettle){
//                    curSettle = false;
//                    continue;
//                }
                oldEndDate = lnsrpyplan.getRepayintedate();
                oldStartDate = lnsrpyplan.getRepayintbdate();
                termEndDate = CalendarUtil.nTimesAfter(lnsrpyplan.getRepayintedate(),intervalTime,delayTime);
                break;
            }

        }
        if(termEndDate==null)
            throw new FabException("LNS119","延长到期日");


        //提前结清了就不展呗；不是主观提前结清的，就展 1、展期
//        if(CalendarUtil.before(afterExpandDate, CalendarUtil.nTimesAfter(oldContractEndDate,intervalTime,delayTime))
//                &&!loanAgreement.getFeeAgreement().isEmpty()){
//            Map<String,String> typeTranDates = new HashMap<>();
//            //费用都结清了
//            for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
//                //结清的分期账本
//                if(ConstantDeclare.FEEREPAYWAY.STAGING.equals(lnsfeeinfo.getRepayway())
//                &&lnsfeeinfo.getFeestat().equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE)){
//                    //筛选最大期数的费用账本
//                    if(typeTranDates.isEmpty()) {
//                        for (TblLnsbill lnsbill : tblLnsBills) {
//                            if (ConstantDeclare.FEETYPE.contains(lnsbill.getBilltype().trim())
//                                    && ConstantDeclare.FEEREPAYWAY.STAGING.equals(lnsbill.getRepayway().trim())) {
//                                //费用
//                                if (typeTranDates.get(lnsbill.getBilltype().trim()) == null
//                                        || CalendarUtil.after(lnsbill.getTrandate().toString(), typeTranDates.get(lnsbill.getBilltype().trim())))
//                                    typeTranDates.put(lnsbill.getBilltype().trim(), lnsbill.getTrandate().toString());
//
//                            }
//
//                        }
//                    }
//                    //费用的最大结费日小于合同到期日 就是
//                    if(CalendarUtil.before(typeTranDates.get(lnsfeeinfo.getFeetype()), oldContractEndDate)){
//
//                    }
//
//
//                }
//
//
//
//            }
//
//
//
//
//
//        }


        //正本贷款 有账本只有一期的
        if(VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE,ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX,ConstantDeclare.REPAYWAY.REPAYWAY_HDE)
                .contains(loanAgreement.getWithdrawAgreement().getRepayWay())){
            Map<String,Object> updateFormula = new HashMap<>();
            RepayWaySupporter loanRepayWaySupporter =LoanSupporterUtil.getRepayWaySupporter(loanAgreement.getWithdrawAgreement().getRepayWay());
            if(ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX
                    .equals(loanAgreement.getWithdrawAgreement().getRepayWay())){

                //随借随还
                termEndDate = afterExpandDate; //当期到期日等于合同到期日

                //利息周期公式
                loanAgreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(1, "M", loanAgreement.getContract().getRepayDate(), loanAgreement.getContract().getStartIntDate(), afterExpandDate));
                updateFormula.put("intperformula", loanAgreement.getInterestAgreement().getPeriodFormula());
                //修改利息周期公式  需要修改费用的周期公式
                Map<String,Object> feeformula = new HashMap<>();
                feeformula.put("feeformula", loanAgreement.getInterestAgreement().getPeriodFormula());
                feeformula.put("acctno",loanAgreement.getContract().getReceiptNo() );
                feeformula.put("openbrc",tranctx.getBrc() );
                LoanDbUtil.update("Lnsfeeinfo.update_formula",feeformula);
            }
            //本金周期公式
            loanAgreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(1, "M", loanAgreement.getContract().getRepayDate(),loanAgreement.getContract().getContractStartDate(), afterExpandDate));
            updateFormula.put("prinperformula", loanAgreement.getWithdrawAgreement().getPeriodFormula());

            //修改主文件的还款方式
            updateFormula.put("acctno",loanAgreement.getContract().getReceiptNo());
            updateFormula.put("openbrc", tranctx.getBrc());


            LoanDbUtil.update("Lnsbasicinfo.update_formula",updateFormula);


        }
        //延期展期 最后一期或者本金结完了
        if(loanAgreement.getContract().getBalance().isZero()
                ||(CalendarUtil.actualDaysBetween(termEndDate, afterExpandDate) <= loanAgreement.getWithdrawAgreement().getPeriodMinDays()
                &&loanAgreement.getWithdrawAgreement().getLastTermMerge())){
            termEndDate = afterExpandDate;
        }

        if(delayTime!=0
                //随借随还修改了周期公式没必要存
                &&!ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(loanAgreement.getWithdrawAgreement().getRepayWay())
                ){
            //主文件拓展表
            param.put("key", "ZQ");
            param.put("acctno", acctNo);
            param.put("openbrc", tranctx.getBrc());

            TblLnsbasicinfoex lnsbasicinfoex = LoanDbUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
            loanAgreement.getBasicExtension().getLastEnddates().put(oldStartDate, termEndDate);

            if(lnsbasicinfoex == null){
                lnsbasicinfoex = new TblLnsbasicinfoex();
                lnsbasicinfoex.setAcctno(acctNo);
                lnsbasicinfoex.setValue1("");
                lnsbasicinfoex.setOpenbrc(tranctx.getBrc());
                lnsbasicinfoex.setKey(ConstantDeclare.KEYNAME.ZQ);
                Map<String,Object> tunneldata = new HashMap<>();
                tunneldata.put("termEndDate",termEndDate);
                Map<String,String> lastEndDate = new HashMap<>();
                lastEndDate.put(oldStartDate, termEndDate);
                tunneldata.put("lastEndDate",lastEndDate);
                lnsbasicinfoex.setTunneldata( JsonTransfer.ToJson(tunneldata));

                LoanDbUtil.insert("Lnsbasicinfoex.insert", lnsbasicinfoex);

            }else{
                //支持多次展期
                Map<String,Object> updateMap = new HashMap<>();
                if(VarChecker.isEmpty(lnsbasicinfoex.getTunneldata())){
                    Map<String,Object> tunneldata = new HashMap<>();
                    tunneldata.put("termEndDate",termEndDate);
                    Map<String,String> lastEndDate = new HashMap<>();
                    lastEndDate.put(oldStartDate, termEndDate);
                    tunneldata.put("lastEndDate",lastEndDate);
                    updateMap.put("tunneldata", JsonTransfer.ToJson(tunneldata));
                }else{
                    JSONObject tunneldata = JSONObject.parseObject(lnsbasicinfoex.getTunneldata());
                    tunneldata.getJSONObject("lastEndDate").put(oldStartDate, termEndDate);
                    tunneldata.put("termEndDate",termEndDate);
                    updateMap.put("tunneldata", tunneldata.toJSONString());
                }
                updateMap.put("acctno", acctNo);
                updateMap.put("openbrc",tranctx.getBrc() );
                updateMap.put("key", ConstantDeclare.KEYNAME.ZQ);

                LoanDbUtil.update("Lnsbasicinfoex.updateTunneldata", updateMap);

            }
        }
        
        loanAgreement.getContract().setContractEndDate(afterExpandDate);


       //摊销

        //判断是否有摊销计划

        List<LnsAmortizeplan> lnsamortizeplans = LoanDbUtil.queryForList("CUSTOMIZE.query_lnsamortizeplan", param,
                    LnsAmortizeplan.class);
        if(!lnsamortizeplans.isEmpty()) {
            Map<String,Object> amortizeplanParam = new HashMap<>();
            amortizeplanParam.put("brc", tranctx.getBrc());
            amortizeplanParam.put("acctno", acctNo);
            amortizeplanParam.put("enddate", afterExpandDate);

            try {
                DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan_enddate", amortizeplanParam);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "lnsamortizeplan");
            }
        }






        Map<String,Object> updateBillParam = new HashMap<>();
        //随借随还
        if(!ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(loanAgreement.getWithdrawAgreement().getRepayWay()))
        {
            //是否有正好当前期的到期日的情况
            for (TblLnsbill lnsbill : tblLnsBills) {
                if (CalendarUtil.beforeAlsoEqual(repayDate, lnsbill.getEnddate().trim()) &&
                        CalendarUtil.after(repayDate, lnsbill.getBegindate().trim())) {
                    updateBillParam.clear();
                    //更新本金上次结息日
                    if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)) {
                        loanAgreement.getContract().setRepayPrinDate(termEndDate);
                        updateBillParam.put("lastprindate", termEndDate);
                        updateBillParam.put("acctno", loanAgreement.getContract().getReceiptNo());
                        updateBillParam.put("openbrc", tranctx.getBrc());
                        //下一期的起止日
//                    if(lnsbill.getSettleflag().trim().equals(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE))
//                        updateBillParam.put("lastintdate", termEndDate);

                        LoanDbUtil.update("Lnsbasicinfo.update_repayPrinDate", updateBillParam);
                    }
                    //随借随还
                    if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
                            && ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE.equals(loanAgreement.getWithdrawAgreement().getRepayWay())
                            && !lnsbill.getCurenddate().trim().equals(loanAgreement.getContract().getContractEndDate())) {

                        updateBillParam.put("term", lnsbill.getPeriod());
                        updateBillParam.put("acctno", loanAgreement.getContract().getReceiptNo());
                        updateBillParam.put("openbrc", tranctx.getBrc());
                        //下一期的起止日
                        LoanDbUtil.update("Lnsbasicinfo.update_term", updateBillParam);
                        loanAgreement.getContract().setCurrPrinPeriod(lnsbill.getPeriod());
                        loanAgreement.getContract().setCurrIntPeriod(lnsbill.getPeriod());

                    }

                    //更新账本表
                    updateBillParam.put("curenddate", termEndDate);
                    updateBillParam.put("repayedate", CalendarUtil.nDaysAfter(termEndDate, loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
                    if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
                        updateBillParam.put("intedate", termEndDate);
                    }
                    updateBillParam.put("trandate", lnsbill.getTrandate());
                    updateBillParam.put("serseqno", lnsbill.getSerseqno());
                    updateBillParam.put("txseq", lnsbill.getTxseq());

                    if (LoanFeeUtils.isFeeType(lnsbill.getBilltype().trim())) {
                        LoanFeeUtils.dealFee(loanAgreement, tblLnsBills, repayDate, termEndDate, updateBillParam, lnsbill);
                    }
                    LoanDbUtil.update("Lnsbill.update_date", updateBillParam);
                }
            }
        }


        //是否有未结清的  当期到期日是原到期日的情况
        if(!termEndDate.equals(oldEndDate)) {

            for (TblLnsbill lnsbill : tblLnsBills) {
                if(lnsbill.getSettleflag().equals(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING)
                            //已结利息的当期到期日  和   提前还本的已结的上一期利息当期到期日
                        &&(lnsbill.getCurenddate().equals(oldEndDate)||lnsbill.getEnddate().equals(tranctx.getTranDate()))){
                    Map<String,Object>  billparam = new HashMap<>();
                    //修改当期到期日
                    billparam.put("trandate", lnsbill.getTrandate());
                    billparam.put("serseqno", lnsbill.getSerseqno());
                    billparam.put("txseq", lnsbill.getTxseq());
                    billparam.put("curenddate", termEndDate);
                    if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
                        billparam.put("intedate", termEndDate);
                    }
                    LoanDbUtil.update("Lnsbill.update_date", billparam);

                }

            }
        }

        //重设置合同到期日

        updateBillParam.put("contduedate",loanAgreement.getContract().getContractEndDate());
        updateBillParam.put("acctno",loanAgreement.getContract().getReceiptNo());
        updateBillParam.put("openbrc", tranctx.getBrc());
        LoanDbUtil.update("Lnsbasicinfo.update_contduedate",updateBillParam);


        Map<String,Object> updateRpyPlan = new HashMap<>();

        //处理还款计划辅助表
        for(TblLnsrpyplan lnsrpyplan:rpyplanlist) {
            if (CalendarUtil.after(repayDate, lnsrpyplan.getRepayintbdate().trim())
                    && CalendarUtil.beforeAlsoEqual(repayDate, lnsrpyplan.getActrepaydate().trim())) {

                loanAgreement.getBasicExtension().setTermEndDate(termEndDate);
                //开始试算
                LnsBillStatistics billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, tranctx.getTranDate(),tranctx);
                //应还 已还统计
                List<LnsBill> totalbill = billStatistics.getTotalbillList();
                //展期
                //展期原最后一期
                if(delayTime == 0
                        &&termEndDate.equals(CalendarUtil.nTimesAfter(oldContractEndDate,intervalTime,delayTime))
                        &&!termEndDate.equals(loanAgreement.getContract().getContractEndDate())
                        &&CalendarUtil.before(repayDate,lnsrpyplan.getRepayintedate().trim())){
                    String currentdate = null;

                    for(LnsBill lnsBill:totalbill){

                        if(CalendarUtil.beforeAlsoEqual(repayDate,lnsBill.getEndDate())&&
                                CalendarUtil.after(repayDate,lnsBill.getStartDate())
                                &&lnsBill.getPeriod().equals(lnsrpyplan.getRepayterm())) {

                                if (ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType()) ||
                                        ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())) {
                                    if (currentdate == null)
                                        currentdate = lnsBill.getCurendDate();
                                    else
                                        currentdate = CalendarUtil.after(currentdate, lnsBill.getCurendDate()) ? currentdate : lnsBill.getCurendDate();
                                }


                        }
                    }
                    termEndDate = currentdate;
                }

                //当期利息
                FabAmount currentNint = new FabAmount();
                //当期本金
                FabAmount currentPrin = new FabAmount();
                //剩余本金
                Double balance = null;

                for (LnsBill lnsBill : totalbill) {
                    //按日期统计一整期最后还款日、已还未还金额
                    if ((CalendarUtil.after(lnsBill.getEndDate(), lnsrpyplan.getRepayintbdate().trim()) &&//在本期起日之后
                            !CalendarUtil.after(lnsBill.getEndDate(), termEndDate)))//在本期实际止日或之前
                    {
                        if (ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())) {
                            //这一期账本中 最小的剩余本金
                            balance = balance == null ? lnsBill.getPrinBal().getVal() : Math.min(balance, lnsBill.getPrinBal().getVal());
                            currentPrin.selfAdd(lnsBill.getBillAmt());
                        }
                        if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
                            currentNint.selfAdd(lnsBill.getBillAmt());
                    }

                }
                //本金余额优化
                if (balance == null)
                    balance = loanAgreement.getContract().getBalance().getVal();
                //重新设置时间
                updateRpyPlan.put("acctno", lnsrpyplan.getAcctno());
                updateRpyPlan.put("brc", lnsrpyplan.getBrc());
                updateRpyPlan.put("repayterm", lnsrpyplan.getRepayterm());
                updateRpyPlan.put("repayintedate", termEndDate);
                updateRpyPlan.put("repayownedate", CalendarUtil.nDaysAfter(termEndDate, loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
                updateRpyPlan.put("termretprin", currentPrin.getVal());
                updateRpyPlan.put("termretint", currentNint.getVal());
                if (balance != null)
                    updateRpyPlan.put("balance", balance);

                LoanDbUtil.delete("Lnsrpyplan.update_delayRepay", updateRpyPlan);
            }
            //去除未来其的 还款辅助表
            if (CalendarUtil.beforeAlsoEqual(repayDate, lnsrpyplan.getRepayintbdate().trim())) {
                updateRpyPlan.put("acctno", lnsrpyplan.getAcctno());
                updateRpyPlan.put("brc", lnsrpyplan.getBrc());
                updateRpyPlan.put("repayterm", lnsrpyplan.getRepayterm());
                LoanDbUtil.delete("Lnsrpyplan.deleteByUk", updateRpyPlan);
            }
        }

        Map<String,Object> dyParam = new HashMap<String,Object>();
        dyParam.put("acctno", acctNo);
        dyParam.put("brc", tranctx.getBrc());
        dyParam.put("trandate", tranctx.getTranDate());
        //更新动态表修改时间
        try {
            DbAccessUtil.execute("CUSTOMIZE.update_dyninfo_603", dyParam);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsaccountdyninfo");
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
     * Gets the value of serviceType.
     *
     * @return the value of serviceType
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Sets the serviceType.
     *
     * @param serviceType serviceType
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;

    }

    /**
     * Gets the value of intervalTime.
     *
     * @return the value of intervalTime
     */
    public String getIntervalTime() {
        return intervalTime;
    }

    /**
     * Sets the intervalTime.
     *
     * @param intervalTime intervalTime
     */
    public void setIntervalTime(String intervalTime) {
        this.intervalTime = intervalTime;

    }

    /**
     * Gets the value of delayTime.
     *
     * @return the value of delayTime
     */
    public Integer getDelayTime() {
        return delayTime;
    }

    /**
     * Sets the delayTime.
     *
     * @param delayTime delayTime
     */
    public void setDelayTime(Integer delayTime) {
        this.delayTime = delayTime;

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
     * Gets the value of afterExpandDate.
     *
     * @return the value of afterExpandDate
     */
    public String getAfterExpandDate() {
        return afterExpandDate;
    }

    /**
     * Sets the afterExpandDate.
     *
     * @param afterExpandDate afterExpandDate
     */
    public void setAfterExpandDate(String afterExpandDate) {
        this.afterExpandDate = afterExpandDate;

    }

    /**
     * Gets the value of beforeExpandDate.
     *
     * @return the value of beforeExpandDate
     */
    public String getBeforeExpandDate() {
        return beforeExpandDate;
    }

    /**
     * Sets the beforeExpandDate.
     *
     * @param beforeExpandDate beforeExpandDate
     */
    public void setBeforeExpandDate(String beforeExpandDate) {
        this.beforeExpandDate = beforeExpandDate;

    }
}

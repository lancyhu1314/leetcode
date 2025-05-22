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
 * 〈功能详细描述〉：延期还款  等本等息
 *
 * @Author 18049705 MYP
 * @Date Created in 10:39 2020/2/26
 * @see
 */
@Scope("prototype")
@Repository
public class Lns608 extends WorkUnit {

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
        if(!ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()) ){
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


        //展期日 当天结息
        repayDate = FaChecker.specialRepayDate(loanAgreement, tblLnsBills, rpyplanlist, repayDate);

        String oldStartDate = null;
        String termEndDate = null;
        String oldEndDate = null;
        //查找被延长的到期日
        for(TblLnsrpyplan lnsrpyplan:rpyplanlist){
            if(CalendarUtil.after(repayDate,lnsrpyplan.getRepayintbdate().trim())
                    &&CalendarUtil.beforeAlsoEqual(repayDate,lnsrpyplan.getActrepaydate().trim()))
            {

                oldEndDate = lnsrpyplan.getRepayintedate();
                oldStartDate = lnsrpyplan.getRepayintbdate();
                termEndDate = CalendarUtil.nTimesAfter(lnsrpyplan.getRepayintedate(),intervalTime,delayTime);
                break;
            }

        }
        if(termEndDate==null)
            throw new FabException("LNS119","延长到期日");



        //延期展期 最后一期或者本金结完了
        if((loanAgreement.getContract().getBalance().isZero()&&oldEndDate.equals(loanAgreement.getContract().getContractEndDate()) )
                ||(CalendarUtil.actualDaysBetween(termEndDate, afterExpandDate) <= loanAgreement.getWithdrawAgreement().getPeriodMinDays()
                &&loanAgreement.getWithdrawAgreement().getLastTermMerge())){
            termEndDate = afterExpandDate;
        }


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


        //是否有正好当前期的到期日的情况
        for (TblLnsbill lnsbill : tblLnsBills) {
            if (CalendarUtil.beforeAlsoEqual(repayDate, lnsbill.getEnddate())) {
                updateBillParam.clear();


                //更新账本表
                updateBillParam.put("curenddate", termEndDate);

                if(CalendarUtil.after(lnsbill.getBegindate(),repayDate)){

                    updateBillParam.put("begindate", CalendarUtil.nTimesAfter(lnsbill.getBegindate(), intervalTime,delayTime));
                    if(lnsbill.getCurenddate().equals(loanAgreement.getContract().getContractEndDate())){
                        updateBillParam.put("curenddate",afterExpandDate);
                    }else
                        updateBillParam.put("curenddate", CalendarUtil.nTimesAfter(lnsbill.getCurenddate(), intervalTime,delayTime));
                    updateBillParam.put("enddate", updateBillParam.get("curenddate").toString());
                    updateBillParam.put("intedate", updateBillParam.get("curenddate").toString());
                }
                updateBillParam.put("repayedate", CalendarUtil.nDaysAfter(updateBillParam.get("curenddate").toString(),
                        loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));

                //更新本金上次结息日
                if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
                        &&CalendarUtil.after(updateBillParam.get("curenddate").toString(), loanAgreement.getContract().getRepayPrinDate())) {
                    loanAgreement.getContract().setRepayPrinDate(updateBillParam.get("curenddate").toString());
                    updateBillParam.put("lastprindate", updateBillParam.get("curenddate").toString());
                    updateBillParam.put("lastintdate", updateBillParam.get("curenddate").toString());
                    updateBillParam.put("acctno", loanAgreement.getContract().getReceiptNo());
                    updateBillParam.put("openbrc", tranctx.getBrc());
                    LoanDbUtil.update("Lnsbasicinfo.update_repayPrinDate", updateBillParam);
                }


                if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
                    updateBillParam.put("intedate", updateBillParam.get("curenddate").toString());
                }
                updateBillParam.put("trandate", lnsbill.getTrandate());
                updateBillParam.put("serseqno", lnsbill.getSerseqno());
                updateBillParam.put("txseq", lnsbill.getTxseq());


                if (LoanFeeUtils.isFeeType(lnsbill.getBilltype().trim())
                        &&CalendarUtil.after(repayDate, lnsbill.getBegindate())) {
                    //未来期费用处理
                    LoanFeeUtils.dealFee(loanAgreement, tblLnsBills, repayDate, termEndDate, updateBillParam, lnsbill);
                }


                LoanDbUtil.update("Lnsbill.update_date", updateBillParam);
            }
        }




        //重设置合同到期日

        updateBillParam.put("contduedate",afterExpandDate);
        updateBillParam.put("acctno",loanAgreement.getContract().getReceiptNo());
        updateBillParam.put("openbrc", tranctx.getBrc());
        LoanDbUtil.update("Lnsbasicinfo.update_contduedate",updateBillParam);


        Map<String,Object> updateRpyPlan = new HashMap<>();




        //处理还款计划辅助表
        for(TblLnsrpyplan lnsrpyplan:rpyplanlist) {
            //去除未来其的 还款辅助表
            if (CalendarUtil.afterAlsoEqual( lnsrpyplan.getActrepaydate().trim(),repayDate)) {
                updateRpyPlan.put("acctno", lnsrpyplan.getAcctno());
                updateRpyPlan.put("brc", lnsrpyplan.getBrc());
                updateRpyPlan.put("repayterm", lnsrpyplan.getRepayterm());
                LoanDbUtil.delete("Lnsrpyplan.deleteByUk", updateRpyPlan);
            }
        }
        //根据账号生成账单
        LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);
        LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, tranctx.getTranDate(),tranctx);
        LoanRepayPlanProvider.interestRepayPlan( tranctx,"DELAY",la, lnsBillStatistics);

        Map<String,Object> dyParam = new HashMap<>();
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

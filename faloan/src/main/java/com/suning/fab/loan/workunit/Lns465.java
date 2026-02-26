package com.suning.fab.loan.workunit;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.RepayWaySupporter;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：展期试算
 *
 * @Author 18049705 MYP
 * @Date Created in 13:57 2020/2/26
 * @see
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns465 extends WorkUnit {


    private String acctNo;
    private String serviceType;//	服务类型		"1.合同到期日展期（默认）2.延期还款（展期结束日期不不输）3.账单分期（预留，本期不上）"
    private String intervalTime; //	延期还款间隔		"M：按月 D：按天（预留，本期不支持） Q：按季（预留，本期不支持）"
    private Integer delayTime;//	延期还款时间	"1：一个月 2：两个月 3：三个月"
    LoanAgreement loanAgreement;
    private String expandDate; //展期后到期日

    LnsBillStatistics billStatistics = new LnsBillStatistics();
    List<TblLnsrpyplan> rpyplanlist;
    private FabAmount termrettotal = new FabAmount(); //	应还合计
    private FabAmount totalAmt = new FabAmount();     //	已还合计

    @Override
    public void run() throws Exception{

        //查询
        //读取账户基本信息
        loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);
        String oldContractEndDate = loanAgreement.getContract().getContractEndDate();
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
        //校验
        FaChecker.delayRpyRuleCheck(tblLnsBills, loanAgreement,tranctx);

        //查询还款计划辅助表

        rpyplanlist = LoanDbUtil.queryForList("CUSTOMIZE.query_lnsrpyplan", param, TblLnsrpyplan.class);
        if (null == rpyplanlist){
            rpyplanlist = new ArrayList<TblLnsrpyplan>();
        }
        String repayDate = tranctx.getTranDate();
        //展期日 当天结息
        if(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()) ){
            repayDate = FaChecker.specialRepayDate(loanAgreement, tblLnsBills, rpyplanlist, repayDate);
        }else{
            repayDate = FaChecker.getRepayDate(loanAgreement, tblLnsBills, rpyplanlist, repayDate);
        }

        String termEndDate = null;
        String oldstart = null;
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
//                    repayDate = CalendarUtil.nDaysAfter(lnsrpyplan.getRepayintedate().trim(),1 ).toString("yyyy-MM-dd");
//                    continue;
//                }
                termEndDate = CalendarUtil.nTimesAfter(lnsrpyplan.getRepayintedate(),intervalTime,delayTime);
                oldstart = lnsrpyplan.getRepayintbdate();
                break;
            }

        }

        if(termEndDate==null)
            throw new FabException("LNS119","延长到期日");

        if ("2".equals(serviceType)){
            expandDate = CalendarUtil.nTimesAfter(loanAgreement.getContract().getContractEndDate(),intervalTime,delayTime);

        }else if(VarChecker.isEmpty(expandDate)){
            throw new FabException("LNS055","合同结束日");
        }

        if(CalendarUtil.before(expandDate, CalendarUtil.nTimesAfter(loanAgreement.getContract().getContractEndDate(),intervalTime,delayTime)))
            throw new FabException("LNS214");

        //周期公式修改
        if(VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE,ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX,ConstantDeclare.REPAYWAY.REPAYWAY_HDE)
                .contains(loanAgreement.getWithdrawAgreement().getRepayWay())){
            RepayWaySupporter loanRepayWaySupporter =LoanSupporterUtil.getRepayWaySupporter(loanAgreement.getWithdrawAgreement().getRepayWay());
            if(ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX
                    .equals(loanAgreement.getWithdrawAgreement().getRepayWay())){
                termEndDate = expandDate;
                loanAgreement.getBasicExtension().getLastEnddates().put(oldstart, termEndDate);
                //利息周期公式
                loanAgreement.getInterestAgreement().setPeriodFormula(loanRepayWaySupporter.getIntPeriodFormula(1, "M", loanAgreement.getContract().getRepayDate(), loanAgreement.getContract().getStartIntDate(),expandDate));
                if(!loanAgreement.getFeeAgreement().isEmpty())
                {
                    for(TblLnsfeeinfo lnsfeeinfo : loanAgreement.getFeeAgreement().getLnsfeeinfos()){
                        lnsfeeinfo.setFeeformula(loanAgreement.getInterestAgreement().getPeriodFormula());
                    }
                }
            }
            //本金周期公式
            loanAgreement.getWithdrawAgreement().setPeriodFormula(loanRepayWaySupporter.getPrinPeriodFormula(1, "M", loanAgreement.getContract().getRepayDate(),loanAgreement.getContract().getContractStartDate(),expandDate));

        }

        //延期展期 最后一期或者本金结完了
        if(loanAgreement.getContract().getBalance().isZero()
                ||(CalendarUtil.actualDaysBetween(termEndDate, expandDate) <= loanAgreement.getWithdrawAgreement().getPeriodMinDays()
                &&loanAgreement.getWithdrawAgreement().getLastTermMerge())){
            termEndDate = expandDate;
        }


        loanAgreement.getBasicExtension().setTermEndDate(termEndDate);
        //是否有正好当前期的到期日的情况
        //随借随还
        if(!ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(loanAgreement.getWithdrawAgreement().getRepayWay())) {
            for (TblLnsbill lnsbill : tblLnsBills) {
                if (CalendarUtil.beforeAlsoEqual(repayDate, lnsbill.getEnddate().trim()) &&
                        CalendarUtil.after(repayDate, lnsbill.getBegindate().trim())) {
                    //需要特殊处理
                    lnsbill.setCurenddate(termEndDate);
                    lnsbill.setRepayedate(CalendarUtil.nTimesAfter(lnsbill.getRepayedate().trim(), intervalTime, delayTime));
                    if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
                        loanAgreement.getContract().setRepayPrinDate(termEndDate);
                    //下一期的起止日
//                if(lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)&&lnsbill.getSettleflag().trim().equals(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE))
//                    loanAgreement.getContract().setRepayIntDate(termEndDate);
                    //利息要从这个时间点记罚息
                    if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
                        lnsbill.setIntedate(termEndDate);
                    }
                    //随借随还
                    if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
                            && ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE.equals(loanAgreement.getWithdrawAgreement().getRepayWay())
                            && !lnsbill.getCurenddate().trim().equals(loanAgreement.getContract().getContractEndDate())) {

                        loanAgreement.getContract().setCurrPrinPeriod(lnsbill.getPeriod());
                        loanAgreement.getContract().setCurrIntPeriod(lnsbill.getPeriod());

                    }
                    if (LoanFeeUtils.isFeeType(lnsbill.getBilltype().trim())) {
                        for (TblLnsfeeinfo lnsfeeinfo : loanAgreement.getFeeAgreement().getLnsfeeinfos()) {
                            if (lnsbill.getBilltype().trim().equals(lnsfeeinfo.getFeetype())
                                    && lnsbill.getRepayway().trim().equals(lnsfeeinfo.getRepayway())) {

                                if (!ConstantDeclare.CALCULATRULE.BYDAY.equals(lnsfeeinfo.getCalculatrule())) {
                                    lnsfeeinfo.setLastfeedate(termEndDate);
                                    lnsbill.setIntedate(termEndDate);

                                    //允许未来期费用结费（还款到未来期的场景） 延期
                                    if (ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(lnsfeeinfo.getAdvancesettle())) {
                                        for (TblLnsbill feeBill : tblLnsBills) {
                                            //费用且是未来期的 依次往后延期
                                            if (lnsbill.getBilltype().equals(feeBill.getBilltype())
                                                    && lnsbill.getRepayway().equals(feeBill.getRepayway())
                                                    && CalendarUtil.beforeAlsoEqual(repayDate, feeBill.getBegindate().trim())) {
                                                //更新账本表
                                                feeBill.setCurenddate(CalendarUtil.nMonthsAfter(termEndDate, feeBill.getPeriod() - lnsbill.getPeriod()).toString("yyyy-MM-dd"));
                                                feeBill.setRepayedate(CalendarUtil.nDaysAfter(feeBill.getCurenddate(), loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
                                                feeBill.setEnddate(feeBill.getCurenddate());
                                                feeBill.setIntedate(feeBill.getCurenddate());
                                                feeBill.setBegindate(CalendarUtil.nMonthsAfter(termEndDate, feeBill.getPeriod() - lnsbill.getPeriod() - 1).toString("yyyy-MM-dd"));

                                                //上次结费日
                                                lnsfeeinfo.setLastfeedate(CalendarUtil.after(feeBill.getCurenddate(), lnsfeeinfo.getLastfeedate()) ? feeBill.getCurenddate() : lnsfeeinfo.getLastfeedate());
                                            }

                                        }
                                    }

                                } else {
                                    //这里需要跟利息一样在试算未来期之前 补延期的费用
                                    //补费用暂时没做 先拦截
                                    throw new FabException("LNS207", "费用按天计费");

                                }

                            }
                        }
                    }
                }
                //等本等息才会存在未来期的
                else if(CalendarUtil.after(lnsbill.getBegindate(),repayDate)){

                    if(lnsbill.getCurenddate().equals(loanAgreement.getContract().getContractEndDate())){
                        lnsbill.setCurenddate(expandDate);
                    }else
                        lnsbill.setCurenddate(CalendarUtil.nTimesAfter(lnsbill.getCurenddate(), intervalTime,delayTime));
                    lnsbill.setEnddate(lnsbill.getCurenddate());
                    lnsbill.setBegindate(CalendarUtil.nTimesAfter(lnsbill.getBegindate(), intervalTime,delayTime));
                    lnsbill.setRepayedate(CalendarUtil.nDaysAfter(lnsbill.getCurenddate(),
                            loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
                    if (lnsbill.getBilltype().trim().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
                            &&CalendarUtil.after(lnsbill.getCurenddate(), loanAgreement.getContract().getRepayPrinDate()))
                        loanAgreement.getContract().setRepayPrinDate(lnsbill.getCurenddate());
                }
            }
        }
        //重设置合同到期日
        loanAgreement.getContract().setContractEndDate(expandDate);

        if(delayTime!=0)
            loanAgreement.getBasicExtension().getLastEnddates().put(oldstart, termEndDate);

        //开始试算
        long start  = System.currentTimeMillis();
        LoggerUtil.info("试算开始：" );

        //初始化
        billStatistics.setIntTotalPeriod(0);
        billStatistics.setPrinTotalPeriod(0);
        LoanAgreement la = LoanTrailCalculationProvider.getLoanAgreement(loanAgreement, tranctx.getTranDate(),tranctx,billStatistics);
        LoanBillHistorySupporter.loanHistoryBill(la,billStatistics,tranctx.getTranDate(),tranctx,tblLnsBills);
        billStatistics = LoanTrailCalculationProvider.getLnsBillStatistics(loanAgreement,tranctx.getTranDate(),tranctx,start,billStatistics,la);
        //当期利息
        FabAmount currentNint = new FabAmount();
        //当期本金
        FabAmount currentPrin = new FabAmount();
        //当期本金
        Double  balance = null;
        //应还 已还统计
        List<LnsBill> totalbill = billStatistics.getTotalbillList();
        for(LnsBill lnsBill:totalbill){
            termrettotal.selfAdd(lnsBill.getBillAmt());

            totalAmt.selfAdd(lnsBill.getBillAmt().sub(lnsBill.getBillBal()));

        }


        //处理还款计划辅助表
        for(int i = 0 ; i< rpyplanlist.size() ;i++){
            if(CalendarUtil.after(repayDate,rpyplanlist.get(i).getRepayintbdate().trim())
                    &&CalendarUtil.beforeAlsoEqual(repayDate,rpyplanlist.get(i).getActrepaydate().trim()))
            {
//展期
                //展期原最后一期
                if(delayTime == 0
                        &&termEndDate.equals(CalendarUtil.nTimesAfter(oldContractEndDate,intervalTime,delayTime))
                        &&!termEndDate.equals(loanAgreement.getContract().getContractEndDate())
                        &&CalendarUtil.before(repayDate,rpyplanlist.get(i).getRepayintedate().trim())){
                    String currentdate = null;

                    for(LnsBill lnsBill:totalbill){

                        if(CalendarUtil.beforeAlsoEqual(repayDate,lnsBill.getEndDate())&&
                                CalendarUtil.after(repayDate,lnsBill.getStartDate())
                                &&lnsBill.getPeriod().equals(rpyplanlist.get(i).getRepayterm())) {
                            {
                                if (ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType()) ||
                                        ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())) {
                                    if (currentdate == null)
                                        currentdate = lnsBill.getCurendDate();
                                    else
                                        currentdate = CalendarUtil.after(currentdate, lnsBill.getCurendDate()) ? currentdate : lnsBill.getCurendDate();
                                }

                            }
                        }
                    }
                    termEndDate = currentdate;
                }
                //重新设置时间
                rpyplanlist.get(i).setRepayintedate(termEndDate);
                rpyplanlist.get(i).setRepayownbdate(termEndDate);
                rpyplanlist.get(i).setRepayownedate(CalendarUtil.nDaysAfter( termEndDate,loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
                rpyplanlist.get(i).setActrepaydate(termEndDate);

                for(LnsBill lnsBill:totalbill){
                    //按日期统计一整期最后还款日、已还未还金额
                    if( (CalendarUtil.after(lnsBill.getEndDate(),rpyplanlist.get(i).getRepayintbdate().trim() ) &&//在本期起日之后
                            !CalendarUtil.after(lnsBill.getEndDate(),rpyplanlist.get(i).getActrepaydate().trim() )) )//在本期实际止日或之前
                    {
                        if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType()))
                        {
                            //这一期账本中 最小的剩余本金
                            balance =  balance == null?lnsBill.getPrinBal().getVal():Math.min(balance, lnsBill.getPrinBal().getVal());
                            currentPrin.selfAdd(lnsBill.getBillAmt());
                        }
                        if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
                            currentNint.selfAdd(lnsBill.getBillAmt());
                    }

                }
                //本金余额优化
                if(balance==null)
                    balance = loanAgreement.getContract().getBalance().getVal();

                rpyplanlist.get(i).setTermretprin(currentPrin.getVal());
                rpyplanlist.get(i).setTermretint(currentNint.getVal());
                if(balance != null)
                    rpyplanlist.get(i).setBalance(balance);

            }
            //去除未来其的 还款辅助表
            //展期原最后一期
            if(CalendarUtil.beforeAlsoEqual(repayDate,rpyplanlist.get(i).getRepayintbdate().trim())){
                rpyplanlist.remove(i);
                i--;
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
     * Gets the value of billStatistics.
     *
     * @return the value of billStatistics
     */
    public LnsBillStatistics getBillStatistics() {
        return billStatistics;
    }

    /**
     * Sets the billStatistics.
     *
     * @param billStatistics billStatistics
     */
    public void setBillStatistics(LnsBillStatistics billStatistics) {
        this.billStatistics = billStatistics;

    }

    /**
     * Gets the value of rpyplanlist.
     *
     * @return the value of rpyplanlist
     */
    public List<TblLnsrpyplan> getRpyplanlist() {
        return rpyplanlist;
    }

    /**
     * Sets the rpyplanlist.
     *
     * @param rpyplanlist rpyplanlist
     */
    public void setRpyplanlist(List<TblLnsrpyplan> rpyplanlist) {
        this.rpyplanlist = rpyplanlist;

    }

    /**
     * Gets the value of termrettotal.
     *
     * @return the value of termrettotal
     */
    public FabAmount getTermrettotal() {
        return termrettotal;
    }

    /**
     * Sets the termrettotal.
     *
     * @param termrettotal termrettotal
     */
    public void setTermrettotal(FabAmount termrettotal) {
        this.termrettotal = termrettotal;

    }

    /**
     * Gets the value of totalAmt.
     *
     * @return the value of totalAmt
     */
    public FabAmount getTotalAmt() {
        return totalAmt;
    }

    /**
     * Sets the totalAmt.
     *
     * @param totalAmt totalAmt
     */
    public void setTotalAmt(FabAmount totalAmt) {
        this.totalAmt = totalAmt;

    }

    /**
     * Gets the value of expandDate.
     *
     * @return the value of expandDate
     */
    public String getExpandDate() {
        return expandDate;
    }

    /**
     * Sets the expandDate.
     *
     * @param expandDate expandDate
     */
    public void setExpandDate(String expandDate) {
        this.expandDate = expandDate;

    }
}

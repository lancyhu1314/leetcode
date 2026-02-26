package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.*;

import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.Amount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 *  〈保费还款试算〉
 *  〈功能详细描述〉：保费还款试算
 * @Author 18049705
 * @Description 保费还款试算
 * @Date Created in 14:13 2019/5/23
 */
@Scope("prototype")
@Repository
public class Lns441 extends WorkUnit {
    String 		acctNo;			//本金账号
    String		ccy;
    String		cashFlag;
    String		termDate;
    String		tranCode;
    FabAmount	repayAmt;
    String		brc;
    Integer		subNo;
    LnsBillStatistics billStatistics;
    FabAmount	intAmt; //利息
    FabAmount	prinAmt; //本金
    FabAmount	forfeitAmt; //罚息
    FabAmount  feeAmt ;//保费

    public FabAmount getPenaltyamt() {
        return penaltyamt;
    }

    public void setPenaltyamt(FabAmount penaltyamt) {
        this.penaltyamt = penaltyamt;
    }

    FabAmount penaltyamt;//违约金
    String		endFlag; //结清标志 1-未结清 3-结清 2-重新生成换计划
    String	repayDate;
    TblLnsbasicinfo lnsbasicinfo;
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;
    @Autowired LoanEventOperateProvider eventProvider;
    @Override

    public void run() throws Exception {





        TranCtx ctx = getTranctx();
        Map<String,FabAmount> repayAmtMapIn = new HashMap<String,FabAmount>();

        //初始化客户还款上送金额 该金额不做运算 在Lns212中与客户剩余金额轧差
        if(!getRepayAmt().isPositive())
        {
            LoggerUtil.debug("上送还款金额不能为零");
            throw new FabException("LNS029");
        }
        LoggerUtil.debug("REPAYAMT:" + getRepayAmt().getVal());

        //根据账号生成账单
        LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
//        if(!VarChecker.asList("2512623","2512617","2512619").contains(la.getPrdId()))
//            throw  new FabException("LNS183",la.getPrdId());
        //读取主文件判断贷款状态(开户,销户)  判断贷款形态(正常,逾期,呆滞,呆账)取得对应还款顺序
        Map<String, Object> bparam = new HashMap<String, Object>();
        bparam.put("acctno", getAcctNo());
        bparam.put("openbrc", ctx.getBrc());
        if (null == lnsbasicinfo) {
            try {
                lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", bparam, TblLnsbasicinfo.class);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS103", "lnsbasicinfo");
            }
        }
        if (null == lnsbasicinfo) {
            throw new FabException("SPS104", "lnsbasicinfo");
        }



        if(billStatistics==null)
            billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate, ctx);

        //获取呆滞呆账期间新罚息复利账单list

        List<LnsBill> lnsBills = new ArrayList<>();
        for (LnsBill lnsBill:billStatistics.getHisBillList())
        {
            if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag()))
                lnsBills.add(lnsBill);
        }
        lnsBills.addAll(billStatistics.getHisSetIntBillList());
        lnsBills.addAll(billStatistics.getCdbillList());
        lnsBills.addAll(billStatistics.getBillInfoList());
        lnsBills.addAll(billStatistics.getFutureOverDuePrinIntBillList());





        if (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbasicinfo.getLoanstat()) ||
                ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbasicinfo.getLoanstat())
                ||LoanBasicInfoProvider.ifLanguishOrBad(lnsbasicinfo,repayDate)  ) {
            LoanRepayOrderHelper.allBadSortBill(lnsBills);

        }else {
            LoanRepayOrderHelper.allNormalSortBill(lnsBills);
        }


        int listsize = lnsBills.size();
        int repaysize  = 0;
        LoggerUtil.debug("LNSBILL:billList:"+ listsize);
        for(LnsBill lnsbill : lnsBills)
        {
            LoggerUtil.debug("LNSBILL:"+lnsbill.getPeriod()+"|"+lnsbill.getBillStatus()+"."+lnsbill.getBillType()+"|"+lnsbill.getBillBal());

            FabAmount minAmt = new FabAmount();

            LoggerUtil.debug("minAmt:" + minAmt + " reminAmt" + getRepayAmt());
            if(repayAmt.sub(lnsbill.getBillBal()).isPositive())
            {
                minAmt.selfAdd(lnsbill.getBillBal());
            }else {
                minAmt.selfAdd(repayAmt);
            }

            repayAmt.selfSub(lnsbill.getBillBal());

            if(repayAmtMapIn.get(lnsbill.getBillType()) == null)
            {
                FabAmount amt = new FabAmount(minAmt.getVal());
                repayAmtMapIn.put(lnsbill.getBillType(), amt);
            }
            else
            {
                repayAmtMapIn.get(lnsbill.getBillType()).selfAdd(minAmt);
            }

            if(!getRepayAmt().isPositive())
            {
                if(lnsbill.getBillBal().equals(minAmt)) //余额等于发生额 该条账单处理结束 加1
                    repaysize++;
                break;
            }
            repaysize++;
        }
        LoggerUtil.debug("LNSBILL:repaysize:"+ repaysize);

        endFlag = "1"; //整笔贷款未结清

        if(!Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate())) && listsize == repaysize)
        {
            endFlag = "3";
        }

        LoggerUtil.debug("["+getAcctNo()+"]"+"主流程账单还款:" + "|" + getRepayAmt().getVal());
        //当期及未来期账单生成及记账 应该拆分 三部分 当期利息 当期本金 未来期本金调三次 然后结清就要根据未来本金余额判断了
        FabAmount	prinAdvancesum = new FabAmount();
        FabAmount	nintAdvancesum = new FabAmount();
        String	prePayFlag = null;
        if(getRepayAmt().isPositive() && Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate()))) {
            //未來期还款  先还当期保费
            for(LnsBill lnsbill : billStatistics.getFutureBillInfoList()){
                //不是费用的
                if(!LoanFeeUtils.isFeeType(lnsbill.getBillType()))
                    continue;
                //未来期的费一次性
                //起息日当天 不收取分期费用
                if(!(lnsbill.getLnsfeeinfo().getAdvancesettle().equals(ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE)
                        && !repayDate.equals(la.getContract().getStartIntDate()))
                        && !lnsbill.getLnsfeeinfo().getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ONETIME)
                        &&CalendarUtil.afterAlsoEqual(lnsbill.getStartDate(), repayDate))
                    continue;

                LoggerUtil.debug("LNSBILL:"+lnsbill.getPeriod()+"|"+lnsbill.getBillStatus()+"."+lnsbill.getBillType()+"|"+lnsbill.getBillBal());

                FabAmount minAmt = new FabAmount();

                Amount feeAmt=VarChecker.isEmpty(lnsbill.getRepayDateInt())?lnsbill.getBillBal():lnsbill.getRepayDateInt().add(lnsbill.getBillBal()).sub(lnsbill.getBillAmt());


                LoggerUtil.debug("minAmt:" + minAmt + " reminAmt" + getRepayAmt());
                if(repayAmt.sub(feeAmt).isPositive())
                {
                    minAmt.selfAdd(feeAmt);
                }else {
                    minAmt.selfAdd(repayAmt);
                }

                repayAmt.selfSub(feeAmt);


                if(repayAmtMapIn.get(lnsbill.getBillType()) == null)
                {
                    FabAmount amt = new FabAmount(minAmt.getVal());
                    repayAmtMapIn.put(lnsbill.getBillType(), amt);
                }
                else
                {
                    repayAmtMapIn.get(lnsbill.getBillType()).selfAdd(minAmt);
                }

                if(!getRepayAmt().isPositive())
                {
                    break;
                }
            }

        }
        if(getRepayAmt().isPositive() && Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate())))
        {

            List<RepayAdvance> prinAdvancelist = new ArrayList<RepayAdvance>();
            List<RepayAdvance> nintAdvancelist = new ArrayList<RepayAdvance>();
            if( ConstantDeclare.ISCALINT.ISCALINT_NO.equals(lnsbasicinfo.getIscalint()) )
                prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN;
            else if(ConstantDeclare.ISCALINT.ISCALINT_YES.equals(lnsbasicinfo.getIscalint()))
                prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_INTPRIN;
            if(ConstantDeclare.REPAYWAY.isEqualInterest(la.getWithdrawAgreement().getRepayWay()))
                endFlag = LoanInterestSettlementUtil.specialRepaymentBillPlan(ctx, subNo,lnsbasicinfo, new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,prePayFlag);
            else
                endFlag = LoanInterestSettlementUtil.interestRepaymentBillPlan(la, ctx, subNo, lnsbasicinfo, new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,prePayFlag);

            LoggerUtil.debug("BREAKWHILE:" + prinAdvancelist.size() + "|" + nintAdvancelist.size());

            if(!prinAdvancelist.isEmpty())
            {
                for(RepayAdvance prinAdvance : prinAdvancelist)
                {
                    prinAdvancesum.selfAdd(prinAdvance.getBillAmt());
                }
                if(repayAmtMapIn.get("PRIN") == null)
                {
                    repayAmtMapIn.put("PRIN", prinAdvancesum);
                }
                else
                {
                    repayAmtMapIn.get("PRIN").selfAdd(prinAdvancesum);
                }
                repayAmt.selfSub(prinAdvancesum);
            }
            if(!nintAdvancelist.isEmpty())
            {
                for(RepayAdvance nintAdvance : nintAdvancelist)
                {
                    nintAdvancesum.selfAdd(nintAdvance.getBillAmt());
                }
                if(repayAmtMapIn.get("NINT") == null)
                {
                    repayAmtMapIn.put("NINT", nintAdvancesum);
                }
                else
                {
                    repayAmtMapIn.get("NINT").selfAdd(nintAdvancesum);
                }
                repayAmt.selfSub(nintAdvancesum);
            }
        }




        prinAmt = new FabAmount();
        intAmt = new FabAmount();
        forfeitAmt = new FabAmount();
        feeAmt = new FabAmount();
        penaltyamt=new FabAmount();
        for (Map.Entry<String, FabAmount> entry : repayAmtMapIn.entrySet()) {
            if (entry.getKey().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
                prinAmt.selfAdd(entry.getValue());
            if (entry.getKey().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
                intAmt.selfAdd(entry.getValue());
            if (entry.getKey().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
                    || entry.getKey().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
                    || entry.getKey().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
                forfeitAmt.selfAdd(entry.getValue());
            if(LoanFeeUtils.isFeeType(entry.getKey()))
                feeAmt.selfAdd(entry.getValue());
            if(Arrays.asList("2412638","2412641","2412640").contains(la.getPrdId())){
                if(LoanFeeUtils.isPenalty(entry.getKey())){
                    feeAmt.selfAdd(entry.getValue());
                }
            }else{
                if(LoanFeeUtils.isPenalty(entry.getKey())){
                    penaltyamt.selfAdd(entry.getValue());
                }
            }
        }

    }



    /**
     * @return the acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }
    /**
     * @param acctNo the acctNo to set
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }
    /**
     * @return the ccy
     */
    public String getCcy() {
        return ccy;
    }
    /**
     * @param ccy the ccy to set
     */
    public void setCcy(String ccy) {
        this.ccy = ccy;
    }
    /**
     * @return the cashFlag
     */
    public String getCashFlag() {
        return cashFlag;
    }
    /**
     * @param cashFlag the cashFlag to set
     */
    public void setCashFlag(String cashFlag) {
        this.cashFlag = cashFlag;
    }
    /**
     * @return the termDate
     */
    public String getTermDate() {
        return termDate;
    }
    /**
     * @param termDate the termDate to set
     */
    public void setTermDate(String termDate) {
        this.termDate = termDate;
    }
    /**
     * @return the tranCode
     */
    public String getTranCode() {
        return tranCode;
    }
    /**
     * @param tranCode the tranCode to set
     */
    public void setTranCode(String tranCode) {
        this.tranCode = tranCode;
    }
    /**
     * @return the repayAmt
     */
    public FabAmount getRepayAmt() {
        return repayAmt;
    }
    /**
     * @param repayAmt the repayAmt to set
     */
    public void setRepayAmt(FabAmount repayAmt) {
        this.repayAmt = repayAmt;
    }
    /**
     * @return the brc
     */
    public String getBrc() {
        return brc;
    }
    /**
     * @param brc the brc to set
     */
    public void setBrc(String brc) {
        this.brc = brc;
    }

    /**
     * @return the subNo
     */
    public Integer getSubNo() {
        return subNo;
    }
    /**
     * @param subNo the subNo to set
     */
    public void setSubNo(Integer subNo) {
        this.subNo = subNo;
    }
    /**
     * @return the billStatistics
     */
    public LnsBillStatistics getBillStatistics() {
        return billStatistics;
    }
    /**
     * @param billStatistics the billStatistics to set
     */
    public void setBillStatistics(LnsBillStatistics billStatistics) {
        this.billStatistics = billStatistics;
    }
    /**
     * @return the intAmt
     */
    public FabAmount getIntAmt() {
        return intAmt;
    }
    /**
     * @param intAmt the intAmt to set
     */
    public void setIntAmt(FabAmount intAmt) {
        this.intAmt = intAmt;
    }
    /**
     * @return the prinAmt
     */
    public FabAmount getPrinAmt() {
        return prinAmt;
    }
    /**
     * @param prinAmt the prinAmt to set
     */
    public void setPrinAmt(FabAmount prinAmt) {
        this.prinAmt = prinAmt;
    }
    /**
     * @return the forfeitAmt
     */
    public FabAmount getForfeitAmt() {
        return forfeitAmt;
    }
    /**
     * @param forfeitAmt the forfeitAmt to set
     */
    public void setForfeitAmt(FabAmount forfeitAmt) {
        this.forfeitAmt = forfeitAmt;
    }
    /**
     * @return the endFlag
     */
    public String getEndFlag() {
        return endFlag;
    }
    /**
     * @param endFlag the endFlag to set
     */
    public void setEndFlag(String endFlag) {
        this.endFlag = endFlag;
    }
    /**
     * @return the sub
     */
    public AccountOperator getSub() {
        return sub;
    }
    /**
     * @param sub the sub to set
     */
    public void setSub(AccountOperator sub) {
        this.sub = sub;
    }
    /**
     * @return the eventProvider
     */
    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }
    /**
     * @param eventProvider the eventProvider to set
     */
    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;
    }

    public String getRepayDate() {
        return repayDate;
    }

    public void setRepayDate(String repayDate) {
        this.repayDate = repayDate;
    }

    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }
}

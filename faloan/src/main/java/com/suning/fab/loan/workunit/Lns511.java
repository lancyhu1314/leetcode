/*
 * Copyright (C), 2002-2019, 苏宁易购电子商务有限公司
 * FileName: Lns511.java
 * Author:   18049702
 * Date:     2018/5/16 19:41
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名    修改时间    版本号       描述
 */
package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉：利息计提 管理费
 *
 * @author 18049702
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns511 extends WorkUnit {

    String acctNo;      //借据号
    String repayDate;   //计提日期
//    TblLnsbasicinfo lnsbasicinfo;//贷款基本信息
    LoanAgreement loanAgreement;
    @Autowired
    LoanEventOperateProvider eventProvider;
    LnsBillStatistics billStatistics;

    int txnseq = 0;

    @Override
    public void run() throws Exception{
        //计提日期不能为空
        if (VarChecker.isEmpty(repayDate)){
            throw new FabException("LNS005");
        }

        TranCtx ctx = getTranctx();
        loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo,ctx , loanAgreement);

        //保费不计提
        boolean flag = true;
        //是否费用结清 默认结清20200207
        boolean isCA = true;
        String childBrc = null;
        for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){

            if(Arrays.asList(ConstantDeclare.FEETYPE.SQFE,ConstantDeclare.FEETYPE.RMFE).contains(lnsfeeinfo.getFeetype())
                    &&ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsfeeinfo.getProvisionflag())
                    //代偿的不计提
                    &&!"DC".equals(lnsfeeinfo.getReserv5())
                    ){
                flag= false;
                childBrc = lnsfeeinfo.getFeebrc();
                //有费用未结清20200207
                if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsfeeinfo.getFeestat())){
                    isCA = false;
                }
            }
        }
        if(flag)
            return;


        billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,repayDate , ctx, billStatistics);
        //统计所有包含费用的账本集合
        List<LnsBill> totalBills  = new ArrayList<>();
        totalBills.addAll(billStatistics.getHisBillList());
        totalBills.addAll(billStatistics.getBillInfoList());
        totalBills.addAll(billStatistics.getFutureBillInfoList());

        //统计账本  一次性和分期的
        LnsBill onetimeBill = null;
        List<LnsBill> lnsBills = new ArrayList<>();
        for(LnsBill lnsBill:totalBills){
            //不含费用的  担保费和风险管理费不会同时存在
            if( Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_SQFE,ConstantDeclare.BILLTYPE.RMFE)
                    .contains(lnsBill.getBillType()))
            {
                if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway()))
                    onetimeBill = lnsBill;
                else
                    lnsBills.add(lnsBill);
            }
        }
        //首先计提固定担保费
        if(onetimeBill!=null){
            //计提的金额 和税金
            FabAmount theAmt = new FabAmount();
            FabAmount theTax = new FabAmount();
            FabAmount totalAmt = new FabAmount();
            TblLnsprovision mfprovision = null;
            //优先是贷款结清
            if(isCA
                    &&ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(onetimeBill.getLnsfeeinfo().getProvisionflag())
                    ) {
                //已经结清的需要全部计提完
             //   mfprovision = getTblLnsprovisionreg(lnsBills.get(0).getBillType(),ConstantDeclare.INTERTYPE.SECURITFEE);
                mfprovision =LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),childBrc,acctNo,onetimeBill.getBillType(),ConstantDeclare.INTERTYPE.SECURITFEE,loanAgreement);
                totalAmt.selfAdd(onetimeBill.getBillAmt());
                //计算本次计提金额
                theAmt.selfAdd(totalAmt).selfSub(mfprovision.getTotalinterest());
                theTax.selfAdd(TaxUtil.calcVAT(totalAmt)).selfSub(mfprovision.getTotaltax());
                //计提close
                provisionflagClose(onetimeBill.getLnsfeeinfo());
                //是否计提判断
            }else if(ifProvision(onetimeBill)) {
                mfprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),childBrc,acctNo,onetimeBill.getBillType(),ConstantDeclare.INTERTYPE.SECURITFEE,loanAgreement);
               // mfprovision = getTblLnsprovisionreg(lnsBills.get(0).getBillType(),ConstantDeclare.INTERTYPE.SECURITFEE);
                String endDate = CalendarUtil.after(repayDate, loanAgreement.getContract().getContractEndDate())?loanAgreement.getContract().getContractEndDate():repayDate;
                //repayDate 在 管理费计提登记簿起始日期之间 需要*部分*计提
                BigDecimal lds = new BigDecimal(Integer.toString(CalendarUtil.actualDaysBetween(loanAgreement.getContract().getContractStartDate(), endDate)));//需要累加的天数
                BigDecimal tds = new BigDecimal(Integer.toString(CalendarUtil.actualDaysBetween(loanAgreement.getContract().getContractStartDate(), loanAgreement.getContract().getContractEndDate())));//区间总天数
                BigDecimal everAmt = BigDecimal.valueOf(onetimeBill.getBillAmt().getVal()).multiply(lds).divide(tds, 2, BigDecimal.ROUND_HALF_UP); //每日天需计提的管理费金额
                totalAmt.selfAdd(everAmt.doubleValue());
                theAmt.selfAdd(totalAmt).selfSub(mfprovision.getTotalinterest());
                theTax.selfAdd(TaxUtil.calcVAT(totalAmt)).selfSub(mfprovision.getTotaltax());  //加上本期需要计提的金额
                //计提close
                if(totalAmt.sub(onetimeBill.getBillAmt()).isZero())
                    provisionflagClose(onetimeBill.getLnsfeeinfo());
            }
            TblLnsprovisiondtl tblLnsprovisiondtl=null;
            if(theAmt.isPositive()||theTax.isPositive()) {
                //存计提登记簿
                tblLnsprovisiondtl=savePrvreg(ctx, childBrc, ctx.getTranDate(), mfprovision, theAmt, theTax, totalAmt, TaxUtil.calcVAT(totalAmt), ConstantDeclare.INTERTYPE.SECURITFEE
                        , ConstantDeclare.INTERFLAG.POSITIVE, ++txnseq,onetimeBill.getBillType(),loanAgreement.getContract().getCurrPrinPeriod());
                //利息计提 更新计提总表
                MapperUtil.map(tblLnsprovisiondtl, mfprovision, "map500_01");
                try {
                    if(mfprovision.isSaveFlag()){
                        DbAccessUtil.execute("Lnsprovision.updateByUk", mfprovision);
                    }else{
                        if(!"".equals(tblLnsprovisiondtl.getInterflag())){
                            DbAccessUtil.execute("Lnsprovision.insert", mfprovision);
                        }
                    }
                }catch(FabSqlException e){
                    LoggerUtil.info("插入计提明细总表异常：",e);
                    throw new FabException(e, "SPS102", "Lnsprovision");
                }
                //担保费用计提事件
                LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
                        ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,loanAgreement.getContract().getCcy());
                List<FabAmount> amtList = new ArrayList<>();
                amtList.add(theTax);
                eventProvider.createEvent(ConstantDeclare.EVENT.ACCRUEDFEE,
                        theAmt, acctinfo, null, getLoanAgreement().getFundInvest(),
                        ConstantDeclare.BRIEFCODE.DBJT, ctx, amtList, repayDate, ctx.getSerSeqNo(), txnseq, childBrc);
            }

            //暂时没有反向计提的
//            else if(!theAmt.isZero()||!theTax.isZero()){
//                theAmt =  new FabAmount(Math.abs(theAmt.getVal()));
//                theTax =  new FabAmount(Math.abs(theTax.getVal()));
//                savePrvreg(ctx, childBrc, repayDate, mfprovision, theAmt, theTax, totalAmt,
//                        TaxUtil.calcVAT(totalAmt),ConstantDeclare.INTERTYPE.SECURITFEE,ConstantDeclare.INTERFLAG.NEGATIVE,++txnseq);
//
//                //写事件
//                LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,
//                        ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency(lnsbasicinfo.getCcy()));
//                List<FabAmount> amtList = new ArrayList<>() ;
//                amtList.add(theTax);
//                //管理费计提  反向
//                // 所有的计提，摊销  都不记税金表
//                // AccountingModeChange.saveProvisionTax(ctx, acctNo, theInt.getVal(), theTax.getVal(),"JT", ConstantDeclare.INTERFLAG.NEGATIVE, ConstantDeclare.BILLTYPE.BILLTYPE_MINT);
//
//                eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFFEE,
//                        theAmt, acctinfo, null, getLoanAgreement().getFundInvest(),
//                        ConstantDeclare.BRIEFCODE.FYCX, ctx, amtList,repayDate, ctx.getSerSeqNo(), txnseq, childBrc);
//
//            }
        }
        //分期为空 不计提分期
        if(lnsBills.isEmpty())
            return;

        //分期的保费
        //优先是贷款结清
        TblLnsprovision mfprovision = null;
        FabAmount theAmt = new FabAmount();
        FabAmount theTax = new FabAmount();
        FabAmount totalAmt = new FabAmount();
        if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsBills.get(0).getLnsfeeinfo().getFeestat())
                &&ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBills.get(0).getLnsfeeinfo().getProvisionflag())
                ) {
            //已经结清的需要全部计提完
           // mfprovision = getTblLnsprovisionreg(lnsBills.get(0).getBillType(),ConstantDeclare.INTERTYPE.MANAGEFEE);
             mfprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),childBrc,acctNo,lnsBills.get(0).getBillType(),ConstantDeclare.INTERTYPE.MANAGEFEE,loanAgreement);
            for (LnsBill lnsBill : lnsBills){
                    //repayDate在管理费计提登记簿结束日期在之后 需要*整期*计提
                totalAmt.selfAdd(lnsBill.getBillAmt());

            }
            //计算本次计提金额
            theAmt.selfAdd(totalAmt).selfSub(mfprovision.getTotalinterest());
            theTax.selfAdd(TaxUtil.calcVAT(totalAmt)).selfSub(mfprovision.getTotaltax());

            provisionflagClose(lnsBills.get(0).getLnsfeeinfo());
            //是否计提判断
        }else if(ifProvision(lnsBills.get(0))) {
       //     mfprovision = getTblLnsprovisionreg(lnsBills.get(0).getBillType(),ConstantDeclare.INTERTYPE.MANAGEFEE);
            mfprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),childBrc,acctNo,lnsBills.get(0).getBillType(),ConstantDeclare.INTERTYPE.MANAGEFEE,loanAgreement);
            int size = 0;
            //遍历管理费登记簿  统计到repaydate为止应计提的所有的管理费
            for (LnsBill lnsBill : lnsBills){
                if(CalendarUtil.beforeAlsoEqual(lnsBill.getEndDate(), repayDate)){
                    //repayDate在管理费计提登记簿结束日期在之后 需要*整期*计提
                    totalAmt.selfAdd(lnsBill.getBillAmt());
                    size++;
                }else if(CalendarUtil.after(repayDate, lnsBill.getStartDate()) && CalendarUtil.beforeAlsoEqual(repayDate, lnsBill.getEndDate())){
                    //repayDate 在 管理费计提登记簿起始日期之间 需要*部分*计提
                    totalAmt.selfAdd(repayDateFee(lnsBill));//加上本期需要计提的金额
                    //到了当前期  之后的管理费账本为未来期 break
                    break;
                }
            }

            theAmt.selfAdd(totalAmt).selfSub(mfprovision.getTotalinterest());
            theTax.selfAdd(TaxUtil.calcVAT(totalAmt)).selfSub(mfprovision.getTotaltax());  //加上本期需要计提的金额
            //计提结束
            if(size==lnsBills.size())   provisionflagClose(lnsBills.get(0).getLnsfeeinfo());

        }
        //theInt和theTax 管理费 税金都为0时不计提也不冲销
        if(theAmt.isZero()&&theTax.isZero()){
            return;
        }

        TblLnsprovisiondtl tblLnsprovisiondtl=null;
        //组装登记入库逻辑
        if (theAmt.isPositive() || theTax.isPositive()){
            tblLnsprovisiondtl=savePrvreg(ctx, childBrc, repayDate, mfprovision, theAmt, theTax, totalAmt,
                    TaxUtil.calcVAT(totalAmt),ConstantDeclare.INTERTYPE.MANAGEFEE,ConstantDeclare.INTERFLAG.POSITIVE,++txnseq,lnsBills.get(0).getBillType(),loanAgreement.getContract().getCurrPrinPeriod());
            //写事件
            LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
                    ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, loanAgreement.getContract().getCcy());
            List<FabAmount> amtList = new ArrayList<>() ;
            amtList.add(theTax);
            //管理费计提
            // 所有的计提，摊销  都不记税金表
            //AccountingModeChange.saveProvisionTax(ctx, acctNo, theInt.getVal(), theTax.getVal(), "JT",ConstantDeclare.INTERFLAG.POSITIVE, ConstantDeclare.BILLTYPE.BILLTYPE_MINT);

            eventProvider.createEvent(ConstantDeclare.EVENT.ACCRUEDFEE,
                    theAmt, acctinfo, null, getLoanAgreement().getFundInvest(),
                    ConstantDeclare.BRIEFCODE.FYJT, ctx, amtList,repayDate, ctx.getSerSeqNo(), txnseq, childBrc);

        }else {
            theAmt =  new FabAmount(Math.abs(theAmt.getVal()));
            theTax =  new FabAmount(Math.abs(theTax.getVal()));
            tblLnsprovisiondtl=savePrvreg(ctx, childBrc, repayDate, mfprovision, theAmt, theTax, totalAmt,
                    TaxUtil.calcVAT(totalAmt),ConstantDeclare.INTERTYPE.MANAGEFEE,ConstantDeclare.INTERFLAG.NEGATIVE,++txnseq,lnsBills.get(0).getBillType(),loanAgreement.getContract().getCurrPrinPeriod());

            //写事件
            LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
                    ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, loanAgreement.getContract().getCcy());
            List<FabAmount> amtList = new ArrayList<>() ;
            amtList.add(theTax);
            //管理费计提  反向
            // 所有的计提，摊销  都不记税金表
            // AccountingModeChange.saveProvisionTax(ctx, acctNo, theInt.getVal(), theTax.getVal(),"JT", ConstantDeclare.INTERFLAG.NEGATIVE, ConstantDeclare.BILLTYPE.BILLTYPE_MINT);

            eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFFEE,
                    theAmt, acctinfo, null, getLoanAgreement().getFundInvest(),
                    ConstantDeclare.BRIEFCODE.FYCX, ctx, amtList,repayDate, ctx.getSerSeqNo(), txnseq, childBrc);

        }
        //利息计提 更新计提总表
        MapperUtil.map(tblLnsprovisiondtl, mfprovision, "map500_01");
        try {
            if(mfprovision.isSaveFlag()){
                DbAccessUtil.execute("Lnsprovision.updateByUk", mfprovision);
            }else{
                if(!"".equals(tblLnsprovisiondtl.getInterflag())){
                    DbAccessUtil.execute("Lnsprovision.insert", mfprovision);
                }
            }
        }catch (FabSqlException e){
            LoggerUtil.info("插入计提明细总表异常：",e);
            throw new FabException(e, "SPS102", "Lnsprovision");
        }

    }

    private Double  repayDateFee(LnsBill lnsBill) {

        if(!VarChecker.isEmpty(lnsBill.getRepayDateInt())
            && lnsBill.getBillAmt().sub(lnsBill.getRepayDateInt()).isPositive()){
            return lnsBill.getRepayDateInt().getVal();
        }
        BigDecimal lds =  BigDecimal.valueOf(CalendarUtil.actualDaysBetween(lnsBill.getStartDate(), repayDate));//需要累加的天数
        BigDecimal tds =  BigDecimal.valueOf(CalendarUtil.actualDaysBetween(lnsBill.getStartDate(),lnsBill.getEndDate()));//区间总天数
        return BigDecimal.valueOf(lnsBill.getBillAmt().getVal()).multiply(lds).divide(tds, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

//    private TblLnsprovision getTblLnsprovision(String billtype, String intertype) throws FabException {
//
//        TblLnsprovisionreg mfprovision;Map<String,Object> provparam = new HashMap<>();
//        provparam.put("receiptno", acctNo);
//        provparam.put("intertype", intertype);
//        provparam.put("billtype",billtype);
//        provparam.put("brc",lnsbasicinfo.getOpenbrc());
//        try {
//            mfprovision = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", provparam, TblLnsprovisionreg.class);
//        }catch (FabSqlException e){
//            throw new FabException(e, "SPS103", "lnsprovision");
//        }
//        if (null == mfprovision){//第一次计提管理费
//            mfprovision = new TblLnsprovisionreg();
//            mfprovision.setPeriod(0);//上一次为第0期
//            //第一期管理费起始日作为上一期计提的结束日期
//            mfprovision.setEnddate(Date.valueOf(loanAgreement.getContract().getContractStartDate()));
//            mfprovision.setTotaltax(0.00);
//            mfprovision.setTotalinterest(0.00);
//            //整比逾期后不再计提
//        }
//        return mfprovision;
//
//    }
    private void provisionflagClose(TblLnsfeeinfo lnsfeeinfo) throws FabException{
        //主文件计提标志

            Map<String,Object> basicinfo = new HashMap<String,Object>();
            basicinfo.put("acctno", lnsfeeinfo.getAcctno());
            basicinfo.put("openbrc", lnsfeeinfo.getOpenbrc());
            basicinfo.put("repayway", lnsfeeinfo.getRepayway());
            basicinfo.put("feetype", lnsfeeinfo.getFeetype());
            basicinfo.put("provisionflag", ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);

            int i = 0;
            try {
                i = DbAccessUtil.execute("Lnsfeeinfo.update_provisionflag", basicinfo);
            }
            catch (FabSqlException e)
            {
                throw new FabException(e, "SPS102", "Lnsfeeinfo");
            }
            if(1 != i){
                throw new FabException("ACC108", lnsfeeinfo.getAcctno());
            }

    }

    private TblLnsprovisiondtl savePrvreg(TranCtx ctx, String childBrc, String virRepayDate, TblLnsprovision mfprovision,
                            FabAmount theInt, FabAmount theTax, FabAmount totalInt, FabAmount totalTax,String intertype,String interflag,Integer txnseq,String billtype,Integer period) throws FabException {
        //登记计提登记薄
        TblLnsprovisiondtl prvreg = new TblLnsprovisiondtl();
        //交易日期
        prvreg.setTrandate(Date.valueOf(repayDate));
        //账务流水号
        prvreg.setSerseqno(ctx.getSerSeqNo());
        //公司号
        prvreg.setBrc(ctx.getBrc());
        //借据
        prvreg.setAcctno(acctNo);
        //期数
        prvreg.setPeriod(period);
        prvreg.setListno(mfprovision.getTotallist()+1);
        //账单类型 NINT利息 DINT罚息 CINT复利 MINT管理费
        prvreg.setBilltype(mfprovision.getBilltype());
        //币种
        prvreg.setCcy(loanAgreement.getContract().getCcy().getCcy());
        //计提利息(管理费)总金额
        prvreg.setTotalinterest(totalInt.getVal());
        //计提税金总金额
        prvreg.setTotaltax(totalTax.getVal());
        //税率
        prvreg.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
        //本次入账利息(管理费)金额
        prvreg.setInterest(theInt.getVal());
        //本次入账税金金额
        prvreg.setTax(theTax.getVal());
        //费用类型 MANAGEFEE管理费
        prvreg.setIntertype(mfprovision.getIntertype());
        //开始日期
        prvreg.setBegindate(mfprovision.getLastenddate());
        //结束日期
        prvreg.setEnddate(Date.valueOf(virRepayDate));
        //正反标志 取值：POSITIVE正向，NEGATIVE反向
        prvreg.setInterflag(interflag);
        //是否作废标志 取值：NORMAL正常，CANCEL作废
        prvreg.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
        //子机构
        prvreg.setChildbrc(childBrc);
        //入表
        LoanProvisionProvider.exchangeProvision(prvreg);
        try {
            DbAccessUtil.execute("Lnsprovisiondtl.insert", prvreg);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS100", "Lnsprovisiondtl-feemanage");
        }
        return prvreg;
    }

    public boolean ifProvision(LnsBill lnsBill){
        //正在计提中
//        if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getLnsfeeinfo().getProvisionflag())){

            //月底计提
            if (ConstantDeclare.PROVISIONRULE.BYTERM.equals(lnsBill.getLnsfeeinfo().getProvisionrule())
                    && CalendarUtil.isMonthEnd(repayDate)
                   ) {
                return true;
            }
            if(ConstantDeclare.PROVISIONRULE.BYDAY.equals(lnsBill.getLnsfeeinfo().getProvisionrule())){
                return true;
            }
//        }
        return false;
    }

    /**
     * @Return java.lang.String acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }

    /**
     * @param acctNo
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    /**
     * @Return java.lang.String repayDate
     */
    public String getRepayDate() {
        return repayDate;
    }

    /**
     * @param repayDate
     */
    public void setRepayDate(String repayDate) {
        this.repayDate = repayDate;
    }


    public LoanAgreement getLoanAgreement(){
        return loanAgreement;
    }

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

    public int getTxnseq() {
        return txnseq;
    }

    public void setTxnseq(int txnseq) {
        this.txnseq = txnseq;
    }
}

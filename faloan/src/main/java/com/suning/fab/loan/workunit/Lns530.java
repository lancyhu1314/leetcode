package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.domain.TblLnspenintprovregdtl;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：费用对应的违约金计提
 *
 * @Author 18049705 MYP
 * @Date Created in 14:18 2020/3/27
 * @see
 */
@Scope("prototype")
@Repository
public class Lns530 extends WorkUnit {


    private String acctNo;      //借据号
    private String repayDate;   //计提日期
    private LoanAgreement loanAgreement;
    private LnsBillStatistics billStatistics;
    private int txnseq = 0;
    @Autowired
    LoanEventOperateProvider eventProvider;
    @Autowired
    @Qualifier("accountAdder")
    AccountOperator add;


    @Override
    public void run() throws Exception {


        loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo,tranctx , loanAgreement);

        boolean hasPenalty = false;
        //判断是否有违约金
        if(!loanAgreement.getFeeAgreement().isEmpty()){
            for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
                if(lnsfeeinfo.getOverrate().compareTo(0.0)>0){
                    hasPenalty = true;
                }
            }
        }
        //不存在违约金
        if(!hasPenalty){
            return;
        }
        //试算信息结果
        billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,repayDate , tranctx, billStatistics);
        //包含违约金的账本
        List<LnsBill> selectedbills = new ArrayList<>();
        selectedbills.addAll(billStatistics.getHisSetIntBillList());
        selectedbills.addAll(billStatistics.getFutureOverDuePrinIntBillList());
//        selectedbills.addAll(billStatistics.getHisBillList());
        List<LnsBill> feebills = new ArrayList<>();
        feebills.addAll(billStatistics.getBillInfoList());
        feebills.addAll(billStatistics.getHisBillList());

        //获取呆滞呆账期间新罚息复利账单list
        //核销的不获取呆滞呆账期间的新罚息复利
        if( !VarChecker.asList( "479001","472006").contains(tranctx.getTranCode()) )
        {
            List<LnsBill> cdbillList = billStatistics.getCdbillList();
            selectedbills.addAll(cdbillList);
        }

        //按类型统计违约金
        Map<String,FabAmount> pints = new HashMap<>();
        //按类型统计违约金
        Map<String,String> feeBrc = new HashMap<>();
        //累加罚息账单金额
        for (LnsBill bill : selectedbills){
            if(bill.isPenalty()){
                if(pints.get(bill.getBillType())==null){
                    pints.put(bill.getBillType(), new FabAmount());

                    matchFeeBrc(feebills, feeBrc, bill);
                }

                pints.get(bill.getBillType()).selfAdd(bill.getBillAmt());
            }
        }
        //累加罚息账单金额历史期
        for (LnsBill bill : billStatistics.getHisBillList()){
            if(bill.isPenalty()){
                if(pints.get(bill.getBillType())==null)
                    pints.put(bill.getBillType(), new FabAmount());
                pints.get(bill.getBillType()).selfAdd(bill.getBillAmt());
            }
        }
        for( Map.Entry<String,FabAmount> entry : pints.entrySet()){
            provision(entry.getKey(),entry.getValue(),selectedbills,feeBrc);
        }



    }

    private void matchFeeBrc(List<LnsBill> feebills, Map<String, String> feeBrc, LnsBill bill) {
        for(LnsBill lnsBill:feebills)
        {
            //三编码匹配上 对应的费用账本
            if(bill.getDerTranDate().equals(lnsBill.getTranDate())
                    &&bill.getDerTxSeq().equals(lnsBill.getTxSeq())
                    &&bill.getDerSerseqno().equals(lnsBill.getSerSeqno())){
                //费用账本匹配上对应的 费用信息  得到 费用机构号
                feeBrc.put(bill.getBillType(), lnsBill.getLnsfeeinfo().getFeebrc());
            }
        }
    }

    private void provision(String billtype,FabAmount totalPint, List<LnsBill> bills,Map<String,String> feeBrc) throws FabException {
        //取计提登记簿信息
        TblLnspenintprovreg penintprovisionPint = null;
        Map<String, Object> param = new HashMap<>();
        param.put("receiptno", acctNo);
        param.put("brc", tranctx.getBrc());
        param.put("billtype", billtype);
        penintprovisionPint = LoanDbUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);

        if (null == penintprovisionPint){
            penintprovisionPint = new TblLnspenintprovreg();
            penintprovisionPint.setBrc(tranctx.getBrc());
            penintprovisionPint.setReceiptno(acctNo);
            penintprovisionPint.setCcy(loanAgreement.getContract().getCcy().getCcy());
            penintprovisionPint.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")));
            penintprovisionPint.setTotalinterest(BigDecimal.valueOf(0.00));
            penintprovisionPint.setTotaltax(BigDecimal.valueOf(0.00));
            penintprovisionPint.setBegindate(java.sql.Date.valueOf(loanAgreement.getContract().getContractStartDate()));
            penintprovisionPint.setTotallist(0);
            penintprovisionPint.setTimestamp(new Date().toString());
            penintprovisionPint.setBilltype(billtype);
            //直接插入表中后面直接更新
            try{
                DbAccessUtil.execute("Lnspenintprovreg.insert", penintprovisionPint);
            }catch (FabSqlException e){
                throw new FabException(e, "SPS100", "Lnspenintprovreg");
            }
        }


        //需要计提事件
        if(totalPint.sub(penintprovisionPint.getTotalinterest().doubleValue()).isPositive()){

            //插入明细
            for (LnsBill billDetail : bills) {

                if (billDetail.getBillType().equals(billtype))
                    insertDtl(penintprovisionPint, billDetail);

            }

            //本次应计提的金额=罚息账单的账单金额（数据库表里面的）+试算出来的罚息账单金额-已计提出来的总金额
            FabAmount interest = new FabAmount(totalPint.sub(penintprovisionPint.getTotalinterest().doubleValue()).getVal());
            List<FabAmount> amtpintList = new ArrayList<>();
            amtpintList.add(new FabAmount(TaxUtil.calcVAT(totalPint).sub(penintprovisionPint.getTotaltax().doubleValue()).getVal()));
            penintprovisionPint.setTotalinterest( BigDecimal.valueOf(totalPint.getVal()) );
            penintprovisionPint.setTotaltax( BigDecimal.valueOf(TaxUtil.calcVAT(totalPint).getVal()) );
            penintprovisionPint.setEnddate(java.sql.Date.valueOf(repayDate));
            penintprovisionPint.setBilltype(billtype);
            LoanDbUtil.update("Lnspenintprovreg.updateByUk", penintprovisionPint);

            String briefCode ;
            if(ConstantDeclare.BILLTYPE.BILLTYPE_DBWY.equals(billtype)){
                briefCode = ConstantDeclare.BRIEFCODE.DBJT;
            }else {
                briefCode =  ConstantDeclare.BRIEFCODE.FWJT;
            }
            LnsAcctInfo lnsPintAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEED, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
                    new FabCurrency(),feeBrc.get(billtype));
            add.operate(lnsPintAcctInfo, null, interest, loanAgreement.getFundInvest(), briefCode, tranctx);
            //罚息计提
            AccountingModeChange.saveProvisionTax(tranctx, acctNo, interest.getVal(), amtpintList.get(0).getVal(), "JT",ConstantDeclare.INTERFLAG.POSITIVE, billtype);
           //reserv1 费用机构号
            eventProvider.createEvent(ConstantDeclare.EVENT.DEFUALPROV, interest, lnsPintAcctInfo, null,
                    loanAgreement.getFundInvest(), briefCode, tranctx, amtpintList,feeBrc.get(billtype));
        }
    }

    private void insertDtl(  TblLnspenintprovreg penintprovision, LnsBill billDetail) throws FabException {
        TblLnspenintprovregdtl penintprovregdtl = new TblLnspenintprovregdtl();
        //登记计提明细登记簿
        penintprovregdtl.setReceiptno(acctNo);
        penintprovregdtl.setBrc(tranctx.getBrc());
        penintprovregdtl.setCcy(loanAgreement.getContract().getCcy().getCcy());
        //总表的条数
        penintprovision.setTotallist(penintprovision.getTotallist() + 1);
        //计提登记详细表
        penintprovregdtl.setTrandate(java.sql.Date.valueOf(tranctx.getTranDate()));
        penintprovregdtl.setSerseqno(tranctx.getSerSeqNo());
        penintprovregdtl.setTxnseq( ++txnseq );
        penintprovregdtl.setPeriod(billDetail.getPeriod());
        penintprovregdtl.setListno(penintprovision.getTotallist());
        penintprovregdtl.setBilltype(billDetail.getBillType());
        penintprovregdtl.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
        penintprovregdtl.setBegindate(java.sql.Date.valueOf(billDetail.getStartDate()));
        penintprovregdtl.setEnddate(java.sql.Date.valueOf(billDetail.getEndDate()));
        penintprovregdtl.setTimestamp(new Date().toString());
        penintprovregdtl.setInterest(billDetail.getBillAmt().getVal());
        penintprovregdtl.setTax(TaxUtil.calcVAT(new FabAmount(penintprovregdtl.getInterest())).getVal());

        //登记详表
        if( new FabAmount(penintprovregdtl.getInterest()).isPositive() )
        {
            LoanDbUtil.insert("Lnspenintprovregdtl.insert", penintprovregdtl);
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
     * Gets the value of repayDate.
     *
     * @return the value of repayDate
     */
    public String getRepayDate() {
        return repayDate;
    }

    /**
     * Sets the repayDate.
     *
     * @param repayDate repayDate
     */
    public void setRepayDate(String repayDate) {
        this.repayDate = repayDate;

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
     * Gets the value of txnseq.
     *
     * @return the value of txnseq
     */
    public int getTxnseq() {
        return txnseq;
    }

    /**
     * Sets the txnseq.
     *
     * @param txnseq txnseq
     */
    public void setTxnseq(int txnseq) {
        this.txnseq = txnseq;

    }
}

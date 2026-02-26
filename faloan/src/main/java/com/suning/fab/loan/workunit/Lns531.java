package com.suning.fab.loan.workunit;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：费用对应的违约金落表
 *
 * @Author 18049705 MYP
 * @Date Created in 14:22 2020/3/27
 * @see
 */
@Scope("prototype")
@Repository
public class Lns531 extends WorkUnit {

    String	acctNo;
    String	repayDate;
    LnsBillStatistics lnsBillStatistics;
    TblLnsbasicinfo lnsbasicinfo;
    LoanAgreement loanAgreement;


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

    @Override
    public void run() throws Exception {

        //获取贷款协议信息
        loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx,loanAgreement);

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
        lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,repayDate , tranctx, lnsBillStatistics);
        List<LnsBill> billList = new ArrayList<>();
        billList.addAll(lnsBillStatistics.getHisSetIntBillList());
        billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());


        for (LnsBill bill : billList){



            if (bill.isPenalty()){
                TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(loanAgreement,bill,tranctx);
                tblLnsbill.setTrandate(Date.valueOf(tranctx.getTranDate()));
                tblLnsbill.setSerseqno(tranctx.getSerSeqNo());
                bill.setTranDate(tblLnsbill.getTrandate());
                bill.setSerSeqno(tranctx.getSerSeqNo());
                if(tblLnsbill.getTxseq() == 0)
                {
                    //优化插入账本插入数据的问题 账本数量太多
                    lnsBillStatistics.setBillNo(lnsBillStatistics.getBillNo()+1);
                    tblLnsbill.setTxseq(lnsBillStatistics.getBillNo());
                }

                if(bill.getBillAmt().isPositive())
                {
                    pepareDerCoding(bill,tblLnsbill);
                    LoanDbUtil.insert("Lnsbill.insert",tblLnsbill);


                    Map<String, Object> param = new HashMap<String, Object>();

                    pepareDerCoding(tranctx, bill, param);

                    try {
                        DbAccessUtil.execute("CUSTOMIZE.update_hisbill_513", param);
                    } catch (FabException e) {
                        throw new FabException(e, "SPS102", "lnsbills");
                    }

                }

            }
        }
    }

    private void pepareDerCoding(TranCtx ctx, LnsBill bill, Map<String, Object> param) {
        if(!VarChecker.isEmpty(bill.getHisBill())){
            param.put("trandate", bill.getHisBill().getTranDate());
            param.put("serseqno", bill.getHisBill().getSerSeqno());
            param.put("txseq", bill.getHisBill().getTxSeq());
            param.put("intedate1",ctx.getTranDate());
        }else{
            param.put("trandate", bill.getDerTranDate());
            param.put("serseqno", bill.getDerSerseqno());
            param.put("txseq", bill.getDerTxSeq());
            param.put("intedate1",ctx.getTranDate());
        }

    }
    private void pepareDerCoding(LnsBill bill, TblLnsbill tblLnsbill) {
        if(!VarChecker.isEmpty(bill.getHisBill())){
            tblLnsbill.setDerserseqno(bill.getHisBill().getSerSeqno());
            tblLnsbill.setDertrandate( bill.getHisBill().getTranDate());
            tblLnsbill.setDertxseq( bill.getHisBill().getTxSeq());
        }

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
     * Gets the value of lnsbasicinfo.
     *
     * @return the value of lnsbasicinfo
     */
    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    /**
     * Sets the lnsbasicinfo.
     *
     * @param lnsbasicinfo lnsbasicinfo
     */
    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;

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

}

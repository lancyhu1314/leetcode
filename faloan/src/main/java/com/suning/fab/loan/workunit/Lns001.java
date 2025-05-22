package com.suning.fab.loan.workunit;

import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：试算子交易
 *
 * @Author 18049705 MYP
 * @Date Created in 10:11 2019/11/18
 * @see
 */
@Scope("prototype")
@Repository
public class Lns001 extends WorkUnit {

    //账号
    private String acctNo;
    private String receiptNo;
    private String repayDate;

    private LoanAgreement loanAgreement;
    private LnsBillStatistics lnsBillStatistics;
    @Override
    public void run() throws Exception {

        //有时候是receiotno
        acctNo = VarChecker.isEmpty(acctNo)?receiptNo:acctNo;

        //读取账户基本信息
         loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);

         //没有repayDate  默认用账务日期
         repayDate =  VarChecker.isEmpty(repayDate)?tranctx.getTranDate():repayDate;

        //取试算信息
         lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,repayDate ,tranctx);


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
     * Gets the value of receiptNo.
     *
     * @return the value of receiptNo
     */
    public String getReceiptNo() {
        return receiptNo;
    }

    /**
     * Sets the receiptNo.
     *
     * @param receiptNo receiptNo
     */
    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;

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

}

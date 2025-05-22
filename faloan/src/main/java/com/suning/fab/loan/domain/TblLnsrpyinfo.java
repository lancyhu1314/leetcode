package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSRPYINFO")
public class TblLnsrpyinfo {
    private String acctno = "";

    private Integer repayterm = 0;

    private String acctstat = "";

    private String trandate = "";

    private Integer seqno = 0;

    private String brc = "";

    private String profitbrc = "";

    private String diacct = "";

    private String ccy = "";

    private Double tranamt = 0.00;

    private String flag = "";

    private String loantype1 = "";

    private String loantype2 = "";

    private String loantype3 = "";

    private String rpyflag = "";

    private Double repayamt = 0.00;

    private Double noretamt = 0.00;

    private String bdate = "";

    private String edate = "";

    private Double execurate = 0.00;

    public TblLnsrpyinfo() {
         
    }

    /** 本金账号 */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /** 本金账号 */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    /** 还款期数 */
    @Column(name = "Repayterm")
    public Integer getRepayterm() {
        return repayterm;
    }

    /** 还款期数 */
    public void setRepayterm(Integer repayterm) {
        if(repayterm == null){
            this.repayterm = 0;
        }else{
            this.repayterm = repayterm;
        }
    }

    /** 分户状态 */
    @Column(name = "Acctstat")
    public String getAcctstat() {
        return acctstat;
    }

    /** 分户状态 */
    public void setAcctstat(String acctstat) {
        this.acctstat = acctstat == null ? "" : acctstat.trim();
    }

    /** 账务日期 */
    @Column(name = "Trandate")
    public String getTrandate() {
        return trandate;
    }

    /** 账务日期 */
    public void setTrandate(String trandate) {
        this.trandate = trandate == null ? "" : trandate.trim();
    }

    /** 流水号 */
    @Column(name = "Seqno")
    public Integer getSeqno() {
        return seqno;
    }

    /** 流水号 */
    public void setSeqno(Integer seqno) {
        if(seqno == null){
            this.seqno = 0;
        }else{
            this.seqno = seqno;
        }
    }

    /** 开户机构 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 开户机构 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    /** 核算机构 */
    @Column(name = "Profitbrc")
    public String getProfitbrc() {
        return profitbrc;
    }

    /** 核算机构 */
    public void setProfitbrc(String profitbrc) {
        this.profitbrc = profitbrc == null ? "" : profitbrc.trim();
    }

    /** 对方账号 */
    @Column(name = "Diacct")
    public String getDiacct() {
        return diacct;
    }

    /** 对方账号 */
    public void setDiacct(String diacct) {
        this.diacct = diacct == null ? "" : diacct.trim();
    }

    /** 币种 */
    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    /** 币种 */
    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    /** 交易金额 */
    @Column(name = "Tranamt")
    public Double getTranamt() {
        return tranamt;
    }

    /** 交易金额 */
    public void setTranamt(Double tranamt) {
        if(tranamt == null){
            this.tranamt = 0.00;
        }else{
            this.tranamt = tranamt;
        }
    }

    /** 还款方式 */
    @Column(name = "Flag")
    public String getFlag() {
        return flag;
    }

    /** 还款方式 */
    public void setFlag(String flag) {
        this.flag = flag == null ? "" : flag.trim();
    }

    /** 贷款种类1 */
    @Column(name = "Loantype1")
    public String getLoantype1() {
        return loantype1;
    }

    /** 贷款种类1 */
    public void setLoantype1(String loantype1) {
        this.loantype1 = loantype1 == null ? "" : loantype1.trim();
    }

    /** 贷款种类2 */
    @Column(name = "Loantype2")
    public String getLoantype2() {
        return loantype2;
    }

    /** 贷款种类2 */
    public void setLoantype2(String loantype2) {
        this.loantype2 = loantype2 == null ? "" : loantype2.trim();
    }

    /** 贷款种类3 */
    @Column(name = "Loantype3")
    public String getLoantype3() {
        return loantype3;
    }

    /** 贷款种类3 */
    public void setLoantype3(String loantype3) {
        this.loantype3 = loantype3 == null ? "" : loantype3.trim();
    }

    /** 还款标致 */
    @Column(name = "Rpyflag")
    public String getRpyflag() {
        return rpyflag;
    }

    /** 还款标致 */
    public void setRpyflag(String rpyflag) {
        this.rpyflag = rpyflag == null ? "" : rpyflag.trim();
    }

    /** 应还金额 */
    @Column(name = "Repayamt")
    public Double getRepayamt() {
        return repayamt;
    }

    /** 应还金额 */
    public void setRepayamt(Double repayamt) {
        if(repayamt == null){
            this.repayamt = 0.00;
        }else{
            this.repayamt = repayamt;
        }
    }

    /** 本金余额 */
    @Column(name = "Noretamt")
    public Double getNoretamt() {
        return noretamt;
    }

    /** 本金余额 */
    public void setNoretamt(Double noretamt) {
        if(noretamt == null){
            this.noretamt = 0.00;
        }else{
            this.noretamt = noretamt;
        }
    }

    /** 起息日期 */
    @Column(name = "Bdate")
    public String getBdate() {
        return bdate;
    }

    /** 起息日期 */
    public void setBdate(String bdate) {
        this.bdate = bdate == null ? "" : bdate.trim();
    }

    /** 止息日期 */
    @Column(name = "Edate")
    public String getEdate() {
        return edate;
    }

    /** 止息日期 */
    public void setEdate(String edate) {
        this.edate = edate == null ? "" : edate.trim();
    }

    /** 执行利率 */
    @Column(name = "Execurate")
    public Double getExecurate() {
        return execurate;
    }

    /** 执行利率 */
    public void setExecurate(Double execurate) {
        if(execurate == null){
            this.execurate = 0.00;
        }else{
            this.execurate = execurate;
        }
    }
}
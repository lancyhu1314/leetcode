package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSBILLPLAN")
public class TblLnsbillplan {
    private String acctno = "";

    private String brc = "";

    private String billtype = "";

    private Integer period = 0;

    private String trandate = "";

    private Integer serseqno = 0;

    private Integer txseq = 0;

    private Double billamt = 0.00;

    private Double prinbal = 0.00;

    private String begindate = "";

    private String enddate = "";

    private String repayedate = "";

    private String timestamp = "";

    public TblLnsbillplan() {
         
    }

    /** 账号 */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /** 账号 */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    /** 公司代码 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 公司代码 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    /** 账单类型 */
    @Column(name = "Billtype")
    public String getBilltype() {
        return billtype;
    }

    /** 账单类型 */
    public void setBilltype(String billtype) {
        this.billtype = billtype == null ? "" : billtype.trim();
    }

    /** 期数 */
    @Column(name = "Period")
    public Integer getPeriod() {
        return period;
    }

    /** 期数 */
    public void setPeriod(Integer period) {
        if(period == null){
            this.period = 0;
        }else{
            this.period = period;
        }
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
    @Column(name = "Serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }

    /** 流水号 */
    public void setSerseqno(Integer serseqno) {
        if(serseqno == null){
            this.serseqno = 0;
        }else{
            this.serseqno = serseqno;
        }
    }

    /** 子序号 */
    @Column(name = "Txseq")
    public Integer getTxseq() {
        return txseq;
    }

    /** 子序号 */
    public void setTxseq(Integer txseq) {
        if(txseq == null){
            this.txseq = 0;
        }else{
            this.txseq = txseq;
        }
    }

    /** 账单金额 */
    @Column(name = "Billamt")
    public Double getBillamt() {
        return billamt;
    }

    /** 账单金额 */
    public void setBillamt(Double billamt) {
        if(billamt == null){
            this.billamt = 0.00;
        }else{
            this.billamt = billamt;
        }
    }

    /** 账单对应本金余额 */
    @Column(name = "Prinbal")
    public Double getPrinbal() {
        return prinbal;
    }

    /** 账单对应本金余额 */
    public void setPrinbal(Double prinbal) {
        if(prinbal == null){
            this.prinbal = 0.00;
        }else{
            this.prinbal = prinbal;
        }
    }

    /** 账单起始日期 */
    @Column(name = "Begindate")
    public String getBegindate() {
        return begindate;
    }

    /** 账单起始日期 */
    public void setBegindate(String begindate) {
        this.begindate = begindate == null ? "" : begindate.trim();
    }

    /** 账单结束日期 */
    @Column(name = "Enddate")
    public String getEnddate() {
        return enddate;
    }

    /** 账单结束日期 */
    public void setEnddate(String enddate) {
        this.enddate = enddate == null ? "" : enddate.trim();
    }

    /** 账单应还款止日 */
    @Column(name = "Repayedate")
    public String getRepayedate() {
        return repayedate;
    }

    /** 账单应还款止日 */
    public void setRepayedate(String repayedate) {
        this.repayedate = repayedate == null ? "" : repayedate.trim();
    }

    /** 时间戳 */
    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    /** 时间戳 */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
    }
}
package com.suning.fab.loan.domain;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSPROVISIONREG")
public class TblLnsprovisionreg {
    private String receiptno = "";

    private String intertype = "";

    private Integer period = 0;

    private Date trandate = new Date(0);

    private Integer serseqno = 0;

    private Integer txnseq = 0;

    private String brc = "";

    private String teller = "";

    private String billtype = "";

    private String ccy = "";

    private Double totalinterest = 0.00;

    private Double totaltax = 0.00;

    private Double taxrate = 0.00;

    private Double interest = 0.00;

    private Double tax = 0.00;

    private Date begindate = new Date(0);

    private Date enddate = new Date(0);

    private String interflag = "";

    private String sendflag = "";

    private Integer sendnum = 0;

    private String cancelflag = "";

    private String timestamp = "";

    private String reserv1 = "";

    private String reserv2 = "";

    public TblLnsprovisionreg() {

    }

    /** 借据 */
    @Column(name = "Receiptno")
    public String getReceiptno() {
        return receiptno;
    }

    /** 借据 */
    public void setReceiptno(String receiptno) {
        this.receiptno = receiptno == null ? "" : receiptno.trim();
    }

    /** 计提/摊销类型 */
    @Column(name = "Intertype")
    public String getIntertype() {
        return intertype;
    }

    /** 计提/摊销类型 */
    public void setIntertype(String intertype) {
        this.intertype = intertype == null ? "" : intertype.trim();
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

    /** 交易日期 */
    @Column(name = "Trandate")
    public Date getTrandate() {
        return trandate;
    }

    /** 交易日期 */
    public void setTrandate(Date trandate) {
        if(trandate == null){
            this.trandate = new Date(0);
        }else{
            this.trandate = trandate;
        }
    }

    /** 账务流水号 */
    @Column(name = "Serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }

    /** 账务流水号 */
    public void setSerseqno(Integer serseqno) {
        if(serseqno == null){
            this.serseqno = 0;
        }else{
            this.serseqno = serseqno;
        }
    }

    /** 序号 */
    @Column(name = "Txnseq")
    public Integer getTxnseq() {
        return txnseq;
    }

    /** 序号 */
    public void setTxnseq(Integer txnseq) {
        if(txnseq == null){
            this.txnseq = 0;
        }else{
            this.txnseq = txnseq;
        }
    }

    /** 机构 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 机构 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    /** 柜员 */
    @Column(name = "Teller")
    public String getTeller() {
        return teller;
    }

    /** 柜员 */
    public void setTeller(String teller) {
        this.teller = teller == null ? "" : teller.trim();
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

    /** 币种 */
    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    /** 币种 */
    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    /** 计提利息总金额 */
    @Column(name = "Totalinterest")
    public Double getTotalinterest() {
        return totalinterest;
    }

    /** 计提利息总金额 */
    public void setTotalinterest(Double totalinterest) {
        if(totalinterest == null){
            this.totalinterest = 0.00;
        }else{
            this.totalinterest = totalinterest;
        }
    }

    /** 计提税金总金额 */
    @Column(name = "Totaltax")
    public Double getTotaltax() {
        return totaltax;
    }

    /** 计提税金总金额 */
    public void setTotaltax(Double totaltax) {
        if(totaltax == null){
            this.totaltax = 0.00;
        }else{
            this.totaltax = totaltax;
        }
    }

    /** 税率 */
    @Column(name = "Taxrate")
    public Double getTaxrate() {
        return taxrate;
    }

    /** 税率 */
    public void setTaxrate(Double taxrate) {
        if(taxrate == null){
            this.taxrate = 0.00;
        }else{
            this.taxrate = taxrate;
        }
    }

    /** 本次入账利息金额 */
    @Column(name = "Interest")
    public Double getInterest() {
        return interest;
    }

    /** 本次入账利息金额 */
    public void setInterest(Double interest) {
        if(interest == null){
            this.interest = 0.00;
        }else{
            this.interest = interest;
        }
    }

    /** 本次入账税金金额 */
    @Column(name = "Tax")
    public Double getTax() {
        return tax;
    }

    /** 本次入账税金金额 */
    public void setTax(Double tax) {
        if(tax == null){
            this.tax = 0.00;
        }else{
            this.tax = tax;
        }
    }

    /** 开始日期 */
    @Column(name = "Begindate")
    public Date getBegindate() {
        return begindate;
    }

    /** 开始日期 */
    public void setBegindate(Date begindate) {
        if(begindate == null){
            this.begindate = new Date(0);
        }else{
            this.begindate = begindate;
        }
    }

    /** 结束日期 */
    @Column(name = "Enddate")
    public Date getEnddate() {
        return enddate;
    }

    /** 结束日期 */
    public void setEnddate(Date enddate) {
        if(enddate == null){
            this.enddate = new Date(0);
        }else{
            this.enddate = enddate;
        }
    }

    /** 正反标志 */
    @Column(name = "Interflag")
    public String getInterflag() {
        return interflag;
    }

    /** 正反标志 */
    public void setInterflag(String interflag) {
        this.interflag = interflag == null ? "" : interflag.trim();
    }

    /** 发送标志 */
    @Column(name = "Sendflag")
    public String getSendflag() {
        return sendflag;
    }

    /** 发送标志 */
    public void setSendflag(String sendflag) {
        this.sendflag = sendflag == null ? "" : sendflag.trim();
    }

    /** 发送次数 */
    @Column(name = "Sendnum")
    public Integer getSendnum() {
        return sendnum;
    }

    /** 发送次数 */
    public void setSendnum(Integer sendnum) {
        if(sendnum == null){
            this.sendnum = 0;
        }else{
            this.sendnum = sendnum;
        }
    }

    /** 是否作废标志 */
    @Column(name = "Cancelflag")
    public String getCancelflag() {
        return cancelflag;
    }

    /** 是否作废标志 */
    public void setCancelflag(String cancelflag) {
        this.cancelflag = cancelflag == null ? "" : cancelflag.trim();
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

    /** 备用1 */
    @Column(name = "Reserv1")
    public String getReserv1() {
        return reserv1;
    }

    /** 备用1 */
    public void setReserv1(String reserv1) {
        this.reserv1 = reserv1 == null ? "" : reserv1.trim();
    }

    /** 备用2 */
    @Column(name = "Reserv2")
    public String getReserv2() {
        return reserv2;
    }

    /** 备用2 */
    public void setReserv2(String reserv2) {
        this.reserv2 = reserv2 == null ? "" : reserv2.trim();
    }
}
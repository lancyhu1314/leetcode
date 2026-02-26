package com.suning.fab.loan.domain;

import java.sql.Date;
import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSPROVISIONDTL")
public class TblLnsprovisiondtl {
    private Date trandate = new Date(0);

    private Integer serseqno = 0;

    private Integer txnseq = 0;

    private String acctno = "";

    private String brc = "";

    private String childbrc = "";

    private String ccy = "";

    private Integer period = 0;

    private String billtype = "";

    private String intertype = "";

    private Double taxrate = 0.00;

    private Double interest = 0.00;

    private Double tax = 0.00;

    private Double totalinterest = 0.00;

    private Double totaltax = 0.00;

    private Date begindate = new Date(0);

    private Date enddate = new Date(0);

    private Double irr = 0.00;

    private String interflag = "";

    private String cancelflag = "";

    private Integer listno = 0;

    private String timestamp = "";

    private String reserv1 = "";

    private String reserv2 = "";

    public TblLnsprovisiondtl() {
         
    }

    @Column(name = "Trandate")
    public Date getTrandate() {
        return trandate;
    }

    public void setTrandate(Date trandate) {
        if(trandate == null){
            this.trandate = new Date(0);
        }else{
            this.trandate = trandate;
        }
    }

    @Column(name = "Serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }

    public void setSerseqno(Integer serseqno) {
        if(serseqno == null){
            this.serseqno = 0;
        }else{
            this.serseqno = serseqno;
        }
    }

    @Column(name = "Txnseq")
    public Integer getTxnseq() {
        return txnseq;
    }

    public void setTxnseq(Integer txnseq) {
        if(txnseq == null){
            this.txnseq = 0;
        }else{
            this.txnseq = txnseq;
        }
    }

    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    @Column(name = "Childbrc")
    public String getChildbrc() {
        return childbrc;
    }

    public void setChildbrc(String childbrc) {
        this.childbrc = childbrc == null ? "" : childbrc.trim();
    }

    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    @Column(name = "Period")
    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        if(period == null){
            this.period = 0;
        }else{
            this.period = period;
        }
    }

    @Column(name = "Billtype")
    public String getBilltype() {
        return billtype;
    }

    public void setBilltype(String billtype) {
        this.billtype = billtype == null ? "" : billtype.trim();
    }

    @Column(name = "Intertype")
    public String getIntertype() {
        return intertype;
    }

    public void setIntertype(String intertype) {
        this.intertype = intertype == null ? "" : intertype.trim();
    }

    @Column(name = "Taxrate")
    public Double getTaxrate() {
        return taxrate;
    }

    public void setTaxrate(Double taxrate) {
        if(taxrate == null){
            this.taxrate = 0.00;
        }else{
            this.taxrate = taxrate;
        }
    }

    @Column(name = "Interest")
    public Double getInterest() {
        return interest;
    }

    public void setInterest(Double interest) {
        if(interest == null){
            this.interest = 0.00;
        }else{
            this.interest = interest;
        }
    }

    @Column(name = "Tax")
    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        if(tax == null){
            this.tax = 0.00;
        }else{
            this.tax = tax;
        }
    }

    @Column(name = "Totalinterest")
    public Double getTotalinterest() {
        return totalinterest;
    }

    public void setTotalinterest(Double totalinterest) {
        if(totalinterest == null){
            this.totalinterest = 0.00;
        }else{
            this.totalinterest = totalinterest;
        }
    }

    @Column(name = "Totaltax")
    public Double getTotaltax() {
        return totaltax;
    }

    public void setTotaltax(Double totaltax) {
        if(totaltax == null){
            this.totaltax = 0.00;
        }else{
            this.totaltax = totaltax;
        }
    }

    @Column(name = "Begindate")
    public Date getBegindate() {
        return begindate;
    }

    public void setBegindate(Date begindate) {
        if(begindate == null){
            this.begindate = new Date(0);
        }else{
            this.begindate = begindate;
        }
    }

    @Column(name = "Enddate")
    public Date getEnddate() {
        return enddate;
    }

    public void setEnddate(Date enddate) {
        if(enddate == null){
            this.enddate = new Date(0);
        }else{
            this.enddate = enddate;
        }
    }

    @Column(name = "Irr")
    public Double getIrr() {
        return irr;
    }

    public void setIrr(Double irr) {
        if(irr == null){
            this.irr = 0.00;
        }else{
            this.irr = irr;
        }
    }

    @Column(name = "Interflag")
    public String getInterflag() {
        return interflag;
    }

    public void setInterflag(String interflag) {
        this.interflag = interflag == null ? "" : interflag.trim();
    }

    @Column(name = "Cancelflag")
    public String getCancelflag() {
        return cancelflag;
    }

    public void setCancelflag(String cancelflag) {
        this.cancelflag = cancelflag == null ? "" : cancelflag.trim();
    }

    @Column(name = "Listno")
    public Integer getListno() {
        return listno;
    }

    public void setListno(Integer listno) {
        if(listno == null){
            this.listno = 0;
        }else{
            this.listno = listno;
        }
    }

    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
    }

    @Column(name = "Reserv1")
    public String getReserv1() {
        return reserv1;
    }

    public void setReserv1(String reserv1) {
        this.reserv1 = reserv1 == null ? "" : reserv1.trim();
    }

    @Column(name = "Reserv2")
    public String getReserv2() {
        return reserv2;
    }

    public void setReserv2(String reserv2) {
        this.reserv2 = reserv2 == null ? "" : reserv2.trim();
    }
}
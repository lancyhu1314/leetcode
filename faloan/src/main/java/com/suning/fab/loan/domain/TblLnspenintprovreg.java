package com.suning.fab.loan.domain;

import java.math.BigDecimal;
import java.sql.Date;
import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSPENINTPROVREG")
public class TblLnspenintprovreg {
    private String receiptno = "";

    private String brc = "";

    private String billtype = "";

    private String ccy = "";

    private BigDecimal taxrate = BigDecimal.valueOf(0.00);

    private BigDecimal totalinterest = BigDecimal.valueOf(0.00);

    private BigDecimal totaltax = BigDecimal.valueOf(0.00);

    private Date begindate = new Date(0);

    private Date enddate = new Date(0);

    private String timestamp = "";

    private Integer totallist = 0;

    private String reserv1 = "";

    private String reserv2 = "";

    public TblLnspenintprovreg() {
         
    }

    @Column(name = "Receiptno")
    public String getReceiptno() {
        return receiptno;
    }

    public void setReceiptno(String receiptno) {
        this.receiptno = receiptno == null ? "" : receiptno.trim();
    }

    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    @Column(name = "Billtype")
    public String getBilltype() {
        return billtype;
    }

    public void setBilltype(String billtype) {
        this.billtype = billtype == null ? "" : billtype.trim();
    }

    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    @Column(name = "Taxrate")
    public BigDecimal getTaxrate() {
        return taxrate;
    }

    public void setTaxrate(BigDecimal taxrate) {
        if(taxrate == null){
            this.taxrate = BigDecimal.valueOf(0.00);
        }else{
            this.taxrate = taxrate;
        }
    }

    @Column(name = "Totalinterest")
    public BigDecimal getTotalinterest() {
        return totalinterest;
    }

    public void setTotalinterest(BigDecimal totalinterest) {
        if(totalinterest == null){
            this.totalinterest = BigDecimal.valueOf(0.00);
        }else{
            this.totalinterest = totalinterest;
        }
    }

    @Column(name = "Totaltax")
    public BigDecimal getTotaltax() {
        return totaltax;
    }

    public void setTotaltax(BigDecimal totaltax) {
        if(totaltax == null){
            this.totaltax = BigDecimal.valueOf(0.00);
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

    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
    }

    @Column(name = "Totallist")
    public Integer getTotallist() {
        return totallist;
    }

    public void setTotallist(Integer totallist) {
        if(totallist == null){
            this.totallist = 0;
        }else{
            this.totallist = totallist;
        }
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
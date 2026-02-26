package com.suning.fab.loan.domain;

import java.sql.Date;
import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSPROVISION")
public class TblLnsprovision {
    private String brc = "";

    private String acctno = "";

    private String billtype = "";

    private String childbrc = "";

    private String ccy = "";

    private Double taxrate = 0.00;

    private Double totalinterest = 0.00;

    private Double totaltax = 0.00;

    private Double lastinterest = 0.00;

    private Double lasttax = 0.00;

    private Date lastbegindate = new Date(0);

    private Date lastenddate = new Date(0);

    private String interflag = "";

    private String intertype = "";

    private Date begindate = new Date(0);

    private Date enddate = new Date(0);

    private Double irr = 0.00;

    private String provisionflag = "";

    private Integer totallist = 0;

    private String timestamp = "";

    private String reserv1 = "";

    private String reserv2 = "";


//查询明细记录是否存在 包含新的明细和老的明细
    private boolean exist=true;

    public boolean isSaveFlag() {
        return saveFlag;
    }

    public void setSaveFlag(boolean saveFlag) {
        this.saveFlag = saveFlag;
    }
    //保存状态
    private boolean saveFlag=true;

    public TblLnsprovision() {
         
    }

    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    @Column(name = "Billtype")
    public String getBilltype() {
        return billtype;
    }

    public void setBilltype(String billtype) {
        this.billtype = billtype == null ? "" : billtype.trim();
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

    @Column(name = "Lastinterest")
    public Double getLastinterest() {
        return lastinterest;
    }

    public void setLastinterest(Double lastinterest) {
        if(lastinterest == null){
            this.lastinterest = 0.00;
        }else{
            this.lastinterest = lastinterest;
        }
    }

    @Column(name = "Lasttax")
    public Double getLasttax() {
        return lasttax;
    }

    public void setLasttax(Double lasttax) {
        if(lasttax == null){
            this.lasttax = 0.00;
        }else{
            this.lasttax = lasttax;
        }
    }

    @Column(name = "Lastbegindate")
    public Date getLastbegindate() {
        return lastbegindate;
    }

    public void setLastbegindate(Date lastbegindate) {
        if(lastbegindate == null){
            this.lastbegindate = new Date(0);
        }else{
            this.lastbegindate = lastbegindate;
        }
    }

    @Column(name = "Lastenddate")
    public Date getLastenddate() {
        return lastenddate;
    }

    public void setLastenddate(Date lastenddate) {
        if(lastenddate == null){
            this.lastenddate = new Date(0);
        }else{
            this.lastenddate = lastenddate;
        }
    }

    @Column(name = "Interflag")
    public String getInterflag() {
        return interflag;
    }

    public void setInterflag(String interflag) {
        this.interflag = interflag == null ? "" : interflag.trim();
    }

    @Column(name = "Intertype")
    public String getIntertype() {
        return intertype;
    }

    public void setIntertype(String intertype) {
        this.intertype = intertype == null ? "" : intertype.trim();
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

    @Column(name = "Provisionflag")
    public String getProvisionflag() {
        return provisionflag;
    }

    public void setProvisionflag(String provisionflag) {
        this.provisionflag = provisionflag == null ? "" : provisionflag.trim();
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

    public boolean isExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
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
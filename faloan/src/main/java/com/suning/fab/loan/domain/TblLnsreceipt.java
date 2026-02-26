package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSRECEIPT")
public class TblLnsreceipt {
    private String acctno = "";

    private String mid = "";

    private String customid = "";

    private String custtype = "";

    private String subsys = "";

    private String opendate = "";

    private String closedate = "";

    private String status = "";

    private Double sumdeint = 0.00;

    private Double sumdefint = 0.00;

    private Double sumfee = 0.00;

    private Double sumgrmnt = 0.00;

    private Double remnt1 = 0.00;

    private Double remnt2 = 0.00;

    private String reverse1 = "";

    private String reverse2 = "";

    private String brc = "";

    private Double sumrint = 0.00;

    private Double sumrfint = 0.00;

    private Double sumdelint = 0.00;

    private Double sumdelfint = 0.00;

    public TblLnsreceipt() {
         
    }

    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    @Column(name = "Mid")
    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid == null ? "" : mid.trim();
    }

    @Column(name = "Customid")
    public String getCustomid() {
        return customid;
    }

    public void setCustomid(String customid) {
        this.customid = customid == null ? "" : customid.trim();
    }

    @Column(name = "Custtype")
    public String getCusttype() {
        return custtype;
    }

    public void setCusttype(String custtype) {
        this.custtype = custtype == null ? "" : custtype.trim();
    }

    @Column(name = "Subsys")
    public String getSubsys() {
        return subsys;
    }

    public void setSubsys(String subsys) {
        this.subsys = subsys == null ? "" : subsys.trim();
    }

    @Column(name = "Opendate")
    public String getOpendate() {
        return opendate;
    }

    public void setOpendate(String opendate) {
        this.opendate = opendate == null ? "" : opendate.trim();
    }

    @Column(name = "Closedate")
    public String getClosedate() {
        return closedate;
    }

    public void setClosedate(String closedate) {
        this.closedate = closedate == null ? "" : closedate.trim();
    }

    @Column(name = "Status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "" : status.trim();
    }

    @Column(name = "Sumdeint")
    public Double getSumdeint() {
        return sumdeint;
    }

    public void setSumdeint(Double sumdeint) {
        if(sumdeint == null){
            this.sumdeint = 0.00;
        }else{
            this.sumdeint = sumdeint;
        }
    }

    @Column(name = "Sumdefint")
    public Double getSumdefint() {
        return sumdefint;
    }

    public void setSumdefint(Double sumdefint) {
        if(sumdefint == null){
            this.sumdefint = 0.00;
        }else{
            this.sumdefint = sumdefint;
        }
    }

    @Column(name = "Sumfee")
    public Double getSumfee() {
        return sumfee;
    }

    public void setSumfee(Double sumfee) {
        if(sumfee == null){
            this.sumfee = 0.00;
        }else{
            this.sumfee = sumfee;
        }
    }

    @Column(name = "Sumgrmnt")
    public Double getSumgrmnt() {
        return sumgrmnt;
    }

    public void setSumgrmnt(Double sumgrmnt) {
        if(sumgrmnt == null){
            this.sumgrmnt = 0.00;
        }else{
            this.sumgrmnt = sumgrmnt;
        }
    }

    @Column(name = "Remnt1")
    public Double getRemnt1() {
        return remnt1;
    }

    public void setRemnt1(Double remnt1) {
        if(remnt1 == null){
            this.remnt1 = 0.00;
        }else{
            this.remnt1 = remnt1;
        }
    }

    @Column(name = "Remnt2")
    public Double getRemnt2() {
        return remnt2;
    }

    public void setRemnt2(Double remnt2) {
        if(remnt2 == null){
            this.remnt2 = 0.00;
        }else{
            this.remnt2 = remnt2;
        }
    }

    @Column(name = "Reverse1")
    public String getReverse1() {
        return reverse1;
    }

    public void setReverse1(String reverse1) {
        this.reverse1 = reverse1 == null ? "" : reverse1.trim();
    }

    @Column(name = "Reverse2")
    public String getReverse2() {
        return reverse2;
    }

    public void setReverse2(String reverse2) {
        this.reverse2 = reverse2 == null ? "" : reverse2.trim();
    }

    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    @Column(name = "Sumrint")
    public Double getSumrint() {
        return sumrint;
    }

    public void setSumrint(Double sumrint) {
        if(sumrint == null){
            this.sumrint = 0.00;
        }else{
            this.sumrint = sumrint;
        }
    }

    @Column(name = "Sumrfint")
    public Double getSumrfint() {
        return sumrfint;
    }

    public void setSumrfint(Double sumrfint) {
        if(sumrfint == null){
            this.sumrfint = 0.00;
        }else{
            this.sumrfint = sumrfint;
        }
    }

    @Column(name = "Sumdelint")
    public Double getSumdelint() {
        return sumdelint;
    }

    public void setSumdelint(Double sumdelint) {
        if(sumdelint == null){
            this.sumdelint = 0.00;
        }else{
            this.sumdelint = sumdelint;
        }
    }

    @Column(name = "Sumdelfint")
    public Double getSumdelfint() {
        return sumdelfint;
    }

    public void setSumdelfint(Double sumdelfint) {
        if(sumdelfint == null){
            this.sumdelfint = 0.00;
        }else{
            this.sumdelfint = sumdelfint;
        }
    }
}
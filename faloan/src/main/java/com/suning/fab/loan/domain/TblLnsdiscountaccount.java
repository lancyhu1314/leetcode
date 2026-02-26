package com.suning.fab.loan.domain;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSDISCOUNTACCOUNT")
public class TblLnsdiscountaccount {
    private String brc = "";

    private String accttype = "";

    private String acctno = "";

    private String customid = "";

    private String custtype = "";

    private String opendate = "";

    private String closedate = "";

    private BigDecimal balance = BigDecimal.valueOf(0.00);

    private String status = "";

    private String timestamp = "";

    private String reverse1 = "";

    private String reverse2 = "";

    private String name = "";

    public TblLnsdiscountaccount() {
         
    }

    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    @Column(name = "Accttype")
    public String getAccttype() {
        return accttype;
    }

    public void setAccttype(String accttype) {
        this.accttype = accttype == null ? "" : accttype.trim();
    }

    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
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

    @Column(name = "Balance")
    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        if(balance == null){
            this.balance = BigDecimal.valueOf(0.00);
        }else{
            this.balance = balance;
        }
    }

    @Column(name = "Status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "" : status.trim();
    }

    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
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

    @Column(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }
}
package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSPREFUNDACCOUNT")
public class TblLnsprefundaccount {
    private String brc = "";

    private String accsrccode = "";

    private String acctno = "";

    private String customid = "";

    private String custtype = "";

    private String opendate = "";

    private String closedate = "";

    private Double balance = 0.00;

    private String status = "";

    private String timestamp = "";

    private String reverse1 = "";

    private String reverse2 = "";

    private String name = "";

    public TblLnsprefundaccount() {
         
    }

    /** 开户网点 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 开户网点 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    /** 预收户类型 */
    @Column(name = "Accsrccode")
    public String getAccsrccode() {
        return accsrccode;
    }

    /** 预收户类型 */
    public void setAccsrccode(String accsrccode) {
        this.accsrccode = accsrccode == null ? "" : accsrccode.trim();
    }

    /** 预收账号 */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /** 预收账号 */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    /** 客户号 */
    @Column(name = "Customid")
    public String getCustomid() {
        return customid;
    }

    /** 客户号 */
    public void setCustomid(String customid) {
        this.customid = customid == null ? "" : customid.trim();
    }

    /** 客户类型 */
    @Column(name = "Custtype")
    public String getCusttype() {
        return custtype;
    }

    /** 客户类型 */
    public void setCusttype(String custtype) {
        this.custtype = custtype == null ? "" : custtype.trim();
    }

    /** 开户日期 */
    @Column(name = "Opendate")
    public String getOpendate() {
        return opendate;
    }

    /** 开户日期 */
    public void setOpendate(String opendate) {
        this.opendate = opendate == null ? "" : opendate.trim();
    }

    /** 销户日期 */
    @Column(name = "Closedate")
    public String getClosedate() {
        return closedate;
    }

    /** 销户日期 */
    public void setClosedate(String closedate) {
        this.closedate = closedate == null ? "" : closedate.trim();
    }

    /** 余额 */
    @Column(name = "Balance")
    public Double getBalance() {
        return balance;
    }

    /** 余额 */
    public void setBalance(Double balance) {
        if(balance == null){
            this.balance = 0.00;
        }else{
            this.balance = balance;
        }
    }

    /** 账户状态 */
    @Column(name = "Status")
    public String getStatus() {
        return status;
    }

    /** 账户状态 */
    public void setStatus(String status) {
        this.status = status == null ? "" : status.trim();
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

    /** 备注字段1 */
    @Column(name = "Reverse1")
    public String getReverse1() {
        return reverse1;
    }

    /** 备注字段1 */
    public void setReverse1(String reverse1) {
        this.reverse1 = reverse1 == null ? "" : reverse1.trim();
    }

    /** 备注字段2 */
    @Column(name = "Reverse2")
    public String getReverse2() {
        return reverse2;
    }

    /** 备注字段2 */
    public void setReverse2(String reverse2) {
        this.reverse2 = reverse2 == null ? "" : reverse2.trim();
    }

    /** 户名 */
    @Column(name = "Name")
    public String getName() {
        return name;
    }

    /** 户名 */
    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }
}
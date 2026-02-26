package com.suning.fab.loan.domain;

import java.sql.Date;
import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSADJUSTINFOREG")
public class TblLnsadjustinforeg {
    private Date trandate = new Date(0);

    private String serialno = "";

    private String channelid = "";

    private Integer txnseq = 0;

    private Date accdate = new Date(0);

    private Integer serseqno = 0;

    private String transource = "";

    private String brc = "";

    private String ccy = "";

    private Double tranamt = 0.00;

    private Double amt1 = 0.00;

    private Double amt2 = 0.00;

    private Double amt3 = 0.00;

    private Double amt4 = 0.00;

    private Double amt5 = 0.00;

    private Double amt6 = 0.00;

    private String trancode = "";

    private String teller = "";

    private String briefcode = "";

    private String tranbrief = "";

    private String receiptno = "";

    private String accstat = "";

    private String acctno = "";

    private String merchantno = "";

    private String toaccstat = "";

    private String toaccountno = "";

    private String tomerchantno = "";

    private String timestamp = "";

    private String flag = "";

    private String bankchannel = "";

    private String reserv0 = "";

    private String reserv1 = "";

    private String reserv2 = "";

    private String reserv3 = "";

    private String reserv4 = "";

    private String reserv5 = "";

    private String reserv6 = "";

    private String reserv7 = "";

    private String reserv8 = "";

    private String reserv9 = "";

    private String tunneldata = "";

    public TblLnsadjustinforeg() {
         
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

    @Column(name = "Serialno")
    public String getSerialno() {
        return serialno;
    }

    public void setSerialno(String serialno) {
        this.serialno = serialno == null ? "" : serialno.trim();
    }

    @Column(name = "Channelid")
    public String getChannelid() {
        return channelid;
    }

    public void setChannelid(String channelid) {
        this.channelid = channelid == null ? "" : channelid.trim();
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

    @Column(name = "Accdate")
    public Date getAccdate() {
        return accdate;
    }

    public void setAccdate(Date accdate) {
        if(accdate == null){
            this.accdate = new Date(0);
        }else{
            this.accdate = accdate;
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

    @Column(name = "Transource")
    public String getTransource() {
        return transource;
    }

    public void setTransource(String transource) {
        this.transource = transource == null ? "" : transource.trim();
    }

    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    @Column(name = "Tranamt")
    public Double getTranamt() {
        return tranamt;
    }

    public void setTranamt(Double tranamt) {
        if(tranamt == null){
            this.tranamt = 0.00;
        }else{
            this.tranamt = tranamt;
        }
    }

    @Column(name = "Amt1")
    public Double getAmt1() {
        return amt1;
    }

    public void setAmt1(Double amt1) {
        if(amt1 == null){
            this.amt1 = 0.00;
        }else{
            this.amt1 = amt1;
        }
    }

    @Column(name = "Amt2")
    public Double getAmt2() {
        return amt2;
    }

    public void setAmt2(Double amt2) {
        if(amt2 == null){
            this.amt2 = 0.00;
        }else{
            this.amt2 = amt2;
        }
    }

    @Column(name = "Amt3")
    public Double getAmt3() {
        return amt3;
    }

    public void setAmt3(Double amt3) {
        if(amt3 == null){
            this.amt3 = 0.00;
        }else{
            this.amt3 = amt3;
        }
    }

    @Column(name = "Amt4")
    public Double getAmt4() {
        return amt4;
    }

    public void setAmt4(Double amt4) {
        if(amt4 == null){
            this.amt4 = 0.00;
        }else{
            this.amt4 = amt4;
        }
    }

    @Column(name = "Amt5")
    public Double getAmt5() {
        return amt5;
    }

    public void setAmt5(Double amt5) {
        if(amt5 == null){
            this.amt5 = 0.00;
        }else{
            this.amt5 = amt5;
        }
    }

    @Column(name = "Amt6")
    public Double getAmt6() {
        return amt6;
    }

    public void setAmt6(Double amt6) {
        if(amt6 == null){
            this.amt6 = 0.00;
        }else{
            this.amt6 = amt6;
        }
    }

    @Column(name = "Trancode")
    public String getTrancode() {
        return trancode;
    }

    public void setTrancode(String trancode) {
        this.trancode = trancode == null ? "" : trancode.trim();
    }

    @Column(name = "Teller")
    public String getTeller() {
        return teller;
    }

    public void setTeller(String teller) {
        this.teller = teller == null ? "" : teller.trim();
    }

    @Column(name = "Briefcode")
    public String getBriefcode() {
        return briefcode;
    }

    public void setBriefcode(String briefcode) {
        this.briefcode = briefcode == null ? "" : briefcode.trim();
    }

    @Column(name = "Tranbrief")
    public String getTranbrief() {
        return tranbrief;
    }

    public void setTranbrief(String tranbrief) {
        this.tranbrief = tranbrief == null ? "" : tranbrief.trim();
    }

    @Column(name = "Receiptno")
    public String getReceiptno() {
        return receiptno;
    }

    public void setReceiptno(String receiptno) {
        this.receiptno = receiptno == null ? "" : receiptno.trim();
    }

    @Column(name = "Accstat")
    public String getAccstat() {
        return accstat;
    }

    public void setAccstat(String accstat) {
        this.accstat = accstat == null ? "" : accstat.trim();
    }

    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    @Column(name = "Merchantno")
    public String getMerchantno() {
        return merchantno;
    }

    public void setMerchantno(String merchantno) {
        this.merchantno = merchantno == null ? "" : merchantno.trim();
    }

    @Column(name = "Toaccstat")
    public String getToaccstat() {
        return toaccstat;
    }

    public void setToaccstat(String toaccstat) {
        this.toaccstat = toaccstat == null ? "" : toaccstat.trim();
    }

    @Column(name = "Toaccountno")
    public String getToaccountno() {
        return toaccountno;
    }

    public void setToaccountno(String toaccountno) {
        this.toaccountno = toaccountno == null ? "" : toaccountno.trim();
    }

    @Column(name = "Tomerchantno")
    public String getTomerchantno() {
        return tomerchantno;
    }

    public void setTomerchantno(String tomerchantno) {
        this.tomerchantno = tomerchantno == null ? "" : tomerchantno.trim();
    }

    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
    }

    @Column(name = "Flag")
    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag == null ? "" : flag.trim();
    }

    @Column(name = "Bankchannel")
    public String getBankchannel() {
        return bankchannel;
    }

    public void setBankchannel(String bankchannel) {
        this.bankchannel = bankchannel == null ? "" : bankchannel.trim();
    }

    @Column(name = "Reserv0")
    public String getReserv0() {
        return reserv0;
    }

    public void setReserv0(String reserv0) {
        this.reserv0 = reserv0 == null ? "" : reserv0.trim();
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

    @Column(name = "Reserv3")
    public String getReserv3() {
        return reserv3;
    }

    public void setReserv3(String reserv3) {
        this.reserv3 = reserv3 == null ? "" : reserv3.trim();
    }

    @Column(name = "Reserv4")
    public String getReserv4() {
        return reserv4;
    }

    public void setReserv4(String reserv4) {
        this.reserv4 = reserv4 == null ? "" : reserv4.trim();
    }

    @Column(name = "Reserv5")
    public String getReserv5() {
        return reserv5;
    }

    public void setReserv5(String reserv5) {
        this.reserv5 = reserv5 == null ? "" : reserv5.trim();
    }

    @Column(name = "Reserv6")
    public String getReserv6() {
        return reserv6;
    }

    public void setReserv6(String reserv6) {
        this.reserv6 = reserv6 == null ? "" : reserv6.trim();
    }

    @Column(name = "Reserv7")
    public String getReserv7() {
        return reserv7;
    }

    public void setReserv7(String reserv7) {
        this.reserv7 = reserv7 == null ? "" : reserv7.trim();
    }

    @Column(name = "Reserv8")
    public String getReserv8() {
        return reserv8;
    }

    public void setReserv8(String reserv8) {
        this.reserv8 = reserv8 == null ? "" : reserv8.trim();
    }

    @Column(name = "Reserv9")
    public String getReserv9() {
        return reserv9;
    }

    public void setReserv9(String reserv9) {
        this.reserv9 = reserv9 == null ? "" : reserv9.trim();
    }

    @Column(name = "Tunneldata")
    public String getTunneldata() {
        return tunneldata;
    }

    public void setTunneldata(String tunneldata) {
        this.tunneldata = tunneldata == null ? "" : tunneldata.trim();
    }
}
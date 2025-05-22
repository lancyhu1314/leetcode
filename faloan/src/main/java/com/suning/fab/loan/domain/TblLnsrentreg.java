package com.suning.fab.loan.domain;

import javax.persistence.Entity;
import javax.persistence.Column;
@Entity(name = "LNSRENTREG")
public class TblLnsrentreg {
    private String accdate = "";

    private Integer serseqno = 0;

    private String trandate = "";

    private String serialno = "";

    private String acctno = "";

    private String reqmap = "";

    private String rsqmap = "";

    private String timestamp = "";

    private String brc = "";

    public TblLnsrentreg() {
         
    }

    @Column(name = "Accdate")
    public String getAccdate() {
        return accdate;
    }

    public void setAccdate(String accdate) {
        this.accdate = accdate == null ? "" : accdate.trim();
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

    @Column(name = "Trandate")
    public String getTrandate() {
        return trandate;
    }

    public void setTrandate(String trandate) {
        this.trandate = trandate == null ? "" : trandate.trim();
    }

    @Column(name = "Serialno")
    public String getSerialno() {
        return serialno;
    }

    public void setSerialno(String serialno) {
        this.serialno = serialno == null ? "" : serialno.trim();
    }

    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    @Column(name = "Reqmap")
    public String getReqmap() {
        return reqmap;
    }

    public void setReqmap(String reqmap) {
        this.reqmap = reqmap == null ? "" : reqmap.trim();
    }

    @Column(name = "Rsqmap")
    public String getRsqmap() {
        return rsqmap;
    }

    public void setRsqmap(String rsqmap) {
        this.rsqmap = rsqmap == null ? "" : rsqmap.trim();
    }

    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
    }

    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }
}
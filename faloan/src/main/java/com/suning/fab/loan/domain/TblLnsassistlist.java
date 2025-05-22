package com.suning.fab.loan.domain;

import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSASSISTLIST")
public class TblLnsassistlist {
    private String brc = "";

    private String acctno = "";

    private Integer txnseq = 0;

    private String trandate = "";

    private Integer serseqno = 0;

    private String customid = "";

    private String ccy = "";

    private String feetype = "";

    private String cdflag = "";

    private Double tranamt = 0.00;

    private Double bal = 0.00;

    private String acctstat = "";

    private Integer ordnu = 0;

    private String trancode = "";

    private String memocode = "";

    private String memoname = "";

    private String reserv1 = "";

    private String reserv2 = "";

    private String reserv3 = "";

    private String reserv4 = "";

    private String reserv5 = "";

    private String reserv6 = "";

    private String tunneldata = "";

    private String createtime = "";

    private String modifytime = "";

    public TblLnsassistlist() {
         
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

    @Column(name = "Trandate")
    public String getTrandate() {
        return trandate;
    }

    public void setTrandate(String trandate) {
        this.trandate = trandate == null ? "" : trandate.trim();
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

    @Column(name = "Customid")
    public String getCustomid() {
        return customid;
    }

    public void setCustomid(String customid) {
        this.customid = customid == null ? "" : customid.trim();
    }

    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    @Column(name = "Feetype")
    public String getFeetype() {
        return feetype;
    }

    public void setFeetype(String feetype) {
        this.feetype = feetype == null ? "" : feetype.trim();
    }

    @Column(name = "Cdflag")
    public String getCdflag() {
        return cdflag;
    }

    public void setCdflag(String cdflag) {
        this.cdflag = cdflag == null ? "" : cdflag.trim();
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

    @Column(name = "Bal")
    public Double getBal() {
        return bal;
    }

    public void setBal(Double bal) {
        if(bal == null){
            this.bal = 0.00;
        }else{
            this.bal = bal;
        }
    }

    @Column(name = "Acctstat")
    public String getAcctstat() {
        return acctstat;
    }

    public void setAcctstat(String acctstat) {
        this.acctstat = acctstat == null ? "" : acctstat.trim();
    }

    @Column(name = "Ordnu")
    public Integer getOrdnu() {
        return ordnu;
    }

    public void setOrdnu(Integer ordnu) {
        if(ordnu == null){
            this.ordnu = 0;
        }else{
            this.ordnu = ordnu;
        }
    }

    @Column(name = "Trancode")
    public String getTrancode() {
        return trancode;
    }

    public void setTrancode(String trancode) {
        this.trancode = trancode == null ? "" : trancode.trim();
    }

    @Column(name = "Memocode")
    public String getMemocode() {
        return memocode;
    }

    public void setMemocode(String memocode) {
        this.memocode = memocode == null ? "" : memocode.trim();
    }

    @Column(name = "Memoname")
    public String getMemoname() {
        return memoname;
    }

    public void setMemoname(String memoname) {
        this.memoname = memoname == null ? "" : memoname.trim();
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

    @Column(name = "Tunneldata")
    public String getTunneldata() {
        return tunneldata;
    }

    public void setTunneldata(String tunneldata) {
        this.tunneldata = tunneldata == null ? "" : tunneldata.trim();
    }

    @Column(name = "Createtime")
    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime == null ? "" : createtime.trim();
    }

    @Column(name = "Modifytime")
    public String getModifytime() {
        return modifytime;
    }

    public void setModifytime(String modifytime) {
        this.modifytime = modifytime == null ? "" : modifytime.trim();
    }
}
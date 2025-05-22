package com.suning.fab.loan.domain;


public class TblLnstaxdetail {
    private String trandate;

    private Integer serseqno;

    private Integer txseqno;

    private String brc;

    private String acctno;

    private Double tranamt;

    private Double tax;

    private Double taxrate;

    private String  taxtype;

    private String interflag;

    private String billtype;

    private String billstatus;

    private String billtrandate;

    private Integer billserseqno;

    private Integer billtxseq;

    private String reserv1;

    private String reserv2;

    private String reserve;

    private String timestamp;

    public TblLnstaxdetail(String trandate, Integer serseqno, Integer txseqno, String brc, String acctno, Double tranamt, Double tax, Double taxrate,
                          String taxtype,String interflag,String billtype,String billstatus, String billtrandate, Integer billserseqno, Integer billtxseq, String reserv1, String reserv2, String reserve) {
        this.trandate = trandate;
        this.serseqno = serseqno;
        this.txseqno = txseqno;
        this.brc = brc;
        this.acctno = acctno;
        this.tranamt = tranamt;
        this.tax = tax;
        this.taxrate = taxrate;
        this.taxtype = taxtype;
        this.interflag = interflag;
        this.billtype = billtype;
        this.billstatus = billstatus;
        this.billtrandate = billtrandate;
        this.billserseqno = billserseqno;
        this.billtxseq = billtxseq;
        this.reserv1 = reserv1;
        this.reserv2 = reserv2;
        this.reserve = reserve;
    }

    public TblLnstaxdetail() {
        super();
    }

    public String getTrandate() {
        return trandate;
    }

    public void setTrandate(String trandate) {
        this.trandate = trandate == null ? null : trandate.trim();
    }

    public Integer getSerseqno() {
        return serseqno;
    }

    public void setSerseqno(Integer serseqno) {
        this.serseqno = serseqno;
    }

    public Integer getTxseqno() {
        return txseqno;
    }

    public void setTxseqno(Integer txseqno) {
        this.txseqno = txseqno;
    }

    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? null : brc.trim();
    }

    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? null : acctno.trim();
    }

    public Double getTranamt() {
        return tranamt;
    }

    public void setTranamt(Double tranamt) {
        this.tranamt = tranamt;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getTaxrate() {
        return taxrate;
    }

    public void setTaxrate(Double taxrate) {
        this.taxrate = taxrate;
    }

    public String getBilltype() {
        return billtype;
    }

    public void setBilltype(String billtype) {
        this.billtype = billtype == null ? null : billtype.trim();
    }

    public String getBilltrandate() {
        return billtrandate;
    }

    public void setBilltrandate(String billtrandate) {
        this.billtrandate = billtrandate == null ? null : billtrandate.trim();
    }

    public Integer getBillserseqno() {
        return billserseqno;
    }

    public void setBillserseqno(Integer billserseqno) {
        this.billserseqno = billserseqno;
    }

    public Integer getBilltxseq() {
        return billtxseq;
    }

    public void setBilltxseq(Integer billtxseq) {
        this.billtxseq = billtxseq;
    }

    public String getReserv1() {
        return reserv1;
    }

    public void setReserv1(String reserv1) {
        this.reserv1 = reserv1 == null ? null : reserv1.trim();
    }

    public String getReserv2() {
        return reserv2;
    }

    public void setReserv2(String reserv2) {
        this.reserv2 = reserv2 == null ? null : reserv2.trim();
    }

    public String getReserve() {
        return reserve;
    }

    public void setReserve(String reserve) {
        this.reserve = reserve == null ? null : reserve.trim();
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? null : timestamp.trim();
    }

    public String getTaxtype() {
        return taxtype;
    }

    public void setTaxtype(String taxtype) {
        this.taxtype = taxtype;
    }

    public String getInterflag() {
        return interflag;
    }

    public void setInterflag(String interflag) {
        this.interflag = interflag;
    }

    public String getBillstatus() {
        return billstatus;
    }

    public void setBillstatus(String billstatus) {
        this.billstatus = billstatus;
    }
}
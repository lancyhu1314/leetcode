package com.suning.fab.loan.domain;


public class TblLnsprefundsch {
    private String trandate;

    private Integer serseqno;

    private Integer txseqno;

    private String brc;

    private String acctno;

    private String customid;

    private String accsrccode;

    private String custtype;

    private String name;

    private Double amount;

    private String trancode;

    private String cdflag;

    private String reserv1;

    private String reserv2;

    private String reserve;

    private String timestamp;

    public TblLnsprefundsch(String trandate, Integer serseqno, Integer txseqno, String brc, String acctno, String customid, String accsrccode, String custtype, String name, Double amount, String trancode, String cdflag, String reserv1, String reserv2, String reserve) {
        this.trandate = trandate;
        this.serseqno = serseqno;
        this.txseqno = txseqno;
        this.brc = brc;
        this.acctno = acctno;
        this.customid = customid;
        this.accsrccode = accsrccode;
        this.custtype = custtype;
        this.name = name;
        this.amount = amount;
        this.trancode = trancode;
        this.cdflag = cdflag;
        this.reserv1 = reserv1;
        this.reserv2 = reserv2;
        this.reserve = reserve;
    }

    public TblLnsprefundsch() {
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

    public String getCustomid() {
        return customid;
    }

    public void setCustomid(String customid) {
        this.customid = customid == null ? null : customid.trim();
    }

    public String getAccsrccode() {
        return accsrccode;
    }

    public void setAccsrccode(String accsrccode) {
        this.accsrccode = accsrccode == null ? null : accsrccode.trim();
    }

    public String getCusttype() {
        return custtype;
    }

    public void setCusttype(String custtype) {
        this.custtype = custtype == null ? null : custtype.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getTrancode() {
        return trancode;
    }

    public void setTrancode(String trancode) {
        this.trancode = trancode == null ? null : trancode.trim();
    }

    public String getCdflag() {
        return cdflag;
    }

    public void setCdflag(String cdflag) {
        this.cdflag = cdflag == null ? null : cdflag.trim();
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
}
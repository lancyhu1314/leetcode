package com.suning.fab.loan.domain;

public class TblLnsinterfaceex {
    private String trandate;

    private Integer serseqno;

    private String brc;

    private String acctno;

    private String key;

    private String brief;

    private String value;

    private String reserv1;

    private String reserv2;

    private String reserve;

    private String timestamp;

    public TblLnsinterfaceex(String trandate, Integer serseqno, String brc, String acctno, String key, String brief, String value, String reserv1, String reserv2, String reserve) {
        this.trandate = trandate;
        this.serseqno = serseqno;
        this.brc = brc;
        this.acctno = acctno;
        this.key = key;
        this.brief = brief;
        this.value = value;
        this.reserv1 = reserv1;
        this.reserv2 = reserv2;
        this.reserve = reserve;
    }

    public TblLnsinterfaceex() {
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key == null ? null : key.trim();
    }

    public String getBrief() {
        return brief;
    }

    public void setBrief(String brief) {
        this.brief = brief == null ? null : brief.trim();
    }

    public String getValue() {
        return value == null ? "" : value;
    }

    public void setValue(String value) {
        this.value = value == null ? null : value.trim();
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
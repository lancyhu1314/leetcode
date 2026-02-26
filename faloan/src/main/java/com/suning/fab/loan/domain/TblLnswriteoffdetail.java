package com.suning.fab.loan.domain;


public class TblLnswriteoffdetail {
    private String trandate;

    private Integer serseqno;

    private Integer txseqno;

    private String brc;

    private String acctno;

    private String  trancode;

    private String eventcode;

    private String briefcode;

    private String tranbrief;

    private Double tranamt;

    private String debtcompany;

    private String channeltype;

    private String fundchannel;

    private String outserialno;

    private String investeeId;

    private Double amt1;

    private Double amt2;

    private Double amt3;

    private String reserv1;

    private String reserv2;

    private String reserve;

    private String timestamp;

    public TblLnswriteoffdetail(String trandate, Integer serseqno, Integer txseqno, String brc, String acctno,String trancode, String eventcode, String briefcode, String tranbrief, Double tranamt, String debtcompany, String channeltype, String fundchannel, String outserialno,String investeeId,
             Double amt1, Double amt2, Double amt3, String reserv1, String reserv2, String reserve) {
        this.trandate = trandate;
        this.serseqno = serseqno;
        this.txseqno = txseqno;
        this.brc = brc;
        this.acctno = acctno;
        this.trancode = trancode;
        this.eventcode = eventcode;
        this.briefcode = briefcode;
        this.tranbrief = tranbrief;
        this.tranamt =tranamt;
        this.debtcompany = debtcompany;
        this.channeltype = channeltype;
        this.fundchannel = fundchannel;
        this.outserialno = outserialno;
        this.investeeId = investeeId;
        this.amt1 = amt1;
        this.amt2 = amt2;
        this.amt3 = amt3;
        this.reserv1 = reserv1;
        this.reserv2 = reserv2;
        this.reserve = reserve;
    }

    public TblLnswriteoffdetail() {
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

    public String getEventcode() {
        return eventcode;
    }

    public void setEventcode(String eventcode) {
        this.eventcode = eventcode == null ? null : eventcode.trim();
    }

    public String getBriefcode() {
        return briefcode;
    }

    public void setBriefcode(String briefcode) {
        this.briefcode = briefcode == null ? null : briefcode.trim();
    }

    public String getTranbrief() {
        return tranbrief;
    }

    public void setTranbrief(String tranbrief) {
        this.tranbrief = tranbrief == null ? null : tranbrief.trim();
    }

    public Double getTranamt() {
        return tranamt;
    }

    public void setTranamt(Double tranamt) {
        this.tranamt = tranamt;
    }

    public String getDebtcompany() {
        return debtcompany;
    }

    public void setDebtcompany(String debtcompany) {
        this.debtcompany = debtcompany == null ? null : debtcompany.trim();
    }

    public String getChanneltype() {
        return channeltype;
    }

    public void setChanneltype(String channeltype) {
        this.channeltype = channeltype == null ? null : channeltype.trim();
    }

    public String getFundchannel() {
        return fundchannel;
    }

    public void setFundchannel(String fundchannel) {
        this.fundchannel = fundchannel == null ? null : fundchannel.trim();
    }

    public String getOutserialno() {
        return outserialno;
    }

    public void setOutserialno(String outserialno) {
        this.outserialno = outserialno == null ? null : outserialno.trim();
    }

    public Double getAmt1() {
        return amt1;
    }

    public void setAmt1(Double amt1) {
        this.amt1 = amt1;
    }

    public Double getAmt2() {
        return amt2;
    }

    public void setAmt2(Double amt2) {
        this.amt2 = amt2;
    }

    public Double getAmt3() {
        return amt3;
    }

    public void setAmt3(Double amt3) {
        this.amt3 = amt3;
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

    public String getTrancode() {
        return trancode;
    }

    public void setTrancode(String trancode) {
        this.trancode = trancode;
    }

    public String getInvesteeId() {
        return investeeId;
    }

    public void setInvesteeId(String investeeId) {
        this.investeeId = investeeId;
    }
}
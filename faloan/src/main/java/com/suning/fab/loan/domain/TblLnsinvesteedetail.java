package com.suning.fab.loan.domain;


public class TblLnsinvesteedetail {
    private String trandate;

    private Integer serseqno;

    private Integer txseqno;

    private String brc;

    private String acctno;

    private String investeeid;

    private String newinvestee;

    private String investeeno;

    private String newinvesteeno;

    private Integer period;

    private Double investeeprin;

    private Double investeeint;

    private Double investeedint;

    private Double investeefee;

    private String trancode;

    private String trantype;

    private Double totalamt;

    private String nintflag;

    private String prinflag;

    private String investeeflag;

    private Double diffnint;

    private Double diffprin;

    private String reserv1;

    private String reserv2;

    private String reserve;

    private String timestamp;

    public TblLnsinvesteedetail(String trandate, Integer serseqno, Integer txseqno, String brc, String acctno, String investeeid, String newinvestee, String investeeno, String newinvesteeno, Integer period, Double investeeprin, Double investeeint, Double investeedint, Double investeefee, String trancode,String trantype, Double totalamt, String nintflag, String prinflag, String investeeflag, Double diffnint, Double diffprin, String reserv1, String reserv2, String reserve) {
        this.trandate = trandate;
        this.serseqno = serseqno;
        this.txseqno = txseqno;
        this.brc = brc;
        this.acctno = acctno;
        this.investeeid = investeeid;
        this.newinvestee = newinvestee;
        this.investeeno = investeeno;
        this.newinvesteeno = newinvesteeno;
        this.period = period;
        this.investeeprin =investeeprin;
        this.investeeint = investeeint;
        this.investeedint = investeedint;
        this.investeefee = investeefee;
        this.trancode = trancode;
        this.setTrantype(trantype);
        this.totalamt = totalamt;
        this.nintflag = nintflag;
        this.prinflag = prinflag;
        this.investeeflag = investeeflag;
        this.diffnint = diffnint;
        this.diffprin = diffprin;
        this.reserv1 = reserv1;
        this.reserv2 = reserv2;
        this.reserve = reserve;
    }

    public TblLnsinvesteedetail() {
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

    public String getInvesteeid() {
        return investeeid;
    }

    public void setInvesteeid(String investeeid) {
        this.investeeid = investeeid == null ? null : investeeid.trim();
    }

    public String getNewinvestee() {
        return newinvestee;
    }

    public void setNewinvestee(String newinvestee) {
        this.newinvestee = newinvestee == null ? null : newinvestee.trim();
    }

    public String getInvesteeno() {
        return investeeno;
    }

    public void setInvesteeno(String investeeno) {
        this.investeeno = investeeno == null ? null : investeeno.trim();
    }

    public String getNewinvesteeno() {
        return newinvesteeno;
    }

    public void setNewinvesteeno(String newinvesteeno) {
        this.newinvesteeno = newinvesteeno == null ? null : newinvesteeno.trim();
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Double getInvesteeprin() {
        return investeeprin;
    }

    public void setInvesteeprin(Double investeeprin) {
        this.investeeprin = investeeprin;
    }

    public Double getInvesteeint() {
        return investeeint;
    }

    public void setInvesteeint(Double investeeint) {
        this.investeeint = investeeint;
    }

    public Double getInvesteedint() {
        return investeedint;
    }

    public void setInvesteedint(Double investeedint) {
        this.investeedint = investeedint;
    }

    public Double getInvesteefee() {
        return investeefee;
    }

    public void setInvesteefee(Double investeefee) {
        this.investeefee = investeefee;
    }

    public String getTrancode() {
        return trancode;
    }

    public void setTrancode(String trancode) {
        this.trancode = trancode == null ? null : trancode.trim();
    }

    public Double getTotalamt() {
        return totalamt;
    }

    public void setTotalamt(Double totalamt) {
        this.totalamt = totalamt;
    }

    public String getNintflag() {
        return nintflag;
    }

    public void setNintflag(String nintflag) {
        this.nintflag = nintflag == null ? null : nintflag.trim();
    }

    public String getPrinflag() {
        return prinflag;
    }

    public void setPrinflag(String prinflag) {
        this.prinflag = prinflag == null ? null : prinflag.trim();
    }

    public String getInvesteeflag() {
        return investeeflag;
    }

    public void setInvesteeflag(String investeeflag) {
        this.investeeflag = investeeflag == null ? null : investeeflag.trim();
    }

    public Double getDiffnint() {
        return diffnint;
    }

    public void setDiffnint(Double diffnint) {
        this.diffnint = diffnint;
    }

    public Double getDiffprin() {
        return diffprin;
    }

    public void setDiffprin(Double diffprin) {
        this.diffprin = diffprin;
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

	public String getTrantype() {
		return trantype;
	}

	public void setTrantype(String trantype) {
		this.trantype = trantype;
	}
}
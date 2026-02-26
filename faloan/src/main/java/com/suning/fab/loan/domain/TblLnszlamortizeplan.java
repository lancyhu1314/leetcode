package com.suning.fab.loan.domain;

import java.math.BigDecimal;
import java.sql.Date;
import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSZLAMORTIZEPLAN")
public class TblLnszlamortizeplan {
    private Date trandate = new Date(0);

    private Integer serseqno = 0;

    private String brc = "";

    private String acctno = "";

    private String ccy = "";

    private BigDecimal chargeamt = BigDecimal.valueOf(0.00);

    private BigDecimal contractamt = BigDecimal.valueOf(0.00);

    private BigDecimal rentamt = BigDecimal.valueOf(0.00);

    private BigDecimal monrate = BigDecimal.valueOf(0.00);

    private BigDecimal taxrate = BigDecimal.valueOf(0.00);

    private BigDecimal totalamt = BigDecimal.valueOf(0.00);

    private BigDecimal amortizeamt = BigDecimal.valueOf(0.00);

    private BigDecimal totaltaxamt = BigDecimal.valueOf(0.00);

    private BigDecimal amortizetax = BigDecimal.valueOf(0.00);

    private String lastdate = "";

    private String begindate = "";

    private String enddate = "";

    private Integer period = 0;

    private String amortizeformula = "";

    private String status = "";

    private String timestamp = "";

    public TblLnszlamortizeplan() {
         
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

    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    @Column(name = "Chargeamt")
    public BigDecimal getChargeamt() {
        return chargeamt;
    }

    public void setChargeamt(BigDecimal chargeamt) {
        if(chargeamt == null){
            this.chargeamt = BigDecimal.valueOf(0.00);
        }else{
            this.chargeamt = chargeamt;
        }
    }

    @Column(name = "Contractamt")
    public BigDecimal getContractamt() {
        return contractamt;
    }

    public void setContractamt(BigDecimal contractamt) {
        if(contractamt == null){
            this.contractamt = BigDecimal.valueOf(0.00);
        }else{
            this.contractamt = contractamt;
        }
    }

    @Column(name = "Rentamt")
    public BigDecimal getRentamt() {
        return rentamt;
    }

    public void setRentamt(BigDecimal rentamt) {
        if(rentamt == null){
            this.rentamt = BigDecimal.valueOf(0.00);
        }else{
            this.rentamt = rentamt;
        }
    }

    @Column(name = "Monrate")
    public BigDecimal getMonrate() {
        return monrate;
    }

    public void setMonrate(BigDecimal monrate) {
        if(monrate == null){
            this.monrate = BigDecimal.valueOf(0.00);
        }else{
            this.monrate = monrate;
        }
    }

    @Column(name = "Taxrate")
    public BigDecimal getTaxrate() {
        return taxrate;
    }

    public void setTaxrate(BigDecimal taxrate) {
        if(taxrate == null){
            this.taxrate = BigDecimal.valueOf(0.00);
        }else{
            this.taxrate = taxrate;
        }
    }

    @Column(name = "Totalamt")
    public BigDecimal getTotalamt() {
        return totalamt;
    }

    public void setTotalamt(BigDecimal totalamt) {
        if(totalamt == null){
            this.totalamt = BigDecimal.valueOf(0.00);
        }else{
            this.totalamt = totalamt;
        }
    }

    @Column(name = "Amortizeamt")
    public BigDecimal getAmortizeamt() {
        return amortizeamt;
    }

    public void setAmortizeamt(BigDecimal amortizeamt) {
        if(amortizeamt == null){
            this.amortizeamt = BigDecimal.valueOf(0.00);
        }else{
            this.amortizeamt = amortizeamt;
        }
    }

    @Column(name = "Totaltaxamt")
    public BigDecimal getTotaltaxamt() {
        return totaltaxamt;
    }

    public void setTotaltaxamt(BigDecimal totaltaxamt) {
        if(totaltaxamt == null){
            this.totaltaxamt = BigDecimal.valueOf(0.00);
        }else{
            this.totaltaxamt = totaltaxamt;
        }
    }

    @Column(name = "Amortizetax")
    public BigDecimal getAmortizetax() {
        return amortizetax;
    }

    public void setAmortizetax(BigDecimal amortizetax) {
        if(amortizetax == null){
            this.amortizetax = BigDecimal.valueOf(0.00);
        }else{
            this.amortizetax = amortizetax;
        }
    }

    @Column(name = "Lastdate")
    public String getLastdate() {
        return lastdate;
    }

    public void setLastdate(String lastdate) {
        this.lastdate = lastdate == null ? "" : lastdate.trim();
    }

    @Column(name = "Begindate")
    public String getBegindate() {
        return begindate;
    }

    public void setBegindate(String begindate) {
        this.begindate = begindate == null ? "" : begindate.trim();
    }

    @Column(name = "Enddate")
    public String getEnddate() {
        return enddate;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate == null ? "" : enddate.trim();
    }

    @Column(name = "Period")
    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        if(period == null){
            this.period = 0;
        }else{
            this.period = period;
        }
    }

    @Column(name = "Amortizeformula")
    public String getAmortizeformula() {
        return amortizeformula;
    }

    public void setAmortizeformula(String amortizeformula) {
        this.amortizeformula = amortizeformula == null ? "" : amortizeformula.trim();
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
}
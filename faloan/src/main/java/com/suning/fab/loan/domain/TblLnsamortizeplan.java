package com.suning.fab.loan.domain;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSAMORTIZEPLAN")
public class TblLnsamortizeplan {
    private Date trandate = new Date(0);

    private Integer serseqno = 0;

    private String brc = "";

    private String acctno = "";

    private String amortizetype = "";

    private String ccy = "";

    private Double taxrate = 0.00;

    private Double totalamt = 0.00;

    private Double amortizeamt = 0.00;

    private Double totaltaxamt = 0.00;

    private Double amortizetax = 0.00;

    private String lastdate = "";

    private String begindate = "";

    private String enddate = "";

    private Integer period = 0;

    private String amortizeformula = "";

    private String status = "";

    private String timestamp = "";

    public TblLnsamortizeplan() {
         
    }

    /** 账务日期 */
    @Column(name = "Trandate")
    public Date getTrandate() {
        return trandate;
    }

    /** 账务日期 */
    public void setTrandate(Date trandate) {
        if(trandate == null){
            this.trandate = new Date(0);
        }else{
            this.trandate = trandate;
        }
    }

    /** 流水 */
    @Column(name = "Serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }

    /** 流水 */
    public void setSerseqno(Integer serseqno) {
        if(serseqno == null){
            this.serseqno = 0;
        }else{
            this.serseqno = serseqno;
        }
    }

    /** 网点 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 网点 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    /** 本金账号 */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /** 本金账号 */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    /** 摊销类型 */
    @Column(name = "Amortizetype")
    public String getAmortizetype() {
        return amortizetype;
    }

    /** 摊销类型 */
    public void setAmortizetype(String amortizetype) {
        this.amortizetype = amortizetype == null ? "" : amortizetype.trim();
    }

    /** 币种 */
    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    /** 币种 */
    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    /** 增值税率 */
    @Column(name = "Taxrate")
    public Double getTaxrate() {
        return taxrate;
    }

    /** 增值税率 */
    public void setTaxrate(Double taxrate) {
        if(taxrate == null){
            this.taxrate = 0.00;
        }else{
            this.taxrate = taxrate;
        }
    }

    /** 摊销总金额 */
    @Column(name = "Totalamt")
    public Double getTotalamt() {
        return totalamt;
    }

    /** 摊销总金额 */
    public void setTotalamt(Double totalamt) {
        if(totalamt == null){
            this.totalamt = 0.00;
        }else{
            this.totalamt = totalamt;
        }
    }

    /** 已摊销金额 */
    @Column(name = "Amortizeamt")
    public Double getAmortizeamt() {
        return amortizeamt;
    }

    /** 已摊销金额 */
    public void setAmortizeamt(Double amortizeamt) {
        if(amortizeamt == null){
            this.amortizeamt = 0.00;
        }else{
            this.amortizeamt = amortizeamt;
        }
    }

    /** 摊销总税金 */
    @Column(name = "Totaltaxamt")
    public Double getTotaltaxamt() {
        return totaltaxamt;
    }

    /** 摊销总税金 */
    public void setTotaltaxamt(Double totaltaxamt) {
        if(totaltaxamt == null){
            this.totaltaxamt = 0.00;
        }else{
            this.totaltaxamt = totaltaxamt;
        }
    }

    /** 已摊销税金 */
    @Column(name = "Amortizetax")
    public Double getAmortizetax() {
        return amortizetax;
    }

    /** 已摊销税金 */
    public void setAmortizetax(Double amortizetax) {
        if(amortizetax == null){
            this.amortizetax = 0.00;
        }else{
            this.amortizetax = amortizetax;
        }
    }

    /** 最后摊销日期 */
    @Column(name = "Lastdate")
    public String getLastdate() {
        return lastdate;
    }

    /** 最后摊销日期 */
    public void setLastdate(String lastdate) {
        this.lastdate = lastdate == null ? "" : lastdate.trim();
    }

    /** 合同起始日期 */
    @Column(name = "Begindate")
    public String getBegindate() {
        return begindate;
    }

    /** 合同起始日期 */
    public void setBegindate(String begindate) {
        this.begindate = begindate == null ? "" : begindate.trim();
    }

    /** 合同结束日期 */
    @Column(name = "Enddate")
    public String getEnddate() {
        return enddate;
    }

    /** 合同结束日期 */
    public void setEnddate(String enddate) {
        this.enddate = enddate == null ? "" : enddate.trim();
    }

    /** 已摊销次数 DEFAULT 0 */
    @Column(name = "Period")
    public Integer getPeriod() {
        return period;
    }

    /** 已摊销次数 DEFAULT 0 */
    public void setPeriod(Integer period) {
        if(period == null){
            this.period = 0;
        }else{
            this.period = period;
        }
    }

    /** 摊销公式 */
    @Column(name = "Amortizeformula")
    public String getAmortizeformula() {
        return amortizeformula;
    }

    /** 摊销公式 */
    public void setAmortizeformula(String amortizeformula) {
        this.amortizeformula = amortizeformula == null ? "" : amortizeformula.trim();
    }

    /** 摊销状态 */
    @Column(name = "Status")
    public String getStatus() {
        return status;
    }

    /** 摊销状态 */
    public void setStatus(String status) {
        this.status = status == null ? "" : status.trim();
    }

    /** 修改时间戳 */
    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    /** 修改时间戳 */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
    }
}
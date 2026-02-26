package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.math.BigDecimal;
import java.sql.Date;

@Entity(name = "LNSPENINTPROVREGDTL")
public class TblLnspenintprovregdtl {
    private Date trandate = new Date(0);

    private Integer serseqno = 0;

    private Integer txnseq = 0;

    private String receiptno = "";

    private String brc = "";

    private String ccy = "";

    private Integer period = 0;

    private Integer listno = 0;

    private String billtype = "";

    private Double taxrate = 0.00;

    private Double interest = 0.00;

    private Double tax = 0.00;

    private Date begindate = new Date(0);

    private Date enddate = new Date(0);

    private String timestamp = "";

    private String reserv1 = "";

    private String reserv2 = "";

    public TblLnspenintprovregdtl() {
         
    }

    /** 交易日期 */
    @Column(name = "Trandate")
    public Date getTrandate() {
        return trandate;
    }

    /** 交易日期 */
    public void setTrandate(Date trandate) {
        if(trandate == null){
            this.trandate = new Date(0);
        }else{
            this.trandate = trandate;
        }
    }

    /** 账务流水号 */
    @Column(name = "Serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }

    /** 账务流水号 */
    public void setSerseqno(Integer serseqno) {
        if(serseqno == null){
            this.serseqno = 0;
        }else{
            this.serseqno = serseqno;
        }
    }

    /** 序号 */
    @Column(name = "Txnseq")
    public Integer getTxnseq() {
        return txnseq;
    }

    /** 序号 */
    public void setTxnseq(Integer txnseq) {
        if(txnseq == null){
            this.txnseq = 0;
        }else{
            this.txnseq = txnseq;
        }
    }

    /** 借据 */
    @Column(name = "Receiptno")
    public String getReceiptno() {
        return receiptno;
    }

    /** 借据 */
    public void setReceiptno(String receiptno) {
        this.receiptno = receiptno == null ? "" : receiptno.trim();
    }

    /** 机构 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 机构 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
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

    /** 期数(取试算结果) */
    @Column(name = "Period")
    public Integer getPeriod() {
        return period;
    }

    /** 期数(取试算结果) */
    public void setPeriod(Integer period) {
        if(period == null){
            this.period = 0;
        }else{
            this.period = period;
        }
    }

    /** 条数（累加） */
    @Column(name = "Listno")
    public Integer getListno() {
        return listno;
    }

    /** 条数（累加） */
    public void setListno(Integer listno) {
        if(listno == null){
            this.listno = 0;
        }else{
            this.listno = listno;
        }
    }

    /** 账单类型 GINT宽限期利息 DINT罚息 CINT复利 */
    @Column(name = "Billtype")
    public String getBilltype() {
        return billtype;
    }

    /** 账单类型 GINT宽限期利息 DINT罚息 CINT复利 */
    public void setBilltype(String billtype) {
        this.billtype = billtype == null ? "" : billtype.trim();
    }

    /** 税率 */
    @Column(name = "Taxrate")
    public Double getTaxrate() {
        return taxrate;
    }

    /** 税率 */
    public void setTaxrate(Double taxrate) {
        if(taxrate == null){
            this.taxrate = 0.00;
        }else{
            this.taxrate = taxrate;
        }
    }

    /** 本次入账罚息金额 */
    @Column(name = "Interest")
    public Double getInterest() {
        return interest;
    }

    /** 本次入账罚息金额 */
    public void setInterest(Double interest) {
        if(interest == null){
            this.interest = 0.00;
        }else{
            this.interest = interest;
        }
    }

    /** 本次入账税金金额 */
    @Column(name = "Tax")
    public Double getTax() {
        return tax;
    }

    /** 本次入账税金金额 */
    public void setTax(Double tax) {
        if(tax == null){
            this.tax = 0.00;
        }else{
            this.tax = tax;
        }
    }

    /** 开始日期 */
    @Column(name = "Begindate")
    public Date getBegindate() {
        return begindate;
    }

    /** 开始日期 */
    public void setBegindate(Date begindate) {
        if(begindate == null){
            this.begindate = new Date(0);
        }else{
            this.begindate = begindate;
        }
    }

    /** 结束日期 */
    @Column(name = "Enddate")
    public Date getEnddate() {
        return enddate;
    }

    /** 结束日期 */
    public void setEnddate(Date enddate) {
        if(enddate == null){
            this.enddate = new Date(0);
        }else{
            this.enddate = enddate;
        }
    }

    /** 时间戳 */
    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    /** 时间戳 */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
    }

    /** 备用1 */
    @Column(name = "Reserv1")
    public String getReserv1() {
        return reserv1;
    }

    /** 备用1 */
    public void setReserv1(String reserv1) {
        this.reserv1 = reserv1 == null ? "" : reserv1.trim();
    }

    /** 备用2 */
    @Column(name = "Reserv2")
    public String getReserv2() {
        return reserv2;
    }

    /** 备用2 */
    public void setReserv2(String reserv2) {
        this.reserv2 = reserv2 == null ? "" : reserv2.trim();
    }
}
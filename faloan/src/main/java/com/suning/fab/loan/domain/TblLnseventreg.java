package com.suning.fab.loan.domain;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSEVENTREG")
public class TblLnseventreg {
    private Date trandate = new Date(0);

    private Integer serseqno = 0;

    private Integer txnseq = 0;

    private String billtrandate = "";

    private Integer billserseqno = 0;

    private Integer billtxnseq = 0;

    private String eventcode = "";

    private String brc = "";

    private String teller = "";

    private String receiptno = "";

    private String acctstat = "";

    private String merchantno = "";

    private String custtype = "";

    private String prdcode = "";

    private String toreceiptno = "";

    private String toacctstat = "";

    private String tomerchantno = "";

    private String tocusttype = "";

    private String toprdcode = "";

    private String ccy = "";

    private Double tranamt = 0.00;

    private String trancode = "";

    private String subtrancode = "";

    private String briefcode = "";

    private String tranbrief = "";

    private Double amt1 = 0.00;

    private Double amt2 = 0.00;

    private Double amt3 = 0.00;

    private Double amt4 = 0.00;

    private Double amt5 = 0.00;

    private String bankchannel = "";

    private String investee = "";

    private String investmode = "";

    private String channeltype = "";

    private String fundchannel = "";

    private String outserialno = "";

    private String otherfeetype = "";

    private String chtype = "";

    private String productcode = "";

    private String sendflag = "";

    private Integer sendnum = 0;

    private String timestamp = "";

    private String tunneldata = "";

    private String reserv1 = "";

    private String reserv2 = "";

    private String reserv3 = "";

    private String reserv4 = "";

    private String reserv5 = "";

    public TblLnseventreg() {
         
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

    /** 账单账务日期 */
    @Column(name = "Billtrandate")
    public String getBilltrandate() {
        return billtrandate;
    }

    /** 账单账务日期 */
    public void setBilltrandate(String billtrandate) {
        this.billtrandate = billtrandate == null ? "" : billtrandate.trim();
    }

    /** 账单流水号 */
    @Column(name = "Billserseqno")
    public Integer getBillserseqno() {
        return billserseqno;
    }

    /** 账单流水号 */
    public void setBillserseqno(Integer billserseqno) {
        if(billserseqno == null){
            this.billserseqno = 0;
        }else{
            this.billserseqno = billserseqno;
        }
    }

    @Column(name = "Billtxnseq")
    public Integer getBilltxnseq() {
        return billtxnseq;
    }

    public void setBilltxnseq(Integer billtxnseq) {
        if(billtxnseq == null){
            this.billtxnseq = 0;
        }else{
            this.billtxnseq = billtxnseq;
        }
    }

    /** 事件代码 */
    @Column(name = "Eventcode")
    public String getEventcode() {
        return eventcode;
    }

    /** 事件代码 */
    public void setEventcode(String eventcode) {
        this.eventcode = eventcode == null ? "" : eventcode.trim();
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

    /** 柜员 */
    @Column(name = "Teller")
    public String getTeller() {
        return teller;
    }

    /** 柜员 */
    public void setTeller(String teller) {
        this.teller = teller == null ? "" : teller.trim();
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

    /** 形态 */
    @Column(name = "Acctstat")
    public String getAcctstat() {
        return acctstat;
    }

    /** 形态 */
    public void setAcctstat(String acctstat) {
        this.acctstat = acctstat == null ? "" : acctstat.trim();
    }

    /** 商户编号 */
    @Column(name = "Merchantno")
    public String getMerchantno() {
        return merchantno;
    }

    /** 商户编号 */
    public void setMerchantno(String merchantno) {
        this.merchantno = merchantno == null ? "" : merchantno.trim();
    }

    /** 客户类型 */
    @Column(name = "Custtype")
    public String getCusttype() {
        return custtype;
    }

    /** 客户类型 */
    public void setCusttype(String custtype) {
        this.custtype = custtype == null ? "" : custtype.trim();
    }

    /** 账户产品代码 */
    @Column(name = "Prdcode")
    public String getPrdcode() {
        return prdcode;
    }

    /** 账户产品代码 */
    public void setPrdcode(String prdcode) {
        this.prdcode = prdcode == null ? "" : prdcode.trim();
    }

    /** 转入方借据 */
    @Column(name = "Toreceiptno")
    public String getToreceiptno() {
        return toreceiptno;
    }

    /** 转入方借据 */
    public void setToreceiptno(String toreceiptno) {
        this.toreceiptno = toreceiptno == null ? "" : toreceiptno.trim();
    }

    /** 转入方形态 */
    @Column(name = "Toacctstat")
    public String getToacctstat() {
        return toacctstat;
    }

    /** 转入方形态 */
    public void setToacctstat(String toacctstat) {
        this.toacctstat = toacctstat == null ? "" : toacctstat.trim();
    }

    /** 收款方商户编号 */
    @Column(name = "Tomerchantno")
    public String getTomerchantno() {
        return tomerchantno;
    }

    /** 收款方商户编号 */
    public void setTomerchantno(String tomerchantno) {
        this.tomerchantno = tomerchantno == null ? "" : tomerchantno.trim();
    }

    /** 收款方客户类型 */
    @Column(name = "Tocusttype")
    public String getTocusttype() {
        return tocusttype;
    }

    /** 收款方客户类型 */
    public void setTocusttype(String tocusttype) {
        this.tocusttype = tocusttype == null ? "" : tocusttype.trim();
    }

    /** 收款方账户产品代码 */
    @Column(name = "Toprdcode")
    public String getToprdcode() {
        return toprdcode;
    }

    /** 收款方账户产品代码 */
    public void setToprdcode(String toprdcode) {
        this.toprdcode = toprdcode == null ? "" : toprdcode.trim();
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

    /** 交易金额 */
    @Column(name = "Tranamt")
    public Double getTranamt() {
        return tranamt;
    }

    /** 交易金额 */
    public void setTranamt(Double tranamt) {
        if(tranamt == null){
            this.tranamt = 0.00;
        }else{
            this.tranamt = tranamt;
        }
    }

    /** 交易码 */
    @Column(name = "Trancode")
    public String getTrancode() {
        return trancode;
    }

    /** 交易码 */
    public void setTrancode(String trancode) {
        this.trancode = trancode == null ? "" : trancode.trim();
    }

    /** 子交易代码 */
    @Column(name = "Subtrancode")
    public String getSubtrancode() {
        return subtrancode;
    }

    /** 子交易代码 */
    public void setSubtrancode(String subtrancode) {
        this.subtrancode = subtrancode == null ? "" : subtrancode.trim();
    }

    /** 摘要代码 */
    @Column(name = "Briefcode")
    public String getBriefcode() {
        return briefcode;
    }

    /** 摘要代码 */
    public void setBriefcode(String briefcode) {
        this.briefcode = briefcode == null ? "" : briefcode.trim();
    }

    /** 摘要描述 */
    @Column(name = "Tranbrief")
    public String getTranbrief() {
        return tranbrief;
    }

    /** 摘要描述 */
    public void setTranbrief(String tranbrief) {
        this.tranbrief = tranbrief == null ? "" : tranbrief.trim();
    }

    /** 交易金额 */
    @Column(name = "Amt1")
    public Double getAmt1() {
        return amt1;
    }

    /** 交易金额 */
    public void setAmt1(Double amt1) {
        if(amt1 == null){
            this.amt1 = 0.00;
        }else{
            this.amt1 = amt1;
        }
    }

    /** 交易金额 */
    @Column(name = "Amt2")
    public Double getAmt2() {
        return amt2;
    }

    /** 交易金额 */
    public void setAmt2(Double amt2) {
        if(amt2 == null){
            this.amt2 = 0.00;
        }else{
            this.amt2 = amt2;
        }
    }

    /** 交易金额 */
    @Column(name = "Amt3")
    public Double getAmt3() {
        return amt3;
    }

    /** 交易金额 */
    public void setAmt3(Double amt3) {
        if(amt3 == null){
            this.amt3 = 0.00;
        }else{
            this.amt3 = amt3;
        }
    }

    /** 交易金额 */
    @Column(name = "Amt4")
    public Double getAmt4() {
        return amt4;
    }

    /** 交易金额 */
    public void setAmt4(Double amt4) {
        if(amt4 == null){
            this.amt4 = 0.00;
        }else{
            this.amt4 = amt4;
        }
    }

    /** 交易金额 */
    @Column(name = "Amt5")
    public Double getAmt5() {
        return amt5;
    }

    /** 交易金额 */
    public void setAmt5(Double amt5) {
        if(amt5 == null){
            this.amt5 = 0.00;
        }else{
            this.amt5 = amt5;
        }
    }

    /** 银行渠道编号 */
    @Column(name = "Bankchannel")
    public String getBankchannel() {
        return bankchannel;
    }

    /** 银行渠道编号 */
    public void setBankchannel(String bankchannel) {
        this.bankchannel = bankchannel == null ? "" : bankchannel.trim();
    }

    /** 接收投资者 */
    @Column(name = "Investee")
    public String getInvestee() {
        return investee;
    }

    /** 接收投资者 */
    public void setInvestee(String investee) {
        this.investee = investee == null ? "" : investee.trim();
    }

    /** 投放模式：保理、消费贷 */
    @Column(name = "Investmode")
    public String getInvestmode() {
        return investmode;
    }

    /** 投放模式：保理、消费贷 */
    public void setInvestmode(String investmode) {
        this.investmode = investmode == null ? "" : investmode.trim();
    }

    /** 放款渠道 */
    @Column(name = "Channeltype")
    public String getChanneltype() {
        return channeltype;
    }

    /** 放款渠道 */
    public void setChanneltype(String channeltype) {
        this.channeltype = channeltype == null ? "" : channeltype.trim();
    }

    /** 资金通道 */
    @Column(name = "Fundchannel")
    public String getFundchannel() {
        return fundchannel;
    }

    /** 资金通道 */
    public void setFundchannel(String fundchannel) {
        this.fundchannel = fundchannel == null ? "" : fundchannel.trim();
    }

    /** 外部流水单号 */
    @Column(name = "Outserialno")
    public String getOutserialno() {
        return outserialno;
    }

    /** 外部流水单号 */
    public void setOutserialno(String outserialno) {
        this.outserialno = outserialno == null ? "" : outserialno.trim();
    }

    /** 其他收入类型 */
    @Column(name = "Otherfeetype")
    public String getOtherfeetype() {
        return otherfeetype;
    }

    /** 其他收入类型 */
    public void setOtherfeetype(String otherfeetype) {
        this.otherfeetype = otherfeetype == null ? "" : otherfeetype.trim();
    }

    /** 收单类型 */
    @Column(name = "Chtype")
    public String getChtype() {
        return chtype;
    }

    /** 收单类型 */
    public void setChtype(String chtype) {
        this.chtype = chtype == null ? "" : chtype.trim();
    }

    /** 产品编码 */
    @Column(name = "Productcode")
    public String getProductcode() {
        return productcode;
    }

    /** 产品编码 */
    public void setProductcode(String productcode) {
        this.productcode = productcode == null ? "" : productcode.trim();
    }

    /** 发送标志 0-成功1-失败5-发送中9-初始 */
    @Column(name = "Sendflag")
    public String getSendflag() {
        return sendflag;
    }

    /** 发送标志 0-成功1-失败5-发送中9-初始 */
    public void setSendflag(String sendflag) {
        this.sendflag = sendflag == null ? "" : sendflag.trim();
    }

    /** 发送次数 */
    @Column(name = "Sendnum")
    public Integer getSendnum() {
        return sendnum;
    }

    /** 发送次数 */
    public void setSendnum(Integer sendnum) {
        if(sendnum == null){
            this.sendnum = 0;
        }else{
            this.sendnum = sendnum;
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

    /** 隧道字段 */
    @Column(name = "Tunneldata")
    public String getTunneldata() {
        return tunneldata;
    }

    /** 隧道字段 */
    public void setTunneldata(String tunneldata) {
        this.tunneldata = tunneldata == null ? "" : tunneldata.trim();
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

    /** 备用3 */
    @Column(name = "Reserv3")
    public String getReserv3() {
        return reserv3;
    }

    /** 备用3 */
    public void setReserv3(String reserv3) {
        this.reserv3 = reserv3 == null ? "" : reserv3.trim();
    }

    /** 备用4 */
    @Column(name = "Reserv4")
    public String getReserv4() {
        return reserv4;
    }

    /** 备用4 */
    public void setReserv4(String reserv4) {
        this.reserv4 = reserv4 == null ? "" : reserv4.trim();
    }

    /** 备用5 */
    @Column(name = "Reserv5")
    public String getReserv5() {
        return reserv5;
    }

    /** 备用5 */
    public void setReserv5(String reserv5) {
        this.reserv5 = reserv5 == null ? "" : reserv5.trim();
    }
}
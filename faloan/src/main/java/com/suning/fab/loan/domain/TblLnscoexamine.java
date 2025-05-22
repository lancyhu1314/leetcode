package com.suning.fab.loan.domain;

import javax.persistence.Entity;
import javax.persistence.Column;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：考核表 LNSCOEXAMINE
 *
 * @Author 18049705 MYP
 * @Date Created in 10:04 2019/9/3
 * @see
 */
@Entity(name = "LNSCOEXAMINE")
public class TblLnscoexamine {

    String trandate="";
    String serialno="";
    String accdate="";
    Integer serseqno=0;
    String brc="";
    String trancode="";
    String channelid="";
    String acctno="";
    String userno="";
    String employeeid="";
    String salesstore="";
    String flowchannel="";
    String terminalcode="";
    String contractcode="";
    String promotionid="";
    String fundingmodel="";
    String fundingid="";
    String undertake="";
    String undertakeid="";
    String bsno = "";
    @Column(name = "trandate")
    public String getTrandate() {
        return trandate;
    }
    @Column(name = "serialno")
    public String getSerialno() {
        return serialno;
    }
    @Column(name = "accdate")
    public String getAccdate() {
        return accdate;
    }
    @Column(name = "serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }
    @Column(name = "brc")
    public String getBrc() {
        return brc;
    }
    @Column(name = "trancode")
    public String getTrancode() {
        return trancode;
    }
    @Column(name = "channelid")
    public String getChannelid() {
        return channelid;
    }
    @Column(name = "acctno")
    public String getAcctno() {
        return acctno;
    }
    @Column(name = "userno")
    public String getUserno() {
        return userno;
    }
    @Column(name = "employeeid")
    public String getEmployeeid() {
        return employeeid;
    }
    @Column(name = "salesstore")
    public String getSalesstore() {
        return salesstore;
    }
    @Column(name = "flowchannel")
    public String getFlowchannel() {
        return flowchannel;
    }
    @Column(name = "terminalcode")
    public String getTerminalcode() {
        return terminalcode;
    }
    @Column(name = "contractcode")
    public String getContractcode() {
        return contractcode;
    }
    @Column(name = "promotionid")
    public String getPromotionid() {
        return promotionid;
    }
    @Column(name = "fundingmodel")
    public String getFundingmodel() {
        return fundingmodel;
    }
    @Column(name = "fundingid")
    public String getFundingid() {
        return fundingid;
    }
    @Column(name = "undertake")
    public String getUndertake() {
        return undertake;
    }
    @Column(name = "undertakeid")
    public String getUndertakeid() {
        return undertakeid;
    }
    @Column(name = "bsno")
    public String getBsno() {
        return bsno;
    }
    /**
     * Sets the trandate.
     *
     * @param trandate trandate
     */
    public void setTrandate(String trandate) {
        this.trandate = trandate;

    }

    /**
     * Sets the serialno.
     *
     * @param serialno serialno
     */
    public void setSerialno(String serialno) {
        this.serialno = serialno;

    }

    /**
     * Sets the accdate.
     *
     * @param accdate accdate
     */
    public void setAccdate(String accdate) {
        this.accdate = accdate;

    }

    /**
     * Sets the serseqno.
     *
     * @param serseqno serseqno
     */
    public void setSerseqno(Integer serseqno) {
        this.serseqno = serseqno;

    }

    /**
     * Sets the brc.
     *
     * @param brc brc
     */
    public void setBrc(String brc) {
        this.brc = brc;

    }

    /**
     * Sets the trancode.
     *
     * @param trancode trancode
     */
    public void setTrancode(String trancode) {
        this.trancode = trancode;

    }

    /**
     * Sets the channelid.
     *
     * @param channelid channelid
     */
    public void setChannelid(String channelid) {
        this.channelid = channelid;

    }

    /**
     * Sets the acctno.
     *
     * @param acctno acctno
     */
    public void setAcctno(String acctno) {
        this.acctno = acctno;

    }

    /**
     * Sets the userno.
     *
     * @param userno userno
     */
    public void setUserno(String userno) {
        this.userno = userno;

    }

    /**
     * Sets the employeeid.
     *
     * @param employeeid employeeid
     */
    public void setEmployeeid(String employeeid) {
        this.employeeid = employeeid;

    }

    /**
     * Sets the salesstore.
     *
     * @param salesstore salesstore
     */
    public void setSalesstore(String salesstore) {
        this.salesstore = salesstore;

    }

    /**
     * Sets the flowchannel.
     *
     * @param flowchannel flowchannel
     */
    public void setFlowchannel(String flowchannel) {
        this.flowchannel = flowchannel;

    }

    /**
     * Sets the terminalcode.
     *
     * @param terminalcode terminalcode
     */
    public void setTerminalcode(String terminalcode) {
        this.terminalcode = terminalcode;

    }

    /**
     * Sets the contractcode.
     *
     * @param contractcode contractcode
     */
    public void setContractcode(String contractcode) {
        this.contractcode = contractcode;

    }

    /**
     * Sets the promotionid.
     *
     * @param promotionid promotionid
     */
    public void setPromotionid(String promotionid) {
        this.promotionid = promotionid;

    }

    /**
     * Sets the fundingmodel.
     *
     * @param fundingmodel fundingmodel
     */
    public void setFundingmodel(String fundingmodel) {
        this.fundingmodel = fundingmodel;

    }

    /**
     * Sets the fundingid.
     *
     * @param fundingid fundingid
     */
    public void setFundingid(String fundingid) {
        this.fundingid = fundingid;

    }

    /**
     * Sets the undertake.
     *
     * @param undertake undertake
     */
    public void setUndertake(String undertake) {
        this.undertake = undertake;

    }

    /**
     * Sets the undertakeid.
     *
     * @param undertakeid undertakeid
     */
    public void setUndertakeid(String undertakeid) {
        this.undertakeid = undertakeid;

    }

    /**
     * Sets the bsNo.
     *
     * @param bsno bsno
     */
    public void setBsno(String bsno) {
        this.bsno = bsno;

    }
}

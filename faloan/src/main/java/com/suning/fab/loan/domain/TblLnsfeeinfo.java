package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705 MYP
 * @Date Created in 17:19 2019/12/2
 * @see
 */
@Entity(name = "LNSFEEINFO")
public class TblLnsfeeinfo implements Serializable {

    private static final long serialVersionUID = 1175724579040314358L;

    private String openbrc="";           // 开户机构代码
    private String acctno="";            // 帐号
    private String ccy="";               // 币种
    private String repayway="";	       // 费用交取模式
    private String feetype="";	       // 费用账本类型
    private String feebrc="";	           // 费用机构代码
    private String calculatrule="";   // 费用计算方式
    private String provisionrule="";   // 费用计息方式
    private String lastfeedate="";	   // 上次结费日
    private Integer feeperiod = 1;       //落表的当前期数
    private String advancesettle="";	   // 提前结清收取方式
    private String feebase ="";         //费用计息基数
    private Double deducetionamt=0.00;	   // 扣费金额
    private String feeformula="";	   // 还费周期公式
    private String provisionflag="";   // 计提结束标志
    private Double feerate=0.00;	       // 费率利率
    private Double overrate=0.00;	       // 逾期违约金利率
    private String trandate="";	       // 交易日期
    private String trantime="";	       // 交易时间
    private String opendate="";	       // 开户日工作日期
    private String feestat="";	       // 费用状态
    private String reserv1="";	       // 备用字段1
    private Double amount=0.00;	           // 备用金额1
    private String reserv2="";	       // 备用字段2
    private String reserv3="";	       // 备用字段3
    private String reserv4="";	       // 备用字段4
    private String reserv5="";	       // 备用字段5


    //克隆
    public TblLnsfeeinfo cloneFeeInfo(){

        TblLnsfeeinfo tblLnsfeeinfo = new TblLnsfeeinfo(   openbrc,  acctno,  ccy,  repayway,  feetype,  feebrc,  calculatrule,  provisionrule,  lastfeedate,  feeperiod,  advancesettle,  feebase,    deducetionamt,  feeformula,  provisionflag,    feerate,    overrate,  trandate,  trantime,  opendate,  feestat,  reserv1,    amount,  reserv2,  reserv3,  reserv4,  reserv5);

        return tblLnsfeeinfo;
    }

    public TblLnsfeeinfo() {
    }

    public TblLnsfeeinfo(String openbrc, String acctno, String ccy, String repayway, String feetype, String feebrc, String calculatrule, String provisionrule, String lastfeedate, Integer feeperiod, String advancesettle, String feebase, Double deducetionamt, String feeformula, String provisionflag, Double feerate, Double overrate, String trandate, String trantime, String opendate, String feestat, String reserv1, Double amount, String reserv2, String reserv3, String reserv4, String reserv5) {
        this.openbrc = openbrc;
        this.acctno = acctno;
        this.ccy = ccy;
        this.repayway = repayway;
        this.feetype = feetype;
        this.feebrc = feebrc;
        this.calculatrule = calculatrule;
        this.provisionrule = provisionrule;
        this.lastfeedate = lastfeedate;
        this.feeperiod = feeperiod;
        this.advancesettle = advancesettle;
        this.feebase = feebase;
        this.deducetionamt = deducetionamt;
        this.feeformula = feeformula;
        this.provisionflag = provisionflag;
        this.feerate = feerate;
        this.overrate = overrate;
        this.trandate = trandate;
        this.trantime = trantime;
        this.opendate = opendate;
        this.feestat = feestat;
        this.reserv1 = reserv1;
        this.amount = amount;
        this.reserv2 = reserv2;
        this.reserv3 = reserv3;
        this.reserv4 = reserv4;
        this.reserv5 = reserv5;
    }

    /**
     * Gets the value of feeperiod.
     *
     * @return the value of feeperiod
     */
    public Integer getFeeperiod() {
        return feeperiod;
    }

    /**
     * Sets the feeperiod.
     *
     * @param feeperiod feeperiod
     */
    public void setFeeperiod(Integer feeperiod) {
        this.feeperiod = feeperiod;

    }

    /**
     * Gets the value of calculatrule.
     *
     * @return the value of calculatrule
     */
    public String getCalculatrule() {
        return calculatrule.trim();
    }

    /**
     * Sets the calculatrule.
     *
     * @param calculatrule calculatrule
     */
    public void setCalculatrule(String calculatrule) {
        this.calculatrule = calculatrule;

    }

    /**
     * Gets the value of feebase.
     *
     * @return the value of feebase
     */
    @Column(name = "feebase")
    public String getFeebase() {
        return feebase.trim();
    }

    /**
     * Sets the feebase.
     *
     * @param feebase feebase
     */
    public void setFeebase(String feebase) {
        this.feebase = feebase;

    }

    /**
     * Gets the value of openbrc.
     *
     * @return the value of openbrc
     */
    @Column(name = "openbrc")
    public String getOpenbrc() {
        return openbrc.trim();
    }

    /**
     * Sets the openbrc.
     *
     * @param openbrc openbrc
     */
    public void setOpenbrc(String openbrc) {
        this.openbrc = openbrc;

    }

    /**
     * Gets the value of acctno.
     *
     * @return the value of acctno
     */
    @Column(name = "acctno")
    public String getAcctno() {
        return acctno.trim();
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
     * Gets the value of ccy.
     *
     * @return the value of ccy
     */
    @Column(name = "ccy")
    public String getCcy() {
        return ccy.trim();
    }

    /**
     * Sets the ccy.
     *
     * @param ccy ccy
     */
    public void setCcy(String ccy) {
        this.ccy = ccy;

    }

    /**
     * Gets the value of repayway.
     *
     * @return the value of repayway
     */
    @Column(name = "repayway")
    public String getRepayway() {
        return repayway.trim();
    }

    /**
     * Sets the repayway.
     *
     * @param repayway repayway
     */
    public void setRepayway(String repayway) {
        this.repayway = repayway;

    }

    /**
     * Gets the value of feetype.
     *
     * @return the value of feetype
     */
    @Column(name = "feetype")
    public String getFeetype() {
        return feetype.trim();
    }

    /**
     * Sets the feetype.
     *
     * @param feetype feetype
     */
    public void setFeetype(String feetype) {
        this.feetype = feetype;

    }

    /**
     * Gets the value of feebrc.
     *
     * @return the value of feebrc
     */
    @Column(name = "feebrc")
    public String getFeebrc() {
        return feebrc.trim();
    }

    /**
     * Sets the feebrc.
     *
     * @param feebrc feebrc
     */
    public void setFeebrc(String feebrc) {
        this.feebrc = feebrc;

    }

    /**
     * Gets the value of provisionrule.
     *
     * @return the value of provisionrule
     */
    @Column(name = "provisionrule")
    public String getProvisionrule() {
        return provisionrule.trim();
    }

    /**
     * Sets the provisionrule.
     *
     * @param provisionrule provisionrule
     */
    public void setProvisionrule(String provisionrule) {
        this.provisionrule = provisionrule;

    }

    /**
     * Gets the value of lastfeedate.
     *
     * @return the value of lastfeedate
     */
    @Column(name = "lastfeedate")
    public String getLastfeedate() {
        return lastfeedate.trim();
    }

    /**
     * Sets the lastfeedate.
     *
     * @param lastfeedate lastfeedate
     */
    public void setLastfeedate(String lastfeedate) {
        this.lastfeedate = lastfeedate;

    }

    /**
     * Gets the value of advancesettle.
     *
     * @return the value of advancesettle
     */
    @Column(name = "advancesettle")
    public String getAdvancesettle() {
        return advancesettle.trim();
    }

    /**
     * Sets the advancesettle.
     *
     * @param advancesettle advancesettle
     */
    public void setAdvancesettle(String advancesettle) {
        this.advancesettle = advancesettle;

    }

    /**
     * Gets the value of deducetionamt.
     *
     * @return the value of deducetionamt
     */
    @Column(name = "deducetionamt")
    public Double getDeducetionamt() {
        return deducetionamt;
    }



    /**
     * Gets the value of feeformula.
     *
     * @return the value of feeformula
     */
    @Column(name = "feeformula")
    public String getFeeformula() {
        return feeformula.trim();
    }

    /**
     * Sets the feeformula.
     *
     * @param feeformula feeformula
     */
    public void setFeeformula(String feeformula) {
        this.feeformula = feeformula;

    }

    /**
     * Gets the value of provisionflag.
     *
     * @return the value of provisionflag
     */
    @Column(name = "provisionflag")
    public String getProvisionflag() {
        return provisionflag.trim();
    }

    /**
     * Sets the provisionflag.
     *
     * @param provisionflag provisionflag
     */
    public void setProvisionflag(String provisionflag) {
        this.provisionflag = provisionflag;

    }

    /**
     * Gets the value of feerate.
     *
     * @return the value of feerate
     */
    @Column(name = "feerate")
    public Double getFeerate() {
        return feerate;
    }

    /**
     * Sets the feerate.
     *
     * @param feerate feerate
     */
    public void setFeerate(Double feerate) {
        this.feerate = feerate;

    }

    /**
     * Gets the value of overrate.
     *
     * @return the value of overrate
     */
    @Column(name = "overrate")
    public Double getOverrate() {
        return overrate;
    }

    /**
     * Sets the overrate.
     *
     * @param overrate overrate
     */
    public void setOverrate(Double overrate) {
        this.overrate = overrate;

    }

    /**
     * Gets the value of trandate.
     *
     * @return the value of trandate
     */
    @Column(name = "trandate")
    public String getTrandate() {
        return trandate.trim();
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
     * Gets the value of trantime.
     *
     * @return the value of trantime
     */
    @Column(name = "trantime")
    public String getTrantime() {
        return trantime.trim();
    }

    /**
     * Sets the trantime.
     *
     * @param trantime trantime
     */
    public void setTrantime(String trantime) {
        this.trantime = trantime;

    }

    /**
     * Gets the value of opendate.
     *
     * @return the value of opendate
     */
    @Column(name = "opendate")
    public String getOpendate() {
        return opendate.trim();
    }

    /**
     * Sets the opendate.
     *
     * @param opendate opendate
     */
    public void setOpendate(String opendate) {
        this.opendate = opendate;

    }

    /**
     * Gets the value of feestat.
     *
     * @return the value of feestat
     */
    @Column(name = "feestat")
    public String getFeestat() {
        return feestat.trim();
    }

    /**
     * Sets the feestat.
     *
     * @param feestat feestat
     */
    public void setFeestat(String feestat) {
        this.feestat = feestat;

    }

    /**
     * Gets the value of reserv1.
     *
     * @return the value of reserv1
     */
    @Column(name = "reserv1")
    public String getReserv1() {
        return reserv1;
    }

    /**
     * Sets the reserv1.
     *
     * @param reserv1 reserv1
     */
    public void setReserv1(String reserv1) {
        this.reserv1 = reserv1;

    }

    /**
     * Gets the value of amount.
     *
     * @return the value of amount
     */
    @Column(name = "amount")
    public Double getAmount() {
        return amount;
    }

    /**
     * Sets the amount.
     *
     * @param amount amount
     */
    public void setAmount(Double amount) {
        this.amount = amount;

    }

    /**
     * Gets the value of reserv2.
     *
     * @return the value of reserv2
     */
    @Column(name = "reserv2")
    public String getReserv2() {
        return reserv2;
    }

    /**
     * Sets the reserv2.
     *
     * @param reserv2 reserv2
     */
    public void setReserv2(String reserv2) {
        this.reserv2 = reserv2;

    }

    /**
     * Gets the value of reserv3.
     *
     * @return the value of reserv3
     */
    @Column(name = "reserv3")
    public String getReserv3() {
        return reserv3;
    }

    /**
     * Sets the reserv3.
     *
     * @param reserv3 reserv3
     */
    public void setReserv3(String reserv3) {
        this.reserv3 = reserv3;

    }

    /**
     * Gets the value of reserv4.
     *
     * @return the value of reserv4
     */
    @Column(name = "reserv4")
    public String getReserv4() {
        return reserv4;
    }

    /**
     * Sets the reserv4.
     *
     * @param reserv4 reserv4
     */
    public void setReserv4(String reserv4) {
        this.reserv4 = reserv4;

    }

    /**
     * Gets the value of reserv5.
     *
     * @return the value of reserv5
     */
    @Column(name = "reserv5")
    public String getReserv5() {
        return reserv5;
    }

    /**
     * Sets the reserv5.
     *
     * @param reserv5 reserv5
     */
    public void setReserv5(String reserv5) {
        this.reserv5 = reserv5;

    }

    /**
     * Sets the deducetionamt.
     *
     * @param deducetionamt deducetionamt
     */
    public void setDeducetionamt(Double deducetionamt) {
        this.deducetionamt = deducetionamt;

    }
}

package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.suning.fab.loan.utils.ConstantDeclare;

@Entity(name = "LNSINTERFACE")
public class TblLnsinterface {
    private String serialno = "";

    private String trancode = "";

    private String trandate = "";

    private String accdate = "";

    private Integer serseqno = 0;

    private String brc = "";

    private String acctname = "";

    private String userno = "";

    private String acctno = "";

    private Double tranamt = 0.00;

    private Double sumrint = 0.00;

    private Double sumramt = 0.00;

    private Double sumrfint = 0.00;

    private Double sumdelint = 0.00;

    private Double sumdelfint = 0.00;

    private String acctflag = "";

    private String timestamp = "";

    private String reserv1 = "";

    private String reserv2 = "";

    private String reserv3 = "";

    private String reserv4 = "";

    private String reserv5 = "";

    private String reserv6 = "";

    private String orgid = "";

    private String billno = "";

    private String bankno = "";

    private String magacct = "";

    public TblLnsinterface() {
         
    }

    /** 幂等流水号 */
    @Column(name = "Serialno")
    public String getSerialno() {
        return serialno;
    }

    /** 幂等流水号 */
    public void setSerialno(String serialno) {
        this.serialno = serialno == null ? "" : serialno.trim();
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

    /** 账务日期 */
    @Column(name = "Trandate")
    public String getTrandate() {
        return trandate;
    }

    /** 账务日期 */
    public void setTrandate(String trandate) {
        this.trandate = trandate == null ? "" : trandate.trim();
    }

    /** 自然日期 */
    @Column(name = "Accdate")
    public String getAccdate() {
        return accdate;
    }

    /** 自然日期 */
    public void setAccdate(String accdate) {
        this.accdate = accdate == null ? "" : accdate.trim();
    }

    /** 系统流水号 */
    @Column(name = "Serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }

    /** 系统流水号 */
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

    /** 户名 */
    @Column(name = "Acctname")
    public String getAcctname() {
        return acctname;
    }

    /** 户名 */
    public void setAcctname(String acctname) {
        this.acctname = acctname == null ? "" : acctname.trim();
    }

    /** 预收账号 */
    @Column(name = "Userno")
    public String getUserno() {
        return userno;
    }

    /** 预收账号 */
    public void setUserno(String userno) {
        this.userno = userno == null ? "" : userno.trim();
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

    /** 开户/冲销金额 */
    @Column(name = "Tranamt")
    public Double getTranamt() {
        return tranamt;
    }

    /** 开户/冲销金额 */
    public void setTranamt(Double tranamt) {
        if(tranamt == null){
            this.tranamt = 0.00;
        }else{
            this.tranamt = tranamt;
        }
    }

    /** 还款利息 */
    @Column(name = "Sumrint")
    public Double getSumrint() {
        return sumrint;
    }

    /** 还款利息 */
    public void setSumrint(Double sumrint) {
        if(sumrint == null){
            this.sumrint = 0.00;
        }else{
            this.sumrint = sumrint;
        }
    }

    /** 还款本金 */
    @Column(name = "Sumramt")
    public Double getSumramt() {
        return sumramt;
    }

    /** 还款本金 */
    public void setSumramt(Double sumramt) {
        if(sumramt == null){
            this.sumramt = 0.00;
        }else{
            this.sumramt = sumramt;
        }
    }

    /** 还款罚息 */
    @Column(name = "Sumrfint")
    public Double getSumrfint() {
        return sumrfint;
    }

    /** 还款罚息 */
    public void setSumrfint(Double sumrfint) {
        if(sumrfint == null){
            this.sumrfint = 0.00;
        }else{
            this.sumrfint = sumrfint;
        }
    }

    /** 减免利息 */
    @Column(name = "Sumdelint")
    public Double getSumdelint() {
        return sumdelint;
    }

    /** 减免利息 */
    public void setSumdelint(Double sumdelint) {
        if(sumdelint == null){
            this.sumdelint = 0.00;
        }else{
            this.sumdelint = sumdelint;
        }
    }

    /** 减免罚息 */
    @Column(name = "Sumdelfint")
    public Double getSumdelfint() {
        return sumdelfint;
    }

    /** 减免罚息 */
    public void setSumdelfint(Double sumdelfint) {
        if(sumdelfint == null){
            this.sumdelfint = 0.00;
        }else{
            this.sumdelfint = sumdelfint;
        }
    }

    /** 结清标志 3-结清 */
    @Column(name = "Acctflag")
    public String getAcctflag() {
        return acctflag;
    }

    /** 结清标志 3-结清 */
    public void setAcctflag(String acctflag) {
        this.acctflag = acctflag == null ? "" : acctflag.trim();
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

    /** 备用5存放资金channel */
    @Column(name = "Reserv5")
    public String getReserv5() {
        return reserv5;
    }

    /** 备用5存放资金channel */
    public void setReserv5(String reserv5) {
        this.reserv5 = reserv5 == null ? "" : reserv5.trim();
    }

    /** 备用6 */
    @Column(name = "Reserv6")
    public String getReserv6() {
        return reserv6;
    }

    /** 备用6 */
    public void setReserv6(String reserv6) {
        this.reserv6 = reserv6 == null ? "" : reserv6.trim();
    }

    /** 门店公司商户号 */
    @Column(name = "Orgid")
    public String getOrgid() {
        return orgid;
    }

    /** 门店公司商户号 */
    public void setOrgid(String orgid) {
        this.orgid = orgid == null ? "" : orgid.trim();
    }

    /** 结算单号 */
    @Column(name = "Billno")
    public String getBillno() {
        return billno;
    }

    /** 结算单号 */
    public void setBillno(String billno) {
        this.billno = billno == null ? "" : billno.trim();
    }

    /** 银行流水号/易付宝单号/POS单号 */
    @Column(name = "Bankno")
    public String getBankno() {
        return bankno;
    }

    /** 银行流水号/易付宝单号/POS单号 */
    public void setBankno(String bankno) {
        this.bankno = bankno == null ? "" : bankno.trim();
    }

    /** 借据号 */
    @Column(name = "Magacct")
    public String getMagacct() {
        return magacct;
    }

    /** 借据号 */
    public void setMagacct(String magacct) {
        this.magacct = magacct == null ? "" : magacct.trim();
    }

    /**
     * acctFlag:1-正常还款，2-逾期还款，3-正常结清，4-逾期结清
     * @return endflag 1-未结清，3-结清
     */
    public  String acctFlag2Endflag(){
        if(ConstantDeclare.REPAYFLAG.REPAY_EXCEED.equals(acctflag))
            return ConstantDeclare.REPAYFLAG.REPAY_NORMAL;
        if(ConstantDeclare.REPAYFLAG.SETTLE_EXCEED.equals(acctflag))
            return ConstantDeclare.REPAYFLAG.SETTLE_NORMAL;
        return acctflag;
    }

    /**
     *
     * @param endFlag 1-未结清，3-结清
     * @param ifOverdue 是否逾期
     * @return  1-正常还款，2-逾期还款，3-正常结清，4-逾期结清
     */
    public  static String endflag2AcctFlag2(String endFlag,boolean ifOverdue){

        if(ifOverdue){
            if(ConstantDeclare.REPAYFLAG.REPAY_NORMAL.equals(endFlag))
                return ConstantDeclare.REPAYFLAG.REPAY_EXCEED;
            if(ConstantDeclare.REPAYFLAG.SETTLE_NORMAL.equals(endFlag))
                return ConstantDeclare.REPAYFLAG.SETTLE_EXCEED;
        }
        return endFlag;
    }




}
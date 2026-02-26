package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSACCOUNTLIST")
public class TblLnsaccountlist {
    private String acctno = "";

    private String acctstat = "";

    private Integer ordnu = 0;

    private String brc = "";

    private String trandate = "";

    private String billtrandate = "";

    private Integer billserseqno = 0;

    private Integer billtxnseq = 0;

    private String inacctno = "";

    private String subctrlcode = "";

    private String acctype = "";

    private String ccy = "";

    private String cdflag = "";

    private Double tranamt = 0.00;

    private Double bal = 0.00;

    private Integer days = 0;

    private Double accum = 0.00;

    private String voukind = "";

    private String precharcode = "";

    private String voucherno = "";

    private String memocode = "";

    private String memoname = "";

    private String oppacctno = "";

    private String canacctno = "";

    private String modifytime = "";

    private Integer serseqno = 0;

    private Integer txnseq = 0;

    private String trancode = "";

    private String subtrancode = "";

    private String tranbrc = "";

    private String tellercode = "";

    private String authcode = "";

    private String checkcode = "";

    private Integer pgnm = 0;

    private String printflag = "";

    private String transource = "";

    private String recordtype = "";

    private String flag1 = "";

    private String flag2 = "";

    private String flag3 = "";

    private Integer seq = 0;

    private Integer seq1 = 0;

    public TblLnsaccountlist() {
         
    }

    /** 账号 */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /** 账号 */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    /** 账户形态 */
    @Column(name = "Acctstat")
    public String getAcctstat() {
        return acctstat;
    }

    /** 账户形态 */
    public void setAcctstat(String acctstat) {
        this.acctstat = acctstat == null ? "" : acctstat.trim();
    }

    /** 笔数 */
    @Column(name = "Ordnu")
    public Integer getOrdnu() {
        return ordnu;
    }

    /** 笔数 */
    public void setOrdnu(Integer ordnu) {
        if(ordnu == null){
            this.ordnu = 0;
        }else{
            this.ordnu = ordnu;
        }
    }

    /** 开户机构 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 开户机构 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
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

    /** 内部帐号 */
    @Column(name = "Inacctno")
    public String getInacctno() {
        return inacctno;
    }

    /** 内部帐号 */
    public void setInacctno(String inacctno) {
        this.inacctno = inacctno == null ? "" : inacctno.trim();
    }

    /** 科目控制字 */
    @Column(name = "Subctrlcode")
    public String getSubctrlcode() {
        return subctrlcode;
    }

    /** 科目控制字 */
    public void setSubctrlcode(String subctrlcode) {
        this.subctrlcode = subctrlcode == null ? "" : subctrlcode.trim();
    }

    /** 帐别 */
    @Column(name = "Acctype")
    public String getAcctype() {
        return acctype;
    }

    /** 帐别 */
    public void setAcctype(String acctype) {
        this.acctype = acctype == null ? "" : acctype.trim();
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

    /** 借贷标识 */
    @Column(name = "Cdflag")
    public String getCdflag() {
        return cdflag;
    }

    /** 借贷标识 */
    public void setCdflag(String cdflag) {
        this.cdflag = cdflag == null ? "" : cdflag.trim();
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

    /** 余额 */
    @Column(name = "Bal")
    public Double getBal() {
        return bal;
    }

    /** 余额 */
    public void setBal(Double bal) {
        if(bal == null){
            this.bal = 0.00;
        }else{
            this.bal = bal;
        }
    }

    /** 天数 */
    @Column(name = "Days")
    public Integer getDays() {
        return days;
    }

    /** 天数 */
    public void setDays(Integer days) {
        if(days == null){
            this.days = 0;
        }else{
            this.days = days;
        }
    }

    /** 本金积数 */
    @Column(name = "Accum")
    public Double getAccum() {
        return accum;
    }

    /** 本金积数 */
    public void setAccum(Double accum) {
        if(accum == null){
            this.accum = 0.00;
        }else{
            this.accum = accum;
        }
    }

    /** 凭证类型 */
    @Column(name = "Voukind")
    public String getVoukind() {
        return voukind;
    }

    /** 凭证类型 */
    public void setVoukind(String voukind) {
        this.voukind = voukind == null ? "" : voukind.trim();
    }

    /** 冠字号码 */
    @Column(name = "Precharcode")
    public String getPrecharcode() {
        return precharcode;
    }

    /** 冠字号码 */
    public void setPrecharcode(String precharcode) {
        this.precharcode = precharcode == null ? "" : precharcode.trim();
    }

    /** 凭证号码 */
    @Column(name = "Voucherno")
    public String getVoucherno() {
        return voucherno;
    }

    /** 凭证号码 */
    public void setVoucherno(String voucherno) {
        this.voucherno = voucherno == null ? "" : voucherno.trim();
    }

    /** 摘要码 */
    @Column(name = "Memocode")
    public String getMemocode() {
        return memocode;
    }

    /** 摘要码 */
    public void setMemocode(String memocode) {
        this.memocode = memocode == null ? "" : memocode.trim();
    }

    /** 交易摘要 */
    @Column(name = "Memoname")
    public String getMemoname() {
        return memoname;
    }

    /** 交易摘要 */
    public void setMemoname(String memoname) {
        this.memoname = memoname == null ? "" : memoname.trim();
    }

    /** 对方帐号 */
    @Column(name = "Oppacctno")
    public String getOppacctno() {
        return oppacctno;
    }

    /** 对方帐号 */
    public void setOppacctno(String oppacctno) {
        this.oppacctno = oppacctno == null ? "" : oppacctno.trim();
    }

    /** 销账编号(存放银行资金流水号) */
    @Column(name = "Canacctno")
    public String getCanacctno() {
        return canacctno;
    }

    /** 销账编号(存放银行资金流水号) */
    public void setCanacctno(String canacctno) {
        this.canacctno = canacctno == null ? "" : canacctno.trim();
    }

    @Column(name = "Modifytime")
    public String getModifytime() {
        return modifytime;
    }

    public void setModifytime(String modifytime) {
        this.modifytime = modifytime == null ? "" : modifytime.trim();
    }

    /** 原交易流水 */
    @Column(name = "Serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }

    /** 原交易流水 */
    public void setSerseqno(Integer serseqno) {
        if(serseqno == null){
            this.serseqno = 0;
        }else{
            this.serseqno = serseqno;
        }
    }

    @Column(name = "Txnseq")
    public Integer getTxnseq() {
        return txnseq;
    }

    public void setTxnseq(Integer txnseq) {
        if(txnseq == null){
            this.txnseq = 0;
        }else{
            this.txnseq = txnseq;
        }
    }

    /** 原交易码 */
    @Column(name = "Trancode")
    public String getTrancode() {
        return trancode;
    }

    /** 原交易码 */
    public void setTrancode(String trancode) {
        this.trancode = trancode == null ? "" : trancode.trim();
    }

    /** 子交易码 */
    @Column(name = "Subtrancode")
    public String getSubtrancode() {
        return subtrancode;
    }

    /** 子交易码 */
    public void setSubtrancode(String subtrancode) {
        this.subtrancode = subtrancode == null ? "" : subtrancode.trim();
    }

    /** 交易机构 */
    @Column(name = "Tranbrc")
    public String getTranbrc() {
        return tranbrc;
    }

    /** 交易机构 */
    public void setTranbrc(String tranbrc) {
        this.tranbrc = tranbrc == null ? "" : tranbrc.trim();
    }

    /** 交易柜员 */
    @Column(name = "Tellercode")
    public String getTellercode() {
        return tellercode;
    }

    /** 交易柜员 */
    public void setTellercode(String tellercode) {
        this.tellercode = tellercode == null ? "" : tellercode.trim();
    }

    /** 授权柜员 */
    @Column(name = "Authcode")
    public String getAuthcode() {
        return authcode;
    }

    /** 授权柜员 */
    public void setAuthcode(String authcode) {
        this.authcode = authcode == null ? "" : authcode.trim();
    }

    /** 复核柜员 */
    @Column(name = "Checkcode")
    public String getCheckcode() {
        return checkcode;
    }

    /** 复核柜员 */
    public void setCheckcode(String checkcode) {
        this.checkcode = checkcode == null ? "" : checkcode.trim();
    }

    /** 页次 */
    @Column(name = "Pgnm")
    public Integer getPgnm() {
        return pgnm;
    }

    /** 页次 */
    public void setPgnm(Integer pgnm) {
        if(pgnm == null){
            this.pgnm = 0;
        }else{
            this.pgnm = pgnm;
        }
    }

    /** 打印标志 */
    @Column(name = "Printflag")
    public String getPrintflag() {
        return printflag;
    }

    /** 打印标志 */
    public void setPrintflag(String printflag) {
        this.printflag = printflag == null ? "" : printflag.trim();
    }

    /** 交易来源 */
    @Column(name = "Transource")
    public String getTransource() {
        return transource;
    }

    /** 交易来源 */
    public void setTransource(String transource) {
        this.transource = transource == null ? "" : transource.trim();
    }

    /** 记录类型 */
    @Column(name = "Recordtype")
    public String getRecordtype() {
        return recordtype;
    }

    /** 记录类型 */
    public void setRecordtype(String recordtype) {
        this.recordtype = recordtype == null ? "" : recordtype.trim();
    }

    /** 流水帐标识 */
    @Column(name = "Flag1")
    public String getFlag1() {
        return flag1;
    }

    /** 流水帐标识 */
    public void setFlag1(String flag1) {
        this.flag1 = flag1 == null ? "" : flag1.trim();
    }

    /** 被冲销标识 */
    @Column(name = "Flag2")
    public String getFlag2() {
        return flag2;
    }

    /** 被冲销标识 */
    public void setFlag2(String flag2) {
        this.flag2 = flag2 == null ? "" : flag2.trim();
    }

    /** 备用标志 */
    @Column(name = "Flag3")
    public String getFlag3() {
        return flag3;
    }

    /** 备用标志 */
    public void setFlag3(String flag3) {
        this.flag3 = flag3 == null ? "" : flag3.trim();
    }

    /** 笔序号 */
    @Column(name = "Seq")
    public Integer getSeq() {
        return seq;
    }

    /** 笔序号 */
    public void setSeq(Integer seq) {
        if(seq == null){
            this.seq = 0;
        }else{
            this.seq = seq;
        }
    }

    /** 顺序号 */
    @Column(name = "Seq1")
    public Integer getSeq1() {
        return seq1;
    }

    /** 顺序号 */
    public void setSeq1(Integer seq1) {
        if(seq1 == null){
            this.seq1 = 0;
        }else{
            this.seq1 = seq1;
        }
    }
}
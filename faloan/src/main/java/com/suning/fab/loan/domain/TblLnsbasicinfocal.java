package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSBASICINFOCAL")
public class TblLnsbasicinfocal {
    private String openbrc = "";

    private String acctno = "";

    private String acctype = "";

    private String ccy = "";

    private String custtype = "";

    private String customid = "";

    private String name = "";

    private String prdcode = "";

    private String flag1 = "";

    private Integer prinmindays = 0;

    private Integer intmindays = 0;

    private String iscalint = "";

    private String repayway = "";

    private String loantype1 = "";

    private String loanstat = "";

    private String profitbrc = "";

    private String acctno1 = "";

    private String firstprindate = "";

    private String firstintdate = "";

    private String firstintflag = "";

    private String prinperformula = "";

    private String prinformula = "";

    private String lastprindate = "";

    private String intperformula = "";

    private String intformula = "";

    private String lastintdate = "";

    private Integer printerms = 0;

    private Integer intterms = 0;

    private Integer curprinterm = 0;

    private Integer curintterm = 0;

    private Integer oldprinterm = 0;

    private Integer oldintterm = 0;

    private Double contractbal = 0.00;

    private String opendate = "";

    private String beginintdate = "";

    private Integer gracedays = 0;

    private String contduedate = "";

    private String intenddate = "";

    private Integer extnums = 0;

    private String intrateplan = "";

    private Double bjtxbl = 0.00;

    private String txacctno = "";

    private String diacct = "";

    private String investee = "";

    private String investmode = "";

    private String channeltype = "";

    private String fundchannel = "";

    private String outserialno = "";

    private String contrasctno = "";

    private Double contractamt = 0.00;

    private Double openfee = 0.00;

    private Double deductionamt = 0.00;

    private String provisionflag = "";

    private String memo = "";

    private Double normalrate = 0.00;

    private Double overduerate = 0.00;

    private Double overduerate1 = 0.00;

    private String modifydate = "";

    private String modifytime = "";

    private String trandate = "";

    private String reserv1 = "";

    private Double reservamt1 = 0.00;

    public TblLnsbasicinfocal() {
         
    }

    /** 开户机构代码 */
    @Column(name = "Openbrc")
    public String getOpenbrc() {
        return openbrc;
    }

    /** 开户机构代码 */
    public void setOpenbrc(String openbrc) {
        this.openbrc = openbrc == null ? "" : openbrc.trim();
    }

    /** 帐号 */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /** 帐号 */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
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

    /** 客户类型 */
    @Column(name = "Custtype")
    public String getCusttype() {
        return custtype;
    }

    /** 客户类型 */
    public void setCusttype(String custtype) {
        this.custtype = custtype == null ? "" : custtype.trim();
    }

    /** 商户号/客户号 */
    @Column(name = "Customid")
    public String getCustomid() {
        return customid;
    }

    /** 商户号/客户号 */
    public void setCustomid(String customid) {
        this.customid = customid == null ? "" : customid.trim();
    }

    /** 户名 */
    @Column(name = "Name")
    public String getName() {
        return name;
    }

    /** 户名 */
    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    /** 产品代码 */
    @Column(name = "Prdcode")
    public String getPrdcode() {
        return prdcode;
    }

    /** 产品代码 */
    public void setPrdcode(String prdcode) {
        this.prdcode = prdcode == null ? "" : prdcode.trim();
    }

    /** 静态标志1 */
    @Column(name = "Flag1")
    public String getFlag1() {
        return flag1;
    }

    /** 静态标志1 */
    public void setFlag1(String flag1) {
        this.flag1 = flag1 == null ? "" : flag1.trim();
    }

    /** 本金一周期最小天数 */
    @Column(name = "Prinmindays")
    public Integer getPrinmindays() {
        return prinmindays;
    }

    /** 本金一周期最小天数 */
    public void setPrinmindays(Integer prinmindays) {
        if(prinmindays == null){
            this.prinmindays = 0;
        }else{
            this.prinmindays = prinmindays;
        }
    }

    /** 利息一周期最小天数 */
    @Column(name = "Intmindays")
    public Integer getIntmindays() {
        return intmindays;
    }

    /** 利息一周期最小天数 */
    public void setIntmindays(Integer intmindays) {
        if(intmindays == null){
            this.intmindays = 0;
        }else{
            this.intmindays = intmindays;
        }
    }

    /** 计息标志 */
    @Column(name = "Iscalint")
    public String getIscalint() {
        return iscalint;
    }

    /** 计息标志 */
    public void setIscalint(String iscalint) {
        this.iscalint = iscalint == null ? "" : iscalint.trim();
    }

    /** 计息方式 */
    @Column(name = "Repayway")
    public String getRepayway() {
        return repayway;
    }

    /** 计息方式 */
    public void setRepayway(String repayway) {
        this.repayway = repayway == null ? "" : repayway.trim();
    }

    /** 贷款种类1 */
    @Column(name = "Loantype1")
    public String getLoantype1() {
        return loantype1;
    }

    /** 贷款种类1 */
    public void setLoantype1(String loantype1) {
        this.loantype1 = loantype1 == null ? "" : loantype1.trim();
    }

    /** 贷款状态 */
    @Column(name = "Loanstat")
    public String getLoanstat() {
        return loanstat;
    }

    /** 贷款状态 */
    public void setLoanstat(String loanstat) {
        this.loanstat = loanstat == null ? "" : loanstat.trim();
    }

    /** 核算机构代码 */
    @Column(name = "Profitbrc")
    public String getProfitbrc() {
        return profitbrc;
    }

    /** 核算机构代码 */
    public void setProfitbrc(String profitbrc) {
        this.profitbrc = profitbrc == null ? "" : profitbrc.trim();
    }

    /** 借据号 */
    @Column(name = "Acctno1")
    public String getAcctno1() {
        return acctno1;
    }

    /** 借据号 */
    public void setAcctno1(String acctno1) {
        this.acctno1 = acctno1 == null ? "" : acctno1.trim();
    }

    /** 首次还本日期 */
    @Column(name = "Firstprindate")
    public String getFirstprindate() {
        return firstprindate;
    }

    /** 首次还本日期 */
    public void setFirstprindate(String firstprindate) {
        this.firstprindate = firstprindate == null ? "" : firstprindate.trim();
    }

    /** 首次付息日期 */
    @Column(name = "Firstintdate")
    public String getFirstintdate() {
        return firstintdate;
    }

    /** 首次付息日期 */
    public void setFirstintdate(String firstintdate) {
        this.firstintdate = firstintdate == null ? "" : firstintdate.trim();
    }

    /** 首次结息日标志 */
    @Column(name = "Firstintflag")
    public String getFirstintflag() {
        return firstintflag;
    }

    /** 首次结息日标志 */
    public void setFirstintflag(String firstintflag) {
        this.firstintflag = firstintflag == null ? "" : firstintflag.trim();
    }

    /** 还本周期公式 */
    @Column(name = "Prinperformula")
    public String getPrinperformula() {
        return prinperformula;
    }

    /** 还本周期公式 */
    public void setPrinperformula(String prinperformula) {
        this.prinperformula = prinperformula == null ? "" : prinperformula.trim();
    }

    /** 本金计算公式 */
    @Column(name = "Prinformula")
    public String getPrinformula() {
        return prinformula;
    }

    /** 本金计算公式 */
    public void setPrinformula(String prinformula) {
        this.prinformula = prinformula == null ? "" : prinformula.trim();
    }

    /** 上次结本日 */
    @Column(name = "Lastprindate")
    public String getLastprindate() {
        return lastprindate;
    }

    /** 上次结本日 */
    public void setLastprindate(String lastprindate) {
        this.lastprindate = lastprindate == null ? "" : lastprindate.trim();
    }

    /** 还息周期公式 */
    @Column(name = "Intperformula")
    public String getIntperformula() {
        return intperformula;
    }

    /** 还息周期公式 */
    public void setIntperformula(String intperformula) {
        this.intperformula = intperformula == null ? "" : intperformula.trim();
    }

    /** 利息计算公式 */
    @Column(name = "Intformula")
    public String getIntformula() {
        return intformula;
    }

    /** 利息计算公式 */
    public void setIntformula(String intformula) {
        this.intformula = intformula == null ? "" : intformula.trim();
    }

    /** 上次结息日 */
    @Column(name = "Lastintdate")
    public String getLastintdate() {
        return lastintdate;
    }

    /** 上次结息日 */
    public void setLastintdate(String lastintdate) {
        this.lastintdate = lastintdate == null ? "" : lastintdate.trim();
    }

    /** 还本总期数 */
    @Column(name = "Printerms")
    public Integer getPrinterms() {
        return printerms;
    }

    /** 还本总期数 */
    public void setPrinterms(Integer printerms) {
        if(printerms == null){
            this.printerms = 0;
        }else{
            this.printerms = printerms;
        }
    }

    /** 还息总期数 */
    @Column(name = "Intterms")
    public Integer getIntterms() {
        return intterms;
    }

    /** 还息总期数 */
    public void setIntterms(Integer intterms) {
        if(intterms == null){
            this.intterms = 0;
        }else{
            this.intterms = intterms;
        }
    }

    /** 还本当期期数 */
    @Column(name = "Curprinterm")
    public Integer getCurprinterm() {
        return curprinterm;
    }

    /** 还本当期期数 */
    public void setCurprinterm(Integer curprinterm) {
        if(curprinterm == null){
            this.curprinterm = 0;
        }else{
            this.curprinterm = curprinterm;
        }
    }

    /** 还息当期期数 */
    @Column(name = "Curintterm")
    public Integer getCurintterm() {
        return curintterm;
    }

    /** 还息当期期数 */
    public void setCurintterm(Integer curintterm) {
        if(curintterm == null){
            this.curintterm = 0;
        }else{
            this.curintterm = curintterm;
        }
    }

    /** 本金已还期数 */
    @Column(name = "Oldprinterm")
    public Integer getOldprinterm() {
        return oldprinterm;
    }

    /** 本金已还期数 */
    public void setOldprinterm(Integer oldprinterm) {
        if(oldprinterm == null){
            this.oldprinterm = 0;
        }else{
            this.oldprinterm = oldprinterm;
        }
    }

    /** 利息已还期数 */
    @Column(name = "Oldintterm")
    public Integer getOldintterm() {
        return oldintterm;
    }

    /** 利息已还期数 */
    public void setOldintterm(Integer oldintterm) {
        if(oldintterm == null){
            this.oldintterm = 0;
        }else{
            this.oldintterm = oldintterm;
        }
    }

    /** 合同余额 */
    @Column(name = "Contractbal")
    public Double getContractbal() {
        return contractbal;
    }

    /** 合同余额 */
    public void setContractbal(Double contractbal) {
        if(contractbal == null){
            this.contractbal = 0.00;
        }else{
            this.contractbal = contractbal;
        }
    }

    /** 开户日期 */
    @Column(name = "Opendate")
    public String getOpendate() {
        return opendate;
    }

    /** 开户日期 */
    public void setOpendate(String opendate) {
        this.opendate = opendate == null ? "" : opendate.trim();
    }

    /** 起息日期 */
    @Column(name = "Beginintdate")
    public String getBeginintdate() {
        return beginintdate;
    }

    /** 起息日期 */
    public void setBeginintdate(String beginintdate) {
        this.beginintdate = beginintdate == null ? "" : beginintdate.trim();
    }

    /** 贷款宽限期 */
    @Column(name = "Gracedays")
    public Integer getGracedays() {
        return gracedays;
    }

    /** 贷款宽限期 */
    public void setGracedays(Integer gracedays) {
        if(gracedays == null){
            this.gracedays = 0;
        }else{
            this.gracedays = gracedays;
        }
    }

    /** 合同到期日 */
    @Column(name = "Contduedate")
    public String getContduedate() {
        return contduedate;
    }

    /** 合同到期日 */
    public void setContduedate(String contduedate) {
        this.contduedate = contduedate == null ? "" : contduedate.trim();
    }

    /** 利息结束日期 */
    @Column(name = "Intenddate")
    public String getIntenddate() {
        return intenddate;
    }

    /** 利息结束日期 */
    public void setIntenddate(String intenddate) {
        this.intenddate = intenddate == null ? "" : intenddate.trim();
    }

    /** 展期次数 */
    @Column(name = "Extnums")
    public Integer getExtnums() {
        return extnums;
    }

    /** 展期次数 */
    public void setExtnums(Integer extnums) {
        if(extnums == null){
            this.extnums = 0;
        }else{
            this.extnums = extnums;
        }
    }

    /** 利率计划 */
    @Column(name = "Intrateplan")
    public String getIntrateplan() {
        return intrateplan;
    }

    /** 利率计划 */
    public void setIntrateplan(String intrateplan) {
        this.intrateplan = intrateplan == null ? "" : intrateplan.trim();
    }

    /** 本金贴息比例 */
    @Column(name = "Bjtxbl")
    public Double getBjtxbl() {
        return bjtxbl;
    }

    /** 本金贴息比例 */
    public void setBjtxbl(Double bjtxbl) {
        if(bjtxbl == null){
            this.bjtxbl = 0.00;
        }else{
            this.bjtxbl = bjtxbl;
        }
    }

    /** 贴息帐号 */
    @Column(name = "Txacctno")
    public String getTxacctno() {
        return txacctno;
    }

    /** 贴息帐号 */
    public void setTxacctno(String txacctno) {
        this.txacctno = txacctno == null ? "" : txacctno.trim();
    }

    /** 扣款帐号 */
    @Column(name = "Diacct")
    public String getDiacct() {
        return diacct;
    }

    /** 扣款帐号 */
    public void setDiacct(String diacct) {
        this.diacct = diacct == null ? "" : diacct.trim();
    }

    /** 门店商户号 */
    @Column(name = "Investee")
    public String getInvestee() {
        return investee;
    }

    /** 门店商户号 */
    public void setInvestee(String investee) {
        this.investee = investee == null ? "" : investee.trim();
    }

    /** 投放模式 */
    @Column(name = "Investmode")
    public String getInvestmode() {
        return investmode;
    }

    /** 投放模式 */
    public void setInvestmode(String investmode) {
        this.investmode = investmode == null ? "" : investmode.trim();
    }

    /** 资金来源 */
    @Column(name = "Channeltype")
    public String getChanneltype() {
        return channeltype;
    }

    /** 资金来源 */
    public void setChanneltype(String channeltype) {
        this.channeltype = channeltype == null ? "" : channeltype.trim();
    }

    /** 资金来源帐号 */
    @Column(name = "Fundchannel")
    public String getFundchannel() {
        return fundchannel;
    }

    /** 资金来源帐号 */
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

    /** 合同编号 */
    @Column(name = "Contrasctno")
    public String getContrasctno() {
        return contrasctno;
    }

    /** 合同编号 */
    public void setContrasctno(String contrasctno) {
        this.contrasctno = contrasctno == null ? "" : contrasctno.trim();
    }

    /** 合同金额 */
    @Column(name = "Contractamt")
    public Double getContractamt() {
        return contractamt;
    }

    /** 合同金额 */
    public void setContractamt(Double contractamt) {
        if(contractamt == null){
            this.contractamt = 0.00;
        }else{
            this.contractamt = contractamt;
        }
    }

    /** 开户手续费 */
    @Column(name = "Openfee")
    public Double getOpenfee() {
        return openfee;
    }

    /** 开户手续费 */
    public void setOpenfee(Double openfee) {
        if(openfee == null){
            this.openfee = 0.00;
        }else{
            this.openfee = openfee;
        }
    }

    /** 扣息金额 */
    @Column(name = "Deductionamt")
    public Double getDeductionamt() {
        return deductionamt;
    }

    /** 扣息金额 */
    public void setDeductionamt(Double deductionamt) {
        if(deductionamt == null){
            this.deductionamt = 0.00;
        }else{
            this.deductionamt = deductionamt;
        }
    }

    /** 计提结束标志 */
    @Column(name = "Provisionflag")
    public String getProvisionflag() {
        return provisionflag;
    }

    /** 计提结束标志 */
    public void setProvisionflag(String provisionflag) {
        this.provisionflag = provisionflag == null ? "" : provisionflag.trim();
    }

    /** 备注 */
    @Column(name = "Memo")
    public String getMemo() {
        return memo;
    }

    /** 备注 */
    public void setMemo(String memo) {
        this.memo = memo == null ? "" : memo.trim();
    }

    /** 正常利率 */
    @Column(name = "Normalrate")
    public Double getNormalrate() {
        return normalrate;
    }

    /** 正常利率 */
    public void setNormalrate(Double normalrate) {
        if(normalrate == null){
            this.normalrate = 0.00;
        }else{
            this.normalrate = normalrate;
        }
    }

    /** 逾期利率 */
    @Column(name = "Overduerate")
    public Double getOverduerate() {
        return overduerate;
    }

    /** 逾期利率 */
    public void setOverduerate(Double overduerate) {
        if(overduerate == null){
            this.overduerate = 0.00;
        }else{
            this.overduerate = overduerate;
        }
    }

    /** 复利利率 */
    @Column(name = "Overduerate1")
    public Double getOverduerate1() {
        return overduerate1;
    }

    /** 复利利率 */
    public void setOverduerate1(Double overduerate1) {
        if(overduerate1 == null){
            this.overduerate1 = 0.00;
        }else{
            this.overduerate1 = overduerate1;
        }
    }

    /** 交易日期 */
    @Column(name = "Modifydate")
    public String getModifydate() {
        return modifydate;
    }

    /** 交易日期 */
    public void setModifydate(String modifydate) {
        this.modifydate = modifydate == null ? "" : modifydate.trim();
    }

    /** 交易时间 */
    @Column(name = "Modifytime")
    public String getModifytime() {
        return modifytime;
    }

    /** 交易时间 */
    public void setModifytime(String modifytime) {
        this.modifytime = modifytime == null ? "" : modifytime.trim();
    }

    /** 开户日工作日期 */
    @Column(name = "Trandate")
    public String getTrandate() {
        return trandate;
    }

    /** 开户日工作日期 */
    public void setTrandate(String trandate) {
        this.trandate = trandate == null ? "" : trandate.trim();
    }

    /** 备用字段1 */
    @Column(name = "Reserv1")
    public String getReserv1() {
        return reserv1;
    }

    /** 备用字段1 */
    public void setReserv1(String reserv1) {
        this.reserv1 = reserv1 == null ? "" : reserv1.trim();
    }

    /** 备用金额1 */
    @Column(name = "Reservamt1")
    public Double getReservamt1() {
        return reservamt1;
    }

    /** 备用金额1 */
    public void setReservamt1(Double reservamt1) {
        if(reservamt1 == null){
            this.reservamt1 = 0.00;
        }else{
            this.reservamt1 = reservamt1;
        }
    }
}
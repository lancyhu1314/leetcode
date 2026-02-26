package com.suning.fab.loan.domain;

import javax.persistence.Entity;
import javax.persistence.Column;
@Entity(name = "LNSNONSTDPLAN")
public class TblLnsnonstdplan {
    private String acctno = "";

    private String openbrc = "";

    private Integer repayterm = 0;

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

    private String termbdate = "";

    private String termedate = "";

    private String intbdate = "";

    private String intedate = "";

    private Double termprin = 0.00;

    private Double termint = 0.00;

    private Double balance = 0.00;
    
    private Integer days = 0;

    private String calcintflag1 = "";

    private String calcintflag2 = "";

    private String ratetype = "";

    private String termretdate = "";

    private String repayprinflag = "";

    private String repayintflag = "";

    public TblLnsnonstdplan() {
         
    }

    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    @Column(name = "Openbrc")
    public String getOpenbrc() {
        return openbrc;
    }

    public void setOpenbrc(String openbrc) {
        this.openbrc = openbrc == null ? "" : openbrc.trim();
    }

    @Column(name = "Repayterm")
    public Integer getRepayterm() {
        return repayterm;
    }

    public void setRepayterm(Integer repayterm) {
        if(repayterm == null){
            this.repayterm = 0;
        }else{
            this.repayterm = repayterm;
        }
    }

    @Column(name = "Acctype")
    public String getAcctype() {
        return acctype;
    }

    public void setAcctype(String acctype) {
        this.acctype = acctype == null ? "" : acctype.trim();
    }

    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    @Column(name = "Custtype")
    public String getCusttype() {
        return custtype;
    }

    public void setCusttype(String custtype) {
        this.custtype = custtype == null ? "" : custtype.trim();
    }

    @Column(name = "Customid")
    public String getCustomid() {
        return customid;
    }

    public void setCustomid(String customid) {
        this.customid = customid == null ? "" : customid.trim();
    }

    @Column(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    @Column(name = "Prdcode")
    public String getPrdcode() {
        return prdcode;
    }

    public void setPrdcode(String prdcode) {
        this.prdcode = prdcode == null ? "" : prdcode.trim();
    }

    @Column(name = "Flag1")
    public String getFlag1() {
        return flag1;
    }

    public void setFlag1(String flag1) {
        this.flag1 = flag1 == null ? "" : flag1.trim();
    }

    @Column(name = "Prinmindays")
    public Integer getPrinmindays() {
        return prinmindays;
    }

    public void setPrinmindays(Integer prinmindays) {
        if(prinmindays == null){
            this.prinmindays = 0;
        }else{
            this.prinmindays = prinmindays;
        }
    }

    @Column(name = "Intmindays")
    public Integer getIntmindays() {
        return intmindays;
    }

    public void setIntmindays(Integer intmindays) {
        if(intmindays == null){
            this.intmindays = 0;
        }else{
            this.intmindays = intmindays;
        }
    }

    @Column(name = "Iscalint")
    public String getIscalint() {
        return iscalint;
    }

    public void setIscalint(String iscalint) {
        this.iscalint = iscalint == null ? "" : iscalint.trim();
    }

    @Column(name = "Repayway")
    public String getRepayway() {
        return repayway;
    }

    public void setRepayway(String repayway) {
        this.repayway = repayway == null ? "" : repayway.trim();
    }

    @Column(name = "Loantype1")
    public String getLoantype1() {
        return loantype1;
    }

    public void setLoantype1(String loantype1) {
        this.loantype1 = loantype1 == null ? "" : loantype1.trim();
    }

    @Column(name = "Loanstat")
    public String getLoanstat() {
        return loanstat;
    }

    public void setLoanstat(String loanstat) {
        this.loanstat = loanstat == null ? "" : loanstat.trim();
    }

    @Column(name = "Profitbrc")
    public String getProfitbrc() {
        return profitbrc;
    }

    public void setProfitbrc(String profitbrc) {
        this.profitbrc = profitbrc == null ? "" : profitbrc.trim();
    }

    @Column(name = "Acctno1")
    public String getAcctno1() {
        return acctno1;
    }

    public void setAcctno1(String acctno1) {
        this.acctno1 = acctno1 == null ? "" : acctno1.trim();
    }

    @Column(name = "Firstprindate")
    public String getFirstprindate() {
        return firstprindate;
    }

    public void setFirstprindate(String firstprindate) {
        this.firstprindate = firstprindate == null ? "" : firstprindate.trim();
    }

    @Column(name = "Firstintdate")
    public String getFirstintdate() {
        return firstintdate;
    }

    public void setFirstintdate(String firstintdate) {
        this.firstintdate = firstintdate == null ? "" : firstintdate.trim();
    }

    @Column(name = "Firstintflag")
    public String getFirstintflag() {
        return firstintflag;
    }

    public void setFirstintflag(String firstintflag) {
        this.firstintflag = firstintflag == null ? "" : firstintflag.trim();
    }

    @Column(name = "Prinperformula")
    public String getPrinperformula() {
        return prinperformula;
    }

    public void setPrinperformula(String prinperformula) {
        this.prinperformula = prinperformula == null ? "" : prinperformula.trim();
    }

    @Column(name = "Prinformula")
    public String getPrinformula() {
        return prinformula;
    }

    public void setPrinformula(String prinformula) {
        this.prinformula = prinformula == null ? "" : prinformula.trim();
    }

    @Column(name = "Lastprindate")
    public String getLastprindate() {
        return lastprindate;
    }

    public void setLastprindate(String lastprindate) {
        this.lastprindate = lastprindate == null ? "" : lastprindate.trim();
    }

    @Column(name = "Intperformula")
    public String getIntperformula() {
        return intperformula;
    }

    public void setIntperformula(String intperformula) {
        this.intperformula = intperformula == null ? "" : intperformula.trim();
    }

    @Column(name = "Intformula")
    public String getIntformula() {
        return intformula;
    }

    public void setIntformula(String intformula) {
        this.intformula = intformula == null ? "" : intformula.trim();
    }

    @Column(name = "Lastintdate")
    public String getLastintdate() {
        return lastintdate;
    }

    public void setLastintdate(String lastintdate) {
        this.lastintdate = lastintdate == null ? "" : lastintdate.trim();
    }

    @Column(name = "Printerms")
    public Integer getPrinterms() {
        return printerms;
    }

    public void setPrinterms(Integer printerms) {
        if(printerms == null){
            this.printerms = 0;
        }else{
            this.printerms = printerms;
        }
    }

    @Column(name = "Intterms")
    public Integer getIntterms() {
        return intterms;
    }

    public void setIntterms(Integer intterms) {
        if(intterms == null){
            this.intterms = 0;
        }else{
            this.intterms = intterms;
        }
    }

    @Column(name = "Curprinterm")
    public Integer getCurprinterm() {
        return curprinterm;
    }

    public void setCurprinterm(Integer curprinterm) {
        if(curprinterm == null){
            this.curprinterm = 0;
        }else{
            this.curprinterm = curprinterm;
        }
    }

    @Column(name = "Curintterm")
    public Integer getCurintterm() {
        return curintterm;
    }

    public void setCurintterm(Integer curintterm) {
        if(curintterm == null){
            this.curintterm = 0;
        }else{
            this.curintterm = curintterm;
        }
    }

    @Column(name = "Oldprinterm")
    public Integer getOldprinterm() {
        return oldprinterm;
    }

    public void setOldprinterm(Integer oldprinterm) {
        if(oldprinterm == null){
            this.oldprinterm = 0;
        }else{
            this.oldprinterm = oldprinterm;
        }
    }

    @Column(name = "Oldintterm")
    public Integer getOldintterm() {
        return oldintterm;
    }

    public void setOldintterm(Integer oldintterm) {
        if(oldintterm == null){
            this.oldintterm = 0;
        }else{
            this.oldintterm = oldintterm;
        }
    }

    @Column(name = "Contractbal")
    public Double getContractbal() {
        return contractbal;
    }

    public void setContractbal(Double contractbal) {
        if(contractbal == null){
            this.contractbal = 0.00;
        }else{
            this.contractbal = contractbal;
        }
    }

    @Column(name = "Opendate")
    public String getOpendate() {
        return opendate;
    }

    public void setOpendate(String opendate) {
        this.opendate = opendate == null ? "" : opendate.trim();
    }

    @Column(name = "Beginintdate")
    public String getBeginintdate() {
        return beginintdate;
    }

    public void setBeginintdate(String beginintdate) {
        this.beginintdate = beginintdate == null ? "" : beginintdate.trim();
    }

    @Column(name = "Gracedays")
    public Integer getGracedays() {
        return gracedays;
    }

    public void setGracedays(Integer gracedays) {
        if(gracedays == null){
            this.gracedays = 0;
        }else{
            this.gracedays = gracedays;
        }
    }

    @Column(name = "Contduedate")
    public String getContduedate() {
        return contduedate;
    }

    public void setContduedate(String contduedate) {
        this.contduedate = contduedate == null ? "" : contduedate.trim();
    }

    @Column(name = "Intenddate")
    public String getIntenddate() {
        return intenddate;
    }

    public void setIntenddate(String intenddate) {
        this.intenddate = intenddate == null ? "" : intenddate.trim();
    }

    @Column(name = "Extnums")
    public Integer getExtnums() {
        return extnums;
    }

    public void setExtnums(Integer extnums) {
        if(extnums == null){
            this.extnums = 0;
        }else{
            this.extnums = extnums;
        }
    }

    @Column(name = "Intrateplan")
    public String getIntrateplan() {
        return intrateplan;
    }

    public void setIntrateplan(String intrateplan) {
        this.intrateplan = intrateplan == null ? "" : intrateplan.trim();
    }

    @Column(name = "Bjtxbl")
    public Double getBjtxbl() {
        return bjtxbl;
    }

    public void setBjtxbl(Double bjtxbl) {
        if(bjtxbl == null){
            this.bjtxbl = 0.00;
        }else{
            this.bjtxbl = bjtxbl;
        }
    }

    @Column(name = "Txacctno")
    public String getTxacctno() {
        return txacctno;
    }

    public void setTxacctno(String txacctno) {
        this.txacctno = txacctno == null ? "" : txacctno.trim();
    }

    @Column(name = "Diacct")
    public String getDiacct() {
        return diacct;
    }

    public void setDiacct(String diacct) {
        this.diacct = diacct == null ? "" : diacct.trim();
    }

    @Column(name = "Investee")
    public String getInvestee() {
        return investee;
    }

    public void setInvestee(String investee) {
        this.investee = investee == null ? "" : investee.trim();
    }

    @Column(name = "Investmode")
    public String getInvestmode() {
        return investmode;
    }

    public void setInvestmode(String investmode) {
        this.investmode = investmode == null ? "" : investmode.trim();
    }

    @Column(name = "Channeltype")
    public String getChanneltype() {
        return channeltype;
    }

    public void setChanneltype(String channeltype) {
        this.channeltype = channeltype == null ? "" : channeltype.trim();
    }

    @Column(name = "Fundchannel")
    public String getFundchannel() {
        return fundchannel;
    }

    public void setFundchannel(String fundchannel) {
        this.fundchannel = fundchannel == null ? "" : fundchannel.trim();
    }

    @Column(name = "Outserialno")
    public String getOutserialno() {
        return outserialno;
    }

    public void setOutserialno(String outserialno) {
        this.outserialno = outserialno == null ? "" : outserialno.trim();
    }

    @Column(name = "Contrasctno")
    public String getContrasctno() {
        return contrasctno;
    }

    public void setContrasctno(String contrasctno) {
        this.contrasctno = contrasctno == null ? "" : contrasctno.trim();
    }

    @Column(name = "Contractamt")
    public Double getContractamt() {
        return contractamt;
    }

    public void setContractamt(Double contractamt) {
        if(contractamt == null){
            this.contractamt = 0.00;
        }else{
            this.contractamt = contractamt;
        }
    }

    @Column(name = "Openfee")
    public Double getOpenfee() {
        return openfee;
    }

    public void setOpenfee(Double openfee) {
        if(openfee == null){
            this.openfee = 0.00;
        }else{
            this.openfee = openfee;
        }
    }

    @Column(name = "Deductionamt")
    public Double getDeductionamt() {
        return deductionamt;
    }

    public void setDeductionamt(Double deductionamt) {
        if(deductionamt == null){
            this.deductionamt = 0.00;
        }else{
            this.deductionamt = deductionamt;
        }
    }

    @Column(name = "Provisionflag")
    public String getProvisionflag() {
        return provisionflag;
    }

    public void setProvisionflag(String provisionflag) {
        this.provisionflag = provisionflag == null ? "" : provisionflag.trim();
    }

    @Column(name = "Memo")
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo == null ? "" : memo.trim();
    }

    @Column(name = "Normalrate")
    public Double getNormalrate() {
        return normalrate;
    }

    public void setNormalrate(Double normalrate) {
        if(normalrate == null){
            this.normalrate = 0.00;
        }else{
            this.normalrate = normalrate;
        }
    }

    @Column(name = "Overduerate")
    public Double getOverduerate() {
        return overduerate;
    }

    public void setOverduerate(Double overduerate) {
        if(overduerate == null){
            this.overduerate = 0.00;
        }else{
            this.overduerate = overduerate;
        }
    }

    @Column(name = "Overduerate1")
    public Double getOverduerate1() {
        return overduerate1;
    }

    public void setOverduerate1(Double overduerate1) {
        if(overduerate1 == null){
            this.overduerate1 = 0.00;
        }else{
            this.overduerate1 = overduerate1;
        }
    }

    @Column(name = "Modifydate")
    public String getModifydate() {
        return modifydate;
    }

    public void setModifydate(String modifydate) {
        this.modifydate = modifydate == null ? "" : modifydate.trim();
    }

    @Column(name = "Modifytime")
    public String getModifytime() {
        return modifytime;
    }

    public void setModifytime(String modifytime) {
        this.modifytime = modifytime == null ? "" : modifytime.trim();
    }

    @Column(name = "Trandate")
    public String getTrandate() {
        return trandate;
    }

    public void setTrandate(String trandate) {
        this.trandate = trandate == null ? "" : trandate.trim();
    }

    @Column(name = "Reserv1")
    public String getReserv1() {
        return reserv1;
    }

    public void setReserv1(String reserv1) {
        this.reserv1 = reserv1 == null ? "" : reserv1.trim();
    }

    @Column(name = "Reservamt1")
    public Double getReservamt1() {
        return reservamt1;
    }

    public void setReservamt1(Double reservamt1) {
        if(reservamt1 == null){
            this.reservamt1 = 0.00;
        }else{
            this.reservamt1 = reservamt1;
        }
    }

    @Column(name = "Termbdate")
    public String getTermbdate() {
        return termbdate;
    }

    public void setTermbdate(String termbdate) {
        this.termbdate = termbdate == null ? "" : termbdate.trim();
    }

    @Column(name = "Termedate")
    public String getTermedate() {
        return termedate;
    }

    public void setTermedate(String termedate) {
        this.termedate = termedate == null ? "" : termedate.trim();
    }

    @Column(name = "Intbdate")
    public String getIntbdate() {
        return intbdate;
    }

    public void setIntbdate(String intbdate) {
        this.intbdate = intbdate == null ? "" : intbdate.trim();
    }

    @Column(name = "Intedate")
    public String getIntedate() {
        return intedate;
    }

    public void setIntedate(String intedate) {
        this.intedate = intedate == null ? "" : intedate.trim();
    }

    @Column(name = "Termprin")
    public Double getTermprin() {
        return termprin;
    }

    public void setTermprin(Double termprin) {
        if(termprin == null){
            this.termprin = 0.00;
        }else{
            this.termprin = termprin;
        }
    }

    @Column(name = "Termint")
    public Double getTermint() {
        return termint;
    }

    public void setTermint(Double termint) {
        if(termint == null){
            this.termint = 0.00;
        }else{
            this.termint = termint;
        }
    }

    @Column(name = "Balance")
    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        if(balance == null){
            this.balance = 0.00;
        }else{
            this.balance = balance;
        }
    }

    @Column(name = "Days")
    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        if(days == null){
            this.days = 0;
        }else{
            this.days = days;
        }
    }

    @Column(name = "Calcintflag1")
    public String getCalcintflag1() {
        return calcintflag1;
    }

    public void setCalcintflag1(String calcintflag1) {
        this.calcintflag1 = calcintflag1 == null ? "" : calcintflag1.trim();
    }

    @Column(name = "Calcintflag2")
    public String getCalcintflag2() {
        return calcintflag2;
    }

    public void setCalcintflag2(String calcintflag2) {
        this.calcintflag2 = calcintflag2 == null ? "" : calcintflag2.trim();
    }

    @Column(name = "Ratetype")
    public String getRatetype() {
        return ratetype;
    }

    public void setRatetype(String ratetype) {
        this.ratetype = ratetype == null ? "" : ratetype.trim();
    }

    @Column(name = "Termretdate")
    public String getTermretdate() {
        return termretdate;
    }

    public void setTermretdate(String termretdate) {
        this.termretdate = termretdate == null ? "" : termretdate.trim();
    }

    @Column(name = "Repayprinflag")
    public String getRepayprinflag() {
        return repayprinflag;
    }

    public void setRepayprinflag(String repayprinflag) {
        this.repayprinflag = repayprinflag == null ? "" : repayprinflag.trim();
    }

    @Column(name = "Repayintflag")
    public String getRepayintflag() {
        return repayintflag;
    }

    public void setRepayintflag(String repayintflag) {
        this.repayintflag = repayintflag == null ? "" : repayintflag.trim();
    }
}
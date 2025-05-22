package com.suning.fab.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSACCOUNTDYNINFO")
public class TblLnsaccountdyninfo {
    private String acctno = "";

    private String acctstat = "";

    private String profitbrc = "";

    private String status = "";

    private String prdcode = "";

    private String inacctno = "";

    private String openacctbrc = "";

    private String acctype = "";

    private String subctrlcode = "";

    private String ccy = "";

    private String flag = "";

    private String flag1 = "";

    private String repayorder = "";

    private String ratetype = "";

    private String bald = "";

    private Double intrate = 0.00;

    private Double lastbal = 0.00;

    private Double currbal = 0.00;

    private Double ctrlbal = 0.00;

    private Double accum = 0.00;

    private String begindate = "";

    private String prechgratedate = "";

    private String precalcintdate = "";

    private String nextcalcdate = "";

    private String pretrandate = "";

    private String dac = "";

    private String begincacudate = "";

    private String endcacudate = "";

    private Integer precacudays = 0;

    private Integer cacudays = 0;

    private Double yearstartbal = 0.00;

    private String flagres11 = "";

    private String dateres01 = "";

    private String dateres02 = "";

    private Double amountres1 = 0.00;

    private Double amountres2 = 0.00;

    private Double accumres = 0.00;

    private String acctnores01 = "";

    private String acctnores02 = "";

    private Integer ordnu = 0;

    private String modifydate = "";

    private String modifytime = "";

    public TblLnsaccountdyninfo() {
         
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

    /** 核算机构 */
    @Column(name = "Profitbrc")
    public String getProfitbrc() {
        return profitbrc;
    }

    /** 核算机构 */
    public void setProfitbrc(String profitbrc) {
        this.profitbrc = profitbrc == null ? "" : profitbrc.trim();
    }

    /** 状态 */
    @Column(name = "Status")
    public String getStatus() {
        return status;
    }

    /** 状态 */
    public void setStatus(String status) {
        this.status = status == null ? "" : status.trim();
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

    /** 贷款内部账号 */
    @Column(name = "Inacctno")
    public String getInacctno() {
        return inacctno;
    }

    /** 贷款内部账号 */
    public void setInacctno(String inacctno) {
        this.inacctno = inacctno == null ? "" : inacctno.trim();
    }

    /** 开户机构 */
    @Column(name = "Openacctbrc")
    public String getOpenacctbrc() {
        return openacctbrc;
    }

    /** 开户机构 */
    public void setOpenacctbrc(String openacctbrc) {
        this.openacctbrc = openacctbrc == null ? "" : openacctbrc.trim();
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

    /** 客户代码 */
    @Column(name = "Subctrlcode")
    public String getSubctrlcode() {
        return subctrlcode;
    }

    /** 客户代码 */
    public void setSubctrlcode(String subctrlcode) {
        this.subctrlcode = subctrlcode == null ? "" : subctrlcode.trim();
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

    /** 静态控制标志 */
    @Column(name = "Flag")
    public String getFlag() {
        return flag;
    }

    /** 静态控制标志 */
    public void setFlag(String flag) {
        this.flag = flag == null ? "" : flag.trim();
    }

    /** 动态控制标志 */
    @Column(name = "Flag1")
    public String getFlag1() {
        return flag1;
    }

    /** 动态控制标志 */
    public void setFlag1(String flag1) {
        this.flag1 = flag1 == null ? "" : flag1.trim();
    }

    /** 贷款还款顺序 */
    @Column(name = "Repayorder")
    public String getRepayorder() {
        return repayorder;
    }

    /** 贷款还款顺序 */
    public void setRepayorder(String repayorder) {
        this.repayorder = repayorder == null ? "" : repayorder.trim();
    }

    /** 利率类型 */
    @Column(name = "Ratetype")
    public String getRatetype() {
        return ratetype;
    }

    /** 利率类型 */
    public void setRatetype(String ratetype) {
        this.ratetype = ratetype == null ? "" : ratetype.trim();
    }

    /** 余额方向 */
    @Column(name = "Bald")
    public String getBald() {
        return bald;
    }

    /** 余额方向 */
    public void setBald(String bald) {
        this.bald = bald == null ? "" : bald.trim();
    }

    /** 执行利率 */
    @Column(name = "Intrate")
    public Double getIntrate() {
        return intrate;
    }

    /** 执行利率 */
    public void setIntrate(Double intrate) {
        if(intrate == null){
            this.intrate = 0.00;
        }else{
            this.intrate = intrate;
        }
    }

    /** 昨日余额 */
    @Column(name = "Lastbal")
    public Double getLastbal() {
        return lastbal;
    }

    /** 昨日余额 */
    public void setLastbal(Double lastbal) {
        if(lastbal == null){
            this.lastbal = 0.00;
        }else{
            this.lastbal = lastbal;
        }
    }

    /** 当前余额 */
    @Column(name = "Currbal")
    public Double getCurrbal() {
        return currbal;
    }

    /** 当前余额 */
    public void setCurrbal(Double currbal) {
        if(currbal == null){
            this.currbal = 0.00;
        }else{
            this.currbal = currbal;
        }
    }

    /** 控制余额 */
    @Column(name = "Ctrlbal")
    public Double getCtrlbal() {
        return ctrlbal;
    }

    /** 控制余额 */
    public void setCtrlbal(Double ctrlbal) {
        if(ctrlbal == null){
            this.ctrlbal = 0.00;
        }else{
            this.ctrlbal = ctrlbal;
        }
    }

    /** 积数 */
    @Column(name = "Accum")
    public Double getAccum() {
        return accum;
    }

    /** 积数 */
    public void setAccum(Double accum) {
        if(accum == null){
            this.accum = 0.00;
        }else{
            this.accum = accum;
        }
    }

    /** 起息日期 */
    @Column(name = "Begindate")
    public String getBegindate() {
        return begindate;
    }

    /** 起息日期 */
    public void setBegindate(String begindate) {
        this.begindate = begindate == null ? "" : begindate.trim();
    }

    /** 上次利率调整日 */
    @Column(name = "Prechgratedate")
    public String getPrechgratedate() {
        return prechgratedate;
    }

    /** 上次利率调整日 */
    public void setPrechgratedate(String prechgratedate) {
        this.prechgratedate = prechgratedate == null ? "" : prechgratedate.trim();
    }

    /** 上次结息日 */
    @Column(name = "Precalcintdate")
    public String getPrecalcintdate() {
        return precalcintdate;
    }

    /** 上次结息日 */
    public void setPrecalcintdate(String precalcintdate) {
        this.precalcintdate = precalcintdate == null ? "" : precalcintdate.trim();
    }

    /** 下次结息日 */
    @Column(name = "Nextcalcdate")
    public String getNextcalcdate() {
        return nextcalcdate;
    }

    /** 下次结息日 */
    public void setNextcalcdate(String nextcalcdate) {
        this.nextcalcdate = nextcalcdate == null ? "" : nextcalcdate.trim();
    }

    /** 上笔交易日 */
    @Column(name = "Pretrandate")
    public String getPretrandate() {
        return pretrandate;
    }

    /** 上笔交易日 */
    public void setPretrandate(String pretrandate) {
        this.pretrandate = pretrandate == null ? "" : pretrandate.trim();
    }

    /** 校验位 */
    @Column(name = "Dac")
    public String getDac() {
        return dac;
    }

    /** 校验位 */
    public void setDac(String dac) {
        this.dac = dac == null ? "" : dac.trim();
    }

    /** 累计积数起日 */
    @Column(name = "Begincacudate")
    public String getBegincacudate() {
        return begincacudate;
    }

    /** 累计积数起日 */
    public void setBegincacudate(String begincacudate) {
        this.begincacudate = begincacudate == null ? "" : begincacudate.trim();
    }

    /** 累计积数止日 */
    @Column(name = "Endcacudate")
    public String getEndcacudate() {
        return endcacudate;
    }

    /** 累计积数止日 */
    public void setEndcacudate(String endcacudate) {
        this.endcacudate = endcacudate == null ? "" : endcacudate.trim();
    }

    /** 应计积数天数 */
    @Column(name = "Precacudays")
    public Integer getPrecacudays() {
        return precacudays;
    }

    /** 应计积数天数 */
    public void setPrecacudays(Integer precacudays) {
        if(precacudays == null){
            this.precacudays = 0;
        }else{
            this.precacudays = precacudays;
        }
    }

    /** 已计积数天数 */
    @Column(name = "Cacudays")
    public Integer getCacudays() {
        return cacudays;
    }

    /** 已计积数天数 */
    public void setCacudays(Integer cacudays) {
        if(cacudays == null){
            this.cacudays = 0;
        }else{
            this.cacudays = cacudays;
        }
    }

    /** 年初余额 */
    @Column(name = "Yearstartbal")
    public Double getYearstartbal() {
        return yearstartbal;
    }

    /** 年初余额 */
    public void setYearstartbal(Double yearstartbal) {
        if(yearstartbal == null){
            this.yearstartbal = 0.00;
        }else{
            this.yearstartbal = yearstartbal;
        }
    }

    /** 标志备用 */
    @Column(name = "Flagres11")
    public String getFlagres11() {
        return flagres11;
    }

    /** 标志备用 */
    public void setFlagres11(String flagres11) {
        this.flagres11 = flagres11 == null ? "" : flagres11.trim();
    }

    /** 日期备用 */
    @Column(name = "Dateres01")
    public String getDateres01() {
        return dateres01;
    }

    /** 日期备用 */
    public void setDateres01(String dateres01) {
        this.dateres01 = dateres01 == null ? "" : dateres01.trim();
    }

    /** 日期备用 */
    @Column(name = "Dateres02")
    public String getDateres02() {
        return dateres02;
    }

    /** 日期备用 */
    public void setDateres02(String dateres02) {
        this.dateres02 = dateres02 == null ? "" : dateres02.trim();
    }

    /** 金额备用 */
    @Column(name = "Amountres1")
    public Double getAmountres1() {
        return amountres1;
    }

    /** 金额备用 */
    public void setAmountres1(Double amountres1) {
        if(amountres1 == null){
            this.amountres1 = 0.00;
        }else{
            this.amountres1 = amountres1;
        }
    }

    /** 金额备用 */
    @Column(name = "Amountres2")
    public Double getAmountres2() {
        return amountres2;
    }

    /** 金额备用 */
    public void setAmountres2(Double amountres2) {
        if(amountres2 == null){
            this.amountres2 = 0.00;
        }else{
            this.amountres2 = amountres2;
        }
    }

    /** 积数备用 */
    @Column(name = "Accumres")
    public Double getAccumres() {
        return accumres;
    }

    /** 积数备用 */
    public void setAccumres(Double accumres) {
        if(accumres == null){
            this.accumres = 0.00;
        }else{
            this.accumres = accumres;
        }
    }

    /** 账号备用 */
    @Column(name = "Acctnores01")
    public String getAcctnores01() {
        return acctnores01;
    }

    /** 账号备用 */
    public void setAcctnores01(String acctnores01) {
        this.acctnores01 = acctnores01 == null ? "" : acctnores01.trim();
    }

    /** 账号备用 */
    @Column(name = "Acctnores02")
    public String getAcctnores02() {
        return acctnores02;
    }

    /** 账号备用 */
    public void setAcctnores02(String acctnores02) {
        this.acctnores02 = acctnores02 == null ? "" : acctnores02.trim();
    }

    /** 明细帐序号 */
    @Column(name = "Ordnu")
    public Integer getOrdnu() {
        return ordnu;
    }

    /** 明细帐序号 */
    public void setOrdnu(Integer ordnu) {
        if(ordnu == null){
            this.ordnu = 0;
        }else{
            this.ordnu = ordnu;
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
}
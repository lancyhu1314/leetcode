package com.suning.fab.loan.domain;

import javax.persistence.Entity;
import javax.persistence.Column;
@Entity(name = "SPSSYSCTRLINFO")
public class TblSpssysctrlinfo {
    private String branchno = "";

    private String sysstatus = "";

    private String trandate = "";

    private String predate = "";

    private String nextdate = "";

    private String accdate = "";

    private String yearsumdate = "";

    private String posccy = "";

    private String potccy = "";

    private Integer testinfolevel = 0;

    private String flag = "";

    public TblSpssysctrlinfo() {
         
    }

    @Column(name = "Branchno")
    public String getBranchno() {
        return branchno;
    }

    public void setBranchno(String branchno) {
        this.branchno = branchno == null ? "" : branchno.trim();
    }

    @Column(name = "Sysstatus")
    public String getSysstatus() {
        return sysstatus;
    }

    public void setSysstatus(String sysstatus) {
        this.sysstatus = sysstatus == null ? "" : sysstatus.trim();
    }

    @Column(name = "Trandate")
    public String getTrandate() {
        return trandate;
    }

    public void setTrandate(String trandate) {
        this.trandate = trandate == null ? "" : trandate.trim();
    }

    @Column(name = "Predate")
    public String getPredate() {
        return predate;
    }

    public void setPredate(String predate) {
        this.predate = predate == null ? "" : predate.trim();
    }

    @Column(name = "Nextdate")
    public String getNextdate() {
        return nextdate;
    }

    public void setNextdate(String nextdate) {
        this.nextdate = nextdate == null ? "" : nextdate.trim();
    }

    @Column(name = "Accdate")
    public String getAccdate() {
        return accdate;
    }

    public void setAccdate(String accdate) {
        this.accdate = accdate == null ? "" : accdate.trim();
    }

    @Column(name = "Yearsumdate")
    public String getYearsumdate() {
        return yearsumdate;
    }

    public void setYearsumdate(String yearsumdate) {
        this.yearsumdate = yearsumdate == null ? "" : yearsumdate.trim();
    }

    @Column(name = "Posccy")
    public String getPosccy() {
        return posccy;
    }

    public void setPosccy(String posccy) {
        this.posccy = posccy == null ? "" : posccy.trim();
    }

    @Column(name = "Potccy")
    public String getPotccy() {
        return potccy;
    }

    public void setPotccy(String potccy) {
        this.potccy = potccy == null ? "" : potccy.trim();
    }

    @Column(name = "Testinfolevel")
    public Integer getTestinfolevel() {
        return testinfolevel;
    }

    public void setTestinfolevel(Integer testinfolevel) {
        if(testinfolevel == null){
            this.testinfolevel = 0;
        }else{
            this.testinfolevel = testinfolevel;
        }
    }

    @Column(name = "Flag")
    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag == null ? "" : flag.trim();
    }
}
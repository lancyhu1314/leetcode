package com.suning.fab.loan.domain;

import java.sql.Date;

import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSBASICINFODYN")
public class TblLnsbasicinfodyn {
    private String acctno = "";

    private String openbrc = "";

    private Double sumprin = 0.00;

    private Double sumint = 0.00;

    private Double sumdint = 0.00;

    private Double sumcint = 0.00;

    private Double sumgint = 0.00;

    private String tunneldata = "";

    private String modifytime = "";
    
    private Double sumlbcdint = 0.00;
    
    private Date trandate = new Date(0);

    public TblLnsbasicinfodyn() {
         
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

    @Column(name = "Sumprin")
    public Double getSumprin() {
        return sumprin;
    }

    public void setSumprin(Double sumprin) {
        if(sumprin == null){
            this.sumprin = 0.00;
        }else{
            this.sumprin = sumprin;
        }
    }

    @Column(name = "Sumint")
    public Double getSumint() {
        return sumint;
    }

    public void setSumint(Double sumint) {
        if(sumint == null){
            this.sumint = 0.00;
        }else{
            this.sumint = sumint;
        }
    }

    @Column(name = "Sumdint")
    public Double getSumdint() {
        return sumdint;
    }

    public void setSumdint(Double sumdint) {
        if(sumdint == null){
            this.sumdint = 0.00;
        }else{
            this.sumdint = sumdint;
        }
    }

    @Column(name = "Sumcint")
    public Double getSumcint() {
        return sumcint;
    }

    public void setSumcint(Double sumcint) {
        if(sumcint == null){
            this.sumcint = 0.00;
        }else{
            this.sumcint = sumcint;
        }
    }

    @Column(name = "Sumgint")
    public Double getSumgint() {
        return sumgint;
    }

    public void setSumgint(Double sumgint) {
        if(sumgint == null){
            this.sumgint = 0.00;
        }else{
            this.sumgint = sumgint;
        }
    }

    @Column(name = "Tunneldata")
    public String getTunneldata() {
        return tunneldata;
    }

    public void setTunneldata(String tunneldata) {
        this.tunneldata = tunneldata == null ? "" : tunneldata.trim();
    }

    @Column(name = "Modifytime")
    public String getModifytime() {
        return modifytime;
    }

    public void setModifytime(String modifytime) {
        this.modifytime = modifytime == null ? "" : modifytime.trim();
    }
    
    @Column(name = "Sumlbcdint")
    public Double getSumlbcdint() {
        return sumlbcdint;
    }

    public void setSumlbcdint(Double sumlbcdint) {
        if(sumlbcdint == null){
            this.sumlbcdint = 0.00;
        }else{
            this.sumlbcdint = sumlbcdint;
        }
    }

	public Date getTrandate() {
		return trandate;
	}

	public void setTrandate(Date trandate) {
		this.trandate = trandate;
	}
}
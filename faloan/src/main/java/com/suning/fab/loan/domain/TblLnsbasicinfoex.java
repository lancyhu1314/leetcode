package com.suning.fab.loan.domain;

import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSBASICINFOEX")
public class TblLnsbasicinfoex {
    private String acctno = "";

    private String openbrc = "";

    private String key = "";

    private String value1 = "";

    private Double value2 = 0.00;

    private Double value3 = 0.00;

    private String tunneldata = "";

    private String modifytime = "";

    public TblLnsbasicinfoex() {
         
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

    @Column(name = "Key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key == null ? "" : key.trim();
    }

    @Column(name = "Value1")
    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1 == null ? "" : value1.trim();
    }

    @Column(name = "Value2")
    public Double getValue2() {
        return value2;
    }

    public void setValue2(Double value2) {
        if(value2 == null){
            this.value2 = 0.00;
        }else{
            this.value2 = value2;
        }
    }

    @Column(name = "Value3")
    public Double getValue3() {
        return value3;
    }

    public void setValue3(Double value3) {
        if(value3 == null){
            this.value3 = 0.00;
        }else{
            this.value3 = value3;
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
}
package com.suning.fab.loan.domain;

import javax.persistence.Entity;
import javax.persistence.Column;

@Entity(name = "LNSASSISTDYNINFO")
public class TblLnsassistdyninfo {
    private String brc = "";

    private String feetype = "";

    private String customid = "";

    private String acctno = "";

    private String ccy = "";

    private String custtype = "";

    private Double lastbal = 0.00;

    private Double currbal = 0.00;

    private String pretrandate = "";

    private String status = "";

    private Integer ordnu = 0;

    private String reserv1 = "";

    private String reserv2 = "";

    private String reserv3 = "";

    private String reserv4 = "";

    private String reserv5 = "";

    private String reserv6 = "";

    private String tunneldata = "";

    private String createtime = "";

    private String modifytime = "";

    public TblLnsassistdyninfo() {
         
    }

    @Override
    public  TblLnsassistdyninfo clone(){
    	TblLnsassistdyninfo lnsassistdyninfo = new TblLnsassistdyninfo();
    	lnsassistdyninfo.setAcctno(acctno);
    	lnsassistdyninfo.setBrc(brc);
    	lnsassistdyninfo.setCustomid(customid);	
    	lnsassistdyninfo.setCcy(ccy);
    	lnsassistdyninfo.setFeetype(feetype);
    	lnsassistdyninfo.setCusttype(custtype);
    	lnsassistdyninfo.setLastbal(lastbal);
    	lnsassistdyninfo.setCurrbal(currbal);
    	lnsassistdyninfo.setPretrandate(pretrandate);
    	lnsassistdyninfo.setStatus(status);
    	lnsassistdyninfo.setOrdnu(ordnu);
    	lnsassistdyninfo.setReserv1(reserv1);
    	lnsassistdyninfo.setReserv2(reserv2);
    	lnsassistdyninfo.setReserv3(reserv3);
    	lnsassistdyninfo.setReserv4(reserv4);
    	lnsassistdyninfo.setReserv5(reserv5);
    	lnsassistdyninfo.setReserv6(reserv6);
    	lnsassistdyninfo.setTunneldata(tunneldata);
    	lnsassistdyninfo.setCreatetime(createtime);
    	lnsassistdyninfo.setModifytime(modifytime);
    	
        return lnsassistdyninfo;

    }
    
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    @Column(name = "Feetype")
    public String getFeetype() {
        return feetype;
    }

    public void setFeetype(String feetype) {
        this.feetype = feetype == null ? "" : feetype.trim();
    }

    @Column(name = "Customid")
    public String getCustomid() {
        return customid;
    }

    public void setCustomid(String customid) {
        this.customid = customid == null ? "" : customid.trim();
    }

    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
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

    @Column(name = "Lastbal")
    public Double getLastbal() {
        return lastbal;
    }

    public void setLastbal(Double lastbal) {
        if(lastbal == null){
            this.lastbal = 0.00;
        }else{
            this.lastbal = lastbal;
        }
    }

    @Column(name = "Currbal")
    public Double getCurrbal() {
        return currbal;
    }

    public void setCurrbal(Double currbal) {
        if(currbal == null){
            this.currbal = 0.00;
        }else{
            this.currbal = currbal;
        }
    }

    @Column(name = "Pretrandate")
    public String getPretrandate() {
        return pretrandate;
    }

    public void setPretrandate(String pretrandate) {
        this.pretrandate = pretrandate == null ? "" : pretrandate.trim();
    }

    @Column(name = "Status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "" : status.trim();
    }

    @Column(name = "Ordnu")
    public Integer getOrdnu() {
        return ordnu;
    }

    public void setOrdnu(Integer ordnu) {
        if(ordnu == null){
            this.ordnu = 0;
        }else{
            this.ordnu = ordnu;
        }
    }

    @Column(name = "Reserv1")
    public String getReserv1() {
        return reserv1;
    }

    public void setReserv1(String reserv1) {
        this.reserv1 = reserv1 == null ? "" : reserv1.trim();
    }

    @Column(name = "Reserv2")
    public String getReserv2() {
        return reserv2;
    }

    public void setReserv2(String reserv2) {
        this.reserv2 = reserv2 == null ? "" : reserv2.trim();
    }

    @Column(name = "Reserv3")
    public String getReserv3() {
        return reserv3;
    }

    public void setReserv3(String reserv3) {
        this.reserv3 = reserv3 == null ? "" : reserv3.trim();
    }

    @Column(name = "Reserv4")
    public String getReserv4() {
        return reserv4;
    }

    public void setReserv4(String reserv4) {
        this.reserv4 = reserv4 == null ? "" : reserv4.trim();
    }

    @Column(name = "Reserv5")
    public String getReserv5() {
        return reserv5;
    }

    public void setReserv5(String reserv5) {
        this.reserv5 = reserv5 == null ? "" : reserv5.trim();
    }

    @Column(name = "Reserv6")
    public String getReserv6() {
        return reserv6;
    }

    public void setReserv6(String reserv6) {
        this.reserv6 = reserv6 == null ? "" : reserv6.trim();
    }

    @Column(name = "Tunneldata")
    public String getTunneldata() {
        return tunneldata;
    }

    public void setTunneldata(String tunneldata) {
        this.tunneldata = tunneldata == null ? "" : tunneldata.trim();
    }

    @Column(name = "Createtime")
    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime == null ? "" : createtime.trim();
    }

    @Column(name = "Modifytime")
    public String getModifytime() {
        return modifytime;
    }

    public void setModifytime(String modifytime) {
        this.modifytime = modifytime == null ? "" : modifytime.trim();
    }
}
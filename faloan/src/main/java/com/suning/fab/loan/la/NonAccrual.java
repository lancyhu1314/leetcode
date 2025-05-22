package com.suning.fab.loan.la;

import com.suning.fab.tup4j.amount.FabAmount;

import java.io.Serializable;

public class NonAccrual implements Serializable {
    private static final long serialVersionUID = -2498737165603812849L;
    private Integer period;      //账本期数
    private String billType;      //账本类型
    private String bDate;         //账本开始日
    private String eDate;         //账本结束日
    private String intEDate;      //罚息计止日期
    private FabAmount billAmt;    //账本金额

    private FabAmount aAmt;    //全保留金额
    private FabAmount bAmt;    //仅留税金额
    private FabAmount cAmt;    //全冲销金额

    public NonAccrual() {
        billAmt = new FabAmount(0.00);
        aAmt = new FabAmount(0.00);
        bAmt = new FabAmount(0.00);
        cAmt = new FabAmount(0.00);
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getbDate() {
        return bDate;
    }

    public void setbDate(String bDate) {
        this.bDate = bDate;
    }

    public String geteDate() {
        return eDate;
    }

    public void seteDate(String eDate) {
        this.eDate = eDate;
    }

    public String getIntEDate() {
        return intEDate;
    }

    public void setIntEDate(String intEDate) {
        this.intEDate = intEDate;
    }

    public FabAmount getBillAmt() {
        return billAmt;
    }

    public void setBillAmt(FabAmount billAmt) {
        this.billAmt = billAmt;
    }

    public FabAmount getaAmt() {
        return aAmt;
    }

    public void setaAmt(FabAmount aAmt) {
        this.aAmt = aAmt;
    }

    public FabAmount getbAmt() {
        return bAmt;
    }

    public void setbAmt(FabAmount bAmt) {
        this.bAmt = bAmt;
    }

    public FabAmount getcAmt() {
        return cAmt;
    }

    public void setcAmt(FabAmount cAmt) {
        this.cAmt = cAmt;
    }
}


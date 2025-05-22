package com.suning.fab.loan.entity;

/**
 * 账单期数金额实体类
 */
public class BillPeriodAmt {
    public BillPeriodAmt(Double billAmt, int countDay) {
        this.billAmt = billAmt;
        this.countDay = countDay;
    }

    public String getBillProperty() {
        return billProperty;
    }

    public void setBillProperty(String billProperty) {
        this.billProperty = billProperty;
    }

    private String billProperty;

    public Double getBillAmt() {
        return billAmt;
    }

    public void setBillAmt(Double billAmt) {
        this.billAmt = billAmt;
    }

    public int getCountDay() {
        return countDay;
    }

    public void setCountDay(int countDay) {
        this.countDay = countDay;
    }

    private Double billAmt;//本期金额
    private int countDay;//本期天数
}

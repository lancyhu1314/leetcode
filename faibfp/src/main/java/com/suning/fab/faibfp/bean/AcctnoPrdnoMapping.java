package com.suning.fab.faibfp.bean;

import java.sql.Timestamp;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/3/26
 * @Version 1.0
 */
public class AcctnoPrdnoMapping {

    private String acctNo;

    private String productCode;

    private Timestamp createTime;

    private Timestamp updateTime;


    public AcctnoPrdnoMapping() {
    }

    public AcctnoPrdnoMapping(String acctNo, String productCode, Timestamp createTime, Timestamp updateTime) {
        this.acctNo = acctNo;
        this.productCode = productCode;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * @return the acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }

    /**
     * @param acctNo to set
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    /**
     * @return the productCode
     */
    public String getProductCode() {
        return productCode;
    }

    /**
     * @param productCode to set
     */
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    /**
     * @return the createTime
     */
    public Timestamp getCreateTime() {
        return createTime;
    }

    /**
     * @param createTime to set
     */
    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    /**
     * @return the updateTime
     */
    public Timestamp getUpdateTime() {
        return updateTime;
    }

    /**
     * @param updateTime to set
     */
    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}

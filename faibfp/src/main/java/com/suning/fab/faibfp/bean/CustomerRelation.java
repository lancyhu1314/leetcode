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
public class CustomerRelation {

    private String routeId;
    private String repayacctNo;
    private String customId;
    private Timestamp createTime;
    private Timestamp updateTime;

    public CustomerRelation() {
    }

    public CustomerRelation(String routeId, String repayacctNo, String customId, Timestamp createTime, Timestamp updateTime) {
        this.routeId = routeId;
        this.repayacctNo = repayacctNo;
        this.customId = customId;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * @return the routeId
     */
    public String getRouteId() {
        return routeId;
    }

    /**
     * @param routeId to set
     */
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    /**
     * @return the repayacctNo
     */
    public String getRepayacctNo() {
        return repayacctNo;
    }

    /**
     * @param repayacctNo to set
     */
    public void setRepayacctNo(String repayacctNo) {
        this.repayacctNo = repayacctNo;
    }

    /**
     * @return the customId
     */
    public String getCustomId() {
        return customId;
    }

    /**
     * @param customId to set
     */
    public void setCustomId(String customId) {
        this.customId = customId;
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

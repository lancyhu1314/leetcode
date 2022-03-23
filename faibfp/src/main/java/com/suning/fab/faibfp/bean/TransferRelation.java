package com.suning.fab.faibfp.bean;

import java.sql.Timestamp;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 20015808
 * @Date 2022/2/22
 * @Version 1.0
 */
public class TransferRelation {

    private String routeId;

    /**
     * 迁移状态 1-未迁移;2-老系统处理中;3-迁移中;4-迁移完成
     * 默认 1
     */
    private String status;

    private int counts;

    private Timestamp createTime;

    private Timestamp updateTime;


    public TransferRelation() {
    }

    public TransferRelation(String routeId, String status, int counts, Timestamp createTime, Timestamp updateTime) {
        this.routeId = routeId;
        this.status = status;
        this.counts = counts;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCounts() {
        return counts;
    }

    public void setCounts(int counts) {
        this.counts = counts;
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

/*
 * Copyright (C), 2002-2019, 苏宁易购电子商务有限公司
 * FileName: CommonCheckedField
 * Author:   17060915
 * Date:     2019/8/17 10:01
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名     修改时间     版本号        描述
 */
package com.suning.fab.model.common;

import java.io.Serializable;

/**
 * 〈一句话功能简述〉<br>
 * 公共检查字段
 *
 * @author 17060915
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class CommonCheckedField implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工号
     */
    private String employeeId;

    /**
     * 销售门店
     */
    private String salesStore;

    /**
     * 公司代码
     */
    private String brc;

    /**
     * 流量渠道
     */
    private String flowChannel;

    /**
     * 终端编码
     */
    private String terminalCode;

    /**
     * 唯一标号
     */
    private String bsNo;

    /**
     * 获取 the value of employeeId.
     *
     * @return the value of employeeId
     */
    public String getEmployeeId() {
        return employeeId;
    }

    /**
     * 设置 the employeeNum.
     *
     * @param employeeId employeeId
     */
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    /**
     * 获取 the value of salesStore.
     *
     * @return the value of salesStore
     */
    public String getSalesStore() {
        return salesStore;
    }

    /**
     * 设置 the salesStore.
     *
     * @param salesStore salesStore
     */
    public void setSalesStore(String salesStore) {
        this.salesStore = salesStore;
    }

    /**
     * 获取 the value of brc.
     *
     * @return the value of brc
     */
    public String getBrc() {
        return brc;
    }

    /**
     * 设置 the brc.
     *
     * @param brc brc
     */
    public void setBrc(String brc) {
        this.brc = brc;
    }

    /**
     * 获取 the value of flowChannel.
     *
     * @return the value of flowChannel
     */
    public String getFlowChannel() {
        return flowChannel;
    }

    /**
     * 设置 the flowChannel.
     *
     * @param flowChannel flowChannel
     */
    public void setFlowChannel(String flowChannel) {
        this.flowChannel = flowChannel;
    }

    /**
     * 获取 the value of terminalCode.
     *
     * @return the value of terminalCode
     */
    public String getTerminalCode() {
        return terminalCode;
    }

    /**
     * 设置 the terminalCode.
     *
     * @param terminalCode terminalCode
     */
    public void setTerminalCode(String terminalCode) {
        this.terminalCode = terminalCode;
    }

    /**
     * 获取 the value of bsNo.
     *
     * @return the value of bsNo
     */
    public String getBsNo() {
        return bsNo;
    }

    /**
     * 设置 the bsNo.
     *
     * @param bsNo bsNo
     */
    public void setBsNo(String bsNo) {
        this.bsNo = bsNo;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CommonCheckedField{");
        sb.append("employeeId='").append(employeeId).append('\'');
        sb.append(", salesStore='").append(salesStore).append('\'');
        sb.append(", brc='").append(brc).append('\'');
        sb.append(", flowChannel='").append(flowChannel).append('\'');
        sb.append(", terminalCode='").append(terminalCode).append('\'');
        sb.append(", bsNo='").append(bsNo).append('\'');
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * 公共字段校验方法
     * 各字段具体校验逻辑,应用系统自己实现
     *
     */
    public void fieldValidate() {
        
    }


}

/*
 * Copyright (C), 2002-2016, 苏宁易购电子商务有限公司
 * FileName: TaskConstant.java
 * Author:   15040640
 * Date:     2016年4月13日 下午8:15:25
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.faibfp.utils;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 * 批任务执行常量类
 *
 * @author 15040640
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class TaskConstant {
    /**
     * 任务编号
     */
    public static final String TASK_NO = "taskNo";
    /**
     * 服务名称 service name (spring bean name)
     */
    public static final String SVC_NAME = "svcName";
    /**
     * 业务处理日期
     */
    public static final String TRAN_DATE = "s_TranDate";
    /**
     * 业务处理参数
     */
    public static final String PARAMETER = "s_Parameter";
    /**
     * 批次号
     */
    public static final String BAT_ID = "bat_id";
    /**
     * 任务执行参数
     */
    public static final String ERR_CODE = "errCode";
    public static final String ERR_MSG = "errMsg";
    public static final String ERR_CODE_SUCCESS = "000000";
    public static final String ERR_MSG_SUCCESS = "处理成功";
    public static final String TASK_STAT = "taskStat";
    public static final String END_DATE = "endDate";
    public static final String END_TIME = "endTime";
    public static final String TASK_STAT_T = "T";//通信成功
    public static final String TASK_STAT_SUCC_MESS = "通信成功";//通信成功
    public static final String TASK_OPERATE = "operate";//操作类型
    public static final String TASK_NETSUCC = "netsucc";//成功操作类型
    public static final String TASK_EXNODE = "exNode";//操作节点
    public static final String TASK_EXPSN = "exPsn";//操作节点
    public static final String TASK_EXPSN_SYS = "sys";//操作节点
    /**
     * 任务失败状态-4
     */
    public static final String TASK_STAT_SUCCESS = "4";
    /**
     * 任务失败状态-6
     */
    public static final String TASK_STAT_FAIL = "6";
    /**
     * rmicode
     */
    public static final String RMI_CODE = "rmiCode";
    /**
     * 批处理系统反馈结果的rmicode(cmdcode=feedBack)
     */
    public static final String BATCH_FEED_BACK = "876001";

    /**
     * 批处理系统反馈结果的rmicode(cmdcode=feedBack)
     */
    public static final String FATASK_BATCH_FEED_BACK = "875001";

    /**
     * 批处理系统触发rmicode(cmdcode=launchBySsf)
     */
    public static final String BATCH_LAUNCH_BY_SSF = "876006";

    /**
     * 批处理系统触发rmicode(cmdcode=launchBySsf)
     */
    public static final String BATCH_NEWLAUNCH_BY_RSF = "875002";

    /**
     * 批处理系统调用类型
     */
    public static final String CALLTYPE = "calltype";

    /**
     * 批处理系统同步调用
     */
    public static final String CALLSYN = "syn";

    /**
     * 批处理系统异步调用
     */
    public static final String CALLASY = "asy";

    private TaskConstant() {
    }
}

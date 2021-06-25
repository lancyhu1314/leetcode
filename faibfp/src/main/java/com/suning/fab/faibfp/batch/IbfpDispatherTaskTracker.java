/*
 * Copyright (C), 2002-2016, 苏宁易购电子商务有限公司
 * FileName: TaskTracker.java
 * Author:   15040640
 * Date:     2016年4月10日 下午6:29:35
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.faibfp.batch;

import com.suning.fab.faibfp.intf.DispatherTrackerIntf;
import com.suning.fab.faibfp.utils.OldServiceAgentHelper;
import com.suning.fab.faibfp.utils.TaskConstant;
import com.suning.fab.mulssyn.exception.FabRuntimeException;
import com.suning.fab.mulssyn.service.MulssynService;
import com.suning.fab.mulssyn.utils.GetPropUtil;
import com.suning.fab.mulssyn.utils.LoggerUtil;
import com.suning.rsf.consumer.ServiceAgent;
import com.suning.rsf.provider.annotation.Implement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.suning.fab.faibfp.utils.TaskConstant.*;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉 任务执行模块 接收批处理系统batchTracker模块发送的任务,处理完成后结果返回给batchTracker
 *
 * @author 15040640
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = DispatherTrackerIntf.class, implCode = "FAIBFP-pendingExecute")
public class IbfpDispatherTaskTracker extends ApplicationObjectSupport implements DispatherTrackerIntf {
    Logger logger = LoggerFactory.getLogger(IbfpDispatherTaskTracker.class);

    public void dispather(final Map<String, Object> requestMap) {
        logger.info("线程池收到报文......");
        new Thread() {
            public void run() {
                long start = System.currentTimeMillis();
                logger.info("线程池开始处理报文:{}", requestMap);
                dispatherThread(requestMap);
                long end = System.currentTimeMillis();
                logger.info("线程池结束处理报文:{},总耗时:{} ms", requestMap, end - start);

            }

        }.start();

    }

    /**
     * 功能描述: <br>
     * 〈功能详细描述〉 根据报文内容调度任务 1.根据svcName获取业务实例执行任务 2.任务执行状态根据有无异常判断 3.无异常成功返回taskStat=4 4.有异常失败返回taskStat=6
     *
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public void dispatherThread(Map<String, Object> requestMap) {
        logger.info("批任务执行-接收报文:{}", requestMap);
        // 返回任务执行结果
        Map<String, Object> resMap = new HashMap<String, Object>(requestMap);
        Map<String, Object> resMaptemp = new HashMap<String, Object>(requestMap);
        resMap.put(TaskConstant.RMI_CODE, TaskConstant.FATASK_BATCH_FEED_BACK);
        resMaptemp.put(TaskConstant.RMI_CODE, TaskConstant.FATASK_BATCH_FEED_BACK);
        resMaptemp.put(TaskConstant.TASK_STAT, TaskConstant.TASK_STAT_T);
        resMaptemp.put(TaskConstant.ERR_CODE, TaskConstant.ERR_CODE_SUCCESS);
        resMaptemp.put(TaskConstant.ERR_MSG, TaskConstant.TASK_STAT_SUCC_MESS);
        resMaptemp.put(TaskConstant.TASK_OPERATE, TaskConstant.TASK_NETSUCC);
        resMaptemp.put(TaskConstant.TASK_EXNODE, getLocalIP());
        resMaptemp.put(TaskConstant.TASK_EXPSN, TaskConstant.TASK_EXPSN_SYS);
        try {
            sendMessage(resMaptemp);
            logger.info("发送通信状态成功:{}", resMaptemp);
        } catch (Exception e1) {
            logger.error("发送通信状态异常:", e1);
        }
        boolean taskStat = false;
        try {
            volidCheck(requestMap);
            String svcName = requestMap.get(SVC_NAME).toString();
            Object service = getApplicationContext().getBean(svcName);
            if (isValid(service)) {
                if (service instanceof MulssynService) {
                    Map<String, Object> response = ((MulssynService) service).execute(requestMap);
                    logger.info("批任务执行-执行成功-返回结果:{}", response);
                    resMap.put(TASK_STAT, TASK_STAT_SUCCESS);
                    resMap.put(ERR_CODE, ERR_CODE_SUCCESS);
                    resMap.put(ERR_MSG, ERR_MSG_SUCCESS);
                    taskStat = true;
                } else {
                    logger.error("批任务执行-svcName:{}对应的服务对象类型:{}不属于TaskService", svcName,
                            service.getClass().getSimpleName());
                    resMap.put(ERR_CODE, "TTK001");
                    resMap.put(ERR_MSG, "任务类型不匹配svcName:" + svcName);
                }
            } else {
                logger.error("批任务执行-不存在svcName:{}的服务", svcName);
                resMap.put(ERR_CODE, "TTK002");
                resMap.put(ERR_MSG, "不存在的服务svcName:" + svcName);
            }
        } catch (FabRuntimeException e) {
            logger.error("批处理执行-内部异常-:{}", e);
            resMap.put(ERR_CODE, e.getErrCode());
            resMap.put(ERR_MSG, e.getMessage());
        } catch (Exception e) {
            logger.error("批任务执行-系统异常-{}", e);
            resMap.put(ERR_CODE, "TTK999");
            resMap.put(ERR_MSG, "系统异常");
        }
        if (taskStat) {
            resMap.put(TASK_STAT, TASK_STAT_SUCCESS);
        } else {
            resMap.put(TASK_STAT, TASK_STAT_FAIL);
        }
        Date date = new Date();
        String endDate = new SimpleDateFormat("yyyy-MM-dd").format(date);
        String endTime = new SimpleDateFormat("HH:mm:ss").format(date);
        resMap.put(END_DATE, endDate);
        resMap.put(END_TIME, endTime);
        resMap.put(TaskConstant.CALLTYPE, TaskConstant.CALLSYN);
        sendMessage(resMap);
    }

    /**
     * 功能描述: <br>
     * 〈功能详细描述〉 报文有效性检查:{svcName,instDate,parameter,taskNo...}
     *
     * @param requestMap
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    private void volidCheck(Map<String, Object> requestMap) {
        if (!isValid(requestMap)) {
            logger.info("批任务执行-报文检查-请求报文为空");
            throw new FabRuntimeException("TTR001", "批任务执行-报文检查-请求报文为空");
        }
        if (!isValid(requestMap.get(TRAN_DATE))) {
            logger.error("批任务执行-报文检查-缺少业务处理日期:s_TranDate");
            throw new FabRuntimeException("TTR002", "批任务执行-报文检查-缺少业务处理日期:s_TranDate");
        }
        requestMap.put(TRAN_DATE, requestMap.get(TRAN_DATE).toString().trim());
        if (!isValid(requestMap.get(SVC_NAME))) {
            logger.error("批任务执行-报文检查-缺少服务名称:svcName");
            throw new FabRuntimeException("TTR003", "批任务执行-报文检查-缺少服务名称:svcName");
        }
        requestMap.put(SVC_NAME, requestMap.get(SVC_NAME).toString().trim());
        if (!isValid(requestMap.get(TASK_NO))) {
            logger.error("批任务执行-报文检查-缺少任务编码:taskNo");
            throw new FabRuntimeException("TTR004", "批任务执行-报文检查-缺少任务编码:taskNo");
        }
        requestMap.put(TASK_NO, requestMap.get(TASK_NO).toString().trim());

    }


    /**
     * 功能描述: <br>
     * 〈功能详细描述〉 判断对象是否有效 null | " " | 空集合 | 空Map ==> false
     *
     * @param obj
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    @SuppressWarnings("rawtypes")
    private boolean isValid(Object obj) {
        if (obj == null) {
            return false;
        }
        if ("".equals(obj.toString().trim())) {
            return false;
        }
        if (obj instanceof Collection) {
            return !((Collection) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return !((Map) obj).isEmpty();
        }
        return true;
    }

    /**
     * 将获取当前系统IP
     *
     * @return String
     */
    public static String getLocalIP() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            return localhost.getHostAddress();
        } catch (Exception e) {
            LoggerUtil.error("getLocalIP Exception:{}", e);
            return "";
        }
    }

    /**
     * 向fatask发送批处理状态
     *
     * @param param
     */
    private void sendMessage(Map<String, Object> param) {
        ServiceAgent agent = OldServiceAgentHelper.getAgent((String) param.get(TaskConstant.RMI_CODE));
        String method = GetPropUtil.getProperty("rsf_elements_old." + param.get(TaskConstant.RMI_CODE)).split("@")[2];
        agent.invoke(method, new Object[]{param}, new Class[]{Map.class});
    }
}

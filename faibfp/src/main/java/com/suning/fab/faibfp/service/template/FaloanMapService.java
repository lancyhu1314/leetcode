/*
 * Copyright (C), 2002-2014, 苏宁易购电子商务有限公司
 * FileName: ApiRemoteMapService.java
 * Author:   10075910
 * Date:     2014年9月3日 下午5:04:10
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.faibfp.service.template;

import com.suning.rsf.provider.annotation.Contract;
import com.suning.rsf.provider.annotation.Method;

import java.util.Map;

/**
 * 〈苏宁RSF框架中远程业务根接口〉<br>
 * 〈服务提供方实现该根接口与SUNING-API系统进行远程通讯〉
 *
 * @author 10075910
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Contract(name = "suning.api.remote.map.service", internal = false, description = "贷款账务核心契约")
public interface FaloanMapService {

    /**
     * 功能描述: <br>
     * 〈使用Map结构进行数据传递〉
     *
     * @param reqMsg
     * @return
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    @Method(idempotent = false, timeout = 5000, retryTimes = 0, priority = "H", description = "execute service")
    Map<String, Object> execute(Map<String, Object> reqMsg);
}

/*
 * Copyright (C), 2002-2016, 苏宁易购电子商务有限公司
 * FileName: BatchClientIntf.java
 * Author:   16092019
 * Date:     2016年9月28日 下午4:18:39
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.faibfp.intf;

import com.suning.rsf.provider.annotation.Contract;
import com.suning.rsf.provider.annotation.Method;

import java.util.Map;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉
 * @author 19043955
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Contract(name="taskTracker",description="任务调度接口",warningPhones="18512580473")
public interface DispatherTrackerIntf {
	
	/**
	 * 
	 * 功能描述: <br>
	 * 1.根据svcName获取业务实例执行任务
	 *	 2.任务执行状态根据有无异常判断
	 *	 3.无异常成功返回taskStat=4
	 *	 4.有异常失败返回taskStat=6
	 * 〈功能详细描述〉
	 *	接收map格式的批任务请求,返回启动结果
	 * @param requestMap
	 * @return
	 * @see [相关类/方法](可选)
	 * @since [产品/模块版本](可选)
	 */
	@Method(description="获取业务实例执行任务")
	void dispather(Map<String, Object> requestMap);

}

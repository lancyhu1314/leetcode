/*
 * Copyright (C), 2002-2016, 苏宁易购电子商务有限公司
 * FileName: LogTraceNum.java
 * Author:   15040640
 * Date:     2016年4月15日 下午3:50:20
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.model.utils;

import java.util.UUID;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 *	logback日志配置中，生成唯一uuid作为线程号标识
 * @author 15040640
 * @since 2018-04-13
 */
public class LogbackMDCConverter extends ClassicConverter {
	/**
	 * 存储当前线程线程号
	 */
	private static final ThreadLocal<String> LOGTRACE = new ThreadLocal<>();
	
	@Override
	public String convert(ILoggingEvent event) {
		String traceNum = LOGTRACE.get();
		if (traceNum == null) {
			traceNum = getUUIDSequence();
			LOGTRACE.set(traceNum);
		}
		return traceNum;
	}
	 /**
	  * 获取唯一uuid，去掉了间隔号"-"；
	  * @return 返回唯一uuid；
	  */
    public static String getUUIDSequence(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}

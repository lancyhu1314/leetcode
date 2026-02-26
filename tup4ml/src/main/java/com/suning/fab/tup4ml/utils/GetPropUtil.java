/*
 * Copyright (C), 2002-2016, 苏宁易购电子商务有限公司
 * FileName: ErrorCodeSupper.java
 * Author:   15040640
 * Date:     2016年5月27日 下午7:05:35
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.suning.fab.model.constant.CommonConstant;
import com.suning.fab.tup4ml.exception.FabRuntimeException;

/**
 * 属性映射列表；
 * 要求属性文件使用UTF-8编码；
 * @author 15040640
 */
public abstract class GetPropUtil {
    private static Map<String, Properties> propertyMap = new ConcurrentHashMap<>();

	private GetPropUtil() {
		throw new IllegalStateException("GetPropUtil class");
	}

	public static String getProperty(String key) {
		String[] tmp = key.split("\\.");
		if (tmp.length != 2){
			throw new IllegalArgumentException(CommonConstant.ExceptionConstant.ARGUMENTILLEGAL);
		}
		Properties entry = propertyMap.get(tmp[0]);
		if (entry == null) {
			try{
				entry = new Properties();
				entry.load(Thread.currentThread().getContextClassLoader()
						.getResourceAsStream("conf/" + tmp[0] + ".properties"));
				propertyMap.put(tmp[0], entry);
			}catch(IOException ioExp){
				throw new FabRuntimeException(ioExp, "TUP102", tmp[0] + ".properties");
			}
		}
		String iso8859 = entry.getProperty(tmp[1]);
		if(null != iso8859) {
			try {
				return new String(iso8859.getBytes("ISO-8859-1"),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				LoggerUtil.error("parse encode exception:{}",e.getMessage());
				return null;
			}
		}else {
			return null;
		}
	}

	public static String getPropertyOrDefault(String key, String def) {
		String ret = getProperty(key);
		if (ret == null) {
			ret = def;
		}
		return ret;
	}
}

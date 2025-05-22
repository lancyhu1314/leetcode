package com.suning.fab.tup4ml.utils;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.tup4ml.elfin.AfterSpringInitProcessor;
import com.suning.rsf.consumer.ServiceAgent;

/**
 * RSF消费者代理工具类；<br/>
 * 目前只存TCC服务模板对应的RSF消费者代理类，
 * 需要可以到{@link AfterSpringInitProcessor}代码里添加其他RSF消费者代理类
 * @author 16030888
 *
 */
public abstract class RsfConsumerUtil {
	private static Map<String, ServiceAgent> rsfConsumerBeans = new HashMap<>();

	private RsfConsumerUtil() {
		throw new IllegalStateException("RsfConsumerUtil class");
	}
	
	/**
	 * 获取TCC服务模板对应的RSF消费者代理类；
	 * @param key 服务契约与实现编码的组合，格式：服务契约 + "." + 实现编码
	 * @return RSF消费者代理类
	 */
	public static ServiceAgent getServiceAgent(String key){
		return rsfConsumerBeans.get(key);
	}
	
	public static ServiceAgent putServiceAgent(String key, ServiceAgent srv){
		return rsfConsumerBeans.put(key, srv);
	}
}

package com.suning.fab.tup4ml.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * ThreadLocal工具类
 *
 * @author 17060915
 * @since 2018年3月13日下午5:54:02
 * @version 1.0
 */
public abstract class ThreadLocalUtil {

	private static final ThreadLocal<Map<String,Object>> objectLocal = new ThreadLocal<>();
	
	private ThreadLocalUtil() {
		throw new IllegalStateException("ThreadLocalUtil class");
	}
    
    /**
     * 
     * 功能描述: <br>
     * 将object放入ThreadLocal中
     *
     * @param key
     * @return
     * @since 1.0
     */
    public static void set(String key, Object object) {
        Map<String,Object> map = objectLocal.get();
        
        if(null == map){
            map = new HashMap<>();
            map.put(key, object);
            objectLocal.set(map);
        }else{
            map.put(key, object);
        }
    }
    
    /**
     * 
     * 功能描述: <br>
     * 从ThreadLocal中获取object
     *
     * @param key
     * @return
     * @since 1.0
     */
    public static Object get(String key) {
        
        Map<String,Object> map = objectLocal.get();
        if(null == map){
            map = new HashMap<>();
            objectLocal.set(map);
        }
        
        return map.get(key);
    }
    
    
    /**
     * 
     * 功能描述: <br>
     * 释放threadload中存储的引用
     *
     * @since 1.0
     */
    public static void clean() {
        objectLocal.set(null);
    }
    
}

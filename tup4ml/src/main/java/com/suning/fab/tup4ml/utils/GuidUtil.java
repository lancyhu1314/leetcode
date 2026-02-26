package com.suning.fab.tup4ml.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 子序号生成工具类
 *
 * @author 17060915
 * @since 2018年3月13日下午5:54:02
 * @version 1.0
 */
public abstract class GuidUtil {
	
	private static final ThreadLocal<Map<String,Integer>> indexMapLocal = new ThreadLocal<>();

	private GuidUtil() {
		throw new IllegalStateException("GuidUtil class");
	}
	
	/**
	 * 返回UUID
	 */
    public static String getUuidSequence(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
    
    /**
     * 
     * 功能描述: <br>
     * 根据key值从map中获取相对应的序号。返回旧值，并加1
     *
     * @param key
     * @return
     * @since 1.0
     */
    public static Integer incrSubSeq(String key) {
        
        Map<String,Integer> map = indexMapLocal.get();
        Integer integer = Integer.valueOf(1);
        if(null == map){
            map = new HashMap<>();
            indexMapLocal.set(map);
        }else{
            integer = map.get(key);
        }
        map.put(key, integer + 1);
        
        return integer;
    }
    
    /**
     * 
     * 功能描述: <br>
     * 根据key值从map中获取相对应的序号。返回当前值
     *
     * @param key
     * @return
     * @since 1.0
     */
    public static Integer getSubSeq(String key) {
        Map<String,Integer> map = indexMapLocal.get();
        Integer integer = Integer.valueOf(1);
        if(null == map){
            map = new HashMap<>();
            map.put(key, integer);
            indexMapLocal.set(map);
        }else{
            integer = map.get(key);
        }
        
        return integer;
    }
    
    /**
     * 
     * 功能描述: <br>
     * 释放threadload中存储的引用
     *
     * @since 1.0
     */
    public static void clean() {
        indexMapLocal.set(null);
    }
    
    
}

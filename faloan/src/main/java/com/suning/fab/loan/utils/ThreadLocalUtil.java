package com.suning.fab.loan.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储上下文使用的变量
 *
 */
public class ThreadLocalUtil {
    private static final ThreadLocal<Map<String, Object>> variableThreadLocal = new ThreadLocal<>();

    public static <T> T get(String key,T defaultValue) {
        Map map = variableThreadLocal.get();
        if(map==null){
            return defaultValue;
        }else{
            return map.get(key) == null ? defaultValue : (T)map.get(key);
        }
    }

    public static void set(String key, Object value) {
        Map map = variableThreadLocal.get();
        if(map==null){
            map=new HashMap();
            variableThreadLocal.set(map);
        }
        map.put(key, value);
    }

    public static void clean(){
        variableThreadLocal.remove();
    }
}

package com.suning.fab.loan.utils;
/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: AcctStatistics.java
 * Author:   17060915
 * Date:     2018年2月1日 下午7:45:49
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */


import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
//import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.framework.sedis.JedisAction;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import redis.clients.jedis.Jedis;

/**
 *
 * @author 17060915
 * @date 2018年2月1日下午7:45:49
 * @since 1.0
 */
public abstract class AcctStatistics {

    static ExecutorService executor = Executors.newFixedThreadPool(50);

    /**
     * key超时时间
     */
//    protected static final int SECONDS = 1800;

    /**
     * key值最大限制
     */
    private static  int limit = 1;
    
    /**
     * 线程执行最大时间
     */
    protected static final int EXEC_LIMIT = 10;

    /**
     * 
     * 功能描述: <br>
     * 1、封装一个独立的方法（对其它包或代码无依赖），入参是acct，出参是true,false 
     * 2、用acct+redis当前时间做key，进行incr，这个key的失效时间是3秒
     * 3、当某一个keyincr返回值大于limit，返回false，否则返回true 
     * 4、访问redis最长时间为EXEC_LIMIT ms，超过了不管什么原因抛出异常完事
     *
     * @param acct
     * @return
     * @since 1.0
     */
    public static boolean isNeedLimitRate(final String serialNo,final String key) {

		final String limitRepeatingRepaymentsIntervalTime = GlobalScmConfUtil.getProperty("LimitRepeatingRepaymentsIntervalTime", "1");
        // 首先判断redis中是否存在该key，如果不存在则新建并设置超时时间。如果存在就加1
        try {
            return RedisUtil.execute(new JedisAction<Boolean>() {

                @Override
                public Boolean doAction(Jedis jedis) {
                    // setnx返回1说明key不存在
                    if(1 == jedis.setnx(key,serialNo)){
                        jedis.expire(key, Integer.parseInt(limitRepeatingRepaymentsIntervalTime));
//                        LoggerUtil.info("{} set expire time", key);
                    }else{
                        // 如果流水号不一样则拒绝该次交易，如果流水号一样就允许该次交易。因为当执行补偿机制时，流水号一致
                        if(!jedis.get(key).equals(serialNo)){
//                            LoggerUtil.warn("key:{} exceed limit ", key);
                            return false;
                        }
                    }
                    
                    return true;
                }
            });
        } catch (Exception e) {
            // catch异常，不做任何处理
            loggerMethod("redis opeaErr", e);
        }

        return true;
    }
    
    
    public static boolean isNeedLimitRateWithLua(final String acct) {
        
        // 首先判断redis中是否存在该key，如果不存在则新建并设置超时时间。如果存在就加1
        try {
            return RedisUtil.execute(new JedisAction<Boolean>() {
                
                @Override
                public Boolean doAction(Jedis jedis) {
                    String key = acct + jedis.time().get(0);

                    Object result = jedis.eval("local current = redis.call('incr',KEYS[1]) if tonumber(current) ==1 then redis.call('expire',KEYS[1],3) end return current", 1, key);
                    if(Integer.parseInt(result.toString()) > limit){
                        return false;
                    }
                    return true;
                }
            });
        } catch (Exception e) {
            // catch异常，不做任何处理
            loggerMethod("redis opeaErr", e);
        }
        
        return true;
    }

    /**
     * 增加自主超时控制
     * 功能描述: <br>
     * 
     *
     * @param key 票据号+金额
     * @param serialNo 流水号
     * @return
     * @since 1.0
     */
    public static boolean execute(final String key,final String serialNo) {

        Future<Boolean> f1 = null;
        try {
            
            f1 = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return isNeedLimitRate(serialNo,key);
                }
            });
            
            // future将在execLimit毫秒之后取结果
            return f1.get(EXEC_LIMIT, TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
//            LoggerUtil.warn("InterExcep");
            f1.cancel(true);// 中断执行此任务的线程
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            loggerMethod("ExecuExcep", e);
            f1.cancel(true);
        } catch (TimeoutException e) {
        	loggerMethod(key + "timeout", e);
            f1.cancel(true);
        }catch(Exception e){
        	loggerMethod("futurExcep", e);
            
            if(f1 != null){
                f1.cancel(true);
            }
        }

        // 不管出现什么异常，业务都继续执行
        return true;
    }
    
    /**
     * 
     * 功能描述: <br>
     * 解决严重问题的下下策
     *
     * @param str
     * @param e
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static void loggerMethod(String str, Exception e) {
//    	LoggerUtil.warn(str);
    }


    /**  
     * 获取limit  
     * @return limit  
     */
    public static int getLimit() {
        return limit;
    }


    /**  
     * 设置limit  
     * @param limit  
     */
    public static void setLimit(int limit) {
        AcctStatistics.limit = limit;
    }


}

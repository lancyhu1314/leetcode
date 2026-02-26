/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: SqlStatementInterceptor.java
 * Author:   17060915
 * Date:     2018年5月3日 下午5:56:31
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.db;

import java.util.Properties;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.suning.fab.tup4ml.utils.LoggerUtil;

/**
 * 数据库操作性能拦截器,记录耗时
 */
@Intercepts(value = {
        @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }),
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class }),
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class }) })
public class SqlStatementInterceptor implements Interceptor {

    private Properties properties;

    @Override
    public Object intercept(Invocation arg0) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) arg0.getArgs()[0];

        String sqlId = mappedStatement.getId();
        Object returnValue;

        long start = System.currentTimeMillis();
        returnValue = arg0.proceed();
        long end = System.currentTimeMillis();
        long time = end - start;

        if(time > 50){
            StringBuilder str = new StringBuilder(100);
            str.append(sqlId);
            str.append(": ");
            str.append("cost time ");
            str.append(time);
            str.append(" ms.");
            LoggerUtil.info(str.toString());
        }else if(time > 10){
            StringBuilder str = new StringBuilder(100);
            str.append(sqlId);
            str.append(": ");
            str.append("cost time ");
            str.append(time);
            str.append(" ms.");
            LoggerUtil.debug(str.toString());
        }
        
        return returnValue;
    }

    @Override
    public Object plugin(Object arg0) {
        return Plugin.wrap(arg0, this);
    }

    @Override
    public void setProperties(Properties arg0) {
        this.properties = arg0;
    }
}
/**
 * SUNING APPLIANCE CHAINS.
 * Copyright (c) 2012-2012 All Rights Reserved.
 */
package com.suning.fab.tup4ml.entity;

import java.util.List;

import com.suning.fab.tup4ml.exception.FabSqlException;

/**
 * BaseDao
 * 
 * @author 12061742
 */

interface ISqlOperate {

	/**
	 * select一行记录，返回对象的类型跟mybatis所配置xml里的resultType有关；
	 * @param sqlId mybatis所配置xml的id；
	 * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
	 * @return 返回select查询到的某一行记录；
	 */
    <T> T selectOne(String sqlId, Object param) throws FabSqlException;
    
    /**
     * select多行记录，返回对象的类型跟mybatis所配置xml里的resultType有关；
     * @param sqlId mybatis所配置xml的id；
     * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @return 返回select查询到的多行记录；
     */
    <T> List<T> selectList(String sqlId, Object param) throws FabSqlException;
    
    /**
     * 分页查询 select多行记录，返回对象的类型跟mybatis所配置xml里的resultType有关；
     * @param sqlId mybatis所配置xml的id；
     * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @param currentPage 当前页
     * @param pageSize 页数大小
     * @return 返回select查询到的多行记录；
     */
    <T> List<T> selectList(String sqlId, Object param, int currentPage, int pageSize) throws FabSqlException;


    /**
     * insert记录；
     * @param sqlId mybatis所配置xml的id；
     * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @return 返回insert所影响的条数
     */
    int insert(String sqlId, Object param) throws FabSqlException;
    
    /**
     * update记录；
     * @param sqlId mybatis所配置xml的id；
     * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @return 返回update所影响的条数
     */
    int update(String sqlId, Object param) throws FabSqlException;

    /**
     * delete记录；
     * @param sqlId mybatis所配置xml的id；
     * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @return 返回delete所影响的条数
     */
    int delete(String sqlId, Object param) throws FabSqlException;
}

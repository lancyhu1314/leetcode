package com.suning.fab.loan.utils;

import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：简化 数据库操作的异常抛送
 *
 * @Author 18049705 MYP
 * @Date Created in 10:36 2020/2/18
 * @see
 */
public class LoanDbUtil {

    //删除
    public static void delete(String sqlId, Map<String, Object> paramMap) throws FabException{
        try{
            DbAccessUtil.execute(sqlId,paramMap);
        } catch (FabSqlException e) {
            //用sqlId 报错
            throw new FabException(e, "SPS101", sqlId);
        }
    }
    //更新
    public static void update(String sqlId, Map<String, Object> paramMap) throws FabException{
        try{
            DbAccessUtil.execute(sqlId,paramMap);
        } catch (FabSqlException e) {
            //用sqlId 报错
            throw new FabException(e, "SPS102", sqlId);
        }
    }
    //更新
    public static void update(String sqlId,  Object object) throws FabException{
        try{
            DbAccessUtil.execute(sqlId,object);
        } catch (FabSqlException e) {
            //用sqlId 报错
            throw new FabException(e, "SPS102", sqlId);
        }
    }
    //插入
    public static void insert(String sqlId, Map<String, Object> paramMap) throws FabException{
        try{
            DbAccessUtil.execute(sqlId,paramMap);
        } catch (FabSqlException e) {
            //用sqlId 报错
            throw new FabException(e, "SPS100", sqlId);
        }
    }
    //插入
    public static void insert(String sqlId,  Object param) throws FabException{
        try{
            DbAccessUtil.execute(sqlId,param);
        } catch (FabSqlException e) {
            //用sqlId 报错
            throw new FabException(e, "SPS100", sqlId);
        }
    }
    //批量更新
    public static void batchUpdate(String sqlId, Map<String, Object>[] batchValues) throws FabException{
        try{
            DbAccessUtil.batchUpdate(sqlId,batchValues);
        } catch (FabSqlException e) {
            //用sqlId 报错
            throw new FabException(e, "SPS102", sqlId);
        }
    }

    //更新
    public static  <T> List<T> queryForList(String sqlId, Map<String, Object> paramMap, Class<T> elementType) throws FabException {
        try {
            return DbAccessUtil.queryForList(sqlId, paramMap, elementType);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "sqlId");
        }
    }
    //更新
    public static  <T> T queryForObject(String sqlId, Map<String, Object> paramMap, Class<T> elementType) throws FabException {
        try {
            return DbAccessUtil.queryForObject(sqlId, paramMap, elementType);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "sqlId");
        }
    }
}

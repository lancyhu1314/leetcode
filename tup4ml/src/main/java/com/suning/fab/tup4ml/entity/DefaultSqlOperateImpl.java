package com.suning.fab.tup4ml.entity;

import java.util.List;

import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import com.suning.fab.tup4ml.exception.FabSqlException;
import com.suning.fab.tup4ml.utils.LoggerUtil;

/**
 * BaseDao的实现类，对数据库进行操作的封装类
 * 
 * @author 12061742
 */
@Repository
class DefaultSqlOperateImpl implements ISqlOperate{

	@Autowired
	private SqlSessionTemplate tplSqlSession;

	@Override
	public <T> T selectOne(String sqlId, Object param) throws FabSqlException {
		try {
			return tplSqlSession.selectOne(sqlId, param);
		} catch (RuntimeException e){
		    LoggerUtil.debug("DefaultSqlOperateImpl selectOne ：{}",e);

			DataAccessException translated = tplSqlSession.getPersistenceExceptionTranslator().translateExceptionIfPossible(e);
			if(null == translated){
				throw new FabSqlException(e, sqlId);
			}else{
				throw new FabSqlException(translated, sqlId);
			}
		}
	}

	@Override
	public <T> List<T> selectList(String sqlId, Object param) throws FabSqlException {
		try {
			return tplSqlSession.selectList(sqlId, param);
		} catch (RuntimeException e){
		    LoggerUtil.debug("DefaultSqlOperateImpl selectList ：{}",e);

			DataAccessException translated = tplSqlSession.getPersistenceExceptionTranslator().translateExceptionIfPossible(e);
			if(null == translated){
				throw new FabSqlException(e, sqlId);
			}else{
				throw new FabSqlException(translated, sqlId);
			}
		}
	}

	@Override
	public int insert(String sqlId, Object param) throws FabSqlException{
		try {
			return tplSqlSession.insert(sqlId, param);
		} catch (RuntimeException e){
		    LoggerUtil.debug("DefaultSqlOperateImpl insert ：{}",e);
		    
			DataAccessException translated = tplSqlSession.getPersistenceExceptionTranslator().translateExceptionIfPossible(e);
			if(null == translated){
				throw new FabSqlException(e, sqlId);
			}else{
				throw new FabSqlException(translated, sqlId);
			}
		}
	}

	@Override
	public int update(String sqlId, Object param) throws FabSqlException {
		try {
			return tplSqlSession.update(sqlId, param);
		} catch (RuntimeException e){
	          LoggerUtil.debug("DefaultSqlOperateImpl update ：{}",e);

			DataAccessException translated = tplSqlSession.getPersistenceExceptionTranslator().translateExceptionIfPossible(e);
			if(null == translated){
				throw new FabSqlException(e, sqlId);
			}else{
				throw new FabSqlException(translated, sqlId);
			}
		}
	}

	@Override
	public int delete(String sqlId, Object param) throws FabSqlException {
		try {
			return tplSqlSession.delete(sqlId, param);
		} catch (RuntimeException e){
	          LoggerUtil.debug("DefaultSqlOperateImpl delete ：{}",e);

			DataAccessException translated = tplSqlSession.getPersistenceExceptionTranslator().translateExceptionIfPossible(e);
			if(null == translated){
				throw new FabSqlException(e, sqlId);
			}else{
				throw new FabSqlException(translated, sqlId);
			}
		}
	}

    @Override
    public <T> List<T> selectList(String sqlId, Object param, int currentPage, int pageSize) throws FabSqlException {
        try {
            return tplSqlSession.selectList(sqlId, param, new RowBounds((currentPage-1) * pageSize, pageSize));
        } catch (RuntimeException e){
            LoggerUtil.debug("DefaultSqlOperateImpl selectList ：{}",e);

            DataAccessException translated = tplSqlSession.getPersistenceExceptionTranslator().translateExceptionIfPossible(e);
            if(null == translated){
                throw new FabSqlException(e, sqlId);
            }else{
                throw new FabSqlException(translated, sqlId);
            }
        }
    }
}

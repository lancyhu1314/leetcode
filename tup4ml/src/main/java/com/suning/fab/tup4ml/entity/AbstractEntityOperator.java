package com.suning.fab.tup4ml.entity;

import java.util.List;

import com.suning.fab.model.domain.entity.IEntityOperator;
import com.suning.fab.tup4ml.ctx.TranCtx;
import com.suning.fab.tup4ml.elfin.ServiceFactory;
import com.suning.fab.tup4ml.exception.FabSqlException;
import com.suning.fab.tup4ml.utils.CtxUtil;

/**
 * 实体操作的抽象类，可以使用DB操作；
 * @author 16030888
 *
 */
public abstract class AbstractEntityOperator implements IEntityOperator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * SQL操作接口
	 */
	private transient ISqlOperate sqlOpt;
	
	public AbstractEntityOperator() {
		this.sqlOpt = ServiceFactory.getBean(ISqlOperate.class);
	}

	public TranCtx getCtx() {
		return CtxUtil.getCtx();
	}

	/**
	 * select一行记录，返回对象的类型跟mybatis所配置xml里的resultType有关；
	 * @param sqlId mybatis所配置xml的id；
	 * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
	 * @return 返回select查询到的某一行记录；
	 */
	protected final <T> T selectOne(String sqlId, Object param) throws FabSqlException {
			return sqlOpt.selectOne(sqlId, param);
	}
    
    /**
     * select多行记录，返回对象的类型跟mybatis所配置xml里的resultType有关；
     * @param sqlId mybatis所配置xml的id；
     * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @return 返回select查询到的多行记录；
     */
	protected final <T> List<T> selectList(String sqlId, Object param) throws FabSqlException {
    	return sqlOpt.selectList(sqlId, param);
    }
	
	/**
	 * 分页查询 select多行记录，返回对象的类型跟mybatis所配置xml里的resultType有关；
	 * @param sqlId mybatis所配置xml的id；
	 * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @param currentPage 当前页
     * @param pageSize 页数大小
	 * @return 返回select查询到的多行记录；
	 */
	protected final <T> List<T> selectList(String sqlId, Object param, int currentPage, int pageSize) throws FabSqlException {
	    return sqlOpt.selectList(sqlId, param, currentPage, pageSize);
	}


    /**
     * insert记录；
     * @param sqlId mybatis所配置xml的id；
     * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @return 返回insert所影响的条数
     */
	protected final int insert(String sqlId, Object param) throws FabSqlException {
    	return sqlOpt.insert(sqlId, param);
    }
    
    /**
     * update记录；
     * @param sqlId mybatis所配置xml的id；
     * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
     * @return 返回update所影响的条数
     */
	protected final int update(String sqlId, Object param) throws FabSqlException {
    	return sqlOpt.update(sqlId, param);
    }
	

	/**
	 * delete记录；
	 * @param sqlId mybatis所配置xml的id；
	 * @param param sql参数，注意跟mybatis所配置xml里的parameterType属性有关；
	 * @return 返回delete所影响的条数
	 */
	protected int delete(String sqlId, Object param) throws FabSqlException {
    	return sqlOpt.delete(sqlId, param);
	}
}

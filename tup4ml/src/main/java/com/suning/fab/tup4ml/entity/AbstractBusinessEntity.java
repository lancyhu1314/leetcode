package com.suning.fab.tup4ml.entity;

import java.util.ArrayList;
import java.util.List;

import com.suning.fab.model.domain.entity.IBalanceSet;
import com.suning.fab.model.domain.entity.IBusinessEntity;
import com.suning.fab.model.domain.entity.IEntityOperator;
import com.suning.fab.model.domain.entity.IKeyObject;
import com.suning.fab.model.domain.entity.IStatusSet;
import com.suning.fab.model.domain.entity.ITimePoints;
import com.suning.fab.tup4ml.ctx.TranCtx;
import com.suning.fab.tup4ml.elfin.ServiceFactory;
import com.suning.fab.tup4ml.exception.FabRuntimeException;
import com.suning.fab.tup4ml.exception.FabSqlException;
import com.suning.fab.tup4ml.utils.CtxUtil;

/**
 * 实体抽象类，完成底层实体的DB操作，逻辑操作等；
 * @author 16030888
 *
 */
public abstract class AbstractBusinessEntity implements IBusinessEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 内部唯一ID;
	 */
	private final IKeyObject uid;
	
	/**
	 * 余额；
	 */
	protected IBalanceSet balance;
	
	/**
	 * 状态；
	 */
	protected IStatusSet status;
	
	/**
	 * 时间点；
	 */
	protected ITimePoints timePoints;
	
	/**
	 * 实体操作列表；
	 */
	protected List<AbstractEntityOperator> operations=new ArrayList<>();

	/**
	 * SQL操作接口
	 */
	private transient ISqlOperate sqlOpt;
	
	public AbstractBusinessEntity(IKeyObject uid) {
		this.uid = uid;
		this.sqlOpt = ServiceFactory.getBean(ISqlOperate.class);
	}

	public TranCtx getCtx() {
		return CtxUtil.getCtx();
	}

	@Override
	public void save() {
		for(IEntityOperator op:this.operations){
			op.operate(this);
			op.save();
		}
		this.load();
		if(false == this.finalValidate()) {
			throw new FabRuntimeException("TUP112", this.getClass().getSimpleName());
		}
		//commit
	}

	@Override
	public final IKeyObject getUid() {
		return uid;
	}

	/**
	 * 添加一项操作； 
	 * @param operand 操作项；
	 */
	public final void accept(AbstractEntityOperator operand){
		this.operations.add(operand);
	}

	/**
	 * 获取余额；
	 */
	@Override
	public IBalanceSet getBalance() {
		return balance;
	}

	/**
	 * 设置余额；
	 * @param balance IBalanceSet余额接口；
	 */
	@Override
	public void setBalance(IBalanceSet balance) {
		this.balance = balance;
	}

	/**
	 * 获取状态；
	 */
	@Override
	public IStatusSet getStatus() {
		return status;
	}

	/**
	 * 设置状态；
	 * @param status IStatusSet状态接口；
	 */
	@Override
	public void setStatus(IStatusSet status) {
		this.status = status;
	}

	/**
	 * 获取时间点；
	 */
	@Override
	public ITimePoints getTimePoints() {
		return timePoints;
	}

	/**
	 * 设置时间点；
	 * @param timePoints ITimePoints时间点接口；
	 */
	@Override
	public void setTimePoints(ITimePoints timePoints) {
		this.timePoints = timePoints;
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
}

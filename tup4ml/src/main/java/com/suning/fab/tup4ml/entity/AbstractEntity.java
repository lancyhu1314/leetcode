package com.suning.fab.tup4ml.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.suning.fab.model.domain.entity.IBaseDao;
import com.suning.fab.model3.domain.entity.AbstractDetail;
import com.suning.fab.model3.domain.entity.AbstractEntityAttribute;
import com.suning.fab.model3.domain.entity.IDetail;
import com.suning.fab.model3.domain.entity.IEntity;
import com.suning.fab.model3.domain.entity.IEntityAttribute;
import com.suning.fab.tup4ml.ctx.TranCtx;
import com.suning.fab.tup4ml.elfin.ServiceFactory;
import com.suning.fab.tup4ml.exception.FabRuntimeException;
import com.suning.fab.tup4ml.exception.FabSqlException;
import com.suning.fab.tup4ml.utils.CtxUtil;

/**
 * 零钱宝模式抽象实体
 */
public abstract class AbstractEntity implements IEntity, IBaseDao {

	private static final long serialVersionUID = 1L;
	
	/**
	 * 内部唯一ID;
	 */
	private final Object uid;
	
	private final List<AbstractEntity> entitylist = new ArrayList<AbstractEntity>();
	
	private final List<IDetail> detaillist = new ArrayList<IDetail>();
	
	private AbstractEntityAttribute originalAttr;
	
	private AbstractEntityAttribute currentAttr;
	
	/**
	 * SQL操作接口
	 */
	private transient ISqlOperate sqlOpt;
	
	public AbstractEntity(Object uid) {
		this.uid = uid;
		this.sqlOpt = ServiceFactory.getBean(ISqlOperate.class);
	}
	
	public TranCtx getCtx() {
		return CtxUtil.getCtx();
	}
	
	@Override
	public void save() {
		for(AbstractEntity e:this.getEntityList())
		{
			AbstractEntity entity = (AbstractEntity)e;
			if(entity.isModified()) {
				e.save();
				e.finalValidate();//挨个检查每个子实体的合法性
			}
		}
		
		this.load();//增加合法性校验
		if(false == this.finalValidate()) {//大实体合法性检查
			throw new FabRuntimeException("TUP112", this.getClass().getSimpleName());
		}
		
	}

	@Override
	public void load() {
		for(AbstractEntity e:this.getEntityList())
		{
			e.load();
		}
		
	}

	@Override
	public Object getUid() {
		return this.uid;
	}


	protected IEntityAttribute getOriginalAttr() {
		return this.originalAttr;
	}

	protected IEntityAttribute getCurrentAttr() {
		return this.currentAttr;
	}

	protected void setCurrentAttr(IEntityAttribute attr) throws ClassNotFoundException, IOException {
		this.currentAttr = (AbstractEntityAttribute)attr;
		this.originalAttr = (AbstractEntityAttribute)this.currentAttr.deepClone();
		
	}

	protected List<IDetail> getDetail() {
		return this.detaillist;
	}

	protected void setDetail(IDetail detail) {
		this.detaillist.add((AbstractDetail)detail);
	}

	protected List<AbstractEntity> getEntityList() {
		return this.entitylist;
	}

	protected void addEntity(AbstractEntity entity) {
		this.entitylist.add(entity);
		
	}

	protected Boolean isModified() {
		return this.detaillist.size()>0?true:false;
	}
	
	@Override
	public void setUid(Object uid) {
		// TODO Auto-generated method stub
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

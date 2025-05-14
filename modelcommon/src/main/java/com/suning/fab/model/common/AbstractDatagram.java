package com.suning.fab.model.common;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

import com.suning.fab.model.constant.CommonConstant;
import com.suning.fab.model.utils.VersionUtil;
import com.suning.framework.sedis.ReflectionUtils;

/**
 * 通用参数接口；
 * @author 16030888
 *
 */
public abstract class AbstractDatagram implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 上下文；
	 */
	protected IContext ctx = null;
	
	protected String datagramVersion = VersionUtil.getJarVersion(this.getClass());
	
	/**
	 * 获取路由ID；
	 * @return 路由ID；
	 */
	public abstract String getRouteId();
	
	/**
	 * 获取该报文的HashCode，通常是可以唯一标识的字符串；
	 * @return 返回String的HashCode
	 */
	public abstract String getProtocalHashCode();

	/**
	 * 获取该报文的外部流水号；
	 * @return 返回String的外部流水号；
	 */
	public abstract String getOuterSerialNumber();

	/**
	 * 获取该报文的请求日期；
	 * @return 返回Date的请求日期；
	 */
	public abstract Date getRequestDate();

	/**
	 * 校验相关参数合法性；
	 * 规范要求：validate()里面校验该类相关属性的合法性，统一抛出IllegalArgumentException异常，格式举例：<br/>
	 * validate(){<br/>
	 * &nbsp;&nbsp;&nbsp;&nbsp;if(VarChecker.isEmpty(某个属性字段字段XXX)) {<br/>
	 *	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;throw new IllegalArgumentException("XXX不能为空");<br/>
	 *	&nbsp;&nbsp;&nbsp;&nbsp;}<br/>
	 * }<br/>
	 * @return 通过校验返回true；没通过则返回false；
	 */
	public abstract boolean validate();

	/**
	 * 获取某个属性字段的值，没找到返回null；<br/>
	 * 不支持List&lt;?&gt;、May&lt;?&gt;的获取，因为需要索引信息；
	 * @param key 属性字段名，支持点号"."分隔的属性递归查找；
	 * @return 找到对应属性字段的值，如果没找到则返回null；
	 */
	public Object getValue(String key){
		if(null == key || key.isEmpty()){
			throw new IllegalArgumentException(CommonConstant.ExceptionConstant.ARGUMENTISNULL);
		}
		String[] keySplit = key.split("\\.");
		Object ret = this;
		for(String k : keySplit){
			Field f = ReflectionUtils.findField(ret.getClass(), k);
			if(null != f) {
				f.setAccessible(true);
				ret = ReflectionUtils.getField(f, ret);
				f.setAccessible(false);
				if(null == ret)
					break;
			} else {
				ret = null;
				break;
			}
		}
		return ret;
	}

	/**
	 * 设置某个属性字段的值，没找到属性字段则忽略；<br/>
	 * 不支持List&lt;?&gt;、May&lt;?&gt;的设置，因为需要索引信息；
	 * @param key 属性字段名，支持点号"."分隔的属性递归查找；
	 * @param value 属性字段的值；
	 */
	public void setValue(String key, Object value){
		if(null == key || key.isEmpty()){
			throw new IllegalArgumentException(CommonConstant.ExceptionConstant.ARGUMENTISNULL);
		}
		String[] keySplit = key.split("\\.");
		Object ins = this;
		for(int i = 0; i < keySplit.length - 1;++i){
			Field f = ReflectionUtils.findField(ins.getClass(), keySplit[i]);
			if(null != f) {
				f.setAccessible(true);
				ins = ReflectionUtils.getField(f, ins);
				f.setAccessible(false);
				if(null == ins)
					return;
			} else {
				return;
			}
		}

		Field f = ReflectionUtils.findField(ins.getClass(), keySplit[keySplit.length - 1]);
		f.setAccessible(true);
		ReflectionUtils.setField(f, ins, value);
		f.setAccessible(false);
	}

	/**
	 * 获取本次报文相关的上下文；
	 * @return 上下文IContext；
	 */
	public IContext getCtx(){
		return ctx;
	}

	/**
	 * 设置本次报文相关的上下文；
	 * @param ctx 平台层定义的上下文，应用层通常需要setCtx()；
	 */
	public void setCtx(IContext ctx) {
		this.ctx = ctx;
	}

	public AbstractDatagram() {
		//do nothing
	}
}

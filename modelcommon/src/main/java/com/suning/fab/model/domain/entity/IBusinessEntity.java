package com.suning.fab.model.domain.entity;

import java.io.Serializable;

/**
 * 抽象业务实体类；
 * @author 16030888
 * @version 1.0
 * @since 2017-12-09 20:37:29
 */
public interface IBusinessEntity extends IBaseDao, Serializable {
	/**
	 * 做最终的校验；
	 * @return 成功返回true；失败返回false；
	 */
	public boolean finalValidate();
	
	/**
	 * 获取唯一标识；
	 * @return 返回唯一标识Key接口；
	 */
	public IKeyObject getUid();
	
	/**
	 * 获取余额；
	 * @return 返回余额接口；
	 */
	public IBalanceSet getBalance();

	/**
	 * 设置余额；
	 * @param balance 表示余额的接口；
	 */
	public void setBalance(IBalanceSet balance);

	/**
	 * 获取状态；
	 * @return 返回表示状态的接口；
	 */
	public IStatusSet getStatus();

	/**
	 * 设置状态；
	 * @param status 返回表示状态的接口；
	 */
	public void setStatus(IStatusSet status);

	/**
	 * 获取时间点；
	 * @return 返回表示时间点的接口；
	 */
	public ITimePoints getTimePoints();

	/**
	 * 设置时间点；
	 * @param timePoints 表示时间点的接口；
	 */
	public void setTimePoints(ITimePoints timePoints);
}
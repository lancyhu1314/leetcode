package com.suning.fab.model.domain.entity;

/**
 * 账户操作接口
 * @author 16030888
 * @version 1.0
 * @since 2017-12-09 20:37:52
 */
public interface IOperator {

	/**
	 * 执行某种操作；
	 * @param abstractBusinessEntity 要执行操作的抽象业务实体；
	 */
	public void operate(IBusinessEntity abstractBusinessEntity);

}
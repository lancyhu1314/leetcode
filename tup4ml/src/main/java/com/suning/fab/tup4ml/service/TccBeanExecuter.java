package com.suning.fab.tup4ml.service;

import com.suning.dtf.client.interceptor.TccTransactionContext;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.exception.FabException;

/**
 * TCC事务执行器基类；
 * @author 16030888
 *
 */
abstract class TccBeanExecuter {

	protected ServiceTemplate owner;

	public void setOwner(ServiceTemplate owner) {
		this.owner = owner;
	}

	public ServiceTemplate getOwner() {
		return owner;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TccBeanExecuter that = (TccBeanExecuter) o;
		return owner.getClass().getSimpleName().equals(that.owner.getClass().getSimpleName());
	}
	
	@Override
	public int hashCode() {
		return owner.getClass().getSimpleName().hashCode();
	}

	public abstract AbstractDatagram prepare(TccTransactionContext tc, boolean ignore, String routeId, String businessId, AbstractDatagram param) throws FabException;

	public abstract AbstractDatagram confirm(TccTransactionContext tc, boolean ignore, String routeId, String businessId, AbstractDatagram param) throws FabException;

	public abstract AbstractDatagram cancel(TccTransactionContext tc, boolean ignore, String routeId, String businessId, AbstractDatagram param) throws FabException;
}

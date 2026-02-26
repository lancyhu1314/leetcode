package com.suning.fab.tup4ml.service;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.common.IContext;
import com.suning.fab.tup4ml.exception.FabException;

public interface IInvoker {
	public AbstractDatagram invoke(Class<? extends ServiceTemplate> serviceClass, AbstractDatagram param) throws FabException;
	public void clean();
	public void setContext(IContext ctx);
}

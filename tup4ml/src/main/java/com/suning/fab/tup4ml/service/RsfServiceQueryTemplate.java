package com.suning.fab.tup4ml.service;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.ctx.LocalTranCtx;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;

/**
 * 只用于查询的标准RSF服务模板，里面包含本地DB事务但会回滚以防止在查询中做写操作；
 * 请使用callLocalService()方法调用本地服务；
 * @author 16030888
 *
 */
public abstract class RsfServiceQueryTemplate extends RsfServiceStandTemplate {

	public RsfServiceQueryTemplate(){
	}

	/**
	 * 创建本次交易的本地服务交易上下文；
	 * @return LocalTranCtx对象实例；
	 */
	protected LocalTranCtx createLocalTranCtx() {
		LocalTranCtx ctx = super.createLocalTranCtx();
		ctx.setBid(null);//不需要流水号
		return ctx;
	}

	@Override
	protected AbstractDatagram onProtoReg(AbstractDatagram in, AbstractDatagram out) {
		return PlatConstant.RSPVALUE.NOTSET;
	}

	@Override
	protected Boolean doIdempotency(AbstractDatagram param) throws FabException {
		return Boolean.FALSE;
	}

	@Override
	protected AbstractDatagram onIdempotency(AbstractDatagram param) {
		return PlatConstant.RSPVALUE.NOTSET;
	}
}

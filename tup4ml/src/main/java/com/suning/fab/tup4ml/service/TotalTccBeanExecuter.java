package com.suning.fab.tup4ml.service;

import com.suning.dtf.client.annotation.DtfIdempotent;
import com.suning.dtf.client.annotation.DtfParameter;
import com.suning.dtf.client.annotation.DtfTransaction;
import com.suning.dtf.client.interceptor.TccTransactionContext;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.ctx.TccTranCtx;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.utils.CtxUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.SceneUtil;
import com.suning.fab.tup4ml.utils.StringUtil;

/**
 * 编写freemaker模板检查语法用，无其他用途；
 * TCC总事务执行器；
 * @author 16030888
 *
 */
public class TotalTccBeanExecuter extends TccBeanExecuter {

	@Override
	@DtfTransaction(confirmMethod = "confirm", cancelMethod = "cancel")
	public AbstractDatagram prepare(@DtfParameter TccTransactionContext tc, @DtfParameter boolean ignore, @DtfParameter String routeId, @DtfParameter @DtfIdempotent String businessId, @DtfParameter AbstractDatagram param) throws FabException {
		try {
			TccTranCtx ctx = (TccTranCtx)param.getCtx();
			ctx.setTc(tc);//第一次dtf开启总事务时，tc被aop重新赋值过，这时参数param的tc是null的
			return getOwner().prepare(param);
		}catch(FabException e) {
			//打印异常，方便知道调用进度和顺序，再往外层就是dtf组件的aop处理了
			LoggerUtil.logException(StringUtil.formatOuterSerialNumber(param), SceneUtil.getSceneFromThreadLocal(), e, param);
			throw e;
		}
	}

	@Override
	public AbstractDatagram confirm(TccTransactionContext tc, boolean ignore, String routeId, String businessId, AbstractDatagram param) throws FabException {
		TccTranCtx ctx = (TccTranCtx)param.getCtx();
		ctx.setTc(tc);//第一次dtf开启总事务时，tc被aop重新赋值过，这时参数param的tc是null的
		CtxUtil.setCtx(ctx);//注意，这里dtf组件新开一个线程来进行第二阶段提交/回滚
		AbstractDatagram ret = getOwner().confirm(param);
		//清理工作
		getOwner().onClean();
		return ret;
	}

	@Override
	public AbstractDatagram cancel(TccTransactionContext tc, boolean ignore, String routeId, String businessId, AbstractDatagram param) throws FabException {
		TccTranCtx ctx = (TccTranCtx)param.getCtx();
		ctx.setTc(tc);//第一次dtf开启总事务时，tc被aop重新赋值过，这时参数param的tc是null的
		CtxUtil.setCtx(ctx);//注意，这里dtf组件新开一个线程来进行第二阶段提交/回滚
		AbstractDatagram ret = getOwner().cancel(param);
		//清理工作
		getOwner().onClean();
		return ret;
	}

}

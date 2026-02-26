package com.suning.fab.tup4ml.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.dtf.client.annotation.DtfIdempotent;
import com.suning.dtf.client.annotation.DtfParameter;
import com.suning.dtf.client.annotation.DtfTransaction;
import com.suning.dtf.client.interceptor.TccTransactionContext;
import com.suning.dtf.support.ExceptionCode;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.domain.entity.IBaseDao;
import com.suning.fab.model.domain.protocal.ExitBusinessCommon;
import com.suning.fab.tup4ml.ctx.TccTranCtx;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.utils.CtxUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.StringUtil;

/**
 * 编写freemaker模板时检查语法用，无其他用途；
 * TCC子事务执行器；
 * @author 16030888
 *
 */
public class SubTccBeanExecuter extends TccBeanExecuter {
	
	@Override
	@DtfTransaction(confirmMethod = "confirm", cancelMethod = "cancel")
	public AbstractDatagram prepare(@DtfParameter TccTransactionContext tc, @DtfParameter boolean ignore, @DtfParameter String routeId, @DtfParameter @DtfIdempotent String businessId, @DtfParameter AbstractDatagram param) throws FabException {
		AbstractDatagram ret = getOwner().runLocalDBTransaction(
				param, 
				(AbstractDatagram arg) ->getOwner().doIdempotency(param), 
				(AbstractDatagram arg) ->{
					//子TCC的每一步都幂等，所以操作幂等表遇到幂等后继续其流程；
					LoggerUtil.info("进入TCC子事务正常业务入口 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					AbstractDatagram retVal = getOwner().prepare(param);
					LoggerUtil.info("完成TCC子事务业务正常流程，预处理数据保存 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					if(null != Repository.getItems()) {
						for(IBaseDao iBaseDao: Repository.getItems()){
							iBaseDao.save();
						}
					}
					return retVal;
				},
				(AbstractDatagram arg) ->getOwner().onIdempotency(arg), 
				Boolean.TRUE);
		
		if(ret != null){
		    ExitBusinessCommon extBusinessCommon = (ExitBusinessCommon) ret;
		    FabException fabException = new FabException(extBusinessCommon.getRspCode());
		    fabException.setErrMsg(extBusinessCommon.getRspMsg());
		    throw fabException;
		}
		
		return ret;      
	}

	@Override
	public AbstractDatagram confirm(TccTransactionContext tc, boolean ignore, String routeId, String businessId, AbstractDatagram param) throws FabException {
		LoggerUtil.info("TCC子事务【二阶段提交】开始 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
		TccTranCtx ctx = (TccTranCtx)param.getCtx();
		CtxUtil.setCtx(ctx);
		getOwner().storeScene(param);

		AbstractDatagram ret = getOwner().runLocalDBTransaction(
				param, 
				null, 
				(AbstractDatagram arg) -> {
					LoggerUtil.info("进入TCC子事务confirm()入口 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					AbstractDatagram retVal = getOwner().confirm(param);
					LoggerUtil.info("完成TCC子事务confirm()方法，预处理数据保存 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					if(null != Repository.getItems()) {
						for(IBaseDao iBaseDao: Repository.getItems()){
							iBaseDao.save();
						}
					}
					return retVal;
				},
				null, 
				Boolean.FALSE);
		if(PlatConstant.RSPVALUE.NOTSET  != ret) {
			//如果二阶段提交有问题，抛出异常给dtf组件重试confirm()；
			String rspCode = (String)ret.getValue("rspCode");
			String rspMsg = (String)ret.getValue("rspMsg");
			if( (null != rspCode) && (0 !=rspCode.compareToIgnoreCase(PlatConstant.RSPCODE.OK) )) {
				FabException expRet = new FabException(rspCode);
				expRet.setErrCode(rspCode);
				expRet.setErrMsg(rspMsg);
				getOwner().onClean();
				throw expRet;
			}
		}

		//清理工作
		getOwner().onClean();

		LoggerUtil.info("TCC子事务【二阶段提交】结束 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
		
		return ret;
	}

	@Override
	public AbstractDatagram cancel(TccTransactionContext tc, boolean ignore, String routeId, String businessId, AbstractDatagram param) throws FabException {
		LoggerUtil.info("TCC子事务【二阶段回滚】开始 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
		TccTranCtx ctx = (TccTranCtx)param.getCtx();
		
		// 将dtf解析获取到的ExceptionCode放入ctx中，以备给业务使用
		ExceptionCode exceptionCode = tc.getExceptionCode();
		
		Map<String,String> map = new HashMap<>();
		map.put("errorCode", exceptionCode.code);
		map.put("errorMsg", exceptionCode.codeType.toString());
		ctx.setExceptionCode(map);
		
		CtxUtil.setCtx(ctx);
	    getOwner().storeScene(param);
		AbstractDatagram ret = getOwner().runLocalDBTransaction(
				param, 
				null, 
				(AbstractDatagram arg) -> {
					LoggerUtil.info("进入TCC子事务cancel()入口 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					AbstractDatagram retVal = getOwner().cancel(param);
					LoggerUtil.info("完成TCC子事务cancel()方法，预处理数据保存 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					if(null != Repository.getItems()) {
						for(IBaseDao iBaseDao: Repository.getItems()){
							iBaseDao.save();
						}
					}
					return retVal;
				},
				null, 
				Boolean.FALSE);
		if(PlatConstant.RSPVALUE.NOTSET  != ret) {
			//如果二阶段回滚有问题，抛出异常给dtf组件重试cancel()；
			String rspCode = (String)ret.getValue("rspCode");
			String rspMsg = (String)ret.getValue("rspMsg");
			if( (null != rspCode) && (0 !=rspCode.compareToIgnoreCase(PlatConstant.RSPCODE.OK) )) {
				FabException expRet = new FabException(rspCode);
				expRet.setErrCode(rspCode);
				expRet.setErrMsg(rspMsg);
				getOwner().onClean();
				throw expRet;
			}
		}

		//清理工作
		getOwner().onClean();

		LoggerUtil.info("TCC子事务【二阶段回滚】结束 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
		
		return ret;
	}

}

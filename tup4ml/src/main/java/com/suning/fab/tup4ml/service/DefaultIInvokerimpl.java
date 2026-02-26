package com.suning.fab.tup4ml.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.common.IContext;
import com.suning.fab.model.constant.CommonConstant;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.elfin.ServiceFactory;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.utils.AnotUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.RsfConsumerUtil;
import com.suning.fab.tup4ml.utils.StringUtil;
import com.suning.fab.tup4ml.utils.ThreadLocalUtil;
import com.suning.rsf.consumer.ServiceAgent;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 
 * 实现本地调用或者远程RSF调用的默认实现类；
 *
 * @author 17060915
 * @since 2017年12月20日上午11:09:51
 */
@Component
class DefaultIInvokerimpl implements IInvoker {
	public DefaultIInvokerimpl() {
	}

	private class FabContext{
		//线程相关的上下文；
		IContext ctx;

		//线程相关的上一次调用类型：-1 -- 未定义；0 -- LocalServiceTemplate；1 -- RsfServiceStandTemplate；2 -- RsfServiceQueryTemplate；3 -- TccServiceTemplate；
		Integer callType;

		FabContext(IContext iContext, Integer callType){
			this.ctx = iContext;
			this.callType = callType;
		}
	}

	private static final ThreadLocal<FabContext> tlCtx = new ThreadLocal<>();	
	private static final String CONTRACT = "contract";
	private static final String IMPL_CODE = "implCode";
	private static final String METHOD_NAME = "execute";

	private AbstractDatagram rsfAndTccInvoke(Class<? extends ServiceTemplate> srvClass, AbstractDatagram param) throws FabException {

		Implement implement = AnotUtil.findAnnotation(srvClass, Implement.class);
		if (null == implement) {
			throw new IllegalArgumentException(CommonConstant.ExceptionConstant.ARGUMENTNOTSATISFY);
		}

		Map<String, Object> annotationMap = AnotUtil.getAnnotationAttributes(implement, true);
		String implCode = (String) annotationMap.get(IMPL_CODE);
		String contract = (String) annotationMap.get(CONTRACT);
		if(implCode.isEmpty() && contract.isEmpty()){
			throw new IllegalArgumentException(CommonConstant.ExceptionConstant.ARGUMENTNOTSATISFY);
		}
		String key = contract + "." + implCode;        
		ServiceAgent serviceAgent = RsfConsumerUtil.getServiceAgent(key);

		if(null == tlCtx.get()){//上下文为空，则默认使用传参的上下文
			tlCtx.set(new FabContext(param.getCtx(), 3));
		}else{
			if(-1 == tlCtx.get().callType || 3 == tlCtx.get().callType){//第一次调用，或者跟上次调用类型一致
				param.setCtx(tlCtx.get().ctx);
				if(-1 == tlCtx.get().callType) {
					tlCtx.get().callType = 3;
				}
			} else {
				throw new FabException(PlatConstant.RSPCODE.SERVICETYPEERROR);
			}
		}


		String rspCode = null;
		String rspMsg = null;
		AbstractDatagram ret = null;
		try{
			//远程同步调用
			tlCtx.get().ctx.setSubSeq(tlCtx.get().ctx.getSubSeq() + 1);
			ret = (AbstractDatagram)serviceAgent.invoke(METHOD_NAME, new Object[] { param }, new Class[] { AbstractDatagram.class });

			//判断远程调用的返回报文，失败则默认抛出错误码的异常；
			rspCode = (String)ret.getValue("rspCode");
			rspMsg = (String)ret.getValue("rspMsg");
		}catch(Exception e){
			LoggerUtil.error("rsf 远程调用异常 | OuterSerialNumber【{}】：{}；", StringUtil.formatOuterSerialNumber(param), e);
			throw new FabException(e, "TUP103");
		}

		if( (null != rspCode) && (0 !=rspCode.compareToIgnoreCase(PlatConstant.RSPCODE.OK) )) {
			FabException expRet = new FabException(rspCode);
			expRet.setErrCode(rspCode);
			expRet.setErrMsg(rspMsg);
			throw expRet;
		} else {
			return ret;
		}
	}

	private AbstractDatagram localInvoke(Class<? extends ServiceTemplate> srvClass, AbstractDatagram param) throws FabException {
		if(null == tlCtx.get()){//上下文为空，则默认使用传参的上下文
			tlCtx.set(new FabContext(param.getCtx(), 0));
		}else{
			if(-1 == tlCtx.get().callType || 0 == tlCtx.get().callType){//第一次调用，或者跟上次调用类型一致
				param.setCtx(tlCtx.get().ctx);
				if(-1 == tlCtx.get().callType) {
					tlCtx.get().callType = 0;
				}
			} else {
				throw new FabException(PlatConstant.RSPCODE.SERVICETYPEERROR);
			}
		}
		LocalServiceTemplate srvInstance = (LocalServiceTemplate) ServiceFactory.getBean(srvClass);
		return srvInstance.execute(param);
	}

	@Override
	public void clean(){
		tlCtx.set(null);
	}

	@Override
	public AbstractDatagram invoke(Class<? extends ServiceTemplate> serviceClass, AbstractDatagram param) throws FabException {
	    
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) ThreadLocalUtil.get(PlatConstant.PLATCONST.TRANS_CALL_CHAIN);
	    if(list == null || list.isEmpty()){
	        list = new ArrayList<>();
	    }
	    list.add(serviceClass.getSimpleName());
	    ThreadLocalUtil.set(PlatConstant.PLATCONST.TRANS_CALL_CHAIN, list);
	    
		if(LocalServiceTemplate.class.isAssignableFrom(serviceClass)){
	        LoggerUtil.info("调用本地服务【{}】的execute() ", serviceClass.getSimpleName());
			return localInvoke(serviceClass, param);
		}else if(RsfServiceQueryTemplate.class.isAssignableFrom(serviceClass)){
			throw new FabException(PlatConstant.RSPCODE.SERVICETYPEERROR);
		}else if(RsfServiceStandTemplate.class.isAssignableFrom(serviceClass)){
			throw new FabException(PlatConstant.RSPCODE.SERVICETYPEERROR);
		}else if(SubTccServiceTemplate.class.isAssignableFrom(serviceClass)){
			LoggerUtil.info("调用TCC子服务【{}】的execute() | OuterSerialNumber【{}】", serviceClass.getSimpleName(), StringUtil.formatOuterSerialNumber(param));
			return rsfAndTccInvoke(serviceClass, param);
		}
		return null;
	}

	@Override
	public void setContext(IContext ctx) {
		tlCtx.set(new FabContext(ctx, -1));
	}
}

package com.suning.fab.tup4ml.service;

import java.util.Date;
import java.util.Map;

import com.suning.dtf.common.exception.DtfCommitException;
import com.suning.dtf.common.exception.DtfException;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.common.IFabRsfService;
import com.suning.fab.model.domain.protocal.ExitBusinessCommon;
import com.suning.fab.tup4ml.ctx.TccTranCtx;
import com.suning.fab.tup4ml.elfin.Pair;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.exception.FabRateLimiterException;
import com.suning.fab.tup4ml.utils.CtxUtil;
import com.suning.fab.tup4ml.utils.GuidUtil;
import com.suning.fab.tup4ml.utils.JsonTransferUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.RateLimiterUtil;
import com.suning.fab.tup4ml.utils.SceneUtil;
import com.suning.fab.tup4ml.utils.StringUtil;
import com.suning.rsf.statistic.Request;

public abstract class TotalTccServiceTemplate extends ServiceTemplate implements IFabRsfService {

	public TotalTccServiceTemplate(){
		//nothing
	}

	@Override
	public AbstractDatagram execute(AbstractDatagram param) {
		//起始计数
		Long startInterval = System.currentTimeMillis();
		try {
			param.setCtx(null);
			return tccTotalTransactionEntry(param, startInterval);
		}catch(Exception e){
			LoggerUtil.info("未知异常 | OuterSerialNumber【{}】：{}", StringUtil.formatOuterSerialNumber(param), e);	
			AbstractDatagram response = ResponseHelper.createDefaultErrorRespone("unknown", new Date());
			if(null != e.getMessage()) {
				response.setValue("rspMsg", e.getMessage());
			}else{
				Throwable exp = e.getCause();
				while((null != exp) && (null == exp.getMessage())) {
					exp = exp.getCause();
				}
				if((null != exp) && (null != exp.getMessage())) {
					response.setValue("rspMsg", exp.getMessage());
				}else{
					response.setValue("rspMsg", PlatConstant.RSPMSG.UNKNOWN);
				}
			}

			//返回报文前处理
			doFinish(param, startInterval, response);

			return response;
		}
	}

	@Override
	public Map<String, Object> execute(Map<String, Object> reqMsg) {
		throw new IllegalStateException("SubTccServiceTemplate execute(Map<String, Object> ) is not allow");
	}

	/**
	 * TCC总事务公共入口，进入总事务入口prepare()，处理最外部异常、写monitor日志等；<br/>
	 * @param param 外部传过来的参数；
	 * @param startInterval 计数开始；
	 * @return 返回应用层提供的IDatagram类；如果有异常则返回默认的ExitProtoCommon类；
	 */
	protected  AbstractDatagram tccTotalTransactionEntry(AbstractDatagram param, Long startInterval){
		//记录入口报文日志
		LoggerUtil.info("入口报文 | ServiceName:{} |OuterSerialNumber【{}】| reqMap={}", this.getClass().getSimpleName(),StringUtil.formatOuterSerialNumber(param), JsonTransferUtil.toJson(param));

		storeScene(param);
		
		//创建交易上下文
		TccTranCtx ctx = createTccTranCtx();
		
		AbstractDatagram ret = null;

		//是否限流
		try {
			ret = RateLimiterUtil.handleRateLimited(param);
			if(PlatConstant.RSPVALUE.NOTSET  != ret) {
				//返回报文前处理
				doFinish(param, startInterval, ret);
				return ret;
			}
		} catch (FabRateLimiterException e) {	
			LoggerUtil.info("服务【{}】限流 | OuterSerialNumber【{}】", this.getClass().getSimpleName(), StringUtil.formatOuterSerialNumber(param));	
			ExitBusinessCommon tmp = ResponseHelper.createDefaultErrorRespone(null, null);
			tmp.setRspCode("SPS141");
            tmp.setRspMsg(e.getMessage());

			//返回报文前处理
			doFinish(param, startInterval, tmp);

			return tmp;			
		}

		//校验入参合法性
		ret = onValidate(param);
		if(PlatConstant.RSPVALUE.NOTSET != ret) {//校验报文出错则返回错误报文

			//返回报文前处理
			doFinish(param, startInterval, ret);

			return ret;
		}

		try {
			param.setCtx(ctx);//给报文设置上下文
			//TCC总事务
			TccBeanExecuter fabTcc = TccBeansHelper.getTccBean(this, true);
			LoggerUtil.info("进入TCC总事务正常业务入口 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
			ret = fabTcc.prepare(ctx.getTc(), ctx.getIgnore(), param.getRouteId(), param.getProtocalHashCode(), param);//tcc入口
			LoggerUtil.info("完成TCC总事务业务正常流程 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));

			//设置返回报文
			if(PlatConstant.RSPVALUE.NOTSET == ret) {
				ret = ResponseHelper.createSuccessRespone(ctx.getBid(), ctx.getTranDate());
			}else {
				if(null == ret.getValue("rspCode")) {
					ret.setValue("serSeqNo", ctx.getBid());
					ret.setValue("tranDate", ctx.getTranDate());
					ret.setValue("rspCode", PlatConstant.RSPCODE.OK);
					ret.setValue("rspMsg", PlatConstant.RSPMSG.OK);
				}else if(0 == PlatConstant.RSPCODE.IDEMPOTENCY.compareToIgnoreCase((String)ret.getValue("rspCode"))) {
					ret.setValue("rspCode", PlatConstant.RSPCODE.OK);
					ret.setValue("rspMsg", PlatConstant.RSPMSG.OK);
				}
			}
		}  catch (Exception e) {
			Exception exp = e;
	        if(e instanceof DtfCommitException) {
                LoggerUtil.error("dtf main二阶段出现异常| OuterSerialNumber【{}】：{}；", StringUtil.formatOuterSerialNumber(param), exp);
	        }else if(e instanceof DtfException) {
				DtfException dtfExp = (DtfException)e;
				exp = (Exception)dtfExp.getBizThrowable();
				if(null == exp) { 
					exp = e;
				}else {
					LoggerUtil.error("TCC阶段业务异常| OuterSerialNumber【{}】：{}；", StringUtil.formatOuterSerialNumber(param), dtfExp);
				}
			}
	        
			Pair<String, String> x = LoggerUtil.logException(StringUtil.formatOuterSerialNumber(param), SceneUtil.getSceneFromThreadLocal(), exp, param);

			//设置返回报文
			ExitBusinessCommon tmp = ResponseHelper.createDefaultErrorRespone(ctx.getBid(), ctx.getTranDate());
			if(!(e instanceof DtfCommitException)) {
    			tmp.setRspCode(x.getFirst());
    			tmp.setRspMsg(x.getSecond());
			}
			ret = tmp;
		}
		
		//返回报文前处理
		doFinish(param, startInterval, ret);

		return ret;
	}
	
	/**
	 * 创建本次交易的TCC服务交易上下文；
	 * @return TccTranCtx对象实例；
	 */
	protected TccTranCtx createTccTranCtx() {
		TccTranCtx ctx = new TccTranCtx();
		ctx.setBid(GuidUtil.getUuidSequence());
		if(null != Request.getThreadLocal())
			ctx.setSrcSystem(Request.getThreadLocal().getClient());
		else
			ctx.setSrcSystem("LocalInvoke");
		ctx.setTranDate(new Date());
		ctx.setTc(null);//启动TCC新的总事务，要求为null
		ctx.setIgnore(false);//总事务的流程不能忽略
		CtxUtil.setCtx(ctx);
		invoker.setContext(ctx);
		return ctx;
	}

	/**
	 * 调用远程rsf的tcc服务；
	 * @param srvClass TccServiceTemplate类型服务；
	 * @param param prepare()的入参；
	 * @return 返回IDatagram结果；
	 * @throws FabException 业务异常；
	 */
	protected AbstractDatagram invoke(Class<? extends ServiceTemplate> srvClass, AbstractDatagram param) throws FabException {
		return invoker.invoke(srvClass, param);
	}

}

package com.suning.fab.tup4ml.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.common.IFabRsfService;
import com.suning.fab.model.domain.entity.IBaseDao;
import com.suning.fab.model.domain.protocal.ExitBusinessCommon;
import com.suning.fab.tup4ml.ctx.LocalTranCtx;
import com.suning.fab.tup4ml.ctx.TranCtx;
import com.suning.fab.tup4ml.db.IdempotencyCtrlHandler;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.exception.FabRateLimiterException;
import com.suning.fab.tup4ml.utils.CtxUtil;
import com.suning.fab.tup4ml.utils.GuidUtil;
import com.suning.fab.tup4ml.utils.HashUtil;
import com.suning.fab.tup4ml.utils.JsonTransferUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.RateLimiterUtil;
import com.suning.fab.tup4ml.utils.StringUtil;
import com.suning.rsf.statistic.Request;

/**
 * 标准RSF服务模板，一个该模板服务会创建本地DB事务并调用0~N个本地服务，不能参与TCC，只能调用本地服务模板；
 * 请使用callLocalService()方法调用本地服务；
 * @author 16030888
 *
 */
public abstract class RsfServiceStandTemplate extends ServiceTemplate implements IFabRsfService {

	public RsfServiceStandTemplate(){
	}

	@Override
	public Map<String, Object> execute(Map<String, Object> reqMsg) {
		//起始计数
		Long startInterval = System.currentTimeMillis();
		boolean initErrorFlag = false;
		try {
			LoggerUtil.info("Map格式入口报文：{}", JsonTransferUtil.toJson(reqMsg));
			AbstractDatagram inputParam = null;
			try{
			    inputParam = convertMap2Datagram(reqMsg);
			}catch(IllegalArgumentException ex){
			    // 如果出现参数检查异常则设置一个标志，继续往上抛异常
			    initErrorFlag = true;
			    throw ex;
			}
			Map<String, Object> out = convertDatagram2Map(execute(inputParam));
			LoggerUtil.info("Map格式出口报文：{}", JsonTransferUtil.toJson(out));	
			return out;
		}catch(Exception e){//这个Map异常里面没法更新报文，Map格式不兼容，没有routeId
			LoggerUtil.info("未知异常：{}", e);	
			Map<String, Object> response = new HashMap<>();
			if(null != e.getMessage()) {
				response.put("rspMsg", e.getMessage());
			}else{
				Throwable exp = e.getCause();
				while((null != exp) && (null == exp.getMessage())) {
					exp = exp.getCause();
				}
				if((null != exp) && (null != exp.getMessage())) {
					response.put("rspMsg", exp.getMessage());
				}else{
					response.put("rspMsg", PlatConstant.RSPMSG.UNKNOWN);
				}
			}
			response.put("rspCode", PlatConstant.RSPCODE.UNKNOWN);
			if(initErrorFlag){
			    response.put("rspCode", PlatConstant.RSPCODE.VALIDATEERROR);
			}
			response.put("tranDate", "unknown");
			response.put("tranTime", "unknown");
			response.put("serSeqNo", "unknown");

			//清理工作
			onClean();

			//打印monitor日志
			String tranCode = this.getClass().getSimpleName();
			LoggerUtil.logMonitor(tranCode, (String)reqMsg.get("serialNo"),(String)response.get("serSeqNo")
					, (String)reqMsg.get("channelId"), (String)response.get("rspCode")
					, (String)response.get("rspMsg"), startInterval);		

			//记录出口报文日志
            String rspMsg = (String) response.get("rspMsg");
            if (null != rspMsg && rspMsg.length() > 42) {
                response.put("rspMsg", StringUtil.subString(rspMsg));
            }

			LoggerUtil.info("Map格式出口报文rspMap={}", JsonTransferUtil.toJson(response));

			return response;
		}
	}

	@Override
	public AbstractDatagram execute(AbstractDatagram param){
		//起始计数
		Long startInterval = System.currentTimeMillis();
		try {
			return executeEntry(param, startInterval);
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

	/**
	 * 公共入口，处理最外部异常、写monitor日志等；<br/>
	 * 启动本地DB事务管理器，并触发应用层入口prepare()；
	 * @param param 外部传过来的报文；
	 * @param startInterval 计数开始；
	 * @return 返回报文；
	 */
	protected AbstractDatagram executeEntry(AbstractDatagram param, Long startInterval){
		//记录入口报文日志
		LoggerUtil.info("入口报文 | ServiceName:{} |OuterSerialNumber【{}】| reqMap={}", this.getClass().getSimpleName(),StringUtil.formatOuterSerialNumber(param), JsonTransferUtil.toJsonWithOutSecret(param));
		
	    storeScene(param);
		
		//创建交易上下文
		LocalTranCtx ctx = createLocalTranCtx();
		
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
		if(PlatConstant.RSPVALUE.NOTSET  != ret) {//校验报文出错则返回错误报文

			//返回报文前处理
			doFinish(param, startInterval, ret);

			return ret;
		}

		//处理业务流程
		param.setCtx(ctx);//给报文设置上下文
		ret = runLocalDBTransaction(
				param, 
				(AbstractDatagram arg) -> this.doIdempotency(arg), 
				(AbstractDatagram arg) -> {
					//跑业务流程
					LoggerUtil.info("进入正常业务入口 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					AbstractDatagram retVal = this.prepare(arg);
					LoggerUtil.info("完成业务正常流程，预处理数据保存 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					if(null != Repository.getItems()) {
						for(IBaseDao iBaseDao: Repository.getItems()){
							iBaseDao.save();
						}
					}
					return retVal;
				},
				(AbstractDatagram arg) -> this.onIdempotency(arg), 
				Boolean.FALSE);

		//处理业务返回的报文
		if(PlatConstant.RSPVALUE.NOTSET == ret) {
			ret = ResponseHelper.createSuccessRespone(ctx.getBid(), ctx.getTranDate());
		}else {
			if(null == ret.getValue("rspCode")) {
				ret.setValue("serSeqNo", ctx.getBid());
				ret.setValue("tranDate", ctx.getTranDate());
				ret.setValue("rspCode", PlatConstant.RSPCODE.OK);
				ret.setValue("rspMsg", PlatConstant.RSPMSG.OK);
			}else {
				String rspCode = (String)ret.getValue("rspCode");
				if(0 == PlatConstant.RSPCODE.IDEMPOTENCY.compareToIgnoreCase(rspCode)) {
					ret.setValue("rspCode", PlatConstant.RSPCODE.OK);
					ret.setValue("rspMsg", PlatConstant.RSPMSG.OK);
				}else if(0 == PlatConstant.RSPCODE.OK.compareToIgnoreCase(rspCode)) {
					ret.setValue("serSeqNo", ctx.getBid());
					ret.setValue("tranDate", ctx.getTranDate());
					ret.setValue("rspCode", PlatConstant.RSPCODE.OK);
					ret.setValue("rspMsg", PlatConstant.RSPMSG.OK);
				}else {
					ret.setValue("serSeqNo", ctx.getBid());
					ret.setValue("tranDate", ctx.getTranDate());
				}
			}
		}

		//返回报文前处理
		doFinish(param, startInterval, ret);

		return ret;		
	}

	/**
	 * 创建本次交易的本地服务交易上下文；
	 * @return LocalTranCtx对象实例；
	 */
	protected LocalTranCtx createLocalTranCtx() {
		LocalTranCtx ctx = new LocalTranCtx();
		ctx.setBid(GuidUtil.getUuidSequence());
		if(null != Request.getThreadLocal())
			ctx.setSrcSystem(Request.getThreadLocal().getClient());
		else
			ctx.setSrcSystem("LocalInvoke");
		ctx.setTranDate(new Date());
		CtxUtil.setCtx(ctx);
		invoker.setContext(ctx);
		return ctx;
	}

	/**
	 * 操作幂等表，如果抛出幂等异常，平台层会默认处理
	 * @param param 入参；
	 * @return true -- 表示幂等；false -- 表示其他；
	 * @throws Exception 通常是数据库异常；
	 */
	@Override
	protected Boolean doIdempotency(AbstractDatagram param) throws FabException {
		LoggerUtil.info("幂等判断 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
		//先select幂等表，非幂等插入幂等表
		TranCtx ctx = CtxUtil.getCtx();
		IdempotencyCtrlHandler idempotencyHandler = new IdempotencyCtrlHandler();
		
        idempotencyHandler.setUserno(param.getRouteId());
        idempotencyHandler.setHashcode(HashUtil.getMD5(param.getProtocalHashCode()));
        idempotencyHandler.load();
		
        if(null != idempotencyHandler.getSerseqno()){
            return Boolean.TRUE;
        }else{
            idempotencyHandler.setUserno(param.getRouteId());
            idempotencyHandler.setSerseqno(ctx.getBid());
            idempotencyHandler.setTrandate(ctx.getTranDate());
            idempotencyHandler.setHashcode(HashUtil.getMD5(param.getProtocalHashCode()));
            idempotencyHandler.save();
        }
		
		return Boolean.FALSE;
	}

	/**
	 * 幂等时，本地DB事务回滚前获取幂等相关信息，主要是以前的业务流水号和交易日期；
	 * @param param 入参；
	 * @return 返回幂等报文；
	 */
	@Override
	protected AbstractDatagram onIdempotency(AbstractDatagram param) throws FabException{
		//从幂等表获取业务流水号及交易日期；
		IdempotencyCtrlHandler idempotencyHandler = new IdempotencyCtrlHandler();
		idempotencyHandler.setUserno(param.getRouteId());
		idempotencyHandler.setHashcode(HashUtil.getMD5(param.getProtocalHashCode()));
		idempotencyHandler.load();
		return ResponseHelper.createIdempotencyRespone(idempotencyHandler.getSerseqno(), idempotencyHandler.getTrandate());
	}

	/**
	 * 调用本地服务；
	 * @param srvClass LocalServiceTemplate类型服务；
	 * @param param prepare()的入参；
	 * @return 返回IDatagram结果；
	 * @throws FabException 业务异常；
	 */
	protected AbstractDatagram invoke(Class<? extends ServiceTemplate> srvClass, AbstractDatagram param) throws FabException {
		return invoker.invoke(srvClass, param);
	}

}

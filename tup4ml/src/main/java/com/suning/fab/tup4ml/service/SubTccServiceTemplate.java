package com.suning.fab.tup4ml.service;

import java.util.Date;
import java.util.Map;

import com.suning.dtf.common.exception.DtfCommitException;
import com.suning.dtf.common.exception.DtfException;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.common.IFabRsfService;
import com.suning.fab.model.domain.entity.IBaseDao;
import com.suning.fab.model.domain.protocal.ExitBusinessCommon;
import com.suning.fab.tup4ml.ctx.TccTranCtx;
import com.suning.fab.tup4ml.ctx.TranCtx;
import com.suning.fab.tup4ml.db.IdempotencyCtrlHandler;
import com.suning.fab.tup4ml.elfin.Pair;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.utils.CtxUtil;
import com.suning.fab.tup4ml.utils.HashUtil;
import com.suning.fab.tup4ml.utils.JsonTransferUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.SceneUtil;
import com.suning.fab.tup4ml.utils.StringUtil;

/**
 * TCC服务模板，TCC总事务只能调用TCC子事务，TCC子事务只能调用本地服务模板；
 * 请使用callLocalService()方法调用本地服务实现子事务服务；
 * 请使用callRemoteService()方法调用远程TCC子事务的服务；
 * 改模板子类必须是单例，一方面是RSF机制如此，另一方面是TCC的使用机制要求；
 * @author 16030888
 *
 */
public abstract class SubTccServiceTemplate extends ServiceTemplate implements IFabRsfService {	

	public SubTccServiceTemplate(){
		//nothing
	}

	@Override
	public Map<String, Object> execute(Map<String, Object> reqMsg) {
		throw new IllegalStateException("SubTccServiceTemplate execute(Map<String, Object> ) is not allow");
	}

	@Override
	public AbstractDatagram execute(AbstractDatagram param){
		//起始计数
		Long startInterval = System.currentTimeMillis();
		try {
			return tccSubTransactionEntry(param, startInterval);
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
            doFinishWithOutInterMsg(param, startInterval, response);

			return response;
		}
	}

	/**
	 * TCC子事务公共入口，处理业务异常、写monitor日志等；<br/>
	 * 启动本地DB事务管理器，并触发TCC入口prepare()；
	 * @param param 外部传过来的参数；
	 * @param startInterval 开始计数；
	 * @return 返回应用层提供的AbstractDatagram类；如果没指定或者有异常则返回默认的ExitProtoCommon类；
	 */
	private  AbstractDatagram tccSubTransactionEntry(AbstractDatagram param, Long startInterval){
		//记录入口报文日志
		LoggerUtil.info("入口报文 |ServiceName:{}| OuterSerialNumber【{}】| reqMap={}", this.getClass().getSimpleName() ,StringUtil.formatOuterSerialNumber(param), JsonTransferUtil.toJson(param));
		
		storeScene(param);
        
		//TCC子事务从外部传参获取相关上下文；
		TccTranCtx ctx = (TccTranCtx)param.getCtx();
		ctx.setInitSubSeq(ctx.getSubSeq());
		CtxUtil.setCtx(ctx);
		invoker.setContext(ctx);

		//校验入参合法性
		AbstractDatagram ret = onValidate(param);
		if(PlatConstant.RSPVALUE.NOTSET != ret) {//校验报文出错则返回错误报文

			//返回报文前处理
            doFinishWithOutInterMsg(param, startInterval, ret);

			return ret;
		}

		//处理业务流程，this.doIdempotency()做幂等判断；this.prepare()做正常业务流程；this.onIdempotency()做幂等时默认操作；
		try {
			TccBeanExecuter fabTcc = TccBeansHelper.getTccBean(this, false);
			ret = fabTcc.prepare(ctx.getTc(), ctx.getIgnore(), param.getRouteId(), param.getProtocalHashCode(), param);//tcc入口

			//设置返回报文
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
		} catch (Exception e) {
			Exception exp = e;
			if(e instanceof DtfCommitException) {
                LoggerUtil.error("dtf sub二阶段出现异常| OuterSerialNumber【{}】：{}；", StringUtil.formatOuterSerialNumber(param), exp);
            }else if(e instanceof DtfException) {
				DtfException dtfExp = (DtfException)e;
				exp = (Exception)dtfExp.getBizThrowable();
				if(null == exp) { 
					exp = e;
				}else {
					LoggerUtil.error("子TCC出现异常 | OuterSerialNumber【{}】：{}；", StringUtil.formatOuterSerialNumber(param), dtfExp);
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
        doFinishWithOutInterMsg(param, startInterval, ret);

		return ret;
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

	/**
	 * 操作幂等表，如果抛出幂等异常，平台层会默认处理
	 * @param param 入参；
	 * @return true -- 表示幂等；false -- 表示其他；
	 * @throws Exception 通常是数据库异常；
	 */
	@Override
	protected Boolean doIdempotency(AbstractDatagram param) throws FabException {
		LoggerUtil.info("执行TCC子事务幂等判断 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
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
	 * 幂等时，不立即回滚本地DB事务，而是继续执行别的流程的方法；
	 * @param param
	 * @return
	 */
	@Override
	protected AbstractDatagram onIdempotency(AbstractDatagram param) throws FabException{
		//子TCC的每一步都幂等，所以操作幂等表遇到幂等后继续其流程；
		LoggerUtil.info("进入TCC子事务正常业务入口 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
		AbstractDatagram retVal = this.prepare(param);
		LoggerUtil.info("完成TCC子事务业务正常流程，预处理数据保存 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
		if(null != Repository.getItems()) {
			for(IBaseDao iBaseDao: Repository.getItems()){
				iBaseDao.save();
			}
		}
		return retVal;
	}

}

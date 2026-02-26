package com.suning.fab.tup4ml.service;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.domain.protocal.ExitBusinessCommon;
import com.suning.fab.tup4ml.ctx.TranCtx;
import com.suning.fab.tup4ml.db.ProtoRegHandler;
import com.suning.fab.tup4ml.elfin.Pair;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.elfin.SceneInfo;
import com.suning.fab.tup4ml.elfin.ServiceFactory;
import com.suning.fab.tup4ml.elfin.TransTimeController;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.exception.FabSqlException;
import com.suning.fab.tup4ml.scmconf.ScmDynaGetterUtil;
import com.suning.fab.tup4ml.utils.CtxUtil;
import com.suning.fab.tup4ml.utils.DateUtil;
import com.suning.fab.tup4ml.utils.GuidUtil;
import com.suning.fab.tup4ml.utils.JsonTransferUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.SceneUtil;
import com.suning.fab.tup4ml.utils.StringUtil;
import com.suning.fab.tup4ml.utils.ThreadLocalUtil;

public abstract class ServiceTemplate implements IFabEntry {
	@Autowired(required=false)
	protected IInvoker invoker;

    @Autowired
    private IValidator validator;

	public IInvoker getInvoker() {
		return invoker;
	}

	public void setInvoker(IInvoker invoker) {
		this.invoker = invoker;
	}

	/**
	 * 把Map转换成AbstractDatagram对象；
	 * @param reqMsg Map对象；
	 * @return AbstractDatagram对象；
	 */
	protected AbstractDatagram convertMap2Datagram(Map<String, Object> reqMsg) {
		throw new IllegalStateException("convertMap2Datagram() is empty");
	}

	/**
	 * 把AbstractDatagram对象转换成Map；
	 * @param datagram AbstractDatagram对象；
	 * @return Map对象；
	 */
	protected Map<String, Object> convertDatagram2Map(AbstractDatagram datagram) {
		throw new IllegalStateException("convertDatagram2Map() is empty");
	}

	/**
	 * 幂等操作；
	 * @param param 入参；
	 * @return true -- 表示幂等；false -- 表示其他；
	 * @throws Exception 幂等相关的异常；
	 */
	protected Boolean doIdempotency(AbstractDatagram param) throws FabException {
		return Boolean.FALSE;
	}

	/**
	 * 应用校验预留方法,默认不实现任何逻辑；
	 * @param param 入参；
	 * @return true -- 表示验证成功；false -- 标示校验失败；
	 * @throws Exception 校验相关的异常；
	 */
	protected boolean validate(AbstractDatagram param) throws FabException {
        // 考核公共字段校验
        if ("YES".equals(ScmDynaGetterUtil.getWithDefaultValue(PlatConstant.SCMFILENAME.GLOBAL_SCM, PlatConstant.SCMFIELDNAME.ASSESS_FIELD_FLAG, "YES"))) {
            validator.validate(param, this.getClass());
        }

		return true;
	}

	/**
	 * 交易日期校验预留方法,默认实现对所有交易时间的控制
	 * @param param 入参；
	 * @return true -- 表示验证成功；false -- 标示校验失败；
	 * @throws Exception 校验相关的异常；
	 */
	protected boolean timeValidate(AbstractDatagram param) throws FabException {
		// 校验交易时间 ,需要登记报文后再判断
		return TransTimeController.validate(param);
	}

	/**
	 * 幂等时，本地DB事务回滚前处理；
	 * @param param 入参；
	 * @return 返回报文；
	 */
	protected AbstractDatagram onIdempotency(AbstractDatagram param) throws FabException{
		return PlatConstant.RSPVALUE.NOTSET;
	}

	/**
	 * 清理工作；
	 */
	protected void onClean() {
		//清理调用管理器的资源
		invoker.clean();
		CtxUtil.setCtx(null);
		GuidUtil.clean();	
		Repository.cleanItems();
		ThreadLocalUtil.clean();
	}

	/**
	 * 打印monitor日志；
	 * @param in 入参；
	 * @param out 出参；
	 * @param start 计时开始计数；
	 */
	protected void logMonitor(AbstractDatagram in, AbstractDatagram out, Long start) {
		//打印monitor日志；
		String tranCode = this.getClass().getSimpleName();
		LoggerUtil.logMonitor(tranCode, StringUtil.formatOuterSerialNumber(in)
				, (String)out.getValue("serSeqNo"),(String)in.getValue("channelId"), (String)out.getValue("rspCode")
				, (String)out.getValue("rspMsg"), start);		
	}

	/**
	 * 报文校验；
	 * @param param 入参；
	 * @return 如果报文校验通过则返回PlatConstant.RSPVALUE.NOTSET；否则返回TUP105报文；
	 */
	protected AbstractDatagram onValidate(AbstractDatagram param) {
		AbstractDatagram ret = PlatConstant.RSPVALUE.NOTSET ;
		try {
			LoggerUtil.info("校验入参报文合法性 | OuterSerialNumber【{}】 ", StringUtil.formatOuterSerialNumber(param));

			if(false == timeValidate(param)){
				throw new FabException(PlatConstant.RSPCODE.TRANS_TIME_VALIDATE_ERROR, SceneUtil.getSceneFromThreadLocal(), DateUtil.dateToString(param.getRequestDate()), Integer.parseInt(ScmDynaGetterUtil.getWithDefaultValue("GlobalScm.properties", "TransTimeInterval", "3")));
			}

			if(false == param.validate()){
				throw new FabException(PlatConstant.RSPCODE.VALIDATEERROR, "");
			}

			if(false == validate(param)){
				throw new FabException(PlatConstant.RSPCODE.APPLICATION_VALIDATE_ERROR);
			}

		}catch(FabException e) {
			LoggerUtil.error("校验异常 | OuterSerialNumber【{}】：{}", StringUtil.formatOuterSerialNumber(param), e);     
			ExitBusinessCommon tmp = ResponseHelper.createDefaultErrorRespone(null, null);
			tmp.setRspCode(e.getErrCode());
			tmp.setRspMsg(e.getMessage());
			ret = tmp;
		}catch(Exception e) {
			LoggerUtil.error("校验异常 | OuterSerialNumber【{}】：{}", StringUtil.formatOuterSerialNumber(param), e);     
			ExitBusinessCommon tmp = ResponseHelper.createDefaultErrorRespone(null, null);
			
			FabException fabException = new FabException(PlatConstant.RSPCODE.VALIDATEERROR, e.getMessage());
			tmp.setRspCode(fabException.getErrCode());
			tmp.setRspMsg(fabException.getErrMsg());
			ret = tmp;
		}
		return ret;
	}

	/**
	 * 登记报文；
	 * @param in 请求报文；
	 * @param out 返回报文；
	 * @return  如果报文登记正常则返回null；否则返回TUP106报文；
	 */
	protected AbstractDatagram onProtoReg(AbstractDatagram in, AbstractDatagram out) {
		AbstractDatagram ret = PlatConstant.RSPVALUE.NOTSET;
		try {
			LoggerUtil.info("登记报文 | OuterSerialNumber【{}】 ", StringUtil.formatOuterSerialNumber(in));
			ret = runLocalDBTransaction(
					in, 
					null,
					(AbstractDatagram arg) -> {
						//登记报文，写到DB里面
						TranCtx ctx = CtxUtil.getCtx();
						ProtoRegHandler regHandler = new ProtoRegHandler();
						regHandler.setUserno(arg.getRouteId());
						regHandler.setSerseqno(ctx.getBid());
						regHandler.setTrandate(ctx.getTranDate());
						regHandler.setHops(ctx.getInitSubSeq());
						regHandler.setSerialno(StringUtil.formatOuterSerialNumber(arg));
						regHandler.setRequest(arg);
						regHandler.setResponse(out);
						regHandler.save();
						return PlatConstant.RSPVALUE.NOTSET ;
					}, 
					null, 
					Boolean.FALSE);
		} catch (Exception e) {
			LoggerUtil.error("登记报文时DB事务异常 | OuterSerialNumber【{}】：{}", StringUtil.formatOuterSerialNumber(in), e);  
			ExitBusinessCommon tmp = ResponseHelper.createDefaultErrorRespone(null, null);
			tmp.setRspCode(PlatConstant.RSPCODE.PROTOREGERROR);
			tmp.setRspMsg(e.getMessage());
			ret = tmp;
		}
		return ret;
	}

	/**
	 * 完成请求时做些通用操作；
	 * @param in 请求报文；
	 * @param startInterval 计数开始；
	 * @param out 响应报文；
	 */
	protected void doFinish(AbstractDatagram in, Long startInterval, AbstractDatagram out) {

		//更新返回报文
		onProtoReg(in, out);

		//清理工作
		onClean();

		//打印monitor日志
		logMonitor(in, out, startInterval);

		StringUtil.interceptResponeMsg(out);

		//记录出口报文日志
		LoggerUtil.info("出口报文 | OuterSerialNumber【{}】| rspMap={}", StringUtil.formatOuterSerialNumber(in), JsonTransferUtil.toJsonWithOutSecret(out));
	}

    /**
     * 完成请求时做些通用操作，该操作中没有截取出口报文
     *
     * @param in            请求报文；
     * @param startInterval 计数开始；
     * @param out           响应报文；
     */
    protected void doFinishWithOutInterMsg(AbstractDatagram in, Long startInterval, AbstractDatagram out) {

        //更新返回报文
        onProtoReg(in, out);

        //清理工作
        onClean();

        //打印monitor日志
        logMonitor(in, out, startInterval);

        //记录出口报文日志
        LoggerUtil.info("出口报文 | OuterSerialNumber【{}】| rspMap={}", StringUtil.formatOuterSerialNumber(in), JsonTransferUtil.toJson(out));
    }


	/**
	 * 利用Function运行在本地DB事务中，以便完成本地DB事务的提交或回滚；<br/>
	 * 这里的本地，是相对于分布式服务来说的；
	 * @param param 入参；
	 * @param rollbackAnyway 是否总是回滚本地DB事务；
	 * @param prepare 正常业务方法；
	 * @param continueOnIdempotency 幂等后要继续提交本地DB事务的业务方法；
	 * @param beforeRollbackOnIdempotency 幂等后在本地DB事务回滚之前要做的方法；
	 * @return 返回给外部的报文；
	 * @throws FabReturnException 抛出给外层的失败报文；如果是TCC模式，该异常还用于通知TCC组件业务失败了；
	 */
	protected AbstractDatagram runLocalDBTransaction(
			AbstractDatagram param, 
			UncheckedFunction<AbstractDatagram, Boolean> doIdempotency, 
			UncheckedFunction<AbstractDatagram, AbstractDatagram> normal, 
			UncheckedFunction<AbstractDatagram, AbstractDatagram> onIdempotency,
			Boolean commitAnyway
			) {

		DataSourceTransactionManager txManager = (DataSourceTransactionManager) ServiceFactory.getBean("txManager");
		TransactionDefinition td = new DefaultTransactionDefinition();
		//设置DB事务超时，默认5秒
		Integer txTimeout = Integer.valueOf(ScmDynaGetterUtil.getWithDefaultValue(PlatConstant.SCMFILENAME.GLOBAL_SCM,"TxTimeout", "5"));
		txManager.setDefaultTimeout(txTimeout);
		TransactionStatus status = txManager.getTransaction(td);
		LoggerUtil.info("本地DB事务开始 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
		boolean commitTrans = false;

		AbstractDatagram ret = null;
		try {
			// 先检查是否是新的事务
			boolean tranFlag = status.isNewTransaction();
			if(!tranFlag){
				LoggerUtil.warn("this trans is using old transaction! | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
				throw new FabException("TUP113");
			}
			//先走幂等
			Boolean isIdempotency = Boolean.FALSE;
			try {
				if(null != doIdempotency) {
					isIdempotency = doIdempotency.apply(param);
				}else {
					isIdempotency = Boolean.FALSE;
				}
			} catch (FabSqlException e) {
				//不是幂等异常的，抛到异常里面
				if(false == e.isIntegrityViolationException()) {
					throw e;
				}else {
					isIdempotency = Boolean.TRUE;
				}
			}

			if(Boolean.FALSE == isIdempotency) {
				//正常流程（非幂等）
				if(null != normal) {
					ret = normal.apply(param);
					commitTrans = true;
				}else {
					ret = ResponseHelper.createDefaultErrorRespone("unknown", new Date());
				}
			}else {
				//幂等处理
				if(null != onIdempotency) {
					ret = onIdempotency(param);
					if(commitAnyway) {
						commitTrans = true;
					}
				}else {
					ret = ResponseHelper.createDefaultErrorRespone("unknown", new Date());
				}
			}
		} catch (Exception e) {
			commitTrans = false;
			Pair<String, String> x = LoggerUtil.logException(StringUtil.formatOuterSerialNumber(param), SceneUtil.getSceneFromThreadLocal(), e, param);

			ExitBusinessCommon tmp = ResponseHelper.createDefaultErrorRespone("unknown", new Date());
			tmp.setRspCode(x.getFirst());
			tmp.setRspMsg(x.getSecond());
			ret = tmp;
		}catch (Throwable throwable) {
			commitTrans = false;

			ExitBusinessCommon tmp = ResponseHelper.createDefaultErrorRespone("unknown", new Date());
			tmp.setRspCode(PlatConstant.RSPCODE.UNKNOWN);
			tmp.setRspMsg(PlatConstant.RSPMSG.UNKNOWN);
			ret = tmp;
			LoggerUtil.warn(throwable.getMessage());

		}finally {
			try {
				if(commitTrans) {
					LoggerUtil.info("开始本地DB事务提交 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					txManager.commit(status);
					LoggerUtil.info("本地DB事务提交完成 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));				
				}else {
					LoggerUtil.info("开始本地DB事务回滚 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
					txManager.rollback(status);
					LoggerUtil.info("本地DB事务回滚完成 | OuterSerialNumber【{}】", StringUtil.formatOuterSerialNumber(param));
				}
			} catch (Exception expTrans) {
				Pair<String, String> x = LoggerUtil.logException(StringUtil.formatOuterSerialNumber(param), SceneUtil.getSceneFromThreadLocal(), expTrans, param);

				ExitBusinessCommon tmp = ResponseHelper.createDefaultErrorRespone("unknown", new Date());
				tmp.setRspCode(x.getFirst());
				tmp.setRspMsg(x.getSecond());
				ret = tmp;
			}
		}
		return ret;
	}
	
	
	/**
	 * 获取场景
	 * 功能描述: <br>
	 * 
	 *
	 * @return
	 * @since 1.0
	 */
    protected SceneInfo getScene(AbstractDatagram param){
        return null;
    }
    
    /**
     * 
     * 功能描述: <br>
     * 将场景存入ThreadLocal中，以备给异常解析使用
     *
     * @param param
     * @since 1.0
     */
    protected void storeScene(AbstractDatagram param){
        SceneInfo sceneInfo = null;
        try{
            sceneInfo = getScene(param);
        }catch(Exception e){
            LoggerUtil.warn("get scene error");
        }
        SceneUtil.putInThreadLocal(sceneInfo, this.getClass());
		SceneUtil.putSerialNoInThreadLocal(StringUtil.getOuterSerialNumber(param));
    }
    
    
}

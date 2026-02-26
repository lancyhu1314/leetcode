package com.suning.fab.tup4ml.service;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.domain.entity.IBaseDao;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.utils.JsonTransferUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.StringUtil;

/**
 * 本地服务模板；
 * 请使用pushToRepository()方法对IBaseDao保存数据；
 * 本地服务不能在多个线程中使用，只能跟其调用者所在线程里面使用，因为要跟本地DB事务管理器在同一个线程中；
 * 该模板如果execute()或者prepare()里面抛出异常，不要自己catch，因为这里不一定是直接返回给外部的报文；
 * @author 16030888
 *
 */
public abstract class LocalServiceTemplate extends ServiceTemplate {
	protected AbstractDatagram execute(AbstractDatagram param) throws FabException {
		LoggerUtil.info(" OuterSerialNumber【{}】| reqLocal={}", StringUtil.formatOuterSerialNumber(param), JsonTransferUtil.toJsonWithOutSecret(param));
		
		if(false == param.validate()){
            throw new FabException(PlatConstant.RSPCODE.VALIDATEERROR, "");
        }
		
        if(false == validate(param)){
            throw new FabException(PlatConstant.RSPCODE.APPLICATION_VALIDATE_ERROR);
        }
		
		AbstractDatagram ret = prepare(param);
		LoggerUtil.info(" OuterSerialNumber【{}】| rspLocal={}", StringUtil.formatOuterSerialNumber(param), JsonTransferUtil.toJsonWithOutSecret(ret)); 
		return ret;
	}

	/**
	 * 提供一个明确方便的方法，用于对跟本类相关的IBaseDao对象进行save()操作；
	 * @param item IBaseDao数据库读写接口；
	 * @return 成功返回true；失败返回false;
	 */
	protected boolean pushToRepository(IBaseDao item){
		return Repository.push(item);
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


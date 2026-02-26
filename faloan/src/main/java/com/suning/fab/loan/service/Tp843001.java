package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    
	* 
	*@version   V1.0.0
	*
	*@see       
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*1.此交易处理由KafkaConsumerService程序触发
	*/
@Scope("prototype") 
@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faload-xxx")
public class Tp843001 extends ServiceTemplate {

	/*@Autowired Epp225 epp225;*/
	public Tp843001() {
		/*不生产账务流水号*/
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception,FabException {
		
		/**
		 * 业务处理流程
		 * 1.二次清算处理
		 */
		
		/*ctx.setSerSeqNo(Integer.valueOf(ctx.getRequestDict("oldSerialNo").toString())); //账务流水
		trigger(epp225,"map225");*/
	}
	
	@Override
	protected void special() throws FabException {
		/*目前方法保留为空*/
	}
}

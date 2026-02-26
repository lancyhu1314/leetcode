package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns423;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：汽车租赁明细查询
 *
 * @author 
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype") 
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-queryCarAccountDetail")
public class Tp475010 extends ServiceTemplate{

	public Tp475010(){
		needSerSeqNo = false;
	}
	
	@Autowired
	Lns423 lns423;
	
	@Override
	protected void run() throws FabException, Exception {
		trigger(lns423);
		
	}

	@Override
	protected void special() throws FabException, Exception {
		/*Reserved*/
		
	}

}

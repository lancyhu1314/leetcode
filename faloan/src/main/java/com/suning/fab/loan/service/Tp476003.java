package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns414;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：非标准还款计划试算
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-nonstdPlanTentativeCalculation")
public class Tp476003 extends ServiceTemplate {

	@Autowired Lns414 lns414;
	
	public Tp476003() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		trigger(lns414);
	}
	@Override
	protected void special() throws Exception {
		/*Reserved*/ 	
	}
}

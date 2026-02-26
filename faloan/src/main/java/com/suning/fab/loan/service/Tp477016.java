
package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns413;
import com.suning.fab.loan.workunit.Lns418;
import com.suning.fab.loan.workunit.Lns419;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    HY
	* 
	*@version   V1.0.0
	*
	*@see       新还款计划查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repayPlanQuery")
public class Tp477016 extends ServiceTemplate {

	@Autowired Lns413 lns413;
	@Autowired Lns418 lns418;
	@Autowired Lns419 lns419;
	public Tp477016() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		//lnsrepayplan 已经没有数据了
		  // trigger(lns418);
		   trigger(lns413);

//		trigger(lns419);
	     
		
	}
	@Override
	protected void special() throws Exception {
		//nothing to do
	}
}

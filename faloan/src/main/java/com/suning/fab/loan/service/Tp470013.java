package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns436;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    SC
	* 
	*@version   V1.0.0
	*
	*@see       汽车租赁预约还款查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-carPayCalculation")
public class Tp470013 extends ServiceTemplate {

	@Autowired 
	Lns436 lns436;
	
	public Tp470013() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		trigger(lns436);
	}
	@Override
	protected void special() throws Exception {
		//nothing to do
	}
}

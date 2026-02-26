package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns407;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    AY
	* 
	*@version   V1.0.0
	*
	*@see       预收账户明细查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-queryAdvanceAccountDetail")
public class Tp475003 extends ServiceTemplate {

	@Autowired 
	Lns407 lns407;
	
	public Tp475003() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		Map<String, Object> param = new HashMap<String,Object>();
		param.put("intertype", "PROVISION");
		
		trigger(lns407, "DEFAULT", param);
	}
	@Override
	protected void special() throws Exception {
		//nothing to do
	}
}

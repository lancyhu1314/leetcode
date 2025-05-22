package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns409;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    AY
	* 
	*@version   V1.0.0
	*
	*@see       利息计提明细查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-queryInterestAccruedDetail")
public class Tp475005 extends ServiceTemplate {

	@Autowired 
	Lns409 lns409;
	
	public Tp475005() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		Map<String, Object> param = new HashMap<String,Object>();
		param.put("intertype", ConstantDeclare.INTERTYPE.PROVISION);
		
		trigger(lns409, "DEFAULT", param);
	}
	@Override
	protected void special() throws Exception {
		//nothing to do
	}
}

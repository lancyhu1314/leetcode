package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns404;
import com.suning.fab.loan.workunit.Lns419;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;


/**
	*@author    AY
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
	*/
@Scope("prototype") 
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-loanRepayplanTransplant")
public class Tp470001 extends ServiceTemplate {

	@Autowired Lns419 lns419;
	
	public Tp470001() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		trigger(lns419);
	}
	
	@Override
	protected void special() throws Exception {
	}
}

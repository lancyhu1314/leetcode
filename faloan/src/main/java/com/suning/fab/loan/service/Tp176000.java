package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns198;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;


/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       预收账号查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-advanceAccountQuery")
public class Tp176000 extends ServiceTemplate {

	@Autowired
    Lns198 lns198;
    public Tp176000() {
        needSerSeqNo=false;
    }
	@Override
	protected void run() throws Exception {
		trigger(lns198);
	}
	@Override
	protected void special() throws Exception {
	}
}

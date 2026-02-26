package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns450;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;


/**
	*@author    AY
	* 
	*@version   V1.0.0
	*
	*@see       贷款账户统计查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-PPIncomeCalculation")
public class Tp470006 extends ServiceTemplate {

	@Autowired 
	Lns450 lns450;
	
	public Tp470006() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		trigger(lns450);
	}
	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}
}

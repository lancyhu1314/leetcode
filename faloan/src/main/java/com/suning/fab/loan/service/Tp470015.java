package com.suning.fab.loan.service;

import com.suning.fab.loan.workunit.Lns121;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns116;
import com.suning.fab.loan.workunit.Lns603;
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
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-RepaydateModify")
public class Tp470015 extends ServiceTemplate {

	@Autowired 
	Lns603 lns603;
	
	@Autowired 
	Lns116 lns116;
	@Autowired
	Lns121 lns121;//考核数据校验 入库

	public Tp470015() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {

		trigger(lns116);
		trigger(lns603);
		//防止幂等报错被拦截
		trigger(lns121);
	}
	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}
}

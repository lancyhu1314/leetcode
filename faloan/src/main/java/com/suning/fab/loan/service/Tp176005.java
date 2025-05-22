package com.suning.fab.loan.service;

import com.suning.fab.loan.workunit.Lns121;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns111;
import com.suning.fab.loan.workunit.Lns246;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;


/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       汽车租赁账户充值
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-carAccountCharge")
public class Tp176005 extends ServiceTemplate {

	@Autowired 
	Lns111 lns111;
	
	@Autowired 
	Lns246 lns246;
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	public Tp176005() {
		needSerSeqNo=true;
	}
	@Override
	protected void run() throws Exception {

		//汽车租赁账户开户
		trigger(lns111);
		//汽车租赁账户充值
		trigger(lns246);
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

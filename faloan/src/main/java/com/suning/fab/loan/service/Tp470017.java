package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


/**
	*@author    SC
	* 
	*@version   V1.0.0
	*
	*@see       展期
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-ExpandDate")
public class Tp470017 extends ServiceTemplate {

	@Autowired 
	Lns116 lns116;
	
	@Autowired 
	Lns606 lns606;
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	@Autowired
	Lns607 lns607;
	@Autowired
	Lns608 lns608;
	public Tp470017() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		trigger(lns607);
		trigger(lns608);
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

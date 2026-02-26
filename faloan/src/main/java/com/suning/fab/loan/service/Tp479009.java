package com.suning.fab.loan.service;

import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       贷款调账
	*
	*@param     briefCode 调账类型:摘要码
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-errorAdjustment")
public class Tp479009 extends ServiceTemplate {

	@Autowired Lns225 lns225;
	@Autowired Lns226 lns226;
	@Autowired Lns227 lns227;
	@Autowired Lns228 lns228;
	@Autowired Lns516 lns516;
	@Autowired
	Lns121 lns121;//考核数据校验 入库

	public Tp479009() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		trigger(lns516);
		
		if (VarChecker.asList(ConstantDeclare.BRIEFCODE.FXHB).contains(ctx.getRequestDict("briefCode"))) {
			trigger(lns225);
		} else if (VarChecker.asList(ConstantDeclare.BRIEFCODE.LXHB).contains(ctx.getRequestDict("briefCode"))) {
			trigger(lns226);
		} else if (VarChecker
				.asList(ConstantDeclare.BRIEFCODE.KHZJ, ConstantDeclare.BRIEFCODE.KHZK, ConstantDeclare.BRIEFCODE.KHZZ)
				.contains(ctx.getRequestDict("briefCode"))) {
			trigger(lns227);
		} else if (VarChecker.asList(ConstantDeclare.BRIEFCODE.YSZJ, ConstantDeclare.BRIEFCODE.YSJS,ConstantDeclare.BRIEFCODE.CKZJ, ConstantDeclare.BRIEFCODE.CKJS)
				.contains(ctx.getRequestDict("briefCode"))) {
			trigger(lns228);
		}
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

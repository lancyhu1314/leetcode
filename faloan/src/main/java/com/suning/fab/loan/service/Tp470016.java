package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

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
	*@author    SC
	*
	*@version   V1.0.0
	*
 	*   修改利率
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-AllKindRateModify")
public class Tp470016 extends ServiceTemplate {

	@Autowired 
	Lns605 lns605;
	@Autowired Lns512 lns512; 
	@Autowired Lns501 lns501; 
	@Autowired Lns116 lns116;

	@Autowired
	Lns121 lns121;//考核数据校验 入库

	public Tp470016() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		trigger(lns116);
        //结息本息
        Map<String, Object> param = new HashMap<>();
        param.put("repayDate", ctx.getTranDate());
        trigger(lns501, "map501", param);

		trigger(lns605);
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

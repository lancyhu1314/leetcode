package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.workunit.Lns401;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：按揭贷款还款计划试算
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repayPlanTentativeCalculation")
public class Tp476002 extends ServiceTemplate {

	@Autowired Lns401 lns401;
	
	public Tp476002() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		
		String productCode = "1212605";	//默认产品代码
		if(!VarChecker.isEmpty(ctx.getRequestDict("productCode"))){
			productCode = ctx.getRequestDict("productCode").toString();
		}
		lns401.setLoanAgreement(LoanAgreementProvider.genLoanAgreement(productCode));
		
		trigger(lns401, "map401");
	}
	@Override
	protected void special() throws Exception {
		/*Reserved*/ 	
	}
}

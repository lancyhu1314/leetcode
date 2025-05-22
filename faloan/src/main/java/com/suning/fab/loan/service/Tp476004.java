package com.suning.fab.loan.service;

import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.workunit.Lns401;
import com.suning.fab.loan.workunit.Lns600;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：房抵贷计划试算
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-housePlanTentativeCalculation")
public class Tp476004 extends ServiceTemplate {

	
	@Autowired 
	Lns600 lns600;
	
	public Tp476004() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		if(VarChecker.isEmpty(ctx.getProductCode()))
			//默认产品代码
			lns600.setLoanAgreement(LoanAgreementProvider.genLoanAgreement("2412615"));
		else
			lns600.setLoanAgreement(LoanAgreementProvider.genLoanAgreement(ctx.getProductCode()));

		
		trigger(lns600, "map600");
	}
	@Override
	protected void special() throws Exception {
		/*Reserved*/ 	
	}
}

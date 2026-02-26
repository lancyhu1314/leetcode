package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.workunit.Lns431;
import com.suning.fab.loan.workunit.Lns432;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：通讯租赁试算
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-messageLeaseCalculation")
public class Tp476001 extends ServiceTemplate {
	//计算租金总本息和总本金，尾款总本息和总本金
	@Autowired Lns431 lns431;
	//还款计划生成
	@Autowired Lns432 lns432;

	public Tp476001() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		String productCode;
		if( ctx.getProductCode() == null )
			productCode = "2412614";		//默认产品代码
		else
			productCode = ctx.getProductCode();	
		
		LoanAgreement la = LoanAgreementProvider.genLoanAgreement(productCode);
		
		lns431.setLoanAgreement(la);
		lns432.setLoanAgreement(la);

		trigger(lns431, "map431");

		lns432.setRepayWay("0");
		lns432.setFinalAmt(lns431.getFinalAmt());
		lns432.setRentAmt(lns431.getRentAmt());
		trigger(lns432);
	}
	@Override
	protected void special() throws Exception {
		/*Reserved*/ 	
	}
}

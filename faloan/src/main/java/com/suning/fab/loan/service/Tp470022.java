package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.workunit.Lns461;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉： 现金免息
 *
 * @Author 
 * @Date Created in 16:48 2019/04/17
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-interestCalculate")
public class Tp470022 extends ServiceTemplate {
	
	@Autowired Lns461 lns461;
	
    public Tp470022(){
        needSerSeqNo=false;
    }
    
	
    @Override
    protected void run() throws Exception {
		
		String productCode = "1212605";	//默认产品代码
		if(VarChecker.isEmpty(ctx.getRequestDict("ccy"))){
			throw new FabException("LNS055","ccy");
		}
		if(!VarChecker.isEmpty(ctx.getRequestDict("productCode"))){
			productCode = ctx.getRequestDict("productCode").toString();
		}
		lns461.setLoanAgreement(LoanAgreementProvider.genLoanAgreement(productCode));
		
		trigger(lns461, "map461");
	}
    
	@Override
    protected void special() throws Exception {

    }

}

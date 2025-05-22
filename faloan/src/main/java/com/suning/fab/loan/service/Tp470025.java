package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns463;
import com.suning.fab.loan.workunit.Lns466;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉： 区分利息状态接口查询
 *
 * @Author 16090227@cnsuning.com
 * @Date Created in 16:48 2020/11/06
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repaymentQueryDetail")
public class Tp470025 extends ServiceTemplate {

	@Autowired
    Lns466 lns466;

    public Tp470025(){
        needSerSeqNo=false;
    }
    
	
    @Override
    protected void run() throws Exception {
		
    	trigger(lns466);
	}
    
	@Override
    protected void special() throws Exception {

    }

}

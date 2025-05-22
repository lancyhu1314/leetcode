package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns467;
import com.suning.fab.loan.workunit.Lns468;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉： 核销通知核销金额迁移至C账户
 *
 * @Author 16090227@cnsuning.com
 * @Date Created in 16:48 2020/12/04
 * @see
 */
@Scope("prototype")
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-transferWriteOffAmt")
public class Tp470027 extends ServiceTemplate {

	@Autowired
    Lns468 lns468;

    public Tp470027(){
        needSerSeqNo=false;
    }
    
	
    @Override
    protected void run() throws Exception {
		
    	trigger(lns468);
	}
    
	@Override
    protected void special() throws Exception {

    }

}

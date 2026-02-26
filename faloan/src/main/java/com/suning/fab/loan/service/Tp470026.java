package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns466;
import com.suning.fab.loan.workunit.Lns467;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉： 批量查询账户剩余本金
 *
 * @Author 16090227@cnsuning.com
 * @Date Created in 16:48 2020/12/04
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-batchRemainPrinRepayQuery")
public class Tp470026 extends ServiceTemplate {

	@Autowired
    Lns467 lns467;

    public Tp470026(){
        needSerSeqNo=false;
    }
    
	
    @Override
    protected void run() throws Exception {
		
    	trigger(lns467);
	}
    
	@Override
    protected void special() throws Exception {

    }

}

package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns464;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉： 还款拆分结果通知
 *
 * @Author 
 * @Date Created in 16:48 2020/03/09
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repayResult")
public class Tp470023 extends ServiceTemplate {
	
	@Autowired 
	Lns464 lns464;
	
    public Tp470023(){
        needSerSeqNo=true;
    }
    
	
    @Override
    protected void run() throws Exception {
    	trigger(lns464);
	}
    
	@Override
    protected void special() throws Exception {

    }

}

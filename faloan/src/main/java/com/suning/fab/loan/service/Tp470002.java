package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns002;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：维护工作日
 *
 * @Author 18049705 MYP
 * @Date Created in 17:15 2020/2/17
 * @see
 */
@Scope("prototype")
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-maintainStatutorydays")
public class Tp470002 extends ServiceTemplate {


    @Autowired
    Lns002 lns002;

    @Override
    public void run() throws Exception {
        trigger(lns002);
    }

    @Override
    protected void special() throws Exception {
    }
}



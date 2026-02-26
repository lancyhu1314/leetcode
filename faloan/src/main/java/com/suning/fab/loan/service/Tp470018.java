package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns441;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 〈保费还款试算〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705 MYP
 * @Date Created in 19:24 2019/5/22
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repaymentTrial")
public class Tp470018 extends ServiceTemplate {

    @Autowired
    Lns441 lns441;


    public Tp470018() {
        needSerSeqNo = false;
    }

    @Override
    protected void run() throws Exception {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("repayDate", ctx.getTranDate());

        trigger(lns441, "DEFAULT", param);

    }


    @Override
    protected void special() throws Exception {
        // nothing to do
    }



}

package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns099;
import com.suning.fab.loan.workunit.Lns100;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705 MYP
 * @Date Created in 15:07 2019/7/24
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-SysCutoff")
public class Tp470000 extends ServiceTemplate {

    @Autowired
    Lns100 lns100;
    @Autowired
    Lns099 lns099;

    @Override
    public void run() throws Exception {
        if ("TEST".equals(ctx.getRequestDict("flag"))) {
            trigger(lns099);
        } else {
            trigger(lns100);
        }
    }

    @Override
    protected void special() throws Exception {
        //什么也不做
    }
}

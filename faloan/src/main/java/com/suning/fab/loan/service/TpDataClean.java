package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns699;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉根据主键进行数据删除
 *
 * @Author 19043955
 * @Date 2021/11/29
 * @Version 1.0
 */
@Service
@Scope("prototype")
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-dataClean")
public class TpDataClean extends ServiceTemplate {

    @Autowired
    private Lns699 lns699;

    public TpDataClean() {
        needSerSeqNo = false;
    }

    @Override
    protected void run() throws FabException, Exception {
        trigger(lns699);
    }

    @Override
    protected void special() throws FabException, Exception {

    }
}

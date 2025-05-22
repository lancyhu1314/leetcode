package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 〈贷款核销子交易〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705
 * @Date Created in 11:19 2018/11/9
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-assetDeal")
public class Tp472007 extends ServiceTemplate {


    public Tp472007(){
        needSerSeqNo = true;
    }

    @Override
    protected void run() throws Exception {
        throw new FabException("LNS253");
    }

    @Override
    protected void special() throws Exception {
    }
}
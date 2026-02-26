package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns102;
import com.suning.fab.loan.workunit.Lns121;
import com.suning.fab.loan.workunit.Lns262;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


/**
 * @param
 * @author TT.Y
 * @version V1.0.0
 * 预收户加款
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-advanceAccountChargeDb")
public class Tp176012 extends ServiceTemplate {

    @Autowired
    Lns262 lns262;
    @Autowired
    Lns102 lns102;// 债务公司开户

    @Autowired
    Lns121 lns121;

    public Tp176012() {
        needSerSeqNo = true;
    }

    @Override
    protected void run() throws Exception {
        //防止幂等报错被拦截
        trigger(lns121);
        trigger(lns102);
        //预收充值
        trigger(lns262);

    }

    @Override
    protected void special() throws Exception {
        if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
                .contains(ctx.getRspCode())) {
            ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
        }
    }
}

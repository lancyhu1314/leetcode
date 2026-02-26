package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns121;
import com.suning.fab.loan.workunit.Lns217;
import com.suning.fab.loan.workunit.Lns257;
import com.suning.fab.loan.workunit.Lns263;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


/**
 * @author TT.Y
 * @version V1.0.0
 * <p>
 * 预收户减款
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-advanceAccountRefundDb")
public class Tp176011 extends ServiceTemplate {

    @Autowired
    Lns263 lns263;

    @Autowired
    Lns217 lns217;
    @Autowired
    Lns121 lns121;//考核数据校验 入库
    @Autowired
    Lns257 lns257;

    public Tp176011() {
        needSerSeqNo = true;
    }

    @Override
    protected void run() throws Exception {
        //防止幂等报错被拦截
        trigger(lns121);
        //预收冲退
        trigger(lns263);
    }

    @Override
    protected void special() throws Exception {
        if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
                .contains(ctx.getRspCode())) {
            ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
        }
    }
}

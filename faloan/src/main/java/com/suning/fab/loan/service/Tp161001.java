package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**

 * 〈功能详细描述〉：法催费用开户
 *
 * @Author
 * @Date 2022-09-26
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-legalCollectFeeAccountCreate")
public class Tp161001 extends ServiceTemplate {

    @Autowired
    Lns271 lns271;

    public Tp161001() {
        needSerSeqNo = true;
    }

    @Override
    protected void run() throws Exception {
        ThreadLocalUtil.clean();


        //法催开户
        trigger(lns271);

        
    }

    @Override
    protected void special() throws Exception {
        if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
                .contains(ctx.getRspCode())) {
            ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
        } else {

        }
    }
}
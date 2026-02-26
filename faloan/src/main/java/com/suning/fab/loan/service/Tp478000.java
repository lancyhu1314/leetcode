/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Tp479010.java
 * Author:   16071579
 * Date:     2017年5月24日 上午10:34:46
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.service;

import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

import java.util.HashMap;
import java.util.Map;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉：摊销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype")
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-penaltyDown")
public class Tp478000 extends ServiceTemplate {

    @Autowired Lns501 lns501;
    @Autowired Lns512 lns512;
    @Autowired Lns520 lns520;
    @Autowired Lns513 lns513;


    public Tp478000() {
        needSerSeqNo=true;
    }

    @Override
    protected void run() throws Exception {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("repayDate", ctx.getTranDate());
        //结息
        trigger(lns501, "map501", param);
        //罚息计提
        lns512.setRepayDate(ctx.getTranDate());
        lns512.setLnssatistics(lns501.getLnsBillStatistics());
        lns512.setLnsbasicinfo(lns501.getLnsbasicinfo());
        trigger(lns512);

        //罚息落表
        if(!VarChecker.isEmpty(lns512.getLa().getInterestAgreement().getCapRate())){
            //罚息落表
            lns520.setRepayDate(ctx.getTranDate());
            lns520.setLnsBillStatistics(lns501.getLnsBillStatistics());
            lns520.setLnsbasicinfo(lns501.getLnsbasicinfo());
            lns520.setLa(lns512.getLa());
            trigger(lns520);
        }else{
            lns513.setRepayDate(ctx.getTranDate());
            lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
            lns513.setLnsbasicinfo(lns501.getLnsbasicinfo());
            trigger(lns513);
        }


    }

    @Override
    protected void special() throws Exception {
        if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
                .contains(ctx.getRspCode())) {
            ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
        }
    }

}

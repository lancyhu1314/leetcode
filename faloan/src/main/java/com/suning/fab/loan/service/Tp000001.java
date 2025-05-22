package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.Getandset;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：罚息复利账单同期合并
 *
 * @Author 19049905
 * @Date Created in 16:48 2018/11/12
 * @see
 */
@Scope("prototype")
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-ConsolidatedBill")
public class Tp000001 extends ServiceTemplate {

    @Autowired
    Lns252 lns252;
    @Autowired
    Lns609 lns609;
    @Autowired
    Lns000 lns000;
    @Autowired
    Lns006 lns006;
    @Autowired
    Lns007 lns007;
    @Autowired Lns512 lns512;

    public Tp000001(){
        needSerSeqNo=true;
    }
    @Override
    protected void run() throws Exception {
        if("7".equals(ctx.getRequestDict("flag"))){
            //补动态封顶的金额
            trigger(lns007);
            //罚息计提
            trigger(lns512);
        }else if("6".equals(ctx.getRequestDict("flag"))){
            //封顶计息  trandate>contduedate  并且已经达到封顶的更新标记
            trigger(lns006);
        }
        else if("5".equals(ctx.getRequestDict("flag"))) {
            trigger(lns000);
            //扫描包里的所有类，并执行getset方法
        }
        else if("4".equals(ctx.getRequestDict("flag"))) {
            trigger(lns609);
            //扫描包里的所有类，并执行getset方法
        }else  if("3".equals(ctx.getRequestDict("flag"))){
            test();
        }else if("2".equals(ctx.getRequestDict("flag"))){
//            //房抵贷罚息落表
//            lns999.setAcctNo(ctx.getRequestDict("receiptNo").toString());
//            lns999.setRepayDate(ctx.getTranDate());
//            trigger(lns999);
        }else{
            trigger(lns252);
        }
    }

    private void test(){


        String perfix = "com.suning.fab.loan.workunit.";
        Class clazz;

        for (int i=100;i<999;i++) {

            String allName =perfix+"Lns"+i;


            Object a ;
            try {
                clazz = Class.forName(allName);
                a = clazz.newInstance();
                com.suning.fab.loan.utils.Getandset.getsetCall(a);
                LoggerUtil.info(allName);
            } catch (Exception e) {
                LoggerUtil.error("实例化失败",e);
            }

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

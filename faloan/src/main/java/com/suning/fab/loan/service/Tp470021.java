package com.suning.fab.loan.service;

import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.RedisUtil;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉： 费用减免
 *
 * @Author 
 * @Date Created in 16:48 2019/04/17
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-feeDeduction")
public class Tp470021 extends ServiceTemplate {

    @Autowired
    Lns462 lns462;
    @Autowired
    Lns469 lns469;
    @Autowired
    Lns121 lns121;//考核数据校验 入库
    @Autowired
    Lns530 lns530;
    @Autowired
    Lns531 lns531;
    @Autowired
    Lns501 lns501;
    @Autowired
    Lns001 lns001;
    public Tp470021(){
        needSerSeqNo=true;
    }

    @Override
    protected void run() throws Exception {
        ThreadLocalUtil.clean();
        checkInput();

        trigger(lns001);
        //费用结息
        lns501.setLa(lns001.getLoanAgreement());
        lns501.setLnsBillStatistics(lns001.getLnsBillStatistics());
        lns501.setRepayDate(ctx.getTranDate());
        trigger(lns501);
        //违约金计提
        lns530.setLoanAgreement(lns001.getLoanAgreement());
        lns530.setBillStatistics(lns001.getLnsBillStatistics());
        lns530.setRepayDate(ctx.getTranDate());
        trigger(lns530);
        lns531.setLoanAgreement(lns001.getLoanAgreement());
        lns531.setLnsBillStatistics(lns001.getLnsBillStatistics());
        //违约金结息
        trigger(lns531);

        if(!VarChecker.isEmpty(ctx.getRequestDict("reduceFee"))&&((FabAmount)ctx.getRequestDict("reduceFee")).isPositive()){
            trigger(lns462);
        }
        if(!VarChecker.isEmpty(ctx.getRequestDict("reduceDamage"))&&((FabAmount)ctx.getRequestDict("reduceDamage")).isPositive()){
            trigger(lns469);
        }
        trigger(lns121);
    }

    private void checkInput() throws FabException {
        if((VarChecker.isEmpty(ctx.getRequestDict("reduceFee"))||((FabAmount)ctx.getRequestDict("reduceFee")).isZero())&&
                (VarChecker.isEmpty(ctx.getRequestDict("reduceDamage"))||((FabAmount)ctx.getRequestDict("reduceDamage")).isZero()))
        {
            throw new FabException("LNS239","违约金和费用的减免金额");
        }
    }
    
	@Override
    protected void special() throws Exception {
        //幂等返回正确
        if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
                .contains(ctx.getRspCode())) {
            ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
        }
    }

}

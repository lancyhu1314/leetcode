package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.FabAmount;
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
 * 〈功能详细描述〉：贷款核销 一般用户坏账核销
 *
 * @Author 18049705
 * @Date Created in 11:19 2018/11/9
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-loanCancellation")
public class Tp472006 extends ServiceTemplate {


    @Autowired
    Lns115 lns115;
    @Autowired
    Lns518 lns518;
    @Autowired
    Lns500 lns500;//利息计提
    @Autowired
    Lns512 lns512;//罚息计提
    @Autowired
    Lns513 lns513;//罚息落表
    @Autowired
    Lns501 lns501;//结息
    @Autowired
    Lns503 lns503;//转列
    @Autowired
    Lns502 lns502;//摊销
    @Autowired
    Lns507 lns507;
    @Autowired
    Lns116 lns116;
    @Autowired
    Lns537 lns537;
    @Autowired Lns121 lns121;//考核数据校验 入库

    public Tp472006(){
        needSerSeqNo = true;
    }

    @Override
    protected void run() throws Exception {
        ThreadLocalUtil.clean();

        //入参校验 幂等
        trigger(lns518);
        lns116.setAcctNo(lns518.getLnsbasicinfo().getAcctno());
        trigger(lns116);

        if( !ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(lns518.getLnsbasicinfo().getLoanstat())) {
            if (new FabAmount(lns518.getLnsbasicinfo().getDeductionamt()).isPositive()) {
                //扣息放款 进行摊销
                lns502.setAcctNo(lns518.getLnsbasicinfo().getAcctno());
                lns502.setRepayDate(ctx.getTranDate());
                trigger(lns502);
            } else {
                //利息计提
                lns500.setLnsbasicinfo(lns518.getLnsbasicinfo());
                lns500.setRepayDate(ctx.getTranDate());
                lns500.setAcctNo(lns518.getLnsbasicinfo().getAcctno());
                trigger(lns500);
            }

            Map<String, Object> param = new HashMap<String, Object>();
            param.put("repayDate", ctx.getTranDate());

            if (VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_YBYX,
                    ConstantDeclare.REPAYWAY.REPAYWAY_XXHB,
                    ConstantDeclare.REPAYWAY.REPAYWAY_XBHX,
                    ConstantDeclare.REPAYWAY.REPAYWAY_ZDY)
                    .contains(lns518.getLnsbasicinfo().getRepayway())) {
                //非标的结息
                lns507.setAcctNo(lns518.getLnsbasicinfo().getAcctno());
                trigger(lns507, "map507", param);
            } else {
                //结息
                lns501.setLnsbasicinfo(lns500.getLnsbasicinfo());
                lns501.setAcctNo(lns518.getLnsbasicinfo().getAcctno());
                trigger(lns501, "map501", param);
            }


            //罚息计提
            lns512.setRepayDate(ctx.getTranDate());
            lns512.setLnsbasicinfo(lns518.getLnsbasicinfo());
            lns512.setAcctNo(lns518.getLnsbasicinfo().getAcctno());
            trigger(lns512);
            //罚息落表
            lns513.setRepayDate(ctx.getTranDate());
            lns513.setLnsBillStatistics(lns512.getLnssatistics());
            lns513.setLnsbasicinfo(lns518.getLnsbasicinfo());
            lns513.setAcctNo(lns518.getLnsbasicinfo().getAcctno());
            trigger(lns513);


            //转列
            lns503.setAcctNo(lns518.getLnsbasicinfo().getAcctno());
            trigger(lns503, "map503", param);

            lns115.setLnsbasicinfo(lns518.getLnsbasicinfo());
            trigger(lns115);
        }
        else {
            lns537.setLnsbasicinfo(lns518.getLnsbasicinfo());
            trigger(lns537);
        }


        //修改核销标志为已核销
        lns518.setCancelFlag("1");
        //防止幂等报错被拦截
        trigger(lns121);
    }

    @Override
    protected void special() throws Exception {
        if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
                .contains(ctx.getRspCode())) {
            ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
        }else{
            lns518.setCancelFlag("0");
        }

    }
}
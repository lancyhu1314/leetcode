package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
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
 * 〈功能详细描述〉：应计转非应计
 *
 * @Author
 * @Date 2022-08-29
 * @see
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-nonAccrualOpt")
public class Tp479003 extends ServiceTemplate {


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
    Lns121 lns121;//考核数据校验 入库


    @Autowired
    Lns535 lns535;
    @Autowired
    Lns536 lns536;

    public Tp479003() {
        needSerSeqNo = true;
    }

    @Override
    protected void run() throws Exception {
        ThreadLocalUtil.clean();


        //入参校验 幂等
        trigger(lns535);

        if (!ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(lns535.getLnsbasicinfo().getLoanstat())) {
            // 补计提+补摊销
            if (new FabAmount(lns535.getLnsbasicinfo().getDeductionamt()).isPositive()) {
                lns502.setAcctNo(lns535.getLnsbasicinfo().getAcctno());
                lns502.setRepayDate(ctx.getTranDate());
                trigger(lns502);
            } else {
                lns500.setLnsbasicinfo(lns535.getLnsbasicinfo());
                lns500.setRepayDate(ctx.getTranDate());
                lns500.setAcctNo(lns535.getLnsbasicinfo().getAcctno());
                trigger(lns500);
            }

            // 补结息
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("repayDate", ctx.getTranDate());
            if (VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_YBYX,
                    ConstantDeclare.REPAYWAY.REPAYWAY_XXHB,
                    ConstantDeclare.REPAYWAY.REPAYWAY_XBHX,
                    ConstantDeclare.REPAYWAY.REPAYWAY_ZDY)
                    .contains(lns535.getLnsbasicinfo().getRepayway())) {
                //非标的结息
                lns507.setAcctNo(lns535.getLnsbasicinfo().getAcctno());
                trigger(lns507, "map507", param);
            } else {
                //结息
                lns501.setLnsbasicinfo(lns500.getLnsbasicinfo());
                lns501.setAcctNo(lns535.getLnsbasicinfo().getAcctno());
                trigger(lns501, "map501", param);
            }


            //罚息计提
            lns512.setRepayDate(ctx.getTranDate());
            lns512.setLnsbasicinfo(lns535.getLnsbasicinfo());
            lns512.setAcctNo(lns535.getLnsbasicinfo().getAcctno());
            trigger(lns512);
            //罚息落表
            lns513.setRepayDate(ctx.getTranDate());
            lns513.setLnsBillStatistics(lns512.getLnssatistics());
            lns513.setLnsbasicinfo(lns535.getLnsbasicinfo());
            lns513.setAcctNo(lns535.getLnsbasicinfo().getAcctno());
            trigger(lns513);

            //转列
            lns503.setAcctNo(lns535.getLnsbasicinfo().getAcctno());
            trigger(lns503, "map503", param);
        }

        // 转非应计
        lns536.setLnsbasicinfo(lns535.getLnsbasicinfo());
        trigger(lns536);

        //考核字段
        trigger(lns121);

        // 功能码2-预校验时，执行到这段代码表示之前交易无异常，抛指定错误码
        if (lns535.getFunctionCode().equals("2"))
            throw new FabException("LNS257");
    }

    /**
     * 全局变量预备
     *
     * @param la
     */
    public void prepareWriteOff(LoanAgreement la){
        if (ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(la.getContract().getLoanStat())) {
            //将账户的状态放入上下文中  只有还款服务才需要存放此字段
            ThreadLocalUtil.set("loanStat", la.getContract().getLoanStat());
        }
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
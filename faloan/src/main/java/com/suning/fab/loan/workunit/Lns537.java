package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈贷款核销子交易〉
 * 〈功能详细描述〉：贷款转非应计后核销
 *
 * @Author
 * @Date 2022-08-29
 * @see
 */
@Scope("prototype")
@Repository
public class Lns537 extends WorkUnit {
    // 借据号
    String receiptNo;

    @Autowired
    LoanEventOperateProvider eventProvider;
    TblLnsbasicinfo lnsbasicinfo;
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;
    @Autowired
    @Qualifier("accountAdder")
    AccountOperator add;

    @Override
    public void run() throws Exception {
        TranCtx ctx = getTranctx();
        LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);

        //查询主文件
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno1", receiptNo);
        param.put("openbrc", ctx.getBrc());
        try {
            lnsbasicinfo = DbAccessUtil.queryForObject("CUSTOMIZE.query_non_acctno", param, TblLnsbasicinfo.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_non_acctno");
        }

        //查询已有的账单表
        param.put("acctno", lnsbasicinfo.getAcctno());
        param.put("brc", ctx.getBrc());
        List<TblLnsbill> tblLnsBills;
        try {
            tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", param, TblLnsbill.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_hisbills");
        }

        //统计各个类型的账单   正常本金账单还需要加上主文件表的合同余额  用map储存
        Map<String, FabAmount> zfyjMap = new HashMap<>();//键值为账户类型 例：PRIN.N3
        // 主文件表的合同余额 作为正常本金(首次转非应计)
        if (new FabAmount(lnsbasicinfo.getContractbal()).isPositive())
            zfyjMap.put("PRIN.F" + ".3", new FabAmount(lnsbasicinfo.getContractbal()));
        String key = null;


        // 账本拆分
        for (TblLnsbill bill : tblLnsBills) {
            param.clear();

            // 汇总本金账本记账金额
            if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(bill.getBilltype())) {
                key = bill.getBilltype() + "." + "F" + ".3";
            } else if (VarChecker.asList("GINT", "DINT", "CINT").contains(bill.getBilltype())) {
                key = "DINT" + "." + bill.getBillstatus() + "." + bill.getCancelflag();
            } else {
                key = bill.getBilltype() + "." + bill.getBillstatus() + "." + bill.getCancelflag();
            }

            if (null != zfyjMap.get(key)) {
                zfyjMap.get(key).selfAdd(bill.getBillbal());
            } else {
                zfyjMap.put(key, new FabAmount(bill.getBillbal()));
            }
        }

        //更新主文件表的 状态为核销  利息计提标志为close
        try {

            param.put("acctno", lnsbasicinfo.getAcctno());
            param.put("brc", ctx.getBrc());
            param.put("loanstat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION);
            param.put("flag1", lnsbasicinfo.getFlag1() + ConstantDeclare.FLAG1.H);
            /*更新主文件表中该数据的贷款状态CA,以及利息计提标志CLOSE*/
            //核销不更改计提标志  在计提交易用贷款状态判断2019-10-31
            DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstatEx_115", param);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "update_lnsbasicinfo_loanstatEx_115");
        }

        //主文件拓展表添加核销日期
        Map<String, Object> exParam = new HashMap<>();
        exParam.put("acctno", receiptNo);
        exParam.put("brc", ctx.getBrc());
        exParam.put("key", "HXRQ");
        exParam.put("value1", ctx.getTranDate());
        exParam.put("value2", 0.00);
        exParam.put("value3", 0.00);
        exParam.put("tunneldata", "");
        try {
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsbasicinfoex");
        }


        //抛多次事件，一个账户类型一个金额一个事件
        for (Map.Entry<String, FabAmount> zfyj : zfyjMap.entrySet()) {
            if (!Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, ConstantDeclare.BILLTYPE.BILLTYPE_NINT, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, ConstantDeclare.BILLTYPE.BILLTYPE_GINT, ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(zfyj.getKey().split("\\.")[0])) {
                continue;
            }

            LnsAcctInfo lnsAcctInfo = null;
            LnsAcctInfo lnsAcctInfo1 = null;
            LnsAcctInfo opAcctInfo = null;
            if (zfyj.getValue().isPositive()) {

                // "1"从原账户转出，"2"和"3"从F户转出
                if ("1".equals(zfyj.getKey().split("\\.")[2])) {
                    lnsAcctInfo = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                            zfyj.getKey().split("\\.")[0], zfyj.getKey().split("\\.")[1], new FabCurrency());
                    lnsAcctInfo.setCancelFlag(zfyj.getKey().split("\\.")[2]);
                } else if (VarChecker.asList("2", "3").contains(zfyj.getKey().split("\\.")[2])) {
                    lnsAcctInfo = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                            zfyj.getKey().split("\\.")[0], ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL, new FabCurrency());
                    lnsAcctInfo.setCancelFlag(zfyj.getKey().split("\\.")[2]);
                }


                // 转入核销账户
                lnsAcctInfo1 = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                        zfyj.getKey().split("\\.")[0], ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION, new FabCurrency());
                lnsAcctInfo1.setCancelFlag(zfyj.getKey().split("\\.")[2]);


                sub.operate(lnsAcctInfo, null, zfyj.getValue(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZFYJ,
                        tranctx);
                add.operate(lnsAcctInfo1, null, zfyj.getValue(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZFYJ,
                        tranctx);

            }


            eventProvider.createEvent(ConstantDeclare.EVENT.LNCNLVRFIN, zfyj.getValue(),
                    lnsAcctInfo, lnsAcctInfo1, la.getFundInvest(), ConstantDeclare.BRIEFCODE.TZHX, tranctx, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
        }
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }

    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;
    }

    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }
}

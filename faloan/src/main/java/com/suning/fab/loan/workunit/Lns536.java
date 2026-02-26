package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.BillSplitter;
import com.suning.fab.loan.bo.IntegerObj;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * 〈贷款核销子交易〉
 * 〈功能详细描述〉：贷款转非应计
 *
 * @Author
 * @Date 2022-08-29
 * @see
 */
@Scope("prototype")
@Repository
public class Lns536 extends WorkUnit {
    // 借据号
    String receiptNo;
    // 渠道流水
    String serialNo;
    // 转非应计日期
    String nonAccrualDate;
    // 是否冲销非应计前收入
    String chargeAginstFlag;
    // 冲销收入开始日
    String chargeAginstDate;


    //输出
    FabAmount settleAmt = new FabAmount(0.00);
    FabAmount compensateDint = new FabAmount(0.00);
    FabAmount nonAccrualPrin = new FabAmount(0.00);
    FabAmount nChargeAginstInt = new FabAmount(0.00);
    FabAmount nChargeAginstDint = new FabAmount(0.00);
    FabAmount comeInDint = new FabAmount(0.00);
    FabAmount nChargeCAInt = new FabAmount(0.00);
    FabAmount chargeAginstDint = new FabAmount(0.00);
    FabAmount chargeAginstInt = new FabAmount(0.00);


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
        LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), tranctx);
        IntegerObj maxTxseq = new IntegerObj(100);

        // 部分逾期和正常贷款  结息到当前日期  --类似于未来期的还款
        if (new FabAmount(lnsbasicinfo.getContractbal()).isPositive() && !ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(lnsbasicinfo.getLoanstat())) {
            if (VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
                    ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR).contains(lnsbasicinfo.getLoanstat()))
                LoanInterestSettlementUtil.inerestBillByDate(tranctx.getTranDate(), la, tranctx);
        }


        //查询主文件  --结息到当前日期 可能更改主文件的合同余额
        Map<String, Object> param = new HashMap<String, Object>();
        //账号
        param.put("acctno1", receiptNo);
        //机构
        param.put("openbrc", tranctx.getBrc());

        try {
            //取主文件信息
            lnsbasicinfo = DbAccessUtil.queryForObject("CUSTOMIZE.query_non_acctno", param, TblLnsbasicinfo.class);

        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_non_acctno");
        }


        // 房抵贷罚息拆分预处理
        if ("2412615".equals(lnsbasicinfo.getPrdcode()) && !ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(lnsbasicinfo.getLoanstat())) {
            param.put("acctno", lnsbasicinfo.getAcctno());
            param.put("brc", tranctx.getBrc());
            List<TblLnsbill> fddBillList;
            try {
                fddBillList = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", param, TblLnsbill.class);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS103", "query_hisbills");
            }

            for (TblLnsbill lnsBill : fddBillList) {
                // 房抵贷切分罚息账本
                if (ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(lnsBill.getBilltype())) {
                    List<BillSplitter> splitterList = new ArrayList<>();
                    BillSplitter splitter = new BillSplitter();
                    splitter.setLeftSection("NORMAL");
                    splitter.setSplitDate(CalendarUtil.nDaysAfter(lnsBill.getBegindate(), 90).toString("yyyy-MM-dd"));
                    splitter.setRightSection("NORMAL");
                    splitterList.add(splitter);
                    splitSectionOperator(la, lnsBill, splitterList, new HashMap<>(), maxTxseq);

                    // 更新切分后数据
                    param.put("trandate", lnsBill.getTrandate());
                    param.put("serseqno", lnsBill.getSerseqno());
                    param.put("txseq", lnsBill.getTxseq());
                    param.put("billamt", lnsBill.getBillamt());
                    param.put("billbal", lnsBill.getBillbal());
                    param.put("begindate", lnsBill.getBegindate());

                    try {
                        DbAccessUtil.execute("CUSTOMIZE.update_lnsbill_cancelflag_FYJ", param);
                    } catch (FabSqlException e) {
                        throw new FabException(e, "SPS102", "lnsbill");
                    }
                }
            }
        }


        //统计各个类型的账单   正常本金账单还需要加上主文件表的合同余额  用map储存
        Map<String, FabAmount> zfyjMap = new HashMap<>();//键值为账户类型 例：PRIN.N3

        //查询已有的账单表
        param.put("acctno", lnsbasicinfo.getAcctno());
        param.put("brc", tranctx.getBrc());
        List<TblLnsbill> tblLnsBills;
        try {
            tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", param, TblLnsbill.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_hisbills");
        }
        // 主文件表的合同余额 作为正常本金(首次转非应计)
        if (new FabAmount(lnsbasicinfo.getContractbal()).isPositive() && !ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(lnsbasicinfo.getLoanstat()))
            zfyjMap.put("PRIN.N" + ".3", new FabAmount(lnsbasicinfo.getContractbal()));
        String key = null;

        // 添加账本map，内存中将账本分好段
        for (TblLnsbill lnsBill : tblLnsBills) {
            // 罚息类账本预处理
            if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_GINT)
                    .contains(lnsBill.getBilltype())) {
                for (TblLnsbill prinBill : tblLnsBills) {
                    if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,
                            ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
                            .contains(prinBill.getBilltype())) {
                        if (lnsBill.getDertrandate().equals(prinBill.getTrandate()) &&
                                lnsBill.getDerserseqno().equals(prinBill.getSerseqno()) &&
                                lnsBill.getDertxseq().equals(prinBill.getTxseq())) {
                            // settledate设为该期本金结束日
                            lnsBill.setSettledate(prinBill.getEnddate());
                            // 统一账本开始日（开始日和结束日跨90天的账本，开始日刷成第90天）
                            if (CalendarUtil.before(lnsBill.getBegindate(), CalendarUtil.nDaysAfter(prinBill.getEnddate(), 90).toString("yyyy-MM-dd")) &&
                                    CalendarUtil.after(lnsBill.getEnddate(), CalendarUtil.nDaysAfter(prinBill.getEnddate(), 90).toString("yyyy-MM-dd"))) {
                                lnsBill.setBegindate(CalendarUtil.nDaysAfter(prinBill.getEnddate(), 90).toString("yyyy-MM-dd"));
                            }
                        }
                        // 同上，脏数据特殊场景特殊处理
                        else if (lnsBill.getPeriod().equals(prinBill.getPeriod())) {
                            if ((VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(prinBill.getBilltype()) && VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(lnsBill.getBilltype()))
                                    ||
                                    (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(prinBill.getBilltype()) && VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT, ConstantDeclare.BILLTYPE.BILLTYPE_DINT).contains(lnsBill.getBilltype()))) {
                                // settledate设为该期本金结束日
                                lnsBill.setSettledate(prinBill.getEnddate());
                                // 统一账本开始日（开始日和结束日跨90天的账本，开始日刷成第90天）
                                if (CalendarUtil.before(lnsBill.getBegindate(), CalendarUtil.nDaysAfter(prinBill.getEnddate(), 90).toString("yyyy-MM-dd")) &&
                                        CalendarUtil.after(lnsBill.getEnddate(), CalendarUtil.nDaysAfter(prinBill.getEnddate(), 90).toString("yyyy-MM-dd"))) {
                                    lnsBill.setBegindate(CalendarUtil.nDaysAfter(prinBill.getEnddate(), 90).toString("yyyy-MM-dd"));
                                }
                            }
                        }
                    }
                }
            }
            if (lnsBill.getTxseq() > maxTxseq.getValue())
                maxTxseq.setValue(lnsBill.getTxseq());
        }

        // 账本拆分
        for (TblLnsbill bill : tblLnsBills) {
            param.clear();

            // 多次转非应计，本金过滤，2和3的过滤
            if (ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(lnsbasicinfo.getLoanstat()) &&
                    (VarChecker.asList("2", "3").contains(bill.getCancelflag()) || ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBilltype())))
                continue;


            // 创建拆分器list
            List<BillSplitter> splitterList = new ArrayList<>();
            // 首段拆分（通用）
            BillSplitter splitter = new BillSplitter();
            splitter.setLeftSection("1");
            splitter.setSplitDate(chargeAginstDate);
            splitter.setRightSection("2");
            splitterList.add(splitter);

            // 罚息/复利/违约金
            if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_GINT)
                    .contains(bill.getBilltype())) {
                // 当期90天对应日期
                String LStartDate = CalendarUtil.nDaysAfter(bill.getSettledate(), 90).toString("yyyy-MM-dd");

                // 冲销开始日和转非应计日期，都小于90天
                if (CalendarUtil.before(chargeAginstDate, LStartDate) &&
                        CalendarUtil.before(nonAccrualDate, LStartDate)) {
                    splitter = new BillSplitter();
                    splitter.setLeftSection("2");
                    splitter.setSplitDate(nonAccrualDate);
                    splitter.setRightSection("3");
                    splitterList.add(splitter);
                }
                // 冲销开始日和转非应计日期，跨90天
                if (CalendarUtil.beforeAlsoEqual(chargeAginstDate, LStartDate) &&
                        CalendarUtil.afterAlsoEqual(nonAccrualDate, LStartDate)) {
                    splitter = new BillSplitter();
                    splitter.setLeftSection("2");
                    splitter.setSplitDate(LStartDate);
                    splitter.setRightSection("3");
                    splitterList.add(splitter);
                }
                // 冲销开始日和转非应计日期，都大于90天
                if (CalendarUtil.after(chargeAginstDate, LStartDate) &&
                        CalendarUtil.after(nonAccrualDate, LStartDate)) {
                    // 注：逻辑特殊，需清除通用首段拆分结果
                    splitterList.clear();
                    splitter.setLeftSection("1");
                    splitter.setSplitDate(LStartDate);
                    splitter.setRightSection("3");
                    splitter.setSplitType("3");
                    splitterList.add(splitter);
                }
            }

            // 利息
            if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
                    .contains(bill.getBilltype())) {
                splitter = new BillSplitter();
                splitter.setLeftSection("2");
                splitter.setSplitDate(nonAccrualDate);
                splitter.setRightSection("3");
                splitterList.add(splitter);
            }

            // 切分处理
            if (!ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBilltype()))
                splitSectionOperator(la, bill, splitterList, zfyjMap, maxTxseq);

            // 更新切分后数据
            param.put("trandate", bill.getTrandate());
            param.put("serseqno", bill.getSerseqno());
            param.put("txseq", bill.getTxseq());
            param.put("cancelflag", bill.getCancelflag());
            param.put("billamt", bill.getBillamt());
            param.put("billbal", bill.getBillbal());
            param.put("begindate", bill.getBegindate());

            try {
                DbAccessUtil.execute("CUSTOMIZE.update_lnsbill_cancelflag_FYJ", param);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "lnsbill");
            }

            // 汇总本金账本记账金额
            if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(bill.getBilltype())) {
                key = bill.getBilltype() + "." + bill.getBillstatus() + ".3";
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

        if (!ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(lnsbasicinfo.getLoanstat())) {
            //更新主文件表
            try {
                param.put("acctno", lnsbasicinfo.getAcctno());
                param.put("brc", tranctx.getBrc());
                param.put("loanstat", ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL);
                DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstatEx_115", param);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "update_lnsbasicinfo_loanstatEx_115");
            }


            //扣息放款  摊销结束
            if (new FabAmount(lnsbasicinfo.getDeductionamt()).isPositive()) {
                param.put("brc", tranctx.getBrc());
                param.put("acctno", lnsbasicinfo.getAcctno());
                param.put("status", ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);

                try {
                    DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan_status", param);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS102", "lnsamortizeplan");
                }
            }
        }


        //主文件拓展表添加转非应计日期
        Map<String, Object> exParam = new HashMap<>();
        exParam.put("acctno", receiptNo);
        exParam.put("brc", tranctx.getBrc());
        exParam.put("key", "ZFYJ");
        exParam.put("value1", nonAccrualDate);
        exParam.put("value2", 0.00);
        exParam.put("value3", 0.00);
        exParam.put("tunneldata", "");
        try {
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
        } catch (FabSqlException e) {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                LoanDbUtil.update("Lnsbasicinfoex.update_lnsbasicinfoex_536", exParam);
            } else
                throw new FabException(e, "SPS102", "lnsbasicinfoex");
        }


        //抛多次事件，一个账户类型一个金额一个事件
        for (Map.Entry<String, FabAmount> zfyj : zfyjMap.entrySet()) {
            if (!Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, ConstantDeclare.BILLTYPE.BILLTYPE_NINT, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, ConstantDeclare.BILLTYPE.BILLTYPE_GINT, ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(zfyj.getKey().split("\\.")[0])) {
                continue;
            }

            if (zfyj.getValue().isPositive() &&
                    !"1".equals(zfyj.getKey().split("\\.")[2])) {
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                        zfyj.getKey().split("\\.")[0], zfyj.getKey().split("\\.")[1], new FabCurrency());

                sub.operate(lnsAcctInfo, null, zfyj.getValue(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZFYJ,
                        tranctx);

                //添加非应计账户
                LnsAcctInfo lnsAcctInfo1 = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                        zfyj.getKey().split("\\.")[0], "F", new FabCurrency());

                add.operate(lnsAcctInfo1, null, zfyj.getValue(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZFYJ,
                        tranctx);

                //登记本金户销户事件
                LnsAcctInfo opAcctInfo = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
                        zfyj.getKey().split("\\.")[0], ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL, new FabCurrency());

                //罚息表外记账类型“3”金额汇总登记事件表amt2
                if (VarChecker.asList("NINT", "DINT").contains(zfyj.getKey().split("\\.")[0])) {
                    List<FabAmount> amtList = new ArrayList<FabAmount>();
                    if ("2".equals(zfyj.getKey().split("\\.")[2])) {
                        amtList.add(new FabAmount(TaxUtil.calcVAT(zfyj.getValue()).getVal()));
                        amtList.add(zfyj.getValue());

                        // 汇总冲销罚息（不保留税金）
                        if (VarChecker.asList("NINT").contains(zfyj.getKey().split("\\.")[0]))
                            chargeAginstDint.selfAdd(zfyj.getValue());
                            // 汇总冲销利息（不保留税金）
                        else if (VarChecker.asList("DINT").contains(zfyj.getKey().split("\\.")[0]))
                            chargeAginstInt.selfAdd(zfyj.getValue());

                    } else if ("3".equals(zfyj.getKey().split("\\.")[2])) {
                        amtList.add(new FabAmount(TaxUtil.calcVAT(zfyj.getValue()).getVal()));
                        amtList.add(new FabAmount(0.00));
                        amtList.add(zfyj.getValue());

                        // 汇总冲销罚息收入（保留税金）
                        if (VarChecker.asList("NINT").contains(zfyj.getKey().split("\\.")[0]))
                            comeInDint.selfAdd(zfyj.getValue());
                            // 汇总冲销利息收入（保留税金）
                        else if (VarChecker.asList("DINT").contains(zfyj.getKey().split("\\.")[0]))
                            nChargeCAInt.selfAdd(zfyj.getValue());
                    }


                    eventProvider.createEvent(ConstantDeclare.EVENT.BATRANSFER, zfyj.getValue(),
                            lnsAcctInfo, opAcctInfo, la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZFYJ, tranctx, amtList, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());

                } else {
                    // 汇总转非应计本金
                    nonAccrualPrin.selfAdd(zfyj.getValue());
                    eventProvider.createEvent(ConstantDeclare.EVENT.BATRANSFER, zfyj.getValue(),
                            lnsAcctInfo, opAcctInfo, la.getFundInvest(), ConstantDeclare.BRIEFCODE.ZFYJ, tranctx, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
                }
            } else if (zfyj.getValue().isPositive() &&
                    "1".equals(zfyj.getKey().split("\\.")[2])) {
                // 汇总未冲销利息
                if (VarChecker.asList("NINT").contains(zfyj.getKey().split("\\.")[0]))
                    nChargeAginstInt.selfAdd(zfyj.getValue());
                    // 汇总未冲销罚息
                else if (VarChecker.asList("DINT").contains(zfyj.getKey().split("\\.")[0]))
                    nChargeAginstDint.selfAdd(zfyj.getValue());
            }
        }
    }

    public void splitSectionOperator(LoanAgreement la, TblLnsbill bill, List<BillSplitter> splitterList, Map<String, FabAmount> zfyjMap, IntegerObj maxTxseq) throws IOException, ClassNotFoundException, FabException {
        // 总金额
        FabAmount totalAmt = new FabAmount(bill.getBillamt());
        // 总余额
        FabAmount totalBal = new FabAmount(bill.getBillbal());
        // 总天数
        int totalDays = CalendarUtil.actualDaysBetween(bill.getBegindate(), bill.getEnddate());
        // 累计金额
        FabAmount sumAmt = new FabAmount(0.00);

        // 统计有效拆分日期个数
        int validDateCount = 0;
        for (BillSplitter splitter : splitterList) {
            if (CalendarUtil.before(bill.getBegindate(), splitter.getSplitDate()) &&
                    CalendarUtil.after(bill.getEnddate(), splitter.getSplitDate()))
                validDateCount++;
        }

        // 循环拆分账本
        int lastIndex = 0;
        for (int i = 0; i < splitterList.size(); i++) {
            // 无需切割，只修改cancelflag
            if (validDateCount == 0) {
                // 在最早一次切割日前
                if (CalendarUtil.beforeAlsoEqual(bill.getEnddate(), splitterList.get(i).getSplitDate())) {
                    bill.setCancelflag(splitterList.get(i).getLeftSection());
                    break;
                }

                // 在最后一次切割日后
                if (i == splitterList.size() - 1 &&
                        CalendarUtil.afterAlsoEqual(bill.getBegindate(), splitterList.get(i).getSplitDate())) {
                    // 冲销日期和转非应计日期都在90天后特殊处理（账本落表日期前1，后3）
                    if ("3".equals(splitterList.get(i).getSplitType())) {
                        if (CalendarUtil.after(bill.getTrandate().toString(), chargeAginstDate))
                            bill.setCancelflag(splitterList.get(i).getRightSection());
                        else
                            bill.setCancelflag(splitterList.get(i).getLeftSection());
                    } else {
                        // 账本开始日大于最后切割日，更新右状态
                        bill.setCancelflag(splitterList.get(i).getRightSection());
                    }
                }
            } else {
                // 过滤无法切割到账本的切割日期
                if (CalendarUtil.afterAlsoEqual(bill.getBegindate(), splitterList.get(i).getSplitDate())) {
                    lastIndex++;
                    continue;
                }
                // 后面无切分
                if (CalendarUtil.beforeAlsoEqual(bill.getBegindate(), splitterList.get(i).getSplitDate()) &&
                        CalendarUtil.beforeAlsoEqual(bill.getEnddate(), splitterList.get(i).getSplitDate())) {
                    continue;
                }

                // 拆分后天数
                int splitDays = CalendarUtil.actualDaysBetween(bill.getBegindate(), splitterList.get(i).getSplitDate());
                // 拆分后金额
                FabAmount splitAmt = new FabAmount(BigDecimal.valueOf(totalAmt.getVal()).multiply(BigDecimal.valueOf(splitDays).divide(BigDecimal.valueOf(totalDays), 20, BigDecimal.ROUND_HALF_UP)).doubleValue());
                // 累计处理金额
                sumAmt.selfAdd(splitAmt);
                // 拆分出的账本
                LnsBill lnsBill = BillTransformHelper.convertToLnsBill(bill);
                TblLnsbill cloneBill = BillTransformHelper.convertToTblLnsBill(la, lnsBill.deepClone(), tranctx);
                cloneBill.setEnddate(splitterList.get(i).getSplitDate());
                cloneBill.setCancelflag(splitterList.get(i).getLeftSection());
                cloneBill.setBillamt(splitAmt.getVal());
                if (totalAmt.sub(sumAmt).sub(totalBal).isNegative()) {
                    cloneBill.setBillbal(totalBal.sub(new FabAmount(totalAmt.sub(sumAmt).getVal())).getVal());
                    totalBal.selfSub(cloneBill.getBillbal());
                } else
                    cloneBill.setBillbal(0.00);

                maxTxseq.setValue(maxTxseq.getValue() + 1);
                cloneBill.setTxseq(maxTxseq.getValue());
                cloneBill.setBillproperty("split");
                try {
                    DbAccessUtil.execute("Lnsbill.insert", cloneBill);
                } catch (FabSqlException e) {
                    LoggerUtil.error("lnsbill.insert.ERROR");
                    throw new FabException(e, "SPS100", "lnsbill");
                }
                bill.setBegindate(splitterList.get(i).getSplitDate());

                // 汇总本金账本记账金额
                String key;
                if (VarChecker.asList("GINT", "DINT", "CINT").contains(cloneBill.getBilltype())) {
                    key = "DINT" + "." + cloneBill.getBillstatus() + "." + cloneBill.getCancelflag();
                } else {
                    key = cloneBill.getBilltype() + "." + cloneBill.getBillstatus() + "." + cloneBill.getCancelflag();
                }

                if (null != zfyjMap.get(key)) {
                    zfyjMap.get(key).selfAdd(cloneBill.getBillbal());
                } else {
                    zfyjMap.put(key, new FabAmount(cloneBill.getBillbal()));
                }

                lastIndex++;
            }
        }
        // 处理最后切割日之后的数据
        if (validDateCount > 0) {
            bill.setCancelflag(splitterList.get(lastIndex - 1).getRightSection());
            bill.setBillamt(totalAmt.sub(sumAmt).getVal());
            bill.setBillbal(totalBal.getVal());
        }
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
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

    public AccountOperator getSub() {
        return sub;
    }

    public void setSub(AccountOperator sub) {
        this.sub = sub;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public String getNonAccrualDate() {
        return nonAccrualDate;
    }

    public void setNonAccrualDate(String nonAccrualDate) {
        this.nonAccrualDate = nonAccrualDate;
    }

    public String getChargeAginstFlag() {
        return chargeAginstFlag;
    }

    public void setChargeAginstFlag(String chargeAginstFlag) {
        this.chargeAginstFlag = chargeAginstFlag;
    }

    public String getChargeAginstDate() {
        return chargeAginstDate;
    }

    public void setChargeAginstDate(String chargeAginstDate) {
        this.chargeAginstDate = chargeAginstDate;
    }

    public AccountOperator getAdd() {
        return add;
    }

    public void setAdd(AccountOperator add) {
        this.add = add;
    }

    public FabAmount getSettleAmt() {
        return settleAmt;
    }

    public void setSettleAmt(FabAmount settleAmt) {
        this.settleAmt = settleAmt;
    }

    public FabAmount getCompensateDint() {
        return compensateDint;
    }

    public void setCompensateDint(FabAmount compensateDint) {
        this.compensateDint = compensateDint;
    }

    public FabAmount getNonAccrualPrin() {
        return nonAccrualPrin;
    }

    public void setNonAccrualPrin(FabAmount nonAccrualPrin) {
        this.nonAccrualPrin = nonAccrualPrin;
    }

    public FabAmount getnChargeAginstInt() {
        return nChargeAginstInt;
    }

    public void setnChargeAginstInt(FabAmount nChargeAginstInt) {
        this.nChargeAginstInt = nChargeAginstInt;
    }

    public FabAmount getnChargeAginstDint() {
        return nChargeAginstDint;
    }

    public void setnChargeAginstDint(FabAmount nChargeAginstDint) {
        this.nChargeAginstDint = nChargeAginstDint;
    }

    public FabAmount getComeInDint() {
        return comeInDint;
    }

    public void setComeInDint(FabAmount comeInDint) {
        this.comeInDint = comeInDint;
    }

    public FabAmount getnChargeCAInt() {
        return nChargeCAInt;
    }

    public void setnChargeCAInt(FabAmount nChargeCAInt) {
        this.nChargeCAInt = nChargeCAInt;
    }

    public FabAmount getChargeAginstDint() {
        return chargeAginstDint;
    }

    public void setChargeAginstDint(FabAmount chargeAginstDint) {
        this.chargeAginstDint = chargeAginstDint;
    }

    public FabAmount getChargeAginstInt() {
        return chargeAginstInt;
    }

    public void setChargeAginstInt(FabAmount chargeAginstInt) {
        this.chargeAginstInt = chargeAginstInt;
    }
}

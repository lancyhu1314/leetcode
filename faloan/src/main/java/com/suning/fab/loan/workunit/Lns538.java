package com.suning.fab.loan.workunit;

import com.suning.fab.loan.bo.BillSplitter;
import com.suning.fab.loan.bo.IntegerObj;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.la.NonAccrual;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈贷款核销子交易〉
 * 〈功能详细描述〉：贷款转非应计汇总查询
 *
 * @Author
 * @Date 2022-11-00
 * @see
 */
@Scope("prototype")
@Repository
public class Lns538 extends WorkUnit {
    //借据号
    String receiptNo;
    // 转非应计日期
    String nonAccrualDate;
    // 是否冲销非应计前收入
    String chargeAginstFlag;
    // 冲销收入开始日
    String chargeAginstDate;

    TblLnsbasicinfo lnsbasicinfo;
    String billType;
    String billList;

    @Override
    public void run() throws Exception {

        //查询主文件
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno1", receiptNo);
        param.put("openbrc", tranctx.getBrc());
        try {
            //取主文件信息
            lnsbasicinfo = DbAccessUtil.queryForObject("CUSTOMIZE.query_non_acctno", param, TblLnsbasicinfo.class);

        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_non_acctno");
        }

        if (null == lnsbasicinfo) {
            billType = "1";
            return;
        }

        //空值校验， 默认值赋值
        checkeInputAndDefault(lnsbasicinfo.getBeginintdate());

        //主文件校验
        checkBasicInfo();

        //查询已有的账单表
        LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), tranctx);
        IntegerObj maxTxseq = new IntegerObj(100);
        param.put("acctno", lnsbasicinfo.getAcctno());
        param.put("brc", tranctx.getBrc());
        List<TblLnsbill> tblLnsBills;
        try {
            tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", param, TblLnsbill.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_hisbills");
        }


        // 同一期GINT和DINT合并
        Map<String, TblLnsbill> removeMap = new HashMap<>();
        List<TblLnsbill> removeList = new ArrayList<>();
        for (TblLnsbill gintBill : tblLnsBills) {
            // 三编码作为key，处理过的不再处理
            removeMap.put(gintBill.getTrandate() + gintBill.getSerseqno().toString() + gintBill.getTxseq().toString(), gintBill);
            if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(gintBill.getBilltype())) {
                for (TblLnsbill dintBill : tblLnsBills) {
                    if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
                            ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(dintBill.getBilltype())
                            && gintBill.getPeriod() == dintBill.getPeriod()
                            && null == removeMap.get(dintBill.getTrandate() + dintBill.getSerseqno().toString() + dintBill.getTxseq().toString())) {
                        gintBill.setBillamt(new FabAmount(gintBill.getBillamt()).selfAdd(dintBill.getBillamt()).getVal());
                        gintBill.setBillbal(new FabAmount(gintBill.getBillbal()).selfAdd(dintBill.getBillbal()).getVal());
                        removeMap.put(dintBill.getTrandate() + dintBill.getSerseqno().toString() + dintBill.getTxseq().toString(), dintBill);
                        removeList.add(dintBill);

                        // 既有DINT又有GINT时，合并到DINT。   其余按原GINT合并在一起
                        if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT).contains(dintBill.getBilltype()))
                            gintBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
                    }
                }
            }
//            else if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(gintBill.getBilltype())) {
//                if (VarChecker.asList("2412608", "1212606", "3010011").contains(lnsbasicinfo.getPrdcode())) {
//                    gintBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
//                }
//            }
        }
        for (TblLnsbill list : removeList)
            tblLnsBills.remove(list);


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
        Map<String, NonAccrual> returnMap = new HashMap<>();
        for (TblLnsbill bill : tblLnsBills) {
            param.clear();

            // 本业务只处理罚息类账本
            if (!VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
                    ConstantDeclare.BILLTYPE.BILLTYPE_GINT)
                    .contains(bill.getBilltype()))
                continue;

//            // 多次转非应计，2和3的过滤
//            if (ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(lnsbasicinfo.getLoanstat()) &&
//                    VarChecker.asList("2", "3").contains(bill.getCancelflag()))
//                continue;


            // 创建拆分器list
            List<BillSplitter> splitterList = new ArrayList<>();
            // 首段拆分（通用）
            BillSplitter splitter = new BillSplitter();
            splitter.setLeftSection("1");
            splitter.setSplitDate(chargeAginstDate);
            splitter.setRightSection("2");
            splitterList.add(splitter);

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

            // 切分处理
            if (!ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBilltype()))
                splitSectionOperator(la, bill, splitterList, returnMap, maxTxseq);
        }


        List<NonAccrual> returnList = new ArrayList<>();
        for (Map.Entry<String, NonAccrual> map : returnMap.entrySet()) {
            map.getValue().setBillType(map.getKey().substring(0, 4));
            map.getValue().setPeriod(Integer.valueOf(map.getKey().substring(4, map.getKey().length())));
            map.getValue().setBillAmt(new FabAmount(map.getValue().getaAmt().add(map.getValue().getbAmt()).add(map.getValue().getcAmt()).getVal()));
            for (TblLnsbill bill : tblLnsBills) {
                if ((ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBilltype()) && VarChecker.asList("DINT", "GINT").contains(map.getKey().substring(0, 4))) ||
                        (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBilltype()) && VarChecker.asList("CINT").contains(map.getKey().substring(0, 4)))) {
                    if (bill.getPeriod().equals(Integer.valueOf(map.getKey().substring(4, map.getKey().length())))) {
                        map.getValue().setbDate(bill.getBegindate());
                        map.getValue().seteDate(bill.getEnddate());
                        map.getValue().setIntEDate(bill.getIntedate());
                    }
                }
            }
            // 垃圾数据特殊处理
            if (null == map.getValue().getbDate()) {
                for (TblLnsbill bill : tblLnsBills) {
                    if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBilltype())) {
                        if (bill.getPeriod().equals(Integer.valueOf(map.getKey().substring(4, map.getKey().length())))) {
                            map.getValue().setbDate(bill.getBegindate());
                            map.getValue().seteDate(bill.getEnddate());
                            map.getValue().setIntEDate(bill.getIntedate());
                            break;
                        }
                    }
                }
            }
            returnList.add(map.getValue());
        }

        // 无可拆账本 返2
        if (0 == returnList.size()) {
            billType = "2";
            return;
        }

        billList = JsonTransfer.ToJson(returnList);
        billType = "3";
    }


    //主文件校验
    private void checkBasicInfo() throws FabException {
        // 状态校验
        if (!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE).contains(lnsbasicinfo.getLoanstat().trim())) {
            throw new FabException("LNS254", receiptNo, lnsbasicinfo.getLoanstat().trim());
        }

        // 日期校验
        if (CalendarUtil.before(nonAccrualDate, lnsbasicinfo.getBeginintdate())) {
            throw new FabException("LNS256", "转非应计日期", "起息日");
        }
        if (CalendarUtil.before(tranctx.getTranDate(), nonAccrualDate)) {
            throw new FabException("LNS256", "当前日期", "转非应计日期");
        }
        if (CalendarUtil.before(nonAccrualDate, chargeAginstDate)) {
            throw new FabException("LNS256", "转非应计日期", "冲销收入开始日");
        }
        if ("N".equals(chargeAginstFlag) && !nonAccrualDate.equals(chargeAginstDate)) {
            throw new FabException("SPS106", "chargeAginstDate");
        }
    }

    //参数校验
    private void checkeInputAndDefault(String StartIntDate) throws FabException {

        // 空值校验
        if (VarChecker.isEmpty(receiptNo))
            throw new FabException("LNS055", "借据号");
        if (!VarChecker.asList("51030000", "51260000", "51310000", "51050000").contains(tranctx.getBrc())) {
            throw new FabException("LNS126", receiptNo, "只支持5103、5126、5131、5105的机构号");
        }

        // 转非应计日期，不传值默认为交易日（会计日、非自然日）
        if (VarChecker.isEmpty(nonAccrualDate))
            setNonAccrualDate(tranctx.getTranDate());

        // 是否冲销非应计前收入（Y/N），不传值默认Y
        if (VarChecker.isEmpty(chargeAginstFlag))
            setChargeAginstFlag("Y");

        // 冲销收入开始日，不传值默认起息日
        if (VarChecker.isEmpty(chargeAginstDate))
            if ("N".equals(chargeAginstFlag))
                setChargeAginstDate(nonAccrualDate);
            else if ("Y".equals(chargeAginstFlag))
                setChargeAginstDate(StartIntDate);
    }

    public void splitSectionOperator(LoanAgreement la, TblLnsbill bill, List<BillSplitter> splitterList, Map<String, NonAccrual> returnMap, IntegerObj maxTxseq) throws IOException, ClassNotFoundException, FabException {
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
                    setAmt(returnMap, bill);
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

                // 设置当期各状态金额、余额
                setAmt(returnMap, bill);
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
                // 设置当期各状态金额、余额
                setAmt(returnMap, cloneBill);

                bill.setBegindate(splitterList.get(i).getSplitDate());
                lastIndex++;
            }
        }
        // 处理最后切割日之后的数据
        if (validDateCount > 0) {
            bill.setCancelflag(splitterList.get(lastIndex - 1).getRightSection());
            bill.setBillamt(totalAmt.sub(sumAmt).getVal());
            bill.setBillbal(totalBal.getVal());
            // 设置当期各状态金额、余额
            setAmt(returnMap, bill);
        }


    }

    public void setAmt(Map<String, NonAccrual> returnMap, TblLnsbill bill) {
        // 统计各阶段金额、余额
        String key = bill.getBilltype() + bill.getPeriod().toString();
        if (null == returnMap.get(key)) {
            returnMap.put(key, new NonAccrual());
        }


        if (bill.getCancelflag().equals("1")) {
            returnMap.get(key).getaAmt().selfAdd(bill.getBillamt());
        } else if (bill.getCancelflag().equals("2")) {
            returnMap.get(key).getbAmt().selfAdd(bill.getBillamt());
        } else if (bill.getCancelflag().equals("3")) {
            returnMap.get(key).getcAmt().selfAdd(bill.getBillamt());
        }
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

    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public String getBillList() {
        return billList;
    }

    public void setBillList(String billList) {
        this.billList = billList;
    }
}

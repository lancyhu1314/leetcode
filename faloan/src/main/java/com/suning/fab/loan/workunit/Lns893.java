package com.suning.fab.loan.workunit;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705 MYP
 * @Date Created in 9:52 2019/5/28
 * @see
 */

@Scope("prototype")
@Repository
public class Lns893 extends WorkUnit {

    //tradeNo
    private String 	tradeNo893;
    //tradeTime
    private String 	tradeTime893;
    //goodsDesc
    private String 	goodsDesc893;
    //period
    private Integer period893;
    //payNo
    private String 	payNo893;
    //investee
    private String 	investee893;
    //investeeNo
    private String 	investeeNo893;
    //payList
    private ListMap payList893;
    //investeeList
    private ListMap investeeList893;
    //customId
    private String customId893;
    //repayNo
    private String repayNo893;
    //repayTotalAmount
    private FabAmount repayTotalAmount893;
    //repayAmount
    private FabAmount repayAmount893;
    //settleFlag
    private String settleFlag893;
    //repayTime
    private String repayTime893;
    //repayList
    private String repayList893;
    //repayAmt
    /**
     *
     */
    //repayAmt
    private FabAmount repayAmt893;
    //repayAmt
    private Integer errSerSeq893;
    //repayAmt
    private Integer graceDays893;
    //repayAmt
    private Integer periodNum893;
    //repayAmt
    private String	repayDate893;
    //repayAmt
    private String debtCompany893;
    //repayAmt
    private FabAmount debtAmt893;
    //repayAmt
    private FabAmount contractAmt893;
    //repayAmt
    private FabAmount discountAmt893;
    //repayAmt
    private FabAmount forfeitAmt893;
    //repayAmt
    private FabAmount intAmt893;
    //repayAmt
    private FabAmount offsetAmt893;
    //repayAmt
    private FabAmount prinAmt893;
    //repayAmt
    private FabAmount refundAmt893;
    //repayAmt
    private FabRate compoundRate893;
    //repayAmt
    private FabAmount    feeRate893;
    //repayAmt
    private FabRate normalRate893;
    //repayAmt
    private FabRate overdueRate893;
    //repayAmt
    private String  cashFlag893;
    //repayAmt
    private String  channelType893;
    //repayAmt
    private String  compoundRateType893;
    //repayAmt
    private String  contractNo893;
    //repayAmt
    private String  customName893;
    //repayAmt
    private String  customType893;
    //repayAmt
    private String  discountFlag893;
    //repayAmt
    private String  errDate893;
    //repayAmt
    private String  fundChannel893;
    //repayAmt
    private String  intPerUnit893;
    //repayAmt
    private String  investeeId893;
    //repayAmt
    private FabAmount  investeePrin893;
    //repayAmt
    private FabAmount  investeeInt893;
    //repayAmt
    private FabAmount  investeeDint893;
    //repayAmt
    private String  investeeFlag893;
    //repayAmt
    private	String	investMode893; //投放模式：保理、消费贷
    //repayAmt
    private String  loanType893;
    //repayAmt
    private String  normalRateType893;
    //repayAmt
    private String  openBrc893;
    //repayAmt
    private String  openDate893;
    //repayAmt
    private String  outSerialNo893;
    //repayAmt
    private String  overdueRateType893;
    //repayAmt
    private String  priorType893;
    //repayAmt
    private String  receiptNo893;
    //repayAmt
    private String  repayAcctNo893;
    //repayAmt
    private String  repayChannel893;
    //repayAmt
    private String  repayWay893;
    //repayAmt
    private String  settleChannel893;
    //repayAmt
    private String  startIntDate893;
    //repayAmt
    private String  tranType893;
    //repayAmt
    private String	periodType893;
    //repayAmt
    private String  acctStat893; 	//账户类型 -- 需要去FaLoanPublic中增加该字段
    private String  loanStat893;	//贷款状态
    //repayAmt
    private Integer repayTerm893;
    //repayAmt
    private String  endDate893;
    //repayAmt
    private String  startDate893;
    //repayAmt
    private String  bankSubject893;
    //repayAmt
    private String  calcIntFlag2893;
    //repayAmt
    private String  calcIntFlag8931;
    //repayAmt
    private String  termeDate893;
    //repayAmt
    private String  intbDate893;
    //repayAmt
    private FabAmount termPrin893;
    //repayAmt
    private FabAmount termInt893;
    //repayAmt
    private String  inteDate893;
    //repayAmt
    private String  days893;
    //repayAmt
    private Double  recoveryRate893;
    //repayAmt
    private Double  finalRate893;
    //repayAmt
    private Double  rentRate893;
    //repayAmt
    private Double  floatRate893;

    @Autowired
    @SuppressWarnings("unused")
    @Override
    public void run() throws Exception {

        if( 1 == 0 )
        {
            LoggerUtil.debug(tradeNo893+
                    tradeTime893+
                    goodsDesc893+
                    period893+
                    payNo893+
                    investee893+
                    investeeNo893+
                    payList893+
                    investeeList893+
                    customId893+
                    repayNo893+
                    repayTotalAmount893+
                    repayAmount893+
                    settleFlag893+
                    repayTime893+
                    repayList893+
                    repayAmt893+
                    errSerSeq893+
                    graceDays893+
                    periodNum893+
                    repayDate893+
                    debtCompany893+
                    debtAmt893+
                    contractAmt893+
                    discountAmt893+
                    forfeitAmt893+
                    intAmt893+
                    offsetAmt893+
                    prinAmt893+
                    refundAmt893+
                    compoundRate893+
                    feeRate893+
                    normalRate893+
                    overdueRate893+
                    cashFlag893+
                    channelType893+
                    compoundRateType893+
                    contractNo893+
                    customName893+
                    customType893+
                    discountFlag893+
                    errDate893+
                    fundChannel893+
                    intPerUnit893+
                    investeeId893+
                    investeePrin893+
                    investeeInt893+
                    investeeDint893+
                    investeeFlag893+
                    investMode893+
                    loanType893+
                    normalRateType893+
                    openBrc893+
                    openDate893+
                    outSerialNo893+
                    overdueRateType893+
                    priorType893+
                    receiptNo893+
                    repayAcctNo893+
                    repayChannel893+
                    repayWay893+
                    settleChannel893+
                    startIntDate893+
                    tranType893+
                    periodType893+
                    acctStat893+
                    loanStat893+
                    repayTerm893+
                    endDate893+
                    startDate893+
                    bankSubject893+
                    calcIntFlag2893+
                    calcIntFlag8931+
                    termeDate893+
                    intbDate893+
                    termPrin893+
                    termInt893+
                    inteDate893+
                    days893+
                    recoveryRate893+
                    finalRate893+
                    rentRate893+
                    floatRate893);
        }
        //开户放款
        //放款冲销
        //放款撤销
        //非标放款冲销
        //房抵贷冲销
        //还款
        //指定类型还款
        //非标还款
        //通讯租赁退货
        //房抵贷还款
        //汽车租赁还款
        //贷款事前利息减免
        //贷款事后利息减免
        //非标贷款事前利息减免
        //非标计划试算
        //房抵贷计划试算
        //任性贷退款给贷款人
        //任性贷结算给云商
        //逾期还款查询
        //预约还款查询
        //非标预约还款查询
        //通讯还款试算
        //汽车租赁预约还款查询
        //房抵贷预约还款查询
        //租赁试算
        //按揭贷款还款计划试算
        //还款计划查询
        //非标还款计划查询
        //房抵贷还款计划查询
        //贷款账户统计查询
        //预收账户充退
        //预收账户充值
        //预收账户余额查询
        //汽车租赁账户充退
        //汽车租赁账户充值
        //汽车租赁账户查询
        //非标开户放款
        //通讯租赁开户
        //房抵贷开户
        //汽车租赁开户
        //还款试算
        //房抵贷还款试算
        //利息计提
        //形态转列
        //贷款销户
        //生成下一期还款计划
        //贷款损失准备计提
        //贷款变更
        //还款顺序变更
        //贷款任意冲正
        //贷款差错账调整
        //利息摊销
        //贷款状态更新
        //贷款放款信息查询
        //贷款账户查询
        //预收账户明细查询
        //贷款还款明细查询
        //利息计提明细查询
        //摊销明细查询
        //账单表查询
        //贷款账户明细查询
        //幂等登记簿查询
        //备份数据查询
        //数据恢复
    }
    //获取getTradeNo893
    public String getTradeNo893() {
        return tradeNo893;
    }
    //获取setTradeNo893
    public void setTradeNo893(String tradeNo893) {
        this.tradeNo893 = tradeNo893;
    }
    //获取getTradeTime893
    public String getTradeTime893() {
        return tradeTime893;
    }
    //获取getTradeTime893
    public void setTradeTime893(String tradeTime893) {
        this.tradeTime893 = tradeTime893;
    }
    //获取getGoodsDesc893
    public String getGoodsDesc893() {
        return goodsDesc893;
    }
    //获取getGoodsDesc893
    public void setGoodsDesc893(String goodsDesc893) {
        this.goodsDesc893 = goodsDesc893;
    }
    //获取getGoodsDesc893
    public Integer getPeriod893() {
        return period893;
    }
    //获取getGoodsDesc893
    public void setPeriod893(Integer period893) {
        this.period893 = period893;
    }
    //获取getGoodsDesc893
    public String getPayNo893() {
        return payNo893;
    }
    //获取getGoodsDesc893
    public void setPayNo893(String payNo893) {
        this.payNo893 = payNo893;
    }
    //获取getGoodsDesc893
    public String getInvestee893() {
        return investee893;
    }
    //获取getGoodsDesc893
    public void setInvestee893(String investee893) {
        this.investee893 = investee893;
    }
    //获取getInvesteeNo893
    public String getInvesteeNo893() {
        return investeeNo893;
    }
    //获取setInvesteeNo893
    public void setInvesteeNo893(String investeeNo893) {
        this.investeeNo893 = investeeNo893;
    }
    //获取setInvesteeNo893
    public ListMap getPayList893() {
        return payList893;
    }
    //获取setInvesteeNo893
    public void setPayList893(ListMap payList893) {
        this.payList893 = payList893;
    }
    //获取setInvesteeNo893
    public ListMap getInvesteeList893() {
        return investeeList893;
    }
    //获取setInvesteeNo893
    public void setInvesteeList893(ListMap investeeList893) {
        this.investeeList893 = investeeList893;
    }
    //获取setInvesteeNo893
    public String getCustomId893() {
        return customId893;
    }
    //获取setInvesteeNo893
    public void setCustomId893(String customId893) {
        this.customId893 = customId893;
    }
    //获取getRepayNo893
    public String getRepayNo893() {
        return repayNo893;
    }
    //获取setRepayNo893
    public void setRepayNo893(String repayNo893) {
        this.repayNo893 = repayNo893;
    }
    //获取getRepayTotalAmount893
    public FabAmount getRepayTotalAmount893() {
        return repayTotalAmount893;
    }
    //获取setRepayTotalAmount893
    public void setRepayTotalAmount893(FabAmount repayTotalAmount893) {
        this.repayTotalAmount893 = repayTotalAmount893;
    }
    //获取getRepayAmount893
    public FabAmount getRepayAmount893() {
        return repayAmount893;
    }
    //获取setRepayAmount893
    public void setRepayAmount893(FabAmount repayAmount893) {
        this.repayAmount893 = repayAmount893;
    }
    //获取setRepayAmount893
    public String getSettleFlag893() {
        return settleFlag893;
    }
    //获取setRepayAmount893
    public void setSettleFlag893(String settleFlag893) {
        this.settleFlag893 = settleFlag893;
    }
    //获取setRepayAmount893
    public String getRepayTime893() {
        return repayTime893;
    }
    //获取setRepayAmount893
    public void setRepayTime893(String repayTime893) {
        this.repayTime893 = repayTime893;
    }
    //获取setRepayAmount893
    public String getRepayList893() {
        return repayList893;
    }
    //获取setRepayList893
    public void setRepayList893(String repayList893) {
        this.repayList893 = repayList893;
    }
    //获取getRepayAmt893
    public FabAmount getRepayAmt893() {
        return repayAmt893;
    }
    //获取setRepayAmt893
    public void setRepayAmt893(FabAmount repayAmt893) {
        this.repayAmt893 = repayAmt893;
    }
    //获取setRepayAmount893
    public Integer getErrSerSeq893() {
        return errSerSeq893;
    }
    //获取setRepayAmount893
    public void setErrSerSeq893(Integer errSerSeq893) {
        this.errSerSeq893 = errSerSeq893;
    }
    //获取getGraceDays893
    public Integer getGraceDays893() {
        return graceDays893;
    }
    //获取setGraceDays893
    public void setGraceDays893(Integer graceDays893) {
        this.graceDays893 = graceDays893;
    }
    //获取getPeriodNum893
    public Integer getPeriodNum893() {
        return periodNum893;
    }
    //获取setPeriodNum893
    public void setPeriodNum893(Integer periodNum893) {
        this.periodNum893 = periodNum893;
    }
    //获取getPeriodNum893
    public String getRepayDate893() {
        return repayDate893;
    }
    //获取setRepayDate893
    public void setRepayDate893(String repayDate893) {
        this.repayDate893 = repayDate893;
    }
    //获取getDebtCompany893
    public String getDebtCompany893() {
        return debtCompany893;
    }
    //获取setDebtCompany893
    public void setDebtCompany893(String debtCompany893) {
        this.debtCompany893 = debtCompany893;
    }
    //获取setDebtCompany893
    public FabAmount getDebtAmt893() {
        return debtAmt893;
    }
    //获取setDebtCompany893
    public void setDebtAmt893(FabAmount debtAmt893) {
        this.debtAmt893 = debtAmt893;
    }
    //获取getContractAmt893
    public FabAmount getContractAmt893() {
        return contractAmt893;
    }
    //获取setContractAmt893
    public void setContractAmt893(FabAmount contractAmt893) {
        this.contractAmt893 = contractAmt893;
    }
    //获取getDiscountAmt893
    public FabAmount getDiscountAmt893() {
        return discountAmt893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setDiscountAmt893(FabAmount discountAmt893) {
        this.discountAmt893 = discountAmt893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getForfeitAmt893() {
        return forfeitAmt893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setForfeitAmt893(FabAmount forfeitAmt893) {
        this.forfeitAmt893 = forfeitAmt893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getIntAmt893() {
        return intAmt893;
    }
    public void setIntAmt893(FabAmount intAmt893) {
        this.intAmt893 = intAmt893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getOffsetAmt893() {
        return offsetAmt893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setOffsetAmt893(FabAmount offsetAmt893) {
        this.offsetAmt893 = offsetAmt893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getPrinAmt893() {
        return prinAmt893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setPrinAmt893(FabAmount prinAmt893) {
        this.prinAmt893 = prinAmt893;
    }
    //获取getRefundAmt893用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getRefundAmt893() {
        return refundAmt893;
    }
    //获取setRefundAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setRefundAmt893(FabAmount refundAmt893) {
        this.refundAmt893 = refundAmt893;
    }
    //获取getCompoundRate893用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getCompoundRate893() {
        return compoundRate893;
    }
    //获取setCompoundRate893用来进行隔天发方法获取一遍用于代码的解读
    public void setCompoundRate893(FabRate compoundRate893) {
        this.compoundRate893 = compoundRate893;
    }
    //获取getFeeRate893用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getFeeRate893() {
        return feeRate893;
    }
    //获取setFeeRate893用来进行隔天发方法获取一遍用于代码的解读
    public void setFeeRate893(FabAmount feeRate893) {
        this.feeRate893 = feeRate893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getNormalRate893() {
        return normalRate893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setNormalRate893(FabRate normalRate893) {
        this.normalRate893 = normalRate893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getOverdueRate893() {
        return overdueRate893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setOverdueRate893(FabRate overdueRate893) {
        this.overdueRate893 = overdueRate893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public String getCashFlag893() {
        return cashFlag893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setCashFlag893(String cashFlag893) {
        this.cashFlag893 = cashFlag893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public String getChannelType893() {
        return channelType893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setChannelType893(String channelType893) {
        this.channelType893 = channelType893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public String getCompoundRateType893() {
        return compoundRateType893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public void setCompoundRateType893(String compoundRateType893) {
        this.compoundRateType893 = compoundRateType893;
    }
    //获取getDiscountAmt893用来进行隔天发方法获取一遍用于代码的解读
    public String getContractNo893() {
        return contractNo893;
    }
    //获取setContractNo893用来进行隔天发方法获取一遍用于代码的解读
    public void setContractNo893(String contractNo893) {
        this.contractNo893 = contractNo893;
    }
    //获取getCustomName893用来进行隔天发方法获取一遍用于代码的解读
    public String getCustomName893() {
        return customName893;
    }
    //获取setCustomName893用来进行隔天发方法获取一遍用于代码的解读
    public void setCustomName893(String customName893) {
        this.customName893 = customName893;
    }
    //获取getCustomType893用来进行隔天发方法获取一遍用于代码的解读
    public String getCustomType893() {
        return customType893;
    }
    public void setCustomType893(String customType893) {
        this.customType893 = customType893;
    }
    //开户放款
    //放款冲销
    //放款撤销
    //非标放款冲销
    //房抵贷冲销
    //还款
    //指定类型还款
    //非标还款
    //通讯租赁退货
    //房抵贷还款
    //汽车租赁还款
    //贷款事前利息减免
    //贷款事后利息减免
    //非标贷款事前利息减免
    //非标计划试算
    //房抵贷计划试算
    //任性贷退款给贷款人
    //任性贷结算给云商
    //逾期还款查询
    //预约还款查询
    //非标预约还款查询
    //通讯还款试算
    //汽车租赁预约还款查询
    //房抵贷预约还款查询
    //租赁试算
    //按揭贷款还款计划试算
    //还款计划查询
    //非标还款计划查询
    //房抵贷还款计划查询
    //贷款账户统计查询
    //预收账户充退
    //预收账户充值
    //预收账户余额查询
    //汽车租赁账户充退
    //汽车租赁账户充值
    //汽车租赁账户查询
    //非标开户放款
    //通讯租赁开户
    //房抵贷开户
    //汽车租赁开户
    //还款试算
    //房抵贷还款试算
    //利息计提
    //形态转列
    //贷款销户
    //生成下一期还款计划
    //贷款损失准备计提
    //贷款变更
    //还款顺序变更
    //贷款任意冲正
    //贷款差错账调整
    //利息摊销
    //贷款状态更新
    //贷款放款信息查询
    //贷款账户查询
    //预收账户明细查询
    //贷款还款明细查询
    //利息计提明细查询
    //摊销明细查询
    //账单表查询
    //贷款账户明细查询
    //幂等登记簿查询
    //备份数据查询
    //数据恢复


}



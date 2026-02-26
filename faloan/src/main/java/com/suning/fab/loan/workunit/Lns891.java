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
public class Lns891 extends WorkUnit {

    //tradeNo
    private String 	tradeNo891;
    //tradeTime
    private String 	tradeTime891;
    //goodsDesc
    private String 	goodsDesc891;
    //period
    private Integer period891;
    //payNo
    private String 	payNo891;
    //investee
    private String 	investee891;
    //investeeNo
    private String 	investeeNo891;
    //payList
    private ListMap payList891;
    //investeeList
    private ListMap investeeList891;
    //customId
    private String customId891;
    //repayNo
    private String repayNo891;
    //repayTotalAmount
    private FabAmount repayTotalAmount891;
    //repayAmount
    private FabAmount repayAmount891;
    //settleFlag
    private String settleFlag891;
    //repayTime
    private String repayTime891;
    //repayList
    private String repayList891;
    //repayAmt
    /**
     *
     */
    //repayAmt
    private FabAmount repayAmt891;
    //repayAmt
    private Integer errSerSeq891;
    //repayAmt
    private Integer graceDays891;
    //repayAmt
    private Integer periodNum891;
    //repayAmt
    private String	repayDate891;
    //repayAmt
    private String debtCompany891;
    //repayAmt
    private FabAmount debtAmt891;
    //repayAmt
    private FabAmount contractAmt891;
    //repayAmt
    private FabAmount discountAmt891;
    //repayAmt
    private FabAmount forfeitAmt891;
    //repayAmt
    private FabAmount intAmt891;
    //repayAmt
    private FabAmount offsetAmt891;
    //repayAmt
    private FabAmount prinAmt891;
    //repayAmt
    private FabAmount refundAmt891;
    //repayAmt
    private FabRate compoundRate891;
    //repayAmt
    private FabAmount    feeRate891;
    //repayAmt
    private FabRate normalRate891;
    //repayAmt
    private FabRate overdueRate891;
    //repayAmt
    private String  cashFlag891;
    //repayAmt
    private String  channelType891;
    //repayAmt
    private String  compoundRateType891;
    //repayAmt
    private String  contractNo891;
    //repayAmt
    private String  customName891;
    //repayAmt
    private String  customType891;
    //repayAmt
    private String  discountFlag891;
    //repayAmt
    private String  errDate891;
    //repayAmt
    private String  fundChannel891;
    //repayAmt
    private String  intPerUnit891;
    //repayAmt
    private String  investeeId891;
    //repayAmt
    private FabAmount  investeePrin891;
    //repayAmt
    private FabAmount  investeeInt891;
    //repayAmt
    private FabAmount  investeeDint891;
    //repayAmt
    private String  investeeFlag891;
    //repayAmt
    private	String	investMode891; //投放模式：保理、消费贷
    //repayAmt
    private String  loanType891;
    //repayAmt
    private String  normalRateType891;
    //repayAmt
    private String  openBrc891;
    //repayAmt
    private String  openDate891;
    //repayAmt
    private String  outSerialNo891;
    //repayAmt
    private String  overdueRateType891;
    //repayAmt
    private String  priorType891;
    //repayAmt
    private String  receiptNo891;
    //repayAmt
    private String  repayAcctNo891;
    //repayAmt
    private String  repayChannel891;
    //repayAmt
    private String  repayWay891;
    //repayAmt
    private String  settleChannel891;
    //repayAmt
    private String  startIntDate891;
    //repayAmt
    private String  tranType891;
    //repayAmt
    private String	periodType891;
    //repayAmt
    private String  acctStat891; 	//账户类型 -- 需要去FaLoanPublic中增加该字段
    private String  loanStat891;	//贷款状态
    //repayAmt
    private Integer repayTerm891;
    //repayAmt
    private String  endDate891;
    //repayAmt
    private String  startDate891;
    //repayAmt
    private String  bankSubject891;
    //repayAmt
    private String  calcIntFlag2891;
    //repayAmt
    private String  calcIntFlag8911;
    //repayAmt
    private String  termeDate891;
    //repayAmt
    private String  intbDate891;
    //repayAmt
    private FabAmount termPrin891;
    //repayAmt
    private FabAmount termInt891;
    //repayAmt
    private String  inteDate891;
    //repayAmt
    private String  days891;
    //repayAmt
    private Double  recoveryRate891;
    //repayAmt
    private Double  finalRate891;
    //repayAmt
    private Double  rentRate891;
    //repayAmt
    private Double  floatRate891;

    @Autowired
    @SuppressWarnings("unused")
    @Override
    public void run() throws Exception {

        if( 1 == 0 )
        {
            LoggerUtil.debug(tradeNo891+
                    tradeTime891+
                    goodsDesc891+
                    period891+
                    payNo891+
                    investee891+
                    investeeNo891+
                    payList891+
                    investeeList891+
                    customId891+
                    repayNo891+
                    repayTotalAmount891+
                    repayAmount891+
                    settleFlag891+
                    repayTime891+
                    repayList891+
                    repayAmt891+
                    errSerSeq891+
                    graceDays891+
                    periodNum891+
                    repayDate891+
                    debtCompany891+
                    debtAmt891+
                    contractAmt891+
                    discountAmt891+
                    forfeitAmt891+
                    intAmt891+
                    offsetAmt891+
                    prinAmt891+
                    refundAmt891+
                    compoundRate891+
                    feeRate891+
                    normalRate891+
                    overdueRate891+
                    cashFlag891+
                    channelType891+
                    compoundRateType891+
                    contractNo891+
                    customName891+
                    customType891+
                    discountFlag891+
                    errDate891+
                    fundChannel891+
                    intPerUnit891+
                    investeeId891+
                    investeePrin891+
                    investeeInt891+
                    investeeDint891+
                    investeeFlag891+
                    investMode891+
                    loanType891+
                    normalRateType891+
                    openBrc891+
                    openDate891+
                    outSerialNo891+
                    overdueRateType891+
                    priorType891+
                    receiptNo891+
                    repayAcctNo891+
                    repayChannel891+
                    repayWay891+
                    settleChannel891+
                    startIntDate891+
                    tranType891+
                    periodType891+
                    acctStat891+
                    loanStat891+
                    repayTerm891+
                    endDate891+
                    startDate891+
                    bankSubject891+
                    calcIntFlag2891+
                    calcIntFlag8911+
                    termeDate891+
                    intbDate891+
                    termPrin891+
                    termInt891+
                    inteDate891+
                    days891+
                    recoveryRate891+
                    finalRate891+
                    rentRate891+
                    floatRate891);
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
    //获取getTradeNo891
    public String getTradeNo891() {
        return tradeNo891;
    }
    //获取setTradeNo891
    public void setTradeNo891(String tradeNo891) {
        this.tradeNo891 = tradeNo891;
    }
    //获取getTradeTime891
    public String getTradeTime891() {
        return tradeTime891;
    }
    //获取getTradeTime891
    public void setTradeTime891(String tradeTime891) {
        this.tradeTime891 = tradeTime891;
    }
    //获取getGoodsDesc891
    public String getGoodsDesc891() {
        return goodsDesc891;
    }
    //获取getGoodsDesc891
    public void setGoodsDesc891(String goodsDesc891) {
        this.goodsDesc891 = goodsDesc891;
    }
    //获取getGoodsDesc891
    public Integer getPeriod891() {
        return period891;
    }
    //获取getGoodsDesc891
    public void setPeriod891(Integer period891) {
        this.period891 = period891;
    }
    //获取getGoodsDesc891
    public String getPayNo891() {
        return payNo891;
    }
    //获取getGoodsDesc891
    public void setPayNo891(String payNo891) {
        this.payNo891 = payNo891;
    }
    //获取getGoodsDesc891
    public String getInvestee891() {
        return investee891;
    }
    //获取getGoodsDesc891
    public void setInvestee891(String investee891) {
        this.investee891 = investee891;
    }
    //获取getInvesteeNo891
    public String getInvesteeNo891() {
        return investeeNo891;
    }
    //获取setInvesteeNo891
    public void setInvesteeNo891(String investeeNo891) {
        this.investeeNo891 = investeeNo891;
    }
    //获取setInvesteeNo891
    public ListMap getPayList891() {
        return payList891;
    }
    //获取setInvesteeNo891
    public void setPayList891(ListMap payList891) {
        this.payList891 = payList891;
    }
    //获取setInvesteeNo891
    public ListMap getInvesteeList891() {
        return investeeList891;
    }
    //获取setInvesteeNo891
    public void setInvesteeList891(ListMap investeeList891) {
        this.investeeList891 = investeeList891;
    }
    //获取setInvesteeNo891
    public String getCustomId891() {
        return customId891;
    }
    //获取setInvesteeNo891
    public void setCustomId891(String customId891) {
        this.customId891 = customId891;
    }
    //获取getRepayNo891
    public String getRepayNo891() {
        return repayNo891;
    }
    //获取setRepayNo891
    public void setRepayNo891(String repayNo891) {
        this.repayNo891 = repayNo891;
    }
    //获取getRepayTotalAmount891
    public FabAmount getRepayTotalAmount891() {
        return repayTotalAmount891;
    }
    //获取setRepayTotalAmount891
    public void setRepayTotalAmount891(FabAmount repayTotalAmount891) {
        this.repayTotalAmount891 = repayTotalAmount891;
    }
    //获取getRepayAmount891
    public FabAmount getRepayAmount891() {
        return repayAmount891;
    }
    //获取setRepayAmount891
    public void setRepayAmount891(FabAmount repayAmount891) {
        this.repayAmount891 = repayAmount891;
    }
    //获取setRepayAmount891
    public String getSettleFlag891() {
        return settleFlag891;
    }
    //获取setRepayAmount891
    public void setSettleFlag891(String settleFlag891) {
        this.settleFlag891 = settleFlag891;
    }
    //获取setRepayAmount891
    public String getRepayTime891() {
        return repayTime891;
    }
    //获取setRepayAmount891
    public void setRepayTime891(String repayTime891) {
        this.repayTime891 = repayTime891;
    }
    //获取setRepayAmount891
    public String getRepayList891() {
        return repayList891;
    }
    //获取setRepayList891
    public void setRepayList891(String repayList891) {
        this.repayList891 = repayList891;
    }
    //获取getRepayAmt891
    public FabAmount getRepayAmt891() {
        return repayAmt891;
    }
    //获取setRepayAmt891
    public void setRepayAmt891(FabAmount repayAmt891) {
        this.repayAmt891 = repayAmt891;
    }
    //获取setRepayAmount891
    public Integer getErrSerSeq891() {
        return errSerSeq891;
    }
    //获取setRepayAmount891
    public void setErrSerSeq891(Integer errSerSeq891) {
        this.errSerSeq891 = errSerSeq891;
    }
    //获取getGraceDays891
    public Integer getGraceDays891() {
        return graceDays891;
    }
    //获取setGraceDays891
    public void setGraceDays891(Integer graceDays891) {
        this.graceDays891 = graceDays891;
    }
    //获取getPeriodNum891
    public Integer getPeriodNum891() {
        return periodNum891;
    }
    //获取setPeriodNum891
    public void setPeriodNum891(Integer periodNum891) {
        this.periodNum891 = periodNum891;
    }
    //获取getPeriodNum891
    public String getRepayDate891() {
        return repayDate891;
    }
    //获取setRepayDate891
    public void setRepayDate891(String repayDate891) {
        this.repayDate891 = repayDate891;
    }
    //获取getDebtCompany891
    public String getDebtCompany891() {
        return debtCompany891;
    }
    //获取setDebtCompany891
    public void setDebtCompany891(String debtCompany891) {
        this.debtCompany891 = debtCompany891;
    }
    //获取setDebtCompany891
    public FabAmount getDebtAmt891() {
        return debtAmt891;
    }
    //获取setDebtCompany891
    public void setDebtAmt891(FabAmount debtAmt891) {
        this.debtAmt891 = debtAmt891;
    }
    //获取getContractAmt891
    public FabAmount getContractAmt891() {
        return contractAmt891;
    }
    //获取setContractAmt891
    public void setContractAmt891(FabAmount contractAmt891) {
        this.contractAmt891 = contractAmt891;
    }
    //获取getDiscountAmt891
    public FabAmount getDiscountAmt891() {
        return discountAmt891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setDiscountAmt891(FabAmount discountAmt891) {
        this.discountAmt891 = discountAmt891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getForfeitAmt891() {
        return forfeitAmt891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setForfeitAmt891(FabAmount forfeitAmt891) {
        this.forfeitAmt891 = forfeitAmt891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getIntAmt891() {
        return intAmt891;
    }
    public void setIntAmt891(FabAmount intAmt891) {
        this.intAmt891 = intAmt891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getOffsetAmt891() {
        return offsetAmt891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setOffsetAmt891(FabAmount offsetAmt891) {
        this.offsetAmt891 = offsetAmt891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getPrinAmt891() {
        return prinAmt891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setPrinAmt891(FabAmount prinAmt891) {
        this.prinAmt891 = prinAmt891;
    }
    //获取getRefundAmt891用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getRefundAmt891() {
        return refundAmt891;
    }
    //获取setRefundAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setRefundAmt891(FabAmount refundAmt891) {
        this.refundAmt891 = refundAmt891;
    }
    //获取getCompoundRate891用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getCompoundRate891() {
        return compoundRate891;
    }
    //获取setCompoundRate891用来进行隔天发方法获取一遍用于代码的解读
    public void setCompoundRate891(FabRate compoundRate891) {
        this.compoundRate891 = compoundRate891;
    }
    //获取getFeeRate891用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getFeeRate891() {
        return feeRate891;
    }
    //获取setFeeRate891用来进行隔天发方法获取一遍用于代码的解读
    public void setFeeRate891(FabAmount feeRate891) {
        this.feeRate891 = feeRate891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getNormalRate891() {
        return normalRate891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setNormalRate891(FabRate normalRate891) {
        this.normalRate891 = normalRate891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getOverdueRate891() {
        return overdueRate891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setOverdueRate891(FabRate overdueRate891) {
        this.overdueRate891 = overdueRate891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public String getCashFlag891() {
        return cashFlag891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setCashFlag891(String cashFlag891) {
        this.cashFlag891 = cashFlag891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public String getChannelType891() {
        return channelType891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setChannelType891(String channelType891) {
        this.channelType891 = channelType891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public String getCompoundRateType891() {
        return compoundRateType891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public void setCompoundRateType891(String compoundRateType891) {
        this.compoundRateType891 = compoundRateType891;
    }
    //获取getDiscountAmt891用来进行隔天发方法获取一遍用于代码的解读
    public String getContractNo891() {
        return contractNo891;
    }
    //获取setContractNo891用来进行隔天发方法获取一遍用于代码的解读
    public void setContractNo891(String contractNo891) {
        this.contractNo891 = contractNo891;
    }
    //获取getCustomName891用来进行隔天发方法获取一遍用于代码的解读
    public String getCustomName891() {
        return customName891;
    }
    //获取setCustomName891用来进行隔天发方法获取一遍用于代码的解读
    public void setCustomName891(String customName891) {
        this.customName891 = customName891;
    }
    //获取getCustomType891用来进行隔天发方法获取一遍用于代码的解读
    public String getCustomType891() {
        return customType891;
    }
    public void setCustomType891(String customType891) {
        this.customType891 = customType891;
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



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
public class Lns892 extends WorkUnit {

    //tradeNo
    private String 	tradeNo892;
    //tradeTime
    private String 	tradeTime892;
    //goodsDesc
    private String 	goodsDesc892;
    //period
    private Integer period892;
    //payNo
    private String 	payNo892;
    //investee
    private String 	investee892;
    //investeeNo
    private String 	investeeNo892;
    //payList
    private ListMap payList892;
    //investeeList
    private ListMap investeeList892;
    //customId
    private String customId892;
    //repayNo
    private String repayNo892;
    //repayTotalAmount
    private FabAmount repayTotalAmount892;
    //repayAmount
    private FabAmount repayAmount892;
    //settleFlag
    private String settleFlag892;
    //repayTime
    private String repayTime892;
    //repayList
    private String repayList892;
    //repayAmt
    /**
     *
     */
    //repayAmt
    private FabAmount repayAmt892;
    //repayAmt
    private Integer errSerSeq892;
    //repayAmt
    private Integer graceDays892;
    //repayAmt
    private Integer periodNum892;
    //repayAmt
    private String	repayDate892;
    //repayAmt
    private String debtCompany892;
    //repayAmt
    private FabAmount debtAmt892;
    //repayAmt
    private FabAmount contractAmt892;
    //repayAmt
    private FabAmount discountAmt892;
    //repayAmt
    private FabAmount forfeitAmt892;
    //repayAmt
    private FabAmount intAmt892;
    //repayAmt
    private FabAmount offsetAmt892;
    //repayAmt
    private FabAmount prinAmt892;
    //repayAmt
    private FabAmount refundAmt892;
    //repayAmt
    private FabRate compoundRate892;
    //repayAmt
    private FabAmount    feeRate892;
    //repayAmt
    private FabRate normalRate892;
    //repayAmt
    private FabRate overdueRate892;
    //repayAmt
    private String  cashFlag892;
    //repayAmt
    private String  channelType892;
    //repayAmt
    private String  compoundRateType892;
    //repayAmt
    private String  contractNo892;
    //repayAmt
    private String  customName892;
    //repayAmt
    private String  customType892;
    //repayAmt
    private String  discountFlag892;
    //repayAmt
    private String  errDate892;
    //repayAmt
    private String  fundChannel892;
    //repayAmt
    private String  intPerUnit892;
    //repayAmt
    private String  investeeId892;
    //repayAmt
    private FabAmount  investeePrin892;
    //repayAmt
    private FabAmount  investeeInt892;
    //repayAmt
    private FabAmount  investeeDint892;
    //repayAmt
    private String  investeeFlag892;
    //repayAmt
    private	String	investMode892; //投放模式：保理、消费贷
    //repayAmt
    private String  loanType892;
    //repayAmt
    private String  normalRateType892;
    //repayAmt
    private String  openBrc892;
    //repayAmt
    private String  openDate892;
    //repayAmt
    private String  outSerialNo892;
    //repayAmt
    private String  overdueRateType892;
    //repayAmt
    private String  priorType892;
    //repayAmt
    private String  receiptNo892;
    //repayAmt
    private String  repayAcctNo892;
    //repayAmt
    private String  repayChannel892;
    //repayAmt
    private String  repayWay892;
    //repayAmt
    private String  settleChannel892;
    //repayAmt
    private String  startIntDate892;
    //repayAmt
    private String  tranType892;
    //repayAmt
    private String	periodType892;
    //repayAmt
    private String  acctStat892; 	//账户类型 -- 需要去FaLoanPublic中增加该字段
    private String  loanStat892;	//贷款状态
    //repayAmt
    private Integer repayTerm892;
    //repayAmt
    private String  endDate892;
    //repayAmt
    private String  startDate892;
    //repayAmt
    private String  bankSubject892;
    //repayAmt
    private String  calcIntFlag2892;
    //repayAmt
    private String  calcIntFlag8921;
    //repayAmt
    private String  termeDate892;
    //repayAmt
    private String  intbDate892;
    //repayAmt
    private FabAmount termPrin892;
    //repayAmt
    private FabAmount termInt892;
    //repayAmt
    private String  inteDate892;
    //repayAmt
    private String  days892;
    //repayAmt
    private Double  recoveryRate892;
    //repayAmt
    private Double  finalRate892;
    //repayAmt
    private Double  rentRate892;
    //repayAmt
    private Double  floatRate892;

    @Autowired
    @SuppressWarnings("unused")
    @Override
    public void run() throws Exception {

        if( 1 == 0 )
        {
            LoggerUtil.debug(tradeNo892+
                    tradeTime892+
                    goodsDesc892+
                    period892+
                    payNo892+
                    investee892+
                    investeeNo892+
                    payList892+
                    investeeList892+
                    customId892+
                    repayNo892+
                    repayTotalAmount892+
                    repayAmount892+
                    settleFlag892+
                    repayTime892+
                    repayList892+
                    repayAmt892+
                    errSerSeq892+
                    graceDays892+
                    periodNum892+
                    repayDate892+
                    debtCompany892+
                    debtAmt892+
                    contractAmt892+
                    discountAmt892+
                    forfeitAmt892+
                    intAmt892+
                    offsetAmt892+
                    prinAmt892+
                    refundAmt892+
                    compoundRate892+
                    feeRate892+
                    normalRate892+
                    overdueRate892+
                    cashFlag892+
                    channelType892+
                    compoundRateType892+
                    contractNo892+
                    customName892+
                    customType892+
                    discountFlag892+
                    errDate892+
                    fundChannel892+
                    intPerUnit892+
                    investeeId892+
                    investeePrin892+
                    investeeInt892+
                    investeeDint892+
                    investeeFlag892+
                    investMode892+
                    loanType892+
                    normalRateType892+
                    openBrc892+
                    openDate892+
                    outSerialNo892+
                    overdueRateType892+
                    priorType892+
                    receiptNo892+
                    repayAcctNo892+
                    repayChannel892+
                    repayWay892+
                    settleChannel892+
                    startIntDate892+
                    tranType892+
                    periodType892+
                    acctStat892+
                    loanStat892+
                    repayTerm892+
                    endDate892+
                    startDate892+
                    bankSubject892+
                    calcIntFlag2892+
                    calcIntFlag8921+
                    termeDate892+
                    intbDate892+
                    termPrin892+
                    termInt892+
                    inteDate892+
                    days892+
                    recoveryRate892+
                    finalRate892+
                    rentRate892+
                    floatRate892);
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
    //获取getTradeNo892
    public String getTradeNo892() {
        return tradeNo892;
    }
    //获取setTradeNo892
    public void setTradeNo892(String tradeNo892) {
        this.tradeNo892 = tradeNo892;
    }
    //获取getTradeTime892
    public String getTradeTime892() {
        return tradeTime892;
    }
    //获取getTradeTime892
    public void setTradeTime892(String tradeTime892) {
        this.tradeTime892 = tradeTime892;
    }
    //获取getGoodsDesc892
    public String getGoodsDesc892() {
        return goodsDesc892;
    }
    //获取getGoodsDesc892
    public void setGoodsDesc892(String goodsDesc892) {
        this.goodsDesc892 = goodsDesc892;
    }
    //获取getGoodsDesc892
    public Integer getPeriod892() {
        return period892;
    }
    //获取getGoodsDesc892
    public void setPeriod892(Integer period892) {
        this.period892 = period892;
    }
    //获取getGoodsDesc892
    public String getPayNo892() {
        return payNo892;
    }
    //获取getGoodsDesc892
    public void setPayNo892(String payNo892) {
        this.payNo892 = payNo892;
    }
    //获取getGoodsDesc892
    public String getInvestee892() {
        return investee892;
    }
    //获取getGoodsDesc892
    public void setInvestee892(String investee892) {
        this.investee892 = investee892;
    }
    //获取getInvesteeNo892
    public String getInvesteeNo892() {
        return investeeNo892;
    }
    //获取setInvesteeNo892
    public void setInvesteeNo892(String investeeNo892) {
        this.investeeNo892 = investeeNo892;
    }
    //获取setInvesteeNo892
    public ListMap getPayList892() {
        return payList892;
    }
    //获取setInvesteeNo892
    public void setPayList892(ListMap payList892) {
        this.payList892 = payList892;
    }
    //获取setInvesteeNo892
    public ListMap getInvesteeList892() {
        return investeeList892;
    }
    //获取setInvesteeNo892
    public void setInvesteeList892(ListMap investeeList892) {
        this.investeeList892 = investeeList892;
    }
    //获取setInvesteeNo892
    public String getCustomId892() {
        return customId892;
    }
    //获取setInvesteeNo892
    public void setCustomId892(String customId892) {
        this.customId892 = customId892;
    }
    //获取getRepayNo892
    public String getRepayNo892() {
        return repayNo892;
    }
    //获取setRepayNo892
    public void setRepayNo892(String repayNo892) {
        this.repayNo892 = repayNo892;
    }
    //获取getRepayTotalAmount892
    public FabAmount getRepayTotalAmount892() {
        return repayTotalAmount892;
    }
    //获取setRepayTotalAmount892
    public void setRepayTotalAmount892(FabAmount repayTotalAmount892) {
        this.repayTotalAmount892 = repayTotalAmount892;
    }
    //获取getRepayAmount892
    public FabAmount getRepayAmount892() {
        return repayAmount892;
    }
    //获取setRepayAmount892
    public void setRepayAmount892(FabAmount repayAmount892) {
        this.repayAmount892 = repayAmount892;
    }
    //获取setRepayAmount892
    public String getSettleFlag892() {
        return settleFlag892;
    }
    //获取setRepayAmount892
    public void setSettleFlag892(String settleFlag892) {
        this.settleFlag892 = settleFlag892;
    }
    //获取setRepayAmount892
    public String getRepayTime892() {
        return repayTime892;
    }
    //获取setRepayAmount892
    public void setRepayTime892(String repayTime892) {
        this.repayTime892 = repayTime892;
    }
    //获取setRepayAmount892
    public String getRepayList892() {
        return repayList892;
    }
    //获取setRepayList892
    public void setRepayList892(String repayList892) {
        this.repayList892 = repayList892;
    }
    //获取getRepayAmt892
    public FabAmount getRepayAmt892() {
        return repayAmt892;
    }
    //获取setRepayAmt892
    public void setRepayAmt892(FabAmount repayAmt892) {
        this.repayAmt892 = repayAmt892;
    }
    //获取setRepayAmount892
    public Integer getErrSerSeq892() {
        return errSerSeq892;
    }
    //获取setRepayAmount892
    public void setErrSerSeq892(Integer errSerSeq892) {
        this.errSerSeq892 = errSerSeq892;
    }
    //获取getGraceDays892
    public Integer getGraceDays892() {
        return graceDays892;
    }
    //获取setGraceDays892
    public void setGraceDays892(Integer graceDays892) {
        this.graceDays892 = graceDays892;
    }
    //获取getPeriodNum892
    public Integer getPeriodNum892() {
        return periodNum892;
    }
    //获取setPeriodNum892
    public void setPeriodNum892(Integer periodNum892) {
        this.periodNum892 = periodNum892;
    }
    //获取getPeriodNum892
    public String getRepayDate892() {
        return repayDate892;
    }
    //获取setRepayDate892
    public void setRepayDate892(String repayDate892) {
        this.repayDate892 = repayDate892;
    }
    //获取getDebtCompany892
    public String getDebtCompany892() {
        return debtCompany892;
    }
    //获取setDebtCompany892
    public void setDebtCompany892(String debtCompany892) {
        this.debtCompany892 = debtCompany892;
    }
    //获取setDebtCompany892
    public FabAmount getDebtAmt892() {
        return debtAmt892;
    }
    //获取setDebtCompany892
    public void setDebtAmt892(FabAmount debtAmt892) {
        this.debtAmt892 = debtAmt892;
    }
    //获取getContractAmt892
    public FabAmount getContractAmt892() {
        return contractAmt892;
    }
    //获取setContractAmt892
    public void setContractAmt892(FabAmount contractAmt892) {
        this.contractAmt892 = contractAmt892;
    }
    //获取getDiscountAmt892
    public FabAmount getDiscountAmt892() {
        return discountAmt892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setDiscountAmt892(FabAmount discountAmt892) {
        this.discountAmt892 = discountAmt892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getForfeitAmt892() {
        return forfeitAmt892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setForfeitAmt892(FabAmount forfeitAmt892) {
        this.forfeitAmt892 = forfeitAmt892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getIntAmt892() {
        return intAmt892;
    }
    public void setIntAmt892(FabAmount intAmt892) {
        this.intAmt892 = intAmt892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getOffsetAmt892() {
        return offsetAmt892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setOffsetAmt892(FabAmount offsetAmt892) {
        this.offsetAmt892 = offsetAmt892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getPrinAmt892() {
        return prinAmt892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setPrinAmt892(FabAmount prinAmt892) {
        this.prinAmt892 = prinAmt892;
    }
    //获取getRefundAmt892用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getRefundAmt892() {
        return refundAmt892;
    }
    //获取setRefundAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setRefundAmt892(FabAmount refundAmt892) {
        this.refundAmt892 = refundAmt892;
    }
    //获取getCompoundRate892用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getCompoundRate892() {
        return compoundRate892;
    }
    //获取setCompoundRate892用来进行隔天发方法获取一遍用于代码的解读
    public void setCompoundRate892(FabRate compoundRate892) {
        this.compoundRate892 = compoundRate892;
    }
    //获取getFeeRate892用来进行隔天发方法获取一遍用于代码的解读
    public FabAmount getFeeRate892() {
        return feeRate892;
    }
    //获取setFeeRate892用来进行隔天发方法获取一遍用于代码的解读
    public void setFeeRate892(FabAmount feeRate892) {
        this.feeRate892 = feeRate892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getNormalRate892() {
        return normalRate892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setNormalRate892(FabRate normalRate892) {
        this.normalRate892 = normalRate892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public FabRate getOverdueRate892() {
        return overdueRate892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setOverdueRate892(FabRate overdueRate892) {
        this.overdueRate892 = overdueRate892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public String getCashFlag892() {
        return cashFlag892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setCashFlag892(String cashFlag892) {
        this.cashFlag892 = cashFlag892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public String getChannelType892() {
        return channelType892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setChannelType892(String channelType892) {
        this.channelType892 = channelType892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public String getCompoundRateType892() {
        return compoundRateType892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public void setCompoundRateType892(String compoundRateType892) {
        this.compoundRateType892 = compoundRateType892;
    }
    //获取getDiscountAmt892用来进行隔天发方法获取一遍用于代码的解读
    public String getContractNo892() {
        return contractNo892;
    }
    //获取setContractNo892用来进行隔天发方法获取一遍用于代码的解读
    public void setContractNo892(String contractNo892) {
        this.contractNo892 = contractNo892;
    }
    //获取getCustomName892用来进行隔天发方法获取一遍用于代码的解读
    public String getCustomName892() {
        return customName892;
    }
    //获取setCustomName892用来进行隔天发方法获取一遍用于代码的解读
    public void setCustomName892(String customName892) {
        this.customName892 = customName892;
    }
    //获取getCustomType892用来进行隔天发方法获取一遍用于代码的解读
    public String getCustomType892() {
        return customType892;
    }
    public void setCustomType892(String customType892) {
        this.customType892 = customType892;
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



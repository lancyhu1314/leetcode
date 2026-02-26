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
    public class Lns890 extends WorkUnit {

        //tradeNo
        private String 	tradeNo890;
        //tradeTime
        private String 	tradeTime890;
        //goodsDesc
        private String 	goodsDesc890;
        //period
        private Integer period890;
        //payNo
        private String 	payNo890;
        //investee
        private String 	investee890;
        //investeeNo
        private String 	investeeNo890;
        //payList
        private ListMap payList890;
        //investeeList
        private ListMap investeeList890;
        //customId
        private String customId890;
        //repayNo
        private String repayNo890;
        //repayTotalAmount
        private FabAmount repayTotalAmount890;
        //repayAmount
        private FabAmount repayAmount890;
        //settleFlag
        private String settleFlag890;
        //repayTime
        private String repayTime890;
        //repayList
        private String repayList890;
        //repayAmt
        /**
         *
         */
        //repayAmt
        private FabAmount repayAmt890;
        //repayAmt
        private Integer errSerSeq890;
        //repayAmt
        private Integer graceDays890;
        //repayAmt
        private Integer periodNum890;
        //repayAmt
        private String	repayDate890;
        //repayAmt
        private String debtCompany890;
        //repayAmt
        private FabAmount debtAmt890;
        //repayAmt
        private FabAmount contractAmt890;
        //repayAmt
        private FabAmount discountAmt890;
        //repayAmt
        private FabAmount forfeitAmt890;
        //repayAmt
        private FabAmount intAmt890;
        //repayAmt
        private FabAmount offsetAmt890;
        //repayAmt
        private FabAmount prinAmt890;
        //repayAmt
        private FabAmount refundAmt890;
        //repayAmt
        private FabRate compoundRate890;
        //repayAmt
        private FabAmount    feeRate890;
        //repayAmt
        private FabRate normalRate890;
        //repayAmt
        private FabRate overdueRate890;
        //repayAmt
        private String  cashFlag890;
        //repayAmt
        private String  channelType890;
        //repayAmt
        private String  compoundRateType890;
        //repayAmt
        private String  contractNo890;
        //repayAmt
        private String  customName890;
        //repayAmt
        private String  customType890;
        //repayAmt
        private String  discountFlag890;
        //repayAmt
        private String  errDate890;
        //repayAmt
        private String  fundChannel890;
        //repayAmt
        private String  intPerUnit890;
        //repayAmt
        private String  investeeId890;
        //repayAmt
        private FabAmount  investeePrin890;
        //repayAmt
        private FabAmount  investeeInt890;
        //repayAmt
        private FabAmount  investeeDint890;
        //repayAmt
        private String  investeeFlag890;
        //repayAmt
        private	String	investMode890; //投放模式：保理、消费贷
        //repayAmt
        private String  loanType890;
        //repayAmt
        private String  normalRateType890;
        //repayAmt
        private String  openBrc890;
        //repayAmt
        private String  openDate890;
        //repayAmt
        private String  outSerialNo890;
        //repayAmt
        private String  overdueRateType890;
        //repayAmt
        private String  priorType890;
        //repayAmt
        private String  receiptNo890;
        //repayAmt
        private String  repayAcctNo890;
        //repayAmt
        private String  repayChannel890;
        //repayAmt
        private String  repayWay890;
        //repayAmt
        private String  settleChannel890;
        //repayAmt
        private String  startIntDate890;
        //repayAmt
        private String  tranType890;
        //repayAmt
        private String	periodType890;
        //repayAmt
        private String  acctStat890; 	//账户类型 -- 需要去FaLoanPublic中增加该字段
        private String  loanStat890;	//贷款状态
        //repayAmt
        private Integer repayTerm890;
        //repayAmt
        private String  endDate890;
        //repayAmt
        private String  startDate890;
        //repayAmt
        private String  bankSubject890;
        //repayAmt
        private String  calcIntFlag2890;
        //repayAmt
        private String  calcIntFlag8901;
        //repayAmt
        private String  termeDate890;
        //repayAmt
        private String  intbDate890;
        //repayAmt
        private FabAmount termPrin890;
        //repayAmt
        private FabAmount termInt890;
        //repayAmt
        private String  inteDate890;
        //repayAmt
        private String  days890;
        //repayAmt
        private Double  recoveryRate890;
        //repayAmt
        private Double  finalRate890;
        //repayAmt
        private Double  rentRate890;
        //repayAmt
        private Double  floatRate890;

        @Autowired
        @SuppressWarnings("unused")
        @Override
        public void run() throws Exception {

            if( 1 == 0 )
            {
                LoggerUtil.debug(tradeNo890+
                        tradeTime890+
                        goodsDesc890+
                        period890+
                        payNo890+
                        investee890+
                        investeeNo890+
                        payList890+
                        investeeList890+
                        customId890+
                        repayNo890+
                        repayTotalAmount890+
                        repayAmount890+
                        settleFlag890+
                        repayTime890+
                        repayList890+
                        repayAmt890+
                        errSerSeq890+
                        graceDays890+
                        periodNum890+
                        repayDate890+
                        debtCompany890+
                        debtAmt890+
                        contractAmt890+
                        discountAmt890+
                        forfeitAmt890+
                        intAmt890+
                        offsetAmt890+
                        prinAmt890+
                        refundAmt890+
                        compoundRate890+
                        feeRate890+
                        normalRate890+
                        overdueRate890+
                        cashFlag890+
                        channelType890+
                        compoundRateType890+
                        contractNo890+
                        customName890+
                        customType890+
                        discountFlag890+
                        errDate890+
                        fundChannel890+
                        intPerUnit890+
                        investeeId890+
                        investeePrin890+
                        investeeInt890+
                        investeeDint890+
                        investeeFlag890+
                        investMode890+
                        loanType890+
                        normalRateType890+
                        openBrc890+
                        openDate890+
                        outSerialNo890+
                        overdueRateType890+
                        priorType890+
                        receiptNo890+
                        repayAcctNo890+
                        repayChannel890+
                        repayWay890+
                        settleChannel890+
                        startIntDate890+
                        tranType890+
                        periodType890+
                        acctStat890+
                        loanStat890+
                        repayTerm890+
                        endDate890+
                        startDate890+
                        bankSubject890+
                        calcIntFlag2890+
                        calcIntFlag8901+
                        termeDate890+
                        intbDate890+
                        termPrin890+
                        termInt890+
                        inteDate890+
                        days890+
                        recoveryRate890+
                        finalRate890+
                        rentRate890+
                        floatRate890);
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
        //获取getTradeNo890
        public String getTradeNo890() {
            return tradeNo890;
        }
        //获取setTradeNo890
        public void setTradeNo890(String tradeNo890) {
            this.tradeNo890 = tradeNo890;
        }
        //获取getTradeTime890
        public String getTradeTime890() {
            return tradeTime890;
        }
        //获取getTradeTime890
        public void setTradeTime890(String tradeTime890) {
            this.tradeTime890 = tradeTime890;
        }
        //获取getGoodsDesc890
        public String getGoodsDesc890() {
            return goodsDesc890;
        }
        //获取getGoodsDesc890
        public void setGoodsDesc890(String goodsDesc890) {
            this.goodsDesc890 = goodsDesc890;
        }
        //获取getGoodsDesc890
        public Integer getPeriod890() {
            return period890;
        }
        //获取getGoodsDesc890
        public void setPeriod890(Integer period890) {
            this.period890 = period890;
        }
        //获取getGoodsDesc890
        public String getPayNo890() {
            return payNo890;
        }
        //获取getGoodsDesc890
        public void setPayNo890(String payNo890) {
            this.payNo890 = payNo890;
        }
        //获取getGoodsDesc890
        public String getInvestee890() {
            return investee890;
        }
        //获取getGoodsDesc890
        public void setInvestee890(String investee890) {
            this.investee890 = investee890;
        }
        //获取getInvesteeNo890
        public String getInvesteeNo890() {
            return investeeNo890;
        }
        //获取setInvesteeNo890
        public void setInvesteeNo890(String investeeNo890) {
            this.investeeNo890 = investeeNo890;
        }
        //获取setInvesteeNo890
        public ListMap getPayList890() {
            return payList890;
        }
        //获取setInvesteeNo890
        public void setPayList890(ListMap payList890) {
            this.payList890 = payList890;
        }
        //获取setInvesteeNo890
        public ListMap getInvesteeList890() {
            return investeeList890;
        }
        //获取setInvesteeNo890
        public void setInvesteeList890(ListMap investeeList890) {
            this.investeeList890 = investeeList890;
        }
        //获取setInvesteeNo890
        public String getCustomId890() {
            return customId890;
        }
        //获取setInvesteeNo890
        public void setCustomId890(String customId890) {
            this.customId890 = customId890;
        }
        //获取getRepayNo890
        public String getRepayNo890() {
            return repayNo890;
        }
        //获取setRepayNo890
        public void setRepayNo890(String repayNo890) {
            this.repayNo890 = repayNo890;
        }
        //获取getRepayTotalAmount890
        public FabAmount getRepayTotalAmount890() {
            return repayTotalAmount890;
        }
        //获取setRepayTotalAmount890
        public void setRepayTotalAmount890(FabAmount repayTotalAmount890) {
            this.repayTotalAmount890 = repayTotalAmount890;
        }
        //获取getRepayAmount890
        public FabAmount getRepayAmount890() {
            return repayAmount890;
        }
        //获取setRepayAmount890
        public void setRepayAmount890(FabAmount repayAmount890) {
            this.repayAmount890 = repayAmount890;
        }
        //获取setRepayAmount890
        public String getSettleFlag890() {
            return settleFlag890;
        }
        //获取setRepayAmount890
        public void setSettleFlag890(String settleFlag890) {
            this.settleFlag890 = settleFlag890;
        }
        //获取setRepayAmount890
        public String getRepayTime890() {
            return repayTime890;
        }
        //获取setRepayAmount890
        public void setRepayTime890(String repayTime890) {
            this.repayTime890 = repayTime890;
        }
        //获取setRepayAmount890
        public String getRepayList890() {
            return repayList890;
        }
        //获取setRepayList890
        public void setRepayList890(String repayList890) {
            this.repayList890 = repayList890;
        }
        //获取getRepayAmt890
        public FabAmount getRepayAmt890() {
            return repayAmt890;
        }
        //获取setRepayAmt890
        public void setRepayAmt890(FabAmount repayAmt890) {
            this.repayAmt890 = repayAmt890;
        }
        //获取setRepayAmount890
        public Integer getErrSerSeq890() {
            return errSerSeq890;
        }
        //获取setRepayAmount890
        public void setErrSerSeq890(Integer errSerSeq890) {
            this.errSerSeq890 = errSerSeq890;
        }
        //获取getGraceDays890
        public Integer getGraceDays890() {
            return graceDays890;
        }
        //获取setGraceDays890
        public void setGraceDays890(Integer graceDays890) {
            this.graceDays890 = graceDays890;
        }
        //获取getPeriodNum890
        public Integer getPeriodNum890() {
            return periodNum890;
        }
        //获取setPeriodNum890
        public void setPeriodNum890(Integer periodNum890) {
            this.periodNum890 = periodNum890;
        }
        //获取getPeriodNum890
        public String getRepayDate890() {
            return repayDate890;
        }
        //获取setRepayDate890
        public void setRepayDate890(String repayDate890) {
            this.repayDate890 = repayDate890;
        }
        //获取getDebtCompany890
        public String getDebtCompany890() {
            return debtCompany890;
        }
        //获取setDebtCompany890
        public void setDebtCompany890(String debtCompany890) {
            this.debtCompany890 = debtCompany890;
        }
        //获取setDebtCompany890
        public FabAmount getDebtAmt890() {
            return debtAmt890;
        }
        //获取setDebtCompany890
        public void setDebtAmt890(FabAmount debtAmt890) {
            this.debtAmt890 = debtAmt890;
        }
        //获取getContractAmt890
        public FabAmount getContractAmt890() {
            return contractAmt890;
        }
        //获取setContractAmt890
        public void setContractAmt890(FabAmount contractAmt890) {
            this.contractAmt890 = contractAmt890;
        }
        //获取getDiscountAmt890
        public FabAmount getDiscountAmt890() {
            return discountAmt890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setDiscountAmt890(FabAmount discountAmt890) {
            this.discountAmt890 = discountAmt890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public FabAmount getForfeitAmt890() {
            return forfeitAmt890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setForfeitAmt890(FabAmount forfeitAmt890) {
            this.forfeitAmt890 = forfeitAmt890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public FabAmount getIntAmt890() {
            return intAmt890;
        }
        public void setIntAmt890(FabAmount intAmt890) {
            this.intAmt890 = intAmt890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public FabAmount getOffsetAmt890() {
            return offsetAmt890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setOffsetAmt890(FabAmount offsetAmt890) {
            this.offsetAmt890 = offsetAmt890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public FabAmount getPrinAmt890() {
            return prinAmt890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setPrinAmt890(FabAmount prinAmt890) {
            this.prinAmt890 = prinAmt890;
        }
        //获取getRefundAmt890用来进行隔天发方法获取一遍用于代码的解读
        public FabAmount getRefundAmt890() {
            return refundAmt890;
        }
        //获取setRefundAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setRefundAmt890(FabAmount refundAmt890) {
            this.refundAmt890 = refundAmt890;
        }
        //获取getCompoundRate890用来进行隔天发方法获取一遍用于代码的解读
        public FabRate getCompoundRate890() {
            return compoundRate890;
        }
        //获取setCompoundRate890用来进行隔天发方法获取一遍用于代码的解读
        public void setCompoundRate890(FabRate compoundRate890) {
            this.compoundRate890 = compoundRate890;
        }
        //获取getFeeRate890用来进行隔天发方法获取一遍用于代码的解读
        public FabAmount getFeeRate890() {
            return feeRate890;
        }
        //获取setFeeRate890用来进行隔天发方法获取一遍用于代码的解读
        public void setFeeRate890(FabAmount feeRate890) {
            this.feeRate890 = feeRate890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public FabRate getNormalRate890() {
            return normalRate890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setNormalRate890(FabRate normalRate890) {
            this.normalRate890 = normalRate890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public FabRate getOverdueRate890() {
            return overdueRate890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setOverdueRate890(FabRate overdueRate890) {
            this.overdueRate890 = overdueRate890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public String getCashFlag890() {
            return cashFlag890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setCashFlag890(String cashFlag890) {
            this.cashFlag890 = cashFlag890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public String getChannelType890() {
            return channelType890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setChannelType890(String channelType890) {
            this.channelType890 = channelType890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public String getCompoundRateType890() {
            return compoundRateType890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public void setCompoundRateType890(String compoundRateType890) {
            this.compoundRateType890 = compoundRateType890;
        }
        //获取getDiscountAmt890用来进行隔天发方法获取一遍用于代码的解读
        public String getContractNo890() {
            return contractNo890;
        }
        //获取setContractNo890用来进行隔天发方法获取一遍用于代码的解读
        public void setContractNo890(String contractNo890) {
            this.contractNo890 = contractNo890;
        }
        //获取getCustomName890用来进行隔天发方法获取一遍用于代码的解读
        public String getCustomName890() {
            return customName890;
        }
        //获取setCustomName890用来进行隔天发方法获取一遍用于代码的解读
        public void setCustomName890(String customName890) {
            this.customName890 = customName890;
        }
        //获取getCustomType890用来进行隔天发方法获取一遍用于代码的解读
        public String getCustomType890() {
            return customType890;
        }
        public void setCustomType890(String customType890) {
            this.customType890 = customType890;
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



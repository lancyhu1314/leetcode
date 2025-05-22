package com.suning.fab.loan.workunit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.LoggerUtil;

/**
 * @author 
 *
 * @version V1.0.0
 *
 * @see 现金贷资金方记账
 *
 * @param 
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns889 extends WorkUnit {

	//tradeNo
		private String 	tradeNo889;
		//tradeTime
		private String 	tradeTime889;
		//goodsDesc
		private String 	goodsDesc889;
		//period
		private Integer period889;
		//payNo
		private String 	payNo889;
		//investee
		private String 	investee889;
		//investeeNo
		private String 	investeeNo889;
		//payList
		private ListMap payList889;
		//investeeList
		private ListMap investeeList889;
		//customId
		private String customId889;
		//repayNo
		private String repayNo889;
		//repayTotalAmount
		private FabAmount repayTotalAmount889;
		//repayAmount
		private FabAmount repayAmount889;
		//settleFlag
		private String settleFlag889;
		//repayTime
		private String repayTime889;
		//repayList
		private String repayList889;
		//repayAmt
		/**
		 * 
		 */
		//repayAmt
		private FabAmount repayAmt889;
		//repayAmt
		private Integer errSerSeq889;
		//repayAmt
		private Integer graceDays889;
		//repayAmt
		private Integer periodNum889;
		//repayAmt
		private String	repayDate889;
		//repayAmt
		private String debtCompany889;
		//repayAmt
		private FabAmount debtAmt889;
		//repayAmt
		private FabAmount contractAmt889;
		//repayAmt
		private FabAmount discountAmt889;
		//repayAmt
		private FabAmount forfeitAmt889;
		//repayAmt
		private FabAmount intAmt889;
		//repayAmt
		private FabAmount offsetAmt889;
		//repayAmt
		private FabAmount prinAmt889;      
		//repayAmt
		private FabAmount refundAmt889;        
		//repayAmt
		private FabRate    compoundRate889;       
		//repayAmt
		private FabAmount    feeRate889;          
		//repayAmt
		private FabRate normalRate889;       
		//repayAmt
		private FabRate overdueRate889;
		//repayAmt
		private String  cashFlag889;
		//repayAmt
		private String  channelType889;
		//repayAmt
		private String  compoundRateType889;
		//repayAmt
		private String  contractNo889;
		//repayAmt
		private String  customName889;
		//repayAmt
		private String  customType889;
		//repayAmt
		private String  discountFlag889;
		//repayAmt
		private String  errDate889;
		//repayAmt
		private String  fundChannel889;
		//repayAmt
		private String  intPerUnit889;
		//repayAmt
		private String  investeeId889;
		//repayAmt
		private FabAmount  investeePrin889;
		//repayAmt
		private FabAmount  investeeInt889;
		//repayAmt
		private FabAmount  investeeDint889;
		//repayAmt
		private String  investeeFlag889;
		//repayAmt
		private	String	investMode889; //投放模式：保理、消费贷
		//repayAmt
		private String  loanType889;
		//repayAmt
		private String  normalRateType889;
		//repayAmt
		private String  openBrc889;
		//repayAmt
		private String  openDate889;
		//repayAmt
		private String  outSerialNo889;
		//repayAmt
		private String  overdueRateType889;
		//repayAmt
		private String  priorType889;
		//repayAmt
		private String  receiptNo889;
		//repayAmt
		private String  repayAcctNo889;
		//repayAmt
		private String  repayChannel889;
		//repayAmt
		private String  repayWay889;
		//repayAmt
		private String  settleChannel889;
		//repayAmt
		private String  startIntDate889;
		//repayAmt
		private String  tranType889;
		//repayAmt
		private String	periodType889;
		//repayAmt
		private String  acctStat889; 	//账户类型 -- 需要去FaLoanPublic中增加该字段
		private String  loanStat889;	//贷款状态
		//repayAmt
		private Integer repayTerm889;
		//repayAmt
		private String  endDate889;
		//repayAmt
		private String  startDate889;
		//repayAmt
		private String  bankSubject889;
		//repayAmt
		private String  calcIntFlag2889;
		//repayAmt
		private String  calcIntFlag8891;
		//repayAmt
		private String  termeDate889;
		//repayAmt
		private String  intbDate889;
		//repayAmt
		private FabAmount termPrin889;
		//repayAmt
		private FabAmount termInt889;
		//repayAmt
		private String  inteDate889;
		//repayAmt
		private String  days889;
		//repayAmt
		private Double  recoveryRate889;
		//repayAmt
		private Double  finalRate889;
		//repayAmt
		private Double  rentRate889;
		//repayAmt
		private Double  floatRate889;
	
	@SuppressWarnings("unused")
	@Override
	public void run() throws Exception {
		
		if( 1 == 0 )
		{
			LoggerUtil.debug(tradeNo889+
					tradeTime889+
					goodsDesc889+
					period889+
					payNo889+
					investee889+
					investeeNo889+
					payList889+
					investeeList889+
					customId889+
					repayNo889+
					repayTotalAmount889+
					repayAmount889+
					settleFlag889+
					repayTime889+
					repayList889+
					repayAmt889+
					errSerSeq889+
					graceDays889+
					periodNum889+
					repayDate889+
					debtCompany889+
					debtAmt889+
					contractAmt889+
					discountAmt889+
					forfeitAmt889+
					intAmt889+
					offsetAmt889+
					prinAmt889+      
					refundAmt889+        
					compoundRate889+       
					feeRate889+          
					normalRate889+       
					overdueRate889+
					cashFlag889+
					channelType889+
					compoundRateType889+
					contractNo889+
					customName889+
					customType889+
					discountFlag889+
					errDate889+
					fundChannel889+
					intPerUnit889+
					investeeId889+
					investeePrin889+
					investeeInt889+
					investeeDint889+
					investeeFlag889+
					investMode889+
					loanType889+
					normalRateType889+
					openBrc889+
					openDate889+
					outSerialNo889+
					overdueRateType889+
					priorType889+
					receiptNo889+
					repayAcctNo889+
					repayChannel889+
					repayWay889+
					settleChannel889+
					startIntDate889+
					tranType889+
					periodType889+
					acctStat889+
					loanStat889+
					repayTerm889+
					endDate889+
					startDate889+
					bankSubject889+
					calcIntFlag2889+
					calcIntFlag8891+
					termeDate889+
					intbDate889+
					termPrin889+
					termInt889+
					inteDate889+
					days889+
					recoveryRate889+
					finalRate889+
					rentRate889+
					floatRate889);
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
	//获取getTradeNo889
	public String getTradeNo889() {
		return tradeNo889;
	}
	//获取setTradeNo889
	public void setTradeNo889(String tradeNo889) {
		this.tradeNo889 = tradeNo889;
	}
	//获取getTradeTime889
	public String getTradeTime889() {
		return tradeTime889;
	}
	//获取getTradeTime889
	public void setTradeTime889(String tradeTime889) {
		this.tradeTime889 = tradeTime889;
	}
	//获取getGoodsDesc889
	public String getGoodsDesc889() {
		return goodsDesc889;
	}
	//获取getGoodsDesc889
	public void setGoodsDesc889(String goodsDesc889) {
		this.goodsDesc889 = goodsDesc889;
	}
	//获取getGoodsDesc889
	public Integer getPeriod889() {
		return period889;
	}
	//获取getGoodsDesc889
	public void setPeriod889(Integer period889) {
		this.period889 = period889;
	}
	//获取getGoodsDesc889
	public String getPayNo889() {
		return payNo889;
	}
	//获取getGoodsDesc889
	public void setPayNo889(String payNo889) {
		this.payNo889 = payNo889;
	}
	//获取getGoodsDesc889
	public String getInvestee889() {
		return investee889;
	}
	//获取getGoodsDesc889
	public void setInvestee889(String investee889) {
		this.investee889 = investee889;
	}
	//获取getInvesteeNo889
	public String getInvesteeNo889() {
		return investeeNo889;
	}
	//获取setInvesteeNo889
	public void setInvesteeNo889(String investeeNo889) {
		this.investeeNo889 = investeeNo889;
	}
	//获取setInvesteeNo889
	public ListMap getPayList889() {
		return payList889;
	}
	//获取setInvesteeNo889
	public void setPayList889(ListMap payList889) {
		this.payList889 = payList889;
	}
	//获取setInvesteeNo889
	public ListMap getInvesteeList889() {
		return investeeList889;
	}
	//获取setInvesteeNo889
	public void setInvesteeList889(ListMap investeeList889) {
		this.investeeList889 = investeeList889;
	}
	//获取setInvesteeNo889
	public String getCustomId889() {
		return customId889;
	}
	//获取setInvesteeNo889
	public void setCustomId889(String customId889) {
		this.customId889 = customId889;
	}
	//获取getRepayNo889
	public String getRepayNo889() {
		return repayNo889;
	}
	//获取setRepayNo889
	public void setRepayNo889(String repayNo889) {
		this.repayNo889 = repayNo889;
	}
	//获取getRepayTotalAmount889
	public FabAmount getRepayTotalAmount889() {
		return repayTotalAmount889;
	}
	//获取setRepayTotalAmount889
	public void setRepayTotalAmount889(FabAmount repayTotalAmount889) {
		this.repayTotalAmount889 = repayTotalAmount889;
	}
	//获取getRepayAmount889
	public FabAmount getRepayAmount889() {
		return repayAmount889;
	}
	//获取setRepayAmount889
	public void setRepayAmount889(FabAmount repayAmount889) {
		this.repayAmount889 = repayAmount889;
	}
	//获取setRepayAmount889
	public String getSettleFlag889() {
		return settleFlag889;
	}
	//获取setRepayAmount889
	public void setSettleFlag889(String settleFlag889) {
		this.settleFlag889 = settleFlag889;
	}
	//获取setRepayAmount889
	public String getRepayTime889() {
		return repayTime889;
	}
	//获取setRepayAmount889
	public void setRepayTime889(String repayTime889) {
		this.repayTime889 = repayTime889;
	}
	//获取setRepayAmount889
	public String getRepayList889() {
		return repayList889;
	}
	//获取setRepayList889
	public void setRepayList889(String repayList889) {
		this.repayList889 = repayList889;
	}
	//获取getRepayAmt889
	public FabAmount getRepayAmt889() {
		return repayAmt889;
	}
	//获取setRepayAmt889
	public void setRepayAmt889(FabAmount repayAmt889) {
		this.repayAmt889 = repayAmt889;
	}
	//获取setRepayAmount889
	public Integer getErrSerSeq889() {
		return errSerSeq889;
	}
	//获取setRepayAmount889
	public void setErrSerSeq889(Integer errSerSeq889) {
		this.errSerSeq889 = errSerSeq889;
	}
	//获取getGraceDays889
	public Integer getGraceDays889() {
		return graceDays889;
	}
	//获取setGraceDays889
	public void setGraceDays889(Integer graceDays889) {
		this.graceDays889 = graceDays889;
	}
	//获取getPeriodNum889
	public Integer getPeriodNum889() {
		return periodNum889;
	}
	//获取setPeriodNum889
	public void setPeriodNum889(Integer periodNum889) {
		this.periodNum889 = periodNum889;
	}
	//获取getPeriodNum889
	public String getRepayDate889() {
		return repayDate889;
	}
	//获取setRepayDate889
	public void setRepayDate889(String repayDate889) {
		this.repayDate889 = repayDate889;
	}
	//获取getDebtCompany889
	public String getDebtCompany889() {
		return debtCompany889;
	}
	//获取setDebtCompany889
	public void setDebtCompany889(String debtCompany889) {
		this.debtCompany889 = debtCompany889;
	}
	//获取setDebtCompany889
	public FabAmount getDebtAmt889() {
		return debtAmt889;
	}
	//获取setDebtCompany889
	public void setDebtAmt889(FabAmount debtAmt889) {
		this.debtAmt889 = debtAmt889;
	}
	//获取getContractAmt889
	public FabAmount getContractAmt889() {
		return contractAmt889;
	}
	//获取setContractAmt889
	public void setContractAmt889(FabAmount contractAmt889) {
		this.contractAmt889 = contractAmt889;
	}
	//获取getDiscountAmt889
	public FabAmount getDiscountAmt889() {
		return discountAmt889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setDiscountAmt889(FabAmount discountAmt889) {
		this.discountAmt889 = discountAmt889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public FabAmount getForfeitAmt889() {
		return forfeitAmt889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setForfeitAmt889(FabAmount forfeitAmt889) {
		this.forfeitAmt889 = forfeitAmt889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public FabAmount getIntAmt889() {
		return intAmt889;
	}
	public void setIntAmt889(FabAmount intAmt889) {
		this.intAmt889 = intAmt889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public FabAmount getOffsetAmt889() {
		return offsetAmt889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setOffsetAmt889(FabAmount offsetAmt889) {
		this.offsetAmt889 = offsetAmt889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public FabAmount getPrinAmt889() {
		return prinAmt889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setPrinAmt889(FabAmount prinAmt889) {
		this.prinAmt889 = prinAmt889;
	}
	//获取getRefundAmt889用来进行隔天发方法获取一遍用于代码的解读
	public FabAmount getRefundAmt889() {
		return refundAmt889;
	}
	//获取setRefundAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setRefundAmt889(FabAmount refundAmt889) {
		this.refundAmt889 = refundAmt889;
	}
	//获取getCompoundRate889用来进行隔天发方法获取一遍用于代码的解读
	public FabRate getCompoundRate889() {
		return compoundRate889;
	}
	//获取setCompoundRate889用来进行隔天发方法获取一遍用于代码的解读
	public void setCompoundRate889(FabRate compoundRate889) {
		this.compoundRate889 = compoundRate889;
	}
	//获取getFeeRate889用来进行隔天发方法获取一遍用于代码的解读
	public FabAmount getFeeRate889() {
		return feeRate889;
	}
	//获取setFeeRate889用来进行隔天发方法获取一遍用于代码的解读
	public void setFeeRate889(FabAmount feeRate889) {
		this.feeRate889 = feeRate889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public FabRate getNormalRate889() {
		return normalRate889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setNormalRate889(FabRate normalRate889) {
		this.normalRate889 = normalRate889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public FabRate getOverdueRate889() {
		return overdueRate889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setOverdueRate889(FabRate overdueRate889) {
		this.overdueRate889 = overdueRate889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public String getCashFlag889() {
		return cashFlag889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setCashFlag889(String cashFlag889) {
		this.cashFlag889 = cashFlag889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public String getChannelType889() {
		return channelType889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setChannelType889(String channelType889) {
		this.channelType889 = channelType889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public String getCompoundRateType889() {
		return compoundRateType889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public void setCompoundRateType889(String compoundRateType889) {
		this.compoundRateType889 = compoundRateType889;
	}
	//获取getDiscountAmt889用来进行隔天发方法获取一遍用于代码的解读
	public String getContractNo889() {
		return contractNo889;
	}
	//获取setContractNo889用来进行隔天发方法获取一遍用于代码的解读
	public void setContractNo889(String contractNo889) {
		this.contractNo889 = contractNo889;
	}
	//获取getCustomName889用来进行隔天发方法获取一遍用于代码的解读
	public String getCustomName889() {
		return customName889;
	}
	//获取setCustomName889用来进行隔天发方法获取一遍用于代码的解读
	public void setCustomName889(String customName889) {
		this.customName889 = customName889;
	}
	//获取getCustomType889用来进行隔天发方法获取一遍用于代码的解读
	public String getCustomType889() {
		return customType889;
	}
	public void setCustomType889(String customType889) {
		this.customType889 = customType889;
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

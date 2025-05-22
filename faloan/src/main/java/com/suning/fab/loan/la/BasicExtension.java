package com.suning.fab.loan.la;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;

public class BasicExtension  implements Serializable{ 

	/**
	 * 
	 */
	private static final long serialVersionUID = -2733264053133376612L;
	//展期
	private  Integer extnums;	//展期所处期数
	private  FabAmount balBeforeEx;		//展期前合同余额
	private  String lastPeriodStr;		//是否最后一期 1是0否
	
	//买方付息
	private  String debtCompany;	//债务公司信息
	private  FabAmount debtAmt;		//债务公司金额
	
	//无追保理
	private  String jsonStr;	//json格式债务公司信息
	
	//汽车租赁还款方式
	private  String carRepayWay;	//还款方式

	//封顶付息
	private  String comeinDate;	//收益计止日期
	private  FabAmount hisComein = new FabAmount(0.0);	//历史期收益
	private  FabAmount hisDintComein = new FabAmount(0.0);	//历史期罚息收益
	private  FabAmount hisLBDintComein = new FabAmount(0.0);	//历史期呆滞呆账罚息收益
	private  FabAmount calOldDintComein = new FabAmount(0.0);	//利息试算
	private  FabAmount calDintComein = new FabAmount(0.0);	//罚息试算
	private  FabAmount capAmt = new FabAmount(0.0);	//罚息试算
	private  FabAmount sumPrin = new FabAmount(0.0);	//所有的未还本金
	//动态封顶计息
	private  FabAmount dynamicCapAmt = new FabAmount(0.0);	//动态收益封顶值
	private  FabAmount dynamicCapDiff = new FabAmount(0.0);	//动态收益封顶 差值(封顶剩余值，封顶值 - 总利息 - 总罚息)
	private  FabAmount prinBal = new FabAmount(0.0);	//本次计算封顶值的剩余本金
	private  FabAmount badDebt = new FabAmount(-1.0); //试算出来的呆滞呆账封顶金额（若未达到封顶则赋负值）
	private  FabAmount salesReturnAmt=null;//退货金额 author:chenchao
	private  Integer initFuturePeriodNum;//初始化退货后 未来期总期数

	private  String firstRepayDate;//首期还款日

	public String getWriteOffDate() {
		return writeOffDate;
	}

	public void setWriteOffDate(String writeOffDate) {
		this.writeOffDate = writeOffDate;
	}

	private String writeOffDate;//核销日期

	private  String  dynamicCapDate  =  "";	//动态封顶时间

	//延期还款 延期后的当期到期日
	private String termEndDate;
	private JSONObject lastEnddates = new JSONObject();	//动态封顶时间

	//放款渠道
	private String channelType;

	public BasicExtension() {
	}

	/**
	 * Gets the value of lastEnddates.
	 *
	 * @return the value of lastEnddates
	 */
	public JSONObject getLastEnddates() {
		return lastEnddates;
	}

	/**
	 * Sets the lastEnddates.
	 *
	 * @param lastEnddates lastEnddates
	 */
	public void setLastEnddates(JSONObject lastEnddates) {
		this.lastEnddates = lastEnddates;

	}

	/**
	 * Gets the value of .
	 *
	 * @return the value termEndDateof termEndDate
	 */
	public String getTermEndDate() {
		return termEndDate;
	}

	/**
	 * Sets the termEndDate.
	 *
	 * @param termEndDate termEndDate
	 */
	public void setTermEndDate(String termEndDate) {
		this.termEndDate = termEndDate;

	}

	//免息实际扣款日自动减免20200207
	private Map<Integer,Double> dropNints = new HashMap<>();

	public boolean dropNintsIsEmpty(){
		return dropNints.isEmpty();
	}
	public Map<Integer, Double> getDropNints() {
		return dropNints;
	}

	public void setDropNints(Map<Integer, Double> dropNints) {
		this.dropNints = dropNints;
	}
	/**
	 * Gets the value of dynamicCapDate.
	 *
	 * @return the value of dynamicCapDate
	 */
	public String getDynamicCapDate() {
		return dynamicCapDate;
	}

	/**
	 * Sets the dynamicCapDate.
	 *
	 * @param dynamicCapDate dynamicCapDate
	 */
	public void setDynamicCapDate(String dynamicCapDate) {
		this.dynamicCapDate = dynamicCapDate;

	}

	/**
	 * Gets the value of prinBal.
	 *
	 * @return the value of prinBal
	 */
	public FabAmount getPrinBal() {
		return prinBal;
	}

	/**
	 * Sets the prinBal.
	 *
	 * @param prinBal prinBal
	 */
	public void setPrinBal(FabAmount prinBal) {
		this.prinBal = prinBal;

	}

	//非标自定义不规则
	private  FabAmount totalInt;		//利息总金额
	
	
	//现金贷免息
	private  FabAmount freeInterest;

	//免融担费
	private Map<String,FabAmount> freeFee;
	
	//2019-09-26 气球贷膨胀期数
	private  Integer expandPeriod;

	//2019-10-12 现金贷提前还款违约金
	private  FabRate feeRate;


	/**
	 * Gets the value of badDebt.
	 *
	 * @return the value of badDebt
	 */
	public FabAmount getBadDebt() {
		return badDebt;
	}

	/**
	 * Sets the badDebt.
	 *
	 * @param badDebt badDebt
	 */
	public void setBadDebt(FabAmount badDebt) {
		this.badDebt = badDebt;

	}


	/**
	 * Gets the value of dynamicCapDiff.
	 *
	 * @return the value of dynamicCapDiff
	 */
	public FabAmount getDynamicCapDiff() {
		return dynamicCapDiff;
	}

	/**
	 * Sets the dynamicCapDiff.
	 *
	 * @param dynamicCapDiff dynamicCapDiff
	 */
	public void setDynamicCapDiff(FabAmount dynamicCapDiff) {
		this.dynamicCapDiff = dynamicCapDiff;

	}


	/**
	 * Gets the value of dynamicCapAmt.
	 *
	 * @return the value of dynamicCapAmt
	 */
	public FabAmount getDynamicCapAmt() {
		return dynamicCapAmt;
	}

	/**
	 * Sets the dynamicCapAmt.
	 *
	 * @param dynamicCapAmt dynamicCapAmt
	 */
	public void setDynamicCapAmt(FabAmount dynamicCapAmt) {
		this.dynamicCapAmt = dynamicCapAmt;

	}

	/**
	 * Gets the value of sumPrin.
	 *
	 * @return the value of sumPrin
	 */
	public FabAmount getSumPrin() {
		return sumPrin;
	}

	/**
	 * Sets the sumPrin.
	 *
	 * @param sumPrin sumPrin
	 */
	public void setSumPrin(FabAmount sumPrin) {
		this.sumPrin = sumPrin;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}


	public Integer getExtnums() {
		return extnums;
	}


	public void setExtnums(Integer extnums) {
		this.extnums = extnums;
	}


	public FabAmount getBalBeforeEx() {
		return balBeforeEx;
	}


	public void setBalBeforeEx(FabAmount balBeforeEx) {
		this.balBeforeEx = balBeforeEx;
	}


	public String getLastPeriodStr() {
		return lastPeriodStr;
	}


	public void setLastPeriodStr(String lastPeriodStr) {
		this.lastPeriodStr = lastPeriodStr;
	}


	/**
	 * @return the debtCompany
	 */
	public String getDebtCompany() {
		return debtCompany;
	}


	/**
	 * @param debtCompany the debtCompany to set
	 */
	public void setDebtCompany(String debtCompany) {
		this.debtCompany = debtCompany;
	}


	/**
	 * @return the debtAmt
	 */
	public FabAmount getDebtAmt() {
		return debtAmt;
	}


	/**
	 * @param debtAmt the debtAmt to set
	 */
	public void setDebtAmt(FabAmount debtAmt) {
		this.debtAmt = debtAmt;
	}


	/**
	 * @return the carRepayWay
	 */
	public String getCarRepayWay() {
		return carRepayWay;
	}


	/**
	 * @param carRepayWay the carRepayWay to set
	 */
	public void setCarRepayWay(String carRepayWay) {
		this.carRepayWay = carRepayWay;
	}


	public FabAmount getHisComein() {
		return hisComein;
	}


	public void setHisComein(FabAmount hisComein) {
		this.hisComein = hisComein;
	}


	public String getComeinDate() {
		return comeinDate;
	}


	public void setComeinDate(String comeinDate) {
		this.comeinDate = comeinDate;
	}


	public FabAmount getHisDintComein() {
		return hisDintComein;
	}


	public void setHisDintComein(FabAmount hisDintComein) {
		this.hisDintComein = hisDintComein;
	}





	public FabAmount getCalDintComein() {
		return calDintComein;
	}


	public void setCalDintComein(FabAmount calDintComein) {
		this.calDintComein = calDintComein;
	}


	public FabAmount getCapAmt() {
		return capAmt;
	}


	public void setCapAmt(FabAmount capAmt) {
		this.capAmt = capAmt;
	}


	public FabAmount getHisLBDintComein() {
		return hisLBDintComein;
	}


	public void setHisLBDintComein(FabAmount hisLBDintComein) {
		this.hisLBDintComein = hisLBDintComein;
	}


	public FabAmount getCalOldDintComein() {
		return calOldDintComein;
	}


	public void setCalOldDintComein(FabAmount calOldDintComein) {
		this.calOldDintComein = calOldDintComein;
	}


	/**
	 * @return the jsonStr
	 */
	public String getJsonStr() {
		return jsonStr;
	}


	/**
	 * @param jsonStr the jsonStr to set
	 */
	public void setJsonStr(String jsonStr) {
		this.jsonStr = jsonStr;
	}


	/**
	 * @return the totalInt
	 */
	public FabAmount getTotalInt() {
		return totalInt;
	}


	/**
	 * @param totalInt the totalInt to set
	 */
	public void setTotalInt(FabAmount totalInt) {
		this.totalInt = totalInt;
	}

	public FabAmount getFreeInterest() {
		return freeInterest;
	}

	public void setFreeInterest(FabAmount freeInterest) {
		this.freeInterest = freeInterest;
	}

	/**
	 * @return the expandPeriod
	 */
	public Integer getExpandPeriod() {
		return expandPeriod;
	}

	/**
	 * @param expandPeriod the expandPeriod to set
	 */
	public void setExpandPeriod(Integer expandPeriod) {
		this.expandPeriod = expandPeriod;
	}

	/**
	 * @return the feeRate
	 */
	public FabRate getFeeRate() {
		return feeRate;
	}

	/**
	 * @param feeRate the feeRate to set
	 */
	public void setFeeRate(FabRate feeRate) {
		this.feeRate = feeRate;
	}

	/**
	 * @return the channelType
	 */
	public String getChannelType() {
		return channelType;
	}

	/**
	 * @param channelType the channelType to set
	 */
	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}


	public FabAmount getSalesReturnAmt() {
		return salesReturnAmt;
	}

	public void setSalesReturnAmt(FabAmount salesReturnAmt) {
		this.salesReturnAmt = salesReturnAmt;
	}


	public Integer getInitFuturePeriodNum() {
		return initFuturePeriodNum;
	}

	public void setInitFuturePeriodNum(Integer initFuturePeriodNum) {
		this.initFuturePeriodNum = initFuturePeriodNum;
	}

	public Map<String, FabAmount> getFreeFee() {
		return freeFee;
	}

	/**
	 * 根据费用账本类型、还款方式 获取费用金额
	 * @param lnsBill
	 * @return
	 */
	public FabAmount getFreeFee(LnsBill lnsBill){
		if(freeFee==null || freeFee.get(lnsBill.getBillType()+":"+lnsBill.getRepayWay()+":"+ ConstantDeclare.PARACONFIG.FREEFEE)==null){
			return new FabAmount(0.00);
		} else{
			//返回账本类型对应的减免费用金额
			return    freeFee.get(lnsBill.getBillType()+":"+lnsBill.getRepayWay()+":"+ ConstantDeclare.PARACONFIG.FREEFEE);
		}
	}

	/**
	 * 将减免费用的最终值赋值给费用map
	 * @param lnsBill
	 * @param fabAmount
	 */
	public void setFreeFee(LnsBill lnsBill,FabAmount fabAmount){
		freeFee.put(lnsBill.getBillType()+":"+lnsBill.getRepayWay()+":"+ ConstantDeclare.PARACONFIG.FREEFEE,fabAmount);
	}

	public void setFreeFee(Map<String, FabAmount> freeFee) {
		this.freeFee = freeFee;
	}

	public String getFirstRepayDate() {
		return firstRepayDate;
	}

	public void setFirstRepayDate(String firstRepayDate) {
		this.firstRepayDate = firstRepayDate;
	}

}

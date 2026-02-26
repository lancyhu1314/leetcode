package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsMessageLeaseCalculation;
import com.suning.fab.loan.account.LnsRentPlanCalculate;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	SC
 *
 * @version V1.0.1
 *
 * @see 	还款计划生成
 *
 * 
 * 
 *
 *
 * @exception
 */

@Scope("prototype")
@Repository
public class Lns432 extends WorkUnit {

	FabAmount contractAmt;	//确认金额
	Integer periodNum;		//期限（扣息周期参数）
	String intPerUnit;		//扣息周期
	FabRate recoveryRate;		//回收比
	FabRate finalRate;	//尾款利率
	FabRate rentRate;		//租金利率
	FabRate floatRate;		//上浮比例
	String ccy;	//币种
	Integer repayDate;		//还款日
	String startIntDate;		//起始日期
	String endDate;		//结束日期
	Integer graceDays;
	FabRate normalRate;		//贷款宽限期利率
	FabRate overdueRate;	//贷款逾期利率
	FabRate compoundRate;	//贷款复利利率
	String normalRateType;	//贷款宽限期利率类型
	String overdueRateType;	//贷款逾期利率类型
	String compoundRateType;//贷款复利利率类型
	
	String repayWay;		//结束日期
	
	LoanAgreement loanAgreement;
	
	FabAmount finalAmt;		//尾款本息
	FabAmount finalPrin;	//尾款本金
	FabAmount rentAmt;		//租金本息
	FabAmount rentPrin;		//租金本金
	String pkgList;
	List<LnsRentPlanCalculate> rentPlanpkgList = new ArrayList<LnsRentPlanCalculate>();

	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();

		setLoanAgreement(LoanAgreementProvider.genLoanAgreementForRentCalculation(loanAgreement, ctx));//给loanAgreement赋值
		loanAgreement.getContract().setBalance(rentAmt);
		loanAgreement.getContract().setRentAmt(rentAmt);
		LnsBillStatistics billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,
				loanAgreement.getContract().getContractStartDate(), ctx);//将还款日期设置成合同起始日期
		if (null == billStatistics || null == billStatistics.getFutureBillInfoList()) {
			throw new FabException("LNS021");//"lns401获取还款计划失败!"
		}
		
		List<LnsBill> lnsBillTemp = billStatistics.getFutureBillInfoList();
		List<LnsMessageLeaseCalculation> lnsRepayPlanCalList = new ArrayList<LnsMessageLeaseCalculation>();
	
		Iterator<LnsBill> iterator = lnsBillTemp.iterator();
		Map<Integer, LnsMessageLeaseCalculation> repayPlanMap = new HashMap<Integer, LnsMessageLeaseCalculation>();
		while(iterator.hasNext()) {
			//获取每条记录的peroid值，如果相同则合并，否则创建一个新key
			LnsBill bill = iterator.next();
			LnsMessageLeaseCalculation lnsMessageLeaseCalculation = new LnsMessageLeaseCalculation();

			if (null != repayPlanMap.get(bill.getPeriod())) {
				
				lnsMessageLeaseCalculation = repayPlanMap.get(bill.getPeriod());
				lnsMessageLeaseCalculation.setTermRent(bill.getBillAmt());
				lnsMessageLeaseCalculation.setTermPrin(bill.getBillAmt());
				
			} else {
				
				lnsMessageLeaseCalculation.setRepayTerm(bill.getPeriod());//还款期数
//				lnsMessageLeaseCalculation.setCcy(loanAgreement.getContract().getCcy().getCcy());//币种
				lnsMessageLeaseCalculation.setBeginDate(bill.getStartDate());//本期起日
				lnsMessageLeaseCalculation.setEndDate(bill.getEndDate());//本期止日

				lnsMessageLeaseCalculation.setTermRent(bill.getBillAmt());
				lnsMessageLeaseCalculation.setTermPrin(bill.getBillAmt());

			}
			
			repayPlanMap.put(bill.getPeriod(), lnsMessageLeaseCalculation);
		}
		
		Iterator<Map.Entry<Integer, LnsMessageLeaseCalculation>> iterMap = repayPlanMap.entrySet().iterator();
		
		while (iterMap.hasNext()) {//将Map中的value放入list中
			LnsMessageLeaseCalculation calculate2 = iterMap.next().getValue();
			lnsRepayPlanCalList.add(calculate2);
			
			LnsRentPlanCalculate calculate3 = new LnsRentPlanCalculate();
			calculate3.setRepayterm(calculate2.getRepayTerm());
			calculate3.setTermeDate(calculate2.getEndDate());
			calculate3.setIntbdate(calculate2.getBeginDate());
			calculate3.setIntedate(calculate2.getEndDate());
			calculate3.setTermPrin(calculate2.getTermPrin());
			calculate3.setTermInt(new FabAmount(0.0));
			calculate3.setDays(graceDays);
			calculate3.setCalcIntFlag1("1");
			calculate3.setCalcIntFlag2("1");
			calculate3.setNormalRate(new FabRate(0.0));
			if(VarChecker.isEmpty(overdueRate)||VarChecker.isEmpty(overdueRateType)){
				calculate3.setOverdueRate(new FabRate(0.0));
			}else{
				calculate3.setOverdueRate(overdueRate);
			}
			if(VarChecker.isEmpty(compoundRate)||VarChecker.isEmpty(compoundRateType)){
				calculate3.setCompoundRate(new FabRate(0.0));
			}else{
				calculate3.setCompoundRate(compoundRate);
			}
			calculate3.setNormalRateType("Y");
			rentPlanpkgList.add(calculate3);
		}
		Collections.sort(lnsRepayPlanCalList);//将list中的内容按repayterm进行排序
		Collections.sort(rentPlanpkgList);
		lnsRepayPlanCalList.get(lnsRepayPlanCalList.size()-1).setTermTotal(finalAmt);
		lnsRepayPlanCalList.get(lnsRepayPlanCalList.size()-1).setTermPrin(new FabAmount(lnsRepayPlanCalList.get(lnsRepayPlanCalList.size()-1).getTermPrin().getVal()+finalAmt.getVal()));
		rentPlanpkgList.get(rentPlanpkgList.size()-1).setTermPrin(lnsRepayPlanCalList.get(lnsRepayPlanCalList.size()-1).getTermPrin());
		setPkgList(JsonTransfer.ToJson(lnsRepayPlanCalList));
	}

	public FabAmount getContractAmt() {
		return contractAmt;
	}

	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	public Integer getPeriodNum() {
		return periodNum;
	}

	public void setPeriodNum(Integer periodNum) {
		this.periodNum = periodNum;
	}

	public String getIntPerUnit() {
		return intPerUnit;
	}

	public void setIntPerUnit(String intPerUnit) {
		this.intPerUnit = intPerUnit;
	}



	public String getCcy() {
		return ccy;
	}

	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	public Integer getRepayDate() {
		return repayDate;
	}

	public void setRepayDate(Integer repayDate) {
		this.repayDate = repayDate;
	}

	public String getStartIntDate() {
		return startIntDate;
	}

	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}

	public FabAmount getFinalAmt() {
		return finalAmt;
	}

	public void setFinalAmt(FabAmount finalAmt) {
		this.finalAmt = finalAmt;
	}

	public FabAmount getFinalPrin() {
		return finalPrin;
	}

	public void setFinalPrin(FabAmount finalPrin) {
		this.finalPrin = finalPrin;
	}

	public FabAmount getRentAmt() {
		return rentAmt;
	}

	public void setRentAmt(FabAmount rentAmt) {
		this.rentAmt = rentAmt;
	}

	public FabAmount getRentPrin() {
		return rentPrin;
	}

	public void setRentPrin(FabAmount rentPrin) {
		this.rentPrin = rentPrin;
	}

	public String getPkgList() {
		return pkgList;
	}

	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
	}

	public String getRepayWay() {
		return repayWay;
	}

	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
	}

	/**
	 * @return the rentPlanpkgList
	 */
	public List<LnsRentPlanCalculate> getRentPlanpkgList() {
		return rentPlanpkgList;
	}

	/**
	 * @param rentPlanpkgList the rentPlanpkgList to set
	 */
	public void setRentPlanpkgList(List<LnsRentPlanCalculate> rentPlanpkgList) {
		this.rentPlanpkgList = rentPlanpkgList;
	}

	/**
	 * @return the recoveryRate
	 */
	public FabRate getRecoveryRate() {
		return recoveryRate;
	}

	/**
	 * @param recoveryRate the recoveryRate to set
	 */
	public void setRecoveryRate(FabRate recoveryRate) {
		this.recoveryRate = recoveryRate;
	}

	/**
	 * @return the finalRate
	 */
	public FabRate getFinalRate() {
		return finalRate;
	}

	/**
	 * @param finalRate the finalRate to set
	 */
	public void setFinalRate(FabRate finalRate) {
		this.finalRate = finalRate;
	}

	/**
	 * @return the rentRate
	 */
	public FabRate getRentRate() {
		return rentRate;
	}

	/**
	 * @param rentRate the rentRate to set
	 */
	public void setRentRate(FabRate rentRate) {
		this.rentRate = rentRate;
	}

	/**
	 * @return the floatRate
	 */
	public FabRate getFloatRate() {
		return floatRate;
	}

	/**
	 * @param floatRate the floatRate to set
	 */
	public void setFloatRate(FabRate floatRate) {
		this.floatRate = floatRate;
	}

	/**
	 * @return the graceDays
	 */
	public Integer getGraceDays() {
		return graceDays;
	}

	/**
	 * @param graceDays the graceDays to set
	 */
	public void setGraceDays(Integer graceDays) {
		this.graceDays = graceDays;
	}

	/**
	 * @return the normalRate
	 */
	public FabRate getNormalRate() {
		return normalRate;
	}

	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}

	/**
	 * @return the overdueRate
	 */
	public FabRate getOverdueRate() {
		return overdueRate;
	}

	/**
	 * @param overdueRate the overdueRate to set
	 */
	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
	}

	/**
	 * @return the compoundRate
	 */
	public FabRate getCompoundRate() {
		return compoundRate;
	}

	/**
	 * @param compoundRate the compoundRate to set
	 */
	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
	}

	/**
	 * @return the normalRateType
	 */
	public String getNormalRateType() {
		return normalRateType;
	}

	/**
	 * @param normalRateType the normalRateType to set
	 */
	public void setNormalRateType(String normalRateType) {
		this.normalRateType = normalRateType;
	}

	/**
	 * @return the overdueRateType
	 */
	public String getOverdueRateType() {
		return overdueRateType;
	}

	/**
	 * @param overdueRateType the overdueRateType to set
	 */
	public void setOverdueRateType(String overdueRateType) {
		this.overdueRateType = overdueRateType;
	}

	/**
	 * @return the compoundRateType
	 */
	public String getCompoundRateType() {
		return compoundRateType;
	}

	/**
	 * @param compoundRateType the compoundRateType to set
	 */
	public void setCompoundRateType(String compoundRateType) {
		this.compoundRateType = compoundRateType;
	}

	



}

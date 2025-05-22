package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	SC
 *
 * @version V1.0.1
 *
 * @see 	计算租金总本息和总本金，尾款总本息和总本金
 *
 *
 *
 *
 *
 * @exception
 */

@Scope("prototype")
@Repository
public class Lns431 extends WorkUnit {

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
	
	LoanAgreement loanAgreement;

	FabAmount finalAmt;		//尾款本息
	FabAmount finalPrin;	//尾款本金
	FabAmount rentAmt;		//租金本息
	FabAmount rentPrin;		//租金本金
	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		//参数验证
		if(VarChecker.isEmpty(loanAgreement.getContract().getContractAmt())||!loanAgreement.getContract().getContractAmt().isPositive()){
			throw new FabException("LNS056","确认金额");
		}
		if(VarChecker.isEmpty(periodNum)||periodNum<0){
			throw new FabException("LNS056","期限");
		}
		if(VarChecker.isEmpty(intPerUnit)){
			throw new FabException("LNS055","扣息周期");
		}
		if(VarChecker.isEmpty(recoveryRate)){
			throw new FabException("LNS055","回收比");
		}
		if(VarChecker.isEmpty(finalRate)){
			throw new FabException("LNS055","尾款利率");
		}
		if(VarChecker.isEmpty(rentRate)){
			throw new FabException("LNS055","租金利率");
		}
		if(VarChecker.isEmpty(floatRate)){
			throw new FabException("LNS055","上浮比例");
		}
		if(VarChecker.isEmpty(loanAgreement.getContract().getCcy())){
			throw new FabException("LNS055","币种");
		}
		if(VarChecker.isEmpty(loanAgreement.getContract().getRepayDate())){
			throw new FabException("LNS055","还款日");
		}
		if(VarChecker.isEmpty(loanAgreement.getContract().getContractStartDate())){
			throw new FabException("LNS055","起始日期");
		}
		if(VarChecker.isEmpty(loanAgreement.getContract().getContractEndDate())){
			throw new FabException("LNS055","结束日期");
		}
		if(!Date.valueOf(loanAgreement.getContract().getContractEndDate()).after(Date.valueOf(loanAgreement.getContract().getContractStartDate())))
		{
			throw new FabException("LNS036");
		}

		//test
		//取试算信息
		setLoanAgreement(LoanAgreementProvider.genLoanAgreementForRentCalculation(loanAgreement, ctx));//给loanAgreement赋值
		loanAgreement.getContract().setBalance(loanAgreement.getContract().getContractAmt());
		LnsBillStatistics billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,
				loanAgreement.getContract().getContractStartDate(), ctx);//将还款日期设置成合同起始日期
		if (null == billStatistics || null == billStatistics.getFutureBillInfoList()) {
			throw new FabException("LNS021");//"lns401获取还款计划失败!"
		}

		Integer periods = billStatistics.getFutureBillInfoList().size();

		//calculate code
		//尾款总本息 = 确认金额*回收比
		BigDecimal finalAmtDec = BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()).multiply(recoveryRate.getVal()).setScale(2,BigDecimal.ROUND_HALF_UP);
		FabAmount finalAmt = new FabAmount(finalAmtDec.doubleValue());
		this.setFinalAmt(finalAmt);

		//实际天数(待确认公式问题是否一致)
		int nDays = CalendarUtil.actualDaysBetween(loanAgreement.getContract().getContractStartDate(), loanAgreement.getContract().getContractEndDate());
		//尾款总本金
		BigDecimal finalPrinRate = finalRate.getDayRate().multiply(BigDecimal.valueOf(nDays)).add(BigDecimal.valueOf(1));
//		BigDecimal finalPrinRate = finalRate.getVal().multiply(new BigDecimal(periods)).divide(new BigDecimal(12),20,BigDecimal.ROUND_HALF_UP).add(new BigDecimal(1));
		BigDecimal finalPrinDec = BigDecimal.valueOf(finalAmt.getVal()).divide(finalPrinRate,20,BigDecimal.ROUND_HALF_UP).setScale(2,BigDecimal.ROUND_HALF_UP);
		FabAmount finalPrin = new FabAmount(finalPrinDec.doubleValue());
		this.setFinalPrin(finalPrin);

		//租金总本金 = 确认金额*（1+上浮比例）-尾款总本金
		BigDecimal rentPrinRate = BigDecimal.valueOf(1).add(floatRate.getVal());
		BigDecimal rentPrinDec = BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()).multiply(rentPrinRate).subtract(BigDecimal.valueOf(finalPrinDec.doubleValue())).setScale(2,BigDecimal.ROUND_HALF_UP);
		FabAmount rentPrin = new FabAmount(rentPrinDec.doubleValue());
		this.setRentPrin(rentPrin);
		
		//租金总本息 = 租金总本金*（1+年利率*期数/12）
		BigDecimal rentAmtRate = BigDecimal.valueOf(periods).multiply(rentRate.getMonthRate()).add(BigDecimal.valueOf(1));
		BigDecimal rentAmtDec = rentPrinDec.multiply(rentAmtRate).setScale(2,BigDecimal.ROUND_HALF_UP);
		FabAmount rentAmt = new FabAmount(rentAmtDec.doubleValue());
		this.setRentAmt(rentAmt);
	}

	/**
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}

	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	/**
	 * @return the periodNum
	 */
	public Integer getPeriodNum() {
		return periodNum;
	}

	/**
	 * @param periodNum the periodNum to set
	 */
	public void setPeriodNum(Integer periodNum) {
		this.periodNum = periodNum;
	}

	/**
	 * @return the intPerUnit
	 */
	public String getIntPerUnit() {
		return intPerUnit;
	}

	/**
	 * @param intPerUnit the intPerUnit to set
	 */
	public void setIntPerUnit(String intPerUnit) {
		this.intPerUnit = intPerUnit;
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
	 * @return the ccy
	 */
	public String getCcy() {
		return ccy;
	}

	/**
	 * @param ccy the ccy to set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	/**
	 * @return the repayDate
	 */
	public Integer getRepayDate() {
		return repayDate;
	}

	/**
	 * @param repayDate the repayDate to set
	 */
	public void setRepayDate(Integer repayDate) {
		this.repayDate = repayDate;
	}

	/**
	 * @return the startIntDate
	 */
	public String getStartIntDate() {
		return startIntDate;
	}

	/**
	 * @param startIntDate the startIntDate to set
	 */
	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
	}

	/**
	 * @return the endDate
	 */
	public String getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	/**
	 * @param loanAgreement the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}

	/**
	 * @return the finalAmt
	 */
	public FabAmount getFinalAmt() {
		return finalAmt;
	}

	/**
	 * @param finalAmt the finalAmt to set
	 */
	public void setFinalAmt(FabAmount finalAmt) {
		this.finalAmt = finalAmt;
	}

	/**
	 * @return the finalPrin
	 */
	public FabAmount getFinalPrin() {
		return finalPrin;
	}

	/**
	 * @param finalPrin the finalPrin to set
	 */
	public void setFinalPrin(FabAmount finalPrin) {
		this.finalPrin = finalPrin;
	}

	/**
	 * @return the rentAmt
	 */
	public FabAmount getRentAmt() {
		return rentAmt;
	}

	/**
	 * @param rentAmt the rentAmt to set
	 */
	public void setRentAmt(FabAmount rentAmt) {
		this.rentAmt = rentAmt;
	}

	/**
	 * @return the rentPrin
	 */
	public FabAmount getRentPrin() {
		return rentPrin;
	}

	/**
	 * @param rentPrin the rentPrin to set
	 */
	public void setRentPrin(FabAmount rentPrin) {
		this.rentPrin = rentPrin;
	}

	

}

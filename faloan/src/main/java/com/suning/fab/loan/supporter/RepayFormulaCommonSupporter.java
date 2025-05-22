package com.suning.fab.loan.supporter;

import java.math.BigDecimal;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.utils.VarChecker;
@Scope("prototype")
@Repository
public class RepayFormulaCommonSupporter extends RepayFormulaSupporter {
	
	public RepayFormulaCommonSupporter() {
		super();
	}
	/**
	 * 计算当前应还本金：总剩余本金除以剩余总期数为每期应还本金
	 * @see 
	 */

	@Override
	public FabAmount getCurrPrin() {
		/*每期还款金额为合同金额除以总期数*/
		FabAmount repayAmt = new FabAmount();
		if (totalPeriods != 0)
		{
			repayAmt.selfAdd(new FabAmount(loanAgreement.getContract().getBalance().getVal()/this.totalPeriods));
		}
		else
		{
			repayAmt.selfAdd(new FabAmount(loanAgreement.getContract().getBalance().getVal()));
		}
		
		repayAmt = super.optMinRepayPrinAmt(repayAmt);
		
		if (loanAgreement.getContract().getBalance().sub(repayAmt).isNegative())
		{
			return loanAgreement.getContract().getBalance();
		}
		
		return repayAmt;
	}
	/**
	 * 获取计息对应本金
	 * 
	 */
	@Override
	public FabAmount getCalIntPrin() {
		return loanAgreement.getContract().getBalance();
	}
	
	/**
	 * 计算剩余本金：总剩余本金减去当期应还本金为剩余本金
	 * @see 
	 */
	@Override
	public FabAmount getBalance() {
		return (FabAmount) loanAgreement.getContract().getBalance().sub(this.getCurrPrin());
	}
	/**
	 * 计算当期利息：计算开始日期到结束日期之间剩余本金所产生的利息
	 * @param balance   剩余本金
	 * @param startDate 计息开始日期 
	 * @param endDate   结束日期
	 * @throws Exception 
	 * @see 
	 */
	@Override
	public FabAmount getCurrInterest(String startDate,String endDate)
	{
		String periodIntStartDate = super.calIntCurrPeriodStartDate();
		if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail()) && !startDate.equals(loanAgreement.getContract().getContractStartDate())){
			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
		}
		BigDecimal interest =  new BigDecimal(CalendarUtil.fixedDaysBetween(periodIntStartDate, endDate, loanAgreement.getInterestAgreement().getIntBase()))
				.multiply(loanAgreement.getRateAgreement().getNormalRate().getDayRate())
				.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal())).setScale(2,BigDecimal.ROUND_HALF_UP);
		
		//计算每天利息并四舍五入
		BigDecimal overInterest = loanAgreement.getRateAgreement().getNormalRate().getDayRate()
						.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
		//乘以开始日期到还款日天数
		overInterest = overInterest.multiply(new BigDecimal(CalendarUtil.actualDaysBetween(periodIntStartDate, startDate)));
	
		FabAmount interest1 =  new FabAmount();
		//总利息减去已结利息
		interest1.selfAdd(interest.subtract(overInterest).doubleValue());
		//封顶计息
		FabAmount capInterest = getCapInterestNoRepayDate(new FabAmount(interest1.getVal()));
		if(!VarChecker.isEmpty(capInterest)){
			interest1 = capInterest;
		}
		return interest1.isNegative()?new FabAmount(0.00):interest1;
	}
	
	
	
	/**
	 * @param startDate 开始日期
	 * @param endDate   结束日期
	 * @param repayDate 还款日
	 * @throws Exception 
	 * @see 
	 */
	@Override
	public FabAmount getRepayDateInterest(String startDate, String endDate,
			String repayDate) {
		//未来期账单计提金额为空
		if (CalendarUtil.beforeAlsoEqual(repayDate, startDate) && "false".equals(loanAgreement.getInterestAgreement().getIsCalTail()))
		{
			return null;
		}
		//P2P特殊处理(算头算尾)
		if (CalendarUtil.before(repayDate, startDate) && "true".equals(loanAgreement.getInterestAgreement().getIsCalTail()))
		{
			return null;
		}
		FabAmount currInterest = this.getCurrInterest(startDate, endDate);
		
		//账速融接账务核心 2019-02-14
		if( "3010014".equals( loanAgreement.getPrdId() ))
		{
			return currInterest;
		}
		
		//未结息利息账单，计提金额等于当前利息
		if (CalendarUtil.after(repayDate, endDate))
		{
			//封顶计息
			FabAmount capInterest = getCapInterest(currInterest,repayDate);
			if(!VarChecker.isEmpty(capInterest)){
				return capInterest;
			}
			return currInterest;
		}
		BigDecimal totalInterest = new BigDecimal(currInterest.getVal());
		//还款日期早于结束日期,利息为开始日期到还款日之间的利息，按照实际天数计算
		if (CalendarUtil.before(repayDate, endDate))
		{
			FabAmount interest =  new FabAmount();
			//计算每天利息并四舍五入
			BigDecimal repayDateInt = loanAgreement.getRateAgreement().getNormalRate().getDayRate()
							.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
			//乘以开始日期到还款日天数
			if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail()) && !startDate.equals(loanAgreement.getContract().getContractStartDate())){
				repayDateInt = repayDateInt.multiply(new BigDecimal(CalendarUtil.actualDaysBetween(startDate, repayDate)+1));
			}else{
				repayDateInt = repayDateInt.multiply(new BigDecimal(CalendarUtil.actualDaysBetween(startDate, repayDate)));
			}
			if (repayDateInt.compareTo(totalInterest)>0)
			{
				interest.selfAdd(totalInterest.doubleValue());
				//封顶计息
				FabAmount capInterest = getCapInterest(interest,repayDate);
				if(!VarChecker.isEmpty(capInterest)){
					return capInterest;
				}
				return interest;
			}
			
			interest.selfAdd(repayDateInt.doubleValue());
			//封顶计息
			FabAmount capInterest = getCapInterest(interest,repayDate);
			if(!VarChecker.isEmpty(capInterest)){
				return capInterest;
			}
			return interest;
		}
		if (CalendarUtil.equalDate(repayDate, endDate))
		{
			//封顶计息
			FabAmount capInterest = getCapInterest(currInterest,repayDate);
			if(!VarChecker.isEmpty(capInterest)){
				return capInterest;
			}
			return currInterest;
			
		}
		return null;
	}
	
	//封顶计息
	public FabAmount getCapInterest(FabAmount currInterest,String repayDate) {
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate()) 
				&& CalendarUtil.after(repayDate, loanAgreement.getBasicExtension().getComeinDate())
				){
			if(!currInterest.add(loanAgreement.getBasicExtension().getHisComein()).
					sub(loanAgreement.getBasicExtension().getCapAmt()).isNegative()){
				FabAmount result = new FabAmount((loanAgreement.getBasicExtension().getCapAmt().getVal())
						-loanAgreement.getBasicExtension().getHisComein().getVal());
				if(result.isNegative()){
					return new FabAmount(0.0);
				}
				return result;
			}else{
//				loanAgreement.getBasicExtension().getHisComein().selfAdd(currInterest);
				return null;
			}
		}
		return null;
	}
	
	//封顶计息
	public FabAmount getCapInterestNoRepayDate(FabAmount currInterest) {
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
			if(!currInterest.add(loanAgreement.getBasicExtension().getHisComein()).
					sub(new FabAmount(loanAgreement.getBasicExtension().getCapAmt().getVal())).isNegative()){
				FabAmount result = new FabAmount((loanAgreement.getBasicExtension().getCapAmt().getVal())
						-loanAgreement.getBasicExtension().getHisComein().getVal());
				if(result.isNegative()){
					return new FabAmount(0.0);
				}
				return result;
			}
		}
		return null;
	}
	
}

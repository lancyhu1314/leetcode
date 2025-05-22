package com.suning.fab.loan.supporter;

import java.math.BigDecimal;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 
 *
 * @version V1.0.0
 *
 * @see 气球贷计算
 *
 */

@Scope("prototype")
@Repository
public class RepayFormulaBalloonSupporter extends RepayFormulaSupporter {

	static int DECIMAL_SCALE = 9;
	static BigDecimal BIGDECAMAL_100 = new BigDecimal(100);
	static BigDecimal BIGDECAMAL_12 = new BigDecimal(12);
	static BigDecimal BIGDECAMAL_30 = new BigDecimal(30);
	static BigDecimal BIGDECAMAL_360 = new BigDecimal(360);

	public RepayFormulaBalloonSupporter() {
		super();
	}
	/**
	 * 计算每期应还本金
	 * 
	 * @param balance 剩余本金
	 * @param totalPeriods 总期数
	 * @return
	 * @throws Exception
	 */
	@Override
	public FabAmount getCurrPrin(){
		//根据等额本息公式计算应还本金
		Integer days =super.calRepayPeriodDays();

		BigDecimal interest = new BigDecimal(days)
								.multiply(loanAgreement.getRateAgreement().getNormalRate().getDayRate())
								.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal()));
		interest =  interest.setScale(2, BigDecimal.ROUND_HALF_UP);
		BigDecimal repayAmt = this.calPeriodRepayAmt(loanAgreement.getContract().getBalance(),
												loanAgreement.getRateAgreement().getNormalRate());
		
		//试算下期剩余本金对应利息是否为0，为0则将所有剩余本金在本期清零，本期还款金额为本期剩余本金金额
		BigDecimal nextPeriodInterest = new BigDecimal(days)
										.multiply(loanAgreement.getRateAgreement().getNormalRate().getDayRate())
										.multiply(BigDecimal.valueOf(loanAgreement.getContract().getBalance().getVal()).subtract(repayAmt).add(interest));
		if (nextPeriodInterest.doubleValue() < 0.005)
		{
			return loanAgreement.getContract().getBalance();
		}
		FabAmount amt = new FabAmount();
		
		amt.selfAdd(repayAmt.subtract(interest).doubleValue());
		LoggerUtil.debug("应还本金：{}",amt.getVal());
		return amt;
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
	 * 计算每期剩余本金
	 * 
	 * @param periodFormula
	 * @param
	 * @return
	 * @throws Exception
	 */
	@Override
	public FabAmount getBalance() {
		
		return (FabAmount) loanAgreement.getContract().getBalance().sub(this.getCurrPrin());
	}
	/**
	 * 计算每期还款利息:等额本息第一期、最后一期、提前还款后第一期按照实际天数计算利息
	 * 
	 * @param periodFormula
	 * @param
	 * @return
	 * @throws Exception
	 * @throws SecurityException
	 */
	@Override
	public FabAmount getCurrInterest(String startDate,String endDate)
	{
		//等额本息第一期、最后一期、提前还款后第一期按照实际天数计算利息（展期时是最后一期的）
		String currPrinStartDate = super.calPrinCurrPeriodStartDate();
		if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail()) && !startDate.equals(loanAgreement.getContract().getContractStartDate())){
			currPrinStartDate = CalendarUtil.nDaysAfter(currPrinStartDate, 1).toString("yyyy-MM-dd");
		}
		Integer days;
		if (!currPrinStartDate.equals(loanAgreement.getContract().getRepayPrinDate()) 
				|| loanAgreement.getContract().getRepayPrinDate().equals(loanAgreement.getContract().getContractStartDate()) 
				|| endDate.equals(loanAgreement.getContract().getContractEndDate())
				|| (!VarChecker.isEmpty(loanAgreement.getContract().getFlag1()) 
						&& loanAgreement.getContract().getFlag1().contains("A")
						&& loanAgreement.getBasicExtension().getLastPeriodStr().contains("1")
						&& (currPrinStartDate.equals(loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[1])
								|| currPrinStartDate.equals(loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[2]))))
		{
			days = CalendarUtil.actualDaysBetween(loanAgreement.getContract().getRepayPrinDate(), endDate);
			if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
				days =super.calRepayPeriodDays();
			}
		}
		else
		{
			//计算周期天数
			days = super.calRepayPeriodDays();
		}
		//周期利息
		BigDecimal totalInterest = new BigDecimal(days)
								.multiply(loanAgreement.getRateAgreement().getNormalRate().getDayRate())
								.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal()))
								.setScale(2, BigDecimal.ROUND_HALF_UP);
	
		//结息日早于等于结本日，则用整期天数计息
		if (CalendarUtil.beforeAlsoEqual(loanAgreement.getContract().getRepayIntDate(), loanAgreement.getContract().getRepayPrinDate()))
		{
			
			FabAmount interest1 =  new FabAmount();
			interest1.selfAdd(totalInterest.doubleValue());
			//周期利息是否超过历史收益20170117
			FabAmount capInterest = getCapInterestNoRepayDate(interest1);
			if(!VarChecker.isEmpty(capInterest)){
				interest1 = capInterest;
			}
			return interest1;
		}
		
		//计算结本日到开始日期之间的天数
		int overDays = CalendarUtil.actualDaysBetween(loanAgreement.getContract().getRepayPrinDate(), startDate);
		//计算每天利息并四舍五入
		BigDecimal overInterest = loanAgreement.getRateAgreement().getNormalRate().getDayRate()
				.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal()))
				.setScale(2, BigDecimal.ROUND_HALF_UP);
		//乘以天数
		overInterest = overInterest.multiply(new BigDecimal(overDays));
		FabAmount interest1 =  new FabAmount();
		//总利息减去已结利息
		interest1.selfAdd(totalInterest.subtract(overInterest).doubleValue());
		//周期利息是否超过历史收益20170117
		FabAmount capInterest = getCapInterestNoRepayDate(interest1);
		if(!VarChecker.isEmpty(capInterest)){
			interest1 = capInterest;
		}
		return interest1.isNegative()?new FabAmount(0.00):interest1;
		
	}
	/**
	 * 计算每期还款总金额
	 * 
	 * @param contractAmt   合同余额
	 * @param rate      利率      
	 * @param periodFormula 还款周期公式
	 * @return  每期还款金额
	 * @throws Exception
	 */
	public BigDecimal calPeriodRepayAmt(FabAmount contractAmt, FabRate rate) {

		BigDecimal totalInt = new BigDecimal(contractAmt.getVal());
		totalInt =  totalInt.setScale(2, BigDecimal.ROUND_HALF_UP);
		//根据还款周期公式计算总天数
		Integer periodDays = super.calRepayPeriodDays();

		
		Integer totalPeriods = super.calTotalPeriods(); 
		if (totalPeriods.equals(0))
		{
			return new BigDecimal("0.00");
		}
		
		BigDecimal  periodRate = new BigDecimal(periodDays).multiply(rate.getDayRate());
		BigDecimal  factorRate = periodRate.add(BigDecimal.valueOf(1)).pow(totalPeriods);
		// 每期还款金额：(合同金额+总利息)/总期数
		BigDecimal repayAmt = new BigDecimal(contractAmt.getVal()).multiply(periodRate)
								.multiply(factorRate).divide(factorRate.subtract(BigDecimal.valueOf(1)), 2, BigDecimal.ROUND_HALF_UP);

		LoggerUtil.debug("等额本息：每期还款总金额：{}   计息天数：{}   剩余本金:{}  总期数:{}   ",repayAmt,periodDays,totalInt,totalPeriods);
		return repayAmt;
	}
	/**
	 * @param startDate 开始日期
	 * @param endDate   结束日期
	 * @param repayDate 还款日
	 * @throws Exception 
	 * @throws SecurityException 
	 * @see 
	 */
	@Override
	public FabAmount getRepayDateInterest(String startDate, String endDate,
			String repayDate){
		
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
		
		//未结息利息账单，计提金额等于当前利息
		if (CalendarUtil.afterAlsoEqual(repayDate, endDate))
		{
			//20190114封顶计息
			FabAmount capInterest = getCapInterest(currInterest,repayDate);
			if(!VarChecker.isEmpty(capInterest)){
				return capInterest;
			}
			return currInterest;
		}
		
		BigDecimal totalInterest = new BigDecimal(currInterest.getVal());
		//还款日期早于结束日期,利息为开始日期到还款日之前的利息，按照实际天数计算
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
				//20190114封顶计息
				FabAmount capInterest = getCapInterest(interest,repayDate);
				if(!VarChecker.isEmpty(capInterest)){
					return capInterest;
				}
				return interest;
			}
			
			interest.selfAdd(repayDateInt.doubleValue());
			//20190114封顶计息
			FabAmount capInterest = getCapInterest(interest,repayDate);
			if(!VarChecker.isEmpty(capInterest)){
				return capInterest;
			}
			return interest;
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

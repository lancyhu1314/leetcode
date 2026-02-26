package com.suning.fab.loan.supporter;

import java.math.BigDecimal;

import com.suning.fab.loan.account.IntegerNum;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.la.AbstractUserDefineAgreement;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.la.UserDefineAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
@Scope("prototype")
@Repository
public class RepayFormulaUserDefineSupporter extends RepayFormulaSupporter {
	
	private UserDefineAgreement userDefineAgreement;

	@Override
	public void setLoanAgreement(LoanAgreement loanAgreement) throws FabException {
		
		this.loanAgreement = loanAgreement;
//		this.currentIntPeriod = loanAgreement.getContract().getCurrIntPeriod();
//		this.currentPrinPeriod = loanAgreement.getContract().getCurrPrinPeriod(); 

		//先本后息、有本有息取本金所在期
		if(ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(loanAgreement.getWithdrawAgreement().getRepayWay()) ||
		   ConstantDeclare.REPAYWAY.REPAYWAY_YBYX.equals(loanAgreement.getWithdrawAgreement().getRepayWay())  ||
		   ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(loanAgreement.getWithdrawAgreement().getRepayWay()))
			this.userDefineAgreement = (UserDefineAgreement)this.findCurrUserDefineAgreement(loanAgreement);
		//先息后本取利息所在期
		else if (ConstantDeclare.REPAYWAY.REPAYWAY_XXHB.equals(loanAgreement.getWithdrawAgreement().getRepayWay()))
			this.userDefineAgreement = (UserDefineAgreement)this.findCurrUserDefineIntAgreement(loanAgreement);
	}
	
	
	/**
	 * 计算当前利息期数   2018-01-24
	 * @return
	 */
	public Integer calCurrIntPeriod()
	{
		//先本后息的利息账单都置为最后一期
		if( ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals( loanAgreement.getWithdrawAgreement().getRepayWay()))
		{
			return loanAgreement.getUserDefineAgreement().size();
		}
		
		//本金账单已落表，上次结本日记到了当期到期日， 上次结息日到当期到期日产生的利息期数仍为上一期
		if( loanAgreement.getContract().getRepayPrinDate().equals(this.userDefineAgreement.getTermbDate()) &&
				CalendarUtil.before(loanAgreement.getContract().getRepayIntDate(),this.userDefineAgreement.getTermbDate()))
				return this.userDefineAgreement.getRepayTerm()-1;
		
		
		return this.userDefineAgreement.getRepayTerm();
	}

	/**
	 * 计算当前本金期数   2018-01-24
	 * @return
	 */
	public Integer calCurrPrinPeriod()
	{
		return this.userDefineAgreement.getRepayTerm();
	}
	
	

	/**
	 * 计算当前应还本金
	 * @see 
	 */
	@Override
	public FabAmount getCurrPrin() {
		/*每期还款金额为合同金额除以总期数*/
		return this.userDefineAgreement.getTermPrin();
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
	 *  balance   剩余本金
	 * @param startDate 计息开始日期 
	 * @param endDate   结束日期
	 * @throws Exception 
	 * @see 
	 */
	@Override
	public FabAmount getCurrInterest(String startDate,String endDate)
	{
		if( ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(loanAgreement.getWithdrawAgreement().getRepayWay()) )
			return userDefineAgreement.getTermInt();
		
		//当天不产生利息
		if(startDate.equals(endDate))
			return new FabAmount(0.00);
		
		String periodIntStartDate = this.userDefineAgreement.getTermbDate();
		
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
		
		//未结息利息账单，计提金额等于当前利息
		if (CalendarUtil.after(repayDate, endDate))
		{
			return currInterest;
		}
		BigDecimal totalInterest = new BigDecimal(currInterest.getVal());
		//还款日期早于结束日期,利息为开始日期到还款日之间的利息，按照实际天数计算
		if (CalendarUtil.before(repayDate, endDate))
		{
			FabAmount interest =  new FabAmount();
			BigDecimal repayDateInt;
			//计算每天利息并四舍五入
			if(ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(loanAgreement.getWithdrawAgreement().getRepayWay())){
				 repayDateInt=totalInterest.divide(new BigDecimal(CalendarUtil.actualDaysBetween(startDate, endDate)),2, BigDecimal.ROUND_HALF_UP);
			}else{
				 repayDateInt = loanAgreement.getRateAgreement().getNormalRate().getDayRate()
							.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
			}
			//乘以开始日期到还款日天数
			if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail()) && !startDate.equals(loanAgreement.getContract().getContractStartDate())){
				repayDateInt = repayDateInt.multiply(new BigDecimal(CalendarUtil.actualDaysBetween(startDate, repayDate)+1));
			}else{
				repayDateInt = repayDateInt.multiply(new BigDecimal(CalendarUtil.actualDaysBetween(startDate, repayDate)));
			}
			if (repayDateInt.compareTo(totalInterest)>0)
			{
				interest.selfAdd(totalInterest.doubleValue());
				return interest;
			}
			
			interest.selfAdd(repayDateInt.doubleValue());
			return interest;
		}
		if (CalendarUtil.equalDate(repayDate, endDate))
		{
			return currInterest;
			
		}
		return null;
	}
	
	/**
	 * 查找当前自定义协议
	 * @return
	 */
	private AbstractUserDefineAgreement findCurrUserDefineAgreement(LoanAgreement loanAgreement)
	{
		for(AbstractUserDefineAgreement userDefine :loanAgreement.getUserDefineAgreement())
		{
			UserDefineAgreement us = (UserDefineAgreement)userDefine;
			if (CalendarUtil.before(loanAgreement.getContract().getRepayPrinDate(), us.getTermeDate()))
				return us;
		}
		return loanAgreement.getUserDefineAgreement().get(loanAgreement.getUserDefineAgreement().size()-1);
	}
	private AbstractUserDefineAgreement findCurrUserDefineIntAgreement(LoanAgreement loanAgreement)
	{
		for(AbstractUserDefineAgreement userDefine :loanAgreement.getUserDefineAgreement())
		{
			UserDefineAgreement us = (UserDefineAgreement)userDefine;
			if (CalendarUtil.before(loanAgreement.getContract().getRepayIntDate(), us.getTermeDate()))
				return us;
		}
		return loanAgreement.getUserDefineAgreement().get(loanAgreement.getUserDefineAgreement().size()-1);
	}
	
	public String calPrinCurrPeriodEndDate()
	{
		return this.userDefineAgreement.getTermeDate();
	}
	
	
	public String calIntCurrPeriodEndDate() 
	{
		String endDate = this.getUserDefineAgreement().getTermeDate();		
		if( CalendarUtil.before(loanAgreement.getContract().getRepayIntDate(), this.getUserDefineAgreement().getTermbDate()) )
			endDate = this.getUserDefineAgreement().getTermbDate();
		
		return endDate;
	}
	
	
	
	
	
	
	/**
	 * @return the userDefineAgreement
	 */
	public UserDefineAgreement getUserDefineAgreement() {
		return userDefineAgreement;
	}
	/**
	 * @param userDefineAgreement the userDefineAgreement to set
	 */
	public void setUserDefineAgreement(UserDefineAgreement userDefineAgreement) {
		this.userDefineAgreement = userDefineAgreement;
	}
}

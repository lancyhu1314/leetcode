package com.suning.fab.loan.supporter;

import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * author :16090227
 * 任性付等本等系公式计算
 *
 * author:chenchao
 *
 */
@Scope("prototype")
@Repository
public class RepayFormulaWayWardSupporter extends RepayFormulaSupporter {

	public RepayFormulaWayWardSupporter() {
		super();
	}
// 在父类方法里做个判断20191203
	@Override
	public void setLoanAgreement(LoanAgreement loanAgreement) throws FabException {
		super.setLoanAgreement(loanAgreement);
		/*super.loanAgreement = loanAgreement;
		super.prinRepayPeriod = RepayPeriodSupporter.genRepayPeriod(super.loanAgreement.getWithdrawAgreement().getPeriodFormula());
		super.intRepayPeriod  = RepayPeriodSupporter.genRepayPeriod(super.loanAgreement.getInterestAgreement().getPeriodFormula());*/
//		int a = this.getTotalPeriods();
//		if(!super.totalPeriods.equals(a)){
//			throw new FabException("error super "+totalPeriods+" "+a);
//		}

	}

	/**
	 * 计算总期数
	 * @return
	 * @throws Exception
	 */
	private Integer getTotalPeriods()
	{
		String sDate = this.loanAgreement.getContract().getContractStartDate();
		Integer periods = 0;
		while (CalendarUtil.actualDaysBetween(sDate, this.loanAgreement.getContract().getContractEndDate()) > 0) {
			periods += 1;
			LoggerUtil.debug("第" + periods + "期 ，  开始日期：" + sDate);
			if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
				sDate = RepayPeriodSupporter.calculateP2PEndDate(sDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod);
			}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("1")){
				sDate = RepayPeriodSupporter.calculateEndDateByFlag1(sDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod);
			}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("8")){
				sDate = RepayPeriodSupporter.calculateEndDateByFlag8(sDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod);
			}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1())
					&& this.loanAgreement.getContract().getFlag1().contains("A")
					&& this.loanAgreement.getBasicExtension().getLastPeriodStr().contains("1")
					&& CalendarUtil.before(sDate, this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[2])
					&& sDate.equals(this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[1])){
				String[]  lastPeriodStr = this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",");
				// 如果晚于合同到期日期，则返回合同到日期
				if (Days.daysBetween(new DateTime(lastPeriodStr[2]),new DateTime(this.loanAgreement.getContract().getContractEndDate()) ).getDays() <= this.loanAgreement.getWithdrawAgreement().getPeriodMinDays()) {
					sDate = this.loanAgreement.getContract().getContractEndDate();
				}else{
					sDate = lastPeriodStr[2];
				}

			}else if(!this.loanAgreement.getWithdrawAgreement().getFirstTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getLastTermMerge()){
				sDate = RepayPeriodSupporter.calculateEndDateBySetting(sDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod,
						this.loanAgreement.getWithdrawAgreement(),true);
			}else{
				sDate = RepayPeriodSupporter.calculateEndDate(sDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod);
			}
			LoggerUtil.debug("第" + periods + "期 ，结束日期：" + sDate);
			if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
				sDate = CalendarUtil.nDaysAfter(sDate, 1).toString("yyyy-MM-dd");
			}
		}
		return  periods;
	}

	/**
	 * 计算当前应还本金：总剩余本金除以剩余总期数为每期应还本金
	 */

	@Override
	public FabAmount getCurrPrin() {
		/*每期还款金额为合同金额除以总期数*/
		FabAmount repayAmt = new FabAmount();
		if (totalPeriods == 0)
		{
			repayAmt.selfAdd(new FabAmount(loanAgreement.getContract().getBalance().getVal()));
		}
		else
		{
			//通过判断退货金额是否为空 判断本金是否需要变化
			if(loanAgreement.getBasicExtension().getSalesReturnAmt()==null){
				repayAmt.selfAdd(new FabAmount(loanAgreement.getContract().getContractAmt().getVal()
						/totalPeriods));
			}else{
				//对特殊的账户 进行特殊处理
				if(GlobalScmConfUtil.getProperty("returnAcctNo","DEFAULT").contains(loanAgreement.getContract().getReceiptNo())){
					Double returnPrin=loanAgreement.getBasicExtension().getSalesReturnAmt().getVal()
							/(totalPeriods-loanAgreement.getBasicExtension().getInitFuturePeriodNum()+1);
					BigDecimal returnBig= BigDecimal.valueOf(returnPrin);
					double   returnValue   =   returnBig.setScale(2,   BigDecimal.ROUND_DOWN).doubleValue();
					repayAmt.selfAdd(new FabAmount(returnValue));
				}else{
									repayAmt.selfAdd(new FabAmount(loanAgreement.getBasicExtension().getSalesReturnAmt().getVal()
						/(totalPeriods-loanAgreement.getBasicExtension().getInitFuturePeriodNum()+1)));
				}
			}
		}
		
		if( "true".equals(loanAgreement.getInterestAgreement().getMinimumPayment()) ){
			repayAmt = super.optScmMinRepayPrinAmt(repayAmt);
			if(loanAgreement.getContract().getBalance().sub(
					new FabAmount(Double.valueOf(GlobalScmConfUtil.getProperty("minimumPayment","1.00")))).sub(
							repayAmt.getVal()).isNegative() )
			{
				repayAmt.selfAdd(loanAgreement.getContract().getBalance());
			}
		}
		else
			repayAmt = super.optMinRepayPrinAmt(repayAmt);

		if (loanAgreement.getContract().getBalance().sub(repayAmt).isNegative() ||
			calPrinCurrPeriodEndDate().equals(loanAgreement.getContract().getContractEndDate()) )
		{
			return loanAgreement.getContract().getBalance();
		}
//		//最后一期取合同余额
//		if( calPrinCurrPeriodEndDate().equals(loanAgreement.getContract().getContractEndDate()))
//		{
//			return loanAgreement.getContract().getBalance();
//		}
		return repayAmt;
	}


	/**
	 * 获取计息对应本金
	 *
	 */
	@Override
	public FabAmount getCalIntPrin() {
		return loanAgreement.getContract().getContractAmt();
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
	 * 计算当期利息：计算开始日期到结束日期之间合同金额所产生的利息

	 * @param startDate 计息开始日期 
	 * @param endDate   结束日期
	 * @throws Exception
	 * @see
	 */
	@Override
	public FabAmount getCurrInterest(String startDate,String endDate)
	{
		FabAmount interest =  new FabAmount();

		BigDecimal bigInt = new BigDecimal(super.calRepayPeriodDays());

		bigInt = bigInt.multiply(loanAgreement.getRateAgreement().getNormalRate().getDayRate());

//		bigInt = bigInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()))
//				.setScale(2, BigDecimal.ROUND_HALF_UP);
		//修改退货后的合同金额
		if(loanAgreement.getBasicExtension().getSalesReturnAmt()==null){
			bigInt = bigInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()))
					.setScale(2, BigDecimal.ROUND_HALF_UP);
		}else{
			bigInt = bigInt.multiply(BigDecimal.valueOf(loanAgreement.getBasicExtension().getSalesReturnAmt().getVal()))
					.setScale(2, BigDecimal.ROUND_HALF_UP);
		}

		interest.selfAdd(bigInt.doubleValue());
		return interest;
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
			//计算每天利息并四舍五入
			BigDecimal repayDateInt = loanAgreement.getRateAgreement().getNormalRate().getDayRate()
					.multiply(new BigDecimal(loanAgreement.getContract().getContractAmt().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
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
}

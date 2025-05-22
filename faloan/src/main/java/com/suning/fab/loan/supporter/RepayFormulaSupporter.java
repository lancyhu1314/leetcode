package com.suning.fab.loan.supporter;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.RepayPeriod;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public abstract class RepayFormulaSupporter {

	protected LoanAgreement loanAgreement;

	protected RepayPeriod prinRepayPeriod;

	protected RepayPeriod intRepayPeriod;

	protected Integer     totalPeriods;

	protected Integer     currentIntPeriod;

	protected Integer     currentPrinPeriod;

	private String prinCurrPeriodEndDate;
	
	protected Integer     totalIntPeriods;


//	//周末改工作日
//	private static List<String> workDays  = new ArrayList<>();
//	//工作日改假日
//	private static List<String> holidays  = new ArrayList<>();



	//初始化
//	static{
//		//查询数据库
//		List<TblStatutorydays> tblStatutorydays = null;
//		try {
//			tblStatutorydays = DbAccessUtil.queryForList("Statutorydays.query", new HashMap<>(), TblStatutorydays.class);
//		}catch (FabException e){
//			//静态代码块不能抛出异常
//			LoggerUtil.error(e.getMessage());
//		}
//		if(tblStatutorydays!=null) {
//			for (TblStatutorydays statutoryday : tblStatutorydays) {
//				if (ConstantDeclare.DATETYPE.WORKDAY.equals(statutoryday.getType())) {
//					workDays.add(statutoryday.getDate().toString());
//				} else {
//					holidays.add(statutoryday.getDate().toString());
//				}
//
//			}
//		}
//	}
	/**
	 * 获取本期应还本金

	 * @return
	 * @throws SecurityException
	 * @throws Exception
	 */
	public abstract FabAmount getCurrPrin();

	/**
	 * 计算本期剩余本金
	 * @return
	 * @throws Exception
	 */
	public abstract FabAmount getBalance();

	/**
	 * 获取计息对应本金
	 * @return
	 */
	public abstract FabAmount getCalIntPrin();

	/**
	 * 计算本期利息
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public abstract FabAmount getCurrInterest(String startDate,String endDate);

	/**
	 * 当前还款日应还利息
	 * @param startDate
	 * @param endDate
	 * @param repayDate
	 * @return
	 * @throws Exception
	 */
	public abstract FabAmount getRepayDateInterest(String startDate,String endDate,String repayDate);
	public LoanAgreement getLoanAgreement() {
		return this.loanAgreement;
	}
	public void setLoanAgreement(LoanAgreement loanAgreement) throws FabException {
		this.loanAgreement = loanAgreement;
		this.prinRepayPeriod = RepayPeriodSupporter.genRepayPeriod(this.loanAgreement.getWithdrawAgreement().getPeriodFormula());
		this.intRepayPeriod  = RepayPeriodSupporter.genRepayPeriod(this.loanAgreement.getInterestAgreement().getPeriodFormula());
        this.totalPeriods = this.calTotalPeriods();
        this.totalIntPeriods = this.calTotalIntPeriods();
//        //本金算的总期数
//		if(VarChecker.isEmpty(totalperiods.getNum())) {
//			this.totalPeriods = this.calTotalPeriods();
//			totalperiods.setNum( totalPeriods);
//		}else{
//			this.totalPeriods = totalperiods.getNum();
//		}
		//2018-01-20 增加  start

		this.currentIntPeriod = loanAgreement.getContract().getCurrIntPeriod();
		if (loanAgreement.getContract().getCurrIntPeriod().longValue()<loanAgreement.getContract().getCurrPrinPeriod().longValue())
		{
			this.currentIntPeriod = loanAgreement.getContract().getCurrPrinPeriod();
		}
		this.currentPrinPeriod = this.currentIntPeriod;

		//计算当前期数
		if (RepayPeriodSupporter.isCumulativeIntPeriod(loanAgreement.getWithdrawAgreement().getRepayWay()))
		{
			loanAgreement.getContract().setCurrIntPeriod(loanAgreement.getContract().getCurrIntPeriod()+1);
			loanAgreement.getContract().setCurrPrinPeriod(loanAgreement.getContract().getCurrIntPeriod());
		}

		//2018-01-20 增加  end
	}

	/**
	 * 计算当前利息期数   2018-01-20
	 * @return
	 */
	public Integer calCurrIntPeriod()
	{
		return this.currentIntPeriod;
	}
	/**
	 * 计算当前本金期数   2018-01-20
	 * @return
	 */
	public Integer calCurrPrinPeriod()
	{
		return this.currentPrinPeriod;
	}

	/**
	 * 根据还款公式计算周期内天数
	 *
	 */
	public Integer calRepayPeriodDays() {

		return this.intRepayPeriod.getPeriodY() * this.loanAgreement.getRateAgreement().getNormalRate().getDaysPerYear()
				+ this.intRepayPeriod.getPeriodH() * 6 * this.loanAgreement.getRateAgreement().getNormalRate().getDaysPerMonth()
				+ this.intRepayPeriod.getPeriodQ() * 3 * this.loanAgreement.getRateAgreement().getNormalRate().getDaysPerMonth()
				+ this.intRepayPeriod.getPeriodM() * this.loanAgreement.getRateAgreement().getNormalRate().getDaysPerMonth()
				+ this.intRepayPeriod.getPeriodD();
	}
	/**
	 * 计算总期数
	 * @return
	 * @throws Exception
	 */
	public Integer calTotalPeriods()
	{
		Integer totalPeriods = 0;
		//2019-09-26 气球贷统计总期数
		if( ConstantDeclare.REPAYWAY.REPAYWAY_QQD.equals(loanAgreement.getWithdrawAgreement().getRepayWay()) )
		{
			String bDate = this.loanAgreement.getContract().getContractStartDate();
			while (CalendarUtil.actualDaysBetween(bDate, this.loanAgreement.getContract().getContractEndDate()) > 0) {
				totalPeriods += 1;
				bDate = RepayPeriodSupporter.calculateEndDate(bDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod);
			}
		}

		String sDate = this.loanAgreement.getContract().getRepayPrinDate();
		//等本等息总期数不变
		if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_EQPRINEQINT)||loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.RORMULA_WAYWARD)){
			sDate = this.loanAgreement.getContract().getContractStartDate();
		}
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
				sDate = RepayPeriodSupporter.calculateEndDateByFlag1(sDate,
						this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod);
			}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("8")){
				sDate = RepayPeriodSupporter.calculateEndDateByFlag8(sDate,
						this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod);
			}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1())
					&& this.loanAgreement.getContract().getFlag1().contains("A")
					&& this.loanAgreement.getBasicExtension().getLastPeriodStr().contains("1")
					&& CalendarUtil.before(sDate, this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[2])
					&& sDate.equals(this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[1])){
				String[]  lastPeriodStr = this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",");
				//季还本月还息最后一期展期单独处理
				if(this.loanAgreement.getWithdrawAgreement().getPeriodFormula().contains("QA")){
					sDate = RepayPeriodSupporter.calculateEndDate(sDate, this.loanAgreement.getContract().getContractStartDate(),
							this.loanAgreement.getContract().getContractEndDate(),
							this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
							this.prinRepayPeriod);
					// 如果晚于合同到期日期，则返回合同到日期
				}else if (Days.daysBetween(new DateTime(lastPeriodStr[2]),new DateTime(this.loanAgreement.getContract().getContractEndDate()) ).getDays() <= this.loanAgreement.getWithdrawAgreement().getPeriodMinDays()) {
					sDate = this.loanAgreement.getContract().getContractEndDate();
				}else{
					sDate = lastPeriodStr[2];
				}
				if(this.loanAgreement.getWithdrawAgreement().getPeriodFormula()
						.equals(getPrinPerformula(this.loanAgreement.getContract().getContractStartDate(),
								this.loanAgreement.getContract().getContractEndDate()))){
					sDate = this.loanAgreement.getContract().getContractEndDate();
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
//			if(currentPrinPeriod==null||currentPrinPeriod==1){
//				sDate = getEndDateStr(sDate,this.loanAgreement,periods);
//			}else{
//				sDate = getEndDateStr(sDate,this.loanAgreement,currentPrinPeriod);
//			}
			if(getCurrentPrinPeriodForTotalPeriods(loanAgreement)==1){
				sDate = getEndDateStr(sDate,this.loanAgreement,periods);
			}else{
				sDate = getEndDateStr(sDate,this.loanAgreement,null);
			}

			LoggerUtil.debug("第" + periods + "期 ，结束日期：" + sDate);
			if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
				sDate = CalendarUtil.nDaysAfter(sDate, 1).toString("yyyy-MM-dd");
			}
		}

		//2019-09-26 气球贷统计剩余期数
		if( ConstantDeclare.REPAYWAY.REPAYWAY_QQD.equals(loanAgreement.getWithdrawAgreement().getRepayWay()) )
		{
			periods = loanAgreement.getBasicExtension().getExpandPeriod() - totalPeriods+periods;
		}

		return  periods;
	}

	/**
	 * 获取当前本金的期数
	 * @param loanAgreement
	 * @return
	 */
	private  Integer getCurrentPrinPeriodForTotalPeriods(LoanAgreement loanAgreement){
		Integer firstPeriod=null;
		//等本等息 算总期数 一直不变 需要
		if(Arrays.asList(ConstantDeclare.REPAYWAY.REPAYWAY_DBDX,ConstantDeclare.REPAYWAY.REPAYWAY_WILLFUL).contains(loanAgreement.getWithdrawAgreement().getRepayWay())){
			return  1;
		}

		if(currentPrinPeriod==null){
			firstPeriod=loanAgreement.getContract().getCurrPrinPeriod();
		}else{
			firstPeriod=Math.min(currentPrinPeriod,loanAgreement.getContract().getCurrIntPeriod());
		}
		return firstPeriod;
	}

	/**
	 * 计算本金当前开始日期
	 * @return
	 * @throws Exception
	 */
	public String calPrinCurrPeriodStartDate()
	{
		String startDate = this.loanAgreement.getContract().getContractStartDate();

		//用结本日为开始日期和本金周期公式计算结束日期
		while(true)
		{
			String endDate = RepayPeriodSupporter.calculateEndDate(startDate,
					this.loanAgreement.getContract().getContractStartDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					this.loanAgreement.getInterestAgreement().getPeriodMinDays(),
					this.prinRepayPeriod);
			if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1())
					&& this.loanAgreement.getContract().getFlag1().contains("A")
					&& this.loanAgreement.getBasicExtension().getLastPeriodStr().contains("1")
					&& CalendarUtil.before(startDate, this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[2])){
				String[]  lastPeriodStr = this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",");
				if(CalendarUtil.after(endDate, lastPeriodStr[1])){
					//季还本月还息最后一期展期单独处理
					if(this.loanAgreement.getWithdrawAgreement().getPeriodFormula().contains("QA")){
						endDate = RepayPeriodSupporter.calculateEndDate(startDate, this.loanAgreement.getContract().getContractStartDate(),
								this.loanAgreement.getContract().getContractEndDate(),
								this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
								this.prinRepayPeriod);
						// 如果晚于合同到期日期，则返回合同到日期
					}else if (Days.daysBetween(new DateTime(lastPeriodStr[2]),new DateTime(this.loanAgreement.getContract().getContractEndDate()) ).getDays() <= this.loanAgreement.getWithdrawAgreement().getPeriodMinDays()) {
						endDate = this.loanAgreement.getContract().getContractEndDate();
					}else{
						endDate = lastPeriodStr[2];
					}
					if(this.loanAgreement.getWithdrawAgreement().getPeriodFormula()
							.equals(getPrinPerformula(this.loanAgreement.getContract().getContractStartDate(),
									this.loanAgreement.getContract().getContractEndDate()))){
						endDate = this.loanAgreement.getContract().getContractEndDate();
					}
				}

			}
			if(!this.loanAgreement.getWithdrawAgreement().getFirstTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getLastTermMerge()){
				endDate = RepayPeriodSupporter.calculateEndDateBySetting(startDate,
						this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getInterestAgreement().getPeriodMinDays(),
						this.prinRepayPeriod,loanAgreement.getWithdrawAgreement(),true);
			}
			if (CalendarUtil.after(endDate, this.loanAgreement.getContract().getRepayPrinDate())
					|| CalendarUtil.equalDate(this.loanAgreement.getContract().getRepayPrinDate(), this.loanAgreement.getContract().getContractEndDate()))
			{
				break;
			}
			else
			{

				startDate = endDate;
			}
		}

		return startDate;
	}

	/**
	 * 计算本金结束日期
	 * @return
	 * @throws Exception
	 */
	public String calPrinCurrPeriodEndDate()
	{
		//mod 20180712
		if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
			prinCurrPeriodEndDate = RepayPeriodSupporter.calculateP2PEndDate(this.loanAgreement.getContract().getRepayPrinDate(),
					this.loanAgreement.getContract().getContractStartDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
					this.prinRepayPeriod);
		}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("1")){
			prinCurrPeriodEndDate = RepayPeriodSupporter.calculateEndDateByFlag1(this.loanAgreement.getContract().getRepayPrinDate(),
					this.loanAgreement.getContract().getContractStartDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
					this.prinRepayPeriod);
		}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("8")){
			prinCurrPeriodEndDate = RepayPeriodSupporter.calculateEndDateByFlag8(this.loanAgreement.getContract().getRepayPrinDate(),
					this.loanAgreement.getContract().getContractStartDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
					this.prinRepayPeriod);
		}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1())
				&& this.loanAgreement.getContract().getFlag1().contains("A")
				&& this.loanAgreement.getBasicExtension().getLastPeriodStr().contains("1")
				&& CalendarUtil.before(this.loanAgreement.getContract().getRepayPrinDate(), this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[2])
				&& this.loanAgreement.getContract().getRepayPrinDate().equals(this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[1])){
			String[]  lastPeriodStr = this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",");
			//季还本月还息最后一期展期单独处理
			if(this.loanAgreement.getWithdrawAgreement().getPeriodFormula().contains("QA")){
				prinCurrPeriodEndDate = RepayPeriodSupporter.calculateEndDate(this.loanAgreement.getContract().getRepayPrinDate(),
						this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.prinRepayPeriod);
			}
			else if(this.loanAgreement.getWithdrawAgreement().getPeriodFormula()
					.equals(getPrinPerformula(this.loanAgreement.getContract().getContractStartDate(),
							this.loanAgreement.getContract().getContractEndDate()))){
				prinCurrPeriodEndDate = this.loanAgreement.getContract().getContractEndDate();
			}
			// 如果晚于合同到期日期，则返回合同到日期
			else if (Days.daysBetween(new DateTime(lastPeriodStr[2]),new DateTime(this.loanAgreement.getContract().getContractEndDate()) ).getDays() <= this.loanAgreement.getWithdrawAgreement().getPeriodMinDays()) {
				prinCurrPeriodEndDate = this.loanAgreement.getContract().getContractEndDate();
			}else{
				prinCurrPeriodEndDate = lastPeriodStr[2];
			}
		}else if(!this.loanAgreement.getWithdrawAgreement().getFirstTermMonth()
				|| !this.loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
				|| !this.loanAgreement.getWithdrawAgreement().getLastTermMerge()){
			prinCurrPeriodEndDate =  RepayPeriodSupporter.calculateEndDateBySetting(this.loanAgreement.getContract().getRepayPrinDate(),
					this.loanAgreement.getContract().getContractStartDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
					this.prinRepayPeriod,
					this.loanAgreement.getWithdrawAgreement(),true);
		}else{
			prinCurrPeriodEndDate =  RepayPeriodSupporter.calculateEndDate(this.loanAgreement.getContract().getRepayPrinDate(),
					this.loanAgreement.getContract().getContractStartDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
					this.prinRepayPeriod);
		}
		//到期日  工作日延期 处理
		prinCurrPeriodEndDate = getEndDateStr(prinCurrPeriodEndDate,this.loanAgreement,currentPrinPeriod);
		return prinCurrPeriodEndDate;
	}

	/**
	 * 计算本金当前还款日  2018-01-20增加
	 * @return
	 */
	public String calPrinCurrPeriodCurrentDate()
	{
		return prinCurrPeriodEndDate== null ? this.calPrinCurrPeriodEndDate() :prinCurrPeriodEndDate;
	}



	/**
	 * 计算利息结束日期：以结本日作为开始日期，用利息周期公式计算结束日期，直到晚于贷款协议中的结息日
	 * @return
	 * @throws Exception
	 * @throws SecurityException
	 */
	public String calIntCurrPeriodEndDate()
	{
		String endDate = this.loanAgreement.getContract().getRepayPrinDate();

		//用结本日为开始日期和利息周期公式计算结束日期，直到结束日期晚于结息日
		while(true)
		{
			endDate = caculateEndDate(endDate,this.loanAgreement.getInterestAgreement().getPeriodMinDays(),
					this.intRepayPeriod);
			if (CalendarUtil.after(endDate, this.loanAgreement.getContract().getRepayIntDate())
					|| CalendarUtil.equalDate(this.loanAgreement.getContract().getRepayIntDate(), this.loanAgreement.getContract().getContractEndDate()))
			{
				break;
			}

		}

		String prinEndDate = prinCurrPeriodEndDate== null ? this.calPrinCurrPeriodEndDate() :prinCurrPeriodEndDate;
		if (CalendarUtil.after(endDate,prinEndDate))
			endDate = prinEndDate;

		return endDate;
	}
	/**
	 * 计算利息结束日期：以结本日作为开始日期，用利息周期公式计算结束日期，直到晚于贷款协议中的结息日
	 * @return
	 * @throws Exception
	 * @throws SecurityException
	 */
	public String calFeeCurrPeriodEndDate(String preEndDate,String Feeformula) throws FabException
	{

		String endDate ;
		//用结本日为开始日期和利息周期公式计算结束日期，直到结束日期晚于结息日
		RepayPeriod feeRepayPeriod =  RepayPeriodSupporter.genRepayPeriod(Feeformula);
		while(true)
		{
			//费用暂时用利息的公式计算
			endDate = caculateEndDate(preEndDate,this.loanAgreement.getInterestAgreement().getPeriodMinDays(),
					feeRepayPeriod);
			if (CalendarUtil.after(endDate, preEndDate)
					|| CalendarUtil.equalDate(preEndDate, this.loanAgreement.getContract().getContractEndDate()))
			{
				break;
			}

		}

		return endDate;
	}
	private String caculateEndDate(String endDate,Integer minDays,RepayPeriod repayPeriod) {
		if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
			endDate = RepayPeriodSupporter.calculateP2PEndDate(endDate,
					this.loanAgreement.getContract().getStartIntDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					minDays,repayPeriod);
		}
		else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("1")){
			endDate = RepayPeriodSupporter.calculateEndDateByFlag1(endDate,
					this.loanAgreement.getContract().getStartIntDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					minDays,repayPeriod);
		}
		else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("8")){
			endDate = RepayPeriodSupporter.calculateEndDateByFlag8(endDate,
					this.loanAgreement.getContract().getStartIntDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					minDays,repayPeriod);
		}
		else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1())
				&& this.loanAgreement.getContract().getFlag1().contains("A")
				&& this.loanAgreement.getBasicExtension().getLastPeriodStr().contains("1")
				&& CalendarUtil.before(endDate, this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[2])
				&& endDate.equals(this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[1])){
			String[]  lastPeriodStr = this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",");
			// 如果晚于合同到期日期，则返回合同到日期
			if (Days.daysBetween(new DateTime(lastPeriodStr[2]),new DateTime(this.loanAgreement.getContract().getContractEndDate()) ).getDays() <= this.loanAgreement.getInterestAgreement().getPeriodMinDays()) {
				endDate = this.loanAgreement.getContract().getContractEndDate();
			}else{
				endDate = lastPeriodStr[2];
			}

		}else if(!this.loanAgreement.getWithdrawAgreement().getFirstTermMonth()
				|| !this.loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
				|| !this.loanAgreement.getWithdrawAgreement().getLastTermMerge()){
			endDate = RepayPeriodSupporter.calculateEndDateBySetting(endDate,
					this.loanAgreement.getContract().getStartIntDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					minDays,repayPeriod,
					this.loanAgreement.getWithdrawAgreement(),false);
		}
		else{
			endDate = RepayPeriodSupporter.calculateEndDate(endDate,
					this.loanAgreement.getContract().getStartIntDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					minDays,repayPeriod);
		}
		endDate = getEndDateStr(endDate,this.loanAgreement,currentPrinPeriod);
		return endDate;
	}

	/**
	 * 计算当期利息实际还款日    2018-01-20增加
	 * @return
	 */
	public String calIntCurrPeriodCurrentDate()
	{
		//先本后息当期期数取最后一期
		if(ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(loanAgreement.getWithdrawAgreement().getRepayWay()))
			return loanAgreement.getContract().getContractEndDate();

		return this.calIntCurrPeriodEndDate();
	}


	/**
	 * 计算利息当期开始日期
	 * @return
	 * @throws Exception
	 */
	public String calIntCurrPeriodStartDate()
	{
		String startDate = this.loanAgreement.getContract().getRepayPrinDate();

		//用结本日为开始日期和利息周期公式计算结束日期
		while(true)
		{
			String endDate = RepayPeriodSupporter.calculateEndDate(startDate,
					this.loanAgreement.getContract().getStartIntDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					this.loanAgreement.getInterestAgreement().getPeriodMinDays(),
					this.intRepayPeriod);
			if(!this.loanAgreement.getWithdrawAgreement().getFirstTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getLastTermMerge()){
				endDate = RepayPeriodSupporter.calculateEndDateBySetting(startDate,
						this.loanAgreement.getContract().getStartIntDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getInterestAgreement().getPeriodMinDays(),
						this.intRepayPeriod,loanAgreement.getWithdrawAgreement(),false);
			}
			//提前还部分利息 利息计算错误修复
			endDate = getEndDateStr(endDate, loanAgreement,currentPrinPeriod);
			if (CalendarUtil.after(endDate, this.loanAgreement.getContract().getRepayIntDate())
					|| CalendarUtil.equalDate(this.loanAgreement.getContract().getRepayIntDate(), this.loanAgreement.getContract().getContractEndDate()))
			{
				break;
			}
			else
			{

				startDate = endDate;
			}
		}


		//考虑倒起息场景，如果开始日期等于合同开始日期返回起息日
		if (CalendarUtil.equalDate(startDate, this.loanAgreement.getContract().getContractStartDate()))
		{
			return this.loanAgreement.getContract().getStartIntDate();
		}

		return startDate;
	}

	/**
	 * 指定每期最小还本金额
	 * @param amt
	 * @return
	 */
	public FabAmount optMinRepayPrinAmt(FabAmount amt)
	{
		if (amt.getVal() <Double.parseDouble("0.01"))
			return new FabAmount(Double.parseDouble("0.01"));
		return amt;
	}

	public FabAmount optScmMinRepayPrinAmt(FabAmount amt)
	{
		if (amt.getVal() <Double.parseDouble(GlobalScmConfUtil.getProperty("minimumPayment","1.00")))
			return new FabAmount(Double.parseDouble(GlobalScmConfUtil.getProperty("minimumPayment","1.00")));
		return amt;
	}

	//获取当前费用
	//有两种计费方式 0-按日计费  1-按期计费
	public FabAmount getCurrFee(LnsBill feeBill, TblLnsfeeinfo lnsfeeinfo, LoanAgreement la)
	{
		FabAmount fee =  new FabAmount();

		if(ConstantDeclare.CALCULATRULE.BYTERM.equals(lnsfeeinfo.getCalculatrule())) {
			//计费基数*年费率/月份
			if(lnsfeeinfo.getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ONETIME))
				//一次性费率
				fee.selfAdd(BigDecimal.valueOf(feeBill.getPrinBal().getVal()).multiply(BigDecimal.valueOf(lnsfeeinfo.getFeerate())).setScale(2, RoundingMode.HALF_UP ).doubleValue());
			else
				fee.selfAdd(BigDecimal.valueOf(feeBill.getPrinBal().getVal()).multiply(BigDecimal.valueOf(lnsfeeinfo.getFeerate())).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP ).doubleValue());

		}else
		{
			if(lnsfeeinfo.getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ONETIME)){
				//天数
				BigDecimal intDec = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(la.getContract().getContractStartDate(), la.getContract().getContractEndDate()));
				//日利息
				BigDecimal dayInt = BigDecimal.valueOf(new FabRate(lnsfeeinfo.getFeerate()).getDayRate().doubleValue());
				//趸交费用
				fee.selfAdd(BigDecimal.valueOf(feeBill.getPrinBal().getVal()).multiply(dayInt).multiply(intDec).setScale(2, RoundingMode.HALF_UP ).doubleValue());
			}
			else {
				String periodIntStartDate ;
				//非标费用按周期计算
				if(ConstantDeclare.FEEREPAYWAY.NONESTATIC.equals(lnsfeeinfo.getRepayway())){
					periodIntStartDate =feeBill.getStartDate() ;
				}else{
					periodIntStartDate = calFeeCurrPeriodStartDate(lnsfeeinfo);
				}

				//计费基数*年费率*期限天数/360天
				fee.selfAdd(BigDecimal.valueOf(feeBill.getPrinBal().getVal()).multiply(BigDecimal.valueOf(lnsfeeinfo.getFeerate()))
						.multiply(new BigDecimal(Integer.toString(CalendarUtil.actualDaysBetween(periodIntStartDate,feeBill.getEndDate())) )).divide(new BigDecimal("360"), 2, RoundingMode.HALF_UP ).doubleValue());

				//计算每天利息并四舍五入
				BigDecimal overInterest = BigDecimal.valueOf(feeBill.getPrinBal().getVal())
						.multiply(BigDecimal.valueOf(lnsfeeinfo.getFeerate()))
						.divide(new BigDecimal("360"), 2, RoundingMode.HALF_UP )
						.multiply(new BigDecimal(CalendarUtil.actualDaysBetween(periodIntStartDate, feeBill.getStartDate())));

				//总费用减去已结费用
				fee.selfSub(overInterest.doubleValue());
			}
		}
		return fee;
	}

	/**
	 * 计算费用当期开始日期
	 * @return
	 * @throws Exception
	 */
	public String calFeeCurrPeriodStartDate(TblLnsfeeinfo lnsfeeinfo)
	{
		String startDate = this.loanAgreement.getContract().getRepayPrinDate();

		//用结本日为开始日期和利息周期公式计算结束日期
		while(true)
		{
			String endDate = RepayPeriodSupporter.calculateEndDate(startDate,
					this.loanAgreement.getContract().getStartIntDate(),
					this.loanAgreement.getContract().getContractEndDate(),
					this.loanAgreement.getInterestAgreement().getPeriodMinDays(),
					this.intRepayPeriod);
			if(!this.loanAgreement.getWithdrawAgreement().getFirstTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getLastTermMerge()){
				endDate = RepayPeriodSupporter.calculateEndDateBySetting(startDate,
						this.loanAgreement.getContract().getStartIntDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getInterestAgreement().getPeriodMinDays(),
						this.intRepayPeriod,loanAgreement.getWithdrawAgreement(),false);
			}
			//提前还部分利息 利息计算错误修复
			endDate = getEndDateStr(endDate, loanAgreement,currentPrinPeriod);
			if (CalendarUtil.after(endDate, lnsfeeinfo.getLastfeedate())
					|| CalendarUtil.equalDate(lnsfeeinfo.getLastfeedate(), this.loanAgreement.getContract().getContractEndDate()))
			{
				break;
			}
			else
			{

				startDate = endDate;
			}
		}


		//考虑倒起息场景，如果开始日期等于合同开始日期返回起息日
		if (CalendarUtil.equalDate(startDate, this.loanAgreement.getContract().getContractStartDate()))
		{
			return this.loanAgreement.getContract().getStartIntDate();
		}

		return startDate;
	}
	public FabAmount getRepayDateFee(LnsBill feeBill, TblLnsfeeinfo lnsfeeinfo,String repayDate) {

		//未来期账单计提金额为空
		if (CalendarUtil.beforeAlsoEqual(repayDate, feeBill.getStartDate()) )
		{
			return null;
		}

		//未结息费用账单，计提金额等于当前费用
		if (CalendarUtil.afterAlsoEqual(repayDate, feeBill.getEndDate()))
		{
			return feeBill.getBillAmt();
		}
		//按期计费
		if(ConstantDeclare.CALCULATRULE.BYTERM.equals(lnsfeeinfo.getCalculatrule()) ||
		   ConstantDeclare.EARLYSETTLRFLAG.CURRCHARGE.equals(lnsfeeinfo.getAdvancesettle())	) {
			return feeBill.getBillAmt();
			//按日计费
		}else if(ConstantDeclare.CALCULATRULE.BYDAY.equals(lnsfeeinfo.getCalculatrule()))
		{
			//计费基数*年费率*期限天数/360天
			 FabAmount feeAmount = new FabAmount(BigDecimal.valueOf(feeBill.getPrinBal().getVal()).multiply(BigDecimal.valueOf(lnsfeeinfo.getFeerate()))
					.divide(new BigDecimal("360"), 2, RoundingMode.HALF_UP ).multiply(new BigDecimal(Integer.toString(CalendarUtil.actualDaysBetween(feeBill.getStartDate(),repayDate)) )).doubleValue());
			//按日计费费用不能大于总费用
			 return feeBill.getBillAmt().sub(feeAmount).isPositive()?feeAmount:feeBill.getBillAmt();
		}
		return null;
	}

	private static String getPrinPerformula(String begin,String end){
		DateTime b = new DateTime(begin);
		DateTime e = new DateTime(end);

		//计算区间天数
		Period p = new Period(b, e, PeriodType.yearMonthDay());
		String result = "";
		if(p.getYears() != 0 && (p.getMonths() != 0 || p.getDays() != 0)){
			result += p.getYears()+"Y";
		}else if(p.getMonths() == 0 && p.getDays() == 0){
			result += p.getYears()+"YA";
		}
		if(p.getMonths() != 0 && p.getDays() != 0){
			result += p.getMonths()+"M";
		}else if(p.getMonths() != 0 && p.getDays() == 0){
			result += p.getMonths()+"MA";
		}
		if(p.getDays() != 0){
			result += p.getDays()+"DA";
		}
		return result;
	}

	//根据试算期数 和 要求期数 调整到期日
	public String calculateEndDateByPeriodNum(Integer periodNum){
		int diff = periodNum - totalIntPeriods;
		if (periodNum==0) {
			return loanAgreement.getContract().getContractEndDate();
		}else if(periodNum>0){
			return new DateTime(loanAgreement.getContract().getContractEndDate()).plusMonths(diff).toString("yyyy-MM-dd");
		}else{
			return new DateTime(loanAgreement.getContract().getContractEndDate()).minusMonths(-diff).toString("yyyy-MM-dd");
		}
	}

	//调整到期日

	//到期日 按工作日延期
	private  String getEndDateStr(String endDate,LoanAgreement loanAgreement,Integer currentPeriod){
//		//宽限期
//		if(loanAgreement.getContract().getGraceDays()!=null){
//			endDate = CalendarUtil.nDaysAfter(endDate, loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd");
//		}
		//延期还款
		if(!VarChecker.isEmpty(loanAgreement.getBasicExtension().getTermEndDate())){
			endDate = iterationEndDate(endDate, loanAgreement);
		}

		//首期还款日不为空  并且是首期的试算
		if(!VarChecker.isEmpty(loanAgreement.getBasicExtension().getFirstRepayDate())&&!VarChecker.isEmpty(currentPeriod)&&(currentPeriod==1)){
			endDate=loanAgreement.getBasicExtension().getFirstRepayDate();
		}

		//到期日是否按照工作日延期
		//合同到期日不会延期
//		if(!loanAgreement.getWithdrawAgreement().getEndDateDelay()||loanAgreement.getContract().getContractEndDate().equals(endDate))
//			return endDate;
//		//工作日改假日
//		while (holidays.contains(endDate)){
//			//向后顺延一天
//			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
//		}
//
//		//是周末 并且不是周末改工作日
//		while (isWeekend(endDate)&& !workDays.contains(endDate)){
//			//向后顺延一天
//			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
//		}
//
		return endDate;
	}
	//迭代到期日
	private static String iterationEndDate(String endDate, LoanAgreement loanAgreement) {
		for(Map.Entry<String,Object> entry:loanAgreement.getBasicExtension().getLastEnddates().entrySet()){
			if(CalendarUtil.before(entry.getKey(), endDate)
					&&CalendarUtil.after(entry.getValue().toString(),endDate )){
				return iterationEndDate(entry.getValue().toString(),loanAgreement);
			}
		}
		return endDate;
	}

	/**
	 * @description: 判断日期字符串是否为周末
	 */
	public static boolean isWeekend(String dateStr) {
		boolean isWeekend = false;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date date = sdf.parse(dateStr);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			isWeekend = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
		} catch (ParseException e) {
			LoggerUtil.error(e.getMessage());
		}
		return isWeekend;
	}

	
	
	/**
	 * 计算利息总期数
	 * @return
	 * @throws Exception
	 */
	public Integer calTotalIntPeriods()
	{
//		Integer totalPeriods = 0;
//		//2019-09-26 气球贷统计总期数
//		if( ConstantDeclare.REPAYWAY.REPAYWAY_QQD.equals(loanAgreement.getWithdrawAgreement().getRepayWay()) )
//		{
//			String bDate = this.loanAgreement.getContract().getContractStartDate();
//			while (CalendarUtil.actualDaysBetween(bDate, this.loanAgreement.getContract().getContractEndDate()) > 0) {
//				totalPeriods += 1;
//				bDate = RepayPeriodSupporter.calculateEndDate(bDate, this.loanAgreement.getContract().getContractStartDate(),
//						this.loanAgreement.getContract().getContractEndDate(),
//						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
//						this.intRepayPeriod);
//			}
//		}

		String sDate = this.loanAgreement.getContract().getRepayPrinDate();
		//等本等息总期数不变
		if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_EQPRINEQINT)||loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.RORMULA_WAYWARD)){
			sDate = this.loanAgreement.getContract().getContractStartDate();
		}
		Integer periods = 0;
		while (CalendarUtil.actualDaysBetween(sDate, this.loanAgreement.getContract().getContractEndDate()) > 0) {
			periods += 1;
			LoggerUtil.debug("第" + periods + "期 ，  开始日期：" + sDate);
			if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
				sDate = RepayPeriodSupporter.calculateP2PEndDate(sDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.intRepayPeriod);
			}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("1")){
				sDate = RepayPeriodSupporter.calculateEndDateByFlag1(sDate,
						this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.intRepayPeriod);
			}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1()) && this.loanAgreement.getContract().getFlag1().contains("8")){
				sDate = RepayPeriodSupporter.calculateEndDateByFlag8(sDate,
						this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.intRepayPeriod);
			}else if(!VarChecker.isEmpty(this.loanAgreement.getContract().getFlag1())
					&& this.loanAgreement.getContract().getFlag1().contains("A")
					&& this.loanAgreement.getBasicExtension().getLastPeriodStr().contains("1")
					&& CalendarUtil.before(sDate, this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[2])
					&& sDate.equals(this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",")[1])){
				String[]  lastPeriodStr = this.loanAgreement.getBasicExtension().getLastPeriodStr().split(",");
				//季还本月还息最后一期展期单独处理
				if(this.loanAgreement.getWithdrawAgreement().getPeriodFormula().contains("QA")){
					sDate = RepayPeriodSupporter.calculateEndDate(sDate, this.loanAgreement.getContract().getContractStartDate(),
							this.loanAgreement.getContract().getContractEndDate(),
							this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
							this.intRepayPeriod);
					// 如果晚于合同到期日期，则返回合同到日期
				}else if (Days.daysBetween(new DateTime(lastPeriodStr[2]),new DateTime(this.loanAgreement.getContract().getContractEndDate()) ).getDays() <= this.loanAgreement.getWithdrawAgreement().getPeriodMinDays()) {
					sDate = this.loanAgreement.getContract().getContractEndDate();
				}else{
					sDate = lastPeriodStr[2];
				}
				if(this.loanAgreement.getWithdrawAgreement().getPeriodFormula()
						.equals(getPrinPerformula(this.loanAgreement.getContract().getContractStartDate(),
								this.loanAgreement.getContract().getContractEndDate()))){
					sDate = this.loanAgreement.getContract().getContractEndDate();
				}

			}else if(!this.loanAgreement.getWithdrawAgreement().getFirstTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
					|| !this.loanAgreement.getWithdrawAgreement().getLastTermMerge()){
				sDate = RepayPeriodSupporter.calculateEndDateBySetting(sDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.intRepayPeriod,
						this.loanAgreement.getWithdrawAgreement(),true);
			}else{
				sDate = RepayPeriodSupporter.calculateEndDate(sDate, this.loanAgreement.getContract().getContractStartDate(),
						this.loanAgreement.getContract().getContractEndDate(),
						this.loanAgreement.getWithdrawAgreement().getPeriodMinDays(),
						this.intRepayPeriod);
			}

//			if(currentPrinPeriod==null||currentPrinPeriod==1){
//				sDate = getEndDateStr(sDate,this.loanAgreement,periods);
//			}else{
//				sDate = getEndDateStr(sDate,this.loanAgreement,currentPrinPeriod);
//			}

			if(getCurrentPrinPeriodForTotalPeriods(loanAgreement)==1){
				sDate = getEndDateStr(sDate,this.loanAgreement,periods);
			}else{
				sDate = getEndDateStr(sDate,this.loanAgreement,null);
			}

			LoggerUtil.debug("第" + periods + "期 ，结束日期：" + sDate);
			if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
				sDate = CalendarUtil.nDaysAfter(sDate, 1).toString("yyyy-MM-dd");
			}
		}

//		//2019-09-26 气球贷统计剩余期数
//		if( ConstantDeclare.REPAYWAY.REPAYWAY_QQD.equals(loanAgreement.getWithdrawAgreement().getRepayWay()) )
//		{
//			periods = loanAgreement.getBasicExtension().getExpandPeriod() - totalPeriods+periods;
//		}

		return  periods;
	}

	/**
	 * @return the totalIntPeriods
	 */
	public Integer getTotalIntPeriods() {
		return totalIntPeriods;
	}

	/**
	 * @param totalIntPeriods the totalIntPeriods to set
	 */
	public void setTotalIntPeriods(Integer totalIntPeriods) {
		this.totalIntPeriods = totalIntPeriods;
	}

}  
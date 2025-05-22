package com.suning.fab.loan.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import com.suning.fab.loan.bo.RepayPeriod;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.la.WithdrawAgreement;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;

public class RepayPeriodSupporter {
	private RepayPeriodSupporter(){
		//nothing to do
	}

	
	
	/**
	 * 利息还款周期列表
	 */
	private static ThreadLocal<Map<String, List<String>>> intstack = new ThreadLocal<Map<String, List<String>>>();

	/**
	 * 本金还款周期列表
	 */
	private static ThreadLocal<Map<String, List<String>>> prinstack = new ThreadLocal<Map<String, List<String>>>();
	/**
	 * 序号
	 */
	private static ThreadLocal<Map<Integer, Integer>> subnostack = new ThreadLocal<Map<Integer, Integer>>();
	/**
	 * 将还款公式字符串解析为还款周期类
	 * @param formula
	 * @return
	 * @throws FabException
	 * @throws Exception
	 */
	public static RepayPeriod genRepayPeriod(String formula) throws FabException {

		String character = "[A-Za-z]";
		String digit = "[0-9]{1,}";
		
		String regEx = "(\\d+[YQHMXD]){1,}A(\\d+[YQHMXD]){0,}([0-9][0-9]){0,2}";
		// 编译正则表达式
		Pattern pattern = Pattern.compile(regEx);
		// 忽略大小写的写法
		Matcher matcher = pattern.matcher(formula);
		// 字符串是否与正则表达式相匹配
		if (!matcher.matches()) {
			// 周期公式非法
			throw new FabException("LNS007", formula);
		}

		Class<RepayPeriod> clz = RepayPeriod.class;
		RepayPeriod repayPeriod;
		try {
			repayPeriod = clz.newInstance();
		} catch (Exception e) {
			throw new FabException(e,ConstantDeclare.RSPCODE.INNERERR, formula);
		}
		String periodStr = formula.split("A")[0];

		String ps = periodStr.replaceAll(character, "$0,");
		String pa[] = ps.split(",");

		Integer days = 0;
			for (String period : pa) {
				Integer n = Integer.valueOf(period.split(character)[0]);
				String unit = period.split(digit)[1];
				Method mset;
				mset = MethodUtil.getMethod(RepayPeriod.class,"setPeriod" + unit, Integer.class);
				Method mget = MethodUtil.getMethod(RepayPeriod.class,"getPeriod" + unit);
				
				MethodUtil.methodInvoke(repayPeriod, mset, (Integer)MethodUtil.methodInvoke(repayPeriod, mget) + n);
	
				if (!("X".equals(unit) || "D".equals(unit))) {
					days += 30;
				}
	
				if ("X".equals(unit)) {
					days += n * 10;
				}
				if ("D".equals(unit)) {
					days += n;
				}
			}
		
			if (formula.split("A").length == 2) {
				String timesStr = formula.split("A")[1];
	
				// 解析周期
				String ts = timesStr.replaceAll(character, "$0,");
	
				String ta[] = ts.split(",");
				for (String times : ta) {
					Pattern p = Pattern.compile(digit);
					Matcher mc = p.matcher(times);
					if (!mc.matches()) {
	
						Integer n = Integer.valueOf(times.split(character)[0]);
	
						String unit = times.split(digit)[1];
						Method mset = MethodUtil.getMethod(RepayPeriod.class,"setTimes" + unit,Integer.class);
						Method mget = MethodUtil.getMethod(RepayPeriod.class,"getTimes" + unit);
						MethodUtil.methodInvoke(repayPeriod, mset,  (Integer)MethodUtil.methodInvoke(repayPeriod, mget) + n);
						
					}
					// 纯数字则认为定时月日
					else {
						// 日
						if (times.length() == 2) {
							Method m = MethodUtil.getMethod(RepayPeriod.class, "setOptDay", Integer.class); 
							
							MethodUtil.methodInvoke(repayPeriod, m, Integer.valueOf(times));
						}
						// 月日
						else {
							Method m = MethodUtil.getMethod(RepayPeriod.class, "setOptMonth", Integer.class); 
							MethodUtil.methodInvoke(repayPeriod, m, Integer.valueOf(times.substring(0, 2)));
							m = MethodUtil.getMethod(RepayPeriod.class, "setOptDay", Integer.class); 
							MethodUtil.methodInvoke(repayPeriod, m, Integer.valueOf(times.substring(2, 4)));
						}
	
					}
				}
			}
		// 还款周期天数小于30并且指定还款日期时报公式非法
		if (days < 30 && repayPeriod.getOptDay() > 0) {
			// 周期公式非法
			throw new FabException("LNS007", formula);
		}

		return repayPeriod;

	}

	/**
	 * 将还款周期转化为还款公式字符串
	 * @param repayPeriod
	 * @return
	 * @throws Exception
	 * @throws InvocationTargetException
	 */
	public static String combinationPeriodFormula(RepayPeriod repayPeriod)
			throws FabException{
		StringBuilder pf = new StringBuilder(32);
		StringBuilder tf = new StringBuilder(32);
		Class<RepayPeriod> c = RepayPeriod.class;
		Method[] methods = c.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().contains("getPeriod")
					&& (Integer)MethodUtil.methodInvoke(repayPeriod, methods[i]) != 0) {

				pf.append(MethodUtil.methodInvoke(repayPeriod, methods[i]).toString()).append(
						methods[i].getName().substring(
								methods[i].getName().length() - 1));
			}
			if (methods[i].getName().contains("getTimes")
					&& (Integer) MethodUtil.methodInvoke(repayPeriod, methods[i]) != 0) {
				tf.append(MethodUtil.methodInvoke(repayPeriod, methods[i]).toString()).append(
						methods[i].getName().substring(
								methods[i].getName().length() - 1));
			}
		}

		if (repayPeriod.getOptMonth() != 0) {
			tf.append(String.format("%02d", repayPeriod.getOptMonth()));
		}
		if (repayPeriod.getOptDay() != 0) {
			tf.append(String.format("%02d", repayPeriod.getOptDay()));
		}
		return pf.append("A").append(tf).toString();
	}
	
	/**
	 * 根据开始日期结算结束日期
	 * @param startDate
	 * @param contractStartDate
	 * @param contractEndDate
	 * @param minDays
	 * @param repayPeriod
	 * @return
	 */
	public static String calculateEndDate(String startDate,
			String contractStartDate, String contractEndDate, Integer minDays,
			RepayPeriod repayPeriod) {
		DateTime b = new DateTime(startDate);
		DateTime cst = new DateTime(contractStartDate);
		DateTime cet = new DateTime(contractEndDate);

		Integer periodMs = repayPeriod.getPeriodY() * 12
				+ repayPeriod.getPeriodH() * 6 + repayPeriod.getPeriodQ() * 3
				+ repayPeriod.getPeriodM() + repayPeriod.getPeriodX() / 3
				+ repayPeriod.getPeriodD() / 30;
		Integer periodDs = repayPeriod.getPeriodX() % 3 * 10
				+ repayPeriod.getPeriodD() % 30;

		Integer timesDs = repayPeriod.getTimesY() * 360
				+ repayPeriod.getTimesH() * 180 + repayPeriod.getTimesQ() * 120
				+ repayPeriod.getTimesM() * 30 + repayPeriod.getTimesX() * 10
				+ repayPeriod.getTimesD();

		DateTime firstEndDate = new DateTime(contractStartDate);
		// 计算根据合同日期计算第一个还款日
		if (timesDs > 0) {

			if (repayPeriod.getTimesY() > 0) {
				firstEndDate = cst.plusYears(repayPeriod.getTimesY() - 1);
			} else if (repayPeriod.getTimesH() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesH() * 6 - 6);
			} else if (repayPeriod.getTimesQ() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesQ() * 3 - 3);
			} else if (repayPeriod.getTimesM() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesM() - 1);
			} else if (repayPeriod.getTimesX() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesX() * 10 - 10);
			} else if (repayPeriod.getTimesD() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesD() - 1);
			}
		} else {
			firstEndDate = cst.plusMonths(periodMs).plusDays(periodDs);
		}
		//指定月
		if (repayPeriod.getOptMonth() > 0) {
			firstEndDate = CalendarUtil.optMonth(firstEndDate,repayPeriod.getOptMonth());
		}

		DateTime edt = firstEndDate;
		// 开始日期小于第一个结束日期返回第一个结束日期
		if (Days.daysBetween(b, firstEndDate).getDays() < 0) {
			// 周期中月数大于0，用年月日方式计算，否则，用天数计算
			if (periodMs > 0) {
				Period p = new Period(firstEndDate, b,
						PeriodType.yearMonthDay());

				Integer diffMoths = p.getYears() * 12 + p.getMonths();
				Integer diffDays = p.getDays();

					edt = firstEndDate.plusMonths((diffMoths / periodMs + 1)
							* periodMs);
				if (periodDs != 0) {
					edt = edt.plusDays((diffDays / periodDs + 1) * periodDs);
				}
			} else {
				Period p = new Period(firstEndDate, b, PeriodType.days());
				Integer diffDays = p.getDays();
				if (periodDs != 0) {
					edt = firstEndDate.plusDays((diffDays / periodDs + 1)
							* periodDs);
				}
			}
		}

		if (edt.getYear()>b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			edt = CalendarUtil.optMonth(edt,repayPeriod.getOptMonth());
		}
		
		if (edt.getYear()==b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			
			edt = CalendarUtil.optMonthPhase(edt,periodMs,repayPeriod.getOptMonth());
		}
		
		
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		//小于周期最小天数加一期
		if (Days.daysBetween(b, edt).getDays() <= minDays) {
			edt = edt.plusMonths(periodMs).plusDays(periodDs);
		}
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		// 如果晚于合同到期日期，则返回合同到日期
		if (Days.daysBetween(edt, cet).getDays() <= minDays) {
			return contractEndDate;
		}

		return edt.toString("yyyy-MM-dd");

	}

	

	/**
	 * 根据开始日期获取结束日期
	 * 
	 * @param startDate
	 * @param dateList
	 * @param minDays
	 * @return
	 */
	public static String getEndDate(String startDate, List<String> dateList,
			Integer minDays) {

		Integer i;
		for (i = 0; i < dateList.size(); i++) {

			if (CalendarUtil.before(startDate, dateList.get(i))) {
				break;
			}
		}
		if (CalendarUtil.actualDaysBetween(startDate, dateList.get(i)) < minDays) {
			return dateList.get(++i == dateList.size() ? --i : i);

		}
		return dateList.get(i);

	}

	/**
	 * 
	 * @param repayDate
	 * @param contractStartDate
	 * @param contractEndDate
	 * @param periodFormula
	 * @param minDays
	 * @return
	 * @throws Exception
	 */
	public static String calculateStartDate(String repayDate,
			String contractStartDate, String contractEndDate,
			Integer minDays,RepayPeriod repayPeriod) {
		String sDate = contractStartDate;
		String dt = contractStartDate;
		while (CalendarUtil.actualDaysBetween(sDate, contractEndDate) > 0) {
			sDate = calculateEndDate(sDate, contractStartDate, contractEndDate,
					minDays, repayPeriod);
			if (CalendarUtil.before(repayDate, sDate))
			{
				return dt;
			}
			else
			{
				dt = sDate;
			}
			
		}
		return null;
	}
	
	/**
	 * 根据开始日期计算剩余期数
	 * 
	 * @param startDate
	 * @param dateList
	 * @return
	 * @throws Exception
	 */
	public static Integer calculateTotalPeriods(String startDate,
			List<String> dateList, Integer minDays) {
		Integer i;
		for (i = 0; i < dateList.size(); i++) {

			if (CalendarUtil.before(startDate, dateList.get(i))) {
				break;
			}
		}
		if (CalendarUtil.actualDaysBetween(startDate, dateList.get(i)) < minDays) {
			i++;

		}
		return dateList.size() - i == 0 ? 1 : dateList.size() - i;
	}

	/**
	 * 判断是否累加本金期数
	 * 
	 * @param repayWay
	 * @return
	 */
	public static Boolean isCumulativePrinPeriod(String repayWay) {
		String str = PropertyUtil.getPropertyOrDefault("period." + repayWay,
				"YES:YES");
		return "YES".equals(str.split(":")[0]);

	}

	/**
	 * 判断是否累加利息期数
	 * 
	 * @param repayWay
	 * @return
	 */
	public static Boolean isCumulativeIntPeriod(String repayWay) {
		String str = PropertyUtil.getPropertyOrDefault("period." + repayWay,
				"YES:YES");
		return "YES".equals(str.split(":")[1]);
	}

	/**
	 * 获取本金还款周期列表
	 * 
	 * @throws Exception
	 * @throws SecurityException
	 */
	public static List<String> genPrinPeriodList(LoanAgreement loanAgreement)
			throws FabException {

		Map<String, List<String>> mapPrin = prinstack.get();

		if (mapPrin == null) {
			mapPrin = new java.util.HashMap<String, List<String>>();
			prinstack.set(mapPrin);
		}

		List<String> dateList = mapPrin.get(loanAgreement.getContract()
				.getReceiptNo());
		if (dateList == null) {

			dateList = new ArrayList<String>();
			mapPrin.clear();
			mapPrin.put(loanAgreement.getContract().getReceiptNo(), dateList);
		} else {
			return dateList;
		}

		String contractStartDate = loanAgreement.getContract()
				.getContractStartDate();
		String contractEndDate = loanAgreement.getContract()
				.getContractEndDate();
		String periodFormula = loanAgreement.getWithdrawAgreement()
				.getPeriodFormula();
		Integer minDays = loanAgreement.getWithdrawAgreement()
				.getPeriodMinDays();
		RepayPeriod repayPeriod = RepayPeriodSupporter
				.genRepayPeriod(periodFormula);
		String sDate = contractStartDate;
		while (CalendarUtil.actualDaysBetween(sDate, contractEndDate) > 0) {
			LoggerUtil.debug("开始日期：" + sDate);
			if(!loanAgreement.getWithdrawAgreement().getFirstTermMonth() 
					|| !loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
					|| !loanAgreement.getWithdrawAgreement().getLastTermMerge()){
				sDate = RepayPeriodSupporter.calculateEndDateBySetting(sDate,
						contractStartDate, contractEndDate, minDays, repayPeriod,loanAgreement.getWithdrawAgreement(),true);
			}else{
				sDate = RepayPeriodSupporter.calculateEndDate(sDate,
						contractStartDate, contractEndDate, minDays, repayPeriod);
			}
			
			LoggerUtil.debug("结束日期：" + sDate);
			dateList.add(sDate);
		}

		return dateList;

	}

	/**
	 * 获取还款日、结本日、结息日对应期数
	 * 
	 * @param repayDate
	 * @param dateList
	 * @param minDays
	 * @return
	 */
	public static Integer getPeriod(String repayDate, List<String> dateList,
			Integer minDays) {

		Integer i;
		for (i = 0; i < dateList.size(); i++) {

			if (CalendarUtil.before(repayDate, dateList.get(i))) {
				break;
			}
		}
		i++;
		if (i >= dateList.size()) {
			return dateList.size();
		}

		if (CalendarUtil.actualDaysBetween(repayDate, dateList.get(i - 1)) < minDays) {
			i++;

		}

		return i;

	}

	/**
	 * 获取利息还款周期列表
	 * @param loanAgreement
	 * @return
	 * @throws Exception
	 */
	public static List<String> getIntPeriodList(LoanAgreement loanAgreement)
			throws FabException {
		Map<String, List<String>> mapInt = intstack.get();

		if (mapInt == null) {
			mapInt = new java.util.HashMap<String, List<String>>();
			intstack.set(mapInt);
		}

		List<String> dateList = mapInt.get(loanAgreement.getContract()
				.getReceiptNo());
		if (dateList == null) {

			dateList = new ArrayList<String>();
			mapInt.clear();
			mapInt.put(loanAgreement.getContract().getReceiptNo(), dateList);
		} else {
			return dateList;
		}

		String contractStartDate = loanAgreement.getContract()
				.getContractStartDate();
		String contractEndDate = loanAgreement.getContract()
				.getContractEndDate();
		String periodFormula = loanAgreement.getInterestAgreement()
				.getPeriodFormula();
		Integer minDays = loanAgreement.getInterestAgreement()
				.getPeriodMinDays();
		RepayPeriod repayPeriod = RepayPeriodSupporter
				.genRepayPeriod(periodFormula);
		String sDate = loanAgreement.getContract().getStartIntDate();
//		String sDate = loanAgreement.getContract().getRepayIntDate();
		while (CalendarUtil.actualDaysBetween(sDate, contractEndDate) > 0) {
			LoggerUtil.debug("开始日期：" + sDate);
			if(!loanAgreement.getWithdrawAgreement().getFirstTermMonth() 
					|| !loanAgreement.getWithdrawAgreement().getMiddleTermMonth()
					|| !loanAgreement.getWithdrawAgreement().getLastTermMerge()){
				sDate = RepayPeriodSupporter.calculateEndDateBySetting(sDate,
						contractStartDate, contractEndDate, minDays, repayPeriod,loanAgreement.getWithdrawAgreement(),false);
			}else{
				sDate = RepayPeriodSupporter.calculateEndDate(sDate,
						contractStartDate, contractEndDate, minDays, repayPeriod);
			}
			LoggerUtil.debug("结束日期：" + sDate);
			dateList.add(sDate);
		}
		return dateList;
	}
	
	
	/** add 20180712
	 * 根据开始日期结算P2P贷款的结束日期
	 * @param startDate
	 * @param contractStartDate
	 * @param contractEndDate
	 * @param minDays
	 * @param repayPeriod
	 * @return
	 */
	public static String calculateP2PEndDate(String startDate,
			String contractStartDate, String contractEndDate, Integer minDays,
			RepayPeriod repayPeriod) {
		DateTime b = new DateTime(startDate);
		DateTime cst = new DateTime(contractStartDate);
		DateTime cet = new DateTime(contractEndDate);

		Integer periodMs = repayPeriod.getPeriodY() * 12
				+ repayPeriod.getPeriodH() * 6 + repayPeriod.getPeriodQ() * 3
				+ repayPeriod.getPeriodM() + repayPeriod.getPeriodX() / 3
				+ repayPeriod.getPeriodD() / 30;
		Integer periodDs = repayPeriod.getPeriodX() % 3 * 10
				+ repayPeriod.getPeriodD() % 30;

		Integer timesDs = repayPeriod.getTimesY() * 360
				+ repayPeriod.getTimesH() * 180 + repayPeriod.getTimesQ() * 120
				+ repayPeriod.getTimesM() * 30 + repayPeriod.getTimesX() * 10
				+ repayPeriod.getTimesD();

		DateTime firstEndDate = new DateTime(contractStartDate);
		// 计算根据合同日期计算第一个还款日
		if (timesDs > 0) {

			if (repayPeriod.getTimesY() > 0) {
				firstEndDate = cst.plusYears(repayPeriod.getTimesY() - 1);
			} else if (repayPeriod.getTimesH() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesH() * 6 - 6);
			} else if (repayPeriod.getTimesQ() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesQ() * 3 - 3);
			} else if (repayPeriod.getTimesM() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesM() - 1);
			} else if (repayPeriod.getTimesX() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesX() * 10 - 10);
			} else if (repayPeriod.getTimesD() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesD() - 1);
			}
		} else {
			firstEndDate = cst.plusMonths(periodMs).plusDays(periodDs).minusDays(1);
		}
		//指定月
		if (repayPeriod.getOptMonth() > 0) {
			firstEndDate = CalendarUtil.optMonth(firstEndDate,repayPeriod.getOptMonth());
		}
		DateTime edt = new DateTime();
		if(startDate.equals(contractStartDate)){
			edt = cst.plusDays(periodDs);
			DateTime edt1 = new DateTime();
			if (repayPeriod.getOptDay() != 0) {
				edt1 = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
				if(Days.daysBetween(edt , edt1).getDays()<0){
					edt = cst.plusMonths(periodMs).plusDays(periodDs);
				}
			}else if(periodDs != 0){
				edt = cst.plusMonths(periodMs).plusDays(periodDs);
			}
		}else{
			edt = cst.plusMonths(periodMs).plusDays(periodDs);
		}
		// 开始日期小于第一个结束日期返回第一个结束日期
		if (Days.daysBetween(b, firstEndDate).getDays() < -1) {
			// 周期中月数大于0，用年月日方式计算，否则，用天数计算
			if (periodMs > 0) {
				Period p = new Period(firstEndDate, b,
						PeriodType.yearMonthDay());

				Integer diffMoths = p.getYears() * 12 + p.getMonths();
				Integer diffDays = p.getDays();

					edt = firstEndDate.plusMonths((diffMoths / periodMs + 1)
							* periodMs);
				if (periodDs != 0) {
					edt = edt.plusDays((diffDays / periodDs + 1) * periodDs);
				}
			} else {
				Period p = new Period(firstEndDate, b, PeriodType.days());
				Integer diffDays = p.getDays();
				if (periodDs != 0) {
					edt = firstEndDate.plusDays((diffDays / periodDs + 1)
							* periodDs);
				}
			}
		}

		if (edt.getYear()>b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			edt = CalendarUtil.optMonth(edt,repayPeriod.getOptMonth());
		}
		
		if (edt.getYear()==b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			
			edt = CalendarUtil.optMonthPhase(edt,periodMs,repayPeriod.getOptMonth());
		}
		
		
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		//小于周期最小天数加一期
		if (Days.daysBetween(b, edt).getDays() <= minDays) {
			edt = edt.plusMonths(periodMs).plusDays(periodDs);
		}
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		// 如果晚于合同到期日期，则返回合同到日期
		if (Days.daysBetween(edt, cet).getDays() <= minDays) {
			return contractEndDate;
		}

		return edt.toString("yyyy-MM-dd");

	}
	
	/**
	 * 根据开始日期结算结束日期
	 * @param startDate
	 * @param contractStartDate
	 * @param contractEndDate
	 * @param minDays
	 * @param repayPeriod
	 * @return
	 */
	public static String calculateEndDateByFlag1(String startDate,
			String contractStartDate, String contractEndDate, Integer minDays,
			RepayPeriod repayPeriod) {
		DateTime b = new DateTime(startDate);
		DateTime cst = new DateTime(contractStartDate);
		DateTime cet = new DateTime(contractEndDate);

		Integer periodMs = repayPeriod.getPeriodY() * 12
				+ repayPeriod.getPeriodH() * 6 + repayPeriod.getPeriodQ() * 3
				+ repayPeriod.getPeriodM() + repayPeriod.getPeriodX() / 3
				+ repayPeriod.getPeriodD() / 30;
		Integer periodDs = repayPeriod.getPeriodX() % 3 * 10
				+ repayPeriod.getPeriodD() % 30;

		Integer timesDs = repayPeriod.getTimesY() * 360
				+ repayPeriod.getTimesH() * 180 + repayPeriod.getTimesQ() * 120
				+ repayPeriod.getTimesM() * 30 + repayPeriod.getTimesX() * 10
				+ repayPeriod.getTimesD();

		DateTime firstEndDate = new DateTime(contractStartDate);
		// 计算根据合同日期计算第一个还款日
		if (timesDs > 0) {

			if (repayPeriod.getTimesY() > 0) {
				firstEndDate = cst.plusYears(repayPeriod.getTimesY() - 1);
			} else if (repayPeriod.getTimesH() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesH() * 6 - 6);
			} else if (repayPeriod.getTimesQ() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesQ() * 3 - 3);
			} else if (repayPeriod.getTimesM() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesM() - 1);
			} else if (repayPeriod.getTimesX() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesX() * 10 - 10);
			} else if (repayPeriod.getTimesD() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesD() - 1);
			}
		} else {
			firstEndDate = cst.plusMonths(periodMs).plusDays(periodDs);
		}
		//指定月
		if (repayPeriod.getOptMonth() > 0) {
			firstEndDate = CalendarUtil.optMonth(firstEndDate,repayPeriod.getOptMonth());
		}

		DateTime edt = firstEndDate;
		if(startDate.equals(contractStartDate)){
			edt = edt.plusMonths(periodMs);
		}
		
		// 开始日期小于第一个结束日期返回第一个结束日期
		if (Days.daysBetween(b, firstEndDate).getDays() < 0) {
			// 周期中月数大于0，用年月日方式计算，否则，用天数计算
			if (periodMs > 0) {
				Period p = new Period(firstEndDate, b,
						PeriodType.yearMonthDay());

				Integer diffMoths = p.getYears() * 12 + p.getMonths();
				Integer diffDays = p.getDays();

					edt = firstEndDate.plusMonths((diffMoths / periodMs + 1)
							* periodMs);
				if (periodDs != 0) {
					edt = edt.plusDays((diffDays / periodDs + 1) * periodDs);
				}
			} else {
				Period p = new Period(firstEndDate, b, PeriodType.days());
				Integer diffDays = p.getDays();
				if (periodDs != 0) {
					edt = firstEndDate.plusDays((diffDays / periodDs + 1)
							* periodDs);
				}
			}
		}

		if (edt.getYear()>b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			edt = CalendarUtil.optMonth(edt,repayPeriod.getOptMonth());
		}
		
		if (edt.getYear()==b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			
			edt = CalendarUtil.optMonthPhase(edt,periodMs,repayPeriod.getOptMonth());
		}
		
		
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		//小于周期最小天数加一期
		if (Days.daysBetween(b, edt).getDays() <= minDays) {
			edt = edt.plusMonths(periodMs).plusDays(periodDs);
		}
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		// 如果晚于合同到期日期，则返回合同到日期
		if (Days.daysBetween(edt, cet).getDays() <= minDays) {
			return contractEndDate;
		}

		return edt.toString("yyyy-MM-dd");

	}
	
	/**
	 * 根据开始日期结算结束日期
	 * @param startDate
	 * @param contractStartDate
	 * @param contractEndDate
	 * @param minDays
	 * @param repayPeriod
	 * @return
	 */
	public static String calculateEndDateByFlag8(String startDate,
			String contractStartDate, String contractEndDate, Integer minDays,
			RepayPeriod repayPeriod) {
		DateTime b = new DateTime(startDate);
		DateTime cst = new DateTime(contractStartDate);
		DateTime cet = new DateTime(contractEndDate);

		Integer periodMs = repayPeriod.getPeriodY() * 12
				+ repayPeriod.getPeriodH() * 6 + repayPeriod.getPeriodQ() * 3
				+ repayPeriod.getPeriodM() + repayPeriod.getPeriodX() / 3
				+ repayPeriod.getPeriodD() / 30;
		Integer periodDs = repayPeriod.getPeriodX() % 3 * 10
				+ repayPeriod.getPeriodD() % 30;

		Integer timesDs = repayPeriod.getTimesY() * 360
				+ repayPeriod.getTimesH() * 180 + repayPeriod.getTimesQ() * 120
				+ repayPeriod.getTimesM() * 30 + repayPeriod.getTimesX() * 10
				+ repayPeriod.getTimesD();

		DateTime firstEndDate = new DateTime(contractStartDate);
		// 计算根据合同日期计算第一个还款日
		if (timesDs > 0) {

			if (repayPeriod.getTimesY() > 0) {
				firstEndDate = cst.plusYears(repayPeriod.getTimesY() - 1);
			} else if (repayPeriod.getTimesH() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesH() * 6 - 6);
			} else if (repayPeriod.getTimesQ() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesQ() * 3 - 3);
			} else if (repayPeriod.getTimesM() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesM() - 1);
			} else if (repayPeriod.getTimesX() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesX() * 10 - 10);
			} else if (repayPeriod.getTimesD() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesD() - 1);
			}
		} else {
			firstEndDate = cst.plusMonths(periodMs).plusDays(periodDs);
		}
		//指定月
		if (repayPeriod.getOptMonth() > 0) {
			firstEndDate = CalendarUtil.optMonth(firstEndDate,repayPeriod.getOptMonth());
		}

		DateTime edt = firstEndDate;
		
		
		// 开始日期小于第一个结束日期返回第一个结束日期
		if (Days.daysBetween(b, firstEndDate).getDays() < 0) {
			// 周期中月数大于0，用年月日方式计算，否则，用天数计算
			if (periodMs > 0) {
				Period p = new Period(firstEndDate, b,
						PeriodType.yearMonthDay());

				Integer diffMoths = p.getYears() * 12 + p.getMonths();
				Integer diffDays = p.getDays();

					edt = firstEndDate.plusMonths((diffMoths / periodMs + 1)
							* periodMs);
				if (periodDs != 0) {
					edt = edt.plusDays((diffDays / periodDs + 1) * periodDs);
				}
			} else {
				Period p = new Period(firstEndDate, b, PeriodType.days());
				Integer diffDays = p.getDays();
				if (periodDs != 0) {
					edt = firstEndDate.plusDays((diffDays / periodDs + 1)
							* periodDs);
				}
			}
		}

		if (edt.getYear()>b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			edt = CalendarUtil.optMonth(edt,repayPeriod.getOptMonth());
		}
		
		if (edt.getYear()==b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			
			edt = CalendarUtil.optMonthPhase(edt,periodMs,repayPeriod.getOptMonth());
		}
		
		
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		//小于周期最小天数加一期
		if(startDate.equals(contractStartDate)){
			if (Days.daysBetween(b, edt).getDays() < minDays) {
				edt = edt.plusMonths(periodMs).plusDays(periodDs);
			}
		}else{
			if (Days.daysBetween(b, edt).getDays() <= minDays) {
				edt = edt.plusMonths(periodMs).plusDays(periodDs);
			}
		}
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		// 如果晚于合同到期日期，则返回合同到日期
		if (Days.daysBetween(edt, cet).getDays() <= minDays) {
			return contractEndDate;
		}

		return edt.toString("yyyy-MM-dd");

	}
	
	/**
	 * 根据开始日期结算结束日期
	 * @param startDate
	 * @param contractStartDate
	 * @param contractEndDate
	 * @param minDays
	 * @param repayPeriod
	 * @return
	 */
	public static String calculateEndDateBySetting(String startDate,
			String contractStartDate, String contractEndDate, Integer minDays,
			RepayPeriod repayPeriod,WithdrawAgreement withdrawAgreement ,boolean isPrin) {
		DateTime b = new DateTime(startDate);
		DateTime cst = new DateTime(contractStartDate);
		DateTime cet = new DateTime(contractEndDate);

		Integer periodMs = repayPeriod.getPeriodY() * 12
				+ repayPeriod.getPeriodH() * 6 + repayPeriod.getPeriodQ() * 3
				+ repayPeriod.getPeriodM() + repayPeriod.getPeriodX() / 3
				+ repayPeriod.getPeriodD() / 30;
		Integer periodDs = repayPeriod.getPeriodX() % 3 * 10
				+ repayPeriod.getPeriodD() % 30;

		Integer timesDs = repayPeriod.getTimesY() * 360
				+ repayPeriod.getTimesH() * 180 + repayPeriod.getTimesQ() * 120
				+ repayPeriod.getTimesM() * 30 + repayPeriod.getTimesX() * 10
				+ repayPeriod.getTimesD();

		DateTime firstEndDate = new DateTime(contractStartDate);
		// 计算根据合同日期计算第一个还款日
		if (timesDs > 0) {

			if (repayPeriod.getTimesY() > 0) {
				firstEndDate = cst.plusYears(repayPeriod.getTimesY() - 1);
			} else if (repayPeriod.getTimesH() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesH() * 6 - 6);
			} else if (repayPeriod.getTimesQ() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesQ() * 3 - 3);
			} else if (repayPeriod.getTimesM() > 0) {
				firstEndDate = cst.plusMonths(repayPeriod.getTimesM() - 1);
			} else if (repayPeriod.getTimesX() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesX() * 10 - 10);
			} else if (repayPeriod.getTimesD() > 0) {
				firstEndDate = cst.plusDays(repayPeriod.getTimesD() - 1);
			}
		} else if(
			//preiodMs*30+periodDs>30 还款周期不是1M  首期跨月 20191205
				( (periodMs*30+periodDs) <= 30)
				&&!withdrawAgreement.getFirstTermMonth()
				&& repayPeriod.getOptDay() != 0
				&& (!Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_LSBQ
						+","+ConstantDeclare.REPAYWAY.REPAYWAY_LZHK
						+","+ConstantDeclare.REPAYWAY.REPAYWAY_JYHK).split(",")).contains(withdrawAgreement.getRepayWay()) || 
						!isPrin)){
			firstEndDate = cst.plusDays(periodDs);
		} else {
			firstEndDate = cst.plusMonths(periodMs).plusDays(periodDs);
		}
		//指定月
		if (repayPeriod.getOptMonth() > 0) {
			firstEndDate = CalendarUtil.optMonth(firstEndDate,repayPeriod.getOptMonth());
		}

		DateTime edt = firstEndDate;
		// 开始日期小于第一个结束日期返回第一个结束日期
		if ((Days.daysBetween(b, firstEndDate).getDays() < 0
				|| !startDate.equals(contractStartDate)) && repayPeriod.getOptDay() != 0) {
			// 周期中月数大于0，用年月日方式计算，否则，用天数计算
			if (periodMs > 0) {
				Period p = new Period(firstEndDate, b,
						PeriodType.yearMonthDay());

				Integer diffMoths = p.getYears() * 12 + p.getMonths();
				Integer diffDays = p.getDays();
				if(withdrawAgreement.getMiddleTermMonth()){
					if(!(b.toString("yyyy-MM-dd").substring(0, 7).compareTo(firstEndDate.toString("yyyy-MM-dd").substring(0, 7))<0 && diffDays<0)){
						edt = firstEndDate.plusMonths((diffMoths / periodMs + 1)
								* periodMs);
						if (periodDs != 0) {
						edt = edt.plusDays((diffDays / periodDs + 1) * periodDs);
						}
					}
				}else{
					if (repayPeriod.getOptDay() != 0) {
						firstEndDate = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
					}
					p = new Period(firstEndDate, b,
							PeriodType.yearMonthDay());
					diffMoths = p.getYears() * 12 + p.getMonths();
					diffDays = p.getDays();
					if(repaywayjuage(b,firstEndDate,diffDays,isPrin,withdrawAgreement)){
						edt = firstEndDate.plusMonths((diffMoths / periodMs)
								* periodMs);
						if (periodDs != 0) {
						edt = edt.plusDays((diffDays / periodDs) * periodDs);
						}
					}
				}
				
			} else {
				Period p = new Period(firstEndDate, b, PeriodType.days());
				Integer diffDays = p.getDays();
				if (periodDs != 0) {
					edt = firstEndDate.plusDays((diffDays / periodDs + 1)
							* periodDs);
				}
			}
		}

		if (edt.getYear()>b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			edt = CalendarUtil.optMonth(edt,repayPeriod.getOptMonth());
		}
		
		if (edt.getYear()==b.getYear() && repayPeriod.getOptMonth() > 0)
		{
			
			edt = CalendarUtil.optMonthPhase(edt,periodMs,repayPeriod.getOptMonth());
		}
		
		
		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}

		//小于周期最小天数加一期
		if (Days.daysBetween(b, edt).getDays() <= minDays) {
			edt = edt.plusMonths(periodMs).plusDays(periodDs);
		}

		// 指定日
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
		}
		
		//小于周期最小天数加一期
		if (Days.daysBetween(b, edt).getDays() <= minDays) {
			edt = edt.plusMonths(periodMs).plusDays(periodDs);
			// 指定日
			if (repayPeriod.getOptDay() != 0) {
				// 如果本月没有指定日期，则指定该月月末
				edt = CalendarUtil.optDay(edt, repayPeriod.getOptDay());
			}
		}

		// 如果晚于合同到期日期，则返回合同到日期
		//到期日与合同到期日之间天数小于最小天数（合同到期日-到期日<=15）
		if(withdrawAgreement.getLastTermMerge()){
			if (Days.daysBetween(edt, cet).getDays() <= minDays) {
				return contractEndDate;
			}
		}
		else{
			if (Days.daysBetween(edt, cet).getDays() <= 0) {
				if(repayPeriod.getOptDay() != 0){
					String secondLastEnd = calculateLastStartdate(repayPeriod,periodMs,periodDs,edt);
					if(startDate.compareTo(secondLastEnd)<0 
							&& !startDate.equals(contractStartDate)){
						return secondLastEnd;
					}
				}
				return contractEndDate;
			}
		}
		
		//光大人保助贷，最后一期合并处理 2020-04-03
		if( withdrawAgreement.getLastTowTermMerge() ) {
			if (  Integer.valueOf(cet.toString().subSequence(5, 7).toString())  
					- Integer.valueOf(b.toString().subSequence(5, 7).toString()) 
					== 1 ) {
				return contractEndDate;
			}
		}
		
		return edt.toString("yyyy-MM-dd");

	}
	
	public static String calculateLastStartdate(
			RepayPeriod repayPeriod,Integer periodMs,Integer periodDs, DateTime edt) {
		DateTime cet1 = edt.minusMonths(periodMs).minusDays(periodDs);
		if (repayPeriod.getOptDay() != 0) {
			// 如果本月没有指定日期，则指定该月月末
			cet1 = CalendarUtil.optDay(cet1, repayPeriod.getOptDay());
		}
		return cet1.toString("yyyy-MM-dd");
	}
	
	/**
	 * 
	 * @param withdrawAgreement 
	 * @param isPrin 是否是本金账本
	 * @param diffDays 相差天数
	 * @param firstEndDate 第一个结束日期
	 * @param b  开始日期 
	 * @return
	 */
	public static boolean repaywayjuage(DateTime b, DateTime firstEndDate, Integer diffDays, boolean isPrin, WithdrawAgreement withdrawAgreement) {
		//判断还款方式是利随本清/年还本,月还息或者按2周还款/半年本月还息 或者季年本月还息
		if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_LSBQ
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_LZHK
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_JYHK).split(",")).contains(withdrawAgreement.getRepayWay())){
			//如果开始日期在第一次结束日期之前并且相差天数小于0
			if(b.toString("yyyy-MM-dd").substring(0, 7).compareTo(firstEndDate.toString("yyyy-MM-dd").substring(0, 7))>0 || diffDays>0){
				return true;
			}else if(!isPrin){	//如果是利息账本
				return true;
			}else{
				return false;
			}
		}else{
			return true;
		}
	}
	
}

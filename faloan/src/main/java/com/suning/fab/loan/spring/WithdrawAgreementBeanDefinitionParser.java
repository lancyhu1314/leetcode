package com.suning.fab.loan.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.suning.fab.loan.la.WithdrawAgreement;
import com.suning.fab.tup4j.utils.VarChecker;

public class WithdrawAgreementBeanDefinitionParser implements
		BeanDefinitionParser {
	private static final String ATTRIBUTE_REPAYCHANNEL = "repayChannel";
	private static final String ATTRIBUTE_ISPREPAY = "isPrepay";
	private static final String ATTRIBUTE_PERIODFORMULA = "periodFormula";
	private static final String ATTRIBUTE_PERIODMINDAYS = "periodMinDays";
	private static final String ATTRIBUTE_REPAYAMTFORMULA = "repayAmtFormula";
	
	private static final String ATTRIBUTE_ISAGREEPARTREPAY = "isAgreePartRepay";
	private static final String ATTRIBUTE_GENCURRREPAYPLANOPT = "genCurrRepayPlanOpt";
	private static final String ATTRIBUTE_REPAYWAY = "repayWay";
	private static final String ATTRIBUTE_INTISSUES = "intIssueS";
	private static final String ATTRIBUTE_ISSUETYPE = "issueType";
	private static final String ATTRIBUTE_USEREPAYWAY = "useRepayWay";
	private static final String ATTRIBUTE_FIRSTTERMMONTH = "firstTermMonth";
	private static final String ATTRIBUTE_MIDDLETERMMONTH = "middleTermMonth";
	private static final String ATTRIBUTE_LASTTERMMERGE = "lastTermMerge";
	private static final String ATTRIBUTE_ENDDATEDELAY = "endDateDelay";
	@Override
	public BeanDefinition parse(Element element, ParserContext context) {

		BeanDefinitionBuilder def = BeanDefinitionBuilder
				.rootBeanDefinition(WithdrawAgreement.class);
		
		//允许的还款方式
		String useRepayWay = element.getAttribute(ATTRIBUTE_USEREPAYWAY);
		if (!VarChecker.isEmpty(useRepayWay)) {
			def.addPropertyValue(ATTRIBUTE_USEREPAYWAY,useRepayWay);
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_USEREPAYWAY,"NO");
		}
		
		//首期是否需要加上周期月数
		String firstTermMonth = element.getAttribute(ATTRIBUTE_FIRSTTERMMONTH);
		if (!VarChecker.isEmpty(firstTermMonth)) {
			def.addPropertyValue(ATTRIBUTE_FIRSTTERMMONTH,firstTermMonth);
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_FIRSTTERMMONTH,true);
		}
		
		//中间期是否需要合并
		String middleTermMonth = element.getAttribute(ATTRIBUTE_MIDDLETERMMONTH);
		if (!VarChecker.isEmpty(middleTermMonth)) {
			def.addPropertyValue(ATTRIBUTE_MIDDLETERMMONTH,middleTermMonth);
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_MIDDLETERMMONTH,true);
		}
				
		//最后一期是否需要合并
		String lastTermMerge = element.getAttribute(ATTRIBUTE_LASTTERMMERGE);
		if (!VarChecker.isEmpty(lastTermMerge)) {
			def.addPropertyValue(ATTRIBUTE_LASTTERMMERGE,lastTermMerge);
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_LASTTERMMERGE,true);
		}

		String endDateDelay = element.getAttribute(ATTRIBUTE_ENDDATEDELAY);
		//最后一期是否需要合并
		if (!VarChecker.isEmpty(endDateDelay)) {
			def.addPropertyValue(ATTRIBUTE_ENDDATEDELAY,endDateDelay);
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_ENDDATEDELAY,false);
		}
		String repayChannel = element.getAttribute(ATTRIBUTE_REPAYCHANNEL);
		if (!VarChecker.isEmpty(repayChannel)) {
			
			def.addPropertyValue(ATTRIBUTE_REPAYCHANNEL, repayChannel);
		}

		String isPrepay = element.getAttribute(ATTRIBUTE_ISPREPAY);
		if (!VarChecker.isEmpty(isPrepay)) {
			def.addPropertyValue(ATTRIBUTE_ISPREPAY,Boolean.parseBoolean(isPrepay));
		}

		String periodMinDays = element.getAttribute(ATTRIBUTE_PERIODMINDAYS);
		if (!VarChecker.isEmpty(periodMinDays))
		{
			def.addPropertyValue(ATTRIBUTE_PERIODMINDAYS, periodMinDays);
		}
		String periodFormula = element.getAttribute(ATTRIBUTE_PERIODFORMULA);
		if (!VarChecker.isEmpty(periodFormula))
		{
			def.addPropertyValue(ATTRIBUTE_PERIODFORMULA, periodFormula);
		}
		
		String repayAmtFormula = element
				.getAttribute(ATTRIBUTE_REPAYAMTFORMULA);
		if (!VarChecker.isEmpty(repayAmtFormula))
		{
			def.addPropertyValue(ATTRIBUTE_REPAYAMTFORMULA, repayAmtFormula);
		}

		String genCurrRepayPlanOpt = element
				.getAttribute(ATTRIBUTE_GENCURRREPAYPLANOPT);
		if (!VarChecker.isEmpty(genCurrRepayPlanOpt))
		{
			def.addPropertyValue(ATTRIBUTE_GENCURRREPAYPLANOPT,
					Boolean.getBoolean(genCurrRepayPlanOpt));
		}

		String isAgreePartRepay = element
				.getAttribute(ATTRIBUTE_ISAGREEPARTREPAY);
		if (!VarChecker.isEmpty(isAgreePartRepay))
		{
			def.addPropertyValue(ATTRIBUTE_ISAGREEPARTREPAY,
					Boolean.getBoolean(isAgreePartRepay));
		}

		String repayWay = element.getAttribute(ATTRIBUTE_REPAYWAY);
		if (!VarChecker.isEmpty(repayWay))
		{
			def.addPropertyValue(ATTRIBUTE_REPAYWAY, repayWay);
			
		}

		String intIssueS = element.getAttribute(ATTRIBUTE_INTISSUES);
		if (!VarChecker.isEmpty(intIssueS))
		{
			def.addPropertyValue(ATTRIBUTE_INTISSUES, intIssueS);
		}

		String issueType = element.getAttribute(ATTRIBUTE_ISSUETYPE);
		if (!VarChecker.isEmpty(issueType))
		{
			def.addPropertyValue(ATTRIBUTE_ISSUETYPE, issueType);
		}

		return def.getBeanDefinition();
	}
}

package com.suning.fab.loan.spring;


import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.suning.fab.loan.la.NormalLoanAgreement;
import com.suning.fab.tup4j.utils.VarChecker;

public class NormalLoanAgreementBeanDefinitionParser implements BeanDefinitionParser {
	private static final String ATTRIBUTE_GRACEDAYS = "graceDays";
	private static final String ATTRIBUTE_REPAYCHANNEL = "repayChannel";
	private static final String ATTRIBUTE_ISPREPAY = "isPrepay";
	@Override
	public BeanDefinition parse(Element element, ParserContext context) {

		BeanDefinitionBuilder def = BeanDefinitionBuilder
				.rootBeanDefinition(NormalLoanAgreement.class);

		String graceDays = element.getAttribute(ATTRIBUTE_GRACEDAYS);
		
		if(!VarChecker.isEmpty(graceDays))
		{
			def.addPropertyValue(ATTRIBUTE_GRACEDAYS, Integer.valueOf(graceDays));
		}

		String repayChannel = element.getAttribute(ATTRIBUTE_REPAYCHANNEL);
		if(!VarChecker.isEmpty(repayChannel))
		{
			def.addPropertyValue(ATTRIBUTE_REPAYCHANNEL, Integer.valueOf(repayChannel));
		}

		String isPrepay = element.getAttribute(ATTRIBUTE_ISPREPAY);
		if(!VarChecker.isEmpty(isPrepay))
		{
			def.addPropertyValue(ATTRIBUTE_ISPREPAY, Boolean.getBoolean(isPrepay));
		}
		
		return def.getBeanDefinition();
	}
}

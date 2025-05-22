package com.suning.fab.loan.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.suning.fab.loan.la.FeeAgreement;
import com.suning.fab.tup4j.utils.VarChecker;

public class FeeAgreementBeanDefinitionParser implements BeanDefinitionParser {
	
	private static final String ATTRIBUTE_FEETYPE = "feeType";
	
	
	
	
	
	
	
	
	
	/*private static final String ATTRIBUTE_RATE = "rate";
	private static final String ATTRIBUTE_DAYSOFYEAR = "daysofmonth";
	private static final String ATTRIBUTE_DAYSOFMONTH = "daysofyear";*/
	@Override
	public BeanDefinition parse(Element element, ParserContext context) {

		BeanDefinitionBuilder def = BeanDefinitionBuilder
				.rootBeanDefinition(FeeAgreement.class);

		
		/* 管理费 */
		String feeType = element.getAttribute(ATTRIBUTE_FEETYPE);
		if (!VarChecker.isEmpty(feeType))
		{
			def.addPropertyValue(ATTRIBUTE_FEETYPE, feeType);
		}
		else
		{
			def.addPropertyValue(ATTRIBUTE_FEETYPE, "");
		}
		
		return def.getBeanDefinition();
	}
}

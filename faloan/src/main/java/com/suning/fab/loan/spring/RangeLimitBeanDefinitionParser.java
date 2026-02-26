package com.suning.fab.loan.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.suning.fab.loan.la.RangeLimit;

public class RangeLimitBeanDefinitionParser implements BeanDefinitionParser {
	/*private static final String ATTRIBUTE_RATE = "rate";
	private static final String ATTRIBUTE_DAYSOFYEAR = "daysofmonth";
	private static final String ATTRIBUTE_DAYSOFMONTH = "daysofyear";*/
	@Override
	public BeanDefinition parse(Element element, ParserContext context) {

		BeanDefinitionBuilder def = BeanDefinitionBuilder
				.rootBeanDefinition(RangeLimit.class);

		/*String rate = element.getAttribute(ATTRIBUTE_RATE);
		emptyAttributeCheck(element.getLocalName(), ATTRIBUTE_RATE, rate);
		def.addPropertyValue(ATTRIBUTE_RATE, Double.valueOf(rate));

		String daysofmonth = element.getAttribute(ATTRIBUTE_DAYSOFMONTH);
		emptyAttributeCheck(element.getLocalName(), ATTRIBUTE_DAYSOFMONTH, daysofmonth);
		def.addPropertyValue(ATTRIBUTE_DAYSOFMONTH, Integer.valueOf(daysofmonth));

		String daysofyear = element.getAttribute(ATTRIBUTE_DAYSOFYEAR);
		emptyAttributeCheck(element.getLocalName(), ATTRIBUTE_DAYSOFYEAR, daysofyear);
		def.addPropertyValue(ATTRIBUTE_DAYSOFYEAR, Integer.valueOf(daysofyear));*/
		
		return def.getBeanDefinition();
	}
}

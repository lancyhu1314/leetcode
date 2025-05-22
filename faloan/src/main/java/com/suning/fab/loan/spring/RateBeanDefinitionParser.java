package com.suning.fab.loan.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.utils.VarChecker;

public class RateBeanDefinitionParser implements BeanDefinitionParser {
	private static final String ATTRIBUTE_RATE = "rate";
	private static final String ATTRIBUTE_DAYSPERYEAR = "daysPerYear";
	private static final String ATTRIBUTE_DAYSPERMONTH = "daysPerMonth";
	private static final String ATTRIBUTE_RATEUNIT = "rateUnit";
	@Override
	public BeanDefinition parse(Element element, ParserContext context) {

		BeanDefinitionBuilder def = BeanDefinitionBuilder
				.rootBeanDefinition(FabRate.class);

		String rate = element.getAttribute(ATTRIBUTE_RATE);
		if (!VarChecker.isEmpty(rate)) {
			def.addPropertyValue(ATTRIBUTE_RATE, Double.valueOf(rate));
		}

		String daysPerYear = element.getAttribute(ATTRIBUTE_DAYSPERYEAR);
		if (!VarChecker.isEmpty(daysPerYear)) {

			def.addPropertyValue(ATTRIBUTE_DAYSPERYEAR,
					Integer.valueOf(daysPerYear));
		}

		String daysPerMonth = element.getAttribute(ATTRIBUTE_DAYSPERMONTH);
		if (!VarChecker.isEmpty(daysPerMonth)) {

			def.addPropertyValue(ATTRIBUTE_DAYSPERMONTH,
					Integer.valueOf(daysPerMonth));
		}

		String rateUnit = element.getAttribute(ATTRIBUTE_RATEUNIT);
		if (!VarChecker.isEmpty(rateUnit)) {
			def.addPropertyValue(ATTRIBUTE_RATEUNIT, rateUnit);
		}

		return def.getBeanDefinition();
	}
}

package com.suning.fab.loan.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import com.suning.fab.loan.la.RateAgreement;

public class RateAgreementBeanDefinitionParser implements BeanDefinitionParser {
	private static final String ATTRIBUTE_NORMALRATE = "normalRate";
	private static final String ATTRIBUTE_OVERDUERATE = "overdueRate";
	private static final String ATTRIBUTE_COMPOUNDRATE = "compoundRate";
	@Override
	public BeanDefinition parse(Element element, ParserContext context) {

		BeanDefinitionBuilder def = BeanDefinitionBuilder
				.genericBeanDefinition(RateAgreement.class);

		Element normalRate = DomUtils.getChildElementByTagName(element,
				ATTRIBUTE_NORMALRATE);
		if (normalRate != null) {
			BeanDefinitionHolder obj = (BeanDefinitionHolder) context
					.getDelegate().parsePropertySubElement(normalRate, null);
			def.addPropertyValue(ATTRIBUTE_NORMALRATE, obj);
		}

		Element overdueRate = DomUtils.getChildElementByTagName(element,
				ATTRIBUTE_OVERDUERATE);
		if (overdueRate != null) {
			BeanDefinitionHolder obj = (BeanDefinitionHolder) context
					.getDelegate().parsePropertySubElement(overdueRate, null);
			def.addPropertyValue(ATTRIBUTE_OVERDUERATE, obj);
		}

		Element compoundRate = DomUtils.getChildElementByTagName(element,
				ATTRIBUTE_COMPOUNDRATE);
		if (compoundRate != null) {
			BeanDefinitionHolder obj = (BeanDefinitionHolder) context
					.getDelegate().parsePropertySubElement(compoundRate, null);
			def.addPropertyValue(ATTRIBUTE_COMPOUNDRATE, obj);
		}

		return def.getBeanDefinition();
	}
}

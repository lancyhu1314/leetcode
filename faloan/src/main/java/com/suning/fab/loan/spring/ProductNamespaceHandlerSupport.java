package com.suning.fab.loan.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class ProductNamespaceHandlerSupport extends NamespaceHandlerSupport {
	@Override
	public void init() {
		registerBeanDefinitionParser("LnPrd",new ProductBeanDefinitionParser());
		registerBeanDefinitionParser("rateAgreement",new RateAgreementBeanDefinitionParser());
		registerBeanDefinitionParser("feeAgreement",new FeeAgreementBeanDefinitionParser());
		registerBeanDefinitionParser("rangeLimit",new RangeLimitBeanDefinitionParser());
		registerBeanDefinitionParser("grantAgreement",new GrantAgreementBeanDefinitionParser());
		registerBeanDefinitionParser("openAgreement",new OpenAgreementBeanDefinitionParser());
		registerBeanDefinitionParser("interestAgreement",new InterestAgreementBeanDefinitionParser());
		registerBeanDefinitionParser("withdrawAgreement",new WithdrawAgreementBeanDefinitionParser());
		registerBeanDefinitionParser("normalRate",new RateBeanDefinitionParser());
		registerBeanDefinitionParser("overdueRate",new RateBeanDefinitionParser());
		registerBeanDefinitionParser("compoundRate",new RateBeanDefinitionParser());
//		registerBeanDefinitionParser("TermsLoan",new TermsLoanAgreementBeanDefinitionParser());
//		registerBeanDefinitionParser("NormalLoan",new NormalLoanAgreementBeanDefinitionParser());
	}
}

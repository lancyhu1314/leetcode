package com.suning.fab.loan.spring;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import com.suning.fab.loan.la.Product;
public class ProductBeanDefinitionParser implements BeanDefinitionParser {
	@Override
	 public BeanDefinition parse(Element element, ParserContext context)  
	    {  
	        RootBeanDefinition def = new RootBeanDefinition();  
	  
	        // 设置Bean Class  
	        def.setBeanClass(Product.class);  
	  
	        // 注册ID属性  
	        String id = element.getAttribute("id");  
	        BeanDefinitionHolder idHolder = new BeanDefinitionHolder(def, id);  
	        BeanDefinitionReaderUtils.registerBeanDefinition(idHolder,  
	                context.getRegistry());  
	  
	        // 注册属性  
	        String prdId = element.getAttribute("prdId");  
	        String prdName = element.getAttribute("prdName");  
	  
	        BeanDefinitionHolder prdIdHolder = new BeanDefinitionHolder(def, prdId);  
	        BeanDefinitionHolder prdNameHolder = new BeanDefinitionHolder(def,  
	        		prdName);  
	  
	        BeanDefinitionReaderUtils.registerBeanDefinition(prdIdHolder,  
	                context.getRegistry());  
	        BeanDefinitionReaderUtils.registerBeanDefinition(prdNameHolder,  
	                context.getRegistry());  
	        def.getPropertyValues().addPropertyValue("prdId", prdId);  
	        def.getPropertyValues().addPropertyValue("prdName", prdName);  
	  
	        List<Element> rateElements = DomUtils.getChildElementsByTagName(element, "rateAgreement");
	        if (rateElements != null && (!rateElements.isEmpty())) {
	            for (Element rateElement : rateElements){
	                BeanDefinitionHolder obj = (BeanDefinitionHolder) context.getDelegate().parsePropertySubElement(rateElement, null);
	                def.getPropertyValues().addPropertyValue("rateAgreement", obj);  
	            }
	        }
	        
	        Element feeAgreement = DomUtils.getChildElementByTagName(element, "feeAgreement");
	        if (feeAgreement != null )
	        {
	        	BeanDefinitionHolder obj = (BeanDefinitionHolder) context.getDelegate().parsePropertySubElement(feeAgreement, null);
	        	def.getPropertyValues().addPropertyValue("feeAgreement", obj);  
	        }
	        
	        Element rangeLimit = DomUtils.getChildElementByTagName(element, "rangeLimit");
	        if (rangeLimit != null )
	        {
	        	BeanDefinitionHolder obj = (BeanDefinitionHolder) context.getDelegate().parsePropertySubElement(rangeLimit, null);
	        	def.getPropertyValues().addPropertyValue("rangeLimit", obj);  
	        }
	        
	        Element grantAgreement = DomUtils.getChildElementByTagName(element, "grantAgreement");
	        if (grantAgreement != null )
	        {
	        	BeanDefinitionHolder obj = (BeanDefinitionHolder) context.getDelegate().parsePropertySubElement(grantAgreement, null);
	        	def.getPropertyValues().addPropertyValue("grantAgreement", obj);  
	        }
	        
	        Element openAgreement = DomUtils.getChildElementByTagName(element, "openAgreement");
	        if (openAgreement != null )
	        {
	        	BeanDefinitionHolder obj = (BeanDefinitionHolder) context.getDelegate().parsePropertySubElement(openAgreement, null);
	        	def.getPropertyValues().addPropertyValue("openAgreement", obj);  
	        }
	        
	        
	        Element interestAgreement = DomUtils.getChildElementByTagName(element, "interestAgreement");
	        if (interestAgreement != null )
	        {
	        	BeanDefinitionHolder obj = (BeanDefinitionHolder) context.getDelegate().parsePropertySubElement(interestAgreement, null);
	        	def.getPropertyValues().addPropertyValue("interestAgreement", obj);  
	        }
	        
	        Element withdrawPlanElement = DomUtils.getChildElementByTagName(element, "withdrawAgreement");
	        if (withdrawPlanElement != null )
	        {
	        	BeanDefinitionHolder obj = (BeanDefinitionHolder) context.getDelegate().parsePropertySubElement(withdrawPlanElement, null);
	        	def.getPropertyValues().addPropertyValue("withdrawAgreement", obj);  
	        }
	        return def;  
	    }  
}

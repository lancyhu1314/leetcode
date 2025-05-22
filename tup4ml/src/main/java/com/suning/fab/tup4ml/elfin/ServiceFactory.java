
package com.suning.fab.tup4ml.elfin;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
* spring服务工厂注入类
* @author 14050269 Howard
* @since 2016年7月8日 下午6:20:12
*/
@Component
public class ServiceFactory implements ApplicationContextAware {

	private static ApplicationContext applicationContext;
	

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ServiceFactory.applicationContext = applicationContext;
	}
	
	public static ApplicationContext getApplicationContext() {
		return ServiceFactory.applicationContext;
	}


	public static <T> T getBean(Class<T> requiredType) {
		return applicationContext.getBean(requiredType);
	}

	public static Object getBean(String name) {
		return applicationContext.getBean(name);
	}

}

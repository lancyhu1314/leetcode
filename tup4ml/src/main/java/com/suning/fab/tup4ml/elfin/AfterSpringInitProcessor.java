package com.suning.fab.tup4ml.elfin;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.suning.fab.model.constant.CommonConstant;
import com.suning.fab.tup4ml.service.ServiceTemplate;
import com.suning.fab.tup4ml.service.SubTccBeanExecuter;
import com.suning.fab.tup4ml.service.SubTccServiceTemplate;
import com.suning.fab.tup4ml.service.TotalTccBeanExecuter;
import com.suning.fab.tup4ml.service.TotalTccServiceTemplate;
import com.suning.fab.tup4ml.utils.AnotUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.RsfConsumerUtil;
import com.suning.fab.tup4ml.utils.ServiceReadyFlag;
import com.suning.rsf.consumer.ServiceLocator;
import com.suning.rsf.provider.annotation.Implement;


/**
 * 当Spring容器启动完成后执行下面的这个Bean
 * @author 16030888
 *
 */
@Component
@Scope("singleton")
public class AfterSpringInitProcessor implements ApplicationListener<ContextRefreshedEvent> {
	private static final String CONTRACT = "contract";
	private static final String IMPL_CODE = "implCode";

	//动态注册bean到spring里面；
	private static void dynamicRegisterTccBean(Class<?> beanClass, String beanName, ServiceTemplate instance) {
		//将applicationContext转换为ConfigurableApplicationContext
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) ServiceFactory.getApplicationContext();

		// 获取bean工厂并转换为DefaultListableBeanFactory
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();

		// 通过BeanDefinitionBuilder创建bean定义
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);

		//设置owner，防止多个机器不是机器时，有些服务没有完整起来
		beanDefinitionBuilder.addPropertyValue("owner",instance);
		
		// 注册bean
		defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {		
		//root application context 没有parent ，它包含其他子容器，
		//因此这里做整个spring bean容器完成初始化后的工作
		if (event.getApplicationContext().getParent() == null) {

			//以下实现所有TccServiceTemplate子类的远程代理初始化
			Map<String, ServiceTemplate> srvTemplate = ServiceFactory.getApplicationContext().getBeansOfType(ServiceTemplate.class, true, true);
			Iterator<Entry<String, ServiceTemplate>> entries = srvTemplate.entrySet().iterator(); 
			while (entries.hasNext()) {
				Entry<String, ServiceTemplate> entry = entries.next(); 
				
				ServiceTemplate instance = entry.getValue();
				String className;
				String beanName;				
				if(TotalTccServiceTemplate.class.isAssignableFrom(instance.getClass())) {
					className = instance.getClass().getSimpleName() + "TotalTccBeanExecuter";
					beanName = className.substring(0, 1).toLowerCase() + className.substring(1);		
					dynamicRegisterTccBean(TotalTccBeanExecuter.class, beanName, instance);			
				}else if(SubTccServiceTemplate.class.isAssignableFrom(instance.getClass())) {
					className = instance.getClass().getSimpleName() + "SubTccBeanExecuter";
					beanName = className.substring(0, 1).toLowerCase() + className.substring(1);
					dynamicRegisterTccBean(SubTccBeanExecuter.class, beanName, instance);					
				}
				
				Implement implement = AnotUtil.findAnnotation(instance.getClass(), Implement.class);
				// 本地服务不缓存
				if (null == implement) {
				    continue;
				}

				Map<String, Object> annotationMap = AnotUtil.getAnnotationAttributes(implement, true);
				String implCode = (String) annotationMap.get(IMPL_CODE);
				String contract = (String) annotationMap.get(CONTRACT);
				if(implCode.isEmpty() && contract.isEmpty()){
					throw new IllegalArgumentException(CommonConstant.ExceptionConstant.ARGUMENTNOTSATISFY);
				}
				String key = contract + "." + implCode;
				RsfConsumerUtil.putServiceAgent(key, ServiceLocator.getServiceAgent(contract, implCode, false));
			}
			//所有服务准备好之后，设置标识让外部可以发起请求服务
			ServiceReadyFlag.getCountDown().countDown();			
			LoggerUtil.info("after Spring up then do processor finished");
		}
	}
}

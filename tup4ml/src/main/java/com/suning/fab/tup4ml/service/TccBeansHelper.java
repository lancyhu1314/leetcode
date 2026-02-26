package com.suning.fab.tup4ml.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.tup4ml.elfin.ServiceFactory;

/**
 * 用于辅助创建TCC的Bean对象，以方便spring注入并实现AOP。
 * 维护一个字典列表，对应每个具体服务的3个TCC接口，
 * 因为TCC后面会调用提交或回滚，所以维持对象实例的存在是必要的；
 * @author 16030888
 *
 */
abstract class TccBeansHelper {
	private static Map<String, TccBeanExecuter> tccBeans = new HashMap<>();

	private TccBeansHelper() {
		throw new IllegalStateException("TccBeansHelper class");
	}

	/**
	 * 获取对应单例TccServiceTemplate子类的TCC类实例，该实例是个bean对象；
	 * @param instance 单例TccServiceTemplate子类，完成某个rsf服务；
	 * @param entryTcc 是否是TCC总事务入口：true -- 总事务入口；false -- 其他；
	 * @return 返回spring bean对象实例的TccBeanImpl；
	 */
	public static TccBeanExecuter getTccBean(ServiceTemplate instance, boolean entryTcc) {
		String key = instance.getClass().getSimpleName();
		TccBeanExecuter tccImpl = tccBeans.get(key);
		if(null == tccImpl){
			String className;
			String beanName;
			if(entryTcc) {
				className = key + "TotalTccBeanExecuter";
				beanName = className.substring(0, 1).toLowerCase() + className.substring(1);
			}else {
				className = key + "SubTccBeanExecuter";
				beanName = className.substring(0, 1).toLowerCase() + className.substring(1);
			}
			tccImpl = (TccBeanExecuter) ServiceFactory.getBean(beanName);
			tccImpl.setOwner(instance);
			tccBeans.put(key, tccImpl);
		}
		return tccImpl;
	}
}

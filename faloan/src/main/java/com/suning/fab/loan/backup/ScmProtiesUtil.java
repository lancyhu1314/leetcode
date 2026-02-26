package com.suning.fab.loan.backup;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.framework.scm.client.SCMClientImpl;
import com.suning.framework.scm.client.SCMListener;
import com.suning.framework.scm.client.SCMNodeImpl;

@Service
public class ScmProtiesUtil implements InitializingBean{

	//scm配置都以属性文件的格式生成与使用
	protected static Properties properties = new Properties();

	private static SCMNodeImpl hbaseTableConf = null;

	@Override
	public synchronized void afterPropertiesSet() throws Exception {

		if (hbaseTableConf == null) {
			// 配置变化后业务处理
			hbaseTableConf = (SCMNodeImpl) SCMClientImpl.getInstance().getConfig(ConstantDeclare.HBASE.HBASENODENAME);
			hbaseTableConf.sync();
			//初始化hbaseTableConf的值
			initialHtableConfMap(hbaseTableConf.getValue());
			hbaseTableConf.monitor(new SCMListener() {
				@Override
				public void execute(String oldValue, String newValue) {
					LoggerUtil.info("hbaseTableConf节点发生变化,变化前的值为:{},变化后的值为:{}", oldValue, newValue);
					//SCM上配置发生变化时增加业务需求
					//配置变化时更新hbaseTableConf
					initialHtableConfMap(newValue);
				}
			});
		}
	}

	/**
	 * 解析从scm获取的字符串到properties
	 * @param content	解析对象
	 */
	public static synchronized void initialHtableConfMap(String content){
		if(null == content || content.isEmpty())
			return;

		StringReader stringReader = new StringReader(content);
		try {
			if (properties == null) {
				properties = new Properties();
			}
			properties.clear();
			properties.load(stringReader);
			//修改线程数
			ThreadPoolUtil.setAsyncCallPoolSize(Integer.valueOf(getProperty("ThreadPoolMaxSize","10")));
		} catch (IOException e) {
			LoggerUtil.error("scm config file Hbase load Properties IOException: {}", e);
		}
	}

	/**
	 * 获取属性值
	 * @param key			属性名称
	 * @param defaultValue	默认值
	 * @return
	 */
	public static String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

}

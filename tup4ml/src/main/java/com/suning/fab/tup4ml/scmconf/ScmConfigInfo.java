/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: ScmConfigInfo.java
 * Author:   17060915
 * Date:     2018年3月14日 下午8:11:04
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.scmconf;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import com.suning.fab.tup4ml.elfin.ServiceFactory;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.framework.scm.client.SCMClient;
import com.suning.framework.scm.client.SCMClientFactory;
import com.suning.framework.scm.client.SCMListener;
import com.suning.framework.scm.client.SCMNode;

/**
 * scm配置文件实体类
 * 
 * @author 17060915
 * @since 2018年3月14日下午8:11:04
 * @version 1.0
 */
public class ScmConfigInfo {

	//scm配置文件；
    private String scmFileName;
    
    private Properties properties;

    public ScmConfigInfo(String scmFileName) {
        this.scmFileName = scmFileName;
    }

    /**
     * scm回调函数
     */
    private final SCMListener configListener = (oldValue, newValue) -> {
        Properties propertiesInfo = loadPropertieFromScm(newValue);
        ScmDynaGetterUtil.scmConfigInfoCache.get(scmFileName).setProperties(propertiesInfo);
        
        // 循环实现了IScmExtraAction接口的
        String [] beanNams = ServiceFactory.getApplicationContext().getBeanNamesForType(IScmExtraAction.class);
        for(String beanName : beanNams){
            IScmExtraAction scmExtraAction = (IScmExtraAction) ServiceFactory.getBean(beanName);
            scmExtraAction.doExtraAction(scmFileName,propertiesInfo);
        }
    };

    /**
     * 初始化该配置文件的相关类
     * @return 成功返回true；失败返回false；
     */
    public boolean init() {
        if (scmFileName.lastIndexOf(".properties") == -1) {
            LoggerUtil.error("scm util only support properties file!!!!");
            return false;
        }

        SCMClient scmClient = SCMClientFactory.getSCMClient();
        SCMNode configNode = scmClient.getConfig(scmFileName);
        configNode.sync();
        String configValue = configNode.getValue();

        if (configValue == null || "".equals(configValue.trim())) {
            LoggerUtil.error("can't find " + scmFileName + " config or config content is empty.");
            return false;
        }

        setProperties(loadPropertieFromScm(configValue));
        configNode.monitor(configNode.getValue(), configListener);
        return true;
    }

    /**
     * 将从scm中读取的值输出到properties，以备后续使用
     * @param configValue 读取到的scm配置字符串；
     * @return 返回对应属性类；
     */
    private Properties loadPropertieFromScm(String configValue) {
        StringReader stringReader = new StringReader(configValue);
        Properties properties = null;
        try {
            properties = new Properties();
            properties.clear();
            properties.load(stringReader);
        } catch (IOException e) {
            LoggerUtil.error("scm config file {} load Properties IOException: {}", scmFileName, e);
        }
        return properties;
    }

    public String getScmFileName() {
        return scmFileName;
    }

    public void setScmFileName(String scmFileName) {
        this.scmFileName = scmFileName;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

}

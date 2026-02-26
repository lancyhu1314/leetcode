/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: ScmProperiesReader.java
 * Author:   17060915
 * Date:     2018年1月23日 下午3:18:58
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.scmconf;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;

import com.suning.fab.fatapclient.util.LoggerUtil;

/**
 *
 * @author 17060915
 * @date 2018年1月23日下午3:18:58
 * @since 1.0
 */
public class ScmProperiesReader extends AbstractScmConf  implements InitializingBean {

    //scm配置都以属性文件的格式生成与使用
    protected Properties properties = new Properties();
    
    @Override
    protected void formatReader() {
        
        StringReader stringReader = new StringReader(getNode().getValue());
        try {
            if (properties == null) {
                properties = new Properties();
            }
            properties.clear();
            properties.load(stringReader);
            
            reinitConf();
        } catch (IOException e) {
            LoggerUtil.error("scm config file {} load Properties IOException: {}", scmPath, e);
        }
    }

    @Override
    protected void reinitConf() {
        
    }

    /**  
     * 获取properties  
     * @return properties  
     */
    public Properties getProperties() {
        return properties;
    }

    /**  
     * 设置properties  
     * @param properties  
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
    
}

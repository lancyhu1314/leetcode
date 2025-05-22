/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: ScmDynaGetterUtil.java
 * Author:   17060915
 * Date:     2018年3月14日 下午3:55:10
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.scmconf;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.suning.fab.tup4ml.exception.FabException;

/**
 * scm动态获取值工具类
 * 
 * @author 17060915
 * @since 2018年3月14日下午3:55:10
 * @version 1.0
 */
public abstract class ScmDynaGetterUtil {

    public static final Map<String, ScmConfigInfo> scmConfigInfoCache = new ConcurrentHashMap<>();

    private static final Object MUX = new Object();

    /**
     * 根据配置文件名称直接从scm中获取key的值,如果获取不到值就返回默认值
     *
     * @param scmFileName scm上配置的文件名称
     * @param keyName 配置文件中的key名称
     * @param defaultValue 如果取不到key值时的默认值
     * @return 返回字符串值；
     * @throws FabException faepp异常；
     * @version 1.0
     */
    public static String getWithDefaultValue(final String scmFileName, String keyName, String defaultValue) {
        Properties properties = getProperties(scmFileName);
        return properties == null ? null :properties.getProperty(keyName,defaultValue);
    }
    
    /**
     * 根据配置文件名称直接从scm中获取key的值,获取不到返回null
     *
     * @param scmFileName scm上配置的文件名称
     * @param keyName 配置文件中的key名称
     * @return 返回字符串值；
     * @throws FabException  faepp异常；
     * @version 1.0
     */
    public static String getValue(final String scmFileName, String keyName) {
        
        return getWithDefaultValue(scmFileName,keyName,null);
    }
    
    
    /**
     * 获取properties
     *
     * @param scmFileName
     * @return 返回属性类；
     * @throws FabException  faepp异常；
     * @version 1.0
     */
    public static Properties getProperties(final String scmFileName) {
        
        // 如果为空则新建
        if (!scmConfigInfoCache.containsKey(scmFileName)) {
            synchronized (MUX) {
                if(!scmConfigInfoCache.containsKey(scmFileName)){
                    ScmConfigInfo scmConfigInfo = new ScmConfigInfo(scmFileName);
                    if(scmConfigInfo.init()){
                        scmConfigInfoCache.put(scmFileName, scmConfigInfo);
                    }else {
                        return null;
                    }
                }
            }
        } 
        
        return scmConfigInfoCache.get(scmFileName).getProperties();
    }

}

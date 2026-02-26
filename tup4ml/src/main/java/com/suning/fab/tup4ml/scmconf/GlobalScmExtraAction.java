/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: GlobalScmExtraAction.java
 * Author:   17060915
 * Date:     2018年8月18日 上午10:21:41
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.scmconf;

import java.util.Properties;

import org.springframework.stereotype.Component;

import com.suning.fab.tup4ml.utils.LogRuleOutUtil;
import com.suning.fab.tup4ml.utils.LoggerUtil;
import com.suning.fab.tup4ml.utils.ThreadPoolUtil;

/**
 * GlobalScm中更改活动线程大小AND 更改日志排除字段
 * 
 * @author 17060915
 * @date 2018年8月18日上午10:21:41
 * @since 1.0
 */
@Component
public class GlobalScmExtraAction implements IScmExtraAction{

    @Override
    public void doExtraAction(String scmFileName,Properties propertiesInfo) {
        if("GlobalScm.properties".equals(scmFileName)){
            String threadPoolMaxSize = propertiesInfo.getProperty("ThreadPoolMaxSize");
            if(null != threadPoolMaxSize){
                ThreadPoolUtil.setPoolSize(Integer.parseInt(threadPoolMaxSize));
            }
            LoggerUtil.info("set threadPoolMaxSize success");
            
            String logRuleOut = propertiesInfo.getProperty("logRuleOut");
            if (null != logRuleOut) {
            	LogRuleOutUtil.setLogFilter(logRuleOut);
			}
            LoggerUtil.info("set logRuleOut success");
        }
    }
}

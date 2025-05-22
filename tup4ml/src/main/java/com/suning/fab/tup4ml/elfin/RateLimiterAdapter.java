/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: RateLimiterAdapter.java
 * Author:   17060915
 * Date:     2018年5月16日 上午10:46:21
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.elfin;

import org.springframework.stereotype.Component;

import com.suning.fab.fatapclient.base.BaseRateLimiter;
import com.suning.fab.fatapclient.bean.DroolsParseRuleInfo;
import com.suning.fab.fatapclient.exception.RateLimiterControlException;
import com.suning.fab.fatapclient.util.LoggerUtil;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.exception.FabRateLimiterException;

/**
 * 流控适配器
 * @author 17060915
 * @date 2018年5月16日上午10:46:21
 * @since 1.0
 */
@Component
public class RateLimiterAdapter implements IRateLimiter
{
    private static final BaseRateLimiter baseRateLimiter = new BaseRateLimiter();
    
    /**
     * true代表触发流控 false代表不需要触发流控
     */
    @Override
    public AbstractDatagram handleRateLimited(AbstractDatagram param) throws FabRateLimiterException {
        
        try {
            DroolsParseRuleInfo droolsParseRuleInfo = baseRateLimiter.execute(param);
            
            if(null == droolsParseRuleInfo){
                return PlatConstant.RSPVALUE.NOTSET;
            }
            
        } catch (RateLimiterControlException e) {
            // 出现流控异常
            throw new FabRateLimiterException("SPS141",e.getCode());
        } catch (Exception e) {
            // 其他异常暂时放过
            LoggerUtil.error("handle rate limiter occure error: {}",e.getMessage());
        }
        
        return PlatConstant.RSPVALUE.NOTSET;
    }

}

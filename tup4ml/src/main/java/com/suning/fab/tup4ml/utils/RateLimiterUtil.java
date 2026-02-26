package com.suning.fab.tup4ml.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.elfin.IRateLimiter;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.elfin.ServiceFactory;
import com.suning.fab.tup4ml.exception.FabRateLimiterException;

@Component
public class RateLimiterUtil {
	@Autowired(required=false)
	private IRateLimiter rateLimiter;
	
	public RateLimiterUtil() {
		//do nothing
	}
	
	//实现是否限流
	public static AbstractDatagram handleRateLimited(AbstractDatagram param) throws FabRateLimiterException {
		RateLimiterUtil rateLimitUtil = ServiceFactory.getBean(RateLimiterUtil.class);
		if(null != rateLimitUtil.rateLimiter) {
			return rateLimitUtil.rateLimiter.handleRateLimited(param);
		}
		return PlatConstant.RSPVALUE.NOTSET;
	}
}

package com.suning.fab.tup4ml.elfin;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.exception.FabRateLimiterException;

/**
 * 如果要实现限流，在应用层实现此接口，并注入到spring中即可；
 * @author 16030888
 *
 */
public interface IRateLimiter {
	/**
	 * 流控处理，如果需要流控，抛出流控异常FabRateLimiterException；
	 * @param param 入参，用于流控处理；
	 * @return 如果返回PlatConstant.RSPVALUE.NOTSET (即null)表示不需要任何特殊处理，否则返回的是对外报文；
	 * @throws FabRateLimiterException 表示需要流控；
	 */
	public AbstractDatagram handleRateLimited(AbstractDatagram param) throws FabRateLimiterException;
}

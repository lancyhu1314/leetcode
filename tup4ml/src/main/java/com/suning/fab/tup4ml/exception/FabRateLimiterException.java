package com.suning.fab.tup4ml.exception;

/**
 * 限流异常，需要限流时抛出该异常；
 * @author 16030888
 *
 */
public class FabRateLimiterException extends FabException {
	private static final long serialVersionUID = 1L;
	
	public FabRateLimiterException(Throwable cause, String errCode, Object...args) {
		super(cause, errCode, args);
	}

	public FabRateLimiterException(String errCode, Object...args) {
		super(errCode, args);
	}

}

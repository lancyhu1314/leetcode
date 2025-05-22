package com.suning.fab.tup4ml.exception;

/**
 * 账务核心通用业务异常类（运行时异常），要参阅“错误码”表；<br/>
 * 这是在生产阶段产生的异常，只返回给前端调用错误码及错误消息；<br/>
 * 编码测试阶段就应该发现的异常，请使用java标准异常；<br/>
 * 默认错误码为999999； <br/>
 * @author 16030888
 *
 */
public class FabRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final FabExceptionHandler expHandler;
	
	public FabRuntimeException(Throwable cause, String errCode, Object...args) {
		super(cause);
		this.expHandler = new FabExceptionHandler(cause, errCode, args);
	}
		
	public FabRuntimeException(String errCode, Object...args) {
		this.expHandler = new FabExceptionHandler(errCode, args);
	}

	public void setErrCode(String errCode) {
		this.expHandler.setErrCode(errCode);
	}

	public void setErrMsg(String errMsg) {
		this.expHandler.setErrMsg(errMsg);
	}

	public String getErrMsg() {
		return expHandler.getErrMsg();
	}

	public String getErrCode() {
		return expHandler.getErrCode();
	}
	
	/**
	 * 根据错误码去配置文件获取对应的格式化字符串
	 * 先去common_error.properties找，找不到再去error.properties找；
	 */
	@Override
	public String getMessage() {		
		return expHandler.getErrMsg();
	}
}

package com.suning.fab.tup4ml.exception;

import java.io.Serializable;

import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.utils.GetPropUtil;
import com.suning.fab.tup4ml.utils.SceneUtil;
import com.suning.fab.tup4ml.utils.StringUtil;

/**
 * 实际处理异常的类，可以组合于运行时异常或者非运行时异常；
 * @author 16030888
 *
 */
class FabExceptionHandler implements Serializable {
	private static final long serialVersionUID = 1L;

	//错误码，默认999999
	protected String errCode = PlatConstant.RSPCODE.UNKNOWN;
	
	//错误参数，用于格式化错误信息；
	//依赖于抛出异常时所指定错误码格式，比如“EPP146”
	protected String errMsg = PlatConstant.RSPMSG.UNKNOWN;
	
	public FabExceptionHandler(Throwable cause, String errCode, Object...args){
		this.errCode = errCode;
		this.errMsg = getMessage(args);
		if(null == this.errMsg) {
			this.errMsg = cause.getMessage();
		}
	}

	public FabExceptionHandler(String errCode, Object...args){
		this.errCode = errCode;
		this.errMsg = getMessage(args);
	}

	public String getErrCode() {
		return errCode;
	}
	
	public String getErrMsg() {
		return errMsg;
	}

	public void setErrCode(String errCode) {
		this.errCode = errCode;
	}

	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}

	/**
	 * 根据错误码去配置文件获取对应的格式化字符串
	 * 先去common_error.properties找，找不到再去error.properties找；
	 */
	protected String getMessage(Object...args) {
		String format= GetPropUtil.getProperty("common_error." + errCode);				
		if(null == format){
			format= GetPropUtil.getPropertyOrDefault("error." + errCode, PlatConstant.RSPMSG.UNKNOWN);
		}
		
		// 这边对平台层的响应码过滤，不在异常信息前带交易名称
		String message;   
		try {
    		Object [] objectNew = null;
    		if(!("SPS141".equals(errCode) || errCode.startsWith(PlatConstant.RSPCODE.PREFIX) )){
    		    objectNew = new Object[args.length + 1];
    		    objectNew[0] = SceneUtil.getSceneFromThreadLocal();
    	        int i = 0;
    	        for(Object object : args){
    	            objectNew [++ i] = object;
    	        }
            }else{
                objectNew = args;
            }
		
			message = String.format(format, objectNew);

            message = StringUtil.appendSerialNo(message);

		}catch(Exception e){
			message = format;
		}
		return message;
	}
}

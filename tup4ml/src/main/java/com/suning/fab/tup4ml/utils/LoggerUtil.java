
package com.suning.fab.tup4ml.utils;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.transaction.TransactionTimedOutException;

import com.alibaba.fastjson.JSON;
import com.suning.dtf.common.exception.BusinessIdExistException;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.elfin.Pair;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.exception.FabRuntimeException;
import com.suning.fab.tup4ml.exception.FabSqlException;

/**
 * 生产FABLOG日志和MONITOR日志的辅助类
 * @author 14050269 Howard
 * @since 2016年5月31日 下午9:26:03
 */
public abstract class LoggerUtil {
	private LoggerUtil() {
		throw new IllegalStateException("LoggerUtil class");
	}

	private static 	Logger logger = LoggerFactory.getLogger("FABLOG");
	private static Logger monitor = LoggerFactory.getLogger("MONITOR");
	public static void monitor(String arg0, Object... arg1) {
		monitor.info(arg0, arg1);
	}

	/**
	 * 打印异常相关信息到日志中；
	 * @param outerSerialNumber 外部单号，用于串联本次交易的所有日志；
	 * @param classSimpleName 交易类名；
	 * @param e 需要打印到日志的相关异常；
	 * @return 返回解析出来的Pair<错误码, 错误信息>；
	 */
	public static Pair<String, String> logException(String outerSerialNumber, String classSimpleName, Exception e, AbstractDatagram param){
	    boolean errorFlag = false;
		String errCode;
		String errMsg = e.getMessage();
		if((null != e.getCause()) && (e.getCause() instanceof TransactionTimedOutException)) {//事务超时异常
            errCode = PlatConstant.RSPCODE.TRANSACTION_TIME_OUT;

            errMsg = formatMsg(param, errCode, true);
			errMsg = StringUtil.appendSerialNo(errMsg);
		}else if(e instanceof FabSqlException) {//sql异常
			FabSqlException fabSqlExp = (FabSqlException)e;
            if (null != fabSqlExp.getSqlState() && PlatConstant.RSPCODE.TRANSACTION_RATE_LIMIT.equals(fabSqlExp.getSqlState())) {
                errCode = PlatConstant.RSPCODE.TRANSACTION_RATE_LIMIT;
                errMsg = "DB" + formatMsg(param, errCode, true);
                errMsg = StringUtil.appendSerialNo(errMsg);
            } else {
                errCode = fabSqlExp.getErrCode();
                errMsg = StringUtil.appendSerialNo(fabSqlExp.getMessage());
            }
		}else if(e instanceof BusinessIdExistException) {
			errCode = PlatConstant.RSPCODE.DTF_PRIMARY_CONFILCT;
			errMsg = formatMsg(param, errCode, false);
		}else if(e instanceof FabException){
			errCode = ((FabException)e).getErrCode();
		}else if(e instanceof FabRuntimeException){
			errCode = ((FabRuntimeException)e).getErrCode();
		}else{
			errCode = PlatConstant.RSPCODE.UNKNOWN;
			errMsg = e.getMessage();

			errorFlag = true;
		}
		LoggerUtil.error("交易【{}】出现异常，错误码【{}】 | OuterSerialNumber【{}】", classSimpleName, errCode, outerSerialNumber);
		if(e instanceof FabSqlException){
			FabSqlException sqlExp = (FabSqlException)e;
			LoggerUtil.error("错误信息 | OuterSerialNumber【{}】：{}；【sqlId={}，sqlCode={}】", outerSerialNumber, errMsg, sqlExp.getSqlId(), sqlExp.getSqlCode());
		}else{
		    
		    @SuppressWarnings("unchecked")
            List<String> list = (List<String>) ThreadLocalUtil.get(PlatConstant.PLATCONST.TRANS_CALL_CHAIN);
		    StringBuilder callChain = new StringBuilder();
		    if(null != list){
		        callChain.append(classSimpleName);
		        for(String service: list){
		            callChain.append("-").append(service);
		        }
		        callChain.append("-");
		    }
		    
			LoggerUtil.error("错误信息 | OuterSerialNumber【{}】：{}；", outerSerialNumber, callChain.toString() + errMsg);
		}
		if(errCode.equalsIgnoreCase(PlatConstant.RSPCODE.DBERROR) 
				|| errCode.equalsIgnoreCase(PlatConstant.RSPCODE.UNKNOWN)){
			for(StackTraceElement elem : e.getStackTrace()) {
				LoggerUtil.warn(elem.toString());
			}
		}else{
			LoggerUtil.warn(e.getStackTrace()[0].toString());
		}
		
		if(errorFlag){
		    // 如果不是像业务这些具体知道的异常，则统一返回INTERVAL ERROR，不然异常外漏容易造成安全隐患
            errMsg = PlatConstant.RSPMSG.UNKNOWN;
			errMsg = StringUtil.appendSerialNo(errMsg);
		}
		
		// 最后再加个判断，如果获取到的异常信息为null，则赋默认值
		if(null == errMsg){
			errMsg = PlatConstant.RSPMSG.UNKNOWN;
			errMsg = StringUtil.appendSerialNo(errMsg);
		}

		return new Pair<>(errCode, errMsg);
	}

	/**
	 * 根据返回码格式化返回信息
	 * 功能描述: <br>
	 * 
	 *
	 * @param param
	 * @param errCode
	 * @return
	 * @since 1.0
	 */
    private static String formatMsg(AbstractDatagram param, String errCode, boolean formatFlag) {
        String errMsg;
        String formatMsg = GetPropUtil.getProperty("common_error." + errCode);              
        if(null == formatMsg){
            formatMsg= PlatConstant.RSPMSG.UNKNOWN;
        }

        // 如果不需要格式化直接退出
        if(!formatFlag){
        	return formatMsg;
		}

        try {
            errMsg = String.format(formatMsg, null == param.getRouteId() ? "" : param.getRouteId());
        } catch (Exception e2) {
            errMsg = formatMsg;
        }
        return errMsg;
    }

	/**
	 * 打印monitor日志；
	 * @param tranCode 交易码；
	 * @param serialNo 流水号；
	 * @param channelId 渠道ID；
	 * @param rspCode 响应码；
	 * @param rspMsg 响应消息；
	 * @param startInterval 耗时开始计时，单位毫秒；
	 */
	public static void logMonitor(String tranCode, String serialNo,String uuid, String channelId, String rspCode, String rspMsg, Long startInterval) {
		Long endInterval = System.currentTimeMillis();
		LinkedHashMap<String, Object> monitor = new LinkedHashMap<>();
		monitor.put("method", tranCode);
		Long interval = endInterval - startInterval;
		monitor.put("time", interval.toString());
		monitor.put("tradeChannel", channelId);
		monitor.put("code", rspCode);
		monitor.put("responseStatus", rspMsg);
		String localIP = "";
		try{
			localIP = InetAddress.getLocalHost().getHostAddress();
		}catch(Exception e){
			LoggerUtil.warn("InetAddress.getLocalHost().getHostAddress() error:{}", e);
		}
		monitor.put("ipAddress", localIP);
		monitor.put("orderNum", serialNo);
		String serviceName = "Unknow";
		try {
			serviceName =  GetPropUtil.getProperty("main-setting.serviceName");
		} catch (Exception e) {
			LoggerUtil.warn("获取服务名称配置异常【main-setting.properties】-->【serviceName】：{}", e);
		}
		LoggerUtil.monitor("{}|prefix={}{}", uuid, serviceName, JSON.toJSONString(monitor));
	}

	public static void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
		logger.debug(arg0, arg1, arg2, arg3);
	}
	public static void debug(Marker arg0, String arg1, Object... arg2) {
		logger.debug(arg0, arg1, arg2);
	}
	public static void debug(Marker arg0, String arg1, Object arg2) {
		logger.debug(arg0, arg1, arg2);
	}
	public static void debug(Marker arg0, String arg1, Throwable arg2) {
		logger.debug(arg0, arg1, arg2);
	}
	public static void debug(Marker arg0, String arg1) {
		logger.debug(arg0, arg1);
	}
	public static void debug(String arg0, Object arg1, Object arg2) {
		logger.debug(arg0, arg1, arg2);
	}
	public static void debug(String arg0, Object... arg1) {
		logger.debug(arg0, arg1);
	}
	public static void debug(String arg0, Object arg1) {
		logger.debug(arg0, arg1);
	}
	public static void debug(String arg0, Throwable arg1) {
		logger.debug(arg0, arg1);
	}
	public static void debug(String arg0) {
		logger.debug(arg0);
	}
	public static void error(Marker arg0, String arg1, Object arg2, Object arg3) {
		logger.error(arg0, arg1, arg2, arg3);
	}
	public static void error(Marker arg0, String arg1, Object... arg2) {
		logger.error(arg0, arg1, arg2);
	}
	public static void error(Marker arg0, String arg1, Object arg2) {
		logger.error(arg0, arg1, arg2);
	}
	public static void error(Marker arg0, String arg1, Throwable arg2) {
		logger.error(arg0, arg1, arg2);
	}
	public static void error(Marker arg0, String arg1) {
		logger.error(arg0, arg1);
	}
	public static void error(String arg0, Object arg1, Object arg2) {
		logger.error(arg0, arg1, arg2);
	}
	public static void error(String arg0, Object... arg1) {
		logger.error(arg0, arg1);
	}
	public static void error(String arg0, Object arg1) {
		logger.error(arg0, arg1);
	}
	public static void error(String arg0, Throwable arg1) {
		logger.error(arg0, arg1);
	}
	public static void error(String arg0) {
		logger.error(arg0);
	}
	public static String getName() {
		return logger.getName();
	}
	public static void info(Marker arg0, String arg1, Object arg2, Object arg3) {
		logger.info(arg0, arg1, arg2, arg3);
	}
	public static void info(Marker arg0, String arg1, Object... arg2) {
		logger.info(arg0, arg1, arg2);
	}
	public static void info(Marker arg0, String arg1, Object arg2) {
		logger.info(arg0, arg1, arg2);
	}
	public static void info(Marker arg0, String arg1, Throwable arg2) {
		logger.info(arg0, arg1, arg2);
	}
	public static void info(Marker arg0, String arg1) {
		logger.info(arg0, arg1);
	}
	public static void info(String arg0, Object arg1, Object arg2) {
		logger.info(arg0, arg1, arg2);
	}
	public static void info(String arg0, Object... arg1) {
		logger.info(arg0, arg1);
	}
	public static void info(String arg0, Object arg1) {
		logger.info(arg0, arg1);
	}
	public static void info(String arg0, Throwable arg1) {
		logger.info(arg0, arg1);
	}
	public static void info(String arg0) {
		logger.info(arg0);
	}
	public static boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}
	public static boolean isDebugEnabled(Marker arg0) {
		return logger.isDebugEnabled(arg0);
	}
	public static boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}
	public static boolean isErrorEnabled(Marker arg0) {
		return logger.isErrorEnabled(arg0);
	}
	public static boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}
	public static boolean isInfoEnabled(Marker arg0) {
		return logger.isInfoEnabled(arg0);
	}
	public static boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}
	public static boolean isTraceEnabled(Marker arg0) {
		return logger.isTraceEnabled(arg0);
	}
	public static boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}
	public static boolean isWarnEnabled(Marker arg0) {
		return logger.isWarnEnabled(arg0);
	}
	public static void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
		logger.trace(arg0, arg1, arg2, arg3);
	}
	public static void trace(Marker arg0, String arg1, Object... arg2) {
		logger.trace(arg0, arg1, arg2);
	}
	public static void trace(Marker arg0, String arg1, Object arg2) {
		logger.trace(arg0, arg1, arg2);
	}
	public static void trace(Marker arg0, String arg1, Throwable arg2) {
		logger.trace(arg0, arg1, arg2);
	}
	public static void trace(Marker arg0, String arg1) {
		logger.trace(arg0, arg1);
	}
	public static void trace(String arg0, Object arg1, Object arg2) {
		logger.trace(arg0, arg1, arg2);
	}
	public static void trace(String arg0, Object... arg1) {
		logger.trace(arg0, arg1);
	}
	public static void trace(String arg0, Object arg1) {
		logger.trace(arg0, arg1);
	}
	public static void trace(String arg0, Throwable arg1) {
		logger.trace(arg0, arg1);
	}
	public static void trace(String arg0) {
		logger.trace(arg0);
	}
	public static void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
		logger.warn(arg0, arg1, arg2, arg3);
	}
	public static void warn(Marker arg0, String arg1, Object... arg2) {
		logger.warn(arg0, arg1, arg2);
	}
	public static void warn(Marker arg0, String arg1, Object arg2) {
		logger.warn(arg0, arg1, arg2);
	}
	public static void warn(Marker arg0, String arg1, Throwable arg2) {
		logger.warn(arg0, arg1, arg2);
	}
	public static void warn(Marker arg0, String arg1) {
		logger.warn(arg0, arg1);
	}
	public static void warn(String arg0, Object arg1, Object arg2) {
		logger.warn(arg0, arg1, arg2);
	}
	public static void warn(String arg0, Object... arg1) {
		logger.warn(arg0, arg1);
	}
	public static void warn(String arg0, Object arg1) {
		logger.warn(arg0, arg1);
	}
	public static void warn(String arg0, Throwable arg1) {
		logger.warn(arg0, arg1);
	}
	public static void warn(String arg0) {
		logger.warn(arg0);
	}
}

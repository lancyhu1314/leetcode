
package com.suning.fab.tup4ml.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.suning.fab.tup4ml.exception.FabRuntimeException;

/**
* 时间日期格式化辅助类
* @author 14050269 Howard
* @since 创建时间：2016年6月2日 上午1:15:05
*/
public abstract class DateUtil {
	private static final String DATEFORMAT = "yyyy-MM-dd";
	private static final String TIMEFORMAT = "HHmmss";
	private static final String TIMESTAMPFORMAT = "yyyy-MM-dd HH:mm:ss,SSS";

	private DateUtil() {
		throw new IllegalStateException("DateUtil class");
	}
	
	public static String dateToString(Date date) {
	    if(null == date){
            return null;
        }
		return new SimpleDateFormat(DATEFORMAT).format(date);
	}
	
	public static Date stringToDate(String str) {
		try {
			return new SimpleDateFormat(DATEFORMAT).parse(str);
		} catch (ParseException e) {
			throw new FabRuntimeException(e, "TUP100");
		}
	}
	
	public static String timeToString(Date date)	{
	    if(null == date){
	        return null;
	    }
		return new SimpleDateFormat(TIMEFORMAT).format(date);		
	}
	
	public static Date stringToTime(String str) {
		try {
			return new SimpleDateFormat(TIMEFORMAT).parse(str);
		} catch (ParseException e) {
			throw new FabRuntimeException(e, "TUP101");
		}	
	}
	
	public static String currentTimestamp() {
			return new SimpleDateFormat(TIMESTAMPFORMAT).format(new Date());
	}

	public static String timestampToString(Date date) {
			return new SimpleDateFormat(TIMESTAMPFORMAT).format(date);
	}

	public static String currentDate() {
			return new SimpleDateFormat(DATEFORMAT).format(new Date());
	}

	public static String currentTime() {
			return new SimpleDateFormat(TIMEFORMAT).format(new Date());
	}
}

package com.suning.fab.faibfp.utils;

import com.suning.fab.mulssyn.exception.FabException;
import com.suning.fab.mulssyn.exception.FabRuntimeException;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    private DateUtils() {
    }

    /**
     * 比较传过来的两个日期
     *
     * @return
     */
    public static Date getOtherDate(Date date, int i) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, i);
        return calendar.getTime();

    }

    public static Date stringToDate(String str) {
        try {
            return null == str ? null : new SimpleDateFormat("yyyy-MM-dd").parse(str);
        } catch (ParseException var2) {
            throw new FabRuntimeException(var2, "TUP100", new Object[0]);
        }
    }

    public static String dateToString(Date date) {
        if (null == date) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    public static boolean isSameDay(Date date1, Date date2) throws FabException {
        if (date1 != null && date2 != null) {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(date1);
            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(date2);
            return isSameDay(cal1, cal2);
        } else {
            throw new FabException("999999", "比较的日期不能为空");
        }
    }

    public static boolean isSameDay(Calendar cal1, Calendar cal2) throws FabException {
        if (cal1 != null && cal2 != null) {
            return cal1.get(0) == cal2.get(0) && cal1.get(1) == cal2.get(1) && cal1.get(6) == cal2.get(6);
        } else {
            throw new FabException("999999", "比较的日期不能为空");
        }
    }

    /**
     * 判断day1是否早于day2
     *
     * @param day1
     * @param day2
     * @return day1早于day2, 返回true, day1晚于day2, 返回false
     */
    public static Boolean before(Date day1, Date day2) {
        DateTime d1 = new DateTime(day1);
        DateTime d2 = new DateTime(day2);
        return Days.daysBetween(d1, d2).getDays() > 0;
    }

    /**
     * 获取之前的日期
     *
     * @param date1
     * @param date2
     * @return
     */
    public static Date getBeforeDate(Date date1, Date date2) {
        if (null == date1) {
            return date2;
        }
        if (null == date2) {
            return date1;
        }
        if (before(date1, date2)) return date1;
        return date2;
    }

    /**
     * 获取之后的日期
     *
     * @param date1
     * @param date2
     * @return
     */
    public static Date getAfterDate(Date date1, Date date2) {
        if (null == date1) {
            return date2;
        }
        if (null == date2) {
            return date1;
        }
        if (before(date1, date2)) return date2;
        return date1;
    }
}

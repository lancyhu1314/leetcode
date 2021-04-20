package com.suning.fab.faibfp.utils;

import com.suning.fab.mulssyn.exception.FabException;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Administrator
 */
public class CalendarUtil {
    private CalendarUtil() {
        //nothing to do
    }

    /**
     * 按对年对月对日的方式计算日期差
     *
     * @param begin 开始日期,格式:yyyy-mm-dd
     * @param end   结束日期,格式:yyyy-mm-dd
     * @return 天数
     */
    public static int fixedDaysBetween(String begin, String end) {
        DateTime b = new DateTime(begin);
        DateTime e = new DateTime(end);

        //计算区间天数
        Period p = new Period(b, e, PeriodType.yearMonthDay());
        return p.getYears() * 360 + p.getMonths() * 30 + p.getDays();

    }

    /**
     * 按自然日的方式计算日期差
     *
     * @param begin 开始日期,格式:yyyy-mm-dd
     * @param end   结束日期,格式:yyyy-mm-dd
     * @return 天数
     */
    public static int actualDaysBetween(Date begin, Date end) {
        DateTime b = new DateTime(begin);
        DateTime e = new DateTime(end);

        //计算区间天数
        Period p = new Period(b, e, PeriodType.days());
        return p.getDays();

    }

    /**
     * 按自然日的方式计算日期差
     *
     * @param begin 开始日期,格式:yyyy-mm-dd
     * @param end   结束日期,格式:yyyy-mm-dd
     * @return 天数
     */
    public static int actualDaysBetween(String begin, String end) {
        DateTime b = new DateTime(begin);
        DateTime e = new DateTime(end);

        //计算区间天数
        Period p = new Period(b, e, PeriodType.days());
        return p.getDays();

    }

    /**
     * 计算月差,舍入方式为下滚,即不满一月的部分舍去
     *
     * @param begin 开始日期,格式:yyyy-mm-dd
     * @param end   结束日期,格式:yyyy-mm-dd
     * @return 天数
     */
    public static int monthsBetween(String begin, String end) {
        DateTime b = new DateTime(begin);
        DateTime e = new DateTime(end);

        //计算区间天数
        Period p = new Period(b, e, PeriodType.months());
        return p.getMonths();

    }

    /**
     * 计算自然月月差
     *
     * @param begin 开始日期
     * @param end   结束日期
     */
    public static int monthDifference(String begin, String end) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM");
        DateTime s = formatter.parseDateTime(begin.substring(0, 7));
        DateTime e = formatter.parseDateTime(end.substring(0, 7));
        int months = Months.monthsBetween(s, e).getMonths();
        return months;
    }

    /**
     * 计算年差,舍入方式为下滚,即不满一年的部分舍去
     *
     * @param begin 开始日期,格式:yyyy-mm-dd
     * @param end   结束日期,格式:yyyy-mm-dd
     * @return 天数
     */
    public static int yearsBetween(String begin, String end) {
        DateTime b = new DateTime(begin);
        DateTime e = new DateTime(end);

        //计算区间天数
        Period p = new Period(b, e, PeriodType.years());
        return p.getYears();
    }

    /**
     * 是否年末
     *
     * @param day 日期,格式:yyyy-mm-dd
     * @return 是否
     */
    public static boolean isYearEnd(String day) {
        DateTime d = new DateTime(day);
        return d.dayOfYear().withMaximumValue().isEqual(d);
    }

    /**
     * 是否月末
     *
     * @param day 日期,格式:yyyy-mm-dd
     * @return 是否
     */
    public static boolean isMonthEnd(String day) {
        DateTime d = new DateTime(day);
        int n = d.getDayOfMonth();
        return d.dayOfMonth().withMaximumValue().getDayOfMonth() == n;
    }

    /**
     * 是否上旬末
     *
     * @param day 日期,格式:yyyy-mm-dd
     * @return 是否
     */
    public static boolean is1stXunEnd(String day) {
        DateTime d = new DateTime(day);
        int n = d.getDayOfMonth();
        return n == 10;
    }

    /**
     * 是否中旬末
     *
     * @param day 日期,格式:yyyy-mm-dd
     * @return 是否
     */
    public static boolean is2ndXunEnd(String day) {
        DateTime d = new DateTime(day);
        int n = d.getDayOfMonth();
        return n == 20;

    }

    /**
     * 是否旬末，包括上中下旬末（月末）
     *
     * @param day 日期,格式:yyyy-mm-dd
     * @return 是否
     */
    public static boolean isXunEnd(String day) {
        return is1stXunEnd(day) || is2ndXunEnd(day) || isMonthEnd(day);
    }

    /**
     * 是否季末
     *
     * @param day 日期,格式:yyyy-mm-dd
     * @return 是否
     */
    public static boolean isQuarterEnd(String day) {
        if (!isMonthEnd(day)) {
            return false;
        }
        DateTime d = new DateTime(day);
        int n = d.getMonthOfYear();
        return n % 3 == 0;
    }

    /**
     * 是否半年末
     *
     * @param day 日期,格式:yyyy-mm-dd
     * @return 是否
     */
    public static boolean isHalfyearEnd(String day) {
        if (!isMonthEnd(day)) {
            return false;
        }
        DateTime d = new DateTime(day);
        int n = d.getMonthOfYear();
        return n % 6 == 0;
    }

    /**
     * 计算N天后的到期日
     *
     * @param begin 开始日期
     * @param days  天数
     * @return 到期日
     */
    public static DateTime nDaysAfter(Date begin, int days) {
        DateTime day = new DateTime(begin);
        return day.plusDays(days);
    }

    /**
     * 计算N天前的到期日
     *
     * @param begin 开始日期
     * @param days  天数
     * @return 到期日
     */
    public static DateTime nDaysBefore(String begin, int days) {
        DateTime day = new DateTime(begin);
        return day.minusDays(days);
    }

    /**
     * 计算N月后的到期日
     *
     * @param begin 开始日期
     * @param days  月数
     * @return 到期日
     */
    public static DateTime nMonthsAfter(Date begin, int days) {
        DateTime day = new DateTime(begin);
        return day.plusMonths(days);
    }

    /**
     * 计算N年后的到期日
     *
     * @param begin 开始日期
     * @param days  年数
     * @return 到期日
     */
    public static DateTime nYearsAfter(Date begin, int days) {
        DateTime day = new DateTime(begin);
        return day.plusYears(days);
    }

    /**
     * 计算N年前的到期日
     *
     * @param begin 开始日期
     * @param days  年数
     * @return 到期日
     */
    public static DateTime nYearsBefore(String begin, int days) {
        DateTime day = new DateTime(begin);
        return day.minusYears(days);
    }

    /**
     * 计算N周后的到期日
     *
     * @param begin 开始日期
     * @param days  周数
     * @return 到期日
     */
    public static DateTime nWeeksAfter(String begin, int days) {
        DateTime day = new DateTime(begin);
        return day.plusWeeks(days);
    }

    /**
     * 取得月末日期
     *
     * @param day 基准时间
     * @return 月末日期
     */
    public static DateTime getMonthEnd(String day) {
        DateTime d = new DateTime(day);
        return d.dayOfMonth().withMaximumValue();
    }

    /**
     * 取得年末日期
     *
     * @param day 基准时间
     * @return 年末日期
     */
    public static DateTime getYearEnd(String day) {
        DateTime d = new DateTime(day);
        return d.dayOfYear().withMaximumValue();
    }

    /**
     * 取得旬末日期
     *
     * @param day 基准时间
     * @return 旬末日期
     */
    public static DateTime getXunEnd(String day) {
        DateTime d = new DateTime(day);
        int n = d.getDayOfMonth();
        if (n > 20) {
            return d.dayOfMonth().withMaximumValue();
        }
        int x = 10 - n % 10;
        return d.plusDays(x % 10);
    }

    /**
     * 取得周末日期
     *
     * @param day 基准时间
     * @return 周末日期
     */
    public static DateTime getWeekEnd(String day) {
        DateTime d = new DateTime(day);
        return d.dayOfWeek().withMaximumValue();
    }

    /**
     * 取得半年末日期
     *
     * @param day 基准时间
     * @return 半年末日期
     */
    public static DateTime getHalfyearEnd(String day) {
        DateTime d = new DateTime(day);
        DateTime yearEnd = d.dayOfYear().withMaximumValue();
        if (d.getMonthOfYear() > 6)
            return yearEnd;
        return yearEnd.minusMonths(6);
    }

    /**
     * 取得季末日期
     *
     * @param day 基准时间
     * @return 季末日期
     */
    public static DateTime getQuarterEnd(String day) {
        DateTime d = new DateTime(day);
        DateTime yearEnd = d.dayOfYear().withMaximumValue();
        int n = d.getMonthOfYear() - 1;
        return yearEnd.minusMonths((3 - n / 3) * 3);    //不能简化为9-n，因为是整数除
    }

    /**
     * 当月天数
     *
     * @param day 基准时间
     * @return day所在月的总天数
     */
    public static int totalDayInMonth(String day) {
        DateTime d = new DateTime(day);
        return d.dayOfMonth().getMaximumValue();
    }

    /**
     * 当月天数
     *
     * @param day 基准时间
     * @return day所在月的总天数
     */
    public static DateTime nextMonthHead(String day) {
        DateTime d = new DateTime(day);
        return nextMonthHead(d);
    }

    /**
     * 当月天数
     *
     * @param day 基准时间
     * @return day所在月的总天数
     */
    public static DateTime nextMonthHead(DateTime day) {
        return day.plusMonths(1).dayOfMonth().withMinimumValue();
    }

    /**
     * 指定日
     *
     * @param dt 基准时间
     * @param n
     * @return day所在月的总天数
     */
    public static DateTime optDay(DateTime dt, Integer n) {
        if (dt.dayOfMonth().withMaximumValue().getDayOfMonth() < n)
            return dt.dayOfMonth().withMaximumValue();
        return new DateTime(dt.getYear(), dt.getMonthOfYear(), n, 0, 0);
    }

    /**
     * 如果指定月份为3，则指定日期为  3月  6月  9月 12月
     *
     * @param dt 日期
     * @param n  月份
     * @return
     */
    public static DateTime optMonthPhase(DateTime dt, Integer periodMs, Integer n) {
        DateTime dt1 = optMonth(dt, n);

        Period p = new Period(dt1, dt, PeriodType.yearMonthDay());

        Integer diffMoths = p.getMonths() >= 0 ? p.getMonths() : -p.getMonths();

        dt1 = dt1.plusMonths((diffMoths / periodMs)
                * periodMs);


        return dt1;
    }

    /**
     * @param dt 日期
     * @param n  月份
     * @return
     */
    public static DateTime optMonth(DateTime dt, Integer n) {
        DateTime dtmp = new DateTime(dt.getYear(), n, 1, 0, 0);
        Integer day;
        // 如果本月没有指定日期，则指定该月月末
        if (dtmp.dayOfMonth().withMaximumValue().getDayOfMonth() < dt
                .getDayOfMonth()) {
            day = dtmp.dayOfMonth().withMaximumValue().getDayOfMonth();
        } else {

            day = dt.getDayOfMonth();
        }
        return new DateTime(dt.getYear(), n, day, 0, 0);
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
     * 判断day1是否早于等于day2
     *
     * @param day1
     * @param day2
     * @return day1早于等于day2, 返回true, day1晚于day2, 返回false
     */
    public static Boolean beforeAlsoEqual(Date day1, Date day2) {
        DateTime d1 = new DateTime(day1);
        DateTime d2 = new DateTime(day2);
        return Days.daysBetween(d1, d2).getDays() >= 0;
    }

    /**
     * 判断day1是否晚于day2
     *
     * @param day1
     * @param day2
     * @return day1早于day2, 返回false, day1晚于day2, 返回true
     */
    public static Boolean after(Date day1, Date day2) {
        DateTime d1 = new DateTime(day1);
        DateTime d2 = new DateTime(day2);
        return Days.daysBetween(d1, d2).getDays() < 0;
    }

    /**
     * 判断day1是否晚于等于day2
     *
     * @param day1
     * @param day2
     * @return day1早于day2, 返回false, day1晚于等于day2, 返回true
     */
    public static Boolean afterAlsoEqual(Date day1, Date day2) {
        DateTime d1 = new DateTime(day1);
        DateTime d2 = new DateTime(day2);
        return Days.daysBetween(d1, d2).getDays() <= 0;
    }

    /**
     * 判断day1等于day2
     *
     * @param day1
     * @param day2
     * @return day1等于day2, 返回true, day1不等day2, 返回false
     */
    public static Boolean equalDate(Date day1, Date day2) {
        DateTime d1 = new DateTime(day1);
        DateTime d2 = new DateTime(day2);
        return Days.daysBetween(d1, d2).getDays() == 0;
    }


    /**
     * 判断几月几号是今年第几天
     *
     * @param dateStr
     * @return int
     * @throws ParseException
     */
    public static int dateTranInt(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();

        cal.setTime(sdf.parse(dateStr));

        return cal.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 判断数字是几月几号
     *
     * @param dayNum
     * @param yearNum
     * @return int
     * @throws ParseException
     */
    public static String intTranDate(int dayNum, int yearNum) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, yearNum);
        cal.set(Calendar.DAY_OF_YEAR, dayNum);
        return sdf.format(cal.getTime());
    }

    /**
     * @param timeType     时间类型
     * @param timeInterval 时间间隔
     * @return 天数
     */
    public static String nTimesAfter(Date date, String timeType, Integer timeInterval) throws FabException {

        if ("D".equals(timeType)) {// 日
            return CalendarUtil.nDaysAfter(date, timeInterval)
                    .toString("yyyy-MM-dd");
        } else if ("M".equals(timeType)) {// 月
            return
                    CalendarUtil.nMonthsAfter(date, timeInterval)
                            .toString("yyyy-MM-dd");
        } else if ("Q".equals(timeType)) {// 季
            return
                    CalendarUtil.nMonthsAfter(date, timeInterval * 3)
                            .toString("yyyy-MM-dd");
        } else if ("H".equals(timeType)) {// 半年
            return
                    CalendarUtil.nMonthsAfter(date, timeInterval * 6)
                            .toString("yyyy-MM-dd");
        } else if ("Y".equals(timeType)) {// 年
            return
                    CalendarUtil.nYearsAfter(date, timeInterval)
                            .toString("yyyy-MM-dd");
        }
        throw new FabException("SPS106", timeType);
    }


}

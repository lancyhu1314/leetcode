package com.suning.fab.faibfp.localTest.forcoverage;

import com.suning.fab.faibfp.utils.CalendarUtil;
import com.suning.fab.faibfp.utils.DateUtils;
import com.suning.fab.mulssyn.exception.FabException;
import org.joda.time.DateTime;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/4/20
 * @Version 1.0
 */
public class CalendarUtilTest {

    @Test
    public void test() throws ParseException, FabException {
        String dateStr1 = "2020-05-13";
        String dateStr2 = "2021-09-21";
        Date date1 = DateUtils.stringToDate(dateStr1);
        Date date2 = DateUtils.stringToDate(dateStr2);

        CalendarUtil.actualDaysBetween(DateUtils.stringToDate(dateStr1), DateUtils.stringToDate(dateStr2));
        CalendarUtil.actualDaysBetween(dateStr1, dateStr2);
        CalendarUtil.afterAlsoEqual(DateUtils.stringToDate(dateStr1), DateUtils.stringToDate(dateStr2));
        CalendarUtil.after(DateUtils.stringToDate(dateStr1), DateUtils.stringToDate(dateStr2));
        CalendarUtil.isMonthEnd("2020-12-30");
        CalendarUtil.nDaysAfter(DateUtils.stringToDate(dateStr1), 5);
        CalendarUtil.nMonthsAfter(DateUtils.stringToDate(dateStr1), 2);
        CalendarUtil.nextMonthHead("2020-12-21");
        CalendarUtil.before(DateUtils.stringToDate(dateStr1), DateUtils.stringToDate(dateStr2));
        CalendarUtil.beforeAlsoEqual(DateUtils.stringToDate(dateStr1), DateUtils.stringToDate(dateStr2));
        CalendarUtil.is1stXunEnd(dateStr1);
        CalendarUtil.is2ndXunEnd(dateStr2);
        CalendarUtil.nYearsAfter(DateUtils.stringToDate(dateStr1), 1);
        CalendarUtil.optMonth(new DateTime(dateStr1), 3);
        CalendarUtil.dateTranInt(dateStr1);
        CalendarUtil.equalDate(date1, date2);
        CalendarUtil.fixedDaysBetween(dateStr1, dateStr2);
        CalendarUtil.getHalfyearEnd(dateStr1);
        CalendarUtil.getMonthEnd(dateStr1);
        CalendarUtil.getQuarterEnd(dateStr1);
        CalendarUtil.getWeekEnd(dateStr1);
        CalendarUtil.getXunEnd(dateStr1);
        CalendarUtil.getYearEnd(dateStr1);
        CalendarUtil.intTranDate(100, 2021);
        CalendarUtil.isHalfyearEnd(dateStr1);
        CalendarUtil.isQuarterEnd(dateStr1);
        CalendarUtil.isXunEnd(dateStr1);
        CalendarUtil.isYearEnd(dateStr1);
        CalendarUtil.monthDifference(dateStr1, dateStr2);
        CalendarUtil.monthsBetween(dateStr1, dateStr2);
        CalendarUtil.nDaysBefore(dateStr1, 1);
        CalendarUtil.nTimesAfter(date1, "D", 1);
        CalendarUtil.nTimesAfter(date1, "M", 1);
        CalendarUtil.nTimesAfter(date1, "Q", 1);
        CalendarUtil.nTimesAfter(date1, "H", 1);
        CalendarUtil.nTimesAfter(date1, "Y", 1);
        CalendarUtil.nWeeksAfter(dateStr1, 2);
        CalendarUtil.nYearsBefore(dateStr1, 2);
        CalendarUtil.optDay(new DateTime(dateStr1), 3);
        CalendarUtil.optMonthPhase(new DateTime(dateStr1), 2, 2);
        CalendarUtil.totalDayInMonth(dateStr1);
        CalendarUtil.yearsBetween(dateStr1, dateStr1);


    }

}

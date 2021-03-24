package com.suning.fab.faibfp.utils;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author 19043955
 * @Date 2019/10/17 19:33
 * @Version 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-test.xml"})
public abstract class TestUtil {

    public static final String channelId = "GJ";
    public static final String brc = "51140003";

    public static final String lqbchannelId = "LQ";
    public static final String lqbbrc = "51140002";

    public String getSerialNo() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String str = format.format(new Date());
        return "SERIALNO" + str;
    }

    public String getFreezeNo() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String str = format.format(new Date());
        return "DJNO" + str;
    }

    public String getUnFreezeNo() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String str = format.format(new Date());
        return "JDNO" + str;
    }

    public Date formtDateStr(String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

}

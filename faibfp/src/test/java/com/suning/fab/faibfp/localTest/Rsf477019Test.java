package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.*;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.mulssyn.exception.FabException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/8/11
 * @Version 1.0
 */
public class Rsf477019Test extends TestUtil {

    @Autowired
    Rsf477019 rsf477019;
    @Autowired
    Rsf477020 rsf477020;
    @Autowired
    Rsf473004 rsf473004;
    @Autowired
    RsfSqlExecuteDeal rsfSqlExecuteDeal;

    //状态表 = 4
    @Test
    public void test1() {
        String receiptno_transfer = "TS11187867193819" + System.currentTimeMillis();
        test473004(receiptno_transfer, "0000016");
        //开户状态表是，更新成4

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("acctNo", receiptno_transfer);
        input.put("brc", "51030000");
        input.put("tranCode", "477019");
        input.put("termDate", "2022-02-24");


        input.put("expandDate", "2022-04-01");
        input.put("channelId", "66");
        DateFormat df = new SimpleDateFormat("yyMMddHHmmssSSS");
        input.put("serialNo", "TESTSERIALNO" + df.format(new Date()));

        Map<String, Object> ret = rsf477019.execute(input);
        System.out.println("=============" + ret);
    }



    public void test473004(String receiptno, String productCode) {

        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        input.put("serialNo", "TESTSERIALNO" + df.format(new Date()));
        input.put("outSerialNo", "outSerialNo000000");
        input.put("ccy", "CNY");
        input.put("contractAmt", 120000);
        input.put("contractNo", "contractNo000000");
        input.put("merchantNo", "huyi10");
        input.put("customType", "1");
        input.put("endDate", "2021-04-01");
        input.put("feeAmt", 0.00);
        input.put("feeRate", 0.00); //10.00
        input.put("repayDate", "01");
        input.put("intPerUnit", "M");
        input.put("periodNum", 1);
        input.put("periodType", "M");
        input.put("normalRate", 12.00);
        input.put("overdueRate", 15.00);
        input.put("compoundRate", 15.00);
        input.put("normalRateType", "Y");
        input.put("overdueRateType", "Y");
        input.put("compoundRateType", "Y");
        input.put("channelType", "5");    //放款渠道 1银行 2易付宝 3任性付
        input.put("loanType", "2");
        input.put("discountFlag", "1");
        input.put("receiptNo", receiptno);
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("openDate", "2021-01-01");
        input.put("fundChannel", "fundChannel000000");
        input.put("productCode", productCode);
        input.put("repayWay", "10");
        input.put("startIntDate", "2021-01-01"); //2021-01-01
        input.put("discountAmt", 0);    //扣息金额
        input.put("cashFlag", "2");
        input.put("investee", "");
        input.put("tranCode", "473004");
        input.put("brc", "51030000");
        input.put("termDate", df1.format(new Date()));
        input.put("termTime", df2.format(new Date()));
        input.put("channelId", "66");
        input.put("promotionID", "UNKNOWN");
        input.put("fundingModel", "UNKNOWN");

        Map<String, Object> ret = rsf473004.execute(input);
        System.out.println("=============" + ret);
    }

}

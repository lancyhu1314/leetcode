package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf473004;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.faibfp.utils.TranDateCutUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/4/1
 * @Version 1.0
 */
public class Rsf473004Test extends TestUtil {

    @Autowired
    Rsf473004 rsf473004;

    public void test473004_1(String date,String productCode) {

        TranDateCutUtil.setTranDate("2017-01-01", null, null);

        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        input.put("serialNo", df.format(new Date()));
        input.put("outSerialNo", "outSerialNo000002");
        input.put("ccy", "CNY");
        input.put("contractAmt", 120000);
        input.put("contractNo", "contractNo000002");
        input.put("merchantNo", "huyi2");
        input.put("customName", "customName000002");
        input.put("customType", "1");
        input.put("endDate", "2018-01-01");
        input.put("feeAmt", 0.00);
        input.put("feeRate", 10.00);
        input.put("repayDate", "01");
        input.put("intPerUnit", "M");
        input.put("periodNum", 1);
        input.put("periodType", "M");
        input.put("normalRate", 10.00);
        input.put("overdueRate", 15.00);
        input.put("compoundRate", 15.00);
        input.put("normalRateType", "Y");
        input.put("overdueRateType", "Y");
        input.put("compoundRateType", "Y");
        input.put("channelType", "3");    //放款渠道 1：银行存款 2：易付宝 3、云商分期 4、易付宝扫码
        input.put("loanType", "2");
        input.put("discountFlag", "1");
        input.put("receiptNo", "receiptNo000002");
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("openDate", "2017-01-01");
        input.put("fundChannel", "fundChannel000002");
        input.put("productCode", "2412610");
        input.put("repayWay", "2");
        input.put("startIntDate", "2017-01-01");
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

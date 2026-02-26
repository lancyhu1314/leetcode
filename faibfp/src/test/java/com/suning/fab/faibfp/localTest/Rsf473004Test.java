package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf473004;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.faibfp.utils.TranDateCutUtil;
import com.suning.fab.mulssyn.utils.VarChecker;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉开户放款过债务公司
 *
 * @Author 19043955
 * @Date 2021/4/14
 * @Version 1.0
 */
public class Rsf473004Test extends TestUtil {
    @Autowired
    Rsf473004 rsf473004;

    @Test
    public void test() {

        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmssSSS");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        input.put("routeId", "huyi10");
        input.put("serialNo", df.format(new Date()));
        input.put("outSerialNo", "outSerialNo000000");
        input.put("ccy", "CNY");
        input.put("contractAmt", 12000);
        input.put("contractNo", "contractNo000000");
        input.put("merchantNo", "huyi10");
        input.put("customName", "customName000000");
        input.put("customType", "1");
        input.put("endDate", "2022-09-20");
        input.put("feeAmt", 0.00);
        input.put("feeRate", 0.00); //10.00
        input.put("repayDate", "20");
        input.put("intPerUnit", "M");
        input.put("periodNum", 1);
        input.put("periodType", "M");
        input.put("normalRate", 12.00);
        input.put("overdueRate", 18.00);
        input.put("compoundRate", 0.00);
        input.put("normalRateType", "Y");
        input.put("overdueRateType", "Y");
        input.put("compoundRateType", "Y");
        input.put("channelType", "5");    //放款渠道 1银行 2易付宝 3任性付
        input.put("loanType", "2");
        input.put("discountFlag", "1");
        input.put("receiptNo", "receiptNo0000010");
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("fundChannel", "fundChannel000000");
        input.put("productCode", "2412622");
        input.put("repayWay", "10");
        input.put("startIntDate", "2021-09-20"); //2021-01-01
        input.put("discountAmt", 0);    //扣息金额
        input.put("cashFlag", "2");
        input.put("investee", "");
        input.put("tranCode", "473004");
        input.put("brc", brc);
        input.put("termDate", df1.format(new Date()));
        input.put("termTime", df2.format(new Date()));
        input.put("channelId", "66");
        input.put("cappingrule", "JQFD");


        input.put("promotionID", "UNKNOWN");
        input.put("fundingModel", "UNKNOWN");


        /*List<Map<String, Object>> pkgList3 = new ArrayList<>();
        Map<String, Object> det3 = new HashMap<>();
        det3.put("feeBrc", "51340000");
        det3.put("feerepayWay", "A");
        det3.put("calCulatrule", "1");
        det3.put("feeBase", "0");
        det3.put("freeFee", 100.00);
        det3.put("feeType", "SQFE");
        det3.put("overRate", 12.00);
        det3.put("advanceSettle", "3");
        det3.put("feeRate", 12.00);
        pkgList3.add(det3);
        input.put("pkgList3", pkgList3);*/

        Map<String, Object> ret = rsf473004.execute(input);
        System.out.println("=============" + ret);
    }

}

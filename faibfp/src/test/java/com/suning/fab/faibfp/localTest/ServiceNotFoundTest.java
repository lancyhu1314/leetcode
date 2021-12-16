package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf470011;
import com.suning.fab.faibfp.service.Rsf473004;
import com.suning.fab.faibfp.service.Rsf475001;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.faibfp.utils.TranDateCutUtil;
import org.junit.Test;
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
 * @Date 2021/11/8
 * @Version 1.0
 */
public class ServiceNotFoundTest extends TestUtil {

    @Autowired
    Rsf473004 rsf473004;

    @Autowired
    Rsf475001 rsf475001;

    @Autowired
    Rsf470011 rsf470011;

    @Test
    public void test() {

        test473004("2017-01-01", "0000014");

        test475001();

        test470011();
    }

    public void test470011() {

        Map<String, Object> param = new HashMap<>();
        param.put("acctNo", "receiptNo00000IBFPN");
        param.put("endDate", "2021-12-08");
        Map<String, Object> execute = rsf470011.execute(param);
        System.out.println(execute);

    }

    public void test475001() {

        Map<String, Object> param = new HashMap<>();
        param.put("receiptNo", "receiptNo00000IBFPN");
        param.put("startDate", "2021-11-08");
        param.put("endDate", "2021-11-10");
        param.put("pageSize", 10);
        param.put("currentPage", 1);

        Map<String, Object> execute = rsf475001.execute(param);

        System.out.println(execute);

    }

    public void test473004(String date, String productCode) {

        // 切日期清理数据
        TranDateCutUtil.setTranDateAndInite(date, "receiptNo00000IBFPN", "receiptNo00000IBFPN");

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
        input.put("customName", "");
        input.put("customId", "IBFP");
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
        input.put("receiptNo", "receiptNo00000IBFPN");
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("openDate", "2017-01-01");
        input.put("fundChannel", "fundChannel000002");
        input.put("productCode", productCode);
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

package com.suning.fab.faibfp.localTest;

import com.alibaba.fastjson.JSON;
import com.suning.fab.faibfp.service.Rsf471007;
import com.suning.fab.faibfp.utils.TestUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/3/19
 * @Version 1.0
 */
public class Rsf471007Test extends TestUtil {

    @Autowired
    Rsf471007 rsf471007;

    @Test
    public void test() {

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("serialNo", "SERIALNO20210325094830");
        input.put("routeId", "huyi2");
        input.put("tranCode", "471007");
        input.put("brc", "51030000");
        input.put("termDate", "2017-02-01");
        input.put("termTime", "00:00:00");
        input.put("channelId", "66");
        input.put("acctNo", "receiptNo000002");
        input.put("repayAcctNo", "huyi2");
        input.put("ccy", "CNY");
        input.put("cashFlag", "2");
        input.put("repayAmt", 1000);
        input.put("feeAmt", 0.00);
        input.put("repayChannel", "2");
        input.put("memo", "");
        input.put("outSerialNo", "out1231234321");
        input.put("channelId", "66");
        input.put("realDate", "2017-01-01");
        Map<String, Object> ret = rsf471007.execute(input);
        System.out.println(JSON.toJSONString(ret));
    }

}

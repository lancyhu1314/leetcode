package com.suning.fab.faibfp.localTest;

import com.alibaba.fastjson.JSON;
import com.suning.fab.faibfp.service.Rsf176012;
import com.suning.fab.faibfp.service.Rsf471007;
import com.suning.fab.faibfp.service.Rsf473004;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.faibfp.utils.TranDateCutUtil;
import com.suning.fab.mulssyn.utils.VarChecker;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉还款过预收测试
 *
 * @Author 19043955
 * @Date 2021/4/1
 * @Version 1.0
 */
public class RepayAdvanceTest extends TestUtil {

    @Autowired
    Rsf473004 rsf473004;

    @Autowired
    Rsf471007 rsf471007;

    @Autowired
    Rsf176012 rsf176012;

    @Test
    public void test() {

        test473004_1("2017-01-01", "2412610");
        // 开户开在了新系统，预收充值得使用176012，去除了借据号是否存在的判断
        test176012("2017-01-01", 120000.00);
        Test471007("", "2017-01-01", 120000.00);

    }

    public void test473004_1(String date, String productCode) {

        // 切日期清理数据
        TranDateCutUtil.setTranDateAndInite(date, "receiptNo00000IBFP", "IBFP");

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
        input.put("receiptNo", "receiptNo00000IBFP");
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


    public Map<String, Object> Test471007(String serialNo, String date, Double amt) {

        TranDateCutUtil.setTranDateAndInite(date, null, null);

        Map<String, Object> input = new HashMap<String, Object>();
        DateFormat df = new SimpleDateFormat("yyMMddHHmmssSSS");
        input.put("tranCode", "471007");
        input.put("brc", "51030000");
        input.put("termDate", "2017-02-01");
        input.put("termTime", "00:00:00");
        input.put("channelId", "66");
        input.put("serialNo", VarChecker.isEmpty(serialNo) ? "TESTSERIALNO" + df.format(new Date()) : serialNo);
        input.put("acctNo", "receiptNo00000IBFP");
        input.put("repayAcctNo", "IBFP");
        input.put("ccy", "CNY");
        input.put("cashFlag", "2");
        input.put("repayAmt", amt);
        input.put("feeAmt", 0.00);
        input.put("repayChannel", "2");
        input.put("memo", "");
        input.put("channelId", "66");
        input.put("realDate", date);
        Map<String, Object> ret = rsf471007.execute(input);
        System.out.println(JSON.toJSONString(ret));
        return ret;
    }

    public void test176012(String date, Double amt) {

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("tranCode", "176002");
        input.put("termDate", "2017-01-01");
        input.put("termTime", "10:12:12");
        input.put("channelId", "66");
        input.put("brc", "51030000");
        input.put("repayAcctNo", "IBFP");
        input.put("ccy", "CNY");
        input.put("amt", amt);
        input.put("outSerialNo", "bankNo000002");
        input.put("receiptNo", "receiptNo00000IBFP");
        input.put("channelType", "123");
        input.put("customType", "1");
        DateFormat df = new SimpleDateFormat("yyMMddHHmmssSSS");
        input.put("serialNo", "TESTSERIALNO" + df.format(new Date()));
        Map<String, Object> ret = rsf176012.execute(input);
        System.out.println(JSON.toJSONString(ret));
    }
}

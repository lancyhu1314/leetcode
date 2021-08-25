/*
 * Copyright (C), 2002-2016, 苏宁易购电子商务有限公司
 * FileName: FaTest.java
 * Author:   15040640
 * Date:     2016年5月26日 上午9:34:35
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.*;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.faibfp.utils.TranDateCutUtil;
import com.suning.fab.mulssyn.exception.FabException;
import com.suning.fab.mulssyn.utils.VarChecker;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉 借新还旧案例
 *
 * @author
 * @see
 * @since [产品/模块版本] （可选）
 */
public class FaloanJXHJTest extends TestUtil {
    @Autowired
    Rsf473004 rsf473004;
    @Autowired
    Rsf471007 rsf471007;
    @Autowired
    Rsf477016 rsf477018;
    @Autowired
    Rsf471014 rsf471014;
    @Autowired
    Rsf470012 rsf470012;

    @Test
    public void test() throws ParseException, InterruptedException, FabException {

        test473004_new("2021-02-10", "", 1000.00, "receiptNo000010");
    }


    public void test473004_kh(String date) throws FabException {
        TranDateCutUtil.setTranDateAndInite(date, "", "");

        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        input.put("routeId", "huyi10");
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
        input.put("receiptNo", "receiptNo000010");
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("openDate", "2021-01-01");
        input.put("fundChannel", "fundChannel000000");
        input.put("productCode", "0000015");
        input.put("repayWay", "10");
        input.put("startIntDate", date); //2021-01-01
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


    public void test473004_new(String date, String serialNo, Double tranamt, String exAcctno) throws FabException {
        TranDateCutUtil.setTranDateAndInite(date, "", "");

        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        input.put("routeId", "huyi10");
        input.put("serialNo", VarChecker.isEmpty(serialNo) ? "TESTSERIALNO" + df.format(new Date()) : serialNo);
        input.put("outSerialNo", "outSerialNo000000");
        input.put("ccy", "CNY");
        input.put("contractAmt", tranamt);
        input.put("contractNo", "contractNo000000");
        input.put("merchantNo", "huyi10");
        input.put("customName", "customName000000");
        input.put("customType", "1");
        input.put("endDate", "2021-12-11");
        input.put("feeAmt", 0.00);
        input.put("feeRate", 0.00); //10.00
        input.put("repayDate", "10");
        input.put("intPerUnit", "M");
        input.put("periodNum", 1);
        input.put("periodType", "M");
        input.put("normalRate", 0.00);
        input.put("overdueRate", 15.00);
        input.put("compoundRate", 15.00);
        input.put("normalRateType", "Y");
        input.put("overdueRateType", "Y");
        input.put("compoundRateType", "Y");
        input.put("channelType", "E");    //放款渠道 1银行 2易付宝 3任性付
        input.put("loanType", "1");
        input.put("discountFlag", "1");
        input.put("receiptNo", "new_receiptNo000010");
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("openDate", "2021-01-15");
        input.put("fundChannel", "fundChannel000000");
        input.put("productCode", "0000015");
        input.put("repayWay", "10");
        input.put("startIntDate", date); //2021-01-01
        input.put("discountAmt", 0);    //扣息金额
        input.put("cashFlag", "2");
        input.put("investee", "");
        input.put("tranCode", "473004");
        input.put("brc", "51030000");
        input.put("termDate", df1.format(new Date()));
        input.put("termTime", df2.format(new Date()));
        input.put("channelId", "66");

        input.put("exAcctno", exAcctno);
        input.put("exBrc", "51030000");
        input.put("switchloanType", "1");
        input.put("memo", "JXHJ");

        input.put("promotionID", "UNKNOWN");
        input.put("fundingModel", "UNKNOWN");
        Map<String, Object> ret = rsf473004.execute(input);
        System.out.println("=============" + ret);
    }

    public Map<String, Object> Test471007(String date, Double amt, String compensateFlag) throws ParseException {
        TranDateCutUtil.setTranDateAndInite(date, "", "");

        Map<String, Object> input = new HashMap<String, Object>();
        DateFormat df = new SimpleDateFormat("yyMMddHHmmssSSS");
        input.put("routeId", "huyi10");
        input.put("tranCode", "471007");
        input.put("brc", "51030000");
        input.put("termDate", "2017-02-01");
        input.put("termTime", "00:00:00");
        input.put("channelId", "66");
        input.put("serialNo", "TESTSERIALNO" + df.format(new Date()));
        input.put("acctNo", "receiptNo000010");
        input.put("repayAcctNo", "huyi10");
        input.put("ccy", "CNY");
        input.put("cashFlag", "2");
        input.put("repayAmt", amt);
        input.put("feeAmt", 0.00);
        input.put("repayChannel", "1");
        input.put("memo", "");
        input.put("channelId", "66");
        input.put("realDate", date);
        input.put("compensateFlag", compensateFlag);
        Map<String, Object> ret = rsf471007.execute(input);
        return ret;
    }

    public void Test471014(String trandate, Double amt, String realDate) throws FabException {
        TranDateCutUtil.setTranDateAndInite(trandate, "", "");


        Map<String, Object> input = new HashMap<String, Object>();
        input.put("tranCode", "471014");
        input.put("brc", "51030000");
        input.put("termDate", "2021-02-01");
        input.put("termTime", "00:00:00");
        input.put("channelId", "66");
        DateFormat df = new SimpleDateFormat("yyMMddHHmmssSSS");
        input.put("serialNo", "TESTSERIALNO" + df.format(new Date()));
        input.put("routeId", "huyi10");
        input.put("acctNo", "receiptNo000010");
        input.put("repayAcctNo", "huyi10");
        input.put("ccy", "CNY");
        input.put("repayAmt", amt);
        input.put("repayChannel", "1");
        input.put("outSerialNo", "1");
        input.put("memo", "");
        input.put("realDate", realDate);
        input.put("cashFlag", "2");


        Map<String, Object> ret = rsf471014.execute(input);
        System.out.println("@@@=============" + ret);
    }

    public String test477018(String routeId, String acctno) throws FabException {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("routeId", routeId);
        input.put("brc", "51030000");
        input.put("acctNo", acctno);
        input.put("tranCode", "477018");
        input.put("termDate", "2017-10-10");

        Map<String, Object> ret = rsf477018.execute(input);
        System.out.println("=============" + ret);
        return ret.get("pkgList").toString();
    }


    //预约还款查询汇总
    private BigDecimal get470012(String endDate) {

        //汇总预约结果
        Map<String, Object> input = new HashMap<>();
        input.put("termDate", endDate);
        input.put("brc", "51030000");
        input.put("routeId", "huyi10");
        input.put("acctNo", "receiptNo000010");
        input.put("endDate", endDate);
        Map<String, Object> ret = rsf470012.execute(input);


        BigDecimal cleanAmt = new BigDecimal(ret.get("cleanPrin").toString()).add(new BigDecimal(ret.get("cleanInt").toString()))
                .add(new BigDecimal(ret.get("cleanForfeit").toString())).add(new BigDecimal(ret.get("cleanFee").toString()));


        return cleanAmt;
    }
}

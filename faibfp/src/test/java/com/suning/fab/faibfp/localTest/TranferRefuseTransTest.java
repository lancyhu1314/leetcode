package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf470020;
import com.suning.fab.faibfp.service.Rsf473004;
import com.suning.fab.faibfp.service.Rsf477020;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.faibfp.utils.TranDateCutUtil;
import com.suning.fab.mulssyn.utils.VarChecker;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉交易拒绝逻辑验证
 *
 * @Author 19043955
 * @Date 2021/12/20
 * @Version 1.0
 */
public class TranferRefuseTransTest extends TestUtil {

    @Autowired
    Rsf473004 rsf473004;

    @Test
    public void test() {
        test473004("2021-12-20", "");
        test477016();
        test470020();
    }

    public void test473004(String date, String serialNo) {
        TranDateCutUtil.setTranDateAndInite(date, "", "");

        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        input.put("serialNo", VarChecker.isEmpty(serialNo) ? "TESTSERIALNO" + df.format(new Date()) : serialNo);
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
        input.put("receiptNo", "receiptNo0000re");
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("openDate", "2021-01-01");
        input.put("fundChannel", "fundChannel000000");
        input.put("productCode", "0000017");
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

    @Autowired
    Rsf477020 rsf477020;

    public void test477016() {

        Map<String, Object> param = new HashMap<>();
        param.put("acctNo", "receiptNo0000re");
        param.put("brc", "51340000");
        param.put("termDate", "2021-10-25");
        param.put("tranCode", "477020");
        Map<String, Object> execute = rsf477020.execute(param);
        System.out.println("===========" + execute);

    }

    @Autowired
    Rsf470020 rsf470020;

    public void test470020(){

        List<Map<String, Object>> mapList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("acctNo", "receiptNo0000re");
        map.put("brc", "51340000");
        map.put("termDate", "2021-10-25");
        map.put("tranCode", "477020");
        mapList.add(map);

        Map<String, Object> param = new HashMap<>();
        param.put("pkgList", mapList);

        Map<String, Object> execute = rsf470020.execute(param);



    }


}

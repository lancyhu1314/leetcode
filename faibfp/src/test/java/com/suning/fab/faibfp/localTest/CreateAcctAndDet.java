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
public class CreateAcctAndDet extends TestUtil {
    @Autowired
    Rsf473004 rsf473004;


    @Test
    public void test() {

        TranDateCutUtil.setTranDateAndInite("", "receiptNo0000det", "");

        test473004("2021-01-01", "");

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
        input.put("receiptNo", "receiptNo0000det");
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("openDate", "2021-01-01");
        input.put("fundChannel", "fundChannel000000");
        input.put("productCode", "2412611");
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

        List<Map<String, Object>> pkgList = new ArrayList<>();
        Map<String, Object> det1 = new HashMap<>();
        det1.put("debtCompany", "5400");
        det1.put("debtAmt", 60000.00);
        Map<String, Object> det2 = new HashMap<>();
        det2.put("debtCompany", "5410");
        det2.put("debtAmt", 20000.00);
        Map<String, Object> det3 = new HashMap<>();
        det3.put("debtCompany", "5430");
        det3.put("debtAmt", 40000.00);
        pkgList.add(det1);
        pkgList.add(det2);
        pkgList.add(det3);
        input.put("pkgList", pkgList);

        Map<String, Object> ret = rsf473004.execute(input);
        System.out.println("=============" + ret);
    }

}

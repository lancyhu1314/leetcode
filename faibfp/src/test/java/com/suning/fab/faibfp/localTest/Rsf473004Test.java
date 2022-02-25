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

        input.put("termDate", "2022-02-01");
        input.put("startIntDate", "2022-02-01");
        input.put("endDate", "2023-02-01");
        input.put("graceDays", 3);

        input.put("channelType", "5");

        input.put("normalRateType", "Y");
        input.put("brc", "51350000");
        input.put("ccy", "CNY");
        input.put("intPerUnit", "M");
        input.put("undertakeID", "1");
        input.put("customName", "zf");
        input.put("compoundRateType", "Y");
        input.put("serialNo", "GBFML632_zf0118_0224_0024");
        input.put("overdueRate", 18.00);
        input.put("receiptNo", "BFML632_zf_0224_002");
        input.put("contractAmt", 120000.00);
        input.put("fundingModel", "M02");
        input.put("repayDate", "7");
        input.put("sysPrdCode", "123456");
        input.put("discountIntFlag", "Y");
        input.put("merchantNo", "70666777");
        input.put("periodNum", 1);
        input.put("normalRate", 12.00);
        input.put("underTake", "G01");
        input.put("flowChannel", "4444");
        input.put("loanType", "2");
        input.put("freeInterest", 100.00);
        input.put("contractNo", "JNRCB1002381077");
        input.put("sysReceiptNo", "BFML632_zf_0224_002");
        input.put("customId", "70666777");
        input.put("outSerialNo", "BS21121400027951");
        input.put("repayWay", "8");
        input.put("termTime", "151603");
        input.put("cashFlag", "2");
        input.put("repayAcctNo", "70666777");
        input.put("bsNo", "AAGBFML600");
        input.put("openBrc", "51350000");
        input.put("channelId", "DH");
        input.put("compoundRate", 18.00);
        input.put("employeeId", "88554990");
        input.put("overdueRateType", "Y");
        input.put("promotionID", "ACCX001");
        input.put("salesStore", "XF");
        input.put("customType", "1");
        input.put("periodType", "M");
        input.put("productCode", "2412638");
        input.put("discountFlag", "1");
        input.put("tranCode", "473004");
        input.put("terminalCode", "API");

        List<Map<String, Object>> pkgList1 = new ArrayList<>();
        Map<String, Object> det1 = new HashMap<>();
        det1.put("investeeId", "70062597");
        det1.put("investeePrin", 120000.00);
        pkgList1.add(det1);
        input.put("pkgList1", pkgList1);

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

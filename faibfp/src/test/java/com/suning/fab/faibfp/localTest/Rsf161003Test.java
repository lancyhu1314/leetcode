package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf161001;
import com.suning.fab.faibfp.utils.TestUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/8/11
 * @Version 1.0
 */
public class Rsf161003Test extends TestUtil {

    @Autowired
    Rsf161001 tp161003;

    @Test
    public void test() {
        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        input.put("tranCode", "161002");
        input.put("brc", "51030000");
        input.put("termDate", "2022-09-29");
        input.put("termTime", "120012");
        input.put("ccy", "CNY");
        input.put("channelId", "FC");
        input.put("employeeId", "112233");
        input.put("flowChannel", "UNKNOWN");
        input.put("salesStore", "GL");
        input.put("terminalCode", "PC");
        input.put("repayAcctNo", "merchantNo000000");
        input.put("serialNo", "TEST0001" + df.format(new Date()));
        //input.put("serialNo",  "TEST0001221014150746");
        //input.put("serialNo",  "121212");
        input.put("feeBusiType", "LSF");
        input.put("feeBusiNo", "FC2022092900025");
        input.put("repayAmt", 500.00);
        input.put("bankSubject", "bankSubject000001");
        input.put("repayChannel", "2");
        input.put("platformId", "platformId000001");
        input.put("outSerialNo", "outSerialNo000001");


        //input.put("acctNo", "PTX200112865644");
        //input.put("acctNo", "PTX200411703751");
        //input.put("acctNo", "XF20061502203285");


        Map<String, Object> ret = tp161003.execute(input);
        System.out.println("@@@=============" + ret);
    }


}

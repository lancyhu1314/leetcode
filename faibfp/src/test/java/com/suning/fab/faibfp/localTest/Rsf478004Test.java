package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf478004;
import com.suning.fab.faibfp.service.Rsf479003;
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
 * @Date 2021/7/19
 * @Version 1.0
 */
public class Rsf478004Test extends TestUtil {

    @Autowired
    Rsf478004 rsf478004;



    @Test
    public void test() {
        //"brc":"51350000",
        // "acctNo":"RXDL22040810862865",
        // "termTime":"024452",
        // "rmiCode":"479001",
        // "sysGroup":"FALOAN",
        // "sysReceiptNo":"RXDL22040810862865",
        // "repayDate":"2022-04-08",
        // "tranCode":"479001",
        // "sysPrdCode":"2512642",
        // "termDate":"2022-04-08",
        // "channelId":"66"}
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("acctNo", "BFML6925");
        input.put("brc", "51350000");
        input.put("tranCode", "479001");
        input.put("termDate", "2022-02-24");
        input.put("termTime", "2022-02-24");
        input.put("channelId", "66");
        input.put("repayDate", "2022-02-24");


        Map<String, Object> ret = rsf478004.execute(input);
        System.out.println("=============" + ret);
    }

}

package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf471015;
import com.suning.fab.faibfp.utils.TestUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/8/11
 * @Version 1.0
 */
public class Rsf471015Test extends TestUtil {

    @Autowired
    Rsf471015 tp471015;

    @Test
    public void test() {
        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        input.put("tranCode", "471015");
        input.put("brc", "51030000");
        input.put("termDate", "2022-09-29");
        input.put("termTime", "120012");
        input.put("channelId", "66");




        input.put("serialNo", "PTX200112865644");
        input.put("acctNo", "PTX200411703751");
        input.put("switchFee", 1.00);
        input.put("cooperateId", "123");


        Map<String, Object> ret = tp471015.execute(input);
        System.out.println("@@@=============" + ret);
    }

}

package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf477016;
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
 * @Date 2021/8/11
 * @Version 1.0
 */
public class Rsf477016Test extends TestUtil {

    @Autowired
    Rsf477016 rsf477016;

    @Test
    public void test() {

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("acctNo", "BFML632_zf_0224_002");
        input.put("brc", "51350000");
        input.put("tranCode", "477016");
        input.put("termDate", "2022-02-24");

        Map<String, Object> ret = rsf477016.execute(input);
        System.out.println("=============" + ret);
    }
}

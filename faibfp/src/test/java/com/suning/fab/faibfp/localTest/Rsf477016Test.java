package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf477016;
import com.suning.fab.faibfp.service.Rsf477020;
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
    @Autowired
    Rsf477020 rsf477020;

    @Test
    public void test() {

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("acctNo", "BFML6791");
        input.put("brc", "51350000");
        input.put("tranCode", "477016");
        input.put("termDate", "2022-02-24");

        Map<String, Object> ret = rsf477016.execute(input);
        System.out.println("=============" + ret);
    }
    @Test
    public void test2() {

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("sysGroup", "FALOAN");
        input.put("acctNo", "TS032978671938191648543661181");
        input.put("brc", "51030000" );
        input.put("termDate", "2021-12-22");
        input.put("tranCode", "477020");

        Map<String, Object> ret = rsf477020.execute(input);
        System.out.println("=============" + ret);
    }

    //状态表 = 4
    @Test
    public void test3() {

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("acctNo", "TS033178671938191648692067011");
        input.put("brc", "51350000");
        input.put("tranCode", "477016");
        input.put("termDate", "2022-02-24");

        Map<String, Object> ret = rsf477016.execute(input);
        System.out.println("=============" + ret);
    }

    //状态表 = 3
    @Test
    public void test4() {

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("acctNo", "TS033178671938191648692025379");
        input.put("brc", "51350000");
        input.put("tranCode", "477016");
        input.put("termDate", "2022-02-24");

        Map<String, Object> ret = rsf477016.execute(input);
        System.out.println("=============" + ret);
    }

}

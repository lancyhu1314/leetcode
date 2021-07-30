package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf470020;
import com.suning.fab.faibfp.utils.TestUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/7/19
 * @Version 1.0
 */
public class Rsf470020T extends TestUtil {

    @Autowired
    Rsf470020 rsf470020;

    @Test
    public void test() {

        Map<String, Object> param = new HashMap<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("acctNo", "receiptNo0000det11111");
        map1.put("enCode", "51030000");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("acctNo", "receiptNo0000det");
        map2.put("enCode", "51030000");
        List<Map> list = new ArrayList<>();
        list.add(map1);
        list.add(map2);
        param.put("pkgList", list);
        param.put("brc", "51030000");
        param.put("tranCode", "470020");
        param.put("termDate", "2021-01-01");
        param.put("channelId", "66");
        Map<String, Object> execute = rsf470020.execute(param);
        System.out.println(execute);

    }
}

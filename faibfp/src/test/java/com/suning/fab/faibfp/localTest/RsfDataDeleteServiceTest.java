package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.RsfDataDeleteService;
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
 * @Date 2021/6/30
 * @Version 1.0
 */
public class RsfDataDeleteServiceTest extends TestUtil {

    @Autowired
    RsfDataDeleteService dataDeleteService;

    @Test
    public void test() {

        Map<String, Object> reqMsg = new HashMap<>();
        reqMsg.put("id", 1);
        reqMsg.put("routeId", "ulvp67nqt68c1nokidak");

        Map<String, Object> execute = dataDeleteService.execute(reqMsg);
        System.out.println(execute);

    }
}

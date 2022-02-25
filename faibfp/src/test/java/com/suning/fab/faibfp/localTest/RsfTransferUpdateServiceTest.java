package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.RsfTransferUpdateService;
import com.suning.fab.faibfp.utils.TestUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 * @ClassName RsfTransferUpdateService.java
 * @Description TODO
 * @createTime 2022年02月23日 20:19:00
 */
public class RsfTransferUpdateServiceTest extends TestUtil {

    @Autowired
    RsfTransferUpdateService rsfTransferUpdateService;

    @Test
    public void test() {

        Map<String, Object> reqMsg = new HashMap<>();
        reqMsg.put("id", 1);
        reqMsg.put("routeId", "123456");
        reqMsg.put("operation", "success");

        Map<String, Object> execute = rsfTransferUpdateService.execute(reqMsg);
        System.out.println(execute);

    }
}

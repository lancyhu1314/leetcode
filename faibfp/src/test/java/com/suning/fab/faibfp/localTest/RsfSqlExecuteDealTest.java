package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.RsfSqlExecuteDeal;
import com.suning.fab.faibfp.service.RsfTransferUpdateService;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.mulssyn.exception.FabException;
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
public class RsfSqlExecuteDealTest extends TestUtil {

    @Autowired
    RsfSqlExecuteDeal rsfSqlExecuteDeal;

    @Test
    public void test() throws FabException {

        Map<String, Object> reqMsg = new HashMap<>();
        reqMsg.put("type", "query");
        reqMsg.put("sql", "select * from transferrelation limit 1");

        Map<String, Object> execute = rsfSqlExecuteDeal.prepare(reqMsg);
        System.out.println(execute);

    }
}

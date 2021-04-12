package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.mulssyn.service.PendingExecuteService;
import org.junit.Test;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/3/25
 * @Version 1.0
 */
public class PendingExecuteServiceTest extends TestUtil {


    @Test
    public void test() {
        new PendingExecuteService().execute();
    }

}

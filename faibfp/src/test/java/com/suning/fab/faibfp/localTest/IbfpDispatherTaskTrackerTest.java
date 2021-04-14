package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.batch.IbfpDispatherTaskTracker;
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
 * @Date 2021/4/12
 * @Version 1.0
 */
public class IbfpDispatherTaskTrackerTest extends TestUtil {

    @Autowired
    IbfpDispatherTaskTracker tracker;

    @Test
    public void test() {

        Map<String, Object> param = new HashMap<>();
        param.put("s_TranDate", "2021-04-12");
        param.put("svcName", "pendingExecuteService");
        param.put("taskNo", "001011");
        tracker.dispather(param);

    }
}

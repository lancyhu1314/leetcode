package com.suning.fab.faibfp.bean;

import com.suning.fab.faibfp.utils.Getandset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 21071622
 * @Date 2021/10/26
 * @Version 1.0
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LnsInvalidCode.class,LnsInvalidCode1.class})
public class LnsInvalidCodeTest extends PowerMockito {
    @Test
    public void test() {
        LnsInvalidCode bean = spy(new LnsInvalidCode());
        Getandset.getsetCall(bean);

        LnsInvalidCode1 bean1 = spy(new LnsInvalidCode1());
        Getandset.getsetCall(bean1);
    }
}

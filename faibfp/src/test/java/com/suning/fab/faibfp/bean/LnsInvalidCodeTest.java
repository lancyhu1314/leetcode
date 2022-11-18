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
@PrepareForTest({LnsInvalidCode.class,LnsInvalidCode1.class,LnsInvalidCode2.class,LnsInvalidCode3.class})
public class LnsInvalidCodeTest extends PowerMockito {
    @Test
    public void test() {
        LnsInvalidCode bean = spy(new LnsInvalidCode());
        Getandset.getsetCall(bean);

        LnsInvalidCode1 bean1 = spy(new LnsInvalidCode1());
        Getandset.getsetCall(bean1);

        LnsInvalidCode2 bean2 = spy(new LnsInvalidCode2());
        Getandset.getsetCall(bean2);

        LnsInvalidCode3 bean3 = spy(new LnsInvalidCode3());
        Getandset.getsetCall(bean3);

        LnsInvalidCode4 bean4 = spy(new LnsInvalidCode4());
        Getandset.getsetCall(bean4);

        LnsInvalidCode5 bean5 = spy(new LnsInvalidCode5());
        Getandset.getsetCall(bean5);
    }
}

/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: ServiceReadyFlag.java
 * Author:   17060915
 * Date:     2018年11月2日 上午9:30:50
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.utils;

import java.util.concurrent.CountDownLatch;

/**
 *
 * @author 17060915
 * @date 2018年11月2日上午9:30:50
 * @since 1.0
 */
public class ServiceReadyFlag {

    private static final CountDownLatch ready = new CountDownLatch(1);

    public static CountDownLatch getCountDown(){
        return ready;
    }
    
}

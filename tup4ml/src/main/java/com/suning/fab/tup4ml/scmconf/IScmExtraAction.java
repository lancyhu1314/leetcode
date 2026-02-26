/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: IScmExtraAction.java
 * Author:   17060915
 * Date:     2018年8月18日 上午9:45:42
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.scmconf;

import java.util.Properties;

/**
 * 预留监听动态更改scm事件接口
 * 使用方法见GlobalScmExtraAction.java
 * 
 * @author 17060915
 * @date 2018年8月18日上午9:45:42
 * @since 1.0
 */
public interface IScmExtraAction {
    /**
     * 
     * 功能描述: <br>
     * 监听动态更改scm事件后相对应的处理方法
     *
     * @param scmFileName
     * @param properties
     * @since 1.0
     */
    void doExtraAction(String scmFileName,Properties properties);
}

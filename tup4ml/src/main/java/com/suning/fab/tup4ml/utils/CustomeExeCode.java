/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: CustomeExeCode.java
 * Author:   17060915
 * Date:     2018年9月13日 下午8:37:55
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.utils;

import org.springframework.transaction.TransactionTimedOutException;

import com.suning.dtf.client.support.AbstractExceptionExtract;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;

/**
 * dtf异常码解析
 * @author 17060915
 * @date 2018年9月13日下午8:37:55
 * @since 1.0
 */
public class CustomeExeCode extends AbstractExceptionExtract{

    @Override
    public String doExceptionExtract(Throwable exception) {
        if(exception instanceof FabException){
            return ((FabException)exception).getErrCode();
        }else if(exception instanceof TransactionTimedOutException){
            return PlatConstant.RSPCODE.TRANSACTION_TIME_OUT;
        }else{
            LoggerUtil.warn("CustomeExeCode: can't transformat exception code ");
            return PlatConstant.RSPCODE.UNKNOWN;
        }
    }

}

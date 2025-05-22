package com.suning.fab.tup4ml.elfin;

import java.util.Calendar;
import java.util.Date;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.utils.VarChecker;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.scmconf.ScmDynaGetterUtil;

/**
 * 交易时间控制
 *
 * @author 17060915
 * @since 2018年3月12日下午7:57:46
 * @version 1.0
 */
public class TransTimeController {

    /**
     * 
     * 功能描述: <br>
     * 校验交易的日期，如果大于scm中所设置的时间间隔，则提示所交易间隔时间过长
     * @param param
     * @throws FabException 
     * @since 1.0
     */
    public static boolean validate(AbstractDatagram param) throws FabException {
        
        if (VarChecker.isEmpty(param.getRequestDate())) {
            throw new FabException(PlatConstant.RSPCODE.VALIDATEERROR, "requestDate");
        }
        Calendar ca = Calendar.getInstance();
        ca.setTime(param.getRequestDate());
        ca.add(Calendar.MONTH, + Integer.parseInt(ScmDynaGetterUtil.getWithDefaultValue("GlobalScm.properties", "TransTimeInterval", "3")));
        Date newDate = ca.getTime();
        if (newDate.before(new Date())) {
            return false;
        }
        return true;
    }

}

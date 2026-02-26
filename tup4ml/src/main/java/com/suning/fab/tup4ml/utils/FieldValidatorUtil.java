/*
 * Copyright (C), 2002-2019, 苏宁易购电子商务有限公司
 * FileName: FieldValidatorUtil
 * Author:   17060915
 * Date:     2019/8/29 10:18
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名     修改时间     版本号        描述
 */
package com.suning.fab.tup4ml.utils;

import java.lang.reflect.Field;

import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.scmconf.ScmDynaGetterUtil;
import com.suning.framework.sedis.ReflectionUtils;

/**
 * 〈一句话功能简述〉<br>
 * <p>
 * 该类主要功能是为了给应用层进行校验
 *
 * @author 17060915
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class FieldValidatorUtil {

    static String[] defaultFields = {"employeeId", "salesStore", "brc", "flowChannel", "terminalCode", "contractCode"};

    /**
     * 功能描述: <br>
     *
     * @param:
     * @return:
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static void validateFields(Object object, String[] judgeFields, String judgeValueFiled) throws FabException {

        // 考核公共字段校验
        if (!"YES".equals(ScmDynaGetterUtil.getWithDefaultValue(PlatConstant.SCMFILENAME.GLOBAL_SCM, PlatConstant.SCMFIELDNAME.ASSESS_FIELD_FLAG, "YES"))) {
            return;
        }

        if (null == object) {
            throw new FabException(PlatConstant.RSPCODE.CHECK_FIELD_VALIDATE_ERROR, "");
        }

        Object ret = null;
        for (String field : judgeFields) {
            Field f = ReflectionUtils.findField(object.getClass(), field);
            if (null != f) {
                f.setAccessible(true);
                ret = ReflectionUtils.getField(f, object);
                f.setAccessible(false);
                if (null == ret)
                    throw new FabException(PlatConstant.RSPCODE.CHECK_FIELD_VALIDATE_ERROR, field);
                if (field.equals(judgeValueFiled)) {
                    String terminalCodeValue = GetPropUtil.getProperty(PlatConstant.PROPERFILENAME.CHECK_FIELD_TRANS_PROPER + "." + judgeValueFiled);
                    if (!terminalCodeValue.contains((String) ret)) {
                        throw new FabException(PlatConstant.RSPCODE.CHECK_FIELD_VALIDATE_ERROR, field);
                    }
                }
            }
        }
    }

    /**
     * 功能描述: <br>
     * 默认公共字段校验
     *
     * @param:
     * @return:
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static void validateFields(Object object) throws FabException {
        validateFields(object, defaultFields, "terminalCode");
    }

}

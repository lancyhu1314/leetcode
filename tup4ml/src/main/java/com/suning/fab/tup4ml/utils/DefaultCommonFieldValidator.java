/*
 * Copyright (C), 2002-2019, 苏宁易购电子商务有限公司
 * FileName: CommonFieldValidator
 * Author:   17060915
 * Date:     2019/8/12 10:05
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名     修改时间     版本号        描述
 */
package com.suning.fab.tup4ml.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.common.CommonCheckedField;
import com.suning.fab.model.domain.protocal.EntryBusinessCommon;
import com.suning.fab.model.utils.VarChecker;
import com.suning.fab.tup4ml.elfin.PlatConstant;
import com.suning.fab.tup4ml.exception.FabException;
import com.suning.fab.tup4ml.service.IValidator;
import com.suning.fab.tup4ml.service.ServiceTemplate;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 * <p>
 * 公共考核字段校验类
 *
 * @author 17060915
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Component
public class DefaultCommonFieldValidator implements IValidator {
    private static final String TERMINAL_CODE = "terminalCode";

    static Map<String, String> fieldCache = new ConcurrentHashMap<String, String>(10);

    static {
        try {
            String terminalCodeValue = GetPropUtil.getProperty(PlatConstant.PROPERFILENAME.CHECK_FIELD_TRANS_PROPER + "." + TERMINAL_CODE);
            String[] terminalArr = terminalCodeValue.split("\\|");
            for (String str : terminalArr) {
                fieldCache.put(str, str);
            }
        } catch (Exception e) {
            LoggerUtil.warn("parse terminalCode value from checkfieldtrans error");
        }
    }

    /**
     * 功能描述: <br>
     * 判断是否为空
     *
     * @param:
     * @return:
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public boolean judgeNull(EntryBusinessCommon entryBusinessCommon) throws FabException {

        CommonCheckedField commonCheckedField = entryBusinessCommon.getCommonCheckedField();

        if (null == commonCheckedField) {
            throw new FabException(PlatConstant.RSPCODE.CHECK_FIELD_VALIDATE_ERROR, "");
        }

        String execpField = null;
        if (VarChecker.isEmpty(commonCheckedField.getBrc())) {
            execpField = "brc";
        } else if (VarChecker.isEmpty(commonCheckedField.getEmployeeId())) {
            execpField = "employeeId";
        } else if (VarChecker.isEmpty(commonCheckedField.getBsNo())) {
            execpField = "bsNo";
        } else if (VarChecker.isEmpty(commonCheckedField.getFlowChannel())) {
            execpField = "flowChannel";
        } else if (VarChecker.isEmpty(commonCheckedField.getSalesStore())) {
            execpField = "salesStore";
        } else if (VarChecker.isEmpty(commonCheckedField.getTerminalCode())) {
            execpField = "terminalCode";
        }

        if (null != execpField) {
            throw new FabException(PlatConstant.RSPCODE.CHECK_FIELD_VALIDATE_ERROR, execpField);
        }

        return true;
    }

    /**
     * 功能描述: <br>
     * 判断枚举值
     * 为了应对未来枚举值的改变，所以将枚举值放到对应应用了
     *
     * @param:
     * @return:
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public boolean judgeValue(EntryBusinessCommon entryBusinessCommon) {

        CommonCheckedField commonCheckedField = entryBusinessCommon.getCommonCheckedField();
        String terminalCode = commonCheckedField.getTerminalCode();

        return null == fieldCache.get(terminalCode);
    }

    @Override
    public boolean validate(AbstractDatagram param) throws FabException {

        if (param instanceof EntryBusinessCommon) {

            EntryBusinessCommon entryBusinessCommon = (EntryBusinessCommon) param;
            if (judgeNull(entryBusinessCommon)) {
                if (judgeValue(entryBusinessCommon)) {
                    throw new FabException(PlatConstant.RSPCODE.CHECK_FIELD_VALIDATE_ERROR, TERMINAL_CODE);
                }
            }
        }

        return true;
    }

    @Override
    public boolean validate(AbstractDatagram param, Class<? extends ServiceTemplate> cls) throws FabException {

        String transName = GetPropUtil.getProperty(PlatConstant.PROPERFILENAME.CHECK_FIELD_TRANS_PROPER + "." + cls.getSimpleName());
        if (null == transName || "".equals(transName)) {
            return true;
        }

        return validate(param);
    }
}

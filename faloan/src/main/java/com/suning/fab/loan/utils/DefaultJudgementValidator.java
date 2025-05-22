/*
 * Copyright (C), 2002-2019, 苏宁易购电子商务有限公司
 * FileName: DefaultJudgementValidator.java
 * Author:   17060915
 * Date:     2019年9月2日 上午11:07:15
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.loan.utils;

import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.PlatConstant;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.validate.IValidatorJudgement;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author 17060915
 * @date 2019年9月2日上午11:07:15
 * @since 1.0
 */
@Component
public class DefaultJudgementValidator implements IValidatorJudgement{

    private static final String CHANNEL_FLAG= "channelFlag";
    private static final String DEFAULTVALUE = "FALSE";
    static Map<String, String> checkTranCodes = new ConcurrentHashMap(10);
    static Map<String, String> channelIds = new ConcurrentHashMap(10);

    @Override
    public boolean judgement(PubDict pubDict, String tranCode) {
        return judgement(pubDict.getChannelId(), tranCode);

    }

    public boolean judgement(String  channelId, String tranCode) {
        // 首先判断该交易是否需要校验
        // 首先根据应用系统配置的channelFlag标志，判断是否需要进行渠道校验。
        // 如果需要根据不同渠道判断不同的交易，则取channelId.tranCode去配置文件取值判断
        // 否则直接到配置里取交易名
        String channelFlag = PropertyUtil.getPropertyOrDefault(PlatConstant.PROPERFILENAME.CHECK_FIELD_TRANS_PROPER + "." + CHANNEL_FLAG, DEFAULTVALUE);
        if("YES".equals(channelFlag)){

            if(null != channelId){
                //2019-11-07 简化配置文件
                if(channelIds.containsKey(channelId)&&checkTranCodes.containsKey(tranCode)){
                    return true;
                }
                //特殊交易处理
                String tranCodeFlag = "";
                try {
                    tranCodeFlag = PropertyUtil.getPropertyWithCombine(PlatConstant.PROPERFILENAME.CHECK_FIELD_TRANS_PROPER +
                            PlatConstant.FIELDSPLITSYMBOL.DOUBLE_FIELD_SPLIT_SYMBOL + channelId + "." + tranCode);
                } catch (Exception e) {
                    LoggerUtil.warn("get tranCodeFlag from checkfieldtrans error",e);
                }
                return "YES".equals(tranCodeFlag);
            }
        }else{
            String transName = PropertyUtil.getPropertyOrDefault(PlatConstant.PROPERFILENAME.CHECK_FIELD_TRANS_PROPER + "." + tranCode, DEFAULTVALUE);
            return transName.equals("YES");
        }

        return false;
    }

    static {
        try {
            String checkTranCode = PropertyUtil.getProperty("checkfieldtrans.checkTranCode");
            String[] terminalArr = checkTranCode.split("\\|");

            for(String str:terminalArr) {
                checkTranCodes.put(str, str);
            }
        } catch (Exception var6) {
            LoggerUtil.warn("parse checkTranCode value from checkfieldtrans error",var6);
        }
        try {
            String channelId = PropertyUtil.getProperty("checkfieldtrans.channelId");
            String[] terminalArr = channelId.split("\\|");

            for(String str:terminalArr) {
                channelIds.put(str, str);
            }
        } catch (Exception var6) {
            LoggerUtil.warn("parse channelId value from checkfieldtrans error",var6);
        }
    }
}

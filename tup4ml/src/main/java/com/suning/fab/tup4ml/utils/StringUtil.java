/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: StringUtil.java
 * Author:   17060915
 * Date:     2018年6月4日 上午10:02:26
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.suning.fab.tup4ml.utils;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.domain.protocal.ExitBusinessCommon;

/**
 *
 * @author 17060915
 * @date 2018年6月4日上午10:02:26
 * @since 1.0
 */
public abstract class StringUtil {
	
    /**
     * 
     * 功能描述: <br>
     * 
     * 
     * @param param
     * @return
     * @since 1.0
     */
    public static String parseString(Object arg0) {
        return arg0 == null ? "" : arg0.toString().trim();
    }
    
    /**
     * 
     * 功能描述: <br>
     * 
     * 
     * @param param
     * @return
     * @since 1.0
     */
    public static String formatOuterSerialNumber(AbstractDatagram param){
        String sn = "NA";
        try{
            sn = param.getOuterSerialNumber();
        }catch(Exception e){
            sn = "NA";
        }
        return sn;
    }

    /**
     *
     * 功能描述: <br>
     *
     *
     * @param param
     * @return
     * @since 1.0
     */
    public static String getOuterSerialNumber(AbstractDatagram param){
        String outSerialNum = null;
        try{
            outSerialNum = param.getOuterSerialNumber();
            if(null == outSerialNum){
                return null;
            }
        }catch(Exception e){
            outSerialNum = "NA";
        }
        return outSerialNum;
    }

    /**
     * 功能描述: <br>
     * 截取返回信息长度
     *
     * @param param
     * @return
     * @since 1.0
     */
    public static void interceptResponeMsg(AbstractDatagram param) {
        if (param instanceof ExitBusinessCommon) {
            ExitBusinessCommon exitBusinessCommon = (ExitBusinessCommon) param;
            if (null != exitBusinessCommon && exitBusinessCommon.getRspMsg() != null) {
                if (exitBusinessCommon.getRspMsg().length() > 42) {
                    exitBusinessCommon.setRspMsg(subString(exitBusinessCommon.getRspMsg()));
                }
            }
        }
    }


    /**
     * 功能描述: <br>
     * 计算中文汉字的长度
     *
     * @param:
     * @return:
     * @see [相关类/方法](可选)
     * @since [产品/模块版本](可选)
     */
    public static String subString(String value) {
        int valueLength = 0;
        // 最终返回截取的字符串
        String lastStr = "";
        String chinese = "[\u0391-\uFFE5]";
        /* 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1 */
        for (int i = 0; i < value.length(); i++) {
            /* 获取一个字符 */
            String temp = value.substring(i, i + 1);
            /* 判断是否为中文字符 */
            if (temp.matches(chinese)) {
                /* 中文字符长度为3 */
                valueLength += 3;
            } else {
                /* 其他字符长度为1 */
                valueLength += 1;
            }

            if (valueLength > 128) {
                return lastStr;
            } else {
                lastStr += temp;
            }
        }
        return value;
    }


    /**
     * 功能描述: <br>
     * 在返回报文信息后添加流水号
     *
     * @param message
     * @return
     * @since 1.0
     */
    public static String appendSerialNo(String message) {
        // 在尾部添加流水号 格式如：[%s]时，支付流水号[%s]不存在！请求流水：[%s]
        String serialNum = SceneUtil.getSerialNumFromThreadLocal();
        if (null != serialNum) {
            return message + " 请求流水：[" + serialNum + "]";
        }
        return message;
    }

}

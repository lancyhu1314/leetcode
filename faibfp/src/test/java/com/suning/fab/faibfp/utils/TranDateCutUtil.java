package com.suning.fab.faibfp.utils;

import com.alibaba.fastjson.JSON;
import com.suning.fab.mulssyn.utils.ServiceAgentHelper;
import com.suning.rsf.consumer.ServiceAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/4/1
 * @Version 1.0
 */
public class TranDateCutUtil {

    public static void setTranDateAndInite(String date, String acctNo, String customId) {

        Map<String, Object> param = new HashMap<>();
        param.put("cutDate", date);
        param.put("acctNo", acctNo);
        param.put("customId", customId);
        ServiceAgent agent = ServiceAgentHelper.getAgent("479999");
        Map<String, Object> result = (Map<String, Object>) agent.invoke("execute", new Object[]{param}, new Class[]{Map.class});
        System.out.println(JSON.toJSONString(result));

    }

}

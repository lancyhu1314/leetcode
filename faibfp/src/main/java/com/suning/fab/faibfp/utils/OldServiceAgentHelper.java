package com.suning.fab.faibfp.utils;

import com.suning.fab.mulssyn.utils.GetPropUtil;
import com.suning.fab.mulssyn.utils.LoggerUtil;
import com.suning.fab.mulssyn.utils.PlatConstant;
import com.suning.rsf.consumer.ServiceAgent;
import com.suning.rsf.consumer.ServiceLocator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/3/15
 * @Version 1.0
 */
@Component
public class OldServiceAgentHelper {

    /**
     * 存放rsf服务的agent
     */
    public static Map<String, ServiceAgent> agentMap = new HashMap<>();

    /**
     * 根据trancode对应接口的rsf的Agent
     *
     * @param tranCode
     * @return
     */
    public static ServiceAgent getAgent(String tranCode) {
        ServiceAgent agent = agentMap.get(tranCode);
        if (null == agent) {
            String[] property = GetPropUtil.getProperty(PlatConstant.PROPERFILENAME.RSF_ELEMENTS + "." + tranCode).split("@");
            agent = ServiceLocator.getService(property[0], property[1]);
            agentMap.put(tranCode, agent);
        }
        return agent;
    }

    @PostConstruct
    public void init() {

        LoggerUtil.info("RSF服务的Agent进行预先加载");
        Properties properties = GetPropUtil.getProperties(ConstVar.CONFIGFILENAME.RSF_ELEMENTS_OLD);
        for (Map.Entry entry : properties.entrySet()) {
            String[] rsfInfo = String.valueOf(entry.getValue()).split("@");
            ServiceAgent agent = ServiceLocator.getServiceAgent(rsfInfo[0], rsfInfo[1]);
            agentMap.put(String.valueOf(entry.getKey()), agent);
        }
    }

}

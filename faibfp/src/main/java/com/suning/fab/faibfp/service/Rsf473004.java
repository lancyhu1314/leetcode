package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfAccountOpenIntfTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉开户放款
 *
 * @Author 19043955
 * @Date 2021/4/1
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-createAcctAndLoan")
public class Rsf473004 extends RsfAccountOpenIntfTemplate {

    @Override
    protected Map<String, Object> rspHandle(Map<String, Map<String, Object>> resps) {
        return resps.get("473004");
    }

    @Override
    public String getTranCode() {
        return "473004";
    }

}

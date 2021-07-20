package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfAccountOpenIntfTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 〈一句话功能简述〉<br>
 * 非标开户
 *
 * @author 19043955
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-nonstdCreateAcctAndLoan")
public class Rsf473005 extends RsfAccountOpenIntfTemplate {


    @Override
    public String getTranCode() {
        return "473005";
    }

    @Override
    protected Map<String, Object> rspHandle(Map<String, Map<String, Object>> resps) {
        return resps.get("473005");
    }

}

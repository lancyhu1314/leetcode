package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfLoanRepayTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/4/16
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-leaseRefund")
public class Rsf471010 extends RsfLoanRepayTemplate {
    @Override
    public String getTranCode() {
        return "471010";
    }

    @Override
    protected Map<String, Object> rspHandle(Map<String, Map<String, Object>> resps) {
        return resps.get("471010");
    }
}

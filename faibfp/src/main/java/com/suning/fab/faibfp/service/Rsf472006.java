package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉贷款核销
 *
 * @Author 19043955
 * @Date 2021/1/20
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-loanCancellation")
public class Rsf472006 extends RsfServiceTemplate {

    @Override
    public String getTranCode() {
        return "472006";
    }
}

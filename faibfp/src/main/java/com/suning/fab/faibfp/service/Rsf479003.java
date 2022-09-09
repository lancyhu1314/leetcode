package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉贷款转非应计
 *
 * @Author
 * @Date
 * @Version
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-nonAccrualOpt")
public class Rsf479003 extends RsfServiceTemplate {

    @Override
    public String getTranCode() {
        return "479003";
    }
}

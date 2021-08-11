package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/4/2
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-advanceAccountCharge")
public class Rsf176002 extends RsfServiceTemplate {

    /**
     * 预收户不迁移，暂时只调用老系统
     *
     * @param productCode
     * @return
     */
    @Override
    public boolean isCallOldSystem(String productCode) {
        return true;
    }

    @Override
    public String getTranCode() {
        return "176002";
    }
}

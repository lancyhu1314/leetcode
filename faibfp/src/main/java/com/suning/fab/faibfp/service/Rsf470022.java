package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉利息试算
 *
 * @Author 19043955
 * @Date 2021/2/22
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-interestCalculate")
public class Rsf470022 extends RsfServiceTemplate {


    @Override
    public String getTranCode() {
        return "470022";
    }
}

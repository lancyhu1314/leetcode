package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.intf.FaloanMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉展期
 *
 * @Author 19043955
 * @Date 2021/02/01
 * @Version 1.0
 */
@Service
@Implement(contract = FaloanMapService.class, implCode = "faloan-ExpandDate")
public class Rsf470017 extends RsfServiceTemplate {

    @Override
    public String getTranCode() {
        return "470017";
    }
}

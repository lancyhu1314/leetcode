package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.intf.FaloanMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉非标冲销
 *
 * @Author 19043955
 * @Date 2021/1/5
 * @Version 1.0
 */
@Service
@Implement(contract = FaloanMapService.class, implCode = "faloan-nonWriteOff")
public class Rsf472004 extends RsfServiceTemplate {


    @Override
    public String getTranCode() {
        return "472004";
    }
}

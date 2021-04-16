package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.intf.FaloanMapService;
import com.suning.fab.faibfp.service.template.RsfQuerServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉<br>
 * 息费计划试算
 *
 * @author 19043955
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = FaloanMapService.class, implCode = "faloan-housePlanTentativeCalculation")
public class Rsf476004 extends RsfQuerServiceTemplate {


    @Override
    public String getTranCode() {
        return "476004";
    }
}

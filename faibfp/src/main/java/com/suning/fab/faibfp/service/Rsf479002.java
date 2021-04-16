package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.intf.FaloanMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉<br>
 * 结息转列
 *
 * @author 19043955
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = FaloanMapService.class, implCode = "faloan-settlePrincipalSettleInterest")
public class Rsf479002 extends RsfServiceTemplate {


    @Override
    public String getTranCode() {
        return "479002";
    }
}

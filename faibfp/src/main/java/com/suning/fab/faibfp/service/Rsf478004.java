package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉<br>
 * 法催费用还款
 *
 * @author
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-exanteFeeDeduction")
public class Rsf478004 extends RsfServiceTemplate {

    @Override
    public String getTranCode() {
        return "478004";
    }
}

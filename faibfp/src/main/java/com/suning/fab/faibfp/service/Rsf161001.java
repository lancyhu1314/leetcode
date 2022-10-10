package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉<br>
 * 法催费用开户
 *
 * @author
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-legalCollectFeeAccountCreate")
public class Rsf161001 extends RsfServiceTemplate {


    /**
     * 法催相关交易，暂时只调用老系统
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
        return "161001";
    }
}

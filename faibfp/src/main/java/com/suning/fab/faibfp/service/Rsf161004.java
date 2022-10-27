package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfQuerServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

/**
 * 〈一句话功能简述〉<br>
 * 法催费用余额查询
 *
 * @author
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-legalCollectFeeBalanceQuery")
public class Rsf161004 extends RsfQuerServiceTemplate {


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
        return "161004";
    }
}

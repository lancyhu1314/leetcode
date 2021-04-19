package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.intf.FaloanMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉放款撤销
 *
 * @Author 19043955
 * @Date 2021/1/5
 * @Version 1.0
 */
@Service
@Implement(contract = FaloanMapService.class, implCode = "faloan-loanRevoke")
public class Rsf472003 extends RsfServiceTemplate {


    @Override
    public String getTranCode() {
        return "472003";
    }

    @Override
    public String getProductMapRouteId(String receiptNo, Map<String, Object> reqMsg) {
        return (String) reqMsg.get(ConstVar.PARAMETER.ERRSERSEQ);
    }
}

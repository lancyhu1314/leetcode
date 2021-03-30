package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.fab.mulssyn.bean.TransDetail;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/3/18
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repay")
public class Rsf471007 extends RsfServiceTemplate {

    @Override
    protected List<TransDetail> paramSplite(Map<String, Object> param) {

        Map<String, Object> param_176001 = new HashMap<>();
        param_176001.put("tranCode", "176001");
        param_176001.put("termDate", param.get("termDate"));
        param_176001.put("termTime", param.get("termTime"));
        param_176001.put("channelId", param.get("channelId"));
        param_176001.put("brc", param.get("brc"));
        param_176001.put("routeId", param.get("repayAcctNo"));
        param_176001.put("channelType", param.get("repayChannel"));
        param_176001.put("repayAcctNo", param.get("repayAcctNo"));
        param_176001.put("ccy", param.get("ccy"));
        param_176001.put("amt", param.get("repayAmt"));
        param_176001.put("outSerialNo", param.get("outSerialNo"));
        param_176001.put("memo", param.get("memo"));
        param_176001.put("receiptNo", param.get("receiptNo"));
        param_176001.put("cooperateId", param.get("cooperateId"));
        param_176001.put("pkgList", param.get("pkgList"));

        TransDetail detail_17 = new TransDetail("176001", "176002", param_176001, 1);
        TransDetail detail = new TransDetail("471007", "", param, 2);
        List<TransDetail> list = new ArrayList<>();
        list.add(detail_17);
        list.add(detail);
        return list;
    }

    @Override
    protected Map<String, Object> rspHandle(Map<String, Map<String, Object>> resps) {

        return resps.get("471007");
    }
}

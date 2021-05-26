package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.bean.TransDetail;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉<br>
 * 指定类型还款
 *
 * @author 19043955
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-specialRepay")
public class Rsf471008 extends RsfServiceTemplate {

    @Override
    protected List<TransDetail> paramSplite(Map<String, Object> param) {

        Map<String, Object> param_1760011 = new HashMap<>();
        param_1760011.put("tranCode", "176011");
        param_1760011.put("termDate", param.get("termDate"));
        param_1760011.put("termTime", param.get("termTime"));
        param_1760011.put("channelId", param.get("channelId"));
        param_1760011.put("brc", param.get("brc"));
        param_1760011.put("repayAcctNo", param.get("repayAcctNo"));
        param_1760011.put("ccy", param.get("ccy"));
        param_1760011.put("amt", param.get("repayAmt"));
        param_1760011.put("receiptNo", param.get("acctNo"));
        param_1760011.put("pkgList", param.get("pkgList"));
        // 添加路由字段为老系统：预收方面暂时都调用老系统
        param_1760011.put("sysGroup", "FALOAN");

        TransDetail detail_17 = new TransDetail("176011", "176012", param_1760011, 1);
        TransDetail detail = new TransDetail("471008", "", param, 2);
        List<TransDetail> list = new ArrayList<>();
        list.add(detail_17);
        list.add(detail);
        return list;
    }

    @Override
    protected Map<String, Object> rspHandle(Map<String, Map<String, Object>> resps) {

        return resps.get("471008");
    }

    @Override
    public String getTranCode() {
        return "471008";
    }

    /**
     * 调用新模型的时候，判断是否需要走跨库事务
     *
     * @param reqMsg
     * @return
     */
    @Override
    public boolean isNeedSpliteTransation(Map<String, Object> reqMsg) {

        // 从参数中获取还款渠道
        String repayChannel = (String) reqMsg.get(ConstVar.PARAMETER.REPAYCHANNEL);
        // 渠道为2的时候，表示过预收，需要扣减预收户，需要跨库事务
        if ("2".equals(repayChannel)) {
            return true;
        }
        return false;
    }

}

package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.intf.FaloanMapService;
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
@Implement(contract = FaloanMapService.class, implCode = "faloan-specialRepay")
public class Rsf471008 extends RsfServiceTemplate {

    @Override
    protected List<TransDetail> paramSplite(Map<String, Object> param) {

        Map<String, Object> param_176001 = new HashMap<>();
        param_176001.put("tranCode", "176011");
        param_176001.put("termDate", param.get("termDate"));
        param_176001.put("termTime", param.get("termTime"));
        param_176001.put("channelId", param.get("channelId"));
        param_176001.put("brc", param.get("brc"));
        param_176001.put("repayAcctNo", param.get("repayAcctNo"));
        param_176001.put("ccy", param.get("ccy"));
        param_176001.put("amt", param.get("repayAmt"));
        param_176001.put("receiptNo", param.get("acctNO"));
        param_176001.put("pkgList", param.get("pkgList"));

        TransDetail detail_17 = new TransDetail("176011", "176012", param_176001, 1);
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

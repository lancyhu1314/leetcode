package com.suning.fab.faibfp.service.template;

import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.bean.TransDetail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/7/20
 * @Version 1.0
 */
public abstract class RsfLoanRepayTemplate extends RsfServiceTemplate {

    @Override
    protected List<TransDetail> paramSplite(Map<String, Object> param) {

        Map<String, Object> param_1760011 = new HashMap<>();
        param_1760011.put("tranCode", "176011");
        param_1760011.put("termDate", param.get("termDate"));
        param_1760011.put("termTime", param.get("termTime"));
        param_1760011.put("channelId", param.get("channelId"));
        //471011特殊处理下，如果子机构传值了，取子机构号，否则取公共子机构号
        if (getTranCode().equals("471011")) {
            if (param.get("childBrc") != null) {
                param_1760011.put("brc", param.get("childBrc"));
            } else {
                param_1760011.put("brc", param.get("brc"));
            }
        } else {
            param_1760011.put("brc", param.get("brc"));
        }
        param_1760011.put("repayAcctNo", param.get("repayAcctNo"));
        param_1760011.put("ccy", param.get("ccy"));
        param_1760011.put("amt", param.get("repayAmt"));
        param_1760011.put("receiptNo", param.get("acctNo"));
        param_1760011.put("pkgList", param.get("pkgList"));
        // 添加路由字段为老系统：预收方面暂时都调用老系统
        param_1760011.put("sysGroup", "FALOAN");

        TransDetail detail_17 = new TransDetail("176011", "176012", param_1760011, 1);
        TransDetail detail = new TransDetail(getTranCode(), "", param, 2);
        List<TransDetail> list = new ArrayList<>();
        list.add(detail_17);
        list.add(detail);

        return list;
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

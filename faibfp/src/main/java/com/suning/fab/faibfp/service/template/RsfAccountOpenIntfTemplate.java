package com.suning.fab.faibfp.service.template;

import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.bean.TransDetail;
import com.suning.fab.mulssyn.utils.VarChecker;
import org.springframework.util.CollectionUtils;

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
public abstract class RsfAccountOpenIntfTemplate extends RsfServiceTemplate {

    @Override
    protected List<TransDetail> paramSplite(Map<String, Object> param) {

        List<TransDetail> detailList = new ArrayList<>();
        // 债务公司
        if (!CollectionUtils.isEmpty((List) param.get(ConstVar.PARAMETER.PKGLIST))) {
            Map<String, Object> detParam = new HashMap<>();

            detParam.put("tranCode", "1760012");
            detParam.put("termDate", param.get("termDate"));
            detParam.put("termTime", param.get("termTime"));
            detParam.put("channelId", param.get("channelId"));
            detParam.put("brc", param.get("brc"));
            detParam.put("receiptNo", param.get("receiptNo"));
            detParam.put("amt", param.get("contractAmt"));
            detParam.put("customType", param.get("customType"));
            detParam.put("pkgList", param.get(ConstVar.PARAMETER.PKGLIST));
            // 添加路由字段为老系统：预收方面暂时都调用老系统
            detParam.put("sysGroup", "FALOAN");
            TransDetail detail = new TransDetail("176012", "176011", detParam, 2);
            detailList.add(detail);
        }
        // 借新还旧之还款
        if (VarChecker.asList("E").contains(param.get(ConstVar.PARAMETER.CHANNELTYPE))) {
            Map<String, Object> repayParam = new HashMap<>();
            repayParam.put("tranCode", "471007");
            repayParam.put("termDate", param.get("termDate"));
            repayParam.put("termTime", param.get("termTime"));
            repayParam.put("channelId", param.get("channelId"));
            repayParam.put("brc", param.get("exBrc"));
            repayParam.put("acctNo", param.get("exAcctno"));
            repayParam.put("repayAmt", param.get("contractAmt"));
            repayParam.put("settleFlag", "1");
            repayParam.put("cashFlag", "2");
            repayParam.put("ccy", param.get("ccy"));
            // 暂时设置为0，无
            repayParam.put("repayChannel", "0");
            repayParam.put("compensateFlag", 3);
            TransDetail detail = new TransDetail("471007", "", repayParam, 1);
            detailList.add(detail);
        } else {
            Map<String, Object> param_open = new HashMap<>();
            param_open.put("tranCode", "176013");
            param_open.put("termDate", param.get("termDate"));
            param_open.put("termTime", param.get("termTime"));
            param_open.put("channelId", param.get("channelId"));
            param_open.put("brc", param.get("brc"));
            param_open.put("merchantNo", param.get("merchantNo"));
            param_open.put("customType", param.get("customType"));
            // 添加路由字段为老系统：预收方面暂时都调用老系统
            param_open.put("sysGroup", "FALOAN");
            TransDetail detail = new TransDetail("176013", "176013", param_open, 1);
            detailList.add(detail);
        }

        // 放款
        detailList.add(new TransDetail(getTranCode(), "472001", param, 3));

        return detailList;
    }

    /**
     * 开户类的接口都需要尝试去调用老系统开预收户，所以都得走跨库事务
     *
     * @param reqMsg
     * @return
     */
    @Override
    public boolean isNeedSpliteTransation(Map<String, Object> reqMsg) {
        return true;
    }
}

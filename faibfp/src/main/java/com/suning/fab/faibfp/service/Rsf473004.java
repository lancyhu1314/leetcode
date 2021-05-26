package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfServiceTemplate;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.bean.TransDetail;
import com.suning.fab.mulssyn.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉开户放款
 *
 * @Author 19043955
 * @Date 2021/4/1
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-createAcctAndLoan")
public class Rsf473004 extends RsfServiceTemplate {


    @Override
    protected List<TransDetail> paramSplite(Map<String, Object> param) {

        List<TransDetail> detailList = new ArrayList<>();
        int seq = 1;
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
            TransDetail detail = new TransDetail("176012", "176011", detParam, seq++);
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
            TransDetail detail = new TransDetail("471007", "", repayParam, seq + 1);
            detailList.add(detail);
        }

        // 放款
        detailList.add(new TransDetail("473004", "472001", param, seq));

        return detailList;
    }

    @Override
    protected Map<String, Object> rspHandle(Map<String, Map<String, Object>> resps) {
        return resps.get("473004");
    }

    @Override
    public String getTranCode() {
        return "473004";
    }

    @Override
    public boolean isNeedSpliteTransation(Map<String, Object> reqMsg) {

        // 借新还旧
        if (VarChecker.asList("E").contains(reqMsg.get(ConstVar.PARAMETER.CHANNELTYPE))) {
            return true;
        }
        // 有债务公司
        if (!CollectionUtils.isEmpty((List) reqMsg.get(ConstVar.PARAMETER.PKGLIST))) {
            return true;
        }
        return super.isNeedSpliteTransation(reqMsg);
    }

}

package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfLoanRepayTemplate;
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
 * 功能描述: <br>
 * 〈功能详细描述〉还款
 *
 * @Author 19043955
 * @Date 2021/3/18
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repay")
public class Rsf471007 extends RsfLoanRepayTemplate {

    @Override
    protected Map<String, Object> rspHandle(Map<String, Map<String, Object>> resps) {

        return resps.get("471007");
    }

    @Override
    public String getTranCode() {
        return "471007";
    }


}

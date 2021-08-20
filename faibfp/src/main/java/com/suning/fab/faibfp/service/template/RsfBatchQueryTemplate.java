package com.suning.fab.faibfp.service.template;

import com.alibaba.fastjson.JSON;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.exception.FabException;
import com.suning.fab.mulssyn.scmconf.ScmDynaGetterUtil;
import com.suning.fab.mulssyn.utils.LoggerUtil;
import com.suning.fab.mulssyn.utils.PlatConstant;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉批量查询服务模板
 *
 * @Author 19043955
 * @Date 2021/8/20
 * @Version 1.0
 */
public abstract class RsfBatchQueryTemplate extends RsfQuerServiceTemplate {

    /**
     * @param reqMsg
     * @param startInterval
     * @return
     * @throws FabException
     */
    @Override
    public Map<String, Object> dataDistribute(Map<String, Object> reqMsg, long startInterval) throws FabException {

        // 是否启用跨系统调用预约还款计划拼接逻辑
        String isOpenBatchCross = ScmDynaGetterUtil.getWithDefaultValue("GlobalScm.properties", "isOpenBatchCross", "false");

        if ("true".equalsIgnoreCase(isOpenBatchCross)) {
            // 将报文重新拆分
            List<Map<String, Object>> reqs = (List<Map<String, Object>>) reqMsg.get("pkgList");
            // 未迁移报文
            List<Map<String, Object>> unMigrated = new ArrayList<>();
            // 已迁移报文
            List<Map<String, Object>> migrated = new ArrayList<>();
            for (Map<String, Object> req : reqs) {
                // 获取借据号
                String receiptNo = compatibleWithCAcctno(req);
                // 获取产品编码
                String productCode = getMappintProductCode(req, receiptNo);
                // 将产品添加到参数中
                req.put(ConstVar.PARAMETER.SYSPRDCODE, productCode);
                // 设置路由字段
                req.put(PlatConstant.PARAMETER.ROUTEID, receiptNo);
                // 判断是否迁移，将报文分类
                if (isCallOldSystem(productCode)) {
                    unMigrated.add(req);
                    LoggerUtil.info("前置拆分调用老系统：借据号：{}，产品：{}", receiptNo, productCode);
                } else {
                    migrated.add(req);
                    LoggerUtil.info("前置拆分调用新模型：借据号：{}，产品：{}", receiptNo, productCode);
                }
            }

            Map<String, Object> ret_old = null;
            // 调用老系统
            if (!CollectionUtils.isEmpty(unMigrated)) {
                Map<String, Object> req_old = new HashMap<>();
                req_old.putAll(reqMsg);
                req_old.put("pkgList", unMigrated);
                req_old.put("sysGroup", "FALOAN");
                ret_old = transparentExecute(req_old, false, startInterval);
            }

            Map<String, Object> ret_new = null;
            // 调用新系统
            if (!CollectionUtils.isEmpty(migrated)) {
                Map<String, Object> req_new = new HashMap<>();
                req_new.putAll(reqMsg);
                req_new.put("pkgList", migrated);
                ret_new = transparentExecute(req_new, true, startInterval);
            }

            // 两次调用有一个报错直接返回报错
            if (null != ret_old && !PlatConstant.RSPCODE.OK.equals(ret_old.get("rspCode")))
                return ret_old;
            if (null != ret_new && !PlatConstant.RSPCODE.OK.equals(ret_new.get("rspCode")))
                return ret_new;

            if (null != ret_new) {
                List<Map> pk_new = JSON.parseArray((String) ret_new.get("pkgList1"), Map.class);

                if (null != ret_old) {
                    List<Map> pk_old = JSON.parseArray((String) ret_old.get("pkgList1"), Map.class);
                    pk_new.addAll(pk_old);
                }
                ret_new.put("pkgList1", JSON.toJSONString(pk_new));
                return ret_new;

            } else {
                return ret_old;
            }
        } else {
            return super.dataDistribute(reqMsg, startInterval);
        }

    }

}
package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.dbhandler.DataDeleteHandler;
import com.suning.fab.mulssyn.utils.LoggerUtil;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉专门提供：删除PROTOREG表中数据用的，按照主键id和路由字段来删
 *
 * @Author 19043955
 * @Date 2021/6/30
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faibfp-dataDeleteService")
public class RsfDataDeleteService {

    public Map<String, Object> execute(Map<String, Object> reqMsg) {

        Map<String, Object> result = new HashMap<>();
        try {
            new DataDeleteHandler().deleteProtoregData((String) reqMsg.get("routeId"), (int) reqMsg.get("id"));
            result.put("rspCode", "000000");
            result.put("rspMsg", "交易成功");
        } catch (Exception e) {
            LoggerUtil.error("删除报文表数据出错", e);
            result.put("rspCode", "999999");
            result.put("rspMsg", e.getMessage());
        }
        return result;
    }

}

package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.bean.TransferRelation;
import com.suning.fab.faibfp.dbhandler.TransferRelationHandler;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.utils.LoggerUtil;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 * @ClassName RsfTransferUpdateStatus.java
 * @Description TODO
 * @createTime 2022年02月23日 20:14:00
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faibfp-transferUpdateService")
public class RsfTransferUpdateService {
    public Map<String, Object> execute(Map<String, Object> reqMsg) {

        Map<String, Object> result = new HashMap<>();
        try {
            String routeid = (String) reqMsg.get("routeId");
            String operation = (String) reqMsg.get("operation");
            TransferRelationHandler transferHandler = new TransferRelationHandler();
            //迁移前，库里有则更新，没有则新插入
            if ("lock".equals(operation)) {
                //存在查询的时候没有，但是到插入过程中，其他线程插入了，导致这里插入失败了，会抛出异常，最后捕获了
                TransferRelation transfer = transferHandler.load(routeid);
                if (transfer == null) {
                    int counts = transferHandler.save(routeid, ConstVar.TRANSFERSTATUS.TRANSFERING, 0);
                    if (counts == 1) {
                        result.put("rspCode", "000000");
                        result.put("rspMsg", "交易成功");
                        return result;
                    }
                } else {
                    int counts = transferHandler.updateStatus(routeid, ConstVar.TRANSFERSTATUS.NOT_TRANSFER, ConstVar.TRANSFERSTATUS.TRANSFERING);
                    if (counts == 1) {
                        result.put("rspCode", "000000");
                        result.put("rspMsg", "交易成功");
                        return result;
                    }
                }
            }
            //迁移后，成功，更新状态 迁移中为 已迁移
            else if ("success".equals(operation)) {
                int counts = transferHandler.updateStatus(routeid, ConstVar.TRANSFERSTATUS.TRANSFERING, ConstVar.TRANSFERSTATUS.END_TRANSFER);
                if (counts == 1) {
                    result.put("rspCode", "000000");
                    result.put("rspMsg", "交易成功");
                    return result;
                }
            }
            //迁移后，失败，更新状态 迁移中为 未迁移
            else if ("error".equals(operation)) {
                int counts = transferHandler.updateStatus(routeid, ConstVar.TRANSFERSTATUS.TRANSFERING, ConstVar.TRANSFERSTATUS.NOT_TRANSFER);
                if (counts == 1) {
                    result.put("rspCode", "000000");
                    result.put("rspMsg", "交易成功");
                    return result;
                }
            } else {
                result.put("rspCode", "999997");
                result.put("rspMsg", "操作条件非法！");
                return result;
            }
            result.put("rspCode", "999998");
            result.put("rspMsg", "交易失败，更新条数为0");
        } catch (Exception e) {
            LoggerUtil.error("更新迁移状态表出错", e);
            result.put("rspCode", "999999");
            result.put("rspMsg", "更新迁移状态表出错:" + e.getMessage());
        }
        return result;
    }

}

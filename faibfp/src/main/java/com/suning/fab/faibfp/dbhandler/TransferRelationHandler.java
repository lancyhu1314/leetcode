package com.suning.fab.faibfp.dbhandler;

import com.suning.fab.faibfp.bean.TransferRelation;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.db.AbstractBaseDao;

import java.util.HashMap;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 * @ClassName TransferRelationHandler.java
 * @Description TODO
 * @createTime 2022年02月23日 09:08:00
 */
public class TransferRelationHandler extends AbstractBaseDao {

    /**
     * 新开户的插入
     */
    public int save(String routeId, String status, int count) {
        TransferRelation relation = new TransferRelation();
        relation.setRouteId(routeId);
        relation.setStatus(status);
        relation.setCounts(count);
        return this.insert("TRANSFERRELATION.insert", relation);
    }

    /**
     * 新开户的插入 更新
     */
    public int update(String routeId, String status, int count) {
        TransferRelation relation = new TransferRelation();
        relation.setRouteId(routeId);
        relation.setStatus(status);
        relation.setCounts(count);
        return this.update("TRANSFERRELATION.update", relation);
    }

    /**
     * 查询
     */
    public TransferRelation load(String routeId) {
        Map<String, Object> param = new HashMap<>();
        param.put(ConstVar.PARAMETER.ROUTEID, routeId);
        return this.selectOne("TRANSFERRELATION.selectByKey", param);
    }

    /**
     * 更新状态+笔数,sql中加上判断条件，只有 1,2状态的可以进行本更新
     */
    public int updateCounts(String routeId, int counts) {
        Map<String, Object> param = new HashMap<>();
        param.put(ConstVar.PARAMETER.ROUTEID, routeId);
        param.put(ConstVar.PARAMETER.COUNTS, counts);
        return this.update("TRANSFERRELATION.updateCounts", param);
    }

    /**
     * 更新状态
     */
    public int updateStatus(String routeId, String oldstatus, String newstatus) {
        Map<String, Object> param = new HashMap<>();
        param.put(ConstVar.PARAMETER.ROUTEID, routeId);
        param.put("oldstatus", oldstatus);
        param.put("newstatus", newstatus);
        return this.update("TRANSFERRELATION.updateStatus", param);
    }
}

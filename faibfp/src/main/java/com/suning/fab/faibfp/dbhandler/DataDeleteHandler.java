package com.suning.fab.faibfp.dbhandler;

import com.suning.fab.mulssyn.db.AbstractBaseDao;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/6/30
 * @Version 1.0
 */
public class DataDeleteHandler extends AbstractBaseDao {

    public int deleteProtoregData(String routeId, int id) {
        Map<String, Object> param = new HashMap<>();
        param.put("routeId", routeId);
        param.put("id", id);
        return this.delete("CUSTOMER.deleteProtoregData", param);

    }

}

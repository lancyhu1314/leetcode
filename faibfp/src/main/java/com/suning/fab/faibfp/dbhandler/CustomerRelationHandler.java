package com.suning.fab.faibfp.dbhandler;

import com.suning.fab.faibfp.bean.CustomerRelation;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.mulssyn.db.AbstractBaseDao;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/3/26
 * @Version 1.0
 */
public class CustomerRelationHandler extends AbstractBaseDao {

    public CustomerRelation load(String routeId) {
        Map<String, Object> param = new HashMap<>();
        param.put(ConstVar.PARAMETER.ROUTEID, routeId);
        return this.selectOne("CUSTOMERRELATION.selectByKey", param);
    }
}

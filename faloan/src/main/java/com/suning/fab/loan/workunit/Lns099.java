package com.suning.fab.loan.workunit;

import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：系统日切
 *
 * @Author 18049705 MYP
 * @Date Created in 10:30 2019/8/2
 * @see
 */
@Scope("prototype")

@Repository
public class Lns099 extends WorkUnit {
    String termeDate;

    @Override
    public void run() throws Exception {
        LoggerUtil.info("更新系统控制表开始");

        // 更新系统控制信息
        Map<String, Object> sqlMap = new HashMap<>();
        // 渠道日期
        sqlMap.put("predate", termeDate);
        sqlMap.put("accdate", termeDate);
        sqlMap.put("trandate", termeDate);
        sqlMap.put("nextdate", termeDate);

        int res = 0;
        try {
            res = DbAccessUtil.execute("CUSTOMIZE.update_spssysctrlinfo_test", sqlMap);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "spssysctrlinfo");
        }

        if (res != 1) {
            LoggerUtil.error("更新系统控制表失败" + sqlMap);
            throw new FabException("SPS112");
        }

        LoggerUtil.info("更新系统控制表结束,日切前日期[{}],当前日期[{}]", termeDate, termeDate);
    }

    public String getTermeDate() {
        return termeDate;
    }

    public void setTermeDate(String termeDate) {
        this.termeDate = termeDate;
    }
}

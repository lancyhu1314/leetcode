package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSON;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉贷款数据清理
 *
 * @Author 19043955
 * @Date 2021/11/30
 * @Version 1.0
 */
@Service
@Scope("prototype")
public class Lns699 extends WorkUnit {

    /**
     * 表名
     */
    private String tableName;
    /**
     * 参数json
     */
    private String uniqueKeys;


    @Override
    public void run() throws Exception {


        int batchUpdateNum = Integer.parseInt(GlobalScmConfUtil.getProperty("batchUpdateNum", "500"));
        // sqlid统一为表名的小写加上Delete
        StringBuilder builder = new StringBuilder("Archiving.");
        builder.append(tableName.toLowerCase()).append("Delete");

        if (!VarChecker.isEmpty(uniqueKeys)) {

            List<Map> list = JSON.parseArray(uniqueKeys, Map.class);

            int index = 0;

            if (list.size() < batchUpdateNum) {
                DbAccessUtil.batchUpdate(builder.toString(), list.toArray(new HashMap[list.size()]));
                return;
            }

            while (index < list.size()) {
                if (index + batchUpdateNum > list.size()) {
                    batchUpdateNum = list.size() - index;
                }
                List<Map> subList = list.subList(index, index + batchUpdateNum);
                DbAccessUtil.batchUpdate(builder.toString(), list.toArray(new HashMap[subList.size()]));
                index += batchUpdateNum;
            }
        }


    }

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @param tableName to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * @return the uniqueKeys
     */
    public String getUniqueKeys() {
        return uniqueKeys;
    }

    /**
     * @param uniqueKeys to set
     */
    public void setUniqueKeys(String uniqueKeys) {
        this.uniqueKeys = uniqueKeys;
    }
}

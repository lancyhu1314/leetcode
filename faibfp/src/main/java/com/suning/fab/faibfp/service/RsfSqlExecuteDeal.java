package com.suning.fab.faibfp.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.faibfp.service.template.RsfQuerServiceTemplate;
import com.suning.fab.mulssyn.exception.FabException;
import com.suning.rsf.provider.annotation.Implement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 * 功能描述: <br>
 * 门户调用RSF接口到发票平台执行SQL的方法
 * 〈功能详细描述〉
 *
 * @Author 21071622
 * @Date 2021/9/23
 * @Version 1.0
 */
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faibfp-sqlFaloanDeal")
public class RsfSqlExecuteDeal {
    Logger logger = LoggerFactory.getLogger(RsfSqlExecuteDeal.class);
    @Autowired
    DataSource dataDs;

    public Map<String, Object> prepare(Map<String, Object> reqMsg) throws FabException {
        //sqlExecuteDeal接口入参
        logger.info("type:" + reqMsg.get("type") + ", sql:" + reqMsg.get("sql"));
        Map<String, Object> sqlexecute = sqlexecute(reqMsg);
        //将返回值放入出口对象
        return sqlexecute;
    }

    /**
     * 门户调用RSF接口到发票平台执行SQL的方法
     *
     * @param reqMap
     * @return
     */
    public Map<String, Object> sqlexecute(Map<String, Object> reqMap) {
        logger.info("门户调用RSF接口到发票平台执行SQL的方法开始,调用信息reqMap:{}", reqMap);
        if ("query".equals(reqMap.get("type"))) {
            String sql = (String) reqMap.get("sql");
            return queryInfo(sql);
        } else if ("batch".equals(reqMap.get("type"))) {
            String sql = (String) reqMap.get("sql");
            return executeSql(sql);
        } else {//else是执行SQL
            String sql = (String) reqMap.get("sql");
            return exeSql(sql);
        }
    }

    public Map<String, Object> executeSql(String sql) {
        logger.info("获取数据库连接！");
        Connection con = null;
        Statement stmt = null;

        Map<String, Object> resMap = new HashMap<String, Object>();
        try {
            con = dataDs.getConnection();
            if (con == null) {
                resMap.put("resCode", "F");
                resMap.put("result", "sql执行失败,获取数据源失败!");
                return resMap;
            }
            con.setAutoCommit(false);
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String[] allSql = sql.split(";");
            for (String oneSql : allSql) {
                stmt.addBatch(oneSql);
            }
            stmt.executeBatch();
            con.commit();
            resMap.put("resCode", "T");
            resMap.put("message", "批量sql执行成功！");
        } catch (SQLException e) {
            logger.error("sql执行失败:{}", e);
            resMap.put("resCode", "F");
            resMap.put("message", "sql执行失败，请检查sql语句是否正确!错误信息：" + e);
            return resMap;
        } finally {
            if (null != con) {
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}", e);
                }
            }
            if (null != stmt) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}", e);
                }
            }
        }
        return resMap;
    }

    private Map<String, Object> queryInfo(String sql) {
        Map<String, Object> resMap = new HashMap<String, Object>();
        resMap.put("resCode", "T");//先默认是成功的,如果有失败后面后覆盖掉
        List<Map<String, Object>> detailList = new ArrayList<Map<String, Object>>();
        PreparedStatement ps = null;
        Connection con = null;
        try {
            con = dataDs.getConnection();
            if (con == null) {
                resMap.put("resCode", "F");
                resMap.put("message", "sql执行失败,无法获取数据源!");
                return resMap;
            }
            ps = con.prepareStatement(sql);
            //执行单个结果集的sql语句
            ResultSet rs = ps.executeQuery();
            //获取表结构
            ResultSetMetaData rsMd = rs.getMetaData();
            //得到表结构的字段个数
            int columnsNum = rsMd.getColumnCount();
            String[] title = new String[columnsNum];
            String titleString = "";
            for (int i = 0; i < columnsNum; i++) {
                title[i] = rsMd.getColumnName(i + 1);
                titleString += rsMd.getColumnName(i + 1) + ",";
            }
            titleString = titleString
                    .substring(0, titleString.length() - 1);
            while (rs.next()) {
                Map<String, Object> rsmap = new HashMap<String, Object>();
                for (int i = 0; i < columnsNum; i++) {
                    rsmap.put(title[i], rs.getString(title[i]));
                }
                detailList.add(rsmap);
                int totalnum = rs.getRow();
                if (totalnum > 100000) {
                    resMap.put("resCode", "F");
                    resMap.put("message", "查询的数据量过大,暂时不支持导出！建议先用select count(1) from 的SQL查询一下数据条数！");
                    return resMap;
                }
            }
            resMap.put("title", title);
            resMap.put("detailList", detailList);
            resMap.put("titleString", titleString);
            resMap.put("message", "查询sql执行成功");
        } catch (Exception e) {
            logger.error("发生SQLException", e);
            resMap.put("resCode", "F");
            resMap.put("message", "sql执行失败，请检查sql语句是否正确!错误信息：" + e);
        } finally {
            if (null != con) {
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}", e);
                }
            }
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}", e);
                }
            }
        }
        return resMap;
    }

    private Map<String, Object> exeSql(String sql) {
        Map<String, Object> resMap = new HashMap<String, Object>();
        resMap.put("resCode", "T");//先默认是成功的,如果有失败后面后覆盖掉
        PreparedStatement ps = null;
        Connection con = null;
        try {
            con = dataDs.getConnection();
            if (con == null) {
                resMap.put("resCode", "F");
                resMap.put("message", "sql执行失败,无法获取数据源!");
                return resMap;
            }
            ps = con.prepareStatement(sql);
            ps.execute();
            resMap.put("message", "sql执行成功");
        } catch (Exception e) {
            logger.error("sql执行失败:{}", e);
            resMap.put("resCode", "F");
            resMap.put("message", "sql执行失败，请检查sql语句是否正确!错误信息：" + e);
        } finally {
            if (null != con) {
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}", e);
                }
            }
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}", e);
                }
            }
        }
        return resMap;
    }

}

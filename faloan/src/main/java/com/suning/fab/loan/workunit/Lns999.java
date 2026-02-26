package com.suning.fab.loan.workunit;

import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.JsonTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Scope("prototype")
@Repository
public class Lns999 extends WorkUnit {
    Logger logger = LoggerFactory.getLogger(Lns999.class);

    String type;
    String sql;

    String detailList;
    String titleString;


    @Autowired
    DataSource dataDs;
    @Override
    public void run() throws Exception {
        logger.info("门户调用RSF接口到发票平台执行SQL的方法开始");
        if("query".equals(type)){
            String sqlDeal = sql;
            queryInfo(sqlDeal);
        }else if ("batch".equals(type)){
            String sqlDeal = sql;
            executeSql(sqlDeal);
        }else{//else是执行SQL
            String sqlDeal = sql;
            exeSql(sqlDeal);
        }

    }

    public Map<String, Object> executeSql(String sql) {
        logger.info("获取数据库连接！");
        Connection con = null;
        Statement stmt = null;

        Map<String, Object> resMap = new HashMap<String, Object>();
        try {
            con = dataDs.getConnection();
            if(con==null){
                resMap.put("resCode", "F");
                resMap.put("result", "sql执行失败,获取数据源失败!");
                return resMap;
            }
            con.setAutoCommit(false);
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String[] allSql = sql.split(";");
            for(String oneSql:allSql){
                stmt.addBatch(oneSql);
            }
            stmt.executeBatch();
            con.commit();
            resMap.put("resCode", "T");
            resMap.put("message", "批量sql执行成功！");
        } catch (SQLException e) {
            logger.error("sql执行失败:{}",e);
            resMap.put("resCode", "F");
            resMap.put("message", "sql执行失败，请检查sql语句是否正确!错误信息：" + e);
            return resMap;
        } finally {
            if(null!=con){
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}",e);
                }
            }
            if(null!=stmt){
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}",e);
                }
            }
        }
        return resMap;
    }

    private Map<String, Object> queryInfo(String sql) {
        String[] title;
        List<Map<String, Object>> detail = new ArrayList<Map<String, Object>>();


        Map<String, Object> resMap = new HashMap<String, Object>();
        resMap.put("resCode", "T");//先默认是成功的,如果有失败后面后覆盖掉

        Connection con = null;
        Statement stat = null;
        try {
            con = dataDs.getConnection();
            if (con == null) {
                resMap.put("resCode", "F");
                resMap.put("message", "sql执行失败,无法获取数据源!");
                return resMap;
            }
            // 3.获取执行SQL 语句对象
            stat = con.createStatement();
//            ps = con.prepareStatement(sql);
            String[] allSql = sql.split(";");
            for (String oneSql : allSql) {
                ResultSet rs = stat.executeQuery(oneSql);
                //获取表结构
                ResultSetMetaData rsMd = rs.getMetaData();
                //得到表结构的字段个数
                int columnsNum = rsMd.getColumnCount();
                title = new String[columnsNum];

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
                    detail.add(rsmap);
                    int totalnum = rs.getRow();
                    if (totalnum > 100000) {
                        resMap.put("resCode", "F");
                        resMap.put("message", "查询的数据量过大,暂时不支持导出！建议先用select count(1) from 的SQL查询一下数据条数！");
                        return resMap;
                    }
                }
                detailList = JsonTransfer.ToJson(detail);
            }
            //stat.close();
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
            if(null!=stat){
                try {
                	stat.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}",e);
                }
            }
        }
        return resMap;
    }

    private Map<String, Object> exeSql(String sql){
        Map<String, Object> resMap = new HashMap<String, Object>();
        resMap.put("resCode", "T");//先默认是成功的,如果有失败后面后覆盖掉
        PreparedStatement ps = null;
        Connection con = null;
        try{
            con = dataDs.getConnection();
            if(con==null){
                resMap.put("resCode", "F");
                resMap.put("message", "sql执行失败,无法获取数据源!");
                return resMap;
            }
            ps = con.prepareStatement(sql);
            ps.execute();
            resMap.put("message", "sql执行成功");
        }catch(Exception e){
            logger.error("sql执行失败:{}", e);
            resMap.put("resCode", "F");
            resMap.put("message", "sql执行失败，请检查sql语句是否正确!错误信息：" + e);
        }finally{
            if(null!=con){
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}",e);
                }
            }
            if(null!=ps){
                try {
                    ps.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败:{}",e);
                }
            }
        }
        return resMap;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

}

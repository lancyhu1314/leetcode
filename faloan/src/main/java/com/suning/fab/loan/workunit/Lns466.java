package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONArray;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RpyInfosel;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnsinterfaceex;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.RepayWaySupporter;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.framework.dal.pagination.PaginationResult;
import org.aspectj.weaver.ast.Var;
import org.mortbay.util.StringUtil;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：还款明细查询
 *
 * @Author 16090227@cnsuning.com
 * @Date Created in 13:57 2020/10/28
 * @see
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns466 extends WorkUnit {
    private String acctNo;

    private String brc;
    String pkgList;


    @Override
    public void run() throws Exception{
        if(VarChecker.isEmpty(acctNo) ){
            throw new FabException("ACC103");
        }
        Map<String,Object> queryparam = new HashMap<>();
        queryparam.put("acctno", acctNo);
        queryparam.put("brc", brc);
        queryparam.put("key", ConstantDeclare.KEYNAME.HKMX);
        List<TblLnsinterfaceex> interfaceexs;
        try {
            interfaceexs = DbAccessUtil.queryForList("AccountingMode.query_lnsinterfaceex", queryparam, TblLnsinterfaceex.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_lnsinterfaceex");
        }
        //存放每一期的还款明细
        Map<Integer,Map<String,Object>>  searchList=new HashMap();
        //查询所有账户还款明细集合
        for(TblLnsinterfaceex interfaceex:interfaceexs) {
            //查询还款明细value字段
            if (!VarChecker.isEmpty(interfaceex.getValue())) {
                String value=interfaceex.getValue();
                //兼容老数据，只有压缩的数据才进行解压
                if(!value.contains("{")){
                    value=StringDecodeUtil.decompressData(value);
                }
                List<Map<String, Object>> searchArray = (List<Map<String, Object>>) JSONArray.parse(value);
                for (Map<String, Object> searchItem : searchArray) {
                    Map<String, Object> searchMap;
                    Integer period = (Integer) searchItem.get("repayterm");
                    if (searchList.get(period) == null) {
                        searchMap = new HashMap();
                        searchMap.put("dintAmt", ((BigDecimal) searchItem.get("forfeitAmt")).doubleValue());
                        searchMap.put("repayterm", period);
                    } else {
                        searchMap = searchList.get(period);
                        searchMap.put("dintAmt", new BigDecimal((Double) searchMap.get("dintAmt")).add((BigDecimal)searchItem.get("forfeitAmt")).doubleValue());
                    }
                    addDetailAmt(searchMap, searchItem.get("nintBillStatus")==null?"U":searchItem.get("nintBillStatus").toString(), (BigDecimal) searchItem.get("intAmt"));
                    searchList.put(period, searchMap);
                }
            }
        }
        List<Map<String,Object>> searchResult=new ArrayList<>();
        for(Map.Entry<Integer,Map<String,Object>> temp:searchList.entrySet()){
            searchResult.add(temp.getValue());
        }
        pkgList = JsonTransfer.ToJson(searchResult);
    }

    /**
     * 查询结果添加金额
     * @param searchMap
     * @param billStatus
     * @param tranamt
     */
    public void addDetailAmt(Map<String,Object> searchMap,String billStatus,BigDecimal tranamt){
        String itemKey="";
        //根据不同的账本类型进行转换处理
        if("N".equals(billStatus)){
            itemKey="normalNintAmt";
        }else if("B".equals(billStatus)){
            itemKey="baddebtsNintAmt";
        }else if("O".equals(billStatus)){
            itemKey="overduNintAmt";
        }else if("L".equals(billStatus)){
            itemKey="languishNintAmt";
        }else if("U".equals(billStatus)){
            itemKey="undifferNintAmt";
        }
        if(searchMap.get(itemKey)==null){
            searchMap.put(itemKey,tranamt.doubleValue());
        }else{
            searchMap.put(itemKey,new BigDecimal((Double)searchMap.get(itemKey)).add(tranamt).doubleValue());
        }
    }


    /**
     * Gets the value of acctNo.
     *
     * @return the value of acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }

    /**
     * Sets the acctNo.
     *
     * @param acctNo acctNo
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;

    }

    public String getPkgList() {
        return pkgList;
    }

    public void setPkgList(String pkgList) {
        this.pkgList = pkgList;
    }

    public String getBrc() {
        return brc;
    }

    public void setBrc(String brc) {
        this.brc = brc;
    }


}

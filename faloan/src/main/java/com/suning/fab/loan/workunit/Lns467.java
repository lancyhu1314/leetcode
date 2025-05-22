package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.domain.TblLnsinterfaceex;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.StringDecodeUtil;
import com.suning.fab.loan.utils.StringUtil;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：批量查询账户剩余本金
 *
 * @Author 16090227@cnsuning.com
 * @Date Created in 13:57 2020/12/04
 * @see
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns467 extends WorkUnit {



    public String getPkgList1() {
        return pkgList1;
    }

    public void setPkgList1(String pkgList1) {
        this.pkgList1 = pkgList1;
    }

    public int getTotalLine() {
        return totalLine;
    }

    public void setTotalLine(int totalLine) {
        this.totalLine = totalLine;
    }


    String pkgList1;
    int totalLine;

    @Override
    public void run() throws Exception{
        ListMap pkgList=getTranctx().getRequestDict("pkgList");
        //判断查询参数是否为空或者条数超过范围
        if(VarChecker.isEmpty(pkgList)|| pkgList.size()>20 ){
            throw new FabException("LNS236");
        }
        List<Map> resultList=new ArrayList<>();
        try {
            for (PubDict pkg : pkgList.getLoopmsg()) {
                Map acctMap = new HashMap();
                String acctNo = StringUtil.parseString(pkg.getAcctNo());
                String brc = StringUtil.parseString(pkg.getBrc());
                Map<String, Object> queryparam = new HashMap<>();
                queryparam.put("acctno", acctNo);
                queryparam.put("brc", brc);
                queryparam.put("acctstat", StringUtil.getAcctStatus(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN));
                Map acctResult = DbAccessUtil.queryForMap("AccountingMode.query_lnsaccountdyninfo_bal", queryparam);
                acctMap.put("remainPrin", acctResult.get("currbal") == null ? 0.00 : acctResult.get("currbal"));
                acctMap.put("brc", brc);
                acctMap.put("acctNo", acctNo);
                resultList.add(acctMap);
            }
        } catch (FabSqlException e1) {
        throw new FabException(e1, "SPS103", "query_lnsaccountdyninfo_bal");
    }
        pkgList1 = JsonTransfer.ToJson(resultList);
        totalLine=resultList.size();
    }




}

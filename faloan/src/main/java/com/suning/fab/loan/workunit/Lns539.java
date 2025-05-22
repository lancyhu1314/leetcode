package com.suning.fab.loan.workunit;

import com.suning.fab.loan.bo.BillSplitter;
import com.suning.fab.loan.bo.IntegerObj;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.la.NonAccrual;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈贷款核销子交易〉
 * 〈功能详细描述〉：动态表
 *
 * @Author
 * @Date 2022-12-06
 * @see
 */
@Scope("prototype")
@Repository
public class Lns539 extends WorkUnit {
    //借据号
    String receiptNo;
    // 罚息余额
    FabAmount dintAmt = new FabAmount();//总未还罚息;

    TblLnsbasicinfo lnsbasicinfo;

    @Override
    public void run() throws Exception {

        //查询主文件
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno1", receiptNo);
        param.put("openbrc", tranctx.getBrc());
        try {
            //取主文件信息
            lnsbasicinfo = DbAccessUtil.queryForObject("CUSTOMIZE.query_non_acctno", param, TblLnsbasicinfo.class);

        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_non_acctno");
        }



        //查询动态表
        param.put("acctno", lnsbasicinfo.getAcctno());
        param.put("brc", tranctx.getBrc());
        //获取动态表信息
        List<Map<String, Object>> lnsaccountdyninfoList = null;
        try {
            lnsaccountdyninfoList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsaccountdyninfo", param);
        } catch (FabSqlException e) {
            LoggerUtil.error("查询lnsaccountdyninfo表错误{}", e);
        }

        for(Map<String, Object> map :lnsaccountdyninfoList){
            if(!VarChecker.isEmpty(map.get("acctstat")) && "DINT.N".equals(map.get("acctstat"))){
                dintAmt.selfAdd(new FabAmount(Double.valueOf(map.get("currbal").toString())));
            }
        }


    }


    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public FabAmount getDintAmt() {
        return dintAmt;
    }

    public void setDintAmt(FabAmount dintAmt) {
        this.dintAmt = dintAmt;
    }

    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }
}

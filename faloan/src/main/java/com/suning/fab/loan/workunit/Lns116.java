package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsprefundaccount;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;


/**
 * @author LH
 *
 * @version V1.0.1
 *
      备份主文件
 *

 */
@Scope("prototype")

@Repository
public class Lns116 extends WorkUnit {

    //账号
    String acctNo;

    @Override
    public void run() throws Exception {
        TranCtx ctx = getTranctx();

        Map<String,Object> backParam = new HashMap<String,Object>();
        backParam.put("acctno", acctNo);
        backParam.put("brc", ctx.getBrc());
        backParam.put("memo", ctx.getTranCode());

        try{
            /*展期时将该条记录插入到lnsbasicinfoback表中*/
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoback", backParam);

        }catch (FabSqlException e){
            throw new FabException(e, "SPS100", "lnsbasicinfoback");
        }

        //更新memo为交易码
        try{
            DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfoback", backParam );
        } catch(FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsbasicinfoback");
        }

    }

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }



}
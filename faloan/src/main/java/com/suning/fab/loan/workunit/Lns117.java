package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanInterestSettlementUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanInterestSettlementUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：核销还款后 动态表金额转回
 *
 * @Author 18049705
 * @Date Created in 19:08 2018/12/14
 * @see
 */
@Scope("prototype")
@Repository
public class Lns117 extends WorkUnit {

    String acctNo;
    //渠道流水
    String serialNo;
    LoanAgreement la;
    @Autowired
    LoanEventOperateProvider eventProvider;

    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;

    @Override
    public void run() throws Exception {

        TranCtx ctx = getTranctx();


//        if (la == null)
//            la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
        //查询主文件  --结息到当前日期 可能更改主文件的合同余额
        Map<String, Object> param = new HashMap<String, Object>();
//        //账号
//        param.put("acctno", acctNo);
//        //机构
//        param.put("openbrc", ctx.getBrc());
//        TblLnsbasicinfo lnsbasicinfo;
//        try {
//            //取主文件信息
//            lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
//
//        } catch (FabSqlException e) {
//            throw new FabException(e, "SPS103", "Lnsbasicinfo");
//        }

        /*
         *  因为结息 是在交易结束时插动态表  所以需要手动求和
         */
        //统计各个类型的账单   正常本金账单还需要加上主文件表的合同余额  用map储存
//        Map<String, FabAmount> tzhxMap = new HashMap<>();
//        //查询已有的账单表
//        param.put("acctno", acctNo);
//        param.put("brc", ctx.getBrc());
//        List<TblLnsbill> tblLnsBills;
//        try {
//            tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_runingbills", param, TblLnsbill.class);
//        } catch (FabSqlException e) {
//            throw new FabException(e, "SPS103", "query_runingbills");
//        }
//
//        if (new FabAmount(lnsbasicinfo.getContractbal()).isPositive())
//            tzhxMap.put("PRIN.N", new FabAmount(lnsbasicinfo.getContractbal()));
//        String key;
//        for (TblLnsbill lnsBill : tblLnsBills) {
//            if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
//                    ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
//                    ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
//                    .contains(lnsBill.getBilltype())
//            ) {
//                key = ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_DINT + "." + lnsBill.getBillstatus();
//            } else {
//                key = lnsBill.getBilltype() + "." + lnsBill.getBillstatus();
//            }
//            if (null != tzhxMap.get(key)) {
//                tzhxMap.get(key).selfAdd(lnsBill.getBillbal());
//            } else {
//                tzhxMap.put(key, new FabAmount(lnsBill.getBillbal()));
//            }
//        }
//
//
//        //抛多次事件，一个账户类型一个金额一个事件
//        for (Map.Entry<String, FabAmount> tzhx : tzhxMap.entrySet()) {
//            LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
//                    tzhx.getKey().split("\\.")[0], tzhx.getKey().split("\\.")[1], new FabCurrency());
//
//            sub.operate(lnsAcctInfo, null, tzhx.getValue(), la.getFundInvest(), ConstantDeclare.BRIEFCODE.TZHX,
//                    ctx);
//            //登记本金户销户事件
//            eventProvider.createEvent(ConstantDeclare.EVENT.LNCNLVRFIN, tzhx.getValue(),
//                    lnsAcctInfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.TZHX, ctx, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
//        }


        //更新主文件表的 状态为核销  利息计提标志为close
        //Lns505会更改主文件贷款状态,批量是通过dps控制核销借据不跑批量	20190527|14050183
        try {

            param.put("acctno", acctNo);
            param.put("brc", ctx.getBrc());
            param.put("loanstat", la.getContract().getLoanStat());
            /*更新主文件表中该数据的贷款状态CA,以及利息计提标志CLOSE*/
            DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstatEx", param);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "update_lnsbasicinfo_loanstatEx");
        }


    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }

    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;
    }

    public AccountOperator getSub() {
        return sub;
    }

    public void setSub(AccountOperator sub) {
        this.sub = sub;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    public LoanAgreement getLa() {
        return la;
    }

    public void setLa(LoanAgreement la) {
        this.la = la;
    }
}

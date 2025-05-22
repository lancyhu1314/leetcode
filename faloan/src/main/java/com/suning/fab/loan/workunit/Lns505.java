package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see -贷款状态更新
 *
 * @param -acctNo 贷款账号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns505 extends WorkUnit {

    String acctNo;

    @Override
    public void run() throws Exception {

        TranCtx ctx = getTranctx();
        Map<String, Object> param = new HashMap<>();
        param.put("acctno", acctNo);
        param.put("openbrc", ctx.getBrc());
        TblLnsbasicinfo lnsbasicinfo;
        try {
            lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbasicinfo");
        }
        if (null == lnsbasicinfo) {
            throw new FabException("SPS104", "lnsbasicinfo");
        }


        LoggerUtil.info("更新前贷款状态:" + lnsbasicinfo.getLoanstat());

        Map<String, Object> billMap = new HashMap<String, Object>();
        billMap.put("acctno", acctNo);
        billMap.put("brc", ctx.getBrc());
        billMap.put("feetypes", LoanFeeUtils.getFeetypes());
        List<TblLnsbill> tblLnsBills;
        try {
            tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_runingprinintbills", billMap, TblLnsbill.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "query_runingprinintbills");
        }
		if (tblLnsBills.isEmpty())
			return;
		String loanStat = ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL;
        //最后一条账单的结束日期如果等于合同到期日
        if (!CalendarUtil.before(tblLnsBills.get(tblLnsBills.size() - 1).getEnddate(), lnsbasicinfo.getContduedate())) {

            /**
             *  存在本金不转列的账单 ，需要取未结清账单的借据 最大期数的 *转列* 账本的状态作为 最新账单的状态
             *  add  at 2019-03-13
             */
            // 初始化，取最后一期的账本的Billstatus
            List<TblLnsbill> maxPeriodLnsbills;
            try {
                maxPeriodLnsbills = DbAccessUtil.queryForList("Lnsbill.query_maxperiod_lnsbill", billMap, TblLnsbill.class);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS103", "query_maxperiod_lnsbill");
            }
            String  billstatus;
            //因为存在本金利息账单，所以一定有值
            if(maxPeriodLnsbills.isEmpty()){
                billstatus = ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL;
            }else {
                billstatus  =  maxPeriodLnsbills.get(maxPeriodLnsbills.size()-1).getBillstatus();
            }

            //取 最大期数所有账单 中 最大的billtype
            for(TblLnsbill lnsbill : maxPeriodLnsbills){
                if(compareBillType(billstatus,lnsbill.getBillstatus())<0)
                    billstatus = lnsbill.getBillstatus();
            }

            //最后一条账单的状态如果不是正常状态，取最后一条账单的状态更新主文件为全部逾期(呆滞/呆账)
            if (!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE)
                    .contains(billstatus)) {
                loanStat = billstatus;
            } else {
                for (TblLnsbill lnsbill : tblLnsBills) {
                    if (!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE)
                            .contains(lnsbill.getBillstatus())) {
                        loanStat = ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR;
                        break;
                    }
                }
            }
        } else {
            //之前期如果有不是正常状态的未结清账单，则整比贷款状态更新为部分逾期，否则为正常
            for (TblLnsbill lnsbill : tblLnsBills) {
                if (!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE)
                        .contains(lnsbill.getBillstatus())) {
                    loanStat = ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR;
                    break;
                }
            }
        }
        //更新后贷款状态相同，不更新
        if(lnsbasicinfo.getLoanstat().trim().equals(loanStat)){
            return;
        }
        LoggerUtil.info("更新后贷款状态:" + loanStat);
        Map<String, Object> basicMap = new HashMap<String, Object>();
        basicMap.put("loanstat", loanStat);
        basicMap.put("acctno", acctNo);
        basicMap.put("openbrc", ctx.getBrc());
        basicMap.put("oldloanstat", lnsbasicinfo.getLoanstat());
        basicMap.put("modifyDate", ctx.getTranDate());

        if( !(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL,
                ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION).contains(lnsbasicinfo.getLoanstat()) &&
                !ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(loanStat)) ) {
            int count;
            try {
                count = DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstat", basicMap);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "lnsbasicinfo");
            }
            if (1 != count) {
                throw new FabException("SPS102", "lnsbasicinfo");
            }
        }
    }

    /**
     *  比较账单状态 顺序从小到大为：N正常G宽限期O逾期L呆滞B呆账
     * @param billtypeA  账单状态
     * @param billtypeB  账单状态
     * @return  相等：0 ， billtypeA 小：-1 ，  billtypeA 大：1
     */
    private Integer  compareBillType(String billtypeA,String billtypeB){
        return
                Integer.valueOf(PropertyUtil.getPropertyOrDefault("billstatus." + billtypeA, "0"))
                        .compareTo(
                                Integer.valueOf(PropertyUtil.getPropertyOrDefault("billstatus." + billtypeB, "0")));
    }
    /**
     * @return the acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }

    /**
     * @param acctNo the acctNo to set
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

}

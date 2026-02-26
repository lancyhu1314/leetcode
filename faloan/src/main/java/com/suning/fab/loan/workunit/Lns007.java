package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsInterfaceQueryM;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.DynamicCapUtil;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author
 * @Date Created in 19:13 2019/10/11
 * @see
 */

@Scope("prototype")
@Repository
public class Lns007 extends WorkUnit {


    String acctNo;
    String brc;


    public void run() throws Exception {

        LoanAgreement la= LoanAgreementProvider.genLoanAgreementFromDB(acctNo, getTranctx());
        // 主文件
        TblLnsbasicinfo lnsbasicinfo;
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", acctNo);
        param.put("openbrc", brc);
        try {
            //取主文件信息
            lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbasicinfo");
        }

        if (null == lnsbasicinfo) {
            throw new FabException("SPS104", "lnsbasicinfo");
        }

        // 封顶值
        TblLnspenintprovreg lnspenintprovreg;
        param = new HashMap<>();
        param.put("billtype", ConstantDeclare.KEYNAME.DTFD);
        param.put("receiptno", acctNo);
        param.put("brc", brc);
        try {
            lnspenintprovreg = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "Lnspenintprovreg");
        }

        // 未还本金
        param = new HashMap<String, Object>();
        param.put("acctno", acctNo);
        param.put("brc", brc);
        BigDecimal orgPrin;
        try {
            orgPrin = DbAccessUtil.queryForObject("CUSTOMIZE.query_orgPrin", param, BigDecimal.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbill");
        }

        //幂等求出最大还款日
        Map<String, Object> query = new HashMap<>();
        query.put("acctno", acctNo);
        query.put("brc", brc);
        Map interfaceMap=null;
        try {
             interfaceMap = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsinterfaceMaxRepayDate", query);
        } catch (FabException e) {
            throw new FabException(e, "SPS103", "LNSINTERFACE");
        }
        String lastRepayDate="";//上次还款日
        if(interfaceMap!=null&&interfaceMap.get("accdate")!=null){
            lastRepayDate=interfaceMap.get("accdate").toString();
        }
        String maxDate="";//合同到期日 还款日  最后封顶日 的 最大日期进行计算
        //等本等息 在合同到期日前封顶  取lastrepaydate 与 contduedate的 最大值
        if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())&&CalendarUtil.beforeAlsoEqual(lnspenintprovreg.getEnddate().toString(),lnsbasicinfo.getContduedate())){
            if(CalendarUtil.beforeAlsoEqual(lastRepayDate,lnsbasicinfo.getContduedate())){
                maxDate=lnsbasicinfo.getContduedate();
            }else{
                maxDate=lastRepayDate;
            }
        }else{
            if(CalendarUtil.beforeAlsoEqual(lnspenintprovreg.getEnddate().toString(),lastRepayDate)){
                maxDate=lastRepayDate;
            }else{
                maxDate=lnspenintprovreg.getEnddate().toString();
            }
        }
        FabAmount accumulatePrin = new FabAmount(orgPrin.doubleValue());
        double repairAmount = BigDecimal.valueOf(accumulatePrin.getVal()).multiply(new FabRate(lnspenintprovreg.getTaxrate().doubleValue()).getDayRate())
                .setScale(2, BigDecimal.ROUND_HALF_UP ).multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(maxDate,tranctx.getTranDate()))).doubleValue();
        //补动态封顶金额
        DynamicCapUtil.capRecord2DB(la, getTranctx(), lnspenintprovreg,  accumulatePrin.getVal(), repairAmount,getTranctx().getTranDate());

    }

    /**
     * @return the brc
     */
    public String getBrc() {
        return brc;
    }

    /**
     * @param brc to set
     */
    public void setBrc(String brc) {
        this.brc = brc;
    }


    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }
}

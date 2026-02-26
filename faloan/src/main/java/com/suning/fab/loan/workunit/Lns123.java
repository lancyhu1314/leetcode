package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author 19043955
 * @Date 2020/5/14 11:12
 * @Version 1.0
 */
@Scope("prototype")
@Repository
public class Lns123 extends WorkUnit {

    //----入参
    //借新还旧原借据号
    String exAcctno;
    //借新还旧原机构号
    String exBrc;


    //----返回
    FabAmount dynamicCapValue;


    @Override
    public void run() throws Exception {

        TblLnsbasicinfo lnsinfo;
        Map<String, Object> param = new HashMap<>();
        param.put("acctno", exAcctno);
        param.put("openbrc", exBrc);
        try {
            //取主文件信息
            lnsinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbasicinfo");
        }

        if (null == lnsinfo) {
            throw new FabException("SPS104", "lnsbasicinfo");
        }

        //查询 lnspennintprovreg DTFD的值
        TblLnspenintprovreg lnspenintprovreg;
        param.put("billtype", ConstantDeclare.KEYNAME.DTFD);
        param.put("receiptno", exAcctno);
        param.put("brc", exBrc);
        try {
            lnspenintprovreg = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "Lnspenintprovreg");
        }
        BigDecimal dynamicCapVal;
        // 如果没有封顶值，设置为0
        if (VarChecker.isEmpty(lnspenintprovreg)) {
            dynamicCapValue = new FabAmount(0.00);
            return;
        }

        if ( ConstantDeclare.REPAYWAY.isEqualInterest(lnsinfo.getRepayway()) ) {
        	//等本等息封顶周期为整个合同周期
        	String lastEndDate = lnspenintprovreg.getEnddate().toString();
        	if( CalendarUtil.after(lastEndDate,  tranctx.getTranDate()) )
        		lastEndDate = tranctx.getTranDate();
            // 计算封顶的起始日期-计止日期差：A
            int ndays = CalendarUtil.actualDaysBetween(lnspenintprovreg.getBegindate().toString(), lnspenintprovreg.getEnddate().toString());
            // 计算封顶起始日期--当前日期差:B
            int days = CalendarUtil.actualDaysBetween(lnspenintprovreg.getBegindate().toString(), lastEndDate);
            // 总的封顶值*B/A,也有可能当前日期>=计止日期，取表里面的金额
            dynamicCapVal = ndays >= days ? lnspenintprovreg.getTotalinterest() : lnspenintprovreg.getTotalinterest().multiply(new BigDecimal(days)).divide(new BigDecimal(ndays)).setScale(2, BigDecimal.ROUND_HALF_UP);

        } else {
            dynamicCapVal = lnspenintprovreg.getTotalinterest();
        }
        //转换成标准格式
        dynamicCapValue = new FabAmount(dynamicCapVal.doubleValue());

    }

    /**
     * @return the exAcctno
     */
    public String getExAcctno() {
        return exAcctno;
    }

    /**
     * @param exAcctno to set
     */
    public void setExAcctno(String exAcctno) {
        this.exAcctno = exAcctno;
    }

    /**
     * @return the exBrc
     */
    public String getExBrc() {
        return exBrc;
    }

    /**
     * @param exBrc to set
     */
    public void setExBrc(String exBrc) {
        this.exBrc = exBrc;
    }

    /**
     * @return the dynamicCapValue
     */
    public FabAmount getDynamicCapValue() {
        return dynamicCapValue;
    }

    /**
     * @param dynamicCapValue to set
     */
    public void setDynamicCapValue(FabAmount dynamicCapValue) {
        this.dynamicCapValue = dynamicCapValue;
    }
}

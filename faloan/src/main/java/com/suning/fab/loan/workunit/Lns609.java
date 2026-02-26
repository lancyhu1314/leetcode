package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.domain.TblLnspenintprovregdtl;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705 MYP
 * @Date Created in 19:13 2019/10/11
 * @see
 */
@Scope("prototype")
@Repository
public class Lns609 extends WorkUnit {

    String startDate;
    String endDate;
    String acctNo;
    FabAmount sumrFint;

    @Override
    public void run() throws Exception{

        //查询基本信息给子交易句柄 并判断产品类型
        TblLnsbasicinfo lnsinfo;
        Map<String,Object> param = new HashMap<String,Object>();
        param.put("acctno", acctNo);
        param.put("openbrc", tranctx.getBrc());
        try {
            //取主文件信息
            lnsinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS103", "lnsbasicinfo");
        }

        if (null == lnsinfo){
            throw new FabException("SPS104", "lnsbasicinfo");
        }

        if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsinfo.getLoanstat()))
            return;

        TblLnspenintprovreg lnspenintprovreg = new TblLnspenintprovreg();
        lnspenintprovreg.setEnddate(java.sql.Date.valueOf(endDate));
        lnspenintprovreg.setBegindate(java.sql.Date.valueOf(startDate));
        lnspenintprovreg.setBilltype(ConstantDeclare.KEYNAME.DTFD);
        lnspenintprovreg.setBrc(tranctx.getBrc());
        lnspenintprovreg.setReceiptno(acctNo);
        lnspenintprovreg.setCcy("01");
        lnspenintprovreg.setTaxrate(new BigDecimal("0.24"));
        lnspenintprovreg.setTotalinterest(BigDecimal.valueOf(sumrFint.getVal()));
        lnspenintprovreg.setTotaltax(BigDecimal.valueOf(0.00));
        lnspenintprovreg.setTotallist(1);
        lnspenintprovreg.setReserv2("lns609");//数据归集标志

        //直接插入表中 计提直接更新
        try{
            DbAccessUtil.execute("Lnspenintprovreg.insert", lnspenintprovreg);
        }catch (FabSqlException e){
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {

                throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            }
            throw new FabException(e, "SPS100", "Lnspenintprovreg");
        }
        TblLnspenintprovregdtl penintprovregdtl = new TblLnspenintprovregdtl();

        //登记动态封顶 明细
        penintprovregdtl.setReceiptno(acctNo);
        penintprovregdtl.setBrc(tranctx.getBrc());
        penintprovregdtl.setCcy("01");
        penintprovregdtl.setTrandate(Date.valueOf(tranctx.getTranDate()));
        penintprovregdtl.setSerseqno(tranctx.getSerSeqNo());
        penintprovregdtl.setTxnseq( 0 );//默认给0，每次只会新增一条数据
        penintprovregdtl.setPeriod(0);//默认给0
        penintprovregdtl.setListno(1);
        penintprovregdtl.setBilltype(ConstantDeclare.KEYNAME.DTFD);
        penintprovregdtl.setTaxrate(0.00);
        penintprovregdtl.setBegindate(lnspenintprovreg.getEnddate());
        penintprovregdtl.setEnddate(Date.valueOf(tranctx.getTranDate()));
        penintprovregdtl.setTimestamp(new java.util.Date().toString());
        penintprovregdtl.setInterest(sumrFint.getVal());
        penintprovregdtl.setTax(0.00);
        penintprovregdtl.setReserv1("0.00");//剩余本金,迁移过来的数据
        penintprovregdtl.setReserv2("0.24");//动态封顶利率

        //登记详表
        if( new FabAmount(penintprovregdtl.getInterest()).isPositive() )
        {
            try {
                DbAccessUtil.execute("Lnspenintprovregdtl.insert", penintprovregdtl);
            }catch (FabSqlException e){

                throw new FabException(e, "SPS100", "Lnspenintprovregdtl");
            }
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

    /**
     * Gets the value of sumrFint.
     *
     * @return the value of sumrFint
     */
    public FabAmount getSumrFint() {
        return sumrFint;
    }

    /**
     * Sets the sumrFint.
     *
     * @param sumrFint sumrFint
     */
    public void setSumrFint(FabAmount sumrFint) {
        this.sumrFint = sumrFint;

    }

    /**
     * Gets the value of startDate.
     *
     * @return the value of startDate
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * Sets the startDate.
     *
     * @param startDate startDate
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;

    }

    /**
     * Gets the value of endDate.
     *
     * @return the value of endDate
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * Sets the endDate.
     *
     * @param endDate endDate
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;

    }
}

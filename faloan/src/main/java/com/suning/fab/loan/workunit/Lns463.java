/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Lns502.java
 * Author:
 * Date:
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAmortizeplan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉：摊销调整
 *
 * @author
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns463 extends WorkUnit {

    String serialNo;		//业务流水号
    String acctNo;			//贷款账号
    String tranBrc;			//交易机构
    String feeType;			//费用类型
    String adjustType;		//调整类型
    FabAmount adjustAmt;	//摊销调整金额

    @Autowired
    LoanEventOperateProvider eventProvider;

    @Override
    public void run() throws Exception {

        TranCtx ctx = getTranctx();

		if( VarChecker.isEmpty(serialNo) || 
			VarChecker.isEmpty(acctNo) || 
			VarChecker.isEmpty(tranBrc) || 
			VarChecker.isEmpty(feeType) || 
			VarChecker.isEmpty(adjustType) )
		{
			throw new FabException("LNS055", "接口入参");
		}
		
        //取主文件信息
        TblLnsbasicinfo lnsbasicinfo = null;
        Map<String,Object> param = new HashMap<String,Object>();
        param.put("acctno", acctNo);
        param.put("openbrc", ctx.getBrc());
        try {
            lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        }
        catch (FabSqlException e){
            throw new FabException(e, "SPS103", "lnsbasicinfo");
        }

        if (null == lnsbasicinfo){
            throw new FabException("ACC108", acctNo);
        }


        //幂等登记薄
        TblLnsinterface	lnsinterface = new TblLnsinterface();
        lnsinterface.setTrandate(ctx.getTermDate());
        lnsinterface.setSerialno(serialNo);
        lnsinterface.setAccdate(ctx.getTranDate());
        lnsinterface.setSerseqno(ctx.getSerSeqNo());
        lnsinterface.setTrancode(ctx.getTranCode());
        lnsinterface.setBrc(ctx.getBrc());
        lnsinterface.setAcctname(lnsbasicinfo.getName());
        lnsinterface.setUserno(lnsbasicinfo.getCustomid());
        lnsinterface.setAcctno(lnsbasicinfo.getAcctno());
        lnsinterface.setTranamt(adjustAmt.getVal());
        lnsinterface.setReserv1(adjustType);
        lnsinterface.setReserv3(feeType);
        lnsinterface.setReserv4(tranBrc);


        try{
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
        } catch(FabSqlException e) {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                throw new FabException(e,TRAN.IDEMPOTENCY);
            }
            else
                throw new FabException(e, "SPS100", "lnsinterface");
        }




        param.put("brc", ctx.getBrc());
        param.put("acctno", acctNo);
        param.put("amortizetype", ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF);
        LnsAmortizeplan lnsamortizeplan;

        try {
            lnsamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsamortizeplan", param,
                    LnsAmortizeplan.class);
        } catch(FabSqlException e) {
            throw new FabException(e, "SPS102", "CUSTOMIZE.query_lnsamortizeplan");
        }

        if (null == lnsamortizeplan) {
            throw new FabException("LNS021");
        }

        if( "CLOSE".equals(lnsamortizeplan.getStatus()) )
            throw new FabException("LNS030");

        if( "1".equals(adjustType) &&
                "51340000".equals(tranBrc) &&
                "1".equals(feeType) )
        {
            //更新表中已摊销金额，已摊销税金，最后摊销日期
            param.put("adjustamt", adjustAmt.getVal());
            param.put("adjustamttax", TaxUtil.calcVAT(adjustAmt).getVal());

            try {
                DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan_463", param);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "lnsamortizeplan");
            }
        }
        else
            throw new FabException("LNS217");	//入参不满足调整条件

    }

    /**
     * @return the serialNo
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * @param serialNo the serialNo to set
     */
    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
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

    /**
     * @return the tranBrc
     */
    public String getTranBrc() {
        return tranBrc;
    }

    /**
     * @param tranBrc the tranBrc to set
     */
    public void setTranBrc(String tranBrc) {
        this.tranBrc = tranBrc;
    }

    /**
     * @return the feeType
     */
    public String getFeeType() {
        return feeType;
    }

    /**
     * @param feeType the feeType to set
     */
    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    /**
     * @return the adjustType
     */
    public String getAdjustType() {
        return adjustType;
    }

    /**
     * @param adjustType the adjustType to set
     */
    public void setAdjustType(String adjustType) {
        this.adjustType = adjustType;
    }

    /**
     * @return the adjustAmt
     */
    public FabAmount getAdjustAmt() {
        return adjustAmt;
    }

    /**
     * @param adjustAmt the adjustAmt to set
     */
    public void setAdjustAmt(FabAmount adjustAmt) {
        this.adjustAmt = adjustAmt;
    }

    /**
     * @return the eventProvider
     */
    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }

    /**
     * @param eventProvider the eventProvider to set
     */
    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;
    }
}

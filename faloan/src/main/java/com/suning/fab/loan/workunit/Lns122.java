package com.suning.fab.loan.workunit;

import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;


/**
 * @param
 * @author 19043955
 * @version V1.0.0
 * @return
 * @exception
 * @see --借新还旧登记幂等登记簿
 */
@Scope("prototype")
@Repository
public class Lns122 extends WorkUnit {
    //新借据号
    String receiptNo;
    //借新还旧原借据号
    String exAcctno;
    //借新还旧原机构号
    String exBrc;
    //旧借据的结清金额
    FabAmount contractAmt;
    //借新还旧类型："1-房抵贷债务重组 2-任性付账单分期 3-任性付最低还款额"
    String switchloanType;


    @Override
    public void run() throws Exception {
        TranCtx ctx = getTranctx();

        if (VarChecker.isEmpty(exAcctno)) {
            throw new FabException("LNS055", "exAcctno");
        }

        if (VarChecker.isEmpty(exBrc)) {
            throw new FabException("LNS055", "exBrc");
        }

        if (VarChecker.isEmpty(contractAmt)) {
            throw new FabException("LNS055", "contractAmt");
        }

        Map<String, Object> stringJson = new HashMap<>();
        stringJson.put("exAcctno", exAcctno);//借新还旧原借据号
        stringJson.put("exBrc", exBrc);//借新还旧原机构号
        stringJson.put("contractAmt", contractAmt.getVal()); //旧借据的结清金额
        stringJson.put("switchloanType", switchloanType);
        AccountingModeChange.saveInterfaceEx(ctx, receiptNo, ConstantDeclare.KEYNAME.JXHJ_JX, "借新还旧-借新", JsonTransfer.ToJson(stringJson));

        Map<String, Object> stringJson2 = new HashMap<>();
        stringJson2.put("acctNo", receiptNo);//借新还旧放款账号
        stringJson2.put("brc", ctx.getBrc());//借新还旧放款机构号
        stringJson2.put("contractAmt", contractAmt.getVal()); //放款金额
        stringJson2.put("switchloanType", switchloanType);
        AccountingModeChange.saveInterfaceEx(ctx, exAcctno, ConstantDeclare.KEYNAME.JXHJ_HJ, "借新还旧-还旧", JsonTransfer.ToJson(stringJson2));


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
     * @return the contractAmt
     */
    public FabAmount getContractAmt() {
        return contractAmt;
    }

    /**
     * @param contractAmt to set
     */
    public void setContractAmt(FabAmount contractAmt) {
        this.contractAmt = contractAmt;
    }

    /**
     * @return the receiptNo
     */
    public String getReceiptNo() {
        return receiptNo;
    }

    /**
     * @param receiptNo to set
     */
    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    /**
     * @return the switchloanType
     */
    public String getSwitchloanType() {
        return switchloanType;
    }

    /**
     * @param switchloanType to set
     */
    public void setSwitchloanType(String switchloanType) {
        this.switchloanType = switchloanType;
    }
}
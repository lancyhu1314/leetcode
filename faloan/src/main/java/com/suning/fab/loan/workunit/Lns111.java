package com.suning.fab.loan.workunit;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsdiscountaccount;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

@Scope("prototype")
@Repository
public class Lns111 extends WorkUnit {


    String		receiptNo;
    String		customType;
    String		customName;
    String		merchantNo;


    @Override
    public void run() throws Exception {
    	
    	if(VarChecker.isEmpty(receiptNo)){
			throw new FabException("LNS055","出帐编号");
		}
    	
    	if(VarChecker.isEmpty(merchantNo)){
			throw new FabException("LNS055","商户号");
		}
		
//		if(VarChecker.isEmpty(customName)){
//			throw new FabException("LNS055","客户名称");
//		}
		
		if(VarChecker.isEmpty(customType)){
			throw new FabException("LNS055","客户类别");
		}
		
		
        TranCtx ctx = getTranctx();

        //贴息预收户登记簿
        TblLnsdiscountaccount lnsdiscountaccount = new TblLnsdiscountaccount();
        lnsdiscountaccount.setBrc(ctx.getBrc());
        lnsdiscountaccount.setCustomid(getMerchantNo());
        lnsdiscountaccount.setAcctno(getReceiptNo());
        lnsdiscountaccount.setName("");
        lnsdiscountaccount.setAccttype("1");//贴息户类型
        if (customType.equals("1")) {
        	lnsdiscountaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
        } else if (customType.equals("2")) {
        	lnsdiscountaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
        }
        lnsdiscountaccount.setOpendate(ctx.getTranDate());
        lnsdiscountaccount.setClosedate("");
        lnsdiscountaccount.setStatus(ConstantDeclare.STATUS.NORMAL);
        lnsdiscountaccount.setReverse1("");
        lnsdiscountaccount.setReverse2("");

        try{
            DbAccessUtil.execute("Lnsdiscountaccount.insert", lnsdiscountaccount);
            lnsdiscountaccount.setAccttype("2");//贴息户类型
            DbAccessUtil.execute("Lnsdiscountaccount.insert", lnsdiscountaccount);
            lnsdiscountaccount.setAccttype("3");//贴息户类型
            DbAccessUtil.execute("Lnsdiscountaccount.insert", lnsdiscountaccount);
        }
        catch (FabSqlException e)
        {
        	if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                LoggerUtil.info("该账号已开户"+getMerchantNo());
            }
            else
                throw new FabException(e, "SPS100", "lnsdiscountaccount");
        }


    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public String getCustomType() {
        return customType;
    }

    public void setCustomType(String customType) {
        this.customType = customType;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getMerchantNo() {
        return merchantNo;
    }

    public void setMerchantNo(String merchantNo) {
        this.merchantNo = merchantNo;
    }
}

package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 预收账号查询
 * @Author 
 * @Date 2020/8/10 10:17
 * @Version 1.0
 */
@Scope("prototype")
@Repository
public class Lns198 extends WorkUnit {

    String	merchantNo;
    String  receiptNo;
    String  repayAcctNo;
    String  feeType;

    @Override
    public void run() throws Exception {
    	String customId = "";
    	TranCtx ctx = getTranctx();
    	if( VarChecker.isEmpty(receiptNo) && VarChecker.isEmpty(merchantNo) )
    		throw new FabException("LNS055","receiptNo/merchantNo");	//不可都为空
    	
    	if(!VarChecker.isEmpty(receiptNo)){
    	    Map<String,Object> param = new HashMap<String,Object>();
    	    param.put("acctno1", receiptNo);
    	    param.put("openbrc", ctx.getBrc());
    	    TblLnsbasicinfo lnsbasicinfo = null;
	        try {
	            lnsbasicinfo = DbAccessUtil.queryForObject("CUSTOMIZE.query_non_acctno", param, TblLnsbasicinfo.class);
	        }catch (FabSqlException e) {
	            throw new FabException(e, "SPS103", "query_non_acctno");
	        }
	        
	        if( null != lnsbasicinfo )
	        	customId = lnsbasicinfo.getCustomid();
	        else
	        	throw new FabException("SPS106",receiptNo);
	        
	        if( !VarChecker.isEmpty(merchantNo) ){
	    	    if( !merchantNo.equals(customId) )
	    	    	throw new FabException("SPS106",merchantNo);
	    	}
    	}
    	else {
    		customId = merchantNo;
    	}
    	
    	
    	
    	
		Map<String, Object> param = new HashMap<>();
		param.put("brc", ctx.getBrc());
		param.put("customid", customId);
		if( VarChecker.isEmpty(feeType) )
			param.put("feetype", ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT);
		else
			param.put("feetype", feeType);
		TblLnsassistdyninfo lnsassistdyninfo;
		try {
			lnsassistdyninfo = DbAccessUtil.queryForObject("Lnsassistdyninfo.selectByUk", param, TblLnsassistdyninfo.class);
		} catch (FabException e) {
		    throw new FabException(e, "SPS103", "lnsassistdyninfo");
		}
		if( null == lnsassistdyninfo )
			throw new FabException("LNS013");	//预收账户不存在
			
		repayAcctNo = lnsassistdyninfo.getAcctno();
    }

	/**
	 * @return the merchantNo
	 */
	public String getMerchantNo() {
		return merchantNo;
	}

	/**
	 * @param merchantNo the merchantNo to set
	 */
	public void setMerchantNo(String merchantNo) {
		this.merchantNo = merchantNo;
	}

	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}

	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	/**
	 * @return the repayAcctNo
	 */
	public String getRepayAcctNo() {
		return repayAcctNo;
	}

	/**
	 * @param repayAcctNo the repayAcctNo to set
	 */
	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
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

}

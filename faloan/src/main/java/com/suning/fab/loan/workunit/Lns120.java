package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 开户--代偿登记幂等登记簿
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns120 extends WorkUnit {
	String receiptNo;
    //代偿原小贷账号
    String exAcctno;
    //代偿原平台账号
    String exAcctno1;
    //代偿期数
    String exPeriod;
    //代偿资金方
    String exinvesteeId;
    //费用交取模式
    String flag;
    @Override
    public void run() throws Exception {
        TranCtx ctx = getTranctx();
        
        if(VarChecker.isEmpty(exAcctno)){
        	throw new FabException("LNS055","exAcctno");
        }
        
        if(VarChecker.isEmpty(exPeriod)){
        	throw new FabException("LNS055","exPeriod");
        }
        
        if(VarChecker.isEmpty(exinvesteeId)){
        	throw new FabException("LNS055","exinvesteeId");
        }

        //代偿开户，费用交取模式为4，转换费用金额必输
        if( "4".equals(flag) && VarChecker.isEmpty(ctx.getRequestDict("switchFee") )){
        	throw new FabException("SPS107","转换费用金额");
		}

		Map<String,String> stringJson = new HashMap<>();
		stringJson.put("exAcctno", exAcctno);//代偿原小贷账号
		stringJson.put("exAcctno1", exAcctno1);//代偿原平台账号
		stringJson.put("exPeriod", exPeriod); //代偿期数
		stringJson.put("exinvesteeId",exinvesteeId);//代偿资金方
        AccountingModeChange.saveInterfaceEx(ctx, receiptNo, ConstantDeclare.KEYNAME.DC, "代偿", JsonTransfer.ToJson(stringJson));


    }



	public String getReceiptNo() {
		return receiptNo;
	}

	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}



	public String getExAcctno() {
		return exAcctno;
	}



	public void setExAcctno(String exAcctno) {
		this.exAcctno = exAcctno;
	}



	public String getExPeriod() {
		return exPeriod;
	}



	public void setExPeriod(String exPeriod) {
		this.exPeriod = exPeriod;
	}



	public String getExinvesteeId() {
		return exinvesteeId;
	}



	public void setExinvesteeId(String exinvesteeId) {
		this.exinvesteeId = exinvesteeId;
	}



	/**
	 * @return the flag
	 */
	public String getFlag() {
		return flag;
	}



	/**
	 * @param flag the flag to set
	 */
	public void setFlag(String flag) {
		this.flag = flag;
	}



	/**
	 * @return the exAcctno1
	 */
	public String getExAcctno1() {
		return exAcctno1;
	}



	/**
	 * @param exAcctno1 the exAcctno1 to set
	 */
	public void setExAcctno1(String exAcctno1) {
		this.exAcctno1 = exAcctno1;
	}



}
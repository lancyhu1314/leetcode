package com.suning.fab.loan.workunit;

import java.util.Arrays;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author Hzg
 *
 * @version V1.0.1
 *
 * @see 本金减免业务校验
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns125 extends WorkUnit {
	String prdcode;
    //结清标志	
    String settleFlag;
    //还款日期
    String realDate;
    //还款金额
    FabAmount repayAmt;
    //利息减免金额
    FabAmount reduceIntAmt;
    //罚息减免金额
    FabAmount reduceFintAmt;
    //本金减免金额
    FabAmount reducePrinAmt;
    //结清本金
    FabAmount cleanPrin;
    //结清利息
    FabAmount cleanInt;
    //结清罚息
    FabAmount cleanForfeit;
    //已到期本金
    FabAmount overPrin;
    //已到期利息
    FabAmount overInt;
    //已到期罚息
    FabAmount overForfeit;

    @Override
    public void run() throws Exception {

    	TranCtx ctx = getTranctx();
        if(!VarChecker.isEmpty(reducePrinAmt) && reducePrinAmt.isPositive()){
        	//实际还款日非当日报错
        	if(!VarChecker.isEmpty(realDate) && !realDate.equals(ctx.getTranDate())){
        		throw new FabException("LNS249");
        	}
        	//非配置产品校验报错
        	if(!Arrays.asList(GlobalScmConfUtil.getProperty("reducePrinContral","").split(",")).contains(prdcode)){
        		 throw new FabException("LNS248");
    		}
        	//结清校验处理
        	if("1".equals(settleFlag)){    		
        		FabAmount settleAllAmt =new FabAmount(0.00);
        		FabAmount repayTotalAmt =new FabAmount(0.00);        	
        		settleAllAmt.selfAdd(cleanPrin).selfAdd(cleanInt).selfAdd(cleanForfeit);

        		repayTotalAmt.selfAdd(repayAmt).selfAdd(reducePrinAmt);
        		if (!settleAllAmt.sub(repayTotalAmt).isZero()){
        			throw new FabException("LNS202");
        		}
        	}else if("2".equals(settleFlag)){    		
        		FabAmount settleAllAmt =new FabAmount(0.00);
        		FabAmount repayTotalAmt =new FabAmount(0.00);        	
        		settleAllAmt.selfAdd(overPrin).selfAdd(overInt).selfAdd(overForfeit);

        		repayTotalAmt.selfAdd(repayAmt).selfAdd(reducePrinAmt);
        		if (!settleAllAmt.sub(repayTotalAmt).isZero()){
        			throw new FabException("LNS202");
        		}
        	}
        	
        	
        }


    }



	public FabAmount getRepayAmt() {
		return repayAmt;
	}



	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}



	public FabAmount getReduceIntAmt() {
		return reduceIntAmt;
	}



	public void setReduceIntAmt(FabAmount reduceIntAmt) {
		this.reduceIntAmt = reduceIntAmt;
	}



	public FabAmount getReduceFintAmt() {
		return reduceFintAmt;
	}



	public void setReduceFintAmt(FabAmount reduceFintAmt) {
		this.reduceFintAmt = reduceFintAmt;
	}



	public String getRealDate() {
		return realDate;
	}



	public void setRealDate(String realDate) {
		this.realDate = realDate;
	}



	public FabAmount getReducePrinAmt() {
		return reducePrinAmt;
	}



	public void setReducePrinAmt(FabAmount reducePrinAmt) {
		this.reducePrinAmt = reducePrinAmt;
	}



	public FabAmount getCleanPrin() {
		return cleanPrin;
	}



	public void setCleanPrin(FabAmount cleanPrin) {
		this.cleanPrin = cleanPrin;
	}



	public FabAmount getCleanInt() {
		return cleanInt;
	}


	public void setCleanInt(FabAmount cleanInt) {
		this.cleanInt = cleanInt;
	}


	public FabAmount getCleanForfeit() {
		return cleanForfeit;
	}


	public void setCleanForfeit(FabAmount cleanForfeit) {
		this.cleanForfeit = cleanForfeit;
	}

	public String getPrdcode() {
		return prdcode;
	}


	public void setPrdcode(String prdcode) {
		this.prdcode = prdcode;
	}


	public String getSettleFlag() {
		return settleFlag;
	}

	public void setSettleFlag(String settleFlag) {
		this.settleFlag = settleFlag;
	}



	public FabAmount getOverPrin() {
		return overPrin;
	}



	public void setOverPrin(FabAmount overPrin) {
		this.overPrin = overPrin;
	}



	public FabAmount getOverInt() {
		return overInt;
	}



	public void setOverInt(FabAmount overInt) {
		this.overInt = overInt;
	}



	public FabAmount getOverForfeit() {
		return overForfeit;
	}



	public void setOverForfeit(FabAmount overForfeit) {
		this.overForfeit = overForfeit;
	}



	


}
package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsdiscountaccount;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	18049705
 *
 * @version V1.1.1
 *
 * @see 	P2P幂等处理
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns114  extends WorkUnit{

	String serialNo;
	FabAmount sumdelint;
	FabAmount sumdelfint;
	FabAmount penaltyAmt;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();		
				
		// 幂等登记薄 

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("sumdelint", sumdelint.getVal());
		param.put("sumdelfint", sumdelfint.getVal());
		/*
		 * 暂时用reserv4 暂代
		 */
		param.put("reserv4", String.valueOf(getPenaltyAmt().getVal()>0?getPenaltyAmt().getVal():"0.00"));
		param.put("serialno", getSerialNo());
		param.put("trancode", ctx.getTranCode());

			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_114", param);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsinterface");
			}
		
	}


	/**
	 * 
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
	 * 
	 * @return the sumdelint
	 */
	public FabAmount getSumdelint() {
		return sumdelint;
	}


	/**
	 * @param sumdelint the sumdelint to set
	 */
	public void setSumdelint(FabAmount sumdelint) {
		this.sumdelint = sumdelint;
	}


	/**
	 * 
	 * @return the sumdelfint
	 */
	public FabAmount getSumdelfint() {
		return sumdelfint;
	}


	/**
	 * @param sumdelfint the sumdelfint to set
	 */
	public void setSumdelfint(FabAmount sumdelfint) {
		this.sumdelfint = sumdelfint;
	}


	/**
	 * 
	 * @return the penaltyAmt
	 */
	public FabAmount getPenaltyAmt() {
		return penaltyAmt;
	}


	/**
	 * @param penaltyAmt the penaltyAmt to set
	 */
	public void setPenaltyAmt(FabAmount penaltyAmt) {
		this.penaltyAmt = penaltyAmt;
	}
	
	

}

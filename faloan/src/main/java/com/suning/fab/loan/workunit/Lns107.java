package com.suning.fab.loan.workunit;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 	HY
 *
 * @version V1.1.1
 *
 * @see 	非标幂等处理
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository

public class Lns107 extends WorkUnit {	
	
	FabAmount	dintAmt;
	FabAmount	nintAmt;
	FabAmount	prinAmt;
	FabAmount   incomeAmt;
	String	endFlag;
	String	serialNo;
	String  acctNo;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
	
		if("471009".equals(ctx.getTranCode())) //非标还款幂等处理
		{	
			Map<String, Object> map471009 = new HashMap<String, Object>();
		
			map471009.put("sumrfint", dintAmt.getVal());
			map471009.put("sumrint", nintAmt.getVal());
			map471009.put("sumramt", prinAmt.getVal());
			map471009.put("acctflag", endFlag);
			map471009.put("serialno", getSerialNo());
			map471009.put("trancode", ctx.getTranCode());
			
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_repay", map471009);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsinterface");
			}
			
			
		}
		if("471010".equals(ctx.getTranCode())) //非标还款幂等处理
		{	
			Map<String, Object> map471009 = new HashMap<String, Object>();
		
			map471009.put("sumrfint", dintAmt.getVal());
			map471009.put("sumrint", nintAmt.getVal());
			map471009.put("sumramt", prinAmt.getVal());
			map471009.put("acctflag", endFlag);
			map471009.put("serialno", getSerialNo());
			map471009.put("trancode", ctx.getTranCode());
			map471009.put("reserv4", incomeAmt.getVal());
			
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_leaseRefund", map471009);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsinterface");
			}
			
			
		}
		if("471012".equals(ctx.getTranCode())) //汽车租赁还款幂等处理
		{	
			Map<String, Object> map471009 = new HashMap<String, Object>();
		
			map471009.put("sumrfint", dintAmt.getVal());
			map471009.put("sumrint", nintAmt.getVal());
			map471009.put("sumramt", prinAmt.getVal());
			map471009.put("acctflag", endFlag);
			map471009.put("serialno", getSerialNo());
			map471009.put("trancode", ctx.getTranCode());
			
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_repay", map471009);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsinterface");
			}
			
			
		}

		if("478001".equals(ctx.getTranCode())||"478003".equals(ctx.getTranCode())) {
			Map<String, Object> map472004 = new HashMap<String, Object>();
			map472004.put("acctflag", endFlag);
			map472004.put("serialno", getSerialNo());
			map472004.put("trancode", ctx.getTranCode());

			try {
				DbAccessUtil.execute("Lnsinterface.update_acctflag", map472004);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsinterface");
			}
		}
		/*if("472004".equals(ctx.getTranCode())) //非标冲销幂等处理
		{	
			Map<String, Object> map472004 = new HashMap<String, Object>();
		
			map472004.put("sumrfint", dintAmt.getVal());
			map472004.put("sumrint", nintAmt.getVal());
			map472004.put("sumramt", prinAmt.getVal());
			map472004.put("acctflag", endFlag);
			map472004.put("serialno", getSerialNo());
			map472004.put("trancode", ctx.getTranCode());
			
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_repay", map472004);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsinterface");
			}
		}*/
	}


	/**
	 * 获取 dintAmt
	 *
	 * @return dintAmt the dintAmt get
	 */
	public FabAmount getDintAmt() {
		return this.dintAmt;
	}

	/**
	 * 设置 dintAmt
	 *
	 * @param dintAmt the dintAmt set
	 */
	public void setDintAmt(FabAmount dintAmt) {
		this.dintAmt = dintAmt;
	}

	/**
	 * 获取 nintAmt
	 *
	 * @return nintAmt the nintAmt get
	 */
	public FabAmount getNintAmt() {
		return this.nintAmt;
	}

	/**
	 * 设置 nintAmt
	 *
	 * @param nintAmt the nintAmt set
	 */
	public void setNintAmt(FabAmount nintAmt) {
		this.nintAmt = nintAmt;
	}

	/**
	 * 获取 prinAmt
	 *
	 * @return prinAmt the prinAmt get
	 */
	public FabAmount getPrinAmt() {
		return this.prinAmt;
	}

	/**
	 * 设置 prinAmt
	 *
	 * @param prinAmt the prinAmt set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}

	/**
	 * 获取 incomeAmt
	 *
	 * @return incomeAmt the incomeAmt get
	 */
	public FabAmount getIncomeAmt() {
		return this.incomeAmt;
	}

	/**
	 * 设置 incomeAmt
	 *
	 * @param incomeAmt the incomeAmt set
	 */
	public void setIncomeAmt(FabAmount incomeAmt) {
		this.incomeAmt = incomeAmt;
	}

	/**
	 * 获取 endFlag
	 *
	 * @return endFlag the endFlag get
	 */
	public String getEndFlag() {
		return this.endFlag;
	}

	/**
	 * 设置 endFlag
	 *
	 * @param endFlag the endFlag set
	 */
	public void setEndFlag(String endFlag) {
		this.endFlag = endFlag;
	}

	/**
	 * 获取 serialNo
	 *
	 * @return serialNo the serialNo get
	 */
	public String getSerialNo() {
		return this.serialNo;
	}

	/**
	 * 设置 serialNo
	 *
	 * @param serialNo the serialNo set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	/**
	 * 获取 acctNo
	 *
	 * @return acctNo the acctNo get
	 */
	public String getAcctNo() {
		return this.acctNo;
	}

	/**
	 * 设置 acctNo
	 *
	 * @param acctNo the acctNo set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

}

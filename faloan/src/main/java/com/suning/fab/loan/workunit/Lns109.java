package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	HY
 *
 * @version V1.1.1
 *
 * @see 	-非标冲销合法性校验
 *

 */
@Scope("prototype")
@Repository
public class Lns109 extends WorkUnit {

	//账号
	String acctNo;
	
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		// 查看还款明细表，查看待冲销用户有没有还款记录，有则直接终止放款冲销交易，
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", getAcctNo());
		param.put("brc", ctx.getBrc());
		Map<String, Object> retMap;
		try {
			retMap = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsrpyinfo_count", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsrpyinfo");
		}

		if (Integer.parseInt(retMap.get("NUM").toString()) > 0) {// 如果还款明细表中有数据，则不能冲销，直接返回，结束
			throw new FabException("LNS001");
		}

		// 查看用户有没有出现过逾期的记录，有则直接终止放款冲销交易，查看账单表
		List<Map<String, Object>> retList;
		try {
			retList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_info", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}

		if (null != retList && !retList.isEmpty()) {
			for (Map<String, Object> map : retList) {
				if (VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(map.get("billstatus")))
					throw new FabException("LNS002");
				if (!new FabAmount(Double.valueOf(map.get("billamt").toString()))
						.sub(Double.valueOf(map.get("billbal").toString())).isZero())
					throw new FabException("LNS174", map.get("billamt"), map.get("billbal"));
				if (CalendarUtil.after(ctx.getTranDate(), map.get("enddate").toString()))
					throw new FabException("LNS175");
			}
		}
		
		List<Map<String, Object>> graceList;
		try {
			graceList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_grace", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}

		if (null != graceList && !graceList.isEmpty()) {
			for (Map<String, Object> map : graceList) {
				if (ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(map.get("BILLTYPE"))) {// 如果出现逾期情况，则直接退出，结束程序
					throw new FabException("LNS002");
				}
			}
		}
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

}

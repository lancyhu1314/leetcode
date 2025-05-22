package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;


/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 转列
 *
 * @param repayDate 转列日期
 * @param acctNo 贷款账号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns503 extends WorkUnit {

	String	acctNo;
	String	repayDate;

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		//获取贷款协议信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());
		param.put("feetypes", LoanFeeUtils.getFeetypes());

		//查询未结清状态的本金利息账单
		List<TblLnsbill> tblLnsBills;
		try{
			tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_runingprinintbills", param, TblLnsbill.class);
		}catch (FabException e){
			throw new FabException(e, "SPS103", "lnsbill");
		}
		for(TblLnsbill tbBill:tblLnsBills){
			LnsBill lnsBill = BillTransformHelper.convertToLnsBill(tbBill);
			//转列
			LoanTransferProvider.loanAcctTransfer(la, lnsBill, repayDate, ctx);
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

	/**
	 * @return the repayDate
	 */
	public String getRepayDate() {
		return repayDate;
	}

	/**
	 * @param repayDate the repayDate to set
	 */
	public void setRepayDate(String repayDate) {
		this.repayDate = repayDate;
	}

}

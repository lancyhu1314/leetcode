package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbillplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.LoanRePayPlanSupporter;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	AY
 *
 * @version V1.0.1
 *
 * @see 	还款计划查询
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns402 extends WorkUnit {

	String acctNo;
	Integer pageSize;
	Integer currentPage;
	Integer repayTerm;
	
	Integer totalLine;
	String pkgList;

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		
		//读取账户基本信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		
		TblLnsbasicinfo lnsbasicinfo = null;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		
		if (null == lnsbasicinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		
		//辅助账单
		Map<String,Object> billparam = new HashMap<String,Object>();
		billparam.put("acctno", acctNo);
		billparam.put("brc", ctx.getBrc());
		List<TblLnsbillplan> tblBillPlanList = null;

		try {
			tblBillPlanList = DbAccessUtil.queryForList("CUSTOMIZE.query_billplan", billparam, TblLnsbillplan.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbillplan");
		}

		if (null == tblBillPlanList){
			tblBillPlanList = new ArrayList<TblLnsbillplan>();
		}

		//根据账号生成账单
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
		
		LoanRePayPlanSupporter loanRePayPlanSupporter = LoanSupporterUtil.getRepayPlanRepayWaySupporter(la.getWithdrawAgreement().getRepayWay());
		
		List<RePayPlan> repayPlan = loanRePayPlanSupporter.genRepayPlan(la, lnsbasicinfo, lnsBillStatistics, tblBillPlanList, ctx);
		
		if(repayPlan.isEmpty()){
			throw new FabException("LNS021");
		}
		
		totalLine = repayPlan.size();
		pkgList = JsonTransfer.ToJson(repayRetTerm(repayPlan));
	}
	
	public List<RePayPlan> repayRetTerm(List<RePayPlan> planList){
		
		if(!VarChecker.isEmpty(repayTerm)){
			if(0 != repayTerm){
				List<RePayPlan> planRetTerm = new ArrayList<RePayPlan>();
				for(RePayPlan plan:planList){
					if(repayTerm.equals(plan.getRepayterm())){
						planRetTerm.add(plan);
					}
				}
				totalLine = planRetTerm.size();
				return repayRet(planRetTerm);
			}
		}
		return repayRet(planList);
	}
	
	public List<RePayPlan> repayRet(List<RePayPlan> planList){
		
		if(!VarChecker.isEmpty(pageSize) && !VarChecker.isEmpty(currentPage)){
			if (pageSize > 0 && currentPage > 0){
				List<RePayPlan> planret = new ArrayList<RePayPlan>();
				for (int i = (currentPage - 1) * pageSize; i < (currentPage * pageSize); i++){
					if (i < planList.size()){
						planret.add(planList.get(i));
					}
				}
				return planret;
			} 
		}
		return planList;
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
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}

	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * @return the currentPage
	 */
	public Integer getCurrentPage() {
		return currentPage;
	}

	/**
	 * @param currentPage the currentPage to set
	 */
	public void setCurrentPage(Integer currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * @return the repayTerm
	 */
	public Integer getRepayTerm() {
		return repayTerm;
	}

	/**
	 * @param repayTerm the repayTerm to set
	 */
	public void setRepayTerm(Integer repayTerm) {
		this.repayTerm = repayTerm;
	}

	/**
	 * @return the totalLine
	 */
	public Integer getTotalLine() {
		return totalLine;
	}

	/**
	 * @param totalLine the totalLine to set
	 */
	public void setTotalLine(Integer totalLine) {
		this.totalLine = totalLine;
	}

	/**
	 * @return the pkgList
	 */
	public String getPkgList() {
		return pkgList;
	}

	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
	}
	

}

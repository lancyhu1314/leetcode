package com.suning.fab.loan.workunit;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.RpyPlanResult;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.MapperUtil;

/**
 * @author 18049705
 *
 * @see 还款计划 添加减免利息 减免罚息 和违约金字段
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns420 extends WorkUnit {
	// 账号
	String acctNo;
	//期数
	Integer repayTerm;
	List<RePayPlan>  rpyPlanList;
	//json串
	String pkgList;
	
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		//读取账户基本信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(),ctx);
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史账单
		billList.addAll(lnsBillStatistics.getHisBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
		billList.addAll(lnsBillStatistics.getBillInfoList());
		//未来期账单：从还款日到合同到期日之间的本金和利息账单
		billList.addAll(lnsBillStatistics.getFutureBillInfoList());
		Map<String,LnsBill> rfMap = new HashMap<String,LnsBill>();
		Map<String,LnsBill> pnlMap = new HashMap<String,LnsBill>();
		for(LnsBill lnsBill:billList){
			//选择费用账单类型
			if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE)){
				
				
				rfMap.put(String.valueOf(lnsBill.getPeriod()), lnsBill);
			}			
			else if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PNLA))
			{
				pnlMap.put(String.valueOf(lnsBill.getPeriod()), lnsBill);
			}
		}
		
		//定义查询map
		Map<String,Object> param = new HashMap<String,Object>();
		//账号
		param.put("acctno", acctNo);
		if(null != repayTerm && 0!=repayTerm ){
			param.put("repayterm", repayTerm);
		}
		List<TblLnsrpyplan> addInfoList;
		try {
			
			addInfoList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsrpyplan_420", param, TblLnsrpyplan.class);
		}
		catch (FabSqlException e)
		{
			//异常
			throw new FabException(e, "SPS103", "lnsrpyplan");
		}
				
		Map<String,TblLnsrpyplan> rpyMap = new HashMap<String,TblLnsrpyplan>();
		for(TblLnsrpyplan addInfo:addInfoList)
		{
			rpyMap.put(String.valueOf(addInfo.getRepayterm()), addInfo)	;		
		}			
		//添加违约金 减免罚息  减免利息
		List<RpyPlanResult> result = new ArrayList<RpyPlanResult>();
		for(RePayPlan repayPlan:rpyPlanList){
			RpyPlanResult rpyPlan = new  RpyPlanResult();
			MapperUtil.map(repayPlan, rpyPlan);
			TblLnsrpyplan addInfo = rpyMap.get(String.valueOf(repayPlan.getRepayterm()));
			if(null != addInfo)
			{				
				//违约金penaltyAmt 
				rpyPlan.setPenaltyAmt(new FabAmount(addInfo.getReserve2()));
				 //减免罚息 sumdelint 在表中展示为已还罚息
				rpyPlan.setSumdelint(new FabAmount(addInfo.getSumrfint()));
				 //减免利息 sumdelfint 在表中展示为已还利息
				rpyPlan.setSumdelfint(new FabAmount(addInfo.getIntamt()));
				
				//去除 已还中减免的部分
				rpyPlan.getSumrfint().selfSub(addInfo.getSumrfint());
				rpyPlan.getIntAmt().selfSub(addInfo.getIntamt());

			}
			LnsBill pnlBill = pnlMap.get(String.valueOf(repayPlan.getRepayterm()));
			if(null != pnlBill)
			{
				//违约金penaltyAmt 
				rpyPlan.setPenaltyAmt(pnlBill.getBillAmt());
			}
			
			LnsBill lnsBill = rfMap.get(String.valueOf(repayPlan.getRepayterm()));
			if(null != lnsBill){
				//本期应还服务费
				rpyPlan.setTermretFee(lnsBill.getBillAmt());
				//已还服务费
				rpyPlan.setFeeAmt(new FabAmount(lnsBill.getBillAmt().sub(lnsBill.getBillBal()).getVal()));
				//未还服务费
				rpyPlan.setNoretFee(lnsBill.getBillBal());

			}
			result.add(rpyPlan);
		}
		
		
		setPkgList(JsonTransfer.ToJson(result));
	}	
	/*	
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo
	 *            the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}
	
	/**
	 * 
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
	 * 
	 * @return the rpyPlanList
	 */
	public List<RePayPlan> getRpyPlanList() {
		return rpyPlanList;
	}
	/**
	 * @param rpyPlanList the rpyPlanList to set
	 */
	public void setRpyPlanList(List<RePayPlan> rpyPlanList) {
		this.rpyPlanList = rpyPlanList;
	}
	/**
	 * 
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

package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanBillSettleInterestSupporter;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	AY
 *
 * @version V1.0.1
 *
 * @see 	贷款账户统计查询
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns404 extends WorkUnit {

	String acctNo;

	FabAmount cleanPrin 	= new FabAmount();		//未还本金
	FabAmount prinAmt 		= new FabAmount();		//已还本金
	FabAmount cleanInt 		= new FabAmount();		//未还利息
	FabAmount intAmt 		= new FabAmount();		//已还利息
	FabAmount cleanForfeit 	= new FabAmount();		//未还罚息
	FabAmount forfeitAmt 	= new FabAmount();		//已还罚息
	FabAmount feeAmt 		= new FabAmount();		//已交手续费
	FabAmount contractAmt 	= new FabAmount();		//贷款金额
	String status;				//贷款状态

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		//获取账户信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		//根据账号生成账单
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);

		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());

		TblLnsbasicinfo lnsbasicinfo;
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
		status = lnsbasicinfo.getLoanstat();
		feeAmt = new FabAmount();
		contractAmt = new FabAmount(lnsbasicinfo.getContractamt());

		String endDate = ctx.getTranDate();
		String repayDate = endDate;
		//预约日期大于合同结束日期取合同结束日
		if(CalendarUtil.after(endDate, lnsbasicinfo.getContduedate())){
			endDate = lnsbasicinfo.getContduedate();
		}

		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前期和未来期
		List<LnsBill> lnsbill = new ArrayList<LnsBill>();
		lnsbill.addAll(lnsBillStatistics.getBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());

		//用于记录提前还款还了部分账单的期数标识
		int period = 0;
		//遍历历史账单
		for(LnsBill bill:hisLnsbill){
			//过滤AMLT账单
			if("AMLT".equals(bill.getBillType())){
				continue;
			}

			if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
				cleanPrin.selfAdd(bill.getBillBal());
				prinAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
				if (famt != null)
				{
					cleanForfeit.selfAdd(famt);
				}
			}
			else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
				cleanInt.selfAdd(bill.getBillBal());
				intAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
				FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
				if (fint != null)
				{
					cleanForfeit.selfAdd(fint);
				}
				if(period < bill.getPeriod()){
					period = bill.getPeriod();
				}

			}
			else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
					ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
					ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
				cleanForfeit.selfAdd(bill.getBillBal());
				forfeitAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
			} 
		}
		//遍历当前期账单和未来期账单
		for(LnsBill bill:lnsbill){
			//过滤AMLT账单
			if("AMLT".equals(bill.getBillType())){
				continue;
			}
			//计算呆滞呆账罚息复利
			if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
				if (famt != null)
				{
					cleanForfeit.selfAdd(famt);
				}
			}
			else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
				FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
				if (fint != null)
				{
					cleanForfeit.selfAdd(fint);
				}
			}

			//当前期
			if(CalendarUtil.after(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())
					&& VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, 
							ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())
					&& (period == 0 || period == bill.getPeriod())){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
				} 
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					//等本等息
					if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
						cleanInt.selfAdd(bill.getBillBal());
						intAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
					}
					//非等本等息
					else{
						cleanInt.selfAdd(bill.getRepayDateInt());
						intAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
					}
				}
				continue;
			}
			//试算出的历史期
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(), endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					cleanInt.selfAdd(bill.getBillBal());
					intAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
				} 
			}
			//未来期
			else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
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

	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}

	/**
	 * @param prinAmt the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}

	/**
	 * @return the cleanPrin
	 */
	public FabAmount getCleanPrin() {
		return cleanPrin;
	}

	/**
	 * @param cleanPrin the cleanPrin to set
	 */
	public void setCleanPrin(FabAmount cleanPrin) {
		this.cleanPrin = cleanPrin;
	}

	/**
	 * @return the intAmt
	 */
	public FabAmount getIntAmt() {
		return intAmt;
	}

	/**
	 * @param intAmt the intAmt to set
	 */
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}

	/**
	 * @return the cleanInt
	 */
	public FabAmount getCleanInt() {
		return cleanInt;
	}

	/**
	 * @param cleanInt the cleanInt to set
	 */
	public void setCleanInt(FabAmount cleanInt) {
		this.cleanInt = cleanInt;
	}

	/**
	 * @return the forfeitAmt
	 */
	public FabAmount getForfeitAmt() {
		return forfeitAmt;
	}

	/**
	 * @param forfeitAmt the forfeitAmt to set
	 */
	public void setForfeitAmt(FabAmount forfeitAmt) {
		this.forfeitAmt = forfeitAmt;
	}

	/**
	 * @return the cleanForfeit
	 */
	public FabAmount getCleanForfeit() {
		return cleanForfeit;
	}

	/**
	 * @param cleanForfeit the cleanForfeit to set
	 */
	public void setCleanForfeit(FabAmount cleanForfeit) {
		this.cleanForfeit = cleanForfeit;
	}

	/**
	 * @return the feeAmt
	 */
	public FabAmount getFeeAmt() {
		return feeAmt;
	}

	/**
	 * @param feeAmt the feeAmt to set
	 */
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;
	}

	/**
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}

	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}


}

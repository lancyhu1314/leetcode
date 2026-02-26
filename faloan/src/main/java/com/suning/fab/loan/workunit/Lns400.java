package com.suning.fab.loan.workunit;

import java.util.*;

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
 *
 *	 		借据号
 * 			endDate			预约还款日期
 *
 * @return	cleanPrin		结清本金
 *			prinAmt			正常还款本金
 *			cleanInt		结清利息
 *			intAmt			正常还款利息
 *			cleanForfeit	结清罚息
 *			forfeitAmt		正常还款罚息
 *			overPrin		逾期本金
 *			overInt			逾期利息
 *			overDint		逾期罚息
 *			overFee			逾期管理费
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns400 extends WorkUnit {

	String acctNo;
	String endDate;
	String listBrc;

	FabAmount cleanPrin = new FabAmount();
	FabAmount prinAmt = new FabAmount();
	FabAmount cleanInt = new FabAmount();
	FabAmount intAmt = new FabAmount();
	FabAmount cleanForfeit = new FabAmount();
	FabAmount forfeitAmt = new FabAmount();
	FabAmount overPrin = new FabAmount();
	FabAmount overInt = new FabAmount();
	FabAmount overDint = new FabAmount();

	LnsBillStatistics lnsBillStatistics;
	LoanAgreement la;
	TranCtx ctx;
	@Override
	public void run() throws Exception {
		ctx = getTranctx();

		if(VarChecker.isEmpty(endDate)){
			throw new FabException("LNS005");
		}
		//预约日期大于交易日
		if(CalendarUtil.before(endDate, ctx.getTranDate())){
			throw new FabException("LNS034");
		}

		//预约还款批量查询   2019-04-09
		if( null != listBrc && !VarChecker.isEmpty(listBrc) )
			ctx.setBrc(listBrc);
		
		//读取借据主文件
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
		//结息日为空就取起息日
		if(VarChecker.isEmpty(lnsbasicinfo.getLastintdate())){
			lnsbasicinfo.setLastintdate(lnsbasicinfo.getBeginintdate());
		}
		if("2412615".equals(lnsbasicinfo.getPrdcode().trim()))
			throw new FabException("LNS183","2412615");
		//根据账号生成协议
		la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,la);
		if (la.getContract().getContractStartDate().equals(endDate) && 
				"3".equals(la.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
		}
		if(CalendarUtil.equalDate(lnsbasicinfo.getOpendate(), lnsbasicinfo.getBeginintdate())){
			//开户日当天还款还到最后一期本金，但是没还清
			if(!CalendarUtil.equalDate(lnsbasicinfo.getLastintdate(), lnsbasicinfo.getContduedate())
					&& CalendarUtil.equalDate(endDate, lnsbasicinfo.getOpendate())
					&& !VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_ZDY).contains(lnsbasicinfo.getRepayway())){
				if(new FabAmount(lnsbasicinfo.getContractamt())
						.sub(new FabAmount(lnsbasicinfo.getContractbal())).isZero()){
					cleanPrin = new FabAmount(lnsbasicinfo.getContractbal());
					return;  //如果是倒起息开户 当天预约那就有利息不能直接返回 此段放子交易最后 重置本金
				}
			}
		}

		
		//生成还款计划
		lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, endDate, ctx);
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		List<LnsBill> lnsbill = new ArrayList<LnsBill>();
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前期和未来期
		lnsbill.addAll(lnsBillStatistics.getBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());

		String repayDate = endDate;
		//预约日期大于合同结束日期取合同结束日
		if(CalendarUtil.after(endDate, lnsbasicinfo.getContduedate())){
			endDate = lnsbasicinfo.getContduedate();
		}
		//用于记录提前还款还了部分账单的期数标识
		int period = 0;
		//遍历历史账单
		for(LnsBill bill:hisLnsbill){
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(bill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE).contains(bill.getSettleFlag())){
				continue;
			}

			if(CalendarUtil.after(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
					FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
					if (famt != null)
					{
						cleanForfeit.selfAdd(famt);
						forfeitAmt.selfAdd(famt);
						overDint.selfAdd(famt);
					}
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){

					if( VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_XXHB, 
										  ConstantDeclare.REPAYWAY.REPAYWAY_XBHX, 
										  ConstantDeclare.REPAYWAY.REPAYWAY_YBYX).contains(lnsbasicinfo.getRepayway()) && 
						!VarChecker.isEmpty(bill.getHisBill()) )
					{
						cleanInt.selfAdd(bill.getRepayDateInt());
						intAmt.selfAdd(bill.getRepayDateInt());
					}	
					else if( VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_ZDY).contains(lnsbasicinfo.getRepayway()) && 
							 !VarChecker.isEmpty(bill.getHisBill()))
					{
						cleanInt.selfAdd(bill.getBillBal());
						intAmt.selfAdd(bill.getBillBal());
					}
					else
					{	
						cleanInt.selfAdd(bill.getBillBal());
						intAmt.selfAdd(bill.getBillBal());
						FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
						if (fint != null)
						{
							cleanForfeit.selfAdd(fint);
							forfeitAmt.selfAdd(fint);
							overDint.selfAdd(fint);
						}
					}
					if(period < bill.getPeriod()){
						period = bill.getPeriod();
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
				continue;
			}
			//历史期
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(), endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overPrin.selfAdd(bill.getBillBal());
					}



					//计算呆滞呆账罚息复利
					FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
					if (famt != null)
					{
						cleanForfeit.selfAdd(famt);
						forfeitAmt.selfAdd(famt);
						overDint.selfAdd(famt);
					}
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					cleanInt.selfAdd(bill.getBillBal());
					intAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overInt.selfAdd(bill.getBillBal());
					}

					//计算呆滞呆账罚息复利
					FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
					if (fint != null)
					{
						cleanForfeit.selfAdd(fint);
						forfeitAmt.selfAdd(fint);
						overDint.selfAdd(fint);
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
			}
			//未来期
			else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					if( VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_XXHB, 
										  ConstantDeclare.REPAYWAY.REPAYWAY_XBHX, 
										  ConstantDeclare.REPAYWAY.REPAYWAY_YBYX).contains(lnsbasicinfo.getRepayway()) &&
						!VarChecker.isEmpty(bill.getHisBill()) )
					{
						cleanInt.selfAdd(bill.getRepayDateInt());
					}		
					else
					{	
						cleanInt.selfAdd(bill.getBillBal());
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
			}
		}
		String termStartDate = "";
		int termperiod = 0;
		//遍历当前期账单和未来期账单
		for(LnsBill bill:lnsbill){
			
			if ("".equals(termStartDate) || CalendarUtil.after(bill.getStartDate(), termStartDate)){
				termStartDate = bill.getStartDate();
			}
			
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(bill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE ).contains(bill.getSettleFlag())){
				continue;
			}
			//计算呆滞呆账罚息复利
			if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
				if (famt != null)
				{
					cleanForfeit.selfAdd(famt);
					forfeitAmt.selfAdd(famt);
					overDint.selfAdd(famt);
				}
			}
			else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
				FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
				if (fint != null)
				{
					cleanForfeit.selfAdd(fint);
					forfeitAmt.selfAdd(fint);
					overDint.selfAdd(fint);
				}
			}

			//当前期
			if((CalendarUtil.after(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())
					&& VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, 
							ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())
					&& (period == 0 || period >= bill.getPeriod())) 
					|| ("true".equals(la.getInterestAgreement().getIsCalTail())
							&& CalendarUtil.afterAlsoEqual(endDate, bill.getStartDate())
							&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())
							&& VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, 
									ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())
							&& (period == 0 || period >= bill.getPeriod()))){//还款方式为12（先本后息）时，会先还利息，第一期的本金会在未来期出现
				
				if(termperiod == 0){
					termperiod = bill.getPeriod();
				}
				
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					if(termperiod == bill.getPeriod()){
						prinAmt.selfAdd(bill.getBillBal());
					}
				} 
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					//等本等息
					if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway()) ||
					   ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway())){
						intAmt.selfAdd(bill.getBillBal());
						cleanInt.selfAdd(bill.getBillBal());
					}
					//非等本等息
					else {
						intAmt.selfAdd(bill.getRepayDateInt());
						cleanInt.selfAdd(bill.getRepayDateInt());
					}
				}
				continue;
			}
			//试算出的历史期
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(), endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overPrin.selfAdd(bill.getBillBal());
					}


				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					cleanInt.selfAdd(bill.getBillBal());
					intAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overInt.selfAdd(bill.getBillBal());
					}

				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
			}
			//未来期
			else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					//还款方式为8（随借随还）时，还多少本金插多少本金，当天之后的本金全部属于未来期
					if("8".equals(lnsbasicinfo.getRepayway())){
						prinAmt.selfAdd(bill.getBillBal());
					}
				}
				else if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType()))
				{
					//2019-05-15 非标自定义不规则
					if(ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway())){
						cleanInt.selfAdd(bill.getBillBal());
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				} 
			}
		}
		//开户日当天还款还到最后一期本金，但是没还清
		/*if(!CalendarUtil.equalDate(lnsbasicinfo.getLastintdate(), lnsbasicinfo.getContduedate())
				&& CalendarUtil.equalDate(endDate, lnsbasicinfo.getOpendate())){
			cleanPrin = new FabAmount(lnsbasicinfo.getContractbal());
			//return;  如果是倒起息开户 当天预约那就有利息不能直接返回 此段放子交易最后 重置本金
		}*/
		if("12".equals(lnsbasicinfo.getRepayway()) && 
				endDate.compareTo(termStartDate) <= 0)
		{
			intAmt.setVal(0.00);
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
	 * @return the endDate
	 */
	public String getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
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

	public FabAmount getOverDint() {
		return overDint;
	}

	public void setOverDint(FabAmount overDint) {
		this.overDint = overDint;
	}

	/**
	 * 
	 * @return the lnsBillStatistics
	 */
	public LnsBillStatistics getLnsBillStatistics() {
		return lnsBillStatistics;
	}

	/**
	 * @param lnsBillStatistics the lnsBillStatistics to set
	 */
	public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
		this.lnsBillStatistics = lnsBillStatistics;
	}

	/**
	 * @return the ctx
	 */
	public TranCtx getCtx() {
		return ctx;
	}

	/**
	 * @param ctx the ctx to set
	 */
	public void setCtx(TranCtx ctx) {
		this.ctx = ctx;
	}

	/**
	 * @return the listBrc
	 */
	public String getListBrc() {
		return listBrc;
	}

	/**
	 * @param listBrc the listBrc to set
	 */
	public void setListBrc(String listBrc) {
		this.listBrc = listBrc;
	}

	/**
	 * @return the la
	 */
	public LoanAgreement getLa() {
		return la;
	}

	/**
	 * @param la the la to set
	 */
	public void setLa(LoanAgreement la) {
		this.la = la;
	}


}

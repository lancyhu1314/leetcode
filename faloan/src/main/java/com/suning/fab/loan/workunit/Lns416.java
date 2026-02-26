package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanBillSettleInterestSupporter;
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
 * @see 	非标准预约还款查询
 *
 * @param	acctNo			借据号
 * 			endDate			预约还款日期
 *
 * @return	cleanPrin		结清本金
 *			prinAmt			正常还款本金
 *			cleanInt		结清利息
 *			intAmt			正常还款利息
 *			cleanForfeit	结清罚息
 *			forfeitAmt		正常还款罚息
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns416 extends WorkUnit {

	String acctNo;
	String endDate;

	FabAmount cleanPrin = new FabAmount();
	FabAmount prinAmt = new FabAmount();
	FabAmount cleanInt = new FabAmount();
	FabAmount intAmt = new FabAmount();
	FabAmount cleanForfeit = new FabAmount();
	FabAmount forfeitAmt = new FabAmount();

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();

		if(VarChecker.isEmpty(endDate)){
			throw new FabException("LNS005");
		}
		//预约日期大于交易日
		if(CalendarUtil.before(endDate, ctx.getTranDate())){
			throw new FabException("LNS034");
		}

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
		//开户日当天还款还到最后一期本金，但是没还清
		if(!CalendarUtil.equalDate(lnsbasicinfo.getLastintdate(), lnsbasicinfo.getContduedate())
				&& CalendarUtil.equalDate(endDate, lnsbasicinfo.getOpendate())){
			cleanPrin = new FabAmount(lnsbasicinfo.getContractbal());
			return;
		}

		//根据账号生成协议
		LoanAgreement la = new LoanAgreement();
		la.getContract().setReceiptNo(getAcctNo());
		
		
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		//当前期和未来期
		List<LnsBill> lnsbill = new ArrayList<LnsBill>();
		
		String repayDate = endDate;
		//预约日期大于合同结束日期取合同结束日
		if(CalendarUtil.after(endDate, lnsbasicinfo.getContduedate())){
			endDate = lnsbasicinfo.getContduedate();
		}
		//遍历历史账单
		for(LnsBill bill:hisLnsbill){
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(bill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE ).contains(bill.getSettleFlag())){
				continue;
			}

			if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
				cleanPrin.selfAdd(bill.getBillBal());
				prinAmt.selfAdd(bill.getBillBal());
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
				if (famt != null)
				{
					cleanForfeit.selfAdd(famt);
					forfeitAmt.selfAdd(famt);
				}
			}
			else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
				cleanInt.selfAdd(bill.getBillBal());
				intAmt.selfAdd(bill.getBillBal());
				FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
				if (fint != null)
				{
					cleanForfeit.selfAdd(fint);
					forfeitAmt.selfAdd(fint);
				}
			}
			else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
					ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
					ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
				cleanForfeit.selfAdd(bill.getBillBal());
				forfeitAmt.selfAdd(bill.getBillBal());
			} 
		}
		//当期结束日（用于控制先息后本的本金账单）
		String currentDate = null;
		//遍历当前期账单和未来期账单
		for(LnsBill bill:lnsbill){
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
				}
			}
			else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
				FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
				if (fint != null)
				{
					cleanForfeit.selfAdd(fint);
					forfeitAmt.selfAdd(fint);
				}
			}
			
			//当前期
			if(CalendarUtil.after(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())
					&& VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, 
							ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
				//试算出的同期账单结束日以利息为主
				if(null == currentDate && ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					currentDate = bill.getEndDate();
				}
				if(CalendarUtil.equalDate(currentDate, bill.getEndDate())){
					if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
						cleanPrin.selfAdd(bill.getBillBal());
						prinAmt.selfAdd(bill.getBillBal());
					} 
					else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
						//等本等息
						if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
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
			}
			//试算出的历史期
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(), endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					cleanInt.selfAdd(bill.getBillBal());
					intAmt.selfAdd(bill.getBillBal());
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT, 
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
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
					forfeitAmt.selfAdd(bill.getBillBal());
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


}

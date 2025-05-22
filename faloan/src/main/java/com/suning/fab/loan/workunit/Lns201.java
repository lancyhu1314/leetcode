package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsamortizeplan;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 开户--扣息税金
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns201 extends WorkUnit { 

	String		receiptNo;
	FabAmount	contractAmt;
	FabAmount	discountAmt;
	String		discountFlag;
	String		openBrc;
	String		openDate;
	String		startIntDate;
	String		endDate;
	FabAmount	taxAmt; 
	Double		taxRate = new Double(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06"));
	
	
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
	
		/**
		 * 放款方式:discountFlag	1：全额放款 2：扣息放款
		 * 20190528|14050183
		 */
		if("1".equals(discountFlag) && getDiscountAmt() != null && discountAmt.isPositive())
		{	
			LoggerUtil.debug("全额放款扣息金额非法");
			throw new FabException("LNS026");
		}	
		
		if("2".equals(discountFlag))
		{	
			if(VarChecker.isEmpty(discountAmt)||!discountAmt.isPositive()){
				LoggerUtil.debug("扣息放款扣息金额非法");
				throw new FabException("LNS027");
			}
		}	
		
		if (!VarChecker.isEmpty(discountAmt) && discountAmt.isPositive())// 扣息大于零进行如下处理
		{
			taxAmt = TaxUtil.calcVAT(getDiscountAmt());

			// 摊销计划表
			TblLnsamortizeplan lnsamortizeplan = new TblLnsamortizeplan();
			lnsamortizeplan.setTrandate(Date.valueOf(ctx.getTranDate()));
			lnsamortizeplan.setSerseqno(ctx.getSerSeqNo());
			lnsamortizeplan.setBrc(ctx.getBrc());
			lnsamortizeplan.setAcctno(getReceiptNo());
			lnsamortizeplan.setAmortizetype("1");
			lnsamortizeplan.setCcy("01");
			lnsamortizeplan.setTaxrate(taxRate);
			lnsamortizeplan.setTotalamt(getDiscountAmt().getVal());
			lnsamortizeplan.setAmortizeamt(0.00);
			lnsamortizeplan.setTotaltaxamt(taxAmt.getVal());
			lnsamortizeplan.setAmortizetax(0.00);
			//if (getStartIntDate() == null) {
			//MOD by TT.Y考虑日期为空逻辑
			if(VarChecker.isEmpty(startIntDate)){
				lnsamortizeplan.setLastdate(getOpenDate());
				lnsamortizeplan.setBegindate(getOpenDate());
			} else {
				lnsamortizeplan.setLastdate(getStartIntDate());
				lnsamortizeplan.setBegindate(getStartIntDate());
			}
			lnsamortizeplan.setEnddate(getEndDate());
			lnsamortizeplan.setAmortizeformula("");
			lnsamortizeplan.setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);

			try {
				DbAccessUtil.execute("Lnsamortizeplan.insert", lnsamortizeplan);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "lnsamortizeplan");
			}

			if (taxAmt.isPositive())// 税金大于零抛事件
			{
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				eventProvider.createEvent(ConstantDeclare.EVENT.DISCONTTAX, taxAmt, lnsAcctInfo, null, null,
						ConstantDeclare.BRIEFCODE.KXSJ, ctx);
			}
			/*
			 * 保存扣息税金
			 */
			AccountingModeChange.saveDiscountTax(ctx,receiptNo , getDiscountAmt().getVal(), taxAmt.getVal(),
					ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
		}
	}
	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}
	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
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
	 * @return the discountAmt
	 */
	public FabAmount getDiscountAmt() {
		return discountAmt;
	}
	/**
	 * @param discountAmt the discountAmt to set
	 */
	public void setDiscountAmt(FabAmount discountAmt) {
		this.discountAmt = discountAmt;
	}
	/**
	 * @return the discountFlag
	 */
	public String getDiscountFlag() {
		return discountFlag;
	}
	/**
	 * @param discountFlag the discountFlag to set
	 */
	public void setDiscountFlag(String discountFlag) {
		this.discountFlag = discountFlag;
	}
	/**
	 * @return the openBrc
	 */
	public String getOpenBrc() {
		return openBrc;
	}
	/**
	 * @param openBrc the openBrc to set
	 */
	public void setOpenBrc(String openBrc) {
		this.openBrc = openBrc;
	}
	/**
	 * @return the openDate
	 */
	public String getOpenDate() {
		return openDate;
	}
	/**
	 * @param openDate the openDate to set
	 */
	public void setOpenDate(String openDate) {
		this.openDate = openDate;
	}
	/**
	 * @return the startIntDate
	 */
	public String getStartIntDate() {
		return startIntDate;
	}
	/**
	 * @param startIntDate the startIntDate to set
	 */
	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
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
	 * @return the taxAmt
	 */
	public FabAmount getTaxAmt() {
		return taxAmt;
	}
	/**
	 * @param taxAmt the taxAmt to set
	 */
	public void setTaxAmt(FabAmount taxAmt) {
		this.taxAmt = taxAmt;
	}
	/**
	 * @return the taxRate
	 */
	public Double getTaxRate() {
		return taxRate;
	}
	/**
	 * @param taxRate the taxRate to set
	 */
	public void setTaxRate(Double taxRate) {
		this.taxRate = taxRate;
	}
	/**
	 * @return the add
	 */
	public AccountOperator getAdd() {
		return add;
	}
	/**
	 * @param add the add to set
	 */
	public void setAdd(AccountOperator add) {
		this.add = add;
	}
	/**
	 * @return the eventProvider
	 */
	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}
	/**
	 * @param eventProvider the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}


}

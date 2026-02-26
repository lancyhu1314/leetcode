package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsAmortizeplan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsprovisiondtl;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉：租赁摊销
 *
 * @author 18043620
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns510 extends WorkUnit {

	String acctNo; // 本金账号
	String repayDate; // 摊销日期
	String repayAcctNo; // 支付账号
	String customid; // 个人号
	String customType; // 客户类型
	FabAmount amortizAmt; // 本次摊销金额
	FabAmount taxAmt; // 本次摊销税金
	FabAmount dAmortizAmt; // 已摊销金额--用于更新摊销计划表以及利息计提登记薄
	FabAmount dTaxAmt; // 已摊销税金--用于更新摊销计划表以及利息计提登记薄
	String status; // 摊销状态 "RUNNING"，"CLOSE", "CANCEL"
	String settleFlag;// 结清标识 1标示结清，0标识走正常流程
	FabAmount chargeAmt; // 结清金额

	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();

		if (VarChecker.isEmpty(chargeAmt)) {
			chargeAmt = new FabAmount(0.00);
		}

		if("51240001".equals(ctx.getBrc())){
			if(VarChecker.isEmpty(customType)||VarChecker.isEmpty(customid)){
				Map<String,Object> basicParam = new HashMap<String,Object>();
				basicParam.put("acctno", acctNo);
				basicParam.put("openbrc", ctx.getBrc());
	
				TblLnsbasicinfo lnsbasicinfo = null;
				try {
					lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", basicParam, TblLnsbasicinfo.class);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS103", "lnsbasicinfo");
				}
	
				if (null == lnsbasicinfo){
					throw new FabException("SPS104", "lnsbasicinfo");
				}
				setCustomType(lnsbasicinfo.getCusttype());
				setCustomid(lnsbasicinfo.getCustomid());
			}
		}
		/*
		 * if (VarChecker.isEmpty(repayDate)) { throw new
		 * FabException("LNS005"); }
		 */

		if (VarChecker.isEmpty(repayDate)) {
			setRepayDate(ctx.getTranDate());
		}

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("brc", ctx.getBrc());
		param.put("acctno", acctNo);
		LnsAmortizeplan lnsamortizeplan;

		try {
			lnsamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnszlamortizeplan", param, LnsAmortizeplan.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "CUSTOMIZE.query_lnsamortizeplan");
		}

		if (null == lnsamortizeplan) {
			throw new FabException("LNS021");
		}
		if("1".equals(settleFlag) && CalendarUtil.after(ctx.getTranDate(), lnsamortizeplan.getEnddate())){
			throw new FabException("LNS176");
		}
		if (!VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING).contains(lnsamortizeplan.getStatus())) {
			throw new FabException("LNS030");
		}
		FabAmount cxAmount = new FabAmount(0.00);//冲销金额
		FabAmount cxTax = new FabAmount(0.00);//冲销税金
		FabAmount totalAmt = new FabAmount(lnsamortizeplan.getTotalamt()); // 摊销总金额
		FabAmount totalTaxAmt = new FabAmount(lnsamortizeplan.getTotaltaxamt()); // 摊销总税金
		FabAmount amortizedAmt = new FabAmount(lnsamortizeplan.getAmortizeamt()); // 上次已摊销金额
		FabAmount taxedAmt = new FabAmount(lnsamortizeplan.getAmortizetax()); // 上次已摊销税金
		FabAmount contractamt = new FabAmount(lnsamortizeplan.getContractamt());// 购机款
		FabAmount rentamt = new FabAmount(lnsamortizeplan.getRentamt());// 租金本息
		FabRate monrate = new FabRate(lnsamortizeplan.getMonrate());// 月利率
		String Lastdate = lnsamortizeplan.getLastdate();// 上次摊销日
		// 因为摊销总金额是含税的，所以需要将摊销总金额除以（1+利率）再乘以利率才是税金
		BigDecimal rate = new BigDecimal(lnsamortizeplan.getTaxrate());
		BigDecimal ratefactor = rate.add(new BigDecimal(1));
		if (!"1".equals(settleFlag)) {
			// 合同结束日年月一定大于等于当期日期的年月
			if (CalendarUtil.monthDifference(lnsamortizeplan.getEnddate(), repayDate) > 0) {
				throw new FabException("LNS062");
			}
			// 合同开始日年月一定小于等于当期日期的年月
			if (CalendarUtil.monthDifference(repayDate, lnsamortizeplan.getBegindate()) > 0) {
				throw new FabException("LNS062");
			}
			// 计算总的期数
			int totalPeriod = CalendarUtil.monthDifference(lnsamortizeplan.getBegindate(), lnsamortizeplan.getEnddate());
			// 当期期数
			int currPeriod = CalendarUtil.monthDifference(lnsamortizeplan.getBegindate(), repayDate);
			// 上一次摊销的期数
			int lastPeriod = CalendarUtil.monthDifference(lnsamortizeplan.getBegindate(), Lastdate);
			// 如果合同开始日期不是月末，则在当月的月末进行一次摊销
			if (!CalendarUtil.isMonthEnd(lnsamortizeplan.getBegindate())) {
				totalPeriod++;
				currPeriod++;
				lastPeriod++;
			}
			// 判断当前日期是否是月末
			if (CalendarUtil.isMonthEnd(repayDate)) {
				// 判断当期期数和上期期数是否相同
				if (currPeriod == lastPeriod && CalendarUtil.isMonthEnd(Lastdate)) {
					throw new FabException("LNS063");
				}
				// 每一期的租金本息
				FabAmount oncerentamt = new FabAmount(BigDecimal.valueOf(rentamt.getVal()).divide(new BigDecimal(totalPeriod), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
				// 判断是否是最后一期
				if (totalPeriod == currPeriod) {
					// 当期期的摊销金额
					setAmortizAmt((FabAmount) totalAmt.sub(amortizedAmt));
					// 当前期摊销税金
					setTaxAmt((FabAmount) totalTaxAmt.sub(taxedAmt));
					setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				} else if (currPeriod == 1) {
					// 当前期的摊销金额
					FabAmount currAmt = getamortiAmount(contractamt, currPeriod, oncerentamt, monrate);
					setAmortizAmt(currAmt);
					// 当前期摊销税金
					setTaxAmt(new FabAmount((BigDecimal.valueOf(currAmt.getVal()).divide(ratefactor, 9, BigDecimal.ROUND_HALF_UP).multiply(rate)).doubleValue()));
					setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				} else {
					// 当前期的摊销金额
					FabAmount currAmt = new FabAmount();
					if (!CalendarUtil.isMonthEnd(Lastdate)) {
						for (int i = currPeriod; i >= lastPeriod; i--) {
							currAmt.selfAdd(getamortiAmount(contractamt, i, oncerentamt, monrate).getVal());
						}
					} else {
						for (int i = currPeriod; i > lastPeriod; i--) {
							currAmt.selfAdd(getamortiAmount(contractamt, i, oncerentamt, monrate).getVal());
						}
					}
					// 当期期的摊销金额赋值
					setAmortizAmt(currAmt);
					// 当前期摊销税金
					setTaxAmt(new FabAmount((BigDecimal.valueOf(currAmt.getVal()).divide(ratefactor, 9, BigDecimal.ROUND_HALF_UP).multiply(rate)).doubleValue()));
					setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				}
			}
			// 结清标识
		} else if ("1".equals(settleFlag)) {
			// 当前期的摊销金额赋值
			setAmortizAmt((FabAmount) totalAmt.sub(amortizedAmt).sub(chargeAmt));
			//摊销金额为负数是
			if(amortizAmt.isNegative()){
				cxAmount =new FabAmount(0.00-amortizAmt.getVal());
			}
			// 当前期摊销税金
			setTaxAmt(new FabAmount((BigDecimal.valueOf(amortizAmt.getVal()).divide(ratefactor, 9, BigDecimal.ROUND_HALF_UP).multiply(rate)).doubleValue()));
			//摊销税金为负数是
			if(taxAmt.isNegative()){
				cxTax =new FabAmount(0.00-taxAmt.getVal());
			}
			//setTaxAmt((FabAmount) totalTaxAmt.sub(taxedAmt));
			// 摊销结束
			setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			//setChargeAmt(amortizAmt);
		}
		// 如果amortizAmt为空，则repayDate不是月底
		if (VarChecker.isEmpty(amortizAmt)) {
			return;
		}

		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsamortizeplan.getLoanstat().trim())) {
			setStatus(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);// 摊销结束
		}

		// 更新表中已摊销金额，已摊销税金，最后摊销日期
		param.clear();
		param.put("brc", ctx.getBrc());
		param.put("acctno", acctNo);
		param.put("lastdate", repayDate);
		param.put("amortizamt", amortizAmt.getVal());
		param.put("taxamt", taxAmt.getVal());
		param.put("status", status);
		if ("1".equals(settleFlag)) {
			param.put("chargeamt", chargeAmt.getVal());
		}

		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnszlamortizeplan", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsamortizeplan");
		}
		LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		// 登记利息计提摊销登记薄 lnsprovisionreg
		TblLnsprovisiondtl lnsprovisiondtl = new TblLnsprovisiondtl();
		lnsprovisiondtl.setAcctno(acctNo);
		lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
		lnsprovisiondtl.setTxnseq(1);
		lnsprovisiondtl.setBrc(ctx.getBrc());
		lnsprovisiondtl.setPeriod(loanAgreement.getContract().getCurrPrinPeriod());
		lnsprovisiondtl.setListno(lnsamortizeplan.getPeriod() + 1);
		lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);// 设置为利息
		lnsprovisiondtl.setCcy(new FabCurrency().getCcy());
		lnsprovisiondtl.setTotalinterest((amortizedAmt.add(amortizAmt)).getVal());// 已摊销金额+本次摊销金额
		lnsprovisiondtl.setTotaltax((taxedAmt.add(taxAmt)).getVal());// 已摊销税金+本次摊销税金
		lnsprovisiondtl.setTaxrate(lnsamortizeplan.getTaxrate());// 税率
		if(cxAmount.isPositive()){
			lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.NEGATIVE);// "POSITIVE");//正向
			lnsprovisiondtl.setInterest(cxAmount.getVal()); // 本次摊销金额
			lnsprovisiondtl.setTax(cxTax.getVal()); // 本次摊销税金
		}else{
			lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.POSITIVE);// "POSITIVE");//正向
			lnsprovisiondtl.setInterest(amortizAmt.getVal()); // 本次摊销金额
			lnsprovisiondtl.setTax(taxAmt.getVal()); // 本次摊销税金
		}
		lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.AMORTIZE);// "AMORTIZE");//摊销
		lnsprovisiondtl.setBegindate(Date.valueOf(lnsamortizeplan.getLastdate()));// 摊销表中最后摊销日期为本表起始日期
		lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
		lnsprovisiondtl.setReserv1(null);
		lnsprovisiondtl.setReserv2(null);
		lnsprovisiondtl.setTrandate(Date.valueOf(repayDate));
		lnsprovisiondtl.setEnddate(Date.valueOf(repayDate));// 本次调用的日期为结束日期
		LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
		try {
			DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "lnsprovisiondtl");
		}

		//摊销
		// 所有的计提，摊销  都不记税金表
		//AccountingModeChange.saveProvisionTax(ctx, acctNo, lnsprovisionreg.getInterest(), lnsprovisionreg.getTax(), "TX",
		//		lnsprovisionreg.getInterflag(), ConstantDeclare.BILLTYPE.BILLTYPE_NINT);

		LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());


		List<FabAmount> amtList = new ArrayList<FabAmount>();
		amtList.add(taxAmt);



		// 写事件
		//冲销时间
		if(cxAmount.isPositive()){
			amtList.clear();
			amtList.add(cxTax);
			if("51240001".equals(ctx.getBrc())){
				acctinfo.setCustType(customType);
				String reserv1=null;
				if("1".equals(customType)||"PERSON".equals(customType)){
					reserv1 = "70215243";
				}else if("2".equals(customType)||"COMPANY".equals(customType)){
					reserv1 = customid;
				}else{
					reserv1 = "";
				}
				eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZOFF, cxAmount, acctinfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.TXCX, ctx, amtList, repayDate,ctx.getSerSeqNo(), 1,reserv1);
			}else{
				eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZOFF, cxAmount, acctinfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.TXCX, ctx, amtList, repayDate,ctx.getSerSeqNo(), 1,customid);
			}
		}else{
			if("51240001".equals(ctx.getBrc())){
				acctinfo.setCustType(customType);
				String reserv1=null;
				if("1".equals(customType)||"PERSON".equals(customType)){
					reserv1 = "70215243";
				}else if("2".equals(customType)||"COMPANY".equals(customType)){
					reserv1 = customid;
				}else{
					reserv1 = "";
				}
				eventProvider.createEvent(ConstantDeclare.EVENT.LNAMORTIZE, amortizAmt, acctinfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.LXTX, ctx, amtList, repayDate,ctx.getSerSeqNo(), 1,reserv1);
			}else{
				eventProvider.createEvent(ConstantDeclare.EVENT.LNAMORTIZE, amortizAmt, acctinfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.LXTX, ctx, amtList, repayDate,ctx.getSerSeqNo(), 1,customid);
			}
		}
	}

	/**
	 * @param contractamt 购机款
	 *            
	 * 
	 * @param currperiod 当前期数
	 *            
	 * 
	 * @param oncerentamt  每期租金本息
	 *           
	 * 
	 * @param monrate 月利率
	 *            
	 * 
	 * @return amorAmt
	 */
	public FabAmount getamortiAmount(FabAmount contractamt, int currperiod, FabAmount oncerentamt, FabRate monrate) {
		//第n期摊销金额=[购机款*（1+期利率)^n-1 -每期租金本息*[(期利率+1)^n-1 -1]/期利率]*期利率
		//第一部分（购机款*（1+期利率)^n-1）
		BigDecimal firstPart = new BigDecimal(contractamt.getVal()).multiply(BigDecimal.valueOf(Math.pow(monrate.getVal().add(BigDecimal.valueOf(1)).doubleValue(), BigDecimal.valueOf(currperiod).subtract(BigDecimal.valueOf(1)).doubleValue()))); 
		//第二部分（每期租金本息*[(期利率+1)^n-1 -1]/期利率]）
		BigDecimal secondPart = new BigDecimal(oncerentamt.getVal()).multiply((BigDecimal.valueOf(Math.pow(monrate.getVal().add(BigDecimal.valueOf(1)).doubleValue(), BigDecimal.valueOf(currperiod).subtract(BigDecimal.valueOf(1)).doubleValue())).subtract(BigDecimal.valueOf(1))).divide(monrate.getVal(),2,BigDecimal.ROUND_HALF_UP));
		//第n期摊销金额=(第一部分-第二部分)*期利率
		return new FabAmount((firstPart.subtract(secondPart)).multiply(monrate.getVal()).doubleValue());
	}

	/**
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
	 * @return the repayDate
	 */
	public String getRepayDate() {
		return repayDate;
	}

	/**
	 * @param repayDate
	 *            the repayDate to set
	 */
	public void setRepayDate(String repayDate) {
		this.repayDate = repayDate;
	}

	/**
	 * @return the amortizAmt
	 */
	public FabAmount getAmortizAmt() {
		return amortizAmt;
	}

	/**
	 * @param amortizAmt
	 *            the amortizAmt to set
	 */
	public void setAmortizAmt(FabAmount amortizAmt) {
		this.amortizAmt = amortizAmt;
	}

	/**
	 * @return the taxAmt
	 */
	public FabAmount getTaxAmt() {
		return taxAmt;
	}

	/**
	 * @param taxAmt
	 *            the taxAmt to set
	 */
	public void setTaxAmt(FabAmount taxAmt) {
		this.taxAmt = taxAmt;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the dAmortizAmt
	 */
	public FabAmount getdAmortizAmt() {
		return dAmortizAmt;
	}

	/**
	 * @param dAmortizAmt
	 *            the dAmortizAmt to set
	 */
	public void setdAmortizAmt(FabAmount dAmortizAmt) {
		this.dAmortizAmt = dAmortizAmt;
	}

	/**
	 * @return the dTaxAmt
	 */
	public FabAmount getdTaxAmt() {
		return dTaxAmt;
	}

	/**
	 * @param dTaxAmt
	 *            the dTaxAmt to set
	 */
	public void setdTaxAmt(FabAmount dTaxAmt) {
		this.dTaxAmt = dTaxAmt;
	}

	/**
	 * @return the settleFlag
	 */
	public String getSettleFlag() {
		return settleFlag;
	}

	/**
	 * @param settleFlag
	 *            the settleFlag to set
	 */
	public void setSettleFlag(String settleFlag) {
		this.settleFlag = settleFlag;
	}

	/**
	 * @return the chargeAmt
	 */
	public FabAmount getChargeAmt() {
		return chargeAmt;
	}

	/**
	 * @param chargeAmt
	 *            the chargeAmt to set
	 */
	public void setChargeAmt(FabAmount chargeAmt) {
		this.chargeAmt = chargeAmt;
	}

	/**
	 * @return the repayAcctNo
	 */
	public String getRepayAcctNo() {
		return repayAcctNo;
	}

	/**
	 * @param repayAcctNo the repayAcctNo to set
	 */
	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
	}

	/**
	 * @return the customid
	 */
	public String getCustomid() {
		return customid;
	}

	/**
	 * @param customid the customid to set
	 */
	public void setCustomid(String customid) {
		this.customid = customid;
	}

	/**
	 * @return the customType
	 */
	public String getCustomType() {
		return customType;
	}

	/**
	 * @param customType the customType to set
	 */
	public void setCustomType(String customType) {
		this.customType = customType;
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

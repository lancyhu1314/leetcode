package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 呆滞结息
 *
 * @param repayDate 结息日期 
 * @param acctNo 贷款账号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns504 extends WorkUnit { 

	String	brc;
	String	acctNo;
	String	repayDate;
	Integer subNo;
	LnsBillStatistics billStatistics;
	LoanAgreement la;
	
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		//获取贷款协议信息
		la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		
		// 读取贷款账单
		Map<String, Object> parambill = new HashMap<String, Object>();
		parambill.put("acctno", getAcctNo());
		parambill.put("brc", ctx.getBrc());
		List<TblLnsbill> billList;
		try {
			billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", parambill, TblLnsbill.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}
		
		//如何确认一个字符串为空 从数据库里面取出来的可能有空格啊 难道要写一个去空格方法
		//如果是空格 那在比较中是大还是小
		for (TblLnsbill lnsbill : billList) {
			//首先处理呆滞本金产生呆滞罚息账单
			if( (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbill.getBillstatus()) || 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbill.getBillstatus())) && 
					lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
				//当前工作日期减罚息记至日期得到计罚息天数
				int nDays = CalendarUtil.actualDaysBetween(lnsbill.getIntedate(),ctx.getTranDate());
				if(nDays <= 0) //罚息已记至当日
					continue;
				
				BigDecimal intDec = new BigDecimal(nDays);
				//取账单余额 从la中取得利率
				//总利息 复利天利率*账本余额四舍五入后  乘以天数
				intDec = intDec.multiply(la.getRateAgreement().getCompoundRate().getDayRate().multiply( BigDecimal.valueOf(lnsbill.getBillbal()))
						.setScale(2, BigDecimal.ROUND_HALF_UP));
				FabAmount cBillBal = new FabAmount(intDec.doubleValue());
				
				if(cBillBal.isZero()) //计算罚息金额为零
					continue;
				
				FabAmount cTax = TaxUtil.calcVAT(cBillBal);
				
				//更新历史本金账单罚息记至日期
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("trandate", lnsbill.getTrandate());
				param.put("serseqno", lnsbill.getSerseqno());
				param.put("txseq", lnsbill.getTxseq());
				param.put("intedate", lnsbill.getIntedate());
				param.put("intedate1", ctx.getTranDate());
				param.put("accumulate", lnsbill.getAccumulate());
				
				try {
					DbAccessUtil.execute("CUSTOMIZE.update_hisbill", param);
				} catch (FabException e) {
					throw new FabException(e, "SPS102", "lnsbills");
				}
				
				//lnsBillStatistics.getBillNo();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(format.parse(ctx.getTranDate()).getTime()));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(++subNo);
				tblLnsbill.setAcctno(getAcctNo());
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				tblLnsbill.setPeriod(lnsbill.getPeriod());
				tblLnsbill.setBillamt(cBillBal.getVal());
				tblLnsbill.setBillbal(cBillBal.getVal());
				tblLnsbill.setPrinbal(lnsbill.getBillbal());
				tblLnsbill.setBillrate(la.getRateAgreement().getOverdueRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsbill.getIntedate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsbill.getRepayway());
				tblLnsbill.setCcy(lnsbill.getCcy());
				tblLnsbill.setDertrandate(lnsbill.getTrandate());
				tblLnsbill.setDerserseqno(lnsbill.getSerseqno());
				tblLnsbill.setDertxseq(lnsbill.getTxseq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsbill.getIntedate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}

				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);

				try{
					DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);		
				}catch (FabException e){
					throw new FabException(e, "SPS100", "lnsbill");
				}
				
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_DINT, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				FundInvest  fundInvest = new FundInvest();
				
				List<FabAmount> amtList = new ArrayList<FabAmount>();
				if(!cTax.isZero()) //计算税金金额不为零
					amtList.add(cTax);
				//罚息计提
				AccountingModeChange.saveProvisionTax(ctx, acctNo,cBillBal.getVal(), cTax.getVal(),
						"JT",ConstantDeclare.INTERFLAG.POSITIVE, ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				add.operate(lnsAcctInfo, null, cBillBal, fundInvest, ConstantDeclare.BRIEFCODE.FXJT, ctx.getTranDate(), ctx.getSerSeqNo(), subNo, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.DEFAULTINT, cBillBal, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.FXJT, ctx, amtList, ctx.getTranDate(), ctx.getSerSeqNo(), subNo, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
			}
			
			//处理呆滞利息产生呆滞复利账单
			if( (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbill.getBillstatus()) || 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbill.getBillstatus())) && 
					lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) ) {
				//当前工作日期减复利记至日期得到计复利天数
				int nDays = CalendarUtil.actualDaysBetween(lnsbill.getIntedate(),ctx.getTranDate());
				if(nDays <= 0) //复利已记至当日
					continue;
				
				BigDecimal intDec = new BigDecimal(nDays);
				//取账单余额  从la中取得利率
				//总利息 复利天利率*账本余额四舍五入后  乘以天数
				intDec = intDec.multiply(
						la.getRateAgreement().getCompoundRate().getDayRate().multiply( BigDecimal.valueOf(lnsbill.getBillbal()))
						.setScale(2, BigDecimal.ROUND_HALF_UP)
				);
				
				FabAmount cBillBal = new FabAmount(intDec.doubleValue());
				if(cBillBal.isZero()) //计算复利金额为零
					continue;
				
				FabAmount cTax = TaxUtil.calcVAT(cBillBal);
				
				//更新历史利息账单罚息记至日期
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("trandate", lnsbill.getTrandate());
				param.put("serseqno", lnsbill.getSerseqno());
				param.put("txseq", lnsbill.getTxseq());
				param.put("intedate", lnsbill.getIntedate());
				param.put("intedate1", ctx.getTranDate());
				param.put("accumulate", lnsbill.getAccumulate());
				
				try {
					DbAccessUtil.execute("CUSTOMIZE.update_hisbill", param);
				} catch (FabException e) {
					throw new FabException(e, "SPS102", "lnsbills");
				}
				
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(format.parse(ctx.getTranDate()).getTime()));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(++subNo);
				tblLnsbill.setAcctno(getAcctNo());
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_CINT);
				tblLnsbill.setPeriod(lnsbill.getPeriod());
				tblLnsbill.setBillamt(cBillBal.getVal());
				tblLnsbill.setBillbal(cBillBal.getVal());
				tblLnsbill.setPrinbal(lnsbill.getBillbal());
				tblLnsbill.setBillrate(la.getRateAgreement().getCompoundRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsbill.getIntedate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsbill.getRepayway());
				tblLnsbill.setCcy(lnsbill.getCcy());
				tblLnsbill.setDertrandate(lnsbill.getTrandate());
				tblLnsbill.setDerserseqno(lnsbill.getSerseqno());
				tblLnsbill.setDertxseq(lnsbill.getTxseq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsbill.getIntedate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}

				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				
				try{
					DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);		
				}catch (FabException e){
					throw new FabException(e, "SPS100", "lnsbill");
				}
				//罚息计提
				AccountingModeChange.saveProvisionTax(ctx, acctNo,cBillBal.getVal(), cTax.getVal(),
						"JT",ConstantDeclare.INTERFLAG.POSITIVE, ConstantDeclare.BILLTYPE.BILLTYPE_CINT);
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_DINT, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				FundInvest  fundInvest = new FundInvest();
				
				List<FabAmount> amtList = new ArrayList<FabAmount>();
				if(!cTax.isZero()) //计算税金金额不为零
					amtList.add(cTax);
				
				add.operate(lnsAcctInfo, null, cBillBal, fundInvest, ConstantDeclare.BRIEFCODE.FLJT, ctx.getTranDate(), ctx.getSerSeqNo(), subNo, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.COMPONDINT, cBillBal, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.FLJT, ctx, amtList, ctx.getTranDate(), ctx.getSerSeqNo(), subNo, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
			
			}
		
		}
	
	}	

	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}

	/**
	 * @param brc the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
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

	/**
	 * @return the subNo
	 */
	public Integer getSubNo() {
		return subNo;
	}

	/**
	 * @param subNo the subNo to set
	 */
	public void setSubNo(Integer subNo) {
		this.subNo = subNo;
	}

	/**
	 * @return the billStatistics
	 */
	public LnsBillStatistics getBillStatistics() {
		return billStatistics;
	}

	/**
	 * @param billStatistics the billStatistics to set
	 */
	public void setBillStatistics(LnsBillStatistics billStatistics) {
		this.billStatistics = billStatistics;
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


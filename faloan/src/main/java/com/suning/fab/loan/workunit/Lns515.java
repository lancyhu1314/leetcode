
package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsprovision;
import com.suning.fab.loan.domain.TblLnsprovisiondtl;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author 	
 *
 * @version V1.0.1
 *
 * 	服务费计提
 *
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns515 extends WorkUnit {

	String acctNo;
	String repayDate;
	String settleFlag;
	LnsBillStatistics lnssatistics;	//试算信息
	TblLnsbasicinfo lnsbasicinfo;	//贷款基本信息

	@Autowired 
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception{
		
		TranCtx ctx = getTranctx();
		
		if (VarChecker.isEmpty(repayDate)){
			throw new FabException("LNS005");
		}

		//贷款基本信息在原交易479001中已经获取
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		if (null == lnsbasicinfo){
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
		}

		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		//计提标志为close不计提
		if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsbasicinfo.getProvisionflag())){
			return;
		}

		Map<String,Object> provision = new HashMap<String,Object>();
		provision.put("receiptno", lnsbasicinfo.getAcctno());
		provision.put("intertype", ConstantDeclare.INTERTYPE.SERVERFEE);
		
//		TblLnsprovisionreg lnsprovision;
		TblLnsprovision lnsprovision_new;
		try {
			lnsprovision_new = LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,lnsbasicinfo.getAcctno(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.SERVERFEE,la );
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsprovision");
		}
		
//		if (null == lnsprovision){
//			lnsprovision = new TblLnsprovisionreg();
//			//第一次计提使用起息日
//			lnsprovision.setEnddate(Date.valueOf(lnsbasicinfo.getBeginintdate()));
//			lnsprovision.setTotaltax(0.00);
//			lnsprovision.setTotalinterest(0.00);
//
//		}
		//上次计提日期晚于交易日期不计提,当天不可重复计提
		 if (CalendarUtil.after(lnsprovision_new.getLastenddate().toString(), repayDate)){
			return;
		}
		//结息日为空取起息日
		if (VarChecker.isEmpty(lnsbasicinfo.getLastintdate())){
			lnsbasicinfo.setLastintdate(lnsbasicinfo.getBeginintdate());
		}
		
		//计提总税金
		FabAmount totaltax = new FabAmount();
		//计提总利息
		FabAmount totalnint = new FabAmount();
		
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())){
			//贷款结清直接累加所有利息账单金额
			Double nintAmt;
			try {
				nintAmt = DbAccessUtil.queryForObject("CUSTOMIZE.query_serfeebillsum", param, Double.class);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS103", "lnsbill");
			}

			if(null != nintAmt){
				totalnint.selfAdd(nintAmt);
				totaltax.selfAdd(TaxUtil.calcVAT(totalnint));
			}
		}
		else{
			
//			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
			//生成还款计划
			LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate, ctx);
			lnssatistics = lnsBillStatistics;
				
			//历史期账单（账单表）
			List<LnsBill> lnsbill = new ArrayList<LnsBill>();
			lnsbill.addAll(lnssatistics.getHisBillList());
			lnsbill.addAll(lnssatistics.getHisSetIntBillList());
			//当前期和未来期
			lnsbill.addAll(lnssatistics.getBillInfoList());
			lnsbill.addAll(lnssatistics.getFutureBillInfoList());
			lnsbill.addAll(lnssatistics.getFutureOverDuePrinIntBillList());
			
			
			//累加已还利息
			for (LnsBill bill:lnsbill){
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE).contains(bill.getBillType())){
					//计提日前的账单直接累加
					if (CalendarUtil.beforeAlsoEqual(bill.getEndDate(), repayDate)){
						totalnint.selfAdd(bill.getBillAmt());
						totaltax.selfAdd(TaxUtil.calcVAT(bill.getBillAmt()));
					}
					else if ((CalendarUtil.afterAlsoEqual(repayDate, bill.getStartDate())
							&& CalendarUtil.beforeAlsoEqual(repayDate, bill.getEndDate()))){
						
						if( "471013".equals(ctx.getTranCode())&&"1".equals(settleFlag) ){
							totalnint.selfAdd(bill.getBillBal());
							totaltax.selfAdd(TaxUtil.calcVAT(bill.getBillBal()));							
						}else
						{
							//等本等息利息计算
							if (ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
								//按天计算利息
								BigDecimal bigInt = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(bill.getStartDate(), repayDate)) 
												.multiply(BigDecimal.valueOf(lnsbasicinfo.getContractamt()) 
												.multiply(new FabRate(lnsbasicinfo.getReservamt1()).getDayRate()))
												.setScale(2,BigDecimal.ROUND_HALF_UP);
								//按天计算的利息大于账单金额取账单金额
								if (new FabAmount(bigInt.doubleValue()).sub(bill.getBillAmt()).isPositive()){
									totalnint.selfAdd(bill.getBillAmt());
									totaltax.selfAdd(TaxUtil.calcVAT(bill.getBillAmt()));
								}
								else {
									totalnint.selfAdd(bigInt.doubleValue());
									totaltax.selfAdd(TaxUtil.calcVAT(new FabAmount(bigInt.doubleValue())));
								}
							}
							//非等本等息
							else {
								totalnint.selfAdd(bill.getRepayDateInt());
								totaltax.selfAdd(TaxUtil.calcVAT(bill.getRepayDateInt()));
							}
						}
					}
				}
			}
		}
		
		//计提税金
		FabAmount tax = new FabAmount(totaltax.sub(lnsprovision_new.getTotaltax()).getVal());
		//计提利息
		FabAmount pronint = new FabAmount(totalnint.sub(lnsprovision_new.getTotalinterest()).getVal());
		
		if (pronint.isPositive() || tax.isPositive()){
			//登记计提登记薄
			TblLnsprovisiondtl lnsprovisiondtl = new TblLnsprovisiondtl();
			//交易日期
			lnsprovisiondtl.setTrandate(Date.valueOf(repayDate));
			//账务流水号
			lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
			//序号
			lnsprovisiondtl.setTxnseq(1);
			//机构
			lnsprovisiondtl.setBrc(ctx.getBrc());
			//借据
			lnsprovisiondtl.setAcctno(acctNo);
			//期数
			lnsprovisiondtl.setListno(lnsprovision_new.getTotallist() + 1);
			lnsprovisiondtl.setPeriod(la.getContract().getCurrPrinPeriod());
			//账单类型 NINT利息 DINT罚息 CINT复利
			lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE);
			//币种
			lnsprovisiondtl.setCcy(lnsbasicinfo.getCcy());
			//计提利息总金额
			lnsprovisiondtl.setTotalinterest(totalnint.getVal());
			//计提税金总金额
			lnsprovisiondtl.setTotaltax(totaltax.getVal());
			//税率
			lnsprovisiondtl.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
			//本次入账利息金额
			lnsprovisiondtl.setInterest(pronint.getVal());
			//本次入账税金金额
			lnsprovisiondtl.setTax(tax.getVal());
			//计提，摊销类型 取值：PROVISION计提，AMORTIZE摊销
			lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.SERVERFEE);
			//开始日期
			lnsprovisiondtl.setBegindate(lnsprovision_new.getLastenddate());
			//结束日期
			lnsprovisiondtl.setEnddate(Date.valueOf(repayDate));
			//正反标志 取值：POSITIVE正向，NEGATIVE反向
			lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.POSITIVE);
			//是否作废标志 取值：NORMAL正常，CANCEL作废
			lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);

//			try {
//				DbAccessUtil.execute("Lnsprovisionreg.insert", lnsprovisionreg);
//			}
//			catch (FabSqlException e)
//			{
//				throw new FabException(e, "SPS100", "lnsprovisionreg");
//			}
			LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
			try {
				DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS100", "lnsprovisiondtl");
			}
			MapperUtil.map(lnsprovisiondtl, lnsprovision_new, "map500_01");
			try {
				if(lnsprovision_new.isSaveFlag()){
					DbAccessUtil.execute("Lnsprovision.updateByUk", lnsprovision_new);
				}else{
					if(!"".equals(lnsprovisiondtl.getInterflag())){
						DbAccessUtil.execute("Lnsprovision.insert", lnsprovision_new);
					}
				}
			}catch (FabSqlException e){
				LoggerUtil.info("插入计提明细总表异常：",e);
				throw new FabException(e, "SPS102", "Lnsprovision");
			}

			//服务费计提
			// 所有的计提，摊销  都不记税金表
			//AccountingModeChange.saveProvisionTax(ctx, acctNo, pronint.getVal(), tax.getVal(), "JT",ConstantDeclare.INTERFLAG.POSITIVE, ConstantDeclare.BILLTYPE.BILLTYPE_AFEE);
			LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency(lnsbasicinfo.getCcy()));
			LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
			//写事件
			List<FabAmount> amtList = new ArrayList<FabAmount>() ;
			amtList.add(tax);

			eventProvider.createEvent(ConstantDeclare.EVENT.ACCRUEAFEE, 
					pronint, acctinfo, null, loanAgreement.getFundInvest(), 
					ConstantDeclare.BRIEFCODE.XFJT, ctx, amtList, repayDate, ctx.getSerSeqNo(), 1);
		}
		//主文件计提标志
		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())){

			Map<String,Object> basicinfo = new HashMap<String,Object>();
			basicinfo.put("acctno", lnsbasicinfo.getAcctno());
			basicinfo.put("brc", lnsbasicinfo.getOpenbrc());
			basicinfo.put("provisionflag", ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);

			int i = 0;
			try {
				i = DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_provisionflag", basicinfo);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS102", "lnsbasicinfo");
			}
			if(1 != i){
				throw new FabException("ACC108", lnsbasicinfo.getAcctno());
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
	 * @Return com.suning.fab.loan.bo.LnsBillStatistics lnssatistics
	 */
	public LnsBillStatistics getLnssatistics() {
		return lnssatistics;
	}

	/**
	 * @param lnssatistics
	 */
	public void setLnssatistics(LnsBillStatistics lnssatistics) {
		this.lnssatistics = lnssatistics;
	}

	/**
	 * @Return com.suning.fab.loan.domain.TblLnsbasicinfo lnsbasicinfo
	 */
	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	/**
	 * @param lnsbasicinfo
	 */
	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}

	/**
	 * 
	 * @return the settleFlag
	 */
	public String getSettleFlag() {
		return settleFlag;
	}

	/**
	 * @param settleFlag the settleFlag to set
	 */
	public void setSettleFlag(String settleFlag) {
		this.settleFlag = settleFlag;
	}
	
}

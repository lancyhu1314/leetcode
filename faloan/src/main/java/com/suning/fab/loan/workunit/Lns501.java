package com.suning.fab.loan.workunit;

import com.suning.fab.loan.bo.LnsBasicInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see -结息
 *
 * @param -repayDate 结息日期
 * @param -acctNo 贷款账号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns501 extends WorkUnit { 

	String	acctNo;
	String	repayDate;
	LnsBillStatistics lnsBillStatistics;
	TblLnsbasicinfo lnsbasicinfo;
	LoanAgreement la;

	//0金额结息费用list
	 Map<String, Map<String,FabAmount>> billFeeRepay=new HashMap<String, Map<String,FabAmount>>();
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		Integer prinTerm=0;
		Integer intTerm=0;
		Integer qqdCurrPrin=0;
		
		//获取贷款协议信息
		la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,la);
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", la.getContract().getReceiptNo());
		param.put("openbrc", ctx.getBrc());

		if(lnsbasicinfo==null) {
			try {
				lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}
			if (null == lnsbasicinfo) {
				throw new FabException("SPS104", "lnsbasicinfo");
			}
		}
		
		String lastPrinDate = la.getContract().getRepayPrinDate();
		String lastIntDate = la.getContract().getRepayIntDate();
		String lastDate = "";
		FabAmount tranAmt=new FabAmount(0.00);
		//根据预还款日期和预还款金额试算贷款各期账单，包括已结清、未结清账单
		lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate, ctx,lnsBillStatistics);
		//封顶时，更新
		DynamicCapUtil.updateDTFDA(la,ctx);
		while(lnsBillStatistics.hasHisSetIntBill())
		{
			LnsBill lnsBill = lnsBillStatistics.getHisSetIntBill();
			TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(la,lnsBill,ctx);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			tblLnsbill.setTrandate(new Date(format.parse(ctx.getTranDate()).getTime()));
			tblLnsbill.setSerseqno(ctx.getSerSeqNo());
			lnsBill.setTranDate(tblLnsbill.getTrandate());
			lnsBill.setSerSeqno(ctx.getSerSeqNo());
			
			//2018-01-26 START
			if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
			{	
				tblLnsbill.setBillamt(lnsBill.getRepayDateInt().getVal());
				tblLnsbill.setBillbal(lnsBill.getRepayDateInt().getVal());
				tblLnsbill.setEnddate(ctx.getTranDate());
			}
			//2018-01-26 END
			
			//复利累计积数产生的中间过程账单不插账单表
			if (!"AMLT".equals(lnsBill.getBillType()) && 
				!"GINT".equals(lnsBill.getBillType()) &&
				!"CINT".equals(lnsBill.getBillType()) &&
				!"DINT".equals(lnsBill.getBillType()) &&
					!lnsBill.isPenalty()&&
				new FabAmount(tblLnsbill.getBillamt()).isPositive())
			{
				try{
					DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);		
				}catch (FabException e){
					throw new FabException(e, "SPS100", "lnsbill");
				}
			}
			
			if (!VarChecker.isEmpty(lnsBill.getHisBill()))
			{
				//2018-01-26 START
				//结息后账单为利息，修改原利息账单利息记至日期和形态
				if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
				{
					/*更新本金账单上次结息日 add by huyi 2017-01-24*/
					lnsBill.setIntendDate(repayDate);
					lnsBill.setBillAmt(lnsBill.getRepayDateInt());
					if( lnsBill.getBillAmt().isPositive() ){  //2018-12-07 零利率利息账本
						LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
						//结息税金
//						LoanAcctChargeProvider.event(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ, TaxUtil.calcVAT(lnsBill.getBillAmt()));
						//结息税金
						AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
					}
					//更新原账单状态、利息记至日期、积数
					LoanTrailCalculationProvider.modifyBillInfo(lnsBill, la, ctx);
				}
				//2018-01-26 END
				
//				//结息后账单为宽限期利息，修改原本金账单利息记至日期和形态，利息记至日期改为结息后账单截止日期，形态根据日期修改
//				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT, ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
//							.contains(lnsBill.getBillType())) {
//					//宽限期利息记罚息，补充税金
//					List<FabAmount> amtList = new ArrayList<FabAmount>();
//					amtList.add(TaxUtil.calcVAT(lnsBill.getBillAmt()));
//					LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.DEFAULTINT, ConstantDeclare.BRIEFCODE.FXJT, amtList);
//				}
//
//				//结息后账单为复利，修改原利息账单利息记至日期和形态
//				else if (ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnsBill.getBillType()))
//				{
//					//复利入账，写事件
//					List<FabAmount> amtList = new ArrayList<FabAmount>();
//					amtList.add(TaxUtil.calcVAT(lnsBill.getBillAmt()));
//					LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.COMPONDINT, ConstantDeclare.BRIEFCODE.FLJT, amtList);
//				}
				
			}
		}

		while(lnsBillStatistics.hasBill())
		{
			//循环截止主文件上次结本日，上次结息日后的账单，账单结束日期小于等于当前工作日的账单插表，更新主文件上次结本日，结息日
			LnsBill lnsBill = lnsBillStatistics.getBill();
			if(!CalendarUtil.after(lnsBill.getEndDate(), repayDate)){
				//罚息未结
				//复利积数算出了值则不修改	利息计止日期
				//过了宽限期就没有复利积数了，落表后即为历史账本不会再结息落表
				//if(VarChecker.isEmpty(lnsBill.getAccumulate())||!lnsBill.getAccumulate().isPositive())
				lnsBill.setIntendDate(lnsBill.getEndDate());

				//免息 0利息账本，不插表 20190904
				if(lnsBill.getBillAmt().isPositive()) {
					lnsBillStatistics.writeBill(la, ctx, lnsBill);
				}else if(lnsBill.getBillAmt().isZero()&&LoanFeeUtils.isFeeType(lnsBill.getBillType())){
					//零费用金额账本落表
					lnsBillStatistics.writeBill(la, ctx, lnsBill);
				}

				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()))
				{
					lastPrinDate = lnsBill.getEndDate();
					lastDate = lastPrinDate;
					prinTerm = lnsBill.getPeriod();
					tranAmt.selfAdd(lnsBill.getBillAmt());
				}else if((VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()))){
					lastIntDate = lnsBill.getEndDate();
					lastDate = lastIntDate;
					intTerm = lnsBill.getPeriod();
					//结息
					if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL).contains(lnsBill.getBillStatus())){
						if( lnsBill.getBillAmt().isPositive() ){  //2018-12-07 零利率利息账本
							LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
							//结息税金
//							LoanAcctChargeProvider.event(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ, TaxUtil.calcVAT(lnsBill.getBillAmt()));
							//结息税金
							AccountingModeChange.saveIntSetmTax(getTranctx(),  la.getContract().getReceiptNo(), lnsBill);
							if(!lnsBill.getSerSeqno().equals(lnsBill.getDerSerseqno())
									&&lnsBill.getHisBill()!=null)
								//更新原账单状态、利息记至日期、积数
								LoanTrailCalculationProvider.modifyBillInfo(lnsBill, la, ctx);
						}
						if(!VarChecker.isEmpty(lnsBill.getTermFreeInterest())){
							//现金贷免息税金表生成记录
							AccountingModeChange.saveFreeInterestTax(ctx,acctNo,lnsBill);
							//主文件动态表对应免息金额减少
							AccountingModeChange.updateBasicDynMX(ctx,la.getBasicExtension().getFreeInterest().sub(lnsBill.getTermFreeInterest()).getVal(),acctNo,repayDate,lnsBill);
							//防止出现 有两期未结息的情况
							la.getBasicExtension().getFreeInterest().selfSub(lnsBill.getTermFreeInterest());
						}
					}
				}else if( LoanFeeUtils.isFeeType(lnsBill.getBillType())  ){
					if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL).contains(lnsBill.getBillStatus())){
						if( lnsBill.getBillAmt().isPositive() ){
							LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.FEESETLEMT, LoanFeeUtils.feeSettleBrief(lnsBill));
							if(!VarChecker.isEmpty(lnsBill.getTermFreeInterest())){
								AccountingModeChange.updateBasicDynMF(ctx,la.getBasicExtension().getFreeFee(lnsBill).sub(lnsBill.getTermFreeInterest()).getVal(),acctNo,repayDate,lnsBill);
								la.getBasicExtension().getFreeFee(lnsBill).selfSub(lnsBill.getTermFreeInterest());
							}
							//保费结息转列计提均不抛送SAP
							if(!LoanFeeUtils.isPremium(la.getPrdId())) {
								//结息税金
								AccountingModeChange.saveIntSetmTax(ctx, la.getContract().getReceiptNo(), lnsBill);
							}
							//更新上次结费日
							LoanFeeUtils.updateLastFeeDate(lnsBill);
						}else{
							//减免金额结费 减费
							if(!VarChecker.isEmpty(lnsBill.getTermFreeInterest())){
								AccountingModeChange.updateBasicDynMF(ctx,la.getBasicExtension().getFreeFee(lnsBill).sub(lnsBill.getTermFreeInterest()).getVal(),acctNo,repayDate,lnsBill);
								la.getBasicExtension().getFreeFee(lnsBill).selfSub(lnsBill.getTermFreeInterest());
								//更新上次结费日
								LoanFeeUtils.updateLastFeeDate(lnsBill);
								LoanRpyInfoUtil.addRpyList(billFeeRepay, lnsBill.getBillType(), lnsBill.getPeriod().toString(), new FabAmount(0.00),lnsBill.getRepayWay());

							}
						}
					}
				}
				//还款时展期的数据
			}else{
				/*
				当期的基本不会跑进来
				1、展期借据
				2、还款时
				3、利息账本
				4、在当期展期到期日
				5、利息对应上本金
				 */
				if(!VarChecker.isEmpty(la.getBasicExtension().getTermEndDate())
						&&VarChecker.asList("471007","471008","471009","471012").contains(ctx.getTranCode())
						&&(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()))
						&&CalendarUtil.before(repayDate, la.getBasicExtension().getTermEndDate())
						&&lnsBill.getHisBill()!=null){


					if(lnsBill.getRepayDateInt()!=null){
						//根据这个修改 本金的计止日期
						LnsBill bill =  lnsBill.deepClone();
						bill.setBillAmt(bill.getRepayDateInt());
						bill.setBillBal(bill.getRepayDateInt());
						bill.setEndDate(repayDate);
						bill.setIntendDate(repayDate);
						lnsBillStatistics.writeBill(la, ctx, bill);
						LoanTrailCalculationProvider.modifyBillInfo(bill, la, ctx);
						LoanAcctChargeProvider.add(bill, ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
						LnsBasicInfo basicinfo = new LnsBasicInfo();
						basicinfo.setLastIntDate(repayDate);
						LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);
					}
				}
			}
		}
		//还款时展期的数据
		/*
				1、展期借据
				2、还款时
				3、利息账本
				4、在当期展期到期日
				5、利息对应上本金
				 */
		if(VarChecker.asList("471007", "471008", "471009", "471012").contains(ctx.getTranCode())
				&&!VarChecker.isEmpty(la.getBasicExtension().getTermEndDate())) {
			for (LnsBill lnsBill:lnsBillStatistics.getFutureBillInfoList()){

				if ( (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()))
						&& CalendarUtil.before(repayDate, la.getBasicExtension().getTermEndDate())
						&& lnsBill.getHisBill() != null) {

					if (lnsBill.getRepayDateInt() != null) {
						//根据这个修改 本金的计止日期
						LnsBill bill = lnsBill.deepClone();
						bill.setBillAmt(bill.getRepayDateInt());
						bill.setBillBal(bill.getRepayDateInt());
						bill.setEndDate(repayDate);
						bill.setIntendDate(repayDate);
						LoanAcctChargeProvider.add(bill, ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
						lnsBillStatistics.writeBill(la, ctx, bill);
						LoanTrailCalculationProvider.modifyBillInfo(bill, la, ctx);
						LnsBasicInfo basicinfo = new LnsBasicInfo();
						basicinfo.setLastIntDate(repayDate);
						LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);
					}
					break;
				}
			}
		}
		if(prinTerm!=0 || intTerm!=0){
			
			//sun DOTO20180213合并
			//本金期数以利息期数为准
			if (prinTerm<intTerm){
				qqdCurrPrin = prinTerm;
				prinTerm = intTerm;
			}else
				intTerm = prinTerm;
			
			LnsBasicInfo basicinfo = new LnsBasicInfo();
			basicinfo.setIntTerm(intTerm-la.getContract().getCurrIntPeriod()+1);
			basicinfo.setLastIntDate(lastIntDate);
			//等额本息还款计划优化 2019-01-31
			if( "1".equals( la.getWithdrawAgreement().getRepayWay()))
				basicinfo.setLastIntDate(lastPrinDate);
			//2018-12-14 非标自定义迁移不允许提前还款（本息期数一致）
			if(!VarChecker.isEmpty(lnsbasicinfo.getFlag1()) && lnsbasicinfo.getFlag1().contains("2")){
				basicinfo.setLastPrinDate(lastIntDate);
			}else{
				basicinfo.setLastPrinDate(lastPrinDate);
			}
			
			basicinfo.setPrinTerm(prinTerm-la.getContract().getCurrPrinPeriod()+1);
			basicinfo.setTranAmt(tranAmt);
			
			//非标自定义不规则上次节本结息日同步
			if( ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(la.getWithdrawAgreement().getRepayWay()) )
			{
				basicinfo.setLastIntDate(lastDate);
				basicinfo.setLastPrinDate(lastDate);
			}
			
			//气球贷膨胀期数过大导致本金为0的场景，以利息期数为准
			if( "9".equals( la.getWithdrawAgreement().getRepayWay()))
			{
				if( qqdCurrPrin == 0 )
					basicinfo.setLastPrinDate(lastIntDate);
			}
				
			
			LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);
			
		}
		while(lnsBillStatistics.hasfutureOverDuePrinBill())
		{
			//循环截止主文件上次结本日，上次结息日后的账单，账单结束日期小于等于当前工作日的账单插表，更新主文件上次结本日，结息日
			LnsBill lnsBill = lnsBillStatistics.getFutureOverDuePrinIntBill();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			lnsBill.setTranDate(new Date(format.parse(ctx.getTranDate()).getTime()));
			lnsBill.setSerSeqno(ctx.getSerSeqNo());
			lnsBill.setDerTranDate(lnsBill.getTranDate());
			lnsBill.setDerSerseqno(lnsBill.getSerSeqno());
			
			if (!VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT, ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
					.contains(lnsBill.getBillType())
					&&!lnsBill.isPenalty())
			{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
			}

//			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT, ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
//					.contains(lnsBill.getBillType())) {
//				// 宽限期利息记罚息，补充税金
//				List<FabAmount> amtList = new ArrayList<FabAmount>();
//				amtList.add(TaxUtil.calcVAT(lnsBill.getBillAmt()));
//				LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.DEFAULTINT, ConstantDeclare.BRIEFCODE.FXJT, amtList);
//			}
//
//			//结息后账单为复利，修改原利息账单利息记至日期和形态
//			else if (ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnsBill.getBillType()))
//			{
//				//复利入账，补充税金
//				List<FabAmount> amtList = new ArrayList<FabAmount>();
//				amtList.add(TaxUtil.calcVAT(lnsBill.getBillAmt()));
//				LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.COMPONDINT, ConstantDeclare.BRIEFCODE.FLJT, amtList);
//			}
		}
		//2018-01-26 start
		if(!ConstantDeclare.REPAYWAY.REPAYWAY_XXHB.equals(la.getWithdrawAgreement().getRepayWay()) &&
		   !ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(la.getWithdrawAgreement().getRepayWay()) &&
		   !ConstantDeclare.REPAYWAY.REPAYWAY_YBYX.equals(la.getWithdrawAgreement().getRepayWay()) &&
		   !ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(la.getWithdrawAgreement().getRepayWay()))
			LoanRepayPlanProvider.interestRepayPlan( ctx,"SETTLE",la,lnsBillStatistics);
		//2018-01-26 end
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
	 * @return the lnsbasicinfo
	 */
	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	/**
	 * @param lnsbasicinfo the lnsbasicinfo to set
	 */
	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}


	/**
	 * Gets the value of la.
	 *
	 * @return the value of la
	 */
	public LoanAgreement getLa() {
		return la;
	}

	/**
	 * Sets the la.
	 *
	 * @param la la
	 */
	public void setLa(LoanAgreement la) {
		this.la = la;

	}

	public Map<String, Map<String, FabAmount>> getBillFeeRepay() {
		return billFeeRepay;
	}

	public void setBillFeeRepay(Map<String, Map<String, FabAmount>> billFeeRepay) {
		this.billFeeRepay = billFeeRepay;
	}

}


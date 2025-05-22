package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.utils.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBasicInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 结息
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
public class Lns507 extends WorkUnit { 

	String	acctNo;
	String	repayDate;
	LnsBillStatistics lnsBillStatistics;
	TblLnsbasicinfo lnsbasicinfo;
	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		Integer prinTerm=0;
		Integer intTerm=0;
		
		//获取贷款协议信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", la.getContract().getReceiptNo());
		param.put("openbrc", ctx.getBrc());
		
		try{
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		
		String lastPrinDate = la.getContract().getRepayPrinDate();
		String lastIntDate = la.getContract().getRepayIntDate();
		String lastDate = "";
		FabAmount tranAmt=new FabAmount(0.00);
		
		//根据预还款日期和预还款金额试算贷款各期账单，包括已结清、未结清账单
		lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate, ctx);
		while(lnsBillStatistics.hasHisSetIntBill())
		{
			LnsBill lnsBill = lnsBillStatistics.getHisSetIntBill();
			TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(la,lnsBill,ctx);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			tblLnsbill.setTrandate(new Date(format.parse(ctx.getTranDate()).getTime()));
			tblLnsbill.setSerseqno(ctx.getSerSeqNo());
			lnsBill.setTranDate(tblLnsbill.getTrandate());
			lnsBill.setSerSeqno(ctx.getSerSeqNo());
			
			//补记本金余额产生的利息
			if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
			{	
				tblLnsbill.setBillamt(lnsBill.getRepayDateInt().getVal());
				tblLnsbill.setBillbal(lnsBill.getRepayDateInt().getVal());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
			}
			
			//复利累计积数产生的中间过程账单不插账单表
			if (!"AMLT".equals(lnsBill.getBillType()) && 
				!"GINT".equals(lnsBill.getBillType()) &&
				!"CINT".equals(lnsBill.getBillType()) &&
				!"DINT".equals(lnsBill.getBillType()) &&
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
				//结息后账单为利息，修改原利息账单利息记至日期和形态
				if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
				{
					/*更新本金账单上次结息日 add by huyi 2017-01-24*/
					lnsBill.setIntendDate(repayDate);
					lnsBill.setBillAmt(lnsBill.getRepayDateInt());
					
					LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
					//结息税金
//					LoanAcctChargeProvider.event(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ, TaxUtil.calcVAT(lnsBill.getBillAmt()));
					//结息税金
					AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
					//更新原账单状态、利息记至日期、积数
					LoanTrailCalculationProvider.modifyBillInfo(lnsBill, la, ctx);
				}
				
//				//结息后账单为宽限期利息，修改原本金账单利息记至日期和形态，利息记至日期改为结息后账单截止日期，形态根据日期修改
//				else if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT, ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
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
				lnsBill.setIntendDate(lnsBill.getEndDate());
				lnsBillStatistics.writeBill(la,ctx,lnsBill);
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
						LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
						//结息税金
//						LoanAcctChargeProvider.event(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ, TaxUtil.calcVAT(lnsBill.getBillAmt()));
						//结息税金
						AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
					}
				}
			}else
				break;
		}
		/*if(prinTerm!=0 || intTerm!=0){
			

			//本金期数以利息期数为准
			if (prinTerm<intTerm){
				prinTerm = intTerm;
			}else
				intTerm = prinTerm;
			
			LnsBasicInfo basicinfo = new LnsBasicInfo();
			basicinfo.setIntTerm(intTerm-la.getContract().getCurrIntPeriod()+1);
			basicinfo.setLastIntDate(lastIntDate);
			basicinfo.setLastPrinDate(lastPrinDate);
			basicinfo.setPrinTerm(prinTerm-la.getContract().getCurrPrinPeriod()+1);
			basicinfo.setTranAmt(tranAmt);
			
			LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);
			
		}*/
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
					.contains(lnsBill.getBillType()))
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
		
		
		/* 提前还款时当期利息账单落表  add by huyi 2017-01-24*/
		while( lnsBillStatistics.hasFutureBill() && !la.getContract().getFlag1().contains("2") &&
			   !ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway())	//2019-05-09
				) //2018-12-14 非标自定义迁移不允许提前还款（利息）
		{
			LnsBill lnsBill = lnsBillStatistics.getFutureBill();
			//循环截止主文件上次结本日，上次结息日后的账单，当期账单插表，更新主文件上次结本日，结息日
			if(CalendarUtil.after(repayDate,lnsBill.getStartDate()) &&
			   CalendarUtil.before(repayDate,lnsBill.getEndDate()	)){

					if((VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()))){
						lnsBill.setEndDate(repayDate);
						lnsBill.setBillAmt(lnsBill.getRepayDateInt());
						lnsBill.setBillBal(lnsBill.getBillAmt());
						if( ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(la.getWithdrawAgreement().getRepayWay()))
							lnsBill.setPeriod(la.getUserDefineAgreement().size());
						lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
						lnsBillStatistics.writeBill(la,ctx,lnsBill);
						
						
						lastIntDate = lnsBill.getEndDate();
						intTerm = lnsBill.getPeriod();
						
						//结息
						if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL).contains(lnsBill.getBillStatus())){
							LoanAcctChargeProvider.add(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
							//结息税金
//							LoanAcctChargeProvider.event(lnsBill, ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ, TaxUtil.calcVAT(lnsBill.getBillAmt()));
							//结息税金
							AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
						}
					}
				}else
					break;
			}
		
		if(prinTerm!=0 || intTerm!=0){
		
			LnsBasicInfo basicinfo = new LnsBasicInfo();
			basicinfo.setIntTerm(intTerm-la.getContract().getCurrIntPeriod()+1);
			basicinfo.setLastIntDate(lastIntDate);
			basicinfo.setLastPrinDate(lastPrinDate);
			basicinfo.setPrinTerm(prinTerm-la.getContract().getCurrPrinPeriod()+1);
			basicinfo.setTranAmt(tranAmt);
			
			//非标自定义不规则上次节本结息日同步
			if( ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(la.getWithdrawAgreement().getRepayWay()) )
			{
				basicinfo.setLastIntDate(lastDate);
				basicinfo.setLastPrinDate(lastDate);
			}
			LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);
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



}


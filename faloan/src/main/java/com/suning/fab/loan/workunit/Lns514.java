package com.suning.fab.loan.workunit;

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
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 18049705
 *
 * @version V1.0.0
 *
 * @see 利息罚息 减免金额试算
 *
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns514 extends WorkUnit {
	
	LoanAgreement loanAgreement;
	String dealDate;
	String realDate;
	String settleFlag;
	String acctNo;
	LnsBillStatistics lnsBillStatistics ;
	TblLnsbasicinfo lnsbasicinfo;
	FabAmount nintRed =  new FabAmount();
	FabAmount dintRed =  new FabAmount();
	FabAmount feeRed  =  new FabAmount();

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		//Integer dateInvl = CalendarUtil.actualDaysBetween(realRepayDate, ctx.getTranDate());
		if( !VarChecker.isEmpty(realDate))
			dealDate=realDate;
		
		//查询主文件表
		Map<String, Object> preparam = new HashMap<String, Object>();
		if(null == lnsbasicinfo)
        {
            preparam.put("acctno", acctNo);
            preparam.put("openbrc", tranctx.getBrc());
            try{
                lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", preparam, TblLnsbasicinfo.class);
            }catch (FabSqlException e){
                throw new FabException(e, "SPS103", "lnsbasicinfo");
            }
            if(lnsbasicinfo==null)
                 throw new FabException("SPS104", "lnsbasicinfo");
        }

        if(CalendarUtil.before(dealDate, lnsbasicinfo.getOpendate())){
			throw new FabException("LNS204");

		}
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);
		List<LnsBill> tranDateBill  = new ArrayList<LnsBill>();

		tranDateBill.addAll(lnsBillStatistics.getHisSetIntBillList());//历史期的利息 罚息
		tranDateBill.addAll(lnsBillStatistics.getBillInfoList());//当前期
		tranDateBill.addAll(lnsBillStatistics.getHisBillList());//
		tranDateBill.addAll(lnsBillStatistics.getCdbillList());//

//		tranDateBill.addAll(lnsBillStatistics.getFutureBillInfoList());//
		tranDateBill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况


		String maxDate = "1970-01-01";
		//统计trandate的罚息
		Map<String, LnsBill> DintMap = new HashMap<String, LnsBill>();
		if( "2".equals(settleFlag) )
		{
			for(LnsBill lnsBill : tranDateBill)
			{
				if( CalendarUtil.beforeAlsoEqual(lnsBill.getEndDate(), realDate))
				{
					if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType()) ||
							   ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()) ||
							   ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(lnsBill.getBillType())){
								DintMap.put(lnsBill.getTranDate().toString()
										+"+"+lnsBill.getSerSeqno().toString()
										+"+"+lnsBill.getTxSeq().toString(), lnsBill);
					}
				}
			}
		}
		maxDate = dintRedTrandate(tranDateBill, maxDate, DintMap);
		if("1".equals(settleFlag)) {
			for (LnsBill lnsBill : lnsBillStatistics.getFutureBillInfoList()) {
				if (lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)&&null != lnsBill.getRepayDateInt()){
						//等本等息取全期
						nintRed.selfAdd(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay())?
								lnsBill.getBillBal():lnsBill.getRepayDateInt());
				}
			}
		}

		//实际还款日不能小于上次还款日
		if( CalendarUtil.before(dealDate,maxDate) )  
			throw new FabException("LNS203");
		//实际还款日小于上次还款日 试算会空指针
		LnsBillStatistics lnsBills = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, dealDate, ctx);

		List<LnsBill> dealDateBill  = new ArrayList<>();
		//免息的实际还款日 减免 20200207
		if(!VarChecker.isEmpty(loanAgreement.getBasicExtension().getFreeInterest())
				&& !loanAgreement.getBasicExtension().dropNintsIsEmpty()){
			subFreeNint(lnsBills.getBillInfoList());
			subFreeNint(lnsBills.getFutureBillInfoList());
		}
		dealDateBill.addAll(lnsBills.getHisSetIntBillList());//历史期的利息 罚息
		dealDateBill.addAll(lnsBills.getBillInfoList());//当前期
		dealDateBill.addAll(lnsBills.getFutureOverDuePrinIntBillList());//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		dealDateBill.addAll(lnsBills.getHisBillList());//
//		dealDateBill.addAll(lnsBills.getFutureBillInfoList());//
		dealDateBill.addAll(lnsBills.getCdbillList());//呆滞呆账罚息

		/*
		 *计算减免罚息 利息
		 * 
		 */

		for(LnsBill lnsBill : dealDateBill)
		{			
			if (lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)||
					lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)||
					lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT))
			{
				
				dintRed.selfSub(lnsBill.getBillAmt());
		    }
		    if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)&&"1".equals(settleFlag)){

					nintRed.selfSub(lnsBill.getBillBal());

		    }
		}
		if("1".equals(settleFlag)) {
			for (LnsBill lnsBill : lnsBills.getFutureBillInfoList()) {
				if (lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)&&null != lnsBill.getRepayDateInt()){
					//等本等息取全期
					nintRed.selfSub(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay())?
							lnsBill.getBillBal():lnsBill.getRepayDateInt());
				}
			}
		}
		Map<String,Object> stringJson = new HashMap<>();
		stringJson.put("reduceint", nintRed.getVal());		//自动减免利息
		stringJson.put("reducedint", dintRed.getVal()); 	//自动减免罚息
		stringJson.put("adjustfee",feeRed.getVal());			//调整违约金
		stringJson.put("realDate",dealDate);				//实际扣款日
		
        AccountingModeChange.saveInterfaceEx(ctx, acctNo, ConstantDeclare.KEYNAME.ZDJM, "自动减免", JsonTransfer.ToJson(stringJson));
        
		//结清的时候需要减免  未结清利息账单 利息
//		if("1".equals(settleFlag)){
//			
//			Iterator<LnsBill> dealDatebillIterator = lnsBills.getFutureBillInfoList().iterator();			
//			Iterator<LnsBill> tranDatebillIterator = lnsBillStatistics.getFutureBillInfoList().iterator();
//			LnsBill lnsBill;
//			while(tranDatebillIterator.hasNext()){
//				lnsBill = tranDatebillIterator.next();
//				if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
//					!VarChecker.isEmpty(lnsBill.getRepayDateInt())	)
//				{
//			    	if( null != lnsBill.getRepayDateInt() )
//			    		fnintRed.selfAdd(lnsBill.getRepayDateInt());
//					break;
//				}
//			}
//			while(dealDatebillIterator.hasNext())
//			{
//				lnsBill = dealDatebillIterator.next();
//				if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
//					!VarChecker.isEmpty(lnsBill.getRepayDateInt())	)
//				{
//			    	if( null != lnsBill.getRepayDateInt() )
//			    		fnintRed.selfSub(lnsBill.getRepayDateInt());
//					break;
//				}
//			}
//			
//		}
		
		
	}

	//免息的实际还款日 减免 20200207
	private void subFreeNint(List<LnsBill> futureBillInfoList) {
		for (LnsBill lnsBill : futureBillInfoList) {
			if (lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) && loanAgreement.getBasicExtension().getDropNints().get(lnsBill.getPeriod()) != null) {
				double freeNint = lnsBill.getBillAmt().sub(loanAgreement.getBasicExtension().getDropNints().get(lnsBill.getPeriod())).getVal();
				lnsBill.getBillAmt().selfSub(freeNint);
				lnsBill.setBillBal(lnsBill.getBillAmt());
				if (lnsBill.getRepayDateInt() != null)
					lnsBill.getRepayDateInt().selfSub(freeNint);
			}
		}
	}

	private String dintRedTrandate(List<LnsBill> tranDateBill, String maxDate, Map<String, LnsBill> dintMap) {
		for(LnsBill lnsBill : tranDateBill)
		{

			//过滤费用账本 20200407   实际还款日 ，费用先还，本息罚报错优化
			if(LoanFeeUtils.isFeeType(lnsBill.getBillType()) ||
			   LoanFeeUtils.isPenalty(lnsBill.getBillType()) ){
				continue;
			}


			if (lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)||
				lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)||
				lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT))
			{
				if( "2".equals(settleFlag) )
				{	LnsBill gintBill;
					//如果携带了历史账本 取历史账本的三编码  呆滞呆账用
					if(lnsBill.getHisBill()!=null)
						gintBill= dintMap.get(lnsBill.getHisBill().getTranDate().toString()
								+"+"+lnsBill.getHisBill().getSerSeqno().toString()
								+"+"+lnsBill.getHisBill().getTxSeq().toString());
					else
						gintBill= dintMap.get(lnsBill.getDerTranDate().toString()
							+"+"+lnsBill.getDerSerseqno().toString()
							+"+"+lnsBill.getDerTxSeq().toString());
					if( !VarChecker.isEmpty(gintBill))
						dintRed.selfAdd(lnsBill.getBillAmt());
				}
				else
				{
					dintRed.selfAdd(lnsBill.getBillAmt());
				}

		    }
		    if("1".equals(settleFlag)&& lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)){

					nintRed.selfAdd(lnsBill.getBillBal());

			}
		    if( !VarChecker.isEmpty(lnsBill.getLastDate()) &&
		    	CalendarUtil.after(lnsBill.getLastDate(),maxDate))
		    	maxDate = lnsBill.getLastDate();

		}
		return maxDate;
	}


	/**
	 * 
	 * @return the dealDate
	 */
	public String getDealDate() {
		return dealDate;
	}



	/**
	 * @param dealDate the dealDate to set
	 */
	public void setDealDate(String dealDate) {
		this.dealDate = dealDate;
	}



	/**
	 * 
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	/**
	 * @param loanAgreement the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
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
	 * 
	 * @return the nintRed
	 */
	public FabAmount getNintRed() {
		return nintRed;
	}

	/**
	 * @param nintRed the nintRed to set
	 */
	public void setNintRed(FabAmount nintRed) {
		this.nintRed = nintRed;
	}

	/**
	 * 
	 * @return the dintRed
	 */
	public FabAmount getDintRed() {
		return dintRed;
	}

	/**
	 * @param dintRed the dintRed to set
	 */
	public void setDintRed(FabAmount dintRed) {
		this.dintRed = dintRed;
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
	 * @return the realDate
	 */
	public String getRealDate() {
		return realDate;
	}



	/**
	 * @param realDate the realDate to set
	 */
	public void setRealDate(String realDate) {
		this.realDate = realDate;
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
	 * @return the feeRed
	 */
	public FabAmount getFeeRed() {
		return feeRed;
	}



	/**
	 * @param feeRed the feeRed to set
	 */
	public void setFeeRed(FabAmount feeRed) {
		this.feeRed = feeRed;
	}
	
	
}

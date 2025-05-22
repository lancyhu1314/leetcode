package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AdvanceFeeUtil;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.VarChecker;


@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns440 extends WorkUnit {

	String endDate;
	String acctNo;
    FabRate cleanFeeRate = new FabRate(0.00);
    FabAmount cleanFee = new FabAmount(0.00);
	LnsBillStatistics lnsBillStatistics;
	LoanAgreement la;
	
    @Override
    public void run() throws FabException, Exception {
		//根据账号生成协议
		TranCtx ctx = getTranctx();
    	if( null == la )
			la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
    	
//	    FabAmount totalPrin = new FabAmount(0.00);
//        //计算利息
//		FabAmount totalNint=new FabAmount(0.00);
//        //费用收取方式
//		String advanceFeeType=la.getInterestAgreement().getAdvanceFeeType();
//	    
	    //2019-10-12 现金贷提前还款违约金
	    if( null != la.getBasicExtension().getFeeRate() && 
	    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
	    	la.getBasicExtension().getFeeRate().isPositive() )
	    {
	    	cleanFeeRate = la.getBasicExtension().getFeeRate();
	    }
	    
	    if (la.getContract().getContractStartDate().equals(endDate) && 
				"3".equals(la.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
		}
	    
    	//放款日当天还款，手续费不收  ,  随借随还手续费不收
		if(!"3".equals(la.getInterestAgreement().getAdvanceFeeType()) && la.getContract().getContractStartDate().equals(endDate) ||
			la.getContract().getContractEndDate().equals(endDate) ||
			"8".equals(la.getWithdrawAgreement().getRepayWay()) ||
			null == cleanFeeRate )
		{
			cleanFee.setVal(0.00);
		}
		else{
			cleanFee = AdvanceFeeUtil.calAdvanceFee( cleanFeeRate, endDate, la, lnsBillStatistics);
		}
    	
//		else
//		{
//			List<LnsBill> lnsbill = new ArrayList<LnsBill>();
//
//			//取账本信息
//			lnsbill.addAll(lnsBillStatistics.getHisBillList());
//			lnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
//			lnsbill.addAll(lnsBillStatistics.getBillInfoList());
//			lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
//			lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
//			if( Arrays.asList("0","1","2","9","10").contains(la.getWithdrawAgreement().getRepayWay()))
//			{
//				for(LnsBill billNint:lnsbill){
//					//预约还款日期小于等于账本开始日期的属于未来期
//					if( CalendarUtil.beforeAlsoEqual(endDate, billNint.getStartDate())){
//						//累加未来期未还本金
//						if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
//							totalPrin.selfAdd(billNint.getBillBal());
//						}
//						//累加未来期利息金额
//						else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(billNint.getBillType())){
//							totalNint.selfAdd(billNint.getBillBal());
//						}
//
//					}
//					
//					//2019-10-12 现金贷提前还款违约金   开户传费率时按开户费率计算
//					if( null != la.getBasicExtension().getFeeRate() && 
//					    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
//					    	la.getBasicExtension().getFeeRate().isPositive() )
//				    {
//						if( CalendarUtil.after(endDate, billNint.getStartDate()) &&
//							CalendarUtil.beforeAlsoEqual(endDate, billNint.getEndDate()) 	){
//							//累加当期本金
//							if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
//								totalPrin.selfAdd(billNint.getBillBal());
//							}
//						}
//				    }
//				}
//			}
//			else
//			{
////					throw new FabException("LNS051");
//				Map<Integer,LnsBill> map = new HashMap<Integer,LnsBill>();
//				for(LnsBill bill:lnsbill){
//					LnsBill billList = map.get(bill.getPeriod());
//					if( null == billList )
//					{
//						//存利息账本开始结束日
//						bill.setBillBal( new FabAmount(0.00) );
//						map.put(bill.getPeriod(), bill);
//					}
//					else
//					{
//						//取较小的作为开始日期
//						if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
//							CalendarUtil.before(bill.getStartDate(), billList.getStartDate() ) )
//							billList.setStartDate(bill.getStartDate());
//						//取较大的作为结束日期
//						if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
//							CalendarUtil.after(bill.getEndDate(), billList.getEndDate() ) )
//							billList.setEndDate(bill.getEndDate());
//						//取本金余额作为当期本金
//						if( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())  &&
//							bill.getBillBal().sub(billList.getBillBal()).isPositive() )
//							billList.setBillBal(bill.getBillBal());
//						map.put(bill.getPeriod(), billList);
//					}
//				}
//				
//				for (LnsBill value : map.values()) { 
//					//预约还款日期小于等于账本开始日期的属于未来期
//					if( CalendarUtil.beforeAlsoEqual(endDate, value.getStartDate())){
//						//累加未来期未还本金
//						totalPrin.selfAdd(value.getBillBal());
//					}
//					
//					if( null != la.getBasicExtension().getFeeRate() && 
//					    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
//					    	la.getBasicExtension().getFeeRate().isPositive() )
//				    {
//						if( CalendarUtil.after(endDate, value.getStartDate()) &&
//							CalendarUtil.beforeAlsoEqual(endDate, value.getEndDate()) 	){
//							//累加当期本金
//								totalPrin.selfAdd(value.getBillBal());
//							}
//					}
//				} 
//			}
//			
//			//服务费=剩余本金*手续费率
//			if( null != cleanFeeRate && !VarChecker.isEmpty(cleanFeeRate) )
//			{
//				cleanFee = new FabAmount(  BigDecimal.valueOf(totalPrin.getVal()).multiply(cleanFeeRate.getVal()).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue()    );
//				if( la.getBasicExtension().getDynamicCapAmt().isPositive() &&
//					la.getBasicExtension().getDynamicCapDiff().sub(cleanFee).isNegative())
//					cleanFee = la.getBasicExtension().getDynamicCapDiff();
//				//利息和费用大小比较
//                if(ConstantDeclare.ADVANCEFEETYPE.FIXED.equals(advanceFeeType)&&totalNint.sub(cleanFee).isNegative()){
//                	//取利息和费用 最小的作为收取费用
//					cleanFee=totalNint;
//				}
//			}
//		}
    	
    	

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
	 * @return the cleanFee
	 */
	public FabAmount getCleanFee() {
		return cleanFee;
	}

	/**
	 * @param cleanFee the cleanFee to set
	 */
	public void setCleanFee(FabAmount cleanFee) {
		this.cleanFee = cleanFee;
	}



	/**
	 * @return the cleanFeeRate
	 */
	public FabRate getCleanFeeRate() {
		return cleanFeeRate;
	}

	/**
	 * @param cleanFeeRate the cleanFeeRate to set
	 */
	public void setCleanFeeRate(FabRate cleanFeeRate) {
		this.cleanFeeRate = cleanFeeRate;
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

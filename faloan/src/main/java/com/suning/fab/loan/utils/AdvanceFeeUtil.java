package com.suning.fab.loan.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 计算税金
 *
 * @param tranAmt 计税金额 
 *
 * @return FabAmount 税金
 *
 * @exception
 */
public class AdvanceFeeUtil {
	private AdvanceFeeUtil(){
		//nothing to do
	}
	
	public static FabAmount calAdvanceFee(FabRate cleanFeeRate, String endDate, LoanAgreement la, LnsBillStatistics lnsBillStatistics )
	{
		FabAmount cleanFee = new FabAmount(0.00);
		FabAmount totalPrin = new FabAmount(0.00);
		FabAmount totalNint=new FabAmount(0.00);
			
		if( cleanFeeRate.isZero() ) {
			cleanFee.setVal(0.00);
		}
//		if( null != la.getBasicExtension().getFeeRate() && 
//	    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
//	    	la.getBasicExtension().getFeeRate().isPositive() )
//	    {
//	    	cleanFeeRate = la.getBasicExtension().getFeeRate();
//	    }
//	    
//    	//放款日当天还款，手续费不收  ,  随借随还手续费不收
//		if( la.getContract().getContractStartDate().equals(endDate) ||
//			la.getContract().getContractEndDate().equals(endDate) ||
//			"8".equals(la.getWithdrawAgreement().getRepayWay()))
//		{
//			cleanFee.setVal(0.00);
//		}
//		else
//		{
			
		List<LnsBill> lnsbill = new ArrayList<LnsBill>();

		//取账本信息
		lnsbill.addAll(lnsBillStatistics.getHisBillList());
		lnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		lnsbill.addAll(lnsBillStatistics.getBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		if( Arrays.asList("0","1","2","9","10").contains(la.getWithdrawAgreement().getRepayWay()))
		{
			for(LnsBill billNint:lnsbill){
				//预约还款日期小于等于账本开始日期的属于未来期
				if( CalendarUtil.beforeAlsoEqual(endDate, billNint.getStartDate())){
					//累加未来期未还本金
					if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
						totalPrin.selfAdd(billNint.getBillBal());
					}
					//累加未来期利息金额
					else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(billNint.getBillType())){
						totalNint.selfAdd(billNint.getBillBal());
					}

				}
				
				//2019-10-12 现金贷提前还款违约金   开户传费率时按开户费率计算
				if( null != la.getBasicExtension().getFeeRate() && 
				    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
				    	la.getBasicExtension().getFeeRate().isPositive() )
			    {
					if( CalendarUtil.after(endDate, billNint.getStartDate()) &&
						CalendarUtil.beforeAlsoEqual(endDate, billNint.getEndDate()) 	){
						//累加当期本金
						if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
							totalPrin.selfAdd(billNint.getBillBal());
						}
					}
			    }
			}
		}
		else
		{
//						throw new FabException("LNS051");
			Map<Integer,LnsBill> map = new HashMap<Integer,LnsBill>();
			for(LnsBill bill:lnsbill){
				LnsBill billList = map.get(bill.getPeriod());
				if( null == billList )
				{
					//存利息账本开始结束日
					bill.setBillBal( new FabAmount(0.00) );
					map.put(bill.getPeriod(), bill);
				}
				else
				{
					//取较小的作为开始日期
					if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
						CalendarUtil.before(bill.getStartDate(), billList.getStartDate() ) )
						billList.setStartDate(bill.getStartDate());
					//取较大的作为结束日期
					if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
						CalendarUtil.after(bill.getEndDate(), billList.getEndDate() ) )
						billList.setEndDate(bill.getEndDate());
					//取本金余额作为当期本金
					if( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())  &&
						bill.getBillBal().sub(billList.getBillBal()).isPositive() )
						billList.setBillBal(bill.getBillBal());
					map.put(bill.getPeriod(), billList);
				}
			}
			
			for (LnsBill value : map.values()) { 
				//预约还款日期小于等于账本开始日期的属于未来期
				if( CalendarUtil.beforeAlsoEqual(endDate, value.getStartDate())){
					//累加未来期未还本金
					totalPrin.selfAdd(value.getBillBal());
				}
				
				if( null != la.getBasicExtension().getFeeRate() && 
				    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
				    	la.getBasicExtension().getFeeRate().isPositive() )
			    {
					if( CalendarUtil.after(endDate, value.getStartDate()) &&
						CalendarUtil.beforeAlsoEqual(endDate, value.getEndDate()) 	){
						//累加当期本金
							totalPrin.selfAdd(value.getBillBal());
						}
				}
			} 
		}
		
//		}
			
		cleanFee = new FabAmount(  BigDecimal.valueOf(totalPrin.getVal()).multiply(cleanFeeRate.getVal()).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue()    );

		//利息和费用大小比较
		if(ConstantDeclare.ADVANCEFEETYPE.FIXED.equals(la.getInterestAgreement().getAdvanceFeeType())&&totalNint.sub(cleanFee).isNegative()){
			//取利息和费用 最小的作为收取费用
			cleanFee=totalNint;
		}
		
		if( la.getBasicExtension().getDynamicCapAmt().isPositive() &&
			la.getBasicExtension().getDynamicCapDiff().sub(cleanFee).isNegative())
			cleanFee = la.getBasicExtension().getDynamicCapDiff();
			
		return cleanFee;
	}

	/**
	 * 预约还款查询 计算费用所用未来期本金 及 利息
	 * @param cleanFeeRate
	 * @param endDate
	 * @param la
	 * @param lnsBillStatistics
	 * @return
	 */
	public static Map<String,FabAmount> calAdvanceTotalPrin(String endDate, LoanAgreement la, LnsBillStatistics lnsBillStatistics )
	{
		Map<String,FabAmount> prinAndNint=new HashMap();
		FabAmount totalPrin = new FabAmount(0.00);
		FabAmount totalNint=new FabAmount(0.00);
		List<LnsBill> lnsbill = new ArrayList<LnsBill>();
		//取账本信息
		lnsbill.addAll(lnsBillStatistics.getHisBillList());
		lnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		lnsbill.addAll(lnsBillStatistics.getBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		if( Arrays.asList("0","1","2","9","10").contains(la.getWithdrawAgreement().getRepayWay()))
		{
			for(LnsBill billNint:lnsbill){
				//预约还款日期小于等于账本开始日期的属于未来期
				if( CalendarUtil.beforeAlsoEqual(endDate, billNint.getStartDate())){
					//累加未来期未还本金
					if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
						totalPrin.selfAdd(billNint.getBillBal());
					}
					//累加未来期利息金额
					else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(billNint.getBillType())){
						totalNint.selfAdd(billNint.getBillBal());
					}

				}

				//2019-10-12 现金贷提前还款违约金   开户传费率时按开户费率计算
				if( null != la.getBasicExtension().getFeeRate() &&
						!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
						la.getBasicExtension().getFeeRate().isPositive() )
				{
					if( CalendarUtil.after(endDate, billNint.getStartDate()) &&
							CalendarUtil.beforeAlsoEqual(endDate, billNint.getEndDate()) 	){
						//累加当期本金
						if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
							totalPrin.selfAdd(billNint.getBillBal());
						}
					}
				}
			}
		}
		else
		{
			Map<Integer,LnsBill> map = new HashMap<Integer,LnsBill>();
			for(LnsBill bill:lnsbill){
				LnsBill billList = map.get(bill.getPeriod());
				if( null == billList )
				{
					//存利息账本开始结束日
					bill.setBillBal( new FabAmount(0.00) );
					map.put(bill.getPeriod(), bill);
				}
				else
				{
					//取较小的作为开始日期
					if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
							CalendarUtil.before(bill.getStartDate(), billList.getStartDate() ) )
						billList.setStartDate(bill.getStartDate());
					//取较大的作为结束日期
					if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
							CalendarUtil.after(bill.getEndDate(), billList.getEndDate() ) )
						billList.setEndDate(bill.getEndDate());
					//取本金余额作为当期本金
					if( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())  &&
							bill.getBillBal().sub(billList.getBillBal()).isPositive() )
						billList.setBillBal(bill.getBillBal());
					map.put(bill.getPeriod(), billList);
				}
			}

			for (LnsBill value : map.values()) {
				//预约还款日期小于等于账本开始日期的属于未来期
				if( CalendarUtil.beforeAlsoEqual(endDate, value.getStartDate())){
					//累加未来期未还本金
					totalPrin.selfAdd(value.getBillBal());
				}

				if( null != la.getBasicExtension().getFeeRate() &&
						!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
						la.getBasicExtension().getFeeRate().isPositive() )
				{
					if( CalendarUtil.after(endDate, value.getStartDate()) &&
							CalendarUtil.beforeAlsoEqual(endDate, value.getEndDate()) 	){
						//累加当期本金
						totalPrin.selfAdd(value.getBillBal());
					}
				}
			}
		}
		prinAndNint.put("totalPrin",totalPrin);
		prinAndNint.put("totalNint",totalNint);
        return prinAndNint;
	}

	/**
	 * 根据本金和利息计算罚息
	 * @param cleanFeeRate
	 * @param prinAndNint
	 * @return
	 */
	public static FabAmount calAdvanceFeeByTotalPrin(FabRate cleanFeeRate,Map<String,FabAmount> prinAndNint,String advanceFeeType,FabAmount dynamicCapAmt,FabAmount dynamicCapDiff){
		FabAmount cleanFee;
		//获取本金和利息
		FabAmount totalPrin = prinAndNint.get("totalPrin")!=null?prinAndNint.get("totalPrin"):new FabAmount(0.00);
		FabAmount totalNint=prinAndNint.get("totalNint")!=null?prinAndNint.get("totalNint"):new FabAmount(0.00);
		cleanFee = new FabAmount(  BigDecimal.valueOf(totalPrin.getVal()).multiply(cleanFeeRate.getVal()).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
		//利息和费用大小比较
		if(ConstantDeclare.ADVANCEFEETYPE.FIXED.equals(advanceFeeType)&&totalNint.sub(cleanFee).isNegative()){
			//取利息和费用 最小的作为收取费用
			cleanFee=totalNint;
		}
		if( dynamicCapAmt.isPositive() &&
				dynamicCapDiff.sub(cleanFee).isNegative())
			cleanFee = dynamicCapDiff;
		return cleanFee;
	}
}

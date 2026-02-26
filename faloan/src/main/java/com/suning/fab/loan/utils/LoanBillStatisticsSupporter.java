package com.suning.fab.loan.utils;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;

public class LoanBillStatisticsSupporter {
	private LoanBillStatisticsSupporter(){
		//nothing to do
	}
	
	
	/**
	 * 统计账单信息
	 * @param LnsBillStatistics
	 * @param LnsBill
	 * 
	 */
	public static void billStatistics(LnsBillStatistics lnsBillInfoAll,LnsBill lnsBill)
	{
		//本金
		if (ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType()))
		{
			//账单属性为结息，本金期数加1
			if (ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET.equals(lnsBill.getBillProperty()))
			{
				//累加总期数
				lnsBillInfoAll.setPrinTotalPeriod(lnsBillInfoAll.getPrinTotalPeriod()+1);
				//第一期 
				if (lnsBillInfoAll.getPrinTotalPeriod() == 1)
				{
					lnsBillInfoAll.setFirstRepayPrinDate(lnsBill.getEndDate());
				}
			}

//			lnsBill.setPeriod(lnsBillInfoAll.getPrinTotalPeriod());
//			
//			if (lnsBill.getPeriod() == 0)
//				lnsBill.setPeriod(1);
			
			//累加未还本金
			lnsBillInfoAll.getUnrepayPrin().selfAdd(lnsBill.getBillBal());
			//累加已还本金
			lnsBillInfoAll.getRepayPrin().selfAdd(lnsBill.getBillAmt().sub(lnsBill.getBillBal()));
			
		}
		//利息、宽限期利息
		if (lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
		{
			//账单属性为结息，本金期数加1
			if (ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET.equals(lnsBill.getBillProperty()))
			{
				//累加总期数
				lnsBillInfoAll.setIntTotalPeriod(lnsBillInfoAll.getIntTotalPeriod()+1);
				//第一期 
				if (lnsBillInfoAll.getIntTotalPeriod() == 1)
				{
					lnsBillInfoAll.setFirstRepayIntDate(lnsBill.getEndDate());
				}
			}
			//累加未还利息
			lnsBillInfoAll.getUnpaidInterest().selfAdd(lnsBill.getBillBal());
			//累加已还利息
			lnsBillInfoAll.getPaidInterest().selfAdd(lnsBill.getBillAmt().sub(lnsBill.getBillBal()));
			
//			lnsBill.setPeriod(lnsBillInfoAll.getIntTotalPeriod());
//			if (lnsBill.getPeriod() == 0)
//				lnsBill.setPeriod(1);
		}
		
		//罚息
		if (ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(lnsBill.getBillType()) ||
				ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(lnsBill.getBillType()))
		{
			//累加未还利息
			lnsBillInfoAll.getUnpaidforfeitInterest().selfAdd(lnsBill.getBillBal());
			//累加已还利息
			lnsBillInfoAll.getPaidforfeitInterest().selfAdd(lnsBill.getBillAmt().sub(lnsBill.getBillBal()));
			
		}
		//复利
		if (ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnsBill.getBillType()))
		{
			//累加未还利息
			lnsBillInfoAll.getUnpaidCompoundInterest().selfAdd(lnsBill.getBillBal());
			//累加已还利息
			lnsBillInfoAll.getPaidCompoundInterest().selfAdd(lnsBill.getBillAmt().sub(lnsBill.getBillBal()));
			
		}
	}
}

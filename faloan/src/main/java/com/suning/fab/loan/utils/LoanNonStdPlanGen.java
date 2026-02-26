package com.suning.fab.loan.utils;

import java.util.ArrayList;
import java.util.List;

import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.RepayWayRepayAtMaturitySupporter;
import com.suning.fab.loan.supporter.RepayWaySupporter;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.PlatConstant;

public class LoanNonStdPlanGen {
	
	public LoanNonStdPlanGen() {
		//do nothing
	}
	
	public static List<LnsBillStatistics> genNonStdPlan (String acctNo, TranCtx ctx, String repayDate ) throws FabException {
	
		List<LnsBillStatistics> LnsBillStatisticsList = new ArrayList<LnsBillStatistics>();

		List<LoanAgreement> laList = LoanAgreementProvider.genNonLoanAgreementFromDB(acctNo, ctx);
	
		for(LoanAgreement la : laList){
			LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate,ctx);
			LnsBillStatisticsList.add(lnsBillStatistics);
		}
		return LnsBillStatisticsList;
	}
	
}

package com.suning.fab.loan.supporter;

import java.util.List;

import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbillplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.base.TranCtx;

public interface  LoanRePayPlanSupporter {
	
	public  List<RePayPlan> genRepayPlan(LoanAgreement la, TblLnsbasicinfo lnsbasicinfo, LnsBillStatistics lnsBillStatistics, List<TblLnsbillplan> tblBillPlanList, TranCtx ctx);
	
}

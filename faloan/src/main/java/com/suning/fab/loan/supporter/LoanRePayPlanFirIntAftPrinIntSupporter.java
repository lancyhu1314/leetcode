package com.suning.fab.loan.supporter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbillplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.base.TranCtx;
@Scope("prototype")
@Repository
/** 先还息,后本息  repayWay：4 */
public class LoanRePayPlanFirIntAftPrinIntSupporter implements LoanRePayPlanSupporter {

	@Override
	public List<RePayPlan> genRepayPlan(LoanAgreement la, TblLnsbasicinfo lnsbasicinfo,
			LnsBillStatistics lnsBillStatistics, List<TblLnsbillplan> tblBillPlanList, TranCtx ctx) {
		return new ArrayList<RePayPlan>();
	}
}

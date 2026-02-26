package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns413;
import com.suning.fab.loan.workunit.Lns422;
import com.suning.fab.loan.workunit.Lns451;
import com.suning.fab.loan.workunit.Lns465;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：展期还款计划试算
 *
 * @author 18049687
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-ExpandDateCalculation")
public class Tp477019 extends ServiceTemplate {

	@Autowired Lns451 lns451;
	@Autowired Lns422 lns422;
	@Autowired Lns413 lns413;
	@Autowired Lns465 lns465;
	//总行数
	Integer totalLine;
	FabAmount termrettotal; //	应还合计
	FabAmount totalAmt;     //	已还合计
	//json串
	String pkgList;

	public Tp477019() {
		needSerSeqNo=false;
	}
	
	@Override
	protected void run() throws Exception {


			trigger(lns465);
			//根据费用区分一下
			if(lns465.getLoanAgreement().getFeeAgreement().isEmpty())
			{
				lns413.setLnsBillStatistics(lns465.getBillStatistics());
				lns413.setLa(lns465.getLoanAgreement());
				lns413.setTblRpyPlanList(lns465.getRpyplanlist());
				trigger(lns413);
				totalLine = lns413.getTotalLine();
				pkgList = lns413.getPkgList();
			}else{
				lns422.setLnsBillStatistics(lns465.getBillStatistics());
				lns422.setLa(lns465.getLoanAgreement());
				lns422.setTblRpyPlanList(lns465.getRpyplanlist());
				trigger(lns422);
				totalLine = lns422.getTotalLine();
				pkgList = lns422.getPkgList();
			}
			termrettotal = lns465.getTermrettotal();
			totalAmt = lns465.getTotalAmt();



	}

	@Override
	protected void special() throws Exception {
		//nothing to do
	}

}

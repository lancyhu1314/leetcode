package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns413;
import com.suning.fab.loan.workunit.Lns418;
import com.suning.fab.loan.workunit.Lns420;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
*@author    HY
* 
*@version   V1.0.0
*
*@see       P2P还款计划查询
*
*@param     
*
*@return    
*
*@exception 
*/
@Scope("prototype") 
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-PPrepayPlanQuery")
public class Tp477015 extends ServiceTemplate {

@Autowired Lns413 lns413;

@Autowired Lns420 lns420;
public Tp477015() {
	needSerSeqNo=false;
}

@Override
protected void run() throws Exception {
	   
	   trigger(lns413);
	   lns420.setRpyPlanList(lns413.getRepayPlanList());
	   trigger(lns420);

     
	
}
@Override
protected void special() throws Exception {
	//nothing to do
}
}


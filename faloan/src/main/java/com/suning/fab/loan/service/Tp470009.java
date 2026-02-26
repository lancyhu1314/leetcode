package com.suning.fab.loan.service;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns400;
import com.suning.fab.loan.workunit.Lns439;
import com.suning.fab.loan.workunit.Lns440;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

import java.util.ArrayList;
import java.util.List;

/**
	*@author    AY
	* 
	*@version   V1.0.0
	*
	*@see       预约还款查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-provisionRepayQuery")
public class Tp470009 extends ServiceTemplate {

	@Autowired 
	Lns400 lns400;
	
	@Autowired
	Lns439 lns439;
	
	@Autowired
	Lns440 lns440;
	
	public Tp470009() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		trigger(lns400);
		trigger(lns439);
		
		lns440.setLnsBillStatistics(lns400.getLnsBillStatistics());
		lns440.setLa(lns400.getLa());
		trigger(lns440);

	}
	@Override
	protected void special() throws Exception {
		//nothing to do
	}
}

package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.workunit.Lns434;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns106;
import com.suning.fab.loan.workunit.Lns400;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    AY
	* 
	*@version   V1.0.0
	*
	      预约还款查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-newProvisionRepayQuery")
public class Tp470010 extends ServiceTemplate {

	@Autowired 	Lns106 lns106;
	@Autowired	Lns400 lns400;
	@Autowired
	Lns434 lns434;
	
	public Tp470010() {
		needSerSeqNo=false;
	}
	
	FabAmount cleanPrin = new FabAmount();
	FabAmount prinAmt = new FabAmount();
	FabAmount cleanInt = new FabAmount();
	FabAmount intAmt = new FabAmount();
	FabAmount cleanForfeit = new FabAmount();
	FabAmount forfeitAmt = new FabAmount();

	@Override
	protected void run() throws Exception {
		Integer curTerm = Integer.valueOf(0);
		
		trigger(lns106);
		Map<Integer, String> acctMap = lns106.getAcctList();
		curTerm = lns106.getCurTerm();
		
		for (Map.Entry<Integer, String> entry : acctMap.entrySet())
		{
			if(curTerm == (0))
			{	
				trigger(lns400);
				setCleanPrin(lns400.getCleanPrin());
				setPrinAmt(lns400.getPrinAmt());
				setCleanInt(lns400.getCleanInt());
				setIntAmt(lns400.getIntAmt());
				setCleanForfeit(lns400.getCleanForfeit());
				setForfeitAmt(lns400.getForfeitAmt());
                if(lns400.getLa()!=null&&!lns400.getLa().getFeeAgreement().isEmpty()){
					//非标费用查询
					lns434.setLnsBillStatistics(lns400.getLnsBillStatistics());
					lns434.setCtx(lns400.getCtx());
					lns434.setLoanAgreement(lns400.getLa());
					trigger(lns434);
				}
			}
			else
			{
				Map<String, Object> map400 = new HashMap<String, Object>();
				map400.put("acctNo", entry.getValue());
				trigger(lns400, "map400", map400);
				cleanPrin.selfAdd(lns400.getCleanPrin());
				cleanInt.selfAdd(lns400.getCleanInt());
				cleanForfeit.selfAdd(lns400.getCleanForfeit());
				if( entry.getKey() <= curTerm)
				{	
					prinAmt.selfAdd(lns400.getPrinAmt());
					intAmt.selfAdd(lns400.getIntAmt());
					forfeitAmt.selfAdd(lns400.getForfeitAmt());
				}
			}	
		}
	}
	@Override
	protected void special() throws Exception {
		//nothing to do
	}
	public FabAmount getCleanPrin() {
		return cleanPrin;
	}
	public void setCleanPrin(FabAmount cleanPrin) {
		this.cleanPrin = cleanPrin;
	}
	public FabAmount getPrinAmt() {
		return prinAmt;
	}
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}
	public FabAmount getCleanInt() {
		return cleanInt;
	}
	public void setCleanInt(FabAmount cleanInt) {
		this.cleanInt = cleanInt;
	}
	public FabAmount getIntAmt() {
		return intAmt;
	}
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}
	public FabAmount getCleanForfeit() {
		return cleanForfeit;
	}
	public void setCleanForfeit(FabAmount cleanForfeit) {
		this.cleanForfeit = cleanForfeit;
	}
	public FabAmount getForfeitAmt() {
		return forfeitAmt;
	}
	public void setForfeitAmt(FabAmount forfeitAmt) {
		this.forfeitAmt = forfeitAmt;
	}


}

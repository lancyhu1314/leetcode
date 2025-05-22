package com.suning.fab.loan.service;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AcctStatistics;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.workunit.Lns114;
import com.suning.fab.loan.workunit.Lns210;
import com.suning.fab.loan.workunit.Lns212;
import com.suning.fab.loan.workunit.Lns213;
import com.suning.fab.loan.workunit.Lns247;
import com.suning.fab.loan.workunit.Lns248;
import com.suning.fab.loan.workunit.Lns250;
import com.suning.fab.loan.workunit.Lns251;
import com.suning.fab.loan.workunit.Lns421;
import com.suning.fab.loan.workunit.Lns501;
import com.suning.fab.loan.workunit.Lns503;
import com.suning.fab.loan.workunit.Lns504;
import com.suning.fab.loan.workunit.Lns506;
import com.suning.fab.loan.workunit.Lns508;
import com.suning.fab.loan.workunit.Lns509;
import com.suning.fab.loan.workunit.Lns512;
import com.suning.fab.loan.workunit.Lns513;
import com.suning.fab.loan.workunit.Lns514;
import com.suning.fab.loan.workunit.Lns515;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    18049705
	* 
	*@version   V1.0.0
	*
	*@see       P2P还款
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-PPRepay")
public class Tp471013 extends ServiceTemplate{
	@Autowired Lns508 lns508;
	@Autowired Lns509 lns509;
	
	@Autowired Lns501 lns501;
	@Autowired Lns503 lns503;
	
	@Autowired Lns210 lns210;
	

	
	@Autowired Lns212 lns212;
	
	@Autowired Lns213 lns213;
	
	@Autowired Lns504 lns504;
	
	@Autowired Lns506 lns506;
	
	@Autowired Lns421 lns421;
	@Autowired Lns512 lns512; 
	@Autowired Lns513 lns513; 

	@Autowired Lns247 lns247; 
	@Autowired Lns248 lns248; 
	@Autowired Lns250 lns250;
	@Autowired Lns251 lns251;
	@Autowired Lns114 lns114;
	@Autowired Lns514 lns514;
	@Autowired Lns515 lns515;
	public Tp471013() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {

		String limitRepeatingRepaymentsProductList = GlobalScmConfUtil.getProperty("LimitRepeatingRepaymentsProductList", "");
		List<String> productList =Arrays.asList(limitRepeatingRepaymentsProductList.split(","));
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("acctNo").toString(), ctx);
		
		String key = ctx.getRequestDict("acctNo").toString() + ctx.getRequestDict("repayAmt").toString();
		if(!productList.contains(la.getPrdId()) &&  
		   false == (AcctStatistics.execute(key,ctx.getRequestDict("serialNo").toString())))
		{
			throw new FabException("LNS054");
		}
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("repayDate", ctx.getTranDate());
		//写Hbase
		trigger(lns421);
		//还款时手续费
		trigger(lns213);
		//结息
		trigger(lns501, "map501", param);
		
		FabAmount nintRed = new FabAmount();
		FabAmount dintRed = new FabAmount();
		FabAmount fnintRed = new FabAmount();
		if( ctx.getRequestDict("reduceIntAmt") != null )
		{
			if( Double.valueOf(ctx.getRequestDict("reduceIntAmt").toString()) > 0.00 )
			{
				nintRed.selfAdd(Double.valueOf(ctx.getRequestDict("reduceIntAmt").toString()));
			}
		}
		
		if(ctx.getRequestDict("reduceFintAmt") != null)
		{
			if(Double.valueOf(ctx.getRequestDict("reduceFintAmt").toString()) > 0.00 )
				dintRed.selfAdd(Double.valueOf(ctx.getRequestDict("reduceFintAmt").toString()));
		}
		//判断是否需要计算减免利息
		if(CalendarUtil.before(ctx.getRequestDict("dealDate").toString()
				, ctx.getTranDate()))
		{
			lns514.setLnsBillStatistics(lns501.getLnsBillStatistics());
			lns514.setLoanAgreement(la);
			
			trigger(lns514);
			nintRed.selfAdd(lns514.getNintRed());
			dintRed.selfAdd(lns514.getDintRed());
			//减免  未结清利息账单 利息  在未来期减免
		}
		//罚息计提
		lns512.setRepayDate(ctx.getTranDate());
		lns512.setLnssatistics(lns501.getLnsBillStatistics());
		lns512.setLnsbasicinfo(lns501.getLnsbasicinfo());
		trigger(lns512);
		
		//罚息落表
		
		lns513.setRepayDate(ctx.getTranDate());	
		lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
		lns513.setLnsbasicinfo(lns501.getLnsbasicinfo());
		trigger(lns513);
		//服务费计提
		if("1".equals(ctx.getRequestDict("settleFlag")))
		{
			lns515.setRepayDate(ctx.getRequestDict("dealDate") );
		}else
		{
			lns515.setRepayDate(ctx.getTranDate());
		}
		lns515.setLnsbasicinfo(lns501.getLnsbasicinfo());
		trigger(lns515);
		//转列
		trigger(lns503, "map503", param);
		Map<String, Object> param114 = new HashMap<String, Object>();
		param114.put("sumdelint", new FabAmount());
		param114.put("sumdelfint", new FabAmount());

		boolean ifOverdue = false;

		//利息减免
		if(nintRed.isPositive() || fnintRed.isPositive())
		{
			Map<String, Object> map247 = new HashMap<String, Object>();
			map247.put("subNo", lns501.getLnsBillStatistics().getBillNo());
			map247.put("billStatistics", lns501.getLnsBillStatistics());
			map247.put("reduceIntAmt", nintRed);
			map247.put("fnintRed", fnintRed);
			trigger(lns247,"map247", map247);
			ifOverdue = lns247.getIfOverdue();
			lns501.getLnsBillStatistics().setBillNo(lns247.getSubNo() + 2);
			param114.put("sumdelint", lns247.getSumdelint());
		}
		
		//减免罚息
		
		if(dintRed.isPositive())
		{
			Map<String, Object> map248 = new HashMap<String, Object>();
			map248.put("reduceFintAmt", dintRed);
			map248.put("ifOverdue",ifOverdue );
			trigger(lns248,"map248",map248);
			ifOverdue = lns248.getIfOverdue();
			param114.put("sumdelfint", lns248.getSumdelfint());
		}
		 
		
		
		Map<String, Object> map251 = new HashMap<String, Object>();
		map251.put("lnsbasicinfo", lns501.getLnsbasicinfo());
		map251.put("subNo", lns501.getLnsBillStatistics().getBillNo());
		map251.put("billStatistics", lns501.getLnsBillStatistics());
		//P2P违约金标志
		if("1".equals(ctx.getRequestDict("settleFlag")))
		{			
			lns250.setTblLnsbasicinfo(lns501.getLnsbasicinfo());
			lns250.setLnsBillStatistics(lns501.getLnsBillStatistics());
			Map<String, Object> map250 = new HashMap<String, Object>();
			map250.put("subNo", lns501.getLnsBillStatistics().getBillNo());
			trigger(lns250,"map250",map250);
			param114.put("penaltyAmt", lns250.getPenaltyAmt());
			FabAmount repayAmt = new FabAmount(Double.valueOf(ctx.getRequestDict("repayAmt").toString())); 
			repayAmt.selfSub(lns250.getPenaltyAmt());
			map251.put("repayAmt",repayAmt);
			map251.put("penaltyAmt",lns250.getPenaltyAmt());
			map251.put("subNo", lns250.getSubNo());

		}else{
			param114.put("penaltyAmt", new FabAmount(0.00));
			
		}
		lns251.setIfOverdue(ifOverdue);
		trigger(lns251,"map251",map251);
				
		
		trigger(lns210);
		
		//还款--还款渠道
		Map<String, Object> map212 = new HashMap<String, Object>();
		map212.put("repayAmtMap", lns251.getRepayAmtMap());
		lns212.setLoanAgreement(lns251.getLoanAgreement());
		trigger(lns212,"map212",map212);
		//贷款结清状态更新
		trigger(lns506);
		lns251.setEndFlag(lns506.getEndFlag());//非幂等的时候取lns506的endflag

		trigger(lns114,"map114",param114);
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		if( !VarChecker.isEmpty(pkgList1) )
		{
			Map<String, Object> map508 = new HashMap<String, Object>();
			map508.put("prinAmt", lns251.getPrinAmt());
			map508.put("nintAmt", lns251.getNintAmt());
			map508.put("dintAmt", lns251.getDintAmt());
			trigger(lns508,"DEFAULT",map508);
			trigger(lns509,"DEFAULT",map508);
		}
		
	}
	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}
}

package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.workunit.Lns101;
import com.suning.fab.loan.workunit.Lns102;
import com.suning.fab.loan.workunit.Lns201;
import com.suning.fab.loan.workunit.Lns202;
import com.suning.fab.loan.workunit.Lns203;
import com.suning.fab.loan.workunit.Lns204;
import com.suning.fab.loan.workunit.Lns521;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabRuntimeException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       贷款开户
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
//@Service
//@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-PPcreateAcct")
public class Tp473008 extends ServiceTemplate {

	@Autowired Lns101 lns101;
	
	@Autowired Lns102 lns102;
	
	@Autowired Lns201 lns201;
	
	@Autowired Lns202 lns202;
	
	@Autowired Lns203 lns203;
	
	@Autowired Lns204 lns204;
	
	@Autowired Lns521 lns521;	//开户校验
	
	public Tp473008() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		//载入协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreement("4010001");
		//开户校验
		lns521.setLa(la);
		trigger(lns521);
		
		//2017-12-05 起息日期传值，合同开始日取起息日
		//la.getContract().setContractStartDate(ctx.getTranDate());

		if(VarChecker.isEmpty( ctx.getRequestDict("startIntDate") ) || ctx.getRequestDict("startIntDate").toString().isEmpty())
		{
			la.getContract().setStartIntDate(ctx.getTranDate());
			la.getContract().setContractStartDate(ctx.getTranDate());
		}
		else
		{
			if( CalendarUtil.actualDaysBetween(ctx.getRequestDict("startIntDate").toString(), ctx.getTranDate()) >15 )
			{
				throw new FabException("LNS077");
			}
			la.getContract().setStartIntDate(ctx.getRequestDict("startIntDate").toString());
			la.getContract().setContractStartDate(ctx.getRequestDict("startIntDate").toString());
		}
		
		Map<String, Object> map473006 = new HashMap<String, Object>();
		map473006.put("ccy", "01");
		map473006.put("intPerUnit", "M");
		map473006.put("periodNum", 1);
		map473006.put("normalRateType", "Y");
		map473006.put("overdueRateType", "Y");
		map473006.put("compoundRateType", "Y");
		if("8".equals(ctx.getRequestDict("repayWay").toString()))
			map473006.put("loanType", "1");
		else
			map473006.put("loanType", "2");
		map473006.put("discountFlag", "1");
		map473006.put("productCode", "4010001");
		map473006.put("discountAmt", 0);
		map473006.put("cashFlag", "2");
		
		map473006.put("serialNo", ctx.getRequestDict("serialNo").toString());
		map473006.put("outSerialNo", ctx.getRequestDict("outSerialNo").toString());
			map473006.put("contractAmt", new FabAmount(Double.valueOf(ctx.getRequestDict("contractAmt").toString())));
		map473006.put("contractNo", ctx.getRequestDict("contractNo").toString());
		map473006.put("merchantNo", ctx.getRequestDict("merchantNo").toString());
		map473006.put("customName", "");
		map473006.put("customType", ctx.getRequestDict("customType").toString());
		map473006.put("endDate", ctx.getRequestDict("endDate").toString());
		map473006.put("repayDate", ctx.getRequestDict("repayDate").toString());
		if(ctx.getRequestDict("normalRate")==null){
			throw new FabException("LNS055","正常利率");
		}
		if(ctx.getRequestDict("overdueRate")==null){
			throw new FabException("LNS055","逾期利率");
		}
		if(ctx.getRequestDict("compoundRate")==null){
			throw new FabException("LNS055","复利利率");
		}
			map473006.put("normalRate", new FabRate(Double.valueOf(ctx.getRequestDict("normalRate").toString())));
			map473006.put("overdueRate", new FabRate(Double.valueOf(ctx.getRequestDict("overdueRate").toString())));
			map473006.put("compoundRate", new FabRate(Double.valueOf(ctx.getRequestDict("compoundRate").toString())));
		map473006.put("channelType", ctx.getRequestDict("channelType").toString());
		map473006.put("receiptNo", ctx.getRequestDict("receiptNo").toString());
			map473006.put("graceDays", Integer.valueOf(ctx.getRequestDict("graceDays").toString()));
		map473006.put("openBrc", ctx.getRequestDict("openBrc").toString());
		map473006.put("fundChannel", ctx.getRequestDict("fundChannel").toString());
		map473006.put("repayWay", ctx.getRequestDict("repayWay").toString());
		map473006.put("startIntDate", ctx.getRequestDict("startIntDate").toString());
			map473006.put("serviceFeeRate", new FabRate(Double.valueOf(ctx.getRequestDict("serviceFeeRate").toString())));
			map473006.put("penaltyFeeRate", new FabRate(Double.valueOf(ctx.getRequestDict("penaltyFeeRate").toString())));

		
		lns101.setPenaltyFeeRate(new FabRate(Double.valueOf(ctx.getRequestDict("penaltyFeeRate").toString())));
		lns101.setLoanAgreement(la);

		trigger(lns101,"DEFAULT",map473006);
		trigger(lns102,"DEFAULT",map473006);
		trigger(lns201,"DEFAULT",map473006);
		trigger(lns202,"DEFAULT",map473006);
		trigger(lns203,"DEFAULT",map473006);
		trigger(lns204,"DEFAULT",map473006);
	}
	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}
	
	@Override	
	protected void normalBeforeRun(){
//		if(!VarChecker.isValidConstOption(ctx.getRequestDict("repayWay").toString(), ConstantDeclare.REPAYWAY.class))
//		{
//			throw new FabRuntimeException("LNS051");
//		}
		
//		if(VarChecker.isEmpty( ctx.getRequestDict("loanType").toString()))
//			throw new FabRuntimeException("LNS052");
	}
}

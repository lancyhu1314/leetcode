package com.suning.fab.loan.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       非标准贷款开户
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-nonstdCreateAcctAndLoan")
public class Tp473005 extends ServiceTemplate {

	@Autowired Lns104 lns104; //开户登记
	
	@Autowired Lns105 lns105; //本金放款
	
	@Autowired Lns203 lns203; //放款渠道
	
	@Autowired Lns119 lns119;	//非标自定义不算利
	
	@Autowired Lns521 lns521;	//开户校验
	
	@Autowired Lns202 lns202;	//债务公司-放款
	@Autowired Lns102 lns102;	//债务公司-开户
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	@Autowired
	Lns255 lns255;    //费用list入库
	public Tp473005() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		//非标自定义不能扣息放款
		if( !VarChecker.isEmpty(ctx.getRequestDict("discountAmt")) )
		{
			if( new FabAmount( Double.valueOf(ctx.getRequestDict("discountAmt").toString())).isPositive() )
					throw new FabException("LNS093");
		}
		
		if( !VarChecker.isEmpty(ctx.getRequestDict("discountFlag")))
		{
			if( "2".equals(ctx.getRequestDict("discountFlag").toString()) )
					throw new FabException("LNS093");
		}
		//calcIntFlag1和calcIntFlag2只支持是1
		if( !VarChecker.isEmpty(ctx.getRequestDict("calcIntFlag1"))) {
			if (!"1".equals(ctx.getRequestDict("calcIntFlag1").toString()))
				throw new FabException("LNS196");
		}
		if( !VarChecker.isEmpty(ctx.getRequestDict("calcIntFlag2"))) {
			if (!"1".equals(ctx.getRequestDict("calcIntFlag2").toString()))
				throw new FabException("LNS196");
		}
		//载入协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreement(ctx.getRequestDict("productCode").toString());


		//开户校验
		lns521.setLa(la);
		trigger(lns521);
				
				
		trigger(lns105);
		
		Map<String, Object> map473005 = new HashMap<String, Object>();
		if( ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(ctx.getRequestDict("repayWay").toString()))
			//非标自定义不算利
			trigger(lns119);
		else
		{
			//非标自定义
			trigger(lns104);
			map473005.put("receiptNo", lns104.getAcctNoNon());
		}
		
		//2019-05-13 非标自定义增加债务公司
		if( !"2".equals(ctx.getRequestDict("calcIntFlag1").toString()))
		{
			trigger(lns102);
			trigger(lns202);
		}
		else
		{
			ListMap pkgList = ctx.getRequestDict("pkgList");
			if(pkgList == null || pkgList.size() == 0)
			{
				//无追保理和买方付息产品的债务公司必输
				if( Arrays.asList("3010006","3010013","3010014","3010015").contains((ctx.getRequestDict("productCode").toString())))
				{
					throw new FabException("LNS055","债务公司");
				}
			}
			else
				throw new FabException("LNS163");
		}
		//防止阻碍幂等
		trigger(lns121);
		trigger(lns203, "map473005", map473005);
		//增加费用入库功能
		if (!VarChecker.isEmpty(ctx.getRequestDict("pkgList3"))) {
			LoanAgreement loanAgreement=LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("receiptNo").toString(), ctx);
			//存放计息方式
			loanAgreement.getInterestAgreement().setPeriodFormula("3");
			lns255.setLoanAgreement(loanAgreement);
			lns255.setOpenBrc(ctx.getBrc());
			trigger(lns255);
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

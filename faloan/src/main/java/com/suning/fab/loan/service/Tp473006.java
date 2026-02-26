package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.loopmsg.ListMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    
	* 
	*@version   V1.0.0
	*
	*@see       租赁开户
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-createLeaseAcct")
public class Tp473006 extends ServiceTemplate {

	//计算租金总本息和总本金，尾款总本息和总本金
	@Autowired Lns431 lns431;
	//还款计划生成
	@Autowired Lns432 lns432;
	
	@Autowired Lns104 lns104; //开户登记
	
	@Autowired Lns105 lns105; //本金放款
	
	@Autowired Lns113 lns113; //本金放款
	
	@Autowired Lns200 lns200; //放款渠道
	
	@Autowired Lns249 lns249; //放款渠道
	
	@Autowired Lns237 lns237;
	@Autowired Lns121 lns121;//考核数据校验 入库

	public Tp473006() {
		needSerSeqNo=true;
	}
	

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

//		if(!ctx.getTranDate().equals(ctx.getTermDate()))
//			throw new FabException("LNS024");
		//租赁试算
		String productCode = ctx.getProductCode();	//默认产品代码（和胡老板确认）
		LoanAgreement la = LoanAgreementProvider.genLoanAgreement(productCode);
		lns431.setLoanAgreement(la);
		lns432.setLoanAgreement(la);

		trigger(lns431, "map431");

		lns432.setRepayWay("0");
		lns432.setRentAmt(lns431.getRentAmt());
		lns432.setFinalAmt(lns431.getFinalAmt());
		trigger(lns432);

		checkInvestee();

		//初始化开户信息
		Map<String, Object> map473006 = new HashMap<String, Object>();
		map473006.put("contractAmt", new FabAmount(lns431.getFinalAmt().add(lns431.getRentAmt()).getVal()));

		if( null == ctx.getRequestDict("overdueRate") )
			map473006.put("overdueRate", new FabRate(0.00));
		else
			map473006.put("overdueRate", ctx.getRequestDict("overdueRate"));
		
		
		if( null == ctx.getRequestDict("compoundRate") )
			map473006.put("compoundRate", new FabRate(0.00));
		else
			map473006.put("compoundRate", ctx.getRequestDict("compoundRate"));
		
		map473006.put("normalRate", new FabRate(0.00));
//		map473006.put("overdueRate", new FabRate(0.00));
//		map473006.put("compoundRate", new FabRate(0.00));
		map473006.put("loanType", "3");
		map473006.put("discountFlag", "1");
		map473006.put("repayWay", "13");
		map473006.put("calcIntFlag1", "1");
		map473006.put("calcIntFlag2", "1");
		//开户
		if("2412617".equals(productCode)){
			lns113.setRentPlanpkgList(lns432.getRentPlanpkgList());
			trigger(lns113, "DEFAULT", map473006);
			lns105.setOldrepayacct(lns113.getOldrepayacct());
			lns105.setReceiptNo(lns113.getReceiptNo());
			lns105.setCustomName(lns113.getCustomName());
		}else{
			lns105.setRentPlanpkgList(lns432.getRentPlanpkgList());
			trigger(lns105, "DEFAULT", map473006);
		}
		lns104.setRentPlanpkgList(lns432.getRentPlanpkgList());
		trigger(lns104, "DEFAULT", map473006);

		//放款渠道
		if("2412617".equals(productCode)){
			trigger(lns249);
		}else{
			trigger(lns200);
		}
		//防止幂等报错被拦截
		trigger(lns121);

		//登记租金试算登记簿
		lns237.setFinalAmt(lns431.getFinalAmt());
		lns237.setFinalPrin(lns431.getFinalPrin());
		lns237.setRentAmt(lns431.getRentAmt());
		lns237.setRentPrin(lns431.getRentPrin());
		lns237.setRentPlanpkgList(lns432.getRentPlanpkgList());
		trigger(lns237);
	}
	//机构号为51230004 必传资金方
	private void checkInvestee() throws FabException {
		if("51230004".equals(ctx.getBrc())){
			ListMap pkgList1 = ctx.getRequestDict("pkgList1");
			if(null==pkgList1||pkgList1.size()==0)
			throw  new FabException("LNS167","开户机构号",ctx.getBrc());
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

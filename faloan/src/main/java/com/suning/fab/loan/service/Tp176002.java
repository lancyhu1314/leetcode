package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;


/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*      预收账户充值
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-advanceAccountCharge")
public class Tp176002 extends ServiceTemplate {

	@Autowired 
	Lns214 lns214;
	@Autowired
	Lns199 lns199;//批量预收充值
	
	@Autowired 
	Lns215 lns215;
	@Autowired
	Lns256 lns256;
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	
	public Tp176002() {
		needSerSeqNo=true;
	}
	@Override
	protected void run() throws Exception {

		//2-赔付户（赔付时开户并充值）
		//3-保费户
		if(!VarChecker.isEmpty( ctx.getRequestDict("depositType"))
				&&!"1".equals(ctx.getRequestDict("depositType").toString())){
			trigger(lns256);
		}else{
			if (VarChecker.asList("2").contains(ctx.getRequestDict("batchType"))) {
				//预收账户充值多笔
				trigger(lns199);
			}else {
				//预收账户充值单笔
				trigger(lns214);

			}
		}




		//防止幂等报错被拦截
		trigger(lns121);
		FabAmount amt = ctx.getRequestDict("amt");
		ListMap pkgList = ctx.getRequestDict("pkgList");
		//用户累加循环金额
		FabAmount sumAmt = new FabAmount();
		//遍历循环报文
		if(pkgList != null && pkgList.size() > 0){
			for (PubDict pkg:pkgList.getLoopmsg()) {
				String debtCompany = PubDict.getRequestDict(pkg, "debtCompany");
				FabAmount debtAmt = PubDict.getRequestDict(pkg, "debtAmt");
				
				Map<String, Object> param = new HashMap<String,Object>();
				param.put("debtCompany", debtCompany);
				param.put("debtAmt", debtAmt);
				
				//债务公司预收充值
				trigger(lns215, "DEFAULT", param);
				//累加循环报文金额
				sumAmt.selfAdd(debtAmt);
			}
			//判断预收金额和保理总金额是否合法
			if(sumAmt.selfSub(amt).isPositive()){
				LoggerUtil.debug("充值充退累积金额与总金额校验非法:" + sumAmt + "|" + amt);
				throw new FabException("LNS028");
			}
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

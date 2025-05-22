package com.suning.fab.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.loan.workunit.Lns121;
import com.suning.fab.loan.workunit.Lns124;
import com.suning.fab.loan.workunit.Lns516;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：贷款封顶数据调整
 *
 * @author 
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-errorAdjustDynStatus")
public class Tp479008 extends ServiceTemplate{

	public Tp479008(){
		needSerSeqNo = true;
	}
	
	@Autowired
    Lns516 lns516;//幂等处理
	@Autowired
    Lns121 lns121;//考核数据校验 入库
	@Autowired
	Lns124 lns124;//贷款封顶数据调整
	
	
	@Override
	protected void run() throws FabException, Exception {
		
		//幂等登记
		lns516.setReceiptNo(ctx.getRequestDict("acctNo"));
		lns516.setTranAmt(new FabAmount(0.00));
		trigger(lns516);
		
		//考核数据校验 入库
		trigger(lns121);
		
		//贷款封顶数据调整
		trigger(lns124);
		
	}

	@Override
	protected void special() throws FabException, Exception {
		// 幂等成功处理
		if (VarChecker.asList(TRAN.SUCCESS,
				TRAN.IDEMPOTENCY).contains(
				ctx.getRspCode())) {
			/*
			 * 判断如果是幂等响应码则赋值全局响应码000000
			 */
			ctx.setRspCode(TRAN.SUCCESS);
		}
	}

}

/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Tp479010.java
 * Author:   16071579
 * Date:     2017年5月24日 上午10:34:46
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.service;

import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：非标数据迁移
 *
 * @author 
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-newProvisionDatetransfer")
public class Tp479000 extends ServiceTemplate {

	@Autowired Lns601 lns601;
	@Autowired Lns602 lns602;

	@Autowired Lns121 lns121;//考核数据校验 入库

	public Tp479000() {
		needSerSeqNo=true;
	}

	String repayAcctNo;
	String customName;
	String acctNo;
	
	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		//罚息利率不能大于利息利率 2019-01-14
		if(ctx.getRequestDict("normalRate")==null){
			throw new FabException("LNS055","正常利率");
		}
		if(ctx.getRequestDict("overdueRate")==null){
			throw new FabException("LNS055","逾期利率");
		}
		if(ctx.getRequestDict("compoundRate")==null){
			throw new FabException("LNS055","复利利率");
		}
		trigger(lns121);
		Double normalRate = Double.valueOf(ctx.getRequestDict("normalRate").toString());
		Double overRate = Double.valueOf(ctx.getRequestDict("overdueRate").toString());
		Double compoundRate = Double.valueOf(ctx.getRequestDict("compoundRate").toString());
		if(  (overRate.compareTo(normalRate)<0 && !new FabAmount(overRate).isZero())  || 
			 (compoundRate.compareTo(normalRate)<0 && !new FabAmount(compoundRate).isZero()) )
		{
			throw new FabException("LNS128");
		}

		//迁移不允许有还款和逾期 20190321
		if( "1".equals(ctx.getRequestDict("transferFlag").toString()) ||
			"2".equals(ctx.getRequestDict("transferFlag").toString()) 	)
			throw new FabException("LNS153",ctx.getRequestDict("transferFlag").toString());
	
		if( "3".equals(ctx.getRequestDict("transferFlag").toString())){
			trigger(lns601);
			repayAcctNo = lns601.getOldrepayacct();
			customName = lns601.getCustomName();
			acctNo = lns601.getReceiptNo();
		}else if( "4".equals(ctx.getRequestDict("transferFlag").toString())){
			trigger(lns602);
			repayAcctNo = lns602.getOldrepayacct();
			customName = lns602.getCustomName();
			acctNo = lns602.getReceiptNo();
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

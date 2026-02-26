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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.workunit.Lns502;
import com.suning.fab.loan.workunit.Lns510;
import com.suning.fab.loan.workunit.Lns523;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：摊销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-interestAmortize")
public class Tp479010 extends ServiceTemplate {

	@Autowired Lns502 lns502;
	@Autowired Lns510 lns510;
	@Autowired Lns523 lns523;
	
	public Tp479010() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		String brc = ctx.getBrc();
		//手机租赁、汽车租赁摊销
		if("51230004".equals(brc)||"51240000".equals(brc)||"51240001".equals(brc)){
			trigger(lns510);		
		}else{ 
//			if (!VarChecker.isEmpty(ctx.getRequestDict("amortizeType"))
//					&& (ConstantDeclare.AMORTIZETYPE.AMORTIZEFEE.equals(ctx.getRequestDict("amortizeType"))))
//				trigger(lns523);
			if (!VarChecker.isEmpty(ctx.getRequestDict("amortizeType"))
					&& (ConstantDeclare.AMORTIZETYPE.AMORTIZEINT.equals(ctx.getRequestDict("amortizeType"))))
				trigger(lns502);
			else
				trigger(lns523);
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

/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Tp475001.java
 * Author:   15032049
 * Date:     2017年6月9日 上午10:34:46
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
import com.suning.fab.loan.workunit.Lns405;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：贷款放款信息查询
 *
 * @author 15032049
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-queryLoanInformation")
public class Tp475001 extends ServiceTemplate {

	@Autowired Lns405 lns405;
	
	public Tp475001() {
		needSerSeqNo=false;
	}

	@Override
	protected void run() throws Exception {
		trigger(lns405);
	}
	
	@Override
	protected void special() throws Exception {
		/*Reserved*/ 	
	}
	
}

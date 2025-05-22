package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns412;
import com.suning.fab.loan.workunit.Lns501;
import com.suning.fab.loan.workunit.Lns503;
import com.suning.fab.loan.workunit.Lns504;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
 * @author SUN
 *
 * @version V1.0.0
 *
 * @see 还款金额试算
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repayTentativeCalculation")
public class Tp470008 extends ServiceTemplate {

	@Autowired
	Lns412 lns412;
	@Autowired
	Lns501 lns501;
	@Autowired
	Lns503 lns503;
	//@Autowired
	//Lns504 lns504;

	public Tp470008() {
		needSerSeqNo = true;
	}

	@Override
	protected void run() throws Exception {
		Map<String, Object> bparam = new HashMap<String, Object>();
		bparam.put("acctno", ctx.getRequestDict("acctNo"));
		bparam.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", bparam, TblLnsbasicinfo.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		Map<String, Object> param = new HashMap<>();
		param.put("repayDate", ctx.getTranDate());
		param.put("lnsbasicinfo", lnsbasicinfo);
		
		//核销处理
		if (!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lnsbasicinfo.getLoanstat())) {
			lns501.setLnsbasicinfo(lnsbasicinfo);
			trigger(lns501, "map51", param);
			trigger(lns503, "map53", param);
			trigger(lns412, "map412", lns501);
		}else{
			trigger(lns412,"DEFAULT",param);
		}
	}

	@Override
	protected void special() throws Exception {
		// nothing to do
	}
}

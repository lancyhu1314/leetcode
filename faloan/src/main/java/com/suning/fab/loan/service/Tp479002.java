package com.suning.fab.loan.service;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       贷款结息转列
	*
	*@param     repayDate 结息转列日期
	*@param		acctNo	  贷款账号	
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-settlePrincipalSettleInterest")
public class Tp479002 extends ServiceTemplate {

	@Autowired Lns501 lns501;
	@Autowired Lns503 lns503;
	@Autowired Lns505 lns505;
	//罚息落表
	@Autowired
	Lns513 lns513;

	public Tp479002() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		//查询基本信息给子交易句柄 并判断产品类型
		String acctNum = ctx.getRequestDict("acctNo");
		TblLnsbasicinfo lnsinfo;
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNum);
		param.put("openbrc", ctx.getBrc());
		try {
			//取主文件信息
			lnsinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if (null == lnsinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		/** 核销/非应计的贷款 不进行结息 2019-10-27  */
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains( lnsinfo.getLoanstat())){
			return;
		}
		lns501.setLnsbasicinfo(lnsinfo);
		trigger(lns501);
		trigger(lns503);
		trigger(lns505);
		/** 宽限期到期日  罚息落表 2019-07-31 start */
		//肯定没有逾期 呆滞呆账， 并且有宽限期
        //限制产品 乐业贷货押贷、零售云-货速融苏宁银行助贷
		if(VarChecker.asList("2412625","2521001").contains( lns501.getLa().getPrdId())
            && lns501.getLa().getContract().getGraceDays()>0
                && VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR)
				.contains( lns501.getLnsbasicinfo().getLoanstat()))
		{
			List<LnsBill> lnsBills = new ArrayList<>();
			lnsBills.addAll(lns501.getLnsBillStatistics().getBillInfoList());
			lnsBills.addAll( lns501.getLnsBillStatistics().getHisBillList());
			//遍历 当前账单
			for( LnsBill lnsBill:lnsBills)
			{
				//未结清的本金利息账本 宽限期到期日
				if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag())
						&&VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType())
						&&lns501.getLa().getContract().getGraceDays() ==  CalendarUtil.actualDaysBetween(lnsBill.getCurendDate(), ctx.getTranDate())){

					//罚息落表
					lns513.setRepayDate(ctx.getTranDate());
					lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
					lns513.setLnsbasicinfo( lns501.getLnsbasicinfo());
					trigger(lns513);
					break;
				}
			}
		}
		/* 2019-07-31end */
	}
	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}
}

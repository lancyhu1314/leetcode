package com.suning.fab.loan.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
 *@author    AY
 *
 *@version   V1.0.0
 *
 *
 *@param
 *
 *@return
 *
 *@exception
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-interestTentativeCalculation")
public class Tp479001 extends ServiceTemplate {

	//利息计提
	@Autowired Lns500 lns500;
	//管理费计提( 房抵贷 )
	@Autowired Lns511 lns511;
	//罚息计提
	@Autowired Lns512 lns512;
	//信息服务费计提( P2P )
	@Autowired Lns515 lns515;
	//罚息落表（ 房抵贷封顶计息 ）
	@Autowired Lns520 lns520;
	//违约金计提
	@Autowired Lns530 lns530;
	public Tp479001() {
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

		//利息计提
		lns500.setLnsbasicinfo(lnsinfo);

		//罚息计提
		lns512.setLnsbasicinfo(lnsinfo);
		//信息服务费计提( P2P )
		lns515.setLnsbasicinfo(lnsinfo);

		//P2P需要月末计提服务费
		if( "4010001".equals(lnsinfo.getPrdcode()) &&
			CalendarUtil.isMonthEnd(ctx.getTranDate()) ){
			trigger(lns515);	//服务费计提
		}
		
		//利息计提
		trigger(lns500);
		//试算信息传子交易计提罚息
		if (null != lns500.getLnssatistics()) {
			lns512.setLnssatistics(lns500.getLnssatistics());
		}
		trigger(lns512);
		/** 核销的贷款 不进行利息计提 2018-12-20 start */
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains( lnsinfo.getLoanstat())){
			return;
		}
		/** end **/
		//费用计提
		if (!lns512.getLa().getFeeAgreement().isEmpty()){
			lns511.setLoanAgreement(lns512.getLa());
			lns511.setBillStatistics(lns512.getLnssatistics());
			trigger(lns511);
		}
		//违约金计提
		lns530.setLoanAgreement(lns512.getLa());
		lns530.setBillStatistics(lns512.getLnssatistics());
		lns530.setTxnseq(lns512.getTxnseq());
		trigger(lns530);
		//罚息落表（ 房抵贷老数据封顶计息 ）已经结清的不进此逻辑  不是C的是老数据 capfalg是是否封顶标志
		if(!lns512.getLa().getContract().getFlag1().contains("C") 
				&& !VarChecker.isEmpty(lns512.getLa().getInterestAgreement().getCapRate())
				&& lns512.getCapFlag()
				&& null != lns500.getLnssatistics()){
			lns520.setRepayDate(ctx.getTranDate());
			lns520.setLnsBillStatistics(lns500.getLnssatistics());
			lns520.setLnsbasicinfo(lns500.getLnsbasicinfo());
			lns520.setLa(lns512.getLa());
			//罚息落表
			trigger(lns520);
		}

	}
	@Override
	protected void special() throws Exception {
		//nothing to do
	}
}

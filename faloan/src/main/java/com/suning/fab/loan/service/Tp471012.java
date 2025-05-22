package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.domain.TblLnsinterface;

import com.suning.fab.loan.utils.DynamicCapUtil;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    SC
	* 
	*@version   V1.0.0
	*
	*@see       汽车租赁还款
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-carRepay")
public class Tp471012 extends ServiceTemplate {

	@Autowired Lns112 lns112; //非标准账号准备
	@Autowired Lns107 lns107; //非标准幂等处理
	@Autowired Lns507 lns507; //非标准计息
	@Autowired Lns503 lns503; //通用转列

	@Autowired Lns229 lns229; //非标准还款本息
	@Autowired Lns504 lns504; //通用呆滞呆账计息
	
	@Autowired Lns212 lns212; //还款渠道
	
	@Autowired Lns506 lns506; //结清状态更新
	@Autowired Lns238 lns238; //还款/退货收入冲销
	@Autowired Lns436 lns436; //汽车还款试算
	@Autowired Lns510 lns510; //租赁摊销
	@Autowired Lns512 lns512; 
	@Autowired Lns513 lns513;
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	
	public Tp471012() {
		needSerSeqNo=true;
	}

	FabAmount	nintAmt = new FabAmount();
	FabAmount	prinAmt = new FabAmount();
	FabAmount	dintAmt = new FabAmount();
	FabAmount	forfeetAmt = new FabAmount();

	String		endFlag = "";
	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		FabAmount	repayAmt =  ctx.getRequestDict("repayAmt") ;
		trigger(lns112);
		
		//汽车租赁试算结清金额
		FabAmount	tranAmt = new FabAmount(repayAmt.getVal()) ;
		if("1".equals(ctx.getRequestDict("settleFlag")))
		{
			Map<String, Object> map471009 = new HashMap<String, Object>();
			map471009.put("acctNo", ctx.getRequestDict("acctNo") );
			map471009.put("endDate", ctx.getTranDate());
			trigger(lns436, "DEFAULT", map471009);
			
				
		}
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("repayDate", ctx.getTranDate());
		trigger(lns507, "map507", param); //计息
		
		//罚息计提
		lns512.setRepayDate(ctx.getTranDate());
		lns512.setLnssatistics(lns507.getLnsBillStatistics());
		lns512.setLnsbasicinfo(lns507.getLnsbasicinfo());
		trigger(lns512);
		
		//罚息落表
		lns513.setRepayDate(ctx.getTranDate());
		lns513.setLnsBillStatistics(lns507.getLnsBillStatistics());
		lns513.setLnsbasicinfo(lns507.getLnsbasicinfo());
		trigger(lns513);
		
		
		trigger(lns503, "map5039", param); //转列
		/*del 2018-06-28 罚息结息时已记呆滞呆账*/
//		trigger(lns504, "map5049", lns507);
//		lns507.getLnsBillStatistics().setBillNo(lns504.getSubNo());
		trigger(lns229, "map229", lns507);
		
		nintAmt = lns229.getNintAmt();
		prinAmt = lns229.getPrinAmt();
		dintAmt = lns229.getDintAmt();
		//还款还到本金落表,动态封顶的封顶值变更
		//DynamicCapUtil.saveCapRecord(lns512.getLa(),ctx ,prinAmt);
		Map<String, Object> map2129 = new HashMap<String, Object>();
		map2129.put("repayAmtMap", lns229.getRepayAmtMap());
		map2129.put("prdId", lns229.getPrdId());
		map2129.put("custType", lns229.getCustType());
		map2129.put("customId", lns112.getCustomId());
		map2129.put("customName", lns112.getCustomName());
		map2129.put("customName", lns112.getCustomName());
		map2129.put("txseqno", lns112.getTxseqno());
		//防止幂等报错被拦截
		trigger(lns121);
		trigger(lns212,"map2129",map2129);
		
		//汽车租赁结清
		if("1".equals(ctx.getRequestDict("settleFlag")))
		{
			if(CalendarUtil.after(ctx.getTranDate(), lns507.getLnsbasicinfo().getContduedate())){
				throw new FabException("LNS176");
			}
			lns238.setCleanPrin(lns436.getCleanTotal());
			lns238.setTranAmt(tranAmt);
			trigger(lns238);
			prinAmt.selfAdd(lns238.getInAmt());
			
			
			//租赁公司结清贷款收益冲销后剩余收益一次摊销
			if( "51240001".equals(ctx.getBrc()))
			{
				lns510.setSettleFlag("1");
				lns510.setChargeAmt(lns238.getInAmt());
				lns510.setCustomid(lns112.getCustomId());
				lns510.setCustomType(lns112.getCustomType());
				trigger(lns510);
			}
		}
		
		trigger(lns506);
		endFlag = lns506.getEndFlag();
		
		Map<String, Object> map471009 = new HashMap<String, Object>();
		map471009.put("dintAmt", dintAmt.getVal());
		map471009.put("nintAmt", nintAmt.getVal());
		map471009.put("prinAmt", prinAmt.getVal());
		map471009.put("endFlag", TblLnsinterface.endflag2AcctFlag2(endFlag, lns229.getIfOverdue()));
		forfeetAmt.selfAdd(prinAmt);
		forfeetAmt.selfAdd(nintAmt);
		forfeetAmt.selfAdd(dintAmt);
		trigger(lns107,  "map471009", map471009);
		
	}
	@Override
	protected void special() throws Exception {
		if (VarChecker.asList( ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			//幂等时  对返回参数赋值
			endFlag = lns112.getEndFlag();
			prinAmt = lns112.getPrinAmt();
			nintAmt = lns112.getNintAmt();
			dintAmt = lns112.getDintAmt();
			forfeetAmt.selfAdd(prinAmt);
			forfeetAmt.selfAdd(nintAmt);
			forfeetAmt.selfAdd(dintAmt);
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}
	public FabAmount getNintAmt() {
		return nintAmt;
	}
	public void setNintAmt(FabAmount nintAmt) {
		this.nintAmt = nintAmt;
	}
	public FabAmount getPrinAmt() {
		return prinAmt;
	}
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}
	public FabAmount getDintAmt() {
		return dintAmt;
	}
	public void setDintAmt(FabAmount dintAmt) {
		this.dintAmt = dintAmt;
	}
	public String getEndFlag() {
		return endFlag;
	}
	public void setEndFlag(String endFlag) {
		this.endFlag = endFlag;
	}
}

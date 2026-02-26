package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       非标准贷款还款
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-leaseRefund")
public class Tp471010 extends ServiceTemplate {

	@Autowired Lns106 lns106; //非标准账号准备
	@Autowired Lns107 lns107; //非标准幂等处理
	@Autowired Lns507 lns507; //非标准计息
	@Autowired Lns501 lns501; //非标准计息
	@Autowired Lns503 lns503; //通用转列

	@Autowired Lns231 lns231; //非标13-2-2还款本息
	@Autowired Lns229 lns229; //非标准还款本息
	@Autowired Lns504 lns504; //通用呆滞呆账计息
	
	@Autowired Lns212 lns212; //还款渠道
	
	@Autowired Lns506 lns506; //结清状态更新
	@Autowired Lns238 lns238; //还款/退货收入冲销
	@Autowired Lns508 lns508; //现金贷资金方记账（资金方还款）
	@Autowired Lns239 lns239; //满足退货条件的冲销摊销收益
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	public Tp471010() {
		needSerSeqNo=true;
	}

	FabAmount	nintAmt = new FabAmount();
	FabAmount	prinAmt = new FabAmount();
	FabAmount	dintAmt = new FabAmount();
	FabAmount	incomeAmt = new FabAmount();

	String		endFlag = "";
	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		Map<String, FabAmount> repayAmtMapAll = new HashMap<String, FabAmount>();
		Map<String, FabAmount> repayAmtMapTmp;
		
		int	i = 0;
		int subNo = 0;
		FabAmount	repayAmt =  ctx.getRequestDict("repayAmt") ;
		FabAmount	nonRepayAmt =  new FabAmount(repayAmt.getVal()) ;
		String		acctNo = ctx.getRequestDict("acctNo") ;


		trigger(lns239);
		trigger(lns106);
		Map<Integer, String> acctMap = lns106.getAcctList();
		int	curTerm = lns106.getCurTerm();
		boolean ifOverdue =false;
		if(curTerm == 0)
		{	
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("repayDate", ctx.getTranDate());
			trigger(lns507, "map5070", param); //计息
			trigger(lns503, "map5040", param); //转列
			trigger(lns504, "map5050", lns507);
			lns507.getLnsBillStatistics().setBillNo(lns504.getSubNo());
			trigger(lns229, "map230", lns507);
			ifOverdue = lns229.getIfOverdue();
			nintAmt = lns229.getNintAmt();
			prinAmt = lns229.getPrinAmt();
			dintAmt = lns229.getDintAmt();
	
			Map<String, Object> map2130 = new HashMap<String, Object>();
			map2130.put("repayAmtMap", lns229.getRepayAmtMap());
			map2130.put("prdId", lns229.getPrdId());
			map2130.put("custType", lns229.getCustType());
			trigger(lns212,"map2130",map2130);
			
			//手机租赁退货
			lns238.setIfOverdue(ifOverdue);
			trigger(lns238);
			ifOverdue = lns238.getIfOverdue();
			incomeAmt = lns238.getInAmt();
			trigger(lns506);
			endFlag = lns506.getEndFlag();
		}
		else
		{
			for (Map.Entry<Integer, String> entry : acctMap.entrySet())
			{
				i++;
				
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("repayDate", ctx.getTranDate());
				param.put("acctNo", entry.getValue());
				trigger(lns501, "map5018", param); //计息
				trigger(lns503, "map5040", param); //转列
				trigger(lns504, "map50501", lns501);
				//还款剩余金额如何
				param.put("repayAmt", nonRepayAmt);
				lns501.getLnsBillStatistics().setBillNo(lns504.getSubNo() + subNo);
				param.put("lnsBillStatistics", lns501.getLnsBillStatistics());
				param.put("subNo", lns504.getSubNo() + subNo);

				lns231.setSubNo(lns504.getSubNo() + subNo);
				trigger(lns231, "map2302", param);
				ifOverdue = lns231.getIfOverdue();
				Map<String, Object> map2130 = new HashMap<String, Object>();
				map2130.put("repayAmtMap", lns231.getRepayAmtMap());
				map2130.put("prdId", lns231.getPrdId());
				map2130.put("custType", lns231.getCustType());
				map2130.put("acctNo", entry.getValue());
				trigger(lns212,"map2130",map2130);

				trigger(lns506, "map5060", param);
				repayAmtMapTmp = lns231.getRepayAmtMap();

				for (Map.Entry<String, FabAmount> entrytmp : repayAmtMapTmp.entrySet()) {
					if(repayAmtMapAll.containsKey(entrytmp.getKey()))
						repayAmtMapAll.get(entrytmp.getKey()).selfAdd(entrytmp.getValue());
					else
						repayAmtMapAll.put(entrytmp.getKey(), entrytmp.getValue());
				}
				
				if(!nonRepayAmt.isPositive())
				{
					 break;
				}	
			}
			
			for (Map.Entry<String, FabAmount> entry : repayAmtMapAll.entrySet()) {
				if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
					prinAmt.selfAdd(entry.getValue());
				if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
					nintAmt.selfAdd(entry.getValue());
				if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
						|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
						|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
					dintAmt.selfAdd(entry.getValue());
			}

			if( "3".equals(lns506.getEndFlag()) && (i == acctMap.size()) )
			{	
				endFlag = "3";
				//更新非标计划表 方便抽数 安洋可能需要备份恢复这张表
				{
					Map<String, Object> nonStatMap = new HashMap<String, Object>();
					nonStatMap.put("loanstat", "CA");
					nonStatMap.put("acctno", acctNo);
					nonStatMap.put("openbrc", ctx.getBrc());
					nonStatMap.put("modifyDate", ctx.getTranDate());	
					
					int count;
					try{
						count = DbAccessUtil.execute("CUSTOMIZE.update_lnsnonstdplan_loanstat", nonStatMap);
					}catch (FabSqlException e){
						throw new FabException(e, "SPS102", "lnsnonstdplan");
					}
					if(1!= count){
						throw new FabException("SPS102", "lnsnonstdplan");
					}
				}		
			}	
			else
				endFlag = "1";
		}
		
		Map<String, Object> map471010 = new HashMap<String, Object>();
		map471010.put("dintAmt", dintAmt.getVal());
		map471010.put("nintAmt", nintAmt.getVal());
		map471010.put("prinAmt", prinAmt.getVal());
		map471010.put("incomeAmt", incomeAmt.getVal());
		map471010.put("endFlag", TblLnsinterface.endflag2AcctFlag2(endFlag, ifOverdue));

		trigger(lns107,  "map471010", map471010);
		//防止幂等报错被拦截
		trigger(lns121);
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		if( !VarChecker.isEmpty(pkgList1) )
		{
			Map<String, Object> map508 = new HashMap<String, Object>();
			map508.put("nintAmt", lns229.getNintAmt());
			map508.put("dintAmt", lns229.getDintAmt());
			trigger(lns508,"DEFAULT",map508);
		}
	}
	@Override
	protected void special() throws Exception {
		if (VarChecker.asList( ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			
			//幂等时  对返回值赋值
			dintAmt = lns106.getDintAmt();
			nintAmt = lns106.getNintAmt();
			prinAmt = lns106.getPrinAmt();
			endFlag = lns106.getEndFlag();
			incomeAmt = lns106.getIncomeAmt();
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
	/**
	 * @return the incomeAmt
	 */
	public FabAmount getIncomeAmt() {
		return incomeAmt;
	}
	/**
	 * @param incomeAmt the incomeAmt to set
	 */
	public void setIncomeAmt(FabAmount incomeAmt) {
		this.incomeAmt = incomeAmt;
	}
}

package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.workunit.*;

import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
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
	*@see       -非标准贷款还款
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-newRepay")
public class Tp471009 extends ServiceTemplate {

	@Autowired Lns106 lns106; //非标准账号准备
	@Autowired Lns107 lns107; //非标准幂等处理
	@Autowired Lns117 lns117;
	@Autowired Lns212 lns212; //还款渠道
	@Autowired Lns229 lns229; //非标准还款本息
	@Autowired Lns231 lns231; //非标13-2-2还款本息
	@Autowired Lns238 lns238; //还款/退货收入冲销
	@Autowired Lns244 lns244;
	@Autowired Lns247 lns247;
	@Autowired Lns248 lns248;
	@Autowired Lns433 lns433; //通讯还款试算
	@Autowired Lns500 lns500;
	@Autowired Lns501 lns501; //非标准计息
	@Autowired Lns502 lns502;
	@Autowired Lns503 lns503; //通用转列
	@Autowired Lns504 lns504; //通用呆滞呆账计息
	@Autowired Lns505 lns505;
	@Autowired Lns506 lns506; //结清状态更新
	@Autowired Lns507 lns507; //非标准计息
	@Autowired Lns508 lns508; //现金贷资金方记账（资金方还款）
	@Autowired Lns510 lns510; //租赁摊销
	@Autowired Lns512 lns512; 
	@Autowired Lns513 lns513;
	@Autowired Lns517 lns517;
	@Autowired
	Lns468 lns468;
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	public Tp471009() {
		needSerSeqNo=true;
	}

	FabAmount	nintAmt = new FabAmount();
	FabAmount	prinAmt = new FabAmount();
	FabAmount	dintAmt = new FabAmount();

	String		endFlag = "";
	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("acctNo").toString(), ctx);
		prepareWriteOff(la);//核销变量预备

		Map<String, FabAmount> repayAmtMapAll = new HashMap<String, FabAmount>();
		Map<String, FabAmount> repayAmtMapTmp;
		
		int	i = 0;
		int subNo = 0;
		FabAmount	repayAmt =  ctx.getRequestDict("repayAmt") ;

		FabAmount	nonRepayAmt =  new FabAmount(repayAmt.getVal()) ;
		String		acctNo = ctx.getRequestDict("acctNo") ;
		trigger(lns106); //登记幂等登记簿
		Map<Integer, String> acctMap = lns106.getAcctList();
		int	curTerm = lns106.getCurTerm();
		
		//手机租赁试算结清金额
		FabAmount	tranAmt = new FabAmount(repayAmt.getVal()) ;
		if("1".equals(ctx.getRequestDict("settleFlag")))
		{
			Map<String, Object> map471009 = new HashMap<String, Object>();
			map471009.put("acctNo", ctx.getRequestDict("acctNo") );
			map471009.put("endDate", ctx.getTranDate());
			trigger(lns433, "DEFAULT", map471009);
			
				
		}
		//是否逾期还款
		boolean ifOverdue = false; //add at 2019-03-25
		if(curTerm == 0)
		{

			//核销的贷款预处理   2018-12-20
			trigger(lns517);

			Map<String, Object> param = new HashMap<String, Object>();
			param.put("repayDate", ctx.getTranDate());
			trigger(lns507, "map507", param); //计息
			
			//罚息计提
			lns512.setRepayDate(ctx.getTranDate());
			lns512.setLnssatistics(lns507.getLnsBillStatistics());
			lns512.setLnsbasicinfo(lns517.getLnsbasicinfo());
			trigger(lns512);
			
			//罚息落表
			lns513.setRepayDate(ctx.getTranDate());
			lns513.setLnsBillStatistics(lns507.getLnsBillStatistics());
			lns513.setLnsbasicinfo(lns517.getLnsbasicinfo());
			trigger(lns513);


			trigger(lns503, "map5039", param); //转列
			
			/*del 2018-06-28 罚息结息时已记呆滞呆账*/
			trigger(lns504, "map5049", lns507);
			lns507.getLnsBillStatistics().setBillNo(lns504.getSubNo());

			trigger(lns505); //更新状态  2018-12-20

			/** 非标还款 增加利息减免功能  2018-12-20 start**/
			if( ctx.getRequestDict("reduceIntAmt") != null )
			{
				if( Double.valueOf(ctx.getRequestDict("reduceIntAmt").toString()) > 0.00 )
				{
					trigger(lns247, "map221", lns501);
					ifOverdue = lns247.getIfOverdue();
				}
			}
			if(ctx.getRequestDict("reduceFintAmt") != null)
			{
				if(Double.valueOf(ctx.getRequestDict("reduceFintAmt").toString()) > 0.00 ) {
                    lns248.setIfOverdue(ifOverdue);
                    trigger(lns248);
                    ifOverdue = lns248.getIfOverdue();
				}
			}
			/** end**/
			
			//
			Map<String, Object> map2129 = new HashMap<String, Object>();
			if( VarChecker.isEmpty( ctx.getRequestDict("specifiedType") ))
			{
				lns229.setIfOverdue(ifOverdue);
				trigger(lns229, "map229", lns507);
				
				ifOverdue = lns229.getIfOverdue();
				nintAmt = lns229.getNintAmt();
				prinAmt = lns229.getPrinAmt();
				dintAmt = lns229.getDintAmt();
				
				map2129.put("repayAmtMap", lns229.getRepayAmtMap());
				map2129.put("prdId", lns229.getPrdId());
				map2129.put("custType", lns229.getCustType());

			}
			else
			{
				lns244.setIfOverdue(ifOverdue);
				trigger(lns244, "map244", lns507);
				
				ifOverdue = lns244.getIfOverdue();
				nintAmt = lns244.getNintAmt();
				prinAmt = lns244.getPrinAmt();
				dintAmt = lns244.getDintAmt();
				
				map2129.put("repayAmtMap", lns244.getRepayAmtMap());
				map2129.put("prdId", lns244.getPrdId());
				map2129.put("custType", lns244.getCustType());
				map2129.put("txseqno", lns244.getTxseqno());

			}
			trigger(lns212,"map2129",map2129);

			//还款还到本金落表,动态封顶的封顶值变更
			//DynamicCapUtil.saveCapRecord(lns512.getLa(),ctx ,prinAmt);

			//手机租赁结清
			if("1".equals(ctx.getRequestDict("settleFlag")))
			{
				if(CalendarUtil.after(ctx.getTranDate(), lns517.getLnsbasicinfo().getContduedate())){
					throw new FabException("LNS176");
				}
				lns238.setCleanPrin(lns433.getCleanPrin());
				lns238.setTranAmt(tranAmt);
				trigger(lns238);
				prinAmt.selfAdd(lns238.getInAmt());
				
				
				//租赁公司结清贷款收益冲销后剩余收益一次摊销
				if( "51240000".equals(ctx.getBrc()))
				{
					lns510.setSettleFlag("1");
					lns510.setChargeAmt(lns238.getInAmt());
					trigger(lns510);
				}
			}
			
			trigger(lns506);
			/** 核销的贷款 需要计提利息 如果没有结清 通知核销动态表的金额 2018-12-20 start**/
			if(lns517.getIfCertification()){
				if(new FabAmount(lns517.getLnsbasicinfo().getDeductionamt()).isPositive()){
					//扣息放款 进行摊销
					lns502.setRepayDate(ctx.getTranDate());
					trigger(lns502);
				}else{
					lns500.setLnsbasicinfo(lns517.getLnsbasicinfo());
					lns500.setRepayDate(ctx.getTranDate());
					trigger(lns500);
				}
				//如果没有结清贷款，  核销后需要将已经结息的金额 通知核销
				if(!"3".equals(lns506.getEndFlag())){
					lns117.setLa(la);
					trigger(lns117);
				}
			}
			/** end **/
			endFlag = lns506.getEndFlag();
		}
		else
		{
			/** 利息罚息减免 13-2-2 的只能事前利息减免 2018-12-20   strat**/
			if( ctx.getRequestDict("reduceIntAmt") != null )
			{
				if( Double.valueOf(ctx.getRequestDict("reduceIntAmt").toString()) > 0.00 )
					throw new FabException("LNS127",ctx.getRequestDict("acctNo"));
			}
			if(ctx.getRequestDict("reduceFintAmt") != null)
			{
				if(Double.valueOf(ctx.getRequestDict("reduceFintAmt").toString()) > 0.00 )
					throw new FabException("LNS127",ctx.getRequestDict("acctNo"));
			}
			/** end **/
			for (Map.Entry<Integer, String> entry : acctMap.entrySet())
			{
				i++;
				
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("repayDate", ctx.getTranDate());
				param.put("acctNo", entry.getValue());

//				lns517.setAcctNo(entry.getValue());
//				trigger(lns517);
//
//				//判断核销的贷款
//				if(lns517.getIfCertification()){
//					if(new FabAmount(lns517.getLnsbasicinfo().getDeductionamt()).isPositive()){
//						//扣息放款 进行摊销
//						lns502.setAcctNo(entry.getValue());
//						lns502.setRepayDate(ctx.getTranDate());
//						trigger(lns502);
//					}else{
//						lns500.setLnsbasicinfo(lns517.getLnsbasicinfo());
//						lns500.setAcctNo(entry.getValue());
//						lns500.setRepayDate(ctx.getTranDate());
//						trigger(lns500);
//					}
//				}

				trigger(lns501, "map5017", param); //计息
				
//				//罚息计提
//				lns512.setRepayDate(ctx.getTranDate());
//				lns512.setLnssatistics(lns501.getLnsBillStatistics());
//				lns512.setLnsbasicinfo(lns501.getLnsbasicinfo());
//				trigger(lns512);
//
//				//罚息落表
//				lns513.setRepayDate(ctx.getTranDate());
//				lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
//				lns513.setLnsbasicinfo(lns501.getLnsbasicinfo());
//				trigger(lns513);
				
				trigger(lns503, "map5039", param); //转列
				trigger(lns504, "map50491", lns501);
				//还款剩余金额如何
				param.put("repayAmt", nonRepayAmt);
				lns501.getLnsBillStatistics().setBillNo(lns504.getSubNo() + subNo);
				param.put("lnsBillStatistics", lns501.getLnsBillStatistics());
				param.put("subNo", lns504.getSubNo() + subNo);

				lns231.setSubNo(lns504.getSubNo() + subNo);
				param.put("ifOverdue", ifOverdue);
				trigger(lns231, "map2291", param);
				ifOverdue = lns231.getIfOverdue();
				Map<String, Object> map2129 = new HashMap<String, Object>();
				map2129.put("repayAmtMap", lns231.getRepayAmtMap());
				map2129.put("prdId", lns231.getPrdId());
				map2129.put("custType", lns231.getCustType());
				map2129.put("acctNo", entry.getValue());
				trigger(lns212,"map2129",map2129);

				trigger(lns506, "map506", param);
				
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
		
		Map<String, Object> map471009 = new HashMap<String, Object>();
		map471009.put("dintAmt", dintAmt.getVal());
		map471009.put("nintAmt", nintAmt.getVal());
		map471009.put("prinAmt", prinAmt.getVal());
		map471009.put("endFlag", TblLnsinterface.endflag2AcctFlag2(endFlag, ifOverdue));

		trigger(lns107,  "map471009", map471009);
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

	/**
	 * 核销全局变量预备
	 *
	 * @param la
	 */
	public void prepareWriteOff(LoanAgreement la) throws Exception {
		//当账户是核销状态时
		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION.equals(la.getContract().getLoanStat())) {
			//进行实时迁移
			if (!la.getContract().getFlag1().contains(ConstantDeclare.FLAG1.H)) {
				trigger(lns468);
				//进行实时赋值
				ThreadLocalUtil.set(ConstantDeclare.BASICINFOEXKEY.HXRQ, lns468.getWriteOffDate());
			} else {
				ThreadLocalUtil.set(ConstantDeclare.BASICINFOEXKEY.HXRQ, la.getBasicExtension().getWriteOffDate());
			}
			//将账户的状态放入上下文中  只有还款服务才需要存放此字段
			ThreadLocalUtil.set("loanStat", la.getContract().getLoanStat());
			//核销之后是否转罚息
			ThreadLocalUtil.set(ConstantDeclare.PARACONFIG.IGNOREOFFDINT, la.getInterestAgreement().getIgnoreOffDint());

		}
		else if(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(la.getContract().getLoanStat())){
			//将账户的状态放入上下文中  只有还款服务才需要存放此字段
			ThreadLocalUtil.set("loanStat", la.getContract().getLoanStat());
		}
	}

	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			//幂等时  对返回值赋值
			dintAmt = lns106.getDintAmt();
			nintAmt = lns106.getNintAmt();
			prinAmt = lns106.getPrinAmt();
			endFlag = lns106.getEndFlag();
			
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

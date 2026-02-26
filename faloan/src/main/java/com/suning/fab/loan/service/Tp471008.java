package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
	*@author    TT.Y
	* 
	*@version   V1.0.0
	*
	*@see       -贷款指定还款
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-specialRepay")
public class Tp471008 extends ServiceTemplate {

	@Autowired Lns501 lns501;
	
	@Autowired Lns503 lns503;
	
	@Autowired Lns222 lns222;
	
	@Autowired Lns212 lns212;
		
	@Autowired Lns504 lns504;
	
	@Autowired Lns506 lns506;
	
	@Autowired Lns421 lns421;
	
	@Autowired Lns508 lns508;
	
	@Autowired Lns509 lns509;	
	@Autowired Lns512 lns512;	
	@Autowired Lns513 lns513;
	
	@Autowired Lns247 lns247; 
	@Autowired Lns248 lns248; 
	@Autowired Lns505 lns505;
	@Autowired Lns500 lns500;
	@Autowired Lns517 lns517;
	@Autowired Lns502 lns502;
	@Autowired
	Lns121 lns121;//考核数据校验 入库
	@Autowired
	Lns468 lns468;

	@Autowired
    Lns117 lns117;
	public Tp471008() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();
//		String key = ctx.getRequestDict("acctNo").toString() + ctx.getRequestDict("repayAmt").toString();
//		if( false == (AcctStatistics.execute(key,ctx.getRequestDict("serialNo").toString())))
//		{
//			throw new FabException("LNS054");
//		}
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("acctNo").toString(), ctx);
		prepareWriteOff(la);//核销变量预备
		//核销的贷款还款预处理 2018-12-20
		trigger(lns517);

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("repayDate", ctx.getTranDate());
		
		trigger(lns421);
		lns501.setLnsbasicinfo(lns517.getLnsbasicinfo());
		trigger(lns501, "map501s", param); //贷款结息
		
		//罚息计提
		lns512.setRepayDate(ctx.getTranDate());
		lns512.setLnssatistics(lns501.getLnsBillStatistics());
		lns512.setLnsbasicinfo(lns501.getLnsbasicinfo());
		trigger(lns512);
		
		//罚息落表
		lns513.setRepayDate(ctx.getTranDate());
		lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
		lns513.setLnsbasicinfo(lns501.getLnsbasicinfo());
		trigger(lns513);
				
				
		trigger(lns503, "map503s", param); //贷款转列
		trigger(lns505); //更新状态
//		FabAmount repayAmt = ctx.getRequestDict("repayAmt"); //还款金额
		
		/*del 2018-06-28 罚息结息时已记呆滞呆账*/
		//有还款金额的话做呆滞结息 纯退款没有还款金额不做呆滞结息
//		if(repayAmt!= null && repayAmt.isPositive())
//		{	
//			trigger(lns504, "map504s", lns501); //呆滞结息
//			lns501.getLnsBillStatistics().setBillNo(lns504.getSubNo());
//		}
		//是否逾期还款
		boolean ifOverdue = false; //add at 2019-03-25
		//还款加利息减免功能
		if( ctx.getRequestDict("reduceIntAmt") != null )
		{
			if( Double.valueOf(ctx.getRequestDict("reduceIntAmt").toString()) > 0.00 )
			{
				trigger(lns247, "map221", lns501);
				ifOverdue = lns247.getIfOverdue();
				lns501.getLnsBillStatistics().setBillNo(lns247.getSubNo() + 1);
			}
		}
		if(ctx.getRequestDict("reduceFintAmt") != null)
		{
			if(Double.valueOf(ctx.getRequestDict("reduceFintAmt").toString()) > 0.00 ){
				lns248.setIfOverdue(ifOverdue);
				trigger(lns248);
				ifOverdue = lns248.getIfOverdue();
			}
		}
		
		lns222.setIfOverdue(ifOverdue);
		trigger(lns222, "map222", lns501); //指定还款主程序
		//防止幂等报错被拦截
		trigger(lns121);
		Map<String, Object> map212s = new HashMap<String, Object>();
		map212s.put("repayAmtMap", lns222.getRepayAmtMap());
		map212s.put("txseqno", lns222.getTxseqno());
		lns212.setLoanAgreement(lns222.getLoanAgreement());
		trigger(lns212,"map212s",map212s);
		trigger(lns506);

		/** 核销的贷款 需要计提利息 如果没有结清 通知核销动态表的金额 2018-12-20 start**/
		if(lns517.getIfCertification()){
			if(new FabAmount(lns517.getLnsbasicinfo().getDeductionamt()).isPositive()){
				//扣息放款 进行摊销
				lns502.setRepayDate(ctx.getTranDate());
				trigger(lns502);
			}else{
				if("3".equals(lns506.getEndFlag()))
					lns517.getLnsbasicinfo().setLoanstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
				lns500.setLnsbasicinfo(lns517.getLnsbasicinfo());
				lns500.setRepayDate(ctx.getTranDate());
				trigger(lns500);
			}

			//如果没有结清贷款，  核销后需要将已经结息的金额 通知核销
			if(!"3".equals(lns506.getEndFlag())){
				lns117.setLa(lns222.getLoanAgreement());
				trigger(lns117);
			}
		}
		/** end **/
		lns222.setIfOverdue(ifOverdue);
		lns222.setEndFlag(lns506.getEndFlag());//非幂等的时候取lns506的endflag
		
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		if( !VarChecker.isEmpty(pkgList1) )
		{
			Map<String, Object> map508 = new HashMap<String, Object>();
			map508.put("prinAmt", lns222.getPrinAmt());
			map508.put("nintAmt", lns222.getNintAmt());
			map508.put("dintAmt", lns222.getDintAmt());
			if ("01".equals(ctx.getRequestDict("investeeRepayFlg"))) {
                lns508.getInvesteePrinPlatform().selfAdd(lns222.getPrinAmt());
                lns508.getInvesteeIntPlatform().selfAdd(lns222.getNintAmt());
                lns508.getInvesteeDintPlatform().selfAdd(lns222.getDintAmt());
            }
			trigger(lns508,"DEFAULT",map508);
			//多资金方校验赋值本息罚20210623
            if ("01".equals(ctx.getRequestDict("investeeRepayFlg")) || pkgList1.size() > 1) {
                lns509.getInvesteePrinPlatform().selfAdd(lns222.getPrinAmt());
                lns509.getInvesteeIntPlatform().selfAdd(lns222.getNintAmt());
                lns509.getInvesteeDintPlatform().selfAdd(lns222.getDintAmt());
            }
			//为校验，获多资金方获取产品代码 20210623 还款清分多资金方需求
            lns509.setProductCode(lns222.getLoanAgreement().getPrdId());
			trigger(lns509,"DEFAULT",map508);
		}
		
		if("3".equals(lns506.getEndFlag()) && ConstantDeclare.REPAYWAY.isEqualInterest(lns517.getLnsbasicinfo().getRepayway())){
			lns500.setLnsbasicinfo(lns517.getLnsbasicinfo());
			lns500.setRepayDate(ctx.getTranDate());
			trigger(lns500);
		}
		ThreadLocalUtil.clean();
	}
	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}

	/**
	 * 核销全局变量预备
	 * @param la
	 */
	public void prepareWriteOff(LoanAgreement la)throws Exception{
		//当账户是核销状态时
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION.equals(la.getContract().getLoanStat())){
			//进行实时迁移
			if(!la.getContract().getFlag1().contains(ConstantDeclare.FLAG1.H)){
				trigger(lns468);
				//进行实时赋值
				ThreadLocalUtil.set(ConstantDeclare.BASICINFOEXKEY.HXRQ,lns468.getWriteOffDate());
			}else{
				ThreadLocalUtil.set(ConstantDeclare.BASICINFOEXKEY.HXRQ,la.getBasicExtension().getWriteOffDate());
			}
			//将账户的状态放入上下文中  只有还款服务才需要存放此字段
			ThreadLocalUtil.set("loanStat",la.getContract().getLoanStat());
			//核销之后是否转罚息
			ThreadLocalUtil.set(ConstantDeclare.PARACONFIG.IGNOREOFFDINT,la.getInterestAgreement().getIgnoreOffDint());

		}
		//当账户是债转状态时
		else if(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(la.getContract().getLoanStat())){
			//将账户的状态放入上下文中  只有还款服务才需要存放此字段
			ThreadLocalUtil.set("loanStat",la.getContract().getLoanStat());
		}
	}
}

package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabRuntimeException;
import com.suning.fab.tup4j.base.ServiceTemplate;
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
	*@see       房抵贷开户
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-createHouseAcct")
public class Tp473007 extends ServiceTemplate {

	@Autowired Lns101 lns101;
	
	@Autowired Lns118 lns118;
	
	@Autowired Lns102 lns102;
	
	@Autowired Lns201 lns201;
	
	@Autowired Lns202 lns202;
	
	@Autowired Lns203 lns203;
	
	@Autowired Lns204 lns204;
	
	@Autowired Lns241 lns241;
	
	@Autowired Lns501 lns501;

	@Autowired Lns503 lns503;

	@Autowired Lns512 lns512; 
	
	@Autowired Lns520 lns520; 
	
	@Autowired Lns521 lns521;	//开户校验
	
	@Autowired Lns120 lns120;	//代偿幂等拓展表

	@Autowired Lns253 lns253;
	//罚息落表
	@Autowired Lns513 lns513;
	@Autowired Lns121 lns121;//考核数据校验 入库
	@Autowired Lns254 lns254;//房抵贷融担代偿开户汇总费用
	
	public Tp473007() {
		needSerSeqNo=true;
	}

	@Override
	protected void run() throws Exception {
		ThreadLocalUtil.clean();

		//载入协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreement(ctx.getRequestDict("productCode").toString());
		//开户校验
		lns521.setLa(la);
		trigger(lns521);
		if(VarChecker.isEmpty( ctx.getRequestDict("startIntDate") ) || ctx.getRequestDict("startIntDate").toString().isEmpty())
		{
			la.getContract().setStartIntDate(ctx.getTranDate());
			la.getContract().setContractStartDate(ctx.getTranDate());
		}
		else
		{
			la.getContract().setStartIntDate(ctx.getRequestDict("startIntDate").toString());
			la.getContract().setContractStartDate(ctx.getRequestDict("startIntDate").toString());
		}
		
		//子机构号和年利率为空报错
		PubDictUtils.checkReqNull(ctx,"childBrc");

		lns101.setLoanAgreement(la);
		lns118.setLoanAgreement(la);

		//保费不限制 宽限期 复利利率 和 倒起息呆滞呆账  yearFeeRate
		//新增2512626和2412626
		//只有房抵贷才走这个判断
		if("2412615".equals(ctx.getRequestDict("productCode").toString())) {
//			//房抵贷开户暂时不存在宽限期和复利利率
//			if (!VarChecker.isEmpty(ctx.getRequestDict("graceDays")) && 
//				!ctx.getRequestDict("graceDays").toString().equals("0")) {
//				throw new FabException("LNS138");
//			}

			if ( !VarChecker.isEmpty(ctx.getRequestDict("compoundRate")) &&
				 !new FabAmount(Double.valueOf(ctx.getRequestDict("compoundRate").toString())).isZero()) {
				throw new FabException("LNS139");
			}
			//注释了这段  风险管理费可以不传yearFeeRate了  需要根据flag判断
//			if(VarChecker.isEmpty(ctx.getRequestDict("yearFeeRate"))){
//				throw new FabException("LNS055","yearFeeRate");
//			}
		}
		//倒起息呆滞呆账，可以都限制一下
		if ( !"true".equals(la.getInterestAgreement().getIsCompensatory()) && //代偿产品支持呆滞呆账到起息开户
			 !VarChecker.isEmpty(ctx.getRequestDict("startIntDate")) &&
			 CalendarUtil.actualDaysBetween(ctx.getRequestDict("startIntDate").toString(), ctx.getTranDate()) > 90) {
			throw new FabException("LNS150");
		}
		trigger(lns101,"map101");
		//防止幂等报错被拦截
		trigger(lns121);
		trigger(lns118);
		//代偿 （2512617去掉）
//		if( VarChecker.asList("2412631","2412632","2412633","2412623").contains(ctx.getRequestDict("productCode").toString()) )
		if( "true".equals(la.getInterestAgreement().getIsCompensatory()) )
		{	
			if(!VarChecker.isEmpty(ctx.getRequestDict("exAcctno"))
					&& !VarChecker.isEmpty(ctx.getRequestDict("exPeriod"))
					&& !VarChecker.isEmpty(ctx.getRequestDict("exinvesteeId"))){
				trigger(lns120);
			}
			
		}
		trigger(lns102);
		trigger(lns201);

		//整笔代偿后使用转换费用开户
		if( !VarChecker.isEmpty(ctx.getRequestDict("switchFee") ) )
		{
			lns254.setLoanAgreement(la);
			trigger(lns254);
		}
		else{
			lns241.setLoanAgreement(la);
			trigger(lns241);
		}

		trigger(lns202);
		/**
		 * 放款渠道:channelType	0:多渠道
		 */
		if("0".equals(ctx.getRequestDict("channelType").toString())){
			trigger(lns253);
		}else{
			trigger(lns203);
		}
		trigger(lns204);
		//lns241.setLoanAgreement(la);
		

		
		if( !VarChecker.isEmpty(ctx.getRequestDict("startIntDate")) && 
			CalendarUtil.after(ctx.getTranDate(), ctx.getRequestDict("startIntDate").toString())){
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("repayDate", ctx.getTranDate());
            param.put("acctNo", ctx.getRequestDict("receiptNo").toString());

            //结息
			trigger(lns501, "map501", param);
			//增加转列
			trigger(lns503, "DEFAULT", param);

			//罚息计提
			lns512.setAcctNo(ctx.getRequestDict("receiptNo").toString());
			lns512.setRepayDate(ctx.getTranDate());
			lns512.setLnssatistics(lns501.getLnsBillStatistics());
			lns512.setLnsbasicinfo(lns501.getLnsbasicinfo());
			trigger(lns512);
			
			//罚息落表

			//罚息落表
			if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())){
				//罚息落表
				lns520.setAcctNo(ctx.getRequestDict("receiptNo").toString());
				lns520.setRepayDate(ctx.getTranDate());
				lns520.setLnsBillStatistics(lns501.getLnsBillStatistics());
				lns520.setLnsbasicinfo(lns501.getLnsbasicinfo());
				lns520.setLa(lns512.getLa());
				trigger(lns520);
			}else{
				lns513.setAcctNo(ctx.getRequestDict("receiptNo").toString());
				lns513.setRepayDate(ctx.getTranDate());
				lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
				lns513.setLnsbasicinfo(lns501.getLnsbasicinfo());
				trigger(lns513);
			}


		}
		
	}



	@Override
	protected void special() throws Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
		} 	
	}
	
	@Override	
	protected void normalBeforeRun(){
		if(!VarChecker.isValidConstOption(ctx.getRequestDict("repayWay").toString(), ConstantDeclare.REPAYWAY.class))
		{
			throw new FabRuntimeException("LNS051");
		}
		
		if(VarChecker.isEmpty( ctx.getRequestDict("loanType").toString()))
			throw new FabRuntimeException("LNS052");
	}
}

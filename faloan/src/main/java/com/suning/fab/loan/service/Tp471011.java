package com.suning.fab.loan.service;

import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.loan.utils.ThreadLocalUtil;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.Amount;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
*@author    
* 
*@version   V1.0.0
*
*      费用还款
*
*@param     
*
*@return    
*
*@exception 
*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-houseRepay")
public class Tp471011 extends ServiceTemplate{

	@Autowired
	Lns121 lns121;//考核数据校验 入库
	@Autowired Lns242 lns242;
	@Autowired Lns511 lns511;
	@Autowired
	Lns531 lns531;
	@Autowired
	Lns501 lns501;
	@Autowired
	Lns001 lns001;
	@Autowired
	Lns532 lns532;
	@Autowired
	Lns530 lns530;
	@Autowired
	Lns260 lns260;
	@Autowired
	Lns533 lns533;
	@Autowired
	Lns244 lns244;
	@Autowired
	Lns506 lns506;
	@Autowired
	Lns212 lns212;
	@Autowired
	Lns508 lns508;
	@Autowired
	Lns400 lns400;
	@Autowired
	Lns434 lns434;
	@Autowired
	Lns534 lns534;
	@Autowired
	Lns470 lns470;

	@Override
	protected void run() throws FabException, Exception {
		ThreadLocalUtil.clean();

		String  endFlag = "N";
		trigger(lns001);
		if(LoanFeeUtils.isPremium(lns001.getLoanAgreement().getPrdId())){
			lns242.setLoanAgreement(lns001.getLoanAgreement());
			lns242.setBillStatistics(lns001.getLnsBillStatistics());
			trigger(lns242);
			endFlag = lns242.getEndFlag();
			
			//260统一返回，242幂等无法返回
			lns260.setEndFlag(endFlag);
			lns260.setForfeetAmt(lns242.getForfeetAmt());
		}else{
            //农商行结清标志限制
			if("2512634".equals(lns001.getLoanAgreement().getPrdId())&&!Arrays.asList("1","2").contains(ctx.getRequestDict("settleFlag"))){
                throw new FabException("LNS233");
			}
			//农商行 融担代偿 结清标志传1
			if("2512634".equals(lns001.getLoanAgreement().getPrdId())&&ConstantDeclare.COMPENSATEFLAG.OTHERPAY.equals(ctx.getRequestDict("compensateFlag"))
					&&!"1".equals(ctx.getRequestDict("settleFlag"))){
				throw new FabException("LNS235");
			}
			lns532.setLoanAgreement(lns001.getLoanAgreement());
			lns532.setLnsBillStatistics(lns001.getLnsBillStatistics());
			trigger(lns532);
			lns501.setLa(lns001.getLoanAgreement());
			lns501.setLnsBillStatistics(lns001.getLnsBillStatistics());
			lns501.setRepayDate(ctx.getTranDate());
			trigger(lns501);
			//违约金计提
			//违约金计提
			lns530.setLoanAgreement(lns001.getLoanAgreement());
			lns530.setBillStatistics(lns001.getLnsBillStatistics());
            lns530.setRepayDate(ctx.getTranDate());
			trigger(lns530);
			lns531.setLoanAgreement(lns001.getLoanAgreement());
			lns531.setLnsBillStatistics(lns001.getLnsBillStatistics());
			//违约金结息
			trigger(lns531);
			lns533.setDamageReduceAmts(lns532.getDamageReduceAmts());
            lns533.setTermReduceAmts(lns532.getTermReduceAmts());
            //到期结清不需要减免费用，农商行新增
            if("2".equals(ctx.getRequestDict("settleFlag"))) {
                lns533.setDayReduceAmts(new HashMap());
            }else{
                lns533.setDayReduceAmts(lns532.getDayReduceAmts());
            }
			trigger(lns533);
			lns242.setAdjustFee(lns533.getAdjustFee());
			
			//消金-催收-预减免优化
			if( !VarChecker.isEmpty(ctx.getRequestDict("pkgList2")) ){
				trigger(lns470);
				lns260.setReduceRepayList(lns470.getRepayList());
			}
			//费用结息时，零金额的结息明细需要展示，负责无法统计减免费用总金额
			if(lns501.getBillFeeRepay().size()>0){
				lns260.setBillFeeRepay(lns501.getBillFeeRepay());
			}
			lns260.setAdjustDamage(lns533.getAdjustDamage());
			lns260.setAdjustFee(lns533.getAdjustFee());
			
			trigger(lns260);
			endFlag = lns260.getEndFlag();
			//上海农商行指定还费接口 增加还本金  根据是否传资金方明细判断新老业务  代偿时忽略
            if("2512634".equals(lns001.getLoanAgreement().getPrdId())&&(!VarChecker.isEmpty(ctx.getRequestDict("pkgList1"))||ConstantDeclare.COMPENSATEFLAG.OTHERPAY.equals(ctx.getRequestDict("compensateFlag")))){
				//还款本金 非标指定还款 还款本金
				lns244.setIfOverdue(false);
				lns244.setBillStatistics(lns001.getLnsBillStatistics());
				lns244.setSpecifiedType("3");
				lns400.setEndDate(lns001.getRepayDate());
				//获取结清本清
				trigger(lns400);
				//获取到期本金
				if(lns400.getLa()!=null&&!lns400.getLa().getFeeAgreement().isEmpty()){
					//非标费用查询
					lns434.setLnsBillStatistics(lns400.getLnsBillStatistics());
					lns434.setCtx(lns400.getCtx());
					lns434.setLoanAgreement(lns400.getLa());
					lns434.setEndDate(lns001.getRepayDate());
					trigger(lns434);
				}
				if("2".equals(ctx.getRequestDict("settleFlag"))){
					//到期结清 传送到期本金
					((Amount)ctx.getRequestDict("repayAmt")).selfAdd(lns434.getOverPrin());
				}else if ("1".equals(ctx.getRequestDict("settleFlag"))){
					//全部结清 传送剩余本金
					((Amount)ctx.getRequestDict("repayAmt")).selfAdd(lns400.getCleanPrin());
				}
				trigger(lns244);
				//更新贷款结清标志
				trigger(lns506);
				lns212.setRepayAmtMap(lns244.getRepayAmtMap());
				lns212.setTxseqno(lns244.getTxseqno());
				//还款渠道
				trigger(lns212);
				//还费接口增加资金方
				trigger(lns508);
			}
		}
		//贴费时  抛费用结转事件
		if(lns260.getCouponFeeAmt().isPositive()){
			lns534.getFeeAmtPlatform().selfAdd(lns260.getForfeetAmt());//平台
			lns534.getFeeAmtInverest().selfAdd(lns260.getForfeetAmt()).selfAdd(lns260.getCouponFeeAmt());//资金方
			trigger(lns534);
		}
		//防止幂等报错被拦截
		trigger(lns121);
		if(endFlag.equals("Y"))
		{
			lns511.setRepayDate(ctx.getTranDate());
			trigger(lns511);
		}
		//农商行 违约金合并费用
		if(Arrays.asList("2412638","2412641","2412640").contains(lns001.getLoanAgreement().getPrdId())){
			lns260.getForfeetAmt().selfAdd(lns260.getDamagesAmt());
			lns260.setDamagesAmt(new FabAmount(0.00));
			lns242.getAdjustFee().selfAdd(lns533.getAdjustDamage());
			lns533.setAdjustDamage(new FabAmount(0.00));
		}

		//更新贷款结清标志
		if(Arrays.asList(GlobalScmConfUtil.getProperty("priorityFee","DEFAULT").split(",")).contains(lns001.getLoanAgreement().getPrdId())){
			trigger(lns506);
		}
	}

	@Override
	protected void special() throws FabException, Exception {
		if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
				.contains(ctx.getRspCode())) {
			ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
			
			//保费的幂等返回已还金额
			if(LoanFeeUtils.isPremium(lns001.getLoanAgreement().getPrdId())){
				lns260.setForfeetAmt(lns242.getForfeetAmt());
				lns260.setEndFlag(lns242.getEndFlag());
			}
		} 	
	}

}

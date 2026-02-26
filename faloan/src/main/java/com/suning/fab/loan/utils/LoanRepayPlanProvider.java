package com.suning.fab.loan.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	HY
 *
 * @version V1.1.1
 *
 * @see 	
 *
 * @param
 *
 * @return
 *
 * @exception
 */


public class LoanRepayPlanProvider {
	
	/**
     * 登记还款计划
     * 开户放款、结息、提前还款还到本金时会调用该方法生成未来一期（提前结清生成多期）还款计划并落表
     * 
     * @param ctx  					公共信息
     * @param acctNo 				帐号
     * @param repayChannel 			还款渠道
     * 								OPEN 	-- 开户日
     * 								SETTLE 	-- 结息日
     * 								REPAY 	-- 提前还款
     * 								ADVANCE	-- 提前还款且全部结清
     * 
     * @return  
     * @since  1.1.1 
     */
	
	public static void interestRepayPlan(TranCtx ctx ,String acctNo, String repayChannel) throws FabException
	{		
		//根据账号生成账单
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(),ctx);
				

		interestRepayPlan(ctx, repayChannel, la, lnsBillStatistics);


	}

	public  static void interestRepayPlan(TranCtx ctx, String repayChannel, LoanAgreement la, LnsBillStatistics lnsBillStatistics) throws FabException {

		//获取主文件信息
		Map<String, Object> bparam = new HashMap<String, Object>();
		//帐号
		bparam.put("acctno", la.getContract().getReceiptNo());
		//机构
		bparam.put("openbrc", ctx.getBrc());
		//主文件结构
		TblLnsbasicinfo lnsbasicinfo;
		try {
			//按帐号、机构取主文件信息
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", bparam, TblLnsbasicinfo.class);
		} catch (FabSqlException e) {
			//异常
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			//无记录
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		//定义账单list
		List<LnsBill> listBill = new ArrayList<LnsBill>();

		//历史账单
		listBill.addAll(lnsBillStatistics.getHisBillList());
		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
		listBill.addAll(lnsBillStatistics.getBillInfoList());
		//未来期账单：从还款日到合同到期日之间的本金和利息账单
		listBill.addAll(lnsBillStatistics.getFutureBillInfoList());

		//用于记录上个账单的key
		String lastPeriod = "";

		//还款计划表结构
		TblLnsrpyplan rpyPlan = new TblLnsrpyplan();
		//map用于存储每期还款计划
		Map<String,TblLnsrpyplan> rfMap = new LinkedHashMap<String,TblLnsrpyplan>();

		//遍历账单list
		for (LnsBill lnsBill:listBill)
		{
			boolean zeroNint = false;
			if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())
					&& lnsBill.getBillAmt().isZero()){
				zeroNint = true;
			}
			//非本金、非利息账单不处理，已结清账单不处理
//			if( !ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()) &&
//				!ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType()) &&
//				!ConstantDeclare.BILLTYPE.BILLTYPE_RMFE.equals(lnsBill.getBillType()) &&
//				!ConstantDeclare.BILLTYPE.BILLTYPE_SQFE.equals(lnsBill.getBillType()) &&
//				!ConstantDeclare.BILLTYPE.BILLTYPE_ISFE.equals(lnsBill.getBillType()) ||
//				(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleFlag()) && !zeroNint && !"DELAY".equals(repayChannel)))
//				continue;
			
			//新增费用类型 2020-06-09
			if( !ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()) &&
				!ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType()) &&
				!LoanFeeUtils.isFeeType(lnsBill.getBillType()) ||
				(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleFlag()) && !zeroNint && !"DELAY".equals(repayChannel)))
				continue;

			//本金利息开始日期区间可能不同，所以以结束日期作为key
			String key = lnsBill.getEndDate();

			//取账单所在当前期还款计划
			rpyPlan = rfMap.get(key);

			//如果是空，说明是新的一期计划，清空原计划repayPlan，利息信息存入repayPlan
			//如果不是空，说明已经有了利息信息，将同期本金信息合并到repayPlan生成一整期还款计划
			if (null == rpyPlan){
				rpyPlan = new TblLnsrpyplan();
			}

			//到下一期时做判断
			if( !lastPeriod.equals(key)  && !lastPeriod.isEmpty() )
			{
				//结息或还款，取到下一期计划
				if("SETTLE".equals(repayChannel) || "REPAY".equals(repayChannel)||"DELAY".equals(repayChannel))
				{
					if(lnsBill.getPeriod() > lnsbasicinfo.getCurintterm())
						break;
				}
				//开户只取第一期计划
				else if("OPEN".equals(repayChannel) )
					break;
			}

			if ("DELAY".equals(repayChannel)){
				lnsBill.setEndDate(lnsBill.getCurendDate());
			}
			//合并一期本金利息账单生成还款计划，清空repayPlan
			dealRepayPlan(la, ctx, repayChannel, lnsbasicinfo, lnsBill, rpyPlan);
			rfMap.put(key, rpyPlan);
			rpyPlan = new TblLnsrpyplan();

			//记录当前账单期数
			lastPeriod = key;
		}

		for (Map.Entry<String, TblLnsrpyplan> entry : rfMap.entrySet()) {

			TblLnsrpyplan lnsrpyplan = entry.getValue();
			if(VarChecker.isEmpty(lnsrpyplan.getRepayintbdate())){
				continue;
			}

		   try {
				//计划落表
				DbAccessUtil.execute("Lnsrpyplan.insert", lnsrpyplan);
			}
			catch (FabSqlException e){
				//唯一索引重复不抛异常
	 			if ( !ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
					throw new FabException(e, "SPS100", "lnsrpyplan");
				}
			}
		}
	}


	/**
     * 功能：汇总账单，生成一期还款计划
     * 描述：将同一期本金、利息账单汇总到一期生成还款计划
     * 
     * @param 	la  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	repayChannel 			还款渠道
     * 									OPEN 	-- 开户日
     * 									SETTLE 	-- 结息日
     * 									REPAY 	-- 提前还款
     * 									ADVANCE	-- 提前还款且全部结清
     * 									FUTURE  -- 未来期（查询时标记未来期计划，不落表）
     * @param	lnsBill					账单
     * @param	repayPlan				还款计划
     * 
     * @since  1.1.1 
     */
	
	public static void dealRepayPlan(LoanAgreement la, TranCtx ctx, String repayChannel, TblLnsbasicinfo lnsbasicinfo, LnsBill lnsBill, TblLnsrpyplan rpyPlan){
		//机构
		rpyPlan.setBrc(ctx.getBrc());
		//账号
		rpyPlan.setAcctno(la.getContract().getReceiptNo());
		//期数
		rpyPlan.setRepayterm(lnsBill.getPeriod());
		//还款渠道
		rpyPlan.setRepayway(repayChannel);
		//剩余本金金额
		rpyPlan.setBalance(lnsBill.getPrinBal().getVal());
		
		//费用账单
//		if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_RMFE,
//							  ConstantDeclare.BILLTYPE.BILLTYPE_SQFE,
//							  ConstantDeclare.BILLTYPE.BILLTYPE_ISFE).contains(lnsBill.getBillType()) )
		//新增费用类型 2020-06-09
		if( LoanFeeUtils.isFeeType(lnsBill.getBillType()) )
		{
			//2019-12-31 预扣费在第一期应还和已还展示
			if( rpyPlan.getRepayterm() == 1 )
			{
				for( TblLnsfeeinfo withholdAmt : la.getFeeAgreement().getLnsfeeinfos())
				{
					if( "C".equals(withholdAmt.getRepayway())  )
					{
						rpyPlan.setReserve2(new FabAmount(rpyPlan.getReserve2()).selfAdd(withholdAmt.getDeducetionamt()).getVal());
					}
				}
			}
			//应还费用
			rpyPlan.setReserve2(new FabAmount(rpyPlan.getReserve2()).selfAdd(lnsBill.getBillAmt()).getVal());
		}
		//利息账单
		if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()) )
		{
			//本期起日
			rpyPlan.setRepayintbdate(lnsBill.getStartDate());
			//本期止日
			rpyPlan.setRepayintedate(lnsBill.getEndDate());
			//还款起日(同本期起日)
			rpyPlan.setRepayownbdate(lnsBill.getEndDate());
			//还款止日(包含宽限期)
			rpyPlan.setRepayownedate(lnsBill.getRepayendDate());
			//实际还款日
			rpyPlan.setActrepaydate(lnsBill.getEndDate());
			
			//应还利息
			rpyPlan.setTermretint(lnsBill.getBillAmt().getVal());	
			
		}
		//本金账单
		else if( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType()) )
		{
			//扣息放款无利息账单日期特殊处理（其他方式取利息账单日期作为还款计划日期）   3/4/5/6/7零利率不取本金日期
			if( new FabAmount(lnsbasicinfo.getDeductionamt()).isPositive() ||
				Arrays.asList("1").contains(la.getWithdrawAgreement().getRepayWay())||  //等额本息期利率计算取本金日期
				(  !new FabAmount(rpyPlan.getTermretint()).isPositive()  &&    //2018-12-07 零利率利息账本
				   !Arrays.asList("3","4","5","6","7").contains(la.getWithdrawAgreement().getRepayWay()) ))
			{
				//本期起日
				rpyPlan.setRepayintbdate(lnsBill.getStartDate());
				//本期止日
				rpyPlan.setRepayintedate(lnsBill.getEndDate());
				//还款起日(同本期起日)
				rpyPlan.setRepayownbdate(lnsBill.getEndDate());
				//还款止日(包含宽限期)
				rpyPlan.setRepayownedate(lnsBill.getRepayendDate());
				//实际还款日
				rpyPlan.setActrepaydate(lnsBill.getEndDate());
			}
			
			//应还本金
			rpyPlan.setTermretprin(lnsBill.getBillAmt().getVal());
			//剩余本金
			rpyPlan.setBalance(lnsBill.getPrinBal().getVal());
		}
	}
}
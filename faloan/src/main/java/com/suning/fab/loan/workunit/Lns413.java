package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabRate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	HY
 *
 * @version V1.1.1
 *
 * @see 	-还款计划查询
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns413 extends WorkUnit {

	//账号
	String acctNo;
	//每页大小
	Integer pageSize;
	//当前页
	Integer currentPage;
	//期数
	Integer repayTerm;
	
	//总行数
	Integer totalLine;
	//json串
	String pkgList;

	//用于存储每期还款计划的所有信息
	List<RePayPlan> repayPlanList = new ArrayList<RePayPlan>();
    TblLnsbasicinfo lnsbasicinfo;
	LnsBillStatistics lnsBillStatistics;
	LoanAgreement la;
	List<TblLnsrpyplan> tblRpyPlanList;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		
		//读取账户基本信息
		 la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,la);
		//最大结清日期
		 String maxSettleDate ="";
		//定义查询map
		Map<String,Object> param = new HashMap<String,Object>();
		//账号
		param.put("acctno", acctNo);
		//机构
		param.put("openbrc", ctx.getBrc());

		try {
			//取主文件信息
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			//异常
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		
		if (null == lnsbasicinfo){
			//lnsbasicinfo无数据
			throw new FabException("ACC108", acctNo);
		}

		//设置贴息 贴费标志
		FaloanJson tunnelMap=LoanRpyInfoUtil.getStickFlag(ctx,acctNo);

		//取试算信息
		lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(),ctx,lnsBillStatistics);
		
		//定义未来期账单list（用于生成未来期还款计划）
		List<LnsBill> listFutureBillInfo = new ArrayList<LnsBill>();
		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
		listFutureBillInfo.addAll(lnsBillStatistics.getBillInfoList());
		//未来期账单：从还款日到合同到期日之间的本金和利息账单
		listFutureBillInfo.addAll(lnsBillStatistics.getFutureBillInfoList());
		

		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史账单
		billList.addAll(lnsBillStatistics.getHisBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
		billList.addAll(lnsBillStatistics.getBillInfoList());
		//未来期账单：从还款日到合同到期日之间的本金和利息账单
		billList.addAll(lnsBillStatistics.getFutureBillInfoList());
		//获取呆滞呆账期间新罚息复利账单list
		billList.addAll(lnsBillStatistics.getCdbillList());

		
		//遍历未来期账单list生成未来期还款计划存入rfMap
		Map<String,TblLnsrpyplan> rfMap = new LinkedHashMap<String,TblLnsrpyplan>();
		for (LnsBill lnsBill:listFutureBillInfo)
		{
			String key = lnsBill.getPeriod().toString();
			TblLnsrpyplan repayPlan = rfMap.get(key);
			
			//如果是空，说明是新的一期计划，清空原计划repayPlan，利息信息存入repayPlan
			//如果不是空，说明已经有了利息信息，将同期本金信息合并到repayPlan生成一整期还款计划
			if (null == repayPlan){
				repayPlan = new TblLnsrpyplan();
			}
//			TblLnsrepayplan repayPlan = rfMap.getOrDefault(key,new TblLnsrepayplan());
			LoanRepayPlanProvider.dealRepayPlan(la, ctx, "FUTURE", lnsbasicinfo, lnsBill, repayPlan);
			
			//合并一期本金利息账单生成还款计划入map
			rfMap.put(key, repayPlan);
		}
		
		
		//取还款计划登记簿的历史数据
		if( tblRpyPlanList == null)
		 	tblRpyPlanList = repayplanquery(ctx);
		if(tblRpyPlanList.isEmpty())
		{
			tblRpyPlanList.add(rfMap.get("1"));
//			throw new FabException("SPS104", "lnsrepayplan");
		}
		
		//合并历史数据和rfMap的未来期数据生成tblRepayPlanList
		for(Entry<String, TblLnsrpyplan> m:rfMap.entrySet()){
			if( m.getValue().getRepayterm()>(tblRpyPlanList.get(tblRpyPlanList.size()-1).getRepayterm()))
				tblRpyPlanList.add(m.getValue());
		}
		
		//for转map key格式为，billType+tranDate+serSeqno+txSeq
		//
		Map<String, LnsBill> gintMap = new HashMap<String, LnsBill>();
		Map<String, LnsBill> prinMap = new HashMap<String, LnsBill>();
		Map<String, LnsBill> prinNoTranDateMap = new HashMap<String, LnsBill>();
		Map<String, LnsBill> prinOrNintMap = new HashMap<String, LnsBill>();
		Map<String, LnsBill> prinOrNintNoTranDateMap = new HashMap<String, LnsBill>();
		for(LnsBill bill : billList){
			if(ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(bill.getBillType())){
				gintMap.put(bill.getTranDate().toString()
						+"+"+bill.getSerSeqno().toString()
						+"+"+bill.getTxSeq().toString(), bill);
			}
			if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
				prinMap.put(bill.getTranDate().toString()
						+"+"+bill.getSerSeqno().toString()
						+"+"+bill.getTxSeq().toString(), bill);
				if(bill.getTranDate()==null){
					prinNoTranDateMap.put(bill.getTxSeq().toString(), bill);
				}
			}
			if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType()) ||
					   ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
				prinOrNintMap.put(bill.getTranDate().toString()
						+"+"+bill.getSerSeqno().toString()
						+"+"+bill.getTxSeq().toString(), bill);
				if(bill.getTranDate()==null){
					prinOrNintNoTranDateMap.put(bill.getTxSeq().toString(), bill);
				}
			}
		}


		//循环每期还款计划
		for( TblLnsrpyplan repayPlan : tblRpyPlanList )
		{
			RePayPlan plan = new RePayPlan();
			String settleDate ="";
			plan.setBrc(repayPlan.getBrc());
			plan.setAcctno(repayPlan.getAcctno());
			plan.setRepayterm(repayPlan.getRepayterm());
			plan.setRepayownbdate(repayPlan.getRepayownbdate());
			plan.setRepayownedate(repayPlan.getRepayownedate());
			plan.setRepayintbdate(repayPlan.getRepayintbdate());
			plan.setRepayintedate(repayPlan.getRepayintedate());
			plan.setTermretprin( new FabAmount(repayPlan.getTermretprin()));
			plan.setTermretint( new FabAmount(repayPlan.getTermretint()));
			plan.setTermcdint(new FabAmount(0.00));
			//天数（取本期开始日到本期结束日之间的实际天数）
			plan.setDays(CalendarUtil.actualDaysBetween(repayPlan.getRepayintbdate(),repayPlan.getRepayintedate()));
			
			//算头算尾时天数加一天
			if("true".equals(la.getInterestAgreement().getIsCalTail()) && !repayPlan.getRepayintbdate().equals(la.getContract().getContractStartDate())){
				plan.setDays(plan.getDays()+1);
			}
			
			plan.setTermstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
			plan.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			plan.setBalance(new FabAmount(repayPlan.getBalance()));
			plan.setTermretdate( repayPlan.getTermretdate());
			plan.setDeductionamt(new FabAmount(lnsbasicinfo.getDeductionamt()));
			
			//取罚息复利账单取对应本金/利息账单的截止日期（按截止日期统计账单所处的还款计划）

			FabAmount freeInterest = new FabAmount(0.00);//期减免金额

			for( LnsBill bill : billList)
			{
				//账单结束日期
				String billEndDate = bill.getEndDate();
				
				//统计罚息复利对应本金利息截止日期
				if( ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(bill.getBillType()) ||//罚息
					ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(bill.getBillType()) ||//复利
					ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(bill.getBillType()) )//宽限期利息
				{
					//取对应罚息的宽限期利息数据
					if(ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(bill.getBillType()))
					{
						LnsBill gintBill = gintMap.get(bill.getDerTranDate().toString()
														+"+"+bill.getDerSerseqno().toString()
														+"+"+bill.getDerTxSeq().toString());
						if(gintBill != null){
							//取对应宽限期利息的本金数据
							//账单结束日期取本金数据的账单结束日期
							if(gintBill.getDerTranDate()==null)
							{
								LnsBill lnsBill = prinNoTranDateMap.get(gintBill.getDerTxSeq().toString());
								if(lnsBill != null){
									billEndDate = lnsBill.getEndDate();
								}
							}
							else
							{
								LnsBill lnsBill = prinMap.get(gintBill.getDerTranDate().toString()
										+"+"+gintBill.getDerSerseqno().toString()
										+"+"+gintBill.getDerTxSeq().toString());
								if(lnsBill != null){
									billEndDate = lnsBill.getEndDate();
								}
							}
						}
					}
					//取本金和利息数据
					//如果账务日期为空
					if(bill.getDerTranDate()==null)
					{
						//如果是bill对应的本金数据
						//账单结束日期取本金数据的账单结束日期
						LnsBill lnsBill = prinOrNintNoTranDateMap.get(bill.getDerTxSeq().toString());
						if(lnsBill != null){
							billEndDate = lnsBill.getEndDate();
						}
					}
					else
					{
						//如果是bill对应的父数据
  						LnsBill lnsBill = prinOrNintMap.get(bill.getDerTranDate().toString()
								+"+"+bill.getDerSerseqno().toString()
								+"+"+bill.getDerTxSeq().toString());
						if(lnsBill != null){
							billEndDate = lnsBill.getEndDate();
						}
					}
				}
				//如果不是罚息、复利和宽限期利息数据
				
				//放款日还款的账单结束日期加一天，算到第一期还款计划（还款计划本期起日<账单结束日期<=还款计划本期止日）
				if( billEndDate.equals(lnsbasicinfo.getOpendate())&&bill.getStartDate().equals(bill.getEndDate()))
				{
					billEndDate = CalendarUtil.nDaysAfter(billEndDate, 1).toString("yyyy-MM-dd");
				}
				
				//按日期统计一整期最后还款日、已还未还金额
				if( (CalendarUtil.after(billEndDate,repayPlan.getRepayintbdate() ) &&//在本期起日之后
					!CalendarUtil.after(billEndDate,repayPlan.getActrepaydate() )) )//在本期实际止日或之前
				{
					if(!"RETURN".equals(bill.getBillProperty())){
						if(VarChecker.isEmpty(settleDate)){
							if(!VarChecker.isEmpty(bill.getLastDate())){
								settleDate=bill.getLastDate();
							}	
						}else{
							if(!VarChecker.isEmpty(bill.getLastDate())){
								if (CalendarUtil.after(bill.getLastDate(),settleDate)){
									settleDate=bill.getLastDate();
								}
							}
						}
					}
					plan.setBalance(new FabAmount(repayPlan.getBalance()));
					//代偿fundsource02
					if(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN.equals( bill.getBillProperty())){
						plan.setFundsource("02");
					} else if (ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH.equals(bill.getBillProperty())) {
						plan.setFundsource("03");
					} else if (ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL.equals(bill.getBillProperty())) {
						plan.setFundsource("04");
					}
					if( ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(bill.getSettleFlag()))
						plan.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
					
					if(VarChecker.isEmpty(bill.getTranDate()))
					{
						plan.setTermretdate(plan.getTermretdate()==null?"":plan.getTermretdate());
					}
					else
					{
						String tranDate = bill.getLastDate();//最后交易日期
						if(plan.getTermretdate().isEmpty())
						{
							if(VarChecker.isEmpty(tranDate))
							{
								plan.setTermretdate( "" );//本期结清日
							}
							else
							{
								plan.setTermretdate(tranDate);
							}
						}
						else
						{
							if(!VarChecker.isEmpty(tranDate))
							{
								if(!"ADVANCE".equals(repayPlan.getRepayway()) &&
									!bill.getBillAmt().equals(bill.getBillBal()))
									plan.setTermretdate( CalendarUtil.after(tranDate,plan.getTermretdate())?tranDate:plan.getTermretdate());
							}
						}
					}
					//判断该期次还款计划辅助表的RESERVE1为 ’EX’ - 展期时，则该期的状态取账本状态，不进行状态状态赋值
					if (!("EX".equals(repayPlan.getReserve1().trim()) && plan.getRepayterm()<bill.getPeriod())){
						//按状态大小排序取最大
						Integer p1 = Integer.valueOf(PropertyUtil
								.getPropertyOrDefault(
										"billstatus." + plan.getTermstatus(),
										"0"));
						Integer p2 = Integer.valueOf(PropertyUtil
								.getPropertyOrDefault(
										"billstatus." + bill.getBillStatus(),
										"0"));
						plan.setTermstatus(p1.compareTo(p2)<0?bill.getBillStatus():plan.getTermstatus());//计划状态：N正常G宽限期O逾期L呆滞B呆账
					}
                    //每一期的贷款状态 不依赖跑批,“EX”展期不参与计算
					if(!"EX".equals(repayPlan.getReserve1().trim())){
						LoanTransferProvider.updateTermStatus(ctx, la, plan,bill,lnsbasicinfo) ;
					}

                    if( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType()) )
					{
						plan.getPrinAmt().selfAdd( bill.getBillAmt().sub(bill.getBillBal()) );//已还本金
						plan.getNoretamt().selfAdd(bill.getBillBal());//未还本金
					}
					if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType()) )
					{
						plan.getIntAmt().selfAdd( bill.getBillAmt().sub(bill.getBillBal()));//已还利息
						plan.getNoretint().selfAdd(bill.getBillBal());//未还利息
						if( !VarChecker.isEmpty(bill.getRepayDateInt()))
						{
							plan.getTermcdint().selfAdd(bill.getRepayDateInt());//剩余结息利息
						}

						if("Y".equals(tunnelMap.getString(ConstantDeclare.PARACONFIG.DISCOUNTINTFLAG))){
							if(new FabRate(tunnelMap.getDouble(ConstantDeclare.PARACONFIG.ZQLL)).isPositive()){
								//应还 已还 打折利率
								plan.getTermretint().selfAdd(LoanRpyInfoUtil.getRepayTermDiscountFeeByRate(tunnelMap,bill.getBillType(),bill.getRepayWay(),bill.getBillAmt(),la).doubleValue());
								plan.getIntAmt().selfAdd(LoanRpyInfoUtil.getRepayTermDiscountFeeByRate(tunnelMap,bill.getBillType(),bill.getRepayWay(),(FabAmount) bill.getBillAmt().sub(bill.getBillBal()),la).doubleValue());
							}else{
								//加上减免金额 应还
								plan.getTermretint().selfAdd(bill.getTermFreeInterest()== null?new FabAmount(0.00):bill.getTermFreeInterest());
								freeInterest.selfAdd(bill.getTermFreeInterest()== null?new FabAmount(0.00):bill.getTermFreeInterest());//存放期减免金额 用于随借随还
							}
						}
					}
					if( ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(bill.getBillType()) ||
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(bill.getBillType()) ||
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(bill.getBillType()) )
					{
						plan.getSumrfint().selfAdd( bill.getBillAmt().sub(bill.getBillBal()));//已还罚息
						plan.getSumfint().selfAdd(bill.getBillAmt());//累计罚息
					}
					
					
				}
				
				//2018-12-04 放款撤销还款结清标志计划展示close
				if( "CA".equals(lnsbasicinfo.getLoanstat()))
					plan.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			}
			
			//结清日期赋值
			if( ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(plan.getSettleflag())){
				if(!VarChecker.isEmpty(settleDate)){
					plan.setSettledate(settleDate);//当期结清日期赋值
					if(VarChecker.isEmpty(maxSettleDate)){
						maxSettleDate=settleDate;//最大的结清日期赋值
					}else if (CalendarUtil.after(settleDate,maxSettleDate)){
						maxSettleDate=settleDate;//当前最大的结清日期更新
					}		
				}else{
					//未来期结清日期取账本最大的结清日期
					plan.setSettledate(maxSettleDate);
				}
			}
			//是否有到期日标志为NO时repayintbdate赋合同到期日
			if("NO".equals(la.getInterestAgreement().getNeedDueDate())){
                plan.setRepayintbdate(plan.getRepayintedate());
            }

			//展示的计提利息要减去已还利息
			plan.getTermcdint().selfSub(plan.getIntAmt());
			
			//随借随还的应还本金，应还利息，计提利息特殊处理    
			if( ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(lnsbasicinfo.getRepayway()) )
			{
				//非标移植数据应还利息展示在扣息金额字段  2018-02-02
				if( "NO".equals(lnsbasicinfo.getIscalint()) &&
				"OLD".equals(tblRpyPlanList.get(0).getRepayway()))
					plan.setDeductionamt(plan.getTermretint());
				
				//应还本金字段不会变
				plan.setTermretprin(new FabAmount(lnsbasicinfo.getContractamt()));
				
				//还到本金时应还利息要变
				FabAmount termIntAmt = new FabAmount(plan.getIntAmt().getVal());
				termIntAmt.selfAdd(plan.getNoretint());
				plan.setTermretint(termIntAmt);

				//随借随还需特殊处理，因换到本金 当期利息发生变化
				if("Y".equals(tunnelMap.getString(ConstantDeclare.PARACONFIG.DISCOUNTINTFLAG))){
					if(new FabRate(tunnelMap.getDouble(ConstantDeclare.PARACONFIG.ZQLL)).isPositive()){
						//应还 已还 打折利率
						plan.getTermretint().selfAdd(LoanRpyInfoUtil.getRepayTermDiscountFeeByRate(tunnelMap,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,null,plan.getTermretint(),la).doubleValue());
					}else{
						//加上减免金额 应还  减免金额是所有期总金额
						plan.getTermretint().selfAdd(freeInterest);
					}
				}
			}
			//等本等息计提利息取未还利息
			else if( ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway()) )
			{
				plan.setTermcdint(plan.getNoretint());
			}
			
			//新增未还罚息字段
			FabAmount termfInt = new FabAmount(plan.getSumfint().getVal());
			termfInt.selfSub(plan.getSumrfint());
			plan.setTermfint(termfInt);
			
			//提前结清还款日展示
			if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat()))
			{
//				if(plan.getTermretdate().isEmpty())
//					plan.setTermretdate(lnsbasicinfo.getModifydate());
				
				
				
				//2018-12-07 零利率利息账本    
				if(Arrays.asList("1","3","4","5","6","7").contains(la.getWithdrawAgreement().getRepayWay()) )
				{
					if( plan.getTermretint().isPositive() ||
						plan.getTermretprin().isPositive())
					{
						if(plan.getTermretdate().isEmpty())
							plan.setTermretdate(lnsbasicinfo.getModifydate());
					}
				}
				else
				{
					if(plan.getTermretdate().isEmpty())
						plan.setTermretdate(lnsbasicinfo.getModifydate());
				}
				
			}
				
			if( VarChecker.isEmpty(repayPlanList))
			{
				repayPlanList = new ArrayList<RePayPlan>();
			}
			repayPlanList.add(plan);
			
		}
		
		totalLine = repayPlanList.size();
		
		//封顶付息房抵贷还款计划未来期利息特殊处理
		if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate()) 
				&& !la.getContract().getFlag1().contains("C")){
			repayPlanList = futrueNintRpyplan(repayPlanList,lnsBillStatistics.getCdbillList(), ctx, la);
		}
		repayPlanList = repayRetTerm(repayPlanList);
		//还款计划加上免息 免费券
		LoanRpyInfoUtil.stickDiscountNint(repayPlanList,tunnelMap);
		pkgList = JsonTransfer.ToJson(repayPlanList);
	}



    //读取还款计划登记簿
	public List<TblLnsrpyplan> repayplanquery(TranCtx ctx) throws FabException{

		//定义查询map
		Map<String,Object> billparam = new HashMap<String,Object>();
		//按账号查询
		billparam.put("acctno", acctNo);
		billparam.put("brc", ctx.getBrc());
		List<TblLnsrpyplan> rpyplanlist = null;

		try {
			//取还款计划登记簿数据
			rpyplanlist = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsrpyplan", billparam, TblLnsrpyplan.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsrpyplan");
		}

		if (null == rpyplanlist){
			rpyplanlist = new ArrayList<TblLnsrpyplan>();
		}
		
		//返回还款计划list
		return rpyplanlist;
	}
	
	
	public List<RePayPlan> repayRetTerm(List<RePayPlan> planList){
		
		if(!VarChecker.isEmpty(repayTerm)){
			if(0 != repayTerm){
				List<RePayPlan> planRetTerm = new ArrayList<RePayPlan>();
				for(RePayPlan plan:planList){
					if(repayTerm.equals(plan.getRepayterm())){
						planRetTerm.add(plan);
					}
				}
				totalLine = planRetTerm.size();
				return repayRet(planRetTerm);
			}
		}
		return repayRet(planList);
	}
	
	public List<RePayPlan> repayRet(List<RePayPlan> planList){
		
		if(!VarChecker.isEmpty(pageSize) && !VarChecker.isEmpty(currentPage)){
			if (pageSize > 0 && currentPage > 0){
				List<RePayPlan> planret = new ArrayList<RePayPlan>();
				for (int i = (currentPage - 1) * pageSize; i < (currentPage * pageSize); i++){
					if (i < planList.size()){
						planret.add(planList.get(i));
					}
				}
				return planret;
			} 
		}
		return planList;
	}
	
	public List<RePayPlan> futrueNintRpyplan(List<RePayPlan> planList, List<LnsBill> cdbillList, TranCtx ctx, LoanAgreement la) throws FabException{
		//未来期利息的钱
//		FabAmount futrue = new FabAmount(0.0);
		//得到已计利息
		Map<String,Object> provision = new HashMap<String,Object>();
		provision.put("receiptno", la.getContract().getReceiptNo());
		provision.put("intertype", ConstantDeclare.INTERTYPE.PROVISION);
		
//		TblLnsprovisionreg lnsprovision;
		TblLnsprovision lnsprovision_new;
		try {
//			lnsprovision = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", provision, TblLnsprovisionreg.class);
			lnsprovision_new=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,la.getContract().getReceiptNo(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.PROVISION, la);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsprovision");
		}
		
//		if (!lnsprovision_new.isExist()){
//			lnsprovision = new TblLnsprovisionreg();
//			//第一次计提使用起息日
//			lnsprovision.setEnddate(Date.valueOf(la.getContract().getContractStartDate()));
//			lnsprovision.setTotaltax(0.00);
//			lnsprovision.setTotalinterest(0.00);
//		}

		//得到所有呆滞呆账罚息
		FabAmount LBDintAmt = new FabAmount(0.0);
		for( LnsBill tblLnsbill:cdbillList)
		{
			LBDintAmt.selfAdd(tblLnsbill.getBillAmt());
		}
		//封顶值-（已计利息+已计罚息+呆滞呆账罚息）
		FabAmount futrue = new FabAmount( la.getBasicExtension().getCapAmt().sub(lnsprovision_new.getTotalinterest()).sub(la.getBasicExtension().getHisDintComein()).sub(LBDintAmt).getVal());
		for(RePayPlan plan:planList){
			if(CalendarUtil.after(plan.getRepayintbdate(), ctx.getTranDate())){
				if(futrue.sub(plan.getNoretint()).isNegative()){
					plan.setNoretint(futrue);
					plan.setTermretint(futrue);
					futrue = new FabAmount(0.0);
				}else{
					futrue.selfSub(plan.getNoretint());
				}
			}
		}
		return planList;
	}


	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}


	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}


	/**
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}


	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}


	/**
	 * @return the currentPage
	 */
	public Integer getCurrentPage() {
		return currentPage;
	}


	/**
	 * @param currentPage the currentPage to set
	 */
	public void setCurrentPage(Integer currentPage) {
		this.currentPage = currentPage;
	}


	/**
	 * @return the repayTerm
	 */
	public Integer getRepayTerm() {
		return repayTerm;
	}


	/**
	 * @param repayTerm the repayTerm to set
	 */
	public void setRepayTerm(Integer repayTerm) {
		this.repayTerm = repayTerm;
	}


	/**
	 * @return the totalLine
	 */
	public Integer getTotalLine() {
		return totalLine;
	}


	/**
	 * @param totalLine the totalLine to set
	 */
	public void setTotalLine(Integer totalLine) {
		this.totalLine = totalLine;
	}


	/**
	 * @return the pkgList
	 */
	public String getPkgList() {
		return pkgList;
	}


	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
	}


	/**
	 * @return the repayPlanList
	 */
	public List<RePayPlan> getRepayPlanList() {
		return repayPlanList;
	}


	/**
	 * @param repayPlanList the repayPlanList to set
	 */
	public void setRepayPlanList(List<RePayPlan> repayPlanList) {
		this.repayPlanList = repayPlanList;
	}

    /**
     * Gets the value of lnsbasicinfo.
     *
     * @return the value of lnsbasicinfo
     */
    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    /**
     * Sets the lnsbasicinfo.
     *
     * @param lnsbasicinfo lnsbasicinfo
     */
    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }

	/**
	 * Gets the value of lnsBillStatistics.
	 *
	 * @return the value of lnsBillStatistics
	 */
	public LnsBillStatistics getLnsBillStatistics() {
		return lnsBillStatistics;
	}

	/**
	 * Sets the lnsBillStatistics.
	 *
	 * @param lnsBillStatistics lnsBillStatistics
	 */
	public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
		this.lnsBillStatistics = lnsBillStatistics;

	}

	/**
	 * Gets the value of la.
	 *
	 * @return the value of la
	 */
	public LoanAgreement getLa() {
		return la;
	}

	/**
	 * Sets the la.
	 *
	 * @param la la
	 */
	public void setLa(LoanAgreement la) {
		this.la = la;

	}

	/**
	 * Gets the value of tblRpyPlanList.
	 *
	 * @return the value of tblRpyPlanList
	 */
	public List<TblLnsrpyplan> getTblRpyPlanList() {
		return tblRpyPlanList;
	}

	/**
	 * Sets the tblRpyPlanList.
	 *
	 * @param tblRpyPlanList tblRpyPlanList
	 */
	public void setTblRpyPlanList(List<TblLnsrpyplan> tblRpyPlanList) {
		this.tblRpyPlanList = tblRpyPlanList;

	}
}

package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanBillSettleInterestSupporter;
import com.suning.fab.loan.utils.LoanPlanCintDint;
import com.suning.fab.loan.utils.LoanRepayPlanProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
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
 * @see 	展期还款计划试算
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns451 extends WorkUnit {

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
	String expandDate;
	
	FabAmount termrettotal = new FabAmount(0.0);
	FabAmount totalAmt = new FabAmount(0.0);

	//用于存储每期还款计划的所有信息
	List<RePayPlan> repayPlanList = new ArrayList<RePayPlan>();
	
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		
		Integer dbdxMaxPeriod = null;
		
		//定义查询map
		Map<String,Object> param = new HashMap<String,Object>();
		//账号
		param.put("acctno", acctNo);
		//机构
		param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo = null;
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
		
		if (VarChecker.isEmpty(expandDate) ){
			throw new FabException("LNS055","展期前到期日");
		}
		
		
		if(CalendarUtil.beforeAlsoEqual(expandDate, lnsbasicinfo.getContduedate())){
			throw new FabException("LNS111");
		}
		
		//非标不符合要求
		if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_XXHB
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_XBHX
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_YBYX).split(",")).contains(lnsbasicinfo.getRepayway())){
			throw new FabException("LNS045");
		}
		//汽车、手机、房抵贷不支持
		if(Arrays.asList("2412615,2412617,2412614".split(",")).contains(lnsbasicinfo.getPrdcode())){
			throw new FabException("LNS110");
		}
		//逾期借据不予展期
//				if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(lnsbasicinfo.getLoanstat())){
//					throw new FabException("LNS107");
//				}
		
		//判断是否是二次展期
		if(lnsbasicinfo.getFlag1().contains("A")){
			throw new FabException("LNS109");
		}
		
		//合同到期日之后无法展期
		if(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), lnsbasicinfo.getContduedate())){
			throw new FabException("LNS108");
		}
		
		//等本等息最后一期已还款，不能展期
		if(new FabAmount(lnsbasicinfo.getContractbal()).isZero()){
			throw new FabException("LNS125");
		}
		
		//放款当天不允许展期
		if(ctx.getTranDate().equals(lnsbasicinfo.getOpendate())){
			throw new FabException("LNS121");
		}
		
		//判断是短期、中期还是长期贷款
		int yearsNum = CalendarUtil.yearsBetween(lnsbasicinfo.getBeginintdate(), lnsbasicinfo.getContduedate());
		int expandYearNum = CalendarUtil.yearsBetween(lnsbasicinfo.getContduedate(), expandDate);
		if(yearsNum<1 || (yearsNum==1 && lnsbasicinfo.getBeginintdate().substring(4).equals(lnsbasicinfo.getContduedate().substring(4)))){
			if(expandYearNum>1 || (expandYearNum==1 && !lnsbasicinfo.getContduedate().substring(4).equals(expandDate.substring(4)))){
				throw new FabException("LNS105");
			}
		}else if(yearsNum<5 || (yearsNum==5 && lnsbasicinfo.getBeginintdate().substring(4).equals(lnsbasicinfo.getContduedate().substring(4)))){
			if(CalendarUtil.actualDaysBetween(lnsbasicinfo.getContduedate(), expandDate)>(CalendarUtil.actualDaysBetween(lnsbasicinfo.getBeginintdate(), lnsbasicinfo.getContduedate())/2)){
				throw new FabException("LNS105");
			}
		}else{
			if(expandYearNum==3 && !lnsbasicinfo.getContduedate().substring(4).equals(expandDate.substring(4))){
				throw new FabException("LNS105");
			}
		}
		
		//还款方式是5和6不符合要求
		if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_LSBQ
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_LZHK).split(",")).contains(lnsbasicinfo.getRepayway())){
			throw new FabException("LNS045");
		}
		
		//读取账户基本信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		
		//生成还款计划
		LnsBillStatistics beforeBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
		List<LnsBill> beforelnsbill = new ArrayList<LnsBill>();
		//历史期账单（账单表）
		beforelnsbill.addAll(beforeBillStatistics.getHisBillList());
		beforelnsbill.addAll(beforeBillStatistics.getHisSetIntBillList());
		//当前期和未来期
		beforelnsbill.addAll(beforeBillStatistics.getBillInfoList());
		beforelnsbill.addAll(beforeBillStatistics.getFutureBillInfoList());
		beforelnsbill.addAll(beforeBillStatistics.getFutureOverDuePrinIntBillList());
		
		//最后一期判断
		boolean isLastPeriod = false;
		boolean isLastNintPeriod = false;
		//最后一期本金账本
		LnsBill lastPrinBill = new LnsBill();
		
		for(LnsBill bill : beforelnsbill){
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(bill.getBillType())){
				if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
						|| 
						(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
					if(bill.getEndDate().equals(lnsbasicinfo.getContduedate())){
						isLastPeriod = true;
						lastPrinBill = bill;
					}
				}
			}
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
				if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
						|| 
						(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
					if(bill.getEndDate().equals(lnsbasicinfo.getContduedate())){
						isLastNintPeriod = true;
					}
				}
			}
		}
		
		if(isLastNintPeriod && isLastPeriod){
			isLastPeriod = true;
		}else{
			isLastPeriod = false;
		}
		
		
		//最后一期展期做特殊处理
		if(isLastPeriod
				&& !ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(lnsbasicinfo.getRepayway())){
			//value1字段存入格式 是最后一期的话（1，[原最后一期开始日]，[原最后一期结束日]）
			String value1 = "1"+","+lastPrinBill.getStartDate()+","+lastPrinBill.getEndDate();
			la.getBasicExtension().setLastPeriodStr(value1);
		}else{
			la.getBasicExtension().setLastPeriodStr("0");
		}
		//展期前本金余额
		la.getBasicExtension().setBalBeforeEx(new FabAmount(lnsbasicinfo.getContractbal()));
		//展期所处本金期数
		//如果是等本等息
		if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
			//查询等本等息未生成账本的期数
			//机构
			param.put("brc", ctx.getBrc());
			
			try {
				//取主文件信息
				dbdxMaxPeriod = DbAccessUtil.queryForObject("CUSTOMIZE.query_period", param, Integer.class);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbill");
			}
			if(null == dbdxMaxPeriod){
				dbdxMaxPeriod = 0;
			}
			la.getBasicExtension().setExtnums(dbdxMaxPeriod+1);
		}else{
			la.getBasicExtension().setExtnums(lnsbasicinfo.getCurprinterm());
		}
		
		la.getContract().setContractEndDate(expandDate);
//		if(ConstantDeclare.REPAYWAY.REPAYWAY_DBDX.equals(lnsbasicinfo.getRepayway())){
//			la.getContract().setFlag1("A,"+(lnsbasicinfo.getCurprinterm()-1)+","+lnsbasicinfo.getContractbal());
//		}else{
			la.getContract().setFlag1("A");
//		}
		
		if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_HDE
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX).split(",")).contains(lnsbasicinfo.getRepayway())){
			la.getWithdrawAgreement().setPeriodFormula(getPrinPerformula(la.getContract().getStartIntDate(),expandDate));
			if(ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(lnsbasicinfo.getRepayway())){
				la.getInterestAgreement().setPeriodFormula(getPrinPerformula(la.getContract().getStartIntDate(),expandDate));
			}
		}
		//取试算信息
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(),ctx);
		
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
		List<TblLnsbill> cdbillList = genCintDintList(getAcctNo(), ctx,expandDate,la,lnsBillStatistics);
		for( TblLnsbill tblLnsbill:cdbillList)
		{
			billList.add(BillTransformHelper.convertToLnsBill(tblLnsbill));
		}
		
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
		List<TblLnsrpyplan> tblRpyPlanList = repayplanquery(ctx);
		//当期还款计划
		TblLnsrpyplan presentRpyplan = new TblLnsrpyplan();
		if(tblRpyPlanList.isEmpty())
		{
			tblRpyPlanList.add(rfMap.get("1"));
//			throw new FabException("SPS104", "lnsrepayplan");
		}else{
			presentRpyplan = tblRpyPlanList.get(tblRpyPlanList.size()-1);
			if(!ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
				tblRpyPlanList = tblRpyPlanList.subList(0, tblRpyPlanList.size()-1);
			}else if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway()) && !dbdxMaxPeriod.equals(tblRpyPlanList.get(tblRpyPlanList.size()-1).getRepayterm()) ){
				tblRpyPlanList = tblRpyPlanList.subList(0, tblRpyPlanList.size()-1);
			}
			
		}
		
		//判断是否是展期前的最后一期
//		boolean isLastPeriod = false;
//		if(presentRpyplan.getRepayterm().equals(beforeBillStatistics.getPrinTotalPeriod())){
//			isLastPeriod = true;
//		}
		
		//合并历史数据和rfMap的未来期数据生成tblRepayPlanList
		for(Entry<String, TblLnsrpyplan> m:rfMap.entrySet()){
//			if( !m.getValue().getRepayterm().equals(tblRpyPlanList.get(tblRpyPlanList.size()-1).getRepayterm()))
			//当期还款计划更新
			TblLnsrpyplan presentRpyplan1 = presentRpyUpdate(presentRpyplan,m.getValue(),isLastPeriod,billList);
			if(null == presentRpyplan1){
				tblRpyPlanList.add(m.getValue());
			}else{
				tblRpyPlanList.add(presentRpyplan1);
			}
				
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
			plan.setTermstatus("N");
			plan.setDeductionamt(new FabAmount(lnsbasicinfo.getDeductionamt()));
			
			//取罚息复利账单取对应本金/利息账单的截止日期（按截止日期统计账单所处的还款计划）
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
					plan.setBalance(new FabAmount(repayPlan.getBalance()));
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
					}
					if( ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(bill.getBillType()) ||
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(bill.getBillType()) ||
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(bill.getBillType()) )
					{
						plan.getSumrfint().selfAdd( bill.getBillAmt().sub(bill.getBillBal()));//已还罚息
						plan.getSumfint().selfAdd(bill.getBillAmt());//累计罚息
					}
					
					
				}
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
				if(plan.getTermretdate().isEmpty())
					plan.setTermretdate(lnsbasicinfo.getModifydate());
			}
				
			if( VarChecker.isEmpty(repayPlanList))
			{
				repayPlanList = new ArrayList<RePayPlan>();
			}
			repayPlanList.add(plan);
			
		}
		
		totalLine = repayPlanList.size();
		repayPlanList = repayRetTerm(repayPlanList);
		for(RePayPlan plan:repayPlanList){
			termrettotal.selfAdd(plan.getSumfint());
			termrettotal.selfAdd(plan.getSumcint());
			termrettotal.selfAdd(plan.getTermretint());
			termrettotal.selfAdd(plan.getTermretprin());
			totalAmt.selfAdd(plan.getPrinAmt());
			totalAmt.selfAdd(plan.getIntAmt());
			totalAmt.selfAdd(plan.getSumrcint());
			totalAmt.selfAdd(plan.getSumrfint());
		}
		pkgList = JsonTransfer.ToJson(repayPlanList);
	}
	
	/**
	 * 
	 * @param presentRpyplan  展期前当前期还款计划
	 * @param tblLnsrpyplan	展期后当前期到展期日的还款计划
	 * @param isLastPeriod  是否最后一期展期
	 * @param billList		展期后试算结果
	 * @return
	 */
	private TblLnsrpyplan presentRpyUpdate(TblLnsrpyplan presentRpyplan,
			TblLnsrpyplan tblLnsrpyplan, boolean isLastPeriod, List<LnsBill> billList) {
		// TODO Auto-generated method stub
		//利用展期后试算结果算出展期后当前期的应还利息
		//应还本金
//		FabAmount termretprin = new FabAmount(0.00);
		FabAmount termretint = new FabAmount(0.00);
		for(LnsBill bill : billList ){
			if(presentRpyplan.getRepayterm().equals(bill.getPeriod())){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					termretint.selfAdd(bill.getBillAmt());
				}
			}
			
		}
		
		TblLnsrpyplan presentRpyplan1 = tblLnsrpyplan;
		if(!presentRpyplan.getRepayterm().equals(presentRpyplan1.getRepayterm())){
			return null;
		}else{
			
			if(!presentRpyplan.getRepayintbdate().equals(presentRpyplan1.getRepayintbdate())){
				presentRpyplan1.setRepayintbdate(presentRpyplan.getRepayintbdate());
				presentRpyplan1.setIntamt(presentRpyplan.getIntamt()+presentRpyplan1.getIntamt());
				presentRpyplan1.setNoretamt(presentRpyplan.getNoretamt()+presentRpyplan1.getNoretamt());
				presentRpyplan1.setNoretint(presentRpyplan.getNoretint()+presentRpyplan1.getNoretint());
				presentRpyplan1.setPrinamt(presentRpyplan.getPrinamt()+presentRpyplan1.getPrinamt());
				if(isLastPeriod){
					presentRpyplan1.setTermretint(termretint.getVal());
				}else{
					presentRpyplan1.setTermretint(presentRpyplan.getTermretint());
				}
				
			}
			return presentRpyplan1;
		}
		
	}


//	private List<TblLnsrpyplan> presentRpyUpdate(
//			List<TblLnsrpyplan> tblRpyPlanList,List<LnsBill> lnsbill,List<LnsBill> afterlnsbill, TblLnsbasicinfo lnsbasicinfo, TranCtx ctx) {
//		//主逻辑
//			//如果是等本等息、等额本息、等额本金、季还本，月还息
//			if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_DBDX
//					+","+ConstantDeclare.REPAYWAY.REPAYWAY_DEBX
//					+","+ConstantDeclare.REPAYWAY.REPAYWAY_DEBJ
//					+","+ConstantDeclare.REPAYWAY.REPAYWAY_JYHK).split(",")).contains(lnsbasicinfo.getRepayway())){
//				for(int i=0;i<afterlnsbill.size();i++){
//					LnsBill bill = afterlnsbill.get(i);
//					if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(bill.getBillType())){
//						if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
//								&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
//								|| 
//								(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
//								&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
//							param.clear();
//							param.put("acctno", acctNo);
//							param.put("openbrc", ctx.getBrc());
//							param.put("brc", ctx.getBrc());
//							param.put("repayterm", bill.getPeriod());
//							param.put("termretprin", bill.getBillAmt().getVal());
//							FabAmount prinbalance = new FabAmount(lnsbasicinfo.getContractbal().doubleValue());
//							prinbalance.selfSub(bill.getBillAmt());
//							param.put("balance", prinbalance.getVal());
//							if(bill.getPeriod().equals(afterBillStatistics.getIntTotalPeriod())){
//								param.put("repayownbdate", expandDate);
//								param.put("repayownedate", CalendarUtil.nDaysAfter(expandDate, lnsbasicinfo.getGracedays()).toString("yyyy-MM-dd"));
//								param.put("repayintbdate", lnsbasicinfo.getBeginintdate());
//								param.put("repayintedate", expandDate);
//								param.put("actrepaydate", expandDate);
//								param.put("balance", 0.00);
//							}
//						}
//					}
//					//bianli出当前期利息账单进行处理
//					if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
//						if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
//								&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
//								|| 
//								(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
//								&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
//							if(bill.getBillAmt().equals(bill.getBillBal())){
//								param.clear();
//								param.put("acctno", acctNo);
//								param.put("openbrc", ctx.getBrc());
//								param.put("brc", ctx.getBrc());
//								param.put("termretint", bill.getBillBal().getVal());
//								param.put("repayterm", bill.getPeriod());
//								
//							}
//						}
//					}
//				}
//			}
//			//
//			if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_HDE
//					+","+ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE
//					+","+ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX).split(",")).contains(lnsbasicinfo.getRepayway())){
//				
//				for(int i=0;i<lnsbill.size();i++){
//					LnsBill bill = lnsbill.get(i);
//					if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
//							&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
//							|| 
//							(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
//							&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
//						if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
//							LnsBill afterbill = afterlnsbill.get(i);
//							if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_HDE
//									+","+ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE).split(",")).contains(lnsbasicinfo.getRepayway())){
//								if(!bill.getPeriod().equals(afterBillStatistics.getIntTotalPeriod())){
//									param.put("termretprin", 0.00);
//								}
//							}
//							FabAmount prinbalance = new FabAmount(lnsbasicinfo.getContractbal().doubleValue());
//							param.put("balance", prinbalance.getVal());
//							if(bill.getPeriod().equals(afterBillStatistics.getIntTotalPeriod())){
//								param.put("repayownbdate", expandDate);
//								param.put("repayownedate", CalendarUtil.nDaysAfter(expandDate, lnsbasicinfo.getGracedays()).toString("yyyy-MM-dd"));
//								param.put("repayintbdate", lnsbasicinfo.getBeginintdate());
//								param.put("repayintedate", expandDate);
//								param.put("actrepaydate", expandDate);
//								param.put("balance", 0.00);
//							}
//							param.put("termretint", afterbill.getBillBal().getVal());
//						}
//					}
//				}
//				
//			}
//	}


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

	public static List<TblLnsbill> genCintDintList(String acctNo, TranCtx ctx, String expandDate2, LoanAgreement la2, LnsBillStatistics lnsBillStatistics2) throws FabException{
		//定义返回呆滞呆账罚息复利账单list
		List<TblLnsbill> cdBillList = new ArrayList<TblLnsbill>();	
		//获取贷款协议信息
		//生成还款计划
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		hisLnsbill.addAll(lnsBillStatistics2.getHisBillList());
		hisLnsbill.addAll(lnsBillStatistics2.getHisSetIntBillList());
		hisLnsbill.addAll(lnsBillStatistics2.getFutureOverDuePrinIntBillList());
		//循环处理呆滞呆账状态的本金和利息账单
		for (LnsBill lnsHisbill : hisLnsbill) {
			//首先处理呆滞呆账本金产生 罚息账单
				
			if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
				
				if(VarChecker.asList("3","4").contains(la2.getInterestAgreement().getDintSource()) &&
				lnsHisbill.getSettleFlag().equals("CLOSE")	)
				{
					continue;
				}
				
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la2,lnsHisbill, la2.getRateAgreement().getOverdueRate(), ctx.getTranDate());
				if(famt == null || famt.isZero())
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				tblLnsbill.setPeriod(lnsHisbill.getPeriod());
				tblLnsbill.setBillamt(famt.getVal());
				tblLnsbill.setBillbal(famt.getVal());
				tblLnsbill.setPrinbal(lnsHisbill.getBillBal().getVal());
				tblLnsbill.setBillrate(la2.getRateAgreement().getOverdueRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsHisbill.getIntendDate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsHisbill.getRepayWay());
				tblLnsbill.setCcy(lnsHisbill.getCcy());
				tblLnsbill.setDertrandate(lnsHisbill.getTranDate());
				tblLnsbill.setDerserseqno(lnsHisbill.getSerSeqno());
				tblLnsbill.setDertxseq(lnsHisbill.getTxSeq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsHisbill.getIntendDate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la2.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}

				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				
				cdBillList.add(tblLnsbill);
			}
			
			//处理呆滞利息产生呆滞复利账单
			if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) ) {
				
				if( VarChecker.asList("3","4").contains(la2.getInterestAgreement().getDintSource()) &&
				lnsHisbill.getSettleFlag().equals("CLOSE")	)
				{
					continue;
				}
				
				//当前工作日期减复利记至日期得到计复利天数
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la2,lnsHisbill, la2.getRateAgreement().getCompoundRate(), ctx.getTranDate());
				if(famt == null || famt.isZero())
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_CINT);
				tblLnsbill.setPeriod(lnsHisbill.getPeriod());
				tblLnsbill.setBillamt(famt.getVal());
				tblLnsbill.setBillbal(famt.getVal());
				tblLnsbill.setPrinbal(lnsHisbill.getBillBal().getVal());
				tblLnsbill.setBillrate(la2.getRateAgreement().getCompoundRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsHisbill.getIntendDate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsHisbill.getRepayWay());
				tblLnsbill.setCcy(lnsHisbill.getCcy());
				tblLnsbill.setDertrandate(lnsHisbill.getTranDate());
				tblLnsbill.setDerserseqno(lnsHisbill.getSerSeqno());
				tblLnsbill.setDertxseq(lnsHisbill.getTxSeq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsHisbill.getIntendDate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la2.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}
				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				
				cdBillList.add(tblLnsbill);
			}
	
		}	
		
		return cdBillList;
	}

	private String getPrinPerformula(String begin,String end){
		DateTime b = new DateTime(begin);  
		DateTime e = new DateTime(end);  

		//计算区间天数  
		Period p = new Period(b, e, PeriodType.yearMonthDay());
		String result = "";
		if(p.getYears() != 0 && (p.getMonths() != 0 || p.getDays() != 0)){
			result += p.getYears()+"Y";
		}else if(p.getMonths() == 0 && p.getDays() == 0){
			result += p.getYears()+"YA";
		}
		if(p.getMonths() != 0 && p.getDays() != 0){
			result += p.getMonths()+"M";
		}else if(p.getMonths() != 0 && p.getDays() == 0){
			result += p.getMonths()+"MA";
		}
		if(p.getDays() != 0){
			result += p.getDays()+"DA";
		}
		return result;
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
	 * @return the expandDate
	 */
	public String getExpandDate() {
		return expandDate;
	}

	/**
	 * @param expandDate the expandDate to set
	 */
	public void setExpandDate(String expandDate) {
		this.expandDate = expandDate;
	}

	/**
	 * @return the termrettotal
	 */
	public FabAmount getTermrettotal() {
		return termrettotal;
	}

	/**
	 * @param termrettotal the termrettotal to set
	 */
	public void setTermrettotal(FabAmount termrettotal) {
		this.termrettotal = termrettotal;
	}

	/**
	 * @return the totalAmt
	 */
	public FabAmount getTotalAmt() {
		return totalAmt;
	}

	/**
	 * @param totalAmt the totalAmt to set
	 */
	public void setTotalAmt(FabAmount totalAmt) {
		this.totalAmt = totalAmt;
	}


	

}

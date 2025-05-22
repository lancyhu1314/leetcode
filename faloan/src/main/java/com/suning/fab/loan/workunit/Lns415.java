package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.suning.fab.loan.account.FeeRepayPlanQuery;
import com.suning.fab.loan.account.NonStaticFeeRepayPlanQuery;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.utils.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
//import com.suning.fab.loan.domain.TblLnsrepayplan;
import com.suning.fab.loan.domain.TblLnsrpyplan;
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
public class Lns415 extends WorkUnit {

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

	TblLnsbasicinfo lnsbasicinfo = null;

	@Override
	public void run() throws Exception {
	TranCtx ctx = getTranctx();
		
		//读取账户基本信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
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
		
		//取试算信息
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(),ctx);
		

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
		List<TblLnsbill> cdbillList;

		//获取呆滞呆账期间新罚息复利账单list
		billList.addAll(lnsBillStatistics.getCdbillList());
		
		List<TblLnsrpyplan> tblRpyPlanList = rpyplanquery(ctx);
		
		//for转map key格式为，billType+tranDate+serSeqno+txSeq
		Map<String, LnsBill> gintMap = new HashMap<String, LnsBill>();
		Map<String, LnsBill> prinMap = new HashMap<String, LnsBill>();
		Map<String, LnsBill> prinNoTranDateMap = new HashMap<String, LnsBill>();
		Map<String, LnsBill> prinOrNintMap = new HashMap<String, LnsBill>();
		Map<String, LnsBill> prinOrNintNoTranDateMap = new HashMap<String, LnsBill>();

		Map<String, LnsBill> feeMap = new HashMap<>();
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
			//让违约金匹配上还款计划  让违约金能找到对应的账本的ENDDATE
			if(LoanFeeUtils.isFeeType(bill.getBillType())
					&&!VarChecker.isEmpty(bill.getLnsfeeinfo())
					&& bill.getLnsfeeinfo().getOverrate().compareTo(0.0)>0){

				if(bill.getTranDate()==null) {
					feeMap.put("txseq"+bill.getTxSeq().toString(), bill);
				}
				else {
					feeMap.put(bill.getTranDate().toString()
							+ "+" + bill.getSerSeqno().toString()
							+ "+" + bill.getTxSeq().toString(), bill);
				}
			}
		}
		
		//用于存储每期还款计划的所有信息
		List<RePayPlan> rpyPlanList = new ArrayList<RePayPlan>();
		String compensiteFlag="0";
		//循环每期还款计划
		for( TblLnsrpyplan rpyPlan : tblRpyPlanList )
		{
			NonStaticFeeRepayPlanQuery plan = new NonStaticFeeRepayPlanQuery();
			String settleDate ="";
			plan.setBrc(rpyPlan.getBrc());
			plan.setAcctno(rpyPlan.getAcctno());
			plan.setRepayterm(rpyPlan.getRepayterm());
			plan.setRepayownbdate(rpyPlan.getRepayownbdate());
			plan.setRepayownedate(rpyPlan.getRepayownedate());
			plan.setRepayintbdate(rpyPlan.getRepayintbdate());
			plan.setRepayintedate(rpyPlan.getRepayintedate());
			plan.setTermretprin( new FabAmount(rpyPlan.getTermretprin()));
			plan.setTermretint( new FabAmount(rpyPlan.getTermretint()));
			plan.setTermcdint(new FabAmount(0.00));
			plan.setTermcdfee(new FabAmount(0.00));

			//天数（取本期开始日到本期结束日之间的实际天数）
			plan.setDays(CalendarUtil.actualDaysBetween(rpyPlan.getRepayintbdate(),rpyPlan.getRepayintedate()));
			plan.setTermstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
			plan.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			plan.setBalance(new FabAmount(rpyPlan.getBalance()));
			plan.setTermretdate( rpyPlan.getTermretdate());
			plan.setTermstatus("N");
			plan.setDeductionamt(new FabAmount(lnsbasicinfo.getDeductionamt()));
			
			//取罚息复利账单取对应本金/利息账单的截止日期（按截止日期统计账单所处的还款计划）
			for( LnsBill bill : billList)
			{
				//账单结束日期
//				String billEndDate = bill.getEndDate();
				//当期到期日
				String billCurrDate = bill.getCurendDate();
				
				//统计罚息复利对应本金利息截止日期
				if( ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(bill.getBillType()) ||
					ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(bill.getBillType()) ||
					ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(bill.getBillType()) )
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
									billCurrDate = lnsBill.getEndDate();
								}
							}
							else
							{
								LnsBill lnsBill = prinMap.get(gintBill.getDerTranDate().toString()
										+"+"+gintBill.getDerSerseqno().toString()
										+"+"+gintBill.getDerTxSeq().toString());
								if(lnsBill != null){
									billCurrDate = lnsBill.getEndDate();
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
							billCurrDate = lnsBill.getEndDate();
						}
					}
					else
					{
						//如果是bill对应的父数据
						LnsBill lnsBill = prinOrNintMap.get(bill.getDerTranDate().toString()
								+"+"+bill.getDerSerseqno().toString()
								+"+"+bill.getDerTxSeq().toString());
						if(lnsBill != null){
							billCurrDate = lnsBill.getEndDate();
						}
					}
				}

				//违约金
				if(bill.isPenalty()){
					//取本金和利息数据
					//如果账务日期为空
					if(bill.getDerTranDate()==null)
					{
						//如果是bill对应的本金数据
						//账单结束日期取本金数据的账单结束日期
						LnsBill lnsBill = feeMap.get("txseq"+bill.getDerTxSeq().toString());
						if(lnsBill != null){
							billCurrDate = lnsBill.getEndDate();
						}
					}
					else
					{
						//如果是bill对应的父数据
						LnsBill lnsBill = feeMap.get(bill.getDerTranDate().toString()
								+"+"+bill.getDerSerseqno().toString()
								+"+"+bill.getDerTxSeq().toString());
						if(lnsBill != null){
							billCurrDate = lnsBill.getEndDate();
						}
					}
				}
				
//				//放款日还款的账单结束日期加一天，算到第一期还款计划（还款计划本期起日<账单结束日期<=还款计划本期止日）
//				if( bill.getEndDate().equals(lnsbasicinfo.getOpendate()))
//				{
//					billEndDate = CalendarUtil.nDaysAfter(bill.getEndDate(), 1).toString("yyyy-MM-dd");
//				}
				
				//按日期统计一整期最后还款日、已还未还金额
				if( (CalendarUtil.after(billCurrDate,rpyPlan.getRepayintbdate() ) &&
					!CalendarUtil.after(billCurrDate,rpyPlan.getActrepaydate() )) )
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
					plan.setBalance(new FabAmount(rpyPlan.getBalance()));
					if( ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(bill.getSettleFlag()))
						plan.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
					
					if(VarChecker.isEmpty(bill.getTranDate()))
					{
						plan.setTermretdate(plan.getTermretdate()==null?"":plan.getTermretdate());
					}
					else
					{
						String tranDate = bill.getLastDate();
						if(plan.getTermretdate().isEmpty())
						{
							if(VarChecker.isEmpty(tranDate))
							{
								plan.setTermretdate( "" );
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
								if(!"ADVANCE".equals(rpyPlan.getRepayway()) &&
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


					//本息罚的费用状态
					if( !LoanFeeUtils.isFeeType(bill.getBillType())
							&&!bill.isPenalty()){
						plan.setTermstatus(p1.compareTo(p2)<0?bill.getBillStatus():plan.getTermstatus());
						// 每一期的贷款状态 不依赖跑批的还款计划
						LoanTransferProvider.updateTermStatus(ctx, la, plan,bill,lnsbasicinfo);
					}

					//2019-01-06 费用逾期状态
					else if(LoanFeeUtils.isFeeType(bill.getBillType()))
					{
//						plan.setCurrentstat(p1.compareTo(p2)<0?bill.getBillStatus():plan.getTermstatus());//费用状态：N正常 O逾期

						plan.setCurrentstat(calculationStat(bill,la));

					}


					if( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType()) )
					{
						plan.getPrinAmt().selfAdd( bill.getBillAmt().sub(bill.getBillBal()) );
						plan.getNoretamt().selfAdd(bill.getBillBal());
					}
					if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType()) )
					{
						plan.getIntAmt().selfAdd( bill.getBillAmt().sub(bill.getBillBal()));
						plan.getNoretint().selfAdd(bill.getBillBal());
						if( !VarChecker.isEmpty(bill.getRepayDateInt()))
						{
							plan.getTermcdint().selfAdd(bill.getRepayDateInt());
						}
					}
					if( ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(bill.getBillType()) ||
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(bill.getBillType()) ||
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(bill.getBillType()) )
					{
						plan.getSumrfint().selfAdd( bill.getBillAmt().sub(bill.getBillBal()));
						plan.getSumfint().selfAdd(bill.getBillAmt());
					}

					//2019-01-06 费用结清状态
					if( LoanFeeUtils.isFeeType(bill.getBillType()) )
					{

						plan.getFeeamt().selfAdd(bill.getBillAmt().sub(bill.getBillBal()));
						plan.getNoretfee().selfAdd(bill.getBillBal());
						plan.getTermretfee().selfAdd(bill.getBillAmt());
						if( !bill.getBillBal().isZero())
							plan.setFeeflag("01");

						if( !VarChecker.isEmpty(bill.getRepayDateInt()))
						{
//								plan.setTermcdfee(new FabAmount());
							plan.getTermcdfee().selfAdd(bill.getRepayDateInt());//剩余结息利息
						}

						if( "COMPEN".equals(bill.getBillProperty()) || "1".equals(compensiteFlag) ) {
							plan.setFeeflag("02");
							compensiteFlag = "1";
						}
					}

					//违约金加入罚息字段
					if(bill.isPenalty()){
//						plan.getSumfint().selfAdd(bill.getBillAmt());//应还
//						plan.getSumrfint().selfAdd(bill.getBillAmt().sub(bill.getBillBal()));//已还
						plan.setTermretpenalty(bill.getBillAmt());//应还
						plan.setPenaltyamt((FabAmount) (bill.getBillAmt().sub(bill.getBillBal())));//已还
						plan.setNoretpenalty(bill.getBillBal());//未还违约金
					}
				}
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
			//2019-12-31 预扣费在第一期应还和已还展示
			if( plan.getRepayterm() == 1 )
			{
				for( TblLnsfeeinfo withholdAmt : la.getFeeAgreement().getLnsfeeinfos())
				{
					if( "C".equals(withholdAmt.getRepayway())  )
					{
						plan.getFeeamt().selfAdd(withholdAmt.getDeducetionamt());
						plan.getTermretfee().selfAdd(withholdAmt.getDeducetionamt());
					}
				}
			}
			
			//展示的计提利息要减去已还利息
			plan.getTermcdint().selfSub(plan.getIntAmt());
			plan.getTermcdfee().selfSub(plan.getFeeamt());//剩余结息利息
			//随借随还的应还本金，应还利息，计提利息特殊处理    
			if( ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(lnsbasicinfo.getRepayway()) )
			{
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
				
			rpyPlanList.add(plan);
		}
		
		totalLine = rpyPlanList.size();
	
		pkgList = JsonTransfer.ToJson(repayRetTerm(rpyPlanList));
	}

	//计算管理费的状态 不依赖跑批结果
	private String calculationStat( LnsBill lnsBill,LoanAgreement loanAgreement) {
		//02结清
		if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleFlag())
				||!loanAgreement.getInterestAgreement().getNeedRisksClassificationFlag()){
			if(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(lnsBill.getBillStatus()))
				return "01";
			else
				return "02";
		}

		if(CalendarUtil.before(CalendarUtil.nDaysAfter(lnsBill.getCurendDate(),
				lnsbasicinfo.getGracedays()).toString("yyyy-MM-dd"),
				getTranctx().getTranDate())) {
			return "02";
		}

		return "01";
	}
	
	
	//读取还款计划登记簿
	public List<TblLnsrpyplan> rpyplanquery(TranCtx ctx) throws FabException{

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
	

}

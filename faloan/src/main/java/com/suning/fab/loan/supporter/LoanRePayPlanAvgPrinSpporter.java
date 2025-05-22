package com.suning.fab.loan.supporter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbillplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.VarChecker;
@Scope("prototype")
@Repository
/** 等额本金  repayWay：2 */
public class LoanRePayPlanAvgPrinSpporter implements LoanRePayPlanSupporter {

	private boolean flag = true;
	
	@Override
	public List<RePayPlan> genRepayPlan(LoanAgreement la, TblLnsbasicinfo lnsbasicinfo,
			LnsBillStatistics lnsBillStatistics, List<TblLnsbillplan> tblBillPlanList, TranCtx ctx) {
		
		Map<String,RePayPlan> rfMap = new LinkedHashMap<String, RePayPlan>();
		
		dealLnsBillA(lnsbasicinfo, lnsBillStatistics.getHisBillList(),   rfMap);
		dealLnsBillA(lnsbasicinfo, lnsBillStatistics.getHisSetIntBillList(),   rfMap);
		dealLnsBillA(lnsbasicinfo, lnsBillStatistics.getBillInfoList(),   rfMap);
		dealLnsBillA(lnsbasicinfo, lnsBillStatistics.getFutureOverDuePrinIntBillList(),   rfMap);
		dealLnsBillA(lnsbasicinfo, lnsBillStatistics.getFutureBillInfoList(),   rfMap);
		
		for (TblLnsbillplan lnsBillPlan: tblBillPlanList)
		{
			String key = lnsBillPlan.getPeriod().toString();
			RePayPlan rePayPlan = rfMap.get(key);
			if(rePayPlan == null){
				//账单和未来期没有，说明提前结清，结清日期取辅助表交易日期
				rePayPlan = new RePayPlan();
				if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat()))
				{
					rePayPlan.setTermretdate(lnsbasicinfo.getModifydate());
				}
				rfMap.put(key, rePayPlan);
			}

			//如果结清日期为空，且已结清，赋值主文件modifyDate
			if (VarChecker.isEmpty(rePayPlan.getTermretdate()) && ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat()))
			{
				rePayPlan.setTermretdate(lnsbasicinfo.getModifydate());
			}
			
			if (ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBillPlan.getBilltype()))
			{
				rePayPlan.setTermretprin(new FabAmount(lnsBillPlan.getBillamt()));
				rePayPlan.setBalance(new FabAmount(lnsBillPlan.getPrinbal()));
			}
			
			if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBillPlan.getBilltype()))
			{//过滤到期还本还息应还利息
				if(ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(lnsbasicinfo.getRepayway())){
					continue;
				}
				rePayPlan.setTermretint(new FabAmount(lnsBillPlan.getBillamt()));
				rePayPlan.setNoretint(new FabAmount(new FabAmount(lnsBillPlan.getBillamt()).sub(rePayPlan.getIntAmt()).getVal()));//未还利息
			}

			rePayPlan.setBrc(lnsbasicinfo.getOpenbrc());//公司代码
			rePayPlan.setAcctno(lnsbasicinfo.getAcctno());//账号
			rePayPlan.setRepayterm(lnsBillPlan.getPeriod());//期数
			rePayPlan.setRepayintbdate(lnsBillPlan.getBegindate());//本期起日
			rePayPlan.setRepayintedate(lnsBillPlan.getEnddate());//本期止日
			rePayPlan.setRepayownbdate(lnsBillPlan.getEnddate());//还款起日
			rePayPlan.setRepayownedate(formateDate(lnsBillPlan.getEnddate(), lnsbasicinfo.getGracedays()));//还款止日
			rePayPlan.setDays(CalendarUtil.actualDaysBetween(rePayPlan.getRepayintbdate(), rePayPlan.getRepayintedate()));//计息天数
		}
		
		List<RePayPlan> planList = new ArrayList<RePayPlan>();

		for(Entry<String, RePayPlan> m:rfMap.entrySet()){
			if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(m.getValue().getSettleflag())
					|| VarChecker.isEmpty(m.getValue().getSettleflag())){
				m.getValue().setNoretamt(new FabAmount());
				m.getValue().setNoretint(new FabAmount());
			}
			
			if(VarChecker.isEmpty(m.getValue().getSettleflag())){
				m.getValue().setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				m.getValue().setTermstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
			}
			
			//等本等息计提利息为未还利息
			if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
				m.getValue().setTermcdint(m.getValue().getNoretint());
			}
			else {
				//计提利息-已还利息
				m.getValue().setTermcdint(new FabAmount(m.getValue().getTermcdint().sub(m.getValue().getIntAmt()).getVal()));//本期未还罚息
			}

			planList.add(m.getValue());
		}
		
		Collections.sort(planList,new Comparator<RePayPlan>(){
			@Override
            public int compare(RePayPlan arg0, RePayPlan arg1) {
                return arg0.getRepayterm().compareTo(arg1.getRepayterm());
            }
        });
		
		return planList;
	}
	
	public  void dealLnsBillA(TblLnsbasicinfo lnsbasicinfo, List<LnsBill> billList, Map<String,RePayPlan> rfMap){
		
		for (LnsBill lnsBill:billList)
		{
			String key = lnsBill.getPeriod().toString();
			RePayPlan rePayPlan = rfMap.get(key);
			if (null == rePayPlan){
				flag = true;
				rePayPlan = new RePayPlan();
			}
			dealLnsBill(lnsBill, lnsbasicinfo, rePayPlan);
			rfMap.put(key, rePayPlan);
		}
	}
	
	public void dealLnsBill(LnsBill bill,  TblLnsbasicinfo lnsbasicinfo, RePayPlan rePayPlan){
		
		settleCheck(bill, rePayPlan);//检查账单是否结清
		rePayPlan.setBrc(lnsbasicinfo.getOpenbrc());//公司代码
		rePayPlan.setDeductionamt(new FabAmount(lnsbasicinfo.getDeductionamt()));//扣息金额
		rePayPlan.setAcctno(lnsbasicinfo.getAcctno());//账号
		rePayPlan.setRepayterm(bill.getPeriod());//期数
		
		//未来期账单最后交易日期空
		//还款日期取值，取账单表最后交易日期，同期多次还款，取最后一次还款日
		if (!VarChecker.isEmpty(bill.getLastDate()) && 
				!bill.getBillAmt().equals(bill.getBillBal()))
		{
			rePayPlan.setTermretdate(datemax(rePayPlan.getTermretdate(), bill.getLastDate()));//本期还款日
		}
		
		if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(bill.getBillType())){

			rePayPlan.setTermstatus(bill.getBillStatus());//账单状态
			rePayPlan.setRepayintbdate(datemin(rePayPlan.getRepayintbdate(), bill.getStartDate()));//本期起日
			rePayPlan.setRepayintedate(datemax(rePayPlan.getRepayintedate(), bill.getEndDate()));//本期止日
			rePayPlan.setRepayownbdate(datemax(rePayPlan.getRepayownbdate(), bill.getEndDate()));//还款起日
			rePayPlan.setRepayownedate(datemax(rePayPlan.getRepayownedate(), formateDate(bill.getEndDate(), lnsbasicinfo.getGracedays())));//还款止日
			
			if (ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET.equals(bill.getBillProperty()))
			{
				//应还本金
				if (rePayPlan.getTermretprin() == null )
				{
					rePayPlan.setTermretprin(bill.getBillAmt());//本期应还本金
				}
				else
				{
					rePayPlan.getTermretprin().selfAdd(bill.getBillAmt());//本期应还本金
				}
				
				rePayPlan.setBalance(bill.getPrinBal());//贷款剩余本金
			}
			else {
				if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
					rePayPlan.getTermretprin().selfAdd(bill.getBillAmt());//本期应还本金
					rePayPlan.setBalance(bill.getPrinBal());//贷款剩余本金
				}
			}
			//已还本金
			if (rePayPlan.getPrinAmt() == null)
			{
				rePayPlan.setPrinAmt(new FabAmount(bill.getBillAmt().sub(bill.getBillBal()).getVal()));//已还本金
			}
			else
			{
				rePayPlan.getPrinAmt().selfAdd(bill.getBillAmt());//已还本金
				rePayPlan.getPrinAmt().selfSub(bill.getBillBal());//已还本金
			}
			//未还本金
			if (rePayPlan.getNoretamt() == null)
			{
				rePayPlan.setNoretamt(new FabAmount(bill.getBillBal().getVal()));//未还本金
			}
			else
			{
				rePayPlan.getNoretamt().selfAdd(bill.getBillBal());//未还本金
			}
			
			flag = false;
		}
		else if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
		
			rePayPlan.setTermstatus(bill.getBillStatus());//账单状态
			if(flag){
				rePayPlan.setRepayintbdate(datemin(rePayPlan.getRepayintbdate(), bill.getStartDate()));//本期起日
				rePayPlan.setRepayintedate(datemax(rePayPlan.getRepayintedate(), bill.getEndDate()));//本期止日
				rePayPlan.setRepayownbdate(datemax(rePayPlan.getRepayownbdate(), bill.getEndDate()));//还款起日
				rePayPlan.setRepayownedate(datemax(rePayPlan.getRepayownedate(), formateDate(bill.getEndDate(), lnsbasicinfo.getGracedays())));//还款止日
			}
			//应还利息
			if (rePayPlan.getTermretint() == null)
			{
				rePayPlan.setTermretint(bill.getBillAmt());//本期应还利息
			}
			else
			{
				rePayPlan.getTermretint().selfAdd(bill.getBillAmt());//本期应还利息
			}
			//已还利息
			if (rePayPlan.getIntAmt() == null)
			{
				rePayPlan.setIntAmt(new FabAmount(bill.getBillAmt().sub(bill.getBillBal()).getVal()));//已还利息
			}
			else
			{
				rePayPlan.getIntAmt().selfAdd(bill.getBillAmt());//已还利息
				rePayPlan.getIntAmt().selfSub(bill.getBillBal());//已还利息
			}
			//未还利息
			if (rePayPlan.getNoretint() == null)
			{
				rePayPlan.setNoretint(new FabAmount(bill.getBillBal().getVal()));//未还利息
			}
			else
			{
				rePayPlan.getNoretint().selfAdd(bill.getBillBal());//未还利息
			}
			//本期计提利息
			if(!VarChecker.isEmpty(bill.getRepayDateInt())){
				rePayPlan.getTermcdint().selfAdd(bill.getRepayDateInt());//本期计提利息
			}
		}
		else if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT, ConstantDeclare.BILLTYPE.BILLTYPE_CINT, ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
			
			//累计罚息
			if (rePayPlan.getSumfint() == null)
			{
				rePayPlan.setSumfint(bill.getBillAmt());//累计罚息
			}
			else
			{
				rePayPlan.getSumfint().selfAdd(bill.getBillAmt());//累计罚息
			}
			//已还罚息
			if (rePayPlan.getSumrfint() == null)
			{
				rePayPlan.setSumrfint(new FabAmount(bill.getBillAmt().sub(bill.getBillBal()).getVal()));//已还罚息
			}
			else
			{
				rePayPlan.getSumrfint().selfAdd(bill.getBillAmt());//已还罚息
				rePayPlan.getSumrfint().selfSub(bill.getBillBal());//已还罚息
			}
			//本期未还罚息
			rePayPlan.setTermfint(new FabAmount(rePayPlan.getSumfint().sub(rePayPlan.getSumrfint()).getVal()));//本期未还罚息
		}
		else if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(bill.getBillType())){
			rePayPlan.setTermcint(new FabAmount());
		}
		
		rePayPlan.setDays(CalendarUtil.actualDaysBetween(rePayPlan.getRepayintbdate(), rePayPlan.getRepayintedate()));//计息天数
		
	}
	
	public static void settleCheck(LnsBill bill, RePayPlan rePayPlan){
		if(VarChecker.isEmpty(rePayPlan.getSettleflag())
				|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING).contains(bill.getSettleFlag())){
			rePayPlan.setSettleflag(bill.getSettleFlag());
		}
	}
	
	private String formateDate(String day,Integer n){
		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormater.format(CalendarUtil.nDaysAfter(day, n).toDate());
	}

	public static String strval(String str){
		if(VarChecker.isEmpty(str)){
			return "";
		}else{
			return str;
		}
	}

	public static String datemin(String bstr, String astr){
		if(VarChecker.isEmpty(bstr)){
			return astr==null?"":astr;
		}else{
			return CalendarUtil.before(strval(bstr), strval(astr))?bstr:astr;
		}
	}

	public static String datemax(String bstr, String astr){
		if(VarChecker.isEmpty(bstr)){
			return astr==null?"":astr;
		}else{
			return CalendarUtil.after(strval(bstr), strval(astr))?bstr:astr;
		}
	}

}

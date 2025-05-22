package com.suning.fab.loan.supporter;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.suning.fab.tup4j.utils.ServiceFactory;
//@Resource(name="studentDao")
@Component
@Scope("singleton")
public class LoanSupporterUtil {

//	Map<String,RepayFormulaSupporter> formulas;
	Map<String,RepayWaySupporter> repayWays;
	Map<String,SettleInterestSupporter> prinSettleInterest;
	Map<String,SettleInterestSupporter> intSettleInterest;
	Map<String,TransferSupporter> prinAcctTransfer;
	Map<String,TransferSupporter> intAcctTransfer;
	Map<String,TransferSupporter> feeAcctTransfer;
	Map<String,LoanRePayPlanSupporter> repayPlanRepayWay;
	Map<String,PrepayTermSupporter> prepayrepayway;
	
//	public static RepayFormulaSupporter getFormulaSupporter(String formula){
//		return ServiceFactory.getBean(LoanSupporterUtil.class).formulas.get(formula);
//	}
	public static RepayWaySupporter getRepayWaySupporter(String repayWay){
		return ServiceFactory.getBean(LoanSupporterUtil.class).repayWays.get(repayWay);
	}

	public static LoanRePayPlanSupporter getRepayPlanRepayWaySupporter(String repayWay){
		return ServiceFactory.getBean(LoanSupporterUtil.class).repayPlanRepayWay.get(repayWay);
	}
	
	public static PrepayTermSupporter getPrepayRepaywaySupporter(String repayWay){
		return ServiceFactory.getBean(LoanSupporterUtil.class).prepayrepayway.get(repayWay);
	}
	
//	public static Map<String, RepayFormulaSupporter> getFormulas() {
//		return ServiceFactory.getBean(LoanSupporterUtil.class).formulas;
//	}
	public static SettleInterestSupporter getPrinSettleInterest(String loanForm) {
		return ServiceFactory.getBean(LoanSupporterUtil.class).prinSettleInterest.get(loanForm);
	}
	public static SettleInterestSupporter getIntSettleInterest(String loanForm) {
		return ServiceFactory.getBean(LoanSupporterUtil.class).intSettleInterest.get(loanForm);
	}
	public static TransferSupporter getPrinAcctTransfer(String loanForm) {
		return ServiceFactory.getBean(LoanSupporterUtil.class).prinAcctTransfer.get(loanForm);
	}
	public static TransferSupporter getIntAcctTransfer(String loanForm) {
		return ServiceFactory.getBean(LoanSupporterUtil.class).intAcctTransfer.get(loanForm);
	}
	public static TransferSupporter getFeeAcctTransfer(String loanForm) {
		return ServiceFactory.getBean(LoanSupporterUtil.class).feeAcctTransfer.get(loanForm);
	}
//	@Resource(name="formulas")
//	public void setFormulas(Map<String, RepayFormulaSupporter> formulas) {
//		this.formulas = formulas;
//	}
	
	@Resource(name="repayWay")
	public void setRepayWay(Map<String, RepayWaySupporter> repayWay) {
		this.repayWays = repayWay;
	}
	
	@Resource(name="repayPlanRepayWay")
	public void setRepayPlanRepayWay(Map<String, LoanRePayPlanSupporter> repayWay) {
		this.repayPlanRepayWay = repayWay;
	}
	
	@Resource(name="prepayrepayway")
	public void setPrepayRepayway(Map<String, PrepayTermSupporter> repayWay) {
		this.prepayrepayway = repayWay;
	}
	
	@Resource(name="prinSettleInterest")
	public void setPrinSettleInterest(Map<String, SettleInterestSupporter> prinSettleInterest) {
		this.prinSettleInterest = prinSettleInterest;
	}
	
	@Resource(name="intSettleInterest")
	public void setIntSettleInterest(Map<String, SettleInterestSupporter> intSettleInterest) {
		this.intSettleInterest = intSettleInterest;
	}
	@Resource(name="prinAcctTransfer")
	public void setPrinAcctTransfer(Map<String, TransferSupporter> prinAcctTransfer) {
		this.prinAcctTransfer = prinAcctTransfer;
	}
	@Resource(name="intAcctTransfer")
	public void setIntAcctTransfer(Map<String, TransferSupporter> intAcctTransfer) {
		this.intAcctTransfer = intAcctTransfer;
	}
	@Resource(name="feeAcctTransfer")
	public void setFeeAcctTransfer(Map<String, TransferSupporter> feeAcctTransfer) {
		this.feeAcctTransfer = feeAcctTransfer;
	}
	
}

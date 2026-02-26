package com.suning.fab.loan.supporter;

import com.suning.fab.loan.account.IntegerNum;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.FabException;

public  class RepayFormulaSupporterProducter {

	public static RepayFormulaSupporter producter(LoanAgreement loanAgreement) throws FabException
	{
		RepayFormulaSupporter supporter = null;

		if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_EQPRINEQINT))
		{
			supporter = new RepayFormulaEqPrinEqIntSupporter();
			supporter.setLoanAgreement(loanAgreement);
		}
		else if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_EQPRININT))
		{
			supporter = new RepayFormulaEqPrinAndIntSupporter();
			supporter.setLoanAgreement(loanAgreement);
		}
		else if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_OTHER))
		{
			supporter = new RepayFormulaCommonSupporter();
			supporter.setLoanAgreement(loanAgreement);
		}
		else if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_USERDEFINE))
		{
			supporter = new RepayFormulaUserDefineSupporter();
			supporter.setLoanAgreement(loanAgreement);
		}
		//2019-09-26 气球贷
		else if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_BOLLOON))
		{
			supporter = new RepayFormulaBalloonSupporter();
			supporter.setLoanAgreement(loanAgreement);
		}
		//2020-05-12任性付等本等息 user:chenchao
		else if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.RORMULA_WAYWARD)){
			supporter = new RepayFormulaWayWardSupporter();
			supporter.setLoanAgreement(loanAgreement);
		}else if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_EQPRININTFEE)){
			//等额本息费还款方式
			supporter = new RepayFormulaEqPrinIntFeeSupporter();
			supporter.setLoanAgreement(loanAgreement);
		}
		return supporter;
	}
}  
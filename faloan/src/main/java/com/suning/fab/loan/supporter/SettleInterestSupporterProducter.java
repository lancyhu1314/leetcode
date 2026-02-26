package com.suning.fab.loan.supporter;

import com.suning.fab.loan.utils.ConstantDeclare;

public abstract class SettleInterestSupporterProducter {  

	public static SettleInterestSupporter intProducter(String loanForm)
	{
		SettleInterestSupporter settleInterestSupporter = null;
		if (loanForm.equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE))
		{
			settleInterestSupporter = new SettleInterestIntAmltSupporter();
		}
		else if (loanForm.equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU))
		{
			settleInterestSupporter = new SettleInterestIntCintSupporter();
		}
		return settleInterestSupporter;
	}
	public static SettleInterestSupporter prinProducter(String loanForm)
	{
		SettleInterestSupporter settleInterestSupporter = null;
		if (loanForm.equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE))
		{
			settleInterestSupporter = new SettleInterestPrinGintSupporter();
		}
		else if (loanForm.equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU))
		{
			settleInterestSupporter = new SettleInterestPrinDintSupporter();
		}
		else if (loanForm.equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL))
		{
			settleInterestSupporter = new SettleInterestPrinIntSupporter();
		}
		return settleInterestSupporter;
	}

	public static SettleInterestSupporter feeProducter(String loanForm)
	{
		SettleInterestSupporter settleInterestSupporter = null;
		 if (loanForm.equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU))
		{
			settleInterestSupporter = new SettleInterestFeePenaltySupporter();
		}
		return settleInterestSupporter;
	}
	public static SettleInterestSupporter capProducter(String loanForm)
	{
		SettleInterestSupporter settleInterestSupporter = null;
		if (loanForm.equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE))
		{
			settleInterestSupporter = new SettleInterestPrinGintSupporter();
		}
		else if (loanForm.equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU))
		{
			settleInterestSupporter = new SettleInterestPrinCapDintSupporter();
		}
		return settleInterestSupporter;
	}
}  
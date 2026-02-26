package com.suning.fab.loan.supporter;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LoanFormInfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;

public abstract class SettleInterestSupporter {  

	private LoanFormInfo loanFormInfo;
	
	public abstract LnsBill settleInterest(LoanAgreement loanAgreement,LnsBill hisBill,String repayDate) throws FabException;
	
	protected LnsBill createLnsBill(LnsBill hisBill,String repayDate)
	{
		LnsBill lnsBill = new LnsBill();
		lnsBill.setPeriod(hisBill.getPeriod());//期数
		lnsBill.setStartDate(hisBill.getIntendDate());
		//如果利息记至日期早于当前账单结束日期，用当前账单结束日期作为罚息复利账单开始日期
		if (CalendarUtil.before(hisBill.getIntendDate(), hisBill.getCurendDate()))
		{
			lnsBill.setStartDate(hisBill.getCurendDate());
		}
		
		lnsBill.setEndDate(repayDate);
		lnsBill.setCurendDate(lnsBill.getCurendDate());
		lnsBill.setDerTranDate(hisBill.getTranDate());//账务日期
		lnsBill.setDerSerseqno(hisBill.getSerSeqno());//流水号
		lnsBill.setDerTxSeq(hisBill.getTxSeq());//子序号
		lnsBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);//账单状态N正常G宽限期O逾期L呆滞B呆账
		lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET);//账单属性INTSET正常结息REPAY还款
		lnsBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);//利息入账标志NO未入YES已入
		lnsBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);//账单作废标志NORMAL正常CANCEL作废
		lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);//结清标志RUNNING未结CLOSE已结
		lnsBill.setRepayendDate(lnsBill.getEndDate());
		lnsBill.setIntendDate(lnsBill.getEndDate());
		lnsBill.setPrinBal(new FabAmount(hisBill.getBillBal().getVal()));//账单对应本金余额
		
		return lnsBill;
	}

	public LoanFormInfo getLoanFormInfo() {
		return loanFormInfo;
	}

	public void setLoanFormInfo(LoanFormInfo loanFormInfo) {
		this.loanFormInfo = loanFormInfo;
	}
}  
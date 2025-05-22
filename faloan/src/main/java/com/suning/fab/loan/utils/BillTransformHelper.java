package com.suning.fab.loan.utils;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.VarChecker;

public class BillTransformHelper {
	private BillTransformHelper(){
		//nothing to do
	}
	/**
	 * 转为数据表表单格式
	 * @param lnsBill
	 * @return
	 */
	public static TblLnsbill convertToTblLnsBill(LoanAgreement la,LnsBill lnsBill,TranCtx ctx)
	{
		TblLnsbill tbbill = new TblLnsbill();
		
		MapperUtil.iomap(lnsBill, tbbill);
		tbbill.setAcctno(la.getContract().getReceiptNo());
		tbbill.setBrc(ctx.getBrc());
		tbbill.setBillamt(lnsBill.getBillAmt().getVal());//账单金额
		tbbill.setBillbal(lnsBill.getBillBal().getVal());//账单余额
		if (!VarChecker.isEmpty(lnsBill.getPrinBal()))
		{
			
			tbbill.setPrinbal(lnsBill.getPrinBal().getVal());//账单对应本金余额
		}
		if (!VarChecker.isEmpty(lnsBill.getBillRate()))
		{
		tbbill.setBillrate(lnsBill.getBillRate().getYearRate().doubleValue());//账单执行利率
		}
		if (!VarChecker.isEmpty(lnsBill.getAccumulate()))
		{
			
			tbbill.setAccumulate(lnsBill.getAccumulate().getVal());//罚息/复利积数
		}
		
		tbbill.setBegindate(lnsBill.getStartDate());//账单起始日期
		tbbill.setRepayedate(lnsBill.getRepayendDate());//账单应还款止日
		tbbill.setIntedate(lnsBill.getIntendDate());
		tbbill.setSettledate(lnsBill.getSettleDate());
		tbbill.setStatusbdate(lnsBill.getStatusbDate());
		tbbill.setCcy(lnsBill.getCcy());
		return tbbill;
	}
	/**
	 * 转为应用层格式
	 * @param TblLnsbill
	 * @return lnsBill
	 */
	public static LnsBill convertToLnsBill(TblLnsbill tbBill)
	{
		LnsBill lnsBill = new LnsBill();
		
		lnsBill.setTranDate(tbBill.getTrandate());
		lnsBill.setSerSeqno(tbBill.getSerseqno());
		lnsBill.setTxSeq(tbBill.getTxseq());
		lnsBill.setBillType(tbBill.getBilltype());//账单类型PRIN本金NINT利息DINT罚息CINT复利
		lnsBill.setPeriod(tbBill.getPeriod());//期数
		lnsBill.setBillAmt(new FabAmount(tbBill.getBillamt()));//账单金额
		lnsBill.setBillBal(new FabAmount(tbBill.getBillbal()));//账单余额
		lnsBill.setPrinBal(new FabAmount(tbBill.getPrinbal()));//账单对应本金余额
		lnsBill.setBillRate(new FabRate(tbBill.getBillrate()));//账单执行利率
		lnsBill.setAccumulate(new FabAmount(tbBill.getAccumulate()));//罚息/复利积数
		lnsBill.setStartDate(tbBill.getBegindate());//账单起始日期
		lnsBill.setEndDate(tbBill.getEnddate());//账单结束日期
		lnsBill.setCurendDate(tbBill.getCurenddate());//账单当期结束日期
		lnsBill.setRepayendDate(tbBill.getRepayedate());//账单应还款止日
		lnsBill.setLastDate(tbBill.getLastdate());//最后交易日期
		lnsBill.setSettleDate(tbBill.getSettledate());//账单结清日期
		lnsBill.setIntendDate(tbBill.getIntedate());//利息计止日期
		lnsBill.setFintendDate(tbBill.getFintedate());//罚息计止日期
		lnsBill.setCintendDate(tbBill.getCintedate());//复利计至日期
		lnsBill.setStatusbDate(tbBill.getStatusbdate());
		lnsBill.setDerTranDate(tbBill.getDertrandate());//账务日期
		lnsBill.setDerSerseqno(tbBill.getDerserseqno());//流水号
		lnsBill.setDerTxSeq(tbBill.getDertxseq());//子序号
		lnsBill.setBillStatus(tbBill.getBillstatus());//账单状态N正常G宽限期O逾期L呆滞B呆账
		lnsBill.setBillProperty(tbBill.getBillproperty());//账单属性INTSET正常结息REPAY还款
		lnsBill.setIntrecordFlag(tbBill.getIntrecordflag());//利息入账标志NO未入YES已入
		lnsBill.setCancelFlag(tbBill.getCancelflag());//账单作废标志NORMAL正常CANCEL作废
		lnsBill.setSettleFlag(tbBill.getSettleflag());//结清标志RUNNING未结CLOSE已结
		lnsBill.setCcy(tbBill.getCcy());
		lnsBill.setRepayWay(tbBill.getRepayway());
		return lnsBill;
	}
}

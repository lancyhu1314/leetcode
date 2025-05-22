package com.suning.fab.loan.bo;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;

public class LnsBillStatistics {

	private Integer intTotalPeriod;
	private Integer prinTotalPeriod;
	private String  firstRepayPrinDate;
	private String  firstRepayIntDate;
	//合同金额
	private FabAmount totalPrin = new FabAmount();
	//总利息：包括已还和未还
	private FabAmount totalInt = new FabAmount();
	//已还本金
	private FabAmount repayPrin = new FabAmount();
	//未还本金
	private FabAmount unrepayPrin = new FabAmount();
	//已还利息
	private FabAmount paidInterest = new FabAmount();
	//未还利息
	private FabAmount unpaidInterest = new FabAmount();
	//已还罚息
	private FabAmount paidforfeitInterest = new FabAmount();
	//未还罚息
	private FabAmount unpaidforfeitInterest = new FabAmount();
	//已还复利
	private FabAmount paidCompoundInterest = new FabAmount();
	//未还复利
	private FabAmount unpaidCompoundInterest = new FabAmount();
	//历史账单
	private List<LnsBill> hisBillList = new ArrayList<LnsBill>();
	//历史未结清账单结息账单
	private List<LnsBill> hisSetIntBillList= new ArrayList<LnsBill>();
	//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
	private List<LnsBill> billInfoList = new ArrayList<LnsBill>();
	//未来期账单：从还款日到合同到期日之间的本金和利息账单
	private List<LnsBill> futureBillInfoList = new ArrayList<LnsBill>();
	//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
	private List<LnsBill> futureOverDuePrinIntBillList = new ArrayList<LnsBill>();

	//未落表的呆滞呆账的罚息复利
	private List<LnsBill> cdbillList = new ArrayList<LnsBill>();

	//账单序号，对应账单表中的子序号
	private Integer billNo = 0;

	private Integer currHisSetIntBillNo=0;
	private Integer currBillNo=0;
	private Integer currFutureBillNo=0;
	private Integer currfutureOverDuePrinIntBillNo=0;

	public Boolean hasHisSetIntBill()
	{
		if (currHisSetIntBillNo < hisSetIntBillList.size() )
		{
			currHisSetIntBillNo++;
			return true;
		}
		return false;
	}
	public Boolean hasBill()
	{
		if (currBillNo < billInfoList.size() )
		{
			currBillNo++;
			return true;
		}
		return false;
	}
	public Boolean hasFutureBill()
	{
		if (currFutureBillNo < futureBillInfoList.size() )
		{
			currFutureBillNo++;
			return true;
		}
		return false;
	}
	public Boolean hasfutureOverDuePrinBill()
	{
		if (currfutureOverDuePrinIntBillNo < futureOverDuePrinIntBillList.size() )
		{
			currfutureOverDuePrinIntBillNo++;
			return true;
		}
		return false;
	}

	public LnsBill getHisSetIntBill()
	{
		return hisSetIntBillList.get(currHisSetIntBillNo-1);
	}
	public LnsBill getBill()
	{
		return billInfoList.get(currBillNo-1);
	}
	public LnsBill getFutureBill()
	{
		return futureBillInfoList.get(currFutureBillNo-1);
	}
	public LnsBill getFutureOverDuePrinIntBill()
	{
		return futureOverDuePrinIntBillList.get(currfutureOverDuePrinIntBillNo-1);
	}

	//写当前账单
	public void writeBill(LoanAgreement la,TranCtx ctx,LnsBill lnsBill) throws FabException
	{
		TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(la,lnsBill,ctx);
		tblLnsbill.setTrandate(Date.valueOf(ctx.getTranDate()));
		tblLnsbill.setSerseqno(ctx.getSerSeqNo());
		lnsBill.setTranDate(tblLnsbill.getTrandate());
		lnsBill.setSerSeqno(ctx.getSerSeqNo());

		//2018-12-07  零利率利息账本
		if ( !lnsBill.getBillAmt().isPositive() )
		{
			if(LoanFeeUtils.isFeeType(lnsBill.getBillType())){
				//费用零金额账本 不需要结清
				tblLnsbill.setSettleflag("RUNNING");
			}else{
				tblLnsbill.setSettleflag("CLOSE");
			}

		}


		if (!"AMLT".equals(lnsBill.getBillType()))
		{
			try{
				DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS100", "lnsbill");
			}
		}
	}
	@Override
	public String toString() {
		return "LnsBillStatistics [intTotalPeriod=" + intTotalPeriod
				+ ", prinTotalPeriod=" + prinTotalPeriod
				+ ", firstRepayPrinDate=" + firstRepayPrinDate
				+ ", firstRepayIntDate=" + firstRepayIntDate + ", totalPrin="
				+ totalPrin + ", totalInt=" + totalInt + ", repayPrin="
				+ repayPrin + ", unrepayPrin=" + unrepayPrin
				+ ", paidInterest=" + paidInterest + ", unpaidInterest="
				+ unpaidInterest + ", paidforfeitInterest="
				+ paidforfeitInterest + ", unpaidforfeitInterest="
				+ unpaidforfeitInterest + ", paidCompoundInterest="
				+ paidCompoundInterest + ", unpaidCompoundInterest="
				+ unpaidCompoundInterest + ", hisBillList=" + hisBillList
				+ ", hisSetIntBillList=" + hisSetIntBillList
				+ ", billInfoList=" + billInfoList + "]";
	}
	/**
	 * @return the intTotalPeriod
	 */
	public Integer getIntTotalPeriod() {
		return intTotalPeriod;
	}
	/**
	 * @param intTotalPeriod the intTotalPeriod to set
	 */
	public void setIntTotalPeriod(Integer intTotalPeriod) {
		this.intTotalPeriod = intTotalPeriod;
	}
	/**
	 * @return the prinTotalPeriod
	 */
	public Integer getPrinTotalPeriod() {
		return prinTotalPeriod;
	}
	/**
	 * @param prinTotalPeriod the prinTotalPeriod to set
	 */
	public void setPrinTotalPeriod(Integer prinTotalPeriod) {
		this.prinTotalPeriod = prinTotalPeriod;
	}
	/**
	 * @return the firstRepayPrinDate
	 */
	public String getFirstRepayPrinDate() {
		return firstRepayPrinDate;
	}
	/**
	 * @param firstRepayPrinDate the firstRepayPrinDate to set
	 */
	public void setFirstRepayPrinDate(String firstRepayPrinDate) {
		this.firstRepayPrinDate = firstRepayPrinDate;
	}
	/**
	 * @return the firstRepayIntDate
	 */
	public String getFirstRepayIntDate() {
		return firstRepayIntDate;
	}
	/**
	 * @param firstRepayIntDate the firstRepayIntDate to set
	 */
	public void setFirstRepayIntDate(String firstRepayIntDate) {
		this.firstRepayIntDate = firstRepayIntDate;
	}
	/**
	 * @return the totalPrin
	 */
	public FabAmount getTotalPrin() {
		return totalPrin;
	}
	/**
	 * @param totalPrin the totalPrin to set
	 */
	public void setTotalPrin(FabAmount totalPrin) {
		this.totalPrin = totalPrin;
	}
	/**
	 * @return the totalInt
	 */
	public FabAmount getTotalInt() {
		return totalInt;
	}
	/**
	 * @param totalInt the totalInt to set
	 */
	public void setTotalInt(FabAmount totalInt) {
		this.totalInt = totalInt;
	}
	/**
	 * @return the repayPrin
	 */
	public FabAmount getRepayPrin() {
		return repayPrin;
	}
	/**
	 * @param repayPrin the repayPrin to set
	 */
	public void setRepayPrin(FabAmount repayPrin) {
		this.repayPrin = repayPrin;
	}
	/**
	 * @return the unrepayPrin
	 */
	public FabAmount getUnrepayPrin() {
		return unrepayPrin;
	}
	/**
	 * @param unrepayPrin the unrepayPrin to set
	 */
	public void setUnrepayPrin(FabAmount unrepayPrin) {
		this.unrepayPrin = unrepayPrin;
	}
	/**
	 * @return the paidInterest
	 */
	public FabAmount getPaidInterest() {
		return paidInterest;
	}
	/**
	 * @param paidInterest the paidInterest to set
	 */
	public void setPaidInterest(FabAmount paidInterest) {
		this.paidInterest = paidInterest;
	}
	/**
	 * @return the unpaidInterest
	 */
	public FabAmount getUnpaidInterest() {
		return unpaidInterest;
	}
	/**
	 * @param unpaidInterest the unpaidInterest to set
	 */
	public void setUnpaidInterest(FabAmount unpaidInterest) {
		this.unpaidInterest = unpaidInterest;
	}
	/**
	 * @return the paidforfeitInterest
	 */
	public FabAmount getPaidforfeitInterest() {
		return paidforfeitInterest;
	}
	/**
	 * @param paidforfeitInterest the paidforfeitInterest to set
	 */
	public void setPaidforfeitInterest(FabAmount paidforfeitInterest) {
		this.paidforfeitInterest = paidforfeitInterest;
	}
	/**
	 * @return the unpaidforfeitInterest
	 */
	public FabAmount getUnpaidforfeitInterest() {
		return unpaidforfeitInterest;
	}
	/**
	 * @param unpaidforfeitInterest the unpaidforfeitInterest to set
	 */
	public void setUnpaidforfeitInterest(FabAmount unpaidforfeitInterest) {
		this.unpaidforfeitInterest = unpaidforfeitInterest;
	}
	/**
	 * @return the paidCompoundInterest
	 */
	public FabAmount getPaidCompoundInterest() {
		return paidCompoundInterest;
	}
	/**
	 * @param paidCompoundInterest the paidCompoundInterest to set
	 */
	public void setPaidCompoundInterest(FabAmount paidCompoundInterest) {
		this.paidCompoundInterest = paidCompoundInterest;
	}
	/**
	 * @return the unpaidCompoundInterest
	 */
	public FabAmount getUnpaidCompoundInterest() {
		return unpaidCompoundInterest;
	}
	/**
	 * @param unpaidCompoundInterest the unpaidCompoundInterest to set
	 */
	public void setUnpaidCompoundInterest(FabAmount unpaidCompoundInterest) {
		this.unpaidCompoundInterest = unpaidCompoundInterest;
	}
	/**
	 * @return the hisBillList
	 */
	public List<LnsBill> getHisBillList() {
		return hisBillList;
	}
	/**
	 * @param hisBillList the hisBillList to set
	 */
	public void setHisBillList(List<LnsBill> hisBillList) {
		this.hisBillList = hisBillList;
	}
	/**
	 * @return the hisSetIntBillList
	 */
	public List<LnsBill> getHisSetIntBillList() {
		return hisSetIntBillList;
	}
	/**
	 * @param hisSetIntBillList the hisSetIntBillList to set
	 */
	public void setHisSetIntBillList(List<LnsBill> hisSetIntBillList) {
		this.hisSetIntBillList = hisSetIntBillList;
	}
	/**
	 * @return the billInfoList
	 */
	public List<LnsBill> getBillInfoList() {
		return billInfoList;
	}
	/**
	 * @param billInfoList the billInfoList to set
	 */
	public void setBillInfoList(List<LnsBill> billInfoList) {
		this.billInfoList = billInfoList;
	}
	/**
	 * @return the futureBillInfoList
	 */
	public List<LnsBill> getFutureBillInfoList() {
		return futureBillInfoList;
	}
	/**
	 * @param futureBillInfoList the futureBillInfoList to set
	 */
	public void setFutureBillInfoList(List<LnsBill> futureBillInfoList) {
		this.futureBillInfoList = futureBillInfoList;
	}
	/**
	 * @return the futureOverDuePrinIntBillList
	 */
	public List<LnsBill> getFutureOverDuePrinIntBillList() {
		return futureOverDuePrinIntBillList;
	}
	/**
	 * @param futureOverDuePrinIntBillList the futureOverDuePrinIntBillList to set
	 */
	public void setFutureOverDuePrinIntBillList(List<LnsBill> futureOverDuePrinIntBillList) {
		this.futureOverDuePrinIntBillList = futureOverDuePrinIntBillList;
	}
	/**
	 * @return the billNo
	 */
	public Integer getBillNo() {
		return billNo;
	}
	/**
	 * @param billNo the billNo to set
	 */
	public void setBillNo(Integer billNo) {
		this.billNo = billNo;
	}
	/**
	 * @return the currHisSetIntBillNo
	 */
	public Integer getCurrHisSetIntBillNo() {
		return currHisSetIntBillNo;
	}
	/**
	 * @param currHisSetIntBillNo the currHisSetIntBillNo to set
	 */
	public void setCurrHisSetIntBillNo(Integer currHisSetIntBillNo) {
		this.currHisSetIntBillNo = currHisSetIntBillNo;
	}
	/**
	 * @return the currBillNo
	 */
	public Integer getCurrBillNo() {
		return currBillNo;
	}
	/**
	 * @param currBillNo the currBillNo to set
	 */
	public void setCurrBillNo(Integer currBillNo) {
		this.currBillNo = currBillNo;
	}
	/**
	 * @return the currFutureBillNo
	 */
	public Integer getCurrFutureBillNo() {
		return currFutureBillNo;
	}
	/**
	 * @param currFutureBillNo the currFutureBillNo to set
	 */
	public void setCurrFutureBillNo(Integer currFutureBillNo) {
		this.currFutureBillNo = currFutureBillNo;
	}
	/**
	 * @return the currfutureOverDuePrinIntBillNo
	 */
	public Integer getCurrfutureOverDuePrinIntBillNo() {
		return currfutureOverDuePrinIntBillNo;
	}
	/**
	 * @param currfutureOverDuePrinIntBillNo the currfutureOverDuePrinIntBillNo to set
	 */
	public void setCurrfutureOverDuePrinIntBillNo(Integer currfutureOverDuePrinIntBillNo) {
		this.currfutureOverDuePrinIntBillNo = currfutureOverDuePrinIntBillNo;
	}

	/**
	 * Gets the value of cdbillList.
	 *
	 * @return the value of cdbillList
	 */
	public List<LnsBill> getCdbillList() {
		return cdbillList;
	}

	/**
	 * Sets the cdbillList.
	 *
	 * @param cdbillList cdbillList
	 */
	public void setCdbillList(List<LnsBill> cdbillList) {
		this.cdbillList = cdbillList;

	}
	public List<LnsBill> getTotalbillList() {
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<>();
		//历史账单
		billList.addAll(getHisBillList());
		//历史未结清账单结息账单
		billList.addAll(getHisSetIntBillList());
		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
		billList.addAll(getBillInfoList());
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(getFutureOverDuePrinIntBillList());
		//未来期账单：从还款日到合同到期日之间的本金和利息账单
		billList.addAll(getFutureBillInfoList());
		//获取呆滞呆账期间新罚息复利账单list
		billList.addAll(getCdbillList());
		return billList;

	}


}

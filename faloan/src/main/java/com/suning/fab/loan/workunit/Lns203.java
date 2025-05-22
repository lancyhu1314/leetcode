package com.suning.fab.loan.workunit;

import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.tup4j.amount.FabRate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.InvestInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 *  开户--放款渠道
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns203 extends WorkUnit { 

	String		openBrc;
	String		channelType;
	String		fundChannel;
	String		outSerialNo;
	String		investee;
	String		investMode;
	
	String		receiptNo;
	String		loanType;
	FabAmount	contractAmt;
	FabAmount	discountAmt;
	FabAmount	fundAmt;
	//代偿资金方
	String exinvesteeId;

	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		String investeeId = "";
		String investeeAcctno="";//资金方借据号
		String investeePropor="";//资金方占比
		FabAmount investeePrin = new FabAmount(0.00);
		if( VarChecker.isEmpty(pkgList1) && "51230003".equals(ctx.getBrc()))
		{
			throw new FabException("LNS053");
		}
		//非标取pkgList2 作为资金方
		if("473005".equals(ctx.getTranCode())){
			pkgList1= ctx.getRequestDict("pkgList2");
		}
		//非标的登记资金方
		if( !VarChecker.isEmpty(pkgList1))
		{
			for (PubDict pkg:pkgList1.getLoopmsg()) {
				investeeId = PubDict.getRequestDict(pkg, "investeeId");
				investeePrin = PubDict.getRequestDict(pkg, "investeePrin");
				if(PubDict.getRequestDict(pkg, "investeeAcctno")!=null){
					investeeAcctno=PubDict.getRequestDict(pkg, "investeeAcctno");
				}
				if(PubDict.getRequestDict(pkg, "investeePropor")!=null){
					investeePropor=((FabRate)PubDict.getRequestDict(pkg, "investeePropor")).getVal().doubleValue()+"";
				}
			}
			
			if(   !investeePrin.sub(new FabAmount(contractAmt.getVal())).isZero() &&  !"473006".equals(ctx.getTranCode()))
			{
				throw new FabException("LNS050");
			}
			//登记资金方登记簿  lns101只允许一个资金方
			//AccountingModeChange.createInvesteeDetail(ctx, 1,getReceiptNo(),investeeId ,outSerialNo, investeePrin.getVal() )
			InvestInfo investInfo = new InvestInfo();
			investInfo.setInvesteeId(investeeId);
			investInfo.setInvesteeNo(VarChecker.isEmpty(outSerialNo) ? "" : outSerialNo);
			investInfo.setInvesteePrin(investeePrin);
			investInfo.setNewInvestee(investeeAcctno);
			investInfo.setReserv(investeePropor);
			AccountingModeChange.createInvesteeDetail(ctx, 1, receiptNo, investInfo, ConstantDeclare.TRANTYPE.KH, investeePrin);
		}

		LoggerUtil.debug("getReceiptNo" + getReceiptNo());
		
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		
		if(getDiscountAmt() != null)
			fundAmt = new FabAmount(contractAmt.sub(discountAmt).getVal());
		else
			fundAmt = new FabAmount(contractAmt.getVal());
		LoggerUtil.debug("FUNDAMT:" + fundAmt.getVal() + "|" + getChannelType());
		FundInvest openFundInvest = new FundInvest(getInvestee(), getInvestMode(), getChannelType(), getFundChannel(), getOutSerialNo());
		eventProvider.createEvent(ConstantDeclare.EVENT.LOANCHANEL, fundAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.FKQD, ctx, VarChecker.isEmpty(exinvesteeId)?investeeId:exinvesteeId);
	}
	/**
	 * @return the openBrc
	 */
	public String getOpenBrc() {
		return openBrc;
	}
	/**
	 * @param openBrc the openBrc to set
	 */
	public void setOpenBrc(String openBrc) {
		this.openBrc = openBrc;
	}
	/**
	 * @return the channelType
	 */
	public String getChannelType() {
		return channelType;
	}
	/**
	 * @param channelType the channelType to set
	 */
	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}
	/**
	 * @return the fundChannel
	 */
	public String getFundChannel() {
		return fundChannel;
	}
	/**
	 * @param fundChannel the fundChannel to set
	 */
	public void setFundChannel(String fundChannel) {
		this.fundChannel = fundChannel;
	}
	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}
	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}
	/**
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}
	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}
	/**
	 * @return the discountAmt
	 */
	public FabAmount getDiscountAmt() {
		return discountAmt;
	}
	/**
	 * @param discountAmt the discountAmt to set
	 */
	public void setDiscountAmt(FabAmount discountAmt) {
		this.discountAmt = discountAmt;
	}
	/**
	 * @return the fundAmt
	 */
	public FabAmount getFundAmt() {
		return fundAmt;
	}
	/**
	 * @param fundAmt the fundAmt to set
	 */
	public void setFundAmt(FabAmount fundAmt) {
		this.fundAmt = fundAmt;
	}
	/**
	 * @return the eventProvider
	 */
	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}
	/**
	 * @param eventProvider the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}
	public String getOutSerialNo() {
		return outSerialNo;
	}
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}
	public String getInvestee() {
		return investee;
	}
	public void setInvestee(String investee) {
		this.investee = investee;
	}
	public String getInvestMode() {
		return investMode;
	}
	public void setInvestMode(String investMode) {
		this.investMode = investMode;
	}

	/**
	 * @return the loanType
	 */
	public String getLoanType() {
		return loanType;
	}
	/**
	 * @param loanType the loanType to set
	 */
	public void setLoanType(String loanType) {
		this.loanType = loanType;
	}

    /**
     * Gets the value of exinvesteeId.
     *
     * @return the value of exinvesteeId
     */
    public String getExinvesteeId() {
        return exinvesteeId;
    }

    /**
     * Sets the exinvesteeId.
     *
     * @param exinvesteeId exinvesteeId
     */
    public void setExinvesteeId(String exinvesteeId) {
        this.exinvesteeId = exinvesteeId;

    }
}

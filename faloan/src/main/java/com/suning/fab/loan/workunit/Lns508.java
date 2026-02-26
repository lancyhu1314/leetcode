package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.List;

import com.suning.fab.loan.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.InvestInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author
 *
 * @version V1.0.0
 *
 * @see -资金方还款记账（资金方还款 LBRPYINVES）
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns508 extends WorkUnit {

	String acctNo;

	String		receiptNo;
	FabAmount	fundAmt;
	FabAmount	discountAmt;
	FabAmount	contractAmt;

	String		openBrc;
	String		channelType;
	String		fundChannel;
	String		outSerialNo;
	String		investee;
	String		investMode;
	FabAmount nintAmt;
	FabAmount dintAmt;

	FabAmount investeePrinPlatform= new FabAmount(0.00);//资金方本金 由平台贴
	FabAmount investeeIntPlatform= new FabAmount(0.00);//资金方利息 由平台贴
	FabAmount investeeDintPlatform= new FabAmount(0.00);//资金方罚息 由平台贴

	FabAmount  couponIntAmt=new FabAmount(0.00);//贴息金额

	//贴息标志
	String discountIntFlag="";

	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();

		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		if( VarChecker.isEmpty(pkgList1) && "51230003".equals(ctx.getBrc()))
		{
			throw new FabException("LNS053");
		}

		if( !VarChecker.isEmpty(pkgList1) )
		{
			int i=1;
			for (PubDict pkg:pkgList1.getLoopmsg()) {
				String investeeId = "";
				String investeeFlag = "";
				FabAmount investeePrin;
				FabAmount investeeInt;
				FabAmount investeeDint;
				FabAmount investeeFee;
				FabAmount totalAmt = new FabAmount(0.00);
				investeeId = PubDict.getRequestDict(pkg, "investeeId");
				//自动贴息
				if("01".equals(ctx.getRequestDict("investeeRepayFlg"))){
					if("Y".equals(discountIntFlag)){
						investeeIntPlatform.selfAdd(couponIntAmt);
					}
					investeePrin=investeePrinPlatform;
					investeeInt=investeeIntPlatform;
					investeeDint=investeeDintPlatform;
				}else{
					//农商行资金还费 传资金方 未传字段可不传 判空
					investeePrin = PubDict.getRequestDict(pkg, "investeePrin")==null?new FabAmount(0.00):PubDict.getRequestDict(pkg, "investeePrin");
					investeeInt = PubDict.getRequestDict(pkg, "investeeInt")==null?new FabAmount(0.00):PubDict.getRequestDict(pkg, "investeeInt");
					investeeDint = PubDict.getRequestDict(pkg, "investeeDint")==null?new FabAmount(0.00):PubDict.getRequestDict(pkg, "investeeDint");
				}
				investeeFlag = PubDict.getRequestDict(pkg, "investeeFlag");
				totalAmt.selfAdd(investeePrin).selfAdd(investeeInt).selfAdd(investeeDint);
				//汽车分期 资金方提前还款手续费
				investeeFee = PubDict.getRequestDict(pkg, "investeeFee");
				if(!VarChecker.isEmpty(investeeFee)){
					totalAmt.selfAdd(investeeFee);
				}

				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				FundInvest openFundInvest = new FundInvest(getInvestee(), getInvestMode(), getChannelType(), getFundChannel(), getOutSerialNo());

				List<FabAmount> amtList = new ArrayList<FabAmount>();
				amtList.add(investeePrin);
				amtList.add(investeeInt);
				amtList.add(investeeDint);
				//汽车分期
				if(!VarChecker.isEmpty(investeeFee)){
					amtList.add(investeeFee);
				}
				if(totalAmt.isPositive()){
					eventProvider.createEvent(ConstantDeclare.EVENT.LBRPYINVES, totalAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.ZJXQ, ctx, amtList, investeeId, investeeFlag);
					//资金方还款 登记资金方明细登记簿
					InvestInfo investInfo = new InvestInfo();
					investInfo.setInvesteeId(investeeId);
					investInfo.setInvesteeNo(VarChecker.isEmpty(outSerialNo) ? "" : outSerialNo);
					investInfo.setInvesteePrin(investeePrin);
					investInfo.setInvesteeInt(investeeInt);
					investInfo.setInvesteeDint(investeeDint);
					investInfo.setInvesteeFlag(investeeFlag);
					if(!VarChecker.isEmpty(investeeFee)&&investeeFee.isPositive())
						investInfo.setInvesteeFee(investeeFee);
					AccountingModeChange.createInvesteeDetail(ctx, i++, acctNo, investInfo, ConstantDeclare.TRANTYPE.HK, totalAmt);
				}
			}
		}
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
	 * @return the outSerialNo
	 */
	public String getOutSerialNo() {
		return outSerialNo;
	}
	/**
	 * @param outSerialNo the outSerialNo to set
	 */
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}
	/**
	 * @return the investee
	 */
	public String getInvestee() {
		return investee;
	}
	/**
	 * @param investee the investee to set
	 */
	public void setInvestee(String investee) {
		this.investee = investee;
	}
	/**
	 * @return the investMode
	 */
	public String getInvestMode() {
		return investMode;
	}
	/**
	 * @param investMode the investMode to set
	 */
	public void setInvestMode(String investMode) {
		this.investMode = investMode;
	}
	/**
	 * @return the nintAmt
	 */
	public FabAmount getNintAmt() {
		return nintAmt;
	}
	/**
	 * @param nintAmt the nintAmt to set
	 */
	public void setNintAmt(FabAmount nintAmt) {
		this.nintAmt = nintAmt;
	}
	/**
	 * @return the dintAmt
	 */
	public FabAmount getDintAmt() {
		return dintAmt;
	}
	/**
	 * @param dintAmt the dintAmt to set
	 */
	public void setDintAmt(FabAmount dintAmt) {
		this.dintAmt = dintAmt;
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

	public FabAmount getInvesteePrinPlatform() {
		return investeePrinPlatform;
	}

	public void setInvesteePrinPlatform(FabAmount investeePrinPlatform) {
		this.investeePrinPlatform = investeePrinPlatform;
	}

	public FabAmount getInvesteeIntPlatform() {
		return investeeIntPlatform;
	}

	public void setInvesteeIntPlatform(FabAmount investeeIntPlatform) {
		this.investeeIntPlatform = investeeIntPlatform;
	}

	public FabAmount getInvesteeDintPlatform() {
		return investeeDintPlatform;
	}

	public void setInvesteeDintPlatform(FabAmount investeeDintPlatform) {
		this.investeeDintPlatform = investeeDintPlatform;
	}

	public FabAmount getCouponIntAmt() {
		return couponIntAmt;
	}

	public void setCouponIntAmt(FabAmount couponIntAmt) {
		this.couponIntAmt = couponIntAmt;
	}

	public String getDiscountIntFlag() {
		return discountIntFlag;
	}

	public void setDiscountIntFlag(String discountIntFlag) {
		this.discountIntFlag = discountIntFlag;
	}



}

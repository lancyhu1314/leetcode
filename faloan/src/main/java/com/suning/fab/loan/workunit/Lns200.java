package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.InvestInfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author 
 *
 * @version V1.0.1
 *
 * @see 开户--手机租赁放款渠道
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns200 extends WorkUnit { 

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


	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		
	

		
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", getReceiptNo());
		param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		
		
		
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		String investeeId = "";
		FabAmount investeePrin = new FabAmount(0.00);
		if( !VarChecker.isEmpty(pkgList1) )
		{
			for (PubDict pkg:pkgList1.getLoopmsg()) {
				investeeId = PubDict.getRequestDict(pkg, "investeeId");
				investeePrin = PubDict.getRequestDict(pkg, "investeePrin");
			}
			if( !investeePrin.sub(lnsbasicinfo.getContractamt()).isZero() )
			{
				throw new FabException("LNS061",investeePrin.getVal(), lnsbasicinfo.getContractamt());
			}
		}
		//登记资金方登记簿  上面的循环只取了最后一个资金方
		//AccountingModeChange.createInvesteeDetail(ctx, 1, getReceiptNo(),investeeId  ,outSerialNo, investeePrin.getVal())
		InvestInfo investInfo = new InvestInfo();
		investInfo.setInvesteeId(investeeId);
		investInfo.setInvesteeNo(VarChecker.isEmpty(outSerialNo) ? "" : outSerialNo);
		investInfo.setInvesteePrin(investeePrin);
		AccountingModeChange.createInvesteeDetail(ctx, 1, receiptNo, investInfo, ConstantDeclare.TRANTYPE.KH, investeePrin);

		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		
		fundAmt = new FabAmount(contractAmt.getVal());
		
		List<FabAmount> amtList = new ArrayList<FabAmount>();
		amtList.add(fundAmt);
		
		LoggerUtil.debug("FUNDAMT:" + fundAmt.getVal() + "|" + getChannelType());
		FundInvest openFundInvest = new FundInvest(getInvestee(), getInvestMode(), getChannelType(), getFundChannel(), getOutSerialNo());
		eventProvider.createEvent(ConstantDeclare.EVENT.LOANCHANEL, new FabAmount(lnsbasicinfo.getContractamt()), lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.FKQD, ctx, amtList, investeeId);
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
}

package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author 
 *
 * @version V1.0.1
 *
 * @see 开户--汽车租赁放款渠道
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns249 extends WorkUnit { 

	String		openBrc;
	String		channelType;
	String		fundChannel;
	String		outSerialNo;
	String		investee;
	String		investMode;
	String		merchantNo;
	
	String		receiptNo;
	String		loanType;
	FabAmount	contractAmt;
	FabAmount	discountAmt;
	FabAmount	fundAmt;
	FabAmount	tailAmt;

	LoanAgreement	loanAgreement;
	
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
		
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

		fundAmt = new FabAmount(contractAmt.getVal());
		
		List<FabAmount> amtList = new ArrayList<FabAmount>();
		if(!VarChecker.isEmpty(tailAmt)){
			amtList.add(tailAmt);
			amtList.add(new FabAmount(0.00));
		}else{
			amtList.add(new FabAmount(0.00));
			amtList.add(new FabAmount(0.00));
		}
		amtList.add(fundAmt);
		
		LoggerUtil.debug("FUNDAMT:" + fundAmt.getVal() + "|" + getChannelType());
		FundInvest openFundInvest = new FundInvest(getInvestee(), getInvestMode(), getChannelType(), getFundChannel(), getOutSerialNo());

		if(!VarChecker.isEmpty(lnsbasicinfo.getCusttype())){
			lnsAcctInfo.setCustType(lnsbasicinfo.getCusttype());
		}
		String reserv1 = null;
		if("1".equals(lnsbasicinfo.getCusttype())||"PERSON".equals(lnsbasicinfo.getCusttype())){
			reserv1 = "70215243";
		}else if("2".equals(lnsbasicinfo.getCusttype())||"COMPANY".equals(lnsbasicinfo.getCusttype())){
			reserv1 = getMerchantNo();
		}else{
			reserv1 = "";
		}
		//CustomName统一放至备用5方便发送事件时转换处理
		//转json存入Tunneldata字段 
		eventProvider.createEvent(ConstantDeclare.EVENT.LOANCHANEL, new FabAmount(lnsbasicinfo.getContractamt()), lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.FKQD, ctx, amtList, reserv1,"","","",lnsbasicinfo.getName());
		
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
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
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
	 * @return the merchantNo
	 */
	public String getMerchantNo() {
		return merchantNo;
	}
	/**
	 * @param merchantNo the merchantNo to set
	 */
	public void setMerchantNo(String merchantNo) {
		this.merchantNo = merchantNo;
	}
	/**
	 * @return the tailAmt
	 */
	public FabAmount getTailAmt() {
		return tailAmt;
	}
	/**
	 * @param tailAmt the tailAmt to set
	 */
	public void setTailAmt(FabAmount tailAmt) {
		this.tailAmt = tailAmt;
	}

}

package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 
 *
 * @version V1.0.0
 *
 * @see -资金方费用结转事件
 *
 * @param 
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns534 extends WorkUnit {

	String acctNo;

	private String cooperateId;

	String		openBrc;
	String		channelType;
	String		fundChannel;
	private LoanAgreement loanAgreement;

	FabAmount feeAmtPlatform=new FabAmount(0.00);//平台费用
	FabAmount feeAmtInverest= new FabAmount(0.00);//资金方费用

	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx,loanAgreement);
		FaloanJson reduceExtend=LoanRpyInfoUtil.getReduceExtend(tranctx,acctNo);
		//不贴费用的无需抛费用结转
		if(!"Y".equals(reduceExtend.getString(ConstantDeclare.PARACONFIG.DISCOUNTFEEFLAG))){
			return;
		}
		TranCtx ctx = getTranctx();
		String    feeFlag = "";
		FabAmount diffFee = new FabAmount(feeAmtInverest.sub(new FabAmount(feeAmtPlatform.getVal())).getVal());
		List<FabAmount> amtList = new ArrayList<FabAmount>();
		amtList.add(feeAmtPlatform);
		amtList.add(feeAmtInverest);
		TblLnsfeeinfo lnsfeeinfo = LoanFeeUtils.matchFeeInfo(loanAgreement,loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getFeetype() ,loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getRepayway());
		String childBrc = lnsfeeinfo.getFeebrc();
		String reserv1="";
		if( !VarChecker.isEmpty(cooperateId) ) {
			reserv1 = cooperateId;
		}else{
			reserv1=childBrc;
		}
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency(),childBrc);
		//收入
		if(diffFee.isPositive())
		{
			feeFlag = "02";
		}
		//无贴息/收入
		else
		{
			feeFlag = "03";
			diffFee.setVal(0.00);
		}
		eventProvider.createEvent(ConstantDeclare.EVENT.FEEINCMCYO, diffFee, lnsAcctInfo, null, null, ConstantDeclare.BRIEFCODE.TRDF, ctx, amtList, reserv1, feeFlag);
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

	public FabAmount getFeeAmtPlatform() {
		return feeAmtPlatform;
	}

	public void setFeeAmtPlatform(FabAmount feeAmtPlatform) {
		this.feeAmtPlatform = feeAmtPlatform;
	}

	public FabAmount getFeeAmtInverest() {
		return feeAmtInverest;
	}

	public void setFeeAmtInverest(FabAmount feeAmtInverest) {
		this.feeAmtInverest = feeAmtInverest;
	}

	public String getCooperateId() {
		return cooperateId;
	}

	public void setCooperateId(String cooperateId) {
		this.cooperateId = cooperateId;
	}
}

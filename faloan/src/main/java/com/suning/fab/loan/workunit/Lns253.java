package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.InvestInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 开户--放款渠道
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns253 extends WorkUnit { 

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
		
	
		//资金方信息:pkgList1		14050183	
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		//放款渠道:pkgList2	14050183		
		ListMap pkgList2 = ctx.getRequestDict("pkgList2");
		String investeeId = "";
		String investeeAcctno="";//资金方借据号
		String investeePropor="";//资金方占比
		FabAmount investeePrin = new FabAmount(0.00);
		if( VarChecker.isEmpty(pkgList1) && "51230003".equals(ctx.getBrc()))
		{
			throw new FabException("LNS053");
		}
		if( VarChecker.isEmpty(pkgList2))
		{
			throw new FabException("LNS055","多渠道信息");
		}
		
		if( !VarChecker.isEmpty(pkgList1) && !"473005".equals(ctx.getTranCode()))
		{
			for (PubDict pkg:pkgList1.getLoopmsg()) {
				investeeId = PubDict.getRequestDict(pkg, "investeeId");
				investeePrin = PubDict.getRequestDict(pkg, "investeePrin");
				if(PubDict.getRequestDict(pkg, "investeeAcctno")!=null){
					investeeAcctno=PubDict.getRequestDict(pkg, "investeeAcctno");
				}
				if(PubDict.getRequestDict(pkg, "investeePropor")!=null){
					investeePropor=((FabRate)PubDict.getRequestDict(pkg, "investeePropor")).getVal().toString();
				}
			}
			
			if(   !investeePrin.sub(new FabAmount(contractAmt.getVal())).isZero() &&  !"473006".equals(ctx.getTranCode()))
			{
				throw new FabException("LNS050");
			}
			//登记资金方登记簿  lns101只允许一个资金方
			//AccountingModeChange.createInvesteeDetail(ctx, 1, getReceiptNo(),investeeId ,getOutSerialNo(), investeePrin.getVal() )
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
		
		FabAmount channelTotal = new FabAmount(0.00);
		for(PubDict pkg:pkgList2.getLoopmsg()){
			if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "channelCode"))){
				throw new FabException("LNS055","放款渠道");
			}
			if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "channelAmt"))){
				throw new FabException("LNS055","渠道金额");
			}
			//代付保费渠道不校验渠道单号 20190321
			//预收费渠道不校验渠道单号 20190805
			if(!VarChecker.asList(ConstantDeclare.CHANNELCODE.A,"9").contains(PubDict.getRequestDict(pkg, "channelCode")))
			{
				if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "channelNo"))){
					throw new FabException("LNS055","渠道单号");
				}
                if(PubDict.getRequestDict(pkg, "channelNo").toString().getBytes().length > 50){
                    throw new FabException("LNS112","渠道单号");
                }
			}

			//银行存款		14050183
			if("1".equals(PubDict.getRequestDict(pkg, "channelCode"))){
				if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "channelSubject"))){
					throw new FabException("LNS055","银行渠道科目");
				}
			}
			FabAmount channelAmt = PubDict.getRequestDict(pkg, "channelAmt");
			channelTotal.selfAdd(channelAmt);
			LoggerUtil.debug("FUNDAMT:" + channelAmt.getVal() + "|" + PubDict.getRequestDict(pkg, "channelCode"));
		}
		if(!channelTotal.equals(fundAmt)){
			throw new FabException("LNS106");
		}
		LoggerUtil.debug("FUNDAMT:" + fundAmt.getVal() + "|" + getChannelType());
		List<Map<String,String>> jsonArray= new ArrayList<>();//存储成jsonArray的格式
		for(PubDict pkg:pkgList2.getLoopmsg()){
			FabAmount channelAmt = PubDict.getRequestDict(pkg, "channelAmt");
			Map<String,String> channelInfo = new HashMap<>();
			channelInfo.put("code", PubDict.getRequestDict(pkg, "channelCode").toString());//放款渠道
			channelInfo.put("no", obtainPkgValue(pkg,"channelNo"));//渠道单号
			channelInfo.put("amt", channelAmt.toString());//渠道金额
			channelInfo.put("subject", obtainPkgValue(pkg,"channelSubject"));//渠道科目
			channelInfo.put("investeeId",investeeId );
			if(ConstantDeclare.CHANNELCODE.A.equals(PubDict.getRequestDict(pkg, "channelCode").toString()))
			{
				//渠道为A-预扣费 时 收费公司商户号
				if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feecompanyID"))){
					throw new FabException("LNS179",ConstantDeclare.CHANNELCODE.A,"feecompanyID");
				}
				channelInfo.put("feecompanyID",PubDict.getRequestDict(pkg, "feecompanyID").toString() );
			}

			jsonArray.add(channelInfo);
			FundInvest openFundInvest = new FundInvest(getInvestee(), getInvestMode(), PubDict.getRequestDict(pkg, "channelCode").toString(), obtainPkgValue(pkg,"channelSubject"), obtainPkgValue(pkg,"channelNo"));
			//收费公司商户号放在备用字段二
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANCHANEL, channelAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.FKQD, ctx, investeeId,	channelInfo.get("feecompanyID")==null?"":channelInfo.get("feecompanyID"));
		}
		Map<String,Object> stringJson = new HashMap<>();
		stringJson.put("QD", jsonArray);
		//存储开户时的多渠道数据   存储在json格式的value中
		if(stringJson.size()>0)
			AccountingModeChange.saveInterfaceEx(ctx, receiptNo, ConstantDeclare.KEYNAME.QD, "多渠道", JsonTransfer.ToJson(stringJson));
	}

	private String obtainPkgValue(PubDict pkg,String key) {
		return VarChecker.isEmpty(PubDict.getRequestDict(pkg, key))?"":PubDict.getRequestDict(pkg, key).toString();
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

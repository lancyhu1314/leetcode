package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.suning.fab.loan.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author
 *
 * @version V1.0.0
 *
 * @see -现金贷资金方记账（收入结转 INCMCARRYO）
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns509 extends WorkUnit {

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
	String		productCode;

	FabAmount prinAmt;
	FabAmount nintAmt;
	FabAmount dintAmt;

	FabAmount investeePrinPlatform= new FabAmount(0.00);//资金方本金 由平台贴
	FabAmount investeeIntPlatform= new FabAmount(0.00);//资金方利息 由平台贴
	FabAmount investeeDintPlatform= new FabAmount(0.00);//资金方罚息 由平台贴

	FabAmount  couponIntAmt=new FabAmount(0.00);//贴息金额

	String discountIntFlag="";//贴息标志


	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		if( VarChecker.isEmpty(pkgList1) && "51230003".equals(ctx.getBrc()))
		{
			throw new FabException("LNS053");
		}
/* 校验多资金方金额且多资金方不进行收入结转直接return
 * 1） 校验资金方明细内本金之和与平台还款本金一致，利息之和与平台还息金额一致，罚息之和与平台还罚息金额一致
 * 2） 校验资金方还款本息罚之和与上述的对应平台还款金额一致
 * 多资金方目前仅支持如下产品："2512630", "2512636", "2512637", "2512631", "2512644", "2512633"
 * at 20210621
 * */
		if (!VarChecker.isEmpty(pkgList1) && pkgList1.size()>1){
			
			if (!Arrays.asList("2512630", "2512636", "2512637", "2512631", "2512644","2512633").contains(productCode)){
				throw new FabException("LNS245");
			}
			FabAmount investeePrinTotal= new FabAmount(0.00);//多资金方明细本金之和
			FabAmount investeeIntTotal= new FabAmount(0.00);//多资金方明细利息之和
			FabAmount investeeDintTotal= new FabAmount(0.00);//多资金方明细罚息之和
			for (PubDict pkg1:pkgList1.getLoopmsg()) {
				FabAmount investeeDtlPrin;
				FabAmount investeeDtlInt;
				FabAmount investeeDtlDint;
				FabAmount platformTotalAmt;
				FabAmount investeeDtlTotal= new FabAmount(0.00);
				investeeDtlPrin = PubDict.getRequestDict(pkg1, "investeePrin")==null?new FabAmount(0.00):PubDict.getRequestDict(pkg1, "investeePrin");
				investeeDtlInt = PubDict.getRequestDict(pkg1, "investeeInt")==null?new FabAmount(0.00):PubDict.getRequestDict(pkg1, "investeeInt");
				investeeDtlDint = PubDict.getRequestDict(pkg1, "investeeDint")==null?new FabAmount(0.00):PubDict.getRequestDict(pkg1, "investeeDint");
				platformTotalAmt = PubDict.getRequestDict(pkg1, "platformTotalAmt")==null?new FabAmount(0.00):PubDict.getRequestDict(pkg1, "platformTotalAmt");

				investeePrinTotal.selfAdd(investeeDtlPrin);
				investeeIntTotal.selfAdd(investeeDtlInt);
				investeeDintTotal.selfAdd(investeeDtlDint);
				investeeDtlTotal.selfAdd(investeeDtlPrin);
				investeeDtlTotal.selfAdd(investeeDtlInt);
				investeeDtlTotal.selfAdd(investeeDtlDint);
				if (!investeeDtlTotal.selfSub(platformTotalAmt).isZero()){
					throw new FabException("LNS244");
				}
			}
			LoggerUtil.info("还款本金:" + investeePrinPlatform.getVal());
			if (!investeePrinTotal.selfSub(investeePrinPlatform).isZero()){
				throw new FabException("LNS241");
			}
			LoggerUtil.info("还款利息:" + investeeIntPlatform.getVal());
			if (!investeeIntTotal.selfSub(investeeIntPlatform).isZero()){
				throw new FabException("LNS242");
			}
			LoggerUtil.info("还款罚息:" + investeeDintPlatform.getVal());
			if (!investeeDintTotal.selfSub(investeeDintPlatform).isZero()){
				throw new FabException("LNS243");
			}

			//多资金方还款不支持自动处理资金方还款信息
			if( pkgList1.size()>1 && "01".equals(ctx.getRequestDict("investeeRepayFlg"))){
				throw new FabException("LNS246");
			}
			return;
		}

		if( !VarChecker.isEmpty(pkgList1) )
		{
			for (PubDict pkg:pkgList1.getLoopmsg()) {
				String investeeId;
				FabAmount investeePrin;
				FabAmount investeeInt;
				FabAmount investeeDint;

				FabAmount repayAmt;
				FabAmount investeeAmt;
				FabAmount diffFee = new FabAmount(0.00);
				FabAmount diffPrin = new FabAmount(0.00);
				String    feeFlag = "";
				String 	  prinFlag = "";
				investeeId = PubDict.getRequestDict(pkg, "investeeId");
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
				// 多资金方收入结转的差额在利息上体现， --【需求变更】：不会有收入结转
//				if(!VarChecker.isEmpty(PubDict.getRequestDict(pkg, "platformTotalAmt"))){
//				prinAmt=investeePrin;
//				dintAmt=investeeDint;
//					nintAmt=(FabAmount) ((FabAmount) PubDict.getRequestDict(pkg, "platformTotalAmt")).sub(prinAmt.getVal()).sub(dintAmt.getVal());
//				}

				//资金方list中，资金方利息+资金方罚息
				investeeAmt = new FabAmount(investeeInt.add(new FabAmount(investeeDint.getVal())).getVal());
				//还款利息+还款罚息
				repayAmt = new FabAmount(nintAmt.add(dintAmt.getVal()).getVal());
				//还款金额-资金方金额
				FabAmount platformFee = new FabAmount(repayAmt.sub(new FabAmount(investeeAmt.getVal())).getVal());
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				FundInvest openFundInvest = new FundInvest(getInvestee(), getInvestMode(), getChannelType(), getFundChannel(), getOutSerialNo());

				List<FabAmount> amtList = new ArrayList<FabAmount>();
				amtList.add(nintAmt);
				amtList.add(dintAmt);
				amtList.add(investeeInt);
				amtList.add(investeeDint);

				//收入
				if(platformFee.isPositive())
				{
					feeFlag = "02";
					diffFee.setVal(platformFee.getVal());
				}
				//费用
				else if(platformFee.isNegative())
				{
					feeFlag = "01";
					diffFee.setVal(new FabAmount(0.00).sub(platformFee).getVal());
				}
				//无贴息/收入
				else
				{
					feeFlag = "03";
					diffFee.setVal(0.00);
				}

				//平台收到本金金额大于资金方收到本金金额
				if( prinAmt.sub(investeePrin).isPositive())
				{
					prinFlag = "04";
					diffPrin.setVal(prinAmt.sub(investeePrin).getVal());
				}
				//平台收到本金金额小于资金方收到本金金额
				else if( prinAmt.sub(investeePrin).isNegative() )
				{
					prinFlag = "05";
					diffPrin.setVal(new FabAmount(0.00).sub(prinAmt.sub(investeePrin)).getVal());
				}
				//平台收到本金金额等于资金方收到本金金额
				else
				{
					prinFlag = "06";
					diffPrin.setVal(new FabAmount(0.00).getVal());
				}

				amtList.add(diffPrin);

				eventProvider.createEvent(ConstantDeclare.EVENT.INCMCARRYO, diffFee, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.SRJZ, ctx, amtList, investeeId, feeFlag,PubDictUtils.getInvesteeFlag(ctx),prinFlag);

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
	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}
	/**
	 * @param prinAmt the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
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
	public String getProductCode() {
		return productCode;
	}
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}
	

}

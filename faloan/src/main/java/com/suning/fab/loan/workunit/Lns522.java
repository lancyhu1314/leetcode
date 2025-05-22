package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.List;

import com.suning.fab.loan.utils.PubDictUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
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
 * @see -手续费结转-INCADFRRYO 
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns522 extends WorkUnit {

	String acctNo;

	String		receiptNo;
	FabAmount	fundAmt;
	FabAmount	discountAmt;
	FabAmount	contractAmt;
	//汽车分期  手续费
	FabAmount feeAmt;

	String		openBrc;
	String		channelType;
	String		fundChannel;
	String		outSerialNo;
	String		investee;
	String		investMode;

	FabAmount prinAmt;
	FabAmount nintAmt;
	FabAmount dintAmt;
	//汽车分期
	LoanAgreement la;

	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		String investeeId = "";
		//汽车分期 资金方提前还款手续费
		FabAmount investeeFee ;

		//汽车分期reserve2
		String adFeeFlag = "";
		//汽车分期结转事件金额
		FabAmount diffAdFee = new FabAmount(0.00);

		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		if( VarChecker.isEmpty(pkgList1) && "51230003".equals(ctx.getBrc()))
		{
			throw new FabException("LNS053");
		}

		if( !VarChecker.isEmpty(pkgList1) )
		{
			for (PubDict pkg:pkgList1.getLoopmsg()) {
				//暂时不会有多资金方手续费金额
//				if(!VarChecker.isEmpty(PubDict.getRequestDict(pkg, "platformFee"))){
//					feeAmt=PubDict.getRequestDict(pkg, "platformFee");
//				}
				//汽车分期 资金方提前还款手续费
				investeeFee = PubDict.getRequestDict(pkg, "investeeFee");

//		if( !VarChecker.isEmpty(investeeFee) && VarChecker.isEmpty(ctx.getRequestDict("settleFlag")))
//		{
//			throw new FabException("LNS055","settleFlag");
//		}

				//2019-10-22 平台手续费feeAmt>0，结清标识必输，资金方手续费investeeFee可以不传值，抛手续费结转事件
				if( !VarChecker.isEmpty(feeAmt) &&	feeAmt.isPositive() )
				{
					if(VarChecker.isEmpty(ctx.getRequestDict("settleFlag")))
						throw new FabException("LNS055","settleFlag");
				}


				//2019-10-22 平台手续费feeAmt空或者0，结清标志非必输，资金方investeeFee大于0，抛手续费结转事件，资金方investeeFee等于0或空不抛结转事件
				if( VarChecker.isEmpty(feeAmt) || feeAmt.isZero() )
					if( VarChecker.isEmpty(investeeFee) ||	investeeFee.isZero() )
						return;

				//2019-10-22 平台手续费feeAmt、investeeFee两个都是0，过滤掉不抛结转事件
				if( !VarChecker.isEmpty(feeAmt) &&	feeAmt.isZero() &&
						!VarChecker.isEmpty(investeeFee) &&	investeeFee.isZero() )
					return;


				if(VarChecker.isEmpty(investeeFee) && VarChecker.isEmpty(feeAmt)){
					return;
				}else if(VarChecker.isEmpty(investeeFee)){
					investeeFee = new FabAmount(0.00);
				}else if(VarChecker.isEmpty(feeAmt)){
					feeAmt = new FabAmount(0.00);
				}
				//汽车分期
				LnsAcctInfo lnsAcctInfoQC = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_ADFE, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());


				FundInvest openFundInvest = new FundInvest(getInvestee(), getInvestMode(), getChannelType(), getFundChannel(), getOutSerialNo());


				//汽车分期
				List<FabAmount> amtListQC = new ArrayList<FabAmount>();
				//平台结转手续费金额
				amtListQC.add(feeAmt);
				//应付资金方手续费金额
				amtListQC.add(investeeFee);
				//费用/收入金额
				FabAmount feeOrComein = new FabAmount(feeAmt.sub(new FabAmount(investeeFee.getVal())).getVal());


				//汽车分期
				//收入
				if(feeOrComein.isPositive())
				{
					adFeeFlag = "02";
					diffAdFee.setVal(feeOrComein.getVal());
				}
				//费用
				else if(feeOrComein.isNegative())
				{
					adFeeFlag = "01";
					diffAdFee.setVal(new FabAmount(0.00).sub(feeOrComein).getVal());
				}
				//无贴息/收入
				else
				{
					adFeeFlag = "03";
					diffAdFee.setVal(0.00);
				}



				//资金方结转 会计系统计算   508子交易处理了
				//AccountingModeChange.incInvesteeDetail(ctx, feeFlag, prinFlag,diffFee.getVal() ,diffPrin.getVal() );
				lnsAcctInfoQC.setPrdCode(la.getPrdId());
				//2019-10-22 汽车分期资金方手续费结转事件
				eventProvider.createEvent(ConstantDeclare.EVENT.INCADFRRYO, diffAdFee, lnsAcctInfoQC, null, openFundInvest, ConstantDeclare.BRIEFCODE.TQJZ, ctx, amtListQC, investeeId, adFeeFlag
						,"",PubDictUtils.getInvesteeFlag(ctx));
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
	public FabAmount getFeeAmt() {
		return feeAmt;
	}
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;
	}
	public LoanAgreement getLa() {
		return la;
	}
	public void setLa(LoanAgreement la) {
		this.la = la;
	}



}

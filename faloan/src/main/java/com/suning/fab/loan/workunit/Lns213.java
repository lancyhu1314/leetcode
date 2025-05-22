package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.domain.TblLnsinterfaceex;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 还款--手续费
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns213 extends WorkUnit { 

	
	String		acctNo;
	String		repayChannel;
	String 		bankSubject;
	String 		outSerialNo;
	FabAmount	feeAmt;
	String		investee;
	LnsBillStatistics lnsBillStatistics;
	
	String settleFlag;
	FabRate cleanFeeRate;
    FabAmount cleanFee = new FabAmount(0.00);
    List<Map<String, Object>> lnsaccountdyninfoList ;
    
	String		investMode;
	String		channelType;
	String		fundChannel;
	LoanAgreement loanAgreement;
	String		repayAcctNo;
	String		brc;
	FabAmount	refundAmt;
	FabAmount	repayAmt;
	FabAmount	offsetAmt;
	String 		memo;
	String		prdId;
	String		custType;
	String		customId;
	String		customName;
	String      realDate;
	Map<String,FabAmount> repayAmtMap;
	FabAmount   dynamicCapDiff;
	FabAmount feeRed  =  new FabAmount();

	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();

		if( VarChecker.isEmpty(ctx.getRequestDict("realDate")) ||
				VarChecker.isEmpty(ctx.getRequestDict("settleFlag"))
				||!VarChecker.asList("1","2").contains(ctx.getRequestDict("settleFlag").toString()) ) {
			realDate = ctx.getTranDate();
		}
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,loanAgreement);

		lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, realDate, ctx,lnsBillStatistics);


	    //2019-10-12 现金贷提前还款违约金
	    if( null != la.getBasicExtension().getFeeRate() &&
	    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
	    	la.getBasicExtension().getFeeRate().isPositive() &&
	    	"1".equals(settleFlag))
	    {
	    	cleanFeeRate = la.getBasicExtension().getFeeRate();
	    }

    	//手续费率传值时，手续费不能为空
		if( (null != cleanFeeRate && null == feeAmt ) )
			throw new FabException("LNS055","手续费");

		//手续费率不传值时，手续费不能大于0
		if( ( null == cleanFeeRate  && null != feeAmt && !feeAmt.isZero()) )
			throw new FabException("LNS136");




		if(  null != feeAmt && null != cleanFeeRate )
		{
			//云商分期退货渠道不收手续费
			if( "5".equals(repayChannel) )
				throw new FabException("LNS137");


			//放款日当天还款，手续费不收
			if(!"3".equals(la.getInterestAgreement().getAdvanceFeeType()) && la.getContract().getContractStartDate().equals(realDate) ||
				la.getContract().getContractEndDate().equals(realDate) ||
				"8".equals(la.getWithdrawAgreement().getRepayWay()) )
			{
				if( !feeAmt.isZero() )
					throw new FabException("LNS135",feeAmt);
			}
			else
			{
				//实际还款日
//				cleanFee = calCleanFee( la,lnsBillStatistics,realDate);
				if (loanAgreement.getContract().getContractStartDate().equals(realDate) &&
						"3".equals(loanAgreement.getInterestAgreement().getAdvanceFeeType())){
					realDate = CalendarUtil.nDaysAfter(realDate, 1).toString("yyyy-MM-dd");
				}

				cleanFee = AdvanceFeeUtil.calAdvanceFee( cleanFeeRate, realDate, la, lnsBillStatistics);
				//
				if(!realDate.equals(ctx.getTranDate())) {
					la.getBasicExtension().setDynamicCapDiff(dynamicCapDiff);
//					feeRed.selfAdd(calCleanFee(la, lnsBillStatistics, ctx.getTranDate()).sub(cleanFee));
					feeRed.selfAdd(cleanFee.sub(AdvanceFeeUtil.calAdvanceFee( cleanFeeRate, ctx.getTranDate(), la, lnsBillStatistics)));
					if(feeRed.isNegative())
						feeRed = new FabAmount(0.00);
					updateFeeRed(ctx);
				}
			    if( null != la.getBasicExtension().getFeeRate() &&
			    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
			    	la.getBasicExtension().getFeeRate().isPositive() )
			    {
			    	//接口服务费传值不大于计算所得服务费
					if( cleanFee.sub(feeAmt.getVal()).isNegative() )
						throw new FabException("LNS188");
			    }
			    else
			    {
			    	//接口服务费传值不等于计算所得服务费
			    	if( !cleanFee.sub(feeAmt.getVal()).isZero() )
						throw new FabException("LNS192");
			    }
			}
		}
		
		FundInvest fundInvest = new FundInvest();
		if(getRepayChannel() != null)
			fundInvest.setChannelType(getRepayChannel());
		if(getInvestee() != null)
			fundInvest.setInvestee(getInvestee());
		if(getBankSubject() != null)
			fundInvest.setFundChannel(getBankSubject());
		if(outSerialNo != null)
			fundInvest.setOutSerialNo(outSerialNo);
		
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
				new FabCurrency());
		
		if(getFeeAmt() != null && getFeeAmt().isPositive())
		{	
			LoggerUtil.debug("REFUNDAMT:" + getFeeAmt().getVal());
			eventProvider.createEvent(ConstantDeclare.EVENT.LNFEECOLCT, feeAmt, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.SSXF, ctx,"","","",PubDictUtils.getInvesteeFlag(ctx));
		}	
		
		//幂等插表
		
	}

	private void updateFeeRed(TranCtx ctx) throws FabException {
		if(!feeRed.isZero()){
			Map<String,Object> param = new HashMap<>();
			param.put("acctno", acctNo);
			param.put("brc", ctx.getBrc());
			param.put("key", ConstantDeclare.KEYNAME.ZDJM);
			TblLnsinterfaceex interfaceex;
			try {
				interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", param, TblLnsinterfaceex.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "query_lnsinterfaceex");
			}
			if(interfaceex!=null) {
				JSONObject jsonObject = JSONObject.parseObject(interfaceex.getValue());
				jsonObject.put("adjustfee", feeRed.getVal());
				//更新表
				param.put("value", jsonObject.toString());
				try {
					DbAccessUtil.execute("AccountingMode.updateInterfaceEx", param);
				} catch (FabSqlException e) {
					throw new FabException("SPS100", "lnsbasicinfoex", e);
				}
			}

		}
	}

//	private FabAmount calCleanFee(LoanAgreement la,LnsBillStatistics billStatistics,String repayDate) {
//		FabAmount totalPrin = new FabAmount(0.00);
//		//计算利息
//		FabAmount totalNint=new FabAmount(0.00);
//		//费用收取方式
//		String advanceFeeType=la.getInterestAgreement().getAdvanceFeeType();
//
//		List<LnsBill> lnsbill = new ArrayList<LnsBill>();
//
//		//取账本信息
//		lnsbill.addAll(billStatistics.getHisBillList());
//		lnsbill.addAll(billStatistics.getHisSetIntBillList());
//		lnsbill.addAll(billStatistics.getBillInfoList());
//		lnsbill.addAll(billStatistics.getFutureBillInfoList());
//		lnsbill.addAll(billStatistics.getFutureOverDuePrinIntBillList());
//		if( Arrays.asList("0","1","2","9","10").contains(la.getWithdrawAgreement().getRepayWay()))
//		{
//			for(LnsBill billNint:lnsbill){
//				//预约还款日期小于等于账本开始日期的属于未来期
//				if( CalendarUtil.beforeAlsoEqual(repayDate, billNint.getStartDate())){
//					//累加未来期未还本金
//					if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
//						totalPrin.selfAdd(billNint.getBillBal());
//					}
//					//累加未来期利息金额
//					else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(billNint.getBillType())){
//						totalNint.selfAdd(billNint.getBillBal());
//					}
//				}
//
//				//2019-10-12 现金贷提前还款违约金   开户传费率时按开户费率计算
//				if( null != la.getBasicExtension().getFeeRate() &&
//						!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
//						la.getBasicExtension().getFeeRate().isPositive() )
//				{
//					if( CalendarUtil.after(repayDate, billNint.getStartDate()) &&
//						CalendarUtil.beforeAlsoEqual(repayDate, billNint.getEndDate()) 	){
//						//累加当期本金
//						if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
//							totalPrin.selfAdd(billNint.getBillBal());
//						}
//					}
//				}
//			}
//		}
//		else
//		{
////					throw new FabException("LNS051");
//			Map<Integer,LnsBill> map = new HashMap<Integer,LnsBill>();
//			for(LnsBill bill:lnsbill){
//				LnsBill billList = map.get(bill.getPeriod());
//				if( null == billList )
//				{
//					//存利息账本开始结束日
//					bill.setBillBal( new FabAmount(0.00) );
//					map.put(bill.getPeriod(), bill);
//				}
//				else
//				{
//					//取较小的作为开始日期
//					if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
//						CalendarUtil.before(bill.getStartDate(), billList.getStartDate() ) )
//						billList.setStartDate(bill.getStartDate());
//					//取较大的作为结束日期
//					if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
//						CalendarUtil.after(bill.getEndDate(), billList.getEndDate() ) )
//						billList.setEndDate(bill.getEndDate());
//					//取本金余额作为当期本金
//					if( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())  &&
//						bill.getBillBal().sub(billList.getBillBal()).isPositive() )
//						billList.setBillBal(bill.getBillBal());
//					map.put(bill.getPeriod(), billList);
//				}
//			}
//
//			for (LnsBill value : map.values()) {
//				//预约还款日期小于等于账本开始日期的属于未来期
//				if( CalendarUtil.beforeAlsoEqual(repayDate, value.getStartDate())){
//					//累加未来期未还本金
//					totalPrin.selfAdd(value.getBillBal());
//				}
//
//				if( null != la.getBasicExtension().getFeeRate() &&
//						!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
//						la.getBasicExtension().getFeeRate().isPositive() )
//				{
//					if( CalendarUtil.after(repayDate, value.getStartDate()) &&
//						CalendarUtil.beforeAlsoEqual(repayDate, value.getEndDate()) 	){
//						//累加当期本金
//							totalPrin.selfAdd(value.getBillBal());
//						}
//				}
//			}
//		}
//
//		//2019-10-12 现金贷提前还款违约金
//		FabAmount cleanFeeAmt = new FabAmount(  BigDecimal.valueOf(totalPrin.getVal()).multiply(cleanFeeRate.getVal()).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue()    );
//		if( la.getBasicExtension().getDynamicCapAmt().isPositive() &&
//			la.getBasicExtension().getDynamicCapDiff().sub(cleanFeeAmt).isNegative())
//			cleanFeeAmt = la.getBasicExtension().getDynamicCapDiff();
//		//利息和费用大小比较
//		if(ConstantDeclare.ADVANCEFEETYPE.FIXED.equals(advanceFeeType)&&totalNint.sub(cleanFeeAmt).isNegative()){
//			//取利息和费用 最小的作为收取费用
//			cleanFeeAmt=totalNint;
//		}
//		return cleanFeeAmt;
//	}

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
	 * @return the feeAmt
	 */
	public FabAmount getFeeAmt() {
		return feeAmt;
	}
	/**
	 * @param feeAmt the feeAmt to set
	 */
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;
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
	 * @return the cleanFeeRate
	 */
	public FabRate getCleanFeeRate() {
		return cleanFeeRate;
	}
	/**
	 * @param cleanFeeRate the cleanFeeRate to set
	 */
	public void setCleanFeeRate(FabRate cleanFeeRate) {
		this.cleanFeeRate = cleanFeeRate;
	}
	/**
	 * @return the cleanFee
	 */
	public FabAmount getCleanFee() {
		return cleanFee;
	}
	/**
	 * @param cleanFee the cleanFee to set
	 */
	public void setCleanFee(FabAmount cleanFee) {
		this.cleanFee = cleanFee;
	}
	/**
	 * @return the lnsaccountdyninfoList
	 */
	public List<Map<String, Object>> getLnsaccountdyninfoList() {
		return lnsaccountdyninfoList;
	}
	/**
	 * @param lnsaccountdyninfoList the lnsaccountdyninfoList to set
	 */
	public void setLnsaccountdyninfoList(List<Map<String, Object>> lnsaccountdyninfoList) {
		this.lnsaccountdyninfoList = lnsaccountdyninfoList;
	}
	/**
	 * @return the settleFlag
	 */
	public String getSettleFlag() {
		return settleFlag;
	}
	/**
	 * @param settleFlag the settleFlag to set
	 */
	public void setSettleFlag(String settleFlag) {
		this.settleFlag = settleFlag;
	}
	/**
	 * @return the lnsBillStatistics
	 */
	public LnsBillStatistics getLnsBillStatistics() {
		return lnsBillStatistics;
	}
	/**
	 * @param lnsBillStatistics the lnsBillStatistics to set
	 */
	public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
		this.lnsBillStatistics = lnsBillStatistics;
	}
	/**
	 * @return the repayChannel
	 */
	public String getRepayChannel() {
		return repayChannel;
	}
	/**
	 * @param repayChannel the repayChannel to set
	 */
	public void setRepayChannel(String repayChannel) {
		this.repayChannel = repayChannel;
	}
	/**
	 * @return the bankSubject
	 */
	public String getBankSubject() {
		return bankSubject;
	}
	/**
	 * @param bankSubject the bankSubject to set
	 */
	public void setBankSubject(String bankSubject) {
		this.bankSubject = bankSubject;
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
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}
	/**
	 * @param loanAgreement the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}
	/**
	 * @return the repayAcctNo
	 */
	public String getRepayAcctNo() {
		return repayAcctNo;
	}
	/**
	 * @param repayAcctNo the repayAcctNo to set
	 */
	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
	}
	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}
	/**
	 * @param brc the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}
	/**
	 * @return the refundAmt
	 */
	public FabAmount getRefundAmt() {
		return refundAmt;
	}
	/**
	 * @param refundAmt the refundAmt to set
	 */
	public void setRefundAmt(FabAmount refundAmt) {
		this.refundAmt = refundAmt;
	}
	/**
	 * @return the repayAmt
	 */
	public FabAmount getRepayAmt() {
		return repayAmt;
	}
	/**
	 * @param repayAmt the repayAmt to set
	 */
	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}
	/**
	 * @return the offsetAmt
	 */
	public FabAmount getOffsetAmt() {
		return offsetAmt;
	}
	/**
	 * @param offsetAmt the offsetAmt to set
	 */
	public void setOffsetAmt(FabAmount offsetAmt) {
		this.offsetAmt = offsetAmt;
	}
	/**
	 * @return the memo
	 */
	public String getMemo() {
		return memo;
	}
	/**
	 * @param memo the memo to set
	 */
	public void setMemo(String memo) {
		this.memo = memo;
	}
	/**
	 * @return the prdId
	 */
	public String getPrdId() {
		return prdId;
	}
	/**
	 * @param prdId the prdId to set
	 */
	public void setPrdId(String prdId) {
		this.prdId = prdId;
	}
	/**
	 * @return the custType
	 */
	public String getCustType() {
		return custType;
	}
	/**
	 * @param custType the custType to set
	 */
	public void setCustType(String custType) {
		this.custType = custType;
	}
	/**
	 * @return the customId
	 */
	public String getCustomId() {
		return customId;
	}
	/**
	 * @param customId the customId to set
	 */
	public void setCustomId(String customId) {
		this.customId = customId;
	}
	/**
	 * @return the customName
	 */
	public String getCustomName() {
		return customName;
	}
	/**
	 * @param customName the customName to set
	 */
	public void setCustomName(String customName) {
		this.customName = customName;
	}
	/**
	 * @return the repayAmtMap
	 */
	public Map<String, FabAmount> getRepayAmtMap() {
		return repayAmtMap;
	}
	/**
	 * @param repayAmtMap the repayAmtMap to set
	 */
	public void setRepayAmtMap(Map<String, FabAmount> repayAmtMap) {
		this.repayAmtMap = repayAmtMap;
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
	 * Gets the value of realDate.
	 *
	 * @return the value of realDate
	 */
	public String getRealDate() {
		return realDate;
	}

	/**
	 * Sets the realDate.
	 *
	 * @param realDate realDate
	 */
	public void setRealDate(String realDate) {
		this.realDate = realDate;

	}

	/**
	 * Gets the value of dynamicCapDiff.
	 *
	 * @return the value of dynamicCapDiff
	 */
	public FabAmount getDynamicCapDiff() {
		return dynamicCapDiff;
	}

	/**
	 * Sets the dynamicCapDiff.
	 *
	 * @param dynamicCapDiff dynamicCapDiff
	 */
	public void setDynamicCapDiff(FabAmount dynamicCapDiff) {
		this.dynamicCapDiff = dynamicCapDiff;

	}

	/**
	 * Gets the value of feeRed.
	 *
	 * @return the value of feeRed
	 */
	public FabAmount getFeeRed() {
		return feeRed;
	}

	/**
	 * Sets the feeRed.
	 *
	 * @param feeRed feeRed
	 */
	public void setFeeRed(FabAmount feeRed) {
		this.feeRed = feeRed;

	}
}

package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsprefundaccount;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanRepayPlanProvider;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * see 开户--放款
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns202 extends WorkUnit { 

	String		openBrc;
	String		receiptNo;
	String		merchantNo;
	FabAmount	contractAmt;
	String		loanType;
	Integer		graceDays;
	String		calcIntFlag1;
	String		calcIntFlag2;
	String		startIntDate;
	FabRate		normalRate;
	FabRate		overdueRate;
	FabRate		compoundRate;
	String		normalRateType;
	String		customType;
	String		customName;
	String		productCode;
	String		repayWay;
	String		debtTotalCompany;
	FabAmount	debtTotalAmt = new FabAmount(0.00);
	Integer 	expandPeriod;
	FabRate     feeRate;
	String 		channelType;
    String		switchloanType;				//借新还旧类型 1-房抵贷债务重组  2-任性付账单分期  3-任性付最低还款额
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName="";
		
		TranCtx ctx = getTranctx();
		ListMap pkgList = ctx.getRequestDict("pkgList");
		Map<String,String> jsonStr = new HashMap<>();
		//
		FabAmount sumAmt = new FabAmount();
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		FundInvest  fundInvest = new FundInvest();
		fundInvest.setChannelType(channelType);
		add.operate(lnsAcctInfo, null, contractAmt, fundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);
		
		
		//2019-10-12 现金贷提前还款违约金
		if( null != feeRate && !VarChecker.isEmpty(feeRate) &&
			feeRate.isPositive()	)
		{
			//修改主文件拓展表
			Map<String,Object> exParam = new HashMap<>();
			exParam.put("acctno", receiptNo);
			exParam.put("brc", ctx.getBrc());
			exParam.put("key", ConstantDeclare.BASICINFOEXKEY.TQHK);
			exParam.put("value1", "");
			exParam.put("value2", 0.00);
			exParam.put("value3", feeRate.getYearRate().setScale(6,BigDecimal.ROUND_HALF_UP));
			exParam.put("tunneldata", "");
			
			try {
				DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfoex");
			}
			
		}
				
				
		//2019-09-26 气球贷存膨胀期数
		if( ConstantDeclare.REPAYWAY.REPAYWAY_QQD.equals(repayWay) )
		{
			//修改主文件拓展表
			Map<String,Object> exParam = new HashMap<>();
			exParam.put("acctno", receiptNo);
			exParam.put("brc", ctx.getBrc());
			exParam.put("key", ConstantDeclare.BASICINFOEXKEY.PZQS);
			exParam.put("value1", expandPeriod.toString());
			exParam.put("value2", 0.00);
			exParam.put("value3", 0.00);
			exParam.put("tunneldata", "");
			
			try {
				DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfoex");
			}
			
			//膨胀期数不能小于当前总期数
			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(receiptNo, ctx);
			String sDate = la.getContract().getContractStartDate();
			Integer periods = 0;
			while (CalendarUtil.actualDaysBetween(sDate, la.getContract().getContractEndDate()) > 0) {
				periods += 1;
				sDate = RepayPeriodSupporter.calculateEndDate(sDate, la.getContract().getContractStartDate(),
						la.getContract().getContractEndDate(),
						la.getWithdrawAgreement().getPeriodMinDays(),
						RepayPeriodSupporter.genRepayPeriod(la.getWithdrawAgreement().getPeriodFormula()));
			}
			if( periods > expandPeriod )
				throw new FabException("LNS186");
		}
		
		if(pkgList == null || pkgList.size() == 0)
		{
			//无追保理和买方付息产品的债务公司必输
			if( Arrays.asList("3010006","3010013","3010014","3010015").contains(productCode))
			{
				throw new FabException("LNS055","债务公司");
			}
			
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANGRANTA, contractAmt, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx,ConstantDeclare.SWITCHLOANTYPE.isSwitchType(switchloanType)?switchloanType:"");
		}
		else if(pkgList.size() > 0){
			int i = 0;//预收户登记簿子序号
			for (PubDict pkg:pkgList.getLoopmsg()) {
				String debtCompany = PubDict.getRequestDict(pkg, "debtCompany");
				FabAmount debtAmt = PubDict.getRequestDict(pkg, "debtAmt");

				if(jsonStr.containsKey(debtCompany)){
					jsonStr.put(debtCompany,new FabAmount(debtAmt.add(Double.valueOf(jsonStr.get(debtCompany))).getVal()).toString() );
				}else{
					jsonStr.put(debtCompany, debtAmt.toString());
				}

				/**
				 * 3010013:无追保理-买方付息
				 * 3010015:非标-买方付息
				 * 20190528|14050183
				 */
				if( Arrays.asList("3010013","3010015").contains(productCode) )
				{
					/**
					 * 买方付息只允许有一个债务公司 	20190528|14050183
					 * debtTotalCompany在for循环每次循环最后赋值
					 */
					if( pkgList.size() > 1 &&
						!VarChecker.isEmpty(debtTotalCompany) &&
						!debtCompany.equals(debtTotalCompany))
							throw new FabException("LNS122");
				}
				
				
				Map<String,Object> lns001map = new HashMap<>();
				lns001map.put("brc", ctx.getBrc());
				lns001map.put("customid", debtCompany);
				lns001map.put("accsrccode", "D");
				lns001map.put("balance", debtAmt.getVal());

				TblLnsprefundaccount lnsprefundaccount1 ;
				try {
					lnsprefundaccount1 = DbAccessUtil.queryForObject("CUSTOMIZE.update_lnsprefundaccountcustom_add", lns001map, TblLnsprefundaccount.class);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS102", "lnsprefundaccount");
				}
				if(null == lnsprefundaccount1){
					throw new FabException("LNS017");
				}
				i++;
				//债务公司 贷款开户
				AccountingModeChange.saveLnsprefundsch(ctx, i, lnsprefundaccount1.getAcctno(), lnsprefundaccount1.getCustomid(), "D",lnsprefundaccount1.getCusttype() ,
						lnsprefundaccount1.getName() ,debtAmt.getVal() ,"add" );

				
				lnsAcctInfo.setMerchantNo(getMerchantNo());
				lnsAcctInfo.setCustType(getCustomType());
				lnsAcctInfo.setCustName("");
				lnsAcctInfo.setPrdCode(getProductCode());
				//抛事件
				//eventProvider.createEvent("LOANGRANTA", debtAmt, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx, getMerchantNo(), debtCompany, ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY)
				/**
				 * 20190610保费分期版本|14050183
				 * 事件代码LOANGRANTA修改为RECGDEBTCO
				 * 摘要BJFK修改为ZWCZ
				 */
				eventProvider.createEvent(ConstantDeclare.EVENT.RECGDEBTCO, debtAmt, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.ZWCZ, ctx, getMerchantNo(), debtCompany, ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);

				
				//累加循环报文金额
				sumAmt.selfAdd(debtAmt);
				
				debtTotalCompany = debtCompany;
				debtTotalAmt.selfAdd(debtAmt);
			}
			
			/**
			 * 20190610保费分期版本|14050183
			 * 债务公司明细汇总抛事件代码LOANGRANTA
			 */
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANGRANTA, contractAmt, lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);

			//2018-12-10 买方付息登记债务公司信息到主文件拓展表
			if( Arrays.asList("3010013","3010015").contains(productCode)  )
			{
				
				//修改主文件拓展表
				Map<String,Object> exParam = new HashMap<>();
				exParam.put("acctno", receiptNo);
				exParam.put("brc", ctx.getBrc());
				exParam.put("key", ConstantDeclare.BASICINFOEXKEY.MFFX);
				exParam.put("value1", debtTotalCompany);
				exParam.put("value2", debtTotalAmt.getVal());
				exParam.put("value3", 0.00);
				exParam.put("tunneldata", "");
				
				try {
					DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS102", "lnsbasicinfoex");
				}
			}
			
			//2018-12-17 无追保理登记债务公司信息到主文件拓展表
			if( Arrays.asList("3010006","3010014").contains(productCode) )
			{
				//修改主文件拓展表
				Map<String,Object> exParam = new HashMap<>();
				exParam.put("acctno", receiptNo);
				exParam.put("brc", ctx.getBrc());
				exParam.put("key", ConstantDeclare.BASICINFOEXKEY.WZBL);
				exParam.put("value1", "");
				exParam.put("value2", 0.00);
				exParam.put("value3", 0.00);
				exParam.put("tunneldata", JsonTransfer.ToJson(jsonStr));
				
				try {
					DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS102", "lnsbasicinfoex");
				}
			}
			
			
			
			sumAmt.selfSub(contractAmt);
			if (!sumAmt.isZero()) {
				throw new FabException("LNS028");
			}
		}
		

		if( !"473005".equals(ctx.getTranCode()))
			LoanRepayPlanProvider.interestRepayPlan( ctx,receiptNo,"OPEN");
	}
	public String getRepayWay() {
		return repayWay;
	}
	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
	}
	public FabRate getCompoundRate() {
		return compoundRate;
	}
	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
	}
	public String getCustomType() {
		return customType;
	}
	public void setCustomType(String customType) {
		this.customType = customType;
	}
	public String getCustomName() {
		return customName;
	}
	public void setCustomName(String customName) {
		this.customName = customName;
	}
	public String getProductCode() {
		return productCode;
	}
	public void setProductCode(String productCode) {
		this.productCode = productCode;
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
	 * @return the add
	 */
	public AccountOperator getAdd() {
		return add;
	}
	/**
	 * @param add the add to set
	 */
	public void setAdd(AccountOperator add) {
		this.add = add;
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
	public String getLoanType() {
		return loanType;
	}
	public void setLoanType(String loanType) {
		this.loanType = loanType;
	}
	public Integer getGraceDays() {
		return graceDays;
	}
	public void setGraceDays(Integer graceDays) {
		this.graceDays = graceDays;
	}
	public String getCalcIntFlag1() {
		return calcIntFlag1;
	}
	public void setCalcIntFlag1(String calcIntFlag1) {
		this.calcIntFlag1 = calcIntFlag1;
	}
	public String getCalcIntFlag2() {
		return calcIntFlag2;
	}
	public void setCalcIntFlag2(String calcIntFlag2) {
		this.calcIntFlag2 = calcIntFlag2;
	}
	public String getStartIntDate() {
		return startIntDate;
	}
	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
	}
	public FabRate getNormalRate() {
		return normalRate;
	}
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}
	public FabRate getOverdueRate() {
		return overdueRate;
	}
	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
	}
	public FabRate getcompoundRate() {
		return compoundRate;
	}
	public void setcompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
	}
	public String getNormalRateType() {
		return normalRateType;
	}
	public void setNormalRateType(String normalRateType) {
		this.normalRateType = normalRateType;
	}

	public String getDebtTotalCompany() {
		return debtTotalCompany;
	}

	public FabAmount getDebtTotalAmt() {
		return debtTotalAmt;
	}

    public void setDebtTotalCompany(String debtTotalCompany) {
        this.debtTotalCompany = debtTotalCompany;
    }

    public void setDebtTotalAmt(FabAmount debtTotalAmt) {
        this.debtTotalAmt = debtTotalAmt;
    }
	/**
	 * @return the expandPeriod
	 */
	public Integer getExpandPeriod() {
		return expandPeriod;
	}
	/**
	 * @param expandPeriod the expandPeriod to set
	 */
	public void setExpandPeriod(Integer expandPeriod) {
		this.expandPeriod = expandPeriod;
	}
	/**
	 * @return the feeRate
	 */
	public FabRate getFeeRate() {
		return feeRate;
	}
	/**
	 * @param feeRate the feeRate to set
	 */
	public void setFeeRate(FabRate feeRate) {
		this.feeRate = feeRate;
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
	 * @return the switchloanType
	 */
	public String getSwitchloanType() {
		return switchloanType;
	}
	/**
	 * @param switchloanType the switchloanType to set
	 */
	public void setSwitchloanType(String switchloanType) {
		this.switchloanType = switchloanType;
	}


}

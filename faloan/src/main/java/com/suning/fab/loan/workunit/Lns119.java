package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.FaLoanPubDict;
import com.suning.fab.loan.account.LnsRentPlanCalculate;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsnonstdplan;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.VarChecker;



/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 开户-非标贷款本金账户
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns119 extends WorkUnit { 

	FabRate		compoundRate;		//复利利率
	String		loanType;			//贷款类型 1-普通 2-按揭
	FabRate		normalRate;			//正常利率
	String		normalRateType;		//正常利率类型
	FabRate		overdueRate;		//逾期利率
	String		repayWay;			//还款方式
	String		serialNo;			//幂等流水号
	String		termDate;			//交易日期
	String		tranCode;			//交易代码
	String		merchantNo;			//商户号
	String		customName;			//户名
	String		customType;			//客户类型
	String		oldrepayacct;		//历史预收账号
	FabAmount	contractAmt;		//合同金额
	String		receiptNo;			//借据号
	String		startIntDate;		//起息日期
	String		endDate;			//合同到期日
	Integer		graceDays;			//宽限期
	String		contractNo;			//合同编号
	String		fundChannel;		//科目
	String		channelType;		//渠道编码
	String		outSerialNo;		//外部流水号
	String		productCode;		//产品编码
	String		calcIntFlag1;		
	String		calcIntFlag2;
	String		acctNoNon;
	
	List<LnsRentPlanCalculate> rentPlanpkgList = new ArrayList<LnsRentPlanCalculate>();


	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		FabAmount totalInt = new FabAmount(0.00);		//利息总额

				
		ListMap pkgListInMap = new ListMap();
		ListMap 	listMap = ctx.getRequestDict("pkgList1");
		if(rentPlanpkgList.isEmpty())
		{
			pkgListInMap=listMap;
		}
		else
		{
			ArrayList<PubDict> list = new ArrayList<PubDict>();
			for(LnsRentPlanCalculate plan:rentPlanpkgList){
				list.add(MapperUtil.map(plan, FaLoanPubDict.class));
			}
			pkgListInMap.setLoopmsg(list);
		}
		
		
		
		int			i = 0;
		FabAmount	contractAmtTmp = new FabAmount(getContractAmt().getVal());
		
		//非标准还款计划插表lnsnonstdplan
		if(pkgListInMap == null || pkgListInMap.size() == 0)
			throw new FabException("LNS046");
		
		List<Map> sortList =  pkgListInMap.ToList();
		//按期数从小到大顺序对list拍讯
		Collections.sort(sortList,
				new Comparator<Map>() {
					@Override
					public int compare(Map o1, Map o2) {
						Integer p1 =  Integer.parseInt(o1.get("repayTerm").toString());
						Integer p2 =  Integer.parseInt(o2.get("repayTerm").toString());
						return p1.compareTo(p2);
					}
		});
		FabAmount sumAmt = new FabAmount();
		TblLnsnonstdplan lnsnonstdplan =  new TblLnsnonstdplan();
		//TblLnsrpyplan rpyPlan = new TblLnsrpyplan();
		TblLnsrpyplan rpyPlan = new TblLnsrpyplan();
		//对接口list按期数从小到大做排序DOTO
		String	termeDateTmp = "";
		for (Map sortListTmp:sortList) {
			i++;

			
			//非标拓展表
			//试算用	20190528|14050183
			lnsnonstdplan.setAcctno(getReceiptNo());
			lnsnonstdplan.setOpenbrc(ctx.getBrc());
			lnsnonstdplan.setRepayway(getRepayWay());
			lnsnonstdplan.setCalcintflag1("");
			lnsnonstdplan.setCalcintflag2("");
			lnsnonstdplan.setIntformula("3");
			lnsnonstdplan.setPrinformula("3");
			//期数
			Integer repayTerm = Integer.parseInt(sortListTmp.get("repayTerm").toString());
			lnsnonstdplan.setRepayterm(repayTerm);
			if(!repayTerm.equals(i))
			{
				//期数不正确
				throw new FabException("LNS158");
			}
			//本期起日
			if(termeDateTmp.compareTo(sortListTmp.get("termeDate").toString().trim() ) >= 0)
			{
				LoggerUtil.error("每期结束日期必须大于上期结束日期:"+"termenddatetmp["+termeDateTmp
				+"]"+"termenddate["+sortListTmp.get("termeDate").toString().trim());
				throw new FabException("LNS161");
			}
			if(repayTerm.equals(1))
				lnsnonstdplan.setTermbdate(ctx.getTranDate());
			else
				lnsnonstdplan.setTermbdate(termeDateTmp);
			//本期止日
			String	termeDate = sortListTmp.get("termeDate").toString().trim();
			lnsnonstdplan.setTermedate(termeDate);
			termeDateTmp = termeDate;
			if(ctx.getTranDate().compareTo(termeDate) >= 0)
			{
				LoggerUtil.error("结束日期必须大于起始日期:"+"startdate["+ctx.getTranDate()
				+"]"+"termenddate["+termeDate);
				throw new FabException("LNS162");
			}
			//宽限期
			int days = -1;
			if(sortListTmp.containsKey("days"))
				days = Integer.parseInt( sortListTmp.get("days").toString());
			if(days != -1)
				lnsnonstdplan.setDays(Integer.valueOf(days));
			else
				lnsnonstdplan.setDays(getGraceDays());
			//计息起止日
			if(!VarChecker.isEmpty(getStartIntDate()) && lnsnonstdplan.getRepayterm().equals(1)){
				lnsnonstdplan.setIntbdate(getStartIntDate());
				if( Date.valueOf(getStartIntDate()).after(Date.valueOf(ctx.getTranDate() )) )
				{
					LoggerUtil.error("起息日期不能大于合同开户日期:"+"STARTINTDATE["+getStartIntDate()
							+"]"+"OPENDATE["+ctx.getTranDate()+"]");
							throw new FabException("LNS025");
				}
			}
			else
				lnsnonstdplan.setIntbdate(lnsnonstdplan.getTermbdate());
			lnsnonstdplan.setIntedate(lnsnonstdplan.getTermedate());
			//应还本金
			FabAmount  termPrin = new FabAmount(0.00);
			if( !VarChecker.isEmpty(sortListTmp.get("termPrin")) )
				termPrin = new FabAmount(Double.parseDouble(sortListTmp.get("termPrin").toString()));
			lnsnonstdplan.setTermprin(termPrin.getVal());
			contractAmtTmp.selfSub(termPrin);
			//应还利息
			FabAmount  termInt = new FabAmount(0.00);
			if( !VarChecker.isEmpty(sortListTmp.get("termInt")) )
				 termInt = new FabAmount(Double.parseDouble(sortListTmp.get("termInt").toString()));
			lnsnonstdplan.setTermint(termInt.getVal());
			
			totalInt.selfAdd(lnsnonstdplan.getTermint());
			//本金余额
			lnsnonstdplan.setBalance(contractAmtTmp.getVal());
			//利率
			lnsnonstdplan.setNormalrate(0.00);
			lnsnonstdplan.setOverduerate(0.00);
			lnsnonstdplan.setOverduerate1(0.00);
			lnsnonstdplan.setRatetype("Y");
			lnsnonstdplan.setModifydate(ctx.getTranDate());
			//公式
			LoanAgreement la = LoanAgreementProvider.genLoanAgreement(getProductCode());
			LoanAgreementProvider.genNonLoanAgreementFromRepayWay(la, ctx, 
					lnsnonstdplan.getTermedate().substring(8,10), 
					lnsnonstdplan.getIntbdate(), lnsnonstdplan.getIntedate(), 
					lnsnonstdplan.getTermbdate(), lnsnonstdplan.getTermedate());
			lnsnonstdplan.setPrinperformula(la.getWithdrawAgreement().getPeriodFormula());
			lnsnonstdplan.setIntperformula(la.getInterestAgreement().getPeriodFormula());
			lnsnonstdplan.setLastprindate(lnsnonstdplan.getTermbdate()); 
			lnsnonstdplan.setLastintdate(lnsnonstdplan.getIntbdate()); 
			
			
			
			
			
			//还款计划辅助表
			//还款计划查询用	20190528|14050183
			rpyPlan.setAcctno(getReceiptNo());
			rpyPlan.setBrc(ctx.getBrc());
			rpyPlan.setRepayterm(repayTerm);
			rpyPlan.setRepayintedate(lnsnonstdplan.getTermedate());
			rpyPlan.setAcctflag("");
			rpyPlan.setRepayway("");
			rpyPlan.setRepayownbdate(lnsnonstdplan.getTermedate());
			DateTime repayownedatetmp = CalendarUtil.nDaysAfter(lnsnonstdplan.getTermedate(),lnsnonstdplan.getDays().intValue());
			rpyPlan.setRepayownedate( repayownedatetmp.toString("yyyy-MM-dd") );
			rpyPlan.setRepayintbdate(lnsnonstdplan.getIntbdate());
			rpyPlan.setTermretprin(termPrin.getVal());
			rpyPlan.setTermretint(termInt.getVal());
			rpyPlan.setBalance(contractAmtTmp.getVal());
			rpyPlan.setActrepaydate(lnsnonstdplan.getTermedate());

			sumAmt.selfAdd(termPrin);	
			try{
				DbAccessUtil.execute("Lnsnonstdplan.insert",lnsnonstdplan);		
			}catch (FabException e){
				LoggerUtil.error("SQLCODE"+e.getErrCode());
				throw new FabException(e, "SPS100", "lnsnonstdplan");
			}
			
			try{
				DbAccessUtil.execute("Lnsrpyplan.insert",rpyPlan);		
			}catch (FabException e){
				throw new FabException(e, "SPS100", "lnsrpyPlan");
			}
			

		}
		
		if(termeDateTmp.compareTo(getEndDate()) != 0)
		{
			LoggerUtil.error("明细最后结束日期必须等于合同结束日期:"+"termeDateTmp["+termeDateTmp
			+"]"+"termenddate["+getEndDate());
			throw new FabException("LNS160");
		}	
		sumAmt.selfSub(contractAmt);
		if (!sumAmt.isZero()) {
			LoggerUtil.error("明细金额之和与合同总金额不符");
			throw new FabException("LNS159");
		}
		
		//修改主文件拓展表
		Map<String,Object> exParam = new HashMap<>();
		exParam.put("acctno", receiptNo);
		exParam.put("brc", ctx.getBrc());
		exParam.put("key", "ZDY");
		exParam.put("value1", "");
		exParam.put("value2", totalInt.getVal());
		exParam.put("value3", 0.00);
		exParam.put("tunneldata", "");
		
		try {
			DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsbasicinfoex");
		}
	}
	/**
	 * @return the compoundRate
	 */
	public FabRate getCompoundRate() {
		return compoundRate;
	}
	/**
	 * @param compoundRate the compoundRate to set
	 */
	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
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
	 * @return the normalRate
	 */
	public FabRate getNormalRate() {
		return normalRate;
	}
	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}
	/**
	 * @return the normalRateType
	 */
	public String getNormalRateType() {
		return normalRateType;
	}
	/**
	 * @param normalRateType the normalRateType to set
	 */
	public void setNormalRateType(String normalRateType) {
		this.normalRateType = normalRateType;
	}
	/**
	 * @return the overdueRate
	 */
	public FabRate getOverdueRate() {
		return overdueRate;
	}
	/**
	 * @param overdueRate the overdueRate to set
	 */
	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
	}
	/**
	 * @return the repayWay
	 */
	public String getRepayWay() {
		return repayWay;
	}
	/**
	 * @param repayWay the repayWay to set
	 */
	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
	}
	/**
	 * @return the serialNo
	 */
	public String getSerialNo() {
		return serialNo;
	}
	/**
	 * @param serialNo the serialNo to set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	/**
	 * @return the termDate
	 */
	public String getTermDate() {
		return termDate;
	}
	/**
	 * @param termDate the termDate to set
	 */
	public void setTermDate(String termDate) {
		this.termDate = termDate;
	}
	/**
	 * @return the tranCode
	 */
	public String getTranCode() {
		return tranCode;
	}
	/**
	 * @param tranCode the tranCode to set
	 */
	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
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
	 * @return the customType
	 */
	public String getCustomType() {
		return customType;
	}
	/**
	 * @param customType the customType to set
	 */
	public void setCustomType(String customType) {
		this.customType = customType;
	}
	/**
	 * @return the oldrepayacct
	 */
	public String getOldrepayacct() {
		return oldrepayacct;
	}
	/**
	 * @param oldrepayacct the oldrepayacct to set
	 */
	public void setOldrepayacct(String oldrepayacct) {
		this.oldrepayacct = oldrepayacct;
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
	 * @return the startIntDate
	 */
	public String getStartIntDate() {
		return startIntDate;
	}
	/**
	 * @param startIntDate the startIntDate to set
	 */
	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
	}
	/**
	 * @return the endDate
	 */
	public String getEndDate() {
		return endDate;
	}
	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	/**
	 * @return the graceDays
	 */
	public Integer getGraceDays() {
		return graceDays;
	}
	/**
	 * @param graceDays the graceDays to set
	 */
	public void setGraceDays(Integer graceDays) {
		this.graceDays = graceDays;
	}
	/**
	 * @return the contractNo
	 */
	public String getContractNo() {
		return contractNo;
	}
	/**
	 * @param contractNo the contractNo to set
	 */
	public void setContractNo(String contractNo) {
		this.contractNo = contractNo;
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
	 * @return the productCode
	 */
	public String getProductCode() {
		return productCode;
	}
	/**
	 * @param productCode the productCode to set
	 */
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}
	/**
	 * @return the calcIntFlag1
	 */
	public String getCalcIntFlag1() {
		return calcIntFlag1;
	}
	/**
	 * @param calcIntFlag1 the calcIntFlag1 to set
	 */
	public void setCalcIntFlag1(String calcIntFlag1) {
		this.calcIntFlag1 = calcIntFlag1;
	}
	/**
	 * @return the calcIntFlag2
	 */
	public String getCalcIntFlag2() {
		return calcIntFlag2;
	}
	/**
	 * @param calcIntFlag2 the calcIntFlag2 to set
	 */
	public void setCalcIntFlag2(String calcIntFlag2) {
		this.calcIntFlag2 = calcIntFlag2;
	}
	/**
	 * @return the acctNoNon
	 */
	public String getAcctNoNon() {
		return acctNoNon;
	}
	/**
	 * @param acctNoNon the acctNoNon to set
	 */
	public void setAcctNoNon(String acctNoNon) {
		this.acctNoNon = acctNoNon;
	}
	/**
	 * @return the rentPlanpkgList
	 */
	public List<LnsRentPlanCalculate> getRentPlanpkgList() {
		return rentPlanpkgList;
	}
	/**
	 * @param rentPlanpkgList the rentPlanpkgList to set
	 */
	public void setRentPlanpkgList(List<LnsRentPlanCalculate> rentPlanpkgList) {
		this.rentPlanpkgList = rentPlanpkgList;
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
	

}

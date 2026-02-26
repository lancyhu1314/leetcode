package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.util.Arrays;

import com.suning.fab.tup4j.utils.AmountUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 
 *
 * @version V1.0.0
 *
 * @see -开户校验
 *
 *
 * @return
 *
 * @exception
 */

@Scope("prototype")
@Repository
public class Lns521 extends WorkUnit { 
	
	LoanAgreement la;
	FabRate normalRate;			//正常利率
	FabRate overdueRate;		//罚息利率
	FabRate compoundRate;		//复利利率
	
	Integer graceDays;			//宽限期
	
	String  receiptNo;			//借据号
	String  openBrc;			//开户机构
	String  repayWay;			//还款方式
	Integer expandPeriod;		//膨胀期数
	String	productCode;		//产品代码
	
	String	customType;			//客户类别
	String	merchantNo;			//商户号
	
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		
		//必输字段校验
		if( VarChecker.isEmpty(customType) ||
			VarChecker.isEmpty(merchantNo)	){
			throw new FabException("SPS107","客户类别/商户号");
		}
				
		//公共报文机构放款机构一致性校验
		if(!ctx.getBrc().equals(openBrc) && !"473005".equals(ctx.getTranCode())){
			throw new FabException("LNS113");
		}


		//利率校验
		if( VarChecker.isEmpty(normalRate) ){
			throw new FabException("LNS055","正常利率");
		}
		if( VarChecker.isEmpty(overdueRate) ){
			throw new FabException("LNS055","逾期利率");
		}
		if( VarChecker.isEmpty(compoundRate) ){
			throw new FabException("LNS055","复利利率");
		}
		
		//罚息利率不能小于正常利率 2019-01-14
		if(  (overdueRate.getVal().compareTo(normalRate.getVal())<0 && !new FabAmount(overdueRate.getVal().doubleValue()).isZero())  || 
			 (compoundRate.getVal().compareTo(normalRate.getVal())<0 && !new FabAmount(compoundRate.getVal().doubleValue()).isZero())  )
		{
			//2019-10-28 房抵贷无款限制 不限制
			if( !"2412615".equals(productCode) )
				throw new FabException("LNS128");
		}
		//利率不能大于封顶利率
		if(!VarChecker.isEmpty(la.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()))) {
			if("51230004".equals(ctx.getBrc())){
				ListMap pkgList1 = ctx.getRequestDict("pkgList1");
				if( !VarChecker.isEmpty(pkgList1) ){
					//没有多资金方
					for(PubDict pkg:pkgList1.getLoopmsg()) {
						if (!VarChecker.asList("R5103", "R5126").contains(PubDict.getRequestDict(pkg, "investeeId").toString()))
							la.getInterestAgreement().setDynamicCapRate(new JSONObject());
					}
				}
			}

		}
		if(!VarChecker.isEmpty(la.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()))) {
			if (BigDecimal.valueOf(la.getInterestAgreement().queryDynamicCapRate(ctx.getBrc())).compareTo(normalRate.getVal()) < 0
					|| BigDecimal.valueOf(la.getInterestAgreement().queryDynamicCapRate(ctx.getBrc())).compareTo(overdueRate.getVal()) < 0
					|| BigDecimal.valueOf(la.getInterestAgreement().queryDynamicCapRate(ctx.getBrc())).compareTo(compoundRate.getVal()) < 0) {
				throw new FabException("LNS187", la.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()));
			}
		}
		//账速融接入核心不允许有宽限期  2019-02-14
		//融担代偿 不允许有宽限期
		if( (VarChecker.asList("3010014").contains(la.getPrdId()) || "true".equals(la.getInterestAgreement().getIsCompensatory())) &&
			!VarChecker.isEmpty(graceDays)  &&
			graceDays > 0  ){
			throw new FabException("LNS134");
		}
		
		//产品配置可用还款方式后校验
		if( !"NO".equals( la.getWithdrawAgreement().getUseRepayWay() ) )
		{
			String [] repayWayArray= la.getWithdrawAgreement().getUseRepayWay().split("/"); 
		    if( !Arrays.asList(repayWayArray).contains(repayWay) )
		    	throw new FabException("LNS152", la.getPrdId(), repayWay);
		}
		
		//非标自定义债务公司校验
		if( !VarChecker.isEmpty(ctx.getRequestDict("repayWay")) && 
			Arrays.asList(ConstantDeclare.REPAYWAY.REPAYWAY_XXHB,
					      ConstantDeclare.REPAYWAY.REPAYWAY_XBHX,
					      ConstantDeclare.REPAYWAY.REPAYWAY_YBYX).contains((ctx.getRequestDict("repayWay").toString())))
		{
			if( !VarChecker.isEmpty(ctx.getRequestDict("pkgList")) && 
				ctx.getRequestDict("pkgList").toString().length() > 0)
	    	throw new FabException("LNS163");
		}
		
		if( !VarChecker.isEmpty(ctx.getRequestDict("repayWay")) )
		{
			if( ConstantDeclare.REPAYWAY.REPAYWAY_DEBX.equals(ctx.getRequestDict("repayWay").toString()))
				if( normalRate.isZero())
					throw new FabException("LNS228");
					
			if(	Arrays.asList(ConstantDeclare.REPAYWAY.REPAYWAY_QQD).contains((ctx.getRequestDict("repayWay").toString())) )
			{
				//气球贷还款方式必须有膨胀期数
				if( null == expandPeriod ||
						VarChecker.isEmpty(expandPeriod) ||
						expandPeriod < 1 )
						throw new FabException("LNS184");
			}
			else
			{
				//非气球贷不允许有膨胀期数
				if( null != expandPeriod &&
					!VarChecker.isEmpty(expandPeriod) &&
					expandPeriod > 0 )
						throw new FabException("LNS185");
			}
			
			
		}


		if("2412615".equals(la.getPrdId())){
			//房抵贷利率不能大于封顶利率
			Double rate = Double.valueOf(GlobalScmConfUtil.getProperty("dayCapRate", "24")) / 100.0D;
			rate = AmountUtil.round(rate, 6);
			//封顶利率
			FabRate capRate=new FabRate(rate);
			if(capRate.getVal().compareTo(normalRate.getVal())<0){
				throw new FabException("LNS187");
			}
            //房抵贷产品不允许有宽限期
			if (graceDays>0) {
				throw new FabException("LNS138");
			}



		}

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
	 * @return the la
	 */
	public LoanAgreement getLa() {
		return la;
	}

	/**
	 * @param la the la to set
	 */
	public void setLa(LoanAgreement la) {
		this.la = la;
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

}


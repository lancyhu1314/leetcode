package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * @author 	14050183
 *
 * @version V1.0.1
 *
 *预收账户查询
 *
 * merchantNo		商户号
 *
 * @return	acctNo			预收账号
 *			customName		户名
 *			openDate		开户日期
 *			bal				余额
 *			merchantNo		商户号
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns403 extends WorkUnit {

	String merchantNo;
	
	String acctNo;
	String repayAcctNo;
	String customName;
	String openDate;
	FabAmount bal;
	FabAmount surplusbal;

	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName="";
		TranCtx ctx = getTranctx();
		
		if(VarChecker.isEmpty(merchantNo) && VarChecker.isEmpty(repayAcctNo)){
			throw new FabException("ACC109");
		}

//		//查询预收登记薄
//		TblLnsprefundaccount lnsprefundaccount;
//		Map<String, Object> params = new HashMap<String, Object>();
//		params.put("brc", ctx.getBrc());
//		params.put("acctno", repayAcctNo);
//		params.put("customid", merchantNo);
//		params.put("accsrccode", "N");
//
//		try{
//			lnsprefundaccount = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprefundaccount", params, TblLnsprefundaccount.class);
//		}catch(FabSqlException e){
//			throw new FabException(e, "SPS103", "lnsprefundaccount");
//		}
//		if(null == lnsprefundaccount){
//			throw new FabException("SPS104", "lnsprefundaccount");
//		}
//		if(VarChecker.asList(ConstantDeclare.STATUS.CANCEL).contains(lnsprefundaccount.getStatus())){
//			throw new FabException("ACC108", merchantNo);
//		}
		
		// 查新预收户信息
		TblLnsassistdyninfo preaccountInfo = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), repayAcctNo, merchantNo, ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,ctx);
		TblLnsassistdyninfo preaccountInfo_C = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), repayAcctNo, merchantNo, ConstantDeclare.ASSISTACCOUNT.SURPLUSACCT,ctx);

		if(null == preaccountInfo && null == preaccountInfo_C ){
			throw new FabException("SPS104", "Lnsassistdyninfo");
		}


		//返回字段赋值
		if(null != preaccountInfo ){
			acctNo = preaccountInfo.getAcctno();
			customName = "";
			openDate = preaccountInfo.getCreatetime().substring(0,10);
			merchantNo = preaccountInfo.getCustomid();
			
			if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL).contains(preaccountInfo.getStatus()))
				bal = new FabAmount(preaccountInfo.getCurrbal());
			else
				bal = new FabAmount(0.00);
		}else
		{
			acctNo = preaccountInfo_C.getAcctno();
			customName = "";
			openDate = preaccountInfo_C.getCreatetime().substring(0,10);
			bal = new FabAmount(0.00);
			merchantNo = preaccountInfo_C.getCustomid();
		}
				
		
		if( null != preaccountInfo_C )
			surplusbal = new FabAmount(preaccountInfo_C.getCurrbal());
		else
			surplusbal = new FabAmount(0.00);
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
	 * @return the openDate
	 */
	public String getOpenDate() {
		return openDate;
	}

	/**
	 * @param openDate the openDate to set
	 */
	public void setOpenDate(String openDate) {
		this.openDate = openDate;
	}

	/**
	 * @return the bal
	 */
	public FabAmount getBal() {
		return bal;
	}

	/**
	 * @param bal the bal to set
	 */
	public void setBal(FabAmount bal) {
		this.bal = bal;
	}

	/**
	 * @return the surplusbal
	 */
	public FabAmount getSurplusbal() {
		return surplusbal;
	}

	/**
	 * @param surplusbal to set
	 */
	public void setSurplusbal(FabAmount surplusbal) {
		this.surplusbal = surplusbal;
	}
}

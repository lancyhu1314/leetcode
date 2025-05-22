/**
* @author 14050269 Toward
* @version 创建时间：2016年6月13日 下午3:02:59
* 类说明
*/
package com.suning.fab.loan.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.tup4j.currency.Currency;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.account.AcctInfo;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

public class AcctInfoProvider {
	private AcctInfoProvider(){
		//nothing to do
	}
	public static LnsAcctInfo readAcctInfo(AcctInfo acct,String brc) throws FabException
	{
		if (acct == null)
			return new LnsAcctInfo("", "", "", new FabCurrency());
		Class<? extends AcctInfo> cl = acct.getClass();
		
		String billType = null;
		try {
			Method  billTypeMethod = cl.getMethod("getBillType");
			billType = (String) billTypeMethod.invoke(acct);
		} catch (Exception e) {
			LoggerUtil.info("账单类型不存在:{}"+e);
			return null;
		}
		String loanForm = null;

		try {
			Method  loanFormMethod = cl.getMethod("getLoanForm");
			loanForm = (String) loanFormMethod.invoke(acct);
		} catch (Exception e) {
			LoggerUtil.info("形态不存在:{}"+e);
			return null;
		}
		if("51240001".equals(brc)){
			return readAcctInfo(billType,loanForm,brc,(LnsAcctInfo)acct);
		}else{
			return readAcctInfo((LnsAcctInfo)acct,billType,loanForm,acct.getCcy(),brc);
		}
	}
	public static LnsAcctInfo readAcctInfo(LnsAcctInfo acct, String billType,String loanForm, Currency ccy,String brc) throws FabException {
		
		//2020-07-20  lnsassistdyninf账户存事件表
		if( VarChecker.asList(ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT,ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT).contains(billType) ||
			VarChecker.asList(ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT,ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT).contains(loanForm)	)
		{
			if( ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT.equals(billType))
				billType = ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT;
			if( ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT.equals(loanForm))
				loanForm = ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT;
			if( ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT.equals(billType))
				billType = ConstantDeclare.ASSISTACCOUNT.SURPLUSACCT;
			if( ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT.equals(loanForm))
				loanForm = ConstantDeclare.ASSISTACCOUNT.SURPLUSACCT;
			
			
			LnsAcctInfo acctinfo = new LnsAcctInfo("", billType, "N", new FabCurrency());
			if( VarChecker.isEmpty(billType) )
				acctinfo = new LnsAcctInfo("", billType, "", new FabCurrency());
			
			if( !VarChecker.isEmpty(loanForm) ){
				acctinfo.setCustType(loanForm+".N");
			}
				
			return acctinfo;
		}
		
		if (acct.getAcctNo().isEmpty() || billType.isEmpty()|| loanForm.isEmpty())
		{
			return new LnsAcctInfo("", "", "", new FabCurrency());
		}
		
		Map<String, Object> param = new HashMap<String, Object>();
		LnsAcctInfo acctinfo = new LnsAcctInfo(acct.getAcctNo(), billType,loanForm, ccy,acct.getChildBrc());
		param.put("acctno", acct.getAcctNo());
		param.put("ccy", ccy.getCcy());
		param.put("acctstat", PropertyUtil.getPropertyOrDefault("billtype."+billType, billType)+"."+loanForm);
		param.put("brc", brc);

		
		
		if( ConstantDeclare.BILLTYPE.BILLTYPE_FEEA.equals(billType))
		{
			//查询所有的费用信息
			param.put("openbrc", brc);
//			List<TblLnsfeeinfo> lnsfeeinfos;
//			try{
//				lnsfeeinfos = DbAccessUtil.queryForList("Lnsfeeinfo.select", param, TblLnsfeeinfo.class);
//			}catch (FabSqlException e){
//				throw  new FabException(e,"SPS103","Lnsfeeinfo");
//			}
//			
//			if( null != lnsfeeinfos && lnsfeeinfos.size() != 0 )
//				acctinfo.setChildBrc(lnsfeeinfos.get(0).getFeebrc());
		}
		//相同的账户类型 不同核算机构处理
		param.put("profitbrc", brc);
		if(!VarChecker.isEmpty(acct.getChildBrc())){
			param.put("profitbrc", acct.getChildBrc());
		}

		Map<String, Object> subinfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_prdcode", param);
		
		if (subinfo == null && !"ADFE.N".equals(param.get("acctstat"))) {
			return acctinfo;
		}
		
		if(subinfo == null && "ADFE.N".equals(param.get("acctstat"))){
			if(acct.getPrdCode()==null){
				acctinfo.setPrdCode("2512622");
			}else{
				acctinfo.setPrdCode(acct.getPrdCode());
			}
		}else if(subinfo != null && !"ADFE.N".equals(param.get("acctstat"))){
			acctinfo.setPrdCode(subinfo.get("prdcode").toString());
		}
		
		param.clear();
		param.put("acctno", acct.getAcctNo());
		param.put("brc", brc);
		Map<String, Object> custominfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_custominfo", param);
		
		if (custominfo != null)
		{
			acctinfo.setMerchantNo(custominfo.get("customid").toString().trim());
			acctinfo.setCustName(custominfo.get("name").toString());
			acctinfo.setCustType(custominfo.get("custtype").toString());
			acctinfo.setReceiptNo(custominfo.get("acctno1").toString());
		}

		
		return acctinfo;
	}
	public static LnsAcctInfo readAcctInfo(String billType,String loanForm,String brc,LnsAcctInfo acct) throws FabException {
		String receiptNo = acct.getAcctNo();
		Currency ccy = acct.getCcy();
		Class<? extends AcctInfo> cl = acct.getClass();
		if (receiptNo.isEmpty() || billType.isEmpty()|| loanForm.isEmpty())
		{
			return new LnsAcctInfo("", "", "", new FabCurrency());
		}
		
		Map<String, Object> param = new HashMap<String, Object>();
		LnsAcctInfo acctinfo = new LnsAcctInfo( receiptNo, billType,loanForm, ccy);
		param.put("acctno", receiptNo);
		param.put("brc", brc);
		Map<String, Object> custominfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_custominfo", param);
		
		if (custominfo != null)
		{
			String merchantNo = null;
			try {
				Method  merchantNoMethod = cl.getMethod("getMerchantNo");
				merchantNo = (String) merchantNoMethod.invoke(acct);
			} catch (Exception e) {
				LoggerUtil.info("商户号不存在:{}"+e);
				return null;
			}
			if(!VarChecker.isEmpty(merchantNo)){
				acctinfo.setMerchantNo(merchantNo);
			}else{
				acctinfo.setMerchantNo(custominfo.get("customid").toString().trim());
			}
			acctinfo.setCustName(custominfo.get("name").toString());
			acctinfo.setCustType(custominfo.get("custtype").toString());
			acctinfo.setReceiptNo(custominfo.get("acctno1").toString());
		}else{
			String merchantNo = null;
			try {
				Method  merchantNoMethod = cl.getMethod("getMerchantNo");
				merchantNo = (String) merchantNoMethod.invoke(acct);
			} catch (Exception e) {
				LoggerUtil.info("商户号不存在:{}"+e);
				return null;
			}
			String custName = null;
			try {
				Method  custNameMethod = cl.getMethod("getCustName");
				custName = (String) custNameMethod.invoke(acct);
			} catch (Exception e) {
				LoggerUtil.info("客户名称不存在:{}"+e);
				return null;
			}
			String custType = null;
			try {
				Method  custTypeMethod = cl.getMethod("getCustType");
				custType = (String) custTypeMethod.invoke(acct);
			} catch (Exception e) {
				LoggerUtil.info("客户类型不存在:{}"+e);
				return null;
			}
			String receiptNo1 = null;
			try {
				Method  receiptNo1Method = cl.getMethod("getReceiptNo");
				receiptNo1 = (String) receiptNo1Method.invoke(acct);
			} catch (Exception e) {
				LoggerUtil.info("借据号不存在:{}"+e);
				return null;
			}
			acctinfo.setMerchantNo(VarChecker.isEmpty(merchantNo)?"":merchantNo);
			acctinfo.setCustName(VarChecker.isEmpty(custName)?"":custName);
			acctinfo.setCustType(VarChecker.isEmpty(custType)?"":custType);
			acctinfo.setReceiptNo(VarChecker.isEmpty(receiptNo1)?"":receiptNo1);
		}

		param.clear();
		
		param.put("acctno", receiptNo);
		param.put("ccy", ccy.getCcy());
		param.put("acctstat", PropertyUtil.getPropertyOrDefault("billtype."+billType, billType)+"."+loanForm);
		param.put("brc", brc);
		//核算机构
		param.put("profitbrc", brc);
		if(!VarChecker.isEmpty(acct.getChildBrc())){
			param.put("profitbrc", acct.getChildBrc());
		}
		Map<String, Object> subinfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_prdcode", param);
		
		if (subinfo == null) {
			if(!"51240001".equals(brc)){
				return new LnsAcctInfo(receiptNo, billType,loanForm, ccy);
			}
		}
		String prdCode = null;
		if("51240001".equals(brc)){
			try {
				Method  prdCodeMethod = cl.getMethod("getPrdCode");
				prdCode = (String) prdCodeMethod.invoke(acct);
			} catch (Exception e) {
				LoggerUtil.info("产品代码不存在:{}"+e);
				return null;
			}
		}
		if(subinfo != null){
			acctinfo.setPrdCode(subinfo.get("prdcode").toString());
		}else{
			acctinfo.setPrdCode(prdCode);
		}
		
		
		return acctinfo;
	}
}

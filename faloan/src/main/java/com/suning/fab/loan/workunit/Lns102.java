package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsprefundaccount;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 开户--开预收户
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns102 extends WorkUnit { 

	String		customType;		//客户类型
	String		merchantNo;		//商户号客户号
	String		customName;		//户名
	String		oldrepayacct;	//原预收账号
	ListMap		pkgList;		//无追保理债务公司列表
	
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		ListMap 	pkgList = ctx.getRequestDict("pkgList");
		
		//循环债务公司列表 插表商户登记薄 遇到803错误忽略不计 写日志该债务公司已开户
		if(pkgList != null && pkgList.size() > 0){
			for (PubDict pkg:pkgList.getLoopmsg()) {
				String debtCompany = PubDict.getRequestDict(pkg, "debtCompany");
				FabAmount debtAmt = PubDict.getRequestDict( pkg, "debtAmt");
				
				Map<String, Object> param = new HashMap<String,Object>();
				param.put("debtCompany", debtCompany);
				param.put("debtAmt", debtAmt);
				
				//预收登记簿 商户登记薄 债务公司登记薄
				TblLnsprefundaccount lnsprefundaccount = new TblLnsprefundaccount();
				lnsprefundaccount.setBrc(ctx.getBrc());
				lnsprefundaccount.setCustomid(debtCompany);
				lnsprefundaccount.setAcctno(debtCompany);
				lnsprefundaccount.setAccsrccode("D");
				if( "2".equals(customType) )
					lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
				else
					lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
				
				lnsprefundaccount.setOpendate(ctx.getTranDate());
				lnsprefundaccount.setClosedate("");
				lnsprefundaccount.setBalance(0.00);
				lnsprefundaccount.setStatus(ConstantDeclare.STATUS.NORMAL);
				lnsprefundaccount.setReverse1("");
				lnsprefundaccount.setReverse2("");
				lnsprefundaccount.setName(debtCompany);
				
				/* 优化插入条件 2019-04-02*/
				LoggerUtil.info("定位执行开始"+debtCompany);
				Map<String, Object> sqlParam = new HashMap<String, Object>();
				Map<String, Object> oldrepayacctno;
				sqlParam.put("brc", ctx.getBrc());
				sqlParam.put("accsrccode", "D");
				sqlParam.put("customid", debtCompany);
				oldrepayacctno = DbAccessUtil.queryForMap("CUSTOMIZE.query_oldrepayacctno", sqlParam );
				if (null == oldrepayacctno) {
					try{
						DbAccessUtil.execute("Lnsprefundaccount.insert", lnsprefundaccount);
						}
						catch (FabSqlException e)
						{
							LoggerUtil.error("错误信息：{}；【sqlId={}，sqlCode={}】", e.getMessage(), e.getSqlId(), e.getSqlCode());
							if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
								LoggerUtil.info("该债务公司已开户"+debtCompany);
							}
							else
								throw new FabException(e, "SPS100", "lnsprefundaccount");
								
						}
				}
				LoggerUtil.info("定位执行结束"+debtCompany);
				
//				LoggerUtil.info("定位执行开始"+debtCompany);
//				
//				try{
//				DbAccessUtil.execute("Lnsprefundaccount.insert", lnsprefundaccount);
//				}
//				catch (FabSqlException e)
//				{
//					LoggerUtil.error("错误信息：{}；【sqlId={}，sqlCode={}】", e.getMessage(), e.getSqlId(), e.getSqlCode());
//					if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
//						LoggerUtil.info("该债务公司已开户"+debtCompany);
//					}
//					else
//						throw new FabException(e, "SPS100", "lnsprefundaccount");
//						
//				}
//				
//				LoggerUtil.info("定位执行结束"+debtCompany);
			}
		}
		
		
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
	 * @return the pkgList
	 */
	public ListMap getPkgList() {
		return pkgList;
	}
	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(ListMap pkgList) {
		this.pkgList = pkgList;
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
	public String getOldrepayacct() {
		return oldrepayacct;
	}
	public void setOldrepayacct(String oldrepayacct) {
		this.oldrepayacct = oldrepayacct;
	}



}

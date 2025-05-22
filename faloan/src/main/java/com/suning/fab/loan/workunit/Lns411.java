/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Lns205.java
 * Author:   16071579
 * Date:     2017年5月25日 下午4:00:57
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctDetailInfoQuery;
import com.suning.fab.loan.account.LnsAcctDetailInfoQueryM;
import com.suning.fab.loan.la.Product;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.framework.dal.pagination.PaginationResult;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：贷款账户明细查询
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns411 extends WorkUnit {
	
	String serialType;	//0：表示贷款帐号， 1：表示借据号
	String acctNo;		//借据号或贷款帐号
	String loanStat;	//贷款状态 -- 需要去FaLoanPublic中增加该字段
	String startDate;	//起始日期
	String endDate;		//结束日期
	String customId;	//客户代码 
	String productCode;	//产品代码-对应产品名称
	String acctStat; 	//账户类型 -- 需要去FaLoanPublic中增加该字段
	Integer currentPage;//当前页
	Integer pageSize;	//每页条数
	String pkgList;		//返回报文
	Integer nCount;		//符合条件的数据总数量
	
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception{
		
		TranCtx ctx = getTranctx();
		checkeInput();//检查接口输入是否正确
		
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("serialType", serialType);
		param.put("acctno", acctNo);
		param.put("loanstat", loanStat);
		param.put("brc", ctx.getBrc());
		param.put("startdate", startDate);
		param.put("enddate", endDate);
		param.put("customid", customId);
		param.put("prdcode", productCode);
		param.put("acctstat", acctStat);
		
		int nOffSet = (currentPage -1)*pageSize; 
		PaginationResult<LnsAcctDetailInfoQueryM> pageResult;
		try {
			pageResult = DbAccessUtil.queryForList("CUSTOMIZE.query_accountdetailinfo", param, 
					LnsAcctDetailInfoQueryM.class, nOffSet, pageSize);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "CUSTOMIZE.query_accountdetailinfo");
		}
		
		if (null == pageResult || pageResult.getCount() == 0) {
			throw new FabException("LNS021");
		}
		
		nCount = pageResult.getCount();
		List<LnsAcctDetailInfoQueryM> lnsAcctDetailInfoQueryMList = pageResult.getResult();
		if (null == lnsAcctDetailInfoQueryMList || lnsAcctDetailInfoQueryMList.isEmpty()) {
			throw new FabException("LNS021");
		}
		
		Map<String, Product> prdNameMap = LoanAgreementProvider.getProducts();
		List<LnsAcctDetailInfoQuery> lnsAcctDetailInfoQueryList = new ArrayList<LnsAcctDetailInfoQuery>();
		String prdName;//产品名称
		for (LnsAcctDetailInfoQueryM lnsAcctDetailInfoQueryM : lnsAcctDetailInfoQueryMList) {
				
			LnsAcctDetailInfoQuery lnsAcctDetailInfoQuery = new LnsAcctDetailInfoQuery();
			MapperUtil.map(lnsAcctDetailInfoQueryM, lnsAcctDetailInfoQuery);
			
			//根据产品代码找到相应的产品名称
			Product prd = prdNameMap.get("pd"+lnsAcctDetailInfoQueryM.getPrdCode().trim());
			if (null == prd) {
				prdName = "产品名称未定义";
			} else {
				prdName = prd.getPrdName();
				if (null == prdName || prdName.isEmpty()) {
					prdName = "产品名称未定义";
				}
			}
			lnsAcctDetailInfoQuery.setPrdName(prdName);//设置产品名称
			lnsAcctDetailInfoQueryList.add(lnsAcctDetailInfoQuery);
		}
		
		setPkgList(JsonTransfer.ToJson(lnsAcctDetailInfoQueryList));
	}

	public void checkeInput() throws FabException {
		if (!VarChecker.asList("0", "1").contains(serialType)) {
			throw new FabException("LNS004");
		} 
		
		if (VarChecker.isEmpty(startDate) || VarChecker.isEmpty(endDate)) {
			throw new FabException("LNS005");
		}
	}

	/**
	 * @return the serialType
	 */
	public String getSerialType() {
		return serialType;
	}

	/**
	 * @param serialType the serialType to set
	 */
	public void setSerialType(String serialType) {
		this.serialType = serialType;
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
	 * @return the loanStat
	 */
	public String getLoanStat() {
		return loanStat;
	}

	/**
	 * @param loanStat the loanStat to set
	 */
	public void setLoanStat(String loanStat) {
		this.loanStat = loanStat;
	}

	/**
	 * @return the startDate
	 */
	public String getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(String startDate) {
		this.startDate = startDate;
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
	 * @return the acctStat
	 */
	public String getAcctStat() {
		return acctStat;
	}

	/**
	 * @param acctStat the acctStat to set
	 */
	public void setAcctStat(String acctStat) {
		this.acctStat = acctStat;
	}

	/**
	 * @return the currentPage
	 */
	public Integer getCurrentPage() {
		return currentPage;
	}

	/**
	 * @param currentPage the currentPage to set
	 */
	public void setCurrentPage(Integer currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}

	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * @return the pkgList
	 */
	public String getPkgList() {
		return pkgList;
	}

	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
	}

	/**
	 * @return the nCount
	 */
	public Integer getnCount() {
		return nCount;
	}

	/**
	 * @param nCount the nCount to set
	 */
	public void setnCount(Integer nCount) {
		this.nCount = nCount;
	}
	
}

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

import com.suning.fab.loan.account.LnsBillInfoQuery;
import com.suning.fab.loan.account.LnsBillInfoQueryM;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.framework.dal.pagination.PaginationResult;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：账单表查询
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns410 extends WorkUnit {
	
	String acctNo;		//帐号
	String startDate;	//起始日期
	String endDate;		//结束日期
	Integer currentPage;//当前页
	Integer pageSize;	//每页条数
	String pkgList;		//返回报文
	Integer nCount;		//符合条件的数据总数量
	
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		
		checkeInput();//检查接口输入是否正确
		
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("startdate", startDate);
		param.put("enddate", endDate);
		//param.put("brc", ctx.getBrc())//账单表中无机构号
		
		int nOffSet = (currentPage -1)*pageSize; 
		PaginationResult<LnsBillInfoQueryM> pageResult;
		try {
			pageResult = DbAccessUtil.queryForList("CUSTOMIZE.query_billinfo", param, 
					LnsBillInfoQueryM.class, nOffSet, pageSize);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "CUSTOMIZE.query_billinfo");
		}
		
		if (null == pageResult || pageResult.getCount() == 0) {
			throw new FabException("LNS021");
		}
		
		nCount = pageResult.getCount();
		List<LnsBillInfoQueryM> lnsBillInfoQueryMList = pageResult.getResult();
		if (null == lnsBillInfoQueryMList || lnsBillInfoQueryMList.isEmpty()) {
			throw new FabException("LNS021");
		}
		
		List<LnsBillInfoQuery> lnsBillInfoQueryList = new ArrayList<LnsBillInfoQuery>();
		for (LnsBillInfoQueryM lnsBillInfoQueryM : lnsBillInfoQueryMList) {
			
			LnsBillInfoQuery lnsBillInfoQuery = new LnsBillInfoQuery();
			MapperUtil.map(lnsBillInfoQueryM, lnsBillInfoQuery);
			lnsBillInfoQueryList.add(lnsBillInfoQuery);
		}
		
		setPkgList(JsonTransfer.ToJson(lnsBillInfoQueryList));
	}

	public void checkeInput() throws FabException {
		
		if (VarChecker.isEmpty(acctNo)) {
			throw new FabException("ACC103");
		}
		
		if (VarChecker.isEmpty(startDate) ^ VarChecker.isEmpty(endDate)) {
			throw new FabException("LNS005");
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

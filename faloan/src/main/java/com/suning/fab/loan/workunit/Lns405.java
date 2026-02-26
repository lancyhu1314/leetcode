/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Lns405.java
 * Author:   15032049
 * Date:     2017年6月9日 下午4:00:57
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

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfoQuery;
import com.suning.fab.loan.account.LnsOpenAcctInfo;
import com.suning.fab.loan.la.Product;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.framework.dal.pagination.PaginationResult;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：贷款放款信息查询
 *
 * @author 15032049	
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns405 extends WorkUnit {
	
	String startDate;	
	String endDate;	
	
	String customId;
	String receiptNo;
	String acctNo;
	String openBrc;
	String productCode;
	String loanStat;
	
	Integer currentPage;//当前页
	Integer pageSize;	//每页条数
	String pkgList;
	Integer totalLine;
	
	@Override
	public void run() throws Exception {
		
		
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("startdate", startDate);
		param.put("enddate", endDate);
		param.put("customid", customId);
		param.put("receiptno", receiptNo);
		param.put("acctno", acctNo);
		param.put("openbrc", openBrc);
		param.put("productcode", productCode);
		param.put("loanstat", loanStat);
		
		
		int nOffSet = (currentPage -1)*pageSize; 
		PaginationResult<LnsOpenAcctInfo> pageResult;
		try {
			pageResult = DbAccessUtil.queryForList("CUSTOMIZE.query_openaccountinfo_count", param, 
					LnsOpenAcctInfo.class, nOffSet, pageSize);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		
		if (null == pageResult || pageResult.getCount() == 0) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		
		totalLine = pageResult.getCount();
		List<LnsOpenAcctInfo> lnsAcctInfoQueryMList = pageResult.getResult();
		if (null == lnsAcctInfoQueryMList || lnsAcctInfoQueryMList.isEmpty()) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		
		List<LnsAcctInfoQuery> lnsAcctInfoQueryList = new ArrayList<LnsAcctInfoQuery>();
		String prdName;//产品名称
		Map<String, Product> prdNameMap = LoanAgreementProvider.getProducts();
		for (LnsOpenAcctInfo lnsAcctInfoQueryM : lnsAcctInfoQueryMList) {
			
			LnsAcctInfoQuery lnsAcctInfoQuery = new LnsAcctInfoQuery();
			MapperUtil.map(lnsAcctInfoQueryM, lnsAcctInfoQuery);
			lnsAcctInfoQueryList.add(lnsAcctInfoQuery);
			
			
			
			//根据产品代码找到相应的产品名称
			Product prd = prdNameMap.get("pd"+lnsAcctInfoQueryM.getPrdCode().trim());
			if (null == prd) {
				prdName = "产品名称未定义";
			} else {
				prdName = prd.getPrdName();
				if (null == prdName || prdName.isEmpty()) {
					prdName = "产品名称未定义";
				}
			}
			lnsAcctInfoQueryM.setPrdName(prdName);//设置产品名称
		}
		
		
		setPkgList(JsonTransfer.ToJson2Fen(lnsAcctInfoQueryMList));
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
	 * @return the totalLine
	 */
	public Integer getTotalLine() {
		return totalLine;
	}

	/**
	 * @param totalLine the totalLine to set
	 */
	public void setTotalLine(Integer totalLine) {
		this.totalLine = totalLine;
	}

}

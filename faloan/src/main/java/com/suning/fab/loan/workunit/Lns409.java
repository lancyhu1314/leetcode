package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.Provision;
import com.suning.fab.loan.la.Product;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.framework.dal.pagination.PaginationResult;

/**
 * @author 	AY
 *
 * @version V1.0.1
 *
 * @see 	利息计提明细查询
 *
 * @param	startDate		计提开始日
 *			endDate			计提结束日
 *			acctNo			借据号
 *			customId		客户代码
 *			productCode		产品代码
 *			pageSize		每页条数
 *			currentPage		查询页数
 *
 * @return	totalLine		总条数
 *			pkgList			循环报文
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns409 extends WorkUnit {

	String startDate;
	String endDate;
	String brc;
	String acctNo;
	String customId;
	Integer pageSize = 0;
	Integer currentPage = 0;
	String intertype;
	String productCode;
	
	int totalLine;
	String pkgList;

	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		if(VarChecker.isEmpty(startDate) || VarChecker.isEmpty(endDate)){
			throw new FabException("LNS005");
		}
		
		//读取借据对应计提或摊销明细数据
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("begindate", startDate);
		param.put("enddate", endDate);
		param.put("brc", ctx.getBrc());
		param.put("receiptno", acctNo);
		param.put("customid", customId);
		param.put("intertype", intertype);
		param.put("productcode", productCode);
		
		PaginationResult<Provision> pagResult = null;
		
		try {
			pagResult = DbAccessUtil.queryForList("CUSTOMIZE.query_provamor", param, Provision.class, (currentPage - 1) * pageSize, pageSize);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsprovisionreg");
		}
		
		if(null == pagResult){
			throw new FabException("SPS104", "lnsprovisionreg");
		}
		
		Map<String, Product> prdNameMap = LoanAgreementProvider.getProducts();
		//产品名称
		String prdName;
		for (Provision provision : pagResult.getResult()) {
			//根据产品代码找到相应的产品名称
			Product prd = prdNameMap.get("pd"+provision.getProductCode().trim());
			if (null == prd) {
				prdName = "产品名称未定义";
			} else {
				prdName = prd.getPrdName();
				if (null == prdName || prdName.isEmpty()) {
					prdName = "产品名称未定义";
				}
			}
			//设置产品名称
			provision.setProductCode(prdName);
		}
		
		totalLine = pagResult.getCount();
		pkgList = JsonTransfer.ToJson(pagResult.getResult());
		
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
	 * @return the intertype
	 */
	public String getIntertype() {
		return intertype;
	}

	/**
	 * @param intertype the intertype to set
	 */
	public void setIntertype(String intertype) {
		this.intertype = intertype;
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
	 * @return the totalLine
	 */
	public int getTotalLine() {
		return totalLine;
	}

	/**
	 * @param totalLine the totalLine to set
	 */
	public void setTotalLine(int totalLine) {
		this.totalLine = totalLine;
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


}

package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.RpyInfosel;
import com.suning.fab.loan.la.Product;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
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
 * @see 	贷款还款明细查询
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns408 extends WorkUnit {

	String startDate;
	String endDate;
	String brc;
	String acctNo;
	String customId;
	String receiptNo;
	String productCode;
	String loanStat;
	String openBrc;
	
	int pageSize = 0;
	int currentPage = 0;
	
	int totalLine;
	String pkgList;

	@Override
	public void run() throws Exception {
		
		if(VarChecker.isEmpty(startDate) || VarChecker.isEmpty(endDate)){
			throw new FabException("LNS005");
		}

		Map<String,Object> param = new HashMap<String,Object>();
		param.put("begindate", startDate);
		param.put("enddate", endDate);
		param.put("brc", openBrc);
		param.put("acctno", acctNo);
		param.put("customid", customId);
		param.put("acctno1", receiptNo);
		param.put("prdcode", productCode);
		param.put("loanstat", loanStat);
		
		PaginationResult<RpyInfosel> pagResult = null;
		
		try {
			pagResult = DbAccessUtil.queryForList("CUSTOMIZE.query_rpyinfo", param, RpyInfosel.class, (currentPage - 1) * pageSize, pageSize);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "LNSRPYINFO");
		}
		
		Map<String, Product> prdNameMap = LoanAgreementProvider.getProducts();
		String prdName;//产品名称
		for (RpyInfosel rpyInfosel : pagResult.getResult()) {
			//根据产品代码找到相应的产品名称
			Product prd = prdNameMap.get("pd"+rpyInfosel.getPrdCode().trim());
			if (null == prd) {
				prdName = "产品名称未定义";
			} else {
				prdName = prd.getPrdName();
				if (null == prdName || prdName.isEmpty()) {
					prdName = "产品名称未定义";
				}
			}
			rpyInfosel.setPrdName(prdName);//设置产品名称
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
	 * @return the pageSize
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * @return the currentPage
	 */
	public int getCurrentPage() {
		return currentPage;
	}

	/**
	 * @param currentPage the currentPage to set
	 */
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
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

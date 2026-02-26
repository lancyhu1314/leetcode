package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsInterfaceQuery;
import com.suning.fab.loan.account.LnsInterfaceQueryM;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.framework.dal.pagination.PaginationResult;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：接口幂等登记薄表查询
 *
 * @author 
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns417 extends WorkUnit{

	String acctNo;		//账号
	String tranInfo;	//交易码
	String startDate;	//开始日期
	String endDate;		//结束日期
	
	Integer currentPage;//当前页号
	Integer pageSize;	//每页条数

	//返回参数
	Integer totalLine;	//总条数
	String pkgList;		//Json格式返回报文

	@Override
	public void run()  throws FabException
	{
		 Map<String,Object> param = new HashMap<String,Object>();
		 param.put("acctno", acctNo);
		 param.put("brc", getTranctx().getBrc());
		 param.put("trancode", tranInfo);
		 param.put("startdate", startDate);
		 param.put("enddate", endDate);
		 int nOffSet = (currentPage - 1) * pageSize;
		 PaginationResult<LnsInterfaceQueryM> pageResult;
		 
		 try{
			 pageResult = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsinterface",param , LnsInterfaceQueryM.class, nOffSet, pageSize);
		 }catch(FabException e){
			 throw new FabException(e, "SPS103", "LNSINTERFACE");
		 }
		 
		 if(null == pageResult || pageResult.getCount() == 0){
			 throw new FabException("LNS021");
		 }
		 
		 List<LnsInterfaceQuery> lnsInterfaceQueryList = new ArrayList<LnsInterfaceQuery>();
		 for(LnsInterfaceQueryM lnsInterfaceQueryM:pageResult.getResult()){
			 LnsInterfaceQuery lnsInterfaceQuery = new  LnsInterfaceQuery();
			 MapperUtil.map(lnsInterfaceQueryM, lnsInterfaceQuery);
			 lnsInterfaceQueryList.add(lnsInterfaceQuery);
		 }
		 
		 totalLine = pageResult.getCount();
		 setPkgList(JsonTransfer.ToJson(lnsInterfaceQueryList));
	}
	
	/**
	 * @return acctNo
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
	 * 
	 * @return currentPage
	 */
	public Integer getCurrentPage() {
		return currentPage;
	}
	/**
	 * 
	 * @param currentPage the currentPage to set
	 */
	public void setCurrentPage(Integer currentPage) {
		this.currentPage = currentPage;
	}
	/**
	 * 
	 * @return pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}
	/**
	 * 
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
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
	 * @return the tranInfo
	 */
	public String getTranInfo() {
		return tranInfo;
	}

	/**
	 * @param tranInfo the tranInfo to set
	 */
	public void setTranInfo(String tranInfo) {
		this.tranInfo = tranInfo;
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
}

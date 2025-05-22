package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.CarAccountDetails;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.framework.dal.pagination.PaginationResult;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：	汽车租赁明细查询
 *
 * @author 
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns423 extends WorkUnit{
	
	String startDate;	//开始日期
	String endDate;		//结束日期
	String acctNo;		//贷款账号
	String openBrc;		//公司代码
	String chargeType;	//账户类型
	int pageSize = 0;
	int currentPage = 0;
	
	//返回参数
	Integer totalLine;	//总条数
	String pkgList;		//Json格式返回报文

	@Override
	public void run()  throws FabException
	{
		if(VarChecker.isEmpty(startDate) || VarChecker.isEmpty(endDate)){
			throw new FabException("LNS005","账务日期");
		}
		
		if(VarChecker.isEmpty(acctNo)){
			throw new FabException("LNS055","贷款账号");
		}
		
		if(VarChecker.isEmpty(openBrc)){
			throw new FabException("LNS055","公司代码");
		}
		
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("begindate", startDate);
		param.put("enddate", endDate);
		param.put("acctno", acctNo);
		param.put("brc", openBrc);
		
		if(!VarChecker.isEmpty(chargeType)){
			if(chargeType.equals("TXCX")){
				param.put("userno", "2");
				param.put("reserv5", "10");
			}else if(chargeType.equals("YBCX")){
				param.put("userno", "3");
				param.put("reserv5", "11");
			}else if(chargeType.equals("QBCX")){
				param.put("userno", "1");
				param.put("reserv5", "13");
			}
		}
		
		PaginationResult<CarAccountDetails> pagResult = null;
		
		try {
			pagResult = DbAccessUtil.queryForList("CUSTOMIZE.query_carAccountDetails", param, CarAccountDetails.class, (currentPage - 1) * pageSize, pageSize);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "LNSINTERFACE");
		}
		if(null == pagResult || 0 == pagResult.getCount()){
			throw new FabException("SPS104", "LNSINTERFACE");
		}
		
		for (CarAccountDetails carAccountDetails : pagResult.getResult()) {
			switch (Integer.parseInt(carAccountDetails.getChargeType().trim())) {
			case 1:
				carAccountDetails.setChargeType("QBCX");
				break;
			case 2:
				carAccountDetails.setChargeType("TXCX");
				break;
			case 3:
				carAccountDetails.setChargeType("YBCX");
				break;
			}
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
	 * @return the chargeType
	 */
	public String getChargeType() {
		return chargeType;
	}

	/**
	 * @param chargeType the chargeType to set
	 */
	public void setChargeType(String chargeType) {
		this.chargeType = chargeType;
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

}

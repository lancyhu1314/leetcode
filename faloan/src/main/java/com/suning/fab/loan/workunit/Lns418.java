package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;

/**
 * @author 18043620
 *
 * @see 还款计划查询日间
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns418 extends WorkUnit {

	// 账号
	String acctNo;
	// 每页大小
	Integer pageSize;
	// 当前页
	Integer currentPage;
	// 期数
	Integer repayTerm;
	// 总行数
	Integer totalLine;
	// json串
	String pkgList;

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		int result;
		// 定义map
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("acctno", acctNo);
		paramMap.put("brc", ctx.getBrc());
		try {
			// 将数据插入新
			result = DbAccessUtil.execute("CUSTOMIZE.insert_lnsrpyplan", paramMap);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS100", "Lnsrpyplan");
		}
		if (result > 0) {
			try {
				// 删除旧表中的数据
				DbAccessUtil.execute("CUSTOMIZE.del_lnsrepayplan", paramMap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS101", "lnsrepayplan");
			}
		} else {
			return;
		}

	}

	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo
	 *            the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	/**
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}

	/**
	 * @param pageSize
	 *            the pageSize to set
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
	 * @param currentPage
	 *            the currentPage to set
	 */
	public void setCurrentPage(Integer currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * @return the repayTerm
	 */
	public Integer getRepayTerm() {
		return repayTerm;
	}

	/**
	 * @param repayTerm
	 *            the repayTerm to set
	 */
	public void setRepayTerm(Integer repayTerm) {
		this.repayTerm = repayTerm;
	}

	/**
	 * @return the totalLine
	 */
	public Integer getTotalLine() {
		return totalLine;
	}

	/**
	 * @param totalLine
	 *            the totalLine to set
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
	 * @param pkgList
	 *            the pkgList to set
	 */
	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
	}

}

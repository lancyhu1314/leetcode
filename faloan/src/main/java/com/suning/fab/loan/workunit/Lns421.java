package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.backup.BackupEventListenerImpl;
import com.suning.fab.loan.backup.BackupManager;
import com.suning.fab.loan.backup.ScmProtiesUtil;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns421 extends WorkUnit {

	String acctNo;

	@Override
	public void run() throws Exception {
		
		String putHbase = ScmProtiesUtil.getProperty("putHbase", "no");
		
		if(VarChecker.asList("no","NO").contains(putHbase)){
			return;
		}
		
		TranCtx ctx = getTranctx();
		
		BackupManager manager =  new BackupManager(ctx, acctNo);

		BackupEventListenerImpl eventListener = new BackupEventListenerImpl();
		manager.setEventListener(eventListener);

		//获取主文件信息
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		param.put("brc", ctx.getBrc());
		
		List<Map<String, Object>> lnsbasicinfo = null;
		try {
			lnsbasicinfo = DbAccessUtil.queryForList("Lnsbasicinfo.selectByUk", param);
		} catch (FabSqlException e) {
			LoggerUtil.error("查询lnsbasicinfo表错误{}", e);
		}
		if (null == lnsbasicinfo) {
			return;
		}

		//获取账单表信息
		List<Map<String, Object>> lnsbillList = null;
		try {
			lnsbillList = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", param);
		} catch (FabSqlException e) {
			LoggerUtil.error("查询lnsbill表错误{}", e);
		}

		//获取还款计划表信息
		List<Map<String, Object>> lnsrpyplanList = null;
		try {
			lnsrpyplanList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsrpyplan", param);
		} catch (FabSqlException e) {
			LoggerUtil.error("查询lnsrpyplan表错误{}", e);
		}

		//获取动态表信息
		List<Map<String, Object>> lnsaccountdyninfoList = null;
		try {
			lnsaccountdyninfoList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsaccountdyninfo", param);
		} catch (FabSqlException e) {
			LoggerUtil.error("查询lnsaccountdyninfo表错误{}", e);
		}

		manager.accept("lnsbasicinfo", lnsbasicinfo);
		manager.accept("lnsbill", lnsbillList);
		manager.accept("lnsrpyplan", lnsrpyplanList);
		manager.accept("lnsaccountdyninfo", lnsaccountdyninfoList);
		manager.backup();
		
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


}

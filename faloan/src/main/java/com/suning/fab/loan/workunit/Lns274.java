package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.loan.utils.StringUtil;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：批量法催费用余额查询
 *
 * @Author 15041590
 * @Date 2022/09/26
 * @see
 */

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns274 extends WorkUnit {

	String pkgList1;
	int totalLine;

	@Override
	public void run() throws Exception{
		ListMap pkgList=getTranctx().getRequestDict("pkgList");
		TblLnsassistdyninfo lnsAssistDynInfo;
		//判断查询参数是否为空或者条数超过范围
		if(VarChecker.isEmpty(pkgList)|| pkgList.size()>20 ){
			throw new FabException("LNS236");
		}
		List<Map<String, Object>> resultList=new ArrayList<>();
		try {
			for (PubDict pkg : pkgList.getLoopmsg()) {
				if( VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeBusiNo")) ||
						VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeBusiType")) ||
						VarChecker.isEmpty(PubDict.getRequestDict(pkg, "brc")))
				{
					throw new FabException("LNS055","费用明细所有查询条件字段");
				}
				Map<String, Object> acctMap = new HashMap<String, Object>();
				String feeBusiNo = StringUtil.parseString(PubDict.getRequestDict(pkg, "feeBusiNo"));
				String brc = StringUtil.parseString(PubDict.getRequestDict(pkg, "brc"));
				String feeBusiType = StringUtil.parseString(PubDict.getRequestDict(pkg, "feeBusiType"));
				lnsAssistDynInfo=LoanAssistDynInfoUtil.queryPreaccountInfo(brc, feeBusiNo, "", feeBusiType, getTranctx());

				if(null!=lnsAssistDynInfo){
					acctMap.put("feeBusiNo",feeBusiNo );
					acctMap.put("feeBusiType", feeBusiType);
					acctMap.put("balance", lnsAssistDynInfo.getCurrbal());
					acctMap.put("feeFunCode", lnsAssistDynInfo.getReserv1());
					acctMap.put("status", lnsAssistDynInfo.getStatus());
					resultList.add(acctMap);
				}else{
					throw new FabException("LNS260",feeBusiNo,feeBusiType,brc);
				}
			}
		} catch (FabSqlException e1) {
			throw new FabException(e1, "SPS103", "query_lnsassistdyninfo_bal");
		}
		pkgList1 = JsonTransfer.ToJson(resultList);
		totalLine=resultList.size();
	}



	public String getPkgList1() {
		return pkgList1;
	}

	public void setPkgList1(String pkgList1) {
		this.pkgList1 = pkgList1;
	}

	public int getTotalLine() {
		return totalLine;
	}

	public void setTotalLine(int totalLine) {
		this.totalLine = totalLine;
	}


}

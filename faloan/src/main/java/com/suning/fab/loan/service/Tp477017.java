package com.suning.fab.loan.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns106;
import com.suning.fab.loan.workunit.Lns413;
import com.suning.fab.loan.workunit.Lns415;
import com.suning.fab.loan.workunit.Lns418;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;

/**
	*@author    AY
	* 
	*@version   V1.0.0
	*
	*@see       还款计划查询
	*
	*@param     
	*
	*@return    
	*
	*@exception 
	*/
@Scope("prototype") 
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-newRepayPlanQuery")
public class Tp477017 extends ServiceTemplate {
	
	@Autowired Lns415 lns415;
	@Autowired Lns413 lns413;
	@Autowired Lns106 lns106;
	@Autowired Lns418 lns418;
	public Tp477017() {
		needSerSeqNo=false;
	}
	
	Integer totalLine;
	String	pkgList;
	String	acctNo;

	@Override
	protected void run() throws Exception {
//		trigger(lns418);
		JSONArray jsonarray = new JSONArray();
		int	i = 0;
		trigger(lns106);
		Map<Integer, String> acctMap = lns106.getAcctList();
		
		if("1".equals(lns106.getFlags()))
		{
			for (Map.Entry<Integer, String> entry : acctMap.entrySet())
			{	
				Map<String, Object> map4133 = new HashMap<String, Object>();
				map4133.put("acctNo", entry.getValue());
				trigger(lns413,"map4133",map4133);
				
				JSONArray jsonarrtmp = JSONArray.parseArray(lns413.getPkgList());
				//只有一条不嘚瑟
				//for(i = 0; i < jsonarrtmp.size(); i++)
				{	
					JSONObject jsonojb = jsonarrtmp.getJSONObject(0);
					jsonojb.put("acctno", ctx.getRequestDict("acctNo"));
					jsonojb.put("repayterm", (++i));
					jsonarray.add(jsonojb);
				}
			}
			setTotalLine(jsonarray.size());
			setPkgList(jsonarray.toJSONString());
		}
		else
		{
			trigger(lns415);
			
//			jsonarray = JSONArray.parseArray(lns415.getPkgList());
			setTotalLine(lns415.getTotalLine());
			setPkgList(lns415.getPkgList());
		}	

	}
	@Override
	protected void special() throws Exception {
		//nothing to do
	}
	public Integer getTotalLine() {
		return totalLine;
	}
	public void setTotalLine(Integer totalLine) {
		this.totalLine = totalLine;
	}
	public String getPkgList() {
		return pkgList;
	}
	public void setPkgList(String pkgList) {
		this.pkgList = pkgList;
	}
	public String getAcctNo() {
		return acctNo;
	}
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

}

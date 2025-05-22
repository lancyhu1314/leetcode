package com.suning.fab.loan.supporter;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.tup4j.amount.FabAmount;

public  class PrepayTermAvgPrinSpporter extends PrepayTermSupporter {

	
	private static final Map<String,String> map = new HashMap<String,String>() ;
	   //还款方式：1、2、9
	
	   //第一列：OPEN开户日/INTD结息日还款/PRIND结本日还款/OTHER当前期还款
	   //       ONLYPRIN只还本/ONLYINT只还息/PRININT还本又还息
	   //第二列：C当前期数/P当前期数减1
	   //		是否累加期数（Y是 N否）
	   //	 	是否插辅助表（已不用）
	   //		是否插辅助表（已不用）
	   static {
	       map.put("OPEN_ONLYPRIN","C:N:N:N");
	       map.put("OPEN_ONLYINT","C:N:N:N");
	       map.put("OPEN_PRININT","C:N:N:N"); 
	       map.put("INTD_ONLYPRIN","P:N:N:N");
	       map.put("INTD_ONLYINT","P:N:N:N");
	       map.put("INTD_PRININT","P:N:N:N");
	       map.put("OTHER_ONLYPRIN","C:Y:N:N");
	       map.put("OTHER_ONLYINT","C:N:N:N");
	       map.put("OTHER_PRININT","C:Y:N:N");
	       
	  }

	@Override
	public void genVal(String a, String b) {
		// TODO Auto-generated method stub
		super.setVal(map.get(a+"_"+b));
	}
	
    @Override
	public  String genMoment(String openDate,String setIntDate ,String setPrinDate,String repayDate)
	{
		//开户日
		if(repayDate.equals(openDate))
		{
			return "OPEN";
		}
		//结本日当天
		if(repayDate.equals(setPrinDate))
		{
			return "INTD";
		}
		//当期
		return "OTHER";
	}
    @Override
	public  String genThing(FabAmount intAmt,FabAmount repayAmt)
	{
		//只还本金
		if (intAmt.isZero())
		{
			return "ONLYPRIN";
		}
		//只还利息
		if (intAmt.getVal()>=repayAmt.getVal())
		{
			return "ONLYINT";
		}
		//还息还本
		if (intAmt.getVal()<repayAmt.getVal())
		{
			return "PRININT";
		}
		return "";
	}
}

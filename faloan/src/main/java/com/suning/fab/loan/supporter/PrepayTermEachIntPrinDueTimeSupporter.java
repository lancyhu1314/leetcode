package com.suning.fab.loan.supporter;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;



public  class PrepayTermEachIntPrinDueTimeSupporter extends PrepayTermSupporter {

	
	private static final Map<String,String> map = new HashMap<String,String>() ;
	   //还款方式3、4、5、6、7
	
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
	       map.put("PRIND_ONLYPRIN","P:N:N:N");
	       map.put("PRIND_ONLYINT","P:N:N:N");
	       map.put("PRIND_PRININT","P:N:N:N");
	       map.put("OTHER_ONLYPRIN","C:Y:N:N");
	       map.put("OTHER_ONLYINT","C:N:N:N");
	       map.put("OTHER_PRININT","C:Y:N:N");
	       
	       map.put("PRINM_ONLYPRIN","C:N:N:N");
	       map.put("PRINM_ONLYINT","C:N:N:N");
	       map.put("PRINM_PRININT","C:N:N:N");
	       
	  }

	@Override
	public void genVal(String a, String b) {
		super.setVal(map.get(a+"_"+b));
	}
	@Override
	public String genMoment(String openDate,String setIntDate ,String setPrinDate,String repayDate) throws FabException
	{
		//开户日
		if(repayDate.equals(openDate))
		{
			return "OPEN";
		}
		//判断是否结息日
		if (isSettleInterestDate(repayDate) )
		{
			return "INTD";
		}
		
		//判断是否结本日
		if (isSettlePrinDate(repayDate))
		{
			return "PRIND";
		}
		else
		{
			if(repayDate.equals(setPrinDate))
				return "PRINM";
		}
		
		//当期
		return "OTHER";
	}
	@Override
	public String genThing(FabAmount intAmt,FabAmount repayAmt)
	{
		//参数：截止到当日利息，提前还款金额
		
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

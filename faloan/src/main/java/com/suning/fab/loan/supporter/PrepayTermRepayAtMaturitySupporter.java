package com.suning.fab.loan.supporter;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.tup4j.amount.FabAmount;

public  class PrepayTermRepayAtMaturitySupporter extends PrepayTermSupporter {

	
	private static final Map<String,String> map = new HashMap<String,String>() ;

	//还款方式：8
	
	//不重新生成计划
	/*      static {
	    map.put("OPEN_ONLYPRIN","C:N:N");
	       map.put("OPEN_ONLYINT","C:N:N");
	       map.put("OPEN_PRININT","C:N:N"); 
	       map.put("INTD_ONLYPRIN","P:N:N");
	       map.put("INTD_ONLYINT","P:N:N");
	       map.put("INTD_PRININT","P:N:N");
	       map.put("OTHER_ONLYPRIN","C:Y:Y");
	       map.put("OTHER_ONLYINT","C:N:Y");
	       map.put("OTHER_PRININT","C:Y:Y");
	       
	  }*/

	@Override
	public void genVal(String a, String b) {
		// TODO Auto-generated method stub
		super.setVal(map.get(a+"_"+b));
	}
	@Override
	public Integer  genUseTerm(Integer term){
		
		return term;
	}
	@Override
	public boolean isAccumterm()
	{
		return false;
	}
	@Override
	public boolean  isInsertPrinBillPlan()
	{
		return true;
	}
	@Override
	public boolean  isInsertIntBillPlan()
	{
		return true;
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

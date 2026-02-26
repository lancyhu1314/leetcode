package com.suning.fab.loan.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.FeeRepayPlanQuery;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LoanFormInfo;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.TransferSupporter;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.apache.hadoop.hbase.client.Get;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 转列
 *
 *
 * @exception
 */
public class LoanTransferProvider {
	private LoanTransferProvider(){
		//nothing to do
	}
	
	public static void loanAcctTransfer(LoanAgreement loanAgreement,LnsBill lnsbill,String repayDate, TranCtx ctx) throws FabException{
	
		//判断是否转列标识
		if (!loanAgreement.getInterestAgreement().getNeedRisksClassificationFlag())
		{
			return;
		}
		
		//判断本金是否转列标识
		if (!loanAgreement.getInterestAgreement().getNeedRisksClassificationPrin())
		{
			if( "PRIN".equals(lnsbill.getBillType()))
				return;
		}
		
		//判断利息是否转列标识
		if (!loanAgreement.getInterestAgreement().getNeedRisksClassificationInt())
		{
			if( "NINT".equals(lnsbill.getBillType()))
				return;
		}

		//生成一个顺序的贷款形态信息的List，为转列准备
		List<LoanFormInfo> loanFormList = LoanTransferProvider.getLoanFormInfoList(lnsbill.getCurendDate());
		//将List转成Map，方便修改宽限期形态的信息，在最初通过transfer.properties文件生成的List中，宽限期信息不准确，需要重新弄赋值
		Map<String,Object> loanFormMap = LoanTransferProvider.convertListToMap(loanFormList, "LoanForm");
		//重新赋值宽限期信息，此处赋值时原List中的值也会发生变化，地址一样
		if( "false".equals(loanAgreement.getInterestAgreement().getIsCalTail()) )
		{
			((LoanFormInfo)loanFormMap.get(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE)).setCurrStatusDays(loanAgreement.getContract().getGraceDays());
			((LoanFormInfo)loanFormMap.get(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE)).setStatusEndDate(CalendarUtil.nDaysAfter(lnsbill.getCurendDate(), loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
		}
		else
		{
			((LoanFormInfo)loanFormMap.get(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE)).setCurrStatusDays( CalendarUtil.actualDaysBetween(lnsbill.getEndDate(),lnsbill.getRepayendDate()) );
			((LoanFormInfo)loanFormMap.get(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE)).setStatusEndDate(CalendarUtil.nDaysAfter(lnsbill.getCurendDate(), CalendarUtil.actualDaysBetween(lnsbill.getEndDate(),lnsbill.getRepayendDate()) ).toString("yyyy-MM-dd"));
		}
		
		for(int i=0; i<loanFormList.size(); i++)
		{
			Integer days = 9999999;
			if (loanFormList.get(i).getPreNode() != null)
				days = loanFormList.get(i).getPreNode().getCurrStatusDays();
			//当账单表的当期结束日期到当前日期之间的天数大于List当前形态days，且账单的状态对应形态序号小于当前List形态序号时，转列
			if (CalendarUtil.actualDaysBetween(lnsbill.getCurendDate(), repayDate) > days
				&& loanFormList.get(i).getOrder() > ((LoanFormInfo) loanFormMap.get(lnsbill.getBillStatus())).getOrder()) {
				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsbill.getBillType())){
					TransferSupporter acctTransferSupporter = LoanSupporterUtil.getPrinAcctTransfer(loanFormList.get(i).getLoanForm());
					//宽限期不需要转列，因为没有配置宽限期的bean，所以通过null!=acctTransferSupporter可以过滤
					if(null!=acctTransferSupporter){
						acctTransferSupporter.transfer(lnsbill, ctx, loanAgreement, loanFormList.get(i).getPreNode().getStatusEndDate());
					}
				}else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsbill.getBillType())){
					TransferSupporter acctTransferSupporter = LoanSupporterUtil.getIntAcctTransfer(loanFormList.get(i).getLoanForm());
					if(null!=acctTransferSupporter){
						acctTransferSupporter.transfer(lnsbill, ctx, loanAgreement, loanFormList.get(i).getPreNode().getStatusEndDate());
					}
				}
				else if(LoanFeeUtils.isFeeType(lnsbill.getBillType())){
					if( VarChecker.asList("N","G","O").contains(loanFormList.get(i).getLoanForm())  )
					{
						TransferSupporter acctTransferSupporter = LoanSupporterUtil.getFeeAcctTransfer(loanFormList.get(i).getLoanForm());
						if(null!=acctTransferSupporter){
							acctTransferSupporter.transfer(lnsbill, ctx, loanAgreement, loanFormList.get(i).getPreNode().getStatusEndDate());
						}
					}
				}
			}
		}
	}
	public static List<LoanFormInfo> getLoanFormInfoList(String billEndDate) throws FabException
	{
		List<LoanFormInfo> loanFormList = new ArrayList<LoanFormInfo>();
		for(int i=1;;i++)
		{
			String loanform = PropertyUtil.getPropertyOrDefault("transfer."+String.valueOf(i),null);
			
			if (loanform == null)
				break;
			LoanFormInfo loanFormInfo = new LoanFormInfo();
			String tmp[] = loanform.split(":");
			//顺序
			loanFormInfo.setOrder(i);
			//形态
			loanFormInfo.setLoanForm(tmp[0]);
			//形态天数
			loanFormInfo.setCurrStatusDays(Integer.valueOf(tmp[1]));
			//形态结束日期
			loanFormInfo.setStatusEndDate(CalendarUtil.nDaysAfter(billEndDate, loanFormInfo.getCurrStatusDays()).toString("yyyy-MM-dd"));
			if (!loanFormList.isEmpty())
			{
				loanFormInfo.setPreNode(loanFormList.get(loanFormList.size()-1));
			}
			loanFormList.add(loanFormInfo);
		}
		return loanFormList;
	}
	public static  Map<String,Object> convertListToMap(List<? extends Object> list,String...fields) throws FabException{
		
		Map<String,Object> map = new  HashMap<String, Object>();
		for( Object ob:list)
		{
			StringBuilder key = new StringBuilder();
			for (int i = 0; i < fields.length; i++) {
					Method m = MethodUtil.getMethod(ob.getClass(), "get" + fields[i]);
					key.append(String.valueOf(MethodUtil.methodInvoke(ob, m)));
			}
			map.put(key.toString(), ob);
		}
		return map;
	}

	static Map<String,Integer > transferDuration = new ConcurrentHashMap<>();


	/*
	 每一期的贷款状态 不依赖跑批的还款计划
	 @return 是否计算过 装填
	 */
	public static void  updateTermStatus(TranCtx ctx, LoanAgreement la, RePayPlan plan,LnsBill lnsbill,TblLnsbasicinfo lnsbasicinfo) {

		//账单结清了不走
		if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsbill.getSettleFlag()))
			return ;

		//判断是否转列标识
		if (!la.getInterestAgreement().getNeedRisksClassificationFlag())
		{
			return ;
		}
		
        //判断本金是否转列标识
        if (!la.getInterestAgreement().getNeedRisksClassificationPrin())
        {
            if( "PRIN".equals(lnsbill.getBillType()))
                return ;
        }

        //判断利息是否转列标识
        if (!la.getInterestAgreement().getNeedRisksClassificationInt())
        {
            if( "NINT".equals(lnsbill.getBillType()))
                return ;
        }
		//罚息不参与计算
		if ("DINT".equals(lnsbill.getBillType()))
		{
			return ;
		}
		
		//计算 相隔天数
		String curEndDate = plan.getRepayintedate();
		if( isDelayPeriod(plan.getRepayintedate(), la) )
		{
			curEndDate = la.getBasicExtension().getTermEndDate();
		}
		
		int daysBetween = CalendarUtil.actualDaysBetween(curEndDate, ctx.getTranDate());
		LoggerUtil.debug(plan.getTermstatus()+","+plan.getRepayterm()+","+lnsbill.getBillType()+","+plan.getRepayintedate()+","+ctx.getTranDate()+","+daysBetween);

		if(daysBetween<0)
			return ;
		Map<String,Integer>  transfer = getTransfer();
		int currStatusDays;
		int interval = 999999; //transfer.properties 中最大的 数字
		for(Map.Entry<String,Integer> entry :transfer.entrySet()){
			// 宽限期内的 都算作是正常
			if(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(entry.getKey()))
			{
				currStatusDays=la.getContract().getGraceDays();
			}
			else {
				currStatusDays = entry.getValue();
			}

			if(currStatusDays>=daysBetween
					&& currStatusDays-daysBetween < interval) {
				plan.setTermstatus(entry.getKey());
				interval = currStatusDays-daysBetween;
				LoggerUtil.debug(entry.getKey());

			}
		}
	}

	/*
	 每一期的贷款状态 不依赖跑批的还款计划
	 @return 是否计算过 装填
	 */
	public static void  updateFeeTermStatus(TranCtx ctx, LoanAgreement la, FeeRepayPlanQuery plan, LnsBill lnsbill, TblLnsbasicinfo lnsbasicinfo) {

	    //账单结清了不走
		if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsbill.getSettleFlag()))
			return ;

       //判断是否转列标识
       if (!la.getInterestAgreement().getNeedRisksClassificationFlag())
       {
           return ;
       }
       //判断本金是否转列标识
       if (!la.getInterestAgreement().getNeedRisksClassificationPrin())
       {
           if( "PRIN".equals(lnsbill.getBillType()))
               return ;
       }

       //判断利息是否转列标识
       if (!la.getInterestAgreement().getNeedRisksClassificationInt())
       {
           if( "NINT".equals(lnsbill.getBillType()))
               return ;
       }
		//罚息不参与计算
		if ("DINT".equals(lnsbill.getBillType()))
		{
			return ;
		}
		//计算 相隔天数
		
		String curEndDate = plan.getRepayintedate();
		if( isDelayPeriod(plan.getRepayintedate(), la) )
		{
			curEndDate = la.getBasicExtension().getTermEndDate();
		}
        
		int daysBetween = CalendarUtil.actualDaysBetween(curEndDate, ctx.getTranDate());
		LoggerUtil.debug(plan.getTermstatus()+","+plan.getRepayterm()+","+lnsbill.getBillType()+","+plan.getRepayintedate()+","+ctx.getTranDate()+","+daysBetween);

		if(daysBetween<0)
			return ;
		Map<String,Integer>  transfer = getTransfer();
		int currStatusDays;
		int interval = 999999; //transfer.properties 中最大的 数字
		for(Map.Entry<String,Integer> entry :transfer.entrySet()){
			// 宽限期内的 都算作是正常
			if(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(entry.getKey()))
			{
				currStatusDays=la.getContract().getGraceDays();
			}
			else {
				currStatusDays = entry.getValue();
			}

			if(currStatusDays>=daysBetween
					&& currStatusDays-daysBetween < interval) {
				plan.setTermstatus(entry.getKey());
				interval = currStatusDays-daysBetween;
				LoggerUtil.debug(entry.getKey());

			}
		}
	}
	/**
	 *  读取配置文件的内容
	 * @return  transferDuration
	 */
	 private static Map<String,Integer> getTransfer(){
	 	//双检索
		if(transferDuration.isEmpty()){
			synchronized (LoanTransferProvider.class) {
				if (transferDuration.isEmpty()) {
					String loanform;
					for(int i=1;;i++)
					{
						loanform = PropertyUtil.getPropertyOrDefault("transfer." + String.valueOf(i), null);
						if (loanform == null)
							break;
						String tmp[] = loanform.split(":");
						if(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE.equals(tmp[0]))
						{
							//暂时 文件中的宽限期也是0
							transferDuration.put( ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, Integer.valueOf(tmp[1]));
						}else{
							transferDuration.put( tmp[0], Integer.valueOf(tmp[1]));
						}
					}
				}

			}
		}
		return transferDuration;

	}
	 
	private static boolean isDelayPeriod(String endDate, LoanAgreement loanAgreement) {
		for (Map.Entry<String, Object> entry : loanAgreement.getBasicExtension().getLastEnddates().entrySet()) {
			if (CalendarUtil.before(entry.getKey(), endDate)
					&& CalendarUtil.afterAlsoEqual(entry.getValue().toString(), endDate)) {
				return true;
			}

		}
		return false;
	}
}

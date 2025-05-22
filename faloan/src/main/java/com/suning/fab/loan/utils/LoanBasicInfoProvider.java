package com.suning.fab.loan.utils;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.bo.LnsBasicInfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 *
 * @return FabAmount 税金
 *
 * @exception
 */
public class LoanBasicInfoProvider {
	private LoanBasicInfoProvider(){
		//nothing to do
	}
	
	public static void basicInfoUpt(LoanAgreement la, LnsBasicInfo basicinfo, TranCtx ctx) throws FabException
	{
		Map<String, Object> basicInfoMap = new HashMap<String, Object>();
		basicInfoMap.put("lastPrinDate", basicinfo.getLastPrinDate());
		basicInfoMap.put("prinTerm", basicinfo.getPrinTerm());
		if(!VarChecker.isEmpty(basicinfo.getTranAmt()))
			basicInfoMap.put("tranAmt", basicinfo.getTranAmt().getVal());
		basicInfoMap.put("lastIntDate", basicinfo.getLastIntDate());
		basicInfoMap.put("intTerm", basicinfo.getIntTerm());
		basicInfoMap.put("loanStat", basicinfo.getLoanStat());
		basicInfoMap.put("modifyDate", ctx.getTranDate());
		basicInfoMap.put("acctNo", la.getContract().getReceiptNo());
		basicInfoMap.put("brc", ctx.getBrc());
		//日期是否算尾
		if("true".equals(la.getInterestAgreement().getIsCalTail()))
		{
			if(!la.getContract().getRepayPrinDate().equals(la.getContract().getContractStartDate()))
			{
				basicInfoMap.put("oldPrinDate", CalendarUtil.nDaysBefore(la.getContract().getRepayPrinDate(), 1).toString("yyyy-MM-dd"));
			}
			else
			{
				basicInfoMap.put("oldPrinDate", la.getContract().getRepayPrinDate());
			}
			if(!la.getContract().getRepayIntDate().equals(la.getContract().getContractStartDate()))
			{
				basicInfoMap.put("oldIntDate", CalendarUtil.nDaysBefore(la.getContract().getRepayIntDate(), 1).toString("yyyy-MM-dd"));
			}
			else
			{
				basicInfoMap.put("oldIntDate", la.getContract().getRepayIntDate());
			}
			
		}
		else
		{
			basicInfoMap.put("oldPrinDate", la.getContract().getRepayPrinDate());
			basicInfoMap.put("oldIntDate", la.getContract().getRepayIntDate());
		}
		

		
		int ct = 0;
		try {
			ct = DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo", basicInfoMap);
		} catch (FabException e) {
			throw new FabException(e, "SPS102", "lnsbasicinfo");
		}
		if (ct != 1) {
			throw new FabException("LNS041", la.getContract().getReceiptNo());
		}
	}
	
	//还款更新主文件时间戳 2019-04-15
	public static void basicInfoUptForRepay(LoanAgreement la, TranCtx ctx) throws FabException
	{
		Map<String, Object> basicInfoMap = new HashMap<String, Object>();
		basicInfoMap.put("modifyDate", ctx.getTranDate());
		basicInfoMap.put("acctNo", la.getContract().getReceiptNo());
		basicInfoMap.put("brc", ctx.getBrc());
		//日期是否算尾
		if("true".equals(la.getInterestAgreement().getIsCalTail()))
		{
			if(!la.getContract().getRepayPrinDate().equals(la.getContract().getContractStartDate()))
			{
				basicInfoMap.put("oldPrinDate", CalendarUtil.nDaysBefore(la.getContract().getRepayPrinDate(), 1).toString("yyyy-MM-dd"));
			}
			else
			{
				basicInfoMap.put("oldPrinDate", la.getContract().getRepayPrinDate());
			}
			if(!la.getContract().getRepayIntDate().equals(la.getContract().getContractStartDate()))
			{
				basicInfoMap.put("oldIntDate", CalendarUtil.nDaysBefore(la.getContract().getRepayIntDate(), 1).toString("yyyy-MM-dd"));
			}
			else
			{
				basicInfoMap.put("oldIntDate", la.getContract().getRepayIntDate());
			}
			
		}
		else
		{
			basicInfoMap.put("oldPrinDate", la.getContract().getRepayPrinDate());
			basicInfoMap.put("oldIntDate", la.getContract().getRepayIntDate());
		}
		

		
		int ct = 0;
		try {
			ct = DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo", basicInfoMap);
		} catch (FabException e) {
			throw new FabException(e, "SPS102", "lnsbasicinfo");
		}
		if (ct != 1) {
			throw new FabException("LNS041", la.getContract().getReceiptNo());
		}
	}

	/**
	 * 校验一笔借据多机构开户
	 * @param receiptNo 借据号
	 * @throws FabException LNS080	借据号已存在
	 */
	public static void checkReceiptNo(String receiptNo) throws FabException {
		//禁止一笔借据多机构开户
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("acctno", receiptNo);
		Map<String, Object> custominfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsbasicinfo_521", paramMap);
		if (custominfo != null)
		{
			throw new FabException("LNS080",receiptNo);
		}

	}

	//为核销的贷款 判断呆滞呆账
	//2019-11-12 调整成工具方法
	public static  Boolean ifLanguishOrBad(TblLnsbasicinfo lnsbasicinfo,String repayDate	)
	{
		if(!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lnsbasicinfo.getLoanstat().trim())){
			return false;
		}
		String loanform = PropertyUtil.getPropertyOrDefault("transfer."+String.valueOf(3),null);
		Integer days;
		if (loanform == null)
			days = 90;
		else {
			String tmp[] = loanform.split(":");
			//逾期天数
			days =Integer.parseInt(tmp[1]);
		}

		//计算逾期开始日期
		String overStartDate = CalendarUtil.nDaysAfter(lnsbasicinfo.getContduedate(), days).toString("yyyy-MM-dd");

		//判断呆滞呆账未呆滞呆账
		//90天与90天之前不呆滞呆账
		return !(CalendarUtil.beforeAlsoEqual(repayDate, overStartDate));

	}
}

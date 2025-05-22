package com.suning.fab.loan.supporter;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see 转列
 *
 * @param fundInvestInit(LoanAgreement loanAgreement)
 * @param billStatusUpdate(String tranDate, Integer serSeqNo, Integer txSeq, String billStatus, String statusbDate)
 * @param transfer(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String statusbDate)
 * 
 * @return
 *
 * @exception
 */
public abstract class TransferSupporter {  
	/**
	 * 初始化FundInvest信息
	 * @param loanAgreement 合同协议信息
	 * @return FundInvest
	 */
	public FundInvest fundInvestInit(LoanAgreement loanAgreement){
		return new FundInvest(loanAgreement.getFundInvest().getInvestee(),
				   loanAgreement.getFundInvest().getInvestMode(), 
				   loanAgreement.getFundInvest().getChannelType(),
				   loanAgreement.getFundInvest().getFundChannel(),
				   loanAgreement.getFundInvest().getOutSerialNo());
	}
	/**
	 * 转列更新账单
	 * @param tranDate    账单日期
	 * @param serSeqNo    账单流水
	 * @param txSeq       账单子序号
	 * @param billStatus  账单状态
	 * @param statusbDate 账单状态开始日期
	 * @throws FabException
	 */
	public void billStatusUpdate(String tranDate, Integer serSeqNo, Integer txSeq, String billStatus, String statusbDate) throws FabException{
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", tranDate);
		param.put("serseqno", serSeqNo);
		param.put("txseq", txSeq);
		param.put("billStatus", billStatus);
		param.put("statusbDate", statusbDate);
		try{
			DbAccessUtil.execute("CUSTOMIZE.update_hisbillstatus", param);
		}catch(FabException e){
			throw new FabException(e,"SPS102", "lnsbill");
		}
	}

	/**
	 * 转列账务操作
	 * @param lnsBill  账单信息
	 * @param ctx	        交易上下文
	 * @param loanAgreement   贷款协议信息
	 * @param statusbDate     账单状态开始日期
	 * @throws FabException
	 */
	public abstract void transfer(LnsBill lnsBill, TranCtx ctx, LoanAgreement loanAgreement, String statusbDate) throws FabException;
}  
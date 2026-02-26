/**
* @author 14050269 Howard
* @version 创建时间：2016年6月8日 下午8:38:13
* 类说明
*/
package com.suning.fab.loan.accounting;

import java.util.List;

import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.account.AcctInfo;
import com.suning.fab.tup4j.account.AcctOperationMemo;
import com.suning.fab.tup4j.account.AcctOperationType;
import com.suning.fab.tup4j.accounting.Event;
import com.suning.fab.tup4j.amount.Amount;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.AccountOperationMemoBuilder;

public class LnsEvent extends Event{
	AcctInfo oppAcctInfo;
	FundInvest fundInvestInfo;
	String billTranDate;
	Integer billSerSeqno;
	Integer billTxnSeq;
	AcctOperationType operatrionType;	///<操作类型
	private List<FabAmount> amtList;
	private String briefCode;
	private String briefText;
    private String reserv1;
    private String reserv2;
    private String reserv3;
    private String reserv4;
    private String reserv5;
	TranCtx ctx;
	public LnsEvent(String eventCode, Amount tranAmt, AcctInfo acctInfo, AcctInfo oppAcctInfo,
			FundInvest fundInvestInfo) {
		super(eventCode, tranAmt, acctInfo);
		this.oppAcctInfo = oppAcctInfo;
		this.fundInvestInfo = fundInvestInfo;
	}
	public LnsEvent(String eventCode, Amount tranAmt, AcctInfo acctInfo, AcctInfo oppAcctInfo,
			FundInvest fundInvestInfo,String briefCode) {
		super(eventCode, tranAmt, acctInfo);
		this.oppAcctInfo = oppAcctInfo;
		this.fundInvestInfo = fundInvestInfo;
		this.briefCode = briefCode;
		AcctOperationMemo memo = AccountOperationMemoBuilder.generic(briefCode);
		this.briefText = memo.getBreifText();
	}
	public LnsEvent(String eventCode, Amount tranAmt, AcctInfo acctInfo, AcctInfo oppAcctInfo,
			FundInvest fundInvestInfo,String briefCode,List<FabAmount> amtList) {
		super(eventCode, tranAmt, acctInfo);
		this.oppAcctInfo = oppAcctInfo;
		this.fundInvestInfo = fundInvestInfo;
		this.briefCode = briefCode;
		AcctOperationMemo memo = AccountOperationMemoBuilder.generic(briefCode);
		this.briefText = memo.getBreifText();
		this.amtList = amtList;
	}
	public LnsEvent(String eventCode, Amount tranAmt, AcctInfo acctInfo, AcctInfo oppAcctInfo,
			FundInvest fundInvestInfo,String briefCode,List<FabAmount> amtList,String billTranDate,Integer billSerSeqno,Integer billTxnSeq) {
		super(eventCode, tranAmt, acctInfo);
		this.oppAcctInfo = oppAcctInfo;
		this.fundInvestInfo = fundInvestInfo;
		this.briefCode = briefCode;
		AcctOperationMemo memo = AccountOperationMemoBuilder.generic(briefCode);
		this.briefText = memo.getBreifText();
		this.amtList = amtList;
		this.billTranDate = billTranDate;
		this.billSerSeqno = billSerSeqno;
		this.billTxnSeq = billTxnSeq;
	}

	/**
	 * 克隆事件登记簿
	 * @return
	 */
	public LnsEvent  clone(){
		LnsEvent lnsEvent=new LnsEvent(this.getEventCode(), this.getTranAmt(),this.getAcctInfo(),oppAcctInfo,
				fundInvestInfo,briefCode, amtList,billTranDate,billSerSeqno,billTxnSeq);
		lnsEvent.setOperatrionType(operatrionType);
		lnsEvent.setAmtList(amtList);
		lnsEvent.setBriefCode(briefCode);
		lnsEvent.setBriefText(briefText);
		lnsEvent.setReserv1(reserv1);
		lnsEvent.setReserv2(reserv2);
		lnsEvent.setReserv3(reserv3);
		lnsEvent.setReserv4(reserv4);
		lnsEvent.setReserv5(reserv5);
		lnsEvent.setCtx(ctx);
		return lnsEvent;
	}
	/**
	 * @return the oppAcctInfo
	 */
	public AcctInfo getOppAcctInfo() {
		return oppAcctInfo;
	}
	/**
	 * @param oppAcctInfo the oppAcctInfo to set
	 */
	public void setOppAcctInfo(AcctInfo oppAcctInfo) {
		this.oppAcctInfo = oppAcctInfo;
	}
	/**
	 * @return the fundInvestInfo
	 */
	public FundInvest getFundInvestInfo() {
		return fundInvestInfo;
	}
	/**
	 * @param fundInvestInfo the fundInvestInfo to set
	 */
	public void setFundInvestInfo(FundInvest fundInvestInfo) {
		this.fundInvestInfo = fundInvestInfo;
	}
	/**
	 * @return the billTranDate
	 */
	public String getBillTranDate() {
		return billTranDate;
	}
	/**
	 * @param billTranDate the billTranDate to set
	 */
	public void setBillTranDate(String billTranDate) {
		this.billTranDate = billTranDate;
	}
	/**
	 * @return the billSerSeqno
	 */
	public Integer getBillSerSeqno() {
		return billSerSeqno;
	}
	/**
	 * @param billSerSeqno the billSerSeqno to set
	 */
	public void setBillSerSeqno(Integer billSerSeqno) {
		this.billSerSeqno = billSerSeqno;
	}
	/**
	 * @return the billTxnSeq
	 */
	public Integer getBillTxnSeq() {
		return billTxnSeq;
	}
	/**
	 * @param billTxnSeq the billTxnSeq to set
	 */
	public void setBillTxnSeq(Integer billTxnSeq) {
		this.billTxnSeq = billTxnSeq;
	}
	/**
	 * @return the operatrionType
	 */
	public AcctOperationType getOperatrionType() {
		return operatrionType;
	}
	/**
	 * @param operatrionType the operatrionType to set
	 */
	public void setOperatrionType(AcctOperationType operatrionType) {
		this.operatrionType = operatrionType;
	}
	/**
	 * @return the amtList
	 */
	public List<FabAmount> getAmtList() {
		return amtList;
	}
	/**
	 * @param amtList the amtList to set
	 */
	public void setAmtList(List<FabAmount> amtList) {
		this.amtList = amtList;
	}
	/**
	 * @return the briefCode
	 */
	public String getBriefCode() {
		return briefCode;
	}
	/**
	 * @param briefCode the briefCode to set
	 */
	public void setBriefCode(String briefCode) {
		this.briefCode = briefCode;
	}
	/**
	 * @return the briefText
	 */
	public String getBriefText() {
		return briefText;
	}
	/**
	 * @param briefText the briefText to set
	 */
	public void setBriefText(String briefText) {
		this.briefText = briefText;
	}
	/**
	 * @return the reserv1
	 */
	public String getReserv1() {
		return reserv1;
	}
	/**
	 * @param reserv1 the reserv1 to set
	 */
	public void setReserv1(String reserv1) {
		this.reserv1 = reserv1;
	}
	/**
	 * @return the reserv2
	 */
	public String getReserv2() {
		return reserv2;
	}
	/**
	 * @param reserv2 the reserv2 to set
	 */
	public void setReserv2(String reserv2) {
		this.reserv2 = reserv2;
	}
	/**
	 * @return the reserv3
	 */
	public String getReserv3() {
		return reserv3;
	}
	/**
	 * @param reserv3 the reserv3 to set
	 */
	public void setReserv3(String reserv3) {
		this.reserv3 = reserv3;
	}
	/**
	 * @return the reserv4
	 */
	public String getReserv4() {
		return reserv4;
	}
	/**
	 * @param reserv4 the reserv4 to set
	 */
	public void setReserv4(String reserv4) {
		this.reserv4 = reserv4;
	}
	/**
	 * @return the reserv5
	 */
	public String getReserv5() {
		return reserv5;
	}
	/**
	 * @param reserv5 the reserv5 to set
	 */
	public void setReserv5(String reserv5) {
		this.reserv5 = reserv5;
	}
	/**
	 * @return the ctx
	 */
	public TranCtx getCtx() {
		return ctx;
	}
	/**
	 * @param ctx the ctx to set
	 */
	public void setCtx(TranCtx ctx) {
		this.ctx = ctx;
	}
	
}

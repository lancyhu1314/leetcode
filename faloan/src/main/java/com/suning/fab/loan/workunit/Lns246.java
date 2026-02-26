package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.utils.AccountingModeChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsdiscountaccount;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	18049687
 *
 * @version V1.0.1
 *
 * @see		汽车租赁账户充值
 *
 * @param	serialNo		业务流水号
 *			channelType		预收渠道
 *			chargeType		充值类型
 *			fundChannel		借方总账科目
 *			discountAcctNo	贴息账号
 *			ccy				币种
 *			amt				金额
 *			outSerialNo		外部流水号
 *			memo			摘要信息
 *			receiptNo		出帐编号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns246 extends WorkUnit {

	String  	serialNo;		//业务流水号
	String		channelType;	//充值渠道
	String		chargeType;		//充值类型
	String		fundChannel;	//借方总账科目
	String 		ccy;			//币种
	FabAmount 	amt;			//金额
	String 		outSerialNo;	//外部流水号
	String 		memo;			//摘要信息
	String 		receiptNo;		//出帐编号
    String		customName;
    String		merchantNo;
	
	@Autowired 
	LoanEventOperateProvider eventProvider;
	
	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName = "";
		if(amt.isNegative() || amt.isZero()){
			throw new FabException("LNS065");
		}
		
		if(VarChecker.isEmpty(channelType)){
			throw new FabException("LNS055","充值渠道");
		}
		
		if(VarChecker.isEmpty(chargeType)){
			throw new FabException("LNS055","充值类型");
		}
		
		
		
		
		TranCtx ctx = getTranctx();
		
		//写幂等登记薄
		TblLnsinterface lnsinterface = new TblLnsinterface();
		//账务日期
		lnsinterface.setTrandate(ctx.getTermDate());
		//幂等流水号
		lnsinterface.setSerialno(ctx.getSerialNo());
		//交易码
		lnsinterface.setTrancode(ctx.getTranCode());
		//自然日期
		lnsinterface.setAccdate(ctx.getTranDate());
		//系统流水号
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		//网点
		lnsinterface.setBrc(ctx.getBrc());
		//预收账号
		if(chargeType.equals("TXCZ")){
			lnsinterface.setUserno("2");
		}else if(chargeType.equals("YBCZ")){
			lnsinterface.setUserno("3");
		}else if(chargeType.equals("QBCZ")){
			lnsinterface.setUserno("1");
		}
		//开户金额
		lnsinterface.setTranamt(amt.getVal());
		//时间戳
		lnsinterface.setTimestamp(ctx.getTranTime());
		//银行流水号/易付宝单号/POS单号
		lnsinterface.setBankno(outSerialNo);
		//借据号
		lnsinterface.setMagacct(receiptNo);
		//预收渠道 2019-01-16
		lnsinterface.setReserv5(channelType);
		//借方总账科目
		if(!VarChecker.isEmpty(fundChannel)){
			lnsinterface.setBillno(fundChannel);
		}
		try {
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
		}
		catch (FabSqlException e)
		{
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
			} else {
				throw new FabException(e, "SPS100", "lnsinterface");
			}
		}
		
		//更新预收户金额
		Map<String,Object> lns001map = new HashMap<String, Object>();
		
		lns001map.put("brc", ctx.getBrc());
		lns001map.put("acctno", receiptNo);
		if(chargeType.equals("TXCZ")){
			lns001map.put("accttype", "2");
		}else if(chargeType.equals("YBCZ")){
			lns001map.put("accttype", "3");
		}else if(chargeType.equals("QBCZ")){
			lns001map.put("accttype", "1");
		}
		lns001map.put("balance", amt.getVal());
		
		TblLnsdiscountaccount lnsdiscountaccount = null;
		try {
			lnsdiscountaccount = DbAccessUtil.queryForObject("CUSTOMIZE.update_lnsdiscountaccount_add", lns001map, TblLnsdiscountaccount.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS102", "lnsdiscountaccount");
		}
		if(null == lnsdiscountaccount){
			throw new FabException("LNS069");
		}
		AccountingModeChange.saveLnsprefundsch(ctx, 1, lnsdiscountaccount.getAcctno(),lnsdiscountaccount.getCustomid() ,
				lnsdiscountaccount.getAccttype(), lnsdiscountaccount.getCusttype(),lnsdiscountaccount.getName() , amt.getVal(),
				"add");
		LnsAcctInfo lnsAcctInfo = null;
		if(!VarChecker.isEmpty(receiptNo)){
			//处理老数据acctno与acctno1不一致
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("acctno1", receiptNo);
			paramMap.put("brc", ctx.getBrc());
			
			Map<String, Object> custominfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_acctno", paramMap);
			if (custominfo != null)
			{
				receiptNo = custominfo.get("acctno").toString();
			}
			lnsAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			lnsAcctInfo.setCustType(lnsdiscountaccount.getCusttype());
			lnsAcctInfo.setReceiptNo(receiptNo);
			lnsAcctInfo.setPrdCode("2412617");
			lnsAcctInfo.setMerchantNo(ctx.getMerchantNo());
		}
		String reserv1 = null;
		if("1".equals(lnsdiscountaccount.getCusttype())||"PERSON".equals(lnsdiscountaccount.getCusttype())){
			reserv1 = "70215243";
		}else if("2".equals(lnsdiscountaccount.getCusttype())||"COMPANY".equals(lnsdiscountaccount.getCusttype())){
			reserv1 = ctx.getMerchantNo();
		}else{
			reserv1 = "";
		}
		FundInvest	fundInvest = new FundInvest("", "", channelType, fundChannel, outSerialNo);
		
		//写事件
		eventProvider.createEvent(ConstantDeclare.EVENT.PREDNTBOND, amt, lnsAcctInfo, null, 
								fundInvest, chargeType, ctx, 
								//预收和保理客户类型统一放至备用5方便发送事件时转换处理
								//转json存入Tunneldata字段 
								reserv1, "","","", customName);
	}

	/**
	 * @return the serialNo
	 */
	public String getSerialNo() {
		return serialNo;
	}

	/**
	 * @param serialNo the serialNo to set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	/**
	 * @return the channelType
	 */
	public String getChannelType() {
		return channelType;
	}

	/**
	 * @param channelType the channelType to set
	 */
	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	/**
	 * @return the chargeType
	 */
	public String getChargeType() {
		return chargeType;
	}

	/**
	 * @param chargeType the chargeType to set
	 */
	public void setChargeType(String chargeType) {
		this.chargeType = chargeType;
	}

	/**
	 * @return the fundChannel
	 */
	public String getFundChannel() {
		return fundChannel;
	}

	/**
	 * @param fundChannel the fundChannel to set
	 */
	public void setFundChannel(String fundChannel) {
		this.fundChannel = fundChannel;
	}

	/**
	 * @return the ccy
	 */
	public String getCcy() {
		return ccy;
	}

	/**
	 * @param ccy the ccy to set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	/**
	 * @return the amt
	 */
	public FabAmount getAmt() {
		return amt;
	}

	/**
	 * @param amt the amt to set
	 */
	public void setAmt(FabAmount amt) {
		this.amt = amt;
	}

	/**
	 * @return the outSerialNo
	 */
	public String getOutSerialNo() {
		return outSerialNo;
	}

	/**
	 * @param outSerialNo the outSerialNo to set
	 */
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}

	/**
	 * @return the memo
	 */
	public String getMemo() {
		return memo;
	}

	/**
	 * @param memo the memo to set
	 */
	public void setMemo(String memo) {
		this.memo = memo;
	}

	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}

	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	/**
	 * @return the eventProvider
	 */
	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}

	/**
	 * @param eventProvider the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}

	/**
	 * @return the customName
	 */
	public String getCustomName() {
		return customName;
	}

	/**
	 * @param customName the customName to set
	 */
	public void setCustomName(String customName) {
		this.customName = customName;
	}

	/**
	 * @return the merchantNo
	 */
	public String getMerchantNo() {
		return merchantNo;
	}

	/**
	 * @param merchantNo the merchantNo to set
	 */
	public void setMerchantNo(String merchantNo) {
		this.merchantNo = merchantNo;
	}

	

}

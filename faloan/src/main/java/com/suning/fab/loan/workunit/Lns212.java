package com.suning.fab.loan.workunit;


//import com.sun.tools.javac.util.List;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see -还款--还款渠道
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns212 extends WorkUnit {
	LoanAgreement loanAgreement;
	String		repayChannel;
	String		acctNo;
	String		repayAcctNo;
	String		brc;
	FabAmount	feeAmt;
	FabAmount	refundAmt;
	FabAmount	repayAmt;
	FabAmount	offsetAmt;
	String		investee;
	String 		memo;
	String		bankSubject;
	String		outSerialNo;
	String		prdId;
	String		custType;
	String		customId;
	String		customName;
	String		priorType;
	String		platformId;
	String		cooperateId;
	

	Map<String,FabAmount> repayAmtMap;
	private Integer txseqno = 0; //预收明细登记簿  子序号
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName="";
		
		//2020-09-27 备用字段
		String reserv5 = "";
		Map<String,String> tunnelData = new HashMap<String,String>();
		
		String	customid = "";
		TranCtx ctx = getTranctx();
		FabAmount	totalRepayAmt = new FabAmount();
		FundInvest fundInvest = new FundInvest();
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
	
		LoggerUtil.debug("bankSubject1" + getBankSubject() + getAcctNo());
		
//		//还款更新主文件时间戳 2019-04-15
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		LoanBasicInfoProvider.basicInfoUptForRepay(loanAgreement, ctx);

		//农商行还费时自动结清本金，还款渠道由系统指定E
		if("2512634".equals(loanAgreement.getPrdId())&&"471011".equals(ctx.getTranCode())){
			repayChannel="E";
		}
		
		if(getRepayAcctNo() != null)
			lnsAcctInfo.setMerchantNo(getRepayAcctNo());//预收内部帐号  java版与商户号一致
		if(getRepayChannel() != null)
			fundInvest.setChannelType(getRepayChannel());
		if(getInvestee() != null)
			fundInvest.setInvestee(getInvestee());
		if(getBankSubject() != null)
			fundInvest.setFundChannel(getBankSubject());
		if(outSerialNo != null)
			fundInvest.setOutSerialNo(outSerialNo);
		ListMap pkgList = ctx.getRequestDict("pkgList");

		//纯退款无还款金额
		if(repayAmtMap != null)
		{	
			for(FabAmount amt:repayAmtMap.values())
			{
				totalRepayAmt.selfAdd(amt);
			}

			/**添加无追保理债务公司的校验 add at 20190827*/
			if(VarChecker.asList("3010013","3010014","3010015","3010006") .contains(loanAgreement.getPrdId())){
				for(Map.Entry<String,FabAmount> entry:repayAmtMap.entrySet())
				{
					if(entry.getKey().startsWith("PRIN") && entry.getValue().isPositive()){
						if(null==pkgList||pkgList.size() == 0){
							throw new FabException("LNS055","债务公司");
						}
						break;
					}
				}
				/**add end*/


			}
		}



		LoggerUtil.debug("LNS212TRANAMT:" + totalRepayAmt);
		
		//手续费来源也是支付渠道但是没算在还款金额totalRepayAmt里面
		if(getFeeAmt() != null && getFeeAmt().isPositive())
			totalRepayAmt.selfAdd(getFeeAmt());
		
		LnsAcctInfo oppNAcctInfo = null;

		if(totalRepayAmt.isPositive() && getRepayChannel().equals(ConstantDeclare.PAYCHANNEL.TWO))
		{
			oppNAcctInfo = new LnsAcctInfo("", ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, "", new FabCurrency());
//			Map<String, Object> params = new HashMap<>();
//			params.put("balance", totalRepayAmt.getVal());
//			params.put("brc", ctx.getBrc());
//			params.put("accsrccode", "N");
//			params.put("acctno", getRepayAcctNo());
//
//			Map<String, Object> newBalance;
//			try{
//				newBalance = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsprefundaccount_repay", params);
//			}catch (FabSqlException e){
//				throw new FabException(e, "SPS103", "lnsprefundaccount");
//			}
//			if(newBalance == null)
//			{
//				throw new FabException("SPS104", "lnsprefundaccount");
//			}
//			customid = newBalance.get("CUSTOMID").toString().trim();
//			Double tabBalance = Double.parseDouble(newBalance.get("balance").toString());
//			LoggerUtil.debug("tabBalance:" + tabBalance);
//			if(tabBalance < 0.00)
//				throw new FabException("LNS019");

			//更新辅助账户动态表并插入一条明细
			TblLnsassistdyninfo lnsassistdyninfo = LoanAssistDynInfoUtil.updatePreaccountInfo(ctx, ctx.getBrc(),getRepayAcctNo(), ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT, totalRepayAmt, "sub","");

			customid = lnsassistdyninfo.getCustomid().trim();
			LoggerUtil.debug("tabBalance:" + lnsassistdyninfo.getCurrbal());
			if(new FabAmount(lnsassistdyninfo.getCurrbal()).isNegative())
				throw new FabException("LNS019");
			txseqno++;

			//预收账户进行赋值
			oppNAcctInfo.setReceiptNo(acctNo);
			oppNAcctInfo.setCustType(lnsassistdyninfo.getCusttype());

		//债务公司还款
//			AccountingModeChange.saveLnsprefundsch(ctx, txseqno, getRepayAcctNo(), customid, "N",  newBalance.get("CUSTTYPE").toString().trim(),
//					loanAgreement.getCustomer().getCustomName() ,totalRepayAmt.getVal() ,"sub" );


		}
		
		if(!VarChecker.isEmpty(getPrdId()))
			lnsAcctInfo.setPrdCode( getPrdId());
		else
			lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
		
		if(!VarChecker.isEmpty(getCustType()))
			lnsAcctInfo.setCustType(getCustType());
		else
			lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
		
		//退款金额不可能从预收出 所以再预收更新之后累加
		if(getRefundAmt() != null && getRefundAmt().isPositive())
			totalRepayAmt.selfAdd(getRefundAmt());
		LoggerUtil.debug("totalRepayAmt:" + totalRepayAmt.getVal());
		if(!totalRepayAmt.isPositive())
		{
			throw new FabException("LNS037");
		}	
		LoggerUtil.debug("tabBalancegetChannelType:" + fundInvest.getChannelType());
		LoggerUtil.debug("tabBalancegetFundChannel:" + fundInvest.getFundChannel());

		//乐业贷退货A渠道，通过产品代码2412625区分，货押贷的还款渠道事件---预留3放471008的本息还款详情
		if(VarChecker.asList("2412625","2521001").contains(loanAgreement.getPrdId())&&"A".equals(repayChannel)){
			eventProvider.createEvent(ConstantDeclare.EVENT.PAYCHANNEL, totalRepayAmt, lnsAcctInfo, oppNAcctInfo, fundInvest, "HKQD", ctx, customId, memo, "priorType",
					PubDictUtils.getInvesteeFlag(ctx),getCustomName());

		}
		else if(repayChannel != null && (repayChannel.contains("5") || repayChannel.contains("6"))){
			eventProvider.createEvent(ConstantDeclare.EVENT.PAYCHANNEL, totalRepayAmt, lnsAcctInfo, oppNAcctInfo, fundInvest, "HKQD", ctx, customid, "", investee
			,PubDictUtils.getInvesteeFlag(ctx)); //getRepayAcctNo()字段超长
		}
		else if( repayChannel != null && "C".equals(repayChannel) ){
			if( Arrays.asList("2512630", "2512636", "2512637", "2512633", "2512641").contains(loanAgreement.getPrdId())) //2020-07-09 绿地小贷往来方代码;20210622 增加光大助贷
				eventProvider.createEvent(ConstantDeclare.EVENT.PAYCHANNEL, totalRepayAmt, lnsAcctInfo, oppNAcctInfo, fundInvest, "HKQD", ctx, cooperateId, platformId,"",
						PubDictUtils.getInvesteeFlag(ctx),getCustomName());
			else
				eventProvider.createEvent(ConstantDeclare.EVENT.PAYCHANNEL, totalRepayAmt, lnsAcctInfo, oppNAcctInfo, fundInvest, "HKQD", ctx, customId, platformId,"",
						PubDictUtils.getInvesteeFlag(ctx),getCustomName());
		}
		else{
			if("51240001".equals(ctx.getBrc())){
				String reserv1;
				if("1".equals(lnsAcctInfo.getCustType())||"PERSON".equals(lnsAcctInfo.getCustType())){
					reserv1 = "70215243";
				}else if("2".equals(lnsAcctInfo.getCustType())||"COMPANY".equals(lnsAcctInfo.getCustType())){
					reserv1 = repayAcctNo;
				}else{
					reserv1 = "";
				}
				//CustomName统一放至备用5方便发送事件时转换处理
				//转json存入Tunneldata字段
				eventProvider.createEvent(ConstantDeclare.EVENT.PAYCHANNEL, totalRepayAmt, lnsAcctInfo, oppNAcctInfo, fundInvest, "HKQD", ctx, reserv1, memo,"",PubDictUtils.getInvesteeFlag(ctx),getCustomName());

			}else{
				//理赔场景拼接理赔公司商户号和类型
				if( repayChannel != null && "2".equals(repayChannel) ) {
					
					TblLnsassistdyninfo preaccountInfo_N = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), repayAcctNo, "", ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT, ctx);
					
					if( "PERSON".equals(loanAgreement.getCustomer().getCustomType()) && "COMPANY".equals(preaccountInfo_N.getCusttype()) ){
						tunnelData.put("claimMerchantNo", preaccountInfo_N.getCustomid() );
					}
				}
				//透传付款方账户
				if(ctx.getRequestDict("payerAccountSubject")!=null&&Arrays.asList("3010013","3010015").contains(loanAgreement.getPrdId())){
					tunnelData.put("reserv6", ctx.getRequestDict("payerAccountSubject"));
				}
				//判断隧道字段是否为空
				if(tunnelData.size()>0){
					reserv5 = JsonTransfer.ToJson(tunnelData);
				}

				eventProvider.createEvent(ConstantDeclare.EVENT.PAYCHANNEL, totalRepayAmt, lnsAcctInfo, oppNAcctInfo, fundInvest, "HKQD", ctx, customId, memo,"",PubDictUtils.getInvesteeFlag(ctx),reserv5);
			}
		}
	}

	public String getPrdId() {
		return prdId;
	}
	public void setPrdId(String prdId) {
		this.prdId = prdId;
	}
	public String getCustType() {
		return custType;
	}
	public void setCustType(String custType) {
		this.custType = custType;
	}
	/**
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}
	/**
	 * @param loanAgreement the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}
	/**
	 * @return the repayChannel
	 */
	public String getRepayChannel() {
		return repayChannel;
	}
	/**
	 * @param repayChannel the repayChannel to set
	 */
	public void setRepayChannel(String repayChannel) {
		this.repayChannel = repayChannel;
	}
	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}
	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}
	/**
	 * @return the repayAcctNo
	 */
	public String getRepayAcctNo() {
		return repayAcctNo;
	}
	/**
	 * @param repayAcctNo the repayAcctNo to set
	 */
	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
	}
	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}
	/**
	 * @param brc the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}
	/**
	 * @return the feeAmt
	 */
	public FabAmount getFeeAmt() {
		return feeAmt;
	}
	/**
	 * @param feeAmt the feeAmt to set
	 */
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;
	}
	/**
	 * @return the refundAmt
	 */
	public FabAmount getRefundAmt() {
		return refundAmt;
	}
	/**
	 * @param refundAmt the refundAmt to set
	 */
	public void setRefundAmt(FabAmount refundAmt) {
		this.refundAmt = refundAmt;
	}
	/**
	 * @return the offsetAmt
	 */
	public FabAmount getOffsetAmt() {
		return offsetAmt;
	}
	/**
	 * @param offsetAmt the offsetAmt to set
	 */
	public void setOffsetAmt(FabAmount offsetAmt) {
		this.offsetAmt = offsetAmt;
	}
	/**
	 * @return the repayAmtMap
	 */
	public Map<String, FabAmount> getRepayAmtMap() {
		return repayAmtMap;
	}
	/**
	 * @param repayAmtMap the repayAmtMap to set
	 */
	public void setRepayAmtMap(Map<String, FabAmount> repayAmtMap) {
		this.repayAmtMap = repayAmtMap;
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
	public FabAmount getRepayAmt() {
		return repayAmt;
	}
	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}
	public String getInvestee() {
		return investee;
	}
	public void setInvestee(String investee) {
		this.investee = investee;
	}
	public String getBankSubject() {
		return bankSubject;
	}
	public void setBankSubject(String bankSubject) {
		this.bankSubject = bankSubject;
	}
	public String getOutSerialNo() {
		return outSerialNo;
	}
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}
	public String getMemo() {
		return memo;
	}
	public void setMemo(String memo) {
		this.memo = memo;
	}
	public String getCustomId() {
		return customId;
	}
	public void setCustomId(String customId) {
		this.customId = customId;
	}
	public String getCustomName() {
		return customName==null?"":customName;
	}
	public void setCustomName(String customName) {
		this.customName = customName;
	}

	public Integer getTxseqno() {
		return txseqno;
	}

	public void setTxseqno(Integer txseqno) {
		this.txseqno = txseqno;
	}

	/**
	 * @return the priorType
	 */
	public String getPriorType() {
		return priorType;
	}

	/**
	 * @param priorType the priorType to set
	 */
	public void setPriorType(String priorType) {
		this.priorType = priorType;
	}

	/**
	 * @return the platformId
	 */
	public String getPlatformId() {
		return platformId;
	}

	/**
	 * @param platformId the platformId to set
	 */
	public void setPlatformId(String platformId) {
		this.platformId = platformId;
	}

	/**
	 * @return the cooperateId
	 */
	public String getCooperateId() {
		return cooperateId;
	}

	/**
	 * @param cooperateId the cooperateId to set
	 */
	public void setCooperateId(String cooperateId) {
		this.cooperateId = cooperateId;
	}
}

package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbasicinfoex;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 	LH
 *
 * @version V1.0.1
 *
 * 预收账户充退
 *
 * serialNo		业务流水号
 *			channelType		预收渠道
 *			fundChannel		借方总账科目
 *			repayAcctNo		贷方账号
 *			ccy				币种
 *			amt				金额
 *			outSerialNo		外部流水号
 *			memo			摘要信息
 *			receiptNo		出帐编号
 *			pkgList			循环报文
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns216 extends WorkUnit {

	String  serialNo; 
	String	channelType;
	String	fundChannel;
	String  repayAcctNo;
	String depositType;//1/空-预收户 4-长款户
	String  ccy; 
	FabAmount amt; 
	String  outSerialNo; 
	String  memo; 
	String  receiptNo; 
	String  platformId; 
	ListMap pkgList;
	LoanAgreement loanAgreement;
	private String  cooperateId;//赔付方代码

	String	customType;

	String reserve="";//会计记账事件 前端透传
	@Autowired 
	LoanEventOperateProvider eventProvider;
	
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		String eventCode = ConstantDeclare.EVENT.NBACPREACT;
		
		if(null == platformId)
		{
			platformId = "";
		}

		if(amt.isNegative() || amt.isZero()){
			throw new FabException("LNS018","预收充退金额");
		}
		
		if(VarChecker.isEmpty(repayAcctNo)){
			throw new FabException("LNS010");
		}
		
		if(VarChecker.isEmpty(channelType)){
			throw new FabException("LNS039");
		}
		else if( "F".equals(channelType) )	//预收转付新增事件
		{
			eventCode = ConstantDeclare.EVENT.TBACPREACT;
		}

		//适应前端数据
		if(!VarChecker.isEmpty(reserve)){
			reserve=reserve.replaceAll("reserve7","reserv7");
			reserve=reserve.replaceAll("reserve8","reserv8");
		}
		//预收渠道8时必输赔付方代码
		if("8".equals(channelType)){
			if(VarChecker.isEmpty(cooperateId)) {
				throw new FabException("LNS055", "赔付方代码");
			}
			if(VarChecker.isEmpty(fundChannel)) {
				throw new FabException("LNS055", "借方总账科目");
			}
			if(VarChecker.isEmpty(outSerialNo)) {
				throw new FabException("LNS055", "外部流水号");
			}
		}
		
		
		//预收渠道5时必输平台方代码
		if( VarChecker.asList("3","5").contains(channelType) ){
			TblLnsbasicinfo lnsbasicinfo = null;
			Map<String,Object> param = new HashMap<String,Object>();
			param.put("acctno", receiptNo);
			param.put("openbrc", ctx.getBrc());
			try {
				lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
			}
			catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}

			if( null != lnsbasicinfo &&
				!VarChecker.asList("2412610","2412622","2412627","2412628","2412629"
							,"2412614","2512614","2512615","2512622","2512621","2512615","2512616").contains(lnsbasicinfo.getPrdcode())) {
				
				if( null != platformId && !VarChecker.isEmpty(platformId)  )
//					throw new FabException("LNS055","平台方代码");
				{
					Map<String,String> stringJson = new HashMap<>();
					stringJson.put("channelType", channelType);//预收渠道
					stringJson.put("platformId", platformId); //平台方代码
			        AccountingModeChange.saveInterfaceEx(ctx, receiptNo, ConstantDeclare.KEYNAME.YCQD, "预收渠道", JsonTransfer.ToJson(stringJson));
				}
			}
		}
				
		//赔付方代码，防止后面出现空指针
		if(VarChecker.isEmpty(cooperateId))
			cooperateId="";

		
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
		lnsinterface.setUserno(repayAcctNo);
		//冲销金额
		lnsinterface.setTranamt(amt.getVal());
		//银行流水号/易付宝单号/POS单号
		lnsinterface.setBankno(outSerialNo);
		//借据号
		lnsinterface.setMagacct(receiptNo);
		//赔付方代码
		lnsinterface.setReserv4(cooperateId);
		//新增预收渠道和银行科目号 2019-01-10
		lnsinterface.setReserv5(channelType);
		lnsinterface.setBillno(fundChannel);

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

//		//更新预收账户金额并读取账户信息
//		Map<String,Object> map = new HashMap<String, Object>();
//
//		map.put("brc", ctx.getBrc());
//		map.put("acctno", repayAcctNo);
//		map.put("accsrccode", "N");
//		map.put("balance", amt.getVal());
//
//		TblLnsprefundaccount lnsprefundaccount = null;
//		try {
//			lnsprefundaccount = DbAccessUtil.queryForObject("CUSTOMIZE.update_lnsprefundaccount_sub", map, TblLnsprefundaccount.class);
//		}
//		catch (FabSqlException e)
//		{
//			throw new FabException(e, "SPS102", "lnsprefundaccount");
//		}
//		if(null == lnsprefundaccount){
//			throw new FabException("LNS013");
//		}
//		//更新后金额为负报错
//		if(new FabAmount(lnsprefundaccount.getBalance()).isNegative()){
//			throw new FabException("LNS019");
//		}

		//将借据号通过tunneldate字段传递给辅助账户明细表
		ctx.setTunnelData(receiptNo);

		//预收账户冲退 4-扣减长款户金额，1/空-扣减预收户金额
		TblLnsassistdyninfo preaccountInfo = LoanAssistDynInfoUtil.updatePreaccountInfo(ctx, ctx.getBrc(), repayAcctNo, "4".equals(depositType) ? 
																						ConstantDeclare.ASSISTACCOUNT.SURPLUSACCT : 
																						ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT, amt, "sub",customType);
		
		List<FabAmount> amtList = new ArrayList<FabAmount>();
		if("4".equals(depositType)){
			amtList.add(new FabAmount(0.00));
			amtList.add(amt);
		}
		else{
			amtList.add(amt);
		}
		LnsAcctInfo oppAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT, new FabCurrency());


		if(new FabAmount(preaccountInfo.getCurrbal()).isNegative())
			throw new FabException("LNS019");

		//预收账户冲退
//		AccountingModeChange.saveLnsprefundsch(ctx, 1, lnsprefundaccount.getAcctno(), lnsprefundaccount.getCustomid(), "N",lnsprefundaccount.getCusttype() ,
//				lnsprefundaccount.getName() ,amt.getVal() ,"sub" );

		LnsAcctInfo lnsAcctInfo = null;
		if(!VarChecker.isEmpty(receiptNo)){
			//处理老数据acctno与acctno1不一致
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("acctno1", receiptNo);
//			paramMap.put("brc", ctx.getBrc());
			
			Map<String, Object> custominfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_acctno_exist", paramMap);
			if (custominfo != null)
			{
				receiptNo = custominfo.get("acctno").toString();
				lnsAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			}
			//2021-07-20 适应新模型开户数据 暂时不报错
//			else
//                throw new FabException("ACC108", receiptNo);
		}
		FundInvest	fundInvest = new FundInvest("", "", channelType, fundChannel, outSerialNo);
		
		

		if( null != receiptNo && !VarChecker.isEmpty(receiptNo)  )
		{
//			loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(receiptNo, ctx);
			Map<String, Object> queryparam = new HashMap<>();
            queryparam.put("acctno", receiptNo);
            queryparam.put("openbrc", ctx.getBrc());
            queryparam.put("key", ConstantDeclare.BASICINFOEXKEY.MFFX);
            TblLnsbasicinfoex lnsbasicinfoex;
            try {
                lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", queryparam, TblLnsbasicinfoex.class);
            } catch (FabSqlException e) {
                throw new FabException("SPS100", "lnsbasicinfoex", e);
            }
			//写事件
			if( !VarChecker.isEmpty(lnsbasicinfoex) )
				eventProvider.createEvent(eventCode, amt, lnsAcctInfo, oppAcctInfo, 
						fundInvest, ConstantDeclare.BRIEFCODE.YSCT, ctx, amtList,
						//预收和保理客户类型统一放至备用3方便发送事件时转换处理
						preaccountInfo.getCustomid(), lnsbasicinfoex.getValue1(), preaccountInfo.getCusttype().trim(),cooperateId,reserve);
			else
//				if("4".equals(depositType))
//					eventProvider.createEvent(ConstantDeclare.EVENT.BACKSURACT, amt, lnsAcctInfo, oppLAcctInfo, 
//							fundInvest, ConstantDeclare.BRIEFCODE.CKCT, ctx,
//							//预收和保理客户类型统一放至备用3方便发送事件时转换处理
//							preaccountInfo.getCustomid(), platformId, preaccountInfo.getCusttype().trim(),cooperateId);
//				else
					eventProvider.createEvent(eventCode, amt, lnsAcctInfo, oppAcctInfo, 
							fundInvest, ConstantDeclare.BRIEFCODE.YSCT, ctx, amtList,
							//预收和保理客户类型统一放至备用3方便发送事件时转换处理
							preaccountInfo.getCustomid(), platformId, preaccountInfo.getCusttype().trim(),cooperateId,reserve);
		
		}
		else
		{
//			if("4".equals(depositType))
//				eventProvider.createEvent(ConstantDeclare.EVENT.BACKSURACT, amt, lnsAcctInfo, oppLAcctInfo, 
//						fundInvest, ConstantDeclare.BRIEFCODE.CKCT, ctx,
//						//预收和保理客户类型统一放至备用3方便发送事件时转换处理
//						preaccountInfo.getCustomid(), platformId, preaccountInfo.getCusttype().trim(),cooperateId);
//			else
				//写事件
				eventProvider.createEvent(eventCode, amt, lnsAcctInfo, oppAcctInfo, 
						fundInvest, ConstantDeclare.BRIEFCODE.YSCT, ctx, amtList,
						//预收和保理客户类型统一放至备用3方便发送事件时转换处理
						preaccountInfo.getCustomid(), platformId, preaccountInfo.getCusttype().trim(),cooperateId,reserve);
		}
		

		
	}

	/**
	 * Gets the value of cooperateId.
	 *
	 * @return the value of cooperateId
	 */
	public String getCooperateId() {
		return cooperateId;
	}

	/**
	 * Sets the cooperateId.
	 *
	 * @param cooperateId cooperateId
	 */
	public void setCooperateId(String cooperateId) {
		this.cooperateId = cooperateId;

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
	 * @return the pkgList
	 */
	public ListMap getPkgList() {
		return pkgList;
	}

	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(ListMap pkgList) {
		this.pkgList = pkgList;
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
	 * @return the depositType
	 */
	public String getDepositType() {
		return depositType;
	}

	/**
	 * @param depositType the depositType to set
	 */
	public void setDepositType(String depositType) {
		this.depositType = depositType;
	}

	/**
	 * @return the customType
	 */
	public String getCustomType() {
		return customType;
	}

	/**
	 * @param customType the customType to set
	 */
	public void setCustomType(String customType) {
		this.customType = customType;
	}

	public String getReserve() {
		return reserve;
	}

	public void setReserve(String reserve) {
		this.reserve = reserve;
	}


}

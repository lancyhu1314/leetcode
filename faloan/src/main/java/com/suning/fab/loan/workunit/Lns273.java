package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
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
 * 法催费用功能转列
 * @Author 15041590
 * @Date 2022/9/23 10:17
 * @Version 1.0
 */
@Scope("prototype")
@Repository
public class Lns273 extends WorkUnit {

	String  serialNo;
	String  feeBusiNo;
	String  feeBusiType;
	String	repayAcctNo;
	String	bankSubject;
	String	ccy;
	String	repayChannel;
	String	platformId;
	String	outSerialNo;
	FabAmount	accumRepayAmt;//累计已还金额	
	FabAmount	repayAmt;//本次已还金额	
	FabAmount	noPayAmt;//未还金额	
	String	status;//结清标识	N-正常 CA-销户（结清）

	LoanAgreement loanAgreement;


	String reserve="";//会计记账事件 前端透传
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		TblLnsassistdyninfo preaccountInfo_O;
		String eventCode = ConstantDeclare.EVENT.LNFCFYRPAY;
		accumRepayAmt=new FabAmount(0.00);
		noPayAmt=new FabAmount(0.00);
		
		//参数校验
		checkeInput();

		preaccountInfo_O=LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), feeBusiNo, "", feeBusiType, getTranctx());
		if(null!=preaccountInfo_O && "CA".equals(preaccountInfo_O.getStatus())){
			throw new FabException("LNS262");	
		}
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
		lnsinterface.setUserno(feeBusiNo);
		//开户金额
		lnsinterface.setTranamt(repayAmt.getVal());
		//时间戳
		lnsinterface.setTimestamp(ctx.getTranTime());
		//客户ID	
		lnsinterface.setMagacct(feeBusiNo);
		lnsinterface.setReserv1(repayChannel);
		lnsinterface.setReserv3(feeBusiType);


		try {
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
		}
		catch (FabSqlException e)
		{
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				 Map<String, Object> params = new HashMap<String, Object>();
	                params.put("serialno", getSerialNo());
	                params.put("trancode", tranctx.getTranCode());

	                try {
	                    lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", params,
	                            TblLnsinterface.class);
	                } catch (FabSqlException f) {
	                    throw new FabException(f, "SPS103", "lnsinterface");
	                }
	                repayAmt = new FabAmount(lnsinterface.getTranamt());
	                feeBusiNo = lnsinterface.getUserno();
	                accumRepayAmt=new FabAmount(lnsinterface.getSumrfint());
	                noPayAmt=new FabAmount(lnsinterface.getSumrint());
	                status=lnsinterface.getReserv2();
	                tranctx.setTranDate(lnsinterface.getAccdate());
	                tranctx.setSerSeqNo(lnsinterface.getSerseqno());
				throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
			} else {
				throw new FabException(e, "SPS100", "lnsinterface");
			}
		}

		Map<String,String> tunnelData = new HashMap<String,String>();
		tunnelData.put("repayAcctNo",repayAcctNo );
		tunnelData.put("bankSubject",bankSubject );
		tunnelData.put("repayChannel",repayChannel );
		tunnelData.put("platformId",platformId );
		tunnelData.put("outSerialNo",outSerialNo );
		tunnelData.put("serialNo",serialNo );


		List<FabAmount> amtList1 = new ArrayList<FabAmount>();

		LnsAcctInfo lnsAcctInfo = null;

		//将借据号通过tunneldate字段传递给辅助账户明细表
		ctx.setTunnelData(feeBusiNo);
		//预收账户减款
		if("2".equals(repayChannel)){
			FundInvest fundInvest = new FundInvest("", "", "", "", "");
			LnsAcctInfo oppAcctInfo = new LnsAcctInfo(feeBusiNo,feeBusiType ,preaccountInfo_O.getReserv1(), new FabCurrency());
			lnsAcctInfo = new LnsAcctInfo(repayAcctNo,ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT , ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			amtList1.add(new FabAmount(0.00));
			//预收户减款
			TblLnsassistdyninfo lnsassistdyninfo = LoanAssistDynInfoUtil.updatePreaccountInfo(ctx, ctx.getBrc(), repayAcctNo, ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT, repayAmt, "sub", null);
			//预收户余额不足
			if(new FabAmount(lnsassistdyninfo.getCurrbal()).isNegative())
				throw new FabException("LNS019");

			//预收还款事件
			eventProvider.createEvent("LNDKHKYSJK", repayAmt, lnsAcctInfo, oppAcctInfo,
					fundInvest, "YSJK", ctx, amtList1,
					//预收和保理客户类型统一放至备用3方便发送事件时转换处理
					"", lnsassistdyninfo.getFeetype(), lnsassistdyninfo.getCusttype(), "", repayAcctNo);
		}
		//预收账户通过tunneldate字段传递给辅助账户明细表
		ctx.setTunnelData(repayAcctNo);
		// 扣减费用户的金额
		String eventCustomid="";
		String eventCusttype="";
		TblLnsassistdyninfo preaccountInfo_N;

		preaccountInfo_N = LoanAssistDynInfoUtil.updateLnsAssistDynInfo(ctx, ctx.getBrc(), feeBusiNo, feeBusiType, repayAmt, "sub","", "","",feeBusiType,"","");
		if(null!=preaccountInfo_N && new FabAmount(preaccountInfo_N.getCurrbal()).isNegative()){
			throw new FabException("LNS258", feeBusiNo, feeBusiType);
		}
		eventCustomid = preaccountInfo_N.getCustomid();
		eventCusttype = preaccountInfo_N.getCusttype();
		
		//合并json串
		Map oldTunnelMap= (Map)JSONObject.parse(preaccountInfo_N.getTunneldata());
		oldTunnelMap.putAll(tunnelData);

		//LnsAcctInfo oppAcctInfo = new LnsAcctInfo(feeBusiNo, feeBusiType, ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT, new FabCurrency());
		//FaloanJson tunnelMap = new FaloanJson(JSONObject.parseObject(preaccountInfo_N.getTunneldata()));
		
		lnsAcctInfo = new LnsAcctInfo(feeBusiNo, feeBusiType, preaccountInfo_N.getReserv1(), new FabCurrency());
		List<FabAmount> amtList = new ArrayList<FabAmount>();
		//amtList.clear();
		//律师费税额
		FabAmount taxAmt=TaxUtil.calcVAT(repayAmt);
		if("LSF".equals(feeBusiType))
			amtList.add(taxAmt);
		else
			amtList.add(new FabAmount(0.00));
		//写费用还款事件
		eventProvider.createEvent(eventCode, repayAmt, lnsAcctInfo, null,
				null, ConstantDeclare.BRIEFCODE.FCHK, ctx,amtList,
				eventCustomid, preaccountInfo_N.getReserv1(), eventCusttype,"",JsonTransfer.ToJson(oldTunnelMap));

	
		accumRepayAmt.selfAdd(new FabAmount(Double.valueOf(preaccountInfo_N.getReserv4())).selfSub(new FabAmount(preaccountInfo_N.getCurrbal())));
		noPayAmt=new FabAmount(preaccountInfo_N.getCurrbal());
		status=preaccountInfo_N.getStatus();
		//幂等信息登记
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("sumrfint", accumRepayAmt.getVal());
		map.put("sumrint", noPayAmt.getVal());
		map.put("sumramt", repayAmt.getVal());
		map.put("status", status);
		map.put("serialno", getSerialNo());
		map.put("trancode", ctx.getTranCode());
		
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_fc_repay", map);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsinterface");
		}
	}
	
	//参数校验
    private void  checkeInput()  throws FabException{
    	if(VarChecker.isEmpty(serialNo)){
			throw new FabException("LNS055","业务流水号");
		}
		if(serialNo.getBytes().length > 50){
			throw new FabException("LNS112","业务流水号");
		}
		if(VarChecker.isEmpty(ccy)){
			throw new FabException("LNS055","币种");
		}
		if(VarChecker.isEmpty(repayChannel)){
			throw new FabException("LNS055","还款渠道");
		}
		
		if("2".equals(repayChannel) && VarChecker.isEmpty(repayAcctNo)){
			throw new FabException("LNS261");
		}
		//费用业务编号非空校验
		if(VarChecker.isEmpty(feeBusiNo)){
			throw new FabException("LNS055","费用业务编号");
		}
		if(feeBusiNo.getBytes().length > 32){
			throw new FabException("LNS112","费用业务编号：feeBusiNo字段");
		}
		//费用类型非空校验
		if(VarChecker.isEmpty(feeBusiType)){
			throw new FabException("LNS055","费用类型");
		}
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

	public String getFeeBusiNo() {
		return feeBusiNo;
	}

	public void setFeeBusiNo(String feeBusiNo) {
		this.feeBusiNo = feeBusiNo;
	}

	public String getFeeBusiType() {
		return feeBusiType;
	}

	public void setFeeBusiType(String feeBusiType) {
		this.feeBusiType = feeBusiType;
	}

	public String getRepayAcctNo() {
		return repayAcctNo;
	}

	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
	}

	public String getBankSubject() {
		return bankSubject;
	}

	public void setBankSubject(String bankSubject) {
		this.bankSubject = bankSubject;
	}

	public String getCcy() {
		return ccy;
	}

	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	public String getRepayChannel() {
		return repayChannel;
	}

	public void setRepayChannel(String repayChannel) {
		this.repayChannel = repayChannel;
	}

	public String getPlatformId() {
		return platformId;
	}

	public void setPlatformId(String platformId) {
		this.platformId = platformId;
	}

	public String getOutSerialNo() {
		return outSerialNo;
	}

	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}

	public FabAmount getAccumRepayAmt() {
		return accumRepayAmt;
	}

	public void setAccumRepayAmt(FabAmount accumRepayAmt) {
		this.accumRepayAmt = accumRepayAmt;
	}

	public FabAmount getRepayAmt() {
		return repayAmt;
	}

	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}

	public FabAmount getNoPayAmt() {
		return noPayAmt;
	}

	public void setNoPayAmt(FabAmount noPayAmt) {
		this.noPayAmt = noPayAmt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public String getReserve() {
		return reserve;
	}

	public void setReserve(String reserve) {
		this.reserve = reserve;
	}
}

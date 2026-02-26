package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.TaxUtil;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
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
 * 法催费用开户
 * @Author 15041590
 * @Date 2022/9/23 10:17
 * @Version 1.0
 */
@Scope("prototype")
@Repository
public class Lns271 extends WorkUnit {

	String  serialNo;
	String  caseNo;
	String  lawFirm;
	String	acceptCourt;
	String	customId;


	ListMap pkgList1;
	LoanAgreement loanAgreement;

	String	customType;

	String reserve="";//会计记账事件 前端透传
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		
		//参数校验
		checkeInput();
		
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
		lnsinterface.setUserno(customId);
		//开户金额
		lnsinterface.setTranamt(0.00);
		//时间戳
		lnsinterface.setTimestamp(ctx.getTranTime());
		//客户ID	
		lnsinterface.setMagacct(customId);
		lnsinterface.setReserv1(customType);
		lnsinterface.setReserv3(acceptCourt);
		lnsinterface.setOrgid(lawFirm);
		
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
		 // 费用开户明细
        ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		for (PubDict dict : pkgList1.getLoopmsg()) {
			List<FabAmount> amtList = new ArrayList<FabAmount>();
			// 诉讼费金额
			FabAmount openAmt = PubDict.getRequestDict(dict, "openAmt") ;
			if(null == openAmt)
				openAmt = new FabAmount(0.00);
			
			
			
			//费用功能码
			String feeFunCode = PubDict.getRequestDict(dict, "feeFunCode");
			//费用业务编号
			String feeBusiNo = PubDict.getRequestDict(dict, "feeBusiNo");
			//费用类型
			String feeBusiType = PubDict.getRequestDict(dict, "feeBusiType");
			//付款单号
			String payorderNo = PubDict.getRequestDict(dict, "payorderNo");
			//付款日期	
			String payDate = PubDict.getRequestDict(dict, "payDate");
			//发票号码	
			String invoiceNo = PubDict.getRequestDict(dict, "invoiceNo");
			//发票代码	
			String invoiceCode = PubDict.getRequestDict(dict, "invoiceCode");
			//发票代码	
			String feeReqNo = PubDict.getRequestDict(dict, "feeReqNo");
			//将借据号通过tunneldate字段传递给辅助账户明细表
			ctx.setTunnelData(customId);
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
			//费用类型为律师费时，发票代码必传校验
			if("LSF".equals(feeBusiType) && VarChecker.isEmpty(invoiceCode)){
				throw new FabException("LNS263");
			}
			//费用类型为律师费时，发票号必传校验
			if("LSF".equals(feeBusiType) && VarChecker.isEmpty(invoiceNo)){
				throw new FabException("LNS264");
			}
			if(VarChecker.isEmpty(feeFunCode)){
				feeFunCode="1";
			}
			if(feeFunCode.getBytes().length > 1){
				throw new FabException("LNS112","费用功能码：feeFunCode字段");
			}
			if(!"1".equals(feeFunCode))
				throw new FabException("LNS169","费用功能码：",feeFunCode);
			//律师费税额
			FabAmount taxAmt=TaxUtil.calcVAT(openAmt);
			if("LSF".equals(feeBusiType))
				amtList.add(taxAmt);
			else
				amtList.add(new FabAmount(0.00));
			// 增加预收户的金额
			String eventCustomid="";
			String eventCusttype="";
			TblLnsassistdyninfo preaccountInfo_N;
			Map<String,String> tunnelData = new HashMap<String,String>();
			tunnelData.put("customId",customId );
			tunnelData.put("caseNo",caseNo );
			tunnelData.put("payorderNo",payorderNo );
			tunnelData.put("payDate",payDate );
			tunnelData.put("invoiceNo",invoiceNo );
			tunnelData.put("invoiceCode",invoiceCode );
			tunnelData.put("feeReqNo",feeReqNo );
			tunnelData.put("feeFunCode",feeFunCode );
			tunnelData.put("acceptCourt",acceptCourt );
			tunnelData.put("lawFirm",lawFirm );

			TblLnsassistdyninfo preaccountInfo_O=LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), feeBusiNo, "", feeBusiType, getTranctx());
			if(null!=preaccountInfo_O ){
				throw new FabException("LNS265",feeBusiNo);	
			}
			preaccountInfo_N = LoanAssistDynInfoUtil.updateLnsAssistDynInfo(ctx, ctx.getBrc(), feeBusiNo, feeBusiType, openAmt, "add", customType,customId,feeFunCode,acceptCourt,lawFirm, JsonTransfer.ToJson(tunnelData));
			eventCustomid = preaccountInfo_N.getCustomid();
			eventCusttype = preaccountInfo_N.getCusttype();

			//LnsAcctInfo oppAcctInfo = new LnsAcctInfo(feeBusiNo, feeBusiType, ConstantDeclare.ASSISTACCOUNTINFO.SURPLUSACCT, new FabCurrency());

			LnsAcctInfo lnsAcctInfo = null;

			lnsAcctInfo = new LnsAcctInfo(feeBusiNo, feeBusiType, feeFunCode, new FabCurrency());

			//FaloanJson tunnelMap = new FaloanJson(JSONObject.parseObject(JsonTransfer.ToJson(tunnelData)));
			//String mapFeeReqNo=tunnelMap.getString("feeReqNo");
			//写事件
			if( openAmt.isPositive() ){
				if("SSF".equals(feeBusiType))
				eventProvider.createEvent(ConstantDeclare.EVENT.LNFCFYKHJT, openAmt, lnsAcctInfo, null,
						null, ConstantDeclare.BRIEFCODE.FCJT, ctx,amtList,
						//预收和保理客户类型统一放至备用3方便发送事件时转换处理
						eventCustomid, feeFunCode, eventCusttype,"",JsonTransfer.ToJson(tunnelData));
				else if("LSF".equals(feeBusiType))
					eventProvider.createEvent(ConstantDeclare.EVENT.LNFCFYKHJT, openAmt, lnsAcctInfo, null,
							null, ConstantDeclare.BRIEFCODE.FCJT, ctx,amtList,
							//预收和保理客户类型统一放至备用3方便发送事件时转换处理
							eventCustomid, feeFunCode, eventCusttype, "",JsonTransfer.ToJson(tunnelData));
			}
		}
	}

	//参数校验
    private void  checkeInput()  throws FabException{
    	// 法催开户的时候，pkgList1不能为空
    			if(null == pkgList1 || pkgList1.size()==0)
    				throw new FabException("LNS223");

    			if(pkgList1.size() > Integer.valueOf(GlobalScmConfUtil.getProperty("PrefundListSize", "200"))) 
    				throw new FabException("LNS112","pkgList1");
    			
    			if(VarChecker.isEmpty(serialNo)){
    				throw new FabException("LNS055","业务流水号");
    			}
    			if(serialNo.getBytes().length > 32){
    				throw new FabException("LNS112","业务流水号");
    			}
    			
    			if(VarChecker.isEmpty(customId)){
    				throw new FabException("LNS055","客户ID");
    			}
    			if(VarChecker.isEmpty(customType)){
    				throw new FabException("LNS055","客户类型");
    			}
    			
    			if(!VarChecker.isEmpty(lawFirm) && lawFirm.getBytes().length > 10){
    				throw new FabException("LNS112","承接律所：lawFirm字段");
    			}
    			if(!VarChecker.isEmpty(acceptCourt) && acceptCourt.getBytes().length > 10){
    				throw new FabException("LNS112","受理法院：acceptCourt字段");
    			}
    			if(!VarChecker.isEmpty(customId) && customId.getBytes().length > 20){
    				throw new FabException("LNS112","客户ID：customId字段");
    			}
    			if(!VarChecker.isEmpty(customType) && customType.getBytes().length > 1){
    				throw new FabException("LNS112","客户类型：customType字段");
    			}
    			 if (!VarChecker.asList("1","2").contains(customType)) {
    				 throw new FabException("LNS169","客户类型：",customType);
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

	
	public String getCustomId() {
		return customId;
	}

	public void setCustomId(String customId) {
		this.customId = customId;
	}


	/**
	 * @return the pkgList1
	 */
	public ListMap getPkgList1() {
		return pkgList1;
	}

	/**
	 * @param pkgList1 the pkgList1 to set
	 */
	public void setPkgList1(ListMap pkgList1) {
		this.pkgList1 = pkgList1;
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

	public String getCaseNo() {
		return caseNo;
	}

	public void setCaseNo(String caseNo) {
		this.caseNo = caseNo;
	}

	public String getLawFirm() {
		return lawFirm;
	}

	public void setLawFirm(String lawFirm) {
		this.lawFirm = lawFirm;
	}

	public String getAcceptCourt() {
		return acceptCourt;
	}

	public void setAcceptCourt(String acceptCourt) {
		this.acceptCourt = acceptCourt;
	}


	public String getReserve() {
		return reserve;
	}

	public void setReserve(String reserve) {
		this.reserve = reserve;
	}
}

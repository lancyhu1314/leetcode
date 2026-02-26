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
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 法催费用功能转列
 * @Author 15041590
 * @Date 2022/9/23 10:17
 * @Version 1.0
 */
@Scope("prototype")
@Repository
public class Lns272 extends WorkUnit {

	String  serialNo;
	String  feeBusiNo;
	String  feeBusiType;
	String	newfeeFunCode;

	LoanAgreement loanAgreement;


	String reserve="";//会计记账事件 前端透传
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		String eventCode = ConstantDeclare.EVENT.LNFCFYZTJZ;

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
		lnsinterface.setUserno(feeBusiNo);
		//开户金额
		lnsinterface.setTranamt(0.00);
		//时间戳
		lnsinterface.setTimestamp(ctx.getTranTime());
		//客户ID	
		lnsinterface.setMagacct(feeBusiNo);
		lnsinterface.setReserv1(newfeeFunCode);
		lnsinterface.setReserv3(feeBusiType);

		
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


			List<FabAmount> amtList = new ArrayList<FabAmount>();
			FabAmount amt =new FabAmount(0.00);
			FabAmount eventAmt;
			//将借据号通过tunneldate字段传递给辅助账户明细表
			ctx.setTunnelData(feeBusiNo);
			TblLnsassistdyninfo preaccountInfo_O;
			
			preaccountInfo_O=LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), feeBusiNo, "", feeBusiType, ctx);
			if (null == preaccountInfo_O) {
				throw new FabException("SPS104", "lnsassistdyninfo");
			}
			eventAmt=new FabAmount(preaccountInfo_O.getCurrbal());

			if (VarChecker.asList(newfeeFunCode).contains(preaccountInfo_O.getReserv1())) {
				throw new FabException(ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
			}else if (!"1".equals(preaccountInfo_O.getReserv1())){
				throw new FabException("LNS266");
			}
			
			if("2".equals(newfeeFunCode)){
				amt=eventAmt;
			}
			// 增加预收户的金额
			String eventCustomid="";
			String eventCusttype="";
			TblLnsassistdyninfo preaccountInfo_N;

			preaccountInfo_N = LoanAssistDynInfoUtil.updateLnsAssistDynInfo(ctx, ctx.getBrc(), feeBusiNo, feeBusiType, amt, "sub","","",newfeeFunCode,feeBusiType,"","");
			if(null!=preaccountInfo_N && new FabAmount(preaccountInfo_N.getCurrbal()).isNegative()){
				throw new FabException("LNS258", feeBusiNo, feeBusiType);
			}
			eventCustomid = preaccountInfo_N.getCustomid();
			eventCusttype = preaccountInfo_N.getCusttype();

			LnsAcctInfo oppAcctInfo = new LnsAcctInfo(feeBusiNo, feeBusiType, newfeeFunCode, new FabCurrency());

			LnsAcctInfo lnsAcctInfo = null;

			lnsAcctInfo = new LnsAcctInfo(feeBusiNo, feeBusiType, preaccountInfo_O.getReserv1(), new FabCurrency());
			
			//律师费税额
			FabAmount taxAmt=TaxUtil.calcVAT(eventAmt);
			if("LSF".equals(feeBusiType)){
				amtList.add(taxAmt);
			}else{
				amtList.add(new FabAmount(0.00));
			}

			//写事件
				eventProvider.createEvent(eventCode, eventAmt, lnsAcctInfo, oppAcctInfo,
						null, ConstantDeclare.BRIEFCODE.FCZL, ctx,amtList,
						eventCustomid, newfeeFunCode, eventCusttype,"",preaccountInfo_N.getTunneldata());
		
	}
	
	//参数校验
    private void  checkeInput()  throws FabException{
    	if(VarChecker.isEmpty(serialNo)){
			throw new FabException("LNS055","业务流水号");
		}
		if(serialNo.getBytes().length > 32){
			throw new FabException("LNS112","业务流水号");
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
		//费用业务编号非空校验
		if(VarChecker.isEmpty(newfeeFunCode)){
			throw new FabException("LNS055","修改后费用功能码");
		}
		if (!VarChecker.asList("2", "3").contains(newfeeFunCode)) {
			throw new FabException("LNS259",newfeeFunCode);
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

	public String getNewfeeFunCode() {
		return newfeeFunCode;
	}

	public void setNewfeeFunCode(String newfeeFunCode) {
		this.newfeeFunCode = newfeeFunCode;
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

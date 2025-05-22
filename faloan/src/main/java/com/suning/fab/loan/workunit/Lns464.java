/*
 * Copyright (C), 2002-2020, 苏宁易购电子商务有限公司
 * FileName: Lns502.java
 * Author:   
 * Date:     
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.workunit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsAmortizeplan;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
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
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：还款结果通知
 *
 * @author 
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns464 extends WorkUnit {

	String serialNo;		//业务流水号
	String acctNo;			//贷款账号
	String tranBrc;			//交易机构
	String feeBrc;			//收费机构
	String feeType;			//费用类型
	String repayChannel;	//还款渠道
	FabAmount tranAmt;		//本次还款金额
	
	@Autowired 
	LoanEventOperateProvider eventProvider;
	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		if( VarChecker.isEmpty(serialNo) || 
			VarChecker.isEmpty(acctNo) || 
			VarChecker.isEmpty(tranBrc) || 
			VarChecker.isEmpty(feeBrc) || 
			VarChecker.isEmpty(feeType) || 
			VarChecker.isEmpty(repayChannel))
		{
			throw new FabException("LNS055", "接口入参");
		}
		
		LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		
		if( "2".equals(feeType))
		{
			if( null != loanAgreement.getContract().getSettleDate() )
			{
				if(loanAgreement.getContract().getSettleDate().equals(ctx.getTranDate()) )
					throw new FabException("结清日当天不允许调用");
				
				if( CalendarUtil.after(ctx.getTranDate(), loanAgreement.getContract().getSettleDate()))
				{
					/*
					 *1-幂等登记簿472023的融担费类型实收金额汇总
					 *2-摊销计划表已摊销金额汇总
					 *3-实收>摊销补一条摊销正向纪录，ACCRUEDFEE-GDJT
					 *4-实收<摊销补一条摊销冲销记录，WRITOFFFEE-GDCX
					 */
					Map<String,Object> param = new HashMap<>();
			        param.put("brc", tranctx.getBrc());
			        param.put("acctno", acctNo);
			        param.put("trancode",  "470023");
					 //查询幂等登记簿
		            List<TblLnsinterface>  lnsinterfaces;
		            try{
		            	lnsinterfaces= DbAccessUtil.queryForList("Lnsinterface.query_repay_fee",param,TblLnsinterface.class);
		            }catch(FabException e){
		    			throw new FabException(e, "SPS103", "LNSINTERFACE");
		    		}
		            
		            FabAmount currAmt = new FabAmount( tranAmt.getVal() );
		            if( null != lnsinterfaces && lnsinterfaces.size()>0 )
		            {
		            	 for(TblLnsinterface lnsinterface:lnsinterfaces){
		            		 if( "2".equals(lnsinterface.getReserv2()) )
		            			 currAmt.selfAdd(lnsinterface.getTranamt());
		                 }
		            }
		            
		            
		            param.put("amortizetype", ConstantDeclare.AMORTIZETYPE.AMORTIZEGDBF);
		            LnsAmortizeplan lnsamortizeplan;

		            try {
		                lnsamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsamortizeplan", param,
		                        LnsAmortizeplan.class);
		            } catch(FabSqlException e) {
		                throw new FabException(e, "SPS102", "CUSTOMIZE.query_lnsamortizeplan");
		            }

		            if (null == lnsamortizeplan) {
		                throw new FabException("LNS021");
		            }
		            
		            String childBrc = null;
		            for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
	    				if(  (ConstantDeclare.BILLTYPE.BILLTYPE_GDBF.equals(lnsfeeinfo.getFeetype() ) ) )
	    					childBrc = lnsfeeinfo.getFeebrc();
		    		}
		            LnsAcctInfo lnsAcctInfo = new LnsAcctInfo( acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
		    				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		            //正向
		            if( currAmt.sub(lnsamortizeplan.getAmortizeamt()).isPositive()){
		            	eventProvider.createEvent(ConstantDeclare.EVENT.ACCRUEDFEE, currAmt.sub(lnsamortizeplan.getAmortizeamt()), lnsAcctInfo, null, 
								loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDJT, ctx, null,
								ctx.getTranDate(), ctx.getSerSeqNo(), 1, childBrc);
		            }
		            //逆向
		            if( currAmt.sub(lnsamortizeplan.getAmortizeamt()).isNegative()){
						eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFFEE, new FabAmount(lnsamortizeplan.getAmortizeamt()).sub(currAmt), lnsAcctInfo, null, 
								loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDCX, ctx, childBrc);
		            }
		           
		            
		            
		            /*
					 *1-幂等登记簿472023的融担费类型实收金额汇总
					 *2-摊销计划表amoruzeformual已结转金额
					 *3-实收>结费补一条摊销正向纪录，FEESETLEMT-GDJZ
					 *4-实收<结费补一条摊销冲销记录，WFFEESETLE-GDJC
					 */
		            //正向
		            if( currAmt.sub(Double.valueOf(lnsamortizeplan.getAmortizeformula())).isPositive()){
		            	eventProvider.createEvent(ConstantDeclare.EVENT.FEESETLEMT, currAmt.sub(Double.valueOf(lnsamortizeplan.getAmortizeformula())), lnsAcctInfo, null, 
								loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDJZ, ctx, null,
								ctx.getTranDate(), ctx.getSerSeqNo(), 1, childBrc);
		            }
		            //逆向
		            if( currAmt.sub(Double.valueOf(lnsamortizeplan.getAmortizeformula())).isNegative()){
						eventProvider.createEvent(ConstantDeclare.EVENT.WFFEESETLE, new FabAmount(Double.valueOf(lnsamortizeplan.getAmortizeformula())).sub(currAmt), lnsAcctInfo, null, 
								loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDJC, ctx, childBrc);
		            }
		            
		            
				}
			}
		}
		

		
		//幂等登记薄
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setSerialno(serialNo);
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode(ctx.getTranCode());
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setAcctname(loanAgreement.getCustomer().getCustomName());
		lnsinterface.setUserno(loanAgreement.getCustomer().getMerchantNo());
		lnsinterface.setAcctno(loanAgreement.getContract().getReceiptNo());
		lnsinterface.setTranamt(tranAmt.getVal());
		lnsinterface.setReserv2(feeType);
		lnsinterface.setReserv3(tranBrc);
		lnsinterface.setReserv4(feeBrc);
		lnsinterface.setReserv5(repayChannel);


		try{
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
		} catch(FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e,TRAN.IDEMPOTENCY);
			}
			else
				throw new FabException(e, "SPS100", "lnsinterface");
		}
		
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		loanAgreement.getFundInvest().setChannelType("C");
		if( "1".equals(feeType) )
			eventProvider.createEvent(ConstantDeclare.EVENT.REPAYSPLIT, tranAmt, lnsAcctInfo, null,
					loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.RBBF, ctx, feeBrc);
		else if( "2".equals(feeType) )
			eventProvider.createEvent(ConstantDeclare.EVENT.REPAYSPLIT, tranAmt, lnsAcctInfo, null,
					loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDBF, ctx, feeBrc);

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
	 * @return the tranBrc
	 */
	public String getTranBrc() {
		return tranBrc;
	}

	/**
	 * @param tranBrc the tranBrc to set
	 */
	public void setTranBrc(String tranBrc) {
		this.tranBrc = tranBrc;
	}

	/**
	 * @return the feeType
	 */
	public String getFeeType() {
		return feeType;
	}

	/**
	 * @param feeType the feeType to set
	 */
	public void setFeeType(String feeType) {
		this.feeType = feeType;
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
	 * @return the feeBrc
	 */
	public String getFeeBrc() {
		return feeBrc;
	}

	/**
	 * @param feeBrc the feeBrc to set
	 */
	public void setFeeBrc(String feeBrc) {
		this.feeBrc = feeBrc;
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
	 * @return the tranAmt
	 */
	public FabAmount getTranAmt() {
		return tranAmt;
	}

	/**
	 * @param tranAmt the tranAmt to set
	 */
	public void setTranAmt(FabAmount tranAmt) {
		this.tranAmt = tranAmt;
	}
}

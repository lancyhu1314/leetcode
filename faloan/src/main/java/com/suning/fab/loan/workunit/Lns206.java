/*
 * Copyright (C), 2002-2017, 苏宁易购电子商务有限公司
 * FileName: Lns205.java
 * Author:   16071579
 * Date:     2017年5月25日 下午4:00:57
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名            修改时间            版本号                    描述
 */
package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：放款渠道冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns206 extends WorkUnit {
	
	String errDate;		// 错误日期
	String errSerSeq;	// 错误流水号
	
	@Autowired
	LoanEventOperateProvider eventProvider;
	private TblLnsbasicinfo lnsbasicinfo;
	LoanAgreement loanAgreement;
	private Integer txseqno = 0;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("eventcode", ConstantDeclare.EVENT.LOANCHANEL);
		param.put("brc", ctx.getBrc());
		List<TblLnseventreg> lnseventregs = null; 
		
		try {
			lnseventregs = DbAccessUtil.queryForList("CUSTOMIZE.query_lnseventreg", param, TblLnseventreg.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}
		
		//账速融非标迁移冲销补充 2019-03-06
		if (null == lnseventregs || lnseventregs.isEmpty()) {
			
			Map<String, Object> param1 = new HashMap<String, Object>();
			param1.put("trandate", errDate);
			param1.put("serseqno", errSerSeq);
			param1.put("eventcode", ConstantDeclare.EVENT.LNDACHANNL);
			param1.put("brc", ctx.getBrc());
			
			try {
				lnseventregs = DbAccessUtil.queryForList("CUSTOMIZE.query_lnseventreg", param1, TblLnseventreg.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnseventreg");
			}
			
			if (null == lnseventregs) {
				return ;
			}
			
			
//			return ;
		}
		
		TblLnseventreg lnseventreg = lnseventregs.get(0);
		if(loanAgreement  == null)
			loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(lnseventreg.getReceiptno(), ctx);
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnseventreg.getReceiptno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		//留购价
		FabAmount tailAmt = new FabAmount(0.00);
		//融资额
		FabAmount contractAmt = new FabAmount(0.00);
		//首付款
		FabAmount downPayment = new FabAmount(0.00);
		//登记放款渠道冲销事件
		if("51240001".equals(ctx.getBrc())){
			lnsAcctInfo.setCustType(lnseventreg.getCusttype());

			List<FabAmount> amounts = new ArrayList<FabAmount>();
			Map<String,Object> finalaram = new HashMap<String,Object>();
			finalaram.put("acctno", lnseventreg.getReceiptno());
			finalaram.put("brc", ctx.getBrc());

			TblLnsrentreg lnsrentreg;
			try {
				lnsrentreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrentreg", finalaram, TblLnsrentreg.class);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS103", "lnsrentreg");
			}

			if (null == lnsrentreg){
				throw new FabException("SPS104", "lnsrentreg");
			}
			
			if(lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("tailAmt"))==-1){
				//根据字符串截取留购价的值
				tailAmt = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("tailAmt")).split(":")[1]));
			}else{
				if(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("tailAmt"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("tailAmt"))).split(":").length>1){
					tailAmt = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("tailAmt"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("tailAmt"))).split(":")[1]));
				}
			}
			if(lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("contractAmt"))==-1){
				//根据字符串截取融资额的值
				contractAmt = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("contractAmt")).split(":")[1]));
			}else{
				if(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("contractAmt"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("contractAmt"))).split(":").length>1){
					contractAmt = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("contractAmt"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("contractAmt"))).split(":")[1]));
				}
			}
//			if(lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("downPayment"))==-1){
//				//根据字符串截取首付款的值
//				if(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("downPayment")).split(":").length>1){
//					downPayment = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("downPayment")).split(":")[1]));
//				}
//			}else{
//				if(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("downPayment"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("downPayment"))).split(":").length>1){
//					downPayment = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("downPayment"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("downPayment"))).split(":")[1]));
//				}
//			}
			amounts.add(tailAmt);
			amounts.add(downPayment);
			amounts.add(contractAmt);
			String reserv1 = null;
			if("1".equals(lnseventreg.getCusttype())||"PERSON".equals(lnseventreg.getCusttype())){
				reserv1 = "70215243";
			}else if("2".equals(lnseventreg.getCusttype())||"COMPANY".equals(lnseventreg.getCusttype())){
				reserv1 = lnseventreg.getMerchantno();
			}else{
				reserv1 = "";
			}
			//CustomName统一放至备用5方便发送事件时转换处理
			//转json存入Tunneldata字段 
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANCNLOFF, new FabAmount(lnseventreg.getTranamt()), 
					lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FDCX, ctx,amounts,reserv1,lnseventreg.getReserv2(),"","","");
		}else{
			if("0".equals(loanAgreement.getFundInvest().getChannelType())){
				for(TblLnseventreg eventreg: lnseventregs){
					FundInvest  fundInvest = new FundInvest();
					fundInvest.setInvestee(loanAgreement.getFundInvest().getInvestee());
					fundInvest.setInvestMode(loanAgreement.getFundInvest().getInvestMode());
					fundInvest.setChannelType(eventreg.getChanneltype());
					if("1".equals(eventreg.getChanneltype()) || !VarChecker.isEmpty(eventreg.getTunneldata())){
						fundInvest.setFundChannel(eventreg.getTunneldata().split(":")[1].substring(eventreg.getTunneldata().split(":")[1].indexOf("\"")+1,eventreg.getTunneldata().split(":")[1].lastIndexOf("\"")));
					}
					fundInvest.setOutSerialNo(eventreg.getOutserialno());
					eventProvider.createEvent(ConstantDeclare.EVENT.LOANCNLOFF, new FabAmount(eventreg.getTranamt()), 
					lnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.FDCX, ctx,"",eventreg.getReserv2());
				}
			}else if ("C".equals(loanAgreement.getFundInvest().getChannelType())){
				//取幂等辅助表的租赁信息
				param.put("acctno", lnsAcctInfo.getAcctNo());
				param.put("brc", ctx.getBrc());
				param.put("key", ConstantDeclare.KEYNAME.QD);
				TblLnsinterfaceex interfaceex;
				try {
					interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", param, TblLnsinterfaceex.class);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS103", "query_lnsinterfaceex");
				}
				
				String entrustedPaybrc = null;
				if(null != interfaceex){
					JSONObject jsonObject = JSONObject.parseObject(interfaceex.getValue()) ;
					entrustedPaybrc = jsonObject.parseArray(jsonObject.getString("QD")).getJSONObject(0).getString("entrustedPaybrc");  
				}
				eventProvider.createEvent(ConstantDeclare.EVENT.LOANCNLOFF, new FabAmount(lnseventreg.getTranamt()), 
						lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FDCX, ctx,lnseventreg.getReserv1(),lnseventreg.getReserv2(),"",entrustedPaybrc);
				
			}else{
				eventProvider.createEvent(ConstantDeclare.EVENT.LOANCNLOFF, new FabAmount(lnseventreg.getTranamt()), 
						lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FDCX, ctx,lnseventreg.getReserv1(),lnseventreg.getReserv2(),"","");
			}
		}
		//放款渠道冲销
		param.clear();
		param.put("trandate", errDate);
		param.put("serseqno",errSerSeq );
		//查询幂等拓展表
		/*
		select * from lnsinterfaceex where trandate  = :lnsinterfaceex and serseqno= :serseqno and key = 'QD'
		 */
		TblLnsinterfaceex lnsinterfaceex;
		try{
			lnsinterfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceexQD", param, TblLnsinterfaceex.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if(lnsbasicinfo==null){
			param.clear();
			param.put("acctno", loanAgreement.getContract().getReceiptNo());
			param.put("openbrc", ctx.getBrc());
			try{
				lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}
		}

		//查询资金方登记簿
		param.put("acctno", loanAgreement.getContract().getReceiptNo());
		param.put("brc", ctx.getBrc());
		param.put("trantype", ConstantDeclare.TRANTYPE.KH);
		TblLnsinvesteedetail lnsinvesteedetail;
		try{
			lnsinvesteedetail = DbAccessUtil.queryForObject("AccountingMode.query_lnsinvesteedetail", param, TblLnsinvesteedetail.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		String investeeId = lnsinvesteedetail==null?"":lnsinvesteedetail.getInvesteeid();
		if(lnsinterfaceex==null){
			TblLnsinterfaceex interfaceex;
			param.put("acctno", loanAgreement.getContract().getReceiptNo());
			param.put("brc", ctx.getBrc());
			param.put("key",ConstantDeclare.KEYNAME.DC);//代偿
			try{
				interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", param, TblLnsinterfaceex.class);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}
			//单渠道  事件金额等于 合同金额减去扣息放款金额
			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, loanAgreement.getContract().getReceiptNo(), ConstantDeclare.EVENT.LOANCNLOFF, ConstantDeclare.BRIEFCODE.FDCX,
					new FabAmount(lnsbasicinfo.getContractamt()).sub(lnsbasicinfo.getDeductionamt()).getVal() , "", loanAgreement.getFundInvest(),
					interfaceex!=null?JSONObject.parseObject(interfaceex.getValue()).getString("exinvesteeId"):investeeId,tailAmt.getVal(),contractAmt.getVal() , downPayment.getVal(),"");

		}else{
			//多渠道
			JSONObject  qdInfo= JSONObject.parseObject(lnsinterfaceex.getValue());
			JSONArray jsonArray = qdInfo.getJSONArray("QD");
			FundInvest  fundInvest = new FundInvest();
			fundInvest.setInvestee(loanAgreement.getFundInvest().getInvestee());
			fundInvest.setInvestMode(loanAgreement.getFundInvest().getInvestMode());

			for(int i=0;i<jsonArray.size();i++){
				JSONObject json = jsonArray.getJSONObject(i);
				/*
				code  放款渠道
				no  渠道单号
				amt 渠道金额
				subject 渠道科目
				 */
				fundInvest.setChannelType(json.getString("code"));
				fundInvest.setFundChannel(json.getString("subject"));
				fundInvest.setOutSerialNo(json.getString("no"));
				json.getString("feecompanyID");
				AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, loanAgreement.getContract().getReceiptNo(), ConstantDeclare.EVENT.LOANCNLOFF, ConstantDeclare.BRIEFCODE.FDCX,
						json.getDouble("amt") , "",fundInvest ,investeeId,tailAmt.getVal(),contractAmt.getVal() , downPayment.getVal(),
						VarChecker.isEmpty(json.getString("feecompanyID"))?"":json.getString("feecompanyID"));
			}
		}
	}

	/**
	 * @return the errDate
	 */
	public String getErrDate() {
		return errDate;
	}

	/**
	 * @param errDate the errDate to set
	 */
	public void setErrDate(String errDate) {
		this.errDate = errDate;
	}

	/**
	 * @return the errSerSeq
	 */
	public String getErrSerSeq() {
		return errSerSeq;
	}

	/**
	 * @param errSerSeq the errSerSeq to set
	 */
	public void setErrSerSeq(String errSerSeq) {
		this.errSerSeq = errSerSeq;
	}

	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}

	public Integer getTxseqno() {
		return txseqno;
	}

	public void setTxseqno(Integer txseqno) {
		this.txseqno = txseqno;
	}

	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}
}

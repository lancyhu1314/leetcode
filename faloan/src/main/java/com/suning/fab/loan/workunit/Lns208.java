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

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.AccountingModeChange;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：放款冲销
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Scope("prototype")
@Repository
public class Lns208 extends WorkUnit {
	
	String errDate;			// 错误日期
	String errSerSeq;		// 错误流水号
	
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator suber;
	@Autowired LoanEventOperateProvider eventProvider;

	LoanAgreement loanAgreement;
	TblLnsinterface lnsinterface;
	Integer txseqno = 0;
	private TblLnsbasicinfo lnsbasicinfo;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		/**
		 * 债务公司新放款数据找预收充值事件(主文件flag1等于E),老数据找放款事件
		 * 20190529|14050183
		 */
		if((null != lnsbasicinfo) && (lnsbasicinfo.getFlag1().contains("E")))
			param.put("eventcode", ConstantDeclare.EVENT.RECGDEBTCO);
		else
			param.put("eventcode", ConstantDeclare.EVENT.LOANGRANTA);
		param.put("brc", ctx.getBrc());
		//当有债务公司的时候，可能有多个放款事件，此处需要循环遍历
		List<TblLnseventreg> lnseventregList;
		
		try {
			lnseventregList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnseventreg", param, TblLnseventreg.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}
		
		if (null == lnseventregList || lnseventregList.isEmpty()) {
			
			//账速融非标迁移冲销补充 2019-03-06
			Map<String, Object> param1 = new HashMap<String, Object>();
			param1.put("trandate", errDate);
			param1.put("serseqno", errSerSeq);
			param1.put("eventcode", ConstantDeclare.EVENT.LNDATRANSF);
			param1.put("brc", ctx.getBrc());
			//当有债务公司的时候，可能有多个放款事件，此处需要循环遍历
			
			try {
				lnseventregList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnseventreg", param1, TblLnseventreg.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnseventreg");
			}
			
			if (null == lnseventregList || lnseventregList.isEmpty()) {
				throw new FabException("LNS003");
			}
			
			
//			throw new FabException("LNS003");
		}



		if(loanAgreement  == null)
			loanAgreement= LoanAgreementProvider.genLoanAgreementFromDB(lnseventregList.get(0).getReceiptno(), ctx);
		
		Iterator<TblLnseventreg> iterator = lnseventregList.iterator();
		FabAmount tailAmt = new FabAmount(0.00);
		FabAmount downPayment = new FabAmount(0.00);
		FabAmount totalDebtAmt = new FabAmount(0.00);
		while(iterator.hasNext()) {
			
			TblLnseventreg lnseventreg = iterator.next();
			List<FabAmount> amounts = new ArrayList<>();

			/**
			 * 手机租赁
			 * 20190529|14050183
			 */
			if("51240001".equals(ctx.getBrc())){

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
					//根据字符串截取尾款利率的值
					tailAmt = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("tailAmt")).split(":")[1]));
				}else{
					tailAmt = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("tailAmt"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("tailAmt"))).split(":")[1]));
				}
//				if(lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("downPayment"))==-1){
//					//根据字符串截取首付款的值
//					if(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("downPayment")).split(":").length>1){
//						downPayment = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("downPayment")).split(":")[1]));
//					}
//				}else{
//					if(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("downPayment"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("downPayment"))).split(":").length>1){
//						downPayment = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("downPayment"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("downPayment"))).split(":")[1]));
//					}
//				}
				amounts.add(tailAmt);
				amounts.add(downPayment);
			}
			
			//资金方冲销 2019-02-15
			Map<String,Object> investeeMap = new HashMap<>();
			if(lnsinterface==null) {
				investeeMap.put("trandate", errDate);
				investeeMap.put("serseqno", errSerSeq);
				investeeMap.put("brc", ctx.getBrc());
				try {
					lnsinterface = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsinterface_208", investeeMap, TblLnsinterface.class);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS103", "lnsinterface");
				}

				if (null == lnsinterface) {
					lnsinterface = new TblLnsinterface();
				}
			}
			
			/**
			 * 事件表LOANGRANTA事件备用字段1非空场景:手机租赁开户,迁移,债务公司
			 * 20190529|14050183
			 */
			if(lnseventreg.getReserv1().trim().isEmpty())
			{	
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnseventreg.getReceiptno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
	
				/*减本金户，放款冲销--将账务处理删掉*/
				suber.operate(lnsAcctInfo, null, new FabAmount(lnseventreg.getTranamt()), loanAgreement.getFundInvest(), 
						ConstantDeclare.BRIEFCODE.FKCX, ctx);
				//登记放款冲销事件
				if("51240001".equals(ctx.getBrc())){
					lnsAcctInfo.setCustType(lnseventreg.getCusttype());
					String reserv1 = null;
					if("1".equals(lnseventreg.getCusttype())||"PERSON".equals(lnseventreg.getCusttype())){
						reserv1 = "70215243";
					}else if("2".equals(lnseventreg.getCusttype())||"COMPANY".equals(lnseventreg.getCusttype())){
						reserv1 = lnseventreg.getMerchantno();
					}else{
						reserv1 = "";
					}
					lnsAcctInfo.setMerchantNo(reserv1);
					//CustomName统一放至备用5方便发送事件时转换处理
					//转json存入Tunneldata字段 
					eventProvider.createEvent(ConstantDeclare.EVENT.LOANWRTOFF, new FabAmount(lnseventreg.getTranamt()), 
							lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FKCX, ctx,amounts,"","","","",loanAgreement.getCustomer().getCustomName());
				}else{
					if( !"".equals(lnsinterface.getReserv4()))
						eventProvider.createEvent(ConstantDeclare.EVENT.LOANWRTOFF, new FabAmount(lnseventreg.getTranamt()), 
								lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FKCX, ctx, lnsinterface.getReserv4(), loanAgreement.getBasicExtension().getDebtCompany());
					else
						eventProvider.createEvent(ConstantDeclare.EVENT.LOANWRTOFF, new FabAmount(lnseventreg.getTranamt()), 
								lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FKCX, ctx, loanAgreement.getCustomer().getMerchantNo(), loanAgreement.getBasicExtension().getDebtCompany());
				}
			}
			else
			{
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnseventreg.getReceiptno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
	
				/*减本金户，放款冲销--将账务处理删掉*/
				//suber.operate(lnsAcctInfo, null, new FabAmount(lnseventreg.getTranamt()), loanAgreement.getFundInvest(), 
					//	ConstantDeclare.BRIEFCODE.FKCX, ctx);
				//登记放款冲销事件
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("balance", lnseventreg.getTranamt());
				params.put("brc", ctx.getBrc());
				params.put("accsrccode", "D");
				params.put("acctno", lnseventreg.getReserv2());

				Map<String, Object> newBalance;
				try {
					newBalance = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsprefundaccount_repay", params);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS103", "lnsprefundaccount");
				}
				if (newBalance == null) {
					throw new FabException("SPS104", "lnsprefundaccount");
				}

				Double tabBalance = Double.parseDouble(newBalance.get("balance").toString());
				LoggerUtil.debug("tabBalance:" + tabBalance);
				if (tabBalance.doubleValue() < 0.00)
					throw new FabException("LNS020");
				
				/**
				 * 手机租赁
				 * 20190529|14050183
				 */
				if("51240001".equals(ctx.getBrc())){
					lnsAcctInfo.setCustType(lnseventreg.getCusttype());
					String reserv1 = null;
					if("1".equals(lnseventreg.getCusttype())||"PERSON".equals(lnseventreg.getCusttype())){
						reserv1 = "70215243";
					}else if("2".equals(lnseventreg.getCusttype())||"COMPANY".equals(lnseventreg.getCusttype())){
						reserv1 = lnseventreg.getMerchantno();
					}else{
						reserv1 = "";
					}
					lnsAcctInfo.setMerchantNo(reserv1);
					//CustomName统一放至备用5方便发送事件时转换处理
					//转json存入Tunneldata字段 
					eventProvider.createEvent(ConstantDeclare.EVENT.LOANWRTOFF, new FabAmount(lnseventreg.getTranamt()), 
							lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FKCX, ctx,amounts,"","","","",loanAgreement.getCustomer().getCustomName());
				}else{
					//客户名称 从Reserv3转存到Tunneldata字段上了  需要从Tunneldata获取    兼容未曾转存数据
					if(VarChecker.isEmpty(lnseventreg.getReserv3())){
						Map<String,String> jsonStr = new HashMap<String,String>();
						jsonStr = JSONObject.parseObject(lnseventreg.getTunneldata().trim(),jsonStr.getClass());
						if(null != jsonStr.get("reserv3"))
							lnseventreg.setReserv3(jsonStr.get("reserv3"));
						
						
					}	
					//CustomName统一放至备用5方便发送事件时转换处理
					//转json存入Tunneldata字段 
					/**
					 * 20190610版本修改此处事件
					 * 放款冲销事件LOANWRTOFF改为债务公司充退BACKDEBTCO,摘要代码FKCX改为ZWCT
					 * 再汇总抛一条放款冲销事件
					 * 20190529|14050183
					 */
					if( "" != lnsinterface.getReserv4())
						eventProvider.createEvent(ConstantDeclare.EVENT.BACKDEBTCO, new FabAmount(lnseventreg.getTranamt()), 
								lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.CXCT, ctx, lnsinterface.getReserv4(), lnseventreg.getReserv2(), "","",lnseventreg.getReserv3());
					else
						eventProvider.createEvent(ConstantDeclare.EVENT.BACKDEBTCO, new FabAmount(lnseventreg.getTranamt()), 
								lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.CXCT, ctx, lnseventreg.getReserv1(), lnseventreg.getReserv2(), "","",lnseventreg.getReserv3());
					totalDebtAmt.selfAdd(lnseventreg.getTranamt());
				}
			}



		}
		
		if(totalDebtAmt.isPositive()){
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnseventregList.get(0).getReceiptno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANWRTOFF, totalDebtAmt, 
					lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FKCX, ctx);
		}
		
		//放款冲销  存入 冲销表中  从预收户明细表里 查询债务公司开户明细
		/*
		select * from lnsprefundsch  where trandate = :trandate and serseqno = :serseqno  and  accsrccode = 'D'
		 */
		Map<String,Object> queryParam =  new HashMap<>();
		queryParam.put("trandate", errDate);
		queryParam.put("serseqno", errSerSeq);
		List<TblLnsprefundsch> lnsprefundschs;
		try {
			lnsprefundschs = DbAccessUtil.queryForList("AccountingMode.query_lnsprefundschD", queryParam, TblLnsprefundsch.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsprefundsch");
		}
		/**
		 * 留购价	tailAmt
		 * 首付款	downPayment
		 * 融资额          contractAmt
		 */
		//只有一个放款事件 幂等表里的tranAmt即为合同金额
		//取幂等辅助表的租赁信息
		param.put("acctno", lnsinterface.getAcctno());
		param.put("brc", ctx.getBrc());
		param.put("key", ConstantDeclare.KEYNAME.ZL);
		TblLnsinterfaceex interfaceex;
		try {
			interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", param, TblLnsinterfaceex.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "query_lnsinterfaceex");
		}
		Double contractAmt = 0.00;
		if(null != interfaceex){
			JSONObject jsonObject = JSONObject.parseObject(interfaceex.getValue());
			tailAmt = new FabAmount(jsonObject.getDouble("tailAmt"));
			downPayment = new FabAmount(jsonObject.getDouble("downPayment"));
			contractAmt = jsonObject.getDouble("contractAmt");

		}
		AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, loanAgreement.getContract().getReceiptNo(), ConstantDeclare.EVENT.LOANWRTOFF, ConstantDeclare.BRIEFCODE.FKCX,
				loanAgreement.getContract().getContractAmt().getVal() , "", loanAgreement.getFundInvest(),tailAmt.getVal(),contractAmt , downPayment.getVal());


		if(lnsprefundschs!=null){
			//多个放款事件
			int i =0;
			for(TblLnsprefundsch lnsprefundsch:lnsprefundschs){
				AccountingModeChange.saveLnsprefundsch(ctx,++i,lnsprefundsch.getAcctno() , lnsprefundsch.getCustomid(), "D"
						,lnsprefundsch.getCusttype() ,lnsprefundsch.getName() ,lnsprefundsch.getAmount() ,"sub" );
                AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, loanAgreement.getContract().getReceiptNo(), ConstantDeclare.EVENT.BACKDEBTCO, ConstantDeclare.BRIEFCODE.CXCT,
                        lnsprefundsch.getAmount() , lnsprefundsch.getCustomid() , loanAgreement.getFundInvest(),0.00,0.00,0.00);


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

	public TblLnsinterface getLnsinterface() {
		return lnsinterface;
	}

	public void setLnsinterface(TblLnsinterface lnsinterface) {
		this.lnsinterface = lnsinterface;
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

	/**
	 * @return the lnsbasicinfo
	 */
	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	/**
	 * @param lnsbasicinfo the lnsbasicinfo to set
	 */
	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}
}

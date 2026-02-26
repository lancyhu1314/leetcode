package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 	
 *
 * @version V1.0.1
 *
 *
 * 
 * 
 *
 *
 * @exception
 */

@Scope("prototype")
@Repository
public class Lns240 extends WorkUnit {

	
	String errDate;		// 错误日期
	String errSerSeq;	// 错误流水号
	
	@Autowired 
	LoanEventOperateProvider eventProvider;
	private Integer txseqno = 0;
	private LoanAgreement loanAgreement;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		//判断是否满足冲销条件，如果不满足则直接return
//		checkCondition();
		
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("brc", ctx.getBrc());
		
		TblLnszlamortizeplan lnszlamortizeplan;
		try {//根据核心流水号取出符合条件的记录，摊销计划表中的核心流水号和开户放款的是一致的
			lnszlamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnszlamortizeplan_off", param, 
						TblLnszlamortizeplan.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnszlamortizeplan");
		}
		
		/*如果摊销计划表中无此数据，则意味着该帐号不需要摊销，直接返回，进行下一步*/
		if (null == lnszlamortizeplan) {
			return ;
		}
		
		//更新摊销计划表中的摊销金额， 登记摊销计提登记薄，登记事件
		FabAmount amortizedAmt = new FabAmount(lnszlamortizeplan.getAmortizeamt().doubleValue());	//已摊销金额
		FabAmount taxedAmt = new FabAmount(lnszlamortizeplan.getAmortizetax().doubleValue());		//已摊销税金
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnszlamortizeplan.getAcctno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		if(loanAgreement == null)
			loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(lnszlamortizeplan.getAcctno(), ctx);
		
		if (amortizedAmt.isPositive()) {//如果已摊销金额不为零，则需要处理利息计提摊销登记薄以及事件登记薄，
									//并将摊销登记表中的已摊销金额，已摊销税金，最后摊销日期进行更新
			FabAmount theInt = new FabAmount();
			FabAmount theTax = new FabAmount();
			//存储当天摊销的金额和税金
			if(CalendarUtil.equalDate(lnszlamortizeplan.getLastdate(), ctx.getTranDate())){
				param.put("receiptno", lnszlamortizeplan.getAcctno());
				param.put("intertype", ConstantDeclare.INTERTYPE.AMORTIZE);

				//TblLnsprovisionreg lnsprovisionreg;
				TblLnsprovision lnsprovision;
				try {
//					lnsprovisionreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", param,
//							TblLnsprovisionreg.class);
					lnsprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,lnszlamortizeplan.getAcctno(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.AMORTIZE, loanAgreement);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS103", "lnsprovision");
				}
				if(ConstantDeclare.INTERFLAG.POSITIVE.equals(lnsprovision.getInterflag())){
//					theInt.selfAdd(lnsprovisionreg.getInterest());
//					theTax.selfAdd(lnsprovisionreg.getTax());
					theInt.selfAdd(lnsprovision.getLastinterest());
					theTax.selfAdd(lnsprovision.getLasttax());
				}else{
//					theInt.selfSub(lnsprovisionreg.getInterest());
//					theTax.selfSub(lnsprovisionreg.getTax());
					theInt.selfSub(lnsprovision.getLastinterest());
					theTax.selfSub(lnsprovision.getLasttax());
				}
			}
			//登记利息计提摊销登记薄，登记正反标志为反向TblLnsprovisionreg
//			TblLnsprovisionreg lnsprovisionreg = new TblLnsprovisionreg();
			TblLnsprovisiondtl lnsprovisiondtl =new TblLnsprovisiondtl();
			lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
			lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
			lnsprovisiondtl.setBrc(ctx.getBrc());
//			lnsprovisionreg.setTeller(ctx.getTeller());
//			lnsprovisionreg.setReceiptno(lnszlamortizeplan.getAcctno());
			lnsprovisiondtl.setAcctno(lnszlamortizeplan.getAcctno());
			lnsprovisiondtl.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
			lnsprovisiondtl.setListno(lnszlamortizeplan.getPeriod()+1);
			lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);//设置为利息
			lnsprovisiondtl.setCcy(new FabCurrency().getCcy());
			lnsprovisiondtl.setTotalinterest(amortizedAmt.getVal());	//已摊销总金额
			lnsprovisiondtl.setTotaltax(taxedAmt.getVal()); 			//已摊销总税金
			lnsprovisiondtl.setTaxrate(lnszlamortizeplan.getTaxrate().doubleValue());//税率
			lnsprovisiondtl.setInterest(amortizedAmt.getVal());	//本次摊销金额为已摊销金额
			lnsprovisiondtl.setTax(taxedAmt.getVal());	//本次摊销税金为已摊销税金
			lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.AMORTIZE);//摊销
			lnsprovisiondtl.setBegindate(Date.valueOf(lnszlamortizeplan.getLastdate()));//摊销表中最后摊销日期为本表起始日期
			lnsprovisiondtl.setEnddate(Date.valueOf(ctx.getTranDate()));//本次调用的账务日期为结束日期
			lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.NEGATIVE);//反向
//			lnsprovisionreg.setSendflag(ConstantDeclare.SENDFLAG.PENDIND);
//			lnsprovisionreg.setSendnum(0);
			lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
			lnsprovisiondtl.setReserv1(null);
			lnsprovisiondtl.setReserv2(null);
//			try {
////				DbAccessUtil.execute("Lnsprovisionreg.insert", lnsprovisionreg);
////			} catch (FabSqlException e) {
////				throw new FabException(e, "SPS100", "lnsprovisionreg");
////			}
			//类型转换并保存
			LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
				try {
					DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS100", "Lnsprovisiondtl");
				}
			List<FabAmount> amtList = new ArrayList<>();
			amtList.add(taxedAmt);
			AccountingModeChange.saveWriteoffDetail(ctx, ++txseqno, lnszlamortizeplan.getAcctno(), ConstantDeclare.EVENT.AMORTIZOFF, ConstantDeclare.BRIEFCODE.TXCX,
					amortizedAmt.getVal() , "", loanAgreement.getFundInvest(),taxedAmt.getVal(),theInt.getVal() , theTax.getVal());

			//登记摊销冲销事件， 将已摊销金额及已摊销税金进行反向处理
			if("51240001".equals(ctx.getBrc())){
				Map<String,Object> basicParam = new HashMap<String,Object>();
				basicParam.put("acctno", lnszlamortizeplan.getAcctno());
				basicParam.put("openbrc", ctx.getBrc());

				TblLnsbasicinfo lnsbasicinfo = null;
				try {
					lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", basicParam, TblLnsbasicinfo.class);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS103", "lnsbasicinfo");
				}

				if (null == lnsbasicinfo){
					throw new FabException("SPS104", "lnsbasicinfo");
				}
				lnsAcctInfo.setCustType(lnsbasicinfo.getCusttype());
				String reserv1=null;
				if("1".equals(lnsbasicinfo.getCusttype())||"PERSON".equals(lnsbasicinfo.getCusttype())){
					reserv1 = "70215243";
				}else if("2".equals(lnsbasicinfo.getCusttype())||"COMPANY".equals(lnsbasicinfo.getCusttype())){
					reserv1 = lnsbasicinfo.getCustomid();
				}else{
					reserv1 = "";
				}
				eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZOFF, amortizedAmt, lnsAcctInfo, null, 
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.TXCX, ctx, amtList,reserv1);

			}else{
				eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZOFF, amortizedAmt, lnsAcctInfo, null, 
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.TXCX, ctx, amtList);

			}
		}//如果摊销金额为零，则还未发生摊销，直接将摊销计划表中的status更改为CLOSE
		
		try {//更新摊销表，只要有数据，则都应该更新摊销计划表中的status为CLOSE,有金额产生时，则还应登记事件登记薄
			param.put("lastdate", ctx.getTranDate());
			DbAccessUtil.execute("CUSTOMIZE.update_lnszlamortizeplan_off", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnszlamortizeplan");
		}
		
	}
	
	public void checkCondition() throws FabException {

		TranCtx ctx = getTranctx();
		/**
		 * 1、登记幂等登记薄，根据serialno以及 trancode来判断是否幂等，
		 * 2、如果幂等，则去幂等登记薄中查询customName,acctNo,contractAmt三个字段对应的值，并返回给外围
		 * 3、如果不幂等，判断是否满足冲销条件(无逾期记录，且未还款)，满足则进行冲销，最后删除数据的借据号相关记录之后，
		 * 	     更新幂等登记薄中刚插入的记录的customName,acctNo,contractAmt三个字段的值，否则直接返回
		 **/
		TblLnsinterface lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setTrancode(ctx.getTranCode());
		lnsinterface.setSerialno(ctx.getSerialNo());
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());

		try {
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
		} catch (FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				LoggerUtil.info("交易幂等:serialNo[" + ctx.getSerialNo() + "]" + ", tranCode[" + ctx.getTranCode() + "]");
				throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
			} else {
				throw new FabException(e, "SPS100", "lnsinterface");
			}
		}

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("brc", ctx.getBrc());
		TblLnseventreg lnseventreg;
		try {
			lnseventreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnseventreg", param, TblLnseventreg.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnseventreg");
		}

		if (null == lnseventreg) {
			throw new FabException("LNS003");
		}

//		// 查看还款明细表，查看待冲销用户有没有还款记录，有则直接终止放款冲销交易，
//		param.clear();
//		param.put("acctno", lnseventreg.getReceiptno());
//		param.put("brc", ctx.getBrc());
//		Map<String, Object> retMap;
//		try {
//			retMap = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsrpyinfo_count", param);
//		} catch (FabSqlException e) {
//			throw new FabException(e, "SPS103", "lnsrpyinfo");
//		}
//
//		if (Integer.parseInt(retMap.get("NUM").toString()) > 0) {// 如果还款明细表中有数据，则不能冲销，直接返回，结束
//			throw new FabException("LNS001");
//		}
//
//		// 查看用户有没有出现过逾期的记录，有则直接终止放款冲销交易，查看账单表
//		List<Map<String, Object>> retList;
//		try {
//			retList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_info", param);
//		} catch (FabSqlException e) {
//			throw new FabException(e, "SPS103", "lnsbill");
//		}
//
//		if (null != retList && !retList.isEmpty()) {
//			for (Map<String, Object> map : retList) {
//				if (ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(map.get("billstatus"))) {// 如果出现逾期情况，则直接退出，结束程序
//					throw new FabException("LNS002");
//				}
//			}
//		}

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

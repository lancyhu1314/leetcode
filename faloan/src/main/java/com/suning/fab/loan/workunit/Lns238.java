package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBasicInfo;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;

/**
 * @author 还款/退货收入冲销
 *
 * @version V1.0.1
 *
 * @see 
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns238 extends WorkUnit {
	String serialNo; // 幂等流水号
	String acctNo; // 本金账号
	String repayAcctNo;
	String ccy;
	String cashFlag;
	String termDate;
	String tranCode;
	String bankSubject; // 帐速融 银行直接还款科目
	FabAmount tranAmt;  //交易金额
	FabAmount repayAmt;
	FabAmount cleanPrin; //结清金额
	FabAmount feeAmt;
	FabAmount refundAmt;
	String repayChannel;
	String memo;
	String brc;
	Map<String, FabAmount> repayAmtMap;
	String outSerialNo;
	String prdId;
	String custType;
	String settleFlag;
	String checkFlag;
	
	String		openBrc;
	String		channelType;
	String		fundChannel;
	String		investee;
	String		investMode;

	//是否为逾期还款
	private boolean  ifOverdue = false; //add at 2019-03-25
	LnsBillStatistics lnsBillStatistics;
	FabAmount prinAmt;
	FabAmount nintAmt;
	FabAmount dintAmt;
	String endFlag;
	LoanAgreement loanAgreement;
	FabAmount inAmt = new FabAmount(0.00);
	
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		int repaysize = 0;
		TranCtx ctx = getTranctx();
		LnsBasicInfo basicinfo = new LnsBasicInfo();
		
		//交易金额不等于试算结清金额
		if( "471009".equals(ctx.getTranCode()) &&
			!cleanPrin.sub(tranAmt.getVal()).isZero() &&
			"1".equals(settleFlag) &&
			!"1".equals(checkFlag))
		{
			throw new FabException("LNS058",tranAmt.getVal(),cleanPrin.getVal());
		}
		if( "471012".equals(ctx.getTranCode()) &&
				!cleanPrin.sub(tranAmt.getVal()).isZero() &&
				"1".equals(settleFlag))
			{
				throw new FabException("LNS058",tranAmt.getVal(),cleanPrin.getVal());
			}
		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", getAcctNo());
		param.put("openbrc", ctx.getBrc());
		param.put("brc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}

		
		if( !new FabAmount(lnsbasicinfo.getContractbal()).isZero() )
		{
			TblLnsbill lnsPrinBill = new TblLnsbill();
			//本金账单赋值

			//账务日期
			lnsPrinBill.setTrandate(Date.valueOf(ctx.getTranDate()));			
			//流水号
			lnsPrinBill.setSerseqno(ctx.getSerSeqNo());							
			//子序号
			lnsPrinBill.setTxseq(12);										
			//账号
			lnsPrinBill.setAcctno(getAcctNo());				
			//机构
			lnsPrinBill.setBrc(ctx.getBrc());
			//账单类型（本金）
			lnsPrinBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN); 	
			//账单金额
			lnsPrinBill.setBillamt(lnsbasicinfo.getContractbal()); 					
			//账单余额0
			lnsPrinBill.setBillbal( 0.00 );				
			//上日余额
			//lnsPrinBill.setLastbal(0.00);  									
			//上笔日期
			lnsPrinBill.setLastdate(ctx.getTranDate());						 	
			//账单对应剩余本金金额（合同余额-账单金额）
			lnsPrinBill.setPrinbal(0.00);
			//账单利率（正常利率）
			lnsPrinBill.setBillrate(0.00);  									
			//首次还款账单开始日为开户日
			lnsPrinBill.setBegindate(ctx.getTranDate());
			
			//账单结束日期
			lnsPrinBill.setEnddate(ctx.getTranDate()); 							
			//期数（利息期数）
			lnsPrinBill.setPeriod(1);     							
			//账单应还款日期
			lnsPrinBill.setRepayedate(ctx.getTranDate());    					
			//账单结清日期
			lnsPrinBill.setSettledate(ctx.getTranDate());    					
			//利息记至日期
			lnsPrinBill.setIntedate(ctx.getTranDate());   						
			//当期到期日
			lnsPrinBill.setCurenddate(lnsbasicinfo.getContduedate());
			//还款方式
			lnsPrinBill.setRepayway(lnsbasicinfo.getRepayway());   
			//币种
			lnsPrinBill.setCcy(lnsbasicinfo.getCcy());
			//账单状态（正常）
			lnsPrinBill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
			//账单状态开始日期
			lnsPrinBill.setStatusbdate(ctx.getTranDate());  		 			
			//账单属性(还款)
			lnsPrinBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);									//账单属性(还款)
			//利息入账标志
			lnsPrinBill.setIntrecordflag("YES");   								
			//账单是否作废标志 
			lnsPrinBill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);  	
			//账单结清标志（结清）
			lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);	
			
			//本金账单金额大于0
			if( lnsPrinBill.getBillamt() > 0.00 )
			{
				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsPrinBill);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}
				
				//修改主文件上次结本日
				basicinfo.setLastPrinDate(ctx.getTranDate());
				//修改主文件合同余额
				basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));
			}
			
//			//已还本金（账单金额-账单余额）
//			totalPrin.setBillAmt(new FabAmount(new FabAmount(lnsPrinBill.getBillamt()).sub(new FabAmount(lnsPrinBill.getBillbal())).getVal()));
//			totalPrin.setBillTrandate(ctx.getTranDate());
//			totalPrin.setBillSerseqno(ctx.getSerSeqNo());
//			totalPrin.setBillTxseq(lnsPrinBill.getTxseq());
//			prinAdvance.add(totalPrin);

//			//更新主文件结本结息日、期数
			Map<String, Object> basicInfoMap = new HashMap<String, Object>();
			basicInfoMap.put("tranAmt", lnsbasicinfo.getContractbal());
			
			
			basicInfoMap.put("lastIntDate", basicinfo.getLastIntDate());
			basicInfoMap.put("intTerm", basicinfo.getIntTerm());

			basicInfoMap.put("modifyDate", ctx.getTranDate());
			basicInfoMap.put("acctNo", getAcctNo());
			basicInfoMap.put("brc", ctx.getBrc());
			
			int ct = 0;
			try {
				ct = DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_238", basicInfoMap);
			} catch (FabException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfo");
			}
			if (ct != 1) {
				throw new FabException("LNS041", getAcctNo());
			}
		}
		


//		FundInvest	fundInvest = new FundInvest("", "", repayChannel, bankSubject, outSerialNo);
		
		// 读取贷款账单

		List<TblLnsbill> billList;
		try {
			billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", param, TblLnsbill.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}
		
//		billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		if(billList == null)
			throw new FabException("LNS021");
		
		LoggerUtil.debug("LNSBILL:billList:" + billList.size());
		
		
		Map<String, Object> upbillmap = new HashMap<String, Object>();
		
		for (TblLnsbill lnsbill : billList) {
			LoggerUtil.debug("LNSBILL:" + lnsbill.getAcctno() + "|" + lnsbill.getPeriod() + "|"
					+ lnsbill.getBillstatus() + "." + lnsbill.getBilltype() + "|" + lnsbill.getBillbal());

			
			if( new FabAmount(lnsbill.getBillbal()).isPositive())
			{
				//如果有逾期或者罚息账本 则还款为逾期还款  add at 2019-03-25
				if(!ifOverdue &&lnsbill.isOverdue())
					ifOverdue = true;
				//add end
				inAmt.selfAdd(lnsbill.getBillbal());
				upbillmap.put("acctno", getAcctNo());
				upbillmap.put("brc", ctx.getBrc());
				upbillmap.put("period", lnsbill.getPeriod());
				upbillmap.put("lastdate", ctx.getTranDate());
				upbillmap.put("lastbal", lnsbill.getBillbal());
				
				Map<String, Object> repaymap;
				try {
					DbAccessUtil.execute("CUSTOMIZE.update_rent_bill", upbillmap);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS102", "lnsrentreg");
				}
			}



			LoggerUtil.debug("REMAINAMT:" + getRepayAmt().getVal());

		}
		
		LoggerUtil.debug("LNSBILL:repaysize:" + repaysize);

		endFlag = "3";

		
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		FundInvest openFundInvest = new FundInvest(getInvestee(), getInvestMode(), repayChannel, getFundChannel(), getOutSerialNo());
		
//		if( ctx.getTranCode().equals("471009"))
//		{
			sub.operate(lnsAcctInfo, null, inAmt, openFundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx);
			eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, inAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx,"","","","",lnsbasicinfo.getName());
			if("51240001".equals(ctx.getBrc())){
				String reserv1 = null;
				if("1".equals(lnsbasicinfo.getCusttype())||"PERSON".equals(lnsbasicinfo.getCusttype())){
					reserv1 = "70215243";
				}else if("2".equals(lnsbasicinfo.getCusttype())||"COMPANY".equals(lnsbasicinfo.getCusttype())){
					reserv1 = repayAcctNo;
				}else{
					reserv1 = "";
				}
				eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZEAD, inAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.TXTZ, ctx, reserv1);
			}else{
				eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZEAD, inAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.TXTZ, ctx);
			}
//		}



		// 当期及未来期账单生成及记账 应该拆分 三部分 当期利息 当期本金 未来期本金调三次 然后结清就要根据未来本金余额判断了
//		FabAmount prinAdvancesum = new FabAmount();
//		FabAmount nintAdvancesum = new FabAmount();
//		String prePayFlag = null;
//		
//		getRepayAmt().selfSub(nintAdvancesum);
//		getRepayAmt().selfSub(prinAdvancesum);
//
//		this.repayAmtMap = repayAmtMap;
//		FabAmount prinAmtTmp = new FabAmount();
//		FabAmount nintAmtTmp = new FabAmount();
//		FabAmount dintAmtTmp = new FabAmount();
//		
//		
//		
//		int	i = 0; //插还款明细需要加序号 否则唯一索引冲突
//		for (Map.Entry<String, FabAmount> entry : repayAmtMap.entrySet()) {
//			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
//				prinAmtTmp.selfAdd(entry.getValue());
//			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
//				nintAmtTmp.selfAdd(entry.getValue());
//			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
//					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
//					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
//				dintAmtTmp.selfAdd(entry.getValue());
//			i++; //明细序号自增 适应唯一索引
//			TblLnsrpyinfo lnsrpyinfo = new TblLnsrpyinfo();
//			lnsrpyinfo.setTrandate(ctx.getTranDate());
//			lnsrpyinfo.setBrc(ctx.getBrc());
//			lnsrpyinfo.setProfitbrc(ctx.getBrc());
//			lnsrpyinfo.setAcctno(getAcctNo());
//			lnsrpyinfo.setDiacct(repayAcctNo);
//			lnsrpyinfo.setCcy("01");
//			lnsrpyinfo.setAcctstat(entry.getKey());
//			lnsrpyinfo.setTranamt(repayAmtMap.get(entry.getKey()).getVal());
//			lnsrpyinfo.setFlag("1");
//			lnsrpyinfo.setSeqno(ctx.getSerSeqNo());
//			lnsrpyinfo.setRepayterm(Integer.valueOf(i));
//			try {
//				DbAccessUtil.execute("Lnsrpyinfo.insert", lnsrpyinfo);
//			} catch (FabSqlException e) {
//				throw new FabException(e, "SPS100", "lnsrpyinfo");
//			}
//		}
//
//		prinAmt = prinAmtTmp;
//		nintAmt = nintAmtTmp;
//		dintAmt = dintAmtTmp;
	}

	/**
	 * @return the serialNo
	 */
	public String getSerialNo() {
		return serialNo;
	}

	/**
	 * @param serialNo
	 *            the serialNo to set
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
	 * @param acctNo
	 *            the acctNo to set
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
	 * @param repayAcctNo
	 *            the repayAcctNo to set
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
	 * @param ccy
	 *            the ccy to set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	/**
	 * @return the cashFlag
	 */
	public String getCashFlag() {
		return cashFlag;
	}

	/**
	 * @param cashFlag
	 *            the cashFlag to set
	 */
	public void setCashFlag(String cashFlag) {
		this.cashFlag = cashFlag;
	}

	/**
	 * @return the termDate
	 */
	public String getTermDate() {
		return termDate;
	}

	/**
	 * @param termDate
	 *            the termDate to set
	 */
	public void setTermDate(String termDate) {
		this.termDate = termDate;
	}

	/**
	 * @return the tranCode
	 */
	public String getTranCode() {
		return tranCode;
	}

	/**
	 * @param tranCode
	 *            the tranCode to set
	 */
	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}

	/**
	 * @return the repayAmt
	 */
	public FabAmount getRepayAmt() {
		return repayAmt;
	}

	/**
	 * @param repayAmt
	 *            the repayAmt to set
	 */
	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}

	/**
	 * @return the feeAmt
	 */
	public FabAmount getFeeAmt() {
		return feeAmt;
	}

	/**
	 * @param feeAmt
	 *            the feeAmt to set
	 */
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;
	}

	/**
	 * @return the repayChannel
	 */
	public String getRepayChannel() {
		return repayChannel;
	}

	/**
	 * @param repayChannel
	 *            the repayChannel to set
	 */
	public void setRepayChannel(String repayChannel) {
		this.repayChannel = repayChannel;
	}

	/**
	 * @return the memo
	 */
	public String getMemo() {
		return memo;
	}

	/**
	 * @param memo
	 *            the memo to set
	 */
	public void setMemo(String memo) {
		this.memo = memo;
	}

	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}

	/**
	 * @param brc
	 *            the brc to set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}

	/**
	 * @return the repayAmtMap
	 */
	public Map<String, FabAmount> getRepayAmtMap() {
		return repayAmtMap;
	}

	/**
	 * @param repayAmtMap
	 *            the repayAmtMap to set
	 */
	public void setRepayAmtMap(Map<String, FabAmount> repayAmtMap) {
		this.repayAmtMap = repayAmtMap;
	}

	/**
	 * @return the outSerialNo
	 */
	public String getOutSerialNo() {
		return outSerialNo;
	}

	/**
	 * @param outSerialNo
	 *            the outSerialNo to set
	 */
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}


	public LnsBillStatistics getLnsBillStatistics() {
		return lnsBillStatistics;
	}

	public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
		this.lnsBillStatistics = lnsBillStatistics;
	}

	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}

	/**
	 * @param prinAmt
	 *            the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}

	/**
	 * @return the nintAmt
	 */
	public FabAmount getNintAmt() {
		return nintAmt;
	}

	/**
	 * @param nintAmt
	 *            the nintAmt to set
	 */
	public void setNintAmt(FabAmount nintAmt) {
		this.nintAmt = nintAmt;
	}

	/**
	 * @return the dintAmt
	 */
	public FabAmount getDintAmt() {
		return dintAmt;
	}

	/**
	 * @param dintAmt
	 *            the dintAmt to set
	 */
	public void setDintAmt(FabAmount dintAmt) {
		this.dintAmt = dintAmt;
	}

	/**
	 * @return the endFlag
	 */
	public String getEndFlag() {
		return endFlag;
	}

	/**
	 * @param endFlag
	 *            the endFlag to set
	 */
	public void setEndFlag(String endFlag) {
		this.endFlag = endFlag;
	}

	/**
	 * @return the sub
	 */
	public AccountOperator getSub() {
		return sub;
	}

	/**
	 * @param sub
	 *            the sub to set
	 */
	public void setSub(AccountOperator sub) {
		this.sub = sub;
	}

	/**
	 * @return the eventProvider
	 */
	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}

	/**
	 * @param eventProvider
	 *            the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}

	public String getBankSubject() {
		return bankSubject;
	}

	public void setBankSubject(String bankSubject) {
		this.bankSubject = bankSubject;
	}

	public FabAmount getRefundAmt() {
		return refundAmt;
	}

	public void setRefundAmt(FabAmount refundAmt) {
		this.refundAmt = refundAmt;
	}
	
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
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
	 * @return the cleanPrin
	 */
	public FabAmount getCleanPrin() {
		return cleanPrin;
	}

	/**
	 * @param cleanPrin the cleanPrin to set
	 */
	public void setCleanPrin(FabAmount cleanPrin) {
		this.cleanPrin = cleanPrin;
	}

	/**
	 * @return the settleFlag
	 */
	public String getSettleFlag() {
		return settleFlag;
	}

	/**
	 * @param settleFlag the settleFlag to set
	 */
	public void setSettleFlag(String settleFlag) {
		this.settleFlag = settleFlag;
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

	/**
	 * @return the inAmt
	 */
	public FabAmount getInAmt() {
		return inAmt;
	}

	/**
	 * @param inAmt the inAmt to set
	 */
	public void setInAmt(FabAmount inAmt) {
		this.inAmt = inAmt;
	}

	/**
	 * @return the openBrc
	 */
	public String getOpenBrc() {
		return openBrc;
	}

	/**
	 * @param openBrc the openBrc to set
	 */
	public void setOpenBrc(String openBrc) {
		this.openBrc = openBrc;
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
	 * @return the investee
	 */
	public String getInvestee() {
		return investee;
	}

	/**
	 * @param investee the investee to set
	 */
	public void setInvestee(String investee) {
		this.investee = investee;
	}

	/**
	 * @return the investMode
	 */
	public String getInvestMode() {
		return investMode;
	}

	/**
	 * @param investMode the investMode to set
	 */
	public void setInvestMode(String investMode) {
		this.investMode = investMode;
	}

	/**
	 * @return the checkFlag
	 */
	public String getCheckFlag() {
		return checkFlag;
	}

	/**
	 * @param checkFlag the checkFlag to set
	 */
	public void setCheckFlag(String checkFlag) {
		this.checkFlag = checkFlag;
	}
	/**
	 * @return the ifOverdue
	 */
	public boolean getIfOverdue() {
		return ifOverdue;
	}
	/**
	 * @param ifOverdue the ifOverdue to set
	 */
	public void setIfOverdue(boolean ifOverdue) {
		this.ifOverdue = ifOverdue;
	}
}

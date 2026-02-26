package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.*;

/**
 * @author//暂不支持多资金方
 *
 * @version V1.0.1
 *
 * @see -本金减免
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns258 extends WorkUnit {

	String acctNo; // 本金账号
	String repayAcctNo;
	String ccy;
	String cashFlag;

	FabAmount reducePrinAmt;
	Map<String, FabAmount> reducePrinAmtMap;
	Integer subNo;

	LnsBillStatistics billStatistics;
	FabAmount prinAmt;
	FabAmount nintAmt;
	FabAmount dintAmt;
	String endFlag;
	LoanAgreement loanAgreement;
	String  prinReduceList;  //罚息减免明细
	


	private Integer   txseqno = 0;  //预收明细登记簿  子序号

	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		Map<String, Map<String,FabAmount>> repayAmtList = new HashMap<String, Map<String,FabAmount>>();
	
		int repaysize = 0;
		TranCtx ctx = getTranctx();
		
		Map<String, FabAmount> reducePrinAmtMap = new HashMap<>();
		
		// 根据账号生成账单
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
		


		if (getReducePrinAmt() == null || !getReducePrinAmt().isPositive()) {
			LoggerUtil.info("上送还款金额为零");
			prinAmt = new FabAmount(0.00);
			nintAmt = new FabAmount(0.00);
			dintAmt = new FabAmount(0.00);
			endFlag = "1";
			return;
			// throw new FabException("LNS029");
		}
		LoggerUtil.debug("REPAYAMT1:" + getReducePrinAmt().getVal()); // 本金减免金额

		// 读取主文件判断贷款状态(开户,销户) 判断贷款形态(正常,逾期,呆滞,呆账)取得对应还款顺序
		Map<String, Object> bparam = new HashMap<String, Object>();
		bparam.put("acctno", getAcctNo());
		bparam.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", bparam, TblLnsbasicinfo.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}

		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())) {
			LoggerUtil.debug("该账户已销户或已核销");
			throw new FabException("ACC108", acctNo);
		}

		
		
		
		
		// 读取贷款账单
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", getAcctNo());
		param.put("brc", ctx.getBrc());
		List<TblLnsbill> billList = new ArrayList<TblLnsbill>();
		try {
			billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", param, TblLnsbill.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}

		//筛选账本  非费用账本
		billList = LoanFeeUtils.filtFee(billList);
		// 正常罚息本顺序还款
		billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
	
		LoggerUtil.debug("LNSBILL:billList:" + billList.size());
		Map<String, Object> upbillmap = new HashMap<String, Object>();


		
		for (TblLnsbill lnsbill : billList) {
			LoggerUtil.debug("LNSBILL:" + lnsbill.getAcctno() + "|" + lnsbill.getPeriod() + "|"
					+ lnsbill.getBillstatus() + "." + lnsbill.getBilltype() + "|" + lnsbill.getBillbal());

			//下面指定还本金  如果账单类型不是本金 不处理 继续循环下一条
			if( !ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsbill.getBilltype()))
			{
				repaysize++;continue;
			}



            //add end
			upbillmap.put("actrandate", ctx.getTranDate());
			upbillmap.put("tranamt", getReducePrinAmt().getVal());

			upbillmap.put("trandate", lnsbill.getTrandate());
			upbillmap.put("serseqno", lnsbill.getSerseqno());
			upbillmap.put("txseq", lnsbill.getTxseq());

			Map<String, Object> repaymap;
			try {
				repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_repay", upbillmap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsbill");
			}
			if (repaymap == null) {
				throw new FabException("SPS104", "lnsbill");
			}

			Double minAmt = Double.parseDouble(repaymap.get("minamt").toString());
			LoggerUtil.debug("minAmt:" + minAmt);
			if (minAmt.equals(0.00)) {
				LoggerUtil.debug("该账单金额已经为零");
				repaysize++;
				continue;
			}
			FabAmount amount = new FabAmount(minAmt);
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), lnsbill.getBilltype(), lnsbill.getBillstatus(),
					new FabCurrency());
			lnsAcctInfo.setMerchantNo(getRepayAcctNo());
			lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
			lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
			lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
			lnsAcctInfo.setAccSrcCod(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN+"."+lnsbill.getBillstatus());
			LoanRpyInfoUtil.addRpyList(repayAmtList, lnsbill.getBilltype(), lnsbill.getPeriod().toString(), amount,lnsbill.getRepayway());
			sub.operate(lnsAcctInfo, null, amount, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX,
					lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), ctx);

			// 如果是本金或者利息呆滞呆账状态,需要转回逾期状态,然后还逾期本金利息,能还多少转回多少
		if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
				lnsOpAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
				lnsOpAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
				lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
				eventProvider.createEvent("LOANRETURN", amount, lnsAcctInfo, lnsOpAcctInfo,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.BJZH, ctx);
				eventProvider.createEvent("REDUDEPRIN", amount, lnsOpAcctInfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
						lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(),loanAgreement.getCustomer().getMerchantNo());
			} else{
				eventProvider.createEvent("REDUDEPRIN", amount, lnsAcctInfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
						lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), loanAgreement.getCustomer().getMerchantNo());
			}

			getReducePrinAmt().selfSub(amount);
			LoggerUtil.debug("REMAINAMT:" + getReducePrinAmt().getVal());

			if (reducePrinAmtMap.get(lnsbill.getBilltype() + "." + lnsbill.getBillstatus()) == null) {
				FabAmount amt = new FabAmount(amount.getVal());
				reducePrinAmtMap.put(lnsbill.getBilltype() + "." + lnsbill.getBillstatus(), amt);
			} else {
				reducePrinAmtMap.get(lnsbill.getBilltype() + "." + lnsbill.getBillstatus()).selfAdd(amount);
			}

			if (!getReducePrinAmt().isPositive()) {
				Double repayBall = Double.parseDouble(repaymap.get("billbal").toString());
				if (repayBall.equals(minAmt)) // 余额等于发生额 该条账单处理结束 加1
					repaysize++;
				break;
			}
			repaysize++;
		}
		
		
		
		
		
		LoggerUtil.debug("LNSBILL:repaysize:" + repaysize);

		endFlag = "1";
		//普通的如果当日大于等于合同结束日期,账单结清,贷款销户
		//胡祎的如果无合同余额,账单结清,贷款销户
		
		//所有的账单金额是零 上面的循环是可以统计的 合同余额也是零 那就OK了 基本可以判断结清 再想想

		
		
		LoggerUtil.debug("[" + getAcctNo() + "]" + "主流程账单还款:" + "|" + getReducePrinAmt().getVal());
		// 当期及未来期账单生成及记账 应该拆分 三部分 当期利息 当期本金 未来期本金调三次 然后结清就要根据未来本金余额判断了
		FabAmount prinAdvancesum = new FabAmount();
		String prePayFlag = null;
		if (!ConstantDeclare.ENDFLAG.ALLCLR.equals(endFlag) && getReducePrinAmt().isPositive() 
				&& Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate()))) {
			List<RepayAdvance> prinAdvancelist = new ArrayList<RepayAdvance>();
			List<RepayAdvance> nintAdvancelist = new ArrayList<RepayAdvance>();

				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYPRIN;
			
			if (ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()))
				endFlag = LoanInterestSettlementUtil.specialOrderDbdxInstBill(loanAgreement, ctx, subNo, lnsbasicinfo,
						new FabAmount(getReducePrinAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,
						prePayFlag);
			else
				endFlag = LoanInterestSettlementUtil.specialOrderInstBill(loanAgreement, ctx, subNo, lnsbasicinfo,
						new FabAmount(getReducePrinAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,
						prePayFlag);

			LoggerUtil.debug("BREAKWHILE:" + prinAdvancelist.size() + "|" + nintAdvancelist.size());

			if (!prinAdvancelist.isEmpty()) {
				for (RepayAdvance prinAdvance : prinAdvancelist) {
					LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), "PRIN", "N", new FabCurrency());
					nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
					nlnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
					nlnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
					nlnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
					nlnsAcctInfo.setAccSrcCod(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN+"."+prinAdvance.getBillStatus());
					LoanRpyInfoUtil.addRpyList(repayAmtList, "PRIN", prinAdvance.getRepayterm().toString(), prinAdvance.getBillAmt(),"");
					sub.operate(nlnsAcctInfo, null, prinAdvance.getBillAmt(), loanAgreement.getFundInvest(),
							ConstantDeclare.BRIEFCODE.HKBX, prinAdvance.getBillTrandate(),
							prinAdvance.getBillSerseqno(), prinAdvance.getBillTxseq(), ctx);
					eventProvider.createEvent("REDUDEPRIN", prinAdvance.getBillAmt(),
								nlnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx,
								null, prinAdvance.getBillTrandate(), prinAdvance.getBillSerseqno(),
								prinAdvance.getBillTxseq(), loanAgreement.getCustomer().getMerchantNo());
					prinAdvancesum.selfAdd(prinAdvance.getBillAmt());
				}
				if (null == reducePrinAmtMap.get("PRIN.N")) {
					reducePrinAmtMap.put("PRIN.N", prinAdvancesum);
				} else {
					reducePrinAmtMap.get("PRIN.N").selfAdd(prinAdvancesum);
				}
			}
		}

		this.reducePrinAmtMap = reducePrinAmtMap;
		FabAmount prinAmtTmp = new FabAmount();
		FabAmount nintAmtTmp = new FabAmount();
		FabAmount dintAmtTmp = new FabAmount();
		int	i = 0; //插还款明细需要加序号 否则唯一索引冲突
		for (Map.Entry<String, FabAmount> entry : reducePrinAmtMap.entrySet()) {
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
				prinAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
				nintAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
				dintAmtTmp.selfAdd(entry.getValue());
			/*i++; //明细序号自增 适应唯一索引
			TblLnsrpyinfo lnsrpyinfo = new TblLnsrpyinfo();
			lnsrpyinfo.setTrandate(ctx.getTranDate());
			lnsrpyinfo.setBrc(ctx.getBrc());
			lnsrpyinfo.setProfitbrc(ctx.getBrc());
			lnsrpyinfo.setAcctno(getAcctNo());
			lnsrpyinfo.setDiacct(repayAcctNo);
			lnsrpyinfo.setCcy("01");
			lnsrpyinfo.setAcctstat(entry.getKey());
			lnsrpyinfo.setTranamt(reducePrinAmtMap.get(entry.getKey()).getVal());
			lnsrpyinfo.setFlag("1");
			lnsrpyinfo.setSeqno(ctx.getSerSeqNo());
			lnsrpyinfo.setRepayterm(Integer.valueOf(i));
			try {
				DbAccessUtil.execute("Lnsrpyinfo.insert", lnsrpyinfo);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "lnsrpyinfo");
			}*/
		}
		prinReduceList = LoanRpyInfoUtil.getRepayList(acctNo, tranctx, repayAmtList,loanAgreement);
		prinAmt = prinAmtTmp;
		nintAmt = nintAmtTmp;
		dintAmt = dintAmtTmp;
		
		
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
	 * @return the reducePrinAmt
	 */
	public FabAmount getReducePrinAmt() {
		return reducePrinAmt;
	}

	/**
	 * @param reducePrinAmt
	 *            the reducePrinAmt to set
	 */
	public void setReducePrinAmt(FabAmount reducePrinAmt) {
		this.reducePrinAmt = reducePrinAmt;
	}



	/**
	 * @return the reducePrinAmt
	 */
	public Map<String, FabAmount> getReducePrinAmtMap() {
		return reducePrinAmtMap;
	}

	/**
	 * @param reducePrinAmtMap
	 *            the reducePrinAmtMap to set
	 */
	public void setRepayAmtMap(Map<String, FabAmount> reducePrinAmtMap) {
		this.reducePrinAmtMap = reducePrinAmtMap;
	}

	/**
	 * @return the subNo
	 */
	public Integer getSubNo() {
		return subNo;
	}

	/**
	 * @param subNo
	 *            the subNo to set
	 */
	public void setSubNo(Integer subNo) {
		this.subNo = subNo;
	}


	/**
	 * @return the billStatistics
	 */
	public LnsBillStatistics getBillStatistics() {
		return billStatistics;
	}

	/**
	 * @param billStatistics
	 *            the billStatistics to set
	 */
	public void setBillStatistics(LnsBillStatistics billStatistics) {
		this.billStatistics = billStatistics;
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
	 * @return the loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	/**
	 * @param loanAgreement
	 *            the loanAgreement to set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
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





	
	public String getPrinReduceList() {
		return prinReduceList;
	}


	public void setPrinReduceList(String prinReduceList) {
		this.prinReduceList = prinReduceList;
	}


	/**
	 * @return txseqno
	 */
	public Integer getTxseqno() {
		return txseqno;
	}
	/**
	 * @param txseqno the txseqno to set
	 */
	public void setTxseqno(Integer txseqno) {
		this.txseqno = txseqno;
	}
}

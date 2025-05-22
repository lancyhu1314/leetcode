package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsrpyinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanInterestSettlementUtil;
import com.suning.fab.loan.utils.LoanRepayOrderHelper;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 非标13-2-2还款本息
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns231 extends WorkUnit {
	String serialNo; // 幂等流水号
	String acctNo; // 本金账号
	String repayAcctNo;
	String ccy;
	String cashFlag;
	String termDate;
	String tranCode;
	String bankSubject; // 帐速融 银行直接还款科目
	FabAmount repayAmt;
	FabAmount feeAmt;
	FabAmount refundAmt;
	String repayChannel;
	String memo;
	String brc;
	Map<String, FabAmount> repayAmtMap;
	Integer subNo;
	String outSerialNo;

	LnsBillStatistics billStatistics;
	FabAmount prinAmt;
	FabAmount nintAmt;
	FabAmount dintAmt;
	String endFlag;
	LoanAgreement loanAgreement;
	String prdId;
	String custType;
	//是否为逾期还款
	private boolean  ifOverdue = false; //add at 2019-03-25
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		int pkgflag = 0;
		int listsize;
		int repaysize = 0;
		TranCtx ctx = getTranctx();
		ListMap pkgList = ctx.getRequestDict("pkgList");
		Map<String, FabAmount> repayAmtMap = new HashMap<String, FabAmount>();

		// 幂等登记薄
		
		// 根据账号生成账单
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
		if(outSerialNo != null)
			loanAgreement.getFundInvest().setOutSerialNo(getOutSerialNo());
		if(getBankSubject() != null)
			loanAgreement.getFundInvest().setFundChannel(getBankSubject());
		if(getRepayChannel() != null)
			loanAgreement.getFundInvest().setChannelType(getRepayChannel());
		if (getRepayAmt() == null || !getRepayAmt().isPositive()) {
			LoggerUtil.info("上送还款金额为零");
			prinAmt = new FabAmount(0.00);
			nintAmt = new FabAmount(0.00);
			dintAmt = new FabAmount(0.00);
			endFlag = "1";
			return;
			// throw new FabException("LNS029");
		}
		LoggerUtil.debug("REPAYAMT1:" + getRepayAmt().getVal()); // 顾客上送还款金额

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

		// 带债务公司的还款
		if (pkgList != null && pkgList.size() > 0)
			pkgflag = 1;

		setPrdId(lnsbasicinfo.getPrdcode());
		setCustType(lnsbasicinfo.getCusttype());
		
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

		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbasicinfo.getLoanstat())
				|| ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbasicinfo.getLoanstat())) {
			billList = LoanRepayOrderHelper.dullBad(billList);
		} else {// 正常罚息本顺序还款
			billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		}
		listsize = billList.size();
		LoggerUtil.debug("LNSBILL:billList:" + billList.size());
		Map<String, Object> upbillmap = new HashMap<String, Object>();

		for (TblLnsbill lnsbill : billList) {
			LoggerUtil.debug("LNSBILL:" + lnsbill.getAcctno() + "|" + lnsbill.getPeriod() + "|"
					+ lnsbill.getBillstatus() + "." + lnsbill.getBilltype() + "|" + lnsbill.getBillbal());
			//如果有逾期或者罚息账本 则还款为逾期还款  add at 2019-03-25
			if(!ifOverdue &&lnsbill.isOverdue())
				ifOverdue = true;
			//add end
			upbillmap.put("actrandate", ctx.getTranDate());
			upbillmap.put("tranamt", getRepayAmt().getVal());

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
			// lnsAcctInfo.setBillType(lnsbill.getBilltype())
			// lnsAcctInfo.setLoanForm(lnsbill.getBillstatus())

			sub.operate(lnsAcctInfo, null, amount, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX,
					lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), ctx);

			// 如果是本金或者利息呆滞呆账状态,需要转回逾期状态,然后还逾期本金利息,能还多少转回多少
			if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_NINT, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
				lnsOpAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
				lnsOpAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
				lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
				eventProvider.createEvent(ConstantDeclare.EVENT.NINTRETURN, amount, lnsAcctInfo, lnsOpAcctInfo,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.LXZH, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
						lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq());
			} else if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) && pkgflag != 1) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
				lnsOpAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
				lnsOpAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
				lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
				eventProvider.createEvent("LOANRETURN", amount, lnsAcctInfo, lnsOpAcctInfo,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.BJZH, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
						lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq());
			} else if (pkgflag != 1 || !ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsbill.getBilltype())) {
				// 如果是无追保理,并且是本金户表外不抛事件,其他均抛事件
				eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsAcctInfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
						lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq());
			} else if(pkgflag == 1 && ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsbill.getBilltype()) &&
					(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus())))
			{
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
				lnsOpAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
				lnsOpAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
				lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
				eventProvider.createEvent("LOANRETURN", amount, lnsAcctInfo, lnsOpAcctInfo,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.BJZH, ctx);
			}	
				

			getRepayAmt().selfSub(amount);
			LoggerUtil.debug("REMAINAMT:" + getRepayAmt().getVal());

			if (repayAmtMap.get(lnsbill.getBilltype() + "." + lnsbill.getBillstatus()) == null) {
				FabAmount amt = new FabAmount(amount.getVal());
				repayAmtMap.put(lnsbill.getBilltype() + "." + lnsbill.getBillstatus(), amt);
			} else {
				repayAmtMap.get(lnsbill.getBilltype() + "." + lnsbill.getBillstatus()).selfAdd(amount);
			}

			if (!getRepayAmt().isPositive()) {
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
		if ( ( !Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate())) 
				|| new FabAmount(lnsbasicinfo.getContractbal()).isZero() )
				&& listsize == repaysize) {
			endFlag = "3";
			LoggerUtil.info("AcctNo:{}结清",lnsbasicinfo.getAcctno());
			
			Map<String, Object> upbasicmap = new HashMap<String, Object>();
			upbasicmap.put("openbrc", lnsbasicinfo.getOpenbrc());
			upbasicmap.put("acctno", lnsbasicinfo.getAcctno());
			upbasicmap.put("loanstat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
			upbasicmap.put("modifyDate", ctx.getTranDate());		
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstat", upbasicmap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfo");
			}
		}

		LoggerUtil.debug("[" + getAcctNo() + "]" + "主流程账单还款:" + "|" + getRepayAmt().getVal());
		// 当期及未来期账单生成及记账 应该拆分 三部分 当期利息 当期本金 未来期本金调三次 然后结清就要根据未来本金余额判断了
		FabAmount prinAdvancesum = new FabAmount();
		FabAmount nintAdvancesum = new FabAmount();
		String prePayFlag = null;
		if (!"3".equals(endFlag) && getRepayAmt().isPositive()
				&& Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate()))) {
			List<RepayAdvance> prinAdvancelist = new ArrayList<RepayAdvance>();
			List<RepayAdvance> nintAdvancelist = new ArrayList<RepayAdvance>();
			if (ConstantDeclare.ISCALINT.ISCALINT_NO.equals(lnsbasicinfo.getIscalint()))
				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN;
			else if (ConstantDeclare.ISCALINT.ISCALINT_YES.equals(lnsbasicinfo.getIscalint()))
				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_INTPRIN;

			endFlag = LoanInterestSettlementUtil.interestRepaymentBill(loanAgreement, ctx, subNo, lnsbasicinfo,
						 getRepayAmt() , prinAdvancelist, nintAdvancelist, billStatistics,
						prePayFlag,"1","");

			LoggerUtil.debug("BREAKWHILE:" + prinAdvancelist.size() + "|" + nintAdvancelist.size());

			if (!prinAdvancelist.isEmpty()) {
				for (RepayAdvance prinAdvance : prinAdvancelist) {
					LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), "PRIN", "N", new FabCurrency());
					nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
					nlnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
					nlnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
					nlnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
					sub.operate(nlnsAcctInfo, null, prinAdvance.getBillAmt(), loanAgreement.getFundInvest(),
							ConstantDeclare.BRIEFCODE.HKBX, prinAdvance.getBillTrandate(),
							prinAdvance.getBillSerseqno(), prinAdvance.getBillTxseq(), ctx);
					if (pkgflag != 1)
						eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, prinAdvance.getBillAmt(),
								nlnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx,
								null, prinAdvance.getBillTrandate(), prinAdvance.getBillSerseqno(),
								prinAdvance.getBillTxseq());
					prinAdvancesum.selfAdd(prinAdvance.getBillAmt());
				}
				if (null == repayAmtMap.get("PRIN.N")) {
					repayAmtMap.put("PRIN.N", prinAdvancesum);
				} else {
					repayAmtMap.get("PRIN.N").selfAdd(prinAdvancesum);
				}
			}
			if (!nintAdvancelist.isEmpty()) {
				for (RepayAdvance nintAdvance : nintAdvancelist) {
					LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), "NINT", "N", new FabCurrency());
					nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
					nlnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
					nlnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
					nlnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
					sub.operate(nlnsAcctInfo, null, nintAdvance.getBillAmt(), loanAgreement.getFundInvest(),
							ConstantDeclare.BRIEFCODE.HKBX, nintAdvance.getBillTrandate(),
							nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(), ctx);
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, nintAdvance.getBillAmt(), nlnsAcctInfo,
							null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							nintAdvance.getBillTrandate(), nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq());
					nintAdvancesum.selfAdd(nintAdvance.getBillAmt());
				}
				if (null == repayAmtMap.get("NINT.N")) {
					repayAmtMap.put("NINT.N", nintAdvancesum);
				} else {
					repayAmtMap.get("NINT.N").selfAdd(nintAdvancesum);
				}
			}
		}

		getRepayAmt().selfSub(nintAdvancesum);
		getRepayAmt().selfSub(prinAdvancesum);
		
		this.repayAmtMap = repayAmtMap;
		FabAmount prinAmtTmp = new FabAmount();
		FabAmount nintAmtTmp = new FabAmount();
		FabAmount dintAmtTmp = new FabAmount();
		int	i = 0; //插还款明细需要加序号 否则唯一索引冲突
		for (Map.Entry<String, FabAmount> entry : repayAmtMap.entrySet()) {
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
				prinAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
				nintAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
				dintAmtTmp.selfAdd(entry.getValue());
			i++; //明细序号自增 适应唯一索引
			TblLnsrpyinfo lnsrpyinfo = new TblLnsrpyinfo();
			lnsrpyinfo.setTrandate(ctx.getTranDate());
			lnsrpyinfo.setBrc(ctx.getBrc());
			lnsrpyinfo.setProfitbrc(ctx.getBrc());
			lnsrpyinfo.setAcctno(getAcctNo());
			lnsrpyinfo.setDiacct(repayAcctNo);
			lnsrpyinfo.setCcy("01");
			lnsrpyinfo.setAcctstat(entry.getKey());
			lnsrpyinfo.setTranamt(repayAmtMap.get(entry.getKey()).getVal());
			lnsrpyinfo.setFlag("1");
			lnsrpyinfo.setSeqno(ctx.getSerSeqNo());
			lnsrpyinfo.setRepayterm(Integer.valueOf(i));
			try {
				DbAccessUtil.execute("Lnsrpyinfo.insert", lnsrpyinfo);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "lnsrpyinfo");
			}
		}

		prinAmt = prinAmtTmp;
		nintAmt = nintAmtTmp;
		dintAmt = dintAmtTmp;

		
		// 幂等登记薄


	}


	/**
	 * 获取 serialNo
	 *
	 * @return serialNo the serialNo get
	 */
	public String getSerialNo() {
		return this.serialNo;
	}

	/**
	 * 设置 serialNo
	 *
	 * @param serialNo the serialNo set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	/**
	 * 获取 acctNo
	 *
	 * @return acctNo the acctNo get
	 */
	public String getAcctNo() {
		return this.acctNo;
	}

	/**
	 * 设置 acctNo
	 *
	 * @param acctNo the acctNo set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	/**
	 * 获取 repayAcctNo
	 *
	 * @return repayAcctNo the repayAcctNo get
	 */
	public String getRepayAcctNo() {
		return this.repayAcctNo;
	}

	/**
	 * 设置 repayAcctNo
	 *
	 * @param repayAcctNo the repayAcctNo set
	 */
	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
	}

	/**
	 * 获取 ccy
	 *
	 * @return ccy the ccy get
	 */
	public String getCcy() {
		return this.ccy;
	}

	/**
	 * 设置 ccy
	 *
	 * @param ccy the ccy set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	/**
	 * 获取 cashFlag
	 *
	 * @return cashFlag the cashFlag get
	 */
	public String getCashFlag() {
		return this.cashFlag;
	}

	/**
	 * 设置 cashFlag
	 *
	 * @param cashFlag the cashFlag set
	 */
	public void setCashFlag(String cashFlag) {
		this.cashFlag = cashFlag;
	}

	/**
	 * 获取 termDate
	 *
	 * @return termDate the termDate get
	 */
	public String getTermDate() {
		return this.termDate;
	}

	/**
	 * 设置 termDate
	 *
	 * @param termDate the termDate set
	 */
	public void setTermDate(String termDate) {
		this.termDate = termDate;
	}

	/**
	 * 获取 tranCode
	 *
	 * @return tranCode the tranCode get
	 */
	public String getTranCode() {
		return this.tranCode;
	}

	/**
	 * 设置 tranCode
	 *
	 * @param tranCode the tranCode set
	 */
	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}

	/**
	 * 获取 bankSubject
	 *
	 * @return bankSubject the bankSubject get
	 */
	public String getBankSubject() {
		return this.bankSubject;
	}

	/**
	 * 设置 bankSubject
	 *
	 * @param bankSubject the bankSubject set
	 */
	public void setBankSubject(String bankSubject) {
		this.bankSubject = bankSubject;
	}

	/**
	 * 获取 repayAmt
	 *
	 * @return repayAmt the repayAmt get
	 */
	public FabAmount getRepayAmt() {
		return this.repayAmt;
	}

	/**
	 * 设置 repayAmt
	 *
	 * @param repayAmt the repayAmt set
	 */
	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}

	/**
	 * 获取 feeAmt
	 *
	 * @return feeAmt the feeAmt get
	 */
	public FabAmount getFeeAmt() {
		return this.feeAmt;
	}

	/**
	 * 设置 feeAmt
	 *
	 * @param feeAmt the feeAmt set
	 */
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;
	}

	/**
	 * 获取 refundAmt
	 *
	 * @return refundAmt the refundAmt get
	 */
	public FabAmount getRefundAmt() {
		return this.refundAmt;
	}

	/**
	 * 设置 refundAmt
	 *
	 * @param refundAmt the refundAmt set
	 */
	public void setRefundAmt(FabAmount refundAmt) {
		this.refundAmt = refundAmt;
	}

	/**
	 * 获取 repayChannel
	 *
	 * @return repayChannel the repayChannel get
	 */
	public String getRepayChannel() {
		return this.repayChannel;
	}

	/**
	 * 设置 repayChannel
	 *
	 * @param repayChannel the repayChannel set
	 */
	public void setRepayChannel(String repayChannel) {
		this.repayChannel = repayChannel;
	}

	/**
	 * 获取 memo
	 *
	 * @return memo the memo get
	 */
	public String getMemo() {
		return this.memo;
	}

	/**
	 * 设置 memo
	 *
	 * @param memo the memo set
	 */
	public void setMemo(String memo) {
		this.memo = memo;
	}

	/**
	 * 获取 brc
	 *
	 * @return brc the brc get
	 */
	public String getBrc() {
		return this.brc;
	}

	/**
	 * 设置 brc
	 *
	 * @param brc the brc set
	 */
	public void setBrc(String brc) {
		this.brc = brc;
	}

	/**
	 * 获取 repayAmtMap
	 *
	 * @return repayAmtMap the repayAmtMap get
	 */
	public Map<String, FabAmount> getRepayAmtMap() {
		return this.repayAmtMap;
	}

	/**
	 * 设置 repayAmtMap
	 *
	 * @param repayAmtMap the repayAmtMap set
	 */
	public void setRepayAmtMap(Map<String, FabAmount> repayAmtMap) {
		this.repayAmtMap = repayAmtMap;
	}

	/**
	 * 获取 subNo
	 *
	 * @return subNo the subNo get
	 */
	public Integer getSubNo() {
		return this.subNo;
	}

	/**
	 * 设置 subNo
	 *
	 * @param subNo the subNo set
	 */
	public void setSubNo(Integer subNo) {
		this.subNo = subNo;
	}

	/**
	 * 获取 outSerialNo
	 *
	 * @return outSerialNo the outSerialNo get
	 */
	public String getOutSerialNo() {
		return this.outSerialNo;
	}

	/**
	 * 设置 outSerialNo
	 *
	 * @param outSerialNo the outSerialNo set
	 */
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}

	/**
	 * 获取 billStatistics
	 *
	 * @return billStatistics the billStatistics get
	 */
	public LnsBillStatistics getBillStatistics() {
		return this.billStatistics;
	}

	/**
	 * 设置 billStatistics
	 *
	 * @param billStatistics the billStatistics set
	 */
	public void setBillStatistics(LnsBillStatistics billStatistics) {
		this.billStatistics = billStatistics;
	}

	/**
	 * 获取 prinAmt
	 *
	 * @return prinAmt the prinAmt get
	 */
	public FabAmount getPrinAmt() {
		return this.prinAmt;
	}

	/**
	 * 设置 prinAmt
	 *
	 * @param prinAmt the prinAmt set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}

	/**
	 * 获取 nintAmt
	 *
	 * @return nintAmt the nintAmt get
	 */
	public FabAmount getNintAmt() {
		return this.nintAmt;
	}

	/**
	 * 设置 nintAmt
	 *
	 * @param nintAmt the nintAmt set
	 */
	public void setNintAmt(FabAmount nintAmt) {
		this.nintAmt = nintAmt;
	}

	/**
	 * 获取 dintAmt
	 *
	 * @return dintAmt the dintAmt get
	 */
	public FabAmount getDintAmt() {
		return this.dintAmt;
	}

	/**
	 * 设置 dintAmt
	 *
	 * @param dintAmt the dintAmt set
	 */
	public void setDintAmt(FabAmount dintAmt) {
		this.dintAmt = dintAmt;
	}

	/**
	 * 获取 endFlag
	 *
	 * @return endFlag the endFlag get
	 */
	public String getEndFlag() {
		return this.endFlag;
	}

	/**
	 * 设置 endFlag
	 *
	 * @param endFlag the endFlag set
	 */
	public void setEndFlag(String endFlag) {
		this.endFlag = endFlag;
	}

	/**
	 * 获取 loanAgreement
	 *
	 * @return loanAgreement the loanAgreement get
	 */
	public LoanAgreement getLoanAgreement() {
		return this.loanAgreement;
	}

	/**
	 * 设置 loanAgreement
	 *
	 * @param loanAgreement the loanAgreement set
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;
	}

	/**
	 * 获取 prdId
	 *
	 * @return prdId the prdId get
	 */
	public String getPrdId() {
		return this.prdId;
	}

	/**
	 * 设置 prdId
	 *
	 * @param prdId the prdId set
	 */
	public void setPrdId(String prdId) {
		this.prdId = prdId;
	}

	/**
	 * 获取 custType
	 *
	 * @return custType the custType get
	 */
	public String getCustType() {
		return this.custType;
	}

	/**
	 * 设置 custType
	 *
	 * @param custType the custType set
	 */
	public void setCustType(String custType) {
		this.custType = custType;
	}

	/**
	 * 获取 ifOverdue 是否为逾期还款
	 *
	 * @return ifOverdue the ifOverdue get
	 */
	public boolean getIfOverdue() {
		return this.ifOverdue;
	}

	/**
	 * 设置 ifOverdue 是否为逾期还款
	 *
	 * @param ifOverdue the ifOverdue set
	 */
	public void setIfOverdue(boolean ifOverdue) {
		this.ifOverdue = ifOverdue;
	}

	/**
	 * 获取 sub @Autowired@Qualifier("accountSuber")
	 *
	 * @return sub the sub get
	 */
	public AccountOperator getSub() {
		return this.sub;
	}

	/**
	 * 设置 sub @Autowired@Qualifier("accountSuber")
	 *
	 * @param sub the sub set
	 */
	public void setSub(AccountOperator sub) {
		this.sub = sub;
	}

	/**
	 * 获取 eventProvider @Autowired
	 *
	 * @return eventProvider the eventProvider get
	 */
	public LoanEventOperateProvider getEventProvider() {
		return this.eventProvider;
	}

	/**
	 * 设置 eventProvider @Autowired
	 *
	 * @param eventProvider the eventProvider set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}

}

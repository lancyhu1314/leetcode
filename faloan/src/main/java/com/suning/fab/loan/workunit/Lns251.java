package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.domain.TblLnsrpyinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 18049705
 *
 * @version V1.0.0
 *
 * @see -P2P费用还款
 *
 *
 */
@Scope("prototype")
@Repository
public class Lns251 extends WorkUnit {
	String serialNo; // 幂等流水号
	String acctNo; // 本金账号
	String repayAcctNo;
	String ccy;
	String cashFlag;
	String termDate;
	String tranCode;
	String bankSubject; // 帐速融 银行直接还款科目
	FabAmount repayAmt; // 还款金额
	String settleFlag;
	String currFlag;  //当笔结清标志
	String repayChannel;

	String brc;
	Map<String, FabAmount> repayAmtMap;
	Integer subNo;
	String outSerialNo;
	TblLnsbasicinfo lnsbasicinfo;
	LnsBillStatistics billStatistics;
	FabAmount prinAmt; // 已还本金
	FabAmount nintAmt; // 已还利息
	FabAmount dintAmt; // 已还罚息
	FabAmount feeAmt; // 已还费用
	FabAmount penaltyAmt = new FabAmount();// 违约金
	String endFlag;
	LoanAgreement loanAgreement;
	String priorType;
	String prdId;
	String custType;
	
    FabAmount reduceIntAmt;				//利息减免金额
    FabAmount reduceFintAmt;			//罚息减免金额
    
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	LoanEventOperateProvider eventProvider;
	//是否为逾期还款
	private boolean  ifOverdue = false; //add at 2019-03-25
	@Override
	public void run() throws Exception {
		int pkgflag = 0;
		int listsize;
		int repaysize = 0;
		TranCtx ctx = getTranctx();
		ListMap pkgList = ctx.getRequestDict("pkgList");
		Map<String, FabAmount> repayAmtMap = new HashMap<>();
		if(penaltyAmt.isPositive())
			repayAmtMap.put("PNLA.N", penaltyAmt);
		//记录日志的还款金额
		FabAmount repayTotal = new FabAmount(getRepayAmt().add(penaltyAmt).getVal());
		// 幂等登记薄
		TblLnsinterface lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setSerialno(getSerialNo());
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode(ctx.getTranCode());
		if (!VarChecker.isEmpty(getRepayAcctNo())) // 帐速融 银行直接还款
			lnsinterface.setUserno(getRepayAcctNo());
		lnsinterface.setAcctno(getAcctNo());
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setBankno(getOutSerialNo());
		lnsinterface.setTranamt(0.00); // 用还款总金额TODO
		lnsinterface.setSumrint(0.00);
		lnsinterface.setSumramt(0.00);
		lnsinterface.setSumrfint(0.00);
		//科目号存在billno
		if(!VarChecker.isEmpty(bankSubject))
			lnsinterface.setBillno(bankSubject);
		//利息罚息减免登记 2019-03-18
		if(!VarChecker.isEmpty(reduceIntAmt)){
			lnsinterface.setSumdelint(reduceIntAmt.getVal());
		}
		if(!VarChecker.isEmpty(reduceFintAmt)){
			lnsinterface.setSumdelfint(reduceFintAmt.getVal());
		}
		
		if (!VarChecker.isEmpty(repayChannel))
			lnsinterface.setReserv5(repayChannel);

		try {
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
		} catch (FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				Map<String, Object> params = new HashMap<>();
				params.put("serialno", getSerialNo());
				params.put("trancode", getTranCode());

				try {
					lnsinterface = DbAccessUtil.queryForObject(
							"Lnsinterface.selectByUk", params,
							TblLnsinterface.class);
				} catch (FabSqlException f) {
					throw new FabException(f, "SPS103", "lnsinterface");
				}
				dintAmt = new FabAmount(lnsinterface.getSumrfint());
				nintAmt = new FabAmount(lnsinterface.getSumrint());
				prinAmt = new FabAmount(lnsinterface.getSumramt());
				feeAmt = (FabAmount) new FabAmount(lnsinterface.getTranamt())
						.sub(prinAmt).sub(dintAmt).sub(nintAmt);
				if(!VarChecker.isEmpty(lnsinterface.getReserv4())){
					penaltyAmt = new FabAmount(Double.valueOf(lnsinterface.getReserv4()));
				}
				

				endFlag = lnsinterface.acctFlag2Endflag();

				throw new FabException(e, TRAN.IDEMPOTENCY);
			} else
				throw new FabException(e, "SPS100", "lnsinterface");
		}

		
		
		// 根据账号生成账单
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(
				getAcctNo(), ctx);
		if (outSerialNo != null)
			loanAgreement.getFundInvest().setOutSerialNo(getOutSerialNo());
		if (getBankSubject() != null)
			loanAgreement.getFundInvest().setFundChannel(getBankSubject());
		if (getRepayChannel() != null)
			loanAgreement.getFundInvest().setChannelType(getRepayChannel());
		if (getRepayAmt() == null || !getRepayAmt().isPositive()) {
			LoggerUtil.info("上送还款金额为零");
			prinAmt = new FabAmount(0.00);
			nintAmt = new FabAmount(0.00);
			dintAmt = new FabAmount(0.00);
			feeAmt = new FabAmount(0.00);
			endFlag = "1";
			return;
			// throw new FabException("LNS029");
		}
		LoggerUtil.debug("REPAYAMT1:" + getRepayAmt().getVal()); // 顾客上送还款金额

		// 读取主文件判断贷款状态(开户,销户) 判断贷款形态(正常,逾期,呆滞,呆账)取得对应还款顺序
		Map<String, Object> bparam = new HashMap<>();
		bparam.put("acctno", getAcctNo());
		bparam.put("openbrc", ctx.getBrc());

		try {
			lnsbasicinfo = DbAccessUtil.queryForObject(
					"Lnsbasicinfo.selectByUk", bparam, TblLnsbasicinfo.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}

		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo
				.getLoanstat())) {
			LoggerUtil.debug("该账户已销户或已核销");
			throw new FabException("ACC108", acctNo);
		}

		// 带债务公司的还款
		if (pkgList != null && pkgList.size() > 0)
			pkgflag = 1;

		setPrdId(lnsbasicinfo.getPrdcode());
		setCustType(lnsbasicinfo.getCusttype());

		// 读取贷款账单
		Map<String, Object> param = new HashMap<>();
		param.put("acctno", getAcctNo());
		param.put("brc", ctx.getBrc());
		List<TblLnsbill> billList ;
		try {
			billList = DbAccessUtil.queryForList(
					"CUSTOMIZE.query_lnsbill_repay", param, TblLnsbill.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}

		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT
				.equals(lnsbasicinfo.getLoanstat())
				|| ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS
						.equals(lnsbasicinfo.getLoanstat())) {
			billList = LoanRepayOrderHelper.dullBad(billList);
		} else {// 正常罚息本顺序还款
			billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		}

		listsize = billList.size();
		FabAmount currAmt = new FabAmount();
		LoggerUtil.debug("LNSBILL:billList:" + billList.size());
		Map<String, Object> upbillmap = new HashMap<>();
		
		//日志记录当期总需还款金额
		for (TblLnsbill lnsbill : billList) 
		{
			currAmt.selfAdd(lnsbill.getBillbal());
		}
		for (TblLnsbill lnsbill : billList) {
			LoggerUtil.debug("LNSBILL:" + lnsbill.getAcctno() + "|"
					+ lnsbill.getPeriod() + "|" + lnsbill.getBillstatus() + "."
					+ lnsbill.getBilltype() + "|" + lnsbill.getBillbal());
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
				repaymap = DbAccessUtil.queryForMap(
						"CUSTOMIZE.update_lnsbill_repay", upbillmap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsbill");
			}
			if (repaymap == null) {
				throw new FabException("SPS104", "lnsbill");
			}

 			Double minAmt = Double.parseDouble(repaymap.get("minamt")
					.toString());
			LoggerUtil.debug("minAmt:" + minAmt);
			if (minAmt.equals(0.00)) {
				LoggerUtil.debug("该账单金额已经为零");
				repaysize++;
				continue;
			}
			FabAmount amount = new FabAmount(minAmt);
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),
					lnsbill.getBilltype(), lnsbill.getBillstatus(),
					new FabCurrency());
			lnsAcctInfo.setMerchantNo(getRepayAcctNo());
			lnsAcctInfo
					.setCustType(loanAgreement.getCustomer().getCustomType());
			lnsAcctInfo
					.setCustName(loanAgreement.getCustomer().getCustomName());
			lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
			// lnsAcctInfo.setBillType(lnsbill.getBilltype())
			// lnsAcctInfo.setLoanForm(lnsbill.getBillstatus())

			sub.operate(lnsAcctInfo, null, amount, loanAgreement
					.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, lnsbill
					.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill
					.getTxseq(), ctx);

		
			// 如果是本金或者利息呆滞呆账状态,需要转回逾期状态,然后还逾期本金利息,能还多少转回多少
			if ((VarChecker.asList(
					ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS)
					.contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(
							ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(),
						ConstantDeclare.BILLTYPE.BILLTYPE_NINT,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,
						new FabCurrency());
				lnsOpAcctInfo.setCustType(loanAgreement.getCustomer()
						.getCustomType());
				lnsOpAcctInfo.setCustName(loanAgreement.getCustomer()
						.getCustomName());
				lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
				eventProvider.createEvent(ConstantDeclare.EVENT.NINTRETURN,
						amount, lnsAcctInfo, lnsOpAcctInfo,
						loanAgreement.getFundInvest(),
						ConstantDeclare.BRIEFCODE.LXZH, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT,
						amount, lnsOpAcctInfo, null, loanAgreement
								.getFundInvest(),
						ConstantDeclare.BRIEFCODE.HKBX, ctx, null, lnsbill
								.getTrandate().toString(), lnsbill
								.getSerseqno(), lnsbill.getTxseq());
			} else if ((VarChecker.asList(
					ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS)
					.contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(
							ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
					&& pkgflag != 1) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(),
						ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,
						new FabCurrency());
				lnsOpAcctInfo.setCustType(loanAgreement.getCustomer()
						.getCustomType());
				lnsOpAcctInfo.setCustName(loanAgreement.getCustomer()
						.getCustomName());
				lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
				eventProvider.createEvent("LOANRETURN", amount, lnsAcctInfo,
						lnsOpAcctInfo, loanAgreement.getFundInvest(),
						ConstantDeclare.BRIEFCODE.BJZH, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT,
						amount, lnsOpAcctInfo, null, loanAgreement
								.getFundInvest(),
						ConstantDeclare.BRIEFCODE.HKBX, ctx, null, lnsbill
								.getTrandate().toString(), lnsbill
								.getSerseqno(), lnsbill.getTxseq());
			} else if (pkgflag != 1
					|| !ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsbill
							.getBilltype())) {
				// 如果是无追保理,并且是本金户表外不抛事件,其他均抛事件
				eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT,
						amount, lnsAcctInfo, null, loanAgreement
								.getFundInvest(),
						ConstantDeclare.BRIEFCODE.HKBX, ctx, null, lnsbill
								.getTrandate().toString(), lnsbill
								.getSerseqno(), lnsbill.getTxseq());
			} 
			else if ( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsbill
							.getBilltype())
					&& (VarChecker.asList(
							ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS)
							.contains(lnsbill.getBillstatus()))) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(),
						ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,
						new FabCurrency());
				lnsOpAcctInfo.setCustType(loanAgreement.getCustomer()
						.getCustomType());
				lnsOpAcctInfo.setCustName(loanAgreement.getCustomer()
						.getCustomName());
				lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
				eventProvider.createEvent("LOANRETURN", amount, lnsAcctInfo,
						lnsOpAcctInfo, loanAgreement.getFundInvest(),
						ConstantDeclare.BRIEFCODE.BJZH, ctx);
			}

			getRepayAmt().selfSub(amount);
			LoggerUtil.debug("REMAINAMT:" + getRepayAmt().getVal());

			if (repayAmtMap.get(lnsbill.getBilltype() + "."
					+ lnsbill.getBillstatus()) == null) {
				FabAmount amt = new FabAmount(amount.getVal());
				repayAmtMap.put(
						lnsbill.getBilltype() + "." + lnsbill.getBillstatus(),
						amt);
			} else {
				repayAmtMap.get(
						lnsbill.getBilltype() + "." + lnsbill.getBillstatus())
						.selfAdd(amount);
			}

			if (!getRepayAmt().isPositive()) {
				Double repayBall = Double.parseDouble(repaymap.get("billbal")
						.toString());
				if (repayBall.equals(minAmt)) // 余额等于发生额 该条账单处理结束 加1
					repaysize++;
				break;
			}
			repaysize++;
		}
		LoggerUtil.debug("LNSBILL:repaysize:" + repaysize);
		
		//不允许部分还款 提示传的金额 和 账单金额
		if(repaysize!= billList.size()){
			throw new FabException("LNS074",repayTotal,currAmt.add(penaltyAmt).getVal());
		}
				
		
		endFlag = "1";
		// 普通的如果当日大于等于合同结束日期,账单结清,贷款销户
		// 胡祎的如果无合同余额,账单结清,贷款销户
		if ((!Date.valueOf(ctx.getTranDate()).before(
				Date.valueOf(lnsbasicinfo.getContduedate())) || new FabAmount(
				lnsbasicinfo.getContractbal()).isZero())
				&& listsize == repaysize) {
			endFlag = "3";
			LoggerUtil.info("AcctNo:{}结清", lnsbasicinfo.getAcctno());

			Map<String, Object> upbasicmap = new HashMap<>();
			upbasicmap.put("openbrc", lnsbasicinfo.getOpenbrc());
			upbasicmap.put("acctno", lnsbasicinfo.getAcctno());
			upbasicmap.put("loanstat",
					ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
			upbasicmap.put("modifyDate", ctx.getTranDate());
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstat",
						upbasicmap);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfo");
			}
		}

		LoggerUtil.debug("[" + getAcctNo() + "]" + "主流程账单还款:" + "|"
				+ getRepayAmt().getVal());
		
		String prePayFlag = null;
		FabAmount prinAdvancesum = new FabAmount();
		FabAmount feeAdvancesum = new FabAmount();
		FabAmount nintAdvancesum = new FabAmount();
		// 当期及未来期账单生成及记账 应该拆分 三部分 当期利息 当期本金 未来期本金调三次 然后结清就要根据未来本金余额判断了
		if (!"3".equals(endFlag)&& getRepayAmt().isPositive()
				&& Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate()))) {
			//随借随还  因为只有一期 吗，没有违约金 所以不能提前还款
			if("8".equals(lnsbasicinfo.getRepayway())){
				throw new FabException("LNS076");
			}
			if(!"1".equals(settleFlag)){
				
				//整笔结清标志不结清时，判断当笔结清标志
				//不允许提前还款  上传金额  和 应还金额   0   
				//if(currFlag.equals("1"))//当笔结清
				
				throw new FabException("LNS075",repayTotal,currAmt.add(penaltyAmt).getVal());
				
			}
			
			List<RepayAdvance> prinAdvancelist = new ArrayList<>();
			List<RepayAdvance> nintAdvancelist = new ArrayList<>();
			List<RepayAdvance> afeeAdvancelist = new ArrayList<>();

			
			if (ConstantDeclare.ISCALINT.ISCALINT_NO.equals(lnsbasicinfo.getIscalint()))
				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN;
			else if (ConstantDeclare.ISCALINT.ISCALINT_YES.equals(lnsbasicinfo.getIscalint()))
				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_INTPRIN;
			endFlag = LoanInterestSettlementUtil.feeRepaymentBill(loanAgreement, ctx, subNo, lnsbasicinfo,
					new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, afeeAdvancelist, billStatistics,
					prePayFlag);
			if(!"3".equals(endFlag)){
				throw new FabException("LNS073");
			}
			LoggerUtil.debug("BREAKWHILE:" + prinAdvancelist.size() + "|" + nintAdvancelist.size());

			if(!afeeAdvancelist.isEmpty())
			{
				for (RepayAdvance feeAdvance : afeeAdvancelist) {
					LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.BILLTYPE.BILLTYPE_AFEE, "N", new FabCurrency());
					nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
					nlnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
					nlnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
					nlnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
					sub.operate(nlnsAcctInfo, null, feeAdvance.getBillAmt(), loanAgreement.getFundInvest(),
							ConstantDeclare.BRIEFCODE.HKBX, feeAdvance.getBillTrandate(),
							feeAdvance.getBillSerseqno(), feeAdvance.getBillTxseq(), ctx);
					if (pkgflag != 1)
						eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, feeAdvance.getBillAmt(),
								nlnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx,
								null, feeAdvance.getBillTrandate(), feeAdvance.getBillSerseqno(),
								feeAdvance.getBillTxseq());
					feeAdvancesum.selfAdd(feeAdvance.getBillAmt());
				}
				if (null == repayAmtMap.get("AFEE.N")) {
					repayAmtMap.put("AFEE.N", feeAdvancesum);
				} else {
					repayAmtMap.get("AFEE.N").selfAdd(feeAdvancesum);
				}
			}
			
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
		if("1".equals(settleFlag)&&!"3".equals(endFlag)){
			//提前结清时 
			throw new FabException("LNS073");
		}
		this.repayAmtMap = repayAmtMap;
		FabAmount prinAmtTmp = new FabAmount();
		FabAmount nintAmtTmp = new FabAmount();
		FabAmount dintAmtTmp = new FabAmount();
		FabAmount feeAmtTmp = new FabAmount();
		int i = 0; // 插还款明细需要加序号 否则唯一索引冲突
		for (Map.Entry<String, FabAmount> entry : repayAmtMap.entrySet()) {
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
				prinAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
				nintAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
					|| entry.getKey().contains(
							ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
					|| entry.getKey().contains(
							ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
				dintAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE))
				feeAmtTmp.selfAdd(entry.getValue());
			i++; // 明细序号自增 适应唯一索引
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
			lnsrpyinfo.setRepayterm(i);
			try {
				DbAccessUtil.execute("Lnsrpyinfo.insert", lnsrpyinfo);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "lnsrpyinfo");
			}
		}

		prinAmt = prinAmtTmp;
		nintAmt = nintAmtTmp;
		dintAmt = dintAmtTmp;
		feeAmt = feeAmtTmp;

		// 幂等登记薄

		Map<String, Object> lns211map = new HashMap<>();

		lns211map.put("sumrfint", dintAmt.getVal());
		lns211map.put("sumrint", nintAmt.getVal());
		lns211map.put("sumramt", prinAmt.getVal());
		lns211map.put("feeamt", feeAmt.getVal());
		/*
		 * 费用暂存 备用字段3
		 */
		lns211map.put("reserv3", String.valueOf(feeAmt.getVal()));
		lns211map.put("acctflag", TblLnsinterface.endflag2AcctFlag2(endFlag, ifOverdue));
		lns211map.put("serialno", getSerialNo());
		lns211map.put("trancode", ctx.getTranCode());

		try {
			DbAccessUtil
					.execute("CUSTOMIZE.update_lnsinterface_251", lns211map);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsinterface");
		}

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
	 * 
	 * @return the penaltyAmt
	 */
	public FabAmount getPenaltyAmt() {
		return penaltyAmt;
	}

	/**
	 * @param penaltyAmt the penaltyAmt to set
	 */
	public void setPenaltyAmt(FabAmount penaltyAmt) {
		this.penaltyAmt = penaltyAmt;
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

	public String getPriorType() {
		return priorType;
	}

	public void setPriorType(String priorType) {
		this.priorType = priorType;
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
	 * 
	 * @return the lnsbasicinfo
	 */
	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	/**
	 * @param lnsbasicinfo
	 *            the lnsbasicinfo to set
	 */
	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}

	/**
	 * 
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
	 * 
	 * @return the currFlag
	 */
	public String getCurrFlag() {
		return currFlag;
	}

	/**
	 * @param currFlag the currFlag to set
	 */
	public void setCurrFlag(String currFlag) {
		this.currFlag = currFlag;
	}

	/**
	 * @return the reduceIntAmt
	 */
	public FabAmount getReduceIntAmt() {
		return reduceIntAmt;
	}

	/**
	 * @param reduceIntAmt the reduceIntAmt to set
	 */
	public void setReduceIntAmt(FabAmount reduceIntAmt) {
		this.reduceIntAmt = reduceIntAmt;
	}

	/**
	 * @return the reduceFintAmt
	 */
	public FabAmount getReduceFintAmt() {
		return reduceFintAmt;
	}

	/**
	 * @param reduceFintAmt the reduceFintAmt to set
	 */
	public void setReduceFintAmt(FabAmount reduceFintAmt) {
		this.reduceFintAmt = reduceFintAmt;
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

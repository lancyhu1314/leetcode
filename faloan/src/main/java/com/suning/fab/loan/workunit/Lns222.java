package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
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
 * @see -还款本息-指定利息-罚息
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns222 extends WorkUnit {
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
	String	priorType;

    FabAmount reduceIntAmt;				//利息减免金额
    FabAmount reduceFintAmt;			//罚息减免金额
	//是否为逾期还款
	private boolean  ifOverdue = false; //add at 2019-03-25
	private Integer   txseqno = 0;  //预收明细登记簿  子序号

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
		Map<String, FabAmount> repayAmtMap = new HashMap<>();
		FabAmount sumAmtGDCint = new FabAmount(); //罚息账单余额汇总
		FabAmount sumAmtNint = new FabAmount(); //利息账单余额汇总
		FabAmount sumAmtPrin = new FabAmount(); //本金账单余额汇总
		FabAmount sumAmtDintNint = new FabAmount(); //本金账单余额汇总
		FabAmount sumAmt = new FabAmount(); //本金利息罚息中两种账单余额汇总
		FabAmount sumPkgAmt = new FabAmount(); //债务公司累计金额
		
		
		
		// 带债务公司的还款
		if (pkgList != null && pkgList.size() > 0)
			pkgflag = 1;
		
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
		//存入幂等登记簿reserv1。
		if (!VarChecker.isEmpty(priorType))
			lnsinterface.setReserv1(priorType);

		
		//资金方信息存入reserv6  2019-02-15
		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
		StringBuffer reserv6 = new StringBuffer();
		if( null != pkgList1  && pkgList1.size() > 0)
		{
			//暂不支持多资金方
//			if(pkgList1.size() > 1)
//			{
//				throw new FabException("LNS133");
//			}
			// 2021-06-21 还款支持多资金方
			
			for(PubDict pkg:pkgList1.getLoopmsg()){
				if(VarChecker.isEmpty(PubDict.getRequestDict(pkg, "investeeId"))){
					throw new FabException("LNS055","资金方");
				}
				
				reserv6.append(PubDict.getRequestDict(pkg, "investeeId") + "|");
			}
			
			reserv6.deleteCharAt(reserv6.length() - 1);
			lnsinterface.setReserv6(reserv6.toString());
		}
		// 根据账号生成账单
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
		//幂等reserv6 登记债务公司代码
		if (pkgflag == 1 && "3010016".equals(loanAgreement.getPrdId())) {
			for (PubDict pkg : pkgList.getLoopmsg()) {
				lnsinterface.setReserv6( PubDict.getRequestDict(pkg, "debtCompany"));
			}
		}

		try {
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
		} catch (FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("serialno", getSerialNo());
				params.put("trancode", ctx.getTranCode());

				try {
					lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", params,
							TblLnsinterface.class);
				} catch (FabSqlException f) {
					throw new FabException(f, "SPS103", "lnsinterface");
				}
				dintAmt = new FabAmount(lnsinterface.getSumrfint());
				nintAmt = new FabAmount(lnsinterface.getSumrint());
				prinAmt = new FabAmount(lnsinterface.getSumramt());
				endFlag = lnsinterface.acctFlag2Endflag();

				throw new FabException(e, TRAN.IDEMPOTENCY);
			} else
				throw new FabException(e, "SPS100", "lnsinterface");
		}

		if(priorType == null)
		{
			throw new FabException("LNS045", "指定还款类型非法");
		}

		if(!ConstantDeclare.PRIORTYPE.GDCINT.equals(priorType) && 
		   !ConstantDeclare.PRIORTYPE.NINT.equals(priorType) &&
		   !ConstantDeclare.PRIORTYPE.PRIN.equals(priorType) &&
		   !ConstantDeclare.PRIORTYPE.INTDINT.equals(priorType) ) 
		
		{
			throw new FabException("LNS045", "指定还款类型非法");
		}	
		

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

		
		if(pkgList == null || pkgList.size() == 0)
		{
			//无追保理和买方付息产品的债务公司必输
			if(  ("3".equals(priorType) && Arrays.asList("3010006","3010013","3010014","3010015").contains(lnsbasicinfo.getPrdcode())) )
			{
				throw new FabException("LNS055","债务公司");
			}
			
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
		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbasicinfo.getLoanstat())
				|| ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbasicinfo.getLoanstat())) {
			billList = LoanRepayOrderHelper.dullBad(billList);
		} else {
			if( ConstantDeclare.PRIORTYPE.INTDINT.equals(priorType)  )
				billList = LoanRepayOrderHelper.bigPrinBigInt(billList);
			else
				// 正常罚息本顺序还款
				billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		}
		listsize = billList.size();
		LoggerUtil.debug("LNSBILL:billList:" + billList.size());
		Map<String, Object> upbillmap = new HashMap<String, Object>();


		
		for (TblLnsbill lnsbill : billList) {
			LoggerUtil.debug("LNSBILL:" + lnsbill.getAcctno() + "|" + lnsbill.getPeriod() + "|"
					+ lnsbill.getBillstatus() + "." + lnsbill.getBilltype() + "|" + lnsbill.getBillbal());

			
			//下面指定还罚息 如果账单类型不是宽限期利息 罚息 复利 不处理 继续循环下一条
			if( (!ConstantDeclare.PRIORTYPE.GDCINT.equals(priorType)    &&
				!ConstantDeclare.PRIORTYPE.INTDINT.equals(priorType) )   &&
				(ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(lnsbill.getBilltype()) || 
				 ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(lnsbill.getBilltype()) || 
				 ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnsbill.getBilltype())) )
			{
				sumAmtGDCint.selfAdd(new FabAmount(lnsbill.getBillbal()));
				repaysize++;continue;
			}
			//下面指定还利息 如果账单类型不是利息 不处理 继续循环下一条
			if( (!ConstantDeclare.PRIORTYPE.NINT.equals(priorType) &&
				!ConstantDeclare.PRIORTYPE.INTDINT.equals(priorType) ) && 
				ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsbill.getBilltype()))
			{

				sumAmtNint.selfAdd(new FabAmount(lnsbill.getBillbal()));
				repaysize++;continue;
			}	
			//下面指定还本金  如果账单类型不是本金 不处理 继续循环下一条
			if( !ConstantDeclare.PRIORTYPE.PRIN.equals(priorType) &&
				ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsbill.getBilltype()))
			{
				sumAmtPrin.selfAdd(new FabAmount(lnsbill.getBillbal()));
				repaysize++;continue;
			}


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
			lnsAcctInfo.setCancelFlag(lnsbill.getCancelflag());
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
				lnsOpAcctInfo.setCancelFlag(lnsbill.getCancelflag());
				eventProvider.createEvent(ConstantDeclare.EVENT.NINTRETURN, amount, lnsAcctInfo, lnsOpAcctInfo,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.LXZH, ctx, loanAgreement.getCustomer().getMerchantNo(), loanAgreement.getBasicExtension().getDebtCompany());
				if( VarChecker.asList("3010013","3010015").contains( lnsbasicinfo.getPrdcode()) )
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
							loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), loanAgreement.getCustomer().getMerchantNo(),loanAgreement.getBasicExtension().getDebtCompany());
				else
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
							loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), loanAgreement.getCustomer().getMerchantNo());
			} else if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
				lnsOpAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
				lnsOpAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
				lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
				lnsOpAcctInfo.setCancelFlag(lnsbill.getCancelflag());
				eventProvider.createEvent("LOANRETURN", amount, lnsAcctInfo, lnsOpAcctInfo,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.BJZH, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
						loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
						lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(),loanAgreement.getCustomer().getMerchantNo());
			} else{
				// 如果是无追保理,并且是本金户表外不抛事件,其他均抛事件
				if( VarChecker.asList("3010013","3010015").contains( lnsbasicinfo.getPrdcode()) &&
					VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT,
									  ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
									  ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
									  ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(lnsbill.getBilltype()))
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsAcctInfo, null,
							loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), loanAgreement.getCustomer().getMerchantNo(),loanAgreement.getBasicExtension().getDebtCompany());
				else
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsAcctInfo, null,
							loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), loanAgreement.getCustomer().getMerchantNo());
			
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
		
		//所有的账单金额是零 上面的循环是可以统计的 合同余额也是零 那就OK了 基本可以判断结清 再想想
		sumAmt.selfAdd(sumAmtPrin).selfAdd(sumAmtNint).selfAdd(sumAmtGDCint).selfAdd(sumAmtDintNint);
		if (sumAmt.equals(new FabAmount(0.00)) && lnsbasicinfo.getContractbal().equals(0.00) && listsize == repaysize) {
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
		if (!ConstantDeclare.ENDFLAG.ALLCLR.equals(endFlag) && getRepayAmt().isPositive() && !ConstantDeclare.PRIORTYPE.GDCINT.equals(priorType)
				&& Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate()))) {
			List<RepayAdvance> prinAdvancelist = new ArrayList<RepayAdvance>();
			List<RepayAdvance> nintAdvancelist = new ArrayList<RepayAdvance>();

			if(ConstantDeclare.PRIORTYPE.NINT.equals(priorType) || ConstantDeclare.PRIORTYPE.INTDINT.equals(priorType))
				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYINT;
			if(ConstantDeclare.PRIORTYPE.PRIN.equals(priorType))
				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYPRIN;
			
			if (ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()))
				endFlag = LoanInterestSettlementUtil.specialOrderDbdxInstBill(loanAgreement, ctx, subNo, lnsbasicinfo,
						new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,
						prePayFlag);
			else
				endFlag = LoanInterestSettlementUtil.specialOrderInstBill(loanAgreement, ctx, subNo, lnsbasicinfo,
						new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,
						prePayFlag);

			LoggerUtil.debug("BREAKWHILE:" + prinAdvancelist.size() + "|" + nintAdvancelist.size());

			if (!prinAdvancelist.isEmpty()) {
				for (RepayAdvance prinAdvance : prinAdvancelist) {
					LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), "PRIN", "N", new FabCurrency());
					nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
					nlnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
					nlnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
					nlnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
					nlnsAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat()) ? "3" : "1");
					sub.operate(nlnsAcctInfo, null, prinAdvance.getBillAmt(), loanAgreement.getFundInvest(),
							ConstantDeclare.BRIEFCODE.HKBX, prinAdvance.getBillTrandate(),
							prinAdvance.getBillSerseqno(), prinAdvance.getBillTxseq(), ctx);
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, prinAdvance.getBillAmt(),
								nlnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx,
								null, prinAdvance.getBillTrandate(), prinAdvance.getBillSerseqno(),
								prinAdvance.getBillTxseq(), loanAgreement.getCustomer().getMerchantNo());
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
					nlnsAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat()) ? "3" : "1");
					sub.operate(nlnsAcctInfo, null, nintAdvance.getBillAmt(), loanAgreement.getFundInvest(),
							ConstantDeclare.BRIEFCODE.HKBX, nintAdvance.getBillTrandate(),
							nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(), ctx);
					if( VarChecker.asList("3010013","3010015").contains( lnsbasicinfo.getPrdcode()) )
						eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, nintAdvance.getBillAmt(), nlnsAcctInfo,
								null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
								nintAdvance.getBillTrandate(), nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(), loanAgreement.getCustomer().getMerchantNo(),loanAgreement.getBasicExtension().getDebtCompany());
					else
						eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, nintAdvance.getBillAmt(), nlnsAcctInfo,
								null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
								nintAdvance.getBillTrandate(), nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(), loanAgreement.getCustomer().getMerchantNo());
					nintAdvancesum.selfAdd(nintAdvance.getBillAmt());
				}
				if (null == repayAmtMap.get("NINT.N")) {
					repayAmtMap.put("NINT.N", nintAdvancesum);
				} else {
					repayAmtMap.get("NINT.N").selfAdd(nintAdvancesum);
				}
			}
		}

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
		
		//         * 产品代码为3010016 不校验债务公司 2019-09-17
		if (pkgflag == 1 && !"3010016".equals( lnsbasicinfo.getPrdcode() ) ) {
			//无追保理还款债务公司要等于放款债务公司
			JSONObject  myJson = JSONObject.parseObject(loanAgreement.getBasicExtension().getJsonStr());
			Map m = myJson; 
			for (PubDict pkg : pkgList.getLoopmsg()) {
				String debtCompany = PubDict.getRequestDict(pkg, "debtCompany");
				FabAmount debtAmt = PubDict.getRequestDict(pkg, "debtAmt");
				

				if(Arrays.asList("3010006","3010014").contains(lnsbasicinfo.getPrdcode()))
				{
					//无追保理还款债务公司要等于放款债务公司
					if( null != m && !m.containsKey(debtCompany) )
					{
						throw new FabException("LNS123");
					}
                    //多债务公司的 校验金额 add at 2019-03-07
                    if(null!=myJson && !myJson.isEmpty() && myJson.size()>1){
                        if(new FabAmount(myJson.getDouble(debtCompany)).sub(debtAmt).isNegative())
                            throw  new FabException("LNS151",debtCompany,myJson.getDouble(debtCompany));
                        myJson.put(debtCompany,new FabAmount(myJson.getDouble(debtCompany)).sub(debtAmt).getVal());
                    }
                    //add end
				}
				
				if( Arrays.asList("3010013","3010015").contains(lnsbasicinfo.getPrdcode()) )
				{
					if( !loanAgreement.getBasicExtension().getDebtCompany().equals(debtCompany))
					{
						throw new FabException("LNS123");
					}
				}

				Map<String, Object> params = new HashMap<String, Object>();
				params.put("balance", debtAmt.getVal());
				params.put("brc", ctx.getBrc());
				params.put("accsrccode", "D");
				params.put("acctno", debtCompany);

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

				txseqno++;

				// 查询预收户信息
				TblLnsassistdyninfo preaccountInfo = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), getRepayAcctNo(), "", ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,ctx);
				if (preaccountInfo == null) {
					throw new FabException("SPS104", "lnsprefundaccount");
				}


//				Map<String, Object> midparams = new HashMap<String, Object>();
//				midparams.put("brc", ctx.getBrc());
//				midparams.put("accsrccode", "N");
//				midparams.put("acctno", getRepayAcctNo());
//
//				Map<String, Object> customid;
//				try {
//					customid = DbAccessUtil.queryForMap("CUSTOMIZE.query_customid", midparams);
//				} catch (FabSqlException e) {
//					throw new FabException(e, "SPS103", "lnsprefundaccount");
//				}
//				if (customid == null) {
//					throw new FabException("SPS104", "lnsprefundaccount");
//				}
//
//				String	customidstr = customid.get("customid").toString();
				//债务公司还款
				AccountingModeChange.saveLnsprefundsch(ctx, txseqno, debtCompany, debtCompany, "D","COMPANY" ,
						debtCompany ,debtAmt.getVal() ,"sub" );
//				Map<String, Object> reparam = new HashMap<String, Object>();
//				reparam.put("debtCompany", debtCompany);
//				reparam.put("debtAmt", debtAmt);

				// 抛事件
				FundInvest fundInvest = new FundInvest();
				if(getRepayChannel() != null)	
					fundInvest.setChannelType(getRepayChannel());
				
				if(getBankSubject() != null)
					fundInvest.setFundChannel(getBankSubject());
				
				if(outSerialNo != null)
					fundInvest.setOutSerialNo(outSerialNo);
				if(ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE.equals(lnsbasicinfo.getLoanstat()) || 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(lnsbasicinfo.getLoanstat()))
				{	
					LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
							new FabCurrency());
					lnsAcctInfo.setMerchantNo(getRepayAcctNo());
					lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
					lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
					lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
					// DOTO 如果是老数据抛商户号的时候 需要先select商户号
					if( debtAmt.isPositive() )
						eventProvider.createEvent(ConstantDeclare.EVENT.BACKDEBTCO, debtAmt, lnsAcctInfo, null, fundInvest, "HKCT", ctx, preaccountInfo.getCustomid(),
								debtCompany, ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
				}
				else
				{	
					LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,
						new FabCurrency());
					lnsAcctInfo.setMerchantNo(getRepayAcctNo());
					lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
					lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
					lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
					// DOTO 如果是老数据抛商户号的时候 需要先select商户号
					if( debtAmt.isPositive() )
					{
						if( "3010014".equals( lnsbasicinfo.getPrdcode()) )
							lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
						
						eventProvider.createEvent(ConstantDeclare.EVENT.BACKDEBTCO, debtAmt, lnsAcctInfo, null, fundInvest, "HKCT", ctx, preaccountInfo.getCustomid(),
								debtCompany, ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
					}
						
				}


				// 累加循环报文金额
				sumPkgAmt.selfAdd(debtAmt);
			}
			sumPkgAmt.selfSub(prinAmt);
			if (!sumPkgAmt.isZero()) {
				throw new FabException("LNS028");
			}
            //修改 主文件辅助表里的tunneldata  add at 2019-03-07
			if(null!=myJson && !myJson.isEmpty() && myJson.size()>1){
                Map<String,Object> updateMap = new HashMap<>();
                updateMap.put("acctno", acctNo);
                updateMap.put("openbrc",ctx.getBrc() );
                updateMap.put("key", "WZBL");
                updateMap.put("tunneldata", JsonTransfer.ToJson(myJson));
                try {
                    DbAccessUtil.execute("Lnsbasicinfoex.updateTunneldata", updateMap);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS100", "lnsrpyinfo");
                }
            }
            //add end
		}

		// 幂等登记薄
		Map<String, Object> lns211map = new HashMap<String, Object>();

		lns211map.put("sumrfint", dintAmt.getVal());
		lns211map.put("sumrint", nintAmt.getVal());
		lns211map.put("sumramt", prinAmt.getVal());
		lns211map.put("acctflag", TblLnsinterface.endflag2AcctFlag2(endFlag, ifOverdue));
		lns211map.put("serialno", getSerialNo());
		lns211map.put("trancode", ctx.getTranCode());

		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_repay", lns211map);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsinterface");
		}
		//还款还到本金落表,动态封顶的封顶值变更
		//DynamicCapUtil.saveCapRecord(loanAgreement,ctx ,prinAmt);
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

	public String getPriorType() {
		return priorType;
	}

	public void setPriorType(String priorType) {
		this.priorType = priorType;
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

package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsrpyinfo;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
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
 * @author LH
 *
 * @version V1.0.1
 *
 * 非标准还款本息
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns229 extends WorkUnit {
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
	String outSerialNo;
	String prdId;
	String custType;

	LnsBillStatistics lnsBillStatistics;
	FabAmount prinAmt;
	FabAmount nintAmt;
	FabAmount dintAmt;
	String endFlag;
	LoanAgreement loanAgreement;
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	LoanEventOperateProvider eventProvider;
	//是否为逾期还款
	private boolean  ifOverdue = false; //add at 2019-03-25
	private Integer   txseqno = 0;  //预收明细登记簿  子序号

	@Override
	public void run() throws Exception {
		int pkgflag = 0;
		int listsize;
		int repaysize = 0;
		TranCtx ctx = getTranctx();
		Map<String, FabAmount> repayAmtMap = new HashMap<String, FabAmount>();
		//非标自定义还款债务公司
		ListMap pkgList = null ;
		FabAmount sumAmt = new FabAmount();
		if(null!= ctx.getRequestDict("pkgList")){
			pkgList= ctx.getRequestDict("pkgList");
			//获取贷款协议信息
			loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		}
		// 带债务公司的还款
		if (pkgList != null && pkgList.size() > 0)
			pkgflag = 1;
		// 幂等登记薄

		// 根据账号生成账单 非标准的不生成la
		//loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
/*		if(outSerialNo != null)
			fundInvest.setOutSerialNo(getOutSerialNo());
		if(getBankSubject() != null)
			fundInvest.setFundChannel(getBankSubject());
		if(getRepayChannel() != null)
			fundInvest.setChannelType(getRepayChannel());*/
		if (getRepayAmt() == null || !getRepayAmt().isPositive()) {
			LoggerUtil.info("上送还款金额为零");
			throw new FabException("LNS029");
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
		
		Integer totalTerms =  lnsbasicinfo.getPrinterms();
		
		setPrdId(lnsbasicinfo.getPrdcode());
		setCustType(lnsbasicinfo.getCusttype());

		FundInvest	fundInvest = new FundInvest("", "", repayChannel, bankSubject, outSerialNo);
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
			
			if((ConstantDeclare.REPAYWAY.REPAYWAY_XBHX).equals(lnsbasicinfo.getRepayway()) && 
			ctx.getTranDate().compareTo(lnsbasicinfo.getContduedate()) < 0 &&
			("NINT").equals(lnsbill.getBilltype()) && !(new FabAmount(lnsbasicinfo.getContractbal()).isZero()) )
			{
				continue;
			}	

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
			lnsAcctInfo.setCustType(lnsbasicinfo.getCusttype() );
			lnsAcctInfo.setCustName(lnsbasicinfo.getName());
			lnsAcctInfo.setPrdCode(lnsbasicinfo.getPrdcode());
			// lnsAcctInfo.setBillType(lnsbill.getBilltype())
			// lnsAcctInfo.setLoanForm(lnsbill.getBillstatus())
			lnsAcctInfo.setCancelFlag(lnsbill.getCancelflag());

			sub.operate(lnsAcctInfo, null, amount, fundInvest, ConstantDeclare.BRIEFCODE.HKBX,
					lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), ctx);

			// 如果是本金或者利息呆滞呆账状态,需要转回逾期状态,然后还逾期本金利息,能还多少转回多少
			if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_NINT, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
				lnsOpAcctInfo.setCustType(lnsbasicinfo.getCusttype() );
				lnsOpAcctInfo.setCustName(lnsbasicinfo.getName());
				lnsOpAcctInfo.setPrdCode(lnsbasicinfo.getPrdcode());
				lnsOpAcctInfo.setCancelFlag(lnsbill.getCancelflag());

				eventProvider.createEvent(ConstantDeclare.EVENT.NINTRETURN, amount, lnsAcctInfo, lnsOpAcctInfo,
						fundInvest, ConstantDeclare.BRIEFCODE.LXZH, ctx);
				if( VarChecker.asList("3010013","3010015").contains( lnsbasicinfo.getPrdcode()) )
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
							fundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(),"",loanAgreement.getBasicExtension().getDebtCompany(),"","",lnsbasicinfo.getName());
				else
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
							fundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(),"","","","",lnsbasicinfo.getName());
				
			} else if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
					&& lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)) {
				LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
						ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
				lnsOpAcctInfo.setCustType(lnsbasicinfo.getCusttype());
				lnsOpAcctInfo.setCustName(lnsbasicinfo.getName());
				lnsOpAcctInfo.setPrdCode(lnsbasicinfo.getPrdcode());
				lnsOpAcctInfo.setCancelFlag(lnsbill.getCancelflag());

				eventProvider.createEvent("LOANRETURN", amount, lnsAcctInfo, lnsOpAcctInfo,
						fundInvest, ConstantDeclare.BRIEFCODE.BJZH, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
						fundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
						lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(),"","","","",lnsbasicinfo.getName());
			} else{ //以上为呆滞呆账的利息和本金处理  下面为正常的罚息本时间处理
				if( VarChecker.asList("3010013","3010015").contains( lnsbasicinfo.getPrdcode()) &&
						VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT,
										  ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
										  ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
										  ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(lnsbill.getBilltype()))
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsAcctInfo, null,
							fundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(),"",loanAgreement.getBasicExtension().getDebtCompany(),"","",lnsbasicinfo.getName());
				else
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsAcctInfo, null,
							fundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
							lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(),"","","","",lnsbasicinfo.getName());
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

		//更新主文件合同余额,减去还掉的正常本金 
/*		Map<String, Object> upbasicmap = new HashMap<String, Object>();
		upbasicmap.put("tranAmt", repayAmtMap.get("PRIN.N").getVal());
		upbasicmap.put("acctNo", lnsbasicinfo.getAcctno());
		upbasicmap.put("brc", lnsbasicinfo.getOpenbrc());
		upbasicmap.put("oldPrinDate", lnsbasicinfo.getLastprindate());
		upbasicmap.put("oldIntDate", lnsbasicinfo.getLastintdate());

		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo", upbasicmap);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsbasicinfo");
		}
		*/		
		LoggerUtil.debug("[" + getAcctNo() + "]" + "主流程账单还款:" + "|" + getRepayAmt().getVal());
		// 当期及未来期账单生成及记账 应该拆分 三部分 当期利息 当期本金 未来期本金调三次 然后结清就要根据未来本金余额判断了
		FabAmount prinAdvancesum = new FabAmount();
		FabAmount nintAdvancesum = new FabAmount();
		String prePayFlag = null;
		if (!"3".equals(endFlag) && getRepayAmt().isPositive()
				&& Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate()))) {
			List<RepayAdvance> prinAdvancelist = new ArrayList<RepayAdvance>();
			List<RepayAdvance> nintAdvancelist = new ArrayList<RepayAdvance>();

			//prePayFlag标志用于处理每期从合同起息日计息的情况 辅助表的repayterm等于0时候赋值1
			if( ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway()) )
			{
				endFlag = LoanInterestSettlementUtil.nonStdIrregularRepaymentBill(loanAgreement, ctx, lnsbasicinfo,
						new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, lnsBillStatistics);
			}
			else
			{
				endFlag = LoanInterestSettlementUtil.nonStdRepaymentBill(loanAgreement, ctx, lnsBillStatistics.getBillNo(), lnsbasicinfo,
						new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, lnsBillStatistics,
						prePayFlag, totalTerms);
			}
			

			LoggerUtil.debug("BREAKWHILE:" + prinAdvancelist.size() + "|" + nintAdvancelist.size());

			if (!prinAdvancelist.isEmpty()) {
				for (RepayAdvance prinAdvance : prinAdvancelist) {
					LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), "PRIN", "N", new FabCurrency());
					nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
					nlnsAcctInfo.setCustType(lnsbasicinfo.getCusttype());
					nlnsAcctInfo.setCustName(lnsbasicinfo.getName());
					nlnsAcctInfo.setPrdCode(lnsbasicinfo.getPrdcode());
					nlnsAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lnsbasicinfo.getLoanstat()) ? "3" : "1");
					sub.operate(nlnsAcctInfo, null, prinAdvance.getBillAmt(), fundInvest,
							ConstantDeclare.BRIEFCODE.HKBX, prinAdvance.getBillTrandate(),
							prinAdvance.getBillSerseqno(), prinAdvance.getBillTxseq(), ctx);
					eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, prinAdvance.getBillAmt(),
							nlnsAcctInfo, null, fundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx,
							null, prinAdvance.getBillTrandate(), prinAdvance.getBillSerseqno(),
							prinAdvance.getBillTxseq(),"","","","",lnsbasicinfo.getName());
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
					nlnsAcctInfo.setCustType(lnsbasicinfo.getCusttype() );
					nlnsAcctInfo.setCustName(lnsbasicinfo.getName());
					nlnsAcctInfo.setPrdCode(lnsbasicinfo.getPrdcode());
					nlnsAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat()) ? "3" : "1");
					sub.operate(nlnsAcctInfo, null, nintAdvance.getBillAmt(), fundInvest,
							ConstantDeclare.BRIEFCODE.HKBX, nintAdvance.getBillTrandate(),
							nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(), ctx);
					if( VarChecker.asList("3010013","3010015").contains( lnsbasicinfo.getPrdcode()) )
						eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, nintAdvance.getBillAmt(), nlnsAcctInfo,
								null, fundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
								nintAdvance.getBillTrandate(), nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(),"",loanAgreement.getBasicExtension().getDebtCompany(),"","",lnsbasicinfo.getName());
					else
						eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, nintAdvance.getBillAmt(), nlnsAcctInfo,
								null, fundInvest, ConstantDeclare.BRIEFCODE.HKBX, ctx, null,
								nintAdvance.getBillTrandate(), nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(),"","","","",lnsbasicinfo.getName());
						
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

		
		if (pkgflag == 1) {
			//无追保理还款债务公司要等于放款债务公司
			JSONObject  myJson = JSONObject.parseObject(loanAgreement.getBasicExtension().getJsonStr());
			Map m = myJson; 
			
			for (PubDict pkg : pkgList.getLoopmsg()) {
				String debtCompany = PubDict.getRequestDict(pkg, "debtCompany");
				FabAmount debtAmt = PubDict.getRequestDict(pkg, "debtAmt");
				
				if( Arrays.asList("3010006","3010014").contains(lnsbasicinfo.getPrdcode()) )
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
//
//				Map<String, Object> reparam = new HashMap<String, Object>();
//				reparam.put("debtCompany", debtCompany);
//				reparam.put("debtAmt", debtAmt);

				// 查询预收户信息
				TblLnsassistdyninfo preaccountInfo_N = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), getRepayAcctNo(),"", ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,ctx);
				if (null == preaccountInfo_N) {
					throw new FabException("SPS104", "lnsassistdyninfo/lnsprefundaccount");
				}
				// 抛事件
				FundInvest fundInvest1 = new FundInvest();
				if(getRepayChannel() != null)	
					fundInvest1.setChannelType(getRepayChannel());
				
				if(getBankSubject() != null)
					fundInvest1.setFundChannel(getBankSubject());
				
				if(outSerialNo != null)
					fundInvest1.setOutSerialNo(outSerialNo);
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
					if(debtAmt.isPositive())
						eventProvider.createEvent(ConstantDeclare.EVENT.BACKDEBTCO, debtAmt, lnsAcctInfo, null, fundInvest1, "HKCT", ctx, preaccountInfo_N.getCustomid().trim(),
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
					if(debtAmt.isPositive())
					{
						if( "3010014".equals( lnsbasicinfo.getPrdcode()) )
							lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

						eventProvider.createEvent(ConstantDeclare.EVENT.BACKDEBTCO, debtAmt, lnsAcctInfo, null, fundInvest1, "HKCT", ctx, preaccountInfo_N.getCustomid().trim(),
								debtCompany, ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
					}
				}


				// 累加循环报文金额
				sumAmt.selfAdd(debtAmt);
			}
			sumAmt.selfSub(prinAmt);
			if (!sumAmt.isZero()) {
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
	 * 获取 lnsBillStatistics
	 *
	 * @return lnsBillStatistics the lnsBillStatistics get
	 */
	public LnsBillStatistics getLnsBillStatistics() {
		return this.lnsBillStatistics;
	}

	/**
	 * 设置 lnsBillStatistics
	 *
	 * @param lnsBillStatistics the lnsBillStatistics set
	 */
	public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
		this.lnsBillStatistics = lnsBillStatistics;
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
}

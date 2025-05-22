package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.domain.TblLnsprovision;
import com.suning.fab.loan.domain.TblLnsprovisiondtl;
import com.suning.fab.loan.utils.LoanProvisionProvider;
import com.suning.fab.tup4j.utils.MapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsamortizeplan;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
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
import com.suning.fab.tup4j.utils.PropertyUtil;

/**
 * @author 16071579
 *
 * @version V1.0.1
 *
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns221 extends WorkUnit {

	String errDate; 	   // 错误日期
	String errSerSeq;      // 错误流水号
	String acctNo;		   // 贷款帐号
	FabAmount tranAmt;	   // 交易金额
	String customName;	   // 户名
	FabAmount contractAmt; // 放款金额
	String orgId;
	String channelType;    // 渠道
	String outSerialNo;    // 外部单号
	String memo;		   // 银行流水号/易付宝单号

	@Autowired
	@Qualifier("accountSuber")
	AccountOperator suber;// 用于老数据放款冲销，减去本金户余额
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName="";
		
		TranCtx ctx = getTranctx();
		// 查询幂等登记薄中473004的记录，有则进行摊销
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("trandate", errDate);
		param.put("serseqno", errSerSeq);
		param.put("brc", ctx.getBrc());

		Map<String, Object> retMap;
		try {
			retMap = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsinterface_old", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "query_lnsinterface_old");
		}

		if (null == retMap || retMap.isEmpty()) {
			throw new FabException("LNS003");
		}

		// 设置帐号和金额
		setAcctNo(retMap.get("ACCTNO").toString().trim());
		setTranAmt(new FabAmount(Double.valueOf(retMap.get("TRANAMT").toString())));
		setOrgId(retMap.get("ORGID").toString().trim());
		setChannelType(retMap.get("RESERV1").toString().trim());
		setOutSerialNo(retMap.get("BANKNO").toString().trim());

		// 判断是否满足冲销条件，如果不满足则直接return
		checkCondition();
		// 2、查询幂等登记薄中是否有数据，如果有则判断是否满足冲销的条件，将步骤三
		// 3、查看是否有过还款记录以及有木器账单，如果都没有则进行以下步骤进行冲销事件
		// 4、 查看有没有计提事件，有则冲销，并登记事件登记薄
		// 5、 反向操作开户，放款，放款渠道冲销三个事件，放款和放款渠道需要传相应的金额

		// 获取贷款帐号相关的信息
		LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		loanAgreement.getFundInvest().setInvestee(orgId);
		loanAgreement.getFundInvest().setChannelType(channelType);
		loanAgreement.getFundInvest().setOutSerialNo(outSerialNo);
		setContractAmt(loanAgreement.getContract().getContractAmt());
		setCustomName("");

		// 查看计提，摊销数据
		// 此处可以拿到借据号，根据借据号查询利息计提摊销登记簿，看看有没有记录，有这说明利息计提过，没有则无需操作
		//摊销冲销
		amortizeOff(loanAgreement);
		//计提冲销
		provisionOff(loanAgreement);

		// 开户，放款，放款渠道冲销三个事件
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		// 登记本金开开户冲销事件
		eventProvider.createEvent(ConstantDeclare.EVENT.LOANMEBCAL, new FabAmount(0.00), lnsAcctInfo, null,
				loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.BJXH, ctx);

		// 登记放款渠道冲销事件
		eventProvider.createEvent(ConstantDeclare.EVENT.LOANCNLOFF, tranAmt, lnsAcctInfo, null,
				loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FDCX, ctx);

		/* 减本金户，放款冲销--将账务处理删掉 */
		suber.operate(lnsAcctInfo, null, tranAmt, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FKCX, ctx);
		// 登记放款冲销事件
		eventProvider.createEvent(ConstantDeclare.EVENT.LOANWRTOFF, tranAmt, lnsAcctInfo, null,
				loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FKCX, ctx);

		// 将主文件表lnsbasicinfo中的贷款状态改为CA,计提状态改为CLOSE
		// 冲销完成后，忘lnsbasicinfocal中插入该冲销记录，同时更新幂等登记薄中acctname,acctno,tranamt
		try {
			param.clear();
			param.put("acctno", acctNo);
			param.put("brc", ctx.getBrc());
			param.put("loanstat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);

			/* 更新主文件表中该数据的贷款状态CA,以及利息计提标志CLOSE */
			DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstatEx", param);

			/* 冲销时将该条记录插入到lnsbasicinfocal表中 */
			DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfocal", param);

			/*
			 * 更新幂等登记薄中的acctname(customName), acctNo, tranamt(contractAmt)三个字段的值
			 */
			param.put("serialno", ctx.getSerialNo());
			param.put("trancode", ctx.getTranCode());
			param.put("acctname", "");
			param.put("tranamt", contractAmt.getVal());
			param.put("reserv5", loanAgreement.getFundInvest().getChannelType());
			param.put("memo", memo);
			DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_off", param);

		} catch (FabSqlException e) {
			throw new FabException(e, "SPS101", "update_lnsinterface_off");
		}
	}

	/* 老数据计提冲销 */
	public void provisionOff(LoanAgreement loanAgreement) throws FabException {

		TranCtx ctx = getTranctx();
		//TblLnsprovisionreg lnsprovisionreg;
		TblLnsprovision lnsprovision;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());
		try {
//			lnsprovisionreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_provision", param,
//					TblLnsprovisionreg.class);
			lnsprovision= LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,acctNo,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.PROVISION, loanAgreement);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsprovision");
		}

		if (lnsprovision.isExist()) {// 没有利息计提数据，则不用处理该子交易，直接返回即可
			// 写利息计提摊销登记簿(lnsprovisionreg)
//			TblLnsprovisionreg lnsprovisionregOff = new TblLnsprovisionreg();
			TblLnsprovisiondtl lnsprovisiondtl = new TblLnsprovisiondtl();
			lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
			lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
			lnsprovisiondtl.setTxnseq(1);
			lnsprovisiondtl.setBrc(ctx.getBrc());
		//	lnsprovisionregOff.setTeller(ctx.getTeller());
		//	lnsprovisionregOff.setReceiptno(lnsprovisionreg.getReceiptno());
			lnsprovisiondtl.setAcctno(lnsprovision.getAcctno());
		//	lnsprovisionregOff.setPeriod(lnsprovisionreg.getPeriod() + 1);// 期数
			lnsprovisiondtl.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
			lnsprovisiondtl.setListno(lnsprovision.getTotallist() + 1);
			lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
			lnsprovisiondtl.setCcy(lnsprovision.getCcy());
			lnsprovisiondtl.setTotalinterest(lnsprovision.getTotalinterest());
			lnsprovisiondtl.setTotaltax(lnsprovision.getTotaltax());
			lnsprovisiondtl
					.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
			lnsprovisiondtl.setInterest(lnsprovision.getTotalinterest()); // 本次利息计提金额为
																				// 已计提总额
			lnsprovisiondtl.setTax(lnsprovision.getTotaltax()); // 本次计提税金为已计提税金总额，冲销掉原来的发生额
			lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.PROVISION);
			lnsprovisiondtl.setBegindate(lnsprovision.getLastenddate());
			lnsprovisiondtl.setEnddate(Date.valueOf(ctx.getTranDate()));
			lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.NEGATIVE);// 反向
//			lnsprovisionregOff.setSendflag(ConstantDeclare.SENDFLAG.PENDIND);
//			lnsprovisionregOff.setSendnum(0);
			lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);

//			try {
//				DbAccessUtil.execute("Lnsprovisionreg.insert", lnsprovisionregOff);
//			} catch (FabSqlException e) {
//				throw new FabException(e, "SPS100", "lnsprovisionreg");
//			}

			LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);

			try {
				DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS100", "lnsprovisiondtl");
			}
			//更新利息计提总表
			MapperUtil.map(lnsprovisiondtl, lnsprovision, "map500_01");
			try {
				if(lnsprovision.isSaveFlag()){
					DbAccessUtil.execute("Lnsprovision.updateByUk", lnsprovision);
				}else{
					if(!"".equals(lnsprovisiondtl.getInterflag())){
						DbAccessUtil.execute("Lnsprovision.insert", lnsprovision);
					}
				}
			}catch (FabSqlException e){
				LoggerUtil.info("插入计提明细总表异常：",e);
				throw new FabException(e, "SPS102", "Lnsprovision");
			}

			LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

			List<FabAmount> amtList = new ArrayList<FabAmount>();
			amtList.add(new FabAmount(lnsprovision.getTotaltax()));

			// 写利息计提冲销事件
			eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFINT,
					new FabAmount(lnsprovision.getTotalinterest()), acctinfo, null, loanAgreement.getFundInvest(),
					ConstantDeclare.BRIEFCODE.LXCX, ctx, amtList);
		}
	}

	/* 老数据摊销冲销 */
	public void amortizeOff(LoanAgreement loanAgreement) throws FabException {

		TranCtx ctx = getTranctx();
		// 根据条件找到相应的记录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());

		TblLnsamortizeplan lnsamortizeplan;
		try {// 根据核心流水号取出符合条件的记录，摊销计划表中的核心流水号和开户放款的是一致的
			lnsamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsamortizeplan_off_old", param,
					TblLnsamortizeplan.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsamortizeplan");
		}

		/* 如果摊销计划表中无此数据，则意味着该帐号不需要摊销，直接返回，进行下一步 */
		if (null == lnsamortizeplan) {
			return;
		}

		// 更新摊销计划表中的摊销金额， 登记摊销计提登记薄，登记事件
		FabAmount amortizedAmt = new FabAmount(lnsamortizeplan.getAmortizeamt()); // 已摊销金额
		FabAmount taxedAmt = new FabAmount(lnsamortizeplan.getAmortizetax()); // 已摊销税金
		LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnsamortizeplan.getAcctno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

		if (amortizedAmt.isPositive()) {// 如果已摊销金额大于零，则需要处理利息计提摊销登记薄以及事件登记薄，
										// 并将摊销登记表中的已摊销金额，已摊销税金，最后摊销日期进行更新

			// 登记利息计提摊销登记薄，登记正反标志为反向TblLnsprovisionreg
	//		TblLnsprovisionreg lnsprovisionreg = new TblLnsprovisionreg();
			TblLnsprovisiondtl lnsprovisiondtl = new TblLnsprovisiondtl();
			lnsprovisiondtl.setTrandate(Date.valueOf(ctx.getTranDate()));
			lnsprovisiondtl.setSerseqno(ctx.getSerSeqNo());
			lnsprovisiondtl.setTxnseq(1);
			lnsprovisiondtl.setBrc(ctx.getBrc());
		//	lnsprovisionreg.setTeller(ctx.getTeller());
			lnsprovisiondtl.setAcctno(lnsamortizeplan.getAcctno());
//			lnsprovisionreg.setReceiptno(lnsamortizeplan.getAcctno());
//			lnsprovisionreg.setPeriod(lnsamortizeplan.getPeriod() + 1);
			lnsprovisiondtl.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
			lnsprovisiondtl.setListno(lnsamortizeplan.getPeriod() + 1);
			lnsprovisiondtl.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);// 设置为利息
			lnsprovisiondtl.setCcy(new FabCurrency().getCcy());
			lnsprovisiondtl.setTotalinterest(amortizedAmt.getVal()); // 已摊销总金额
			lnsprovisiondtl.setTotaltax(taxedAmt.getVal()); // 已摊销总税金
			lnsprovisiondtl.setTaxrate(lnsamortizeplan.getTaxrate());// 税率
			lnsprovisiondtl.setInterest(amortizedAmt.getVal()); // 本次摊销金额为已摊销金额
			lnsprovisiondtl.setTax(taxedAmt.getVal()); // 本次摊销税金为已摊销税金
			lnsprovisiondtl.setIntertype(ConstantDeclare.INTERTYPE.AMORTIZE);// 摊销
			lnsprovisiondtl.setBegindate(Date.valueOf(lnsamortizeplan.getLastdate()));// 摊销表中最后摊销日期为本表起始日期
			lnsprovisiondtl.setEnddate(Date.valueOf(ctx.getTranDate()));// 本次调用的账务日期为结束日期
			lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.NEGATIVE);// 反向
//			lnsprovisionreg.setSendflag(ConstantDeclare.SENDFLAG.PENDIND);
//			lnsprovisionreg.setSendnum(0);
			lnsprovisiondtl.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
			lnsprovisiondtl.setReserv1(null);
			lnsprovisiondtl.setReserv2(null);
			LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
			try {
				DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "lnsprovisiondtl");
			}

			List<FabAmount> amtList = new ArrayList<FabAmount>();
			amtList.add(taxedAmt);
			// 登记摊销冲销事件， 将已摊销金额及已摊销税金进行反向处理
			eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZOFF, amortizedAmt, lnsAcctInfo, null,
					loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.TXCX, ctx, amtList);
		} // 如果摊销金额为零，则还未发生摊销，直接将摊销计划表中的status更改为CLOSE

		try {// 更新摊销表，只要有数据，则都应该更新摊销计划表中的status为CLOSE,有金额产生时，则还应登记事件登记薄
			param.put("lastdate", ctx.getTranDate());
			DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan_off", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsamortizeplan");
		}

	}

	public void checkCondition() throws FabException {

		TranCtx ctx = getTranctx();

		/**
		 * 1、登记幂等登记薄，根据serialno以及 trancode来判断是否幂等，
		 * 2、如果幂等，则去幂等登记薄中查询customName,acctNo,contractAmt三个字段对应的值，并返回给外围
		 * 3、如果不幂等，判断是否满足冲销条件(无逾期记录，且未还款)，满足则进行冲销，最后删除数据的借据号相关记录之后，
		 * 更新幂等登记薄中刚插入的记录的customName,acctNo,contractAmt三个字段的值，否则直接返回
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

		// 查看还款明细表，查看待冲销用户有没有还款记录，有则直接终止放款冲销交易，
		Map<String, Object> param = new HashMap<String, Object>();
		param.clear();
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());
		Map<String, Object> retMap;
		try {
			retMap = DbAccessUtil.queryForMap("CUSTOMIZE.query_lnsrpyinfo_count", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsrpyinfo");
		}

		if (Integer.parseInt(retMap.get("NUM").toString()) > 0) {// 如果还款明细表中有数据，则不能冲销，直接返回，结束
			throw new FabException("LNS001");
		}

		// 查看用户有没有出现过逾期的记录，有则直接终止放款冲销交易，查看账单表
		List<Map<String, Object>> retList;
		try {
			retList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_info", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbill");
		}

		if (null != retList && !retList.isEmpty()) {
			for (Map<String, Object> map : retList) {
				if (ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(map.get("billstatus"))) {// 如果出现逾期情况，则直接退出，结束程序
					throw new FabException("LNS002");
				}
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
	 * @param errDate
	 *            the errDate to set
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
	 * @param errSerSeq
	 *            the errSerSeq to set
	 */
	public void setErrSerSeq(String errSerSeq) {
		this.errSerSeq = errSerSeq;
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
	 * @return the tranAmt
	 */
	public FabAmount getTranAmt() {
		return tranAmt;
	}

	/**
	 * @param tranAmt
	 *            the tranAmt to set
	 */
	public void setTranAmt(FabAmount tranAmt) {
		this.tranAmt = tranAmt;
	}

	/**
	 * @return the customName
	 */
	public String getCustomName() {
		return customName;
	}

	/**
	 * @param customName
	 *            the customName to set
	 */
	public void setCustomName(String customName) {
		this.customName = customName;
	}

	/**
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}

	/**
	 * @param contractAmt
	 *            the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	/**
	 * @return the orgId
	 */
	public String getOrgId() {
		return orgId;
	}

	/**
	 * @param orgId
	 *            the orgId to set
	 */
	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	/**
	 * @return the channelType
	 */
	public String getChannelType() {
		return channelType;
	}

	/**
	 * @param channelType
	 *            the channelType to set
	 */
	public void setChannelType(String channelType) {
		this.channelType = channelType;
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
	 * @return the memo
	 */
	public String getMemo() {
		return memo;
	}

	/**
	 * @param memo the memo to set
	 */
	public void setMemo(String memo) {
		this.memo = memo;
	}

}

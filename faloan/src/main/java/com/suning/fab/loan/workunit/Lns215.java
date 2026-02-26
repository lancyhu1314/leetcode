package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsprefundaccount;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 	LH
 *
 * @version V1.0.1
 *
 * 		债务公司预收充值
 *
 * 	serialNo		业务流水号
 *			channelType		预收渠道
 *			fundChannel		借方总账科目
 *			repayAcctNo		贷方账号
 *			ccy				币种
 *			amt				金额
 *			outSerialNo		外部流水号
 *			memo			摘要信息
 *			receiptNo		出帐编号
 *			pkgList			循环报文
 * 			debtCompany		债务公司代码
 *			debtAmt			债务公司保理金额
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns215 extends WorkUnit {

	String  serialNo;
	String  fundChannel;
	String	channelType;
	String  repayAcctNo;
	String  ccy;
	FabAmount amt;
	String  outSerialNo;
	String  memo;
	String  receiptNo;
	ListMap pkgList;

	String debtCompany;
	FabAmount debtAmt;

	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();

		if(debtAmt.isNegative() || debtAmt.isZero()){
			throw new FabException("LNS014");
		}

		if(VarChecker.isEmpty(debtCompany)){
			throw new FabException("LNS015");
		}

		if(VarChecker.isEmpty(channelType)){
			throw new FabException("LNS039");
		}

//		//查询保理户
//		Map<String,Object> param = new HashMap<String,Object>();
//		param.put("brc", ctx.getBrc());
//		param.put("acctno", repayAcctNo);
//		param.put("accsrccode", "N");
//
//		TblLnsprefundaccount lnsprefundacct = null;
//		try {
//			lnsprefundacct = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprefundaccount_customid", param, TblLnsprefundaccount.class);
//		}
//		catch (FabSqlException e)
//		{
//			throw new FabException(e, "SPS103", "lnsprefundaccount");
//		}
//
//		if (null == lnsprefundacct){
//			throw new FabException("SPS104", "lnsprefundaccount");
//		}

		// 从辅助动态中查询保理户
		TblLnsassistdyninfo lnsassistdyninfo = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), repayAcctNo,"", ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,ctx);

		if (null == lnsassistdyninfo) {
			throw new FabException("SPS104", "lnsassistdyninfo/lnsprefundaccount");
		}


		//保理户开户
		TblLnsprefundaccount lnsprefundaccount = new TblLnsprefundaccount();
		//开户网点
		lnsprefundaccount.setBrc(ctx.getBrc());
		//客户号
		lnsprefundaccount.setCustomid(debtCompany);
		//保理账号
		lnsprefundaccount.setAcctno(debtCompany);
		//保理户类型 N-预收户 D-债务公司
		lnsprefundaccount.setAccsrccode("D");
		//客户类型 PERSON-对私 COMPANY-对公
		lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
		//开户日期
		lnsprefundaccount.setOpendate(ctx.getTranDate());
		//余额
		lnsprefundaccount.setBalance(0.00);
		//账户状态 NORMAL-正常 CANCEL-销户
		lnsprefundaccount.setStatus(ConstantDeclare.STATUS.NORMAL);
		lnsprefundaccount.setName(debtCompany);

		try {
			DbAccessUtil.execute("Lnsprefundaccount.insert", lnsprefundaccount);
		}
		catch (FabSqlException e)
		{
			if (!ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e, "SPS100", "lnsprefundaccount");
			}
		}


		/*
		20190604债务公司事件优化去掉
		20190906 5105的预收账号，如果充值的时候传了债务公司，就抛事件登记预收明细表
		*/
		if("51050000".equals(ctx.getBrc()) || "51310000".equals(ctx.getBrc())){
			LnsAcctInfo lnsAcctInfo = null;
			if(!VarChecker.isEmpty(receiptNo)) {
				//处理老数据acctno与acctno1不一致
				Map<String, Object> paramMap = new HashMap<String, Object>();
				paramMap.put("acctno1", receiptNo);
				paramMap.put("brc", ctx.getBrc());

				Map<String, Object> custominfo = DbAccessUtil.queryForMap("CUSTOMIZE.query_acctno", paramMap);
				if (custominfo != null) {
					receiptNo = custominfo.get("acctno").toString();
					lnsAcctInfo = new LnsAcctInfo(receiptNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				}
			}
			FundInvest	fundInvest = new FundInvest("", "", channelType, fundChannel, outSerialNo);
			//写事件
			eventProvider.createEvent(ConstantDeclare.EVENT.RECGDEBTCO, debtAmt, lnsAcctInfo, null,
					fundInvest, ConstantDeclare.BRIEFCODE.YZHK, ctx,
					lnsassistdyninfo.getCustomid(), debtCompany, ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
		}
	}

	/**
	 * @return the serialNo
	 */
	public String getSerialNo() {
		return serialNo;
	}

	/**
	 * @param serialNo the serialNo to set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
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
	 * @return the repayAcctNo
	 */
	public String getRepayAcctNo() {
		return repayAcctNo;
	}

	/**
	 * @param repayAcctNo the repayAcctNo to set
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
	 * @param ccy the ccy to set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	/**
	 * @return the amt
	 */
	public FabAmount getAmt() {
		return amt;
	}

	/**
	 * @param amt the amt to set
	 */
	public void setAmt(FabAmount amt) {
		this.amt = amt;
	}

	/**
	 * @return the outSerialNo
	 */
	public String getOutSerialNo() {
		return outSerialNo;
	}

	/**
	 * @param outSerialNo the outSerialNo to set
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

	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}

	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}

	/**
	 * @return the pkgList
	 */
	public ListMap getPkgList() {
		return pkgList;
	}

	/**
	 * @param pkgList the pkgList to set
	 */
	public void setPkgList(ListMap pkgList) {
		this.pkgList = pkgList;
	}

	/**
	 * @return the debtCompany
	 */
	public String getDebtCompany() {
		return debtCompany;
	}

	/**
	 * @param debtCompany the debtCompany to set
	 */
	public void setDebtCompany(String debtCompany) {
		this.debtCompany = debtCompany;
	}

	/**
	 * @return the debtAmt
	 */
	public FabAmount getDebtAmt() {
		return debtAmt;
	}

	/**
	 * @param debtAmt the debtAmt to set
	 */
	public void setDebtAmt(FabAmount debtAmt) {
		this.debtAmt = debtAmt;
	}


}

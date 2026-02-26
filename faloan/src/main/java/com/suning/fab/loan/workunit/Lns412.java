package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;

/**
 *  〈一句话功能简述〉
 *  〈功能详细描述〉：
 * @Author SC
 * @Description
 * @Date change in 20:42 2019/5/27
 */

@Scope("prototype")
@Repository
public class Lns412 extends WorkUnit {
	String 		acctNo;			//本金账号
	String		ccy;
	String		cashFlag;
	String		termDate;
	String		tranCode;
	FabAmount	repayAmt;
	String		brc;
	Map<String,FabAmount> repayAmtMap;
	Map<String,FabAmount> refundAmtMap;
	Integer		subNo;
	LnsBillStatistics billStatistics;
	FabAmount	intAmt; //利息
	FabAmount	prinAmt; //本金
	FabAmount	forfeitAmt; //罚息
	String		endFlag; //结清标志 1-未结清 3-结清 2-重新生成换计划
	String	repayDate;
	TblLnsbasicinfo lnsbasicinfo;
	
	FabAmount refundIntAmt;//可退利息
	FabAmount refundPrinAmt;//可退本金
	FabAmount refundForfeitAmt;//可退罚息
	
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override

	public void run() throws Exception {
		int			listsize;
		int			repaysize = 0;
		TranCtx ctx = getTranctx();
		Map<String,FabAmount> repayAmtMapIn = new HashMap<String,FabAmount>();

		//初始化客户还款上送金额 该金额不做运算 在Lns212中与客户剩余金额轧差
		if(!getRepayAmt().isPositive())
		{
			LoggerUtil.debug("上送还款金额不能为零");
			throw new FabException("LNS029");
		}
		LoggerUtil.debug("REPAYAMT:" + getRepayAmt().getVal());

		//根据账号生成账单
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);

		//读取主文件判断贷款状态(开户,销户)  判断贷款形态(正常,逾期,呆滞,呆账)取得对应还款顺序
		Map<String, Object> bparam = new HashMap<String, Object>();
		bparam.put("acctno", getAcctNo());
		bparam.put("openbrc", ctx.getBrc());
		if (null == lnsbasicinfo) {
			try {
				lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", bparam, TblLnsbasicinfo.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}

		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())) {
			LoggerUtil.debug("该账户已销户或已核销");
			throw new FabException("ACC108", acctNo);
		}
		if(billStatistics==null)
			billStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate, ctx);
		//获取呆滞呆账期间新罚息复利账单list
		List<TblLnsbill> cdbillList = new ArrayList<>();
		for(LnsBill lnsbill :billStatistics.getCdbillList()) {
			cdbillList.add(BillTransformHelper.convertToTblLnsBill(la, lnsbill, ctx));
		}


		/* 2018-06-06  罚息账本不是每天落表，罚息复利取试算信息*/
		for( LnsBill lnsbill:billStatistics.getHisSetIntBillList())
		{
			//过滤复利基数账本数据，对后续endFlag有影响
			if("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no"))) {
				if ("AMLT".equals(lnsbill.getBillType()))
					continue;
			}

			cdbillList.add(BillTransformHelper.convertToTblLnsBill(la,lnsbill,ctx));
		}
		for( LnsBill lnsbill:billStatistics.getFutureOverDuePrinIntBillList())
		{
			//过滤复利基数账本数据，对后续endFlag有影响
			if("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no"))) {
				if ("AMLT".equals(lnsbill.getBillType()))
					continue;
			}

			cdbillList.add(BillTransformHelper.convertToTblLnsBill(la,lnsbill,ctx));
		}


		//读取贷款账单
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", getAcctNo());
		param.put("brc", ctx.getBrc());
		List<TblLnsbill> billList = new ArrayList<TblLnsbill>();
		try{
			billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", param, TblLnsbill.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "lnsbill");
		}

		billList.addAll(cdbillList);

		//核销/非应计的贷款未结息  需要加上从上次结本日到当期的未结本息账本
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lnsbasicinfo.getLoanstat().trim()))
		{
			for( LnsBill lnsbill:billStatistics.getBillInfoList())
				billList.add(BillTransformHelper.convertToTblLnsBill(la,lnsbill,ctx));

		}

		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbasicinfo.getLoanstat()) ||
				ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbasicinfo.getLoanstat())
				||LoanBasicInfoProvider.ifLanguishOrBad(lnsbasicinfo,repayDate)  ) {
			billList = LoanRepayOrderHelper.dullBad(billList);
		}
		else
		{//正常罚息本顺序还款
			billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
		}

		listsize = billList.size();
		LoggerUtil.debug("LNSBILL:billList:"+ billList.size());
		for(TblLnsbill lnsbill : billList)
		{
			LoggerUtil.debug("LNSBILL:"+lnsbill.getAcctno()+"|"+lnsbill.getPeriod()+"|"+lnsbill.getBillstatus()+"."+lnsbill.getBilltype()+"|"+lnsbill.getBillbal());
			FabAmount minAmt = new FabAmount();

			//首先给发生额赋值为剩余还款金额 因为之后该金额就要减账单金额发生变化
			minAmt.setVal(getRepayAmt().getVal());

			getRepayAmt().selfSub(lnsbill.getBillbal());
			LoggerUtil.debug("minAmt:" + minAmt + " reminAmt" + getRepayAmt());
			if(getRepayAmt().isPositive())
			{
 				minAmt.setVal(lnsbill.getBillbal());
			}

			if(repayAmtMapIn.get(lnsbill.getBilltype()+"."+lnsbill.getBillstatus()) == null)
			{
				FabAmount amt = new FabAmount(minAmt.getVal());
				repayAmtMapIn.put(lnsbill.getBilltype()+"."+lnsbill.getBillstatus(), amt);
			}
			else
			{
				repayAmtMapIn.get(lnsbill.getBilltype()+"."+lnsbill.getBillstatus()).selfAdd(minAmt);
			}

			if(!getRepayAmt().isPositive())
			{
				if(lnsbill.getBillbal().equals(minAmt.getVal())) //余额等于发生额 该条账单处理结束 加1
					repaysize++;
				break;
			}
			repaysize++;
		}
		LoggerUtil.debug("LNSBILL:repaysize:"+ repaysize);
		
	
		//退货汇总
		this.refundAmtMap = repayAmtMapIn;
		FabAmount   refundPrinAmtTmp=new FabAmount();
		FabAmount   refundNintAmtTmp=new FabAmount();
		FabAmount   refundDintAmtTmp=new FabAmount();
		for (Map.Entry<String, FabAmount> entry : refundAmtMap.entrySet()) {
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
				refundPrinAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
				refundNintAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
				refundDintAmtTmp.selfAdd(entry.getValue());
		}
		
		if( getRepayAmt().isPositive() )
			if( getRepayAmt().sub(la.getContract().getBalance().getVal()).isPositive())
				refundPrinAmtTmp.selfAdd(la.getContract().getBalance());
			else
				refundPrinAmtTmp.selfAdd(getRepayAmt());
		
		refundPrinAmt=refundPrinAmtTmp;
		refundIntAmt=refundNintAmtTmp;
		refundForfeitAmt=refundDintAmtTmp;
		
		
		

		endFlag = "1"; //整笔贷款未结清

		if(!Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate())) && listsize == repaysize)
		{
			endFlag = "3";
		}

		//灰度，老系统历史期提前结清时有问题
        if("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no"))) {
            if (la.getContract().getBalance().isZero() && listsize == repaysize) {
                endFlag = "3";
            }
        }

		LoggerUtil.debug("["+getAcctNo()+"]"+"主流程账单还款:" + "|" + getRepayAmt().getVal());
		//当期及未来期账单生成及记账 应该拆分 三部分 当期利息 当期本金 未来期本金调三次 然后结清就要根据未来本金余额判断了
		FabAmount	prinAdvancesum = new FabAmount();
		FabAmount	nintAdvancesum = new FabAmount();
		String	prePayFlag = null;
		String futureEndFlag = null;
		if(getRepayAmt().isPositive() && Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate())))
		{
			List<RepayAdvance> prinAdvancelist = new ArrayList<RepayAdvance>();
			List<RepayAdvance> nintAdvancelist = new ArrayList<RepayAdvance>();
			if( ConstantDeclare.ISCALINT.ISCALINT_NO.equals(lnsbasicinfo.getIscalint()) )
				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN;
			else if(ConstantDeclare.ISCALINT.ISCALINT_YES.equals(lnsbasicinfo.getIscalint()))
				prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_INTPRIN;
			if(ConstantDeclare.REPAYWAY.isEqualInterest(la.getWithdrawAgreement().getRepayWay()))
				futureEndFlag = LoanInterestSettlementUtil.specialRepaymentBillPlan(ctx, subNo,lnsbasicinfo, new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,prePayFlag);
			else
				futureEndFlag = LoanInterestSettlementUtil.interestRepaymentBillPlan(la, ctx, subNo, lnsbasicinfo, new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,prePayFlag);

			if("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no"))) {
				if( !"3".equals(endFlag))
					endFlag = futureEndFlag;
			}
			else{
				endFlag = futureEndFlag;
			}

			LoggerUtil.debug("BREAKWHILE:" + prinAdvancelist.size() + "|" + nintAdvancelist.size());

			if(!prinAdvancelist.isEmpty())
			{
				for(RepayAdvance prinAdvance : prinAdvancelist)
				{
					prinAdvancesum.selfAdd(prinAdvance.getBillAmt());
				}
				if(repayAmtMapIn.get("PRIN.N") == null)
				{
					repayAmtMapIn.put("PRIN.N", prinAdvancesum);
				}
				else
				{
					repayAmtMapIn.get("PRIN.N").selfAdd(prinAdvancesum);
				}
			}
			if(!nintAdvancelist.isEmpty())
			{
				for(RepayAdvance nintAdvance : nintAdvancelist)
				{
					nintAdvancesum.selfAdd(nintAdvance.getBillAmt());
				}
				if(repayAmtMapIn.get("NINT.N") == null)
				{
					repayAmtMapIn.put("NINT.N", nintAdvancesum);
				}
				else
				{
					repayAmtMapIn.get("NINT.N").selfAdd(nintAdvancesum);
				}
			}
		}

		this.repayAmtMap = repayAmtMapIn;
		FabAmount	prinAmtTmp = new FabAmount();
		FabAmount	nintAmtTmp = new FabAmount();
		FabAmount	dintAmtTmp = new FabAmount();
		for (Map.Entry<String, FabAmount> entry : repayAmtMap.entrySet()) {
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
				prinAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
				nintAmtTmp.selfAdd(entry.getValue());
			if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
					|| entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
				dintAmtTmp.selfAdd(entry.getValue());
		}

		prinAmt = prinAmtTmp;
		intAmt = nintAmtTmp;
		forfeitAmt = dintAmtTmp;

	}

//	//为核销的贷款 判断呆滞呆账
//	private Boolean ifLanguishOrBad(TblLnsbasicinfo lnsbasicinfo	)
//	{
//		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION.equals(lnsbasicinfo.getLoanstat().trim())){
//			return false;
//		}
//		String loanform = PropertyUtil.getPropertyOrDefault("transfer."+String.valueOf(3),null);
//		Integer days;
//		if (loanform == null)
//			days = 90;
//		else {
//			String tmp[] = loanform.split(":");
//			//逾期天数
//			days =Integer.parseInt(tmp[1]);
//		}
//
//		//计算逾期开始日期
//		String overStartDate = CalendarUtil.nDaysAfter(lnsbasicinfo.getContduedate(), days).toString("yyyy-MM-dd");
//
//		//判断呆滞呆账未呆滞呆账
//		return !(CalendarUtil.beforeAlsoEqual(repayDate, overStartDate));
//
//	}
	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}
	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
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
	 * @return the cashFlag
	 */
	public String getCashFlag() {
		return cashFlag;
	}
	/**
	 * @param cashFlag the cashFlag to set
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
	 * @param termDate the termDate to set
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
	 * @param tranCode the tranCode to set
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
	 * @param repayAmt the repayAmt to set
	 */
	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}
	/**
	 * @return the brc
	 */
	public String getBrc() {
		return brc;
	}
	/**
	 * @param brc the brc to set
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
	 * @param repayAmtMap the repayAmtMap to set
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
	 * @param subNo the subNo to set
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
	 * @param billStatistics the billStatistics to set
	 */
	public void setBillStatistics(LnsBillStatistics billStatistics) {
		this.billStatistics = billStatistics;
	}
	/**
	 * @return the intAmt
	 */
	public FabAmount getIntAmt() {
		return intAmt;
	}
	/**
	 * @param intAmt the intAmt to set
	 */
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}
	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}
	/**
	 * @param prinAmt the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}
	/**
	 * @return the forfeitAmt
	 */
	public FabAmount getForfeitAmt() {
		return forfeitAmt;
	}
	/**
	 * @param forfeitAmt the forfeitAmt to set
	 */
	public void setForfeitAmt(FabAmount forfeitAmt) {
		this.forfeitAmt = forfeitAmt;
	}
	/**
	 * @return the endFlag
	 */
	public String getEndFlag() {
		return endFlag;
	}
	/**
	 * @param endFlag the endFlag to set
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
	 * @param sub the sub to set
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
	 * @param eventProvider the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}

	public String getRepayDate() {
		return repayDate;
	}

	public void setRepayDate(String repayDate) {
		this.repayDate = repayDate;
	}

	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}

	/**
	 * @return the refundIntAmt
	 */
	public FabAmount getRefundIntAmt() {
		return refundIntAmt;
	}

	/**
	 * @param refundIntAmt the refundIntAmt to set
	 */
	public void setRefundIntAmt(FabAmount refundIntAmt) {
		this.refundIntAmt = refundIntAmt;
	}

	/**
	 * @return the refundPrinAmt
	 */
	public FabAmount getRefundPrinAmt() {
		return refundPrinAmt;
	}

	/**
	 * @param refundPrinAmt the refundPrinAmt to set
	 */
	public void setRefundPrinAmt(FabAmount refundPrinAmt) {
		this.refundPrinAmt = refundPrinAmt;
	}

	/**
	 * @return the refundForfeitAmt
	 */
	public FabAmount getRefundForfeitAmt() {
		return refundForfeitAmt;
	}

	/**
	 * @param refundForfeitAmt the refundForfeitAmt to set
	 */
	public void setRefundForfeitAmt(FabAmount refundForfeitAmt) {
		this.refundForfeitAmt = refundForfeitAmt;
	}

	/**
	 * @return the refundAmtMap
	 */
	public Map<String, FabAmount> getRefundAmtMap() {
		return refundAmtMap;
	}

	/**
	 * @param refundAmtMap the refundAmtMap to set
	 */
	public void setRefundAmtMap(Map<String, FabAmount> refundAmtMap) {
		this.refundAmtMap = refundAmtMap;
	}
}

package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Date;
import java.text.SimpleDateFormat;
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
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.CalendarUtil;
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
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 18049705
 *
 * @version V1.0.0
 *
 * @see P2P违约金
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns250 extends WorkUnit{
	
	String acctNo;
	TblLnsbasicinfo tblLnsbasicinfo;
	FabAmount penaltyAmt = new FabAmount();
	FabAmount repayAmt = new FabAmount();
	String serialNo;
	LnsBillStatistics lnsBillStatistics;
	String dealDate;
	Integer subNo;	
	String repayAcctNo;
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		//获取违约金费率 
		Double penaltyRate = tblLnsbasicinfo.getBjtxbl();
		
		
		//减去当前期的金额 begin
		List<LnsBill> tranDateBill  = new ArrayList<LnsBill>();		
		tranDateBill.addAll(lnsBillStatistics.getHisSetIntBillList());//历史期的利息
		tranDateBill.addAll(lnsBillStatistics.getBillInfoList());//当前期
		tranDateBill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		tranDateBill.addAll(lnsBillStatistics.getFutureBillInfoList());

		LnsBill currBill = new LnsBill();
		Integer period = 0;
		FabAmount prinBal = new FabAmount();
		
		//先息后本  取利息账单
		if("4".equals(tblLnsbasicinfo.getRepayway())){
			for(LnsBill lnsBill : tranDateBill){
				if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
						&&CalendarUtil.beforeAlsoEqual(dealDate, lnsBill.getEndDate())
						&&CalendarUtil.afterAlsoEqual(dealDate,lnsBill.getStartDate())){
							period = lnsBill.getPeriod();
							currBill = lnsBill;
							prinBal = lnsBill.getBalance();
							break;
				}
			}
			
		}
		
		else
		{
			for(LnsBill lnsBill : tranDateBill){
				if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
						&&CalendarUtil.beforeAlsoEqual(dealDate, lnsBill.getEndDate())
						&&CalendarUtil.afterAlsoEqual(dealDate,lnsBill.getStartDate())){
							period = lnsBill.getPeriod();
							currBill = lnsBill;
							prinBal = lnsBill.getPrinBal();
							break;
				}
			}
		}
		//end
		//获取账单表信息
		
		
		if(period == 0)
		{
			
			/*
			 * 结息日还款本期后，若结清全部贷款  当前期已经入账单表 试算未还账单没有此结息日账单  则调用历史期账单查询 
			 */
			
			//结息日
			List<LnsBill> hisBills = lnsBillStatistics.getHisBillList();
			//先息后本  取利息账单
			if("4".equals(tblLnsbasicinfo.getRepayway())){
				for(LnsBill lnsBill : hisBills){
					if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
							&&CalendarUtil.beforeAlsoEqual(dealDate, lnsBill.getEndDate())
							&&CalendarUtil.afterAlsoEqual(dealDate,lnsBill.getStartDate())){
								period = lnsBill.getPeriod();
								currBill = lnsBill;
								prinBal = lnsBill.getPrinBal();
								break;
					}
				}
				
			}
			
			else
			{
				for(LnsBill lnsBill : hisBills){
					if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
							&&CalendarUtil.beforeAlsoEqual(dealDate, lnsBill.getEndDate())
							&&CalendarUtil.afterAlsoEqual(dealDate,lnsBill.getStartDate())){
								period = lnsBill.getPeriod();
								currBill = lnsBill;
								prinBal = lnsBill.getPrinBal();
								break;
					}
				}
			}
			//历史期账单也没有  则直接返回返回
			if(period==0)  return;
		}

		penaltyAmt = new FabAmount(BigDecimal.valueOf(prinBal.getVal()).multiply(BigDecimal.valueOf(penaltyRate)).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue()); 
        LoggerUtil.debug("  contractbal : " +prinBal.getVal() +"  |penaltyRate : "+penaltyRate+"  |penaltyAmt : " +penaltyAmt);
		//违约金金额不为正数 不做操作
        if(!penaltyAmt.isPositive())
		{
			return;
		}
        
        Map<String, Object> param = new HashMap<String, Object>();
		param.put("repaydate", ctx.getTranDate());
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());
		
		LoggerUtil.debug("  period : " +period);
		//违约金记录lnsrpyplan
		//违约金记入当前期			
		
		param.put("repayterm", period);//期数
		
		param.put("reserve2", getPenaltyAmt().getVal());
		try {
			DbAccessUtil.execute(
					"CUSTOMIZE.update_lnsrpyplan_reserve2", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "update_lnsrpyplan_reserve2");
		}
		
		//违约金落表 LnsBill
					
//		param.put("acctno", value)
//		param.put("brc", value)
		param.put("period", period);
		TblLnsbill  tblLnsbill = new TblLnsbill();
		
		MapperUtil.iomap(currBill, tblLnsbill);
		tblLnsbill.setAcctno(acctNo);
		tblLnsbill.setBrc(ctx.getBrc());
		
		if (!VarChecker.isEmpty(currBill.getPrinBal()))
		{
			
			tblLnsbill.setPrinbal(currBill.getPrinBal().getVal());//账单对应本金余额
		}
		if (!VarChecker.isEmpty(currBill.getBillRate()))
		{
			tblLnsbill.setBillrate(currBill.getBillRate().getYearRate().doubleValue());//账单执行利率
		}
		if (!VarChecker.isEmpty(currBill.getAccumulate()))
		{
			
			tblLnsbill.setAccumulate(currBill.getAccumulate().getVal());//罚息/复利积数
		}
		
		tblLnsbill.setBegindate(currBill.getStartDate());//账单起始日期
		tblLnsbill.setRepayedate(currBill.getRepayendDate());//账单应还款止日
		tblLnsbill.setIntedate(currBill.getIntendDate());
		tblLnsbill.setSettledate(currBill.getSettleDate());
		tblLnsbill.setStatusbdate(currBill.getStatusbDate());
		tblLnsbill.setCcy(currBill.getCcy());
		
		
		
		tblLnsbill.setBilltype("PNLA");	//账单类型  PNLA违约金
		tblLnsbill.setPeriod(period);	//	期数
		tblLnsbill.setBillamt(getPenaltyAmt().getVal());	//账单金额
		tblLnsbill.setBillbal(0.00);	//账单余额		
		tblLnsbill.setTxseq(++subNo);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		tblLnsbill.setTrandate(new Date(format.parse(ctx.getTranDate()).getTime()));
		tblLnsbill.setRepayedate(ctx.getTranDate());
		tblLnsbill.setSerseqno(ctx.getSerSeqNo());
		tblLnsbill.setBillproperty("REPLAY");
		tblLnsbill.setBillstatus("N");
		tblLnsbill.setSettleflag("CLOSE");
		try{
			DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);		
		}catch (FabException e){
			throw new FabException(e, "SPS100", "lnsbill");
		}
		
		//违约金事件
		// 根据账号生成账单
		LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(
						getAcctNo(), ctx);
		LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, "N",
				new FabCurrency());
		nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
		nlnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
		nlnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
		nlnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
		
		//--违约金收取
		eventProvider.createEvent(ConstantDeclare.EVENT.LNPENALSUM, penaltyAmt,
				nlnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.SWYJ, ctx,
				null, ctx.getTranDate(), ctx.getSerSeqNo(),
				tblLnsbill.getTxseq());
		
	}
	
	/**
	 * 
	 * @return the tblLnsbasicinfo
	 */
	public TblLnsbasicinfo getTblLnsbasicinfo() {
		return tblLnsbasicinfo;
	}


	/**
	 * @param tblLnsbasicinfo the tblLnsbasicinfo to set
	 */
	public void setTblLnsbasicinfo(TblLnsbasicinfo tblLnsbasicinfo) {
		this.tblLnsbasicinfo = tblLnsbasicinfo;
		
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
	 * 
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
	 * 
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
	 * 
	 * @return the lnsBillStatistics
	 */
	public LnsBillStatistics getLnsBillStatistics() {
		return lnsBillStatistics;
	}

	/**
	 * @param lnsBillStatistics the lnsBillStatistics to set
	 */
	public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
		this.lnsBillStatistics = lnsBillStatistics;
	}

	/**
	 * 
	 * @return the dealDate
	 */
	public String getDealDate() {
		return dealDate;
	}

	/**
	 * @param dealDate the dealDate to set
	 */
	public void setDealDate(String dealDate) {
		this.dealDate = dealDate;
	}

	/**
	 * 
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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

	

	
	
}

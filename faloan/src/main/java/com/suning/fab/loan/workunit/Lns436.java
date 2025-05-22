package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsrentreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanBillSettleInterestSupporter;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	SC
 *
 * @version V1.0.1
 *
 * @see 	汽车租赁预约还款查询
 *
 * @param	acctNo			（账号）借据号
 * 			endDate			预约还款日期
 *
 * @return	termPrin		正常应还金额
 * 			cleanTotal		正常结清金额
 *			userTermPrin	用户应还金额
 *			userCleanTotal	用户结清金额
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns436 extends WorkUnit {

	String acctNo;
	String endDate;

	FabAmount termPrin = new FabAmount();
	FabAmount cleanTotal = new FabAmount();
	FabAmount userTermPrin = new FabAmount();
	FabAmount userCleanTotal = new FabAmount();
	FabAmount overPrin = new FabAmount();
	FabAmount userOverPrin = new FabAmount();

	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();

		//参数校验
		if(VarChecker.isEmpty(endDate)){
			throw new FabException("LNS005");
		}
		//预约日期大于交易日
		if(CalendarUtil.before(endDate, ctx.getTranDate())){
			throw new FabException("LNS034");
		}

		//读取借据主文件
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());

		TblLnsbasicinfo lnsbasicinfo = null;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if (null == lnsbasicinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		//结息日为空就取起息日
		if(VarChecker.isEmpty(lnsbasicinfo.getLastintdate())){
			lnsbasicinfo.setLastintdate(lnsbasicinfo.getBeginintdate());
		}

		//根据账号生成协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		//生成还款计划
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, endDate, ctx);
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史账单
		billList.addAll(lnsBillStatistics.getHisBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
		billList.addAll(lnsBillStatistics.getBillInfoList());
		//未来期账单：从还款日到合同到期日之间的本金和利息账单
		billList.addAll(lnsBillStatistics.getFutureBillInfoList());

		//获取呆滞呆账期间新罚息复利账单list
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		hisLnsbill.addAll(lnsBillStatistics.getBillInfoList());
		hisLnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		hisLnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		
		ctx.setTranDate(endDate);
		List<TblLnsbill> cdbillList = genCintDintList(getAcctNo(), ctx,hisLnsbill);
		for( TblLnsbill tblLnsbill:cdbillList)
		{
			billList.add(BillTransformHelper.convertToLnsBill(tblLnsbill));
		}
//		String repayDate = endDate;(保留)
		//预约日期大于合同结束日期取合同结束日
//		if(CalendarUtil.after(endDate, lnsbasicinfo.getContduedate())){
//			endDate = lnsbasicinfo.getContduedate();
//		}
		
		//尾款计算
		//查询尾款
		//读取借据主文件
		Map<String,Object> finalaram = new HashMap<String,Object>();
		finalaram.put("acctno", acctNo);
		finalaram.put("brc", ctx.getBrc());
		//查询信息表
		TblLnsrentreg lnsrentreg;
		try {
			lnsrentreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrentreg", finalaram, TblLnsrentreg.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsrentreg");
		}

		if (null == lnsrentreg){
			throw new FabException("SPS104", "lnsrentreg");
		}
		//计算总息
		//租金本息
		BigDecimal rentAmt = new BigDecimal(lnsrentreg.getRsqmap().split("\\|")[2].split(":")[1]);
		//租金本金
		BigDecimal rentPrin = new BigDecimal(lnsrentreg.getRsqmap().split("\\|")[3].split(":")[1]);
		//租金总息
		BigDecimal rentInt = rentAmt.subtract(rentPrin);
		
		
		Double balance;
		try {
			balance = DbAccessUtil.queryForObject("CUSTOMIZE.query_sumbalance", finalaram, Double.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsdiscountaccount");
		}
		//不该算利息的期数
		int notIntCount  = 0;
		//期数
		int period = 0;
		//遍历list算出租金
		for(LnsBill lnsBill : billList){
			if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())){
				period++;
			}
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(lnsBill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE).contains(lnsBill.getSettleFlag())){
				continue;
			}else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())
						||!ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())){
					userCleanTotal.selfAdd(lnsBill.getBillBal());
				}
			}
			if((CalendarUtil.after(endDate, lnsBill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, lnsBill.getEndDate()))||CalendarUtil.before(lnsBill.getEndDate(),endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())
						||!ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())){
					userTermPrin.selfAdd(lnsBill.getBillBal());
				}
			}else if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())){
				if(lnsBill.getPeriod()==1 && endDate.equals(lnsBill.getStartDate()) && !lnsBill.getBillAmt().equals(lnsBill.getBillBal())){
					continue;
				}else if(!lnsBill.getBillAmt().equals(lnsBill.getBillBal())){
					continue;
				}
				notIntCount++;
			}
			
			if(CalendarUtil.beforeAlsoEqual(lnsBill.getEndDate(),endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())
						||!ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())){
					userOverPrin.selfAdd(lnsBill.getBillBal());
				}
			}
			
		}
		//每期利息
		BigDecimal rentTermInt = rentInt.divide(BigDecimal.valueOf(period), 6, BigDecimal.ROUND_HALF_UP);
		//计算已经结清利息的金额
		if(notIntCount != 0){
			BigDecimal intCount = BigDecimal.valueOf(period).subtract(BigDecimal.valueOf(notIntCount));
			rentTermInt = rentTermInt.multiply(intCount);
		}else{
			rentTermInt = rentInt;
		}
		//计算真实的结清金额
		userCleanTotal.selfSub(new FabAmount(rentInt.subtract(rentTermInt).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
		
		termPrin = new FabAmount(userTermPrin.getVal());
		cleanTotal = new FabAmount(userCleanTotal.getVal());
		overPrin = new FabAmount(userOverPrin.getVal());
		userTermPrin.selfSub(balance);
		userCleanTotal.selfSub(balance);
		userOverPrin.selfSub(balance);
		if(userTermPrin.getVal()<0){
			userTermPrin = new FabAmount(0.0);
		}
		if(userCleanTotal.getVal()<0){
			userCleanTotal = new FabAmount(0.0);
		}
		if(userOverPrin.getVal()<0){
			userOverPrin = new FabAmount(0.0);
		}
	}
	
	public List<TblLnsbill> genCintDintList(String acctNo, TranCtx ctx,List<LnsBill> hisLnsbill) throws FabException{
		//定义返回呆滞呆账罚息复利账单list
		List<TblLnsbill> cdBillList = new ArrayList<TblLnsbill>();	
		//获取贷款协议信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		

		//循环处理呆滞呆账状态的本金和利息账单
		for (LnsBill lnsHisbill : hisLnsbill) {
			//首先处理呆滞呆账本金产生 罚息账单
				
			if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
				
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), ctx.getTranDate());
				if(famt == null || famt.isZero())
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				tblLnsbill.setPeriod(lnsHisbill.getPeriod());
				tblLnsbill.setBillamt(famt.getVal());
				tblLnsbill.setBillbal(famt.getVal());
				tblLnsbill.setPrinbal(lnsHisbill.getBillBal().getVal());
				tblLnsbill.setBillrate(la.getRateAgreement().getOverdueRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsHisbill.getIntendDate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsHisbill.getRepayWay());
				tblLnsbill.setCcy(lnsHisbill.getCcy());
				tblLnsbill.setDertrandate(lnsHisbill.getTranDate());
				tblLnsbill.setDerserseqno(lnsHisbill.getSerSeqno());
				tblLnsbill.setDertxseq(lnsHisbill.getTxSeq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsHisbill.getIntendDate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}

				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				
				cdBillList.add(tblLnsbill);
			}
			
			//处理呆滞利息产生呆滞复利账单
			if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) ) {
				//当前工作日期减复利记至日期得到计复利天数
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getCompoundRate(), ctx.getTranDate());
				if(famt == null || famt.isZero())
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_CINT);
				tblLnsbill.setPeriod(lnsHisbill.getPeriod());
				tblLnsbill.setBillamt(famt.getVal());
				tblLnsbill.setBillbal(famt.getVal());
				tblLnsbill.setPrinbal(lnsHisbill.getBillBal().getVal());
				tblLnsbill.setBillrate(la.getRateAgreement().getCompoundRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsHisbill.getIntendDate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsHisbill.getRepayWay());
				tblLnsbill.setCcy(lnsHisbill.getCcy());
				tblLnsbill.setDertrandate(lnsHisbill.getTranDate());
				tblLnsbill.setDerserseqno(lnsHisbill.getSerSeqno());
				tblLnsbill.setDertxseq(lnsHisbill.getTxSeq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsHisbill.getIntendDate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}
				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				
				cdBillList.add(tblLnsbill);
			}
	
		}	
		
		return cdBillList;
	}

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
	 * @return the endDate
	 */
	public String getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the termPrin
	 */
	public FabAmount getTermPrin() {
		return termPrin;
	}

	/**
	 * @param termPrin the termPrin to set
	 */
	public void setTermPrin(FabAmount termPrin) {
		this.termPrin = termPrin;
	}

	/**
	 * @return the cleanTotal
	 */
	public FabAmount getCleanTotal() {
		return cleanTotal;
	}

	/**
	 * @param cleanTotal the cleanTotal to set
	 */
	public void setCleanTotal(FabAmount cleanTotal) {
		this.cleanTotal = cleanTotal;
	}

	/**
	 * @return the userTermPrin
	 */
	public FabAmount getUserTermPrin() {
		return userTermPrin;
	}

	/**
	 * @param userTermPrin the userTermPrin to set
	 */
	public void setUserTermPrin(FabAmount userTermPrin) {
		this.userTermPrin = userTermPrin;
	}

	/**
	 * @return the userCleanTotal
	 */
	public FabAmount getUserCleanTotal() {
		return userCleanTotal;
	}

	/**
	 * @param userCleanTotal the userCleanTotal to set
	 */
	public void setUserCleanTotal(FabAmount userCleanTotal) {
		this.userCleanTotal = userCleanTotal;
	}

	/**
	 * @return the overPrin
	 */
	public FabAmount getOverPrin() {
		return overPrin;
	}

	/**
	 * @param overPrin the overPrin to set
	 */
	public void setOverPrin(FabAmount overPrin) {
		this.overPrin = overPrin;
	}

	/**
	 * @return the userOverPrin
	 */
	public FabAmount getUserOverPrin() {
		return userOverPrin;
	}

	/**
	 * @param userOverPrin the userOverPrin to set
	 */
	public void setUserOverPrin(FabAmount userOverPrin) {
		this.userOverPrin = userOverPrin;
	}


}

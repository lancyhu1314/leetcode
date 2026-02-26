package com.suning.fab.loan.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.VarChecker;

public class LoanBillFefureSupporter {
	private LoanBillFefureSupporter() {
		// nothing to do
	}

	/**
	 * 未来期逾期本金利息结息
	 * 
	 * @param loanAgreement
	 * @param repayDate
	 * @return 账单明细
	 * @throws Exception
	 * @throws SecurityException
	 */
	public static List<LnsBill> genLoanFutureBillDetail(
			LoanAgreement loanAgreement, LnsBillStatistics billInfoAll,
			String repayDate) throws FabException {
		//本金已结息 展期  补记展期利息
		List<LnsBill> billDetail = new ArrayList<>();
		for(LnsBill lnsBill:billInfoAll.getHisBillList()){
			if(isPatchNint(loanAgreement,lnsBill))
				billDetail.add(patchNint(loanAgreement, repayDate,lnsBill));
		}
		billDetail.addAll(LoanBillSettleInterestSupporter
				.genLoanFutureBillDetail(loanAgreement, repayDate));
		for (LnsBill lnsBill : billDetail) {
			// 未到账单结束日期，不生成账单
			if (CalendarUtil.before(repayDate, lnsBill.getEndDate())) {
				addFutureBill(billInfoAll, lnsBill, loanAgreement);
				continue;
			}
			addBill(billInfoAll, lnsBill, loanAgreement);

			// 本金账单计息
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
					.contains(lnsBill.getBillType())
					&& CalendarUtil.before(lnsBill.getIntendDate(), repayDate)) {

				List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
						.hisPrinBillSettleInterest(loanAgreement, billInfoAll,
								lnsBill, repayDate);

				for (LnsBill cfIntBill : lnsBillList) {
					// 修改历史账单利息记至日期
					lnsBill.setIntendDate(cfIntBill.getEndDate());
					// 登记罚息复利
					if (!cfIntBill.getBillAmt().isZero()) {
						addFutureSetIntBill(billInfoAll, cfIntBill);
						LoanBillStatisticsSupporter.billStatistics(billInfoAll,
								cfIntBill);
					}
				}

			}
			// 利息账单结息
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
					.contains(lnsBill.getBillType())
					&& CalendarUtil.before(lnsBill.getIntendDate(), repayDate)) {

				List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
						.hisIntBillSettleInterest(loanAgreement, billInfoAll,
								lnsBill, repayDate);

				for (LnsBill cfIntBill : lnsBillList) {
					// 修改历史账单利息记至日期
					lnsBill.setIntendDate(cfIntBill.getEndDate());
					// 登记罚息复利
					if (!cfIntBill.getBillAmt().isZero()) {
						addFutureSetIntBill(billInfoAll, cfIntBill);
						LoanBillStatisticsSupporter.billStatistics(billInfoAll,
								cfIntBill);
					}
				}

			}//算违费用的约金
			else if(LoanFeeUtils.isFeeType(lnsBill.getBillType())
					&& ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag())){
				List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
						.hisFeeBillSettlePenalty(loanAgreement,
								lnsBill, repayDate);
				for (LnsBill cfIntBill : lnsBillList) {
					cfIntBill.setHisBill(lnsBill);
					addFutureSetIntBill(billInfoAll, cfIntBill);
				}
			}

		}
		return billDetail;

	}

	 static LnsBill patchNint(LoanAgreement loanAgreement, String repayDate,LnsBill hisBill) {
		//为了试算，暂时添加
		loanAgreement.getContract().getBalance().selfAdd(hisBill.getBillBal());
		LnsBill intBill = new LnsBill();
		intBill.setPeriod(hisBill.getPeriod());//期数
		intBill.setStartDate(hisBill.getIntendDate());


		intBill.setEndDate(hisBill.getCurendDate());
		intBill.setDerTranDate(hisBill.getTranDate());//账务日期
		intBill.setDerSerseqno(hisBill.getSerSeqno());//流水号
		intBill.setDerTxSeq(hisBill.getTxSeq());//子序号
		intBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);//账单状态N正常G宽限期O逾期L呆滞B呆账
		intBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET);//账单属性INTSET正常结息REPAY还款
		intBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);//利息入账标志NO未入YES已入
		intBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);//账单作废标志NORMAL正常CANCEL作废
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
			intBill.setCancelFlag("3");
		}

		intBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);//结清标志RUNNING未结CLOSE已结
		intBill.setPrinBal(new FabAmount(loanAgreement.getContract().getBalance().getVal()));

		//计算利息

		BigDecimal interestval =  new BigDecimal(CalendarUtil.fixedDaysBetween(hisBill.getStartDate(), intBill.getEndDate(), loanAgreement.getInterestAgreement().getIntBase()))
				.multiply(loanAgreement.getRateAgreement().getNormalRate().getDayRate())
				.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal())).setScale(2,BigDecimal.ROUND_HALF_UP);

		//计算已结利息
		BigDecimal overInterest =  new BigDecimal(CalendarUtil.fixedDaysBetween(hisBill.getStartDate(), intBill.getStartDate(), loanAgreement.getInterestAgreement().getIntBase()))
				.multiply(loanAgreement.getRateAgreement().getNormalRate().getDayRate())
				.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal())).setScale(2,BigDecimal.ROUND_HALF_UP);
		FabAmount interest =  new FabAmount();
		//总利息减去已结利息
		interest.selfAdd(interestval.subtract(overInterest).doubleValue());
		interest = getCapInterestNoRepayDate(interest,loanAgreement);

		intBill.setBillAmt(interest);
		intBill.setBillBal(intBill.getBillAmt());

		//账单开始日期
		intBill.setStatusbDate(intBill.getStartDate());
		//币种
		intBill.setCcy(intBill.getBillAmt().getCurrency().getCcy());
		intBill.setCurendDate(intBill.getEndDate());
		//本期应还款日
		intBill.setRepayendDate(CalendarUtil.nDaysAfter(intBill.getCurendDate(), loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
		//利息计止日期
		intBill.setIntendDate(intBill.getCurendDate());
		//账单利息
		intBill.setBillRate(loanAgreement.getRateAgreement().getNormalRate());
		intBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
		//现金贷免息
		//判断免息金额是否是空并且是不是正值
		if(!VarChecker.isEmpty(loanAgreement.getBasicExtension().getFreeInterest())
				&& loanAgreement.getBasicExtension().getFreeInterest().isPositive()){
			//如果免息金额大于这期利息的时候
			if(loanAgreement.getBasicExtension().getFreeInterest().sub(interest).isPositive()){
				intBill.setBillBal(new FabAmount(0.0));
				intBill.setBillAmt(new FabAmount(0.0));
				loanAgreement.getBasicExtension().getFreeInterest().selfSub(interest);
			}else{
				intBill.setBillBal(new FabAmount(interest.sub(loanAgreement.getBasicExtension().getFreeInterest()).getVal()));
				intBill.setBillAmt(new FabAmount(interest.sub(loanAgreement.getBasicExtension().getFreeInterest()).getVal()));
				loanAgreement.getBasicExtension().setFreeInterest(new FabAmount(0.0));
			}
		}

		//计算每天利息并四舍五入
		BigDecimal repayDateInt = loanAgreement.getRateAgreement().getNormalRate().getDayRate()
				.multiply(new BigDecimal(loanAgreement.getContract().getBalance().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
		repayDateInt = repayDateInt.multiply(new BigDecimal(CalendarUtil.actualDaysBetween(intBill.getStartDate(), repayDate)));
		if(new FabAmount(repayDateInt.doubleValue()).sub(intBill.getBillAmt()).isPositive())
			intBill.setRepayDateInt(intBill.getBillAmt());
		else
			intBill.setRepayDateInt(new FabAmount(repayDateInt.doubleValue()));

		//现金贷免息
		//到当天应记利息（当当期只有部分利息被免除）
		if(!VarChecker.isEmpty(loanAgreement.getBasicExtension().getFreeInterest())
				&& !interest.equals(intBill.getBillBal())
				){
			//未来期账单试算里面正常情况下interest不会不等于billbal，所以免息的当前情况下只用判断interest和billbal之间的差值就可以判断当期免息金额
			FabAmount termFreeInterest = new FabAmount(interest.sub(intBill.getBillBal()).getVal());
			//不包含等本等息的情况
			/*if(ConstantDeclare.REPAYWAY.REPAYWAY_DBDX.equals(loanAgreement.getWithdrawAgreement().getRepayWay())){
				intBill.setTermFreeInterest(termFreeInterest);
				intBill.setRepayDateInt(new FabAmount(intBill.getBillBal().sub(termFreeInterest).getVal()>0?intBill.getBillBal().sub(termFreeInterest).getVal() : 0.0));
			}
			else*/
			if( !VarChecker.isEmpty(intBill.getRepayDateInt())) {
				if (!termFreeInterest.sub(intBill.getRepayDateInt()).isNegative()) {
					intBill.setTermFreeInterest(intBill.getRepayDateInt());
					intBill.setRepayDateInt(new FabAmount(0.0));
				} else {
					intBill.setTermFreeInterest(termFreeInterest);
					intBill.setRepayDateInt(new FabAmount(intBill.getRepayDateInt().sub(termFreeInterest).getVal()));
				}
			}
		}
		 intBill.setHisBill(hisBill);
		//账单期数
		loanAgreement.getContract().setRepayIntDate(intBill.getEndDate());

		//封顶计息特殊判断20181228历史收益累加周期利息
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate()) ){
			loanAgreement.getBasicExtension().getHisComein().selfAdd(interest);
		}

		loanAgreement.getContract().getBalance().selfSub(hisBill.getBillBal());

		return intBill;
	}
	//封顶计息
	public static FabAmount getCapInterestNoRepayDate(FabAmount currInterest,LoanAgreement loanAgreement) {
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
			if(!currInterest.add(loanAgreement.getBasicExtension().getHisComein()).
					sub(new FabAmount(loanAgreement.getBasicExtension().getCapAmt().getVal())).isNegative()){
				FabAmount result = new FabAmount((loanAgreement.getBasicExtension().getCapAmt().getVal())
						-loanAgreement.getBasicExtension().getHisComein().getVal());
				if(result.isNegative()){
					return new FabAmount(0.0);
				}
				return result;
			}
		}
		return currInterest;
	}
	//判断是否要补利息  为延期还款的补利息（延期的那一期 本金已经落表）
	 static boolean isPatchNint(LoanAgreement loanAgreement, LnsBill hisBill) {
		if(hisBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
				&&hisBill.getCurendDate().equals(loanAgreement.getBasicExtension().getTermEndDate())
				&&CalendarUtil.before(hisBill.getIntendDate(), hisBill.getCurendDate())) {
			//不增记利息的  需要更改上次结息日
			//等本等息 每月利息一样
			if (ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()))
			{
				if(CalendarUtil.before(loanAgreement.getContract().getRepayIntDate(), hisBill.getCurendDate()))
					loanAgreement.getContract().setRepayIntDate(hisBill.getCurendDate());
				return false;
			}
//			//等额本息 每月利息一样（除了首尾期数）
//			if (ConstantDeclare.REPAYWAY.REPAYWAY_DEBX.equals(loanAgreement.getWithdrawAgreement().getRepayWay()))
//			{
//				if(hisBill.getStartDate().equals(loanAgreement.getContract().getStartIntDate())
//						|| hisBill.getEndDate().equals(loanAgreement.getContract().getContractEndDate()))
//				{
//					return true;
//				}else
//				{
//					loanAgreement.getContract().setRepayIntDate(hisBill.getCurendDate());
//					return false;
//				}
//			}
			return true;
		}
		return 	false;
	}
	/**
	 * 登记账单
	 * 
	 * @param LnsBillStatistics
	 * @param LnsBill
	 * 
	 */
	private static void addBill(LnsBillStatistics lnsBillInfoAll,
			LnsBill lnsBill, LoanAgreement la) {
		if (checkZeroNintBill(lnsBill,la)) {
			lnsBillInfoAll.setBillNo(lnsBillInfoAll.getBillNo() + 1);
			lnsBill.setTxSeq(lnsBillInfoAll.getBillNo());
			LoanBillStatisticsSupporter.billStatistics(lnsBillInfoAll, lnsBill);
			lnsBillInfoAll.getBillInfoList().add(lnsBill);
		}
		
	}
	
	//原来的+零利率+现金贷免息的判断（sonar问题解决）
	private static boolean checkZeroNintBill(LnsBill lnsBill, LoanAgreement la) {
		if (lnsBill.getBillAmt().isPositive()){	//2018-12-07 零利率利息账本
			return true;    
		}
		if(( lnsBill.getBillAmt().isZero() || 
		   new FabAmount(la.getRateAgreement().getNormalRate().getYearRate().doubleValue()).sub(0.00).isZero() )  &&
		   Arrays.asList("3","4","5","6","7").contains(la.getWithdrawAgreement().getRepayWay())){
			return true; 
		}
	   if(!VarChecker.isEmpty(la.getBasicExtension().getFreeInterest())){
		   return true; 
		}
	   if(!VarChecker.isEmpty(la.getBasicExtension().getFreeFee())&&la.getBasicExtension().getFreeFee().size()>0){
	   	return true;
	   }

	   return false;
	}

	/**
	 * 登记未来期账单：从还款日到合同到期日之间的账单
	 * 
	 * @param LnsBillStatistics
	 * @param LnsBill
	 * 
	 */
	private static void addFutureBill(LnsBillStatistics lnsBillInfoAll,
			LnsBill lnsBill, LoanAgreement la) {
		if (checkZeroNintBill(lnsBill,la)) {
			lnsBillInfoAll.setBillNo(lnsBillInfoAll.getBillNo() + 1);
			lnsBill.setTxSeq(lnsBillInfoAll.getBillNo());
			LoanBillStatisticsSupporter.billStatistics(lnsBillInfoAll, lnsBill);
			lnsBillInfoAll.getFutureBillInfoList().add(lnsBill);
		}
	}

	/**
	 * 登记未结清结息账单
	 * 
	 * @param LnsBillStatistics
	 * @param LnsBill
	 * 
	 */
	private static void addFutureSetIntBill(LnsBillStatistics lnsBillInfoAll,
			LnsBill lnsBill) {
		// 累加账单序号
		if (lnsBill.getBillAmt().isPositive()
				|| "AMLT".equals(lnsBill.getBillType())) {
			lnsBillInfoAll.setBillNo(lnsBillInfoAll.getBillNo() + 1);
			lnsBill.setTxSeq(lnsBillInfoAll.getBillNo());
			LoanBillStatisticsSupporter.billStatistics(lnsBillInfoAll, lnsBill);
			lnsBillInfoAll.getFutureOverDuePrinIntBillList().add(lnsBill);
		}
	}
}

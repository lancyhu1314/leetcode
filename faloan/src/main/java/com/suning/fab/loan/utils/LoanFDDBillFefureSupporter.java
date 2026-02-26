package com.suning.fab.loan.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.VarChecker;

public class LoanFDDBillFefureSupporter {
	private LoanFDDBillFefureSupporter() {
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
		
		FabAmount sumPrin = new FabAmount(0.0);
		String endDate = "2000-01-01";
		//封顶计息20190116
		//循环历史账本累计已结利息、已结罚息（房抵贷不存在复利）、账本的剩余本金
		for(LnsBill lnsBill : billInfoAll.getHisBillList()){
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
					.contains(lnsBill.getBillType())){
				loanAgreement.getBasicExtension().setCalDintComein(new FabAmount(loanAgreement.getBasicExtension().getCalDintComein()
						.selfAdd(lnsBill.getBillAmt()).getVal()) );
			}
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
					.contains(lnsBill.getBillType())){
				//计算已计未结罚息
				loanAgreement.getBasicExtension().getCalOldDintComein().selfAdd(lnsBill.getBillAmt());
				endDate = CalendarUtil.after(endDate, lnsBill.getEndDate())?endDate:lnsBill.getEndDate();
			}
			if( lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
				sumPrin.selfAdd(lnsBill.getBillBal());
			}
		}
		//所有未还本金：账本剩余本金+合同余额
		sumPrin.selfAdd(loanAgreement.getContract().getBalance());
		//未结罚息：已计提罚息-已结罚息
		loanAgreement.getBasicExtension().setCalOldDintComein(new FabAmount(loanAgreement.getBasicExtension().getHisDintComein().sub(loanAgreement.getBasicExtension().getCalOldDintComein()).getVal()));

		//已计罚息+已结利息
		loanAgreement.getBasicExtension().setCalDintComein(new FabAmount(loanAgreement.getBasicExtension().getCalDintComein()
				.selfAdd(loanAgreement.getBasicExtension().getHisDintComein()).getVal()) );
		loanAgreement.getBasicExtension().setHisComein(new FabAmount(loanAgreement.getBasicExtension().getCalDintComein().getVal()));
		List<LnsBill> billDetail = new ArrayList<>();
		for(LnsBill lnsBill:billInfoAll.getHisBillList()){
			if(LoanBillFefureSupporter.isPatchNint(loanAgreement,lnsBill))
				billDetail.add(LoanBillFefureSupporter.patchNint(loanAgreement, repayDate,lnsBill));
		}
		billDetail.addAll(LoanBillSettleInterestSupporter
				.genLoanFutureBillDetail(loanAgreement, repayDate));

		//20190115封顶计息到当天利息+已计罚息+已结利息
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
			for(LnsBill lnsBill : billDetail){
				if(!VarChecker.isEmpty(lnsBill.getRepayDateInt())
						&&!LoanFeeUtils.isFeeType(lnsBill.getBillType())){
					loanAgreement.getBasicExtension().getCalDintComein().selfAdd(lnsBill.getRepayDateInt()).getVal();
				}
			}
		}
		loanAgreement.getBasicExtension().setSumPrin(sumPrin);
		//罚息还款计划展示问题
		
		//历史期本金利息账单计息
		//判断封顶计息标志
		//逾期本金期数
		Boolean overduePrinTerm = false;
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())
				//房抵贷的罚息 只算第一期
//				||"2412615".equals(loanAgreement.getPrdId())){
				|| (VarChecker.asList("2412615","4010002").contains(loanAgreement.getPrdId())) ){
			
			for (LnsBill lnsBill : billInfoAll.getHisBillList()) {
				
				if(!VarChecker.asList("2412615","4010002").contains(loanAgreement.getPrdId())){
					// 本金结息
					if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
							.contains(lnsBill.getBillType())) {
						if (ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag())){
							List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
									.hisPrinBillSettleInterest(loanAgreement, billInfoAll,
											lnsBill, repayDate);
							for (LnsBill cfIntBill : lnsBillList) {
								cfIntBill.setHisBill(lnsBill);
			
								addNoSetIntBill(billInfoAll, cfIntBill);
							}
							overduePrinTerm = true;
						}
		
					}
				}else{
					// 本金结息
					if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
							.contains(lnsBill.getBillType()) || VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
									.contains(lnsBill.getBillType())) {
						if (ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag())
								&& !overduePrinTerm && sumPrin.isPositive()){
							//优化到期日
							lnsBill.setIntendDate(CalendarUtil.after(endDate,lnsBill.getIntendDate() )?endDate:lnsBill.getIntendDate());
							List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
									.hisCapBillSettleInterest(loanAgreement, billInfoAll,
											lnsBill, repayDate);
							for (LnsBill cfIntBill : lnsBillList) {
								cfIntBill.setHisBill(lnsBill);
			
								addNoSetIntBill(billInfoAll, cfIntBill);
							}
							overduePrinTerm = true;
						}
		
					}
				}
			}
		}
		
		for (LnsBill lnsBill : billDetail) {
			// 未到账单结束日期，不生成账单
			if (CalendarUtil.before(repayDate, lnsBill.getEndDate())) {
				addFutureBill(billInfoAll, lnsBill, loanAgreement);
				continue;
			}
			addBill(billInfoAll, lnsBill, loanAgreement);

			// 本金账单计息
			/**
			 * 封顶计息功能上之前的数据--老数据
			 * 老数据开户不支持先息后本还款方式
			 * 新数据先息后本罚息账本要求在利息账本下
			 * 20190509|14050183
			 */
			if(!loanAgreement.getContract().getFlag1().contains("C")){
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
						.contains(lnsBill.getBillType())
						&& CalendarUtil.before(lnsBill.getIntendDate(), repayDate)) {
					//判断封顶计息特殊判断20190116
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
			}else{
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
						.contains(lnsBill.getBillType()) || VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
								.contains(lnsBill.getBillType())) {
					if (CalendarUtil.before(lnsBill.getIntendDate(), repayDate)
							&& !overduePrinTerm) {
						//判断封顶计息特殊判断20190116
						List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
								.hisCapBillSettleInterest(loanAgreement, billInfoAll,
										lnsBill, repayDate);

						for (LnsBill cfIntBill : lnsBillList) {
							// 修改历史账单利息记至日期
							if(!VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
									.contains(lnsBill.getBillType())){
								lnsBill.setIntendDate(cfIntBill.getEndDate());
							}
							// 登记罚息复利
							if (!cfIntBill.getBillAmt().isZero()) {
								addFutureSetIntBill(billInfoAll, cfIntBill);
								LoanBillStatisticsSupporter.billStatistics(billInfoAll,
										cfIntBill);
							}
						}
						overduePrinTerm = true;
					}
				}
				
			}
			
		}
		return billDetail;

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
		if (lnsBill.getBillAmt().isPositive() ||    //2018-12-07 零利率利息账本
			(( lnsBill.getBillAmt().isZero() || 
			   new FabAmount(la.getRateAgreement().getNormalRate().getYearRate().doubleValue()).sub(0.00).isZero() )  &&
			   Arrays.asList("1","3","4","5","6","7").contains(la.getWithdrawAgreement().getRepayWay()))) {
			lnsBillInfoAll.setBillNo(lnsBillInfoAll.getBillNo() + 1);
			lnsBill.setTxSeq(lnsBillInfoAll.getBillNo());
			LoanBillStatisticsSupporter.billStatistics(lnsBillInfoAll, lnsBill);
			lnsBillInfoAll.getBillInfoList().add(lnsBill);
		}
		
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
		if (lnsBill.getBillAmt().isPositive() ||    //2018-12-07 零利率利息账本
			(( lnsBill.getBillAmt().isZero() || 
			   new FabAmount(la.getRateAgreement().getNormalRate().getYearRate().doubleValue()).sub(0.00).isZero() )  &&
			   Arrays.asList("1","3","4","5","6","7").contains(la.getWithdrawAgreement().getRepayWay()))) {
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
	
	/**
	 * 登记未结清结息账单
	 * 
	 * @param LnsBillStatistics
	 * @param LnsBill
	 * 
	 */
	private static void addNoSetIntBill(LnsBillStatistics lnsBillInfoAll,
			LnsBill lnsBill) {
		// 累加账单序号
		if (lnsBill.getBillAmt().isPositive()
				|| "AMLT".equals(lnsBill.getBillType())) {
			lnsBillInfoAll.setBillNo(lnsBillInfoAll.getBillNo() + 1);
			lnsBill.setTxSeq(lnsBillInfoAll.getBillNo());
			LoanBillStatisticsSupporter.billStatistics(lnsBillInfoAll, lnsBill);
			lnsBillInfoAll.getHisSetIntBillList().add(lnsBill);
		}
	}
	
	public FabAmount getCapInterest(FabAmount currInterest,String repayDate,LoanAgreement loanAgreement) {
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate()) 
				&& CalendarUtil.after(repayDate, loanAgreement.getBasicExtension().getComeinDate())
				){
			if(!currInterest.add(loanAgreement.getBasicExtension().getHisComein()).
					sub(new FabAmount(loanAgreement.getContract().getContractAmt().getVal()
							*loanAgreement.getInterestAgreement().getCapRate())).isNegative()){
				FabAmount result = new FabAmount((loanAgreement.getContract().getContractAmt().getVal()
						*loanAgreement.getInterestAgreement().getCapRate())
						-loanAgreement.getBasicExtension().getHisComein().getVal());
//				loanAgreement.getBasicExtension().setHisComein(new FabAmount(loanAgreement.getContract().getContractAmt().getVal()
//						*loanAgreement.getInterestAgreement().getCapRate()));
				return result;
			}else{
//				loanAgreement.getBasicExtension().getHisComein().selfAdd(currInterest);
				return null;
			}
		}
		return null;
	}
}

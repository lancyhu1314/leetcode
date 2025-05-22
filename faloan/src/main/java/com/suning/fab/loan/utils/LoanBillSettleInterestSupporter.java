package com.suning.fab.loan.utils;

import com.suning.fab.loan.account.IntegerNum;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.LoanFormInfo;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblStatutorydays;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.RepayFormulaSupporter;
import com.suning.fab.loan.supporter.RepayFormulaSupporterProducter;
import com.suning.fab.loan.supporter.SettleInterestSupporter;
import com.suning.fab.loan.supporter.SettleInterestSupporterProducter;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LoanBillSettleInterestSupporter {



	private LoanBillSettleInterestSupporter(){
		//nothing to do
	}

	/**
	 * 生成未来期账单
	 * @param loanAgreement
	 * @param repayDate
	 * @return 账单明细
	 * @throws Exception
	 * @throws SecurityException
	 */
	public static List<LnsBill> genLoanFutureBillDetail(LoanAgreement loanAgreement ,  String repayDate) throws FabException
	{
		List<LnsBill> billDetail = new ArrayList<LnsBill>();

		if (VarChecker.isEmpty(loanAgreement.getContract().getStartIntDate()))
		{
			loanAgreement.getContract().setStartIntDate(loanAgreement.getContract().getContractStartDate());
		}


		if (VarChecker.isEmpty(loanAgreement.getContract().getRepayIntDate()))
		{
			loanAgreement.getContract().setRepayIntDate(loanAgreement.getContract().getStartIntDate());
		}
		if (VarChecker.isEmpty(loanAgreement.getContract().getRepayPrinDate()))
		{
			loanAgreement.getContract().setRepayPrinDate(loanAgreement.getContract().getContractStartDate());
		}

		//结息标志为空，默认为结息
		if ( VarChecker.isEmpty(loanAgreement.getInterestAgreement().getIsCalInt()))
		{
			loanAgreement.getInterestAgreement().setIsCalInt(ConstantDeclare.ISCALINT.ISCALINT_YES);
		}

		//贷款剩余本金为零，不再计息;结本日等于合同结束日期，不再计息
		if (loanAgreement.getContract().getBalance().isZero()
				|| CalendarUtil.equalDate(loanAgreement.getContract().getContractEndDate(), loanAgreement.getContract().getRepayPrinDate()))
		{
			if(!loanAgreement.getFeeAgreement().isEmpty()) {
				RepayFormulaSupporter loanSupporter =RepayFormulaSupporterProducter.producter(loanAgreement);

				String periodEndDate = loanAgreement.getContract().getContractEndDate();

				FeeBillSettle(loanAgreement, repayDate, billDetail, loanSupporter, periodEndDate);
			}
			return billDetail;
		}

		/*RepayFormulaSupporter loanSupporter =LoanSupporterUtil.getFormulaSupporter(loanAgreement.getInterestAgreement().getIntFormula());
		loanSupporter.setLoanAgreement(loanAgreement);*/

		//不用重复试算总期数 20191203
//		IntegerNum totalperiod = new IntegerNum();
		if (ConstantDeclare.ISCALINT.ISCALINT_NO.equals(loanAgreement.getInterestAgreement().getIsCalInt()))
		{
			while(true)
			{

				RepayFormulaSupporter loanSupporter =RepayFormulaSupporterProducter.producter(loanAgreement);
//				loanSupporter.setLoanAgreement(loanAgreement);
				LoggerUtil.info("loanSupporter:"+loanSupporter);
				LnsBill prinBill = settlePrin(loanAgreement,loanSupporter);

				//费用
				if(!loanAgreement.getFeeAgreement().isEmpty()) {

					FeeBillSettle(loanAgreement, repayDate, billDetail, loanSupporter, prinBill.getEndDate());
				}

				loanAgreement.getContract().setRepayPrinDate(prinBill.getEndDate());
				loanAgreement.getContract().setBalance(prinBill.getBalance());

				/*prinBill.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
				//当前期数加一
				if (RepayPeriodSupporter.isCumulativeIntPeriod(loanAgreement.getWithdrawAgreement().getRepayWay()))
				{
					loanAgreement.getContract().setCurrIntPeriod(loanAgreement.getContract().getCurrIntPeriod()+1);
				}*/
				//账单期数
				prinBill.setPeriod(loanSupporter.calCurrPrinPeriod());

				billDetail.add(prinBill);
//				//等本等息不减一
//				if(!loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_EQPRINEQINT)
//						&&!VarChecker.isEmpty(totalperiod.getNum()))
//					//本金少了一起  总期数减一
//					totalperiod.setNum(totalperiod.getNum()-1);
				if (prinBill.getBalance().isZero())
				{
					break;
				}
			}
		}
		else
		{

			while(true)
			{
				RepayFormulaSupporter loanSupporter =RepayFormulaSupporterProducter.producter(loanAgreement);
//					loanSupporter.setLoanAgreement(loanAgreement);
				LoggerUtil.debug("loanSupporter:"+loanSupporter);
				LnsBill prinBill = settlePrin(loanAgreement,loanSupporter);

				LnsBill intBill = settleInterest(loanAgreement,loanSupporter);


				if(!loanAgreement.getFeeAgreement().isEmpty()) {

					String periodEndDate = CalendarUtil.before(prinBill.getEndDate(),intBill.getEndDate() )?prinBill.getEndDate():intBill.getEndDate();

					FeeBillSettle(loanAgreement, repayDate, billDetail, loanSupporter, periodEndDate);
				}


				//未到结本日，正常结息
				if (CalendarUtil.beforeAlsoEqual(intBill.getEndDate(), prinBill.getEndDate()))
				{
					//计算利息
					FabAmount  interest = loanSupporter.getCurrInterest(intBill.getStartDate(),intBill.getEndDate());

					//账速融接账务核心特殊处理,到期日不处理  2019-02-14
					if( "3010014".equals(loanAgreement.getPrdId()) &&
							ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(intBill.getBillType()) &&
							intBill.getStartDate().equals(intBill.getEndDate()))
						interest.setVal(0.00);

					intBill.setBillAmt(interest);
					intBill.setBillBal(interest);
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
					//计算开始日期到还款日利息
					intBill.setRepayDateInt(loanSupporter.getRepayDateInterest(intBill.getStartDate(),intBill.getEndDate(),repayDate));
					//现金贷免息
					//到当天应记利息（当当期只有部分利息被免除）
					if(!VarChecker.isEmpty(loanAgreement.getBasicExtension().getFreeInterest())
							&& !interest.equals(intBill.getBillBal())
							){
						//未来期账单试算里面正常情况下interest不会不等于billbal，所以免息的当前情况下只用判断interest和billbal之间的差值就可以判断当期免息金额
						FabAmount termFreeInterest = new FabAmount(interest.sub(intBill.getBillBal()).getVal());
						if(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay())){
							intBill.setTermFreeInterest(termFreeInterest);
							intBill.setRepayDateInt(new FabAmount(intBill.getBillBal().sub(termFreeInterest).getVal()>0?intBill.getBillBal().sub(termFreeInterest).getVal() : 0.0));
						}
						else if( !VarChecker.isEmpty(intBill.getRepayDateInt())) {
							if (!termFreeInterest.sub(intBill.getRepayDateInt()).isNegative()) {
								intBill.setTermFreeInterest(intBill.getRepayDateInt());
								intBill.setRepayDateInt(new FabAmount(0.0));
							} else {
								intBill.setTermFreeInterest(termFreeInterest);
								intBill.setRepayDateInt(new FabAmount(intBill.getRepayDateInt().sub(termFreeInterest).getVal()));
							}
						}
					}
					//当前期数加一
						/*intBill.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
						if (RepayPeriodSupporter.isCumulativeIntPeriod(loanAgreement.getWithdrawAgreement().getRepayWay()))
						{
							loanAgreement.getContract().setCurrIntPeriod(loanAgreement.getContract().getCurrIntPeriod()+1);
						}*/
					//账单期数
					intBill.setPeriod(loanSupporter.calCurrIntPeriod());
					//更新 费用期数
					if(!billDetail.isEmpty()){
						LnsBill lnsBill = billDetail.get(billDetail.size()-1);
						if(LoanFeeUtils.isFeeType(lnsBill.getBillType())&&lnsBill.getEndDate().equals(intBill.getEndDate())){
							lnsBill.setPeriod(intBill.getPeriod());
						}
					}



					billDetail.add(intBill);

					loanAgreement.getContract().setRepayIntDate(intBill.getEndDate());

					//封顶计息特殊判断20181228历史收益累加周期利息
					if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate()) ){
						loanAgreement.getBasicExtension().getHisComein().selfAdd(interest);
					}


				}

				//本金
				if (CalendarUtil.equalDate(loanAgreement.getContract().getRepayIntDate(), prinBill.getEndDate()))
				{
					//add 20180712
					if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail())){
						loanAgreement.getContract().setRepayIntDate(CalendarUtil.nDaysAfter(intBill.getEndDate(), 1).toString("yyyy-MM-dd"));
						loanAgreement.getContract().setRepayPrinDate(CalendarUtil.nDaysAfter(prinBill.getEndDate(), 1).toString("yyyy-MM-dd"));
					}else{
						loanAgreement.getContract().setRepayPrinDate(prinBill.getEndDate());
					}

					//loanAgreement.getContract().setRepayPrinDate(prinBill.getEndDate());
					loanAgreement.getContract().setBalance(prinBill.getBalance());

					/*//利息账单生成时用利息期数，未有利息账单时取贷款协议中的利息期数
					if (intBill.getPeriod() != null)
					{

						prinBill.setPeriod(intBill.getPeriod());
					}
					else
					{
						prinBill.setPeriod(loanAgreement.getContract().getCurrIntPeriod());
						if (RepayPeriodSupporter.isCumulativeIntPeriod(loanAgreement.getWithdrawAgreement().getRepayWay()))
						{
							loanAgreement.getContract().setCurrIntPeriod(loanAgreement.getContract().getCurrIntPeriod()+1);
						}
					}*/
					//账单期数
					prinBill.setPeriod(loanSupporter.calCurrPrinPeriod());

					//2019-09-26 气球贷处理最后一期本金
					if( ConstantDeclare.REPAYWAY.REPAYWAY_QQD.equals( loanAgreement.getWithdrawAgreement().getRepayWay() ) &&
							loanAgreement.getContract().getContractEndDate().equals(prinBill.getEndDate()))
					{
						FabAmount lastPrin = prinBill.getBillAmt();
						lastPrin.selfAdd(Double.valueOf(loanAgreement.getContract().getBalance().getVal()));
						prinBill.setBillAmt(lastPrin);
						prinBill.setBillBal(lastPrin);
						loanAgreement.getContract().setBalance(new FabAmount(0.00));
					}

					billDetail.add(prinBill);
//					//等本等息不减一
//					if(!loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_EQPRINEQINT)
//							&&!VarChecker.isEmpty(totalperiod.getNum()))
//						//本金少了一起  总期数减一
//						totalperiod.setNum(totalperiod.getNum()-1);

					if (loanAgreement.getContract().getBalance().isZero())
					{
						break;
					}
					
					if((VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_WILLFUL,
	       					  ConstantDeclare.REPAYWAY.REPAYWAY_DBDX).contains(loanAgreement.getWithdrawAgreement().getRepayWay()))){
							if( prinBill.getEndDate().equals(loanAgreement.getContract().getContractEndDate()) )
								break;
						}
				}

				//在非等额本息以及等额本金条件下
				if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_OTHER)
						&& "true".equals(loanAgreement.getInterestAgreement().getIsCalTail()))
				{
					//在算尾条件下
					loanAgreement.getContract().setRepayIntDate(CalendarUtil.nDaysAfter(intBill.getEndDate(), 1).toString("yyyy-MM-dd"));
				}
			}
		}

		return billDetail;
	}

	private static void FeeBillSettle(LoanAgreement loanAgreement, String repayDate, List<LnsBill> billDetail, RepayFormulaSupporter loanSupporter, String periodEndDate) throws FabException {
		for (TblLnsfeeinfo lnsfeeinfo : loanAgreement.getFeeAgreement().getLnsfeeinfos()) {

			TblLnsfeeinfo currFeeInfo = lnsfeeinfo.cloneFeeInfo();
			//结清的不试算了
			if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsfeeinfo.getFeestat()))
				continue;

			//费用方式是一次性的 只有第一期能生成费用
			if (ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsfeeinfo.getRepayway()))
			{
//				if(lnsfeeinfo.getFeeperiod().equals(1)) {
				//更新成用时间来卡 一次性的费率
				if(CalendarUtil.equalDate(loanAgreement.getContract().getContractStartDate(), lnsfeeinfo.getLastfeedate()))  {
					//试算费用的起止日期
					addFeeBill(loanAgreement, repayDate, billDetail, loanSupporter, periodEndDate, lnsfeeinfo,currFeeInfo);
				}else
					continue;
			}

			while(CalendarUtil.before(lnsfeeinfo.getLastfeedate(), periodEndDate)){
				addFeeBill(loanAgreement, repayDate, billDetail, loanSupporter, periodEndDate, lnsfeeinfo,currFeeInfo);
			}

		}
	}

	private static void addFeeBill(LoanAgreement loanAgreement, String repayDate, List<LnsBill> billDetail, RepayFormulaSupporter loanSupporter, String periodEndDate, TblLnsfeeinfo lnsfeeinfo,TblLnsfeeinfo currfeeinfo) throws FabException {

		if(CalendarUtil.equalDate(lnsfeeinfo.getLastfeedate(), loanAgreement.getContract().getContractEndDate()))
			return;

		//试算费用的起止日期
		LnsBill feeBill = settleFee(loanAgreement, loanSupporter, lnsfeeinfo,periodEndDate);
		feeBill.setLnsfeeinfo(currfeeinfo);
		//因为可能有利息账本，不一定有本金账本 所以用利息账本的时间校验
		if (CalendarUtil.beforeAlsoEqual(feeBill.getEndDate(), periodEndDate)) {
			//计算费用
			FabAmount fee = loanSupporter.getCurrFee(feeBill, lnsfeeinfo, loanAgreement);
			feeBill.setBillAmt(fee);
			feeBill.setBillBal(fee);
			//计算开始日期到还款日利息
			feeBill.setRepayDateInt(loanSupporter.getRepayDateFee(feeBill, lnsfeeinfo, repayDate));
			//费用账本 当期免费金额计算，并减免账本金额
			if(loanAgreement.getBasicExtension().getFreeFee(feeBill)!=null
					&& loanAgreement.getBasicExtension().getFreeFee(feeBill).isPositive()){
				//如果免息金额大于这期利息的时候
				if(loanAgreement.getBasicExtension().getFreeFee(feeBill).sub(fee).isPositive()){
					feeBill.setBillBal(new FabAmount(0.0));
					feeBill.setBillAmt(new FabAmount(0.0));
					loanAgreement.getBasicExtension().getFreeFee(feeBill).selfSub(fee);
				}else{
					feeBill.setBillBal(new FabAmount(fee.sub(loanAgreement.getBasicExtension().getFreeFee(feeBill)).getVal()));
					feeBill.setBillAmt(new FabAmount(fee.sub(loanAgreement.getBasicExtension().getFreeFee(feeBill)).getVal()));
					loanAgreement.getBasicExtension().setFreeFee(feeBill,new FabAmount(0.0));
				}

			}
			//当日费用的减免
			if(loanAgreement.getBasicExtension().getFreeFee(feeBill)!=null
					&& !fee.equals(feeBill.getBillBal())
			){
				FabAmount termFreeInterest = new FabAmount(fee.sub(feeBill.getBillBal()).getVal());

				if( !VarChecker.isEmpty(feeBill.getRepayDateInt())) {
					if (!termFreeInterest.sub(feeBill.getRepayDateInt()).isNegative()) {
						feeBill.setTermFreeInterest(feeBill.getRepayDateInt());
						feeBill.setRepayDateInt(new FabAmount(0.0));
					} else {
						feeBill.setTermFreeInterest(termFreeInterest);
						feeBill.setRepayDateInt(new FabAmount(feeBill.getRepayDateInt().sub(termFreeInterest).getVal()));
					}
				}
			}
			feeBill.setPeriod(lnsfeeinfo.getFeeperiod());
			//非一次性的才需要增加期数
			lnsfeeinfo.setFeeperiod(lnsfeeinfo.getFeeperiod()+1);
//			feeBill.setPeriod(loanSupporter.calCurrIntPeriod());
			lnsfeeinfo.setLastfeedate(feeBill.getEndDate());
			if (feeBill.getBillBal().isPositive()||(null!=feeBill.getTermFreeInterest()&&feeBill.getTermFreeInterest().isPositive()))
				billDetail.add(feeBill);
		}
		else
			lnsfeeinfo.setLastfeedate(feeBill.getEndDate());
	}

	/**生成本金账单
	 * @param loanAgreement
	 * @throws Exception
	 * */
	private static LnsBill settlePrin(LoanAgreement loanAgreement,RepayFormulaSupporter loanSupporter) throws FabException {
		LnsBill billPrin = new LnsBill();
		billPrin.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);
		Integer graceDays = loanAgreement.getContract().getGraceDays();
		//如果宽限期为null,默认为0
		if (graceDays == null)
			graceDays = 0;
		/*
		 * 计算还款周期
		 */
//		RepayFormulaSupporter loanSupporter =LoanSupporterUtil.getFormulaSupporter(loanAgreement.getWithdrawAgreement().getRepayAmtFormula());
//		loanSupporter.setLoanAgreement(loanAgreement);

		billPrin.setStartDate(loanAgreement.getContract().getRepayPrinDate());
		billPrin.setEndDate(loanSupporter.calPrinCurrPeriodEndDate());


		billPrin.setCurendDate(loanSupporter.calPrinCurrPeriodCurrentDate());
		//账单状态开始日期默认为账单结束日期
		billPrin.setStatusbDate(billPrin.getEndDate());

		/*
		 * 计算还款金额
		 */

		//账单金额
		billPrin.setBillAmt(loanSupporter.getCurrPrin());
		//账单余额
		billPrin.setBillBal(billPrin.getBillAmt());
		//账单剩余本金
		billPrin.setBalance(loanSupporter.getBalance());

		billPrin.setCcy(billPrin.getBillAmt().getCurrency().getZhCurrencyCode());

		billPrin.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
		//默认结息
		billPrin.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET);
		//利息记至日期
		billPrin.setIntendDate(billPrin.getEndDate());
		//应还款日期
		billPrin.setRepayendDate(CalendarUtil.nDaysAfter(billPrin.getEndDate(), graceDays).toString("yyyy-MM-dd"));
		//默认正常
		billPrin.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
		//结清标志
		billPrin.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);

		billPrin.setCcy(loanAgreement.getContract().getCcy().getCcy());
		billPrin.setPrinBal(billPrin.getBalance());
		billPrin.setRepayWay(loanAgreement.getWithdrawAgreement().getRepayWay());
		//add 20180712 p2p暂时不开启
//		if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail()) && !billPrin.getEndDate().equals(billPrin.getRepayendDate())){
//			try {
//				billPrin.setRepayendDate(getP2PGraceEndDateStr(billPrin.getEndDate(),billPrin.getRepayendDate(),loanAgreement.getContract().getContractStartDate()));
//			} catch (ParseException e) {
//				throw new FabException("999999");
//			}
//		}
		return billPrin;
	}
	/**
	 * 根据本金还款周期计算利息周期，用于日终结息
	 * @throws Exception
	 * @throws SecurityException
	 *
	 */
	private static LnsBill settleInterest(LoanAgreement loanAgreement,RepayFormulaSupporter loanSupporter) throws FabException
	{
		LnsBill intBill = new LnsBill();
		intBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
		String startDate = loanAgreement.getContract().getRepayIntDate();
		Integer graceDays = loanAgreement.getContract().getGraceDays();
		//如果宽限期为null,默认为0
		if (graceDays == null)
			graceDays = 0;

//		RepayFormulaSupporter loanSupporter =LoanSupporterUtil.getFormulaSupporter(loanAgreement.getInterestAgreement().getIntFormula());
//		loanSupporter.setLoanAgreement(loanAgreement);
		intBill.setStartDate(startDate);

		intBill.setEndDate(loanSupporter.calIntCurrPeriodEndDate());
		intBill.setCurendDate(loanSupporter.calIntCurrPeriodCurrentDate());

		//账单状态开始日期默认为账单结束日期
		intBill.setStatusbDate(intBill.getEndDate());
		//剩余本金
		intBill.setBalance(loanAgreement.getContract().getBalance());

		//计息对应本金
		intBill.setPrinBal(loanSupporter.getCalIntPrin());

		intBill.setCcy(intBill.getBalance().getCurrency().getCcy());

		//账单利率
		intBill.setBillRate(loanAgreement.getRateAgreement().getNormalRate());
		//账单状态
		intBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
		//默认结息
		intBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET);
		//利息记至日期
		intBill.setIntendDate(intBill.getEndDate());
		//应还款日期
		intBill.setRepayendDate(CalendarUtil.nDaysAfter(intBill.getEndDate(), graceDays).toString("yyyy-MM-dd"));
		//默认正常
		intBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
		//核销优化登记cancelflag，“3”
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
			intBill.setCancelFlag("3");
		}
		//结清标志
		intBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
		//利息入账标志：默认已入账
		intBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
		intBill.setCcy(loanAgreement.getContract().getCcy().getCcy());
		intBill.setRepayWay(loanAgreement.getWithdrawAgreement().getRepayWay());
		//add 20180712  p2p暂时不开启
//		if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail()) && !intBill.getEndDate().equals(intBill.getRepayendDate())){
//			try {
//				intBill.setRepayendDate(getP2PGraceEndDateStr(intBill.getEndDate(),intBill.getRepayendDate(),loanAgreement.getContract().getContractStartDate()));
//			} catch (ParseException e) {
//				throw new FabException("999999");
//			}
//		}
		return intBill;
	}

	private static LnsBill settleFee(LoanAgreement loanAgreement,RepayFormulaSupporter loanSupporter,TblLnsfeeinfo lnsfeeinfo,String periodEndDate) throws FabException
	{
		LnsBill feeBill = new LnsBill();
		//费用类型
		feeBill.setBillType(lnsfeeinfo.getFeetype());

		Integer graceDays = loanAgreement.getContract().getGraceDays();
		//如果宽限期为null,默认为0
		if (graceDays == null)
			graceDays = 0;

		feeBill.setStartDate(lnsfeeinfo.getLastfeedate());

		//非标模式
		if(ConstantDeclare.FEEREPAYWAY.NONESTATIC.equals(lnsfeeinfo.getRepayway())){
			feeBill.setEndDate(periodEndDate);
		}else{
			feeBill.setEndDate(loanSupporter.calFeeCurrPeriodEndDate(lnsfeeinfo.getLastfeedate(),lnsfeeinfo.getFeeformula()));
		}

		feeBill.setCurendDate(feeBill.getEndDate());

		//账单状态开始日期默认为账单结束日期
		feeBill.setStatusbDate(feeBill.getEndDate());
		//剩余本金
		feeBill.setBalance(loanAgreement.getContract().getBalance());

		//计费对应本金
		//用计费基数判断
		if(ConstantDeclare.FEEBASE.ALL.equals(lnsfeeinfo.getFeebase()))
			feeBill.setPrinBal(loanAgreement.getContract().getContractAmt());
		else
			feeBill.setPrinBal(loanAgreement.getContract().getBalance());
		//用费用的金额类型
		feeBill.setCcy(lnsfeeinfo.getCcy());

		//账单利率
		feeBill.setBillRate(new FabRate(Double.toString(lnsfeeinfo.getFeerate())));
		//账单状态
		feeBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
		//默认结息
		feeBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET);
		//利息记至日期  ---暂时用不到  费用还没有相关联的
		feeBill.setIntendDate(feeBill.getEndDate());
		//应还款日期
		feeBill.setRepayendDate(CalendarUtil.nDaysAfter(feeBill.getEndDate(), graceDays).toString("yyyy-MM-dd"));
		//默认正常
		feeBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
		//结清标志
		feeBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
		//利息入账标志：默认已入账
		feeBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
		feeBill.setCcy(loanAgreement.getContract().getCcy().getCcy());
		feeBill.setRepayWay(lnsfeeinfo.getRepayway());

		//费用日期不算尾
//		if("true".equals(loanAgreement.getInterestAgreement().getIsCalTail()) && !intBill.getEndDate().equals(intBill.getRepayendDate())){
//			try {
//				intBill.setRepayendDate(getP2PGraceEndDateStr(intBill.getEndDate(),intBill.getRepayendDate(),loanAgreement.getContract().getContractStartDate()));
//			} catch (ParseException e) {
//				throw new FabException("999999");
//			}
//		}
		return feeBill;
	}


	/**
	 * 对历史本金未结清账单进行结息
	 * @param loanAgreement
	 * @param hisBill
	 * @return 历史未结清结息账单明细
	 * @throws Exception
	 */
	public static List<LnsBill> hisPrinBillSettleInterest(LoanAgreement loanAgreement,LnsBillStatistics billInfoAll,LnsBill hisBill,String repayDate) throws FabException
	{
		List<LnsBill> lnsBillList = new ArrayList<LnsBill>();
		//判断是否记罚息标识，如果为false,逾期、呆滞、呆账均不记罚息
		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestFlag())
		{
			return lnsBillList;
		}

		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestPrin())
		{
			return lnsBillList;
		}


		//获取形态列表
		List<LoanFormInfo> loanFormList = LoanTransferProvider.getLoanFormInfoList(hisBill.getCurendDate());

		//设置宽限期列表信息
		loanFormList.get(1).setCurrStatusDays(loanAgreement.getContract().getGraceDays());
		loanFormList.get(1).setStatusEndDate(CalendarUtil.nDaysAfter(hisBill.getCurendDate(), loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));


		for(LoanFormInfo loanFormInfo:loanFormList)
		{
			//获取形态对应结息实例
			SettleInterestSupporter settleInterestSupporter = SettleInterestSupporterProducter.prinProducter(loanFormInfo.getLoanForm());
			settleBill(loanAgreement, hisBill, repayDate, lnsBillList, loanFormInfo, settleInterestSupporter);
		}

		return lnsBillList;
	}
	/**
	 * 对历史利息未结清账单进行结息
	 * @param loanAgreement
	 * @param hisBill
	 * @return 历史未结清结息账单明细
	 * @throws Exception
	 */
	public static List<LnsBill> hisIntBillSettleInterest(LoanAgreement loanAgreement,LnsBillStatistics billInfoAll,LnsBill hisBill,String repayDate) throws FabException
	{
		List<LnsBill> lnsBillList = new ArrayList<LnsBill>();
		//判断是否记罚息标识，如果为false,逾期、呆滞、呆账均不记罚息
		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestFlag())
		{
			return lnsBillList;
		}

		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestInt())
		{
			return lnsBillList;
		}


		//获取形态列表
		List<LoanFormInfo> loanFormList = LoanTransferProvider.getLoanFormInfoList(hisBill.getCurendDate());

		//设置宽限期列表信息
		loanFormList.get(1).setCurrStatusDays(loanAgreement.getContract().getGraceDays());
		loanFormList.get(1).setStatusEndDate(CalendarUtil.nDaysAfter(hisBill.getCurendDate(), loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));

		for(LoanFormInfo loanFormInfo:loanFormList)
		{
			SettleInterestSupporter settleInterestSupporter = SettleInterestSupporterProducter.intProducter(loanFormInfo.getLoanForm());
			settleBill(loanAgreement, hisBill, repayDate, lnsBillList, loanFormInfo, settleInterestSupporter);
		}

		return lnsBillList;
	}
	/**
	 * 对历史费用未结清账单 结违约金
	 * @param loanAgreement
	 * @param hisBill
	 * @return 历史未结清结费账单明细
	 * @throws Exception
	 */
	public static List<LnsBill> hisFeeBillSettlePenalty(LoanAgreement loanAgreement,LnsBill hisBill,String repayDate) throws FabException
	{

		List<LnsBill> lnsBillList = new ArrayList<>();
		//没有逾期费率 直接返回
 		if(hisBill.getLnsfeeinfo().getOverrate().compareTo(0.00)<=0 ||
 		   "false".equals(loanAgreement.getInterestAgreement().getIsPenalty()) ) {
			return lnsBillList;
		}

 		//宽限期内不计算违约金
		if(CalendarUtil.beforeAlsoEqual(repayDate,
				CalendarUtil.nDaysAfter(hisBill.getCurendDate(), loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"))){
			return lnsBillList;
		}

		//获取形态列表
		List<LoanFormInfo> loanFormList = LoanTransferProvider.getLoanFormInfoList(hisBill.getCurendDate());

		for(LoanFormInfo loanFormInfo:loanFormList)
		{
			SettleInterestSupporter settleInterestSupporter = SettleInterestSupporterProducter.feeProducter(loanFormInfo.getLoanForm());
			settleBill(loanAgreement, hisBill, repayDate, lnsBillList, loanFormInfo, settleInterestSupporter);
		}

		return lnsBillList;
	}

	private static void settleBill(LoanAgreement loanAgreement, LnsBill hisBill, String repayDate, List<LnsBill> lnsBillList, LoanFormInfo loanFormInfo, SettleInterestSupporter settleInterestSupporter) throws FabException {
		if (settleInterestSupporter != null)
		{
			settleInterestSupporter.setLoanFormInfo(loanFormInfo);
			LnsBill lnsBill = settleInterestSupporter.settleInterest(loanAgreement, hisBill, repayDate);
			if (lnsBill != null)
			{
				lnsBillList.add(lnsBill);
			}
		}
	}

	/**
	 * 对封顶计息本金未结清账单进行结息
	 * @param loanAgreement
	 * @param hisBill
	 * @return 历史未结清结息账单明细
	 * @throws Exception
	 */
	public static List<LnsBill> hisCapBillSettleInterest(LoanAgreement loanAgreement,LnsBillStatistics billInfoAll,LnsBill hisBill,String repayDate) throws FabException
	{
		List<LnsBill> lnsBillList = new ArrayList<LnsBill>();
		//判断是否记罚息标识，如果为false,逾期、呆滞、呆账均不记罚息
		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestFlag())
		{
			return lnsBillList;
		}

		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestPrin())
		{
			return lnsBillList;
		}

		//获取形态列表
		List<LoanFormInfo> loanFormList = LoanTransferProvider.getLoanFormInfoList(hisBill.getCurendDate());

		//设置宽限期列表信息
		loanFormList.get(1).setCurrStatusDays(loanAgreement.getContract().getGraceDays());
		loanFormList.get(1).setStatusEndDate(CalendarUtil.nDaysAfter(hisBill.getCurendDate(), loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));


		for(LoanFormInfo loanFormInfo:loanFormList)
		{
			//获取形态对应结息实例
			SettleInterestSupporter settleInterestSupporter = SettleInterestSupporterProducter.capProducter(loanFormInfo.getLoanForm());
			settleBill(loanAgreement, hisBill, repayDate, lnsBillList, loanFormInfo, settleInterestSupporter);
		}

		return lnsBillList;
	}
	/**
	 * 计算呆账罚息
	 * @param lnsBill 账单
	 * @param rate 利率
	 * @param repayDate 还款日期
	 * @return
	 */
	public static FabAmount calculateBaddebtsInt(LoanAgreement loanAgreement,LnsBill lnsBill,FabRate rate,String repayDate)
	{
		//需要判断是否记罚息标识，如果为false，逾期、呆滞、呆账均不记罚息
		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestFlag())
		{
			return null;
		}

		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestPrin() && VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()))
		{
			return null;
		}

		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestInt() && VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()))
		{
			return null;
		}
		//只对本金账单和利息账单计算呆滞呆账罚息
		if (!VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()))
		{
			return null;
		}
		String loanform = PropertyUtil.getPropertyOrDefault("transfer."+String.valueOf(3),null);

		if (loanform == null)
			return null;
		String tmp[] = loanform.split(":");
		//逾期天数
		Integer days = Integer.parseInt(tmp[1]);

		//计算逾期开始日期
		String overStartDate = CalendarUtil.nDaysAfter(lnsBill.getCurendDate(), days).toString("yyyy-MM-dd");

		//未呆滞呆账不计罚息
		if (CalendarUtil.before(repayDate, overStartDate))
		{
			return null;
		}

		if (CalendarUtil.before(overStartDate, lnsBill.getIntendDate()))
		{
			overStartDate = lnsBill.getIntendDate();
		}



		//计算罚息
		//天数
		BigDecimal  baddebtsInt = new BigDecimal(CalendarUtil.actualDaysBetween(overStartDate, repayDate));
		//判断罚息的正负  为负数不进行处理
		if(baddebtsInt.signum()==-1){
			return null;
		}

		//日利息
		BigDecimal dayInt = rate.getDayRate();

		//2018-05-17 罚息计算取产品xml的罚息来源
		//2-合同余额
		/*if(loanAgreement.getContract().getFlag1().contains("C"))
		{  2019-07-26
			dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()));
		}*/
		//3-合同金额
		if("2".equals(loanAgreement.getInterestAgreement().getDintSource()))
		{
			if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
				dayInt = dayInt.multiply(new BigDecimal(lnsBill.getBillBal().getVal()));
			else
				dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getBalance().getVal()));
		}
		//3-合同金额
		else if("3".equals(loanAgreement.getInterestAgreement().getDintSource()))
		{
			if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
				dayInt = dayInt.multiply(new BigDecimal(lnsBill.getBillBal().getVal()));
			else
				dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()));
		}
		//4-未还本金 仅房抵贷封顶计息用
		else if("4".equals(loanAgreement.getInterestAgreement().getDintSource())){
			if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
				dayInt = dayInt.multiply(new BigDecimal(lnsBill.getBillBal().getVal()));
			else
				dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getBasicExtension().getSumPrin().getVal()));
		}
		//1或其他数字-账单剩余本金
		else
		{

			dayInt = dayInt.multiply(new BigDecimal(lnsBill.getBillBal().getVal()));
		}
		//总利息
		baddebtsInt = baddebtsInt.multiply(dayInt.setScale(2, BigDecimal.ROUND_HALF_UP));

		//封顶计息20190117
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
			//判断新增金额是否超过封顶金额
			FabAmount capDint  = getCapInterest(new FabAmount(baddebtsInt.doubleValue()),loanAgreement);
			if(!VarChecker.isEmpty(capDint)){
				loanAgreement.getBasicExtension().getCalDintComein().selfAdd(capDint);
				baddebtsInt = BigDecimal.valueOf(capDint.getVal()) ;
			}else{
				loanAgreement.getBasicExtension().getCalDintComein().selfAdd(new FabAmount(baddebtsInt.doubleValue()));
			}

		}
		//封顶 呆滞呆账罚息
		if(!loanAgreement.getBasicExtension().getBadDebt().isNegative()){
			if(loanAgreement.getBasicExtension().getBadDebt().sub(baddebtsInt.doubleValue()).isPositive()){
				loanAgreement.getBasicExtension().getBadDebt().selfSub(baddebtsInt.doubleValue());
			}else {
				baddebtsInt = BigDecimal.valueOf(loanAgreement.getBasicExtension().getBadDebt().getVal());
				loanAgreement.getBasicExtension().setBadDebt(new FabAmount());
			}
		}
		FabAmount badInt = new FabAmount();
		badInt.selfAdd(baddebtsInt.doubleValue());
		return badInt;

	}

	public static FabAmount calculateCapBaddebtsInt(LoanAgreement loanAgreement,LnsBill lnsBill,FabRate rate,String repayDate)
	{
		//需要判断是否记罚息标识，如果为false，逾期、呆滞、呆账均不记罚息
		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestFlag())
		{
			return null;
		}

		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestPrin() && VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()))
		{
			return null;
		}

		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestInt() && VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()))
		{
			return null;
		}
		//只对本金账单和利息账单计算呆滞呆账罚息
		if (!VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()))
		{
			return null;
		}
		String loanform = PropertyUtil.getPropertyOrDefault("transfer."+String.valueOf(3),null);

		if (loanform == null)
			return null;
		String tmp[] = loanform.split(":");
		//逾期天数
		Integer days = Integer.parseInt(tmp[1]);

		//计算逾期开始日期
		String overStartDate = CalendarUtil.nDaysAfter(lnsBill.getCurendDate(), days).toString("yyyy-MM-dd");

		//未呆滞呆账不计罚息
		if (CalendarUtil.before(repayDate, overStartDate))
		{
			return null;
		}

		if (CalendarUtil.before(overStartDate, lnsBill.getIntendDate()))
		{
			overStartDate = lnsBill.getIntendDate();
		}

		//计算罚息
		//天数
		BigDecimal  baddebtsInt = new BigDecimal(CalendarUtil.actualDaysBetween(overStartDate, repayDate));
		//日利息
		BigDecimal dayInt = rate.getDayRate();


		if("2".equals(loanAgreement.getInterestAgreement().getDintSource()))
		{
			dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getBalance().getVal()));
		}
		//3-合同金额
		else if("3".equals(loanAgreement.getInterestAgreement().getDintSource()))
		{

			dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()));
		}
		//4-未还本金 仅房抵贷封顶计息用
		else if("4".equals(loanAgreement.getInterestAgreement().getDintSource())){

			dayInt = dayInt.multiply(BigDecimal.valueOf(loanAgreement.getBasicExtension().getSumPrin().getVal()));
		}
		//1或其他数字-账单剩余本金
		else
		{

			dayInt = dayInt.multiply(new BigDecimal(lnsBill.getBillBal().getVal()));
		}
		//总利息
		baddebtsInt = baddebtsInt.multiply(dayInt.setScale(2, BigDecimal.ROUND_HALF_UP));

		//封顶计息20190117
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
			//判断新增金额是否超过封顶金额
			FabAmount capDint  = LoanBillSettleInterestSupporter.getCapInterest(new FabAmount(baddebtsInt.doubleValue()),loanAgreement);
			if(!VarChecker.isEmpty(capDint)){
				loanAgreement.getBasicExtension().getCalDintComein().selfAdd(capDint);
				baddebtsInt = BigDecimal.valueOf(capDint.getVal()) ;
			}else{
				loanAgreement.getBasicExtension().getCalDintComein().selfAdd(new FabAmount(baddebtsInt.doubleValue()));
			}
		}
		//封顶 呆滞呆账罚息
		if(!loanAgreement.getBasicExtension().getBadDebt().isNegative()){
			if(loanAgreement.getBasicExtension().getBadDebt().sub(baddebtsInt.doubleValue()).isPositive()){
				loanAgreement.getBasicExtension().getBadDebt().selfSub(baddebtsInt.doubleValue());
			}else {
				baddebtsInt = BigDecimal.valueOf(loanAgreement.getBasicExtension().getBadDebt().getVal());
				loanAgreement.getBasicExtension().setBadDebt(new FabAmount());
			}
		}
		FabAmount badInt = new FabAmount();
		badInt.selfAdd(baddebtsInt.doubleValue());
		return badInt;

	}




	//add 20180718
	private static String getP2PGraceEndDateStr(String startDate,String endDate,String cal) throws FabException, ParseException{
//		Calendar cal = Calendar.getInstance();
		//整年节假日工作日字符串
		String allYearStr="";
		//下一年节假日工作日字符串
		String nextYearStr="";
		//宽限期年份
		int yearNum = Integer.parseInt(cal.substring(0, 4));
		//判断系统时间和开始时间年分是否相同来取节假日工作日字符串
		if(!String.valueOf(yearNum).equals(startDate.substring(0,4))){
			allYearStr = GlobalScmConfUtil.getProperty(String.valueOf(yearNum+1), "");
//			allYearStr = PropertyUtil.getPropertyOrDefault("holiday."+String.valueOf(cal.get(Calendar.YEAR)+1),null);
			yearNum = yearNum+1;
			if(VarChecker.isEmpty(allYearStr)){
				throw new FabException("LNS072",String.valueOf(yearNum+1));
			}
		}else{
			allYearStr = GlobalScmConfUtil.getProperty(String.valueOf(yearNum), "");
//			allYearStr = PropertyUtil.getPropertyOrDefault("holiday."+String.valueOf(cal.get(Calendar.YEAR)),null);
		}
		//算出宽限期内有多少假日
		int weekendCount = CalendarUtil.dateTranInt(startDate);
//		int graceEnd
		//日期，工作标识集合
		String timePeriod ="";
		// 跨年判断(当开始日大于结束日时 )
		if(CalendarUtil.dateTranInt(startDate) > CalendarUtil.dateTranInt(endDate)){
			nextYearStr = GlobalScmConfUtil.getProperty(String.valueOf(yearNum+1), "");
//			nextYearStr = PropertyUtil.getPropertyOrDefault("holiday."+String.valueOf(cal.get(Calendar.YEAR)+1),null);
			yearNum = yearNum+1;
			if(VarChecker.isEmpty(nextYearStr)){
				throw new FabException("LNS072",String.valueOf(yearNum+1));
			}
			timePeriod = allYearStr.substring(CalendarUtil.dateTranInt(startDate))+nextYearStr.substring(0,CalendarUtil.dateTranInt(endDate));
		}else{
			if(VarChecker.isEmpty(allYearStr)){
				throw new FabException("LNS072",String.valueOf(yearNum));
			}
			timePeriod = allYearStr.substring(CalendarUtil.dateTranInt(startDate),CalendarUtil.dateTranInt(endDate));
		}
		for(int i = 0 ;i<timePeriod.length();i++){
			weekendCount = allYearStr.indexOf("1",  weekendCount);
			if(i != timePeriod.length()-1){
				weekendCount++;
			}
		}

		//宽限期顺延
		String graceEndDate = CalendarUtil.intTranDate(weekendCount+1,yearNum);
		return graceEndDate;
	}

	public static FabAmount getCapInterest(FabAmount currInterest,LoanAgreement loanAgreement) {
		if(!VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
			if(!currInterest.add(loanAgreement.getBasicExtension().getCalDintComein()).
					sub(new FabAmount(loanAgreement.getBasicExtension().getCapAmt().getVal())).isNegative()){
				FabAmount result = new FabAmount((loanAgreement.getBasicExtension().getCapAmt().getVal())
						-loanAgreement.getBasicExtension().getCalDintComein().getVal());
				return result;
			}else{
				return null;
			}
		}
		return null;
	}
}

package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;


@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns434 extends WorkUnit {

    String acctNo;
    String endDate;

    FabAmount cleanForfee = new FabAmount();
    FabAmount forfeetAmt = new FabAmount();
    FabAmount overFee = new FabAmount();	//逾期管理费
    FabAmount overPrin = new FabAmount();
	FabAmount overInt = new FabAmount();
	FabAmount overDint = new FabAmount();
	FabAmount refundableFee = new FabAmount();//逾期退保可退费用
	FabAmount	futureFee  = new FabAmount();//提前结清可退费用
	FabAmount  cleanDamages = new FabAmount();//提前结清违约金
	FabAmount  damagesAmt = new FabAmount();//正常还款违约金
	FabAmount  overDamages = new FabAmount();//到期违约金

	LnsBillStatistics lnsBillStatistics;
    TranCtx ctx;
	LoanAgreement loanAgreement ;

	@Override
    public void run() throws FabException{

        if (VarChecker.isEmpty(endDate)) {
            throw new FabException("LNS005");
        }
		if(loanAgreement.getFeeAgreement().isEmpty())
			throw new FabException("LNS021");


		if (loanAgreement.getContract().getContractStartDate().equals(endDate) && 
				"3".equals(loanAgreement.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
		}




		lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement,endDate , ctx, lnsBillStatistics);
		//统计所有包含费用的账本集合
		List<LnsBill> totalBills  = new ArrayList<>();
		totalBills.addAll(lnsBillStatistics.getHisBillList());
		totalBills.addAll(lnsBillStatistics.getBillInfoList());
		totalBills.addAll(lnsBillStatistics.getFutureBillInfoList());

		//统计账本  一次性和分期的
		LnsBill onetimeBill = null;
		List<LnsBill> lnsBills = new ArrayList<>();
		//保费是否逾期
		Boolean isOverdue = false;

		for(LnsBill lnsBill:totalBills) {
			//不含费用的  担保费和风险管理费不会同时存在
			if (LoanFeeUtils.isFeeType(lnsBill.getBillType())  && !VarChecker.isEmpty(lnsBill.getLnsfeeinfo())) {
				if (ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway()))
					onetimeBill = lnsBill;
				else
					lnsBills.add(lnsBill);

				//保费的状态是逾期的 或者 保费的结束日+宽限期 在endDate之前的
				if (!isOverdue
						&& !ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsBill.getSettleFlag()) &&
						CalendarUtil.before(CalendarUtil.nDaysAfter(lnsBill.getCurendDate(),
								loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"),
								endDate)) {
					isOverdue = true;
				}
			//统计违约金
			}/*else if(ConstantDeclare.BILLTYPE){

			}*/
		}


		boolean earlySettle = false;
		//提前结清收费模式earlysettleFlag（1.提前结清费用收到当期2.提前结清费用全收）
		if(!tranctx.getTranDate().equals(loanAgreement.getContract().getContractStartDate()))
		{
			if (!lnsBills.isEmpty())
				earlySettle = lnsBills.get(0).getLnsfeeinfo().getAdvancesettle().equals(ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE);
			else if (onetimeBill != null)
				earlySettle = onetimeBill.getLnsfeeinfo().getAdvancesettle().equals(ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE);
		}

        for (LnsBill lnsBill : lnsBills) {
        	//当前期和历史期  结束日期在费用开始日之后
            if(CalendarUtil.after(endDate, lnsBill.getStartDate())) {

                //结清费用
				cleanForfee.selfAdd(VarChecker.isEmpty(lnsBill.getRepayDateInt())?lnsBill.getBillBal():lnsBill.getRepayDateInt().add(lnsBill.getBillBal()).sub(lnsBill.getBillAmt()));//兼容按天计费费用
				//正常还款费用
				forfeetAmt.selfAdd(VarChecker.isEmpty(lnsBill.getRepayDateInt())?lnsBill.getBillBal():lnsBill.getRepayDateInt().add(lnsBill.getBillBal()).sub(lnsBill.getBillAmt()));
			//提前结清收费模式earlysettleFlag为2时,提前结清费用全收
			}else if(earlySettle){
				cleanForfee.selfAdd(lnsBill.getBillBal().getVal());
			}
			//当前期和未来期  结束日在费用结束日之前
			if(isOverdue && CalendarUtil.beforeAlsoEqual(endDate, lnsBill.getCurendDate()))
				//逾期可退费用
				refundableFee.selfAdd(lnsBill.getBillBal().getVal());

			//未来期  结束日在费用开始日之前
			if(CalendarUtil.beforeAlsoEqual(endDate, lnsBill.getStartDate()) )
				//提前结清可退费用
				futureFee.selfAdd(lnsBill.getBillBal().getVal());
			if( CalendarUtil.afterAlsoEqual(endDate, lnsBill.getCurendDate())){
				//逾期管理费
				overFee.selfAdd(lnsBill.getBillBal().getVal());
			}

        }

		if(onetimeBill!=null){
			if( CalendarUtil.afterAlsoEqual(endDate, onetimeBill.getCurendDate())){
				//逾期管理费
				overFee.selfAdd(onetimeBill.getBillBal().getVal());
			}

			/*
			 *  房抵贷固定担保特殊处理
			 *  放款当天预约还款查询   结清费用，正常还款费用需要加上一次性的
			 */
			if( !ConstantDeclare.BILLTYPE.BILLTYPE_ISFE.equals(onetimeBill.getBillType())) {

					//结清费用
					cleanForfee.selfAdd(onetimeBill.getBillBal());
					//正常还款费用
					forfeetAmt.selfAdd(onetimeBill.getBillBal());

			}
			if(ConstantDeclare.BILLTYPE.BILLTYPE_ISFE.equals(onetimeBill.getBillType())) {

				if (isOverdue && onetimeBill.getBillAmt().sub(onetimeBill.getBillBal()).isZero()) {
					int totalperiods = LoanFeeUtils.countPeriod(loanAgreement, lnsBillStatistics, ctx);
					refundableFee = new FabAmount(BigDecimal.valueOf(onetimeBill.getBillAmt().getVal()).multiply(new BigDecimal(Integer.toString(totalperiods - 1)))
							.divide(new BigDecimal(Integer.toString(totalperiods)), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}

				//一次性交取模式   未来期保费为0
				futureFee = new FabAmount();
				//一次性交取模式   应还和 一次性结清都为第一期所有未还保费
				cleanForfee = new FabAmount(onetimeBill.getBillBal().getVal());
				forfeetAmt = new FabAmount(onetimeBill.getBillBal().getVal());
			}
		}
        /*已到期本金	overPrin
		已到期利息	overInt
		已到期罚息	overDint*/
        List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
        //为了防止 开户日查询预约还款 lnsBillStatistics 为空
		//历史账单
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		//历史未结清账单结息账单
		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
		hisLnsbill.addAll(lnsBillStatistics.getBillInfoList());
		//未来期账单：从还款日到合同到期日之间的本金和利息账单
		hisLnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		hisLnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
        hisLnsbill.addAll(lnsBillStatistics.getCdbillList());

        for(LnsBill lnsBill: hisLnsbill){
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(lnsBill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE).contains(lnsBill.getSettleFlag())){
				continue;
			}
    		if(CalendarUtil.beforeAlsoEqual(lnsBill.getEndDate(),endDate)) {
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(lnsBill.getBillType())) {
					overDint.selfAdd(lnsBill.getBillBal());
				}
				if (ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())) {
					overPrin.selfAdd(lnsBill.getBillBal());
				}
				if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())) {
					overInt.selfAdd(lnsBill.getBillBal());
				}
				//违约金
				if(lnsBill.isPenalty()){
					cleanDamages.selfAdd(lnsBill.getBillBal());
				}

			}
        }
        damagesAmt = cleanDamages;
		overDamages = cleanDamages;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * @return the cleanForfee
     */
    public FabAmount getCleanForfee() {
        return cleanForfee;
    }

    /**
     * @param cleanForfee the cleanForfee to set
     */
    public void setCleanForfee(FabAmount cleanForfee) {
        this.cleanForfee = cleanForfee;
    }

    /**
     * @return the forfeetAmt
     */
    public FabAmount getForfeetAmt() {
        return forfeetAmt;
    }
    /**
     * @return  overFee the overFee to set
     */

	public FabAmount getOverFee() {
		return overFee;
	}

	public void setOverFee(FabAmount overFee) {
		this.overFee = overFee;
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
	 * @param forfeetAmt the forfeetAmt to set
	 */
	public void setForfeetAmt(FabAmount forfeetAmt) {
		this.forfeetAmt = forfeetAmt;
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
	 * @return the overInt
	 */
	public FabAmount getOverInt() {
		return overInt;
	}

	/**
	 * @param overInt the overInt to set
	 */
	public void setOverInt(FabAmount overInt) {
		this.overInt = overInt;
	}

	/**
	 * @return the overDint
	 */
	public FabAmount getOverDint() {
		return overDint;
	}

	/**
	 * @param overDint the overDint to set
	 */
	public void setOverDint(FabAmount overDint) {
		this.overDint = overDint;
	}

	/**
	 * @return the ctx
	 */
	public TranCtx getCtx() {
		return ctx;
	}

	/**
	 * @param ctx the ctx to set
	 */
	public void setCtx(TranCtx ctx) {
		this.ctx = ctx;
	}

	public FabAmount getRefundableFee() {
		return refundableFee;
	}

	public void setRefundableFee(FabAmount refundableFee) {
		this.refundableFee = refundableFee;
	}

	public FabAmount getFutureFee() {
		return futureFee;
	}

	public void setFutureFee(FabAmount futureFee) {
		this.futureFee = futureFee;
	}

	/**
	 * Gets the value of loanAgreement.
	 *
	 * @return the value of loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	/**
	 * Sets the loanAgreement.
	 *
	 * @param loanAgreement loanAgreement
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;

	}

	/**
	 * Gets the value of cleanDamages.
	 *
	 * @return the value of cleanDamages
	 */
	public FabAmount getCleanDamages() {
		return cleanDamages;
	}

	/**
	 * Sets the cleanDamages.
	 *
	 * @param cleanDamages cleanDamages
	 */
	public void setCleanDamages(FabAmount cleanDamages) {
		this.cleanDamages = cleanDamages;

	}

	/**
	 * Gets the value of damagesAmt.
	 *
	 * @return the value of damagesAmt
	 */
	public FabAmount getDamagesAmt() {
		return damagesAmt;
	}

	/**
	 * Sets the damagesAmt.
	 *
	 * @param damagesAmt damagesAmt
	 */
	public void setDamagesAmt(FabAmount damagesAmt) {
		this.damagesAmt = damagesAmt;

	}

	/**
	 * Gets the value of overDamages.
	 *
	 * @return the value of overDamages
	 */
	public FabAmount getOverDamages() {
		return overDamages;
	}

	/**
	 * Sets the overDamages.
	 *
	 * @param overDamages overDamages
	 */
	public void setOverDamages(FabAmount overDamages) {
		this.overDamages = overDamages;

	}
}

package com.suning.fab.loan.workunit;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns439 extends WorkUnit {

    String acctNo;
    String endDate;

    FabAmount cleanPrin = new FabAmount();
    FabAmount cleanFeeAmt = new FabAmount();
    FabAmount feeAmt = new FabAmount();
    FabAmount overPrin = new FabAmount();
    FabAmount overInt = new FabAmount();
    FabAmount overFee = new FabAmount();
    FabAmount penaltyAmt = new FabAmount();	//结清违约金
	//逾期本金
	FabAmount exceedPrin=new FabAmount();
	//逾期利息
	FabAmount exceedInt =new FabAmount();

    @Override
    public void run() throws FabException, Exception {

    	TranCtx ctx = getTranctx();

        if (VarChecker.isEmpty(endDate)) {
            throw new FabException("LNS005");
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


		//根据账号生成协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		if (la.getContract().getContractStartDate().equals(endDate) &&
				"3".equals(la.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
		}
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

		FabAmount futurePrin = new FabAmount();		//未来期本金
		//如果是先息后本违约金特殊处理
		boolean continueFlag = false;
        for (LnsBill lnsBill : billList) {
        	//过滤AMLT账单和已结清账单
			if("AMLT".equals(lnsBill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE).contains(lnsBill.getSettleFlag())){
				continue;
			}
			//正常管理费计算
			if(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE.equals(lnsBill.getBillType())){
				if((CalendarUtil.after(endDate, lnsBill.getStartDate())
						&& CalendarUtil.beforeAlsoEqual(endDate, lnsBill.getEndDate()))){
						feeAmt.selfAdd(lnsBill.getBillBal());
				}else if(CalendarUtil.before(lnsBill.getEndDate(),endDate)){
						feeAmt.selfAdd(lnsBill.getBillBal());
				}
			}
			//计算已到期本金管理费以及利息
			if(CalendarUtil.beforeAlsoEqual( ("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")) && null != lnsBill.getCurendDate() ) ? lnsBill.getCurendDate() : lnsBill.getEndDate(),endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE.equals(lnsBill.getBillType())){
					overFee.selfAdd(lnsBill.getBillBal());
				}
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())){
					overPrin.selfAdd(lnsBill.getBillBal());
                    //判断预期本金
					if(Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsBill.getBillStatus())){
						exceedPrin.selfAdd(lnsBill.getBillBal());
					}
				}
				if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())){
					overInt.selfAdd(lnsBill.getBillBal());
					//判断预期利息
					if(Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsBill.getBillStatus())){
						exceedInt.selfAdd(lnsBill.getBillBal());
					}
				}
			}

			//违约金计算(未来期本金计算违约金)   P2P项目已下线
//			if( "4010001".equals(la.getPrdId())){
//			if(CalendarUtil.before(endDate, lnsBill.getStartDate()) && ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill.getBillType())){
//				futurePrin.selfAdd(lnsBill.getBillBal());
//			//如果是先息后本特殊处理
//			}else if("4".equals(lnsbasicinfo.getRepayway())
//					&& CalendarUtil.afterAlsoEqual(endDate, lnsBill.getStartDate()) && CalendarUtil.beforeAlsoEqual(endDate, lnsBill.getEndDate()) && !continueFlag){
//					if( !VarChecker.isEmpty(lnsBill.getPrinBal()) ){
//						futurePrin.selfAdd(lnsBill.getPrinBal());
//						continueFlag = true;
//					}
//			}
//			}
        }
        //提前结清未来期管理费不收
        cleanFeeAmt = feeAmt;

        //违约金=违约金利率*未来期本金
        penaltyAmt = new FabAmount(BigDecimal.valueOf(lnsbasicinfo.getBjtxbl()).multiply(BigDecimal.valueOf(futurePrin.getVal())).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
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
	 * @return the cleanPrin
	 */
	public FabAmount getCleanPrin() {
		return cleanPrin;
	}

	/**
	 * @param cleanPrin the cleanPrin to set
	 */
	public void setCleanPrin(FabAmount cleanPrin) {
		this.cleanPrin = cleanPrin;
	}

	/**
	 * @return the cleanFeeAmt
	 */
	public FabAmount getCleanFeeAmt() {
		return cleanFeeAmt;
	}

	/**
	 * @param cleanFeeAmt the cleanFeeAmt to set
	 */
	public void setCleanFeeAmt(FabAmount cleanFeeAmt) {
		this.cleanFeeAmt = cleanFeeAmt;
	}

	/**
	 * @return the feeAmt
	 */
	public FabAmount getFeeAmt() {
		return feeAmt;
	}

	/**
	 * @param feeAmt the feeAmt to set
	 */
	public void setFeeAmt(FabAmount feeAmt) {
		this.feeAmt = feeAmt;
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
	 * @return the overFee
	 */
	public FabAmount getOverFee() {
		return overFee;
	}

	/**
	 * @param overFee the overFee to set
	 */
	public void setOverFee(FabAmount overFee) {
		this.overFee = overFee;
	}

	/**
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


}

package com.suning.fab.loan.supporter;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.utils.VarChecker;

import java.math.BigDecimal;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：计算费用违约金
 *
 * @Author 18049705 MYP
 * @Date Created in 11:38 2020/3/17
 * @see
 */
public class SettleInterestFeePenaltySupporter extends SettleInterestSupporter{
    @Override
    public LnsBill settleInterest(LoanAgreement loanAgreement, LnsBill hisBill, String repayDate)
    {

        LnsBill lnsBill = createLnsBill(hisBill, repayDate);
        //违约金类型
        lnsBill.setBillType(LoanFeeUtils.feePenaltyType(hisBill.getBillType()));
        //违约金记至账单逾期状态结束，之后不再计息
//        if (CalendarUtil.afterAlsoEqual(repayDate,getLoanFormInfo().getStatusEndDate()) )
//        {
//            lnsBill.setEndDate(getLoanFormInfo().getStatusEndDate());
//        }
        lnsBill.setCurendDate(lnsBill.getEndDate());

        //日违约金
        BigDecimal dayInt = new FabRate(Double.toString(hisBill.getLnsfeeinfo().getOverrate())).getDayRate()
                .multiply(new BigDecimal(hisBill.getBillBal().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP);
        //天数
        Integer intDays = CalendarUtil.actualDaysBetween(lnsBill.getStartDate(), lnsBill.getEndDate());
        //总利息
        BigDecimal intDec = new BigDecimal(intDays).multiply(dayInt);




        FabAmount interest = new FabAmount();
        interest.selfAdd(intDec.doubleValue());
        hisBill.setAccumulate(new FabAmount(0.00));

        lnsBill.setStatusbDate(lnsBill.getStartDate());
        lnsBill.setCcy(interest.getCurrency().getCcy());
        lnsBill.setBillAmt(interest);//账单金额
        lnsBill.setBillBal(interest);//账单余额
        lnsBill.setRepayendDate(lnsBill.getEndDate());
        lnsBill.setIntendDate(lnsBill.getEndDate());
        lnsBill.setBillRate(new FabRate(Double.toString(hisBill.getLnsfeeinfo().getOverrate())));
        return lnsBill;


    }
}

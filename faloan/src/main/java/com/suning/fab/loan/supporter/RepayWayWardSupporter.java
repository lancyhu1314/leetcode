package com.suning.fab.loan.supporter;

import com.suning.fab.loan.bo.RepayPeriod;
import com.suning.fab.loan.utils.MethodUtil;
import com.suning.fab.loan.utils.RepayPeriodSupporter;
import com.suning.fab.tup4j.base.FabException;

import java.lang.reflect.Method;

/**
 * @author 16090227
 *
 * @version V1.0.0
 *
 任性付等本等息(对应repayWay:10)映射本金、利息相关公式
 *
 * author:chenchao
 *
 */
public class RepayWayWardSupporter implements RepayWaySupporter {
    public RepayWayWardSupporter(){
        super();
    }

    @Override
    //计算还息周期公式
    public String getIntPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException {
        RepayPeriod repayPeriod = new RepayPeriod();
        Method msetp = MethodUtil.getMethod(repayPeriod.getClass(), "setPeriod"+periodUnit, Integer.class);
        Method msetod = MethodUtil.getMethod(repayPeriod.getClass(), "setOptDay", Integer.class);
        MethodUtil.methodInvoke(repayPeriod, msetp, periodNum);
        MethodUtil.methodInvoke(repayPeriod, msetod, Integer.valueOf(repayDate));
        return RepayPeriodSupporter.combinationPeriodFormula(repayPeriod);
    }

    //计算利息公式
    @Override
    public String getIntFormula() throws FabException{
        return "0";
    }

    @Override
    //计算还本周期公式
    public String getPrinPeriodFormula(Integer periodNum, String periodUnit, String repayDate, String beginDate, String endDate) throws FabException{
        RepayPeriod repayPeriod = new RepayPeriod();
        Method msetp = MethodUtil.getMethod(repayPeriod.getClass(), "setPeriod"+periodUnit, Integer.class);
        Method msetod = MethodUtil.getMethod(repayPeriod.getClass(), "setOptDay", Integer.class);
        MethodUtil.methodInvoke(repayPeriod, msetp, periodNum);
        MethodUtil.methodInvoke(repayPeriod, msetod, Integer.valueOf(repayDate));
        return RepayPeriodSupporter.combinationPeriodFormula(repayPeriod);
    }

    //计算还款金额公式
    @Override
    public String getRepayAmtFormula() throws FabException{
        return "5";
    }

}

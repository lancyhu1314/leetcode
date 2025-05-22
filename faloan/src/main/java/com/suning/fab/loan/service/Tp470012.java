package com.suning.fab.loan.service;


import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.workunit.Lns434;
import com.suning.fab.loan.workunit.Lns440;
import com.suning.fab.loan.workunit.Lns452;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 房抵贷预约还款
 * @author 18051734
 * param 账号           acctNo
 *       预约还款日期	endDate
 *
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-houseProvisionRepayQuery")
public class Tp470012 extends ServiceTemplate {

    @Autowired
    private Lns434 lns434;

    @Autowired
    private Lns452 lns452;

    @Autowired
    private Lns440 lns440;

    public Tp470012() {
        needSerSeqNo=false;
    }

    @Override
    protected void run() throws Exception {
        trigger(lns452);
        lns434.setLnsBillStatistics(lns452.getLnsBillStatistics());
        lns434.setCtx(lns452.getCtx());
        lns434.setLoanAgreement(lns452.getLa());
        trigger(lns434);
        lns440.setLnsBillStatistics(lns452.getLnsBillStatistics());
        lns440.setLa(lns452.getLa());
        trigger(lns440);
        //上海农商行合并违约金到费用
        if(Arrays.asList("2412638","2412641","2412640").contains(lns452.getLa().getPrdId())){
             lns434.getCleanForfee().selfAdd(lns434.getCleanDamages());
             lns434.setCleanDamages(new FabAmount(0.00));
             lns434.getForfeetAmt().selfAdd(lns434.getDamagesAmt());
             lns434.setDamagesAmt(new FabAmount(0.00));
             lns434.getOverFee().selfAdd(lns434.getOverDamages());
             lns434.setOverDamages(new FabAmount(0.00));
        }
    }

    @Override
    protected void special() throws FabException, Exception {

    }
}

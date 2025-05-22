package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈核销的贷款〉
 * 〈功能详细描述〉：根据主文件表的合同余额加上 lnsbill的余额  与 动态表比较补上  差值
 *
 * @Author 18049705
 * @Date Created in 20:16 2018/12/4
 * @see
 */
@Scope("prototype")
@Repository
public class Lns517 extends WorkUnit {

    @Autowired
    @Qualifier("accountAdder")
    AccountOperator add;
    @Autowired
    LoanEventOperateProvider eventProvider;
    TblLnsbasicinfo lnsbasicinfo; //贷款主文件
    LoanAgreement loanAgreement;
    Boolean ifCertification = false;
    String  acctNo; //贷款账号
    @Override
    public void run() throws  Exception
    {
        Map<String, Object> preparam = new HashMap<String, Object>();

        if(null == lnsbasicinfo)
        {
            //查询主文件表
            preparam.put("acctno", acctNo);
            preparam.put("openbrc", tranctx.getBrc());
            try{
                lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", preparam, TblLnsbasicinfo.class);
            }catch (FabSqlException e){
                throw new FabException(e, "SPS103", "lnsbasicinfo");
            }
            if(lnsbasicinfo==null)
                 throw new FabException("SPS104", "lnsbasicinfo");
        }
        //如果不是核销的贷款 跳过该子交易
        if(!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
                ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains( lnsbasicinfo.getLoanstat()))
            return ;
        ifCertification = true;//是否补计提利息标志

        //查询已有的账单表
//        preparam.put("acctno", lnsbasicinfo.getAcctno());
//        preparam.put("brc", tranctx.getBrc());
//        List<TblLnsbill> tblLnsBills;
//        try {
//            tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", preparam, TblLnsbill.class);
//        } catch (FabSqlException e) {
//            throw new FabException(e, "SPS103", "query_hisbills");
//        }
//        //统计各个类型的账单   正常本金账单还需要加上主文件表的合同余额  用map储存
//        Map<String,FabAmount> hxhkMap = new HashMap<>();
//        if( new FabAmount(lnsbasicinfo.getContractbal()).isPositive())
//            hxhkMap.put("PRIN.N",new FabAmount(lnsbasicinfo.getContractbal()));
//
//        //取计提登记簿信息
//        TblLnspenintprovreg penintprovisionDint = null;
//        Map<String, Object> param = new HashMap<String, Object>();
//        param.put("receiptno", acctNo);
//        param.put("brc", tranctx.getBrc());
//        param.put("billtype", "DINT");
//        try {
//            penintprovisionDint = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
//        }catch (FabSqlException e){
//            throw new FabException(e, "LNS103", "罚息计提登记簿Lnspenintprovreg");
//        }
//        //罚息处理  计提总罚息 - 已还的罚息 - 动态表的罚息   （需要特殊处理的）
//        FabAmount  hxhkDint = new FabAmount();
//        if(penintprovisionDint!=null){
//            hxhkDint.selfAdd(penintprovisionDint.getTotalinterest().doubleValue());
//        }
//        param.put("billtype", "CINT");
//        try {
//            penintprovisionDint = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
//        }catch (FabSqlException e){
//            throw new FabException(e, "LNS103", "罚息计提登记簿Lnspenintprovreg");
//        }
//        if(penintprovisionDint!=null){
//            hxhkDint.selfAdd(penintprovisionDint.getTotalinterest().doubleValue());
//        }
//        String key;
//        //已还罚息
//        for(TblLnsbill lnsBill:tblLnsBills)
//        {
//            if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
//                    ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
//                    ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
//                    .contains(lnsBill.getBilltype())
//                    )
//            {
//                hxhkDint.selfSub(new FabAmount(lnsBill.getBillamt()).sub(lnsBill.getBillbal()));
//                continue;
//            }
//            else{
//                key = lnsBill.getBilltype()+"."+lnsBill.getBillstatus();
//            }
//            if(null!=hxhkMap.get(key))
//            {
//                hxhkMap.get(key).selfAdd(lnsBill.getBillbal());
//            }else{
//                hxhkMap.put(key, new FabAmount(lnsBill.getBillbal()));
//            }
//        }
//        hxhkMap.put("DINT.N", hxhkDint);
//
//        //查询动态表的各个类型的金额
//        List<Map<String, Object>> lnsaccountdyninfoList = null;
//         preparam.put("brc", tranctx.getBrc());
//        preparam.put("acctno", lnsbasicinfo.getAcctno());
//        try {
//            lnsaccountdyninfoList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsaccountdyninfo", preparam);
//        } catch (FabSqlException e) {
//            LoggerUtil.error("查询lnsaccountdyninfo表错误{}", e);
//            throw new FabException("SPS103","query_lnsaccountdyninfo");
//        }
//        //用已结息的各个账单的金额 减去 动态表已有的金额  得出 通知核销后需要补充的金额
//        for(Map<String, Object> lnsaccountdyninfo:lnsaccountdyninfoList){
//            if(hxhkMap.get(String.valueOf(lnsaccountdyninfo.get("ACCTSTAT")).trim())!=null)
//                hxhkMap.get(String.valueOf(lnsaccountdyninfo.get("ACCTSTAT")).trim())
//                        .selfSub(Double.valueOf(lnsaccountdyninfo.get("CURRBAL").toString()));
//        }
//        if(loanAgreement==null)
//            loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);
//        //补上动态表金额
//        for(Map.Entry<String,FabAmount> hx:hxhkMap.entrySet()){
//            if(hx.getValue().isPositive())
//            {
//                if(!Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,ConstantDeclare.BILLTYPE.BILLTYPE_GINT,ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(hx.getKey().split("\\.")[0])){
//                    continue;
//                }
//                if(!("true".equals(loanAgreement.getInterestAgreement().getIgnoreOffDint())&&hx.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT))){
//                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnsbasicinfo.getAcctno(),
//                            hx.getKey().split("\\.")[0], hx.getKey().split("\\.")[1], new FabCurrency());
////                add.operate(lnsAcctInfo, null, hx.getValue(), loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HXZH, tranctx);
//                    //还需要有个事件
//                    //登记本金户销户事件
//                    eventProvider.createEvent(ConstantDeclare.EVENT.LNCNFINOFF,hx.getValue(),
//                            lnsAcctInfo, null, loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.HXZH, tranctx,loanAgreement.getCustomer().getMerchantNo() ,loanAgreement.getBasicExtension().getDebtCompany());
//                }
//            }
//        }
        //修改lnsbasicinfo 的贷款计提标志  和 贷款状态 为了通过后面的计提和结息
        //lnsbasicinfo.setLoanstat		慎改!!!!!!	Lns500有用	20190522|14050183
        lnsbasicinfo.setLoanstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
        lnsbasicinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);

    }

    /**
     *
     * @return  add get
     */
    public AccountOperator getAdd() {
        return add;
    }

    /**
     *
     * @param add set
     */
    public void setAdd(AccountOperator add) {
        this.add = add;
    }

    /**
     *
     * @return loanAgreement 合同信息
     */
    public LoanAgreement getLoanAgreement() {
        return loanAgreement;
    }

    /**
     *
     * @param loanAgreement 合同信息
     */
    public void setLoanAgreement(LoanAgreement loanAgreement) {
        this.loanAgreement = loanAgreement;
    }

    /**
     *
     * @return lnsbasicinfo 主文件信息
     */
    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    /**
     *
     * @param lnsbasicinfo set
     */
    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }

    /**
     *
     * @return eventProvider get
     */
    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }

    /**
     *
     * @param eventProvider set
     */
    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;
    }

    /**
     *
     * @return  acctNo get
     */
    public String getAcctNo() {
        return acctNo;
    }

    /**
     *
     * @param acctNo  set
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }

    /**
     *
     * @return ifCertification get
     */
    public Boolean getIfCertification() {
        return ifCertification;
    }

    /**
     *
     * @param ifCertification set
     */
    public void setIfCertification(Boolean ifCertification) {
        this.ifCertification = ifCertification;
    }
}

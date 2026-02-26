package com.suning.fab.loan.workunit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：保费减免
 *
 * @Author 18049705 MYP
 * @Date Created in 14:46 2019/10/22
 * @see
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class Lns462 extends WorkUnit {


    String serialNo;
    String feeType;
    String acctNo;
    FabAmount reduceFee;
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;
    //A风险管理费
    private static String FEETYPEA = "A";
    //B期缴担保费
    private static String FEETYPEB = "B";
    // C固定担保费
    private static String FEETYPEC = "C";
    // D保费
    private static String FEETYPED = "D";
    // E外部融担
    private static String FEETYPEE = "E";


    @Autowired
    LoanEventOperateProvider eventProvider;


    @Override
    public void run() throws Exception {

        LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx);
        //校验入参
        checkInput(loanAgreement);
        boolean ifCA = true;
        //子机构号
        String childBrc = "";
        List<String> feetypes = new ArrayList<>();
        for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos())
        {
            if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsfeeinfo.getFeestat())){
                childBrc = lnsfeeinfo.getFeebrc();
                ifCA = false;
                feetypes.add(lnsfeeinfo.getFeetype());
            }
        }

        if (ifCA)
            throw new FabException("LNS037");//没有未还的费用登记簿


        //幂等
        idempotent(childBrc);


        LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, tranctx.getTranDate(),tranctx);
        String briefCode ;
        if(FEETYPEA.equals(feeType))
            briefCode = ConstantDeclare.BRIEFCODE.FXGL;
        else if(FEETYPEB.equals(feeType))
            briefCode = ConstantDeclare.BRIEFCODE.QJDB;
        else
            briefCode = ConstantDeclare.BRIEFCODE.GDDB;

        //风险管理费减免
         if (FEETYPEA.equals(feeType)) {
            if(!feetypes.contains(ConstantDeclare.FEETYPE.RMFE))
                throw new FabException("LNS119","费用类型");

             reduce(loanAgreement, lnsBillStatistics, ConstantDeclare.FEETYPE.RMFE,ConstantDeclare.BRIEFCODE.RFJM);
        }
        //暂时没有业务
         else if(FEETYPED.equals(feeType)) {
        	 if(!feetypes.contains(ConstantDeclare.FEETYPE.ISFE))
        		 throw new FabException("LNS119","费用类型");

        	 reduce(loanAgreement, lnsBillStatistics, ConstantDeclare.FEETYPE.RMFE,briefCode);
         }else if(FEETYPEE.equals(feeType)) {
        	 if(!feetypes.contains(ConstantDeclare.FEETYPE.WBRD))
        		 throw new FabException("LNS119","费用类型");
        	 
        	 reduce(loanAgreement, lnsBillStatistics, ConstantDeclare.FEETYPE.WBRD,ConstantDeclare.BRIEFCODE.SFJM);
         }
         else {
             if(!feetypes.contains(ConstantDeclare.FEETYPE.SQFE))
                throw new FabException("LNS119","费用类型");
             boolean noGDDB = true;
             for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos())
             {
                 if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsfeeinfo.getFeestat())
                         &&ConstantDeclare.FEETYPE.SQFE.equals(lnsfeeinfo.getFeetype())
                         &&ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsfeeinfo.getRepayway())){
                     noGDDB = false;
                 }
             }
            //固定担保费减免
            if (FEETYPEC.equals(feeType)) {

                if(noGDDB)
                    throw new FabException("LNS037");//没有固定担保费
                Map<String,Object> tmp = new HashMap<>();

                for(LnsBill lnsBill : lnsBillStatistics.getHisBillList()){

                    if(ConstantDeclare.FEETYPE.SQFE.equals(lnsBill.getBillType())
                            &&ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway())) {
                        if (reduceFee.sub(lnsBill.getBillBal()).isPositive())
                            throw new FabException("LNS058", reduceFee.toString(), lnsBill.getBillBal().toString());

                        Double minAmt = LoanFeeUtils.repaysql(reduceFee,tmp,lnsBill,tranctx);
                        LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minAmt,loanAgreement,"",briefCode);
                        //固定担保费 还款事件
                        LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                                new FabCurrency(),childBrc);

                        eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, reduceFee, lnsAcctInfo, null,
                                loanAgreement.getFundInvest(), briefCode, tranctx,childBrc);
                        break;
                    }
                }

                for(LnsBill lnsBill : lnsBillStatistics.getBillInfoList()){
                    if(ConstantDeclare.FEETYPE.SQFE.equals(lnsBill.getBillType())
                            &&ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway())) {
                        if (reduceFee.sub(lnsBill.getBillBal()).isPositive())
                            throw new FabException("LNS058", reduceFee.toString(), lnsBill.getBillBal().toString());

                        LoanFeeUtils.settleFee(tranctx, lnsBill,loanAgreement);

                        Double minAmt = LoanFeeUtils.repaysql(reduceFee,tmp,lnsBill,tranctx);
                        LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minAmt,loanAgreement,"",briefCode);
                        //固定担保费 还款事件
                        LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                                new FabCurrency(),childBrc);

                        eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, reduceFee, lnsAcctInfo, null,
                                loanAgreement.getFundInvest(), briefCode, tranctx,childBrc);
                        break;
                    }
                }
                for(LnsBill lnsBill : lnsBillStatistics.getFutureBillInfoList()){
                    if(ConstantDeclare.FEETYPE.SQFE.equals(lnsBill.getBillType())
                            &&ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway())) {
                        if (reduceFee.sub(lnsBill.getBillBal()).isPositive())
                            throw new FabException("LNS058", reduceFee.toString(), lnsBill.getBillBal().toString());

                        LoanFeeUtils.settleFee(tranctx, lnsBill,loanAgreement);

                        Double minAmt = LoanFeeUtils.repaysql(reduceFee,tmp,lnsBill,tranctx);
                        LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minAmt,loanAgreement,"",briefCode);
                        //固定担保费 还款事件
                        LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                                new FabCurrency(),childBrc);

                        eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, reduceFee, lnsAcctInfo, null,
                                loanAgreement.getFundInvest(), briefCode, tranctx,childBrc);
                        break;
                    }
                }

            }
            //期缴担保费减免
            else {
                if(!noGDDB)
                    throw new FabException("LNS191");
                reduce(loanAgreement, lnsBillStatistics, ConstantDeclare.FEETYPE.SQFE,ConstantDeclare.BRIEFCODE.SFJM);
            }

        }


    }

    private void reduce(LoanAgreement loanAgreement, LnsBillStatistics lnsBillStatistics, String feeType,String briefCode) throws FabException {
        Map<String,Object> tmp = new HashMap<>();
        FabAmount reduce = new FabAmount(reduceFee.getVal());
        for(LnsBill lnsBill : lnsBillStatistics.getHisBillList()){
            if(feeType.equals(lnsBill.getBillType())
                    &&ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag())) {
                premiumCheck(lnsBill);
                Double minamt = LoanFeeUtils.repaysql(reduce,tmp,lnsBill,tranctx);
                LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minamt,loanAgreement,"",briefCode);
                reduce.selfSub(minamt);
                //还款事件
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                        new FabCurrency(),lnsBill.getLnsfeeinfo().getFeebrc());

                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minamt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), briefCode, tranctx,lnsBill.getLnsfeeinfo().getFeebrc());
                if (!reduce.isPositive())
                    break;
            }
        }


        for(LnsBill lnsBill : lnsBillStatistics.getBillInfoList()){
            if(feeType.equals(lnsBill.getBillType())) {

                if (!reduce.isPositive())
                    break;
                //存在一次性的担保费未还 报错
                premiumCheck(lnsBill);

                LoanFeeUtils.settleFee(tranctx, lnsBill,loanAgreement);

                Double minamt = LoanFeeUtils.repaysql(reduce,tmp,lnsBill,tranctx);
                LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minamt,loanAgreement,"",briefCode);
                reduce.selfSub(minamt);
                //还款事件
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                        new FabCurrency(),lnsBill.getLnsfeeinfo().getFeebrc());

                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minamt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), briefCode, tranctx,lnsBill.getLnsfeeinfo().getFeebrc());

            }
        }
        for(LnsBill lnsBill : lnsBillStatistics.getFutureBillInfoList()){
            if(feeType.equals(lnsBill.getBillType())) {

                if (!reduce.isPositive())
                    break;

                //存在一次性的担保费未还 报错
                premiumCheck(lnsBill);

                //不支持未来期的减免
                if(CalendarUtil.beforeAlsoEqual(tranctx.getTranDate(), lnsBill.getStartDate())){
                    throw new FabException("LNS193");
                }
                //按天计费  兼容费用减免
                if(!VarChecker.isEmpty(lnsBill.getRepayDateInt())
                        &&lnsBill.getBillAmt().sub(lnsBill.getRepayDateInt()).isPositive()){
                    lnsBill.setEndDate(tranctx.getTranDate());
                    lnsBill.setBillAmt(lnsBill.getRepayDateInt());
                    lnsBill.setBillBal(lnsBill.getRepayDateInt());
                }
                LoanFeeUtils.settleFee(tranctx, lnsBill,loanAgreement);

                Double minamt = LoanFeeUtils.repaysql(reduce,tmp,lnsBill,tranctx);
                LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minamt,loanAgreement,"",briefCode);
                reduce.selfSub(minamt);
                //还款事件
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                        new FabCurrency(),lnsBill.getLnsfeeinfo().getFeebrc());
//                loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getFeebrc();
                eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minamt), lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), briefCode, tranctx,lnsBill.getLnsfeeinfo().getFeebrc());
            }
        }

        //减免费用 大于所有未还费用
        if (reduce.isPositive()) {
            throw new FabException("LNS058", reduceFee.toString(), reduceFee.sub(reduce).toString());
        }
    }

    private void premiumCheck(LnsBill lnsBill) throws FabException {
        //保费分期的时候 存在一次性的担保费未还 报错
        if (ConstantDeclare.FEETYPE.SQFE.equals(lnsBill.getBillType())&&
                ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway()))
            throw new FabException("LNS191");
    }

    private void checkInput(LoanAgreement loanAgreement) throws FabException {
        if(FEETYPEC.equals(feeType))
            throw new FabException("LNS190","固定担保费");
        if (!VarChecker.asList(FEETYPEA, FEETYPEB, FEETYPEC, FEETYPED, FEETYPEE).contains(feeType)) {
            throw new FabException("LNS169", "费用类型", feeType);
        }

        if (VarChecker.asList("2512617", "2512619").contains(loanAgreement.getPrdId())) {
            throw new FabException("LNS190", loanAgreement.getPrdId());
        }

        if (loanAgreement.getFeeAgreement().isEmpty())
            throw new FabException("LNS037");//没有未还的费用登记簿
    }

    private void idempotent(String childBrc) throws FabException {
        // 幂等登记薄
        TblLnsinterface lnsinterface = new TblLnsinterface();
        lnsinterface.setTrandate(tranctx.getTermDate());
        lnsinterface.setSerialno(getSerialNo());
        lnsinterface.setAccdate(tranctx.getTranDate());
        lnsinterface.setSerseqno(tranctx.getSerSeqNo());
        lnsinterface.setTrancode(tranctx.getTranCode());


        lnsinterface.setAcctno(getAcctNo());
        lnsinterface.setBrc(childBrc);
        lnsinterface.setBankno(getSerialNo());
        lnsinterface.setTranamt(reduceFee.getVal()); // 用还款总金额TODO
        lnsinterface.setSumrint(0.00);
        lnsinterface.setSumramt(0.00);
        lnsinterface.setSumrfint(0.00);
        lnsinterface.setReserv1(feeType);//存在reserv1

        try {
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
        } catch (FabSqlException e) {
            //交易幂等
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("serialno", getSerialNo());
                params.put("trancode", tranctx.getTranCode());

                try {
                    lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", params,
                            TblLnsinterface.class);
                } catch (FabSqlException f) {
                    throw new FabException(f, "SPS103", "lnsinterface");
                }
                reduceFee = new FabAmount(lnsinterface.getTranamt());
                acctNo = lnsinterface.getAcctno();
                throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            } else
                throw new FabException(e, "SPS100", "lnsinterface");
        }
    }




    /**
     * Gets the value of serialNo.
     *
     * @return the value of serialNo
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * Sets the serialNo.
     *
     * @param serialNo serialNo
     */
    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;

    }

    /**
     * Gets the value of feeType.
     *
     * @return the value of feeType
     */
    public String getFeeType() {
        return feeType;
    }

    /**
     * Sets the feeType.
     *
     * @param feeType feeType
     */
    public void setFeeType(String feeType) {
        this.feeType = feeType;

    }

    /**
     * Gets the value of acctNo.
     *
     * @return the value of acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }

    /**
     * Sets the acctNo.
     *
     * @param acctNo acctNo
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;

    }

    /**
     * Gets the value of reduceFee.
     *
     * @return the value of reduceFee
     */
    public FabAmount getReduceFee() {
        return reduceFee;
    }

    /**
     * Sets the reduceFee.
     *
     * @param reduceFee reduceFee
     */
    public void setReduceFee(FabAmount reduceFee) {
        this.reduceFee = reduceFee;

    }
}

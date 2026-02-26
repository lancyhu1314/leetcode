package com.suning.fab.loan.utils;

import ch.qos.logback.core.joran.conditional.ElseAction;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.apache.commons.collections.map.HashedMap;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;

import javax.print.DocFlavor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.*;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：费用辅助类
 *
 * @Author 18049705 MYP
 * @Date Created in 14:50 2019/12/24
 * @see
 */
public class LoanFeeUtils {


    private static String feetypes ="";

    /**
     *  统计期数
     * @param loanAgreement 贷款数据
     */
    public static  Integer countPeriod(LoanAgreement loanAgreement, LnsBillStatistics lnsBillStatistics, TranCtx tranCtx) {
        //遍历未来期账单list生成未来期还款计划存入rfMap
        Map<Integer,TblLnsrpyplan> feeMap = new LinkedHashMap<>();
        //定义账单list
        List<LnsBill> listBill = new ArrayList<LnsBill>();

        //历史账单
        listBill.addAll(lnsBillStatistics.getHisBillList());
        //当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
        listBill.addAll(lnsBillStatistics.getBillInfoList());
        //未来期账单：从还款日到合同到期日之间的本金和利息账单
        listBill.addAll(lnsBillStatistics.getFutureBillInfoList());
        TblLnsbasicinfo lnsbasicinfo = new TblLnsbasicinfo();
        if(!VarChecker.isEmpty(loanAgreement.getContract().getDiscountAmt()))
            lnsbasicinfo.setDeductionamt(loanAgreement.getContract().getDiscountAmt().getVal());
        for (LnsBill lnsBill:listBill)
        {
            Integer key = lnsBill.getPeriod();
            TblLnsrpyplan repayPlan = feeMap.get(key);
            //如果是空，说明是新的一期计划，清空原计划repayPlan，利息信息存入repayPlan
            //如果不是空，说明已经有了利息信息，将同期本金信息合并到repayPlan生成一整期还款计划
            if (null == repayPlan){
                repayPlan = new TblLnsrpyplan();
            }

            LoanRepayPlanProvider.dealRepayPlan(loanAgreement, tranCtx, "FUTURE", lnsbasicinfo, lnsBill, repayPlan);

            //合并一期本金利息账单生成还款计划入map
            feeMap.put(key, repayPlan);
        }
        return feeMap.size();
    }



//    /**
//     *  flag入参校验
//     * @throws FabException  //校验
//     */
//    public static void checkFeeInput(String prdCode,String flag,FabRate	onetimeRate,FabRate	 yearFeeRate) throws  FabException{
//        //保费
//        //产品代码为2512617  2512619，即为保费
//        if(VarChecker.asList("2512617","2512619").contains(prdCode)){
//            //flag为1.一次性收取  yearfeeRate 为0  oneTimeRate 不为0
//            if("1".equals(flag)){
//                if(!rateIsPositive(onetimeRate)||rateIsPositive(yearFeeRate))
//                    throw new FabException("LNS168",flag);
//            }
//            //2.分期收取(不传也默认2)
//            else if (VarChecker.isEmpty(flag)||"2".equals(flag)){
//                if(rateIsPositive(onetimeRate)||!rateIsPositive(yearFeeRate))
//                    throw new FabException("LNS168","2");
////			}
////			//3.分期+一次性
////			else if("3".equals(flag)){
////				if(!rateIsPositive(onetimeRate)||!rateIsPositive(yearFeeRate))
////					throw new FabException("LNS168","分期+一次性保费时，年利率大于0，一次性利率大于0");
//            }else{
//                throw new FabException("LNS169","费用交取模式",flag);
//            }
//
//        }else {
//
//            //flag为1.一次性收取  yearfeeRate 为0  oneTimeRate 不为0
//            if("1".equals(flag)){
//                if(!rateIsPositive(onetimeRate)||rateIsPositive(yearFeeRate))
//                    throw new FabException("LNS168",flag);
//                if("2512623".equals(prdCode))
//                    throw new FabException("LNS169","任性贷期缴融担费","费用交取模式");
//            }
//            //2.分期收取(不传也默认2)
//            else if (VarChecker.isEmpty(flag)||"2".equals(flag)){
//                if(rateIsPositive(onetimeRate))
//                    throw new FabException("LNS168","2");
//            }
//
//            //3.分期+一次性
//            else if("3".equals(flag)){
//                if(!rateIsPositive(onetimeRate)||!rateIsPositive(yearFeeRate))
//                    throw new FabException("LNS168","3");
//                if("2512623".equals(prdCode))
//                    throw new FabException("LNS169","任性贷期缴融担费","费用交取模式");
//            }else{
//                throw new FabException("LNS169","费用交取模式",flag);
//            }
//        }
//    }


    /**
     *  费用开户表结构
     */
    public static TblLnsfeeinfo prepareFeeinfo(LoanAgreement loanAgreement, TranCtx ctx,String flag,FabRate feeRate) throws FabException {
        //落表
        TblLnsfeeinfo lnsfeeinfo = new TblLnsfeeinfo();
        //暂时用利息的
        //有独立的周期计算逻辑
        lnsfeeinfo.setFeeformula(loanAgreement.getInterestAgreement().getPeriodFormula());
        lnsfeeinfo.setOpenbrc(ctx.getBrc());
        lnsfeeinfo.setTrandate(ctx.getTranDate());
        lnsfeeinfo.setOpendate(ctx.getTranDate());
        lnsfeeinfo.setTrantime(ctx.getTranDate());
        lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
        //基本都是保费
        lnsfeeinfo.setFeetype( ConstantDeclare.FEETYPE.SQFE);

        //保费不计提
        if(isPremium(ctx.getProductCode())) {
            //会计抛事件
            lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
            //	趸交保费	一次性	ISFE 保费
            //		期缴保费	分期	ISFE保费
            lnsfeeinfo.setFeetype( ConstantDeclare.FEETYPE.ISFE);
        }
        lnsfeeinfo.setCalculatrule(ConstantDeclare.CALCULATRULE.BYTERM);

        lnsfeeinfo.setProvisionrule(ConstantDeclare.PROVISIONRULE.BYDAY);
        //	风险管理费	分期	RMFE风险管理费
        if("2412615".equals(ctx.getProductCode())&&"51230004".equals(ctx.getRequestDict("childBrc"))) {
            lnsfeeinfo.setFeetype(ConstantDeclare.FEETYPE.RMFE);
            lnsfeeinfo.setProvisionrule(ConstantDeclare.PROVISIONRULE.BYTERM);
        }
        lnsfeeinfo.setFeebase(ConstantDeclare.FEEBASE.ALL);
        lnsfeeinfo.setCcy(loanAgreement.getContract().getCcy().getCcy());
        lnsfeeinfo.setFeestat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);

        lnsfeeinfo.setLastfeedate(loanAgreement.getContract().getContractStartDate());
        lnsfeeinfo.setFeebrc(ctx.getRequestDict("childBrc"));
        lnsfeeinfo.setAcctno(ctx.getRequestDict("receiptNo"));

        //1.提前结清费用收到当期2.提前结清费用全收 (合规要求，不能全收，2020-04-13改)
        lnsfeeinfo.setAdvancesettle(ConstantDeclare.EARLYSETTLRFLAG.CURRCHARGE);
        lnsfeeinfo.setRepayway(flag);
        lnsfeeinfo.setFeerate(feeRate.getVal().doubleValue());

        if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(flag)){
            //转化成年费率
//            lnsfeeinfo.setFeerate(feeRate.getVal().multiply(new BigDecimal("12")).doubleValue());
            deduceFee(loanAgreement,lnsfeeinfo,feeRate);
        }
//        else {
//        }

        return lnsfeeinfo;
    }


    //利率大于0.00
    public  static  Boolean rateIsPositive(FabRate rate){
        return !VarChecker.isEmpty(rate)&&rate.getRate().compareTo(BigDecimal.valueOf(0.00))>0;
    }

    //是否是预扣费
    public static boolean isAdvanceDeduce(String productCode) {
        return VarChecker.asList("2412624","2412626").contains(productCode);
    }
    //是保费
    public static boolean isPremium(String productCode) {
        return     VarChecker.asList("2512617","2512619").contains(productCode);
    }
    private static void deduceFee(LoanAgreement loanAgreement,TblLnsfeeinfo lnsfeeinfo,FabRate onetimeRate) throws FabException {
        FabAmount oneTimeFee = new FabAmount();
        //改为直接更新还款计划\费用登记簿\主文件拓展表里面第一期固定担保费为已还 20191127
        //改成费用登记簿 预扣
        if(LoanFeeUtils.isAdvanceDeduce(loanAgreement.getPrdId())) {
            //预扣费的直接结清
            lnsfeeinfo.setFeestat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
            lnsfeeinfo.setRepayway(ConstantDeclare.FEEREPAYWAY.ADVDEDUCT);
            lnsfeeinfo.setProvisionrule(ConstantDeclare.PROVISIONRULE.ADVDEDUCT);
            //预扣费
            oneTimeFee.selfAdd(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()).multiply(onetimeRate.getVal()).setScale(2, RoundingMode.HALF_UP).doubleValue());
            //预扣的不生成账本
            lnsfeeinfo.setFeetype( "");
            lnsfeeinfo.setDeducetionamt(oneTimeFee.getVal());
            lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
        }
    }


    //费用账本落表
    public static void settleFee(TranCtx ctx, LnsBill lnsBill,LoanAgreement loanAgreement) throws FabException {
        TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(loanAgreement,lnsBill,ctx);
        tblLnsbill.setTrandate(Date.valueOf(ctx.getTranDate()));
        tblLnsbill.setSerseqno(ctx.getSerSeqNo());
        lnsBill.setTranDate(tblLnsbill.getTrandate());
        lnsBill.setSerSeqno(ctx.getSerSeqNo());

        if ( new FabAmount(tblLnsbill.getBillamt()).isPositive())
        {
            try{
                DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);
            }catch (FabSqlException e){
        		 throw new FabException(e, "SPS100", "lnsbill");
            }
            
            LoanAcctChargeProvider.add(lnsBill, ctx, loanAgreement, ConstantDeclare.EVENT.FEESETLEMT, LoanFeeUtils.feeSettleBrief(lnsBill));

            //保费结息转列计提均不抛送SAP
            if(!isPremium(loanAgreement.getPrdId())) {
                //结息税金
                AccountingModeChange.saveIntSetmTax(ctx, loanAgreement.getContract().getReceiptNo(), lnsBill);
            }
            //保存减免金额  更新辅助表
            if(!VarChecker.isEmpty(lnsBill.getTermFreeInterest())&&lnsBill.getTermFreeInterest().isPositive()){
                AccountingModeChange.updateBasicDynMF(ctx,loanAgreement.getBasicExtension().getFreeFee(lnsBill).sub(lnsBill.getTermFreeInterest()).getVal(),loanAgreement.getContract().getReceiptNo(),ctx.getTranDate(),lnsBill);
                loanAgreement.getBasicExtension().getFreeFee(lnsBill).selfSub(lnsBill.getTermFreeInterest());
            }
            updateLastFeeDate(lnsBill);
        }

    }


    /**
     *  更新上次结费日
     * @param lnsBill
     * @throws FabException
     */
    public static void updateLastFeeDate(LnsBill lnsBill) throws FabException{

        Map<String,Object> param = new HashMap<>();
        param.put("acctno", lnsBill.getLnsfeeinfo().getAcctno());
        param.put("brc", lnsBill.getLnsfeeinfo().getOpenbrc());
        param.put("lastfeedate", lnsBill.getEndDate());
        param.put("feetype", lnsBill.getLnsfeeinfo().getFeetype());
        param.put("repayway", lnsBill.getLnsfeeinfo().getRepayway());
        param.put("feeperiod", lnsBill.getPeriod()+1);
        //期数的问题
        //param.put("feeperiod", lnsBill.getPeriod());
        try{
            DbAccessUtil.execute("Lnsfeeinfo.update_lastfeedate",param);
        }catch (FabException e){
            throw new FabException(e, "SPS102", "Lnsfeeinfo");
        }
    }
    /**
     *  更新上次结费日
     * @throws FabException
     */
    public static void updatePeriod(TblLnsfeeinfo lnsfeeinfo,Integer period) throws FabException{

        Map<String,Object> param = new HashMap<>();
        param.put("acctno", lnsfeeinfo.getAcctno());
        param.put("brc", lnsfeeinfo.getOpenbrc());
        param.put("feetype", lnsfeeinfo.getFeetype());
        param.put("repayway", lnsfeeinfo.getRepayway());
        param.put("feeperiod", period);
        //期数的问题
        //param.put("feeperiod", lnsBill.getPeriod());
        try{
            DbAccessUtil.execute("Lnsfeeinfo.update_lastfeedate",param);
        }catch (FabException e){
            throw new FabException(e, "SPS102", "Lnsfeeinfo");
        }
    }
    //筛选账本  非费用账本
    public static List<TblLnsbill> filtFee( List<TblLnsbill> listBill) {
        List<TblLnsbill> lnsBills = new ArrayList<>();
        for(TblLnsbill lnsBill:listBill){
            if(!LoanFeeUtils.isFeeType(lnsBill.getBilltype())
                    &&!LoanFeeUtils.isPenalty(lnsBill.getBilltype())){
                lnsBills.add(lnsBill);
            }
        }
        return lnsBills;
    }
    //筛选账本  非费用账本
    public static List<LnsBill> filtFeeBill( List<LnsBill> listBill) {
        List<LnsBill> lnsBills = new ArrayList<>();
        for(LnsBill lnsBill:listBill){
            if(!LoanFeeUtils.isFeeType(lnsBill.getBillType())
                    &&!lnsBill.isPenalty()){
                lnsBills.add(lnsBill);
            }
        }
        return lnsBills;
    }
    //还款
    public static Double  repaysql(FabAmount amount,Map<String, Object> upbillmap, LnsBill lnsBill,TranCtx tranctx) throws FabException {
        upbillmap.put("actrandate", tranctx.getTranDate());
        upbillmap.put("tranamt", amount.getVal());

        upbillmap.put("trandate", lnsBill.getTranDate());
        upbillmap.put("serseqno", lnsBill.getSerSeqno());
        upbillmap.put("txseq", lnsBill.getTxSeq());

        Map<String, Object> repaymap;
        try {
            repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_repay", upbillmap);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbill");
        }
        if (repaymap == null) {
            throw new FabException("SPS104", "lnsbill");
        }
        double  minamt = Double.parseDouble(repaymap.get("minamt").toString());


        return minamt;
    }
    //还款
    public static Double  repaysql(FabAmount amount,Map<String, Object> upbillmap, TblLnsbill lnsBill,TranCtx tranctx) throws FabException {
        upbillmap.put("actrandate", tranctx.getTranDate());
        upbillmap.put("tranamt", amount.getVal());

        upbillmap.put("trandate", lnsBill.getTrandate());
        upbillmap.put("serseqno", lnsBill.getSerseqno());
        upbillmap.put("txseq", lnsBill.getTxseq());

        Map<String, Object> repaymap;
        try {
            repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_repay", upbillmap);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbill");
        }
        if (repaymap == null) {
            throw new FabException("SPS104", "lnsbill");
        }
        double  minamt = Double.parseDouble(repaymap.get("minamt").toString());

        return minamt;
    }
    public static void accountsub(AccountOperator sub,TranCtx ctx, LnsBill lnsBill, Double minAmt, LoanAgreement loanAgreement, String repayAcctNo,String briefCode) throws FabException {
        LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(), ConstantDeclare.BILLTYPE.BILLTYPE_FEEA, lnsBill.getBillStatus(),
                new FabCurrency(),lnsBill.getLnsfeeinfo().getFeebrc());
        lnsAcctInfo.setMerchantNo(repayAcctNo);
        lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
        lnsAcctInfo.setCustName("");
        lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
        if(VarChecker.isEmpty(briefCode))
            briefCode = ConstantDeclare.BRIEFCODE.FYHK;
        sub.operate(lnsAcctInfo, null, new FabAmount(minAmt), loanAgreement.getFundInvest(), briefCode,
                lnsBill.getTranDate().toString(), lnsBill.getSerSeqno(), lnsBill.getTxSeq(), ctx);
    }

    public static void accountsub(AccountOperator sub,TranCtx ctx, TblLnsbill lnsBill, Double minAmt, LoanAgreement loanAgreement,
                                  String repayAcctNo,String briefCode,String feebrc) throws FabException {
        LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(), ConstantDeclare.BILLTYPE.BILLTYPE_FEEA, lnsBill.getBillstatus(),
                new FabCurrency(),feebrc);
        lnsAcctInfo.setMerchantNo(repayAcctNo);
        lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
        lnsAcctInfo.setCustName("");
        lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
        // lnsAcctInfo.setBillType(lnsbill.getBilltype())
        // lnsAcctInfo.setLoanForm(lnsbill.getBillstatus())
        if(VarChecker.isEmpty(briefCode))
            briefCode = ConstantDeclare.BRIEFCODE.FYHK;
        sub.operate(lnsAcctInfo, null, new FabAmount(minAmt), loanAgreement.getFundInvest(), briefCode,
                lnsBill.getTrandate().toString(), lnsBill.getSerseqno(), lnsBill.getTxseq(), ctx);
    }
    //费用结转事件摘要码
    public static String  feeSettleBrief (LnsBill lnsBill){
        if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_SQFE)){
            if(lnsBill.getRepayWay().equals(ConstantDeclare.FEEREPAYWAY.ONETIME))
                return ConstantDeclare.BRIEFCODE.OFJZ;
            else
                return ConstantDeclare.BRIEFCODE.SFJZ;
        }else if (lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_RMFE))
               return ConstantDeclare.BRIEFCODE.RFJZ;
        else if( LoanFeeUtils.isOtherFee(lnsBill.getBillType()) )
        	   return ConstantDeclare.BRIEFCODE.FYJZ;
        else
            return ConstantDeclare.BRIEFCODE.IFJZ;
    }
    //费用转列事件摘要码
    public static String  feeTransferBrief (LnsBill lnsBill){
        if(lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_SQFE)){
            if(lnsBill.getRepayWay().equals(ConstantDeclare.FEEREPAYWAY.ONETIME))
                return ConstantDeclare.BRIEFCODE.OFZL;
            else
                return ConstantDeclare.BRIEFCODE.SFZL;
        }else if (lnsBill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_RMFE))
            return ConstantDeclare.BRIEFCODE.RFZL;
        else if ( LoanFeeUtils.isOtherFee(lnsBill.getBillType()) )
            return ConstantDeclare.BRIEFCODE.FYZL;
        else
            return ConstantDeclare.BRIEFCODE.IFZL;
    }
    //费用冲销事件摘要码
    public static String  feeWriteOffBrief (TblLnsbill lnsBill){
        if(lnsBill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_SQFE)){
            if(lnsBill.getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ONETIME))
                return ConstantDeclare.BRIEFCODE.OFJC ;
            else
                return ConstantDeclare.BRIEFCODE.SFJC;
        }else if (lnsBill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_RMFE))
            return ConstantDeclare.BRIEFCODE.RFJC;
        else if ( LoanFeeUtils.isOtherFee(lnsBill.getBilltype()) )
            return ConstantDeclare.BRIEFCODE.FYJC;
        else
            return ConstantDeclare.BRIEFCODE.IFJC;
    }



    //费用对用违约金类型
    public static String  feePenaltyType(String billtype){
        if(ConstantDeclare.BILLTYPE.BILLTYPE_SQFE.equals(billtype)){
            return ConstantDeclare.BILLTYPE.BILLTYPE_DBWY;
        }
        else {
            return ConstantDeclare.BILLTYPE.BILLTYPE_FWWY;
        }
    }

    public static boolean isPenalty(String billType ){
        return Arrays.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DBWY,ConstantDeclare.BILLTYPE.BILLTYPE_FWWY).contains(billType);
    }

    //未来期的费用还款
    public static String feeRepaymentBill(String receiptNo,TranCtx ctx, FabAmount tranAmt,List<LnsBill> feeAdvance, String  settleFlag, Map<String,FabAmount> feeRpyInfo , Map<String, Map<String,FabAmount>> repayAmtList) throws FabException {

        LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(receiptNo, ctx);
        LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
        //费用还款  获取未来期的账本


        List<LnsBill> feebills = new ArrayList<>();
        feebills.addAll(lnsBillStatistics.getBillInfoList());
        feebills.addAll(lnsBillStatistics.getFutureBillInfoList());
        List<LnsBill> repayfees = new ArrayList<>();

        int totalsize = 0;
        //筛选费用
        for(LnsBill feeBill :feebills){

            if(!LoanFeeUtils.isFeeType(feeBill.getBillType())){
                continue;
            }

            totalsize++;
            if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals( feeBill.getRepayWay())
                    //提前结清  全收
                    ||ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(feeBill.getLnsfeeinfo().getAdvancesettle()))
                repayfees.add(feeBill);
            else if(CalendarUtil.before( feeBill.getStartDate() ,ctx.getTranDate() )){
                //当期的特殊处理
                if(!VarChecker.isEmpty(feeBill.getRepayDateInt())){
                    //当期的只有金额大于0时才需要偿还。
                    if(feeBill.getRepayDateInt().isPositive()){
                        repayfees.add(feeBill);
                    }else{
                        //免费金额不为空的才添加
                        if( "true".equals(la.getInterestAgreement().getShowRepayList())) {
                            //如果金额为0则排除
                            updateLastFeeDate(feeBill);
                            LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, feeBill.getBillType(), feeBill.getBillStatus(), new FabAmount(0.00));
                            LoanRpyInfoUtil.addRpyList(repayAmtList, feeBill.getBillType(), feeBill.getPeriod().toString(), new FabAmount(0.00), feeBill.getRepayWay());
                            if(!VarChecker.isEmpty(feeBill.getTermFreeInterest())){
                                AccountingModeChange.updateBasicDynMF(ctx, la.getBasicExtension().getFreeFee(feeBill).sub(feeBill.getTermFreeInterest()).getVal(), receiptNo, ctx.getTranDate(), feeBill);
                                la.getBasicExtension().getFreeFee(feeBill).selfSub(feeBill.getTermFreeInterest());
                            }
                        }
                    }
                }else{
                    repayfees.add(feeBill);
                }
            }
        }

        //费用排序
        LoanRepayOrderHelper.allNormalSortBill(repayfees);
        Map<String,Object> param = new HashMap<>();
        int repaysize = 0;
        for(LnsBill repayfee:repayfees){
            if(!tranAmt.isPositive())
                break;
            boolean isPeriod = false;
            //当期的特殊处理
            if(!VarChecker.isEmpty(repayfee.getRepayDateInt())
                    &&!repayfee.getBillBal().equals(repayfee.getRepayDateInt())){
                repayfee.setEndDate(ctx.getTranDate());
                repayfee.setBillAmt(repayfee.getRepayDateInt());
                repayfee.setBillBal(repayfee.getRepayDateInt());
                //代偿还未来期费用
                if(ConstantDeclare.COMPENSATEFLAG.OTHERPAY.equals(ctx.getRequestDict("compensateFlag"))){
                    repayfee.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN);
                }else{
                    repayfee.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
                }


                //按天除非settleflag 否则不能提前结清
                totalsize++;
                //提前还费不更改期数
                isPeriod = true;
            }
            //费用落表
            LoanFeeUtils.settleFee(ctx, repayfee,la );
            //提前还费不更改期数
            if(isPeriod)
                updatePeriod(repayfee.getLnsfeeinfo(), repayfee.getPeriod());
            //还款
            double repayedAmt = repaysql(tranAmt, param, repayfee, ctx);
            LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, repayfee.getBillType(),repayfee.getBillStatus(),new FabAmount(repayedAmt));
            //展示还款明细准备数据 2020-07-17
            if( "true".equals(la.getInterestAgreement().getShowRepayList()) )
            	LoanRpyInfoUtil.addRpyList(repayAmtList,repayfee.getBillType(), repayfee.getPeriod().toString(), new FabAmount(repayedAmt),repayfee.getRepayWay());
            
            tranAmt.selfSub(repayedAmt);
            repayfee.setBillBal(new FabAmount(repayfee.getBillBal().sub(repayedAmt).getVal()));
            feeAdvance.add(repayfee);

            if( repayfee.getBillBal().isZero())
                repaysize++;

        }

        if("1".equals(settleFlag)&&
                repaysize==repayfees.size())
            return "Y";

        if(totalsize==repaysize){
            return "Y";
        }
        return "N";
    }
    //提前还本  费用落表
    public static void earlyRepaySettle(LnsBillStatistics lnsBillStatistics,TranCtx ctx,LoanAgreement loanAgreement,Integer txseq,TblLnsfeeinfo lnsfeeinfo) throws FabException{
        List<LnsBill> futureBills = new ArrayList<>();
        //当前期
        futureBills.addAll(lnsBillStatistics.getBillInfoList());
        futureBills.addAll(lnsBillStatistics.getFutureBillInfoList());



        boolean ifFeeperiod = true;
        for(LnsBill feeBill:futureBills){
            if(!LoanFeeUtils.isFeeType(feeBill.getBillType())){
                continue;
            }
            if(CalendarUtil.before(feeBill.getStartDate(), ctx.getTranDate())){
                ///当期的特殊处理
                if(CalendarUtil.before(ctx.getTranDate(), feeBill.getEndDate())
                        &&!VarChecker.isEmpty(feeBill.getRepayDateInt())
                        &&feeBill.getBillAmt().sub(feeBill.getRepayDateInt()).isPositive()){
                    feeBill.setEndDate(ctx.getTranDate());
                    feeBill.setBillAmt(feeBill.getRepayDateInt());
                    feeBill.setBillBal(feeBill.getRepayDateInt());

                }
                feeBill.setTxSeq(++txseq);
                LoanFeeUtils.settleFee(ctx,feeBill ,loanAgreement );
                ifFeeperiod = false;
            }
        }
        //已经结费的  期数加一
        if(ConstantDeclare.CALCULATRULE.BYDAY.equals(lnsfeeinfo.getCalculatrule()) &&ifFeeperiod){
            LoanFeeUtils.updatePeriod(lnsfeeinfo,lnsfeeinfo.getFeeperiod()+1 );
        }


    }


    public  static TblLnsfeeinfo  matchFeeInfo(LoanAgreement loanAgreement,String billType,String repayWay) {
        if(LoanFeeUtils.isFeeType(billType))
        {
            for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
                if(lnsfeeinfo.getFeetype().equals(billType)
                        &&lnsfeeinfo.getRepayway().equals(repayWay)) {
                    return lnsfeeinfo;
                }
            }
        }
        return  null;
    }


    public static void updateAllCA(String brc,String acctNo) throws FabException {
        Map<String, Object> param = new HashMap<>();
        param.put("acctno", acctNo);
        param.put("openbrc",brc);
        param.put("feestat",ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
        //提前结清  未来期的费用置为0
        try{
            DbAccessUtil.execute("Lnsfeeinfo.updateStat", param);

        }catch (FabSqlException e){
            throw new FabException(e, "SPS102", "Lnsfeeinfo");
        }
    }


    public static void updateCA(TblLnsfeeinfo lnsfeeinfo) throws FabException {
        Map<String, Object> param = new HashMap<>();
        param.put("acctno", lnsfeeinfo.getAcctno());
        param.put("openbrc",lnsfeeinfo.getOpenbrc());
        param.put("feetype",lnsfeeinfo.getFeetype());
        param.put("repayway",lnsfeeinfo.getRepayway());
        param.put("feestat",ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
        lnsfeeinfo.setFeestat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
        //提前结清  未来期的费用置为0
        try{
            DbAccessUtil.execute("Lnsfeeinfo.updateStat", param);

        }catch (FabSqlException e){
            throw new FabException(e, "SPS102", "Lnsfeeinfo");
        }
    }

    //获取费用的全部类型
    public static String getFeetypes (){
        if(VarChecker.isEmpty(feetypes)){
            Field[] fields = ConstantDeclare.FEETYPE.class.getDeclaredFields();
            StringBuilder stringBuilder = new StringBuilder();
            for(Field field:fields){
                try {
                    String value  = field.get(null).toString();
                    stringBuilder.append(",'").append(value).append("'");
                } catch (IllegalAccessException e) {
                    LoggerUtil.error("获取feetype失败",e );
                }
            }
            LoggerUtil.info(stringBuilder.toString());
            feetypes = stringBuilder.substring(1);
        }
        return feetypes;

    }

    public static boolean  isFeeType(String element){
        return VarChecker.isValidConstOption(element, ConstantDeclare.FEETYPE.class);
    }
    public static boolean  isAdvanceFee(String element){
        return VarChecker.isValidConstOption(element, ConstantDeclare.ADVANCEFEE.class);
    }
    public static boolean  isFeeRepayway(String element){
        return VarChecker.isValidConstOption(element, ConstantDeclare.FEEREPAYWAY.class);
    }
    public static boolean  isOtherFee(String element){
        return VarChecker.isValidConstOption(element, ConstantDeclare.OTHERFEE.class);
    }


    public static  void dealFee(LoanAgreement loanAgreement, List<TblLnsbill> tblLnsBills, String repayDate, String termEndDate, Map<String, Object> updateBillParam, TblLnsbill lnsbill) throws FabException {
        //更改费用的上次结息日 暂时没有按天计费的

        for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
            if(lnsbill.getBilltype().trim().equals(lnsfeeinfo.getFeetype())
                    &&lnsbill.getRepayway().trim().equals(lnsfeeinfo.getRepayway())) {

                if(!ConstantDeclare.CALCULATRULE.BYDAY.equals(lnsfeeinfo.getCalculatrule())) {
                    dealFutureFeeBill(loanAgreement, tblLnsBills, repayDate, termEndDate, updateBillParam, lnsbill, lnsfeeinfo);
                } else {
                    //这里需要跟利息一样在试算未来期之前 补延期的费用
                    //补费用暂时没做 先拦截
                    throw new FabException("LNS207","费用按天计费");

                }

            }
        }
    }

    private static  void dealFutureFeeBill(LoanAgreement loanAgreement, List<TblLnsbill> tblLnsBills, String repayDate, String termEndDate, Map<String, Object> updateBillParam, TblLnsbill lnsbill, TblLnsfeeinfo lnsfeeinfo) throws FabException {
        Map<String,Object> feeparam = new HashMap<>();
        feeparam.put("lastfeedate", termEndDate);
        //允许未来期费用结费（还款到未来期的场景） 延期
        if(ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(lnsfeeinfo.getAdvancesettle())){
            for(TblLnsbill feeBill:tblLnsBills){
                //费用且是未来期的 依次往后延期
                if(lnsbill.getBilltype().equals(feeBill.getBilltype())
                        &&lnsbill.getRepayway().equals(feeBill.getRepayway())
                        && CalendarUtil.beforeAlsoEqual(repayDate,feeBill.getBegindate().trim())){
                    Map<String,Object> updatefeeParam = new HashMap<>();

                    //更新账本表
                    updatefeeParam.put("curenddate",CalendarUtil.nMonthsAfter(termEndDate,feeBill.getPeriod()-lnsbill.getPeriod()).toString("yyyy-MM-dd"));
                    updatefeeParam.put("repayedate",CalendarUtil.nDaysAfter(updatefeeParam.get("curenddate").toString(),loanAgreement.getContract().getGraceDays()).toString("yyyy-MM-dd"));
                    updatefeeParam.put("intedate",updatefeeParam.get("curenddate"));
                    updatefeeParam.put("trandate",feeBill.getTrandate());
                    updatefeeParam.put("serseqno",feeBill.getSerseqno());
                    updatefeeParam.put("txseq",feeBill.getTxseq());
                    updatefeeParam.put("enddate",updatefeeParam.get("curenddate"));
                    updatefeeParam.put("begindate", CalendarUtil.nMonthsAfter(termEndDate,feeBill.getPeriod()-lnsbill.getPeriod()-1).toString("yyyy-MM-dd"));
                    LoanDbUtil.update("Lnsbill.update_date",updatefeeParam);


                    //上次结费日
                    feeparam.put("lastfeedate",
                            CalendarUtil.after(updatefeeParam.get("curenddate").toString(), feeparam.get("lastfeedate").toString())
                                    ?updatefeeParam.get("curenddate").toString():feeparam.get("lastfeedate").toString());

                }

            }
        }


        updateBillParam.put("intedate",termEndDate);
        //更改费用的上次计息日
        feeparam.put("acctno", lnsfeeinfo.getAcctno());
        feeparam.put("brc", lnsfeeinfo.getOpenbrc());

        feeparam.put("feetype", lnsfeeinfo.getFeetype());
        feeparam.put("repayway", lnsfeeinfo.getRepayway());
        feeparam.put("feeperiod", lnsfeeinfo.getFeeperiod());
        lnsfeeinfo.setLastfeedate(feeparam.get("lastfeedate").toString());
        LoanDbUtil.update("Lnsfeeinfo.update_lastfeedate",feeparam);
    }


}

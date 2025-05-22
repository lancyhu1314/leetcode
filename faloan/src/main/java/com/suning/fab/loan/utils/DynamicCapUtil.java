package com.suning.fab.loan.utils;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.*;

import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：动态封顶 辅助类
 *
 * @Author 18049705 MYP
 * @Date Created in 15:36 2019/10/10
 * @see
 */
public class DynamicCapUtil {

    /**
     * 统计动态封顶计息
     */
    static void CapInterestCalculation(LoanAgreement loanAgreement,TranCtx ctx,String repayDate) throws FabException
    {
        //不是新的封顶计息  不作考虑
        if(VarChecker.isEmpty(loanAgreement.getInterestAgreement().queryDynamicCapRate(ctx.getBrc())))
            return;

        //查询 lnspennintprovreg DTFD的值
        TblLnspenintprovreg lnspenintprovreg;
        Map<String,Object> param = new HashMap<>();
        param.put("billtype", ConstantDeclare.KEYNAME.DTFD);
        param.put("receiptno", loanAgreement.getContract().getReceiptNo());
        param.put("brc", ctx.getBrc());
        try{
            lnspenintprovreg = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS103", "Lnspenintprovreg");
        }
        if(lnspenintprovreg == null)//老数据  暂不考虑
            return;

        loanAgreement.getBasicExtension().setDynamicCapAmt(new FabAmount(lnspenintprovreg.getTotalinterest().doubleValue()));
        //房抵贷置空
        loanAgreement.getInterestAgreement().setCapRate(null);
        //发布后只跑一次 用时间限制一下
        //修改封顶值
        //查询所有还款数据 是要封顶的借据号

        //还要根据实际还款endDate判断一下
//        if(!loanAgreement.getWithdrawAgreement().getRepayWay().equals(ConstantDeclare.REPAYWAY.REPAYWAY_DBDX)
//                &&CalendarUtil.before(lnspenintprovreg.getEnddate().toString(), "2019-12-20")
//                &&CalendarUtil.before(lnspenintprovreg.getEnddate().toString(), loanAgreement.getContract().getContractEndDate())) {
//            //按时间走
//            param.put("acctno", loanAgreement.getContract().getReceiptNo());
//            param.put("brc", ctx.getBrc());
//            param.put("startdate", lnspenintprovreg.getEnddate().toString());
//            param.put("enddate", ctx.getTranDate());
//            Map<String,FabAmount> dateAmt = new HashMap<>();
//            //查询这两天的还款数据
//            List<TblLnsinterface> lnsinterfaces = DbAccessUtil.queryForList("Lnsinterface.querytmp", param, TblLnsinterface.class);
//            if(lnsinterfaces!=null) {
//                //统计天数 和 还款本金
//                for (TblLnsinterface lnsinterface : lnsinterfaces) {
//                    if (new FabAmount(lnsinterface.getSumramt()).isPositive()) {
//                        if (dateAmt.get(lnsinterface.getAccdate()) == null) {
//                            dateAmt.put(lnsinterface.getAccdate(), new FabAmount());
//                        }
//                        dateAmt.get(lnsinterface.getAccdate()).selfAdd(lnsinterface.getSumramt());
//                    }
//                }
//            }
//
//            if(!dateAmt.isEmpty()) {
//                String lastDate = "";
//                Date tmplastTime = lnspenintprovreg.getEnddate();
//                FabAmount sumprin = new FabAmount(querySumPrin(loanAgreement,ctx));
//                for(Map.Entry<String,FabAmount> entry : dateAmt.entrySet()){
//                    sumprin.selfAdd(entry.getValue());
//                }
//                FabAmount amt  = new FabAmount();
//                int size = dateAmt.size();
//                for(int i =0 ; i< size;i++){
//                    //找到最小的date
//                    for(Map.Entry<String,FabAmount> entry : dateAmt.entrySet()){
//                        lastDate = entry.getKey();
//                        break;
//                    }
//                    for(Map.Entry<String,FabAmount> entry : dateAmt.entrySet()){
//                        if(CalendarUtil.before(entry.getKey(), lastDate))
//                            lastDate = entry.getKey();
//                    }
//                    double dynamicCapValue = BigDecimal.valueOf(sumprin.getVal()).multiply(new FabRate(Double.toString(loanAgreement.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()))).getDayRate())
//                            .setScale(2, BigDecimal.ROUND_HALF_UP ).multiply(BigDecimal.valueOf( CalendarUtil.actualDaysBetween(lnspenintprovreg.getEnddate().toString() ,lastDate))).doubleValue();
//                    amt.selfAdd(dynamicCapValue);
//                    lnspenintprovreg.setEnddate(Date.valueOf(lastDate));
//                    sumprin.selfSub(dateAmt.get(lastDate));
//                    dateAmt.remove(lastDate);
//                }
//
//                //只补一次
//                if(!VarChecker.isEmpty(ctx.getSerSeqNo())&& amt.isPositive()) {
//                    loanAgreement.getBasicExtension().getDynamicCapAmt().selfAdd(amt);
//                    lnspenintprovreg.setEnddate(tmplastTime);
//                    capRecord2DB1(loanAgreement, ctx, lnspenintprovreg, sumprin.getVal(), amt.getVal(), lastDate);
//                    lnspenintprovreg.setEnddate(Date.valueOf(lastDate));
//                    lnspenintprovreg.setTotalinterest(lnspenintprovreg.getTotalinterest().add(new BigDecimal(1)));
//
//                }
//            }
//
//        }



        //实际还款日 试算出之前的封顶值
        //repaydate 不能小于上次还款日  还款接口做了限制
        if(!ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay())
                &&CalendarUtil.after(ctx.getTranDate(),repayDate )
                //实际还款日 不能超过 合同到期日  否则不重新计算封顶值
                &&CalendarUtil.before(repayDate, loanAgreement.getContract().getContractEndDate()))
        {
            Double sumprin = querySumPrin(loanAgreement,ctx);
            int ndays = CalendarUtil.actualDaysBetween(lnspenintprovreg.getEnddate().toString(),repayDate );
            //计算封顶值  并加入总封顶值
            double dynamicCapValue = BigDecimal.valueOf(sumprin).multiply(new FabRate(Double.toString(loanAgreement.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()))).getDayRate())
                    .setScale(2, BigDecimal.ROUND_HALF_UP ).multiply(BigDecimal.valueOf(ndays)).doubleValue();
            loanAgreement.getBasicExtension().getDynamicCapAmt().selfAdd(dynamicCapValue);
            //保存剩余本金
            loanAgreement.getBasicExtension().getPrinBal().selfAdd(sumprin);
            //按实际还款日封顶计息
            //多加了个 repayDate 等于 realDate 的校验 加个保险
            if(!VarChecker.isEmpty(ctx.getRequestDict("realDate"))
                    &&repayDate.equals(ctx.getRequestDict("realDate").toString()))
                capRecord2DB(loanAgreement, ctx, lnspenintprovreg,  sumprin, dynamicCapValue,repayDate);
            return;
        }

        //设置 是否封顶的标志
        if("A".equals(lnspenintprovreg.getReserv1().trim())) {
            //如果封顶，置入封顶时间
//            loanAgreement.getBasicExtension().setDynamicCapDate(lnspenintprovreg.getEnddate().toString());
            String capDate=getDtfdDate(loanAgreement,ctx);
            if(capDate!=null){
                loanAgreement.getBasicExtension().setDynamicCapDate(capDate);
            }else{
                loanAgreement.getBasicExtension().setDynamicCapDate(lnspenintprovreg.getEnddate().toString());
            }
        }
        //上次动态封顶计算时间 不等于账务日期  并且不为B的 已经逾期并且达到封顶的
        if(!repayDate.equals(lnspenintprovreg.getEnddate().toString())&&!lnspenintprovreg.getReserv2().contains("B")){
            //计算上次动态封顶日 到 账务日期之间的 登台封顶值
            //合同到期日 去掉日期的限制
//            if(CalendarUtil.after(repayDate, loanAgreement.getContract().getContractEndDate()))
//                repayDate = loanAgreement.getContract().getContractEndDate();
            //等本等息 合同到期日之前 不需要重新计算封顶值
            LoggerUtil.info("试算封顶值");
//            if(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay())&&CalendarUtil.beforeAlsoEqual(repayDate, loanAgreement.getContract().getContractEndDate())){
//                //等本等息的是固定值封顶
//                LoggerUtil.info("等本等息不进入");
//                return;
//            }

            int ndays = CalendarUtil.actualDaysBetween(lnspenintprovreg.getEnddate().toString(),repayDate );
            if(ndays > 0)
            {
                Double sumprin = querySumPrin(loanAgreement,ctx);
                //计算封顶值  并加入总封顶值
                double dynamicCapValue = BigDecimal.valueOf(sumprin).multiply(new FabRate(Double.toString(loanAgreement.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()))).getDayRate())
                        .setScale(2, BigDecimal.ROUND_HALF_UP ).multiply(BigDecimal.valueOf(ndays)).doubleValue();
                loanAgreement.getBasicExtension().getDynamicCapAmt().selfAdd(dynamicCapValue);
                //保存剩余本金
                loanAgreement.getBasicExtension().getPrinBal().selfAdd(sumprin);

                //合同结束日 还款或者计提 落表
                //2019-11-08 落表数据量太大 去掉计提落表
                if(whether2Storage(ctx,loanAgreement, repayDate))
                    capRecord2DB(loanAgreement, ctx, lnspenintprovreg,  sumprin, dynamicCapValue,repayDate);

            }
        }


    }

    /**
     * 是否落表封顶值
     */
    private static Boolean whether2Storage(TranCtx ctx,LoanAgreement loanAgreement,String repayDate){
        //存在不等于还款日的实际还款日
        if(!VarChecker.isEmpty(ctx.getRequestDict("realDate"))
                &&!repayDate.equals(ctx.getRequestDict("realDate").toString())
                && !VarChecker.isEmpty(ctx.getRequestDict("settleFlag"))
                &&VarChecker.asList("1","2").contains(ctx.getRequestDict("settleFlag").toString())){
            return false;
        }
        if(VarChecker.asList("471007","471008","471009","471012").contains(ctx.getTranCode()))
        {
            return true;
        }
        if(VarChecker.asList("473004").contains(ctx.getTranCode()) &&
                ConstantDeclare.PAYCHANNEL.SALES_RETURN.equals(loanAgreement.getBasicExtension().getChannelType()) )
        {
            return true;
        }
        //合同到期日 必须是有needSerSeqNo = true  为了实际还款日还款减免
//        if(!VarChecker.isEmpty(ctx.getSerSeqNo())&& CalendarUtil.afterAlsoEqual(ctx.getTranDate(), loanAgreement.getContract().getContractEndDate()))
//        {
////            return true;
//            //合同到期日后  只有还款才落明细
//            if(VarChecker.asList("471007","471008","471009","471012").contains(ctx.getTranCode()))
//            {
//                return true;
//            }
//        }
//        if(!VarChecker.isEmpty(ctx.getSerSeqNo())&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), "2019-12-20")) {
//            return true;
//        }
        return false;
    }

    //查询总的剩余本金
    private static Double querySumPrin(LoanAgreement loanAgreement,TranCtx ctx) throws FabException {
        Map<String, Object> param =  new HashMap<>();
        //查询总的未还本金
        param.put("acctno", loanAgreement.getContract().getReceiptNo());
        param.put("billtype", "PRIN");
        param.put("brc", ctx.getBrc());
        Double sumprin ;
        try{
            sumprin = DbAccessUtil.queryForObject("Lnsbill.query_billBalSum", param,Double.class);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS103", "query_billBalSum");
        }
        if(sumprin!=null)
            sumprin = loanAgreement.getContract().getBalance().add(sumprin).getVal();
        else
            sumprin = loanAgreement.getContract().getBalance().getVal();
        return sumprin;
    }

    //还款还到本金落表,动态封顶的封顶值变更 不轧差
    public static  void updateDTFDA(LoanAgreement loanAgreement,TranCtx ctx) throws FabException, ParseException {

        if(loanAgreement.getBasicExtension().getDynamicCapAmt().isPositive()&&loanAgreement.getBasicExtension().getDynamicCapDiff().isZero()){
            //明细表中的动态封顶的标志
            Map<String,Object> param = new HashMap<>();
            param.put("trandate", ctx.getTranDate());
            param.put("receiptno", loanAgreement.getContract().getReceiptNo());
//            String dynamicFlag= GlobalScmConfUtil.getProperty("dynamicFlag","false");
//            //默认为false
//            if("false".equals(dynamicFlag)){
//             try{
//               DbAccessUtil.execute("Lnspenintprovregdtl.updateAByUk", param);
//            }catch (FabSqlException e){
//                throw new FabException(e, "SPS102", "Lnspenintprovregdtl");
//            }
//            }

            //过了到期日 更新总封顶的标志
            //等本等息不需要等到贷款合同到期日后

            //等本等息合同期内
//                if(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay())&&CalendarUtil.beforeAlsoEqual(ctx.getTranDate(),loanAgreement.getContract().getContractEndDate())){
//                    param.put("enddate",loanAgreement.getContract().getContractEndDate() );
//                }else{
//                    param.put("enddate",Date.valueOf(ctx.getTranDate()) );
//                }
            String lastEndDate=saveDtfdDate(loanAgreement,ctx,ctx.getTranDate());// 保存封顶日期，更新封顶标志，并返回上次封顶日


            param.put("brc",ctx.getBrc() );

            try{
                DbAccessUtil.execute("Lnspenintprovreg.updateADTFD", param);
            }catch (FabSqlException e){
                throw new FabException(e, "SPS102", "Lnspenintprovreg");
            }
            if(!(CalendarUtil.before(loanAgreement.getContract().getContractEndDate(),ctx.getTranDate())&&CalendarUtil.actualDaysBetween(loanAgreement.getContract().getContractEndDate(),ctx.getTranDate())>90)){
                if(!ctx.getTranDate().equals(lastEndDate)){
                    capFddtlIntoTable(loanAgreement,ctx,lastEndDate);//保存封顶日期明细 未呆滞的进行明细保存
                }
            }
        }
    }

    /**
     * 保存上次封顶的日期
     * @param loanAgreement
     * @param ctx
     */
    public static String saveDtfdDate(LoanAgreement loanAgreement,TranCtx ctx,String endDate) throws FabException{
        String lastEndDate="";
        Map<String, Object> queryparam = new HashMap<>();
        queryparam.put("acctno", loanAgreement.getContract().getReceiptNo());
        queryparam.put("openbrc", ctx.getBrc());
        queryparam.put("key", ConstantDeclare.BASICINFOEXKEY.FDRQ);
        TblLnsbasicinfoex lnsbasicinfoex;
        try {
            lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", queryparam, TblLnsbasicinfoex.class);
        } catch (FabSqlException e) {
            throw new FabException("SPS100", "lnsbasicinfoex", e);
        }
        if(lnsbasicinfoex==null){
            lnsbasicinfoex = new TblLnsbasicinfoex();
            lnsbasicinfoex.setAcctno(loanAgreement.getContract().getReceiptNo());
            lnsbasicinfoex.setOpenbrc(ctx.getBrc());
            lnsbasicinfoex.setKey(ConstantDeclare.BASICINFOEXKEY.FDRQ);
            lnsbasicinfoex.setValue1(endDate);
            LoanDbUtil.insert("Lnsbasicinfoex.insert", lnsbasicinfoex);
            lastEndDate=loanAgreement.getContract().getStartIntDate();
        }else{
            lastEndDate=lnsbasicinfoex.getValue1();
            lnsbasicinfoex.setValue1(endDate);
            DbAccessUtil.execute("Lnsbasicinfoex.updateByUk", lnsbasicinfoex);
        }
        return lastEndDate;
    }

    /**
     * 获取动态封顶日期
     * @param loanAgreement
     * @param ctx
     * @return
     * @throws FabException
     */
    public static String getDtfdDate(LoanAgreement loanAgreement,TranCtx ctx)throws FabException{
        Map<String, Object> queryparam = new HashMap<>();
        queryparam.put("acctno", loanAgreement.getContract().getReceiptNo());
        queryparam.put("openbrc", ctx.getBrc());
        queryparam.put("key", ConstantDeclare.BASICINFOEXKEY.FDRQ);
        TblLnsbasicinfoex lnsbasicinfoex;
        try {
            lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", queryparam, TblLnsbasicinfoex.class);
        } catch (FabSqlException e) {
            throw new FabException("SPS100", "lnsbasicinfoex", e);
        }
        if(lnsbasicinfoex!=null){
            return lnsbasicinfoex.getValue1();
        }else{
            return null;
        }
    }

//    //还款还到本金落表,动态封顶的封顶值变更 不轧差
//    public static  void saveCapRecord(LoanAgreement loanAgreement,TranCtx ctx,FabAmount payedPrin) throws  FabException{
//        if(loanAgreement.getWithdrawAgreement().getRepayAmtFormula().equals(ConstantDeclare.FORMULA.FORMULA_EQPRINEQINT))//等本等息的是固定值封顶
//            return;
//
//        //还款的本金金额不大于0，
//        if(!(payedPrin.isPositive()&&loanAgreement.getBasicExtension().getDynamicCapAmt().isPositive())){
//            return;
//        }
//        //查询 lnspennintprovreg DTFD的值
//        TblLnspenintprovreg lnspenintprovreg;
//        Map<String,Object> param = new HashMap<>();
//        param.put("billtype", ConstantDeclare.KEYNAME.DTFD);
//        param.put("receiptno", loanAgreement.getContract().getReceiptNo());
//        param.put("brc", ctx.getBrc());
//        try{
//            lnspenintprovreg = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
//        }catch (FabSqlException e){
//            throw new FabException(e, "SPS103", "Lnspenintprovreg");
//        }
//        //如果当日已经登记过，不登记
//        if(ctx.getTranDate().equals(lnspenintprovreg.getEnddate().toString())){
//            return;
//        }
//
//        capRecord2DB(loanAgreement, ctx, lnspenintprovreg,  loanAgreement.getBasicExtension().getPrinBal().getVal(),
//                loanAgreement.getBasicExtension().getDynamicCapAmt().sub(lnspenintprovreg.getTotalinterest().doubleValue()).getVal());
//
//
//    }

    /**
     * 动态封顶  登记到表
     *
     */
    public static void capRecord2DB(LoanAgreement loanAgreement, TranCtx ctx, TblLnspenintprovreg lnspenintprovreg, double sumprin,
                                    double dynamicCapValue,String repayDate)
            throws FabException {
        TblLnspenintprovregdtl penintprovregdtl = new TblLnspenintprovregdtl();


        //登记动态封顶 明细
        penintprovregdtl.setReceiptno(loanAgreement.getContract().getReceiptNo());
        penintprovregdtl.setBrc(ctx.getBrc());
        penintprovregdtl.setCcy(loanAgreement.getContract().getCcy().getCcy());
        penintprovregdtl.setTrandate(Date.valueOf(ctx.getTranDate()));
        penintprovregdtl.setSerseqno(ctx.getSerSeqNo());
        penintprovregdtl.setTxnseq(0);//默认给0，每次只会新增一条数据
        penintprovregdtl.setPeriod(0);//默认给0
        penintprovregdtl.setListno(lnspenintprovreg.getTotallist()+1);
        penintprovregdtl.setBilltype(ConstantDeclare.KEYNAME.DTFD);
        penintprovregdtl.setTaxrate(0.00);
        penintprovregdtl.setBegindate(lnspenintprovreg.getEnddate());
        penintprovregdtl.setEnddate(Date.valueOf(repayDate));
        penintprovregdtl.setTimestamp(new java.util.Date().toString());
        penintprovregdtl.setInterest(dynamicCapValue);
        penintprovregdtl.setTax(0.00);
        penintprovregdtl.setReserv1(Double.toString(sumprin));//剩余本金
        penintprovregdtl.setReserv2(Double.toString(loanAgreement.getInterestAgreement().queryDynamicCapRate(ctx.getBrc())));//动态封顶利率

        //登记详表
        if( !new FabAmount(dynamicCapValue).isZero() )
        {
            try {
                DbAccessUtil.execute("Lnspenintprovregdtl.insert", penintprovregdtl);
            }catch (FabSqlException e){
                if ( !ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                    throw new FabException(e, "SPS100", "Lnspenintprovregdtl");
                }

            }
        }
        //更新  动态封顶总表
        Map<String, Object> param = new HashMap<>();
        param.put("totalinterest", loanAgreement.getBasicExtension().getDynamicCapAmt().getVal());
        param.put("enddate",Date.valueOf(repayDate));
        param.put("totallist",penintprovregdtl.getListno());
        param.put("receiptno",loanAgreement.getContract().getReceiptNo());
        param.put("brc",ctx.getBrc());
        param.put("billtype",ConstantDeclare.KEYNAME.DTFD);

        try{
            DbAccessUtil.execute("Lnspenintprovreg.updateDTFD", param);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS102", "Lnspenintprovreg");
        }
    }

    /**保存封顶明细
     * @param loanAgreement
     * @param ctx
     * @throws FabException
     */
    public static void capFddtlIntoTable(LoanAgreement loanAgreement, TranCtx ctx, String lastEndDate)
            throws FabException, ParseException {

        SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd");
        TblLnspenintprovregdtl penintprovregdtl = new TblLnspenintprovregdtl();
        //登记动态封顶 明细
        penintprovregdtl.setReceiptno(loanAgreement.getContract().getReceiptNo());
        penintprovregdtl.setBrc(ctx.getBrc());
        penintprovregdtl.setCcy(loanAgreement.getContract().getCcy().getCcy());
        penintprovregdtl.setTrandate(Date.valueOf(ctx.getTranDate()));
        penintprovregdtl.setSerseqno(ctx.getSerSeqNo());
        penintprovregdtl.setTxnseq(-1);//默认-1与 封顶金额的明细区分开
        penintprovregdtl.setPeriod(0);//默认给0
        penintprovregdtl.setListno(1);
        penintprovregdtl.setBilltype(ConstantDeclare.BASICINFOEXKEY.FDRQ);
        penintprovregdtl.setTaxrate(0.00);
        penintprovregdtl.setBegindate(new java.sql.Date(sdf.parse(lastEndDate).getTime()));
        penintprovregdtl.setEnddate(new java.sql.Date(sdf.parse(ctx.getTranDate()).getTime()));
        penintprovregdtl.setTimestamp(new java.util.Date().toString());
        penintprovregdtl.setInterest(0.00);
        penintprovregdtl.setTax(0.00);
        penintprovregdtl.setReserv1(Double.toString(0.00));//剩余本金
        penintprovregdtl.setReserv2(Double.toString(loanAgreement.getInterestAgreement().queryDynamicCapRate(ctx.getBrc())));//动态封顶利率

        try {
            //登记详表
            DbAccessUtil.execute("Lnspenintprovregdtl.insert", penintprovregdtl);

        }catch (FabSqlException e) {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode()))
                LoggerUtil.info("动态封顶金额入库，幂等",e);
            else
                throw new FabException("SPS100","Lnspenintprovregdtl");
        }
    }





    private static String  SUMFINT = "sumfint";
    private static String  SUMCINT = "sumcint";

    /**
     *  动态封顶所有收益账本
     *  优先级 利息>罚息>复利
     *  要保证上次计提日还款计划能查出来的罚息复利
     *
     */
    static void dynamCapTotal(LnsBillStatistics billStatistics,LoanAgreement loanAgreement,String repayDate,TranCtx ctx) throws FabException{
        long start  = System.currentTimeMillis();

        //获取所有的利息账本
        List<LnsBill> allNints = new ArrayList<>();
        //获取所有的罚息账本
        Map<Integer,List<LnsBill>> allDints = new TreeMap<>();//自动 按照key值升序排列 罚息宽限期账本
        Map<Integer,List<LnsBill>> allCints = new TreeMap<>();//自动 按照key值升序排列 复利账本

        FabAmount totalNint = new FabAmount();//总利息

        FabAmount hisNint = new FabAmount();//历史期的利息

        FabAmount totalDint = new FabAmount();//总罚息
        FabAmount provregDint = new FabAmount();

        //为了保证上次计提日的罚息，明细簿里没有历史罚息
        //定义查询map
        Map<String,Object> billparam = new HashMap<>();
        //按账号查询
        billparam.put("receiptno", loanAgreement.getContract().getReceiptNo());
        billparam.put("brc", ctx.getBrc());
        if(CalendarUtil.after(ctx.getTranDate(), repayDate))
            billparam.put("trandate", repayDate);

        List<TblLnspenintprovregdtl> rpyplanlist;

        try {
            long sqlstart  = System.currentTimeMillis();
           /* select * from  LNSPENINTPROVREGDTL where  brc = :brc and RECEIPTNO = :receiptno and billtype in ('CINT','DINT','GINT') and
                    trandate = (select max(TRANDATE)  from LNSPENINTPROVREGDTL where  brc = :brc and RECEIPTNO = :receiptno and billtype in ('CINT','DINT','GINT')
                    <#if trandate?exists && trandate != "">
                        and trandate <= :trandate
                    </#if>
             )*/
            //取还款计划登记簿数据
            rpyplanlist = DbAccessUtil.queryForList("Lnspenintprovregdtl.selectMaxTrandate", billparam, TblLnspenintprovregdtl.class);
            LoggerUtil.info("查询LNSPENINTPROVREGDTL耗时："+( System.currentTimeMillis()-sqlstart)+"ms");


        }
        catch (FabSqlException e)
        {
            throw new FabException(e, "SPS103", "Lnspenintprovregdtl");
        }
        //上次 计提  昨天已经确定 的罚息 复利
        Map<Integer,Map<String,FabAmount>> repayPlans  =  new HashMap<>();
        //没有罚息明细时取所有的罚息账本
        String penintprovregDate = CalendarUtil.nDaysAfter(ctx.getTranDate(), 1).toString("yyyy-MM-dd");
        if(rpyplanlist != null) {

            Integer serseqno = null;
            for (TblLnspenintprovregdtl lnspenintprovregdtl : rpyplanlist) {
                penintprovregDate = lnspenintprovregdtl.getTrandate().toString();
                if(serseqno == null)
                    serseqno = lnspenintprovregdtl.getSerseqno();
                if( !serseqno.equals(lnspenintprovregdtl.getSerseqno()))
                    continue;
                computeIfAbsent(repayPlans, lnspenintprovregdtl.getPeriod());
                if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(lnspenintprovregdtl.getBilltype()))
                {
                    repayPlans.get(lnspenintprovregdtl.getPeriod()).get(SUMFINT).selfAdd(lnspenintprovregdtl.getInterest());
                    provregDint.selfAdd(lnspenintprovregdtl.getInterest());
                }
                else if(ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnspenintprovregdtl.getBilltype())){
                    repayPlans.get(lnspenintprovregdtl.getPeriod()).get(SUMCINT).selfAdd(lnspenintprovregdtl.getInterest());
                    provregDint.selfAdd(lnspenintprovregdtl.getInterest());
                }
            }
        }

        //区分罚息利息账本 并 统计所有的利息账本金额
        for(LnsBill lnsBill :billStatistics.getHisBillList()){
            if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
            {
                hisNint.selfAdd(lnsBill.getBillAmt());
            }
            //加上历史期的罚息
            else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_GINT)
                    .contains(lnsBill.getBillType()))
            {
                totalDint.selfAdd(lnsBill.getBillAmt());
                //取 账本账务日期 在 罚息计提登记明细簿 账本日期之前的 （可能会存在还款，历史期和罚息明细登记簿的有重复）
                if(CalendarUtil.after(penintprovregDate, lnsBill.getTranDate().toString())){
                    computeIfAbsent(repayPlans, lnsBill.getPeriod());
                    repayPlans.get(lnsBill.getPeriod()).get(SUMFINT).selfAdd(lnsBill.getBillAmt());
                    provregDint.selfAdd(lnsBill.getBillAmt());
                }
//                if(lnsBill.getSettleDate()lnsBill.getEndDate())
            }else if(ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnsBill.getBillType())){
                totalDint.selfAdd(lnsBill.getBillAmt());
                //取 账本账务日期 在 罚息计提登记明细簿 账本日期之前的
                if(CalendarUtil.after(penintprovregDate, lnsBill.getTranDate().toString())){
                    computeIfAbsent(repayPlans, lnsBill.getPeriod());
                    repayPlans.get(lnsBill.getPeriod()).get(SUMCINT).selfAdd(lnsBill.getBillAmt());
                    provregDint.selfAdd(lnsBill.getBillAmt());
                }
            }
        }
        totalNint.selfAdd(hisNint);
        distinguishBill(billStatistics.getHisSetIntBillList(), allNints, allDints,allCints,totalNint,totalDint);
        distinguishBill(billStatistics.getFutureOverDuePrinIntBillList(), allNints, allDints,allCints,totalNint,totalDint);
        distinguishBill(billStatistics.getBillInfoList(), allNints, allDints,allCints,totalNint,totalDint);

        //未来期的利息需要特殊处理
        distinguishFutureBill(billStatistics.getFutureBillInfoList(), allNints, allDints, allCints,totalNint, totalDint, repayDate,loanAgreement);//未来期的也要特殊处理

        //cdbillList 全是罚息复利账本
        for(LnsBill lnsBill :billStatistics.getCdbillList()){
            totalDint.selfAdd(lnsBill.getBillAmt());
            if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_GINT)
                    .contains(lnsBill.getBillType())){
                if(allDints.get(lnsBill.getPeriod())==null)
                    allDints.put(lnsBill.getPeriod(),  new ArrayList<LnsBill>());
                allDints.get(lnsBill.getPeriod()).add(lnsBill );
            }else  if(ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnsBill.getBillType())){
                if(allCints.get(lnsBill.getPeriod())==null)
                    allCints.put(lnsBill.getPeriod(),  new ArrayList<LnsBill>());
                allCints.get(lnsBill.getPeriod()).add(lnsBill );
            }

        }
        //罚息封顶值
        FabAmount capDint ;
        //剩余封顶值
        loanAgreement.getBasicExtension().setDynamicCapDiff(new FabAmount(loanAgreement.getBasicExtension().getDynamicCapAmt().sub(totalDint).sub(totalNint).getVal()));



        //单独 利息就达到了封顶值的特殊情况(已计提的罚息不会封顶)
        if(loanAgreement.getBasicExtension().getDynamicCapAmt().sub(provregDint).sub(totalNint).isNegative()){

            loanAgreement.getBasicExtension().setDynamicCapDiff(new FabAmount());
            //根据期数  和  enddate 排序
            allNints.sort(new LnsBillCompator());
            //试算利息的封顶值  = 总封顶值 - 历史期利息 - 计提罚息
            FabAmount capNint = new FabAmount(loanAgreement.getBasicExtension().getDynamicCapAmt().sub(provregDint).sub(hisNint).getVal());

            //已经计提的利息  需要保证不会被冲销

            //取计提登记簿状态是PROVISION计提的最后一条数据
            Map<String,Object> provision = new HashMap<String,Object>();
            provision.put("receiptno", loanAgreement.getContract().getReceiptNo());
            provision.put("intertype", ConstantDeclare.INTERTYPE.PROVISION);
            provision.put("billtype",ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
            TblLnsprovision lnsprovision;
            try {
                //   lnsprovision = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_lastperiod", provision, );
                lnsprovision=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,loanAgreement.getContract().getReceiptNo(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.PROVISION,loanAgreement);
            }
            catch (FabSqlException e)
            {
                throw new FabException(e, "SPS103", "lnsprovision");
            }
            //已计提利息
            if(lnsprovision.isExist()) {
                //利息到达封顶的时候，未落表的罚息只能是明细表中的罚息 可以不考虑
                //保证计提的利息
                if (capNint.sub(lnsprovision.getTotalinterest()).add(hisNint).isNegative()) {
                    capNint = new FabAmount(lnsprovision.getTotalinterest());
                    capNint.selfSub(hisNint); //计提的利息是总的利息  需要减去历史期的利息才是试算未来期的利息
                }
            }
            for(LnsBill  lnsBill  : allNints){
                dynamicCapNint(capNint,lnsBill,loanAgreement);//按顺序封顶利息
            }
            //利息用光了封顶值时，总罚息封顶值  = 总的计提罚息
            capDint  = new FabAmount(provregDint.getVal());
        }
        else
        {
            //利息没有封顶时，总罚息封顶值  =  总封顶值 - 总利息
            capDint = new FabAmount(loanAgreement.getBasicExtension().getDynamicCapAmt().sub(totalNint).getVal());

        }

        //罚息封顶值小于总罚息才需要封顶
        if(capDint.sub(totalDint).isNegative()){
            //今天增加的封顶值
            capDint.selfSub(provregDint);

            loanAgreement.getBasicExtension().setDynamicCapDiff(new FabAmount());

            //减去历史期的罚息复利
            for(LnsBill lnsBill :billStatistics.getHisBillList()){
                if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_GINT)
                        .contains(lnsBill.getBillType()))
                {
                    repayPlans.get(lnsBill.getPeriod()).get(SUMFINT).selfSub(lnsBill.getBillAmt());

                }else if(ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnsBill.getBillType())){
                    repayPlans.get(lnsBill.getPeriod()).get(SUMCINT).selfSub(lnsBill.getBillAmt());

                }

            }

            //先封顶罚息
            for(Map.Entry<Integer,List<LnsBill>> termLnsBills  :allDints.entrySet()){
                List<LnsBill>  lnsBills = termLnsBills.getValue();
                lnsBills.sort(new LnsBillCompator());//按顺序封顶利息
                computeIfAbsent(repayPlans, termLnsBills.getKey());

                for(LnsBill lnsBill :lnsBills){
                    dynamicCapCDint(repayPlans, capDint, lnsBill, SUMFINT);
                }

            }
            //后封顶复利
            for(Map.Entry<Integer,List<LnsBill>> termLnsBills  :allCints.entrySet()){
                List<LnsBill>  lnsBills = termLnsBills.getValue();
                lnsBills.sort(new LnsBillCompator());//按顺序封顶利息
                computeIfAbsent(repayPlans, termLnsBills.getKey());

                for(LnsBill lnsBill :lnsBills){
                    dynamicCapCDint(repayPlans, capDint, lnsBill, SUMCINT);
                }

            }
            loanAgreement.getBasicExtension().setBadDebt(new FabAmount());
            //为了预约还款查询
            for(LnsBill lnsBill :billStatistics.getCdbillList()){
                loanAgreement.getBasicExtension().getBadDebt().selfAdd(lnsBill.getBillAmt());
            }
        }
        else
        {
            loanAgreement.getBasicExtension().setBadDebt(new FabAmount(-1.00));
        }


        //等本等息  未来期利息特殊处理，在罚息后封顶
        if(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()))
        {
            //用来算结清的时候的手续费，提前结清不收未来期的利息
            FabAmount tmp = new FabAmount(loanAgreement.getBasicExtension().getDynamicCapDiff().getVal());
            for(LnsBill lnsBill :billStatistics.getFutureBillInfoList()) {
                if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())) {
                    if (CalendarUtil.afterAlsoEqual(lnsBill.getStartDate(), repayDate)) {
                        dynamicCapAmt(tmp, lnsBill);
                    }
                }
            }
        }

        LoggerUtil.info("动态封顶："+( System.currentTimeMillis()-start)+"ms");
    }


    //动态封顶 罚息
    private static void dynamicCapCDint(Map<Integer, Map<String, FabAmount>> repayPlans, FabAmount capDint, LnsBill lnsBill, String sumfint) {
        if (repayPlans.get(lnsBill.getPeriod()).get(sumfint).sub(lnsBill.getBillAmt()).isPositive()) {
            repayPlans.get(lnsBill.getPeriod()).get(sumfint).selfSub(lnsBill.getBillAmt());
        } else {
            //封顶 今天新增的
            capDint.selfAdd(repayPlans.get(lnsBill.getPeriod()).get(sumfint));
            dynamicCapAmt(capDint, lnsBill);//按顺序封顶利息
            repayPlans.get(lnsBill.getPeriod()).put(sumfint, new FabAmount());
        }
    }

    //给账本计划 增加初始值
    private static void computeIfAbsent(Map<Integer, Map<String, FabAmount>> repayPlans, Integer period2) {
        if (repayPlans.get(period2) == null) {
            Map<String, FabAmount> tmp = new HashMap<>();
            tmp.put("sumfint", new FabAmount());
            tmp.put("sumcint", new FabAmount());
            repayPlans.put(period2, tmp);
        }
    }


    private static void dynamicCapNint(FabAmount capAmt, LnsBill lnsBill,LoanAgreement loanAgreement){
        if(capAmt.isNegative())
            capAmt.selfSub(capAmt.getVal());//封顶值为负，置零

        //未全部到期的利息
        //等本等息不用  RepayDateInt()
        if(!ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay())
                &&!VarChecker.isEmpty(lnsBill.getRepayDateInt()))
        {
            //截止还款日当期计提利息  大于封顶值
            if(capAmt.sub(lnsBill.getRepayDateInt()).isNegative()){
                lnsBill.getBillAmt().selfSub(lnsBill.getRepayDateInt().sub(capAmt).getVal());
                lnsBill.setBillBal(lnsBill.getBillAmt());
                lnsBill.setRepayDateInt(new FabAmount(capAmt.getVal()));
                capAmt.selfSub(capAmt.getVal());//剩余封顶值置为0

            }else{
                capAmt.selfSub(lnsBill.getRepayDateInt());
            }
        }else {

            dynamicCapAmt(capAmt, lnsBill);
        }
    }


    /**
     *  账本动态封顶
     */
    private static void dynamicCapAmt(FabAmount capAmt, LnsBill lnsBill) {
        if(capAmt.isNegative())
            capAmt.selfSub(capAmt.getVal());//封顶值为负，置零


        //动态封顶计息
        if(capAmt.sub(lnsBill.getBillBal()).isPositive())//billBal == billAmt
        {
            capAmt.selfSub(lnsBill.getBillBal());
        }
        else
        {
            lnsBill.setBillAmt(new FabAmount(capAmt.getVal()));
            //都是试算出来的账本，billAmt = billBal ,所以不需要特殊处理
            lnsBill.setBillBal(new FabAmount(capAmt.getVal()));

            capAmt.selfSub(capAmt.getVal());//剩余封顶值置为0
        }

    }
    // 排序规则 按期数排序，若期数相同，按enddate排序
    static class LnsBillCompator implements Comparator<LnsBill> {
        @Override
        public int compare(LnsBill o1, LnsBill o2) {
            if(o1.getPeriod().equals(o2.getPeriod())){
                return CalendarUtil.actualDaysBetween(o2.getEndDate(),o1.getEndDate());
            }
            return o1.getPeriod() - o2.getPeriod();
        }
    }

    /**
     *  未来期的利息账本账本特殊判断
     */
    private static void distinguishFutureBill(List<LnsBill> lnsBills, List<LnsBill> allNints,Map<Integer,List<LnsBill>> allDints
            ,Map<Integer,List<LnsBill>> allCints,FabAmount totalNint,FabAmount totalDint,String repayDay,LoanAgreement loanAgreement)
    {
        for(LnsBill lnsBill : lnsBills){
            //利息账本只取 当前期和历史期的
            if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType())
                    && CalendarUtil.after(repayDay,lnsBill.getStartDate() ))
            {
                if(!VarChecker.isEmpty(lnsBill.getRepayDateInt())
                        &&!ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()))
                    totalNint.selfAdd(lnsBill.getRepayDateInt());
                else
                    totalNint.selfAdd(lnsBill.getBillAmt());

                allNints.add(lnsBill);
            }
            else distDint(allDints, allCints, totalDint, lnsBill);
        }
    }

    /**
     *  区分罚息利息账本
     */
    private static void distinguishBill(List<LnsBill> lnsBills, List<LnsBill> allNints, Map<Integer,List<LnsBill>> allDints
            , Map<Integer,List<LnsBill>>allCints,FabAmount totalNint,FabAmount totalDint)
    {
        for(LnsBill lnsBill : lnsBills){
            if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBill.getBillType()))
            {
                totalNint.selfAdd(lnsBill.getBillAmt());
                allNints.add(lnsBill);
            }
            else {
                distDint(allDints, allCints, totalDint, lnsBill);
            }
        }
    }

    /**
     *  区分罚息复利账本
     * @param allDints
     * @param allCints
     * @param totalDint
     * @param lnsBill
     */
    private static void distDint(Map<Integer, List<LnsBill>> allDints, Map<Integer, List<LnsBill>> allCints, FabAmount totalDint, LnsBill lnsBill) {
        if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_GINT)
                .contains(lnsBill.getBillType())){
            totalDint.selfAdd(lnsBill.getBillAmt());
            if(allDints.get(lnsBill.getPeriod())==null)
                allDints.put(lnsBill.getPeriod(),  new ArrayList<LnsBill>());
            allDints.get(lnsBill.getPeriod()).add(lnsBill );
        }else  if(ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(lnsBill.getBillType())){
            totalDint.selfAdd(lnsBill.getBillAmt());
            if(allCints.get(lnsBill.getPeriod())==null)
                allCints.put(lnsBill.getPeriod(),  new ArrayList<LnsBill>());
            allCints.get(lnsBill.getPeriod()).add(lnsBill );
        }
    }

    /**
     * 实际还款日封顶 增加不用标记 大于实际还款日的结息明细登记簿中的罚息明细
     */
    public static  void offDtlByRealDate(String receiptNo,TranCtx ctx) throws FabException{

        if(VarChecker.isEmpty(ctx.getRequestDict("realDate")))
            return;
        Map<String,Object> param = new HashMap<>();
        //按账号查询
        param.put("receiptno", receiptNo);
        param.put("brc", ctx.getBrc());
        param.put("trandate", ctx.getRequestDict("realDate").toString());
        /* delete from  LNSPENINTPROVREGDTL where  brc = :brc and RECEIPTNO = :receiptno and billtype in ('CINT','DINT','GINT') and
                    trandate > :trandate*/
        try{
            DbAccessUtil.execute("Lnspenintprovregdtl.updateOff", param);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS103", "Lnspenintprovreg");
        }

    }




    /**
     *  计算房抵贷封顶值
     * @return 封顶值
     */
    public static  FabAmount  calculateCappingVal(LoanAgreement loanAgreement ) {
        return   new FabAmount(BigDecimal.valueOf(loanAgreement.getInterestAgreement().getCapRate())
                .divide(new BigDecimal(366), 20 ,BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal()))
                .multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(loanAgreement.getContract().getContractStartDate(), loanAgreement.getContract().getContractEndDate())))
                .setScale(2,BigDecimal.ROUND_HALF_UP ).doubleValue());
    }

}

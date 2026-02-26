package com.suning.fab.loan.utils;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.suning.fab.loan.bo.InvestInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.tup4j.account.AcctOperationMemo;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.AccountOperationMemoBuilder;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705 MYP
 * @Date Created in 10:12 2019/5/15
 * @see
 */
public class AccountingModeChange {

    /**
     *  幂等辅助表数据保存
     * @param ctx  tranctx
     * @param acctno 借据号
     * @param key 键
     * @param brief 描述
     * @param value 值
     * @throws FabException  数据库插入异常
     */
    public static void saveInterfaceEx(TranCtx ctx, String acctno, String key, String brief, String value)throws FabException {
        TblLnsinterfaceex lnsinterface = new TblLnsinterfaceex(ctx.getTranDate(), ctx.getSerSeqNo(), ctx.getBrc(),
                acctno, key, brief, value, "", "", "");
        insertLnsinterfaceex(lnsinterface);
    }
    

    public static void createInvesteeDetail(TranCtx ctx, Integer txseqNo, String acctNo, InvestInfo investInfo, String tranType, FabAmount totalAmt) throws FabException{
    	TblLnsinvesteedetail lnsinvesteedetail = new TblLnsinvesteedetail();
    	lnsinvesteedetail.setTrandate(ctx.getTranDate());
    	lnsinvesteedetail.setSerseqno(ctx.getSerSeqNo());
    	lnsinvesteedetail.setTxseqno(txseqNo);
    	lnsinvesteedetail.setBrc(ctx.getBrc());
    	lnsinvesteedetail.setAcctno(acctNo);
    	lnsinvesteedetail.setInvesteeid(investInfo.getInvesteeId());
    	//存放资金方借据号
        lnsinvesteedetail.setNewinvestee(investInfo.getNewInvestee());
    	lnsinvesteedetail.setInvesteeno(investInfo.getInvesteeNo());
    	lnsinvesteedetail.setNewinvesteeno("");
    	lnsinvesteedetail.setPeriod(0);
    	
    	lnsinvesteedetail.setInvesteeprin(investInfo.getInvesteePrin().getVal());
    	lnsinvesteedetail.setInvesteeint(investInfo.getInvesteeInt().getVal());
    	lnsinvesteedetail.setInvesteedint(investInfo.getInvesteeDint().getVal());
    	lnsinvesteedetail.setInvesteefee(investInfo.getInvesteeFee().getVal());
    	lnsinvesteedetail.setTrancode(ctx.getTranCode());
    	lnsinvesteedetail.setTrantype(tranType);
    	lnsinvesteedetail.setTotalamt(totalAmt.getVal());
    	lnsinvesteedetail.setNintflag("");
    	lnsinvesteedetail.setPrinflag("");
    	lnsinvesteedetail.setInvesteeflag(investInfo.getInvesteeFlag());
    	lnsinvesteedetail.setDiffnint(0.00);
    	lnsinvesteedetail.setDiffprin(0.00);
        lnsinvesteedetail.setReserv1("");
    	lnsinvesteedetail.setReserv2(PubDictUtils.seekInvesteeFlag(ctx));
        //存放资金方占比
        lnsinvesteedetail.setReserve(investInfo.getReserv());
        insertLnsinvesteedetail(lnsinvesteedetail);
    }


   /* public static void createInvesteeDetail(TranCtx ctx, Integer txseqno,  String acctno, String investeeid,String outSerialNo, Double investeeprin) throws FabException {
        TblLnsinvesteedetail lnsinvesteedetail = new TblLnsinvesteedetail(ctx.getTranDate(),ctx.getSerSeqNo() , txseqno, ctx.getBrc(), acctno,
                investeeid,"" , VarChecker.isEmpty(outSerialNo) ? "" : outSerialNo,"" ,0 ,investeeprin ,0.0 , 0.00, 0.00,
                ctx.getTranCode() ,"KH", investeeprin,"" ,"" , "", 0.00, 0.00, "","" ,"" );
        insertLnsinvesteedetail(lnsinvesteedetail);
    }*/


   /* public static void repayInvesteeDetail(TranCtx ctx, Integer txseqno,  String acctno, String investeeid,String outSerialNo, Double investeeprin, Double investeeint, Double investeedint,Double totalamt,String investeeflag) throws FabException {
        TblLnsinvesteedetail  lnsinvesteedetail = new TblLnsinvesteedetail(ctx.getTranDate(),ctx.getSerSeqNo() , txseqno, ctx.getBrc(), acctno,
                investeeid,"" , VarChecker.isEmpty(outSerialNo) ? "" : outSerialNo,"" ,0 ,investeeprin ,investeeint , investeedint, 0.00,
                ctx.getTranCode(),"HK" , totalamt,"" ,"" , investeeflag, 0.00, 0.00, "","" ,"" );
        insertLnsinvesteedetail(lnsinvesteedetail);
    }*/
    

   /* public static void repayInvesteeDetailQc(TranCtx ctx, Integer txseqno,  String acctno, String investeeid,String outSerialNo, Double investeeprin, Double investeeint, Double investeedint, Double investeedFee,Double totalamt,String investeeflag) throws FabException {
        TblLnsinvesteedetail  lnsinvesteedetail = new TblLnsinvesteedetail(ctx.getTranDate(),ctx.getSerSeqNo() , txseqno, ctx.getBrc(), acctno,
                investeeid,"" , VarChecker.isEmpty(outSerialNo) ? "" : outSerialNo,"" ,0 ,investeeprin ,investeeint , investeedint, investeedFee,
                ctx.getTranCode(),"HK" , totalamt,"" ,"" , investeeflag, 0.00, 0.00, "","" ,"" );
        insertLnsinvesteedetail(lnsinvesteedetail);
    }*/
    
    //资金方收入结转
    public static void incInvesteeDetail(TranCtx ctx, String intflag,String prinflag,Double diffint, Double diffprin) throws FabException {
        Map<String,Object> param = new HashMap<>();
        param.put("intflag",intflag );
        param.put("prinflag", prinflag);
        param.put("diffint", diffint);
        param.put("diffprin", diffprin);
        param.put("trandate", ctx.getTranDate());
        param.put("serseqno", ctx.getSerSeqNo());
        /*
        update lnsinvesteedetail set intflag = :intflag , prinflag = :prinflag , diffint = :diffint , diffprin = :diffprin  where trandate = :trandate and serseqno = :serseqno and trantype = 'HK'
         */
        try {
            DbAccessUtil.execute("AccountingMode.lnsinvesteedetail_update", param);
        } catch (FabException e) {
            throw new FabException(e, "SPS102", "lnsinvesteedetail_update");
        }
    }

    /**
     * 保存预收户/债务公司的明细
     * @param ctx  tranctx
     * @param txseqno 子序号
     * @param acctno 借据号
     * @param customid 债务公司/预收户
     * @param accsrccode 预收户类型 N-预收户 D-债务公司
     * @param custtype 客户类型 PERSON-对私 COMPANY-对公
     * @param name 户名
     * @param amount 金额
     * @param cdflag 增减标识  sub减  add加
     * @throws FabException 插表异常
     */
    public static void saveLnsprefundsch(TranCtx ctx, Integer txseqno, String acctno, String customid, String accsrccode, String custtype, String name, Double amount, String cdflag)throws FabException  {
        //如果金额为0，就不需要存预收账户明细表了
        if(new FabAmount(amount).isZero())
            return;
        TblLnsprefundsch lnsprefundsch = new TblLnsprefundsch(ctx.getTranDate(), ctx.getSerSeqNo(), txseqno, ctx.getBrc(), acctno, customid, accsrccode, custtype, name, amount, ctx.getTranCode(), cdflag, "","" , "");
        insertLnsprefundsch(lnsprefundsch);
    }
    public static void savePrefundschDebt(TranCtx ctx, Integer txseqno, String acctno, String customid, String accsrccode, String custtype, String name, Double amount, String cdflag,String reserv)throws FabException  {
        //如果金额为0，就不需要存预收账户明细表了
        if(new FabAmount(amount).isZero())
            return;
        TblLnsprefundsch lnsprefundsch = new TblLnsprefundsch(ctx.getTranDate(), ctx.getSerSeqNo(), txseqno, ctx.getBrc(), acctno, customid, accsrccode, custtype, name, amount, ctx.getTranCode(), cdflag, "","" , reserv);
        insertLnsprefundsch(lnsprefundsch);
    }
    /**
     * 扣息税金
     * @param ctx  tranctx
     * @param acctno 借据号
     * @param tranamt 交易金额
     * @param tax 税金
     * @param billtype  账本类型
     * @throws FabException 插表异常
     */
    public static void saveDiscountTax(TranCtx ctx, String acctno, Double tranamt, Double tax, String billtype)throws FabException{
        //税金类型为扣息税金  正向
        insertTaxInfo(ctx, acctno, tranamt, tax, "KX","POSITIVE", billtype, "","", 0, 0 );

    }
    //费用预提
    public static void saveFeeTax(TranCtx ctx, String acctno, Double tranamt, Double tax, String billtype)throws FabException{
        //税金类型为扣息税金  正向
        insertTaxInfo(ctx, acctno, tranamt, tax, ConstantDeclare.KEYNAME.FYYT,"POSITIVE", billtype, "","", 0, 0 );

    }
    //费用预提
    public static void saveFeeTax(TranCtx ctx, String acctno, Double tranamt, Double tax, String billtype,String reserv1)throws FabException{
        //税金类型为扣息税金  正向
        insertTaxInfo(ctx, acctno, tranamt, tax, ConstantDeclare.KEYNAME.FYYT,"POSITIVE", billtype, "","", 0, 0,reserv1 );

    }
    /**
     *  保存税金的相关信息  -- 结息税金
     * @param ctx  tranctx
     * @param acctno 借据号

     * @param bill  账本
     * @throws FabException 插表异常
     */
    public static void saveIntSetmTax(TranCtx ctx,  String acctno,  LnsBill bill)throws FabException{
        insertTaxInfo(ctx, acctno, bill.getBillAmt().getVal(), TaxUtil.calcVAT(bill.getBillAmt()).getVal(),  "JX","POSITIVE", //税金类型为结息税金  正向
                bill.getBillType(),"",bill.getTranDate().toString(), bill.getSerSeqno(), bill.getTxSeq());
    }
    /**
     * 计提税金
     * @param ctx  tranctx
     * @param acctno 借据号
     * @param tranamt 交易金额
     * @param tax 税金
     * @param billtype  账本类型
     * @throws FabException 插表异常
     */
    public static void saveProvisionTax(TranCtx ctx, String acctno, Double tranamt, Double tax,String taxtype,String interflag, String billtype)throws FabException{
       insertTaxInfo(ctx, acctno, tranamt, tax, taxtype,interflag, billtype, "","", 0, 0 );

    }
    /**
     *  利息减免 罚息减免
     * @param ctx  tranctx
     * @param acctno 借据号
     * @param reduceAmt 减免金额
     * @param bill  账本
     * @throws FabException 插表异常
     */
    public static void saveReduceTax(TranCtx ctx, String acctno, FabAmount  reduceAmt, TblLnsbill bill)throws FabException{
        if (bill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)&& (VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
                ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(bill.getBillstatus()))){
            insertTaxInfo(ctx, acctno, reduceAmt.getVal(), TaxUtil.calcVAT(reduceAmt).getVal(),  "JM","POSITIVE",
                    bill.getBilltype(),ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,bill.getTrandate().toString(), bill.getSerseqno(), bill.getTxseq());
        }else{
            insertTaxInfo(ctx, acctno, reduceAmt.getVal(), TaxUtil.calcVAT(reduceAmt).getVal(),  "JM","POSITIVE",
                    bill.getBilltype(),bill.getBillstatus(),bill.getTrandate().toString(), bill.getSerseqno(), bill.getTxseq());
        }

    }
    /**
     *  未来期的利息减免 罚息减免
     * @param ctx  tranctx
     * @param acctno 借据号
     * @param billType 账本类型
     * @param repayAdvance  账本
     * @throws FabException 插表异常
     */
    public static void saveReduceTax(TranCtx ctx, String acctno, String  billType, RepayAdvance repayAdvance)throws FabException{
        insertTaxInfo(ctx, acctno, repayAdvance.getBillAmt().getVal(), TaxUtil.calcVAT( repayAdvance.getBillAmt()).getVal(),  "JM","POSITIVE", //税金类型为结息税金  正向
                billType,ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,repayAdvance.getBillTrandate(), repayAdvance.getBillSerseqno(), repayAdvance.getBillTxseq());
    }

    /**
     * FXHB:罚息还本，LXHB:利息还本
     * @param ctx  tranctx
     * @param acctno 借据号
     * @param tranamt 交易金额
     * @param tax 税金
     * @param taxtype   税金类型
     * @param billtype  账本类型
     * @throws FabException 插表异常
     */
    public static void saveSpecialTax(TranCtx ctx, String acctno, Double tranamt, Double tax, String taxtype,String billtype)throws FabException{
        //税金类型为扣息税金  正向
        insertTaxInfo(ctx, acctno, tranamt, tax, taxtype,"POSITIVE", billtype, "","", 0, 0 );

    }


    //冲销事件存储
    public static void saveWriteoffDetail(TranCtx ctx, Integer txseqno, String acctno, String eventcode, String briefCode, Double tranamt, String debtcompany, FundInvest fundInvest
    , Double amt1, Double amt2, Double amt3)throws FabException{
        saveWriteoffDetailCommon(ctx, txseqno, acctno, eventcode, briefCode, tranamt, debtcompany, fundInvest,"",amt1,amt2,amt3,"", "", "");

    }
    //冲销事件存储  资金方  渠道冲销用
    public static void saveWriteoffDetail(TranCtx ctx, Integer txseqno, String acctno, String eventcode, String briefCode, Double tranamt, String debtcompany, FundInvest fundInvest
            ,String investeeId , Double amt1, Double amt2, Double amt3,String reserv2)throws FabException{
        saveWriteoffDetailCommon(ctx, txseqno, acctno, eventcode, briefCode, tranamt, debtcompany, fundInvest,investeeId,amt1,amt2,amt3,"", reserv2, "");

    }
    //多备用字段reserv2 CINSURANCE-退保
    public static void saveWriteoffDetail(TranCtx ctx, Integer txseqno, String acctno, String eventcode, String briefCode, Double tranamt, String debtcompany, FundInvest fundInvest, Double amt1,String reserv2 )throws FabException{
        saveWriteoffDetailCommon(ctx, txseqno, acctno, eventcode, briefCode, tranamt, debtcompany, fundInvest,"",amt1,0.00,0.00,"", reserv2, "");

    }
    //多备用字段reserv1 WRITOFFFEE-管理费冲销
    public static void saveWriteoffDetail(TranCtx ctx, Integer txseqno, String acctno, String eventcode, String briefCode, Double tranamt, String debtcompany, FundInvest fundInvest, Double amt1,Double amt2,Double amt3,String reserv2 )throws FabException{
        saveWriteoffDetailCommon(ctx, txseqno, acctno, eventcode, briefCode, tranamt, debtcompany, fundInvest,"",amt1,amt2,amt3,"", reserv2, "");
    }
    //多备用字段reserv1 和 reserv CINSURANCE-退保
    public static void saveWriteoffDetail(TranCtx ctx, Integer txseqno, String acctno, String eventcode, String briefCode, Double tranamt, String debtcompany, FundInvest fundInvest,Double amt1, String reserv2,String reserve )throws FabException{
        saveWriteoffDetailCommon(ctx, txseqno, acctno, eventcode, briefCode, tranamt, debtcompany, fundInvest,"",amt1,0.00,0.00,"", reserv2, reserve);
    }

    /**
     *  公共方法
     */
    private static void saveWriteoffDetailCommon(TranCtx ctx, Integer txseqno, String acctno, String eventcode, String briefCode, Double tranamt, String debtcompany, FundInvest fundInvest,
                                           String investeeId,double amt1,double amt2,double amt3,String reserv1, String reserv2, String reserve) throws FabException {
        AcctOperationMemo memo = AccountOperationMemoBuilder.generic(briefCode);
        TblLnswriteoffdetail lnswriteoffdetail = new TblLnswriteoffdetail(ctx.getTranDate(), ctx.getSerSeqNo(),txseqno , ctx.getBrc(), acctno,ctx.getTranCode(),eventcode , briefCode, memo.getBreifText(), tranamt,debtcompany ,fundInvest.getChannelType()
                ,fundInvest.getFundChannel() , fundInvest.getOutSerialNo(),investeeId,amt1 ,amt2 ,amt3 ,reserv1 ,reserv2 ,reserve );
        insertLnswriteoffdetail(lnswriteoffdetail);
    }

    /**
     *   借用了brcName 作为子序号的递增  税率默认0.06
     * @param ctx Tranctx
     * @param acctno 借据号
     * @param tranamt 交易金额
     * @param tax 税金
     * @param billtype 账单类型
     * @param billtrandate 账单账务日期
     * @param billserseqno  账单账务流水
     * @param billtxseq 账单子序号
     * @throws FabException 插表异常
     */
    private static void insertTaxInfo(TranCtx ctx,   String acctno, Double tranamt, Double tax, String taxtype,String interflag,String billtype,String billstatus, String billtrandate, Integer billserseqno, Integer billtxseq) throws FabException{
        if(VarChecker.isEmpty(ctx.getBrcName())||"交易成功".equals(ctx.getBrcName()))
            ctx.setBrcName("1");
        TblLnstaxdetail lnstaxdetail = new TblLnstaxdetail(ctx.getTranDate(), ctx.getSerSeqNo(),Integer.parseInt(ctx.getBrcName()), ctx.getBrc(), acctno, tranamt, tax, 0.06,taxtype,interflag, billtype,billstatus ,billtrandate, billserseqno, billtxseq, "", "", "");
        ctx.setBrcName(String.valueOf(Integer.parseInt(ctx.getBrcName())+1));
        try {
            DbAccessUtil.execute("AccountingMode.lnstaxdetail_insert", lnstaxdetail);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100","lnstaxdetail");
        }
    }
    private static void insertTaxInfo(TranCtx ctx,   String acctno, Double tranamt, Double tax, String taxtype,String interflag,String billtype,String billstatus, String billtrandate, Integer billserseqno, Integer billtxseq,String reserv1) throws FabException{
        if(VarChecker.isEmpty(ctx.getBrcName())||"交易成功".equals(ctx.getBrcName()))
            ctx.setBrcName("1");
        TblLnstaxdetail lnstaxdetail = new TblLnstaxdetail(ctx.getTranDate(), ctx.getSerSeqNo(),Integer.parseInt(ctx.getBrcName()), ctx.getBrc(), acctno, tranamt, tax, 0.06,taxtype,interflag, billtype,billstatus ,billtrandate, billserseqno, billtxseq, reserv1, "", "");
        ctx.setBrcName(String.valueOf(Integer.parseInt(ctx.getBrcName())+1));
        try {
            DbAccessUtil.execute("AccountingMode.lnstaxdetail_insert", lnstaxdetail);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100","lnstaxdetail");
        }
    }
    /**
     * 冲销详情表insert
     * @param lnswriteoffdetail  冲销详情表
     * @throws FabException 插表异常
     */
    private static void insertLnswriteoffdetail(TblLnswriteoffdetail lnswriteoffdetail) throws FabException {
        try {
            DbAccessUtil.execute("AccountingMode.lnswriteoffdetail_insert", lnswriteoffdetail);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100","lnswriteoffdetail");
        }
    }



    /**
     * 预收明细表insert
     * @param lnsprefundsch  预收明细表
     * @throws FabException 插表异常
     */
    private static void insertLnsprefundsch(TblLnsprefundsch lnsprefundsch) throws FabException {
        try {
            DbAccessUtil.execute("AccountingMode.lnsprefundsch_insert", lnsprefundsch);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100","lnsprefundsch");
        }
    }

    /**
     * 幂等拓展表insert
     * @param lnsinterfaceex  幂等拓展表
     * @throws FabException 插表异常
     */
    private static void insertLnsinterfaceex(TblLnsinterfaceex lnsinterfaceex) throws FabException {
        try {
            DbAccessUtil.execute("AccountingMode.lnsinterfaceex_insert", lnsinterfaceex);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100","lnsinterfaceex");
        }
    }

    /**
     *  insert资金方明细表
     * @param lnsinvesteedetail  资金方明细表
     * @throws FabException 插表异常
     */
    private static void insertLnsinvesteedetail(TblLnsinvesteedetail lnsinvesteedetail) throws FabException {
        try {
            DbAccessUtil.execute("AccountingMode.lnsinvesteedetail_insert", lnsinvesteedetail);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100","lnsinvesteedetail");
        }
    }
    
    /**
     * 免息税金
     * @param ctx  tranctx
     * @param acctno 借据号
     * @param bill 账本
     * @throws FabException 插表异常
     */
    public static void saveFreeInterestTax(TranCtx ctx,  String acctno,  LnsBill bill)throws FabException{
        //税金类型为扣息税金  正向
        insertTaxInfo(ctx, acctno, bill.getTermFreeInterest().getVal(), TaxUtil.calcVAT(bill.getTermFreeInterest()).getVal(),  "MX","POSITIVE", //税金类型为结息税金  正向
        		bill.getBillType(),"",bill.getTranDate().toString(), bill.getSerSeqno(), bill.getTxSeq());

    }
    

    //现金贷免息更新主文件动态表免息金额方法
    public static void updateBasicDynMX(TranCtx ctx,Double MX,String acctNo ,String repayDate,LnsBill lnsBill) throws FabException{
        Double periodAmt=lnsBill.getTermFreeInterest().getVal();//每次减免金额进行覆盖
        //新增主文件动态拓展表
        Map<String,Object> dynExParam = new HashMap<String,Object>();
        dynExParam.put("acctno", acctNo);
        dynExParam.put("openbrc", ctx.getBrc());
        dynExParam.put("brc", ctx.getBrc());
        dynExParam.put("trandate", repayDate);
        try{
            //取主文件动态表剩余免息金额
            TblLnsbasicinfodyn lnsbasicinfoDyn  = DbAccessUtil.queryForObject("Lnsbasicinfodyn.selectByUk", dynExParam, TblLnsbasicinfodyn.class);
            Integer period=lnsBill.getPeriod();
            if (null != lnsbasicinfoDyn&&!VarChecker.isEmpty(lnsbasicinfoDyn.getTunneldata())) {
                String json= lnsbasicinfoDyn.getTunneldata();//获取每期减免金额进行累加
                FaloanJson faloanJson = FaloanJson.parseObject(json);
                faloanJson.put(BigDecimal.valueOf(faloanJson.getDouble(period.toString())).add(new BigDecimal(periodAmt)).doubleValue(),period.toString());
                //存放当期总金额
                faloanJson.put(BigDecimal.valueOf(faloanJson.getDouble(period.toString()+":Z")).add(new BigDecimal(periodAmt)).doubleValue(),period.toString()+":Z");
                faloanJson.put(MX, "MX");
                dynExParam.put("tunneldata", faloanJson.toString());
                DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfodyn_common", dynExParam);
            }
        }catch (FabSqlException e){
            throw new FabException(e, "SPS102", "lnsbasicinfoDyn");
        }
    }

    /**
     * 费用减免更新主文件扩展表
     * @param ctx
     * @param MX
     * @param acctNo
     * @param repayDate
     * @param lnsBill
     * @throws FabException
     */
    public static void updateBasicDynMF(TranCtx ctx,Double MX,String acctNo ,String repayDate,LnsBill lnsBill) throws FabException{
        Double periodAmt=lnsBill.getTermFreeInterest().getVal();//每次减免金额进行覆盖
        String feeKey=lnsBill.getBillType()+":"+lnsBill.getRepayWay()+":"+ ConstantDeclare.PARACONFIG.FREEFEE;
        //新增主文件动态拓展表
        Map<String,Object> dynExParam = new HashMap<String,Object>();
        dynExParam.put("acctno", acctNo);
        dynExParam.put("openbrc", ctx.getBrc());
        dynExParam.put("key", ConstantDeclare.PARACONFIG.EXTEND);
        try{
            //取主文件动态表剩余免息金额
            TblLnsbasicinfoex lnsbasicinfoex  = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", dynExParam, TblLnsbasicinfoex.class);
            if (null != lnsbasicinfoex&&!VarChecker.isEmpty(lnsbasicinfoex.getTunneldata())) {
                dynExParam.put("value1",lnsbasicinfoex.getValue1());
                dynExParam.put("value2",lnsbasicinfoex.getValue2());
                dynExParam.put("value3",lnsbasicinfoex.getValue3());
                Integer period=lnsBill.getPeriod();
                String json= lnsbasicinfoex.getTunneldata();//获取每期减免金额进行累加
                FaloanJson faloanJson = FaloanJson.parseObject(json);
                //费用的期数 当次 和 期数总减免费用金额
                faloanJson.put(BigDecimal.valueOf(faloanJson.getDouble(feeKey+":"+period.toString()+":Z")).add(new BigDecimal(periodAmt)).doubleValue(),feeKey+":"+period.toString()+":Z");
                faloanJson.put(BigDecimal.valueOf(faloanJson.getDouble(feeKey+":"+period.toString()+":F")).add(new BigDecimal(periodAmt)).doubleValue(),feeKey+":"+period.toString()+":F");
                faloanJson.put(MX, feeKey);
                dynExParam.put("tunneldata", faloanJson.toString());
                DbAccessUtil.execute("Lnsbasicinfoex.updateByUk", dynExParam);
            }
        }catch (FabSqlException e){
            throw new FabException(e, "SPS102", "lnsbasicinfoDyn");
        }
    }

}

package com.suning.fab.loan.workunit;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

//import com.sun.tools.internal.jxc.ap.Const;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.bo.RpyList;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.*;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;

/**
 * @param
 * @author LH
 * @version V1.0.1
 * @return
 * @exception
 * @see -还款本息
 */
@Scope("prototype")

@Repository
public class Lns211 extends WorkUnit {
    String serialNo; // 幂等流水号
    String acctNo; // 本金账号
    String repayAcctNo;
    String ccy;
    String cashFlag;
    String termDate;
    String tranCode;
    String bankSubject; // 帐速融 银行直接还款科目
    FabAmount repayAmt;
    FabAmount feeAmt;
    FabAmount refundAmt;
    String repayChannel;
    String memo;
    String brc;
    Map<String, FabAmount> repayAmtMap;
    Integer subNo;
    String outSerialNo;
    String settleFlag;
    String compensateFlag; //代偿
    String platformId; //平台方代码
    ListMap pkgList;//债务公司明细
    ListMap pkgList1;//资金方明细
	String  repayList;  //还款明细2020-06-08


    LnsBillStatistics billStatistics;
    FabAmount prinAmt;
    FabAmount nintAmt;
    FabAmount dintAmt;
    String endFlag;
    LoanAgreement loanAgreement;
    String priorType;
    String prdId;
    String custType;
    FabRate cleanFeeRate;
    FabAmount reduceIntAmt;             //利息减免金额
    FabAmount reduceFintAmt;            //罚息减免金额
    FabAmount nintRed;                  //总罚息减免金额
    FabAmount fnintRed;                 //总利息减免金额
    String  intReduceList;
    String  fintReduceList;
    String  prinReduceList;

    String realDate;                    //实际扣款日
    String switchloanType;				//借新还旧类型 1-房抵贷债务重组  2-任性付账单分期  3-任性付最低还款额
    FabAmount couponIntAmt =new FabAmount(0.00) ;//免息金额
    String   discountIntFlag ="N"       ;//贴息标志
    private Integer txseqno = 0; 		//预收明细登记簿子序号
    //代传资金方
    String exinvesteeId = null;
    //是否为逾期还款
    private boolean ifOverdue = false; //add at 2019-03-25

    //已减利息
    private FabAmount subIntAmt=new FabAmount(0.00);

    //已减罚息
    private FabAmount subFintAmt=new FabAmount(0.00);

    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;
    @Autowired
    LoanEventOperateProvider eventProvider;

    @Override
    public void run() throws Exception {
        int pkgflag = 0;
        int listsize;
        int repaysize = 0;
        String revserv1 = "";
        TranCtx ctx = getTranctx();
//		ListMap pkgList = ctx.getRequestDict("pkgList");
        //利息 罚息减免金额赋值
        if(ctx.getRequestDict("reduceIntAmt")!=null){
            subIntAmt=new FabAmount(ctx.getRequestDict("reduceIntAmt"));
        }
        if(ctx.getRequestDict("reduceFintAmt")!=null){
            subFintAmt=new FabAmount(ctx.getRequestDict("reduceFintAmt"));
        }


        //还款本息摘要码透传 2020-05-25
        if( !ConstantDeclare.BRIEFCODE.isBriefCode(memo) )
        	memo = ConstantDeclare.BRIEFCODE.HKBX;
        //switchloanType借新还旧类型透传 2020-06-02
        if( ConstantDeclare.BRIEFCODE.JXHJ.equals(memo) )
        	revserv1 = switchloanType;
        else
        	revserv1 = queryExinvesteeId();
        
        if ("3".equals(compensateFlag)) {
            //借新还旧时，避免使用到放款的pkgList，所以设置值为空
            pkgList = null;
            pkgList1 = null;
        }
        else
        {
        	pkgList = ctx.getRequestDict("pkgList");
        	pkgList1 = ctx.getRequestDict("pkgList1");
        }

        Map<String, FabAmount> repayAmtMap = new HashMap<String, FabAmount>();
        Map<String, Map<String,FabAmount>> repayAmtList = new HashMap<String, Map<String,FabAmount>>();

        FabAmount sumAmt = new FabAmount();

        TblLnsinvesteedetail lnsinvesteedetail = null;
        Boolean isZrdc = false;    //中融代偿
        Boolean isCurrentTerm = true;
        Boolean isReduceFee = false;
        // 幂等登记薄
        TblLnsinterface lnsinterface = new TblLnsinterface();
        lnsinterface.setTrandate(ctx.getTermDate());
        lnsinterface.setSerialno(getSerialNo());
        lnsinterface.setAccdate(ctx.getTranDate());
        lnsinterface.setSerseqno(ctx.getSerSeqNo());
        lnsinterface.setTrancode(ctx.getTranCode());
        if (!VarChecker.isEmpty(getRepayAcctNo())) // 帐速融 银行直接还款
            lnsinterface.setUserno(getRepayAcctNo());

        lnsinterface.setAcctno(getAcctNo());
        lnsinterface.setBrc(ctx.getBrc());
        lnsinterface.setBankno(getOutSerialNo());
        lnsinterface.setTranamt(0.00); // 用还款总金额TODO
        lnsinterface.setSumrint(0.00);
        lnsinterface.setSumramt(0.00);
        lnsinterface.setSumrfint(0.00);
        //利息罚息减免登记 2019-03-18
        if (!VarChecker.isEmpty(nintRed)) {
            lnsinterface.setSumdelint(nintRed.getVal());
        }
        if (!VarChecker.isEmpty(fnintRed)) {
            lnsinterface.setSumdelfint(fnintRed.getVal());
        }


        //提前结清手续费 2019-02-19
        if (!VarChecker.isEmpty(feeAmt))
            lnsinterface.setReserv3(feeAmt.getVal().toString());

        if (!VarChecker.isEmpty(repayChannel))
            lnsinterface.setReserv5(repayChannel);

        //fundChannel登记  2018-01-09
        if (!VarChecker.isEmpty(bankSubject))
            lnsinterface.setBillno(bankSubject);

        //资金方信息存入reserv4  2019-02-15
//		ListMap pkgList1 = ctx.getRequestDict("pkgList1");
        String chtype = "";
        StringBuffer reserv4 = new StringBuffer();
        if (null != pkgList1 && pkgList1.size() > 0) {
            //暂不支持多资金方
//            if (pkgList1.size() > 1) {
//                throw new FabException("LNS133");
//            }
            // 2021-06-21 还款支持多资金方

            for (PubDict pkg : pkgList1.getLoopmsg()) {
                if (VarChecker.isEmpty(PubDict.getRequestDict(pkg, "investeeId"))) {
                    throw new FabException("LNS055", "资金方");
                }

                if ("03".equals(PubDict.getRequestDict(pkg, "investeeFlag"))) {
                    chtype = "chtype";
                }
                reserv4.append(PubDict.getRequestDict(pkg, "investeeId") + "|");
            }

            reserv4.deleteCharAt(reserv4.length() - 1);
            lnsinterface.setReserv4(reserv4.toString());
        }
        // 带债务公司的还款
        if (pkgList != null && pkgList.size() > 0)
            pkgflag = 1;
        // 根据账号生成账单
        loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx, loanAgreement);
        //幂等reserv6 登记债务公司代码
        String debtcompany = "";
        if (pkgflag == 1 && "3010016".equals(loanAgreement.getPrdId())) {
            if (pkgList.getLoopmsg().size() > 1)
                throw new FabException("LNS169", "债务公司应只有一条，", "债务公司");
            for (PubDict pkg : pkgList.getLoopmsg()) {
                lnsinterface.setReserv6(PubDict.getRequestDict(pkg, "debtCompany"));//只有一个
                debtcompany = PubDict.getRequestDict(pkg, "debtCompany");
                break;
            }
        }
        try {
            // 借新还旧在还款的时候不插入幂等表，放款101插入幂等表
//            if (!VarChecker.asList("3").contains(compensateFlag))
            if (VarChecker.asList("3").contains(compensateFlag)) {
            	lnsinterface.setTrancode("SWITCH");
            	lnsinterface.setAccdate(ctx.getTranDate().toString().replaceFirst("2", "1"));
            }
                DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
        } catch (FabSqlException e) {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("serialno", getSerialNo());
                params.put("trancode", ctx.getTranCode());

                try {
                    lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", params,
                            TblLnsinterface.class);
                } catch (FabSqlException f) {
                    throw new FabException(f, "SPS103", "lnsinterface");
                }
                dintAmt = new FabAmount(lnsinterface.getSumrfint());
                nintAmt = new FabAmount(lnsinterface.getSumrint());
                prinAmt = new FabAmount(lnsinterface.getSumramt());
                endFlag = lnsinterface.acctFlag2Endflag();

              //展示还款明细准备数据 2020-06-08
                if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
                {
                	Map<String, Object> param = new HashMap<>();
//                    param.put("acctno", acctNo);
//                    param.put("brc", getTranctx().getBrc());
                    param.put("trandate", lnsinterface.getAccdate());
                    param.put("serseqno", lnsinterface.getSerseqno());
                    param.put("key", ConstantDeclare.KEYNAME.HKMX);
                    TblLnsinterfaceex interfaceex;
                    try {
                        interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex_hkmx", param, TblLnsinterfaceex.class);
                    } catch (FabSqlException e1) {
                        throw new FabException(e1, "SPS103", "query_lnsinterfaceex");
                    }
                    
                    if (interfaceex != null){
                        repayList = interfaceex.getValue();
                        //查询还款明细 对压缩的字段进行解压
                        if(!VarChecker.isEmpty(repayList)&&!repayList.contains("{")){
                            repayList=StringDecodeUtil.decompressData(repayList);
                        }
                    }
                    else
                    	repayList = null;
                }
                
                throw new FabException(e, TRAN.IDEMPOTENCY);
            } else
                throw new FabException(e, "SPS100", "lnsinterface");
        }


        if (outSerialNo != null)
            loanAgreement.getFundInvest().setOutSerialNo(getOutSerialNo());
        if (getBankSubject() != null)
            loanAgreement.getFundInvest().setFundChannel(getBankSubject());
        if (getRepayChannel() != null)
            loanAgreement.getFundInvest().setChannelType(getRepayChannel());
        if (getRepayAmt() == null || !getRepayAmt().isPositive()) {
            LoggerUtil.info("上送还款金额为零");
            prinAmt = new FabAmount(0.00);
            nintAmt = new FabAmount(0.00);
            dintAmt = new FabAmount(0.00);
            endFlag = "1";
            return;
            // throw new FabException("LNS029");
        }
        LoggerUtil.debug("REPAYAMT1:" + getRepayAmt().getVal()); // 顾客上送还款金额

        // 读取主文件判断贷款状态(开户,销户) 判断贷款形态(正常,逾期,呆滞,呆账)取得对应还款顺序
        Map<String, Object> bparam = new HashMap<String, Object>();
        bparam.put("acctno", getAcctNo());
        bparam.put("openbrc", ctx.getBrc());
        TblLnsbasicinfo lnsbasicinfo;
        try {
            lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", bparam, TblLnsbasicinfo.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbasicinfo");
        }
        if (null == lnsbasicinfo) {
            throw new FabException("SPS104", "lnsbasicinfo");
        }

        if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())) {
            LoggerUtil.debug("该账户已销户或已核销");
            throw new FabException("ACC108", acctNo);
        }


        setPrdId(lnsbasicinfo.getPrdcode());
        setCustType(lnsbasicinfo.getCusttype());

        // 读取贷款账单
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", getAcctNo());
        param.put("brc", ctx.getBrc());
        List<TblLnsbill> billList = new ArrayList<TblLnsbill>();
        try {
            billList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", param, TblLnsbill.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbill");
        }

        //筛选账本  非费用账本  核销的优先还本金
        billList = LoanFeeUtils.filtFee(billList);
        if (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbasicinfo.getLoanstat())
                || ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbasicinfo.getLoanstat())||(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(ThreadLocalUtil.get(ConstantDeclare.PARACONFIG.LOANSTAT,""))&& CalendarUtil.actualDaysBetween(lnsbasicinfo.getContduedate(), ctx.getTranDate()) > 90)) {
            billList = LoanRepayOrderHelper.dullBad(billList);
        } else {// 正常罚息本顺序还款
            billList = LoanRepayOrderHelper.smallPrinSmallInt(billList);
        }
        listsize = billList.size();
        LoggerUtil.debug("LNSBILL:billList:" + billList.size());
        Map<String, Object> upbillmap = new HashMap<String, Object>();


        //结清到期实际还款日需要
        FabAmount actualTermAmt = new FabAmount(0.00);
        FabAmount initRepayAmt = new FabAmount(getRepayAmt().getVal());


        if (VarChecker.isEmpty(realDate))
            realDate = ctx.getTranDate();
        for (int i = 0; i < billList.size(); i++) {
            TblLnsbill lnsbill = billList.get(i);

            //2019-11-29 当期结清判断
            if ("2".equals(settleFlag)) {
                actualTermAmt.selfAdd(lnsbill.getBillbal());
            }

            LoggerUtil.debug("LNSBILL:" + lnsbill.getAcctno() + "|" + lnsbill.getPeriod() + "|"
                    + lnsbill.getBillstatus() + "." + lnsbill.getBilltype() + "|" + lnsbill.getBillbal());
            //如果有逾期或者罚息账本 则还款为逾期还款  add at 2019-03-25
            if (!ifOverdue && lnsbill.isOverdue())
                ifOverdue = true;
            //add end
            upbillmap.put("actrandate", ctx.getTranDate());
            upbillmap.put("tranamt", getRepayAmt().getVal());

            upbillmap.put("trandate", lnsbill.getTrandate());
            upbillmap.put("serseqno", lnsbill.getSerseqno());
            upbillmap.put("txseq", lnsbill.getTxseq());

            Map<String, Object> repaymap;
            try {
//              if (VarChecker.asList("2", "3").contains(compensateFlag)) {
            	if (VarChecker.asList("2", "3","4").contains(compensateFlag)) {
                    // 代偿的为COMPEN，借新还旧为：SWITCH
//                  upbillmap.put("billproperty", "2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH);
            		upbillmap.put("billproperty", "2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ("3".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL));
                    repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_dcrepay", upbillmap);
                } else {
                    repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_repay", upbillmap);
                }
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS103", "lnsbill");
            }
            if (repaymap == null) {
                throw new FabException("SPS104", "lnsbill");
            }

            Double minAmt = Double.parseDouble(repaymap.get("minamt").toString());
            LoggerUtil.debug("minAmt:" + minAmt);
            if (minAmt.equals(0.00)) {
                LoggerUtil.debug("该账单金额已经为零");
                repaysize++;
                continue;
            }
            FabAmount amount = new FabAmount(minAmt);
            LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), lnsbill.getBilltype(), lnsbill.getBillstatus(),
                    new FabCurrency());
            lnsAcctInfo.setMerchantNo(getRepayAcctNo());
            lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
            lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
            lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
            lnsAcctInfo.setCancelFlag(lnsbill.getCancelflag());
            // lnsAcctInfo.setBillType(lnsbill.getBilltype())
            // lnsAcctInfo.setLoanForm(lnsbill.getBillstatus())
            // compensateFlag=3:借新还旧时摘要码变为：JXHJ
            sub.operate(lnsAcctInfo, null, amount, loanAgreement.getFundInvest(), memo,
                    lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), ctx);

            // 如果是本金或者利息呆滞呆账状态,需要转回逾期状态,然后还逾期本金利息,能还多少转回多少
            /**
             * 一般是这样的，真正的业务场景是苏宁生态圈的用户,借款人一般都是易购的供应商,都是易购欠供应商的货款，然后供应商找保理公司贷款,易购到期把货款给保理公司
             * 记的就是保理公司的账
             * 有债务公司的还款本金事件是按债务公司的明细抛的,除了本金  其他都是按账单循环抛事件
             * 利息和罚息不一定，一般是借款人还,也有债务公司还的，就是买方付息
             * 20190521|14050183
             */
            if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
                    ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
                    && lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)) {
                LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_NINT,
                        ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
                lnsOpAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                lnsOpAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
                lnsOpAcctInfo.setCancelFlag(lnsbill.getCancelflag());
                eventProvider.createEvent(ConstantDeclare.EVENT.NINTRETURN, amount, lnsAcctInfo, lnsOpAcctInfo,
                        loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.LXZH, ctx);
                eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
                        loanAgreement.getFundInvest(), memo, ctx, null,
                        lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), revserv1, debtcompany, "", chtype);
               
            } else if ((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
                    ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(lnsbill.getBillstatus()))
                    && lnsbill.getBilltype().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)) {
                LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
                        ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU, new FabCurrency());
                lnsOpAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                lnsOpAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                lnsOpAcctInfo.setPrdCode(loanAgreement.getPrdId());
                lnsOpAcctInfo.setCancelFlag(lnsbill.getCancelflag());
                eventProvider.createEvent("LOANRETURN", amount, lnsAcctInfo, lnsOpAcctInfo,
                        loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.BJZH, ctx);
                eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsOpAcctInfo, null,
                        loanAgreement.getFundInvest(), memo, ctx, null,
                        lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), revserv1, debtcompany, "", chtype);
            } else {
                // 如果是无追保理,并且是本金户表外不抛事件,其他均抛事件
                eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, amount, lnsAcctInfo, null,
                        loanAgreement.getFundInvest(), memo, ctx, null,
                        lnsbill.getTrandate().toString(), lnsbill.getSerseqno(), lnsbill.getTxseq(), revserv1, debtcompany, "", chtype);
            }


            getRepayAmt().selfSub(amount);
            LoggerUtil.debug("REMAINAMT:" + getRepayAmt().getVal());

            LoanRpyInfoUtil.addRpyInfo(repayAmtMap, lnsbill.getBilltype(), lnsbill.getBillstatus(), amount);
            //展示还款明细准备数据 2020-06-08
            if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
            	
                //还款明细添加账本的状态 为区分利息状态做准备
            	LoanRpyInfoUtil.addRpyList(repayAmtList, lnsbill.getBilltype(), lnsbill.getPeriod().toString(), amount,lnsbill.getBillstatus());
            

            
            if (!getRepayAmt().isPositive()) {
                Double repayBall = Double.parseDouble(repaymap.get("billbal").toString());
                if (repayBall.equals(minAmt)) { // 余额等于发生额 该条账单处理结束 加1
                    repaysize++;
                    if ("2".equals(compensateFlag) && i != billList.size() - 1) {
                        //代偿历史期需要结清且不允许提前还款
                        throw new FabException("LNS177");
                    }
                } else if ("2".equals(compensateFlag)) {
                    //代偿历史期需要结清且不允许提前还款
                    throw new FabException("LNS177");
                }
                break;
            }
            repaysize++;
        }
        LoggerUtil.debug("LNSBILL:repaysize:" + repaysize);

        if ("2".equals(settleFlag) &&
                !initRepayAmt.sub(actualTermAmt).isZero())
            throw new FabException("LNS202");

        endFlag = "1";

        //2020-03-11  中融代偿只允许还到当天已到期金额
        if ("2".equals(compensateFlag)) {
            //查询资金方登记簿
            param.put("acctno", acctNo);
            param.put("brc", ctx.getBrc());
            param.put("trantype", ConstantDeclare.TRANTYPE.KH);

            try {
                lnsinvesteedetail = DbAccessUtil.queryForObject("AccountingMode.query_lnsinvesteedetail", param, TblLnsinvesteedetail.class);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS103", "lnsbasicinfo");
            }

            if (!VarChecker.isEmpty(lnsinvesteedetail)) {
                String merchantDCList = GlobalScmConfUtil.getProperty("merchantDCList", "");
                List<String> dcList = Arrays.asList(merchantDCList.split(","));
                if (dcList.contains(lnsinvesteedetail.getInvesteeid()))
                    isZrdc = true;
            }

            if ("2512628".equals(lnsbasicinfo.getPrdcode()))
                isZrdc = true;

//			//资金方70开头为中融
//			if( !VarChecker.isEmpty(lnsinvesteedetail) &&
//				"70".equals(lnsinvesteedetail.getInvesteeid().substring(0, 2)) )
//				isZrdc = true;

            if (isZrdc && getRepayAmt().isPositive())
                throw new FabException("LNS178");
        }


        //普通的如果当日大于等于合同结束日期,账单结清,贷款销户
        //胡祎的如果无合同余额,账单结清,贷款销户
        if ((!Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate()))
                || new FabAmount(lnsbasicinfo.getContractbal()).isZero())
                && listsize == repaysize) {
            endFlag = "3";
            LoggerUtil.info("AcctNo:{}结清", lnsbasicinfo.getAcctno());

            Map<String, Object> upbasicmap = new HashMap<String, Object>();
            upbasicmap.put("openbrc", lnsbasicinfo.getOpenbrc());
            upbasicmap.put("acctno", lnsbasicinfo.getAcctno());
            upbasicmap.put("loanstat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
            upbasicmap.put("modifyDate", ctx.getTranDate());
            try {
                DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstat", upbasicmap);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "lnsbasicinfo");
            }
        }


        LoggerUtil.debug("[" + getAcctNo() + "]" + "主流程账单还款:" + "|" + getRepayAmt().getVal());
        // 当期及未来期账单生成及记账 应该拆分 三部分 当期利息 当期本金 未来期本金调三次 然后结清就要根据未来本金余额判断了
        FabAmount prinAdvancesum = new FabAmount();
        FabAmount nintAdvancesum = new FabAmount();
        String prePayFlag = null;

        //2019-11-29 传结清标志但账本未结清，报错
        if (!"3".equals(endFlag) &&
                !getRepayAmt().isPositive() &&
                "1".equals(settleFlag))
            throw new FabException("LNS202");


        //还未来期
        if (!"3".equals(endFlag) && getRepayAmt().isPositive()
                && Date.valueOf(ctx.getTranDate()).before(Date.valueOf(lnsbasicinfo.getContduedate()))) {
            List<RepayAdvance> prinAdvancelist = new ArrayList<RepayAdvance>();
            List<RepayAdvance> nintAdvancelist = new ArrayList<RepayAdvance>();
            if (ConstantDeclare.ISCALINT.ISCALINT_NO.equals(lnsbasicinfo.getIscalint()))
                prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN;
            else if (ConstantDeclare.ISCALINT.ISCALINT_YES.equals(lnsbasicinfo.getIscalint()))
                prePayFlag = ConstantDeclare.PREPAYFLAG.PREPAYFLAG_INTPRIN;

            //退货延用还款接口 author:chenchao
            if(VarChecker.asList("471014").contains(ctx.getTranCode())&&ConstantDeclare.REPAYWAY.REPAYWAY_WILLFUL.equals(loanAgreement.getWithdrawAgreement().getRepayWay())){
                endFlag = LoanInterestSettlementUtil.salesReturn(loanAgreement, ctx, subNo, lnsbasicinfo,
                        new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,
                        prePayFlag,compensateFlag,settleFlag);
            }else{
                //正常还款接口
                if (ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay()))
                    endFlag = LoanInterestSettlementUtil.specialRepaymentBill(loanAgreement, ctx, subNo, lnsbasicinfo,
                            new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,
                            prePayFlag,compensateFlag,settleFlag);
                else
                    endFlag = LoanInterestSettlementUtil.interestRepaymentBill(loanAgreement, ctx, subNo, lnsbasicinfo,
                            new FabAmount(getRepayAmt().getVal()), prinAdvancelist, nintAdvancelist, billStatistics,
                            prePayFlag,compensateFlag,settleFlag);
            }

            LoggerUtil.debug("BREAKWHILE:" + prinAdvancelist.size() + "|" + nintAdvancelist.size());

            if (!prinAdvancelist.isEmpty()) {
                for (RepayAdvance prinAdvance : prinAdvancelist) {
                    LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), "PRIN", "N", new FabCurrency());
                    nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
                    nlnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                    nlnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                    nlnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
                    nlnsAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
                            ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat()) ? "3" : "1");

                    sub.operate(nlnsAcctInfo, null, prinAdvance.getBillAmt(), loanAgreement.getFundInvest(),
                            memo, prinAdvance.getBillTrandate(),
                            prinAdvance.getBillSerseqno(), prinAdvance.getBillTxseq(), ctx);
                    eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, prinAdvance.getBillAmt(),
                            nlnsAcctInfo, null, loanAgreement.getFundInvest(), memo, ctx,
                            null, prinAdvance.getBillTrandate(), prinAdvance.getBillSerseqno(),
                            prinAdvance.getBillTxseq(), revserv1, debtcompany, "", chtype);
                    prinAdvancesum.selfAdd(prinAdvance.getBillAmt());
                    //展示还款明细准备数据 2020-06-08
                    if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
                    	
                    	//本金账本 无需添加状态
                        LoanRpyInfoUtil.addRpyList(repayAmtList, "PRIN", prinAdvance.getRepayterm().toString(), prinAdvance.getBillAmt(),"");
                }
                if (null == repayAmtMap.get("PRIN.N")) {
                    repayAmtMap.put("PRIN.N", prinAdvancesum);
                } else {
                    repayAmtMap.get("PRIN.N").selfAdd(prinAdvancesum);
                }
                
                
            }
            if (!nintAdvancelist.isEmpty()) {
                for (RepayAdvance nintAdvance : nintAdvancelist) {
                    LnsAcctInfo nlnsAcctInfo = new LnsAcctInfo(getAcctNo(), "NINT", "N", new FabCurrency());
                    nlnsAcctInfo.setMerchantNo(getRepayAcctNo());
                    nlnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                    nlnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                    nlnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
                    nlnsAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
                            ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat()) ? "3" : "1");

                    sub.operate(nlnsAcctInfo, null, nintAdvance.getBillAmt(), loanAgreement.getFundInvest(),
                            memo, nintAdvance.getBillTrandate(),
                            nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(), ctx);
                    eventProvider.createEvent(ConstantDeclare.EVENT.CRDTREPYMT, nintAdvance.getBillAmt(), nlnsAcctInfo,
                            null, loanAgreement.getFundInvest(), memo, ctx, null,
                            nintAdvance.getBillTrandate(), nintAdvance.getBillSerseqno(), nintAdvance.getBillTxseq(), revserv1, debtcompany, "", chtype);
                    nintAdvancesum.selfAdd(nintAdvance.getBillAmt());
                    //展示还款明细准备数据 2020-06-08
                    if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
                    	
                    	//利息账本 添加账本状态
                        LoanRpyInfoUtil.addRpyList(repayAmtList, "NINT", nintAdvance.getRepayterm().toString(), nintAdvance.getBillAmt(),nintAdvance.getBillStatus());
                }
                if (null == repayAmtMap.get("NINT.N")) {
                    repayAmtMap.put("NINT.N", nintAdvancesum);
                } else {
                    repayAmtMap.get("NINT.N").selfAdd(nintAdvancesum);
                }
            }
        }

    	 //展示还款明细准备数据 2020-06-08
	    if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
	    {
	    	
	    	repayList = LoanRpyInfoUtil.getRepayList(acctNo, ctx, repayAmtList,loanAgreement);
	    	HashMap<String, RpyList> intReduceInfoMap = new HashMap<String, RpyList>();
	    	HashMap<String, RpyList> fintReduceInfoMap = new HashMap<String, RpyList>();
	    	HashMap<String, RpyList> prinReduceInfoMap = new HashMap<String, RpyList>();
	    	HashMap<String, RpyList> repayInfoMap = new HashMap<String, RpyList>();
	    	List<RpyList> repayToJsonList = new ArrayList<RpyList>();
	    	if(!VarChecker.isEmpty(intReduceList)){
        		
        		
        		List<Map> intReduceListMap= (List<Map>)JSONArray.parse(intReduceList);
        		for(Map temp:intReduceListMap){
        			RpyList repayInfo = new RpyList();
        			repayInfo.setRepayterm((Integer)temp.get("repayterm"));
        			repayInfo.setForfeetAmt(new FabAmount(0.00));
        			repayInfo.setSubIntAmt(new FabAmount(((BigDecimal)temp.get("intAmt")).doubleValue()));
        			repayInfo.setCouponFeeAmt(new FabAmount(0.00));
        			repayInfo.setDiscountIntFlag(StringUtil.obj2str(temp.get("discountIntFlag")));
        			intReduceInfoMap.put(repayInfo.getRepayterm()+"", repayInfo);
        		}
	    	}
	    	
	    	if(!VarChecker.isEmpty(fintReduceList)){
        		
        		List<Map> fintReduceListMap= (List<Map>)JSONArray.parse(fintReduceList);
        		for(Map temp:fintReduceListMap){
        			RpyList repayInfo = new RpyList();
        			repayInfo.setRepayterm((Integer)temp.get("repayterm"));
        			repayInfo.setForfeetAmt(new FabAmount(0.00));
        			repayInfo.setSubFintAmt(new FabAmount(((BigDecimal)temp.get("forfeitAmt")).doubleValue()));
        			repayInfo.setCouponFeeAmt(new FabAmount(0.00));
        			repayInfo.setReduceIntAmt(new FabAmount(0.00));
        			repayInfo.setDiscountIntFlag(StringUtil.obj2str(temp.get("discountIntFlag")));
        			fintReduceInfoMap.put(repayInfo.getRepayterm()+"", repayInfo);
        		}
	    	}
	    	
	    	if(!VarChecker.isEmpty(prinReduceList)){
        		
        		List<Map> prinReduceListMap= (List<Map>)JSONArray.parse(prinReduceList);
        		for(Map temp:prinReduceListMap){
        			RpyList repayInfo = new RpyList();
        			repayInfo.setRepayterm((Integer)temp.get("repayterm"));
        			repayInfo.setForfeetAmt(new FabAmount(0.00));
        			repayInfo.setSubPrinAmt(new FabAmount(((BigDecimal)temp.get("prinAmt")).doubleValue()));
        			repayInfo.setCouponFeeAmt(new FabAmount(0.00));
        			repayInfo.setReduceIntAmt(new FabAmount(0.00));
        			repayInfo.setDiscountIntFlag(StringUtil.obj2str(temp.get("discountIntFlag")));
        			prinReduceInfoMap.put(repayInfo.getRepayterm()+"", repayInfo);
        		}
	    	}

        		//合并费用减免明细
        		List<Map> repayListMap= (List<Map>)JSONArray.parse(repayList);
        		for(Map temp:repayListMap){
        			RpyList repayInfo = new RpyList();
        			repayInfo.setRepayterm((Integer)temp.get("repayterm"));
        			repayInfo.setSubFeeAmt(new FabAmount(0.00));
        			repayInfo.setPrinAmt(new FabAmount(((BigDecimal)temp.get("prinAmt")).doubleValue()));
        			repayInfo.setIntAmt(new FabAmount(((BigDecimal)temp.get("intAmt")).doubleValue()));
        			repayInfo.setForfeitAmt(new FabAmount(((BigDecimal)temp.get("forfeitAmt")).doubleValue()));
        			repayInfo.setReduceIntAmt(new FabAmount(((BigDecimal)temp.get("reduceIntAmt")).doubleValue()));
        			repayInfo.setDiscountfeeFlag(StringUtil.obj2str(temp.get("discountfeeFlag")));
        			repayInfo.setDiscountIntFlag(StringUtil.obj2str(temp.get("discountIntFlag")));
        			repayInfo.setNintBillStatus(StringUtil.obj2str(temp.get("nintBillStatus")));
        			if(null!=intReduceInfoMap.get(repayInfo.getRepayterm()+"")){
        				repayInfo.setSubIntAmt(intReduceInfoMap.get(repayInfo.getRepayterm()+"").getSubIntAmt());
        				
        			}
        			if(null!=fintReduceInfoMap.get(repayInfo.getRepayterm()+"")){
        				repayInfo.setSubFintAmt(fintReduceInfoMap.get(repayInfo.getRepayterm()+"").getSubFintAmt());
        				
        			}
        			if(null!=prinReduceInfoMap.get(repayInfo.getRepayterm()+"")){
        				repayInfo.setSubPrinAmt(prinReduceInfoMap.get(repayInfo.getRepayterm()+"").getSubPrinAmt());
        				
        			}
        			repayInfoMap.put(repayInfo.getRepayterm()+"", repayInfo);
        			repayToJsonList.add(repayInfo);
        			if(temp.get("reduceIntAmt")!=null){
                        couponIntAmt.selfAdd(((BigDecimal)temp.get("reduceIntAmt")).doubleValue());
                    }
                    if("Y".equals(StringUtil.obj2str(temp.get("discountIntFlag")))){
                        discountIntFlag=StringUtil.obj2str(temp.get("discountIntFlag"));
                    }
        		}
        		for(Entry<String, RpyList> m:intReduceInfoMap.entrySet()){
        			if(null==repayInfoMap.get(m.getValue().getRepayterm()+"")){
        				if(null!=fintReduceInfoMap.get(m.getValue().getRepayterm()+"")){
        					m.getValue().setSubFintAmt(fintReduceInfoMap.get(m.getValue().getRepayterm()+"").getSubFintAmt());
        				}
        				if(null!=prinReduceInfoMap.get(m.getValue().getRepayterm()+"")){
        					m.getValue().setSubPrinAmt(prinReduceInfoMap.get(m.getValue().getRepayterm()+"").getSubPrinAmt());
        				}
        				repayToJsonList.add(m.getValue());
        			}
        			
        		}
        		for(Entry<String, RpyList> m:fintReduceInfoMap.entrySet()){
        			if(null==repayInfoMap.get(m.getValue().getRepayterm()+"") && null==intReduceInfoMap.get(m.getValue().getRepayterm()+"")){
        				if(null!=prinReduceInfoMap.get(m.getValue().getRepayterm()+"")){
        					m.getValue().setSubPrinAmt(prinReduceInfoMap.get(m.getValue().getRepayterm()+"").getSubPrinAmt());
        				}
        				repayToJsonList.add(m.getValue());
        			}
        			
        		}
        		for(Entry<String, RpyList> m:prinReduceInfoMap.entrySet()){
        			if(null==repayInfoMap.get(m.getValue().getRepayterm()+"") && null==intReduceInfoMap.get(m.getValue().getRepayterm()+"")
        					&& null==fintReduceInfoMap.get(m.getValue().getRepayterm()+"")){
        				repayToJsonList.add(m.getValue());
        			}    			
        		}
        		repayList =JsonTransfer.ToJson(repayToJsonList);
          	
	    	
	    	//压缩明细数据
            String compressRepayList=StringDecodeUtil.compressData(repayList);
	        AccountingModeChange.saveInterfaceEx(tranctx, acctNo, ConstantDeclare.KEYNAME.HKMX, "还款明细", compressRepayList.length() > 2000 ? compressRepayList.substring(0, 2000) : compressRepayList);
            //对结果集进行求和  进行外层展示
           /* List<Map> repayListMap= (List<Map>) JSONArray.parse(repayList);
            for(Map temp:repayListMap){
                if(temp.get("reduceIntAmt")!=null){
                    couponIntAmt.selfAdd(((BigDecimal)temp.get("reduceIntAmt")).doubleValue());
                }
                if("Y".equals(StringUtil.obj2str(temp.get("discountIntFlag")))){
                    discountIntFlag=StringUtil.obj2str(temp.get("discountIntFlag"));
                }
            }*/
	    } else {
	    	repayList = null;
	    }
        this.repayAmtMap = repayAmtMap;
        FabAmount prinAmtTmp = new FabAmount();
        FabAmount nintAmtTmp = new FabAmount();
        FabAmount dintAmtTmp = new FabAmount();
        int i = 0; //插还款明细需要加序号 否则唯一索引冲突
        for (Map.Entry<String, FabAmount> entry : repayAmtMap.entrySet()) {
            if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN))
                prinAmtTmp.selfAdd(entry.getValue());
            if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_NINT))
                nintAmtTmp.selfAdd(entry.getValue());
            if (entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)
                    || entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)
                    || entry.getKey().contains(ConstantDeclare.BILLTYPE.BILLTYPE_GINT))
                dintAmtTmp.selfAdd(entry.getValue());
            i++; //明细序号自增 适应唯一索引

            LoanRpyInfoUtil.saveRpyInfo(ctx, i, acctNo, repayAcctNo, LoanRpyInfoUtil.getLoantype(compensateFlag), entry);
        }

        prinAmt = prinAmtTmp;
        nintAmt = nintAmtTmp;
        dintAmt = dintAmtTmp;


        /**
         * 有债务公司	14050183
         * 产品代码为3010016 不校验债务公司 2019-09-17
         */
        if (pkgflag == 1 && !"3010016".equals(loanAgreement.getPrdId())) {
            //无追保理还款债务公司要等于放款债务公司
            JSONObject myJson = JSONObject.parseObject(loanAgreement.getBasicExtension().getJsonStr());
            Map m = myJson;

            for (PubDict pkg : pkgList.getLoopmsg()) {
                String debtCompany = PubDict.getRequestDict(pkg, "debtCompany");
                FabAmount debtAmt = PubDict.getRequestDict(pkg, "debtAmt");

                if (Arrays.asList("3010006", "3010014").contains(lnsbasicinfo.getPrdcode())) {
                    //无追保理还款债务公司要等于放款债务公司
                    if (null != m && !m.containsKey(debtCompany)) {
                        throw new FabException("LNS123");
                    }

                    //多债务公司的 校验金额 add at 2019-03-07
                    if (null != myJson && !myJson.isEmpty() && myJson.size() > 1) {
                        //每次还款更新Lnsbasicinfoex		14050183
                        if (new FabAmount(myJson.getDouble(debtCompany)).sub(debtAmt).isNegative())
                            throw new FabException("LNS151", debtCompany, myJson.getDouble(debtCompany));
                        myJson.put(debtCompany, new FabAmount(myJson.getDouble(debtCompany)).sub(debtAmt).getVal());
                    }
                    //add end
                }


                /**
                 * 债务公司预收户类型D	放款时开立	金额就是放款时债务公司对应的金额
                 * 放款时同时开立类型为N的预收户	金额为0	加款走预收充值
                 * 20190521|14050183
                 */
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("balance", debtAmt.getVal());
                params.put("brc", ctx.getBrc());
                params.put("accsrccode", "D");
                params.put("acctno", debtCompany);

                Map<String, Object> newBalance;
                try {
                    newBalance = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsprefundaccount_repay", params);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS103", "lnsprefundaccount");
                }
                if (newBalance == null) {
                    throw new FabException("SPS104", "lnsprefundaccount");
                }

                Double tabBalance = Double.parseDouble(newBalance.get("balance").toString());
                LoggerUtil.debug("tabBalance:" + tabBalance);
                if (tabBalance.doubleValue() < 0.00)
                    throw new FabException("LNS020");

                txseqno++;

                // 查询预收户信息
                TblLnsassistdyninfo preaccountInfo = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), getRepayAcctNo(), "", ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,ctx);
                if (preaccountInfo == null) {
                    throw new FabException("SPS104", "lnsprefundaccount");
                }

//                Map<String, Object> midparams = new HashMap<String, Object>();
//                midparams.put("brc", ctx.getBrc());
//                midparams.put("accsrccode", "N");
//                midparams.put("acctno", getRepayAcctNo());
//
//                Map<String, Object> customid;
//                try {
//                    customid = DbAccessUtil.queryForMap("CUSTOMIZE.query_customid", midparams);
//                } catch (FabSqlException e) {
//                    throw new FabException(e, "SPS103", "lnsprefundaccount");
//                }
//                if (customid == null) {
//                    throw new FabException("SPS104", "lnsprefundaccount");
//                }
//
//                String customidstr = customid.get("customid").toString();
                //债务公司还款
                AccountingModeChange.saveLnsprefundsch(ctx, txseqno, debtCompany, debtCompany, "D", "COMPANY",
                        debtCompany, debtAmt.getVal(), "sub");
//                //之后三行貌似没用	20190521|14050183
//                Map<String, Object> reparam = new HashMap<String, Object>();
//                reparam.put("debtCompany", debtCompany);
//                reparam.put("debtAmt", debtAmt);


                // 抛事件
                FundInvest fundInvest = new FundInvest();
                if (getRepayChannel() != null)
                    fundInvest.setChannelType(getRepayChannel());

                if (getBankSubject() != null)
                    fundInvest.setFundChannel(getBankSubject());

                if (outSerialNo != null)
                    fundInvest.setOutSerialNo(outSerialNo);
                if (ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE.equals(lnsbasicinfo.getLoanstat()) ||
                        ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(lnsbasicinfo.getLoanstat())) {
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL,
                            new FabCurrency());
                    lnsAcctInfo.setMerchantNo(getRepayAcctNo());
                    lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                    lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                    lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());

                    // DOTO 如果是老数据抛商户号的时候 需要先select商户号
                    if (debtAmt.isPositive())
                        eventProvider.createEvent(ConstantDeclare.EVENT.BACKDEBTCO, debtAmt, lnsAcctInfo, null, fundInvest, "HKCT", ctx, preaccountInfo.getCustomid(),
                                debtCompany, ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
                } else {
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,
                            new FabCurrency());
                    lnsAcctInfo.setMerchantNo(getRepayAcctNo());
                    lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                    lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                    lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
                    // DOTO 如果是老数据抛商户号的时候 需要先select商户号
                    if (debtAmt.isPositive()) {
                        if ("3010014".equals(loanAgreement.getPrdId()))
                            lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

                        eventProvider.createEvent(ConstantDeclare.EVENT.BACKDEBTCO, debtAmt, lnsAcctInfo, null, fundInvest, "HKCT", ctx, preaccountInfo.getCustomid(),
                                debtCompany, ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
                    }
                }


                // 累加循环报文金额
                sumAmt.selfAdd(debtAmt);
            }
            sumAmt.selfSub(prinAmt);
            if (!sumAmt.isZero()) {
                throw new FabException("LNS028");
            }

            //修改 主文件辅助表里的tunneldata  add at 2019-03-07
            if (null != myJson && !myJson.isEmpty() && myJson.size() > 1) {
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("acctno", acctNo);
                updateMap.put("openbrc", ctx.getBrc());
                updateMap.put("key", "WZBL");
                updateMap.put("tunneldata", JsonTransfer.ToJson(myJson));
                try {
                    DbAccessUtil.execute("Lnsbasicinfoex.updateTunneldata", updateMap);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS100", "lnsrpyinfo");
                }
            }
            //add end
        }

        //提前结清时，本息罚也要结清
        if (((null != feeAmt && !feeAmt.isZero()) ||
                (null != cleanFeeRate && !new FabAmount(cleanFeeRate.getVal().doubleValue()).isZero())) &&
                !"3".equals(endFlag))
            throw new FabException("LNS132");

        //2019-10-22 房抵贷代偿
        //代偿还款，渠道为D，代偿标志必传2
        if ("D".equals(repayChannel) && !"2".equals(compensateFlag))
            throw new FabException("LNS194");
        //百信银行代偿 走新的还费逻辑
        if ("2".equals(compensateFlag) && !isZrdc && "false".equals(loanAgreement.getInterestAgreement().getIsPenalty())) {
            //结清标志必输
            if (null == settleFlag || VarChecker.isEmpty(settleFlag))
                throw new FabException("LNS177");
            //必须整笔结清
            if (ConstantDeclare.REPAYFLAG.REPAY_NORMAL.equals(endFlag))
                throw new FabException("LNS189");

            Map<String, FabAmount> feeRpyInfo = new HashMap<>();


            //定义各形态账单list（用于统计每期计划已还未还金额）
            LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);

            LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);

            List<LnsBill> feeBillList = new ArrayList<LnsBill>();
            //历史账单
            feeBillList.addAll(lnsBillStatistics.getHisBillList());
            //当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
            feeBillList.addAll(lnsBillStatistics.getBillInfoList());
            //未来期账单：从还款日到合同到期日之间的本金和利息账单
            feeBillList.addAll(lnsBillStatistics.getFutureBillInfoList());
            //代偿结费结到当前期
            for (LnsBill feeBill : feeBillList) {
                //历史期+当前期
                if (LoanFeeUtils.isFeeType(feeBill.getBillType())
                        && CalendarUtil.after(ctx.getTranDate(), feeBill.getStartDate())) {
                    //当期到期日时，实际还款日小于当期应自动减免一期费用
                    if (CalendarUtil.afterAlsoEqual(ctx.getTranDate(), feeBill.getEndDate())) {
                        if (CalendarUtil.beforeAlsoEqual(realDate, feeBill.getStartDate())) {
                            Double minAmt = LoanFeeUtils.repaysql(feeBill.getBillBal(), upbillmap, feeBill, tranctx);
                            LoanFeeUtils.accountsub(sub, tranctx, feeBill, minAmt, loanAgreement, repayAcctNo, "");
//			                 adjustFee.selfAdd(minAmt);
                            Map<String, Object> json = new HashMap<>();
                            json.put("adjustFee", minAmt.toString());
                            json.put("realDate", realDate);
                            //RMFE风险管理费
//			                 //SQFE担保费
//			                 if("51340000".equals(childBrc)) {
//			                     json.put("adjustType", "SQFE");
//                    }
//			                 else {
//			                     json.put("adjustType", "RMFE");
//            }
                            LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, feeBill.getBillStatus(),
                                    new FabCurrency(), feeBill.getLnsfeeinfo().getFeebrc());
                            AccountingModeChange.saveInterfaceEx(tranctx, acctNo, ConstantDeclare.KEYNAME.ZDJF, "自动减费", JsonTransfer.ToJson(json));

                            eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, new FabAmount(minAmt), lnsAcctInfo, null,
                                    loanAgreement.getFundInvest(), "", tranctx, feeBill.getLnsfeeinfo().getFeebrc());
                            isReduceFee = true;
                            continue;
                        }
                    }

                    //未落表的需要结息
                    if (CalendarUtil.before(ctx.getTranDate(), feeBill.getEndDate())) {
                        if (CalendarUtil.before(feeBill.getLnsfeeinfo().getLastfeedate(), feeBill.getEndDate())
                                && CalendarUtil.after(realDate, feeBill.getStartDate())) {
                            feeBill.setBillProperty("CONPEM");
                            LoanFeeUtils.settleFee(ctx, feeBill, loanAgreement);
                        } else{
                        	isCurrentTerm = false;

                        	if( null != la.getFeeAgreement() &&
                        		null != la.getFeeAgreement().getLnsfeeinfos()) {
	                        	for( TblLnsfeeinfo feeinfo : la.getFeeAgreement().getLnsfeeinfos())
	                        	{
	                        		if( ConstantDeclare.CALCULATRULE.BYDAY.equals(feeinfo.getCalculatrule() ))
	                        			isCurrentTerm = true;
	                        	}
                        	}
                        }

                    }

                    if (feeBill.getBillBal().isPositive() && isCurrentTerm) {
                        //代偿结清减账户金额
                        LoanFeeUtils.accountsub(sub, ctx, feeBill, feeBill.getBillBal().getVal(), loanAgreement, repayAcctNo, ConstantDeclare.BRIEFCODE.DCJQ);
                        LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, feeBill.getBillStatus(),
                                new FabCurrency(), feeBill.getLnsfeeinfo().getFeebrc());
                        String reserv = "";
                        if (VarChecker.asList("2412624", "2412626").contains(loanAgreement.getPrdId())) {
                            reserv = "R" + tranctx.getBrc().substring(0, 4);
                        }
                        eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, feeBill.getBillBal(), lnsAcctInfo, null,
                                loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DCJQ, tranctx, feeBill.getLnsfeeinfo().getFeebrc(), platformId, "", reserv);

                        LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, feeBill.getBillType(), feeBill.getBillStatus(), feeBill.getBillBal());

                    }
                } else {
                    if (LoanFeeUtils.isFeeType(feeBill.getBillType()) &&
                            "2".equals(feeBill.getLnsfeeinfo().getAdvancesettle())) {
                        //未落表的需要结息
                        if (CalendarUtil.before(ctx.getTranDate(), feeBill.getEndDate())) {
                            if (CalendarUtil.before(feeBill.getLnsfeeinfo().getLastfeedate(), feeBill.getEndDate())) {
                                feeBill.setBillProperty("CONPEM");
                                LoanFeeUtils.settleFee(ctx, feeBill, loanAgreement);
                            }
                        }

                        if (feeBill.getBillBal().isPositive()) {
                            //代偿结清减账户金额
                            if (!isReduceFee)
                                LoanFeeUtils.accountsub(sub, ctx, feeBill, feeBill.getBillBal().getVal(), loanAgreement, repayAcctNo, ConstantDeclare.BRIEFCODE.DCJQ);
                            LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(loanAgreement.getContract().getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, feeBill.getBillStatus(),
                                    new FabCurrency(), feeBill.getLnsfeeinfo().getFeebrc());
                            String reserv = "";
                            if (VarChecker.asList("2412624", "2412626").contains(loanAgreement.getPrdId())) {
                                reserv = "R" + tranctx.getBrc().substring(0, 4);
                            }
                            eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, feeBill.getBillBal(), lnsAcctInfo, null,
                                    loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DCJQ, tranctx, feeBill.getLnsfeeinfo().getFeebrc(), platformId, "", reserv);

                            LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, feeBill.getBillType(), feeBill.getBillStatus(), feeBill.getBillBal());
                        }
                    }

                }
            }

            LoanRpyInfoUtil.saveRpyInfos(ctx, i, feeRpyInfo, acctNo, repayAcctNo, LoanRpyInfoUtil.getLoantype(compensateFlag));
            //已落表费用账本billproperty更新为代偿"COMPEN"
            Map<String, Object> feeBillparam = new HashMap<String, Object>();
            feeBillparam.put("acctno", acctNo);
            feeBillparam.put("brc", ctx.getBrc());
//            try {
//    			DbAccessUtil.execute("CUSTOMIZE.update_compenbill_211", feeBillparam);
//            } catch (FabSqlException e) {
//    			throw new FabException(e, "SPS102", "update_compenbill_211");
//                }
            //账本结清
            try {
                DbAccessUtil.execute("CUSTOMIZE.update_settlefeebill_211", feeBillparam);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "update_settlefeebill_211");
            }
            //更新费用状态为销户，还款计划查询不试算未来期
            Map<String, Object> feeStatparam = new HashMap<>();
            feeStatparam.put("acctno", acctNo);
            feeStatparam.put("openbrc", ctx.getBrc());
            feeStatparam.put("feestat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
            //提前结清  未来期的费用置为0
            try {
                DbAccessUtil.execute("Lnsfeeinfo.updateStat", feeStatparam);

            } catch (FabSqlException e) {
                throw new FabException(e, "SPS102", "Lnsfeeinfo");
            }

            //查询主文件拓展表
            Map<String, Object> queryparam = new HashMap<>();
            queryparam.put("acctno", acctNo);
            queryparam.put("openbrc", tranctx.getBrc());
            queryparam.put("key", ConstantDeclare.KEYNAME.GDDB);

            TblLnsbasicinfoex lnsbasicinfoex;

            try {
                lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", queryparam, TblLnsbasicinfoex.class);
            } catch (FabSqlException e) {
                throw new FabException("SPS100", "lnsbasicinfoex", e);
            }

            Double termretfee = 0.00;
            if (lnsbasicinfoex != null) {
                JSONObject tunneldata = JSONObject.parseObject(lnsbasicinfoex.getTunneldata());
                //计算未还的固定担保费
                FabAmount unpayGDDB = new FabAmount(tunneldata.getDouble("termretfee"));//应还
                unpayGDDB.selfSub(tunneldata.getDouble("feeamt"));//已还
                if (unpayGDDB.isPositive()) {
                    throw new FabException("LNS191");
                }

                termretfee = JSONObject.parseObject(lnsbasicinfoex.getTunneldata()).getDouble("termretfee");
            }

            //分期计提
//            accrual(termretfee);
        }

        // 幂等登记薄

        Map<String, Object> lns211map = new HashMap<String, Object>();

        lns211map.put("sumrfint", dintAmt.getVal());
        lns211map.put("sumrint", nintAmt.getVal());
        lns211map.put("sumramt", prinAmt.getVal());
        lns211map.put("acctflag", TblLnsinterface.endflag2AcctFlag2(endFlag, ifOverdue));
        lns211map.put("serialno", getSerialNo());
        lns211map.put("trancode", lnsinterface.getTrancode());

        try {
            DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_repay", lns211map);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsinterface");
        }

        //还款还到本金落表,动态封顶的封顶值变更
        //DynamicCapUtil.saveCapRecord(loanAgreement,ctx ,prinAmt);

    }


    private String queryExinvesteeId() throws FabException {
        //代偿
        if (exinvesteeId != null)
            return exinvesteeId;
//        if (VarChecker.asList("2412631", "2412632", "2412633", "2412632", "2412633", "2412623", "2512617").contains(loanAgreement.getPrdId())) {
    	if ( "true".equals(loanAgreement.getInterestAgreement().getIsCompensatory())) {
            Map<String, Object> param = new HashMap<>();
            param.put("acctno", acctNo);
            param.put("brc", getTranctx().getBrc());
            param.put("key", ConstantDeclare.KEYNAME.DC);
            TblLnsinterfaceex interfaceex;
            try {
                interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", param, TblLnsinterfaceex.class);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS103", "query_lnsinterfaceex");
            }
            //空值处理
            if (interfaceex != null)
                exinvesteeId = JSONObject.parseObject(interfaceex.getValue()).getString("exinvesteeId");
            //查询过后让 exinvesteeId 不为 null
            exinvesteeId = exinvesteeId == null ? "" : exinvesteeId;
            LoggerUtil.debug("exinvesteeId:" + exinvesteeId);
        }
        return exinvesteeId;
    }


    /**
     * @return the serialNo
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * @param serialNo the serialNo to set
     */
    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
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
     * @return the repayAcctNo
     */
    public String getRepayAcctNo() {
        return repayAcctNo;
    }

    /**
     * @param repayAcctNo the repayAcctNo to set
     */
    public void setRepayAcctNo(String repayAcctNo) {
        this.repayAcctNo = repayAcctNo;
    }

    /**
     * @return the ccy
     */
    public String getCcy() {
        return ccy;
    }

    /**
     * @param ccy the ccy to set
     */
    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    /**
     * @return the cashFlag
     */
    public String getCashFlag() {
        return cashFlag;
    }

    /**
     * @param cashFlag the cashFlag to set
     */
    public void setCashFlag(String cashFlag) {
        this.cashFlag = cashFlag;
    }

    /**
     * @return the termDate
     */
    public String getTermDate() {
        return termDate;
    }

    /**
     * @param termDate the termDate to set
     */
    public void setTermDate(String termDate) {
        this.termDate = termDate;
    }

    /**
     * @return the tranCode
     */
    public String getTranCode() {
        return tranCode;
    }

    /**
     * @param tranCode the tranCode to set
     */
    public void setTranCode(String tranCode) {
        this.tranCode = tranCode;
    }

    /**
     * @return the repayAmt
     */
    public FabAmount getRepayAmt() {
        return repayAmt;
    }

    /**
     * @param repayAmt the repayAmt to set
     */
    public void setRepayAmt(FabAmount repayAmt) {
        this.repayAmt = repayAmt;
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
     * @return the repayChannel
     */
    public String getRepayChannel() {
        return repayChannel;
    }

    /**
     * @param repayChannel the repayChannel to set
     */
    public void setRepayChannel(String repayChannel) {
        this.repayChannel = repayChannel;
    }

    /**
     * @return the memo
     */
    public String getMemo() {
        return memo;
    }

    /**
     * @param memo the memo to set
     */
    public void setMemo(String memo) {
        this.memo = memo;
    }

    /**
     * @return the brc
     */
    public String getBrc() {
        return brc;
    }

    /**
     * @param brc the brc to set
     */
    public void setBrc(String brc) {
        this.brc = brc;
    }

    /**
     * @return the repayAmtMap
     */
    public Map<String, FabAmount> getRepayAmtMap() {
        return repayAmtMap;
    }

    /**
     * @param repayAmtMap the repayAmtMap to set
     */
    public void setRepayAmtMap(Map<String, FabAmount> repayAmtMap) {
        this.repayAmtMap = repayAmtMap;
    }

    /**
     * @return the subNo
     */
    public Integer getSubNo() {
        return subNo;
    }

    /**
     * @param subNo the subNo to set
     */
    public void setSubNo(Integer subNo) {
        this.subNo = subNo;
    }

    /**
     * @return the outSerialNo
     */
    public String getOutSerialNo() {
        return outSerialNo;
    }

    /**
     * @param outSerialNo the outSerialNo to set
     */
    public void setOutSerialNo(String outSerialNo) {
        this.outSerialNo = outSerialNo;
    }

    /**
     * @return the billStatistics
     */
    public LnsBillStatistics getBillStatistics() {
        return billStatistics;
    }

    /**
     * @param billStatistics the billStatistics to set
     */
    public void setBillStatistics(LnsBillStatistics billStatistics) {
        this.billStatistics = billStatistics;
    }

    /**
     * @return the prinAmt
     */
    public FabAmount getPrinAmt() {
        return prinAmt;
    }

    /**
     * @param prinAmt the prinAmt to set
     */
    public void setPrinAmt(FabAmount prinAmt) {
        this.prinAmt = prinAmt;
    }

    /**
     * @return the nintAmt
     */
    public FabAmount getNintAmt() {
        return nintAmt;
    }

    /**
     * @param nintAmt the nintAmt to set
     */
    public void setNintAmt(FabAmount nintAmt) {
        this.nintAmt = nintAmt;
    }

    /**
     * @return the dintAmt
     */
    public FabAmount getDintAmt() {
        return dintAmt;
    }

    /**
     * @param dintAmt the dintAmt to set
     */
    public void setDintAmt(FabAmount dintAmt) {
        this.dintAmt = dintAmt;
    }

    /**
     * @return the endFlag
     */
    public String getEndFlag() {
        return endFlag;
    }

    /**
     * @param endFlag the endFlag to set
     */
    public void setEndFlag(String endFlag) {
        this.endFlag = endFlag;
    }

    /**
     * @return the loanAgreement
     */
    public LoanAgreement getLoanAgreement() {
        return loanAgreement;
    }

    /**
     * @param loanAgreement the loanAgreement to set
     */
    public void setLoanAgreement(LoanAgreement loanAgreement) {
        this.loanAgreement = loanAgreement;
    }

    /**
     * @return the sub
     */
    public AccountOperator getSub() {
        return sub;
    }

    /**
     * @param sub the sub to set
     */
    public void setSub(AccountOperator sub) {
        this.sub = sub;
    }

    /**
     * @return the eventProvider
     */
    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }

    /**
     * @param eventProvider the eventProvider to set
     */
    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;
    }

    public String getBankSubject() {
        return bankSubject;
    }

    public void setBankSubject(String bankSubject) {
        this.bankSubject = bankSubject;
    }

    public FabAmount getRefundAmt() {
        return refundAmt;
    }

    public void setRefundAmt(FabAmount refundAmt) {
        this.refundAmt = refundAmt;
    }

    public String getPriorType() {
        return priorType;
    }

    public void setPriorType(String priorType) {
        this.priorType = priorType;
    }

    public String getPrdId() {
        return prdId;
    }

    public void setPrdId(String prdId) {
        this.prdId = prdId;
    }

    public String getCustType() {
        return custType;
    }

    public void setCustType(String custType) {
        this.custType = custType;
    }

    /**
     * @return the settleFlag
     */
    public String getSettleFlag() {
        return settleFlag;
    }

    /**
     * @param settleFlag the settleFlag to set
     */
    public void setSettleFlag(String settleFlag) {
        this.settleFlag = settleFlag;
    }

    /**
     * @return the cleanFeeRate
     */
    public FabRate getCleanFeeRate() {
        return cleanFeeRate;
    }

    /**
     * @param cleanFeeRate the cleanFeeRate to set
     */
    public void setCleanFeeRate(FabRate cleanFeeRate) {
        this.cleanFeeRate = cleanFeeRate;
    }

    /**
     * @return the reduceIntAmt
     */
    public FabAmount getReduceIntAmt() {
        return reduceIntAmt;
    }

    /**
     * @param reduceIntAmt the reduceIntAmt to set
     */
    public void setReduceIntAmt(FabAmount reduceIntAmt) {
        this.reduceIntAmt = reduceIntAmt;
    }

    /**
     * @return the reduceFintAmt
     */
    public FabAmount getReduceFintAmt() {
        return reduceFintAmt;
    }

    /**
     * @param reduceFintAmt the reduceFintAmt to set
     */
    public void setReduceFintAmt(FabAmount reduceFintAmt) {
        this.reduceFintAmt = reduceFintAmt;
    }

    /**
     * @return ifOverdue
     */
    public boolean getIsIfOverdue() {
        return ifOverdue;
    }

    /**
     * @param ifOverdue the reduceFintAmt to set
     */
    public void setIfOverdue(boolean ifOverdue) {
        this.ifOverdue = ifOverdue;
    }

    /**
     * @return txseqno
     */
    public Integer getTxseqno() {
        return txseqno;
    }

    /**
     * @param txseqno the txseqno to set
     */
    public void setTxseqno(Integer txseqno) {
        this.txseqno = txseqno;
    }

    public String getCompensateFlag() {
        return compensateFlag;
    }

    public void setCompensateFlag(String compensateFlag) {
        this.compensateFlag = compensateFlag;
    }

    /**
     * @return the fnintRed
     */
    public FabAmount getFnintRed() {
        return fnintRed;
    }

    /**
     * @param fnintRed the fnintRed to set
     */
    public void setFnintRed(FabAmount fnintRed) {
        this.fnintRed = fnintRed;
    }

    /**
     * @return the nintRed
     */
    public FabAmount getNintRed() {
        return nintRed;
    }

    /**
     * @param nintRed the nintRed to set
     */
    public void setNintRed(FabAmount nintRed) {
        this.nintRed = nintRed;
    }

    /**
     * @return the exinvesteeId
     */
    public String getExinvesteeId() {
        return exinvesteeId;
    }

    /**
     * @param exinvesteeId the exinvesteeId to set
     */
    public void setExinvesteeId(String exinvesteeId) {
        this.exinvesteeId = exinvesteeId;
    }

    /**
     * @return the ifOverdue
     */
    public boolean isIfOverdue() {
        return ifOverdue;
    }

    /**
     * @return the realDate
     */
    public String getRealDate() {
        return realDate;
    }

    /**
     * @param realDate the realDate to set
     */
    public void setRealDate(String realDate) {
        this.realDate = realDate;
    }

    /**
     * @return the pkgList
     */
    public ListMap getPkgList() {
        return pkgList;
    }

    /**
     * @param pkgList to set
     */
    public void setPkgList(ListMap pkgList) {
        this.pkgList = pkgList;
    }

    /**
     * @return the pkgList1
     */
    public ListMap getPkgList1() {
        return pkgList1;
    }

    /**
     * @param pkgList1 to set
     */
    public void setPkgList1(ListMap pkgList1) {
        this.pkgList1 = pkgList1;
    }


	/**
	 * @return the switchloanType
	 */
	public String getSwitchloanType() {
		return switchloanType;
	}


	/**
	 * @param switchloanType the switchloanType to set
	 */
	public void setSwitchloanType(String switchloanType) {
		this.switchloanType = switchloanType;
	}

    public FabAmount getSubIntAmt() {
        return subIntAmt;
    }

    public void setSubIntAmt(FabAmount subIntAmt) {
        this.subIntAmt = subIntAmt;
    }

    public FabAmount getSubFintAmt() {
        return subFintAmt;
    }

    public void setSubFintAmt(FabAmount subFintAmt) {
        this.subFintAmt = subFintAmt;
    }

    public FabAmount getCouponIntAmt() {
        return couponIntAmt;
    }

    public void setCouponIntAmt(FabAmount couponIntAmt) {
        this.couponIntAmt = couponIntAmt;
    }

    public String getDiscountIntFlag() {
        return discountIntFlag;
    }

    public void setDiscountIntFlag(String discountIntFlag) {
        this.discountIntFlag = discountIntFlag;
    }


	public String getIntReduceList() {
		return intReduceList;
	}


	public void setIntReduceList(String intReduceList) {
		this.intReduceList = intReduceList;
	}


	public String getFintReduceList() {
		return fintReduceList;
	}


	public void setFintReduceList(String fintReduceList) {
		this.fintReduceList = fintReduceList;
	}


	public String getPrinReduceList() {
		return prinReduceList;
	}


	public void setPrinReduceList(String prinReduceList) {
		this.prinReduceList = prinReduceList;
	}

    
}

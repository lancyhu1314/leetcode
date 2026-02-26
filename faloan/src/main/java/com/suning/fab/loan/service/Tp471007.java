package com.suning.fab.loan.service;

//import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.Amount;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.ServiceTemplate;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import com.suning.rsf.provider.annotation.Implement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @param
 * @author TT.Y
 * @version V1.0.0
 * @return
 * @exception
 * @see -贷款还款
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-repay")
public class Tp471007 extends ServiceTemplate {
    @Autowired
    Lns508 lns508;
    @Autowired
    Lns509 lns509;

    @Autowired
    Lns501 lns501;
    @Autowired
    Lns503 lns503;

    @Autowired
    Lns210 lns210;

    @Autowired
    Lns211 lns211;

    @Autowired
    Lns212 lns212;

    @Autowired
    Lns213 lns213;

    @Autowired
    Lns504 lns504;

    @Autowired
    Lns506 lns506;

    @Autowired
    Lns421 lns421;
    @Autowired
    Lns512 lns512;
    @Autowired
    Lns513 lns513;
    @Autowired
    Lns258 lns258;
    @Autowired
    Lns400 lns400;
    @Autowired
    Lns125 lns125;

    //房抵贷罚息落表封顶复习特殊处理20190127
    @Autowired
    Lns519 lns519;
    @Autowired
    Lns520 lns520;

    @Autowired
    Lns247 lns247;
    @Autowired
    Lns248 lns248;
    @Autowired
    Lns505 lns505;
    @Autowired
    Lns500 lns500;
    @Autowired
    Lns502 lns502;
    @Autowired
    Lns517 lns517;
    @Autowired
    Lns117 lns117;
    @Autowired
    Lns511 lns511;
    @Autowired
    Lns522 lns522;
    @Autowired
    Lns514 lns514;
    @Autowired
    Lns001 lns001;

    @Autowired
    Lns532 lns532;
    @Autowired
    Lns530 lns530;
    @Autowired
    Lns531 lns531;
    @Autowired
    Lns533 lns533;
    @Autowired
    Lns260 lns260;

    @Autowired
    Lns121 lns121;//考核数据校验 入库
    @Autowired
    Lns261 lns261;
    @Autowired
    Lns468 lns468;
    @Autowired
    Lns434 lns434;

    public Tp471007() {
        needSerSeqNo = true;
    }

    @Override
    protected void run() throws Exception {
        ThreadLocalUtil.clean();

        if (!VarChecker.isEmpty(ctx.getRequestDict("compensateFlag")) &&
                ConstantDeclare.COMPENSATEFLAG.SELL.equals(ctx.getRequestDict("compensateFlag").toString()) &&
                !VarChecker.asList("51230004", "51350000").contains(ctx.getBrc())) {
            LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("acctNo").toString(), ctx);
            prepareWriteOff(la);
            trigger(lns261);
//			lns211.setEndFlag("4");
        } else {
            //redis定时 还款防重
            String limitRepeatingRepaymentsProductList = GlobalScmConfUtil.getProperty("LimitRepeatingRepaymentsProductList", "");
            List productList = Arrays.asList(limitRepeatingRepaymentsProductList.split(","));
            LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("acctNo").toString(), ctx);

            String key = ctx.getRequestDict("acctNo").toString() + ctx.getRequestDict("repayAmt").toString();
            if (!productList.contains(la.getPrdId()) &&
                    false == (AcctStatistics.execute(key, ctx.getRequestDict("serialNo").toString()))) {

                throw new FabException("LNS054");
            }
            prepareWriteOff(la);//核销变量预备
            //核销的贷款还款预处理  2018-12-20
            lns517.setLoanAgreement(la);
            trigger(lns517);

            Map<String, Object> param = new HashMap<String, Object>();
            param.put("repayDate", ctx.getTranDate());

            trigger(lns421);
            //20191118  试算子交易
            trigger(lns001);

            //先结息本息
            lns501.setLnsbasicinfo(lns517.getLnsbasicinfo());
            lns501.setLnsBillStatistics(lns001.getLnsBillStatistics());
            lns501.setLa(lns001.getLoanAgreement());
            trigger(lns501, "map501", param);
            lns213.setLnsBillStatistics(lns001.getLnsBillStatistics());
            lns213.setLoanAgreement(lns001.getLoanAgreement());
            lns213.setDynamicCapDiff(lns001.getLoanAgreement().getBasicExtension().getDynamicCapDiff());
            //2019-11-27  实际扣款日和结清标志都传值时，罚息利息自动减免
            FabAmount nintRed = new FabAmount();
            FabAmount fnintRed = new FabAmount();
            //1-整笔提前结清
            //2-到期结清
            if (!VarChecker.isEmpty(ctx.getRequestDict("realDate")) &&
                    !VarChecker.isEmpty(ctx.getRequestDict("settleFlag"))
                    && VarChecker.asList("1", "2").contains(ctx.getRequestDict("settleFlag").toString())) {
                if (CalendarUtil.after(ctx.getRequestDict("realDate"), ctx.getTranDate()))
                    throw new FabException("LNS200");

                if (CalendarUtil.before(ctx.getRequestDict("realDate"), ctx.getTranDate())) {
                    lns514.setLnsBillStatistics(lns001.getLnsBillStatistics());
                    lns514.setLnsbasicinfo(lns517.getLnsbasicinfo());
                    trigger(lns514);

                    fnintRed.selfAdd(lns514.getDintRed());
                    nintRed.selfAdd(lns514.getNintRed());
                    lns213.setLnsBillStatistics(lns514.getLnsBillStatistics());
                    lns213.setLoanAgreement(lns514.getLoanAgreement());
                }
            }


            trigger(lns213);


            //罚息计提
            lns512.setRepayDate(ctx.getTranDate());
            lns512.setLnssatistics(lns501.getLnsBillStatistics());
            lns512.setLnsbasicinfo(lns501.getLnsbasicinfo());
            lns512.setLa(lns001.getLoanAgreement());
            trigger(lns512);

            //罚息落表
            if (!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())) {
                //罚息落表
                lns520.setRepayDate(ctx.getTranDate());
                lns520.setLnsBillStatistics(lns501.getLnsBillStatistics());
                lns520.setLnsbasicinfo(lns501.getLnsbasicinfo());
                lns520.setLa(lns512.getLa());
                trigger(lns520);
            } else {
                lns513.setRepayDate(ctx.getTranDate());
                lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
                lns513.setLnsbasicinfo(lns501.getLnsbasicinfo());
                lns513.setLoanAgreement(lns512.getLa());
                trigger(lns513);
            }

            //转列
            trigger(lns503, "map503", param);
            //贷款状态更新
            trigger(lns505);

//		FabAmount repayAmt = ctx.getRequestDict("repayAmt");

            /*del 2018-06-28 罚息结息时已记呆滞呆账*/
//		//纯退款没有还款金额不做计息转列
//		if(repayAmt!= null && repayAmt.isPositive()) 
//		{	
//			trigger(lns504, "map504", lns501);
//			lns501.getLnsBillStatistics().setBillNo(lns504.getSubNo());
//		}
            //是否逾期还款
            boolean ifOverdue = false; //add at 2019-03-25


            //2019-11-27  自动减免累计减免金额
            if (null != ctx.getRequestDict("reduceIntAmt")) {
                if (Double.valueOf(ctx.getRequestDict("reduceIntAmt").toString()) > 0.00)
                    nintRed.selfAdd(Double.valueOf(ctx.getRequestDict("reduceIntAmt").toString()));
            }

            if (null != ctx.getRequestDict("reduceFintAmt")) {
                if (Double.valueOf(ctx.getRequestDict("reduceFintAmt").toString()) > 0.00)
                    fnintRed.selfAdd(Double.valueOf(ctx.getRequestDict("reduceFintAmt").toString()));
            }


            FabAmount autoNintRed = new FabAmount(nintRed.getVal());
            FabAmount autoFNintRed = new FabAmount(fnintRed.getVal());
            if (nintRed.isPositive()) {
                lns247.setSubNo(lns501.getLnsBillStatistics().getBillNo());
                lns247.setBillStatistics(lns501.getLnsBillStatistics());
                lns247.setAutoNintRed(autoNintRed);

                trigger(lns247);

                ifOverdue = lns247.getIfOverdue();
                lns501.getLnsBillStatistics().setBillNo(lns247.getSubNo() + 1);
                lns211.setIntReduceList(lns247.getIntReduceList());
            }
            if (fnintRed.isPositive()) {
                lns248.setAutoFNintRed(autoFNintRed);
                lns248.setIfOverdue(ifOverdue);
                trigger(lns248);
                ifOverdue = lns248.getIfOverdue();
                lns211.setFintReduceList(lns248.getFintReduceList());
            }
            
            /*2022-01-23  本金减免*/
            if (null != ctx.getRequestDict("reducePrinAmt")) {
                if (Double.valueOf(ctx.getRequestDict("reducePrinAmt").toString()) > 0.00){
                	if(ConstantDeclare.REPAYWAY.isEqualInterest(lns501.getLnsbasicinfo().getRepayway())){
                		 throw new FabException("LNS248");
                	}
                	lns400.setEndDate(lns001.getRepayDate());
                	trigger(lns400);
                	lns125.setCleanForfeit(lns400.getCleanForfeit());
                	lns125.setCleanPrin(lns400.getCleanPrin());
                	lns125.setCleanInt(lns400.getCleanInt());
                	lns125.setOverPrin(lns400.getOverPrin());
                	lns125.setOverInt(lns400.getOverInt());
                	lns125.setOverForfeit(lns400.getOverDint());
                	lns125.setPrdcode(lns001.getLoanAgreement().getPrdId());
                	trigger(lns125);
                	lns258.setSubNo(lns501.getLnsBillStatistics().getBillNo());
                	trigger(lns258);
                	lns501.getLnsBillStatistics().setBillNo(lns258.getSubNo() + 2);
                	lns211.setPrinReduceList(lns258.getPrinReduceList());
                }
            }

            lns211.setBillStatistics(lns501.getLnsBillStatistics());
            lns211.setNintRed(nintRed);
            lns211.setFnintRed(fnintRed);
            lns211.setIfOverdue(ifOverdue);
            //还款本息
            lns211.setLoanAgreement(lns001.getLoanAgreement());
            trigger(lns211, "map211", lns501);

            //票据平台 百信银行代偿 走新的逻辑
            if ("2".equals(ctx.getRequestDict("compensateFlag")) && !lns001.getLoanAgreement().getFeeAgreement().isEmpty() && "true".equals(lns001.getLoanAgreement().getInterestAgreement().getIsPenalty())) {
                //代偿 结清标志传1
//			if(!"1".equals(ctx.getRequestDict("settleFlag"))){
//				throw new FabException("LNS235");
//			}
                //试算相关金额
                lns434.setLnsBillStatistics(lns001.getLnsBillStatistics());
                lns434.setCtx(lns001.getTranctx());
                lns434.setLoanAgreement(lns001.getLoanAgreement());
                //实际应还日期
                lns434.setEndDate(ctx.getTranDate());
                trigger(lns434);
                //计算结清金额
                ((Amount) ctx.getRequestDict("repayAmt")).selfSub((Amount) ctx.getRequestDict("repayAmt")).selfAdd(lns434.getCleanForfee()).selfAdd(lns434.getCleanDamages());
                lns532.setLoanAgreement(lns001.getLoanAgreement());
                lns532.setLnsBillStatistics(lns001.getLnsBillStatistics());
                trigger(lns532);
                //违约金计提
                //违约金计提
                lns530.setTxnseq(lns512.getTxnseq());//序号依次递增
                lns530.setLoanAgreement(lns001.getLoanAgreement());
                lns530.setBillStatistics(lns001.getLnsBillStatistics());
                lns530.setRepayDate(ctx.getTranDate());
                trigger(lns530);
                lns531.setLoanAgreement(lns001.getLoanAgreement());
                lns531.setLnsBillStatistics(lns001.getLnsBillStatistics());
                //违约金结息
                trigger(lns531);
                lns533.setDamageReduceAmts(lns532.getDamageReduceAmts());
                lns533.setTermReduceAmts(lns532.getTermReduceAmts());
                //到期结清不需要减免费用，农商行新增
                lns533.setDayReduceAmts(lns532.getDayReduceAmts());
                trigger(lns533);
                lns260.setAdjustDamage(lns533.getAdjustDamage());
                lns260.setAdjustFee(lns533.getAdjustFee());
                ((Amount) ctx.getRequestDict("repayAmt")).selfSub(lns533.getAdjustDamage()).selfSub(lns533.getAdjustFee());
                trigger(lns260);
            }

            //防止幂等报错被拦截
            trigger(lns121);
            //任性贷退货渠道
            trigger(lns210);
            //还款渠道
            Map<String, Object> map212 = new HashMap<String, Object>();
            map212.put("repayAmtMap", lns211.getRepayAmtMap());
            map212.put("loanAgreement", lns211.getLoanAgreement());
            map212.put("txseqno", lns211.getTxseqno());
            trigger(lns212, "map212", map212);
            //贷款结清状态更新
            trigger(lns506);

            /** 核销的贷款 需要计提利息 如果没有结清 通知核销动态表的金额 2018-12-20 start**/
            if (lns517.getIfCertification()) {
                if (new FabAmount(lns517.getLnsbasicinfo().getDeductionamt()).isPositive()) {
                    //扣息放款 进行摊销
                    lns502.setRepayDate(ctx.getTranDate());
                    trigger(lns502);
                } else {
                    //核销的贷款 还款结清 因为不会跑批 要计提所有的利息
                    //核销贷款主文件信息表中provisionflag计提结束标志为CLOSE	20190527|14050183
                    if ("3".equals(lns506.getEndFlag()))
                        lns517.getLnsbasicinfo().setLoanstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
                    /*
                     * 核销的贷款计提标志为CLOSE,Lns517子交易修改计提标志为正常,再传给Lns500子交易
                     * lns500.setLnsbasicinfo(lns517.getLnsbasicinfo())		慎改!!!!!!!!!
                     * 20190522|14050183
                     */
                    lns500.setLnsbasicinfo(lns517.getLnsbasicinfo());
                    lns500.setRepayDate(ctx.getTranDate());
                    trigger(lns500);
                }
                //如果没有结清贷款，  核销后需要将已经结息的金额 通知核销
                if (!"3".equals(lns506.getEndFlag())) {
                    lns117.setLa(la);
                    trigger(lns117);
                }

            }
            /** end **/


            lns211.setEndFlag(lns506.getEndFlag());//非幂等的时候取lns506的endflag
            ListMap pkgList1 = ctx.getRequestDict("pkgList1");
            if (!VarChecker.isEmpty(pkgList1)) {
                //光大人保助贷不抛 chenchao
                if (!VarChecker.asList("2512627").contains(la.getPrdId()) &&
                        !ConstantDeclare.ADVANCEFEETYPE.FIXED.equals(la.getInterestAgreement().getAdvanceFeeType())) {
                    Map<String, Object> map508 = new HashMap<String, Object>();
                    map508.put("prinAmt", lns211.getPrinAmt());
                    map508.put("nintAmt", lns211.getNintAmt());
                    map508.put("dintAmt", lns211.getDintAmt());
                    if ("01".equals(ctx.getRequestDict("investeeRepayFlg"))) {
                        lns508.getInvesteePrinPlatform().selfAdd(lns211.getPrinAmt());
                        lns508.getInvesteeIntPlatform().selfAdd(lns211.getNintAmt());
                        lns508.getInvesteeDintPlatform().selfAdd(lns211.getDintAmt());
                        lns508.setDiscountIntFlag(lns211.getDiscountIntFlag());
                        lns508.setCouponIntAmt(lns211.getCouponIntAmt());
                    }
                    //资金方还款
                    trigger(lns508, "DEFAULT", map508);
                    //收入结转
                    if ("01".equals(ctx.getRequestDict("investeeRepayFlg")) ||
                            pkgList1.size() > 1) {
                        lns509.getInvesteePrinPlatform().selfAdd(lns211.getPrinAmt());
                        lns509.getInvesteeIntPlatform().selfAdd(lns211.getNintAmt());
                        lns509.getInvesteeDintPlatform().selfAdd(lns211.getDintAmt());
                        lns509.setDiscountIntFlag(lns211.getDiscountIntFlag());
                        lns509.setCouponIntAmt(lns211.getCouponIntAmt());
                    }
                    lns509.setProductCode(lns211.getLoanAgreement().getPrdId());
                    trigger(lns509, "DEFAULT", map508);
                    //手续费收入结转（汽车分期、任性贷期缴融担费、任性贷搭保险、平台现金贷）
                    if (VarChecker.asList("2512622", "2512623", "2512617", "2512612").contains(la.getPrdId())) {
                        lns522.setLa(la);
                        trigger(lns522, "DEFAULT", map508);
                    }
                }
            }

            //房抵贷罚息落表封顶复习特殊处理20190127
            if (VarChecker.asList("2412615", "4010002").contains(la.getPrdId()) && la.getInterestAgreement().getCapRate() != null && la.getContract().getFlag1().contains("C")) {
//			if(!VarChecker.isEmpty(lns513.getThisSerseqno()) && !VarChecker.isEmpty(lns513.getThisTranDate()) && !VarChecker.isEmpty(lns513.getThisTxSeq())){
                //修改intenddate
                lns519.setRepayDate(ctx.getTranDate());
                trigger(lns519);
//			}
            }

            //结清并且为等本等息还款方式并且系统日期不为起息日
            if ("3".equals(lns506.getEndFlag()) &&
                    ConstantDeclare.REPAYWAY.isEqualInterest(la.getWithdrawAgreement().getRepayWay())) {
//			lns500.setLnsbasicinfo(lns517.getLnsbasicinfo());
                //查询基本信息给子交易句柄 并判断产品类型
                String acctNum = ctx.getRequestDict("acctNo").toString();
                TblLnsbasicinfo lnsinfo;
                Map<String, Object> param2 = new HashMap<String, Object>();
                param2.put("acctno", acctNum);
                param2.put("openbrc", ctx.getBrc());
                try {
                    //取主文件信息
                    lnsinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param2, TblLnsbasicinfo.class);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS103", "lnsbasicinfo");
                }

                if (null == lnsinfo) {
                    throw new FabException("SPS104", "lnsbasicinfo");
                }
                Map<String, Object> map500 = new HashMap<String, Object>();
                map500.put("repayDate", ctx.getTranDate());
                map500.put("lnsbasicinfo", lnsinfo);
                trigger(lns500, "DEFAULT", map500);
                if ("2512617".equals(lnsinfo.getPrdcode()))
                    trigger(lns511, "DEFAULT", map500);

            }
        }
        ThreadLocalUtil.clean();
    }

    /**
     * 核销全局变量预备
     *
     * @param la
     */
    public void prepareWriteOff(LoanAgreement la) throws Exception {
        //当账户是核销状态时
        if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION.equals(la.getContract().getLoanStat())) {
            //进行实时迁移
            if (!la.getContract().getFlag1().contains(ConstantDeclare.FLAG1.H)) {
                trigger(lns468);
                //进行实时赋值
                ThreadLocalUtil.set(ConstantDeclare.BASICINFOEXKEY.HXRQ, lns468.getWriteOffDate());
            } else {
                ThreadLocalUtil.set(ConstantDeclare.BASICINFOEXKEY.HXRQ, la.getBasicExtension().getWriteOffDate());
            }
            //将账户的状态放入上下文中  只有还款服务才需要存放此字段
            ThreadLocalUtil.set("loanStat", la.getContract().getLoanStat());
            //核销之后是否转罚息
            ThreadLocalUtil.set(ConstantDeclare.PARACONFIG.IGNOREOFFDINT, la.getInterestAgreement().getIgnoreOffDint());

        }
        else if(ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL.equals(la.getContract().getLoanStat())){
            //将账户的状态放入上下文中  只有还款服务才需要存放此字段
            ThreadLocalUtil.set("loanStat", la.getContract().getLoanStat());
        }
    }

    @Override
    protected void special() throws Exception {
        if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
                .contains(ctx.getRspCode())) {
            ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
        }
    }
}

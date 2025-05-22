package com.suning.fab.loan.service;

import com.suning.api.rsf.service.ApiRemoteMapService;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.workunit.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabRuntimeException;
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
 * <p>
 * 贷款开户放款
 * @return
 * @exception
 */
@Scope("prototype")
@Service
@Implement(contract = ApiRemoteMapService.class, implCode = "faloan-createAcctAndLoan")
public class Tp473004 extends ServiceTemplate {

    @Autowired
    Lns001 lns001;
    @Autowired
    Lns211 lns211;
    @Autowired
    Lns213 lns213;
    @Autowired
    Lns421 lns421;
    @Autowired
    Lns500 lns500;
    @Autowired
    Lns501 lns501;
    @Autowired
    Lns503 lns503;
    @Autowired
    Lns505 lns505;
    @Autowired
    Lns506 lns506;
    @Autowired
    Lns511 lns511;
    @Autowired
    Lns512 lns512;
    @Autowired
    Lns513 lns513;
    @Autowired
    Lns520 lns520;
    @Autowired
    Lns122 lns122;
    @Autowired
    Lns123 lns123;


    @Autowired
    Lns101 lns101;    //生成贷款本金账户

    @Autowired
    Lns118 lns118;    //现金贷开户

    @Autowired
    Lns102 lns102;    //开预收户

    @Autowired
    Lns201 lns201;    //扣息税金

    @Autowired
    Lns202 lns202;    //放款

    @Autowired
    Lns203 lns203;    //放款渠道

    @Autowired
    Lns253 lns253;    //多渠道

    @Autowired
    Lns204 lns204;    //扣息

    @Autowired
    Lns521 lns521;    //开户校验

    @Autowired
    Lns255 lns255;    //费用list入库

    @Autowired
    Lns120 lns120;    //代偿幂等拓展表
    @Autowired
    Lns121 lns121;//考核数据校验 入库

    public Tp473004() {
        needSerSeqNo = true;
    }

    @Override
    protected void run() throws Exception {
        ThreadLocalUtil.clean();
        //增加借新还旧的功能【20200511】

        if (VarChecker.asList("E").contains(ctx.getRequestDict("channelType"))) {
            //redis定时 还款防重
            String limitRepeatingRepaymentsProductList = GlobalScmConfUtil.getProperty("LimitRepeatingRepaymentsProductList", "");
            List productList = Arrays.asList(limitRepeatingRepaymentsProductList.split(","));
            
            if( VarChecker.isEmpty(ctx.getRequestDict("exAcctno")) || 
                VarChecker.isEmpty(ctx.getRequestDict("exBrc")))
                throw new FabException("LNS055","原借据信息");
            if(  !ctx.getBrc().equals(ctx.getRequestDict("exBrc").toString()) )
            	throw new FabException("SPS106","机构号和原机构号");
            
            LoanAgreement repayLa = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("exAcctno").toString(), ctx);

            String key = ctx.getRequestDict("exAcctno").toString() + ctx.getRequestDict("contractAmt").toString();
            if (!productList.contains(repayLa.getPrdId()) &&
                    false == (AcctStatistics.execute(key, ctx.getRequestDict("serialNo").toString()))) {
                throw new FabException("LNS054");
            }

            Map<String, Object> param = new HashMap<>();
            param.put("repayDate", ctx.getTranDate());
            // 借新还旧取原借据号赋值
            param.put("acctNo", ctx.getRequestDict("exAcctno").toString());

            trigger(lns421, "map4214");
            //20191118  试算子交易
            trigger(lns001, "map0014",param);

            //先结息本息
            lns501.setLnsBillStatistics(lns001.getLnsBillStatistics());
            lns501.setLa(lns001.getLoanAgreement());
            trigger(lns501, "map501", param);
            lns213.setLnsBillStatistics(lns001.getLnsBillStatistics());
            lns213.setLoanAgreement(lns001.getLoanAgreement());
            lns213.setDynamicCapDiff(lns001.getLoanAgreement().getBasicExtension().getDynamicCapDiff());
            trigger(lns213, "map2134");

            //罚息计提
            lns512.setLnssatistics(lns501.getLnsBillStatistics());
            lns512.setLnsbasicinfo(lns501.getLnsbasicinfo());
            lns512.setLa(lns001.getLoanAgreement());
            trigger(lns512, "map5124",param);

            //罚息落表
            if (!VarChecker.isEmpty(repayLa.getInterestAgreement().getCapRate())) {
                //罚息落表
                lns520.setRepayDate(ctx.getTranDate());
                lns520.setLnsBillStatistics(lns501.getLnsBillStatistics());
                lns520.setLnsbasicinfo(lns501.getLnsbasicinfo());
                lns520.setLa(lns512.getLa());
                trigger(lns520, "map5204");
            } else {
                lns513.setRepayDate(ctx.getTranDate());
                lns513.setLnsBillStatistics(lns501.getLnsBillStatistics());
                lns513.setLnsbasicinfo(lns501.getLnsbasicinfo());
                lns513.setLoanAgreement(lns512.getLa());
                trigger(lns513, "map5134");
            }

            //转列
            trigger(lns503, "map503", param);
            //贷款状态更新
            trigger(lns505, "map5054");

            /**
             * 借新还旧复用代偿的标记CompensateFlag，赋值为3，和代偿无关，便于对下面的情况进行处理
             * 将账本lnsbill的billproperty登记为SWITCH,
             * 账户明细表登记原借据的账户还款账户减款记录，摘要码为JXHJ借新还旧。
             * 还款明细表登记还款明细表lnsrpyinfo登记本息罚费还款金额loantype1=3
             */
            lns211.setCompensateFlag("3");
            lns211.setBillStatistics(lns501.getLnsBillStatistics());
            lns211.setIfOverdue(false);//是否逾期还款
            lns211.setSettleFlag("1");//整笔结清
            //还款本息
            lns211.setLoanAgreement(lns001.getLoanAgreement());
            Map<String, Object> param_211 = new HashMap<>();
            param_211.put("repayAmt", new FabAmount(Double.valueOf(ctx.getRequestDict("contractAmt").toString())));
            param_211.put("subNo", lns501.getLnsBillStatistics().getBillNo());
            param_211.put("acctNo", ctx.getRequestDict("exAcctno").toString());
//            param_211.put("acctNo", null);
            trigger(lns211, "map2114", param_211);
            //贷款结清状态更新
            trigger(lns506, "map5064");

            //校验放款金额能够结清原合同
            if (!"3".equals(lns506.getEndFlag()))
                throw new FabException("LNS170", "借新还旧,原借据");
            if ( ConstantDeclare.REPAYWAY.isEqualInterest(repayLa.getWithdrawAgreement().getRepayWay()) ) {
//			lns500.setLnsbasicinfo(lns517.getLnsbasicinfo());
                //查询基本信息给子交易句柄 并判断产品类型
                String acctNum = ctx.getRequestDict("exAcctno").toString();
                TblLnsbasicinfo lnsinfo;
                Map<String, Object> param2 = new HashMap<>();
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
                

//                Map<String, Object> map500 = new HashMap<>();
//                map500.put("repayDate", ctx.getTranDate());
//                map500.put("lnsbasicinfo", lnsinfo);
//                trigger(lns500, "DEFAULT", map500);
//				  //借新还旧暂无费用
//                if("2512617".equals(lnsinfo.getPrdcode()))
//      				trigger(lns511,"DEFAULT",map500);
                
              //2020-06-01
              lns500.setRepayDate(ctx.getTranDate());
              lns500.setLnsbasicinfo(lnsinfo);
              trigger(lns500, "map5004" );

            }
            // 借新还旧登记幂等拓展表
            trigger(lns122);

            // 获取老借据的余量封顶值
            trigger(lns123);
            // 将余量封顶值放入lns118进行新的借据计算
            lns118.setOrigDynamicCapValue(lns123.getDynamicCapValue());

        }


        //载入协议
        LoanAgreement la = LoanAgreementProvider.genLoanAgreement(ctx.getRequestDict("productCode").toString());
        //开户校验
        lns521.setLa(la);
        trigger(lns521);
        //2017-12-05 起息日期传值，合同开始日取起息日
        //la.getContract().setContractStartDate(ctx.getTranDate());

        if (VarChecker.isEmpty(ctx.getRequestDict("startIntDate")) || ctx.getRequestDict("startIntDate").toString().isEmpty()) {
            la.getContract().setStartIntDate(ctx.getTranDate());
            la.getContract().setContractStartDate(ctx.getTranDate());
        } else {
            la.getContract().setStartIntDate(ctx.getRequestDict("startIntDate").toString());
            la.getContract().setContractStartDate(ctx.getRequestDict("startIntDate").toString());
        }

        lns101.setLoanAgreement(la);

        trigger(lns101, "map101");
        //防止幂等报错被拦截
        trigger(lns121);
        lns118.setLoanAgreement(la);
        trigger(lns118);

//        VarChecker.asList("2412631", "2412632", "2412633", "2412623", "2512617").contains(ctx.getRequestDict("productCode").toString())
        //代偿
        if ( "true".equals(la.getInterestAgreement().getIsCompensatory())) {
            if (!VarChecker.isEmpty(ctx.getRequestDict("exAcctno"))
                    && !VarChecker.isEmpty(ctx.getRequestDict("exPeriod"))
                    && !VarChecker.isEmpty(ctx.getRequestDict("exinvesteeId"))) {
                trigger(lns120);
            }

        }

        if (!VarChecker.isEmpty(ctx.getRequestDict("pkgList3"))) {
            lns255.setLoanAgreement(la);
            trigger(lns255);
        }
        else
        {
            //借新还旧时，房抵贷没费用给费用信息表登记一条0费用信息，供预约还款查询使用
            if( "2412615".equals(la.getPrdId())  )
            {
                TblLnsfeeinfo lnsfeeinfo = LoanFeeUtils.prepareFeeinfo(la,ctx,ConstantDeclare.FEEREPAYWAY.STAGING,new FabRate(0.00));
                lnsfeeinfo.setProvisionflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
                lnsfeeinfo.setFeestat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
                lnsfeeinfo.setFeebrc("51230004");

                try {
                    DbAccessUtil.execute("Lnsfeeinfo.insert", lnsfeeinfo);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS100", "TblLnsfeeinfo");
                }
            }

        }


        trigger(lns102);
        trigger(lns201);
        trigger(lns202);
        /**
         * 放款渠道:channelType	0:多渠道
         */
        if ( "0".equals(ctx.getRequestDict("channelType").toString()) ) {
            trigger(lns253);
        } else {
        	if ( !"E".equals(ctx.getRequestDict("channelType").toString()) ) 
				trigger(lns203);
        }
        trigger(lns204);



    }

    @Override
    protected void special() throws Exception {
        if (VarChecker.asList(ConstantDeclare.RSPCODE.TRAN.SUCCESS, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY)
                .contains(ctx.getRspCode())) {
            ctx.setRspCode(ConstantDeclare.RSPCODE.TRAN.SUCCESS);
        }
    }

    @Override
    protected void normalBeforeRun() {
        if (!VarChecker.isValidConstOption(ctx.getRequestDict("repayWay").toString(), ConstantDeclare.REPAYWAY.class)) {
            throw new FabRuntimeException("LNS051");
        }

        if (VarChecker.isEmpty(ctx.getRequestDict("loanType").toString()))
            throw new FabRuntimeException("LNS052");
    }
}

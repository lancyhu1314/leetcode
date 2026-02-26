package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RpyList;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：费用还款（除保费的通用还款）
 *
 * @Author 18049705 MYP
 * @Date Created in 16:57 2020/3/30
 * @see
 */
@Scope("prototype")
@Repository
public class Lns260 extends WorkUnit {

    private String acctNo; //贷款账号
    private String repayAcctNo;
    private FabAmount repayAmt; //还款金额
    private FabAmount forfeetAmt = new FabAmount();//已还费用	forfeetAmt
    private FabAmount damagesAmt = new FabAmount();//已还违约金

    private String endFlag;//结清标志	endFlag
    private FabAmount adjustDamage ;//减免违约金
    private FabAmount adjustFee ;
    private String settleFlag;//结清标志 0-不结清  1-结清
    private String bankSubject; //银行科目
    private String outSerialNo;
    private String repayChannel;
    private String customID;
    private String platformId;
    private String cooperateId;
    private LoanAgreement loanAgreement;
    private LnsBillStatistics lnsBillStatistics;

    private String discountfeeFlag;//贴息标志

    private FabAmount couponFeeAmt=new FabAmount(0.00);//贴费金额

    private String compensateFlag;//代偿标志

	String  repayList;  //还款明细2020-07-17
	
	String  reduceRepayList;

    //0金额结息费用list
    private Map<String, Map<String,FabAmount>> billFeeRepay;

    private String	childBrc;
    @Autowired
    LoanEventOperateProvider eventProvider;
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;
    @Override
    public void run() throws Exception {
        //还款
        loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, tranctx,loanAgreement);
        
        if( "YES".equals(GlobalScmConfUtil.getProperty("checkFeeBrc", "")) )
        {
        	if( "2412615".equals(loanAgreement.getPrdId()) && !VarChecker.isEmpty(childBrc) &&
        		loanAgreement.getFeeAgreement().getLnsfeeinfos().size() > 0 &&
        		!childBrc.equals(loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getFeebrc()) )
            {
            	throw new FabException("LNS232");
            }
        }
        
        
        //幂等

        //绿地小贷往来方代码
        String reserv4="";
        if( !VarChecker.isEmpty(cooperateId) )
        	reserv4 = cooperateId;

        Map<String, Map<String,FabAmount>> repayAmtList = new HashMap<String, Map<String,FabAmount>>();
        if(billFeeRepay!=null&&billFeeRepay.size()>0){
            repayAmtList.putAll(billFeeRepay);
        }

        if (outSerialNo != null)
            loanAgreement.getFundInvest().setOutSerialNo(getOutSerialNo());
        if (getBankSubject() != null)
            loanAgreement.getFundInvest().setFundChannel(getBankSubject());
        if (getRepayChannel() != null)
            loanAgreement.getFundInvest().setChannelType(getRepayChannel());

        //还款接口无需登记幂等登记簿
        if(!"471007".equals(getTranctx().getTranCode())){
      TblLnsinterface lnsinterface = new TblLnsinterface();
        lnsinterface.setTrandate(tranctx.getTermDate());
        lnsinterface.setSerialno(tranctx.getSerialNo());
        lnsinterface.setAccdate(tranctx.getTranDate());
        lnsinterface.setSerseqno(tranctx.getSerSeqNo());
        lnsinterface.setTrancode(tranctx.getTranCode());
        lnsinterface.setBrc(tranctx.getBrc());
        lnsinterface.setAcctname(loanAgreement.getCustomer().getCustomName());
        lnsinterface.setUserno(repayAcctNo);
        lnsinterface.setAcctno(loanAgreement.getContract().getReceiptNo());

        lnsinterface.setTranamt(0.00);
        //科目号存在billno
        if(!VarChecker.isEmpty(bankSubject))
            lnsinterface.setBillno(bankSubject);

        lnsinterface.setMagacct(loanAgreement.getContract().getReceiptNo());
        lnsinterface.setOrgid(loanAgreement.getFundInvest().getInvestee());
        lnsinterface.setBankno(loanAgreement.getFundInvest().getOutSerialNo());
        //预收渠道登记 2019-01-16
        lnsinterface.setReserv5(loanAgreement.getFundInvest().getChannelType());
        if(loanAgreement.getFundInvest().getChannelType() != null)
            lnsinterface.setReserv5(loanAgreement.getFundInvest().getChannelType());
        lnsinterface.setSumdelfint(adjustDamage.getVal());
        lnsinterface.setSumdelint(adjustFee.getVal());
        try{
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
        } catch(FabSqlException e) {
            //幂等
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                Map<String, Object> params = new HashMap<>();
                params.put("serialno", tranctx.getSerialNo());
                params.put("trancode", tranctx.getTranCode());
                //幂等登记薄 幂等的话返回数据要从表里面取
                lnsinterface = LoanDbUtil.queryForObject("Lnsinterface.selectByUk", params,
                            TblLnsinterface.class);

//                forfeetAmt = new FabAmount(lnsinterface.getTranamt());
                forfeetAmt = new FabAmount(lnsinterface.getSumrint());
                damagesAmt = new FabAmount(lnsinterface.getSumrfint());

                //3,4都是结清
                endFlag = VarChecker.asList("3","4").contains(lnsinterface.acctFlag2Endflag().trim())?"Y":"N";
                
                //展示还款明细准备数据 2020-07-17
                if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
                {
                	Map<String, Object> param = new HashMap<>();
//                    param.put("acctno", acctNo);
//                    param.put("brc", getTranctx().getBrc());
                    param.put("trandate", lnsinterface.getAccdate());
                    param.put("serseqno", lnsinterface.getSerseqno());
                    param.put("key", ConstantDeclare.KEYNAME.HFMX);
                    TblLnsinterfaceex interfaceex;
                    try {
                        interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex_hkmx", param, TblLnsinterfaceex.class);
                    } catch (FabSqlException e1) {
                        throw new FabException(e1, "SPS103", "query_lnsinterfaceex");
                    }
                    if (interfaceex != null)
                        repayList = interfaceex.getValue();
                    else
                    	repayList = null;
                }

                throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            } else
                throw new FabException(e, "SPS100", "lnsinterface");
        }
        }

        //保存还款信息
        Map<String,FabAmount> feeRpyInfo = new HashMap<>();



        Map<String, Object> bparam = new HashMap<String, Object>();
        bparam.put("acctno", acctNo);
        bparam.put("openbrc", tranctx.getBrc());
        TblLnsbasicinfo lnsbasicinfo = LoanDbUtil.queryForObject("Lnsbasicinfo.selectByUk", bparam, TblLnsbasicinfo.class);

        if (null == lnsbasicinfo) {
            throw new FabException("SPS104", "lnsbasicinfo");
        }

        // 过预收渠道，操作预收户中金额减款
        LnsAcctInfo oppNAcctInfo = null;
        //代偿自动结清费用 不用过预收户
        if(VarChecker.asList(ConstantDeclare.PAYCHANNEL.TWO).contains(repayChannel) &&
           repayAmt.isPositive() && !ConstantDeclare.COMPENSATEFLAG.OTHERPAY.equals(compensateFlag)) {
            //扣减预收户金额
            TblLnsassistdyninfo lnsassistdyninfo = LoanAssistDynInfoUtil.updatePreaccountInfo(getTranctx(), childBrc,getRepayAcctNo(), ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT, repayAmt, "sub","");
            if(lnsassistdyninfo.getCurrbal() < 0.00)
                throw new FabException("LNS019");
            oppNAcctInfo = new LnsAcctInfo("", ConstantDeclare.ASSISTACCOUNTINFO.PREFUNDACCT, "", new FabCurrency());

        }

        //查询历史期账本
        // 读取贷款账单
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", acctNo);
        param.put("brc", tranctx.getBrc());
        List<TblLnsbill> billList = LoanDbUtil.queryForList("Lnsbill.query_feebill_repay", param, TblLnsbill.class);

        boolean ifOverdue = false;
        //筛选费用和违约金账本
        List<TblLnsbill> lnsFeeBills = new ArrayList<>();
        for(TblLnsbill lnsbill :billList) {

            if (LoanFeeUtils.isFeeType(lnsbill.getBilltype())
                    || LoanFeeUtils.isPenalty(lnsbill.getBilltype())) {
                if(new FabAmount(lnsbill.getBillamt()).isPositive()) {
                    lnsFeeBills.add(lnsbill);
                    //判断是否逾期
                    if (!ifOverdue
                            && lnsbill.getSettleflag().trim().equals(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING)
                            && (!lnsbill.getBillstatus().trim().equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL)
                            || CalendarUtil.after(tranctx.getTranDate(), lnsbill.getCurenddate().trim()))) {
                        ifOverdue = true;
                    }
                }else if(new FabAmount(lnsbill.getBillamt()).isZero()){
                    Map<String,Object> upbillmap = new HashMap<>();
                    Double minAmt =LoanFeeUtils.repaysql(repayAmt,upbillmap, lnsbill,tranctx);
                    LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, lnsbill.getBilltype(),lnsbill.getBillstatus(),new FabAmount(minAmt));
                    //展示还款明细准备数据 2020-07-17
                    if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
                        LoanRpyInfoUtil.addRpyList(repayAmtList, lnsbill.getBilltype(), lnsbill.getPeriod().toString(), new FabAmount(minAmt),lnsbill.getRepayway());
                }

            }
        }


        //账本排序  只对费用进行排序
        //排序顺序
        if (ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(lnsbasicinfo.getLoanstat()) ||
                ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(lnsbasicinfo.getLoanstat())) {
            LoanRepayOrderHelper.allBadSort(lnsFeeBills);

        }else {
            LoanRepayOrderHelper.allNormalSort(lnsFeeBills);
        }

            //固定担保费还款金额
        FabAmount gddbAmt = new FabAmount();

        //尽量不在循环里创建对象
        Map<String,Object> upbillmap = new HashMap<>();
        int repaysize = 0;


        //费用还款
        for(TblLnsbill feebill:lnsFeeBills){
            if (!repayAmt.isPositive())
                break;
            String childBrc = "";
            if(LoanFeeUtils.isPenalty(feebill.getBilltype().trim())){
                //匹配
                //三编码匹配上 对应的费用账本
                for(TblLnsbill lnsbill :billList){
                    if (feebill.getDertrandate().equals(lnsbill.getTrandate())
                            && feebill.getDertxseq().equals(lnsbill.getTxseq())
                            && feebill.getDerserseqno().equals(lnsbill.getSerseqno())) {
                        //费用账本匹配上对应的 费用信息  得到 费用机构号
                        childBrc = LoanFeeUtils.matchFeeInfo(loanAgreement, lnsbill.getBilltype().trim(), lnsbill.getRepayway().trim()).getFeebrc();
                        break;
                    }
                }

            }else {
                TblLnsfeeinfo lnsfeeinfo = LoanFeeUtils.matchFeeInfo(loanAgreement, feebill.getBilltype().trim(), feebill.getRepayway().trim());
                childBrc = lnsfeeinfo.getFeebrc();

            }
            //代偿还费修改账单类型
            if(!VarChecker.isEmpty(compensateFlag)){
                upbillmap.put("billproperty",ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN);
            }
            Double minAmt = LoanFeeUtils.repaysql(repayAmt,upbillmap, feebill,tranctx);
//            repayAmt.selfSub(minAmt);

            if(LoanFeeUtils.isPenalty(feebill.getBilltype().trim())){


                damagesAmt.selfAdd(minAmt);
                LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, feebill.getBilltype(),feebill.getBillstatus(),new FabAmount(minAmt));
                //展示还款明细准备数据 2020-07-17
                if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
                	LoanRpyInfoUtil.addRpyList(repayAmtList, feebill.getBilltype(), feebill.getPeriod().toString(), new FabAmount(minAmt),feebill.getRepayway());
                
                //还款事件
                LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEED, feebill.getBillstatus(),
                        new FabCurrency(),childBrc);
                lnsAcctInfo.setMerchantNo(repayAcctNo);
                lnsAcctInfo.setCustType(loanAgreement.getCustomer().getCustomType());
                lnsAcctInfo.setCustName(loanAgreement.getCustomer().getCustomName());
                lnsAcctInfo.setPrdCode(loanAgreement.getPrdId());
                lnsAcctInfo.setCancelFlag(feebill.getCancelflag());
                eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, new FabAmount(minAmt), lnsAcctInfo, oppNAcctInfo,
                        loanAgreement.getFundInvest(), ConstantDeclare.COMPENSATEFLAG.OTHERPAY.equals(compensateFlag)?ConstantDeclare.BRIEFCODE.DCJQ:ConstantDeclare.BRIEFCODE.DBHK, tranctx, childBrc,platformId,"",reserv4);

                sub.operate(lnsAcctInfo, null, new FabAmount(minAmt), loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.DBHK,
                        feebill.getTrandate().toString(), feebill.getSerseqno(), feebill.getTxseq(), tranctx);
            }else{
                //应该还需要区分一下费用 违约金
                forfeetAmt.selfAdd(minAmt);
                LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, feebill.getBilltype(),feebill.getBillstatus(),new FabAmount(minAmt));
                //展示还款明细准备数据 2020-07-17
                if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
                	LoanRpyInfoUtil.addRpyList(repayAmtList, feebill.getBilltype(), feebill.getPeriod().toString(), new FabAmount(minAmt),feebill.getRepayway());
                
                //抛事件

                //固定担保费
                if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(feebill.getRepayway())) {

                    eventGDDB(gddbAmt, feebill.getBillstatus(), minAmt,childBrc,reserv4,oppNAcctInfo);
                    //一次性费用结清了
                    if(new FabAmount(feebill.getBillbal()).sub(minAmt).isZero())
                        LoanFeeUtils.updateCA(LoanFeeUtils.matchFeeInfo(loanAgreement, feebill.getBilltype().trim(),feebill.getRepayway().trim() ));
                }else{
                    //还款事件
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, feebill.getBillstatus(),
                            new FabCurrency(),childBrc);
                    eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, new FabAmount(minAmt), lnsAcctInfo, oppNAcctInfo,
                            loanAgreement.getFundInvest(), ConstantDeclare.COMPENSATEFLAG.OTHERPAY.equals(compensateFlag)?ConstantDeclare.BRIEFCODE.DCJQ:ConstantDeclare.BRIEFCODE.FYHK, tranctx, childBrc,platformId,"",reserv4);
                }
                //多种简要码
                String briefCode  =  ConstantDeclare.BRIEFCODE.SFHK;
                //管理费
                if(ConstantDeclare.FEETYPE.RMFE.equals(feebill.getBilltype().trim())) {
                    briefCode =  ConstantDeclare.BRIEFCODE.RFHK;
                }
                //第三方服务费 2020-06-09
                else if( LoanFeeUtils.isOtherFee(feebill.getBilltype().trim())   ) {
                    briefCode =  ConstantDeclare.BRIEFCODE.FYHK;
                }
                LoanFeeUtils.accountsub( sub,tranctx, feebill, minAmt,loanAgreement,repayAcctNo,briefCode,childBrc);
            }


            if (feebill.getBillbal().equals(minAmt))// 余额等于发生额 该条账单处理结束 加1
                repaysize++;
            repayAmt.selfSub(minAmt);
        }

        endFlag = "Y";
        //到期结清 金额判断 不需要还到未来期
        if("2".equals(settleFlag)){
            if(repayAmt.isPositive()||repaysize != lnsFeeBills.size()){
                throw new FabException("LNS202");
            }else{
                endFlag = "N";
            }
        }
       //未来期的还款
        for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
            if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsfeeinfo.getFeestat()))
                endFlag = "N";
        }
        if ( CalendarUtil.afterAlsoEqual(tranctx.getTranDate(),loanAgreement.getContract().getContractEndDate() )
                && repaysize == lnsFeeBills.size()) {
            endFlag = "Y";
        }


        if(endFlag.equals("N") &&(repayAmt.isPositive()||
                //0金额提前结清优化 历史期必须已结清 未来期0金额结清的情况
                ("1".equals(settleFlag)&&repaysize == lnsFeeBills.size()))
                &&CalendarUtil.before(tranctx.getTranDate(),loanAgreement.getContract().getContractEndDate() )){
           List<LnsBill> feeAdvance = new ArrayList<>();
            endFlag = LoanFeeUtils.feeRepaymentBill(acctNo,tranctx ,repayAmt , feeAdvance ,settleFlag,feeRpyInfo,repayAmtList);
            //事件
            for(LnsBill advance :feeAdvance){

                Double minAmt = advance.getBillAmt().sub(advance.getBillBal()).getVal();
                forfeetAmt.selfAdd(minAmt);
                if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(advance.getRepayWay())) {

                    eventGDDB(gddbAmt, advance.getBillStatus(), minAmt,advance.getLnsfeeinfo().getFeebrc(),reserv4,oppNAcctInfo);
                    //一次性费用结清了
                    if(advance.getBillBal().sub(minAmt).isZero())
                        LoanFeeUtils.updateCA(LoanFeeUtils.matchFeeInfo(loanAgreement, advance.getBillType(),advance.getRepayWay() ));
                }else{
                    //还款事件
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, advance.getBillStatus(),
                            new FabCurrency(),advance.getLnsfeeinfo().getFeebrc());
                    eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, new FabAmount(minAmt), lnsAcctInfo, oppNAcctInfo,
                            loanAgreement.getFundInvest(), ConstantDeclare.COMPENSATEFLAG.OTHERPAY.equals(compensateFlag)?ConstantDeclare.BRIEFCODE.DCJQ:ConstantDeclare.BRIEFCODE.FYHK, tranctx, advance.getLnsfeeinfo().getFeebrc(),platformId,"",reserv4);
                }
                //多种简要码
                String briefCode  =  ConstantDeclare.BRIEFCODE.SFHK;
                //管理费
                if(ConstantDeclare.FEETYPE.RMFE.equals(advance.getBillType().trim())) {
                    briefCode =  ConstantDeclare.BRIEFCODE.RFHK;
                }
                //第三方服务费 2020-06-09
                else if(   LoanFeeUtils.isOtherFee(advance.getBillType().trim())  ) {
                    briefCode =  ConstantDeclare.BRIEFCODE.FYHK;
                }
                LoanFeeUtils.accountsub( sub,tranctx, advance, minAmt,loanAgreement,repayAcctNo,briefCode);
            }

        }

        if("1".equals(settleFlag)){


            if(endFlag.equals("N")){

                if(repaysize != lnsFeeBills.size())
                    throw new FabException("LNS202");




                //要结清的  当天结费 刚好还清
                for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
                    if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsfeeinfo.getFeestat()))
                    {
                        continue;
                    }
                    //本金结清的 未来期算不出费用
                    if(ConstantDeclare.FEEBASE.BAL.equals(lnsfeeinfo.getFeebase())
                            &&!loanAgreement.getContract().getBalance().isPositive()){
                        continue;
                    }

                    if(CalendarUtil.before(lnsfeeinfo.getLastfeedate(), tranctx.getTranDate())
                            ||ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(lnsfeeinfo.getAdvancesettle()))
                        throw new FabException("LNS202");

                }

            }
            endFlag = "Y";
        }

        if(repayAmt.isPositive()){
                throw new FabException("LNS058",repayAmt.add(forfeetAmt).add(damagesAmt).toString() ,forfeetAmt.add(damagesAmt).toString());
        }

        if(endFlag.equals("Y")) {
            LoanFeeUtils.updateAllCA(tranctx.getBrc(),acctNo );
        }
        //更新幂等表
        Map<String, Object> fddmap = new HashMap<>();

        fddmap.put("tranamt", forfeetAmt.add(damagesAmt).getVal());
        fddmap.put("acctflag", TblLnsinterface.endflag2AcctFlag2("Y".equals(endFlag)?"3":"1",ifOverdue ));
        fddmap.put("serialno", tranctx.getSerialNo());
        fddmap.put("trancode", tranctx.getTranCode());
        //reserv3 存固定担保费还款金额
        fddmap.put("reserv3", gddbAmt.toString());
        fddmap.put("sumrint", forfeetAmt.getVal());
        fddmap.put("sumrfint", damagesAmt.getVal());
        try {
            DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_fdd", fddmap);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsinterface");
        }
        //还款
        //1是自费
        LoanRpyInfoUtil.saveRpyInfos(tranctx, 0,feeRpyInfo ,acctNo ,repayAcctNo ,ConstantDeclare.COMPENSATEFLAG.OTHERPAY.equals(compensateFlag)?compensateFlag:"1");
        
        
        //展示还款明细准备数据 2020-07-17
        if( "true".equals(loanAgreement.getInterestAgreement().getShowRepayList()) )
        {
        	repayList = LoanRpyInfoUtil.getRepayList(acctNo, tranctx, repayAmtList,loanAgreement);
        	//新增“当期已减费用”
        	if(!VarChecker.isEmpty(reduceRepayList)){
        		List<RpyList> repayToJsonList = new ArrayList<RpyList>();
        		HashMap<String, RpyList> repayInfoMap = new HashMap<String, RpyList>();
        		List<Map> reduceListMap= (List<Map>)JSONArray.parse(reduceRepayList);
        		for(Map temp:reduceListMap){
        			RpyList repayInfo = new RpyList();
        			repayInfo.setRepayterm((Integer)temp.get("repayterm"));
        			repayInfo.setForfeetAmt(new FabAmount(0.00));
        			repayInfo.setSubFeeAmt(new FabAmount(((BigDecimal)temp.get("forfeetAmt")).doubleValue()));
        			repayInfo.setCouponFeeAmt(new FabAmount(0.00));
        			repayInfo.setDiscountfeeFlag(StringUtil.obj2str(temp.get("discountfeeFlag")));
        			repayInfoMap.put(repayInfo.getRepayterm()+"", repayInfo);
        			repayToJsonList.add(repayInfo);
        		}

        		//合并费用减免明细
        		List<Map> repayListMap= (List<Map>)JSONArray.parse(repayList);
        		if(!repayListMap.isEmpty()){
        			repayToJsonList.clear();
        		}
        		for(Map temp:repayListMap){
        			RpyList repayInfo = new RpyList();
        			repayInfo.setRepayterm((Integer)temp.get("repayterm"));
        			repayInfo.setSubFeeAmt(new FabAmount(0.00));
        			repayInfo.setForfeetAmt(new FabAmount(((BigDecimal)temp.get("forfeetAmt")).doubleValue()));
        			repayInfo.setCouponFeeAmt(new FabAmount(((BigDecimal)temp.get("couponFeeAmt")).doubleValue()));
        			repayInfo.setDiscountfeeFlag(StringUtil.obj2str(temp.get("discountfeeFlag")));
        			if(repayInfoMap.get(repayInfo.getRepayterm()+"")!=null){
        				repayInfo.setSubFeeAmt(repayInfoMap.get(repayInfo.getRepayterm()+"").getSubFeeAmt());
        				for (int i =1;i<repayInfo.getRepayterm();i++){
        					if (repayInfoMap.get(i+"")!=null){
        						repayToJsonList.add(repayInfoMap.get(i+""));
        					}
        				}
        			}

        			repayToJsonList.add(repayInfo);
        			if(temp.get("couponFeeAmt")!=null){
        				couponFeeAmt.selfAdd(((BigDecimal)temp.get("couponFeeAmt")).doubleValue());
        			}
        			if("Y".equals(StringUtil.obj2str(temp.get("discountfeeFlag")))){
        				discountfeeFlag=StringUtil.obj2str(temp.get("discountfeeFlag"));
        			}
        		}
        		repayList =JsonTransfer.ToJson(repayToJsonList);
        		AccountingModeChange.saveInterfaceEx(tranctx, acctNo, ConstantDeclare.KEYNAME.HFMX, "还费明细", repayList.length() > 2000 ? repayList.substring(0, 2000) : repayList);
        	}else{
        		//对结果集进行求和
        		List<Map> repayListMap= (List<Map>)JSONArray.parse(repayList);
        		for(Map temp:repayListMap){
        			if(temp.get("couponFeeAmt")!=null){
        				couponFeeAmt.selfAdd(((BigDecimal)temp.get("couponFeeAmt")).doubleValue());
        			}
        			if("Y".equals(StringUtil.obj2str(temp.get("discountfeeFlag")))){
        				discountfeeFlag=StringUtil.obj2str(temp.get("discountfeeFlag"));
        			}
        		}
        		AccountingModeChange.saveInterfaceEx(tranctx, acctNo, ConstantDeclare.KEYNAME.HFMX, "还费明细", repayList.length() > 2000 ? repayList.substring(0, 2000) : repayList);
        	}
        } else {
        	repayList = null;
        }

    }

    /**
     * Gets the value of acctNo.
     *
     * @return the value of acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }

    private void eventGDDB(FabAmount gddbAmt, String  billstatus, Double minAmt, String childBrc,String reserv4,LnsAcctInfo oppNAcctInfo) throws FabException {
        gddbAmt.selfAdd(minAmt);
        //固定担保费 还款事件
        LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, billstatus,
                new FabCurrency(),childBrc);


        //2412624,2412626任性贷搭融担费，假设放款机构是51030000.子机构是51340000，那么，
        //事件的reserve3传R5103（截取51030000的前四位加R前缀）
        String reserv = "";
        if (VarChecker.asList("2412624", "2412626").contains(loanAgreement.getPrdId())) {
            reserv = "R" + tranctx.getBrc().substring(0, 4);
        }
        else
        	reserv = reserv4;
        eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, new FabAmount(minAmt), lnsAcctInfo, oppNAcctInfo,
                loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDHK, tranctx, childBrc, platformId, "", reserv);
        //更新固定担保费
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
        JSONObject tunneldata = JSONObject.parseObject(lnsbasicinfoex.getTunneldata());
        tunneldata.put("feeamt",new FabAmount(minAmt).add( tunneldata.getDouble("feeamt")).getVal());
        //登记 固定担保费的已还
        queryparam.put("tunneldata", tunneldata.toString());
        try {
            DbAccessUtil.execute("Lnsbasicinfoex.updateTunneldata", queryparam);
        } catch (FabSqlException e) {
            throw new FabException("SPS100", "lnsbasicinfoex", e);
        }


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
     * Gets the value of repayAcctNo.
     *
     * @return the value of repayAcctNo
     */
    public String getRepayAcctNo() {
        return repayAcctNo;
    }

    /**
     * Sets the repayAcctNo.
     *
     * @param repayAcctNo repayAcctNo
     */
    public void setRepayAcctNo(String repayAcctNo) {
        this.repayAcctNo = repayAcctNo;

    }

    /**
     * Gets the value of repayAmt.
     *
     * @return the value of repayAmt
     */
    public FabAmount getRepayAmt() {
        return repayAmt;
    }

    /**
     * Sets the repayAmt.
     *
     * @param repayAmt repayAmt
     */
    public void setRepayAmt(FabAmount repayAmt) {
        this.repayAmt = repayAmt;

    }



    /**
     * Gets the value of forfeetAmt.
     *
     * @return the value of forfeetAmt
     */
    public FabAmount getForfeetAmt() {
        return forfeetAmt;
    }

    /**
     * Sets the forfeetAmt.
     *
     * @param forfeetAmt forfeetAmt
     */
    public void setForfeetAmt(FabAmount forfeetAmt) {
        this.forfeetAmt = forfeetAmt;

    }

    /**
     * Gets the value of endFlag.
     *
     * @return the value of endFlag
     */
    public String getEndFlag() {
        return endFlag;
    }

    /**
     * Sets the endFlag.
     *
     * @param endFlag endFlag
     */
    public void setEndFlag(String endFlag) {
        this.endFlag = endFlag;

    }

    /**
     * Gets the value of settleFlag.
     *
     * @return the value of settleFlag
     */
    public String getSettleFlag() {
        return settleFlag;
    }

    /**
     * Sets the settleFlag.
     *
     * @param settleFlag settleFlag
     */
    public void setSettleFlag(String settleFlag) {
        this.settleFlag = settleFlag;

    }

    /**
     * Gets the value of bankSubject.
     *
     * @return the value of bankSubject
     */
    public String getBankSubject() {
        return bankSubject;
    }

    /**
     * Sets the bankSubject.
     *
     * @param bankSubject bankSubject
     */
    public void setBankSubject(String bankSubject) {
        this.bankSubject = bankSubject;

    }

    /**
     * Gets the value of outSerialNo.
     *
     * @return the value of outSerialNo
     */
    public String getOutSerialNo() {
        return outSerialNo;
    }

    /**
     * Sets the outSerialNo.
     *
     * @param outSerialNo outSerialNo
     */
    public void setOutSerialNo(String outSerialNo) {
        this.outSerialNo = outSerialNo;

    }

    /**
     * Gets the value of repayChannel.
     *
     * @return the value of repayChannel
     */
    public String getRepayChannel() {
        return repayChannel;
    }

    /**
     * Sets the repayChannel.
     *
     * @param repayChannel repayChannel
     */
    public void setRepayChannel(String repayChannel) {
        this.repayChannel = repayChannel;

    }

    /**
     * Gets the value of customID.
     *
     * @return the value of customID
     */
    public String getCustomID() {
        return customID;
    }

    /**
     * Sets the customID.
     *
     * @param customID customID
     */
    public void setCustomID(String customID) {
        this.customID = customID;

    }

    /**
     * Gets the value of platformId.
     *
     * @return the value of platformId
     */
    public String getPlatformId() {
        return platformId;
    }

    /**
     * Sets the platformId.
     *
     * @param platformId platformId
     */
    public void setPlatformId(String platformId) {
        this.platformId = platformId;

    }

    /**
     * Gets the value of loanAgreement.
     *
     * @return the value of loanAgreement
     */
    public LoanAgreement getLoanAgreement() {
        return loanAgreement;
    }

    /**
     * Sets the loanAgreement.
     *
     * @param loanAgreement loanAgreement
     */
    public void setLoanAgreement(LoanAgreement loanAgreement) {
        this.loanAgreement = loanAgreement;

    }

    /**
     * Gets the value of lnsBillStatistics.
     *
     * @return the value of lnsBillStatistics
     */
    public LnsBillStatistics getLnsBillStatistics() {
        return lnsBillStatistics;
    }

    /**
     * Sets the lnsBillStatistics.
     *
     * @param lnsBillStatistics lnsBillStatistics
     */
    public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
        this.lnsBillStatistics = lnsBillStatistics;

    }

    /**
     * Gets the value of damagesAmt.
     *
     * @return the value of damagesAmt
     */
    public FabAmount getDamagesAmt() {
        return damagesAmt;
    }

    /**
     * Sets the damagesAmt.
     *
     * @param damagesAmt damagesAmt
     */
    public void setDamagesAmt(FabAmount damagesAmt) {
        this.damagesAmt = damagesAmt;

    }

    /**
     * Gets the value of adjustDamage.
     *
     * @return the value of adjustDamage
     */
    public FabAmount getAdjustDamage() {
        return adjustDamage;
    }

    /**
     * Sets the adjustDamage.
     *
     * @param adjustDamage adjustDamage
     */
    public void setAdjustDamage(FabAmount adjustDamage) {
        this.adjustDamage = adjustDamage;

    }

    /**
     * Gets the value of adjustFee.
     *
     * @return the value of adjustFee
     */
    public FabAmount getAdjustFee() {
        return adjustFee;
    }

    /**
     * Sets the adjustFee.
     *
     * @param adjustFee adjustFee
     */
    public void setAdjustFee(FabAmount adjustFee) {
        this.adjustFee = adjustFee;

    }

    /**
     * @return the cooperateId
     */
    public String getCooperateId() {
        return cooperateId;
    }

    /**
     * @param cooperateId the cooperateId to set
     */
    public void setCooperateId(String cooperateId) {
        this.cooperateId = cooperateId;
    }
	/**
	 * @return the childBrc
	 */
	public String getChildBrc() {
		return childBrc;
	}

	/**
	 * @param childBrc the childBrc to set
	 */
	public void setChildBrc(String childBrc) {
		this.childBrc = childBrc;
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

    public String getCompensateFlag() {
        return compensateFlag;
    }

    public void setCompensateFlag(String compensateFlag) {
        this.compensateFlag = compensateFlag;
    }


    public String getDiscountfeeFlag() {
        return discountfeeFlag;
    }

    public void setDiscountfeeFlag(String discountfeeFlag) {
        this.discountfeeFlag = discountfeeFlag;
    }

    public Map<String, Map<String, FabAmount>> getBillFeeRepay() {
        return billFeeRepay;
    }

    public void setBillFeeRepay(Map<String, Map<String, FabAmount>> billFeeRepay) {
        this.billFeeRepay = billFeeRepay;
    }

    public FabAmount getCouponFeeAmt() {
        return couponFeeAmt;
    }

    public void setCouponFeeAmt(FabAmount couponFeeAmt) {
        this.couponFeeAmt = couponFeeAmt;
    }

	public String getReduceRepayList() {
		return reduceRepayList;
	}

	public void setReduceRepayList(String reduceRepayList) {
		this.reduceRepayList = reduceRepayList;
	}
    
    

}

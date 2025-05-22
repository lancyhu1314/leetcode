package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.utils.ConstantDeclare.BRIEFCODE;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

/**
 *  〈一句话功能简述〉
 *  〈功能详细描述〉：
 * @Author 18049705
 * @Description  保费还款
 * @Date Change in 17:55 2019/5/22
 */

@Scope("prototype")
@Repository
public class Lns242 extends WorkUnit {


	String acctNo; //贷款账号
	String repayAcctNo;
	FabAmount repayAmt; //还款金额
	String childBrc; //子机构
	FabAmount forfeetAmt;//已还费用	forfeetAmt
	FabAmount damagesAmt;//已还违约金
	String endFlag;//结清标志	endFlag
	LoanAgreement loanAgreement;
	String memo;//摘要
	String settleFlag;//结清标志 0-不结清  1-结清
	String bankSubject; //银行科目
	String outSerialNo;
	String repayChannel;
	String platformId;

	private String customID;

	//幂等表的插入数据
	private String reserv3 = "";
	private String reserv4= "";
	@Autowired LoanEventOperateProvider eventProvider;
    @Autowired
    @Qualifier("accountSuber")
    AccountOperator sub;

	//跨期减免手续费
	private FabAmount 	exceedReduce;

	//自动减免金额
	private FabAmount 	adjustFee = new FabAmount();
	//实际还款日
	private String  realDate;
	//上次还款日
	private String termretdate = "1970-01-01";
	private Boolean ifSettle  = true;//是否结清标识
	private Boolean ifOverdue = false;//是否逾期标识
    LnsBillStatistics billStatistics;


    int	txseq = 0; //插还款明细需要加序号 否则唯一索引冲突
    Map<String,FabAmount> feeRpyInfo = new HashMap<>();

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		// 根据账号生成账单
		loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(getAcctNo(), ctx);
		if (outSerialNo != null)
			loanAgreement.getFundInvest().setOutSerialNo(getOutSerialNo());
		if (getBankSubject() != null)
			loanAgreement.getFundInvest().setFundChannel(getBankSubject());
		if (getRepayChannel() != null)
			loanAgreement.getFundInvest().setChannelType(getRepayChannel());
        //判断有没有费用
        //代偿的直接不校验
        if(loanAgreement.getFeeAgreement().isEmpty())
            throw new FabException("LNS037");
//        Boolean ifCA = true;
//        for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos())
//        {
//            if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsfeeinfo.getFeestat())){
                childBrc = loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getFeebrc();
//                break;
//                ifCA = false;
//            }
//        }
//        if (ifCA)
//            throw new FabException("LNS037");//没有未还的费用登记簿

		//房抵贷还款账户准备
		//幂等登记薄 幂等的话返回数据要从表里面取  现在是刚好借用了请求包数据

		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setSerialno(ctx.getSerialNo());
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode(ctx.getTranCode());
		lnsinterface.setBrc(childBrc);
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

		try{
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
		} catch(FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("serialno", ctx.getSerialNo());
				params.put("trancode", ctx.getTranCode());

				try {
					lnsinterface = DbAccessUtil.queryForObject("Lnsinterface.selectByUk", params,
							TblLnsinterface.class);
				} catch (FabSqlException f) {
					throw new FabException(f, "SPS103", "lnsinterface");
				}

				forfeetAmt = new FabAmount(lnsinterface.getTranamt());
				//3,4都是结清
				endFlag = VarChecker.asList("3","4").contains(lnsinterface.acctFlag2Endflag().trim())?"Y":"N";

				throw new FabException(e, TRAN.IDEMPOTENCY);
			} else
				throw new FabException(e, "SPS100", "lnsinterface");
		}

		//判断是否需要结清
		if(VarChecker.isEmpty(settleFlag)||settleFlag.equals("0")){
			ifSettle = false;
		}

		//试算账本
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement, tranctx.getTranDate(),tranctx,billStatistics);





		//还款金额
		forfeetAmt = new FabAmount();
		//根据产品代码区分 保费和 房抵贷
		if(LoanFeeUtils.isPremium(loanAgreement.getPrdId())) {
			//保费还款
			repay(lnsBillStatistics);

		}else {
			//房抵贷还款
			houseRepay(lnsBillStatistics);
		}

		//更新幂等表
		Map<String, Object> fddmap = new HashMap<>();

		fddmap.put("tranamt", forfeetAmt.getVal());
		fddmap.put("acctflag", TblLnsinterface.endflag2AcctFlag2("Y".equals(endFlag)?"3":"1",ifOverdue ));
		fddmap.put("serialno", ctx.getSerialNo());
		fddmap.put("trancode", ctx.getTranCode());
		//reserv3 存固定担保费还款金额
		fddmap.put("reserv3", reserv3);
		//reserv4 存退保金额
		fddmap.put("reserv4", reserv4);

		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsinterface_fdd", fddmap);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsinterface");
		}
		//1是自费
       LoanRpyInfoUtil.saveRpyInfos(ctx, 0,feeRpyInfo ,acctNo ,repayAcctNo , "1");
	}

	//筛选费用账本
    private List<LnsBill> feeFilter( List<LnsBill> listBill) {
	    List<LnsBill> feeBills = new ArrayList<>();
        for(LnsBill lnsBill:listBill){
            if(LoanFeeUtils.isFeeType(lnsBill.getBillType())){
                feeBills.add(lnsBill);
                //判断是否逾期
                if(!ifOverdue
                        && lnsBill.getSettleFlag().equals(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING)
                        &&(!lnsBill.getBillStatus().equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL)
                        || CalendarUtil.after(tranctx.getTranDate(), lnsBill.getCurendDate() ))){
                    ifOverdue = true;
                }
            }
        }
        return feeBills;
    }
    // 排序规则 按期数排序，若期数相同，一次性在前
    static class LnsBillCompator implements Comparator<LnsBill> {
        @Override
        public int compare(LnsBill o1, LnsBill o2) {
            if(o1.getPeriod().equals(o2.getPeriod())){
                return o2.getLnsfeeinfo().getRepayway().compareTo(o1.getLnsfeeinfo().getRepayway());
            }
            return o1.getPeriod() - o2.getPeriod();
        }
    }
    /**
	 * 保费还款
	 * @throws FabException  异常
	 */
	private void repay(LnsBillStatistics lnsBillStatistics) throws FabException {
		TranCtx ctx = getTranctx();
		if(VarChecker.isEmpty(memo))
			throw new FabException("LNS055","memo");
		if(!VarChecker.asList(BRIEFCODE.BFHK,BRIEFCODE.YQTB,BRIEFCODE.TQJQ).contains(memo))
			throw new FabException("LNS169","摘要信息","memo");

		if(VarChecker.asList(BRIEFCODE.BFHK,BRIEFCODE.TQJQ).contains(memo) && "B".equals(repayChannel))
			throw new FabException("LNS166",memo,"repayChannel不能为B");
		if(BRIEFCODE.YQTB.equals(memo) && !"B".equals(repayChannel))
			throw new FabException("LNS166",memo,"repayChannel应为B");

        //校验是否结清
        int repaysize = 0;
        int settlesize =0;
        //统计总的未还费用账本数
        List<LnsBill> hisbills = new ArrayList<>();
        for(LnsBill lnsbill :feeFilter(lnsBillStatistics.getHisBillList())) {
            if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsbill.getSettleFlag()))
                hisbills.add(lnsbill);
        }
        List<LnsBill> futureBills = feeFilter(lnsBillStatistics.getBillInfoList());
        futureBills.addAll(feeFilter(lnsBillStatistics.getFutureBillInfoList()));
        int totalsize = hisbills.size()+futureBills.size();
        //B.任性贷退保
		if("B".equals(repayChannel)) {
		    //退保
            if(VarChecker.isEmpty(customID)){
                throw  new FabException("LNS055","费用公司商户号" );
            }

            if(!ifOverdue)
                throw new FabException("LNS171");
            for(LnsBill lnsBill:futureBills) {
                LoanFeeUtils.settleFee(ctx, lnsBill,loanAgreement);
            }
            Map<String, Object> updateParam = new HashMap<>();
            FabAmount rpytmp = new FabAmount(repayAmt.getVal());
            hisbills.addAll(futureBills);
            //所有账本
            for(LnsBill lnsBill:hisbills) {
                if (filterYQJQ(ctx, lnsBill)) continue;
                settlesize++;
                if (!rpytmp.isPositive())
                    break;

                if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway()))
                {
                    //一次性的不能重复退保
                    Map<String,Object> param = new HashMap<>();
                    param.put("acctno", acctNo);
                    param.put("brc", ctx.getBrc());
                    param.put("key", ConstantDeclare.KEYNAME.CI);
                    TblLnsinterfaceex interfaceex;
                    try {
                        interfaceex = DbAccessUtil.queryForObject("AccountingMode.query_lnsinterfaceex", param, TblLnsinterfaceex.class);
                    } catch (FabSqlException e) {
                        throw new FabException(e, "SPS103", "query_lnsinterfaceex");
                    }
                    if(null!=interfaceex)
                        throw  new FabException("LNS172");

                    //计算总期数
                    Integer periodNum = LoanFeeUtils.countPeriod(loanAgreement, lnsBillStatistics,ctx);//总期数

                    if( rpytmp.sub(BigDecimal.valueOf(lnsBill.getBillAmt().getVal()).multiply(new BigDecimal(periodNum-1))
                            .divide(BigDecimal.valueOf(periodNum), 2, BigDecimal.ROUND_HALF_UP).doubleValue()).isZero())
                        repaysize++;
                    //更新账本表
                    Map<String,Object> updateBill = new HashMap<>();
                    updateBill.put("trandate", lnsBill.getTranDate());
                    updateBill.put("serseqno",lnsBill.getSerSeqno() );
                    updateBill.put("txseq",lnsBill.getTxSeq() );
                    updateBill.put("amt", lnsBill.getBillAmt().sub(rpytmp).getVal());
                    try {
                       DbAccessUtil.execute("Lnsfeeinfo.updatebillAmt", updateBill);
                    } catch (FabSqlException e) {
                        throw new FabException(e, "SPS102", "updatebillAmt");
                    }

                    LoanFeeUtils.accountsub(  sub,ctx, lnsBill, rpytmp.getVal(),loanAgreement,repayAcctNo,memo);
                    rpytmp.selfSub(repayAmt);
                    //退保事件
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),  ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                            new FabCurrency(),childBrc);
                    eventProvider.createEvent(ConstantDeclare.EVENT.CINSURANCE, repayAmt, lnsAcctInfo,null, loanAgreement.getFundInvest(),
                            memo, ctx ,customID,childBrc);
                }else {
                    Double minAmt = LoanFeeUtils.repaysql(rpytmp,updateParam,lnsBill,ctx );
                    LoanFeeUtils.accountsub(  sub,ctx, lnsBill, minAmt,loanAgreement,repayAcctNo,memo);
                    rpytmp.selfSub(minAmt);
                    //退保事件
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),  ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                            new FabCurrency(),childBrc);
                    eventProvider.createEvent(ConstantDeclare.EVENT.CINSURANCE, new FabAmount(minAmt), lnsAcctInfo,null, loanAgreement.getFundInvest(),
                            memo, ctx ,customID,childBrc);
                    if (lnsBill.getBillBal().sub(minAmt).isZero())
                        repaysize++;
                }
            }

//            for(LnsBill lnsBill:futureBills) {
//                if (filterYQJQ(ctx, lnsBill)) continue;
//
//                settlesize++;
//                if (!rpytmp.isPositive())
//                    break;
//                //账本插表
//                Double minAmt = LoanFeeUtils.repaysql(rpytmp,updateParam,lnsBill,ctx );
//                LoanFeeUtils.accountsub(  sub,ctx, lnsBill, minAmt,loanAgreement,repayAcctNo);
//                rpytmp.selfSub(minAmt);
//                if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway()))
//                {
//                    //计算总期数
//                    Integer periodNum = LoanFeeUtils.countPeriod(loanAgreement, lnsBillStatistics,ctx);//总期数
//                    //还款必结清 所以是退保
//                    if(!lnsBill.getBillAmt().sub(lnsBill.getBillBal()).isZero())
//                        throw  new FabException("LNS172");
//
//                    if( new FabAmount(minAmt).sub(BigDecimal.valueOf(lnsBill.getBillAmt().getVal()).multiply(new BigDecimal(periodNum-1))
//                        .divide(BigDecimal.valueOf(periodNum), 2, BigDecimal.ROUND_HALF_UP).doubleValue()).isZero())
//                        repaysize++;
//                }else if(lnsBill.getBillBal().sub(minAmt).isZero())
//                    repaysize++;
//            }

            if(settlesize!=repaysize)
                throw new FabException("LNS205");

            if(rpytmp.isPositive())
                throw new FabException("LNS058",repayAmt.toString(),repayAmt.sub(rpytmp).toString());





            reserv4 =  repayAmt.toString();
            //保存 退保的费用公司商户号
            Map<String,Object> map = new HashMap<>();
            map.put("customID", customID);
            map.put("returnFee", repayAmt.toString());
            map.put("memo",memo );
            AccountingModeChange.saveInterfaceEx(ctx, acctNo, ConstantDeclare.KEYNAME.CI, "退保", JsonTransfer.ToJson(map));
            setForfeetAmt(repayAmt);
        }
        //还款
		else {
            //提前结清  settle要为true
            if(BRIEFCODE.TQJQ.equals(memo)&&!ifSettle)
                throw new FabException("LNS166",memo,"提前结清标识应为结清");
            //提前结清跨日的情况 ,才可以跨期减免
            if(exceedReduce!=null&&exceedReduce.isPositive()){
                if(!BRIEFCODE.TQJQ.equals(memo))
                    throw new FabException("LNS166",BRIEFCODE.TQJQ,"才可以跨期减免");
            }

            //还款顺序排序
            hisbills.sort(new LnsBillCompator());
            settlesize = hisbills.size();
            //临时变量
            Map<String, Object> upbillmap = new HashMap<>();

            //校验固定担保费的一次性结清
            FabAmount oneTimeIsfe = new FabAmount();
            for( LnsBill lnsBill :hisbills){

                FabAmount minAmt = new FabAmount(repayFee(oneTimeIsfe, upbillmap, lnsBill,BRIEFCODE.IFHK));
                //还费用
                if(lnsBill.getBillBal().sub(minAmt).isZero())
                    repaysize++;

                //不是自动减免
                if(isExceedReduce(lnsBill)
                 ) {
                    //还款事件
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                            new FabCurrency(),childBrc);
                    eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, minAmt, lnsAcctInfo, null, loanAgreement.getFundInvest(),
                            memo, ctx, childBrc,platformId);
                    if (!repayAmt.isPositive())
                        break;
                }



            }

            //未来期
            for(LnsBill lnsBill:futureBills) {


                //过滤
                if (RepayPossibility(ctx, ConstantDeclare.EARLYSETTLRFLAG.CURRCHARGE, lnsBill)){
                    continue;
                }
                settlesize++;
                if(isExceedReduce(lnsBill)
                        ) {
                    if (!repayAmt.isPositive()) {

                        break;
                    }
                }
                //账本插表
                LoanFeeUtils.settleFee(ctx, lnsBill,loanAgreement);
                FabAmount minAmt = new FabAmount(repayFee(oneTimeIsfe, upbillmap, lnsBill,BRIEFCODE.IFHK));
                //还费用
                if(lnsBill.getBillBal().sub(minAmt).isZero())
                    repaysize++;
                //不是自动减免
                if(isExceedReduce(lnsBill)
                        ) {
                    //保费还款事件
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                            new FabCurrency(),childBrc);
                    eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, minAmt, lnsAcctInfo, null, loanAgreement.getFundInvest(),
                            memo, ctx, childBrc,platformId);
                }
            }
            if(oneTimeIsfe.isPositive() ) {
                if(repaysize!=settlesize)
                    throw new FabException("LNS170","开户时flag为1");
            }
            //还款金额大于已还款金额
            if(repayAmt.isPositive()){
                throw new FabException("LNS058",repayAmt.add(forfeetAmt).toString() ,forfeetAmt.toString());
            }



            //提前结清   一次性收取的 不需要退保
            if(BRIEFCODE.TQJQ.equals(memo) ){


                if(repaysize!=settlesize)
                    throw new FabException("LNS170","settleFlag为1");

                //退保 的金额
                FabAmount cancelAmt = new FabAmount();

                for(LnsBill lnsBill:lnsBillStatistics.getFutureBillInfoList()) {
                    if (filterTQJQ(ctx, lnsBill)) continue;

                    cancelAmt.selfAdd(lnsBill.getBillBal());

                    //账本插表
                    LoanFeeUtils.settleFee(ctx, lnsBill,loanAgreement);
                    Double minAmt = LoanFeeUtils.repaysql(lnsBill.getBillBal(),upbillmap,lnsBill,ctx );
                    LoanFeeUtils.accountsub(  sub,ctx, lnsBill, minAmt,loanAgreement,repayAcctNo,memo);
                    //退保事件
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),  ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                            new FabCurrency(),childBrc);
                    FundInvest fundInvest = new FundInvest(loanAgreement.getFundInvest().getInvestee(),loanAgreement.getFundInvest().getInvestMode()
                            ,"B", loanAgreement.getFundInvest().getFundChannel(),loanAgreement.getFundInvest().getOutSerialNo() );
                    eventProvider.createEvent(ConstantDeclare.EVENT.CINSURANCE, new FabAmount(minAmt), lnsAcctInfo,null, fundInvest,
                            memo, ctx ,customID,childBrc);
                }

                reserv4 = cancelAmt.toString();
                if(exceedReduce!=null&&exceedReduce.isPositive()) {
                    adjustFee = exceedReduce;
                    cancelAmt.selfAdd(exceedReduce);
                }
                if(cancelAmt.isPositive()){

                    //保存 退保的费用公司商户号
                    Map<String,Object> map = new HashMap<>();
                    map.put("customID", customID);
                    map.put("returnFee",cancelAmt.toString());

                    AccountingModeChange.saveInterfaceEx(ctx, acctNo, ConstantDeclare.KEYNAME.CI, "退保", JsonTransfer.ToJson(map));

                }

            }
        }
        //未结清管理费的条数 = 此次结清的条数  则结清  TQJQ也是结清
        if((repaysize==totalsize&& !BRIEFCODE.YQTB.equals(memo))||BRIEFCODE.TQJQ.equals(memo)) {
            LoanFeeUtils.updateAllCA(ctx.getBrc(),acctNo);

            setEndFlag("Y");
        }else {
			if(ifSettle){
                throw new FabException("LNS170","settleFlag为1");
            }
            setEndFlag("N");
        }
	}

    /**
     *  true 不是自动减免  false 是自动减免
     * @param lnsBill
     * @return
     */
    private boolean isExceedReduce(LnsBill lnsBill) {
        return exceedReduce==null||!exceedReduce.isPositive() ||
                //判断当前期
                !CalendarUtil.after(tranctx.getTranDate(), lnsBill.getStartDate())
                ||!CalendarUtil.beforeAlsoEqual(tranctx.getTranDate(),lnsBill.getEndDate());
    }


    //提前结清退保筛选
    private boolean filterTQJQ(TranCtx ctx, LnsBill lnsBill) {
        //费用没有未来期全收的
        if (!LoanFeeUtils.isFeeType(lnsBill.getBillType())) return true;
        // 提前结清 一次性的不退保
        if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway())) return true;
        //不是未来期的
        return (CalendarUtil.after(ctx.getTranDate(),lnsBill.getStartDate())) ;
    }
    //逾期退保 账本筛选
    private boolean filterYQJQ(TranCtx ctx, LnsBill lnsBill) {
        //费用没有未来期全收的
        if (!LoanFeeUtils.isFeeType(lnsBill.getBillType())) return true;
        // 逾期退保 一次性的退保
        if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway())) return false;
        //不是当前期和未来期的
        return !CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), lnsBill.getCurendDate()) ;
    }







	/**
	 * 房抵贷还款
	 * @throws FabException  异常
	 */
	private void houseRepay(LnsBillStatistics lnsBillStatistics) throws FabException {
		TranCtx ctx = getTranctx();

		//校验不能超过上次还款日
        List<LnsBill> hisbills = new ArrayList<>();
        //上次还款日
        for(LnsBill lnsBill : feeFilter(lnsBillStatistics.getHisBillList())){

            if(!VarChecker.isEmpty(lnsBill.getLastDate())
                    &&CalendarUtil.before(termretdate, lnsBill.getLastDate()))
            {
                termretdate = lnsBill.getLastDate();
            }
            if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag()))
                hisbills.add(lnsBill);
        }
        //还款顺序排序
        hisbills.sort(new LnsBillCompator());
		//提前结清收费模式earlysettleFlag（1.提前结清费用收到当期2.提前结清费用全收）
        //存表的时候存的是一样的
        String earlySettle = loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getAdvancesettle();
		if(!VarChecker.isEmpty(realDate)){
			//实际还款日，需要传结清标志
			if(!"1".equals(settleFlag)){
				realDate = ctx.getTranDate();
			}
			//实际扣款日不能大于当天
			if(CalendarUtil.after(realDate, ctx.getTranDate())){
				throw new FabException("LNS200");
			}
			//实际划款日不能小于上次还款日
			if(CalendarUtil.before(realDate,termretdate ))
				throw new FabException("LNS203");

		} else{
			realDate = ctx.getTranDate();
		}

        childBrc=loanAgreement.getFeeAgreement().getLnsfeeinfos().get(0).getFeebrc();

        FabAmount gddbAmt = new FabAmount();
        //临时变量
        Map<String, Object> upbillmap = new HashMap<>();

        int repaysize = 0;
        int settlesize = hisbills.size();

        //统计总的未还费用账本数
        List<LnsBill> futureBills = feeFilter(lnsBillStatistics.getBillInfoList());
        futureBills.addAll(feeFilter(lnsBillStatistics.getFutureBillInfoList()));
        int totalsize = settlesize+futureBills.size();

        for( LnsBill lnsBill :hisbills){
            FabAmount minAmt = new FabAmount(repayFee(gddbAmt, upbillmap, lnsBill,repayBrief()));
            //还费用
            if(lnsBill.getBillBal().sub(minAmt).isZero())
                repaysize++;
            if(!gddbAmt.isPositive()){
                //实际还款日
                if(realDate.equals(tranctx.getTranDate())
                        ||!CalendarUtil.afterAlsoEqual(lnsBill.getStartDate(), realDate)
                        ||isFullCharge(lnsBill.getLnsfeeinfo().getAdvancesettle())){
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                            new FabCurrency(),childBrc);
                    eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, minAmt, lnsAcctInfo, null,
                            loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FYHK, ctx, childBrc,platformId);
                }
            }else
                gddbAmt.selfSub(minAmt);

            //为了实际还款日 还款金额为0 要通过 然后去减免这一期的金额
            if (!(!realDate.equals(tranctx.getTranDate())
                    &&CalendarUtil.afterAlsoEqual(lnsBill.getStartDate(), realDate))
                    &&!repayAmt.isPositive())
                break;
        }

       /* //当前期的
        for(LnsBill lnsBill:lnsBillStatistics.getBillInfoList()) {
            if (!LoanFeeUtils.isFeeType(lnsBill.getBillType())) {
                continue;
            }
            settlesize++;
            //为了实际还款日 还款金额为0 要通过 然后去减免这一期的金额
            if (!(!realDate.equals(tranctx.getTranDate())
                    &&CalendarUtil.afterAlsoEqual(lnsBill.getStartDate(), realDate))
                    &&!repayAmt.isPositive())
                break;
            //账本插表
            LoanFeeUtils.settleFee(ctx, lnsBill,loanAgreement);
            if(repayFee(gddbAmt, upbillmap, lnsBill))
                repaysize++;


        }*/
        //当前期的
        for(LnsBill lnsBill:futureBills) {
            if (RepayPossibility(ctx, earlySettle, lnsBill)) continue;
            settlesize++;
            //为了实际还款日 还款金额为0 要通过 然后去减免这一期的金额
            if (!(!realDate.equals(tranctx.getTranDate())
                    &&CalendarUtil.afterAlsoEqual(lnsBill.getStartDate(), realDate))
            &&!repayAmt.isPositive())
                break;
            //账本插表
            LoanFeeUtils.settleFee(ctx, lnsBill,loanAgreement);
            FabAmount minAmt = new FabAmount(repayFee(gddbAmt, upbillmap, lnsBill,repayBrief()));
            //还费用
            if(lnsBill.getBillBal().sub(minAmt).isZero())
                repaysize++;
            //
            if(!gddbAmt.isPositive()){
                    //实际还款日
                    if(realDate.equals(tranctx.getTranDate())
                    ||!CalendarUtil.afterAlsoEqual(lnsBill.getStartDate(), realDate)
                            ||isFullCharge(lnsBill.getLnsfeeinfo().getAdvancesettle())){
                    LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                            new FabCurrency(),childBrc);
                    eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, minAmt, lnsAcctInfo, null,
                            loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.FYHK, ctx, childBrc,platformId);
                }
            }else
                gddbAmt.selfSub(minAmt);
        }


		//结清时 金额 必须相等   普通还款时不能大于 要还的金额
		if(ifSettle && settlesize!=repaysize){
			throw new FabException("LNS202");
		}

		if(repayAmt.isPositive()){
            throw new FabException("LNS058",repayAmt.add(forfeetAmt).toString() ,forfeetAmt.toString());
        }


		//结清标志
		if(ifSettle ||totalsize == repaysize)//count==feeManageregList.size()
		{
            LoanFeeUtils.updateAllCA(ctx.getBrc(),acctNo);

            setEndFlag("Y");

		}else{
			setEndFlag("N");
		}


	}

	private void eventGDHK(LnsBill lnsBill,FabAmount gddbAmt) throws  FabException{
        if(gddbAmt.isPositive()){

            //固定担保费还款金额
            reserv3 = gddbAmt.toString();

            //固定担保费 还款事件
            LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                    new FabCurrency(),childBrc);

//            LoggerUtil.debug("LNSFEEPYMT-:" + gddbAmt.getVal());
            //2412624,2412626任性贷搭融担费，假设放款机构是51030000.子机构是51340000，那么，
            //事件的reserve3传R5103（截取51030000的前四位加R前缀）
            String reserv = "";
            if(VarChecker.asList("2412624","2412626").contains(loanAgreement.getPrdId()))
            {
                reserv = "R"+tranctx.getBrc().substring(0,4 );
            }
            eventProvider.createEvent(ConstantDeclare.EVENT.LNSFEEPYMT, new FabAmount(gddbAmt.getVal()), lnsAcctInfo, null,
                    loanAgreement.getFundInvest(), ConstantDeclare.BRIEFCODE.GDHK, tranctx,childBrc,platformId,"",reserv);
            //更新固定担保费
            updateBasicExFee(tranctx, gddbAmt);
        }
    }

    private void updateBasicExFee(TranCtx ctx, FabAmount gddbAmt) throws FabException {
        //查询主文件拓展表
        Map<String, Object> queryparam = new HashMap<>();
        queryparam.put("acctno", acctNo);
        queryparam.put("openbrc", ctx.getBrc());
        queryparam.put("key", ConstantDeclare.KEYNAME.GDDB);

        TblLnsbasicinfoex lnsbasicinfoex;

        try {
            lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", queryparam, TblLnsbasicinfoex.class);
        } catch (FabSqlException e) {
            throw new FabException("SPS100", "lnsbasicinfoex", e);
        }
        JSONObject tunneldata = JSONObject.parseObject(lnsbasicinfoex.getTunneldata());
        tunneldata.put("feeamt",gddbAmt.add( tunneldata.getDouble("feeamt")).getVal());
        //登记 固定担保费的已还
        queryparam.put("tunneldata", tunneldata.toString());
        try {
            DbAccessUtil.execute("Lnsbasicinfoex.updateTunneldata", queryparam);
        } catch (FabSqlException e) {
            throw new FabException("SPS100", "lnsbasicinfoex", e);
        }
    }




    //未来期的那些账本不能还款
    private boolean RepayPossibility(TranCtx ctx, String earlySettle, LnsBill lnsBill) {
        if (!LoanFeeUtils.isFeeType(lnsBill.getBillType())) {
            return true;
        }
        //不是全收的不是一次性的未来期的
        if(!isFullCharge(earlySettle)){
            return (!ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway())
                    &&CalendarUtil.beforeAlsoEqual(ctx.getTranDate(),lnsBill.getStartDate()));
        }
        return false;
    }

    //是否全收
    private boolean isFullCharge(String earlySettle) {
	    //起息日还款 不考虑全收标志
	    if(!VarChecker.isEmpty(realDate)){
	        if(realDate.equals(loanAgreement.getContract().getStartIntDate()))
	            return false;
        }else if(tranctx.getTranDate().equals(loanAgreement.getContract().getStartIntDate())){
            return false;
        }
        return ConstantDeclare.EARLYSETTLRFLAG.FULLCHARGE.equals(earlySettle);
    }


    private Double repayFee(FabAmount gddbAmt, Map<String, Object> upbillmap, LnsBill lnsBill, String briefCode) throws FabException {
        Double minAmt;
        //一次性费用
        if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(lnsBill.getLnsfeeinfo().getRepayway())){
            //还款
            minAmt = LoanFeeUtils.repaysql(repayAmt,upbillmap, lnsBill,tranctx);


            LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minAmt,loanAgreement,repayAcctNo,briefCode);

            //一次性费用结清了
            if(lnsBill.getBillBal().sub(minAmt).isZero()){
                LoanFeeUtils.updateCA(lnsBill.getLnsfeeinfo());
            }
            gddbAmt.selfAdd(minAmt);
            repayAmt.selfSub(minAmt);
            forfeetAmt.selfAdd(minAmt);
            //不是保费，抛固定担保事件
            if(!LoanFeeUtils.isPremium(loanAgreement.getPrdId())) {
                eventGDHK(lnsBill, gddbAmt);
            }
            LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, lnsBill.getBillType(),lnsBill.getBillStatus(),new FabAmount(minAmt));
        }else
            //保费的自动减免
         if(ConstantDeclare.BILLTYPE.BILLTYPE_ISFE.equals(lnsBill.getBillType())
                 &&exceedReduce!=null&&exceedReduce.isPositive() &&
                //判断当前期
        CalendarUtil.after(tranctx.getTranDate(), lnsBill.getStartDate())
        && CalendarUtil.beforeAlsoEqual(tranctx.getTranDate(),lnsBill.getEndDate())
        ){
             if(!exceedReduce.sub(lnsBill.getBillBal()).isZero())
                 throw new FabException("LNS180");
             minAmt = LoanFeeUtils.repaysql(lnsBill.getBillBal(),upbillmap, lnsBill,tranctx);
             LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minAmt,loanAgreement,repayAcctNo,memo);
             LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(),  ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                     new FabCurrency(),childBrc);
             FundInvest fundInvest = new FundInvest(loanAgreement.getFundInvest().getInvestee(),loanAgreement.getFundInvest().getInvestMode()
                     ,"B", loanAgreement.getFundInvest().getFundChannel(),loanAgreement.getFundInvest().getOutSerialNo() );
             eventProvider.createEvent(ConstantDeclare.EVENT.CINSURANCE, new FabAmount(minAmt), lnsAcctInfo,null, fundInvest,
                     memo, tranctx ,customID,childBrc);
        }else
            //防止保费传实际还款日
             //实际还款日还款
             if (!ConstantDeclare.BILLTYPE.BILLTYPE_ISFE.equals(lnsBill.getBillType())
                     &&lnsBill.getLnsfeeinfo().getAdvancesettle().equals(ConstantDeclare.EARLYSETTLRFLAG.CURRCHARGE)
                     && !realDate.equals(tranctx.getTranDate())
                     &&CalendarUtil.afterAlsoEqual(lnsBill.getStartDate(), realDate)){
                 //对于realdate未来期算成自动减免 实际还款日不能跨期
                 if(adjustFee.isPositive())
                     throw new FabException("LNS201");
                 minAmt = LoanFeeUtils.repaysql(lnsBill.getBillBal(),upbillmap, lnsBill,tranctx);
                 LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minAmt,loanAgreement,repayAcctNo,reduceBrief());
                 adjustFee.selfAdd(minAmt);
                 Map<String,Object> json = new HashMap<>();
                 json.put("adjustFee", adjustFee.toString());
                 json.put("realDate", realDate);
                 //RMFE风险管理费
                 //SQFE担保费
                 if("51340000".equals(childBrc)) {
                     json.put("adjustType", "SQFE");
                 }
                 else {
                     json.put("adjustType", "RMFE");
                 }
                 LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getAcctNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_FEEA, lnsBill.getBillStatus(),
                         new FabCurrency(),childBrc);
                 AccountingModeChange.saveInterfaceEx(tranctx, acctNo ,  ConstantDeclare.KEYNAME.ZDJF, "自动减费",JsonTransfer.ToJson(json) );

                 eventProvider.createEvent(ConstantDeclare.EVENT.FEEREDUCEM, adjustFee, lnsAcctInfo, null,
                         loanAgreement.getFundInvest(), reduceBrief(), tranctx,childBrc);

        } else{
             //还款
            minAmt = LoanFeeUtils.repaysql(repayAmt,upbillmap, lnsBill,tranctx);
            LoanFeeUtils.accountsub(  sub,tranctx, lnsBill, minAmt,loanAgreement,repayAcctNo,briefCode);
            repayAmt.selfSub(minAmt);
            forfeetAmt.selfAdd(minAmt);
                 LoanRpyInfoUtil.addRpyInfo(feeRpyInfo, lnsBill.getBillType(),lnsBill.getBillStatus(),new FabAmount(minAmt));
        }


        return minAmt;
    }

    //实际还款日 摘要码
    private String reduceBrief() {
        if("51340000".equals(childBrc)) {
            return BRIEFCODE.SFJM;
        }
        else {
            return BRIEFCODE.RFJM;
        }
    }
    //费用 担保费还款 摘要码
    private String repayBrief() {
        if("51230004".equals(childBrc)) {
            return BRIEFCODE.RFHK;
        }
        else {
            return BRIEFCODE.SFHK;
        }
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
	 *
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
	 * @return the forfeetAmt
	 */
	public FabAmount getForfeetAmt() {
		return forfeetAmt;
	}



	/**
	 * @param forfeetAmt the forfeetAmt to set
	 */
	public void setForfeetAmt(FabAmount forfeetAmt) {
		this.forfeetAmt = forfeetAmt;
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
	 *
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
	 *
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
	 *
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
	 *
	 * @return the bankSubject
	 */
	public String getBankSubject() {
		return bankSubject;
	}



	/**
	 * @param bankSubject the bankSubject to set
	 */
	public void setBankSubject(String bankSubject) {
		this.bankSubject = bankSubject;
	}



	/**
	 *
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
	 *
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

    public String getCustomID() {
        return customID;
    }

    public void setCustomID(String customID) {
        this.customID = customID;
    }

	/**
	 * Gets the value of exceedReduce.
	 *
	 * @return the value of exceedReduce
	 */
	public FabAmount getExceedReduce() {
		return exceedReduce;
	}

	/**
	 * Sets the exceedReduce.
	 *
	 * @param exceedReduce exceedReduce
	 */
	public void setExceedReduce(FabAmount exceedReduce) {
		this.exceedReduce = exceedReduce;

	}

	/**
	 * Gets the value of realDate.
	 *
	 * @return the value of realDate
	 */
	public String getRealDate() {
		return realDate;
	}

	/**
	 * Sets the realDate.
	 *
	 * @param realDate realDate
	 */
	public void setRealDate(String realDate) {
		this.realDate = realDate;

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
	 * @return the platformId
	 */
	public String getPlatformId() {
		return platformId;
	}

	/**
	 * @param platformId the platformId to set
	 */
	public void setPlatformId(String platformId) {
		this.platformId = platformId;
	}

	/**
	 * @return the reserv3
	 */
	public String getReserv3() {
		return reserv3;
	}

	/**
	 * @param reserv3 the reserv3 to set
	 */
	public void setReserv3(String reserv3) {
		this.reserv3 = reserv3;
	}

	/**
	 * @return the reserv4
	 */
	public String getReserv4() {
		return reserv4;
	}

	/**
	 * @param reserv4 the reserv4 to set
	 */
	public void setReserv4(String reserv4) {
		this.reserv4 = reserv4;
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

	/**
	 * @return the termretdate
	 */
	public String getTermretdate() {
		return termretdate;
	}

	/**
	 * @param termretdate the termretdate to set
	 */
	public void setTermretdate(String termretdate) {
		this.termretdate = termretdate;
	}

	/**
	 * @return the ifSettle
	 */
	public Boolean getIfSettle() {
		return ifSettle;
	}

	/**
	 * @param ifSettle the ifSettle to set
	 */
	public void setIfSettle(Boolean ifSettle) {
		this.ifSettle = ifSettle;
	}

	/**
	 * @return the ifOverdue
	 */
	public Boolean getIfOverdue() {
		return ifOverdue;
	}

	/**
	 * @param ifOverdue the ifOverdue to set
	 */
	public void setIfOverdue(Boolean ifOverdue) {
		this.ifOverdue = ifOverdue;
	}

	/**
	 * @return the txseq
	 */
	public int getTxseq() {
		return txseq;
	}

	/**
	 * @param txseq the txseq to set
	 */
	public void setTxseq(int txseq) {
		this.txseq = txseq;
	}

	/**
	 * @return the feeRpyInfo
	 */
	public Map<String, FabAmount> getFeeRpyInfo() {
		return feeRpyInfo;
	}

	/**
	 * @param feeRpyInfo the feeRpyInfo to set
	 */
	public void setFeeRpyInfo(Map<String, FabAmount> feeRpyInfo) {
		this.feeRpyInfo = feeRpyInfo;
	}

    /**
     * Gets the value of billStatistics.
     *
     * @return the value of billStatistics
     */
    public LnsBillStatistics getBillStatistics() {
        return billStatistics;
    }

    /**
     * Sets the billStatistics.
     *
     * @param billStatistics billStatistics
     */
    public void setBillStatistics(LnsBillStatistics billStatistics) {
        this.billStatistics = billStatistics;

    }

	/**
	 * @return the damagesAmt
	 */
	public FabAmount getDamagesAmt() {
		return damagesAmt;
	}

	/**
	 * @param damagesAmt the damagesAmt to set
	 */
	public void setDamagesAmt(FabAmount damagesAmt) {
		this.damagesAmt = damagesAmt;
	}
}

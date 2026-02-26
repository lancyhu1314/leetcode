package com.suning.fab.loan.utils;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.VarChecker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：主要是入参校验
 *
 * @Author 18049705 MYP
 * @Date Created in 14:58 2020/2/26
 * @see
 */
public class FaChecker {


    //展期延期还款 入参校验
    public static void delayRpyInputCheck(String intervalTime,Integer delayTime) throws FabException {
        //延期还款的服务类型，展期结束日不输，延期还款间隔和延期还款时间必传，
        // 且目前支支持间隔为M-按月，延期还款时间（1-1个月，2-2个月，3-3个月,等按整月的类型）
        if(VarChecker.isEmpty(intervalTime))
            throw new FabException("LNS055","intervalTime");
        if(!"M".equals(intervalTime))
            throw new FabException("LNS169","intervalTime",intervalTime);
        if(VarChecker.isEmpty(delayTime))
            throw new FabException("LNS055","delayTime");
        if(delayTime<=0)
            throw new FabException("LNS169","delayTime",delayTime);
    }
    //展期延期还款 规则校验
    public static void delayRpyRuleCheck(List<TblLnsbill> tblLnsBills, LoanAgreement loanAgreement, TranCtx ctx) throws FabException {

        //贷款整笔合同到期日当天不允许延期还款
        if(loanAgreement.getContract().getContractEndDate().equals(ctx.getTranDate()))
            throw new FabException("LNS209","贷款整笔合同到期日");
        //起息日报错
        if(loanAgreement.getContract().getStartIntDate().equals(ctx.getTranDate()))
            throw new FabException("LNS209","贷款起息日");
        //延期还款的服务类型，非标自定义还款方式不支持，报错。
        if(VarChecker.asList(ConstantDeclare.REPAYWAY.REPAYWAY_XXHB,
                ConstantDeclare.REPAYWAY.REPAYWAY_XBHX,
                ConstantDeclare.REPAYWAY.REPAYWAY_YBYX,
                ConstantDeclare.REPAYWAY.REPAYWAY_ZDY).contains(loanAgreement.getWithdrawAgreement().getRepayWay()))
            throw new FabException("LNS207","还款方式"+loanAgreement.getWithdrawAgreement().getRepayWay());
        //延期展期
        if(!VarChecker.isEmpty(ctx.getRequestDict("serviceType"))
                &&!"1".equals(ctx.getRequestDict("serviceType").toString())
                &&ConstantDeclare.REPAYWAY.REPAYWAY_JYHK.equals(loanAgreement.getWithdrawAgreement().getRepayWay())){
            throw new FabException("LNS207","季还本月还息延期展期");
        }
        //延期展期
        if(!VarChecker.isEmpty(ctx.getRequestDict("serviceType"))
                &&!Arrays.asList("1","2","4").contains(ctx.getRequestDict("serviceType").toString())){
            throw new FabException("LNS169","服务类型",ctx.getRequestDict("serviceType").toString());
        }

        //贷款已经有未结清逾期账本、贷款已经有处于宽限期的未结清账本，不允许展期试算
        for(TblLnsbill lnsbill:tblLnsBills){
            if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsbill.getSettleflag().trim())
                    //兼容提前还款部分利息
                    && CalendarUtil.after(ctx.getTranDate(),lnsbill.getCurenddate().trim()))
                throw new FabException("LNS208");

        }

        //保费的限制
        if(LoanFeeUtils.isPremium(loanAgreement.getPrdId())){
            throw new FabException("LNS190",loanAgreement.getPrdId());
        }

        //核销的贷款不展期
        Map<String, Object> param = new HashMap<>();
        param.put("acctno", loanAgreement.getContract().getReceiptNo());
        param.put("openbrc", ctx.getBrc());
        TblLnsbasicinfo lnsbasicinfo = LoanDbUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat().trim()))
            throw new FabException("LNS207","已结清借据");
        if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
                ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lnsbasicinfo.getLoanstat().trim()))
            throw new FabException("LNS207","已核销/非应计借据");
    }



    /**
     *  flag入参校验
     * @throws FabException  //校验
     */
    public static void checkFeeInput(String prdCode, String flag, FabRate onetimeRate, FabRate	 yearFeeRate) throws  FabException{
        //保费
        //产品代码为2512617  2512619，即为保费
        if(VarChecker.asList("2512617","2512619").contains(prdCode)){
            //flag为1.一次性收取  yearfeeRate 为0  oneTimeRate 不为0
            if("1".equals(flag)){
                if(!LoanFeeUtils.rateIsPositive(onetimeRate)||LoanFeeUtils.rateIsPositive(yearFeeRate))
                    throw new FabException("LNS168",flag);
            }
            //2.分期收取(不传也默认2)
            else if (VarChecker.isEmpty(flag)||"2".equals(flag)){
                if(LoanFeeUtils.rateIsPositive(onetimeRate)||!LoanFeeUtils.rateIsPositive(yearFeeRate))
                    throw new FabException("LNS168","2");
//			}
//			//3.分期+一次性
//			else if("3".equals(flag)){
//				if(!rateIsPositive(onetimeRate)||!rateIsPositive(yearFeeRate))
//					throw new FabException("LNS168","分期+一次性保费时，年利率大于0，一次性利率大于0");
            }else{
                throw new FabException("LNS169","费用交取模式",flag);
            }

        }else {

            //flag为1.一次性收取  yearfeeRate 为0  oneTimeRate 不为0
            if("1".equals(flag)){
                if(!LoanFeeUtils.rateIsPositive(onetimeRate)||LoanFeeUtils.rateIsPositive(yearFeeRate))
                    throw new FabException("LNS168",flag);
                if("2512623".equals(prdCode))
                    throw new FabException("LNS169","任性贷期缴融担费","费用交取模式");
            }
            //2.分期收取(不传也默认2)
            else if (VarChecker.isEmpty(flag)||"2".equals(flag)){
                if(LoanFeeUtils.rateIsPositive(onetimeRate))
                    throw new FabException("LNS168","2");
            }

            //3.分期+一次性
            else if("3".equals(flag)){
                if(!LoanFeeUtils.rateIsPositive(onetimeRate)||!LoanFeeUtils.rateIsPositive(yearFeeRate))
                    throw new FabException("LNS168","3");
                if("2512623".equals(prdCode))
                    throw new FabException("LNS169","任性贷期缴融担费","费用交取模式");
            }else{
                throw new FabException("LNS169","费用交取模式",flag);
            }
        }
    }


    //费用list校验
    public static void checkFeeList(String prdCode, ListMap pkgList ) throws  FabException{
        //保费产品
        if(VarChecker.asList("2512617","2512619").contains(prdCode)){
        	if( pkgList.size() > 1 )
        		throw new FabException("LNS220","ISFE");
        	
        	for(PubDict pkg:pkgList.getLoopmsg()){
        		if( !VarChecker.asList(ConstantDeclare.FEETYPE.ISFE )
        				.contains(PubDict.getRequestDict(pkg, "feeType").toString()))
        			throw new FabException("SPS106","feeType");
        		
        		if( !VarChecker.asList(ConstantDeclare.FEEREPAYWAY.STAGING,ConstantDeclare.FEEREPAYWAY.ONETIME )
        				.contains(PubDict.getRequestDict(pkg, "feerepayWay").toString()))
        			throw new FabException("SPS106","feerepayWay");
        		
        		if( !VarChecker.asList( "2" ) //计费方式枚举值  1-按日  2-按期
        				.contains(PubDict.getRequestDict(pkg, "calCulatrule").toString()))
        			throw new FabException("SPS106","calCulatrule");
        		
        		if( !VarChecker.asList( "1" )	//费用计息基数枚举值  1-合同金额  0-合同余额
        				.contains(PubDict.getRequestDict(pkg, "feeBase").toString()))
        			throw new FabException("SPS106","feeBase");
        	}
        }
        //预扣费产品
//        if( VarChecker.asList("2412624","2412626").contains(prdCode) )
//        {
//        	for(PubDict pkg:pkgList.getLoopmsg()){
//        		if( !VarChecker.asList(ConstantDeclare.FEEREPAYWAY.ADVDEDUCT )
//        				.contains(PubDict.getRequestDict(pkg, "feerepayWay").toString()))
//        			throw new FabException("SPS106","feerepayWay");
//        	}
//        }
    }
        
        
    public static String specialRepayDate(LoanAgreement loanAgreement, List<TblLnsbill> tblLnsBills, List<TblLnsrpyplan> rpyplanlist, String repayDate) throws FabException {
        //到期日
        for(TblLnsrpyplan lnsrpyplan:rpyplanlist){
            if (CalendarUtil.after(repayDate,lnsrpyplan.getRepayintbdate().trim())
                    &&CalendarUtil.beforeAlsoEqual(repayDate,lnsrpyplan.getActrepaydate().trim()))
            {
                //判断是当天为到期日结息  并当天结清当期
                Boolean curSettle = true;
                if(CalendarUtil.beforeAlsoEqual(repayDate,loanAgreement.getContract().getRepayPrinDate())) {
                    for (TblLnsbill lnsbill : tblLnsBills) {
                        if (ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsbill.getSettleflag().trim())) {
                            curSettle = false;
                        }
                    }
                }else
                    curSettle = false;
                if(curSettle)
                    repayDate = CalendarUtil.nDaysAfter(lnsrpyplan.getActrepaydate().trim(),1 ).toString("yyyy-MM-dd");
                break;
            }
        }

        Boolean isfuture = true;
        //贷款已经有未结清逾期账本、贷款已经有处于宽限期的未结清账本，不允许展期试算
        if(CalendarUtil.after(repayDate,loanAgreement.getContract().getRepayPrinDate())) {
            isfuture = false;
        }else {
            for (TblLnsbill lnsbill : tblLnsBills) {
                //等本等息还到未来期 不能延期还款
                if (CalendarUtil.after(repayDate, lnsbill.getBegindate())
                        && ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsbill.getSettleflag()))
                    isfuture = false;
            }
        }
        if(isfuture)
            throw new FabException("LNS210");
        return repayDate;
    }
    public static String getRepayDate(LoanAgreement loanAgreement, List<TblLnsbill> tblLnsBills, List<TblLnsrpyplan> rpyplanlist, String repayDate) {
        //展期日 当天结息
        if(CalendarUtil.equalDate(repayDate,loanAgreement.getContract().getRepayPrinDate())){

            //到期日
            for(TblLnsrpyplan lnsrpyplan:rpyplanlist){
                if (CalendarUtil.equalDate(repayDate,lnsrpyplan.getActrepaydate().trim()))
                {
                    //判断是当天为到期日结息  并当天结清当期
                    Boolean curSettle = true;
                    for(TblLnsbill lnsbill:tblLnsBills){
                        if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsbill.getSettleflag().trim())){
                            curSettle = false;
                        }
                    }
                    //提前还本  这一期作废
                    if(!lnsrpyplan.getActrepaydate().trim().equals(lnsrpyplan.getRepayintedate())){
                        curSettle = true;
                    }
                    if(curSettle)
                        repayDate = CalendarUtil.nDaysAfter(lnsrpyplan.getActrepaydate().trim(),1 ).toString("yyyy-MM-dd");
                    break;
                }
            }
        }
        return repayDate;
    }
}

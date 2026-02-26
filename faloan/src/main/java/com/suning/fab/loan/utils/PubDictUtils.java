package com.suning.fab.loan.utils;

import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.VarChecker;

import java.util.Arrays;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705 MYP
 * @Date Created in 17:44 2019/7/23
 * @see
 */
public class PubDictUtils {

    //获取资金方中的 investeeFlag 为了抛事件用
    public static String getInvesteeFlag(TranCtx ctx) {
        ListMap pkgList1 = ctx.getRequestDict("pkgList1");
        if (null != pkgList1 && pkgList1.size() > 0) {
            //未做次校验 暂不支持多资金方

            if ("03".equals(PubDict.getRequestDict(pkgList1.getLoopmsg().get(0), "investeeFlag"))) {
                return "chtype";
            }
            return "";


        } else {
            return "";
        }
    }
    //获取资金方中的 investeeFlag 为了抛事件用
    public static String seekInvesteeFlag(TranCtx ctx) {
        ListMap pkgList1 = ctx.getRequestDict("pkgList1");
        if (null != pkgList1 && pkgList1.size() > 0) {
            //未做次校验 暂不支持多资金方

            if ("03".equals(PubDict.getRequestDict(pkgList1.getLoopmsg().get(0), "investeeFlag"))) {
                return "03";
            }
            return "";

        } else {
            return "";
        }
    }

    public static <T> T getReqDict(TranCtx ctx, String key,Class<T> requiredType) throws FabException {
        checkReqNull(ctx, key);
        return ctx.getRequestDict(key);
    }
    public static <T> T getReqDict(TranCtx ctx, String key) throws FabException {
        checkReqNull(ctx, key);
        return ctx.getRequestDict(key);
    }
    public static void checkReqNull(TranCtx ctx, String key) throws FabException {
        if(VarChecker.isEmpty(ctx.getRequestDict(key))){
            throw new FabException("LNS055",key);
        }
    }

    /**
     *
     * @param repayDate
     * @throws FabException
     */
    public static void checkFirstRepayDate(String firstRepayDate,String repayDate,String endDate,String startDate,String repayWay)throws FabException{

    	/*还款日为（31号，30号，29号）之一且首期还款日为当月月末日期（该月末日期小于所传还款日）时，不报错*/
        if(!(CalendarUtil.getMonthEnd(firstRepayDate).getDayOfMonth()==Integer.valueOf(firstRepayDate.substring(8,10)) &&
       		 Integer.valueOf(firstRepayDate.substring(8,10))<= Integer.valueOf(repayDate)) && !firstRepayDate.substring(8,10).equals( repayDate.length()==1?("0"+repayDate):repayDate)){
            throw new FabException("LNS240", "首期还款日与每期还款日不一致 ");
        }

        if(CalendarUtil.after(firstRepayDate,endDate)||CalendarUtil.beforeAlsoEqual(firstRepayDate,startDate)){
            throw new FabException("LNS240", "首期还款日需在起息日至到期日之间");
        }

        if(!Arrays.asList(ConstantDeclare.REPAYWAY.REPAYWAY_DBDX,ConstantDeclare.REPAYWAY.REPAYWAY_DEBX,ConstantDeclare.REPAYWAY.REPAYWAY_DEBJ,ConstantDeclare.REPAYWAY.REPAYWAY_DEBXF).contains(repayWay)){
            throw new FabException("LNS240","该还款方式暂不支持首期指定还款日");
        }

    }

}

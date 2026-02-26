package com.suning.fab.loan.utils;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.FeeRepayPlanQuery;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.RePayPlan;
import com.suning.fab.loan.bo.RpyList;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：还款/减免明细
 *
 * @Author 18049705 MYP
 * @Date Created in 10:25 2020/1/15
 * @see
 */
public class LoanRpyInfoUtil {

    public static void addFeeRpyInfo(Map<String, FabAmount> feeRpyInfo, String billStatus, FabAmount repayAmt) {
        addRpyInfo(feeRpyInfo, ConstantDeclare.BILLTYPE.BILLTYPE_FEEA, billStatus, repayAmt);
    }

    public static void addRpyInfo(Map<String, FabAmount> rpyInfo, String billtype, String billStatus, FabAmount repayAmt) {
        if (rpyInfo.get(billtype + "." + billStatus) == null) {
            FabAmount amt = new FabAmount(repayAmt.getVal());
            rpyInfo.put(billtype + "." + billStatus, amt);
        } else {
            rpyInfo.get(billtype + "." + billStatus).selfAdd(repayAmt);
        }
    }

    /**
     * @param rpyInfoList
     * @param billtype
     * @param repayterm
     * @param repayAmt
     * @param billExtend  还款明细需要区分 状态 还款方式时  新增辅助字段
     */
    public static void addRpyList(Map<String, Map<String, FabAmount>> rpyInfoList, String billtype, String repayterm, FabAmount repayAmt, String billExtend) {
        Map<String, FabAmount> rpyInfo = new HashMap<String, FabAmount>();
        //返回前端数据添加账本状态
        String rpyIndex = billtype + "|" + repayterm + "|" + (billExtend == null ? "" : billExtend);

        if (rpyInfoList.get(repayterm) == null) {
            if (rpyInfo.get(rpyIndex) == null) {
                FabAmount amt = new FabAmount(repayAmt.getVal());
                rpyInfo.put(rpyIndex, amt);
            } else {
                rpyInfo.get(rpyIndex).selfAdd(repayAmt);
            }
        } else {
            rpyInfo = rpyInfoList.get(repayterm);
            if (rpyInfo.get(rpyIndex) == null) {
                FabAmount amt = new FabAmount(repayAmt.getVal());
                rpyInfo.put(rpyIndex, amt);
            } else {
                rpyInfo.get(rpyIndex).selfAdd(repayAmt);
            }
        }
        rpyInfoList.put(repayterm, rpyInfo);
    }

    public static Integer saveRpyInfos(TranCtx ctx, int i, Map<String, FabAmount> feeRpyInfo, String acctNo, String repayAcctNo, String loanType) throws FabException {
        for (Map.Entry<String, FabAmount> entry : feeRpyInfo.entrySet()) {
            i++; //明细序号自增 适应唯一索引
            saveRpyInfo(ctx, i, acctNo, repayAcctNo, loanType, entry);
        }
        return i;
    }
    //保存还款信息
    public static void saveRpyInfo(TranCtx ctx, int i, String acctNo, String repayAcctNo, String loanType, Map.Entry<String, FabAmount> entry) throws FabException {
        TblLnsrpyinfo lnsrpyinfo = new TblLnsrpyinfo();
        lnsrpyinfo.setTrandate(ctx.getTranDate());
        lnsrpyinfo.setBrc(ctx.getBrc());
        lnsrpyinfo.setProfitbrc(ctx.getBrc());
        lnsrpyinfo.setAcctno(acctNo);
        lnsrpyinfo.setDiacct(repayAcctNo);
        lnsrpyinfo.setCcy("01");
        lnsrpyinfo.setAcctstat(entry.getKey());
        lnsrpyinfo.setTranamt(entry.getValue().getVal());
        lnsrpyinfo.setFlag("1");
        lnsrpyinfo.setSeqno(ctx.getSerSeqNo());
        lnsrpyinfo.setRepayterm(i);
        lnsrpyinfo.setLoantype1(loanType);
        try {
            DbAccessUtil.execute("Lnsrpyinfo.insert", lnsrpyinfo);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100", "lnsrpyinfo");
        }
    }

    public static String getLoantype(String compensateFlag) {
        if (!VarChecker.isEmpty(compensateFlag)) {
            if (VarChecker.asList("1", "2", "3", "4").contains(compensateFlag)) {
                return compensateFlag;
            }
        }
        return "";
    }

    //组装json格式明细数据2020-08-03
    public static String getRepayList(String acctNo, TranCtx ctx, Map<String, Map<String, FabAmount>> repayAmtList, LoanAgreement la) throws FabException {
        //打折利率基本信息表
        Map discountInfo = null;
        List<RpyList> repayToJsonList = new ArrayList<RpyList>();
        for (Entry<String, Map<String, FabAmount>> mapList : repayAmtList.entrySet()) {
            if ("471007".equals(ctx.getTranCode()) &&
                    ("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")) ? "true".equals(la.getInterestAgreement().getShowRepayList()) : Arrays.asList("2512626", "2512618","2512629", "2512630", "2512636", "2512637", "2512631", "2512644", "2512633","2512623","2512634","2512641","2512643","2512639","2512640","2512642").contains(la.getPrdId()))) {
                if (discountInfo == null) {
                    discountInfo = getDiscountInfo(acctNo, ctx);
                }
            }
            //马上消金 还款查询减免基本信息
            RpyList repayInfo = new RpyList();
            repayInfo.setRepayterm(Integer.valueOf(mapList.getKey()));
            for (Entry<String, FabAmount> m : mapList.getValue().entrySet()) {
                if (ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(m.getKey().split("\\|")[0].toString()))
                    repayInfo.getPrinAmt().selfAdd(m.getValue());
                else if (ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(m.getKey().split("\\|")[0].toString())) {
                    if ("471007".equals(ctx.getTranCode())) {
                        repayInfo.getIntAmt().selfAdd(m.getValue());
                        //还款接口区分利息明细的状态
                        repayInfo.setNintBillStatus(m.getKey().split("\\|")[2]);
                        //还款明细获取减免金额  只有任性贷马上消金对外提供
                        if ("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")) ? "true".equals(la.getInterestAgreement().getShowRepayList()) : Arrays.asList("2512626", "2512618","2512629", "2512630", "2512636", "2512637", "2512631", "2512644","2512633","2512623","2512634","2512641","2512643","2512639","2512640","2512642").contains(la.getPrdId())) {
                            repayInfo.getReduceIntAmt().selfAdd(getReduceAmt(m.getValue().getVal(), mapList.getKey(), discountInfo, la,ctx));
                            if("Y".equals(StringUtil.obj2str(discountInfo.get("discountIntFlag")))&&repayInfo.getReduceIntAmt().isPositive()){
                                    repayInfo.setDiscountIntFlag("Y");
                            }
                        }
                    } else if ("478001".equals(ctx.getTranCode()))
                        repayInfo.getReduceIntAmt().selfAdd(m.getValue());
                } else if (VarChecker.asList(
                        ConstantDeclare.BILLTYPE.BILLTYPE_GINT,
                        ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
                        ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(m.getKey().split("\\|")[0].toString())) {
                    if ("471007".equals(ctx.getTranCode()))
                        repayInfo.getForfeitAmt().selfAdd(m.getValue());
                    else if ("478001".equals(ctx.getTranCode()))
                        repayInfo.getReduceFintAmt().selfAdd(m.getValue());
                } else if (LoanFeeUtils.isFeeType(m.getKey().split("\\|")[0].toString())) {
                    String repayWay = m.getKey().split("\\|")[2];
                    repayInfo.getForfeetAmt().selfAdd(m.getValue());
                    if ("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")) ? "true".equals(la.getInterestAgreement().getShowRepayList()) : Arrays.asList("2512626", "2512618","2512629", "2512630", "2512636", "2512637", "2512631", "2512644","2512633","2512623","2512634","2512641","2512643","2512639","2512640","2512642").contains(la.getPrdId())) {
                        //获取减免费用
                        Map reduceMap = getReduceFeeAmt(m.getValue().getVal(), mapList.getKey(), la, ctx, m.getKey().split("\\|")[0], repayWay);
                        repayInfo.getCouponFeeAmt().selfAdd((Double) reduceMap.get("reduceFeeAmt"));
                        if ("Y".equals(StringUtil.obj2str(reduceMap.get("discountFeeFlag"))) && repayInfo.getCouponFeeAmt().isPositive()) {
                            repayInfo.setDiscountfeeFlag("Y");
                        }
                    }
                }
            }
            //判断当期是否要还利息  阻断当期利息全减的情况
            if ("471007".equals(ctx.getTranCode()) &&
                    repayInfo.getIntAmt().isZero() &&
                    ("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")) ? "true".equals(la.getInterestAgreement().getShowRepayList()) : Arrays.asList("2512643","2512626", "2512618","2512629", "2512630", "2512636", "2512637", "2512631", "2512644","2512633","2512623","2512634","2512641","2512639","2512640","2512642").contains(la.getPrdId()))) {
                repayInfo.getReduceIntAmt().selfAdd(getReduceAmt(0.00, mapList.getKey(), discountInfo, la,ctx));
                if("Y".equals(StringUtil.obj2str(discountInfo.get("discountIntFlag")))&&repayInfo.getReduceIntAmt().isPositive()){
                    repayInfo.setDiscountIntFlag("Y");
                }
            }
            if ("471011".equals(ctx.getTranCode()) && repayInfo.getForfeetAmt().isZero() &&
                    ("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")) ? "true".equals(la.getInterestAgreement().getShowRepayList()) : Arrays.asList("2512643","2512626", "2512618","2512629", "2512630", "2512636", "2512637", "2512631", "2512644","2512633","2512623","2512634","2512641","2512639","2512640","2512642").contains(la.getPrdId()))) {
                //获取减免费用
                Map reduceMap = getReduceFeeAmt(0.00, mapList.getKey(), la, ctx, null, null);
                repayInfo.getCouponFeeAmt().selfAdd((Double) reduceMap.get("reduceFeeAmt"));
                if("Y".equals(StringUtil.obj2str(reduceMap.get("discountFeeFlag")))&&repayInfo.getCouponFeeAmt().isPositive()){
                    repayInfo.setDiscountfeeFlag("Y");
                }
            }
            repayToJsonList.add(repayInfo);
        }
        return JsonTransfer.ToJson(repayToJsonList);
    }

    /**
     * 获取利息减免的金额  优先获取打折利率
     *
     * @param intAmt
     * @param period
     * @return
     */
    public static Double getReduceAmt(Double intAmt, String period, Map discountInfo, LoanAgreement la,TranCtx ctx) throws FabException {
        //获取利率和动态减免金额
        if (discountInfo.get("lnsbasicinfoex") != null && StringUtil.getValueFromTunnelData(((TblLnsbasicinfoex) discountInfo.get("lnsbasicinfoex")).getTunneldata(), ConstantDeclare.PARACONFIG.ZQLL) != null) {
            Double normalRate = la.getRateAgreement().getNormalRate().getRate().doubleValue();//计息利率
            Double discountRate = ((BigDecimal) StringUtil.getValueFromTunnelData(((TblLnsbasicinfoex) discountInfo.get("lnsbasicinfoex")).getTunneldata(), ConstantDeclare.PARACONFIG.ZQLL)).doubleValue();//折前利率
            BigDecimal rateSub = new BigDecimal(discountRate).subtract(new BigDecimal(normalRate)).divide(new BigDecimal(normalRate), 6, BigDecimal.ROUND_HALF_UP);
            BigDecimal reduceIntAmt=new BigDecimal(intAmt).multiply(rateSub).setScale(2,BigDecimal.ROUND_HALF_UP);
            Map param = new HashMap();
            param.put("acctno", la.getContract().getReceiptNo());
            param.put("openbrc",ctx.getBrc());
            param.put("key", ConstantDeclare.PARACONFIG.EXTEND);
            TblLnsbasicinfoex lnsbasicinfoex;//账户辅助表 获取打折利率
            try {
                //获取账户扩展表
                lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
                //获取不到打折信息、获取免息信息
                if (lnsbasicinfoex != null) {
                    param.put("value1", lnsbasicinfoex.getValue1());
                    param.put("value2", lnsbasicinfoex.getValue2());
                    param.put("value3", lnsbasicinfoex.getValue3());
                    FaloanJson tunnelMap = new FaloanJson(JSONObject.parseObject(lnsbasicinfoex.getTunneldata()));
                    tunnelMap.put(BigDecimal.valueOf(tunnelMap.getDouble(period+":Z")).add(reduceIntAmt).doubleValue(),period+":Z");
                    param.put("tunneldata",  tunnelMap.toString());
                    DbAccessUtil.execute("Lnsbasicinfoex.updateByUk", param);
                }
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS103", "Lnsbasicinfoex");
            }
            return reduceIntAmt.doubleValue();
        } else if (discountInfo.get("dynamicDiscountAmt") != null) {
            return ((FaloanJson) discountInfo.get("dynamicDiscountAmt")).getDouble(period);
        } else {
            return Double.valueOf(0.00);
        }
    }

    /**
     * 获取减免费用金额
     *
     * @param period
     * @param la
     * @return
     * @throws FabException
     */
    public static Map getReduceFeeAmt(Double feeAmt, String period, LoanAgreement la, TranCtx ctx, String billType, String repayWay) throws FabException {
        //此地需要修改   --------------------------------------------------------
        //将减免费用 打折相关信息存储在map 中
        Map reduceFeeResult = new HashMap();
        Map param = new HashMap();
        param.put("acctno", la.getContract().getReceiptNo());
        param.put("openbrc", ctx.getBrc());
        param.put("key", ConstantDeclare.PARACONFIG.EXTEND);
        TblLnsbasicinfoex lnsbasicinfoex;//账户辅助表 获取打折利率
        try {
            //获取账户扩展表
            lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
            //获取不到打折信息、获取免息信息
            if (lnsbasicinfoex != null) {
                param.put("value1", lnsbasicinfoex.getValue1());
                param.put("value2", lnsbasicinfoex.getValue2());
                param.put("value3", lnsbasicinfoex.getValue3());
                FaloanJson tunnelMap = new FaloanJson(JSONObject.parseObject(lnsbasicinfoex.getTunneldata()));
                reduceFeeResult.put("discountFeeFlag", StringUtil.getValueFromTunnelData(lnsbasicinfoex.getTunneldata(), ConstantDeclare.PARACONFIG.DISCOUNTFEEFLAG));
                //获取每期减免金额
                Map periodMap = getReducePeriodAmt(lnsbasicinfoex.getTunneldata(), period, billType, repayWay);
                List<String> list = (List<String>) periodMap.get("removeKeys");
                if (StringUtil.getValueFromTunnelData(lnsbasicinfoex.getTunneldata(), billType + ":" + repayWay + ":" + ConstantDeclare.PARACONFIG.FEEDISCOUNT) != null) {
                    Double normalRate = getFeeRate(la.getFeeAgreement().getLnsfeeinfos(), billType, repayWay);
                    Double discountRate = ((BigDecimal) StringUtil.getValueFromTunnelData(lnsbasicinfoex.getTunneldata(), billType + ":" + repayWay + ":" + ConstantDeclare.PARACONFIG.FEEDISCOUNT)).doubleValue();//折前利率
                    //差额利率
                    BigDecimal rateSub = new BigDecimal(discountRate).subtract(new BigDecimal(normalRate)).divide(new BigDecimal(normalRate), 6, BigDecimal.ROUND_HALF_UP);
                    reduceFeeResult.put("reduceFeeAmt", new BigDecimal(feeAmt).multiply(rateSub).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
//                    tunnelMap.put(BigDecimal.valueOf(tunnelMap.getDouble(billType + ":" + repayWay + ":" + ConstantDeclare.PARACONFIG.FREEFEE+":"+period.toString()+":Z")).add(new BigDecimal(feeAmt).multiply(rateSub)).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue(),billType + ":" + repayWay + ":" + ConstantDeclare.PARACONFIG.FREEFEE+":"+period.toString()+":Z");
//                    param.put("tunneldata",  tunnelMap.toString());
//                    DbAccessUtil.execute("Lnsbasicinfoex.updateByUk", param);
                    return reduceFeeResult;
                } else if (list.size() > 0) {
                    for (String removeKey : list) {
                        tunnelMap.removeKey(removeKey);
                    }
                    param.put("tunneldata",  tunnelMap.toString());
                    DbAccessUtil.execute("Lnsbasicinfoex.updateByUk", param);
                    reduceFeeResult.put("reduceFeeAmt", ((FabAmount) periodMap.get("periodAmt")).getVal());
                    return reduceFeeResult;
                }
            } else {
                reduceFeeResult.put("reduceFeeAmt", new Double(0.00));
                return reduceFeeResult;
            }
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "Lnsbasicinfoex");
        }
        reduceFeeResult.put("reduceFeeAmt", new Double(0.00));
        return reduceFeeResult;
    }

    /**
     * 获取费用的还款方式
     *
     * @param billType
     * @param repayWay
     * @return
     */
    public static Double getFeeRate(List<TblLnsfeeinfo> feeBills, String billType, String repayWay) {
        for (TblLnsfeeinfo feeBill : feeBills) {
            if (repayWay.equals(feeBill.getRepayway()) && billType.equals(feeBill.getFeetype())) {
                return feeBill.getFeerate();
            }
        }
        return 0.00;
    }

    /**
     * 获取费用减免金额，当次期数
     *
     * @param tunnelData
     * @param period
     * @return
     */
    public static Map getReducePeriodAmt(String tunnelData, String period, String billType, String repayWay) {
        Map result = new HashMap();
        Map<String, Object> tunnelMap = JSONObject.parseObject(tunnelData, Map.class);
        //当次
        List<String> removeKeys = new ArrayList<String>();
        FabAmount periodAmt = new FabAmount();
        for (Map.Entry<String, Object> tunnelTemp : tunnelMap.entrySet()) {
            if (tunnelTemp.getKey().equals(billType + ":" + repayWay + ":" + ConstantDeclare.PARACONFIG.FREEFEE + ":" + period + ":F")) {
                removeKeys.add(tunnelTemp.getKey());
                periodAmt.selfAdd(((BigDecimal) tunnelTemp.getValue()).doubleValue());
            }
        }
        result.put("removeKeys", removeKeys);
        result.put("periodAmt", periodAmt);
        return result;
    }

    /**
     * 获取打折、减免相关信息
     *
     * @param acctno 账户
     * @param ctx    上下文
     * @return
     */
    public static Map getDiscountInfo(String acctno, TranCtx ctx) throws FabException {
        Map result = new HashMap();
        //查询动态表减免金额 每次获取当此还款金额  优先获取打折利率
        Map param = new HashMap();
        param.put("acctno", acctno);
        param.put("openbrc", ctx.getBrc());
        param.put("key", ConstantDeclare.PARACONFIG.EXTEND);
        TblLnsbasicinfoex lnsbasicinfoex;//账户辅助表 获取打折利率
        FaloanJson dynamicDiscountAmt;//免息金额
        FaloanJson removekeyMap=new FaloanJson();//移除的当次免息金额
        try {
            //获取账户扩展表
            lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
            //获取不到打折信息、获取免息信息
            if (lnsbasicinfoex == null || StringUtil.getValueFromTunnelData(lnsbasicinfoex.getTunneldata(), ConstantDeclare.PARACONFIG.ZQLL) == null) {
                //取主文件动态表剩余免息金额
                TblLnsbasicinfodyn lnsbasicinfoDyn = DbAccessUtil.queryForObject("Lnsbasicinfodyn.selectByUk", param, TblLnsbasicinfodyn.class);
                if (null != lnsbasicinfoDyn && !VarChecker.isEmpty(lnsbasicinfoDyn.getTunneldata())) {
                    String json = lnsbasicinfoDyn.getTunneldata();
                    dynamicDiscountAmt = FaloanJson.parseObject(json); //获取动态扩展表的减免信息
                    removekeyMap=removeMxCurrent(dynamicDiscountAmt);
                    param.put("tunneldata", dynamicDiscountAmt.toString());
                    param.put("trandate", ctx.getTranDate());
                    DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfodyn_common", param);
                }else{
                    //兼容老数据
                    param.put("key", ConstantDeclare.PARACONFIG.ZQLL);
                    //获取账户扩展表
                    lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
                    if(null != lnsbasicinfoex){
                        FaloanJson faloanJson=new FaloanJson();
                        faloanJson.put(lnsbasicinfoex.getValue3(),ConstantDeclare.PARACONFIG.ZQLL);
                        lnsbasicinfoex.setTunneldata(faloanJson.toString());
                    }
                }
            }
            if (lnsbasicinfoex != null) {
                result.put("discountIntFlag", StringUtil.obj2str(StringUtil.getValueFromTunnelData(lnsbasicinfoex.getTunneldata(), ConstantDeclare.PARACONFIG.DISCOUNTINTFLAG)));
            }
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbasicinfoDyn_Lnsbasicinfoex");
        }
        result.put("lnsbasicinfoex", lnsbasicinfoex); //返回打折、免息信息
        result.put("dynamicDiscountAmt", removekeyMap);
        return result;
    }

    /**
     * 将当次用完的免息券进行移除
     *
     * @param dynamicDiscountAmt
     */
    public static FaloanJson removeMxCurrent(FaloanJson dynamicDiscountAmt) {
        FaloanJson removeKeyMap = new FaloanJson();
        Map<String, Object> discountMap = dynamicDiscountAmt.getJson();
        List<String> removeKeys = new ArrayList();
        for (Map.Entry<String, Object> discountKey : discountMap.entrySet()) {
            if (!discountKey.getKey().contains("MX") && !discountKey.getKey().contains("Z")) {
                removeKeys.add(discountKey.getKey());
                removeKeyMap.put(discountKey.getValue(),discountKey.getKey());
            }
        }
            //移除当次免息券
            for (String removeKey : removeKeys) {
                discountMap.remove(removeKey);
            }
            return removeKeyMap;
        }

    /**
     * 设置贴息 贴费标志
     * @param ctx
     */
    public static FaloanJson getStickFlag(TranCtx ctx,String acctNo)throws FabException{
        if(!VarChecker.isEmpty(ctx.getRequestDict("investeeAcctno"))){
            Map searchParam=new HashMap();
            searchParam.put("acctno",acctNo);
            searchParam.put("brc",ctx.getBrc());
            searchParam.put("openbrc", ctx.getBrc());
            searchParam.put("newinvestee",ctx.getRequestDict("investeeAcctno"));
            TblLnsinvesteedetail lnsinvesteedetail;
            try {
                lnsinvesteedetail=DbAccessUtil.queryForObject("AccountingMode.query_lnsinvesteedetail",searchParam,TblLnsinvesteedetail.class);
                if(lnsinvesteedetail!=null){
                    FaloanJson tunnelMap=getReduceExtend(ctx,acctNo);
                    tunnelMap.put("Y",ConstantDeclare.PARACONFIG.DISCOUNTFEEFLAG);
                    tunnelMap.put("Y",ConstantDeclare.PARACONFIG.DISCOUNTINTFLAG);
                    return tunnelMap;
                }
            }
            catch (FabSqlException e)
            {
                throw new FabException(e, "SPS103", "lnsinvesteedetail");
            }
        }
        return new FaloanJson();
    }

    /**
     * 减免费用、明细 获取扩展信息
     * @param ctx 上下文
     * @param acctNo 借据号
     * @return
     */
    public static FaloanJson getReduceExtend(TranCtx ctx,String acctNo)throws FabException{
        Map searchParam=new HashMap();
        searchParam.put("acctno", acctNo);
        searchParam.put("openbrc", ctx.getBrc());
        searchParam.put("key", ConstantDeclare.PARACONFIG.EXTEND);
        try {
            //账户辅助表 获取打折利率
            TblLnsbasicinfoex lnsbasicinfoex = DbAccessUtil.queryForObject("Lnsbasicinfoex.selectByUk", searchParam, TblLnsbasicinfoex.class);
            FaloanJson tunnelMap = new FaloanJson();
            if (lnsbasicinfoex != null) {
                tunnelMap = new FaloanJson(JSONObject.parseObject(lnsbasicinfoex.getTunneldata()));
            }
            TblLnsbasicinfodyn lnsbasicinfoDyn = DbAccessUtil.queryForObject("Lnsbasicinfodyn.selectByUk", searchParam, TblLnsbasicinfodyn.class);
            if (null != lnsbasicinfoDyn && !VarChecker.isEmpty(lnsbasicinfoDyn.getTunneldata())) {
                String json = lnsbasicinfoDyn.getTunneldata();
                //将利息减免的金额进行合并
                tunnelMap.getJson().putAll(FaloanJson.parseObject(json).getJson());
            }
            return tunnelMap;
        }catch (FabSqlException e)
        {
            throw new FabException(e, "SPS103", "lnsinvesteedetail");
        }

    }

    /**
     * 获取当期减免的费用总金额
     * @param period
     * @param tunnelMap
     * @param billType
     * @return
     */
    public static BigDecimal getRepayTermDiscountFeeByFreeAmt(String period,FaloanJson tunnelMap,String billType){
        if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(billType)){
            for(Map.Entry<String,Object> temp :tunnelMap.getJson().entrySet()){
                //利息相等
                if(temp.getKey().equals(period+":Z")){
                    return (BigDecimal)temp.getValue();
                }
            }
        }else if(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE.equals(billType)){
            for(Map.Entry<String,Object> temp :tunnelMap.getJson().entrySet()){
                if(temp.getKey().contains(ConstantDeclare.PARACONFIG.FREEFEE+":"+period+":Z")){
                    return (BigDecimal)temp.getValue();
                }
            }
        }
        return BigDecimal.valueOf(0.00);
    }

    /**
     * 获取当期费用和利息的减免金额
     * @param tunnelMap
     * @return
     */
    public static BigDecimal getRepayTermDiscountFeeByRate(FaloanJson tunnelMap,String billType,String repayWay,FabAmount amt,LoanAgreement la){
        if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(billType)){
            if(new FabRate(tunnelMap.getDouble(ConstantDeclare.PARACONFIG.ZQLL)).isPositive()){
                Double normalRate = la.getRateAgreement().getNormalRate().getRate().doubleValue();//正常利率
                Double discountRate =tunnelMap.getDouble(ConstantDeclare.PARACONFIG.ZQLL);//折前利率
                //差额利率
                BigDecimal rateSub = new BigDecimal(discountRate).subtract(new BigDecimal(normalRate)).divide(new BigDecimal(normalRate), 6, BigDecimal.ROUND_HALF_UP);
                return new BigDecimal(amt.getVal()).multiply(rateSub).setScale(2,BigDecimal.ROUND_HALF_UP);
            }
        }else if(LoanFeeUtils.isFeeType(billType)){
            if(new FabRate(tunnelMap.getDouble(billType + ":" + repayWay + ":" + ConstantDeclare.PARACONFIG.FEEDISCOUNT)).isPositive()){
                Double normalRate = LoanRpyInfoUtil.getFeeRate(la.getFeeAgreement().getLnsfeeinfos(), billType, repayWay);
                Double discountRate =tunnelMap.getDouble(billType + ":" + repayWay + ":" + ConstantDeclare.PARACONFIG.FEEDISCOUNT);//折前利率
                //差额利率
                BigDecimal rateSub = new BigDecimal(discountRate).subtract(new BigDecimal(normalRate)).divide(new BigDecimal(normalRate), 6, BigDecimal.ROUND_HALF_UP);
                return new BigDecimal(amt.getVal()).multiply(rateSub).setScale(2,BigDecimal.ROUND_HALF_UP);
            }
        }
        return BigDecimal.valueOf(0.00);
    }

    /**
     * 补贴折扣券
     * @param repayPlanList 还款明细
     */
    public static void stickDiscount(List<FeeRepayPlanQuery> repayPlanList, FaloanJson tunnelMap)throws FabException{
        if("Y".equals(tunnelMap.getString(ConstantDeclare.PARACONFIG.DISCOUNTFEEFLAG))){
            for(FeeRepayPlanQuery feeRepayPlanQuery:repayPlanList){
                if(!feeRepayPlanQuery.getTermcdfee().isPositive()){
                    feeRepayPlanQuery.setTermcdfee(new FabAmount(0.00));
                }
                BigDecimal stickFee=getRepayTermDiscountFeeByFreeAmt(feeRepayPlanQuery.getRepayterm()+"",tunnelMap,ConstantDeclare.BILLTYPE.BILLTYPE_AFEE);
                if(new FabAmount(stickFee.doubleValue()).isPositive()){
                    if(feeRepayPlanQuery.getFeeamt().isPositive()){
                        //已还费用
                        feeRepayPlanQuery.getFeeamt().selfAdd(stickFee.doubleValue());
                    }else{
                        if(!feeRepayPlanQuery.getTermretfee().isPositive()){
                            //已还费用
                            feeRepayPlanQuery.getFeeamt().selfAdd(stickFee.doubleValue());
                        }
                    }
                    feeRepayPlanQuery.getTermretfee().selfAdd(stickFee.doubleValue());
                }
            }
        }
        if("Y".equals(tunnelMap.getString(ConstantDeclare.PARACONFIG.DISCOUNTINTFLAG))){
            for(FeeRepayPlanQuery feeRepayPlanQuery:repayPlanList){
                if(!feeRepayPlanQuery.getTermcdint().isPositive()){
                    feeRepayPlanQuery.setTermcdint(new FabAmount(0.00));
                }
                BigDecimal stickInt=getRepayTermDiscountFeeByFreeAmt(feeRepayPlanQuery.getRepayterm()+"",tunnelMap,ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
                if(new FabAmount(stickInt.doubleValue()).isPositive()){
                    feeRepayPlanQuery.getTermretint().selfAdd(stickInt.doubleValue());
                    if(feeRepayPlanQuery.getIntAmt().isPositive()){
                        //已还利息
                        feeRepayPlanQuery.getIntAmt().selfAdd(stickInt.doubleValue());
                    }else{
                        if(!feeRepayPlanQuery.getTermretint().isPositive()){
                            //已还利息
                            feeRepayPlanQuery.getIntAmt().selfAdd(stickInt.doubleValue());
                        }
                    }
                }
            }
        }
    }

    /**
     * 补贴折扣券
     * @param repayPlanList 还款明细
     */
    public static void stickDiscountNint(List<RePayPlan> repayPlanList, FaloanJson tunnelMap)throws FabException{
        if("Y".equals(tunnelMap.getString(ConstantDeclare.PARACONFIG.DISCOUNTINTFLAG))){
            for(RePayPlan feeRepayPlanQuery:repayPlanList){
                if(!feeRepayPlanQuery.getTermcdint().isPositive()){
                    feeRepayPlanQuery.setTermcdint(new FabAmount(0.00));
                }
                BigDecimal stickInt=getRepayTermDiscountFeeByFreeAmt(feeRepayPlanQuery.getRepayterm()+"",tunnelMap,ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
                if(new FabAmount(stickInt.doubleValue()).isPositive()){
                    feeRepayPlanQuery.getTermretint().selfAdd(stickInt.doubleValue());
                    if(feeRepayPlanQuery.getIntAmt().isPositive()){
                        //已还费用
                        feeRepayPlanQuery.getIntAmt().selfAdd(stickInt.doubleValue());
                    }else{
                        if(!feeRepayPlanQuery.getTermretint().isPositive()){
                            //已还利息
                            feeRepayPlanQuery.getIntAmt().selfAdd(stickInt.doubleValue());
                        }
                    }
                }
            }
        }
    }

    }
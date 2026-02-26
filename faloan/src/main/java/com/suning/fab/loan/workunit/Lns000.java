package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONArray;
import com.suning.fab.loan.account.LnsInterfaceQueryM;
import com.suning.fab.loan.bo.TopOffDetail;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.domain.TblLnsprovision;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：
 *
 * @Author
 * @Date Created in 19:13 2019/10/11
 * @see
 */

@Scope("prototype")
@Repository
public class Lns000  extends WorkUnit {


    String acctNo;
    String brc;
    String tranDate;

    List<String> stringList = new ArrayList<String>();
    String outResult;



    public void run() throws Exception {
        String settleDate = tranDate;

        // 主文件
        TblLnsbasicinfo lnsbasicinfo;
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", acctNo);
        param.put("openbrc", brc);
        try {
            //取主文件信息
            lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbasicinfo");
        }

        if (null == lnsbasicinfo) {
            throw new FabException("SPS104", "lnsbasicinfo");
        }


        // 还款计划期数
        param = new HashMap<String,Object>();
        param.put("acctno", acctNo);
        param.put("brc", brc);
        Integer maxPeriod;
        try {
            maxPeriod = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrpyplanperiod", param, Integer.class);
        }catch (FabSqlException e){
            throw new FabException(e, "SPS103", "lnsbill");
        }


        // 封顶值
        TblLnspenintprovreg lnspenintprovreg;
        param = new HashMap<>();
        param.put("billtype", ConstantDeclare.KEYNAME.DTFD);
        param.put("receiptno", acctNo);
        param.put("brc", brc);
        try {
            lnspenintprovreg = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "Lnspenintprovreg");
        }


        //幂等
        Map<String, Object> query = new HashMap<>();
        query.put("acctno", acctNo);
        query.put("brc", brc);
        query.put("enddate", lnspenintprovreg.getEnddate());
        List<LnsInterfaceQueryM> interfaces;
        try {
            interfaces = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsinterface000", query, LnsInterfaceQueryM.class);
        } catch (FabException e) {
            throw new FabException(e, "SPS103", "LNSINTERFACE");
        }


        // 初始本金
        param = new HashMap<String, Object>();
        param.put("acctno", acctNo);
        param.put("brc", brc);
        BigDecimal orgPrin;
        try {
            orgPrin = DbAccessUtil.queryForObject("CUSTOMIZE.query_orgPrin", param, BigDecimal.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS103", "lnsbill");
        }




        // 按账务日期倒序排序
        Collections.sort(interfaces, new Comparator<LnsInterfaceQueryM>() {
            @Override
            public int compare(LnsInterfaceQueryM o1, LnsInterfaceQueryM o2) {
                return CalendarUtil.after(o1.getAccdate(), o2.getAccdate()) ? -1 : 0;
            }
        });




        // 统计计止日期
        String endDate = null;
        String lastDate = null;
        String endRepayDate = null;
        String type = "";

        Map<String, TopOffDetail> detailMap = new HashMap<String, TopOffDetail>();
        String lastDintDate = (new SimpleDateFormat("yyyy-MM-dd")).format(lnspenintprovreg.getEnddate());
        FabAmount accumulatePrin = new FabAmount(orgPrin.doubleValue());

        //没有过还款的
        if( interfaces.size() == 0 )
        {
            TopOffDetail detail = new TopOffDetail();
            detail.setDintbdate(lastDintDate);
            detail.setDintedate(tranDate);
            detail.setNoretprin(new FabAmount(accumulatePrin.getVal()));
            detail.setDealrate(minRate(lnsbasicinfo.getOverduerate(), lnsbasicinfo.getPrdcode()));
            detail.setCalDint(new FabAmount(detail.getNoretprin().getVal()
                    * minRate(lnsbasicinfo.getOverduerate(), lnsbasicinfo.getPrdcode())
                    / 360
                    * CalendarUtil.actualDaysBetween(detail.getDintbdate(),detail.getDintedate())));
            if( detail.getNoretprin().isPositive())
                detailMap.put(detail.getDintbdate() + detail.getDintedate(), detail);
        }
        else{
            if( ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat()))
            {
                if( interfaces.size() == 0 )
                    endDate = tranDate;
                else
                    endDate = interfaces.get(0).getAccdate();
            }
            else
                endDate = tranDate;


            for (LnsInterfaceQueryM lnsinterface : interfaces) {
                // 有过债权转让的都不统计
                if( "F".equals(lnsinterface.getReserv5().trim()))
                    type = lnsinterface.getReserv5();

                if( null == lastDate ){
                    if( endDate.equals(tranDate)){
                        TopOffDetail detail = new TopOffDetail();
                        detail.setDintbdate(lnsinterface.getAccdate());
                        detail.setDintedate(tranDate);
                        detail.setNoretprin(new FabAmount(accumulatePrin.getVal()));
                        detail.setDealrate(minRate(lnsbasicinfo.getOverduerate(), lnsbasicinfo.getPrdcode()));
                        detail.setCalDint(new FabAmount(detail.getNoretprin().getVal()
                                * minRate(lnsbasicinfo.getOverduerate(), lnsbasicinfo.getPrdcode())
                                / 360
                                * CalendarUtil.actualDaysBetween(detail.getDintbdate(),detail.getDintedate())));

                        if( detail.getNoretprin().isPositive())
                            detailMap.put(detail.getDintbdate() + detail.getDintedate(), detail);
                    }

                    lastDate = lnsinterface.getAccdate();
                    accumulatePrin.selfAdd(lnsinterface.getSumramt());
                    continue;
                }
                else{
                    if( !lastDate.equals(lnsinterface.getAccdate())){
                        TopOffDetail detail = new TopOffDetail();
                        detail.setDintbdate(lnsinterface.getAccdate());
                        detail.setDintedate(lastDate);
                        detail.setNoretprin(new FabAmount(accumulatePrin.getVal()));
                        detail.setDealrate(minRate(lnsbasicinfo.getOverduerate(), lnsbasicinfo.getPrdcode()));
                        detail.setCalDint(new FabAmount(detail.getNoretprin().getVal()
                                * minRate(lnsbasicinfo.getOverduerate(), lnsbasicinfo.getPrdcode())
                                / 360
                                * CalendarUtil.actualDaysBetween(detail.getDintbdate(),detail.getDintedate())));
                        if( detail.getNoretprin().isPositive())
                            detailMap.put(detail.getDintbdate() + detail.getDintedate(), detail);
                    }

                    lastDate = lnsinterface.getAccdate();
                    accumulatePrin.selfAdd(lnsinterface.getSumramt());
                    continue;
                }
            }
        }


        if( null != lastDate && CalendarUtil.after(lastDate,lastDintDate) ){
            TopOffDetail detail = new TopOffDetail();
            detail.setDintbdate(lastDintDate);
            detail.setDintedate(lastDate);
            detail.setNoretprin(new FabAmount(accumulatePrin.getVal()));
            detail.setDealrate(minRate(lnsbasicinfo.getOverduerate(), lnsbasicinfo.getPrdcode()));
            detail.setCalDint(new FabAmount(detail.getNoretprin().getVal()
                    * minRate(lnsbasicinfo.getOverduerate(), lnsbasicinfo.getPrdcode())
                    / 360
                    * CalendarUtil.actualDaysBetween(detail.getDintbdate(),detail.getDintedate())));
            if( detail.getNoretprin().isPositive())
                detailMap.put(detail.getDintbdate() + detail.getDintedate(), detail);
        }

        for (Map.Entry<String, TopOffDetail> entry : detailMap.entrySet()) {
            //公共参数取值
            String insertDate;
            insertDate = lnsbasicinfo.getAcctno() + "," +
                    lnsbasicinfo.getAcctno1() + "," +
                    lnsbasicinfo.getOpenbrc() + "," +
                    lnsbasicinfo.getPrdcode() + "," +
                    lnsbasicinfo.getRepayway() + "," +
                    lnsbasicinfo.getOpendate() + "," +
                    lnsbasicinfo.getBeginintdate() + "," +
                    lnsbasicinfo.getContduedate() + "," +
                    lnsbasicinfo.getLoanstat() + "," +
                    String.valueOf(CalendarUtil.actualDaysBetween(lnsbasicinfo.getContduedate(), settleDate)) + "," +
                    lnsbasicinfo.getNormalrate().toString() + "," +
                    lnsbasicinfo.getOverduerate().toString() + "," +
                    maxPeriod + "," +
                    (new SimpleDateFormat("yyyy-MM-dd")).format(lnspenintprovreg.getEnddate()).toString() + "," +
                    entry.getValue().getDintbdate() + "," +
                    entry.getValue().getDintedate() + "," +
                    entry.getValue().getNoretprin().toString() + "," +
                    entry.getValue().getDealrate().toString() + "," +
                    entry.getValue().getCalDint().toString()+","+
                    type;

            stringList.add(insertDate);
            outResult= JSONArray.toJSONString(stringList);
        }
    }


    public static Double minRate(Double overRate, String prdCode) {
        if( VarChecker.asList("2512633","2512630","2512637","2512629","2515631","2512636","2512632","2512640","2512639","2512642").contains(prdCode)) {
            if (0.36 < overRate)
                return 0.36;
        }
        else{
            if( 0.24 < overRate)
                return 0.24;
        }

        return overRate;
    }



    /**
     * @return the brc
     */
    public String getBrc() {
        return brc;
    }

    /**
     * @param brc to set
     */
    public void setBrc(String brc) {
        this.brc = brc;
    }

    /**
     * @return the tranDate
     */
    public String getTrandate() {
        return tranDate;
    }

    /**
     * @param tranDate to set
     */
    public void setTrandate(String tranDate) {
        this.tranDate = tranDate;
    }

    /**
     * @return the stringList
     */
    public List<String> getStringList() {
        return stringList;
    }

    /**
     * @param stringList to set
     */
    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;
    }
}

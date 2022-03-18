package com.suning.fab.faibfp.localTest;

import com.alibaba.fastjson.JSON;
import com.suning.fab.faibfp.bean.TransferRelation;
import com.suning.fab.faibfp.dbhandler.TransferRelationHandler;
import com.suning.fab.faibfp.service.*;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.faibfp.utils.TranDateCutUtil;
import com.suning.fab.mulssyn.exception.FabException;
import com.suning.fab.mulssyn.utils.VarChecker;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/7/19
 * @Version 1.0
 */
public class Rsf471007Test extends TestUtil {

    @Autowired
    Rsf473004 rsf473004;
    @Autowired
    Rsf471007 rsf471007;

    @Autowired
    RsfSqlExecuteDeal rsfSqlExecuteDeal;

    @Autowired
    Rsf477016 rsf477016;
    /**
     * 1.配置scm迁移产品之前开户到老系统
     * 2.scm配置迁移产品
     * 3、调用还款(先把迁移表状态删除掉，重新插入一条记录)
     * 4.更细状态为处理中，再次调用还款，则抛异常
     * @throws FabException
     * @throws ParseException
     */
    @Test
    public void test() throws FabException, ParseException {
        //sonar检查
        TransferRelation transferRelation = new TransferRelation("routeId", "status", 1, new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()));
        transferRelation.getCreateTime();
        transferRelation.setCreateTime(new Timestamp(new Date().getTime()));
        transferRelation.getUpdateTime();
        transferRelation.setUpdateTime(new Timestamp(new Date().getTime()));

        TranDateCutUtil.setTranDateAndInite("2021-01-01", "", "");
        TranDateCutUtil.setOldSystemTrandate("2021-01-01");

//        // 迁移中产品开户
//        String receiptno_transfer = "TS11187867193819" + System.currentTimeMillis();
//        test473004(receiptno_transfer, "0000016");
        //先开好户后，再配置scm配置
        String receiptno_transfer = "TS111878671938191647571925755";
        Map<String, Object> reqMsg = new HashMap<>();
        reqMsg.put("type", "update");
        reqMsg.put("sql", "delete from transferrelation where routeid = '"+receiptno_transfer+"';");
        rsfSqlExecuteDeal.prepare(reqMsg);
        //还款
        test471007("serialNo_TS" + System.currentTimeMillis(), "2021-01-01", 1.0, receiptno_transfer);

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("acctNo", receiptno_transfer);
        input.put("brc", "51350000");
        input.put("tranCode", "477016");
        input.put("termDate", "2022-02-24");

        Map<String, Object> ret = rsf477016.execute(input);
        System.out.println("=============" + ret);

        //更新迁移状态为3，抛出异常
        Map<String, Object> reqMsg2 = new HashMap<>();
        reqMsg.put("type", "update");
        reqMsg.put("sql", "update transferrelation set status = '3' where routeid = '"+receiptno_transfer+"';");
        rsfSqlExecuteDeal.prepare(reqMsg);
        //还款
        test471007("serialNo_TS" + System.currentTimeMillis(), "2021-01-01", 1.0, receiptno_transfer);

    }


    public void test473004(String receiptno, String productCode) {

        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        input.put("serialNo", "TESTSERIALNO" + df.format(new Date()));
        input.put("outSerialNo", "outSerialNo000000");
        input.put("ccy", "CNY");
        input.put("contractAmt", 120000);
        input.put("contractNo", "contractNo000000");
        input.put("merchantNo", "huyi10");
        input.put("customType", "1");
        input.put("endDate", "2021-04-01");
        input.put("feeAmt", 0.00);
        input.put("feeRate", 0.00); //10.00
        input.put("repayDate", "01");
        input.put("intPerUnit", "M");
        input.put("periodNum", 1);
        input.put("periodType", "M");
        input.put("normalRate", 12.00);
        input.put("overdueRate", 15.00);
        input.put("compoundRate", 15.00);
        input.put("normalRateType", "Y");
        input.put("overdueRateType", "Y");
        input.put("compoundRateType", "Y");
        input.put("channelType", "5");    //放款渠道 1银行 2易付宝 3任性付
        input.put("loanType", "2");
        input.put("discountFlag", "1");
        input.put("receiptNo", receiptno);
        input.put("graceDays", 5);
        input.put("openBrc", "51030000");
        input.put("openDate", "2021-01-01");
        input.put("fundChannel", "fundChannel000000");
        input.put("productCode", productCode);
        input.put("repayWay", "10");
        input.put("startIntDate", "2021-01-01"); //2021-01-01
        input.put("discountAmt", 0);    //扣息金额
        input.put("cashFlag", "2");
        input.put("investee", "");
        input.put("tranCode", "473004");
        input.put("brc", "51030000");
        input.put("termDate", df1.format(new Date()));
        input.put("termTime", df2.format(new Date()));
        input.put("channelId", "66");
        input.put("promotionID", "UNKNOWN");
        input.put("fundingModel", "UNKNOWN");

        Map<String, Object> ret = rsf473004.execute(input);
        System.out.println("=============" + ret);
    }


    public Map<String, Object> test471007(String serialNo, String date, Double amt, String acctNo) throws ParseException {
        TranDateCutUtil.setTranDateAndInite(date, null, null);

        Map<String, Object> input = new HashMap<String, Object>();
        DateFormat df = new SimpleDateFormat("yyMMddHHmmssSSS");
        input.put("tranCode", "471007");
        input.put("brc", "51030000");
        input.put("termDate", date);
        input.put("termTime", "00:00:00");
        input.put("channelId", "66");
        input.put("serialNo", VarChecker.isEmpty(serialNo) ? "TESTSERIALNO" + df.format(new Date()) : serialNo);
        input.put("acctNo", acctNo);
        //input.put("repayAcctNo", "IBFP");
        input.put("ccy", "CNY");
        input.put("cashFlag", "2");
        input.put("repayAmt", amt);
        input.put("feeAmt", 0.00);
        input.put("repayChannel", "2");
        input.put("memo", "");
        input.put("channelId", "66");
        input.put("realDate", date);
        Map<String, Object> ret = rsf471007.execute(input);
        System.out.println(JSON.toJSONString(ret));
        return ret;
    }

    @Test
    public void sonar(){
        String receiptno_transfer = "TS11187867193819" + System.currentTimeMillis();
        test473004(receiptno_transfer, "0000016");
        new TransferRelationHandler().update(receiptno_transfer,"4",0);
    }
}

package com.suning.fab.faibfp.localTest;

import com.suning.fab.faibfp.service.Rsf470020;
import com.suning.fab.faibfp.service.Rsf473004;
import com.suning.fab.faibfp.service.RsfSqlExecuteDeal;
import com.suning.fab.faibfp.utils.TestUtil;
import com.suning.fab.faibfp.utils.TranDateCutUtil;
import com.suning.fab.mulssyn.exception.FabException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
public class Rsf470020Test extends TestUtil {

    @Autowired
    Rsf470020 rsf470020;

    @Autowired
    Rsf473004 rsf473004;

    @Autowired
    RsfSqlExecuteDeal rsfSqlExecuteDeal;

    @Test
    public void test() {

        // 在新模型和老模型各开一个户
        String receiptno_old = "O11187867193819" + System.currentTimeMillis();
        String receiptno_new = "N11187867193819" + System.currentTimeMillis();
        String receiptno_transfer = "TS11187867193819" + System.currentTimeMillis();
        TranDateCutUtil.setTranDateAndInite("2021-01-01", "", "");
        TranDateCutUtil.setOldSystemTrandate("2021-01-01");
        // 开老系统户
        test473004(receiptno_old, "0000013");
        // 开新模型户
        test473004(receiptno_new, "0000015");
        // 迁移中产品开户
        test473004(receiptno_transfer, "0000016");
//         批量预约还款计划查询
        test470020(receiptno_old, receiptno_new,receiptno_transfer);
        //test470020("O111878671938191629256927306", "N111878671938191629256927306");


    }

    //迁移中的产品，迁移中的借据号，抛出异常
    @Test
    public void test2() throws FabException {

        // 在新模型和老模型各开一个户
        String receiptno_old = "O11187867193819" + System.currentTimeMillis();
        String receiptno_new = "N11187867193819" + System.currentTimeMillis();
        String receiptno_transfer = "TS11187867193819" + System.currentTimeMillis();
        TranDateCutUtil.setTranDateAndInite("2021-01-01", "", "");
        TranDateCutUtil.setOldSystemTrandate("2021-01-01");
        // 开老系统户
        test473004(receiptno_old, "0000013");
        // 开新模型户
        test473004(receiptno_new, "0000015");
        // 迁移中产品开户
        test473004(receiptno_transfer, "0000016");
        //更新迁移中产品的借据号的迁移表状态
        Map<String, Object> reqMsg = new HashMap<>();
        reqMsg.put("type", "update");
        reqMsg.put("sql", "update transferrelation set status = '3' where routeid = '"+receiptno_transfer+"';");
        rsfSqlExecuteDeal.prepare(reqMsg);

//         批量预约还款计划查询
        test470020(receiptno_old, receiptno_new,receiptno_transfer);
        //test470020("O111878671938191629256927306", "N111878671938191629256927306");

    }

    public void test470020(String receiptno_old, String receiptno_new, String receiptno_transfer) {

        Map<String, Object> param = new HashMap<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("acctNo", receiptno_old);
        map1.put("enCode", "51030000");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("acctNo", receiptno_new);
        map2.put("enCode", "51030000");
        Map<String, Object> map3 = new HashMap<>();
        map2.put("acctNo", receiptno_transfer);
        map2.put("enCode", "51030000");
        List<Map> list = new ArrayList<>();
        list.add(map1);
        list.add(map2);
        list.add(map3);
        param.put("pkgList", list);
        param.put("brc", "51030000");
        param.put("tranCode", "470020");
        param.put("termDate", "2021-01-01");
        param.put("channelId", "66");
        Map<String, Object> execute = rsf470020.execute(param);
        System.out.println(execute);
    }

    @Test
    public void test470020() {

        Map<String, Object> param = new HashMap<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("acctNo", "JYD211124279057");
        map1.put("enCode", "51030000");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("acctNo", "receiptno_new");
        map2.put("enCode", "51030000");
        Map<String, Object> map3 = new HashMap<>();
        map2.put("acctNo", "receiptno_transfer");
        map2.put("enCode", "51030000");
        List<Map> list = new ArrayList<>();
        list.add(map1);
        list.add(map2);
        list.add(map3);
        param.put("pkgList", list);
        param.put("brc", "51030000");
        param.put("tranCode", "470020");
        param.put("termDate", "2021-01-01");
        param.put("channelId", "66");
        Map<String, Object> execute = rsf470020.execute(param);
        System.out.println(execute);
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

    //状态表 = 4
    @Test
    public void test470020_3() {

        Map<String, Object> param = new HashMap<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("acctNo", "TS033178671938191648692067011");
        map1.put("enCode", "51350000");
        List<Map> list = new ArrayList<>();
        list.add(map1);
        param.put("pkgList", list);
        param.put("brc", "51030000");
        param.put("tranCode", "470020");
        param.put("termDate", "2021-01-01");
        param.put("channelId", "66");
        Map<String, Object> execute = rsf470020.execute(param);
        System.out.println(execute);
    }

    //状态表 = 3
    @Test
    public void test470020_4() {

        Map<String, Object> param = new HashMap<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("acctNo", "TS033178671938191648692025379");
        map1.put("enCode", "51350000");
        List<Map> list = new ArrayList<>();
        list.add(map1);
        param.put("pkgList", list);
        param.put("brc", "51030000");
        param.put("tranCode", "470020");
        param.put("termDate", "2021-01-01");
        param.put("channelId", "66");
        Map<String, Object> execute = rsf470020.execute(param);
        System.out.println(execute);
    }

}

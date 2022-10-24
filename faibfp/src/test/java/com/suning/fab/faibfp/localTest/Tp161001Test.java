package com.suning.fab.faibfp.localTest;


import com.suning.fab.faibfp.service.Rsf161001;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

public class Tp161001Test {

    @Autowired
    Rsf161001 tp161001;

    @Test
    public void test() {
        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        input.put("tranCode", "161001");
        input.put("brc", "51030000");
        input.put("termDate", "2022-10-21");
        input.put("termTime", "120012");
        input.put("channelId", "FC");
        input.put("employeeId", "112233");
        input.put("flowChannel", "UNKNOWN");
        input.put("salesStore", "GL");
        input.put("terminalCode", "PC");
        input.put("bsNo", "PC000000000001");
        input.put("serialNo", "TEST0001" + df.format(new Date()));
        //input.put("serialNo",  "TEST000202209290210821");
        //input.put("serialNo",  "121212");
        input.put("customId", "merchantNo000009");
        input.put("caseNo", "123123123");
        input.put("lawFirm", "2000008");
        input.put("acceptCourt", "00001110");
        input.put("customType", "13");
        List<HashMap<String, Object>> pkgListIn = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> pkgmap1 = new HashMap<String, Object>();
        pkgmap1.put("feeBusiNo", "FC2022102100035");
        pkgmap1.put("feeBusiType", "LSF");
        pkgmap1.put("payorderNo", "payorderNo000001");
        pkgmap1.put("payDate", "2022-09-29");
        pkgmap1.put("invoiceNo", "InvoiceNo000001");
        pkgmap1.put("invoiceCode", "FPCODE001");
        pkgmap1.put("feeReqNo", "feeReqNo00000001");
        pkgmap1.put("openAmt", 100000);
        //pkgmap1.put("feeFunCode", "12");
        pkgListIn.add(pkgmap1);

        input.put("pkgList1", pkgListIn);


        //input.put("acctNo", "PTX200112865644");
        //input.put("acctNo", "PTX200411703751");
        //input.put("acctNo", "XF20061502203285");


        Map<String, Object> ret = tp161001.execute(input);
        System.out.println("@@@=============" + ret);
    }

}








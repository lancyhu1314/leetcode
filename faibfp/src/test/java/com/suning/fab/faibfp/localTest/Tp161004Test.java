package com.suning.fab.faibfp.localTest;


import com.suning.fab.faibfp.service.Rsf161001;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

public class Tp161004Test {

    @Autowired
    Rsf161001 tp161004;

    @Test
    public void test() {
        Map<String, Object> input = new HashMap<String, Object>();
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        input.put("tranCode", "161004");
        input.put("brc", "51030000");
        input.put("termDate", "2022-09-29");
        input.put("termTime", "120012");
        input.put("channelId", "FC");


        List<HashMap<String, Object>> pkgListIn = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> pkgmap1 = new HashMap<String, Object>();
        pkgmap1.put("feeBusiNo", "FC20220929000002");
        pkgmap1.put("feeBusiType", "SSF");

        pkgmap1.put("brc", "51030000");
        pkgListIn.add(pkgmap1);
        HashMap<String, Object> pkgmap2 = new HashMap<String, Object>();
        pkgmap2.put("feeBusiNo", "FC20220929000007");
        pkgmap2.put("feeBusiType", "SSF");

        pkgmap2.put("brc", "51030000");
        pkgListIn.add(pkgmap2);

        input.put("pkgList", pkgListIn);


        //input.put("acctNo", "PTX200112865644");
        //input.put("acctNo", "PTX200411703751");
        //input.put("acctNo", "XF20061502203285");


        Map<String, Object> ret = tp161004.execute(input);
        System.out.println("@@@=============" + ret);
    }

}








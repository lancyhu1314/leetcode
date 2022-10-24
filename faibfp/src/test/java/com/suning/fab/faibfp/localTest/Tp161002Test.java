package com.suning.fab.faibfp.localTest;


import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.faibfp.service.Rsf161001;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.suning.fab.loan.service.*;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.FabException;


/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:applicationContext-test.xml"})
public class Tp161002Test {
	
	@Autowired
	Rsf161001 tp161002;
	
	@Test
	public void test() {
		Map<String, Object> input = new HashMap<String, Object>();
		SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
		input.put("tranCode","161002");
		input.put("brc","51030000");
		input.put("termDate","2022-09-29");
		input.put("termTime","120012");
		input.put("channelId","FC");
		input.put("employeeId", "112233");
		input.put("flowChannel", "UNKNOWN");
		input.put("salesStore", "GL");
		input.put("terminalCode", "PC");
		input.put("bsNo", "FC2022092900018");
		input.put("serialNo",  "TEST0001"+df.format(new Date()));
		//input.put("serialNo",  "TEST000202209290210821");
		//input.put("serialNo",  "121212");
		input.put("feeBusiType", "LSF");
		input.put("feeBusiNo", "FC2022092900022");
		input.put("newfeeFunCode", "2");
		

		
		
		//input.put("acctNo", "PTX200112865644");
		//input.put("acctNo", "PTX200411703751");
		//input.put("acctNo", "XF20061502203285");
		
		
		Map<String, Object> ret = tp161002.execute(input);
		System.out.println("@@@============="+ret);

	}

}








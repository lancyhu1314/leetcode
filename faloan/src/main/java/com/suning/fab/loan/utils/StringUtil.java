package com.suning.fab.loan.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 
 * 字符串处理工具类
 * 
 * @author 88302759
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class StringUtil {
	private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);

	/**
	 * 
	 * 对字符串判空 〈功能详细描述〉
	 * 
	 * @param str
	 * @return
	 * @see [相关类/方法](可选)
	 * @since [产品/模块版本](可选)
	 */
	public static boolean isNotNull(String str) {
		if (null != str && !"".equals(str.trim())) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 
	 * 格式化金额 <br>
	 * 将金额字段格式化为 "1,123,234,345.12" 保留2位小数，没有小数以".00"结尾 且第三位小数不四舍五入进位
	 * 
	 * @param amount
	 * @return
	 * @see [相关类/方法](可选)
	 * @since [产品/模块版本](可选)
	 */
	public static String formatAmount(String amount) {
		if (!isNotNull(amount)) {
			return "";
		}
		BigDecimal b = new BigDecimal(amount);
		DecimalFormat d1 = new DecimalFormat("#,##0.00");
		d1.setRoundingMode(RoundingMode.FLOOR);
		return d1.format(b);
	}

	public static String formatStrIsNull(String str) {
		return isNotNull(str) ? str : "";
	}



	public static String obj2str(Object obj) {
		if (obj == null) {
			return "";
		}
		return ((String) obj).trim();
	}

	public static boolean isNull(String str) {
		return !isNotNull(str);
	}

	public static String firstUpperCase(Object obj) {
		String str = obj2str(obj);
		if (isNull(str)) {
			return "";
		}
		return str.replaceFirst(str.substring(0, 1), str.substring(0, 1).toUpperCase());
	}

	public static String fillupZero(String str, int zeroLen) {
		if (isNull(str)) {
			String fz = "";
			for (int i = 0; i < zeroLen; i++) {
				fz += "0";
			}
			return fz;
		}

		str = str.trim();

		int len = str.length();
		if (len < zeroLen) {
			String s = "";
			for (int i = 0; i < zeroLen - len; i++) {
				s += "0";
			}
			s += str;
			return s;
		}

		if (len > zeroLen) {
			return str.substring(len - zeroLen, len);
		}

		return str;
	}

	/**
	 * 查找 charStr 在 origStr里出现的次数
	 */
	public static int getOccurNumber(String origStr, String charStr) {

		int x = 0;
		// 遍历数组的每个元素
		for (int i = 0; i <= origStr.length() - 1; i++) {
			String getstr = origStr.substring(i, i + 1);
			if (getstr.equals(charStr)) {
				x++;
			}
		}
		return x;
	}

    public static String parseString(Object arg0){
        return arg0 == null ? "" : arg0.toString().trim();
    }

	//获取费用的全部类型
	public static String getAcctStatus (String billtype){
			Field[] fields = ConstantDeclare.ASSISTACCOUNT.class.getDeclaredFields();
			StringBuilder stringBuilder = new StringBuilder();
			for(Field field:fields){
				try {
					String value  = field.get(null).toString();
					stringBuilder.append(",'").append(billtype+"."+value).append("'");
				} catch (IllegalAccessException e) {
					LoggerUtil.error("获取账户形态",e );
				}
			}
			return stringBuilder.substring(1);
	}

	/**
	 * 从字符串类型的隧道中 获取key 对应的value值
	 * @param tunnelData
	 * @param key
	 * @return
	 */
	public static Object getValueFromTunnelData(String tunnelData,String key){
		//判断是否是json格式
		if(StringUtil.isNotNull(tunnelData)&&tunnelData.contains("{")){
			Map tunnelMap= JSONObject.parseObject(tunnelData);
			return tunnelMap.get(key);
		}else{
			return null;
		}
	}
}
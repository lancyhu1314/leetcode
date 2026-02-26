package com.suning.fab.tup4ml.utils;


import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.suning.fab.model.domain.amount.AmountF;
import com.suning.fab.model.domain.amount.AmountY;
import com.suning.fab.tup4ml.elfin.JsonCustomSerializer;
import com.suning.fab.tup4ml.scmconf.ScmDynaGetterUtil;

public abstract class JsonTransferUtil {
	private JsonTransferUtil(){	
		throw new IllegalStateException("JsonTransferUtil class");	
	}
	
	private static Map<Integer, SerializeConfig> configMapping = new HashMap<>();
	
	private static Integer i = 0;
   
	static {
		SerializeConfig mapping = new SerializeConfig();
		mapping.put(AmountF.class, new JsonCustomSerializer(AmountF.class, false, -1));
		mapping.put(AmountY.class, new JsonCustomSerializer(AmountY.class, false, -1));
		mapping.put(java.sql.Date.class, new JsonCustomSerializer(java.sql.Date.class, true, -1));
		configMapping.put(-1, mapping);
		
		mapping = new SerializeConfig();
		mapping.put(AmountF.class, new JsonCustomSerializer(AmountF.class, false, 0));
		mapping.put(AmountY.class, new JsonCustomSerializer(AmountY.class, false, 0));
		mapping.put(java.sql.Date.class, new JsonCustomSerializer(java.sql.Date.class, true, 0));
		mapping.put(Double.class, new JsonCustomSerializer(Double.class, false, 0));
		configMapping.put(0, mapping);
		
		mapping = new SerializeConfig();
		mapping.put(AmountF.class, new JsonCustomSerializer(AmountF.class, false, 1));
		mapping.put(AmountY.class, new JsonCustomSerializer(AmountY.class, false, 1));
		mapping.put(java.sql.Date.class, new JsonCustomSerializer(java.sql.Date.class, true, 1));
		mapping.put(Double.class, new JsonCustomSerializer(Double.class, false, 1));
		configMapping.put(1, mapping);

		mapping = new SerializeConfig();
		mapping.put(java.sql.Date.class, new JsonCustomSerializer(java.sql.Date.class, true, 2));
		mapping.put(java.util.Date.class, new JsonCustomSerializer(java.util.Date.class, true, 2));
		configMapping.put(2, mapping);
	}
	
	/**
	 * 把对象转成json字符串，对Amount类特殊处理直接转成金额数值
	 * 注意：改方法不涉及Amount类与Double类型之间的单位转换，
	 * Double类型按Double转成Json，Float类型按Float转成Json。
	 * @param obj
	 * @return
	 */
	public static String toJsonWithOutSecret(Object obj){
		if (i<1) {
			String rule = ScmDynaGetterUtil.getWithDefaultValue("GlobalScm.properties", "logRuleOut", "");
			LoggerUtil.info("Get logRuleOut :{}",rule);
			LogRuleOutUtil.setLogFilter(rule);
			i= 10;
		}

		return JSONObject.toJSONString(obj, LogRuleOutUtil.getFilter());
	}
	/**
	 * 把对象转成json字符串，对Amount类特殊处理直接转成金额数值
	 * 注意：改方法不涉及Amount类与Double类型之间的单位转换，
	 * Double类型按Double转成Json，Float类型按Float转成Json。
	 * @param obj
	 * @return
	 */
	public static String toJson(Object obj){
		JSONSerializer serializerTemp = new JSONSerializer(
				new SerializeWriter((Writer) null, JSON.DEFAULT_GENERATE_FEATURE, 
						SerializerFeature.IgnoreErrorGetter, 
						SerializerFeature.IgnoreNonFieldGetter,
						SerializerFeature.WriteDateUseDateFormat), 
				configMapping.get(-1));
		serializerTemp.write(obj);
		return serializerTemp.toString();
	}
	

	public static String toJsonWithDateFormat(Object obj){
		JSONSerializer serializerTemp = new JSONSerializer(
				new SerializeWriter((Writer) null, JSON.DEFAULT_GENERATE_FEATURE, 
						SerializerFeature.IgnoreErrorGetter, 
						SerializerFeature.IgnoreNonFieldGetter,
						SerializerFeature.WriteDateUseDateFormat), 
				configMapping.get(2));
		serializerTemp.write(obj);
		return serializerTemp.toString();
	}
	
	/**
	 * 把对象转成json字符串，对Amount类特殊处理直接转成以分为单位的金额数值
	 * 注意：改方法涉及Amount类与Double类型之间的单位转换，默认Double的单位为元！
	 * 所有的Double类型转成分单位的数值，Float类型按Float转成Json。
	 * @param obj
	 * @return
	 */
	public static String toJsonWithFen(Object obj){
		JSONSerializer serializerTemp = new JSONSerializer(
				new SerializeWriter((Writer) null, JSON.DEFAULT_GENERATE_FEATURE, 
						SerializerFeature.IgnoreErrorGetter, 
						SerializerFeature.IgnoreNonFieldGetter,
						SerializerFeature.WriteDateUseDateFormat), 
				configMapping.get(0));
		serializerTemp.write(obj);
		return serializerTemp.toString();
	}
	
	/**
	 * 把对象转成json字符串，对Amount类特殊处理直接转成以元为单位的金额数值
	 * 注意：改方法涉及Amount类与Double类型之间的单位转换，默认Double的单位为元！
	 * 所有的Double类型转成元单位的数值，Float类型按Float转成Json。
	 * @param obj
	 * @return
	 */
	public static String toJsonWithYuan(Object obj){
		JSONSerializer serializerTemp = new JSONSerializer(
				new SerializeWriter((Writer) null, JSON.DEFAULT_GENERATE_FEATURE, 
						SerializerFeature.IgnoreErrorGetter, 
						SerializerFeature.IgnoreNonFieldGetter,
						SerializerFeature.WriteDateUseDateFormat), 
				configMapping.get(1));
		serializerTemp.write(obj);
		return serializerTemp.toString();
	}
}


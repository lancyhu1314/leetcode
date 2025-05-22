package com.suning.fab.tup4ml.elfin;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.JavaBeanSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.suning.fab.model.domain.amount.AmountF;
import com.suning.fab.model.domain.amount.AmountY;
import com.suning.fab.tup4ml.utils.DateUtil;

public class JsonCustomSerializer extends JavaBeanSerializer {
	private Method toStringMethod = null;//toString()方法类，不带参数！
	private Boolean quotesFlag = false;//自定义输出是否加引号：true -- 加引号；false -- 不加引号；
	private Integer unitFlag = -1;//转出标识：其他 -- 未定义；0 -- 分；1 -- 元；2 -- 日期（yyyy-MM-dd）；
	private static final String METHODNAMEPREFIX = "toString";

	public JsonCustomSerializer(Class<?> clazz, Boolean quotesFlag, Integer unitFlag){
		this(clazz);
		this.quotesFlag = quotesFlag;
		this.unitFlag = unitFlag;
	}
	public JsonCustomSerializer(Class<?> clazz){
		super(clazz);
		for (Method method : clazz.getMethods()) {
			String methodName = method.getName();
			if (methodName.startsWith(METHODNAMEPREFIX)) {
				toStringMethod = method;
				break;
			}
		}
	}

	public JsonCustomSerializer(Class<?> clazz, String... aliasList){
		super(clazz, aliasList);
		for (Method method : clazz.getMethods()) {
			String methodName = method.getName();
			if (methodName.startsWith(METHODNAMEPREFIX)) {
				toStringMethod = method;
				break;
			}
		}
	}

	public JsonCustomSerializer(Class<?> clazz, Map<String, String> aliasMap){
		super(clazz, aliasMap);
		for (Method method : clazz.getMethods()) {
			String methodName = method.getName();
			if (methodName.startsWith(METHODNAMEPREFIX)) {
				toStringMethod = method;
				break;
			}
		}
	}

	@Override
	public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType,  int features) throws IOException {
		SerializeWriter out = serializer.getWriter();
		try {
			String  toString = " ";
			if(1 == unitFlag){//转出单位标识：1 -- 元；
				if(AmountF.class.isAssignableFrom(object.getClass())){
					AmountF amtF = (AmountF)object;
					toString = amtF.convert2Y().toString();
				}else if(Double.class.isAssignableFrom(object.getClass())){//默认Double的单位都是元！
					@SuppressWarnings("deprecation")
					AmountY amtY = new AmountY((Double)object);
					toString = amtY.toString();
				}else{
					if(null != toStringMethod){
						toString = toStringMethod.invoke(object).toString();
					}
				}
			}else if(0 == unitFlag){//转出单位标识：0 -- 分；
				if(AmountY.class.isAssignableFrom(object.getClass())){
					AmountY amtY = (AmountY)object;
					toString = amtY.toStringF();
				}else if(Double.class.isAssignableFrom(object.getClass())){//默认Double的单位都是元！
					@SuppressWarnings("deprecation")
					AmountY amtY = new AmountY((Double)object);
					toString = amtY.toStringF();
				}else{
					if(null != toStringMethod){
						toString = toStringMethod.invoke(object).toString();
					}
				}
			}else if(2 == unitFlag){//转出标识：2 -- 日期格式：yyyy-MM-dd
				if(java.util.Date.class.isAssignableFrom(object.getClass())){
					toString = DateUtil.dateToString((java.util.Date)object);
				}
			}else{//转出单位标识：其他 -- 未定义；
				if(null != toStringMethod){
					toString = toStringMethod.invoke(object).toString();
				}
			}
			if(quotesFlag == true){
				out.append("\"");
				out.append(toString);
				out.append("\"");
			}else{
				out.append(toString);
			}
		} catch (Exception e) {
			throw new JSONException("write JSONSerializer error", e);
		}
	}
}

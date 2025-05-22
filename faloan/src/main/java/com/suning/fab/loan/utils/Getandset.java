package com.suning.fab.loan.utils;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.LoggerUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Date;

public class Getandset {

	private Class<?> clazz;
	private Method[] methods;

	public void setClassparam(Object obj) {
		this.clazz = obj.getClass();
		this.methods = clazz.getDeclaredMethods();
	}

	private Object retType(Type arg){
		Class<?> type;
		try {
			type = (Class) arg;
		}catch(ClassCastException e){
            LoggerUtil.info("ClassCastException",e);
            type =  arg.getClass();
		}
		if(type.isAssignableFrom(String.class)){
			return "";
		}else if(type.isAssignableFrom(Integer.class)||type.isAssignableFrom(int.class)){
			return 0;
		}else if(type.isAssignableFrom(Boolean.class)||type.isAssignableFrom(boolean.class)){
			return true;
		}else if(type.isAssignableFrom(Double.class)||type.isAssignableFrom(double.class)){
			return 0.00;
		}else if(type.isAssignableFrom(BigDecimal.class)){
			return BigDecimal.valueOf(0.0);
		}else if(type.isAssignableFrom(Date.class)){
			return new Date(System.currentTimeMillis());
		}else if(type.isAssignableFrom(FabAmount.class)){
			return new FabAmount(1.00);
		}else if(type.isAssignableFrom(ListMap.class)){
			return new ListMap();
		}
		return null;
	}

	private void gsDemo(Object obj) {
		for (int i = 0; i < methods.length; i++) {
			String name = methods[i].getName();
			try {
				if (name.startsWith("get")) {
					Type[] typelist = methods[i].getGenericParameterTypes();
					if(typelist.length==0)
						methods[i].invoke(obj);
				} else if (name.startsWith("set")) {
					Type[] typelist = methods[i].getGenericParameterTypes();
					methods[i].invoke(obj, retType(typelist[0]));
				}
			}  catch (IllegalAccessException e) {
				LoggerUtil.info("Getandset IllegalAccessException at "+obj.getClass()+"   "+name,e);
			} catch (IllegalArgumentException e) {
				LoggerUtil.info("Getandset IllegalArgumentException at "+obj.getClass()+"   "+name,e);
			} catch (InvocationTargetException e) {
				LoggerUtil.info("Getandset InvocationTargetException at "+obj.getClass()+"   "+name,e);
			}
		}
	}

	public static void getsetCall(Object obj){
		Getandset getandset = new Getandset();
		getandset.setClassparam(obj);
		getandset.gsDemo(obj);
	}

}

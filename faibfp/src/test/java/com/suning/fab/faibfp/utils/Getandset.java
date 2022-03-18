/**
 * @author 16071579
 * @version 创建时间：2017年4月25日 上午11:19:39
 * <p>Title: Getandset</p>
 * <p>Description: </p>
 * <p>Company: Suning</p>
 */
package com.suning.fab.faibfp.utils;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Getandset {
	
	private Class<?> clazz;
	private Method[] methods;
	
	public void setClassparam(Object obj) {
		this.clazz = obj.getClass();
		this.methods = clazz.getDeclaredMethods();
	}
	
	public Object retType(String type){
		if(type.equals("class java.lang.String")){
			return "";
		}else if(type.equals("class java.lang.Integer")){
			return 0;
		}else if(type.equals("class java.lang.Double")){
			return 0.00;
		}else if(type.equals("class java.sql.Date")){
			return new Date(System.currentTimeMillis());
		}
		return null;
	}
	
	public void gsDemo(Object obj) {
		for (int i = 0; i < methods.length; i++) {
			String name = methods[i].getName();
			try {
				if (name.substring(0, 3).equals("get")) {
					methods[i].invoke(obj);
				} else if (name.substring(0, 3).equals("set")) {
					Type[] typelist = methods[i].getGenericParameterTypes();
					List<Object> list = new ArrayList<Object>();
					list.add(null);
					methods[i].invoke(obj, list.toArray());
					methods[i].invoke(obj, retType(typelist[0].toString()));
				}
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {
			} catch (InvocationTargetException e) {
			}
		}
	}
	
	public static void getsetCall(Object obj){
		Getandset getandset = new Getandset();
		getandset.setClassparam(obj);
		getandset.gsDemo(obj);
	}

}

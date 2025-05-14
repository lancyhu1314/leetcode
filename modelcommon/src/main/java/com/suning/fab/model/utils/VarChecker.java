/**
* @author 14050269 Howard
* @version 创建时间：2016年7月12日 下午6:06:20
* 类说明
*/
package com.suning.fab.model.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.suning.framework.sedis.ReflectionUtils;

public class VarChecker {
	private VarChecker() {
		throw new IllegalStateException("VarChecker class");
	}
	
	/*
	 * 判断工具类
	 * 用法:VarChecker.asList(opt1,opt2,opt3...).contains(thevalue);
	 * 用于判断变量thevalue是否在opt1,opt2,opt3中
	 */
	public static <T> List<T> asList(@SuppressWarnings("unchecked") T... args)
	{
		return Arrays.asList(args);
	}
	
	/**
	 * 验证变量是否为空(null或"")
	 * @param x 要验证的变量
	 * @return 如果变量是null或者""(仅对String有效)则返回true，否则false
	 */
	public static boolean isEmpty(Object x)
	{
		if(x==null)
		{
			return true;
		}
		if(String.class.isInstance(x))
		{
			return x.toString().isEmpty();
		}
		return false;
		
	}
	
	/**
	 * 用于访问在一个Const类里的多个成员
	 * @param clazz　类
	 * @param fields 不定参数，可以1到多个，每一个表示一个变量 名
	 * @return 返回一个数组，其中的值和fields中的变量名一一对应
	 * 例子：
	 * Object[] arr = VarChecker.withinConst(PlatConstant.CURRENCY.EDIFICE.class, "EPP","FAB");
	 * 注:返回的数组是可以做为asList的参数用的
	 */
	public static Object[] withinConst(Class<?> clazz,String...fields)
	{
		if(fields.length==0) return null;
		List<Object> arr= new ArrayList<Object>();
		for(String f:fields)
		{
			Field field = ReflectionUtils.findField(clazz, f);
			try {
				Object x = field.get(null);
				arr.add(x);
			} 
			catch (Exception e)
			{
				continue;
			}
		}
		
		return arr.toArray();
		
	}
	
	/**
	 * 判定的变量在Constant class中是否存在
	 * @param value 要判定的变量
	 * @param clazz Constant class
	 * @return	true:存在(有效) false:不存在(无效)
	 */
	public static boolean isValidConstOption(Object value,Class<?> clazz)
	{
		if(isEmpty(value)) return false;
		Field[] fields = clazz.getDeclaredFields();
		for(Field f: fields)
		{
				try {
					if(f.get(null).equals(value))
					{
						return true;
					}
				} 
				catch (Exception e)
				{
					continue;
				}
		}
		return false;
	}
	
}

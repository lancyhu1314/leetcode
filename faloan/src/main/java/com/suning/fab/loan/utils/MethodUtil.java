package com.suning.fab.loan.utils;

import java.lang.reflect.Method;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.utils.PlatConstant;

public class MethodUtil {
	
	
	
	public MethodUtil() {
		//do nothing
	}

	/**
	 * 封装了泛型Class的getMethod方法
	 * @param destClass 包含所需方法的类
	 * @param methodName 方法名
	 * @param parameterTypes 方法参数类型列表
	 * @return 返回对应Method
	 * @throws FabException
	 */
	public static Method getMethod(Class<?> destClass, String methodName, Class<?>... parameterTypes) throws FabException{
		try{
			return destClass.getMethod(methodName, parameterTypes);			
		}catch(Exception e){
			throw new FabException(e, PlatConstant.RSPCODE.UNKNOWN);
		}
	}
	
	/**
	 * 封装了Method的invoke方法
	 * @param instance 类实例
	 * @param m Method类
	 * @param args 参数值列表
	 * @return 返回方法调用的返回值
	 * @throws FabException
	 */
	public static Object methodInvoke(Object instance, Method m, Object... args) throws FabException{
		try{	
			return m.invoke(instance, args);
		}catch(Exception e){
			throw new FabException(e, PlatConstant.RSPCODE.UNKNOWN);
		}
	}
}

package com.suning.fab.tup4ml.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * 
 * 注解工具类<br> 
 *
 * @author 17060915
 * @since 2017年12月21日上午11:22:09
 */
public abstract class AnotUtil {
	private AnotUtil() {
		throw new IllegalStateException("AnotUtil class");
	}

	/**
	 * 
	 * 功能描述: <br>
	 *  查询该类中是否存在该注解
	 *  
	 * @param clazz 类类型
	 * @param annotationType  注解类型
	 * @return
	 */
	public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
		Assert.notNull(clazz, "Class must not be null");
		A annotation = clazz.getAnnotation(annotationType);
		if (annotation != null) {
			return annotation;
		}
		Class<?> superClass = clazz.getSuperclass();
		if (superClass == null || superClass == Object.class) {
			return null;
		}
		return findAnnotation(superClass, annotationType);
	}

	/**
	 * 
	 * 功能描述: <br>
	 *  查询该类中是否存在有该注解类型的方法
	 *  
	 * @param clazz 类类型
	 * @param annotationType  注解类型
	 * @return
	 */
	public static <A extends Annotation> A findAnnotationFromMethod(Class<?> clazz, Class<A> annotationType) {
		Assert.notNull(clazz, "Class must not be null");
		Method[] methods = clazz.getDeclaredMethods();

		A annotation = null;
		for(Method method : methods){
			annotation = method.getAnnotation(annotationType);
			if(null != annotation){
				return annotation;
			}
		}

		Class<?> superClass = clazz.getSuperclass();
		if (superClass == null || superClass == Object.class) {
			return null;
		}
		return findAnnotationFromMethod(superClass, annotationType);
	}

	/**
	 * 
	 * 功能描述: <br>
	 * 获取注解的所有属性
	 *
	 * @param annotation 注解类
	 * @param classValuesAsString 是否将class类型的属性值转为string类型
	 * @return
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation, boolean classValuesAsString) {
		return AnnotationUtils.getAnnotationAttributes(annotation, classValuesAsString, false);
	}

}

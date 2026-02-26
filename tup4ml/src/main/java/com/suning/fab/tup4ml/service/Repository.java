package com.suning.fab.tup4ml.service;

import java.util.ArrayList;
import java.util.List;

import com.suning.fab.model.domain.entity.IBaseDao;

/**
 * @author 16030888
 * @version 1.0
 * @since 2017-12-09 20:36:56
 */
abstract class Repository {

	/**
	 * 要保存或操作的项；
	 */
	private static ThreadLocal<List<IBaseDao>> daoStack = new ThreadLocal<List<IBaseDao>>();
	
	private Repository(){
		throw new IllegalStateException("Repository class");
	}
	
	/**
	 * 加入仓库操作；
	 * @param item DB的IO操作项
	 */
	public static boolean push(IBaseDao item){
		List<IBaseDao> items = daoStack.get();
		if (items == null){
			items = new ArrayList<>();
			daoStack.set(items);
		}
		items.add(item);
		return true;
	}

	/**
	 * 获取线程本地的队列；
	 * @return the items
	 */
	public static List<IBaseDao> getItems() {
		return daoStack.get();
	}
	
	/**
	 * 清空本次线程相关的数据；
	 */
	public static void cleanItems() {
		daoStack.set(null);
	}

}

package com.suning.fab.tup4ml.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.suning.fab.tup4ml.scmconf.ScmDynaGetterUtil;

/**
 * 提供一个线程池执行一些后台任务；
 * @author 16030888
 *
 */
public abstract class ThreadPoolUtil {
	private ThreadPoolUtil() {
		throw new IllegalStateException("ThreadPoolUtil class");
	}

	private static Boolean bSynExecutorService = false;
	private static ThreadPoolExecutor poolExecutor = null;

	public static void setPoolSize(Integer size){
		if(size < 1)
			return;
		if(null != poolExecutor){
			if(poolExecutor.getMaximumPoolSize() != size)
				poolExecutor.setMaximumPoolSize(size);
			if(poolExecutor.getCorePoolSize() != size)
				poolExecutor.setCorePoolSize(size);
		}
	}

	static public void execute(Runnable arg0) {

		//这里实现延迟初始化，以便等待相关条件能完成初始化
		if(null == poolExecutor){
			Integer threadPoolMaxSize = Integer.valueOf(
					ScmDynaGetterUtil.getWithDefaultValue("GlobalScm.properties", "ThreadPoolMaxSize", "8")
					);
			if(threadPoolMaxSize < 1)
				threadPoolMaxSize = 8;

			Integer threadPoolBlockQueueSize = Integer.valueOf(
			        ScmDynaGetterUtil.getWithDefaultValue("GlobalScm.properties", "ThreadPoolBlockQueueSize", "200")
					);
			if(threadPoolBlockQueueSize < 1)
				threadPoolBlockQueueSize = 200;

			synchronized(bSynExecutorService){
				if(null == poolExecutor){
					poolExecutor = new ThreadPoolExecutor(threadPoolMaxSize, threadPoolMaxSize,
							0L, TimeUnit.MILLISECONDS,
							new LinkedBlockingQueue<Runnable>(threadPoolBlockQueueSize));
				}
			}		
		}

		try {
			poolExecutor.execute(arg0);
		}catch(Exception e) {
			LoggerUtil.error("ThreadPoolUtil 异常：{}", e);
		}
	}	
}

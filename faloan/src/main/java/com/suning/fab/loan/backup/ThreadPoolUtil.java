
package com.suning.fab.loan.backup;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import com.suning.fab.tup4j.utils.LoggerUtil;

@Service
public class ThreadPoolUtil implements InitializingBean {

	private static ThreadPoolExecutor s = null;

	public static void setAsyncCallPoolSize(Integer size){
		if(size < 1)
			return;
		if(null != s){
			if(s.getMaximumPoolSize() != size)
				s.setMaximumPoolSize(size);
			if(s.getCorePoolSize() != size)
				s.setCorePoolSize(size);
		}
	}

	public static synchronized void initThreadPool(){
		Integer threadPoolMaxSize = Integer.valueOf(
				ScmProtiesUtil.getProperty("ThreadPoolMaxSize", "10")
				);
		if(threadPoolMaxSize < 1)
			threadPoolMaxSize = 10;

		Integer threadPoolBlockQueueSize = Integer.valueOf(
				ScmProtiesUtil.getProperty("ThreadPoolBlockQueueSize", "500")
				);
		if(threadPoolBlockQueueSize < 1)
			threadPoolBlockQueueSize = 500;

		if(null == s){
			s = new ThreadPoolExecutor(threadPoolMaxSize, threadPoolMaxSize,
					0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(threadPoolBlockQueueSize),
					new ThreadPoolRejectedExecutionHandler());
		}
	}

	public static void execute(Runnable arg0) {
		if(null == s){
			initThreadPool();
		}
		s.execute(arg0);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(null == s){
			initThreadPool();
		}
		LoggerUtil.info("线程池初始化完成！");
	}	
}

package com.suning.fab.loan.backup;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import com.suning.fab.tup4j.utils.LoggerUtil;

public class ThreadPoolRejectedExecutionHandler implements RejectedExecutionHandler {

	public ThreadPoolRejectedExecutionHandler() {
		super();
	}

	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		LoggerUtil.info("线程队列已满，拒绝新线程加入，队列大小为：{}", executor.getQueue().size());
	}

}

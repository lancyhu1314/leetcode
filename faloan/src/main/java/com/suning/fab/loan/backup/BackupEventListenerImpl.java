package com.suning.fab.loan.backup;

import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.ServiceFactory;

public class BackupEventListenerImpl implements BackupEventListener {

	@Override
	public void serializable(BackupEvent be){
		SerializableImpl serializable = new SerializableImpl();
		try {
			be.getBackupData().getData().put(be.getBackupData().getTablename(), 
					serializable.serialize(be.getBackupData().getObj()));
		} catch (Exception e) {
			LoggerUtil.error("BackupData对象序列化失败{}", e);
		}
	}

	@Override
	public void backup(final BackupEvent be){

		ThreadPoolUtil.execute(new Runnable() {
			@Override
			public void run() {
				ServiceFactory.getBean(HbaseOperateImpl.class).singleInsertData(be);
			}
		});
	}
	
}

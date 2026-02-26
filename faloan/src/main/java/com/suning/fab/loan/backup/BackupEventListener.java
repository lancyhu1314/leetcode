package com.suning.fab.loan.backup;

import java.util.EventListener;

public interface BackupEventListener extends EventListener {

	public void serializable(BackupEvent be);

	public void backup(BackupEvent be);

}

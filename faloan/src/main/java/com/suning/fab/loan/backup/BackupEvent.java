package com.suning.fab.loan.backup;

import java.util.EventObject;

public class BackupEvent extends EventObject {

	private static final long serialVersionUID = 1L;

	private transient BackupManager backupManager;

	public BackupEvent(Object source) {
		super(source);
		this.backupManager = (BackupManager)source;
	}

	public BackupManager getBackupData() {
		return backupManager;
	}

	public void setBackupData(BackupManager backupData) {
		this.backupManager = backupData;
	}



}

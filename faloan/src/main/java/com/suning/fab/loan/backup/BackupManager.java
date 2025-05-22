package com.suning.fab.loan.backup;

import java.util.HashMap;
import java.util.Map;

import com.suning.fab.tup4j.base.TranCtx;

public class BackupManager{

	private String tablename;

	private Object obj;

	private TranCtx ctx;

	private Map<String, byte[]> data;

	private String acctno;

	private BackupEventListener eventListener;


	public BackupManager(TranCtx ctx,String acctno) {
		this.data = new HashMap<String, byte[]>();
		this.ctx = ctx;
		this.acctno = acctno;
	}

	/**
	 * 接受数据
	 * @param ctx
	 * @param obj
	 * @throws Exception 
	 */
	public void accept(String tablename, Object obj)
	{
		this.tablename = tablename;
		this.obj = obj;

		if (eventListener != null)
		{
			this.eventListener.serializable(new BackupEvent(this));
		}

	}

	/**
	 * 保存
	 * @throws Exception 
	 */
	public void backup()
	{
		if (eventListener != null)
		{
			this.eventListener.backup(new BackupEvent(this));

		}
	}

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}

	public TranCtx getCtx() {
		return ctx;
	}

	public void setCtx(TranCtx ctx) {
		this.ctx = ctx;
	}

	public Map<String, byte[]> getData() {
		return data;
	}

	public void setData(Map<String, byte[]> data) {
		this.data = data;
	}

	public BackupEventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(BackupEventListener eventListener) {
		this.eventListener = eventListener;
	}

	public String getAcctno() {
		return acctno;
	}

	public void setAcctno(String acctno) {
		this.acctno = acctno;
	}

	public String getTablename() {
		return tablename;
	}


	public void setTablename(String tablename) {
		this.tablename = tablename;
	}


}

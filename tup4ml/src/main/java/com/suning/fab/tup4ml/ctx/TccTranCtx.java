package com.suning.fab.tup4ml.ctx;

import java.util.Map;

import com.suning.dtf.client.interceptor.TccTransactionContext;

/**
 * TCC服务交易上下文；
 * @author 16030888
 *
 */
public class TccTranCtx extends TranCtx {
	private static final long serialVersionUID = 1L;

	/**
	 * TCC事务上下文；
	 */
	protected TccTransactionContext tc;
	
	private Map<String, String> exceptionCode;
	
	/**
	 * 是否忽略该次调用的异常，继续执行TCC的两阶段提交；
	 */
	protected Boolean ignore;

	public TccTranCtx(){
		//nothing
	}

	public TccTransactionContext getTc() {
		return tc;
	}

	public Boolean getIgnore() {
		return ignore;
	}

	public void setTc(TccTransactionContext tc) {
		this.tc = tc;
	}

	public void setIgnore(Boolean ignore) {
		this.ignore = ignore;
	}

    /**  
     * 获取exceptionCode  
     * @return exceptionCode  
     */
    public Map<String, String> getExceptionCode() {
        return exceptionCode;
    }

    /**  
     * 设置exceptionCode  
     * @param exceptionCode  
     */
    public void setExceptionCode(Map<String, String> exceptionCode) {
        this.exceptionCode = exceptionCode;
    }
	
}

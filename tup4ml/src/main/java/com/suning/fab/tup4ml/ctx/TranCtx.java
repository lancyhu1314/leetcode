package com.suning.fab.tup4ml.ctx;

import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;
import com.suning.fab.model.common.IContext;

/**
 * 本次交易的上下文基类；
 * @author 16030888
 *
 */
public abstract class TranCtx implements IContext {
	private static final long serialVersionUID = 1L;

	/**
	 * 内部UUID，业务流水号；
	 */
	protected String bid;

	/**
	 * 请求来源；
	 */
	protected String srcSystem;

	/**
	 * 交易日期；
	 */
	@JSONField(format="yyyy-MM-dd")
	protected Date tranDate;

	/**
	 * 子业务序号；
	 */
	Integer subSeq = 0;
	
	/**
	 * 初始子业务序号
	 */
	Integer initSubSeq = 0;

	public TranCtx(){
		initSubSeq = subSeq;
	}

	public String getSrcSystem() {
		return srcSystem;
	}

	public void setSrcSystem(String srcSystem) {
		this.srcSystem = srcSystem;
	}

	public void setTranDate(Date tranDate) {
		this.tranDate = tranDate;
	}

	@Override
	public Date getTranDate() {
		return tranDate;
	}

	@Override
	public String getBid() {
		return bid;
	}

	@Override
	public void setBid(String bid) {
		this.bid = bid;
	}

	/**
	 * 获取交易子序号；
	 */
	@Override
	public Integer getSubSeq() {
		return subSeq;
	}

	/**
	 * 设置交易子序号；
	 */
	@Override
	public void setSubSeq(Integer subSeq) {
		this.subSeq = subSeq;
	}

	public Integer getInitSubSeq() {
		return initSubSeq;
	}

	public void setInitSubSeq(Integer initSubSeq) {
		this.initSubSeq = initSubSeq;
	}

}

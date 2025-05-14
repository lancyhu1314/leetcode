package com.suning.fab.model.domain.protocal;

import java.io.Serializable;
import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;
import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.model.constant.CommonConstant;
import com.suning.fab.model.utils.VarChecker;

/**
 * 响应报文公共字段
 * @author 16030888
 *
 */
public class ExitBusinessCommon extends AbstractDatagram implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 交易日期，格式：yyyy-MM-dd HH:mm:ss；
	 */
	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	protected Date tranDate;
	
	/**
	 * 主机流水号，账务交易返回记账流水号；
	 */
	protected String serSeqNo;
	
	/**
	 * 响应代码：000000 -- 交易成功；
	 */
	protected String rspCode;
	
	/**
	 * 响应信息；
	 */
	protected String rspMsg;

	public ExitBusinessCommon(){
		//不做任何操作
	}

	public ExitBusinessCommon(String serSeqNo, Date tranDate, String rspCode, String rspMsg){
		if(VarChecker.isEmpty(rspCode) 
				|| VarChecker.isEmpty(rspMsg)){
			throw new IllegalArgumentException(CommonConstant.ExceptionConstant.ARGUMENTISNULL);
		}
		this.serSeqNo = serSeqNo;
		this.tranDate = tranDate;
		this.rspCode = rspCode;
		this.rspMsg = rspMsg;
	}

	@Override
	public boolean validate() {
		return !(VarChecker.isEmpty(tranDate) 
				|| VarChecker.isEmpty(serSeqNo) 
				|| VarChecker.isEmpty(rspCode) 
				|| VarChecker.isEmpty(rspMsg));
	}
	
	public Date getTranDate() {
		return tranDate;
	}

	public String getSerSeqNo() {
		return serSeqNo;
	}

	public String getRspCode() {
		return rspCode;
	}

	public String getRspMsg() {
		return rspMsg;
	}

	public void setTranTimeStamp(Date tranDate) {
		this.tranDate = tranDate;
	}

	public void setSerSeqNo(String serSeqNo) {
		this.serSeqNo = serSeqNo;
	}

	public void setRspCode(String rspCode) {
		this.rspCode = rspCode;
	}

	public void setRspMsg(String rspMsg) {
		this.rspMsg = rspMsg;
	}

	@Override
	public String getRouteId() {
		return null;
	}

	@Override
	public String getProtocalHashCode() {
		return null;
	}

	@Override
	public String getOuterSerialNumber() {
		return null;
	}

	@Override
	public Date getRequestDate() {
		return null;
	}
}

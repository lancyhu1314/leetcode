package com.suning.fab.tup4ml.db;

import java.util.Date;
import java.util.HashMap;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.entity.AbstractBaseDao;

/**
 * 报文登记表处理类
 * @author 16030888
 *
 */
public class ProtoRegHandler extends AbstractBaseDao {
	//路由ID，通常是账户
	private String userno;

	//业务流水号
	private String serseqno;

	//交易日期
	private Date trandate;
	
	//业务子序号
	private Integer hops = 0;

	//外部业务流水号
	private String serialno;

	//请求报文blob数据
	private AbstractDatagram request;

	//响应报文blob数据
	private AbstractDatagram response;

	public String getUserno() {
		return userno;
	}

	public Date getTrandate() {
		return trandate;
	}

	public void setUserno(String userno) {
		this.userno = userno;
	}

	public void setTrandate(Date trandate) {
		this.trandate = trandate;
	}

	public String getSerseqno() {
		return serseqno;
	}

	public void setSerseqno(String serseqno) {
		this.serseqno = serseqno;
	}

	public String getSerialno() {
		return serialno;
	}

	public AbstractDatagram getRequest() {
		return request;
	}

	public AbstractDatagram getResponse() {
		return response;
	}

	public void setSerialno(String serialno) {
		this.serialno = serialno;
	}

	public void setRequest(AbstractDatagram request) {
		this.request = request;
	}

	public void setResponse(AbstractDatagram response) {
		this.response = response;
	}

	public Integer getHops() {
		return hops;
	}

	public void setHops(Integer hops) {
		this.hops = hops;
	}

	public ProtoRegHandler() {
		//do nothing
	}

	@Override
	public void save() {
		HashMap<String, Object> param = new HashMap<>();
		param.put("userno", userno);
		param.put("serseqno", serseqno);
		param.put("trandate", trandate);
		param.put("hops", hops);
		param.put("serialno", serialno);
		param.put("request", request);
		param.put("response", response);
		this.insert("TUPCOMMONCUSTOM.protoInsert", param);
	}
	
	public void update() {
		HashMap<String, Object> param = new HashMap<>();	
		param.put("response", response);
		param.put("userno", userno);
		param.put("serseqno", serseqno);
		param.put("trandate", trandate);
		param.put("hops", hops);
		this.update("TUPCOMMONCUSTOM.protoUpdate", param);
	}
	
	@Override
	public void load() {
	}

}

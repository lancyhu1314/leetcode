package com.suning.fab.tup4ml.db;

import java.util.Date;
import java.util.HashMap;

import com.suning.fab.tup4ml.entity.AbstractBaseDao;

/**
 * 幂等表处理类
 * @author 16030888
 *
 */
public class IdempotencyCtrlHandler extends AbstractBaseDao {
	//账户
	private String userno;

	//业务流水号
	private String serseqno;

	//交易日期
	private Date trandate;

	//业务报文幂等hashCode代码
	private String hashcode;

	@Override
	public void save() {
		HashMap<String, Object> param = new HashMap<>();
		param.put("userno", userno);
		param.put("serseqno", serseqno);
		param.put("trandate", trandate);
		param.put("hashcode", hashcode);
		this.insert("TUPCOMMONCUSTOM.idempotencyInsert", param);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void load() {
		HashMap<String, Object> param = new HashMap<>();
		param.put("userno", userno);
		param.put("hashcode", hashcode);
		HashMap<String, Object> ret = (HashMap<String, Object>)this.selectOne("TUPCOMMONCUSTOM.idempotencySelect", param);
		
		if(ret != null){
		    serseqno = (String) ret.get("SERSEQNO");
		    trandate = (Date) ret.get("TRANDATE");
		}
	}

	public String getUserno() {
		return userno;
	}

	public Date getTrandate() {
		return trandate;
	}

	public String getHashcode() {
		return hashcode;
	}

	public void setUserno(String userno) {
		this.userno = userno;
	}

	public void setTrandate(Date trandate) {
		this.trandate = trandate;
	}

	public void setHashcode(String hashcode) {
		this.hashcode = hashcode;
	}

	public String getSerseqno() {
		return serseqno;
	}

	public void setSerseqno(String serseqno) {
		this.serseqno = serseqno;
	}

}

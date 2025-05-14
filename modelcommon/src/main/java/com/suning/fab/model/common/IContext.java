package com.suning.fab.model.common;

import java.io.Serializable;
import java.util.Date;

public interface IContext extends Serializable {
	public Date getTranDate();
	public void setTranDate(Date tranDate);
	public String getBid();
	public void setBid(String bid);
	public Integer getSubSeq();
	public void setSubSeq(Integer subseq);
}

package com.suning.fab.tup4ml.db;

import com.suning.fab.model.common.AbstractDatagram;

class ProtoRegJsonMaker {
	private String businessClassName;
	private AbstractDatagram businessClass;
	
	public ProtoRegJsonMaker() {
		//do nothing
	}
	public String getBusinessClassName() {
		return businessClassName;
	}
	public AbstractDatagram getBusinessClass() {
		return businessClass;
	}
	public void setBusinessClassName(String businessClassName) {
		this.businessClassName = businessClassName;
	}
	public void setBusinessClass(AbstractDatagram businessClass) {
		this.businessClass = businessClass;
	}
	
}

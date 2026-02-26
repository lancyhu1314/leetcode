package com.suning.fab.tup4ml.service;

import com.suning.fab.model.common.AbstractDatagram;
import com.suning.fab.tup4ml.exception.FabException;

interface IFabEntry {
	public AbstractDatagram prepare(AbstractDatagram param) throws FabException;
	public default AbstractDatagram confirm(AbstractDatagram param) throws FabException{
		return null;
	}
	public default AbstractDatagram cancel(AbstractDatagram param) throws FabException{
		return null;
	}
}

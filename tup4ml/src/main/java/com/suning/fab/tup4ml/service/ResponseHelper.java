package com.suning.fab.tup4ml.service;

import java.util.Date;

import com.suning.fab.model.domain.protocal.ExitBusinessCommon;
import com.suning.fab.tup4ml.elfin.PlatConstant;

abstract class ResponseHelper {
	private ResponseHelper() {
		throw new IllegalStateException("ResponeHelper class");
	}
	
	public static ExitBusinessCommon createSuccessRespone(String serSeqNo, Date tranDate){
		return new ExitBusinessCommon(serSeqNo, tranDate,   
				PlatConstant.RSPCODE.OK, PlatConstant.RSPMSG.OK);
	}
	public static ExitBusinessCommon createDefaultErrorRespone(String serSeqNo, Date tranDate){
		return new ExitBusinessCommon(serSeqNo, tranDate,  
				PlatConstant.RSPCODE.UNKNOWN, PlatConstant.RSPMSG.UNKNOWN);
	}
	public static ExitBusinessCommon createIdempotencyRespone(String serSeqNo, Date tranDate){
		return new ExitBusinessCommon(serSeqNo, tranDate,  
				PlatConstant.RSPCODE.IDEMPOTENCY, PlatConstant.RSPMSG.OK);
	}
}

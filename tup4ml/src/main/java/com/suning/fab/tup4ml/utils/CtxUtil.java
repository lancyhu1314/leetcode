package com.suning.fab.tup4ml.utils;

import com.suning.fab.tup4ml.ctx.TranCtx;

public abstract class CtxUtil {
	private CtxUtil() {
		throw new IllegalStateException("CtxUtil class");
	}

	private static final ThreadLocal<TranCtx> tlCtx = new ThreadLocal<>();
	
	public static TranCtx getCtx() {
		return tlCtx.get();
	}
	
	public static void setCtx(TranCtx ctx) {
		tlCtx.set(ctx);
	}
}

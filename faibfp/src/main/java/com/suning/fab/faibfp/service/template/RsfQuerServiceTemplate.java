package com.suning.fab.faibfp.service.template;

import com.suning.fab.mulssyn.ctx.LocalTranCtx;

import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/4/2
 * @Version 1.0
 */
public abstract class RsfQuerServiceTemplate extends RsfServiceTemplate {

    /*@Override
    public void onProtoReg(Map<String, Object> in, Map<String, Object> out) {
        // 查询接口不登记报文
    }*/

    @Override
    protected LocalTranCtx createLocalTranCtx() {
        LocalTranCtx localTranCtx = super.createLocalTranCtx();
        localTranCtx.setSerialNo(localTranCtx.getBid());
        return localTranCtx;
    }
    @Override
    public void onProtoReg(Map<String, Object> in, Map<String, Object> out){
        //交易类型的请求不落报文表
    }
}

package com.suning.fab.faibfp.service;

import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.faibfp.utils.OldServiceAgentHelper;
import com.suning.fab.mulssyn.bean.Protoreg;
import com.suning.fab.mulssyn.db.ProtoregHandler;
import com.suning.fab.mulssyn.scmconf.ScmDynaGetterUtil;
import com.suning.fab.mulssyn.service.ServiceTemplate;
import com.suning.fab.mulssyn.utils.*;
import com.suning.rsf.consumer.ServiceAgent;

import java.util.Date;
import java.util.Map;

/**
 * 功能描述: <br>
 * 〈功能详细描述〉
 *
 * @Author 19043955
 * @Date 2021/3/26
 * @Version 1.0
 */
public abstract class RsfServiceTemplate extends ServiceTemplate {

    @Override
    public Map<String, Object> execute(Map<String, Object> reqMsg) {
        //起始计数
        Long startInterval = System.currentTimeMillis();
        // 查询借据产品映射表，根据scm配置的产品，判断是调用新系统还是老系统。
        String value = ScmDynaGetterUtil.getValue(ConstVar.SCMFILENAME.MIGRATED_PRODUCTS, ConstVar.KEYNAME.PRODUCT_CODES);
        //
        if (!VarChecker.isEmpty(value)) {

        }


        return super.execute(reqMsg);
    }


    /**
     * 参数透传 直接调用
     *
     * @param param
     * @param migrated      是否迁移
     * @param startInterval
     * @return
     */
    public Map<String, Object> transparentExecute(Map<String, Object> param, boolean migrated, long startInterval) {


        String serseqNo = GuidUtil.getUuidSequence();
        // 返回报文
        Map<String, Object> result = null;
        try {
            // 登记入口报文
            Protoreg protoreg = new Protoreg();
            protoreg.setSerialNo((String) param.get(PlatConstant.PARAMETER.SERIALNO));
            protoreg.setTranCode((String) param.get(PlatConstant.PARAMETER.TRANCODE));
            protoreg.setSerseqNo(serseqNo);
            protoreg.setRequest(param);
            ProtoregHandler protoregHandler = new ProtoregHandler();
            protoregHandler.setProtoreg(protoreg);
            protoregHandler.save();

            ServiceAgent agent;
            if (migrated) {
                // 已迁移：调用新系统
                agent = ServiceAgentHelper.getAgent((String) param.get(PlatConstant.PARAMETER.TRANCODE));
            } else {
                agent = OldServiceAgentHelper.getAgent((String) param.get(PlatConstant.PARAMETER.TRANCODE));
            }
            result = (Map<String, Object>) agent.invoke("execute", new Object[]{param}, new Class[]{Map.class});


        } catch (Exception e) {
            LoggerUtil.error("透传服务{}调用报错：{}", param.get(PlatConstant.PARAMETER.SERIALNO) + "|" + param.get(PlatConstant.PARAMETER.TRANCODE), e.getMessage());
            result = ResponseHelper.createDefaultErrorRespone(serseqNo, new Date());
        } finally {
            // 登记返回报文
            doFinish(param, startInterval, result);
        }
        return result;
    }


    /**
     * 是否需要拆分事务：默认不需要
     *
     * @return
     */
    public boolean isNeedSpliteTransation() {
        return false;
    }
}

package com.suning.fab.faibfp.service.template;

import com.suning.fab.faibfp.bean.AcctnoPrdnoMapping;
import com.suning.fab.faibfp.bean.AcctnoRelation;
import com.suning.fab.faibfp.bean.CustomerRelation;
import com.suning.fab.faibfp.dbhandler.AcctnoPrdnoMappingHandler;
import com.suning.fab.faibfp.dbhandler.AcctnoRelationHandler;
import com.suning.fab.faibfp.dbhandler.CustomerRelationHandler;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.faibfp.utils.OldServiceAgentHelper;
import com.suning.fab.mulssyn.bean.Protoreg;
import com.suning.fab.mulssyn.bean.TransDetail;
import com.suning.fab.mulssyn.db.ProtoregHandler;
import com.suning.fab.mulssyn.exception.FabException;
import com.suning.fab.mulssyn.scmconf.ScmDynaGetterUtil;
import com.suning.fab.mulssyn.service.ServiceTemplate;
import com.suning.fab.mulssyn.utils.*;
import com.suning.rsf.consumer.ServiceAgent;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.*;

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

        Map<String, Object> ret;
        //起始计数
        Long startInterval = System.currentTimeMillis();

        try {
            ret = dataDistribute(reqMsg, startInterval);
        } catch (Exception e) {
            LoggerUtil.error("数据分发错误，错误信息：{}", e);
            ret = new HashMap<>();
            ret.put(PlatConstant.PARAMETER.SERSEQNO, "UNKNOWN");
            ret.put(PlatConstant.PARAMETER.TRANDATE, DateFormatUtils.format(new Date(), "yyy-MM-dd"));
            ret.put(PlatConstant.PARAMETER.RSPCODE, PlatConstant.RSPCODE.UNKNOWN);
            ret.put(PlatConstant.PARAMETER.RSPMSG, PlatConstant.RSPMSG.UNKNOWN);
        }
        return ret;
    }

    public Map<String, Object> dataDistribute(Map<String, Object> reqMsg, long startInterval) throws FabException {
        Map<String, Object> ret;
        // 借据号
        String receiptNo = (String) reqMsg.get(ConstVar.PARAMETER.RECEIPTNO);
        String productCode = "";
        String customId = (String) reqMsg.get(ConstVar.PARAMETER.CUSTOMID);
        // "473004", "473005", "473007" 为开户类接口，需要向映射表插入贷款账号和产品代码的映射关系
        if (VarChecker.asList("473004", "473005", "473007").contains(reqMsg.get(PlatConstant.PARAMETER.TRANCODE))) {
            // 开户类产品直接由入口报文查出
            productCode = (String) reqMsg.get(ConstVar.PARAMETER.PRODUCTCODE);
            AcctnoPrdnoMappingHandler mappingHandler = new AcctnoPrdnoMappingHandler();
            // 考虑到服务调用失败的情况，对应关系可能已经存在与映射表中，防止主键冲突错误，先查询，再插入
            if (null == mappingHandler.load(receiptNo)) {
                // 将产品和借据号（贷款账号）的关系存入到映射表中
                mappingHandler.save(receiptNo, productCode);
            }
        } else {
            // 说明传的是acctNo
            if (VarChecker.isEmpty(receiptNo)) {
                // 去贷款账号和借据号关系对应表，查询出receiptno
                String routeId = (String) reqMsg.get(ConstVar.PARAMETER.ACCTNO);
                AcctnoRelation load = new AcctnoRelationHandler().load(routeId);
                receiptNo = null == load ? routeId : load.getReceiptNo();
            }
        }

        // 通过产品代码是否为空，再次判断是否开户类接口。
        if (VarChecker.isEmpty(productCode)) {
            // 查询借据号和产品映射表
            AcctnoPrdnoMapping prdMapping = new AcctnoPrdnoMappingHandler().load(receiptNo);
            if (null == prdMapping) {
                LoggerUtil.info("借据【{}】未找到对应的产品======", receiptNo);
                throw new FabException("");
            }
            productCode = prdMapping.getProductCode();
        }
        // 查询借据产品映射表，根据scm配置的产品，判断是调用新系统还是老系统。
        String value = ScmDynaGetterUtil.getValue(ConstVar.SCMFILENAME.MIGRATED_PRODUCTS, ConstVar.KEYNAME.PRODUCT_CODES);

        if (VarChecker.isEmpty(value) && !Arrays.asList(value.split(",")).contains(productCode)) {
            // 数据未迁移 调用老系统
            ret = transparentExecute(reqMsg, false, startInterval);
        } else {
            // 数据已迁移，调用新系统
            if (!VarChecker.isEmpty(reqMsg.get(ConstVar.PARAMETER.ACCTNO))) {
                reqMsg.put(ConstVar.PARAMETER.ACCTNO, receiptNo);
            }

            // 只有接口中传repayAcctNo的时候，将repayacctno转成customid
            if (!VarChecker.isEmpty(reqMsg.get(ConstVar.PARAMETER.REPAYACCTNO))) {

                if (VarChecker.isEmpty(customId)) {
                    CustomerRelation load = new CustomerRelationHandler().load((String) reqMsg.get(ConstVar.PARAMETER.REPAYACCTNO));
                    // 未查到，赋值为repayacctno，查到了赋值为新值
                    customId = null == load ? (String) reqMsg.get(ConstVar.PARAMETER.REPAYACCTNO) : load.getCustomId();
                }
                // 将repayacctno覆盖
                reqMsg.put(ConstVar.PARAMETER.REPAYACCTNO, customId);
            }
            // 是否需要跨库事务
            if (isNeedSpliteTransation()) {
                // 调用跨库事务
                ret = executeEntry(reqMsg, startInterval);
            } else {
                ret = transparentExecute(reqMsg, true, startInterval);
            }

        }
        return ret;
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
     * 如果是跨库事务，需要实现，返回true
     * 是否需要拆分事务：默认不需要
     *
     * @return
     */
    public boolean isNeedSpliteTransation() {
        return false;
    }

    /**
     * 如果是跨库事务，需要实现，将入口总报文拆分各个自报文
     *
     * @param param
     * @return
     */
    @Override
    protected List<TransDetail> paramSplite(Map<String, Object> param) {
        return null;
    }

    /**
     * 如果是跨库事务，需要实现，将多个服务的返回报文组装成总的返回报文
     *
     * @param resps
     * @return
     */
    @Override
    protected Map<String, Object> rspHandle(Map<String, Map<String, Object>> resps) {
        return null;
    }

}

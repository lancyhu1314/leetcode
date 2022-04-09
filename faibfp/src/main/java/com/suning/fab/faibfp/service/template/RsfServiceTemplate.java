package com.suning.fab.faibfp.service.template;

import com.alibaba.fastjson.JSON;
import com.suning.fab.faibfp.bean.AcctnoRelation;
import com.suning.fab.faibfp.bean.CustomerRelation;
import com.suning.fab.faibfp.bean.ProductMapping;
import com.suning.fab.faibfp.bean.TransferRelation;
import com.suning.fab.faibfp.dbhandler.AcctnoRelationHandler;
import com.suning.fab.faibfp.dbhandler.CustomerRelationHandler;
import com.suning.fab.faibfp.dbhandler.ProductMappingHandler;
import com.suning.fab.faibfp.dbhandler.TransferRelationHandler;
import com.suning.fab.faibfp.utils.ConstVar;
import com.suning.fab.faibfp.utils.DateUtils;
import com.suning.fab.faibfp.utils.OldServiceAgentHelper;
import com.suning.fab.mulssyn.bean.TransDetail;
import com.suning.fab.mulssyn.ctx.LocalTranCtx;
import com.suning.fab.mulssyn.exception.FabException;
import com.suning.fab.mulssyn.exception.FabRuntimeException;
import com.suning.fab.mulssyn.scmconf.ScmDynaGetterUtil;
import com.suning.fab.mulssyn.service.ServiceTemplate;
import com.suning.fab.mulssyn.utils.*;
import com.suning.rsf.consumer.ServiceAgent;
import com.suning.rsf.consumer.TimeoutException;
import com.suning.rsf.model.ServiceNotFoundException;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.util.CollectionUtils;

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
            // 设置交易码
            ThreadLocalUtil.set(PlatConstant.PARAMETER.TRANCODE, this.getTranCode());
            ret = dataDistribute(reqMsg, startInterval);
        } catch (Exception e) {
            Pair<String, String> pair = LoggerUtil.logException((String) reqMsg.get("serialNo")
                    , this.getClass().getSimpleName(), e, reqMsg);
            ret = new HashMap<>();
            ret.put(PlatConstant.PARAMETER.SERSEQNO, null == CtxUtil.getCtx() ? "UNKNOWN" : CtxUtil.getCtx().getBid());
            ret.put(PlatConstant.PARAMETER.TRANDATE, DateFormatUtils.format(new Date(), "yyy-MM-dd"));
            ret.put(PlatConstant.PARAMETER.RSPCODE, pair.getFirst());
            ret.put(PlatConstant.PARAMETER.RSPMSG, pair.getSecond());
            // 打印monitor日志
            LoggerUtil.logMonitor(this.getClass().getSimpleName(), (String) reqMsg.get("serialNo"),
                    (String) ret.get("serSeqNo"), (String) reqMsg.get("channelId"), (String) ret.get("rspCode"), (String) ret.get("rspMsg"), startInterval);
        } finally {
            this.onClean();
        }
        return ret;
    }

    public Map<String, Object> dataDistribute(Map<String, Object> reqMsg, long startInterval) throws FabException {

        LoggerUtil.info("前置系统入口报文：{}| ServiceName:{} |SerialNo【{}】", JSON.toJSONString(reqMsg), this.getClass().getSimpleName(), reqMsg.get("serialNo"));

        Map<String, Object> ret;
        // 借据号
        String receiptNo = (String) reqMsg.get(ConstVar.PARAMETER.RECEIPTNO);
        String productCode = (String) reqMsg.get(ConstVar.PARAMETER.PRODUCTCODE);
        String customId = (String) reqMsg.get(ConstVar.PARAMETER.CUSTOMID);
        // "473004", "473005", "473007" "479000" 为开户类接口，需要向映射表插入贷款账号和产品代码的映射关系
        if (isOpenAcctTranCode(getTranCode())) {
            if (VarChecker.isEmpty(receiptNo)) {
                throw new FabException("IBF400", "开户借据号");
            }
            // 开户类产品直接由入口报文查出
            ProductMappingHandler mappingHandler = new ProductMappingHandler();
            // 考虑到服务调用失败的情况，对应关系可能已经存在与映射表中，防止主键冲突错误，先查询，再插入
            if (null == mappingHandler.load(receiptNo)) {
                // 将产品和借据号（贷款账号）的关系存入到映射表中
                mappingHandler.save(receiptNo, receiptNo, productCode, ConstVar.ROUTETYPE.RECEIPTNO);
            } else {
                mappingHandler.update(receiptNo, receiptNo, productCode, ConstVar.ROUTETYPE.RECEIPTNO);
            }
        } else {
            receiptNo = compatibleWithCAcctno(reqMsg);
        }
        // 报文单独添加借据号
        reqMsg.put(ConstVar.PARAMETER.SYSRECEIPTNO, receiptNo);

        // 如果不是放款类或者不是试算类的接口，需要去产品映射关系表中查询产品代码
        if (!(isOpenAcctTranCode(getTranCode())
                || VarChecker.asList("470022", "476001", "476002", "476003", "476004").contains(getTranCode()))) {
            // 从产品映射表中获取产品
            productCode = getMappintProductCode(reqMsg, getProductMapRouteId(receiptNo, reqMsg));
        }
        // 将产品添加到参数中
        reqMsg.put(ConstVar.PARAMETER.SYSPRDCODE, productCode);

        // 判断是否拒绝交易
        if (refuseTrans(productCode, receiptNo)) {
            ret = createRefuseResp(reqMsg);
            LoggerUtil.info("新老模型切换中，前置拒绝产品：【{}】的交易。", productCode);
            return ret;
        }
        // 判断是否调用老系统
        // 标记【迁移过程中，产品调用老系统，但是借据号调用新模型】
        boolean toNewFlag = false;
        boolean countChange = false;
        //判断是否是本次迁移产品
        if (isTransferPrdCode(productCode)) {
            //如果包含在 realTimePrd 配置了，则说明正在迁移，否则直接调用
            TransferRelationHandler transferHandler = new TransferRelationHandler();
            TransferRelation transferRelation = transferHandler.load(receiptNo);
            //判断是否是开户类的
            if (isOpenAcctTranCode(getTranCode())) {
                //登记新增借据号，且状态为已迁移
                //DONE 借据号更换了产品代码，怎么处理 ——一般不会出现，不考虑这种情况
                if ( null == transferRelation) {
                    transferHandler.save(receiptNo, ConstVar.TRANSFERSTATUS.END_TRANSFER, 0);
                } else {
                    transferHandler.update(receiptNo, ConstVar.TRANSFERSTATUS.END_TRANSFER, 0);
                }
                //调用新模型
                toNewFlag = true;
            } else {
                //其他trancode，查询借据号状态
                //如果已迁移，走新模型
                if (transferRelation !=null && ConstVar.TRANSFERSTATUS.END_TRANSFER.equals(transferRelation.getStatus())) {
                    toNewFlag = true;
                }
                //如果迁移中，抛出异常
                else if (transferRelation !=null && ConstVar.TRANSFERSTATUS.TRANSFERING.equals(transferRelation.getStatus())) {
                    ret = createRefuseResp(reqMsg);
                    LoggerUtil.info("新老模型切换中，前置拒绝产品：【{}】的交易。", productCode);
                    return ret;
                }
                //如果未迁移/老系统处理中
                else {
                    //查询的过滤掉
                    if (isDealTranCode(getTranCode())) {
                        //如果transferRelation为空，插入一条记录
                        if (null == transferRelation) {
                            transferHandler.save(receiptNo, ConstVar.TRANSFERSTATUS.NOT_TRANSFER, 0);
                            transferRelation = new TransferRelation();
                            transferRelation.setStatus(ConstVar.TRANSFERSTATUS.NOT_TRANSFER);
                        }
                        //判断更新条数，条数=0，抛异常
                        int counts = transferHandler.updateCounts(receiptNo, 1);
                        if (counts == 0) {
                            ret = createRefuseResp(reqMsg);
                            LoggerUtil.info("新老模型切换中，前置拒绝产品：【{}】的交易。", productCode);
                            return ret;
                        }
                        countChange = true;
                    }
                }
            }
        }
        //产品走老系统，且产品下的借据号也是走老系统
        //479001/479010 接口，总是走老系统
        if ((isCallOldSystem(productCode) && !toNewFlag)
                || Arrays.asList("479001","479010").contains(getTranCode()) ) {
            if (Arrays.asList("479001", "479010").contains(getTranCode())) {
                reqMsg.put("sysPrdCode", null);
            }
            // 数据未迁移 调用老系统
            // 报文里面添加调用老系统的组别
            reqMsg.put("sysGroup", "FALOAN");
            ret = transparentExecute(reqMsg, false, startInterval);
            //如果前置迁移表笔数+1了，这里要-1(异常场景已考虑，无论结果如何，都会-1操作)
            if (countChange) {
                TransferRelationHandler transferHandler = new TransferRelationHandler();
                transferHandler.updateCounts(receiptNo, -1);
            }
            //产品配置了走新模型 || 产品配置了迁移中，但是产品下的借据号走新模型
        } else {

            // 数据已迁移，调用新系统，新模型中只存在receiptno(接口字段命名为acctno)，
            // 所以，调用新系统的都是借据号，一般acctno和receiptno一样，C系统在上文已转换。
            if (!VarChecker.isEmpty(reqMsg.get(ConstVar.PARAMETER.ACCTNO))) {
                reqMsg.put(ConstVar.PARAMETER.OLD_ACCTO, reqMsg.get(ConstVar.PARAMETER.ACCTNO));
                reqMsg.put(ConstVar.PARAMETER.ACCTNO, receiptNo);
            }

            String delRpyAcctTrans = ScmDynaGetterUtil.getWithDefaultValue("GlobalScm.properties", "delRpyAcctTrans", "false");
            if ("false".equals(delRpyAcctTrans)) {
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
            }
            // 如果是开户类接口，将repayacctno字段添加到报文中传给新系统
            if (isOpenAcctTranCode(getTranCode())) {
                // 查询预收账号和客户号关系
                CustomerRelation load = new CustomerRelationHandler().load((String) reqMsg.get(ConstVar.PARAMETER.MERCHANTNO));
                // 报文增加repayAcctNo字段
                reqMsg.put(ConstVar.PARAMETER.REPAYACCTNO, null == load ? (String) reqMsg.get(ConstVar.PARAMETER.MERCHANTNO) : load.getRepayacctNo());
            }


            // 是否需要跨库事务
            if (isNeedSpliteTransation(reqMsg)) {
                // 调用跨库事务
                ret = executeEntry(reqMsg, startInterval);
            } else {
                ret = transparentExecute(reqMsg, true, startInterval);
            }

        }

        // 将开户接口成功的返回
        if (isOpenAcctTranCode(getTranCode()) && !CollectionUtils.isEmpty(ret)
                && PlatConstant.RSPCODE.OK.equals(ret.get(PlatConstant.PARAMETER.RSPCODE))) {
            ProductMappingHandler mappingHandler = new ProductMappingHandler();
            // 预防开户多次幂等返回，先查询一下
            if (null == mappingHandler.load(String.valueOf(ret.get(PlatConstant.PARAMETER.SERSEQNO)) + ret.get(PlatConstant.PARAMETER.TRANDATE))) {
                // 保存开户核心流水号和产品的关系，为了冲销使用
                mappingHandler.save(String.valueOf(ret.get(PlatConstant.PARAMETER.SERSEQNO)).trim() + ret.get(ConstVar.PARAMETER.TRANDATE),
                        (String) reqMsg.get(ConstVar.PARAMETER.RECEIPTNO), (String) reqMsg.get(ConstVar.PARAMETER.PRODUCTCODE), ConstVar.ROUTETYPE.SERSEQNO);
            }
        }
        return ret;
    }

    protected Map<String, Object> createRefuseResp(Map<String, Object> reqMsg) {
        Map<String, Object> ret = new HashMap<>();
        ret.put(PlatConstant.PARAMETER.RSPCODE, "999999");
        ret.put(PlatConstant.PARAMETER.RSPMSG, "新老模型切换中，拒绝交易");
        ret.put(PlatConstant.PARAMETER.SERIALNO, reqMsg.get("serialNo"));
        ret.put(PlatConstant.PARAMETER.SERSEQNO, "");
        ret.put(PlatConstant.PARAMETER.TRANDATE, DateUtils.dateToString(new Date()));
        return ret;
    }

    /**
     * 根据配置的产品判断是否拒绝交易
     * 返回：true 拒绝交易 false 继续交易
     *
     * @param productCode
     * @return
     */
    public boolean refuseTrans(String productCode, String receiptNo) {
        String value = ScmDynaGetterUtil.getValue("tradingHalt.properties", "prdCodes");
        // 配置为空继续交易，产品在配置中拒绝交易
        if (!VarChecker.isEmpty(value) && Arrays.asList(value.split(",")).contains(productCode)) {
            String isAllowQuery = ScmDynaGetterUtil.getValue("tradingHalt.properties", "isAllowQuery");
            if ("true".equals(isAllowQuery) && this instanceof RsfQuerServiceTemplate) {
                // 查询类放过，不拒绝交易
                return false;
            }

            String grayAcctnos = ScmDynaGetterUtil.getValue("tradingHalt.properties", "grayAcctnos");
            // 拒绝交易：查询交易或者配置了借据号的，继续交易
            if (!VarChecker.isEmpty(grayAcctnos) && Arrays.asList(grayAcctnos.split(",")).contains(receiptNo)) {
                return false;
            }
            // 异常情况都不满足拒绝交易
            return true;
        }


        return false;
    }

    /**
     * 从产品映射表中获取借据号对应的产品
     *
     * @param reqMsg
     * @param receiptNo
     * @return
     */
    public String getMappintProductCode(Map<String, Object> reqMsg, String receiptNo) {

        String productCode = (String) reqMsg.get(ConstVar.PARAMETER.PRODUCTCODE);
        // 查询通过路由字段查询与产品映射表:产品关系表的路由字段取值为：errserseqnO或者receiptNo
        ProductMapping prdMapping = new ProductMappingHandler().load(getProductMapRouteId(receiptNo, reqMsg));
        if (null == prdMapping) {
            // TODO:去除校验，后续需要打开
            /*if (VarChecker.isEmpty(productCode)) {
                LoggerUtil.error("借据【{}】未找到对应的产品======", receiptNo);
                throw new FabException("IBF402", receiptNo);
            }*/
        } else {
            productCode = prdMapping.getProductCode();
            // 如果原报文中借据号不存在，将借据号存入报文(sysReceiptNo)
            if (VarChecker.isEmpty(reqMsg.get(ConstVar.PARAMETER.RECEIPTNO)))
                reqMsg.put(ConstVar.PARAMETER.SYSRECEIPTNO, prdMapping.getReceiptNo());
        }
        return productCode;
    }

    /**
     * 查询借据号与贷款账号关系表，兼容C系统贷款账号与借据号不一致的老数据
     *
     * @param reqMsg
     * @return
     * @throws FabException
     */
    public String compatibleWithCAcctno(Map<String, Object> reqMsg) throws FabException {

        String receiptNo = (String) reqMsg.get(ConstVar.PARAMETER.RECEIPTNO);
        String routeId = (String) reqMsg.get(ConstVar.PARAMETER.ACCTNO);
        // acctno不为空，且为C系统老数据
        if (!VarChecker.isEmpty(routeId) && isCsystemData(routeId)) {
            // 去借据号关系表找对应关系
            AcctnoRelation load = new AcctnoRelationHandler().load(routeId);
            if (null == load) {
                LoggerUtil.info("【前置系统】C系统老数据{}在借据号关系表中不存在", routeId);
                if (VarChecker.isEmpty(receiptNo))
                    receiptNo = routeId;
            } else {
                receiptNo = load.getReceiptNo();
            }
        } else {
            if (VarChecker.isEmpty(receiptNo)) {
                receiptNo = routeId;
            }
        }
        return receiptNo;
    }

    /**
     * 根据产品判断调用新系统还是老系统
     * 返回true-老系统 false-新模型
     * @param productCode
     * @return
     */
    public boolean isCallOldSystem(String productCode) {
        // 查询借据产品映射表，根据scm配置的产品，判断是调用新系统还是老系统。
        String value = ScmDynaGetterUtil.getValue(ConstVar.SCMFILENAME.MIGRATED_PRODUCTS, ConstVar.KEYNAME.PRODUCT_CODES);
        return VarChecker.isEmpty(value) || !Arrays.asList(value.split(",")).contains(productCode);
    }
    /**
     * 根据产品判断是否是本次迁移产品
     *  返回true-本次迁移 false-非本次迁移
     * @param productCode
     * @return
     */
    public boolean isTransferPrdCode(String productCode) {
        // 查询借据产品映射表，根据scm配置的产品，判断是调用新系统还是老系统。
        String value = ScmDynaGetterUtil.getValue(ConstVar.SCMFILENAME.MIGRATED_PRODUCTS, ConstVar.KEYNAME.REALTIME_PRD);
        return !VarChecker.isEmpty(value) && Arrays.asList(value.split(",")).contains(productCode);
    }
    /**
     * 判断是否为C系统的老数据
     *
     * @param data
     * @return
     */
    private boolean isCsystemData(String data) {
        return data.startsWith("5103") || data.startsWith("5105");
    }

    /**
     * 判断是否为开户类接口
     *
     * @param tranCode
     * @return
     */
    protected boolean isOpenAcctTranCode(String tranCode) {
        return VarChecker.asList("473004", "473005", "473006", "473007", "479000").contains(tranCode);
    }

    /**
     * 判断是否为交易类接口
     *
     * @param tranCode
     * @return
     */
    protected boolean isDealTranCode(String tranCode) {
        String value = ScmDynaGetterUtil.getValue("GlobalScm.properties", "dealtrancodes");
        //交易配置不为空，且包含trancode，返回true
        return !VarChecker.isEmpty(value) && Arrays.asList(value.split(",")).contains(tranCode);
    }

    /**
     * 获取与产品映射关系表的路由字段
     * 部分接口不传acctNo和receiptNo，所以路由字段可能是其他字段，比如放款冲销的errSerSeq
     * 默认返回借据号
     *
     * @param receiptNo
     * @param reqMsg
     * @return
     */
    public String getProductMapRouteId(String receiptNo, Map<String, Object> reqMsg) {
        return receiptNo;
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

        LoggerUtil.info("透传调用| ServiceName:{} |SerialNo【{}】|产品：{} |是否迁移：{}", this.getClass().getSimpleName(), param.get("serialNo"), param.get(ConstVar.PARAMETER.SYSPRDCODE), migrated);
        // 创建上线文
        createLocalTranCtx();
        LocalTranCtx ctx = (LocalTranCtx) CtxUtil.getCtx();
        if (!VarChecker.isEmpty(param.get(PlatConstant.PARAMETER.SERIALNO)))
            ctx.setSerialNo((String) param.get(PlatConstant.PARAMETER.SERIALNO));
        ctx.setTranCode(getTranCode());

        // 设置路由字段
        ctx.setRouteId(VarChecker.isEmpty(param.get(PlatConstant.PARAMETER.SYSRECEIPTNO)) ? ctx.getBid() : (String) param.get(PlatConstant.PARAMETER.SYSRECEIPTNO));

        // 查询类接口可以不传流水号，其他类接口必传流水号
        if (VarChecker.isEmpty(ctx.getSerialNo()) && !(this instanceof RsfQuerServiceTemplate)) {
            throw new FabRuntimeException("IBF400", "seriaNo");
        }
        // 返回报文
        Map<String, Object> result = null;
        try {
            // 登记入口报文
            onProtoReg(param, null);

            ServiceAgent agent;
            if (migrated) {
                // 已迁移：调用新系统
                agent = ServiceAgentHelper.getAgent(getTranCode());
            } else {
                agent = OldServiceAgentHelper.getAgent(getTranCode());
            }
            if (null == agent) {
                throw new FabException("IBF404", migrated, getTranCode());
            }
            //增加挡板开关，默认no TODO:压测的时候用的
            String baffleSwitch = ScmDynaGetterUtil.getWithDefaultValue("GlobalScm.properties", "baffleSwitch", "no");
            if ("yes".equalsIgnoreCase(baffleSwitch)) {
                result = new HashMap<>();
                result.put("rspCode", "000000");
                result.put("rspMsg", "请求成功！挡板开关=yes");
                result.put("tranDate", "unknown");
                result.put("tranTime", "unknown");
                result.put("serSeqNo", "unknown");
            } else {
                result = (Map<String, Object>) agent.invoke("execute", new Object[]{param}, new Class[]{Map.class});
            }

        } catch (ServiceNotFoundException e) {
            LoggerUtil.error("服务不存在异常{}", e);
            result = ResponseHelper.createDefaultErrorRespone(ctx.getBid(), ctx.getTranDate());
            result.put(PlatConstant.PARAMETER.RSPCODE, "IBF404");
            result.put(PlatConstant.PARAMETER.RSPMSG, "是否新模型【" + migrated + "】，服务【" + getTranCode() + "】不存在或者未配置！！！");

        } catch (FabException e) {
            LoggerUtil.error("服务异常，可能服务不存在。{}", e);
            result = ResponseHelper.createDefaultErrorRespone(ctx.getBid(), ctx.getTranDate());
            result.put(PlatConstant.PARAMETER.RSPCODE, e.getErrCode());
            result.put(PlatConstant.PARAMETER.RSPMSG, e.getErrMsg());
        } catch (Exception e) {
            LoggerUtil.error("透传服务{}调用报错：{}", param.get(PlatConstant.PARAMETER.SERIALNO) + "|" + getTranCode(), e);
            result = ResponseHelper.createDefaultErrorRespone(ctx.getBid(), ctx.getTranDate());
            if (e instanceof TimeoutException) {
                result.put(PlatConstant.PARAMETER.RSPMSG, PlatConstant.RSPMSG.TIMEOUT);
            }
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
    public boolean isNeedSpliteTransation(Map<String, Object> reqMsg) {
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

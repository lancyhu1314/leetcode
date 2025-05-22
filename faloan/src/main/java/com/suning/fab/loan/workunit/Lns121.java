package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnscoexamine;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.DefaultJudgementValidator;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.*;
import com.suning.fab.tup4j.validate.IValidatorJudgement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：考核数据校验 入库
 *  交易层：1.开户放款的接口，新增5个考核字段，根据渠道代码不同，校验不同业务条线逻辑
 * @Author 18049705 MYP
 * @Date Created in 11:15 2019/8/29
 * @see
 */
@Scope("prototype")
@Repository
public class Lns121 extends WorkUnit {

    private String promotionID;//促销编码
    private String fundingModel;//业务资金模式
    private String fundingID;//资金提供方代码
    private String underTake;//是否兜底
    private String undertakeID;//兜底方代码
    private String contractNo;	//合同编码

    //公共报文字段
    private String employeeId;//	工号
    private String salesStore;//	销售门店
    private String flowChannel;//	流量渠道
    private String terminalCode;//	终端编码
    private String bsNo;

    //预收账号
    private String repayAcctNo;
    private String merchantNo;
    private String acctNo;
    private String receiptNo;
    private String toAcctNo;
    @Autowired
    DefaultJudgementValidator validatorJudgement;

    static ConcurrentHashMap<String,String> tranCodes = new ConcurrentHashMap<>();

    public void run() throws Exception {
        //总的考核字段开关
        if (!"YES".equals(GlobalScmConfUtil.getProperty("AssessFieldFlag", "YES")))
            return;

         //校验是否考核
        if( validatorJudgement.judgement(tranctx.getChannelId(), tranctx.getTranCode())) {
            //判断是否是存在交易层字段的接口
            doValidator();

            //校验是否考核

            TblLnscoexamine lnscoexamine = new TblLnscoexamine();
            lnscoexamine.setTrandate(tranctx.getTermDate());
            lnscoexamine.setSerialno(tranctx.getSerialNo());
            lnscoexamine.setAccdate(tranctx.getTranDate());
            lnscoexamine.setSerseqno(tranctx.getSerSeqNo());
            lnscoexamine.setBrc(tranctx.getBrc());
            lnscoexamine.setTrancode(tranctx.getTranCode());
            lnscoexamine.setChannelid(tranctx.getChannelId());
            lnscoexamine.setUserno(notNullValue(repayAcctNo, merchantNo));

            //479009 需要特殊判断
            if("479009".equals(tranctx.getTranCode())) {
                //预收户的时候 acctno toAcctNo是预收户 receiptNo是借据号
                if(VarChecker.asList(ConstantDeclare.BRIEFCODE.YSZJ, ConstantDeclare.BRIEFCODE.YSJS)
                        .contains(tranctx.getRequestDict("briefCode"))){
                    lnscoexamine.setUserno(notNullValue(acctNo, toAcctNo));
                    lnscoexamine.setAcctno(receiptNo);

                }else {
                    lnscoexamine.setAcctno(notNullValue(acctNo, toAcctNo));
                }

            }
            else {
                lnscoexamine.setAcctno(notNullValue(acctNo, receiptNo));
            }
            lnscoexamine.setEmployeeid(employeeId);
            lnscoexamine.setSalesstore(salesStore);
            lnscoexamine.setFlowchannel(flowChannel);
            lnscoexamine.setTerminalcode(terminalCode);
            if (getTranCodes().get(getTranctx().getTranCode()) != null)
                lnscoexamine.setContractcode(contractNo);
            lnscoexamine.setPromotionid(promotionID);
            lnscoexamine.setFundingmodel(fundingModel);
            lnscoexamine.setFundingid(fundingID);
            lnscoexamine.setUndertake(underTake);
            lnscoexamine.setUndertakeid(undertakeID);
            lnscoexamine.setBsno(bsNo);
            try {
                DbAccessUtil.execute("Lnscoexamine.insert", lnscoexamine);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS100", "lnscoexamine");
            }



        }

    }

    private void doValidator() throws FabException {
        //交易层：1.开户放款的接口，新增5个考核字段，根据渠道代码不同，校验不同业务条线逻辑
        if (getTranCodes().get(getTranctx().getTranCode()) != null) {
            if (VarChecker.isEmpty(promotionID)) {
                throw new FabException("LNS055", "促销编码");
            }
            if (VarChecker.isEmpty(fundingModel)) {
                throw new FabException("LNS055", "业务资金模式");
            }
            if (VarChecker.isEmpty(contractNo)) {
                throw new FabException("LNS055", "合同编号");
            }
            //M01自有资金 M02助贷资金 M03联营资金
            if (!VarChecker.asList("M01", "M02", "M03").contains(fundingModel)) {
                throw new FabException("LNS169", "业务资金模式", fundingModel);
            }
            //fundingModel为M02或者M3，fundingID不能为空
            if (VarChecker.asList("M02", "M03").contains(fundingModel) && VarChecker.isEmpty(fundingID)) {
                throw new FabException("LNS055", "资金提供方代码");
            }
            //G01兜底 G02非兜底
            if (!VarChecker.isEmpty(underTake)) {
                if (!VarChecker.asList("G01", "G02").contains(underTake)) {
                    throw new FabException("LNS169", "是否兜底", underTake);
                }
                //兜底方代码，G01兜底时必输
                if ("G01".equals(underTake) && VarChecker.isEmpty(undertakeID))
                    throw new FabException("LNS055", "兜底方代码");
            }
            //消费金融必传兜底
            if(VarChecker.asList("DM","DH","ZC","US").contains(tranctx.getChannelId())&&VarChecker.isEmpty(underTake)){
                throw new FabException("LNS055", "是否兜底");
            }
        }
    }

    private String notNullValue(String frist,String last){
        if(!VarChecker.isEmpty(frist))
            return frist;
        if(!VarChecker.isEmpty(last))
            return last;
        return "";
    }


    /**
     * @return tranCodes
     */
    private ConcurrentHashMap<String,String> getTranCodes(){
        if(tranCodes.isEmpty()){
            synchronized (tranCodes) {
                LoggerUtil.debug("start read tranCodes");
                if(tranCodes.isEmpty()) {
                    LoggerUtil.debug("read tranCode from properties");

                    for (String value : PropertyUtil.getPropertyOrDefault("checkfieldtrans.tranCode", "").split("\\|"))
                    {
                        tranCodes.put(value, value);
                    }
                }
                LoggerUtil.info("tranCodes:"+tranCodes.toString());
                LoggerUtil.debug("end read tranCodes");
            }
        }
        return  tranCodes;
    }

    /**
     * Gets the value of toAcctNo.
     *
     * @return the value of toAcctNo
     */
    public String getToAcctNo() {
        return toAcctNo;
    }

    /**
     * Sets the toAcctNo.
     *
     * @param toAcctNo toAcctNo
     */
    public void setToAcctNo(String toAcctNo) {
        this.toAcctNo = toAcctNo;

    }

    /**
     * Gets the value of promotionID.
     *
     * @return the value of promotionID
     */
    public String getPromotionID() {
        return promotionID;
    }

    /**
     * Sets the promotionID.
     *
     * @param promotionID promotionID
     */
    public void setPromotionID(String promotionID) {
        this.promotionID = promotionID;

    }

    /**
     * Gets the value of fundingModel.
     *
     * @return the value of fundingModel
     */
    public String getFundingModel() {
        return fundingModel;
    }

    /**
     * Sets the fundingModel.
     *
     * @param fundingModel fundingModel
     */
    public void setFundingModel(String fundingModel) {
        this.fundingModel = fundingModel;

    }

    /**
     * Gets the value of fundingID.
     *
     * @return the value of fundingID
     */
    public String getFundingID() {
        return fundingID;
    }

    /**
     * Sets the fundingID.
     *
     * @param fundingID fundingID
     */
    public void setFundingID(String fundingID) {
        this.fundingID = fundingID;

    }

    /**
     * Gets the value of underTake.
     *
     * @return the value of underTake
     */
    public String getUnderTake() {
        return underTake;
    }

    /**
     * Sets the underTake.
     *
     * @param underTake underTake
     */
    public void setUnderTake(String underTake) {
        this.underTake = underTake;

    }

    /**
     * Gets the value of undertakeID.
     *
     * @return the value of undertakeID
     */
    public String getUndertakeID() {
        return undertakeID;
    }

    /**
     * Sets the undertakeID.
     *
     * @param undertakeID undertakeID
     */
    public void setUndertakeID(String undertakeID) {
        this.undertakeID = undertakeID;

    }

    /**
     * Gets the value of employeeId.
     *
     * @return the value of employeeId
     */
    public String getEmployeeId() {
        return employeeId;
    }

    /**
     * Sets the employeeId.
     *
     * @param employeeId employeeId
     */
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;

    }

    /**
     * Gets the value of salesStore.
     *
     * @return the value of salesStore
     */
    public String getSalesStore() {
        return salesStore;
    }

    /**
     * Sets the salesStore.
     *
     * @param salesStore salesStore
     */
    public void setSalesStore(String salesStore) {
        this.salesStore = salesStore;

    }

    /**
     * Gets the value of flowChannel.
     *
     * @return the value of flowChannel
     */
    public String getFlowChannel() {
        return flowChannel;
    }

    /**
     * Sets the flowChannel.
     *
     * @param flowChannel flowChannel
     */
    public void setFlowChannel(String flowChannel) {
        this.flowChannel = flowChannel;

    }

    /**
     * Gets the value of terminalCode.
     *
     * @return the value of terminalCode
     */
    public String getTerminalCode() {
        return terminalCode;
    }

    /**
     * Sets the terminalCode.
     *
     * @param terminalCode terminalCode
     */
    public void setTerminalCode(String terminalCode) {
        this.terminalCode = terminalCode;

    }

    /**
     * Gets the value of contractNo.
     *
     * @return the value of contractNo
     */
    public String getContractNo() {
        return contractNo;
    }

    /**
     * Sets the contractNo.
     *
     * @param contractNo contractNo
     */
    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;

    }

    /**
     * Gets the value of repayAcctNo.
     *
     * @return the value of repayAcctNo
     */
    public String getRepayAcctNo() {
        return repayAcctNo;
    }

    /**
     * Sets the repayAcctNo.
     *
     * @param repayAcctNo repayAcctNo
     */
    public void setRepayAcctNo(String repayAcctNo) {
        this.repayAcctNo = repayAcctNo;

    }

    /**
     * Gets the value of acctNo.
     *
     * @return the value of acctNo
     */
    public String getAcctNo() {
        return acctNo;
    }

    /**
     * Sets the acctNo.
     *
     * @param acctNo acctNo
     */
    public void setAcctNo(String acctNo) {
        this.acctNo = acctNo;

    }

    /**
     * Gets the value of bsNo.
     *
     * @return the value of bsNo
     */
    public String getBsNo() {
        return bsNo;
    }

    /**
     * Sets the bsNo.
     *
     * @param bsNo bsNo
     */
    public void setBsNo(String bsNo) {
        this.bsNo = bsNo;

    }

    /**
     * Gets the value of merchantNo.
     *
     * @return the value of merchantNo
     */
    public String getMerchantNo() {
        return merchantNo;
    }

    /**
     * Sets the merchantNo.
     *
     * @param merchantNo merchantNo
     */
    public void setMerchantNo(String merchantNo) {
        this.merchantNo = merchantNo;

    }

    /**
     * Gets the value of receiptNo.
     *
     * @return the value of receiptNo
     */
    public String getReceiptNo() {
        return receiptNo;
    }

    /**
     * Sets the receiptNo.
     *
     * @param receiptNo receiptNo
     */
    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;

    }
}

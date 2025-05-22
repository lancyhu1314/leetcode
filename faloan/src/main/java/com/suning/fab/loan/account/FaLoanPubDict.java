package com.suning.fab.loan.account;

import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.PubDict;

public class FaLoanPubDict extends PubDict {
    private String billList;
    private String billType;
    private String dealDate;
    private Integer errSerSeq;
    private Integer graceDays;
    private Integer periodNum;
    private String repayDate;
    private String debtCompany;
    private FabAmount debtAmt;
    private FabAmount contractAmt;
    private FabAmount discountAmt;
    private FabAmount forfeitAmt;
    private FabAmount intAmt;
    private FabAmount offsetAmt;
    private FabAmount prinAmt;
    private FabAmount refundAmt;
    private FabAmount repayAmt;
    private FabRate compoundRate;
    private FabRate feeRate;
    private FabRate normalRate;
    private FabRate overdueRate;
    private String cashFlag;
    private String channelType;
    private String compoundRateType;
    private String contractNo;
    private String customId;
    private String customName;
    private String customType;
    private String discountFlag;
    private String errDate;
    private String fundChannel;
    private String intPerUnit;
    private String investee;
    private String investeeId;
    private FabAmount investeePrin;
    private FabAmount investeeInt;
    private FabAmount investeeDint;
    private FabAmount investeeFee;
    private String investeeFlag;
    private String investMode; //投放模式：保理、消费贷
    private String loanType;
    private String normalRateType;
    private String openBrc;
    private String openDate;
    private String outSerialNo;
    private String overdueRateType;
    private String priorType;
    private String receiptNo;
    private String repayAcctNo;
    private String repayChannel;
    private String repayWay;
    private String settleChannel;
    private String startIntDate;
    private String tranType;
    private String periodType;
    private String specifiedType;
    private String acctStat;    //账户类型 -- 需要去FaLoanPublic中增加该字段
    private String loanStat;    //贷款状态
    private Integer repayTerm;
    private String endDate;
    private String startDate;
    private String bankSubject;
    private String calcIntFlag2;
    private String calcIntFlag1;
    private String termeDate;
    private String intbDate;
    private FabAmount termPrin;
    private FabAmount termInt;
    private FabAmount tailAmt;
    private FabAmount downPayment;
    private FabAmount merchantCode;
    private String inteDate;
    private String days;
    private String discountAcctNo;
    private String chargeType;

    private Double recoveryRate;
    private Double finalRate;
    private Double rentRate;
    private Double floatRate;
    private String settleFlag;
    private String tranInfo;


    private FabRate yearFeeRate;  //年费率
    private String childBrc;  //子机构
    private String checkFlag;
    private Double reduceFintAmt;
    private Double reduceIntAmt;
    private FabRate serviceFeeRate; //信息服务费费率
    private FabRate penaltyFeeRate; //违约金费率
    private FabAmount penaltyAmt; //违约金

    private FabRate preDiscountRate;
    private String intDays;        //摊销日期

    //for transfer
    private FabAmount contractBal;
    private String repayintBdate;
    private String repayintEdate;
    private FabAmount noretPrin;
    private FabAmount balance;
    private FabAmount noretInt;
    private FabAmount termcdInt;
    private String settleDate;
    private String termretDate;
    private FabAmount feeAmt;
    private FabAmount sumFint;
    private FabAmount termFint;
    private FabAmount sumrFint;
    private String transferFlag;
    private FabAmount totalInt;

    private FabRate realRate;
    private String flag;
    private String rateType;
    private String expandDate;
    private String beforeExpandDate;
    private String afterExpandDate;

    private String channelCode;
    private FabAmount channelAmt;
    private FabAmount reducePrinAmt;
    private String channelNo;
    private String channelSubject;
    private String cancelAmt;
    private String contDueDate;
    private FabRate onetimeRate;
    private FabAmount inSurance;
    private FabRate cleanFeeRate;
    private String customID;

    //代偿，借新还旧
    private String exAcctno;
    private String exAcctno1;
    private String exBrc;
    private String exBrc1;
    private String exPeriod;
    private String exinvesteeId;
    private String compensateFlag;
    private FabAmount switchFee;
    private String switchloanType;//借新还旧类型

    //现金贷免息金额
    private FabAmount freeInterest;
    private String freeDays;
    private FabRate freeRate;  //利息折扣比例
    private double feeDiscountRatio;  //费用折扣比例
    private String salesType;//促销类型
    //收费公司商户号
    private String feecompanyID;
    // 跨期减免
    private FabAmount exceedReduce;

    private String firstRepayDate;//首期还款日

    //提前结清收费模式
    private String earlysettleFlag;
    private String entrustedPaybrc;//受托支付商户号

    private String cooperateId;//赔付方代码
    private String enddateRule;//到期日按月对日规则
    //平台层考核字段
    private String promotionID;//促销编码
    private String fundingModel;//业务资金模式
    private String fundingID;//资金提供方代码
    private String underTake;//是否兜底
    private String undertakeID;//兜底方代码

    //调账增加
    private String debtCompanyin;
    private String debtCompanyout;

    //膨胀期数
    private Integer expandPeriod;
    private String feeType;//费用类型
    private FabAmount reduceFee;//费用减免金额
    private FabAmount reduceDamage;//违约金减免金额
    private String amortizeType;
    //实际扣款日自动减免
    private String realDate;    //实际扣款日

    private String paymentTimes;    //试算期数

    private String platformId;    //平台方代码

    //消金-催收-预减免优化
    private String reduceFeeType;//减免费用类型
    private FabAmount reduceFeeAmt;//减免费用金额

    //费用list
    private FabAmount deducetionAmt;
    private String feerepayWay;
    private String feeBrc;
    private String calCulatrule;
    private String feeBase;
    private String advanceSettle;
    private FabRate overRate;

    //贴息贴费相关信息
    private Integer freefeeDays;//利息、费用试算 免费用天数
    private FabAmount freeFee;//免费金额
    private FabRate feeDiscount;//折前融担费率
    private String discountFeeFlag;//贴费标志
    private String discountIntFlag;//贴息标志
    private String investeeRepayFlg;//贴息标志

    //资金方相关信息
    private String investeeAcctno;//资金方借据号
    private FabRate investeePropor;//资金方占比
    private FabAmount platformFee;//平台还费

    public FabAmount getPlatformFee() {
        return platformFee;
    }

    private FabAmount platformTotalAmt;//平台还款金额

    //周末改工作日
    private String workDays;
    //工作日改假日
    private String holidays;

    private String serviceType;
    private String intervalTime; //	延期还款间隔		"M：按月 D：按天（预留，本期不支持） Q：按季（预留，本期不支持）"
    private Integer delayTime;//	延期还款时间	"1：一个月 2：两个月 3：三个月"

    //光大人保
    private FabAmount adjustAmt;
    private String adjustType;
    private String tranBrc;
    private String switchBrc;
    private String depositType;
    private String dealType;

    private String type;//sql的类型query、update、delete、add、batch

    private String sql;//sql语句

    //期最小天数
    private Integer periodMinDays;


    private String repayList;

    //趸交融担费（绿地小贷）
    private FabAmount singleAmt;

    //批量预收充值
    private FabAmount lawsuitAmt;//诉讼费金额
    private FabAmount surplusAmt;//长款金额
    private FabAmount cooperateAmt;//往来金额
    private FabAmount advanceAmt;//预收金额
    private String batchType;//交易类型 默认1-单笔 2-批量
    private String lawsuitList;//往来明细

    private String payerAccountSubject;//付款方银行账户

    private FabRate interestDiscount;//折前利率

    private String reserve;//会计记账事件 前端透传
    /**
     * 操作类型：冲销交易啥的
     */
    private String operation;

    /**
     * 表名
     */
    private String tableName;

    /**
     * json格式的主键串
     */
    private String uniqueKeys;


    // 功能码
    private String functionCode;
    // 转非应计日期
    private String nonAccrualDate;
    // 是否冲销非应计前收入
    private String chargeAginstFlag;
    // 冲销收入开始日
    private String chargeAginstDate;

    // 费用类型
    private String feeBusiType;
    // 费用业务编号
    private String feeBusiNo;

    private String caseNo;
    private String lawFirm;
    private String acceptCourt;
    private String payDate;
    private String invoiceNo;
    private String invoiceCode;
    private String payorderNo;
    private FabAmount openAmt;
    private String feeFunCode;
    private String newfeeFunCode;
    private String feeReqNo;

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @param tableName to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * @return the uniqueKeys
     */
    public String getUniqueKeys() {
        return uniqueKeys;
    }

    /**
     * @param uniqueKeys to set
     */
    public void setUniqueKeys(String uniqueKeys) {
        this.uniqueKeys = uniqueKeys;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * @return the batchType
     */
    public String getBatchType() {
        return batchType;
    }

    /**
     * @param batchType to set
     */
    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    /**
     * Gets the value of serviceType.
     *
     * @return the value of serviceType
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Sets the serviceType.
     *
     * @param serviceType serviceType
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;

    }

    /**
     * Gets the value of intervalTime.
     *
     * @return the value of intervalTime
     */
    public String getIntervalTime() {
        return intervalTime;
    }

    /**
     * Sets the intervalTime.
     *
     * @param intervalTime intervalTime
     */
    public void setIntervalTime(String intervalTime) {
        this.intervalTime = intervalTime;

    }

    /**
     * Gets the value of delayTime.
     *
     * @return the value of delayTime
     */
    public Integer getDelayTime() {
        return delayTime;
    }

    /**
     * Sets the delayTime.
     *
     * @param delayTime delayTime
     */
    public void setDelayTime(Integer delayTime) {
        this.delayTime = delayTime;

    }

    /**
     * Gets the value of workDays.
     *
     * @return the value of workDays
     */
    public String getWorkDays() {
        return workDays;
    }

    /**
     * Sets the workDays.
     *
     * @param workDays workDays
     */
    public void setWorkDays(String workDays) {
        this.workDays = workDays;

    }

    /**
     * Gets the value of holidays.
     *
     * @return the value of holidays
     */
    public String getHolidays() {
        return holidays;
    }

    /**
     * Sets the holidays.
     *
     * @param holidays holidays
     */
    public void setHolidays(String holidays) {
        this.holidays = holidays;

    }


    /**
     * Gets the value of amortizeType.
     *
     * @return the value of amortizeType
     */
    public String getAmortizeType() {
        return amortizeType;
    }

    /**
     * Sets the amortizeType.
     *
     * @param amortizeType amortizeType
     */
    public void setAmortizeType(String amortizeType) {
        this.amortizeType = amortizeType;

    }

    /**
     * Gets the value of enddateRule.
     *
     * @return the value of enddateRule
     */
    public String getEnddateRule() {
        return enddateRule;
    }

    /**
     * Sets the enddateRule.
     *
     * @param enddateRule enddateRule
     */
    public void setEnddateRule(String enddateRule) {
        this.enddateRule = enddateRule;

    }

    /**
     * Gets the value of cooperateId.
     *
     * @return the value of cooperateId
     */
    public String getCooperateId() {
        return cooperateId;
    }

    /**
     * Sets the cooperateId.
     *
     * @param cooperateId cooperateId
     */
    public void setCooperateId(String cooperateId) {
        this.cooperateId = cooperateId;

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
     * Gets the value of earlysettleFlag.
     *
     * @return the value of earlysettleFlag
     */
    public String getEarlysettleFlag() {
        return earlysettleFlag;
    }

    /**
     * Sets the earlysettleFlag.
     *
     * @param earlysettleFlag earlysettleFlag
     */
    public void setEarlysettleFlag(String earlysettleFlag) {
        this.earlysettleFlag = earlysettleFlag;

    }

    /**
     * Gets the value of exceedReduce.
     *
     * @return the value of exceedReduce
     */
    public FabAmount getExceedReduce() {
        return exceedReduce;
    }

    /**
     * Sets the exceedReduce.
     *
     * @param exceedReduce exceedReduce
     */
    public void setExceedReduce(FabAmount exceedReduce) {
        this.exceedReduce = exceedReduce;

    }

    /**
     * Gets the value of feecompanyID.
     *
     * @return the value of feecompanyID
     */
    public String getFeecompanyID() {
        return feecompanyID;
    }

    /**
     * Sets the feecompanyID.
     *
     * @param feecompanyID feecompanyID
     */
    public void setFeecompanyID(String feecompanyID) {
        this.feecompanyID = feecompanyID;

    }

    public FabAmount getTermInt() {
        return termInt;
    }

    public void setTermInt(FabAmount termInt) {
        this.termInt = termInt;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public FabAmount getTermPrin() {
        return termPrin;
    }

    public void setTermPrin(FabAmount termPrin) {
        this.termPrin = termPrin;
    }

    public String getInteDate() {
        return inteDate;
    }

    public void setInteDate(String inteDate) {
        this.inteDate = inteDate;
    }

    public String getCalcIntFlag2() {
        return calcIntFlag2;
    }

    public void setCalcIntFlag2(String calcIntFlag2) {
        this.calcIntFlag2 = calcIntFlag2;
    }

    public String getCalcIntFlag1() {
        return calcIntFlag1;
    }

    public void setCalcIntFlag1(String calcIntFlag1) {
        this.calcIntFlag1 = calcIntFlag1;
    }

    public String getTermeDate() {
        return termeDate;
    }

    public void setTermeDate(String termeDate) {
        this.termeDate = termeDate;
    }

    public String getIntbDate() {
        return intbDate;
    }

    public void setIntbDate(String intbDate) {
        this.intbDate = intbDate;
    }

    /**
     * @return the errSerSeq
     */
    public Integer getErrSerSeq() {
        return errSerSeq;
    }

    /**
     * @param errSerSeq the errSerSeq to set
     */
    public void setErrSerSeq(Integer errSerSeq) {
        this.errSerSeq = errSerSeq;
    }

    /**
     * @return the graceDays
     */
    public Integer getGraceDays() {
        return graceDays;
    }

    /**
     * @param graceDays the graceDays to set
     */
    public void setGraceDays(Integer graceDays) {
        this.graceDays = graceDays;
    }

    /**
     * @return the periodNum
     */
    public Integer getPeriodNum() {
        return periodNum;
    }

    /**
     * @param periodNum the periodNum to set
     */
    public void setPeriodNum(Integer periodNum) {
        this.periodNum = periodNum;
    }

    /**
     * @return the repayDate
     */
    public String getRepayDate() {
        return repayDate;
    }

    /**
     * @param repayDate the repayDate to set
     */
    public void setRepayDate(String repayDate) {
        this.repayDate = repayDate;
    }

    /**
     * @return the debtCompany
     */
    public String getDebtCompany() {
        return debtCompany;
    }

    /**
     * @param debtCompany the debtCompany to set
     */
    public void setDebtCompany(String debtCompany) {
        this.debtCompany = debtCompany;
    }

    /**
     * @return the debtAmt
     */
    public FabAmount getDebtAmt() {
        return debtAmt;
    }

    /**
     * @param debtAmt the debtAmt to set
     */
    public void setDebtAmt(FabAmount debtAmt) {
        this.debtAmt = debtAmt;
    }

    /**
     * @return the contractAmt
     */
    public FabAmount getContractAmt() {
        return contractAmt;
    }

    /**
     * @param contractAmt the contractAmt to set
     */
    public void setContractAmt(FabAmount contractAmt) {
        this.contractAmt = contractAmt;
    }

    /**
     * @return the discountAmt
     */
    public FabAmount getDiscountAmt() {
        return discountAmt;
    }

    /**
     * @param discountAmt the discountAmt to set
     */
    public void setDiscountAmt(FabAmount discountAmt) {
        this.discountAmt = discountAmt;
    }

    /**
     * @return the forfeitAmt
     */
    public FabAmount getForfeitAmt() {
        return forfeitAmt;
    }

    /**
     * @param forfeitAmt the forfeitAmt to set
     */
    public void setForfeitAmt(FabAmount forfeitAmt) {
        this.forfeitAmt = forfeitAmt;
    }

    /**
     * @return the intAmt
     */
    public FabAmount getIntAmt() {
        return intAmt;
    }

    /**
     * @param intAmt the intAmt to set
     */
    public void setIntAmt(FabAmount intAmt) {
        this.intAmt = intAmt;
    }

    /**
     * @return the offsetAmt
     */
    public FabAmount getOffsetAmt() {
        return offsetAmt;
    }

    /**
     * @param offsetAmt the offsetAmt to set
     */
    public void setOffsetAmt(FabAmount offsetAmt) {
        this.offsetAmt = offsetAmt;
    }

    /**
     * @return the prinAmt
     */
    public FabAmount getPrinAmt() {
        return prinAmt;
    }

    /**
     * @param prinAmt the prinAmt to set
     */
    public void setPrinAmt(FabAmount prinAmt) {
        this.prinAmt = prinAmt;
    }

    /**
     * @return the refundAmt
     */
    public FabAmount getRefundAmt() {
        return refundAmt;
    }

    /**
     * @param refundAmt the refundAmt to set
     */
    public void setRefundAmt(FabAmount refundAmt) {
        this.refundAmt = refundAmt;
    }

    /**
     * @return the repayAmt
     */
    public FabAmount getRepayAmt() {
        return repayAmt;
    }

    /**
     * @param repayAmt the repayAmt to set
     */
    public void setRepayAmt(FabAmount repayAmt) {
        this.repayAmt = repayAmt;
    }

    /**
     * @return the compoundRate
     */
    public FabRate getCompoundRate() {
        return compoundRate;
    }

    /**
     * @param compoundRate the compoundRate to set
     */
    public void setCompoundRate(FabRate compoundRate) {
        this.compoundRate = compoundRate;
    }

    /**
     * @return the normalRate
     */
    public FabRate getNormalRate() {
        return normalRate;
    }

    /**
     * @param normalRate the normalRate to set
     */
    public void setNormalRate(FabRate normalRate) {
        this.normalRate = normalRate;
    }

    /**
     * @return the overdueRate
     */
    public FabRate getOverdueRate() {
        return overdueRate;
    }

    /**
     * @param overdueRate the overdueRate to set
     */
    public void setOverdueRate(FabRate overdueRate) {
        this.overdueRate = overdueRate;
    }

    /**
     * @return the cashFlag
     */
    public String getCashFlag() {
        return cashFlag;
    }

    /**
     * @param cashFlag the cashFlag to set
     */
    public void setCashFlag(String cashFlag) {
        this.cashFlag = cashFlag;
    }

    /**
     * @return the channelType
     */
    public String getChannelType() {
        return channelType;
    }

    /**
     * @param channelType the channelType to set
     */
    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    /**
     * @return the compoundRateType
     */
    public String getCompoundRateType() {
        return compoundRateType;
    }

    /**
     * @param compoundRateType the compoundRateType to set
     */
    public void setCompoundRateType(String compoundRateType) {
        this.compoundRateType = compoundRateType;
    }

    /**
     * @return the contractNo
     */
    public String getContractNo() {
        return contractNo;
    }

    /**
     * @param contractNo the contractNo to set
     */
    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    /**
     * @return the customId
     */
    public String getCustomId() {
        return customId;
    }

    /**
     * @param customId the customId to set
     */
    public void setCustomId(String customId) {
        this.customId = customId;
    }

    /**
     * @return the customName
     */
    public String getCustomName() {
        return customName;
    }

    /**
     * @param customName the customName to set
     */
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    /**
     * @return the customType
     */
    public String getCustomType() {
        return customType;
    }

    /**
     * @param customType the customType to set
     */
    public void setCustomType(String customType) {
        this.customType = customType;
    }

    /**
     * @return the discountFlag
     */
    public String getDiscountFlag() {
        return discountFlag;
    }

    /**
     * @param discountFlag the discountFlag to set
     */
    public void setDiscountFlag(String discountFlag) {
        this.discountFlag = discountFlag;
    }

    /**
     * @return the errDate
     */
    public String getErrDate() {
        return errDate;
    }

    /**
     * @param errDate the errDate to set
     */
    public void setErrDate(String errDate) {
        this.errDate = errDate;
    }

    /**
     * @return the fundChannel
     */
    public String getFundChannel() {
        return fundChannel;
    }

    /**
     * @param fundChannel the fundChannel to set
     */
    public void setFundChannel(String fundChannel) {
        this.fundChannel = fundChannel;
    }

    /**
     * @return the intPerUnit
     */
    public String getIntPerUnit() {
        return intPerUnit;
    }

    /**
     * @param intPerUnit the intPerUnit to set
     */
    public void setIntPerUnit(String intPerUnit) {
        this.intPerUnit = intPerUnit;
    }

    /**
     * @return the investee
     */
    public String getInvestee() {
        return investee;
    }

    /**
     * @param investee the investee to set
     */
    public void setInvestee(String investee) {
        this.investee = investee;
    }

    /**
     * @return the investMode
     */
    public String getInvestMode() {
        return investMode;
    }

    /**
     * @param investMode the investMode to set
     */
    public void setInvestMode(String investMode) {
        this.investMode = investMode;
    }

    /**
     * @return the loanType
     */
    public String getLoanType() {
        return loanType;
    }

    /**
     * @param loanType the loanType to set
     */
    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    /**
     * @return the normalRateType
     */
    public String getNormalRateType() {
        return normalRateType;
    }

    /**
     * @param normalRateType the normalRateType to set
     */
    public void setNormalRateType(String normalRateType) {
        this.normalRateType = normalRateType;
    }

    /**
     * @return the openBrc
     */
    public String getOpenBrc() {
        return openBrc;
    }

    /**
     * @param openBrc the openBrc to set
     */
    public void setOpenBrc(String openBrc) {
        this.openBrc = openBrc;
    }

    /**
     * @return the openDate
     */
    public String getOpenDate() {
        return openDate;
    }

    /**
     * @param openDate the openDate to set
     */
    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    /**
     * @return the outSerialNo
     */
    public String getOutSerialNo() {
        return outSerialNo;
    }

    /**
     * @param outSerialNo the outSerialNo to set
     */
    public void setOutSerialNo(String outSerialNo) {
        this.outSerialNo = outSerialNo;
    }

    /**
     * @return the overdueRateType
     */
    public String getOverdueRateType() {
        return overdueRateType;
    }

    /**
     * @param overdueRateType the overdueRateType to set
     */
    public void setOverdueRateType(String overdueRateType) {
        this.overdueRateType = overdueRateType;
    }

    /**
     * @return the priorType
     */
    public String getPriorType() {
        return priorType;
    }

    /**
     * @param priorType the priorType to set
     */
    public void setPriorType(String priorType) {
        this.priorType = priorType;
    }

    /**
     * @return the receiptNo
     */
    public String getReceiptNo() {
        return receiptNo;
    }

    /**
     * @param receiptNo the receiptNo to set
     */
    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    /**
     * @return the repayAcctNo
     */
    public String getRepayAcctNo() {
        return repayAcctNo;
    }

    /**
     * @param repayAcctNo the repayAcctNo to set
     */
    public void setRepayAcctNo(String repayAcctNo) {
        this.repayAcctNo = repayAcctNo;
    }

    /**
     * @return the repayChannel
     */
    public String getRepayChannel() {
        return repayChannel;
    }

    /**
     * @param repayChannel the repayChannel to set
     */
    public void setRepayChannel(String repayChannel) {
        this.repayChannel = repayChannel;
    }

    /**
     * @return the repayWay
     */
    public String getRepayWay() {
        return repayWay;
    }

    /**
     * @param repayWay the repayWay to set
     */
    public void setRepayWay(String repayWay) {
        this.repayWay = repayWay;
    }

    /**
     * @return the settleChannel
     */
    public String getSettleChannel() {
        return settleChannel;
    }

    /**
     * @param settleChannel the settleChannel to set
     */
    public void setSettleChannel(String settleChannel) {
        this.settleChannel = settleChannel;
    }

    /**
     * @return the startIntDate
     */
    public String getStartIntDate() {
        return startIntDate;
    }

    /**
     * @param startIntDate the startIntDate to set
     */
    public void setStartIntDate(String startIntDate) {
        this.startIntDate = startIntDate;
    }

    /**
     * @return the tranType
     */
    public String getTranType() {
        return tranType;
    }

    /**
     * @param tranType the tranType to set
     */
    public void setTranType(String tranType) {
        this.tranType = tranType;
    }

    /**
     * @return the periodType
     */
    public String getPeriodType() {
        return periodType;
    }

    /**
     * @param periodType the periodType to set
     */
    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    /**
     * @return the acctStat
     */
    public String getAcctStat() {
        return acctStat;
    }

    /**
     * @param acctStat the acctStat to set
     */
    public void setAcctStat(String acctStat) {
        this.acctStat = acctStat;
    }

    /**
     * @return the loanStat
     */
    public String getLoanStat() {
        return loanStat;
    }

    /**
     * @param loanStat the loanStat to set
     */
    public void setLoanStat(String loanStat) {
        this.loanStat = loanStat;
    }

    /**
     * @return the repayTerm
     */
    public Integer getRepayTerm() {
        return repayTerm;
    }

    /**
     * @param repayTerm the repayTerm to set
     */
    public void setRepayTerm(Integer repayTerm) {
        this.repayTerm = repayTerm;
    }

    /**
     * @return the endDate
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * @return the startDate
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getBankSubject() {
        return bankSubject;
    }

    public void setBankSubject(String bankSubject) {
        this.bankSubject = bankSubject;
    }

    /**
     * @return the investeeId
     */
    public String getInvesteeId() {
        return investeeId;
    }

    /**
     * @param investeeId the investeeId to set
     */
    public void setInvesteeId(String investeeId) {
        this.investeeId = investeeId;
    }

    /**
     * @return the investeePrin
     */
    public FabAmount getInvesteePrin() {
        return investeePrin;
    }

    /**
     * @param investeePrin the investeePrin to set
     */
    public void setInvesteePrin(FabAmount investeePrin) {
        this.investeePrin = investeePrin;
    }

    /**
     * @return the investeeInt
     */
    public FabAmount getInvesteeInt() {
        return investeeInt;
    }

    /**
     * @param investeeInt the investeeInt to set
     */
    public void setInvesteeInt(FabAmount investeeInt) {
        this.investeeInt = investeeInt;
    }

    /**
     * @return the investeeDint
     */
    public FabAmount getInvesteeDint() {
        return investeeDint;
    }

    /**
     * @param investeeDint the investeeDint to set
     */
    public void setInvesteeDint(FabAmount investeeDint) {
        this.investeeDint = investeeDint;
    }

    /**
     * @return the recoveryRate
     */
    public Double getRecoveryRate() {
        return recoveryRate;
    }

    /**
     * @param recoveryRate the recoveryRate to set
     */
    public void setRecoveryRate(Double recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    /**
     * @return the finalRate
     */
    public Double getFinalRate() {
        return finalRate;
    }

    /**
     * @param finalRate the finalRate to set
     */
    public void setFinalRate(Double finalRate) {
        this.finalRate = finalRate;
    }

    /**
     * @return the rentRate
     */
    public Double getRentRate() {
        return rentRate;
    }

    /**
     * @param rentRate the rentRate to set
     */
    public void setRentRate(Double rentRate) {
        this.rentRate = rentRate;
    }

    /**
     * @return the floatRate
     */
    public Double getFloatRate() {
        return floatRate;
    }

    /**
     * @param floatRate the floatRate to set
     */
    public void setFloatRate(Double floatRate) {
        this.floatRate = floatRate;
    }

    /**
     * @return the settleFlag
     */
    public String getSettleFlag() {
        return settleFlag;
    }

    /**
     * @param settleFlag the settleFlag to set
     */
    public void setSettleFlag(String settleFlag) {
        this.settleFlag = settleFlag;
    }

    /**
     * @return the investeeFlag
     */
    public String getInvesteeFlag() {
        return investeeFlag;
    }

    /**
     * @param investeeFlag the investeeFlag to set
     */
    public void setInvesteeFlag(String investeeFlag) {
        this.investeeFlag = investeeFlag;
    }

    /**
     * @return the yearFeeRate
     */
    public FabRate getYearFeeRate() {
        return yearFeeRate;
    }

    /**
     * @param yearFeeRate the yearFeeRate to set
     */
    public void setYearFeeRate(FabRate yearFeeRate) {
        this.yearFeeRate = yearFeeRate;
    }

    /**
     * @return the childBrc
     */
    public String getChildBrc() {
        return childBrc;
    }

    /**
     * @param childBrc the childBrc to set
     */
    public void setChildBrc(String childBrc) {
        this.childBrc = childBrc;
    }

    /**
     * @return the tranInfo
     */
    public String getTranInfo() {
        return tranInfo;
    }

    /**
     * @param tranInfo the tranInfo to set
     */
    public void setTranInfo(String tranInfo) {
        this.tranInfo = tranInfo;
    }

    /**
     * @return the discountAcctNo
     */
    public String getDiscountAcctNo() {
        return discountAcctNo;
    }

    /**
     * @param discountAcctNo the discountAcctNo to set
     */
    public void setDiscountAcctNo(String discountAcctNo) {
        this.discountAcctNo = discountAcctNo;
    }

    /**
     * @return the chargeType
     */
    public String getChargeType() {
        return chargeType;
    }

    /**
     * @param chargeType the chargeType to set
     */
    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    /**
     * @return the checkFlag
     */
    public String getCheckFlag() {
        return checkFlag;
    }

    /**
     * @param checkFlag the checkFlag to set
     */
    public void setCheckFlag(String checkFlag) {
        this.checkFlag = checkFlag;
    }

    /**
     * @return the reduceFintAmt
     */
    public Double getReduceFintAmt() {
        return reduceFintAmt;
    }

    /**
     * @param reduceFintAmt the reduceFintAmt to set
     */
    public void setReduceFintAmt(Double reduceFintAmt) {
        this.reduceFintAmt = reduceFintAmt;
    }

    /**
     * @return the reduceIntAmt
     */
    public Double getReduceIntAmt() {
        return reduceIntAmt;
    }

    /**
     * @param reduceIntAmt the reduceIntAmt to set
     */
    public void setReduceIntAmt(Double reduceIntAmt) {
        this.reduceIntAmt = reduceIntAmt;
    }

    public FabAmount getReducePrinAmt() {
        return reducePrinAmt;
    }

    public void setReducePrinAmt(FabAmount reducePrinAmt) {
        this.reducePrinAmt = reducePrinAmt;
    }

    public FabAmount getTailAmt() {
        return tailAmt;
    }

    public void setTailAmt(FabAmount tailAmt) {
        this.tailAmt = tailAmt;
    }

    public FabAmount getDownPayment() {
        return downPayment;
    }

    public void setDownPayment(FabAmount downPayment) {
        this.downPayment = downPayment;
    }

    public FabAmount getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(FabAmount merchantCode) {
        this.merchantCode = merchantCode;
    }

    /**
     * @return the serviceFeeRate
     */
    public FabRate getServiceFeeRate() {
        return serviceFeeRate;
    }

    /**
     * @param serviceFeeRate the serviceFeeRate to set
     */
    public void setServiceFeeRate(FabRate serviceFeeRate) {
        this.serviceFeeRate = serviceFeeRate;
    }

    /**
     * @return the penaltyFeeRate
     */
    public FabRate getPenaltyFeeRate() {
        return penaltyFeeRate;
    }

    /**
     * @param penaltyFeeRate the penaltyFeeRate to set
     */
    public void setPenaltyFeeRate(FabRate penaltyFeeRate) {
        this.penaltyFeeRate = penaltyFeeRate;
    }

    /**
     * @return the dealDate
     */
    public String getDealDate() {
        return dealDate;
    }

    /**
     * @param dealDate the dealDate to set
     */
    public void setDealDate(String dealDate) {
        this.dealDate = dealDate;
    }

    /**
     * @return the penaltyAmt
     */
    public FabAmount getPenaltyAmt() {
        return penaltyAmt;
    }

    /**
     * @param penaltyAmt the penaltyAmt to set
     */
    public void setPenaltyAmt(FabAmount penaltyAmt) {
        this.penaltyAmt = penaltyAmt;
    }

    /**
     * @return the contractBal
     */
    public FabAmount getContractBal() {
        return contractBal;
    }

    /**
     * @param contractBal the contractBal to set
     */
    public void setContractBal(FabAmount contractBal) {
        this.contractBal = contractBal;
    }

    /**
     * @return the repayintBdate
     */
    public String getRepayintBdate() {
        return repayintBdate;
    }

    /**
     * @param repayintBdate the repayintBdate to set
     */
    public void setRepayintBdate(String repayintBdate) {
        this.repayintBdate = repayintBdate;
    }

    /**
     * @return the repayintEdate
     */
    public String getRepayintEdate() {
        return repayintEdate;
    }

    /**
     * @param repayintEdate the repayintEdate to set
     */
    public void setRepayintEdate(String repayintEdate) {
        this.repayintEdate = repayintEdate;
    }

    /**
     * @return the settleDate
     */
    public String getSettleDate() {
        return settleDate;
    }

    /**
     * @param settleDate the settleDate to set
     */
    public void setSettleDate(String settleDate) {
        this.settleDate = settleDate;
    }

    /**
     * @return the termretDate
     */
    public String getTermretDate() {
        return termretDate;
    }

    /**
     * @param termretDate the termretDate to set
     */
    public void setTermretDate(String termretDate) {
        this.termretDate = termretDate;
    }

    /**
     * @return the noretPrin
     */
    public FabAmount getNoretPrin() {
        return noretPrin;
    }

    /**
     * @param noretPrin the noretPrin to set
     */
    public void setNoretPrin(FabAmount noretPrin) {
        this.noretPrin = noretPrin;
    }

    /**
     * @return the balance
     */
    public FabAmount getBalance() {
        return balance;
    }

    /**
     * @param balance the balance to set
     */
    public void setBalance(FabAmount balance) {
        this.balance = balance;
    }

    /**
     * @return the noretInt
     */
    public FabAmount getNoretInt() {
        return noretInt;
    }

    /**
     * @param noretInt the noretInt to set
     */
    public void setNoretInt(FabAmount noretInt) {
        this.noretInt = noretInt;
    }

    /**
     * @return the termcdInt
     */
    public FabAmount getTermcdInt() {
        return termcdInt;
    }

    /**
     * @param termcdInt the termcdInt to set
     */
    public void setTermcdInt(FabAmount termcdInt) {
        this.termcdInt = termcdInt;
    }

    /**
     * @return the feeAmt
     */
    public FabAmount getFeeAmt() {
        return feeAmt;
    }

    /**
     * @param feeAmt the feeAmt to set
     */
    public void setFeeAmt(FabAmount feeAmt) {
        this.feeAmt = feeAmt;
    }

    /**
     * @return the sumFint
     */
    public FabAmount getSumFint() {
        return sumFint;
    }

    /**
     * @param sumFint the sumFint to set
     */
    public void setSumFint(FabAmount sumFint) {
        this.sumFint = sumFint;
    }

    /**
     * @return the termFint
     */
    public FabAmount getTermFint() {
        return termFint;
    }

    /**
     * @param termFint the termFint to set
     */
    public void setTermFint(FabAmount termFint) {
        this.termFint = termFint;
    }

    /**
     * @return the sumrFint
     */
    public FabAmount getSumrFint() {
        return sumrFint;
    }

    /**
     * @param sumrFint the sumrFint to set
     */
    public void setSumrFint(FabAmount sumrFint) {
        this.sumrFint = sumrFint;
    }

    /**
     * @return the transferFlag
     */
    public String getTransferFlag() {
        return transferFlag;
    }

    /**
     * @param transferFlag the transferFlag to set
     */
    public void setTransferFlag(String transferFlag) {
        this.transferFlag = transferFlag;
    }

    /**
     * @return the totalInt
     */
    public FabAmount getTotalInt() {
        return totalInt;
    }

    /**
     * @param totalInt the totalInt to set
     */
    public void setTotalInt(FabAmount totalInt) {
        this.totalInt = totalInt;
    }

    public FabRate getPreDiscountRate() {
        return preDiscountRate;
    }

    public void setPreDiscountRate(FabRate preDiscountRate) {
        this.preDiscountRate = preDiscountRate;
    }

    public FabRate getRealRate() {
        return realRate;
    }

    public void setRealRate(FabRate realRate) {
        this.realRate = realRate;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getRateType() {
        return rateType;
    }

    public void setRateType(String rateType) {
        this.rateType = rateType;
    }

    public String getExpandDate() {
        return expandDate;
    }

    public void setExpandDate(String expandDate) {
        this.expandDate = expandDate;
    }

    public String getBeforeExpandDate() {
        return beforeExpandDate;
    }

    public void setBeforeExpandDate(String beforeExpandDate) {
        this.beforeExpandDate = beforeExpandDate;
    }

    public String getAfterExpandDate() {
        return afterExpandDate;
    }

    public void setAfterExpandDate(String afterExpandDate) {
        this.afterExpandDate = afterExpandDate;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public FabAmount getChannelAmt() {
        return channelAmt;
    }

    public void setChannelAmt(FabAmount channelAmt) {
        this.channelAmt = channelAmt;
    }

    public String getChannelNo() {
        return channelNo;
    }

    public void setChannelNo(String channelNo) {
        this.channelNo = channelNo;
    }

    public String getChannelSubject() {
        return channelSubject;
    }

    public void setChannelSubject(String channelSubject) {
        this.channelSubject = channelSubject;
    }

    public String getCancelAmt() {
        return cancelAmt;
    }

    public void setCancelAmt(String cancelAmt) {
        this.cancelAmt = cancelAmt;
    }

    public String getContDueDate() {
        return contDueDate;
    }

    public void setContDueDate(String contDueDate) {
        this.contDueDate = contDueDate;
    }

    /**
     * @return the cleanFeeRate
     */
    public FabRate getCleanFeeRate() {
        return cleanFeeRate;
    }

    /**
     * @param cleanFeeRate the cleanFeeRate to set
     */
    public void setCleanFeeRate(FabRate cleanFeeRate) {
        this.cleanFeeRate = cleanFeeRate;
    }

    /**
     * @return the intDays
     */
    public String getIntDays() {
        return intDays;
    }

    /**
     * @param intDays the intDays to set
     */
    public void setIntDays(String intDays) {
        this.intDays = intDays;
    }

    public FabRate getOnetimeRate() {
        return onetimeRate;
    }

    public void setOnetimeRate(FabRate onetimeRate) {
        this.onetimeRate = onetimeRate;
    }

    public FabAmount getInSurance() {
        return inSurance;
    }

    public void setInSurance(FabAmount inSurance) {
        this.inSurance = inSurance;
    }

    public String getCustomID() {
        return customID;
    }

    public void setCustomID(String customID) {
        this.customID = customID;
    }

    /**
     * @return the specifiedType
     */
    public String getSpecifiedType() {
        return specifiedType;
    }

    /**
     * @param specifiedType the specifiedType to set
     */
    public void setSpecifiedType(String specifiedType) {
        this.specifiedType = specifiedType;
    }

    public FabAmount getFreeInterest() {
        return freeInterest;
    }

    public void setFreeInterest(FabAmount freeInterest) {
        this.freeInterest = freeInterest;
    }

    public FabAmount getInvesteeFee() {
        return investeeFee;
    }

    public void setInvesteeFee(FabAmount investeeFee) {
        this.investeeFee = investeeFee;
    }

    public String getCompensateFlag() {
        return compensateFlag;
    }

    public void setCompensateFlag(String compensateFlag) {
        this.compensateFlag = compensateFlag;
    }

    public String getExAcctno() {
        return exAcctno;
    }

    public void setExAcctno(String exAcctno) {
        this.exAcctno = exAcctno;
    }

    public String getExPeriod() {
        return exPeriod;
    }

    public void setExPeriod(String exPeriod) {
        this.exPeriod = exPeriod;
    }

    public String getExinvesteeId() {
        return exinvesteeId;
    }

    public void setExinvesteeId(String exinvesteeId) {
        this.exinvesteeId = exinvesteeId;
    }

    public String getFreeDays() {
        return freeDays;
    }

    public void setFreeDays(String freeDays) {
        this.freeDays = freeDays;
    }

    /**
     * @return the debtCompanyin
     */
    public String getDebtCompanyin() {
        return debtCompanyin;
    }

    /**
     * @param debtCompanyin the debtCompanyin to set
     */
    public void setDebtCompanyin(String debtCompanyin) {
        this.debtCompanyin = debtCompanyin;
    }

    /**
     * @return the debtCompanyout
     */
    public String getDebtCompanyout() {
        return debtCompanyout;
    }

    /**
     * @param debtCompanyout the debtCompanyout to set
     */
    public void setDebtCompanyout(String debtCompanyout) {
        this.debtCompanyout = debtCompanyout;
    }

    /**
     * @return the expandPeriod
     */
    public Integer getExpandPeriod() {
        return expandPeriod;
    }

    /**
     * @param expandPeriod the expandPeriod to set
     */
    public void setExpandPeriod(Integer expandPeriod) {
        this.expandPeriod = expandPeriod;
    }

    /**
     * @return the feeRate
     */
    public FabRate getFeeRate() {
        return feeRate;
    }

    /**
     * @param feeRate the feeRate to set
     */
    public void setFeeRate(FabRate feeRate) {
        this.feeRate = feeRate;
    }

    /**
     * Gets the value of feeType.
     *
     * @return the value of feeType
     */
    public String getFeeType() {
        return feeType;
    }

    /**
     * Sets the feeType.
     *
     * @param feeType feeType
     */
    public void setFeeType(String feeType) {
        this.feeType = feeType;

    }

    /**
     * Gets the value of reduceFee.
     *
     * @return the value of reduceFee
     */
    public FabAmount getReduceFee() {
        return reduceFee;
    }

    /**
     * Sets the reduceFee.
     *
     * @param reduceFee reduceFee
     */
    public void setReduceFee(FabAmount reduceFee) {
        this.reduceFee = reduceFee;

    }

    /**
     * @return the switchFee
     */
    public FabAmount getSwitchFee() {
        return switchFee;
    }

    /**
     * @param switchFee the switchFee to set
     */
    public void setSwitchFee(FabAmount switchFee) {
        this.switchFee = switchFee;
    }

    /**
     * Gets the value of entrustedPaybrc.
     *
     * @return the value of entrustedPaybrc
     */
    public String getEntrustedPaybrc() {
        return entrustedPaybrc;
    }

    /**
     * Sets the entrustedPaybrc.
     *
     * @param entrustedPaybrc entrustedPaybrc
     */
    public void setEntrustedPaybrc(String entrustedPaybrc) {
        this.entrustedPaybrc = entrustedPaybrc;

    }

    /**
     * @return the realDate
     */
    public String getRealDate() {
        return realDate;
    }

    /**
     * @param realDate the realDate to set
     */
    public void setRealDate(String realDate) {
        this.realDate = realDate;
    }

    /**
     * @return the paymentTimes
     */
    public String getPaymentTimes() {
        return paymentTimes;
    }

    /**
     * @param paymentTimes the paymentTimes to set
     */
    public void setPaymentTimes(String paymentTimes) {
        this.paymentTimes = paymentTimes;
    }

    /**
     * @return the platformId
     */
    public String getPlatformId() {
        return platformId;
    }

    /**
     * @param platformId the platformId to set
     */
    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    /**
     * @return the exAcctno1
     */
    public String getExAcctno1() {
        return exAcctno1;
    }

    /**
     * @param exAcctno1 the exAcctno1 to set
     */
    public void setExAcctno1(String exAcctno1) {
        this.exAcctno1 = exAcctno1;
    }

    /**
     * @return the exBrc
     */
    public String getExBrc() {
        return exBrc;
    }

    /**
     * @param exBrc the exBrc to set
     */
    public void setExBrc(String exBrc) {
        this.exBrc = exBrc;
    }

    /**
     * @return the exBrc1
     */
    public String getExBrc1() {
        return exBrc1;
    }

    /**
     * @param exBrc1 the exBrc1 to set
     */
    public void setExBrc1(String exBrc1) {
        this.exBrc1 = exBrc1;
    }

    /**
     * @return the deducetionAmt
     */
    public FabAmount getDeducetionAmt() {
        return deducetionAmt;
    }

    /**
     * @param deducetionAmt the deducetionAmt to set
     */
    public void setDeducetionAmt(FabAmount deducetionAmt) {
        this.deducetionAmt = deducetionAmt;
    }

    /**
     * @return the feerepayWay
     */
    public String getFeerepayWay() {
        return feerepayWay;
    }

    /**
     * @param feerepayWay the feerepayWay to set
     */
    public void setFeerepayWay(String feerepayWay) {
        this.feerepayWay = feerepayWay;
    }

    /**
     * @return the feeBrc
     */
    public String getFeeBrc() {
        return feeBrc;
    }

    /**
     * @param feeBrc the feeBrc to set
     */
    public void setFeeBrc(String feeBrc) {
        this.feeBrc = feeBrc;
    }

    /**
     * @return the reduceFeeType
     */
    public String getReduceFeeType() {
        return reduceFeeType;
    }

    /**
     * @param reduceFeeType the reduceFeeType to set
     */
    public void setReduceFeeType(String reduceFeeType) {
        this.reduceFeeType = reduceFeeType;
    }

    /**
     * @return the reduceFeeAmt
     */
    public FabAmount getReduceFeeAmt() {
        return reduceFeeAmt;
    }

    /**
     * @param reduceFeeAmt the reduceFeeAmt to set
     */
    public void setReduceFeeAmt(FabAmount reduceFeeAmt) {
        this.reduceFeeAmt = reduceFeeAmt;
    }

    /**
     * @return the feeBase
     */
    public String getFeeBase() {
        return feeBase;
    }

    /**
     * @param feeBase the feeBase to set
     */
    public void setFeeBase(String feeBase) {
        this.feeBase = feeBase;
    }

    /**
     * @return the advanceSettle
     */
    public String getAdvanceSettle() {
        return advanceSettle;
    }

    /**
     * @param advanceSettle the advanceSettle to set
     */
    public void setAdvanceSettle(String advanceSettle) {
        this.advanceSettle = advanceSettle;
    }

    /**
     * @return the overRate
     */
    public FabRate getOverRate() {
        return overRate;
    }

    /**
     * @param overRate the overRate to set
     */
    public void setOverRate(FabRate overRate) {
        this.overRate = overRate;
    }

    /**
     * @return the adjustAmt
     */
    public FabAmount getAdjustAmt() {
        return adjustAmt;
    }

    /**
     * @param adjustAmt the adjustAmt to set
     */
    public void setAdjustAmt(FabAmount adjustAmt) {
        this.adjustAmt = adjustAmt;
    }

    /**
     * @return the adjustType
     */
    public String getAdjustType() {
        return adjustType;
    }

    /**
     * @param adjustType the adjustType to set
     */
    public void setAdjustType(String adjustType) {
        this.adjustType = adjustType;
    }

    /**
     * @return the tranBrc
     */
    public String getTranBrc() {
        return tranBrc;
    }

    /**
     * @param tranBrc the tranBrc to set
     */
    public void setTranBrc(String tranBrc) {
        this.tranBrc = tranBrc;
    }

    /**
     * @return the switchBrc
     */
    public String getSwitchBrc() {
        return switchBrc;
    }

    /**
     * @param switchBrc the switchBrc to set
     */
    public void setSwitchBrc(String switchBrc) {
        this.switchBrc = switchBrc;
    }

    /**
     * @return the depositType
     */
    public String getDepositType() {
        return depositType;
    }

    /**
     * @param depositType the depositType to set
     */
    public void setDepositType(String depositType) {
        this.depositType = depositType;
    }

    /**
     * @return the dealType
     */
    public String getDealType() {
        return dealType;
    }

    /**
     * @param dealType the dealType to set
     */
    public void setDealType(String dealType) {
        this.dealType = dealType;
    }

    /**
     * @return the calCulatrule
     */
    public String getCalCulatrule() {
        return calCulatrule;
    }

    /**
     * @param calCulatrule the calCulatrule to set
     */
    public void setCalCulatrule(String calCulatrule) {
        this.calCulatrule = calCulatrule;
    }

    /**
     * @return the switchloanType
     */
    public String getSwitchloanType() {
        return switchloanType;
    }

    /**
     * @param switchloanType to set
     */
    public void setSwitchloanType(String switchloanType) {
        this.switchloanType = switchloanType;
    }

    /**
     * @return the lawsuitAmt
     */
    public FabAmount getLawsuitAmt() {
        return lawsuitAmt;
    }

    /**
     * @param lawsuitAmt to set
     */
    public void setLawsuitAmt(FabAmount lawsuitAmt) {
        this.lawsuitAmt = lawsuitAmt;
    }

    /**
     * @return the surplusAmt
     */
    public FabAmount getSurplusAmt() {
        return surplusAmt;
    }

    /**
     * @param surplusAmt to set
     */
    public void setSurplusAmt(FabAmount surplusAmt) {
        this.surplusAmt = surplusAmt;
    }

    /**
     * @return the cooperateAmt
     */
    public FabAmount getCooperateAmt() {
        return cooperateAmt;
    }

    /**
     * @param cooperateAmt to set
     */
    public void setCooperateAmt(FabAmount cooperateAmt) {
        this.cooperateAmt = cooperateAmt;
    }

    /**
     * @return the advanceAmt
     */
    public FabAmount getAdvanceAmt() {
        return advanceAmt;
    }

    /**
     * @param advanceAmt to set
     */
    public void setAdvanceAmt(FabAmount advanceAmt) {
        this.advanceAmt = advanceAmt;
    }

    /**
     * @return the repayList
     */
    public String getRepayList() {
        return repayList;
    }

    /**
     * @param repayList the repayList to set
     */
    public void setRepayList(String repayList) {
        this.repayList = repayList;
    }

    /**
     * @return the singleAmt
     */
    public FabAmount getSingleAmt() {
        return singleAmt;
    }

    /**
     * @param singleAmt the singleAmt to set
     */
    public void setSingleAmt(FabAmount singleAmt) {
        this.singleAmt = singleAmt;
    }

    /**
     * @return the lawsuitList
     */
    public String getLawsuitList() {
        return lawsuitList;
    }

    /**
     * @param lawsuitList the lawsuitList to set
     */
    public void setLawsuitList(String lawsuitList) {
        this.lawsuitList = lawsuitList;
    }

    public FabRate getInterestDiscount() {
        return interestDiscount;
    }

    public void setInterestDiscount(FabRate interestDiscount) {
        this.interestDiscount = interestDiscount;
    }

    public String getPayerAccountSubject() {
        return payerAccountSubject;
    }

    public void setPayerAccountSubject(String payerAccountSubject) {
        this.payerAccountSubject = payerAccountSubject;
    }

    public Integer getPeriodMinDays() {
        return periodMinDays;
    }

    public void setPeriodMinDays(Integer periodMinDays) {
        this.periodMinDays = periodMinDays;
    }

    public Integer getFreefeeDays() {
        return freefeeDays;
    }

    public void setFreefeeDays(Integer freefeeDays) {
        this.freefeeDays = freefeeDays;
    }

    public String getDiscountIntFlag() {
        return discountIntFlag;
    }

    public void setDiscountIntFlag(String discountIntFlag) {
        this.discountIntFlag = discountIntFlag;
    }

    public FabAmount getFreeFee() {
        return freeFee;
    }

    public void setFreeFee(FabAmount freeFee) {
        this.freeFee = freeFee;
    }

    public FabRate getFeeDiscount() {
        return feeDiscount;
    }

    public void setFeeDiscount(FabRate feeDiscount) {
        this.feeDiscount = feeDiscount;
    }

    public double getFeeDiscountRatio() {
        return feeDiscountRatio;
    }

    public void setFeeDiscountRatio(double feeDiscountRatio) {
        this.feeDiscountRatio = feeDiscountRatio;
    }

    public FabRate getFreeRate() {
        return freeRate;
    }

    public void setFreeRate(FabRate freeRate) {
        this.freeRate = freeRate;
    }


    public String getSalesType() {
        return salesType;
    }

    public void setSalesType(String salesType) {
        this.salesType = salesType;
    }

    public String getDiscountFeeFlag() {
        return discountFeeFlag;
    }

    public void setDiscountFeeFlag(String discountFeeFlag) {
        this.discountFeeFlag = discountFeeFlag;
    }

    public String getInvesteeAcctno() {
        return investeeAcctno;
    }

    public void setInvesteeAcctno(String investeeAcctno) {
        this.investeeAcctno = investeeAcctno;
    }

    public FabRate getInvesteePropor() {
        return investeePropor;
    }

    public void setInvesteePropor(FabRate investeePropor) {
        this.investeePropor = investeePropor;
    }


    public String getInvesteeRepayFlg() {
        return investeeRepayFlg;
    }

    public void setInvesteeRepayFlg(String investeeRepayFlg) {
        this.investeeRepayFlg = investeeRepayFlg;
    }

    @Override
    public String getReserve() {
        return reserve;
    }

    @Override
    public void setReserve(String reserve) {
        this.reserve = reserve;
    }

    public FabAmount getReduceDamage() {
        return reduceDamage;
    }

    public void setReduceDamage(FabAmount reduceDamage) {
        this.reduceDamage = reduceDamage;
    }

    public void setPlatformFee(FabAmount platformFee) {
        this.platformFee = platformFee;
    }

    public FabAmount getPlatformTotalAmt() {
        return platformTotalAmt;
    }

    public void setPlatformTotalAmt(FabAmount platformTotalAmt) {
        this.platformTotalAmt = platformTotalAmt;
    }

    public String getFirstRepayDate() {
        return firstRepayDate;
    }

    public void setFirstRepayDate(String firstRepayDate) {
        this.firstRepayDate = firstRepayDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public String getNonAccrualDate() {
        return nonAccrualDate;
    }

    public void setNonAccrualDate(String nonAccrualDate) {
        this.nonAccrualDate = nonAccrualDate;
    }

    public String getChargeAginstFlag() {
        return chargeAginstFlag;
    }

    public void setChargeAginstFlag(String chargeAginstFlag) {
        this.chargeAginstFlag = chargeAginstFlag;
    }

    public String getChargeAginstDate() {
        return chargeAginstDate;
    }

    public void setChargeAginstDate(String chargeAginstDate) {
        this.chargeAginstDate = chargeAginstDate;
    }

    public String getFeeBusiType() {
        return feeBusiType;
    }

    public void setFeeBusiType(String feeBusiType) {
        this.feeBusiType = feeBusiType;
    }

    public String getFeeBusiNo() {
        return feeBusiNo;
    }

    public void setFeeBusiNo(String feeBusiNo) {
        this.feeBusiNo = feeBusiNo;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public void setCaseNo(String caseNo) {
        this.caseNo = caseNo;
    }

    public String getLawFirm() {
        return lawFirm;
    }

    public void setLawFirm(String lawFirm) {
        this.lawFirm = lawFirm;
    }

    public String getAcceptCourt() {
        return acceptCourt;
    }

    public void setAcceptCourt(String acceptCourt) {
        this.acceptCourt = acceptCourt;
    }

    public String getPayDate() {
        return payDate;
    }

    public void setPayDate(String payDate) {
        this.payDate = payDate;
    }


    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getPayorderNo() {
        return payorderNo;
    }

    public void setPayorderNo(String payorderNo) {
        this.payorderNo = payorderNo;
    }

    public FabAmount getOpenAmt() {
        return openAmt;
    }

    public void setOpenAmt(FabAmount openAmt) {
        this.openAmt = openAmt;
    }

    public String getFeeFunCode() {
        return feeFunCode;
    }

    public void setFeeFunCode(String feeFunCode) {
        this.feeFunCode = feeFunCode;
    }

    public String getNewfeeFunCode() {
        return newfeeFunCode;
    }

    public void setNewfeeFunCode(String newfeeFunCode) {
        this.newfeeFunCode = newfeeFunCode;
    }

    public String getFeeReqNo() {
        return feeReqNo;
    }

    public void setFeeReqNo(String feeReqNo) {
        this.feeReqNo = feeReqNo;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public String getBillList() {
        return billList;
    }

    public void setBillList(String billList) {
        this.billList = billList;
    }
}

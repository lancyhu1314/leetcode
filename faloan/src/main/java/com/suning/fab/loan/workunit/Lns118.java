package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.domain.TblLnspenintprovreg;
import com.suning.fab.loan.domain.TblLnspenintprovregdtl;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @param
 * @author LH
 * @version V1.0.1
 * @return
 * @exception
 * @see -开户-生成贷款封顶计息拓展
 */
@Scope("prototype")

@Repository
public class Lns118 extends WorkUnit {

    String openBrc;
    LoanAgreement loanAgreement;    //贷款协议 包含合同 客户 渠道等信息
    String productCode;
    FabAmount freeInterest;

    FabRate interestDiscount;//折前利率

    // 借新还旧旧借据封顶值:默认值为0，计算的时候统一加上这个值，非借新还旧的加0
    FabAmount origDynamicCapValue = new FabAmount(0.00);
    //减免利息标志
    String discountIntFlag;
    //费用减免相关信息
    Map<String,Object> feeReduceConfig=new HashMap<String,Object>();

    @Override
    public void run() throws Exception {
        //新房抵贷用动态封顶
        if (VarChecker.asList("2412615","4010002").contains(productCode)   ) {
            updateBasicInfo("C");
//			interestCapped();
        }


        if (!VarChecker.isEmpty(loanAgreement.getInterestAgreement().queryDynamicCapRate(tranctx.getBrc()))) {
            if ("51230004".equals(tranctx.getBrc())) {
                ListMap pkgList1 = tranctx.getRequestDict("pkgList1");
                if (!VarChecker.isEmpty(pkgList1)) {
                    //没有多资金方
                    for (PubDict pkg : pkgList1.getLoopmsg()) {
                        if (!VarChecker.asList("R5103", "R5126").contains(PubDict.getRequestDict(pkg, "investeeId").toString()))
                            loanAgreement.getInterestAgreement().setDynamicCapRate(new JSONObject());
                    }
                }
            }

        }

        //新的封顶计息利率不为空
        //不是扣息放款
        if (!VarChecker.isEmpty(loanAgreement.getInterestAgreement().queryDynamicCapRate(tranctx.getBrc()))
                && (VarChecker.isEmpty(loanAgreement.getContract().getDiscountAmt()) || !loanAgreement.getContract().getDiscountAmt().isPositive())) {
            newCapped();
        }
        if(!VarChecker.isEmpty(this.getFreeInterest())&&this.getFreeInterest().isPositive()&&!VarChecker.isEmpty(this.getInterestDiscount())&&this.getInterestDiscount().isPositive()){
            throw new FabException("LNS234");
        }
        if (!VarChecker.isEmpty(this.getFreeInterest())) {
            //等本等息用券 限制去掉
//            if ("0".equals(loanAgreement.getWithdrawAgreement().getRepayWay()))
//                throw new FabException("LNS182");
            intersetFee();
        }

        ListMap pkgList = getTranctx().getRequestDict("pkgList3");
        if(pkgList != null && pkgList.size() > 0) {
            for (PubDict pkg : pkgList.getLoopmsg()) {
                if (VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feerepayWay")) ||
                        VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeBrc")) ||
                        VarChecker.isEmpty(PubDict.getRequestDict(pkg, "feeType"))) {
                    throw new FabException("LNS055", "费用入参");
                }
                if(PubDict.getRequestDict(pkg, "freeFee")!=null&&PubDict.getRequestDict(pkg, "feeDiscount")!=null&&((FabAmount)PubDict.getRequestDict(pkg, "freeFee")).isPositive()&&((FabRate)PubDict.getRequestDict(pkg, "feeDiscount")).isPositive()){
                    throw new FabException("LNS234");
                }

                if(PubDict.getRequestDict(pkg, "freeFee")!=null&&((FabAmount)PubDict.getRequestDict(pkg, "freeFee")).isPositive()){
                    feeReduceConfig.put(PubDict.getRequestDict(pkg, "feeType").toString()+":"+PubDict.getRequestDict(pkg, "feerepayWay")+":"+ConstantDeclare.PARACONFIG.FREEFEE,PubDict.getRequestDict(pkg, "freeFee"));
                }
                //PubDict.getRequestDict(pkg, "feeType").toString()+":"+PubDict.getRequestDict(pkg, "feerepayWay")+":"+   --费用打折券目前暂时只支持一种费率打折
                if(PubDict.getRequestDict(pkg, "feeDiscount")!=null&&((FabRate)PubDict.getRequestDict(pkg, "feeDiscount")).isPositive()){
                    feeReduceConfig.put(PubDict.getRequestDict(pkg, "feeType").toString()+":"+PubDict.getRequestDict(pkg, "feerepayWay")+":"+ConstantDeclare.PARACONFIG.FEEDISCOUNT,((FabRate)PubDict.getRequestDict(pkg, "feeDiscount")).getVal().doubleValue());
                }
                //贴费标志  PubDict.getRequestDict(pkg, "feeType").toString()+":"+PubDict.getRequestDict(pkg, "feerepayWay")+":"+
                if(StringUtil.isNotNull(PubDict.getRequestDict(pkg, "discountFeeFlag"))){
                    feeReduceConfig.put(ConstantDeclare.PARACONFIG.DISCOUNTFEEFLAG,PubDict.getRequestDict(pkg, "discountFeeFlag"));
                }
            }
        }

        insertExtendDetail();
    }

    //插表 lnspenintprovreg  DTFD 动态封顶
    private void newCapped() throws FabException {

        TblLnspenintprovreg lnspenintprovreg = new TblLnspenintprovreg();
        lnspenintprovreg.setBegindate(java.sql.Date.valueOf(loanAgreement.getContract().getContractStartDate()));
        lnspenintprovreg.setBilltype(ConstantDeclare.KEYNAME.DTFD);
        lnspenintprovreg.setBrc(getOpenBrc());
        lnspenintprovreg.setReceiptno(loanAgreement.getContract().getReceiptNo());
        lnspenintprovreg.setCcy(loanAgreement.getContract().getCcy().getCcy());
        lnspenintprovreg.setTaxrate(new BigDecimal(loanAgreement.getInterestAgreement().queryDynamicCapRate(tranctx.getBrc()).toString()));
        lnspenintprovreg.setTotalinterest(BigDecimal.valueOf(origDynamicCapValue.getVal()));
        lnspenintprovreg.setTotaltax(BigDecimal.valueOf(0.00));


        //等本等息
        if(ConstantDeclare.REPAYWAY.isEqualInterest(loanAgreement.getWithdrawAgreement().getRepayWay())) {
            //封顶值封顶利率/360*（合同到期日—起息日期）*放款金额
            lnspenintprovreg.setTotalinterest(
                    BigDecimal.valueOf(loanAgreement.getInterestAgreement().queryDynamicCapRate(tranctx.getBrc())).divide(new BigDecimal("360"), 20, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(loanAgreement.getContract().getContractStartDate(), loanAgreement.getContract().getContractEndDate())))
                            .multiply(BigDecimal.valueOf(loanAgreement.getContract().getContractAmt().getVal())).setScale(2, BigDecimal.ROUND_HALF_UP).add(BigDecimal.valueOf(origDynamicCapValue.getVal()))
            );
            lnspenintprovreg.setEnddate(java.sql.Date.valueOf(loanAgreement.getContract().getContractEndDate()));
            lnspenintprovreg.setTotallist(1);
            TblLnspenintprovregdtl penintprovregdtl = new TblLnspenintprovregdtl();

            //登记动态封顶 明细
            penintprovregdtl.setReceiptno(loanAgreement.getContract().getReceiptNo());
            penintprovregdtl.setBrc(tranctx.getBrc());
            penintprovregdtl.setCcy(loanAgreement.getContract().getCcy().getCcy());
            penintprovregdtl.setTrandate(Date.valueOf(tranctx.getTranDate()));
            penintprovregdtl.setSerseqno(tranctx.getSerSeqNo());
            penintprovregdtl.setTxnseq(0);//默认给0，每次只会新增一条数据
            penintprovregdtl.setPeriod(0);//默认给0
            penintprovregdtl.setListno(1);
            penintprovregdtl.setBilltype(ConstantDeclare.KEYNAME.DTFD);
            penintprovregdtl.setTaxrate(0.00);
            penintprovregdtl.setBegindate(lnspenintprovreg.getBegindate());
            penintprovregdtl.setEnddate(lnspenintprovreg.getEnddate());
            penintprovregdtl.setTimestamp(new java.util.Date().toString());
            penintprovregdtl.setInterest(lnspenintprovreg.getTotalinterest().doubleValue());
            penintprovregdtl.setTax(0.00);
            penintprovregdtl.setReserv1("0.00");//剩余本金
            penintprovregdtl.setReserv2(loanAgreement.getInterestAgreement().queryDynamicCapRate(tranctx.getBrc()).toString());//动态封顶利率

            //登记详表
            if (new FabAmount(penintprovregdtl.getInterest()).isPositive()) {
                try {
                    DbAccessUtil.execute("Lnspenintprovregdtl.insert", penintprovregdtl);
                } catch (FabSqlException e) {
                    throw new FabException(e, "SPS100", "Lnspenintprovregdtl");
                }
            }
        } else {
            lnspenintprovreg.setEnddate(java.sql.Date.valueOf(loanAgreement.getContract().getContractStartDate()));
            lnspenintprovreg.setTotallist(0);
        }


        //直接插入表中 计提直接更新
        try {
            DbAccessUtil.execute("Lnspenintprovreg.insert", lnspenintprovreg);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100", "Lnspenintprovreg");
        }

    }

    /**
     * 封顶计息
     */
    private void interestCapped() throws FabException {
        //新增主文件拓展表
        Map<String, Object> exParam = new HashMap<String, Object>();
        exParam.put("acctno", loanAgreement.getContract().getReceiptNo());
        exParam.put("openbrc", getOpenBrc());
        exParam.put("brc", getOpenBrc());
        exParam.put("key", "FDJX");
        exParam.put("value1", getTranctx().getTranDate());
        //封顶值计算
        exParam.put("value2", 0.0);
        exParam.put("value3", loanAgreement.getInterestAgreement().getCapRate());
        try {
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_118", exParam);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsbasicinfoex");
        }

        //新增主文件动态拓展表
        Map<String, Object> dynExParam = new HashMap<String, Object>();
        dynExParam.put("acctno", loanAgreement.getContract().getReceiptNo());
        dynExParam.put("openbrc", getOpenBrc());
        dynExParam.put("brc", getOpenBrc());
        dynExParam.put("sumprin", 0.0);
        dynExParam.put("sumint", 0.0);
        dynExParam.put("sumdint", 0.0);
        dynExParam.put("sumcint", 0.0);
        dynExParam.put("sumlbcdint", 0.0);
        dynExParam.put("sumgint", 0.0);
        dynExParam.put("trandate", CalendarUtil.before(loanAgreement.getContract().getStartIntDate(), getTranctx().getTranDate()) ? loanAgreement.getContract().getStartIntDate() : getTranctx().getTranDate());
        try {
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfodyn_118", dynExParam);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100", "lnsbasicinfodyn");
        }


        updateBasicInfo("C");
    }

    private void intersetFee() throws FabException {

        if (freeInterest.isNegative())
            throw new FabException("LNS169", "免息金额", "freeInterest");
        if (freeInterest.isZero()) {
            return;
        }
        //新增主文件拓展表
        Map<String, Object> exParam = new HashMap<String, Object>();
        exParam.put("acctno", loanAgreement.getContract().getReceiptNo());
        exParam.put("openbrc", getOpenBrc());
        exParam.put("brc", getOpenBrc());

        //现金贷主文件静态拓展表
        exParam.put("key", "MX");
        exParam.put("value1", null);
        exParam.put("value2", this.getFreeInterest().getVal().toString());
        exParam.put("value3", null);

        try {
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_118", exParam);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS100", "lnsbasicinfoex");
        }

        //新增主文件动态拓展表
        Map<String, Object> dynExParam = new HashMap<String, Object>();
        dynExParam.put("acctno", loanAgreement.getContract().getReceiptNo());
        dynExParam.put("openbrc", getOpenBrc());
        dynExParam.put("brc", getOpenBrc());
        dynExParam.put("sumprin", 0.0);
        dynExParam.put("sumint", 0.0);
        dynExParam.put("sumdint", 0.0);
        dynExParam.put("sumcint", 0.0);
        dynExParam.put("sumlbcdint", 0.0);
        dynExParam.put("sumgint", 0.0);
        dynExParam.put("trandate", CalendarUtil.before(loanAgreement.getContract().getStartIntDate(), getTranctx().getTranDate()) ? loanAgreement.getContract().getStartIntDate() : getTranctx().getTranDate());
        FaloanJson faloanJson = new FaloanJson();
        faloanJson.put(this.getFreeInterest().getVal(), "MX");
        dynExParam.put("tunneldata", faloanJson.toString());

        try {
            DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfodyn_118", dynExParam);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsbasicinfodyn");
        }


        updateBasicInfo("F");
    }

    /**
     * 添加合同扩展信息
     * 折前融担费率：feeDiscount,贴费标志：discountFeeFlag,贴息标志：discountIntFlag 折前利率
     * @throws FabException
     */
    public void insertExtendDetail()throws FabException{
        //隧道字段
        Map tunnelData=new HashMap();
        //折前利率
        if(!VarChecker.isEmpty(this.getInterestDiscount())&&this.getInterestDiscount().isPositive()){
            tunnelData.put(ConstantDeclare.PARACONFIG.ZQLL,this.getInterestDiscount().getVal().doubleValue());
        }

        if(StringUtil.isNotNull(discountIntFlag)){
            tunnelData.put(ConstantDeclare.PARACONFIG.DISCOUNTINTFLAG,discountIntFlag);
        }

        //费用减免相关配置
        tunnelData.putAll(feeReduceConfig);
        if(tunnelData.size()>0){
            //新增主文件拓展表
            Map<String, Object> exParam = new HashMap();
            exParam.put("acctno", loanAgreement.getContract().getReceiptNo());
            exParam.put("openbrc", getOpenBrc());
            exParam.put("brc", getOpenBrc());
            //现金贷主文件静态拓展表
            exParam.put("key", "EXTEND");
            exParam.put("value1", null);
            exParam.put("value2", null);
            exParam.put("value3", null);
            exParam.put("tunneldata", JsonTransfer.ToJson(tunnelData));
            try {
                DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
            } catch (FabSqlException e) {
                throw new FabException(e, "SPS100", "lnsbasicinfoex");
            }
        }

    }

    private void updateBasicInfo(String flag1) throws FabException {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("acctno", loanAgreement.getContract().getReceiptNo());
        param.put("openbrc", getOpenBrc());
        param.put("flag1", flag1);
        try {
            DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_rate605", param);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "lnsbasicinfo");
        }
    }


    /**
     * @return the loanAgreement
     */
    public LoanAgreement getLoanAgreement() {
        return loanAgreement;
    }

    /**
     * @param loanAgreement the loanAgreement to set
     */
    public void setLoanAgreement(LoanAgreement loanAgreement) {
        this.loanAgreement = loanAgreement;
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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }


    public FabAmount getFreeInterest() {
        return freeInterest;
    }


    public void setFreeInterest(FabAmount freeInterest) {
        this.freeInterest = freeInterest;
    }

    /**
     * @return the origDynamicCapValue
     */
    public FabAmount getOrigDynamicCapValue() {
        return origDynamicCapValue;
    }

    /**
     * @param origDynamicCapValue to set
     */
    public void setOrigDynamicCapValue(FabAmount origDynamicCapValue) {
        this.origDynamicCapValue = origDynamicCapValue;
    }

    public FabRate getInterestDiscount() {
        return interestDiscount;
    }

    public void setInterestDiscount(FabRate interestDiscount) {
        this.interestDiscount = interestDiscount;
    }

    public String getDiscountIntFlag() {
        return discountIntFlag;
    }

    public void setDiscountIntFlag(String discountIntFlag) {
        this.discountIntFlag = discountIntFlag;
    }
}

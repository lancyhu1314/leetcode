package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈转非应计校验及幂等〉
 * 〈功能详细描述〉：转非应计交易入参校验，幂等登记
 *
 * @Author
 * @Date 2022-08-29
 * @see
 */
@Scope("prototype")
@Repository
public class Lns535 extends WorkUnit {


    //幂等流水
    String serialNo;
    //借据号
    String receiptNo;
    // 功能码
    String functionCode;
    // 转非应计日期
    String nonAccrualDate;
    // 是否冲销非应计前收入
    String chargeAginstFlag;
    // 冲销收入开始日
    String chargeAginstDate;

    // 主文件bean
    TblLnsbasicinfo lnsbasicinfo;

    @Override
    public void run() throws Exception {


        TranCtx ctx = getTranctx();

        //查询主文件
        Map<String, Object> param = new HashMap<String, Object>();
        //账号
        param.put("acctno1", receiptNo);
        //机构
        param.put("openbrc", ctx.getBrc());
        List<TblLnsbasicinfo> lnsbasicinfoList;
        try {
            //取主文件信息
            lnsbasicinfoList = DbAccessUtil.queryForList("CUSTOMIZE.query_non_acctno", param, TblLnsbasicinfo.class);
        } catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "query_non_acctno");
        }
        //无此借据号
        if (lnsbasicinfoList == null || lnsbasicinfoList.size() == 0) {
            throw new FabException("LNS117", receiptNo);
        }
        //不支持一个借据号对应多笔贷款
        if (lnsbasicinfoList.size() > 1) {
            throw new FabException("LNS126", receiptNo, "不支持一个借据号对应多笔贷款");
        }
        lnsbasicinfo = lnsbasicinfoList.get(0);

        //空值校验， 默认值赋值
        checkeInputAndDefault(lnsbasicinfo.getBeginintdate());
        //主文件校验
        checkBasicInfo();


        //幂等登记
        TblLnsinterface lnsinterface = new TblLnsinterface();
        lnsinterface.setTrandate(ctx.getTermDate());
        lnsinterface.setSerialno(getSerialNo());
        lnsinterface.setAccdate(ctx.getTranDate());
        lnsinterface.setSerseqno(ctx.getSerSeqNo());
        lnsinterface.setTrancode(ctx.getTranCode());
        lnsinterface.setBrc(ctx.getBrc());
        lnsinterface.setAcctname(lnsbasicinfo.getName());
        lnsinterface.setUserno(lnsbasicinfo.getDiacct());
        lnsinterface.setAcctno(lnsbasicinfo.getAcctno());
        lnsinterface.setTranamt(0.00);
        lnsinterface.setMagacct(receiptNo);
        lnsinterface.setOrgid(lnsbasicinfo.getInvestee());
        lnsinterface.setBankno(lnsbasicinfo.getOutserialno());
        lnsinterface.setReserv2(chargeAginstFlag);
        lnsinterface.setReserv3(chargeAginstDate);
        lnsinterface.setReserv4(nonAccrualDate);
        lnsinterface.setReserv5(lnsbasicinfo.getChanneltype());

        try {
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
        } catch (FabSqlException e) {
            //交易幂等
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            } else
                throw new FabException(e, "SPS100", "lnsinterface");
        }
    }

    //主文件校验
    private void checkBasicInfo() throws FabException {
        // 状态校验
        if (VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE,
                ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
                ConstantDeclare.LOANSTATUS.LOANSTATUS_DEBTTRANS).contains(lnsbasicinfo.getLoanstat().trim())) {
            throw new FabException("LNS254", receiptNo, lnsbasicinfo.getLoanstat().trim());
        }

//        //拦截 房抵贷
//        if ("2412615".equals(lnsbasicinfo.getPrdcode()))
//            throw new FabException("LNS255");

        // 日期校验
        if (CalendarUtil.before(nonAccrualDate, lnsbasicinfo.getBeginintdate())) {
            throw new FabException("LNS256", "转非应计日期", "起息日");
        }
        if (CalendarUtil.before(tranctx.getTranDate(), nonAccrualDate)) {
            throw new FabException("LNS256", "当前日期", "转非应计日期");
        }
        if (CalendarUtil.before(nonAccrualDate, chargeAginstDate)) {
            throw new FabException("LNS256", "转非应计日期", "冲销收入开始日");
        }
        if ("N".equals(chargeAginstFlag) && !nonAccrualDate.equals(chargeAginstDate)) {
            throw new FabException("SPS106", "chargeAginstDate");
        }
    }

    //参数校验
    private void checkeInputAndDefault(String StartIntDate) throws FabException {

        // 空值校验
        if (VarChecker.isEmpty(serialNo))
            throw new FabException("LNS055", "渠道流水");
        if (VarChecker.isEmpty(receiptNo))
            throw new FabException("LNS055", "借据号");
        if (VarChecker.isEmpty(functionCode))
            throw new FabException("LNS055", "功能码");
        if (!VarChecker.asList("51030000", "51260000", "51310000", "51050000").contains(tranctx.getBrc())) {
            throw new FabException("LNS126", receiptNo, "只支持5103、5126、5131、5105的机构号");
        }

        // 转非应计日期，不传值默认为交易日（会计日、非自然日）
        if (VarChecker.isEmpty(nonAccrualDate))
            setNonAccrualDate(tranctx.getTranDate());

        // 是否冲销非应计前收入（Y/N），不传值默认Y
        if (VarChecker.isEmpty(chargeAginstFlag))
            setChargeAginstFlag("Y");

        // 冲销收入开始日，不传值默认起息日
        if (VarChecker.isEmpty(chargeAginstDate))
            if ("N".equals(chargeAginstFlag))
                setChargeAginstDate(nonAccrualDate);
            else if ("Y".equals(chargeAginstFlag))
                setChargeAginstDate(StartIntDate);
    }

    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
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
}

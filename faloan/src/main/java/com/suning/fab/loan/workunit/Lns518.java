package com.suning.fab.loan.workunit;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈通过核销的结息〉
 * 〈功能详细描述〉：
 *
 * @Author 18049705
 * @Date Created in 14:47 2018/11/19
 * @see
 */
@Scope("prototype")
@Repository
public class Lns518 extends WorkUnit {

    TblLnsbasicinfo lnsbasicinfo;
    //借据号
    String receiptNo;
    //核销金额
    FabAmount cancelAmt;
    //渠道流水
    String serialNo;
    //开户日
    String startIntDate;
    //合同到期日
    String contDueDate;
    String  cancelFlag = "0";
    List<Map<String, Object>> lnsaccountdyninfoList ;

    @Override
    public  void run() throws  Exception{

        //空值校验
        checkeInput();
        TranCtx ctx = getTranctx();

        //查询主文件
        Map<String,Object> param = new HashMap<String,Object>();
        //账号
        param.put("acctno1", receiptNo);
        //机构
        param.put("openbrc", ctx.getBrc());
        List<TblLnsbasicinfo> lnsbasicinfoList;
        try {
            //取主文件信息
            lnsbasicinfoList = DbAccessUtil.queryForList("CUSTOMIZE.query_non_acctno", param, TblLnsbasicinfo.class);
        }catch (FabSqlException e) {
            throw new FabException(e, "SPS102", "query_non_acctno");
        }
        //无此借据号
        if(lnsbasicinfoList == null||lnsbasicinfoList.size()==0){
            throw new FabException("LNS117",receiptNo);
        }
        //不支持一个借据号对应多笔贷款
        if(lnsbasicinfoList.size()>1){
            throw new FabException("LNS126",receiptNo,"不支持一个借据号对应多笔贷款");
        }
        lnsbasicinfo = lnsbasicinfoList.get(0);
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
        lnsinterface.setTranamt(cancelAmt.getVal());
        lnsinterface.setMagacct(receiptNo);
        lnsinterface.setOrgid(lnsbasicinfo.getInvestee());
        lnsinterface.setBankno(lnsbasicinfo.getOutserialno());
        lnsinterface.setReserv5(lnsbasicinfo.getChanneltype());
        try{
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
        } catch(FabSqlException e) {
            //交易幂等
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {

                cancelFlag = "1";
                throw new FabException(e,ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            }
            else
                throw new FabException(e, "SPS100", "lnsinterface");
        }

        //核销金额是否等于该笔借据下所有本金之和  校验
        //获取动态表信息



        param.put("brc", ctx.getBrc());
        param.put("acctno", lnsbasicinfo.getAcctno());
        try {
            lnsaccountdyninfoList = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsaccountdyninfo", param);
        } catch (FabSqlException e) {
            LoggerUtil.error("查询lnsaccountdyninfo表错误{}", e);
            throw new FabException("SPS103","query_lnsaccountdyninfo");
        }
        //统计未还本金和
        FabAmount totalPrin = new FabAmount();
        //统计所需核销的账户类型和金额
        for(Map<String, Object> lnsaccountdyninfo:lnsaccountdyninfoList){
            if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(String.valueOf(lnsaccountdyninfo.get("ACCTSTAT")).trim().split("\\.")[0]))
                totalPrin.selfAdd(new FabAmount(Double.valueOf(lnsaccountdyninfo.get("CURRBAL").toString())));
        }
        if(!totalPrin.sub(cancelAmt).isZero())
            throw new FabException("LNS115");//核销金额与剩余本金不等
    }

    //主文件校验
    private void checkBasicInfo() throws FabException {
        if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat().trim())){
            throw new FabException("ACC108",receiptNo);
        }
        //已经核销的账户
        if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION.equals(lnsbasicinfo.getLoanstat().trim())){
            throw new FabException("LNS116",receiptNo);
        }
        //拦截 无追保理或无追保理-买方付息
        if(Arrays.asList("3010006","3010013","3010014","3010015").contains(lnsbasicinfo.getPrdcode()) )
            throw new FabException("LNS126",receiptNo,"不支持无追保理和无追保理-买方付息");
        //拦截 房抵贷
        if("2412615".equals(lnsbasicinfo.getPrdcode()))
            throw new FabException("LNS126",receiptNo,"不支持房抵贷");


        //校验开户日和合同结束日
        if(!CalendarUtil.equalDate(lnsbasicinfo.getBeginintdate(), startIntDate)||
                !CalendarUtil.equalDate(lnsbasicinfo.getContduedate(),contDueDate)){
            throw new FabException("LNS118",receiptNo);
        }
    }

    //参数校验
    private void  checkeInput()  throws FabException{

        if (VarChecker.isEmpty(receiptNo))
            throw new FabException("LNS055","核销借据号");
        if(cancelAmt == null || cancelAmt.isNegative())
            throw new FabException("LNS056","核销金额");
        if (VarChecker.isEmpty(startIntDate))
            throw new FabException("LNS055","起息日");
        if (VarChecker.isEmpty(contDueDate))
            throw new FabException("LNS055","合同到期日");
        if(!VarChecker.asList("51050000","51030000","51260000","51340000","51310000").contains(tranctx.getBrc())){
            throw new FabException("LNS126",receiptNo,"只支持5105、5103、5126、5134、5131的机构号");
        }


    }

    /**
     *
     * @return lnsbasicinfo get
     */
    public TblLnsbasicinfo getLnsbasicinfo() {
        return lnsbasicinfo;
    }

    /**
     *
     * @param lnsbasicinfo  set
     */
    public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
        this.lnsbasicinfo = lnsbasicinfo;
    }

    /**
     *
     * @return receiptNo get
     */
    public String getReceiptNo() {
        return receiptNo;
    }

    /**
     *
     * @param receiptNo set
     */
    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    /**
     *
     * @return cancelAmt get
     */
    public FabAmount getCancelAmt() {
        return cancelAmt;
    }

    /**
     *
     * @param cancelAmt set
     */
    public void setCancelAmt(FabAmount cancelAmt) {
        this.cancelAmt = cancelAmt;
    }

    /**
     *
     * @return serialNo get
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     *
     * @param serialNo set
     */
    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    /**
     *
     * @return startIntDate get
     */
    public String getStartIntDate() {
        return startIntDate;
    }

    /**
     *
     * @param startIntDate set
     */
    public void setStartIntDate(String startIntDate) {
        this.startIntDate = startIntDate;
    }

    /**
     *
     * @return contDueDate get
     */
    public String getContDueDate() {
        return contDueDate;
    }

    /**
     *
     * @param contDueDate set
     */
    public void setContDueDate(String contDueDate) {
        this.contDueDate = contDueDate;
    }

    /**
     *
     * @return cancelFlag get
     */
    public String getCancelFlag() {
        return cancelFlag;
    }

    /**
     *
     * @param cancelFlag set
     */
    public void setCancelFlag(String cancelFlag) {
        this.cancelFlag = cancelFlag;
    }

    /**
     *
     * @return lnsaccountdyninfoList get
     */
    public List<Map<String, Object>> getLnsaccountdyninfoList() {
        return lnsaccountdyninfoList;
    }

    /**
     *
     * @param lnsaccountdyninfoList get
     */
    public void setLnsaccountdyninfoList(List<Map<String, Object>> lnsaccountdyninfoList) {
        this.lnsaccountdyninfoList = lnsaccountdyninfoList;
    }

}

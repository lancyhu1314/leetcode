package com.suning.fab.loan.workunit;

import java.util.HashMap;
import java.util.Map;

import org.apache.zookeeper.txn.Txn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.domain.TblLnsassistdyninfo;
import com.suning.fab.loan.domain.TblLnsinterface;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanAssistDynInfoUtil;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：赔付户扣减
 *
 * @Author 18049705 MYP
 * @Date Created in 9:02 2020/3/16
 * @see
 */
@Scope("prototype")
@Repository
public class Lns257 extends WorkUnit {

    private String depositType;
    private String  serialNo;
    private String	channelType;
    private String	fundChannel;
    private String  repayAcctNo;
    private FabAmount amt;
    private String  outSerialNo;

    @Autowired
    LoanEventOperateProvider eventProvider;
    @Override
    public void run() throws Exception {
        //写幂等登记薄
        TblLnsinterface lnsinterface = new TblLnsinterface();
        //账务日期
        lnsinterface.setTrandate(tranctx.getTermDate());
        //幂等流水号
        lnsinterface.setSerialno(tranctx.getSerialNo());
        //交易码
        lnsinterface.setTrancode(tranctx.getTranCode());
        //自然日期
        lnsinterface.setAccdate(tranctx.getTranDate());
        //系统流水号
        lnsinterface.setSerseqno(tranctx.getSerSeqNo());
        //网点
        lnsinterface.setBrc(tranctx.getBrc());
        //预收账号
        lnsinterface.setUserno(repayAcctNo);
        //开户金额
        lnsinterface.setTranamt(amt.getVal());
        //时间戳
        lnsinterface.setTimestamp(tranctx.getTranTime());
        //银行流水号/易付宝单号/POS单号
        lnsinterface.setBankno(outSerialNo);
        //借据号
        lnsinterface.setMagacct(repayAcctNo);

        lnsinterface.setAcctno(repayAcctNo);
        lnsinterface.setReserv2(depositType);
        lnsinterface.setReserv5(channelType);
        lnsinterface.setBillno(fundChannel);
        try {
            DbAccessUtil.execute("Lnsinterface.insert", lnsinterface);
        }
        catch (FabSqlException e)
        {
            if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
                throw new FabException(e, ConstantDeclare.RSPCODE.TRAN.IDEMPOTENCY);
            } else {
                throw new FabException(e, "SPS100", "lnsinterface");
            }
        }
        
        TblLnsassistdyninfo lnsassistdyninfo ;
        
        String briefcode = "";
        if("3".equals(depositType)) {//3-保费户
            lnsassistdyninfo = LoanAssistDynInfoUtil.saveLnsassistlist(tranctx, 1,repayAcctNo,repayAcctNo, ConstantDeclare.ASSISTACCOUNT.PREMIUMSACCT, amt, "sub");
            briefcode = ConstantDeclare.BRIEFCODE.RBBF;
        }
        else //2-赔付户（赔付时开户并充值）
        {
        	lnsassistdyninfo = LoanAssistDynInfoUtil.saveLnsassistlist(tranctx, 1,repayAcctNo,repayAcctNo, ConstantDeclare.ASSISTACCOUNT.PAYACCT, amt, "sub");
        	if( VarChecker.isEmpty(lnsassistdyninfo.getReserv2().trim()) )
        		briefcode = ConstantDeclare.BRIEFCODE.PFDD;
        	else if( "51340000".equals(lnsassistdyninfo.getReserv2().trim()) )
        		briefcode = ConstantDeclare.BRIEFCODE.RDPF;
        	else if( "51350000".equals(lnsassistdyninfo.getReserv2().trim()) )
        		briefcode = ConstantDeclare.BRIEFCODE.RBPF;
        }

        if(null == lnsassistdyninfo){
            throw new FabException("LNS080",repayAcctNo);
        }
       
        //更新后金额为负报错
        if(new FabAmount(lnsassistdyninfo.getCurrbal()).isNegative()){
            throw new FabException("LNS070","账户");
        }
        LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(repayAcctNo,tranctx);

        LnsAcctInfo lnsOpAcctInfo = new LnsAcctInfo(repayAcctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,
                ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());

        
		loanAgreement.getFundInvest().setChannelType(channelType);
        eventProvider.createEvent(ConstantDeclare.EVENT.REPAYSPLIT, amt, lnsOpAcctInfo
                , null, loanAgreement.getFundInvest(),
                //RBBF人保保费 （判断是3-保费户的预收充退） PFBX-赔付本息（判断是2-赔付户的预收充退）
                briefcode ,tranctx,VarChecker.isEmpty(lnsassistdyninfo.getReserv2())?tranctx.getBrc():lnsassistdyninfo.getReserv2());
        
        
        Map<String,Object> dyParam = new HashMap<String,Object>();
		dyParam.put("acctno", repayAcctNo);
		dyParam.put("brc", tranctx.getBrc());
		dyParam.put("trandate", tranctx.getTranDate());
		//更新动态表修改时间
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_dyninfo_603", dyParam);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsaccountdyninfo");
		}
    }

    /**
     * Gets the value of depositType.
     *
     * @return the value of depositType
     */
    public String getDepositType() {
        return depositType;
    }

    /**
     * Sets the depositType.
     *
     * @param depositType depositType
     */
    public void setDepositType(String depositType) {
        this.depositType = depositType;

    }

    /**
     * Gets the value of serialNo.
     *
     * @return the value of serialNo
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * Sets the serialNo.
     *
     * @param serialNo serialNo
     */
    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;

    }

    /**
     * Gets the value of channelType.
     *
     * @return the value of channelType
     */
    public String getChannelType() {
        return channelType;
    }

    /**
     * Sets the channelType.
     *
     * @param channelType channelType
     */
    public void setChannelType(String channelType) {
        this.channelType = channelType;

    }

    /**
     * Gets the value of fundChannel.
     *
     * @return the value of fundChannel
     */
    public String getFundChannel() {
        return fundChannel;
    }

    /**
     * Sets the fundChannel.
     *
     * @param fundChannel fundChannel
     */
    public void setFundChannel(String fundChannel) {
        this.fundChannel = fundChannel;

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
     * Gets the value of amt.
     *
     * @return the value of amt
     */
    public FabAmount getAmt() {
        return amt;
    }

    /**
     * Sets the amt.
     *
     * @param amt amt
     */
    public void setAmt(FabAmount amt) {
        this.amt = amt;

    }

    /**
     * Gets the value of outSerialNo.
     *
     * @return the value of outSerialNo
     */
    public String getOutSerialNo() {
        return outSerialNo;
    }

    /**
     * Sets the outSerialNo.
     *
     * @param outSerialNo outSerialNo
     */
    public void setOutSerialNo(String outSerialNo) {
        this.outSerialNo = outSerialNo;

    }

    /**
     * Gets the value of eventProvider.
     *
     * @return the value of eventProvider
     */
    public LoanEventOperateProvider getEventProvider() {
        return eventProvider;
    }

    /**
     * Sets the eventProvider.
     *
     * @param eventProvider eventProvider
     */
    public void setEventProvider(LoanEventOperateProvider eventProvider) {
        this.eventProvider = eventProvider;

    }
}

package com.suning.fab.loan.domain;

import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.utils.VarChecker;

import java.io.Serializable;
import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "LNSBILL")
public class TblLnsbill implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Date trandate = new Date(0);

    private Integer serseqno = 0;

    private Integer txseq = 0;

    private String acctno = "";

    private String brc = "";

    private String billtype = "";

    private Integer period = 0;

    private Double billamt = 0.00;

    private Double billbal = 0.00;

    private Double lastbal = 0.00;

    private String lastdate = "";

    private Double prinbal = 0.00;

    private Double billrate = 0.00;

    private Double accumulate = 0.00;

    private String begindate = "";

    private String enddate = "";

    private String curenddate = "";

    private String repayedate = "";

    private String settledate = "";

    private String intedate = "";

    private String fintedate = "";

    private String cintedate = "";

    private String repayway = "";

    private String ccy = "";

    private Date dertrandate = new Date(0);

    private Integer derserseqno = 0;

    private Integer dertxseq = 0;

    private String billstatus = "";

    private String statusbdate = "";

    private String billproperty = "";

    private String intrecordflag = "";

    private String cancelflag = "";

    private String settleflag = "";

    private String timestamp = "";

    public TblLnsbill() {
         
    }

    /** 账务日期 */
    @Column(name = "Trandate")
    public Date getTrandate() {
        return trandate;
    }

    /** 账务日期 */
    public void setTrandate(Date trandate) {
        if(trandate == null){
            this.trandate = new Date(0);
        }else{
            this.trandate = trandate;
        }
    }

    /** 流水号 */
    @Column(name = "Serseqno")
    public Integer getSerseqno() {
        return serseqno;
    }

    /** 流水号 */
    public void setSerseqno(Integer serseqno) {
        if(serseqno == null){
            this.serseqno = 0;
        }else{
            this.serseqno = serseqno;
        }
    }

    /** 子序号 */
    @Column(name = "Txseq")
    public Integer getTxseq() {
        return txseq;
    }

    /** 子序号 */
    public void setTxseq(Integer txseq) {
        if(txseq == null){
            this.txseq = 0;
        }else{
            this.txseq = txseq;
        }
    }

    /** 账号 */
    @Column(name = "Acctno")
    public String getAcctno() {
        return acctno;
    }

    /** 账号 */
    public void setAcctno(String acctno) {
        this.acctno = acctno == null ? "" : acctno.trim();
    }

    /** 公司代码 */
    @Column(name = "Brc")
    public String getBrc() {
        return brc;
    }

    /** 公司代码 */
    public void setBrc(String brc) {
        this.brc = brc == null ? "" : brc.trim();
    }

    /** 账单类型 PRIN本金NINT利息DINT罚息CINT复利 */
    @Column(name = "Billtype")
    public String getBilltype() {
        return billtype;
    }

    /** 账单类型 PRIN本金NINT利息DINT罚息CINT复利 */
    public void setBilltype(String billtype) {
        this.billtype = billtype == null ? "" : billtype.trim();
    }

    /** 期数 */
    @Column(name = "Period")
    public Integer getPeriod() {
        return period;
    }

    /** 期数 */
    public void setPeriod(Integer period) {
        if(period == null){
            this.period = 0;
        }else{
            this.period = period;
        }
    }

    /** 账单金额 */
    @Column(name = "Billamt")
    public Double getBillamt() {
        return billamt;
    }

    /** 账单金额 */
    public void setBillamt(Double billamt) {
        if(billamt == null){
            this.billamt = 0.00;
        }else{
            this.billamt = billamt;
        }
    }

    /** 账单余额 */
    @Column(name = "Billbal")
    public Double getBillbal() {
        return billbal;
    }

    /** 账单余额 */
    public void setBillbal(Double billbal) {
        if(billbal == null){
            this.billbal = 0.00;
        }else{
            this.billbal = billbal;
        }
    }

    /** 上日余额 */
    @Column(name = "Lastbal")
    public Double getLastbal() {
        return lastbal;
    }

    /** 上日余额 */
    public void setLastbal(Double lastbal) {
        if(lastbal == null){
            this.lastbal = 0.00;
        }else{
            this.lastbal = lastbal;
        }
    }

    /** 上笔日期 */
    @Column(name = "Lastdate")
    public String getLastdate() {
        return lastdate;
    }

    /** 上笔日期 */
    public void setLastdate(String lastdate) {
        this.lastdate = lastdate == null ? "" : lastdate.trim();
    }

    /** 账单对应本金余额 */
    @Column(name = "Prinbal")
    public Double getPrinbal() {
        return prinbal;
    }

    /** 账单对应本金余额 */
    public void setPrinbal(Double prinbal) {
        if(prinbal == null){
            this.prinbal = 0.00;
        }else{
            this.prinbal = prinbal;
        }
    }

    /** 账单执行利率 */
    @Column(name = "Billrate")
    public Double getBillrate() {
        return billrate;
    }

    /** 账单执行利率 */
    public void setBillrate(Double billrate) {
        if(billrate == null){
            this.billrate = 0.00;
        }else{
            this.billrate = billrate;
        }
    }

    /** 罚息/复利积数 */
    @Column(name = "Accumulate")
    public Double getAccumulate() {
        return accumulate;
    }

    /** 罚息/复利积数 */
    public void setAccumulate(Double accumulate) {
        if(accumulate == null){
            this.accumulate = 0.00;
        }else{
            this.accumulate = accumulate;
        }
    }

    /** 账单起始日期 */
    public void setBegindate(String begindate) {
        this.begindate = begindate == null ? "" : begindate.trim();
    }

    /** 账单起始日期 */
    @Column(name = "Begindate")
    public String getBegindate() {
        return begindate;
    }

    /** 账单结束日期 */
    @Column(name = "Enddate")
    public String getEnddate() {
        return enddate;
    }

    /** 账单结束日期 */
    public void setEnddate(String enddate) {
        this.enddate = enddate == null ? "" : enddate.trim();
    }

    /** 当期到期日 */
    @Column(name = "Curenddate")
    public String getCurenddate() {
        return curenddate;
    }

    /** 当期到期日 */
    public void setCurenddate(String curenddate) {
        this.curenddate = curenddate == null ? "" : curenddate.trim();
    }

    /** 账单应还款止日 */
    @Column(name = "Repayedate")
    public String getRepayedate() {
        return repayedate;
    }

    /** 账单应还款止日 */
    public void setRepayedate(String repayedate) {
        this.repayedate = repayedate == null ? "" : repayedate.trim();
    }

    /** 账单结清日期 */
    @Column(name = "Settledate")
    public String getSettledate() {
        return settledate;
    }

    /** 账单结清日期 */
    public void setSettledate(String settledate) {
        this.settledate = settledate == null ? "" : settledate.trim();
    }

    /** 利息计止日期 */
    @Column(name = "Intedate")
    public String getIntedate() {
        return intedate;
    }

    /** 利息计止日期 */
    public void setIntedate(String intedate) {
        this.intedate = intedate == null ? "" : intedate.trim();
    }

    /** 罚息计止日期 */
    @Column(name = "Fintedate")
    public String getFintedate() {
        return fintedate;
    }

    /** 罚息计止日期 */
    public void setFintedate(String fintedate) {
        this.fintedate = fintedate == null ? "" : fintedate.trim();
    }

    /** 复利计至日期 */
    @Column(name = "Cintedate")
    public String getCintedate() {
        return cintedate;
    }

    /** 复利计至日期 */
    public void setCintedate(String cintedate) {
        this.cintedate = cintedate == null ? "" : cintedate.trim();
    }

    /** 计息方式0等本等息1等额本息2等额本金 */
    @Column(name = "Repayway")
    public String getRepayway() {
        return repayway;
    }

    /** 计息方式0等本等息1等额本息2等额本金 */
    public void setRepayway(String repayway) {
        this.repayway = repayway == null ? "" : repayway.trim();
    }

    /** 币种 */
    @Column(name = "Ccy")
    public String getCcy() {
        return ccy;
    }

    /** 币种 */
    public void setCcy(String ccy) {
        this.ccy = ccy == null ? "" : ccy.trim();
    }

    /** 账务日期 */
    @Column(name = "Dertrandate")
    public Date getDertrandate() {
        return dertrandate;
    }

    /** 账务日期 */
    public void setDertrandate(Date dertrandate) {
        if(dertrandate == null){
            this.dertrandate = new Date(0);
        }else{
            this.dertrandate = dertrandate;
        }
    }

    /** 流水号 */
    @Column(name = "Derserseqno")
    public Integer getDerserseqno() {
        return derserseqno;
    }

    /** 流水号 */
    public void setDerserseqno(Integer derserseqno) {
        if(derserseqno == null){
            this.derserseqno = 0;
        }else{
            this.derserseqno = derserseqno;
        }
    }

    /** 子序号 */
    @Column(name = "Dertxseq")
    public Integer getDertxseq() {
        return dertxseq;
    }

    /** 子序号 */
    public void setDertxseq(Integer dertxseq) {
        if(dertxseq == null){
            this.dertxseq = 0;
        }else{
            this.dertxseq = dertxseq;
        }
    }

    /** 账单状态 N正常G宽限期O逾期L呆滞B呆账 */
    @Column(name = "Billstatus")
    public String getBillstatus() {
        return billstatus;
    }

    /** 账单状态 N正常G宽限期O逾期L呆滞B呆账 */
    public void setBillstatus(String billstatus) {
        this.billstatus = billstatus == null ? "" : billstatus.trim();
    }

    /** 账单状态开始日期 */
    @Column(name = "Statusbdate")
    public String getStatusbdate() {
        return statusbdate;
    }

    /** 账单状态开始日期 */
    public void setStatusbdate(String statusbdate) {
        this.statusbdate = statusbdate == null ? "" : statusbdate.trim();
    }

    /** 账单属性 INTSET正常结息 REPAY还款 */
    @Column(name = "Billproperty")
    public String getBillproperty() {
        return billproperty;
    }

    /** 账单属性 INTSET正常结息 REPAY还款 */
    public void setBillproperty(String billproperty) {
        this.billproperty = billproperty == null ? "" : billproperty.trim();
    }

    /** 利息入账标志 NO未入 YES已入 */
    @Column(name = "Intrecordflag")
    public String getIntrecordflag() {
        return intrecordflag;
    }

    /** 利息入账标志 NO未入 YES已入 */
    public void setIntrecordflag(String intrecordflag) {
        this.intrecordflag = intrecordflag == null ? "" : intrecordflag.trim();
    }

    /** 账单作废标志 NORMAL正常 CANCEL作废 */
    @Column(name = "Cancelflag")
    public String getCancelflag() {
        return cancelflag;
    }

    /** 账单作废标志 NORMAL正常 CANCEL作废 */
    public void setCancelflag(String cancelflag) {
        this.cancelflag = cancelflag == null ? "" : cancelflag.trim();
    }

    /** 结清标志 RUNNING未结 CLOSE已结 */
    @Column(name = "Settleflag")
    public String getSettleflag() {
        return settleflag;
    }

    /** 结清标志 RUNNING未结 CLOSE已结 */
    public void setSettleflag(String settleflag) {
        this.settleflag = settleflag == null ? "" : settleflag.trim();
    }

    /** 时间戳 */
    @Column(name = "Timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    /** 时间戳 */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp == null ? "" : timestamp.trim();
    }

    public boolean isOverdue(){
        if((VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(billstatus)
                ||VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT,ConstantDeclare.BILLTYPE.BILLTYPE_DINT).contains(billtype))){
            return true;
        }
        return false;
    }

}
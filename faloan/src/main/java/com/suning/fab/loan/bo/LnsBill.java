package com.suning.fab.loan.bo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Date;
import java.util.Arrays;

import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;

public class LnsBill implements Serializable {

	/**
	 */
	private static final long serialVersionUID = 8882789707385528697L;
	private Date tranDate;
	private Integer serSeqno;
	private Integer txSeq ;
	private String billType		; //账单类型 PRIN本金NINT利息DINT罚息CINT复利
	private Integer period		; //期数
	private FabAmount billAmt	; //账单金额
	private FabAmount billBal	; //账单余额
	private FabAmount prinBal	; //账单对应本金余额
	private FabRate billRate	; //账单执行利率
	private FabAmount accumulate;//罚息/复利积数
	private FabAmount balance;//剩余本金
	private String startDate	;//账单起始日期
	private String endDate		;//账单结束日期
	private String curendDate	;//当期到期日
	private String repayendDate	;//账单应还款止日
	private String settleDate	;//账单结清日期
	private String lastDate;    //最后交易日期
	private FabAmount repayDateInt; //截止还款日当期计提利息
	private String intendDate	;	 //	利息计止日期
	private String fintendDate	;//	罚息计止日期
	private String cintendDate	;//	复利计至日期
	private String statusbDate ; //账单状态开始日期
	private Date  derTranDate	;//账务日期
	private Integer    derSerseqno	;//流水号
	private Integer derTxSeq	;//子序号
	private String  billStatus	;//	账单状态 N正常G宽限期O逾期L呆滞B呆账
	private String  billProperty	;//	账单属性 INTSET正常结息 REPAY还款
	private String  intrecordFlag;//利息入账标志 NO未入 YES已入
	private String  cancelFlag	;//	账单作废标志 NORMAL正常 CANCEL作废
	private String  settleFlag	;//	结清标志 RUNNING未结 CLOSE已结
	private String  ccy;
	private String repayWay;
	private FabAmount termFreeInterest;//现金贷本期免息金额
	
	private LnsBill hisBill;
	private TblLnsfeeinfo lnsfeeinfo;

	public LnsBill() {
		super();
		this.tranDate = new Date(0);
		this.serSeqno = 0;
		this.txSeq = 0;
		this.derTranDate = new Date(0);
		this.derSerseqno = 0;
		this.derTxSeq = 0;
		
	}
	
	public LnsBill deepClone() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        oo.writeObject(this);
        ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(bi);
        return (LnsBill)oi.readObject();
    }
	@Override
	public String toString() {
		return "LnsBill [tranDate=" + tranDate + ", serSeqno=" + serSeqno
				+ ", txSeq=" + txSeq + ", billType=" + billType + ", period="
				+ period + ", billAmt=" + billAmt + ", billBal=" + billBal
				+ ", prinBal=" + prinBal + ", billRate=" + billRate
				+ ", accumulate=" + accumulate + ", balance=" + balance
				+ ", startDate=" + startDate + ", endDate=" + endDate
				+ ", curendDate=" + curendDate + ", repayendDate="
				+ repayendDate + ", settleDate=" + settleDate + ", lastDate="
				+ lastDate + ", repayDateInt=" + repayDateInt + ", intendDate="
				+ intendDate + ", fintendDate=" + fintendDate
				+ ", cintendDate=" + cintendDate + ", statusbDate="
				+ statusbDate + ", derTranDate=" + derTranDate
				+ ", derSerseqno=" + derSerseqno + ", derTxSeq=" + derTxSeq
				+ ", billStatus=" + billStatus + ", billProperty="
				+ billProperty + ", intrecordFlag=" + intrecordFlag
				+ ", cancelFlag=" + cancelFlag + ", settleFlag=" + settleFlag
				+ ", ccy=" + ccy + ", repayWay=" + repayWay + ", hisBill="
				+ hisBill + ", termFreeInterest=" + termFreeInterest + "]";
	}

	/**
	 * @return the tranDate
	 */
	public Date getTranDate() {
		return tranDate;
	}

	/**
	 * @param tranDate the tranDate to set
	 */
	public void setTranDate(Date tranDate) {
		this.tranDate = tranDate;
	}

	/**
	 * @return the serSeqno
	 */
	public Integer getSerSeqno() {
		return serSeqno;
	}

	/**
	 * @param serSeqno the serSeqno to set
	 */
	public void setSerSeqno(Integer serSeqno) {
		this.serSeqno = serSeqno;
	}

	/**
	 * @return the txSeq
	 */
	public Integer getTxSeq() {
		return txSeq;
	}

	/**
	 * @param txSeq the txSeq to set
	 */
	public void setTxSeq(Integer txSeq) {
		this.txSeq = txSeq;
	}

	/**
	 * @return the billType
	 */
	public String getBillType() {
		return billType;
	}

	/**
	 * @param billType the billType to set
	 */
	public void setBillType(String billType) {
		this.billType = billType;
	}

	/**
	 * @return the period
	 */
	public Integer getPeriod() {
		return period;
	}

	/**
	 * @param period the period to set
	 */
	public void setPeriod(Integer period) {
		this.period = period;
	}

	/**
	 * @return the billAmt
	 */
	public FabAmount getBillAmt() {
		return billAmt;
	}

	/**
	 * @param billAmt the billAmt to set
	 */
	public void setBillAmt(FabAmount billAmt) {
		this.billAmt = billAmt;
	}

	/**
	 * @return the billBal
	 */
	public FabAmount getBillBal() {
		return billBal;
	}

	/**
	 * @param billBal the billBal to set
	 */
	public void setBillBal(FabAmount billBal) {
		this.billBal = billBal;
	}

	/**
	 * @return the prinBal
	 */
	public FabAmount getPrinBal() {
		return prinBal;
	}

	/**
	 * @param prinBal the prinBal to set
	 */
	public void setPrinBal(FabAmount prinBal) {
		this.prinBal = prinBal;
	}

	/**
	 * @return the billRate
	 */
	public FabRate getBillRate() {
		return billRate;
	}

	/**
	 * @param billRate the billRate to set
	 */
	public void setBillRate(FabRate billRate) {
		this.billRate = billRate;
	}

	/**
	 * @return the accumulate
	 */
	public FabAmount getAccumulate() {
		return accumulate;
	}

	/**
	 * @param accumulate the accumulate to set
	 */
	public void setAccumulate(FabAmount accumulate) {
		this.accumulate = accumulate;
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
	 * @return the curendDate
	 */
	public String getCurendDate() {
		return curendDate;
	}

	/**
	 * @param curendDate the curendDate to set
	 */
	public void setCurendDate(String curendDate) {
		this.curendDate = curendDate;
	}

	/**
	 * @return the repayendDate
	 */
	public String getRepayendDate() {
		return repayendDate;
	}

	/**
	 * @param repayendDate the repayendDate to set
	 */
	public void setRepayendDate(String repayendDate) {
		this.repayendDate = repayendDate;
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
	 * @return the lastDate
	 */
	public String getLastDate() {
		return lastDate;
	}

	/**
	 * @param lastDate the lastDate to set
	 */
	public void setLastDate(String lastDate) {
		this.lastDate = lastDate;
	}

	/**
	 * @return the repayDateInt
	 */
	public FabAmount getRepayDateInt() {
		return repayDateInt;
	}

	/**
	 * @param repayDateInt the repayDateInt to set
	 */
	public void setRepayDateInt(FabAmount repayDateInt) {
		this.repayDateInt = repayDateInt;
	}

	/**
	 * @return the intendDate
	 */
	public String getIntendDate() {
		return intendDate;
	}

	/**
	 * @param intendDate the intendDate to set
	 */
	public void setIntendDate(String intendDate) {
		this.intendDate = intendDate;
	}

	/**
	 * @return the fintendDate
	 */
	public String getFintendDate() {
		return fintendDate;
	}

	/**
	 * @param fintendDate the fintendDate to set
	 */
	public void setFintendDate(String fintendDate) {
		this.fintendDate = fintendDate;
	}

	/**
	 * @return the cintendDate
	 */
	public String getCintendDate() {
		return cintendDate;
	}

	/**
	 * @param cintendDate the cintendDate to set
	 */
	public void setCintendDate(String cintendDate) {
		this.cintendDate = cintendDate;
	}

	/**
	 * @return the statusbDate
	 */
	public String getStatusbDate() {
		return statusbDate;
	}

	/**
	 * @param statusbDate the statusbDate to set
	 */
	public void setStatusbDate(String statusbDate) {
		this.statusbDate = statusbDate;
	}

	/**
	 * @return the derTranDate
	 */
	public Date getDerTranDate() {
		return derTranDate;
	}

	/**
	 * @param derTranDate the derTranDate to set
	 */
	public void setDerTranDate(Date derTranDate) {
		this.derTranDate = derTranDate;
	}

	/**
	 * @return the derSerseqno
	 */
	public Integer getDerSerseqno() {
		return derSerseqno;
	}

	/**
	 * @param derSerseqno the derSerseqno to set
	 */
	public void setDerSerseqno(Integer derSerseqno) {
		this.derSerseqno = derSerseqno;
	}

	/**
	 * @return the derTxSeq
	 */
	public Integer getDerTxSeq() {
		return derTxSeq;
	}

	/**
	 * @param derTxSeq the derTxSeq to set
	 */
	public void setDerTxSeq(Integer derTxSeq) {
		this.derTxSeq = derTxSeq;
	}

	/**
	 * @return the billStatus
	 */
	public String getBillStatus() {
		return billStatus;
	}

	/**
	 * @param billStatus the billStatus to set
	 */
	public void setBillStatus(String billStatus) {
		this.billStatus = billStatus;
	}

	/**
	 * @return the billProperty
	 */
	public String getBillProperty() {
		return billProperty;
	}

	/**
	 * @param billProperty the billProperty to set
	 */
	public void setBillProperty(String billProperty) {
		this.billProperty = billProperty;
	}

	/**
	 * @return the intrecordFlag
	 */
	public String getIntrecordFlag() {
		return intrecordFlag;
	}

	/**
	 * @param intrecordFlag the intrecordFlag to set
	 */
	public void setIntrecordFlag(String intrecordFlag) {
		this.intrecordFlag = intrecordFlag;
	}

	/**
	 * @return the cancelFlag
	 */
	public String getCancelFlag() {
		return cancelFlag;
	}

	/**
	 * @param cancelFlag the cancelFlag to set
	 */
	public void setCancelFlag(String cancelFlag) {
		this.cancelFlag = cancelFlag;
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
	 * @return the ccy
	 */
	public String getCcy() {
		return ccy;
	}

	/**
	 * @param ccy the ccy to set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
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
	 * @return the hisBill
	 */
	public LnsBill getHisBill() {
		return hisBill;
	}

	/**
	 * @param hisBill the hisBill to set
	 */
	public void setHisBill(LnsBill hisBill) {
		this.hisBill = hisBill;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public FabAmount getTermFreeInterest() {
		return termFreeInterest;
	}

	public void setTermFreeInterest(FabAmount termFreeInterest) {
		this.termFreeInterest = termFreeInterest;
	}

	/**
	 * Gets the value of lnsfeeinfo.
	 *
	 * @return the value of lnsfeeinfo
	 */
	public TblLnsfeeinfo getLnsfeeinfo() {
		return lnsfeeinfo;
	}

	/**
	 * Sets the lnsfeeinfo.
	 *
	 * @param lnsfeeinfo lnsfeeinfo
	 */
	public void setLnsfeeinfo(TblLnsfeeinfo lnsfeeinfo) {
		this.lnsfeeinfo = lnsfeeinfo;

	}

	public boolean  isPenalty(){
		return LoanFeeUtils.isPenalty(billType);
	}


	public  Integer onetimeFee(){
		if(this.getLnsfeeinfo()!=null && this.getLnsfeeinfo().getRepayway().equals(ConstantDeclare.FEEREPAYWAY.ONETIME))
			return 0;
		return 1;
	}
}

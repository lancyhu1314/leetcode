package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.BillTransformHelper;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanPlanCintDint;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 
 *
 * @version V1.0.0
 *
 * @see 罚息结息插账单
 *
 * @param repayDate 结息日期 
 * @param acctNo 贷款账号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns519 extends WorkUnit { 

	String	acctNo; //贷款账号
	String	repayDate; //结息日期
	LnsBillStatistics lnsBillStatistics;
	
	//房抵贷特殊处理20190127
	//此次intenddate修改账本主键
	Date thisTranDate;
	Integer thisSerseqno;
	Integer thisTxSeq;
	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		

		
		//获取贷款协议信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		
		//如果试算信息为空，重新获取
        if (null == lnsBillStatistics){
        	lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, CalendarUtil.nDaysAfter(repayDate, 1).toString("yyyy-MM-dd"), ctx);
        }
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());
		
		//获取呆滞呆账期间新罚息复利账单list
		//核销的不获取呆滞呆账期间的新罚息复利
		billList.addAll(lnsBillStatistics.getCdbillList());


		/**
		 * 房抵贷罚息都挂在所有未结清账单中的最早的那一期
		 * 通过LoanTrailCalculationProvider.genLoanBillDetail试算到repayDate之后一天可以算出罚息账单的归属账单,然后去更新这个归属账单的intedate
		 * 20190527|14050183
		 */
		int num=100;
		for (LnsBill bill : billList){
	        if (bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)){
				TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(la,bill,ctx);
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				tblLnsbill.setTrandate(new Date(format.parse(ctx.getTranDate()).getTime()));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				bill.setTranDate(tblLnsbill.getTrandate());
				bill.setSerSeqno(ctx.getSerSeqNo());
				if(tblLnsbill.getTxseq() == 0)
				{
					tblLnsbill.setTxseq(--num);
				}
				
				if(bill.getBillAmt().isPositive())
				{	
					Map<String, Object> param = new HashMap<String, Object>();
					param.put("trandate", bill.getDerTranDate());
					param.put("serseqno", bill.getDerSerseqno());
					param.put("txseq", bill.getDerTxSeq());
					param.put("intedate1", ctx.getTranDate());
					
					try {
						DbAccessUtil.execute("CUSTOMIZE.update_hisbill_513", param);
					} catch (FabException e) {
						throw new FabException(e, "SPS102", "lnsbills");
					}
					
				}
				
			
	        }
	    }
		
	}

	/**
	 * @return the acctNo
	 */
	public String getAcctNo() {
		return acctNo;
	}

	/**
	 * @param acctNo the acctNo to set
	 */
	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
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
	 * @return the lnsBillStatistics
	 */
	public LnsBillStatistics getLnsBillStatistics() {
		return lnsBillStatistics;
	}

	/**
	 * @param lnsBillStatistics the lnsBillStatistics to set
	 */
	public void setLnsBillStatistics(LnsBillStatistics lnsBillStatistics) {
		this.lnsBillStatistics = lnsBillStatistics;
	}

	/**
	 *
	 * @return thisTranDate get
	 */
	public Date getThisTranDate() {
		return thisTranDate;
	}

	/**
	 *
	 * @param thisTranDate set
	 */
	public void setThisTranDate(Date thisTranDate) {
		this.thisTranDate = thisTranDate;
	}

	/**
	 *
	 * @return thisSerseqno get
	 */
	public Integer getThisSerseqno() {
		return thisSerseqno;
	}

	/**
	 *
	 * @param thisSerseqno set
	 */
	public void setThisSerseqno(Integer thisSerseqno) {
		this.thisSerseqno = thisSerseqno;
	}

	/**
	 *
	 * @return thisTxSeq get
	 */
	public Integer getThisTxSeq() {
		return thisTxSeq;
	}

	/**
	 *
	 * @param thisTxSeq set
	 */
	public void setThisTxSeq(Integer thisTxSeq) {
		this.thisTxSeq = thisTxSeq;
	}




}


package com.suning.fab.loan.workunit;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;

import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;

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
public class Lns513 extends WorkUnit { 

	String	acctNo;
	String	repayDate;
	LnsBillStatistics lnsBillStatistics;
	TblLnsbasicinfo lnsbasicinfo;
	LoanAgreement loanAgreement;
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		//获取贷款协议信息
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,loanAgreement);

		//根据预还款日期和预还款金额试算贷款各期账单，包括已结清、未结清账单  chenchao:罚息计提已登记罚息，需重新试算
		//房抵贷罚息信息落表  需要重新试算
		if(VarChecker.asList("2412615").contains(lnsbasicinfo.getPrdcode())&&(!Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(la.getContract().getLoanStat()) || VarChecker.asList( "471007").contains(ctx.getTranCode()))){
			repayDate=ctx.getTranDate();
			lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate, ctx,null);
		}
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());
		
		//获取呆滞呆账期间新罚息复利账单list
		//宽限期罚息落表的不获取呆滞呆账期间的新罚息复利
		if(!"479002".equals(ctx.getTranCode())) {
			billList.addAll(lnsBillStatistics.getCdbillList());
		}

		for (LnsBill bill : billList){
			//宽限期落表只落宽限期的
            if("479002".equals(ctx.getTranCode())
                     &&(bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)||
                    bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT))) {
                continue;
            }

	        if (bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)||
	            bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)||
	            bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)){
				TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(la,bill,ctx);
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				tblLnsbill.setTrandate(new Date(format.parse(ctx.getTranDate()).getTime()));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				bill.setTranDate(tblLnsbill.getTrandate());
				bill.setSerSeqno(ctx.getSerSeqNo());
				if(tblLnsbill.getTxseq() == 0)
				{
					//优化插入账本插入数据的问题 账本数量太多
					lnsBillStatistics.setBillNo(lnsBillStatistics.getBillNo()+1);
					tblLnsbill.setTxseq(lnsBillStatistics.getBillNo());
				}

				if(bill.getBillAmt().isPositive())
				{
                    pepareDerCoding(bill,tblLnsbill);
					try{
						DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);		
					}catch (FabException e){
						throw new FabException(e, "SPS100", "lnsbill");
					}
					
					Map<String, Object> param = new HashMap<String, Object>();

					pepareDerCoding(ctx, bill, param);
					
					try {
						DbAccessUtil.execute("CUSTOMIZE.update_hisbill_513", param);
					} catch (FabException e) {
						throw new FabException(e, "SPS102", "lnsbills");
					}
					
				}
				
			
	        }
	    }
	}

	private void pepareDerCoding(TranCtx ctx, LnsBill bill, Map<String, Object> param) {
		if(!VarChecker.isEmpty(bill.getHisBill())){
			param.put("trandate", bill.getHisBill().getTranDate());
			param.put("serseqno", bill.getHisBill().getSerSeqno());
			param.put("txseq", bill.getHisBill().getTxSeq());
			param.put("intedate1",ctx.getTranDate());
		}else{
			param.put("trandate", bill.getDerTranDate());
			param.put("serseqno", bill.getDerSerseqno());
			param.put("txseq", bill.getDerTxSeq());
			param.put("intedate1",ctx.getTranDate());
		}

	}
    private void pepareDerCoding(LnsBill bill, TblLnsbill tblLnsbill) {
        if(!VarChecker.isEmpty(bill.getHisBill())){
            tblLnsbill.setDerserseqno(bill.getHisBill().getSerSeqno());
            tblLnsbill.setDertrandate( bill.getHisBill().getTranDate());
            tblLnsbill.setDertxseq( bill.getHisBill().getTxSeq());

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
	 * @return the lnsbasicinfo
	 */
	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	/**
	 * @param lnsbasicinfo the lnsbasicinfo to set
	 */
	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}


	/**
	 * Gets the value of loanAgreement.
	 *
	 * @return the value of loanAgreement
	 */
	public LoanAgreement getLoanAgreement() {
		return loanAgreement;
	}

	/**
	 * Sets the loanAgreement.
	 *
	 * @param loanAgreement loanAgreement
	 */
	public void setLoanAgreement(LoanAgreement loanAgreement) {
		this.loanAgreement = loanAgreement;

	}
}


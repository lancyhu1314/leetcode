package com.suning.fab.loan.workunit;

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
import com.suning.fab.loan.utils.LoanBillSettleInterestSupporter;
import com.suning.fab.loan.utils.LoanPlanCintDint;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.PropertyUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 
 *
 * @version V1.0.0
 *
 * @see -罚息结息插账单
 *
 * @param -repayDate 结息日期
 * @param -acctNo 贷款账号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns520 extends WorkUnit { 

	String	acctNo; //贷款账号
	String	repayDate;//结息日期
	LnsBillStatistics lnsBillStatistics;
	TblLnsbasicinfo lnsbasicinfo; //贷款主文件
	LoanAgreement la;
	
	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		

		
		
		//房抵贷特殊处理
        if( "473007".equals(ctx.getTranCode()) )
        {
        	repayDate = ctx.getTranDate();
        }
		
		//获取贷款协议信息
		la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,la);

		
		
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());
		
		//获取呆滞呆账期间新罚息复利账单list
		//核销的不获取呆滞呆账期间的新罚息复利
		if( !(VarChecker.asList( "479001").contains(ctx.getTranCode()) 
				&& !VarChecker.isEmpty(la.getInterestAgreement().getCapRate()) 
				&& !la.getContract().getFlag1().contains("C"))){
			List<TblLnsbill> cdbillList = genCintDintList(getAcctNo(), ctx);
			
			for( TblLnsbill tblLnsbill:cdbillList)
			{
				billList.add(BillTransformHelper.convertToLnsBill(tblLnsbill));
			}
		}
		

		for (LnsBill bill : billList){
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
					try{
						DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);		
					}catch (FabException e){
						throw new FabException(e, "SPS100", "lnsbill");
					}
					
					Map<String, Object> param = new HashMap<String, Object>();
					param.put("trandate", bill.getDerTranDate());
					param.put("serseqno", bill.getDerSerseqno());
					param.put("txseq", bill.getDerTxSeq());
					param.put("intedate1", ctx.getTranDate());
                    if(VarChecker.asList( "479001").contains(ctx.getTranCode())){
                        param.put("intedate1", bill.getEndDate());
                    }
					try {
						DbAccessUtil.execute("CUSTOMIZE.update_hisbill_513", param);
					} catch (FabException e) {
						throw new FabException(e, "SPS102", "lnsbills");
					}
					
				}
				
			
	        }
	    }
		
	}

	public List<TblLnsbill> genCintDintList(String acctNo, TranCtx ctx) throws FabException{
		String repayDate = null;
		//定义返回呆滞呆账罚息复利账单list
		List<TblLnsbill> cdBillList = new ArrayList<TblLnsbill>();	
		//获取贷款协议信息
		if(VarChecker.isEmpty(la)){
			la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		}
		
		//生成还款计划
		if(VarChecker.isEmpty(lnsBillStatistics)){
			lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
		}
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		//防止跑批漏跑
		hisLnsbill.addAll(lnsBillStatistics.getBillInfoList());

		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		hisLnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		FabAmount sumlbcdint = new FabAmount(0.0);
		FabAmount sumPrin = new FabAmount(0.0);
		
		//封顶计息判断----20190124
		if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())){
			for(LnsBill lnsHisbill : hisLnsbill){
				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.BILLTYPE.BILLTYPE_DINT).contains(lnsHisbill.getBillType()) ) {
					sumlbcdint.selfAdd(lnsHisbill.getBillAmt());
				}
				if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
					sumPrin.selfAdd(lnsHisbill.getBillBal());
				}
			}
			//计算总的剩余本金
			for(LnsBill lnsbill : lnsBillStatistics.getFutureBillInfoList()){
				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsbill.getBillType()) && !VarChecker.isEmpty(lnsbill.getRepayDateInt())) {
					sumlbcdint.selfAdd(lnsbill.getRepayDateInt());
				}
				if( lnsbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) ) {
					sumPrin.selfAdd(lnsbill.getBillBal());
				}
			}


//			sumlbcdint.selfAdd(la.getBasicExtension().getHisDintComein());
			la.getBasicExtension().setCalDintComein(sumlbcdint);
		}
		//设置所有的未还本金
		la.getBasicExtension().setSumPrin(sumPrin);
		boolean breakCap = false;
		//循环处理呆滞呆账状态的本金和利息账单
		for (LnsBill lnsHisbill : hisLnsbill) {
			//首先处理呆滞呆账本金产生 罚息账单
				
			if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN) 
					|| (lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) 
							&& !VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
							&& la.getContract().getFlag1().contains("C"))) cap:{
				if((breakCap && !VarChecker.isEmpty(la.getInterestAgreement().getCapRate())) || (sumPrin.isZero() && !VarChecker.isEmpty(la.getInterestAgreement().getCapRate()))){
					if(la.getContract().getFlag1().contains("C")){
						break cap;
					}
					
				}
				if( VarChecker.asList("3","4").contains(la.getInterestAgreement().getDintSource()) &&
				lnsHisbill.getSettleFlag().equals("CLOSE")	)
				{
					continue;
				}
				
				if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
						&& la.getContract().getFlag1().contains("C")
						&& lnsHisbill.getSettleFlag().equals("CLOSE")){
					continue;
				}
				
				//房抵贷跑批计提特殊处理
				if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate()) && la.getContract().getFlag1().contains("C")){
					
				
					//封顶计息特殊处理20190127
					//计算整笔呆滞呆账逾期开始日期
					String loanform = PropertyUtil.getPropertyOrDefault("transfer."+String.valueOf(3),null);
					
					if (loanform == null)
						return null;
					String tmp[] = loanform.split(":");
					//逾期天数
					Integer days = Integer.parseInt(tmp[1]);
					String baddebtsDate = CalendarUtil.nDaysAfter(la.getContract().getContractEndDate(), days).toString("yyyy-MM-dd");
					if(VarChecker.asList( "479001").contains(ctx.getTranCode())){
						repayDate = CalendarUtil.after(ctx.getTranDate(),baddebtsDate)?baddebtsDate:ctx.getTranDate(); 
						if(CalendarUtil.after(lnsHisbill.getIntendDate(), repayDate)){
							repayDate = lnsHisbill.getIntendDate();
						}
					}else{
						repayDate = ctx.getTranDate();
					}
				}
				FabAmount famt;
				if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) 
						&& !VarChecker.isEmpty(la.getInterestAgreement().getCapRate())) {
					famt = LoanBillSettleInterestSupporter.calculateCapBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), repayDate);
				}else{
					if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate()) && la.getContract().getFlag1().contains("C")) {
						famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), repayDate);
					}else{
						famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getOverdueRate(), ctx.getTranDate());
					}
					
				}
				breakCap = true;
				if(famt == null || famt.isZero())
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
				tblLnsbill.setPeriod(lnsHisbill.getPeriod());
				tblLnsbill.setBillamt(famt.getVal());
				tblLnsbill.setBillbal(famt.getVal());
				tblLnsbill.setPrinbal(lnsHisbill.getBillBal().getVal());
				tblLnsbill.setBillrate(la.getRateAgreement().getOverdueRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsHisbill.getIntendDate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsHisbill.getRepayWay());
				tblLnsbill.setCcy(lnsHisbill.getCcy());
				tblLnsbill.setDertrandate(lnsHisbill.getTranDate());
				tblLnsbill.setDerserseqno(lnsHisbill.getSerSeqno());
				tblLnsbill.setDertxseq(lnsHisbill.getTxSeq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsHisbill.getIntendDate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}

				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				cdBillList.add(tblLnsbill);
			}
			
			//处理呆滞利息产生呆滞复利账单
			if( lnsHisbill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT) ) {
				
				if( VarChecker.asList("3","4").contains(la.getInterestAgreement().getDintSource()) &&
				lnsHisbill.getSettleFlag().equals("CLOSE")	)
				{
					continue;
				}
				
				//当前工作日期减复利记至日期得到计复利天数
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,lnsHisbill, la.getRateAgreement().getCompoundRate(), ctx.getTranDate());
				if(famt == null || famt.isZero())
					continue;
				
				TblLnsbill tblLnsbill = new TblLnsbill();
				tblLnsbill.setTrandate(new Date(0));
				tblLnsbill.setSerseqno(ctx.getSerSeqNo());
				tblLnsbill.setTxseq(0);
				tblLnsbill.setAcctno(acctNo);
				tblLnsbill.setBrc(ctx.getBrc());
				tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_CINT);
				tblLnsbill.setPeriod(lnsHisbill.getPeriod());
				tblLnsbill.setBillamt(famt.getVal());
				tblLnsbill.setBillbal(famt.getVal());
				tblLnsbill.setPrinbal(lnsHisbill.getBillBal().getVal());
				tblLnsbill.setBillrate(la.getRateAgreement().getCompoundRate().getVal().doubleValue());
				tblLnsbill.setBegindate(lnsHisbill.getIntendDate());
				tblLnsbill.setEnddate(ctx.getTranDate());
				tblLnsbill.setCurenddate(ctx.getTranDate());
				tblLnsbill.setRepayedate(ctx.getTranDate());
				tblLnsbill.setIntedate(ctx.getTranDate());
				tblLnsbill.setRepayway(lnsHisbill.getRepayWay());
				tblLnsbill.setCcy(lnsHisbill.getCcy());
				tblLnsbill.setDertrandate(lnsHisbill.getTranDate());
				tblLnsbill.setDerserseqno(lnsHisbill.getSerSeqno());
				tblLnsbill.setDertxseq(lnsHisbill.getTxSeq());
				tblLnsbill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				tblLnsbill.setStatusbdate(lnsHisbill.getIntendDate());
				tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				tblLnsbill.setIntrecordflag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);
				tblLnsbill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					tblLnsbill.setCancelflag("3");
				}
				tblLnsbill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				
				cdBillList.add(tblLnsbill);
			}
	
		}	
		
		return cdBillList;
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


	public LoanAgreement getLa() {
		return la;
	}

	public void setLa(LoanAgreement la) {
		this.la = la;
	}




}


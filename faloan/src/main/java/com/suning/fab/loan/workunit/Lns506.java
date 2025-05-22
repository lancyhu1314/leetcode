package com.suning.fab.loan.workunit;

import java.util.*;

import com.suning.fab.loan.utils.LoanFeeUtils;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsbill;
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

/**
 * @author TT.Y
 *
 * @version V1.0.0
 *
 * @see -贷款结清状态更新
 *
 * @param -acctNo 贷款账号
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns506 extends WorkUnit {

	String acctNo;
	String loanStat;
	String endFlag;

	@Override
	public void run() throws Exception {

		TranCtx ctx = getTranctx();
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		TblLnsbasicinfo lnsbasicinfo;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}
		if (null == lnsbasicinfo) {
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		LoggerUtil.info("更新前贷款状态:" + lnsbasicinfo.getLoanstat());
		
		loanStat = lnsbasicinfo.getLoanstat();
		List<TblLnsbill> tblLnsBills = null;

		if(new FabAmount(lnsbasicinfo.getContractbal()).isZero())
		{
			Map<String, Object> billMap = new HashMap<String, Object>();
			billMap.put("acctno", acctNo);
			billMap.put("brc", ctx.getBrc());
			try {
				tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", billMap, TblLnsbill.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "query_lnsbill_repay");
			}
			//费用账本参与结清标识判断，在还费的时候 更新
			if(!Arrays.asList(GlobalScmConfUtil.getProperty("priorityFee","DEFAULT").split(",")).contains(lnsbasicinfo.getPrdcode())){
				//不是商票的需过滤掉费用
				tblLnsBills = LoanFeeUtils.filtFee(tblLnsBills);
			}
			if (tblLnsBills.isEmpty()){
				loanStat = ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE;
			
				Map<String, Object> basicMap = new HashMap<>();
				basicMap.put("loanstat", ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
				basicMap.put("acctno", acctNo);
				basicMap.put("openbrc", ctx.getBrc());
				basicMap.put("oldloanstat", lnsbasicinfo.getLoanstat());
				basicMap.put("modifyDate", ctx.getTranDate());	

				int count;
				try{
					count = DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstat", basicMap);
				}catch (FabSqlException e){
					throw new FabException(e, "SPS102", "lnsbasicinfo");
				}
				if(1!= count){
					throw new FabException("SPS102", "lnsbasicinfo");
				}
			}
		}
		if(loanStat.trim().equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE))
			endFlag = "3";
		else {
			endFlag = "1";

			/**
			 * 	贷款未结清时 可能存在需要PO 更改状态变为 N
			 * 		1、非PO的贷款 无需变更状态
			 * 		2、如果有非正常（宽限期）的账单 也不需要更改状态
			 * 	    3、如果存在罚息 复利的账单 也不需要更改
			 * 	add at 2018-03-13
			 */
			if(loanStat.trim().equals(ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR)){
				Map<String, Object> billMap = new HashMap<>();
				billMap.put("acctno", acctNo);
				billMap.put("brc", ctx.getBrc());
				if(tblLnsBills == null){
					try {
						tblLnsBills = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsbill_repay", billMap, TblLnsbill.class);
					} catch (FabSqlException e) {
						throw new FabException(e, "SPS103", "query_lnsbill_repay");
					}
				}

				loanStat = ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL;
				for (TblLnsbill lnsbill : tblLnsBills) {
					if (!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, ConstantDeclare.LOANSTATUS.LOANSTATUS_GRACE).contains(lnsbill.getBillstatus())
							||VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_CINT,ConstantDeclare.BILLTYPE.BILLTYPE_DINT).contains(lnsbill.getBilltype())) {
						return;
					}
				}

				//更新贷款状态
				Map<String, Object> basicMap = new HashMap<String, Object>();
				basicMap.put("loanstat", loanStat);
				basicMap.put("acctno", acctNo);
				basicMap.put("openbrc", ctx.getBrc());
				basicMap.put("oldloanstat", lnsbasicinfo.getLoanstat());
				basicMap.put("modifyDate", ctx.getTranDate());

				int count;
				try{
					count = DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_loanstat", basicMap);
				}catch (FabSqlException e){
					throw new FabException(e, "SPS102", "lnsbasicinfo");
				}
				if(1!= count){
					throw new FabException("SPS102", "lnsbasicinfo");
				}
				LoggerUtil.info("更新后贷款状态:" + loanStat);

			}
			//add end
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
	 * @return the endFlag
	 */
	public String getEndFlag() {
		return endFlag;
	}

	/**
	 * @param endFlag the endFlag to set
	 */
	public void setEndFlag(String endFlag) {
		this.endFlag = endFlag;
	}


}

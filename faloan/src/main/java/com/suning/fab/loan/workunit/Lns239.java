package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsrentreg;
import com.suning.fab.loan.domain.TblLnszlamortizeplan;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.loan.utils.LoanTrailCalculationProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * @author 	
 *
 * @version V1.0.1
 *
 * @see 	满足退货条件的冲销摊销收益
 *
 * 
 * 
 *
 *
 * @exception
 */

@Scope("prototype")
@Repository
public class Lns239 extends WorkUnit {

	FabAmount repayAmt;	//退款金额
	String acctNo;		//借据号
	
	@Autowired
	LoanEventOperateProvider eventProvider;
	
	@Override
	public void run() throws Exception {
		
		TranCtx ctx = getTranctx();
		
		//参数验证
		if(VarChecker.isEmpty(repayAmt)||!repayAmt.isPositive()){
			throw new FabException("LNS056","退款金额");
		}
		//查询合同金额
		//读取借据主文件
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("brc", ctx.getBrc());

		TblLnsrentreg lnsrentreg;
		try {
			lnsrentreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrentreg", param, TblLnsrentreg.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsrentreg");
		}

		if (null == lnsrentreg){
			throw new FabException("SPS104", "lnsrentreg");
		}
		//合同金额
		FabAmount contractAmt;
		if(VarChecker.isEmpty(lnsrentreg.getReqmap())){
			throw new FabException("SPS104", "lnsrentreg");
		}
		if(lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("contractAmt"))==-1){
			//根据字符串截取合同金额的值
			contractAmt = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("contractAmt")).split(":")[1]));
		}else{
			contractAmt = new FabAmount(Double.valueOf(lnsrentreg.getReqmap().substring(lnsrentreg.getReqmap().indexOf("contractAmt"),lnsrentreg.getReqmap().indexOf("|", lnsrentreg.getReqmap().indexOf("contractAmt"))).split(":")[1]));
		}
		
		Map<String,Object> billParam = new HashMap<String,Object>();
		billParam.put("acctno", acctNo);
		billParam.put("brc", ctx.getBrc());
		
		//获取账单表信息
		List<Map<String, Object>> lnsbillList = null;
		try {
			lnsbillList = DbAccessUtil.queryForList("CUSTOMIZE.query_hisbills", billParam);
		} catch (FabSqlException e) {
			LoggerUtil.error("查询lnsbill表错误{}", e);
		}
		
		FabAmount repayedMoney = new FabAmount(0.0);
		//计算已还金额
		if(lnsbillList != null && !lnsbillList.isEmpty()){
			//遍历账单计算
			for(Map<String, Object> lnsbill : lnsbillList){
				BigDecimal billamt = new BigDecimal(lnsbill.get("billamt").toString());
				BigDecimal billbal = new BigDecimal(lnsbill.get("billbal").toString());
				repayedMoney.selfAdd(new FabAmount(billamt.subtract(billbal).doubleValue()));
			}
		}
		
		//判断合同金额减去已还金额是否等于退款金额
		contractAmt.selfSub(repayedMoney);
		if(!contractAmt.isPositive()){
			throw new FabException("LNS060");
		}else if(!repayAmt.equals(contractAmt)){
			throw new FabException("LNS059", repayAmt.getVal().toString(), contractAmt.getVal().toString());
		}
		
		//租赁公司处理摊销信息
		if( "51240000".equals(ctx.getBrc()))
		{
			//获取摊销信息
			TblLnszlamortizeplan lnszlamortizeplan;
			try {
				lnszlamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnszlamortizeplan", billParam, TblLnszlamortizeplan.class);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "lnszlamortizeplan");
			}
			if (null == lnszlamortizeplan) {
				throw new FabException("SPS104", "lnszlamortizeplan");
			}
			
			
			
			LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo,ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN,ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
			List<FabAmount> amtList = new ArrayList<FabAmount>();
			amtList.add(   new FabAmount(lnszlamortizeplan.getAmortizetax().doubleValue())   );
	
			// 写事件
			if(new FabAmount(lnszlamortizeplan.getAmortizeamt().doubleValue()).isPositive())
				eventProvider.createEvent(ConstantDeclare.EVENT.AMORTIZOFF,  new FabAmount(lnszlamortizeplan.getAmortizeamt().doubleValue()),
						acctinfo, null, loanAgreement.getFundInvest(),
						ConstantDeclare.BRIEFCODE.TXCX, ctx, amtList, ctx.getTranDate(),
						ctx.getSerSeqNo(), 1);
			
			int count;
			try{
				count = DbAccessUtil.execute("CUSTOMIZE.update_lnszlamortizeplan_cx", billParam);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS102", "lnszlamortizeplan");
			}
			if(1!= count){
				throw new FabException("SPS102", "lnszlamortizeplan");
			}
		}
			
		
		
		
	}


	public FabAmount getRepayAmt() {
		return repayAmt;
	}


	public void setRepayAmt(FabAmount repayAmt) {
		this.repayAmt = repayAmt;
	}


	public String getAcctNo() {
		return acctNo;
	}


	public void setAcctNo(String acctNo) {
		this.acctNo = acctNo;
	}

	

}

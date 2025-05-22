
package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.utils.*;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsAmortizeplan;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.VarChecker;


/**
 * @author 	
 *
 * @version V1.0.1
 *
 * @see 	修改利率
 *
 * @param	acctNo			借据号
 * 			rateType		 利率类型
 * 			realRate		利率
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns606 extends WorkUnit {

	String acctNo;  //借据号
	String beforeExpandDate; //展期前到期日
	String afterExpandDate; //展期后到期日
	String serialNo; //业务流水号
	String flag;
	@Autowired LoanEventOperateProvider eventProvider;


	@Override
	public void run() throws Exception{
		
		TranCtx ctx = getTranctx();
		
		//参数为空判断
		if (VarChecker.isEmpty(beforeExpandDate) ){
			throw new FabException("LNS055","展期前到期日");
		}
		if (VarChecker.isEmpty(afterExpandDate)){
			throw new FabException("LNS056","展期后到期日");
		}
		if (VarChecker.isEmpty(acctNo) ){
			throw new FabException("LNS055","借据号");
		}
		if (VarChecker.isEmpty(serialNo) ){
			throw new FabException("LNS055","业务流水号");
		}
		if(CalendarUtil.beforeAlsoEqual(afterExpandDate, beforeExpandDate)){
			throw new FabException("LNS111");
		}

		
		//查询主文件信息
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		param.put("brc", ctx.getBrc());

		TblLnsbasicinfo lnsbasicinfo;
		try {
			lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsbasicinfo");
		}

		if (null == lnsbasicinfo){
			throw new FabException("SPS104", "lnsbasicinfo");
		}
		

		//非标不符合要求
		if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_XXHB
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_XBHX
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_YBYX
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_ZDY).split(",")).contains(lnsbasicinfo.getRepayway())){
			throw new FabException("LNS045");
		}
		//汽车、手机、房抵贷不支持
		if(Arrays.asList("2412615,2412617,2412614".split(",")).contains(lnsbasicinfo.getPrdcode())){
			throw new FabException("LNS110");
		}
		
		//逾期借据不予展期
//		if(!ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL.equals(lnsbasicinfo.getLoanstat())){
//			throw new FabException("LNS107");
//		}
		
		//判断是否是二次展期
		if(lnsbasicinfo.getFlag1().contains("A")){
			throw new FabException("LNS109");
		}
		if(!lnsbasicinfo.getContduedate().equals(beforeExpandDate)){
			throw new FabException("LNS119","合同到期日");
		}
		//合同到期日之后无法展期
		if(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), lnsbasicinfo.getContduedate())){
			throw new FabException("LNS108");
		}
		//等本等息最后一期已还款，不能展期
		if(new FabAmount(lnsbasicinfo.getContractbal()).isZero()){
			throw new FabException("LNS125");
		}
		
		//放款当天不允许展期
		if(ctx.getTranDate().equals(lnsbasicinfo.getOpendate())){
			throw new FabException("LNS121");
		}
		
		//判断是短期、中期还是长期贷款
		int yearsNum = CalendarUtil.yearsBetween(lnsbasicinfo.getBeginintdate(), lnsbasicinfo.getContduedate());
		int expandYearNum = CalendarUtil.yearsBetween(beforeExpandDate, afterExpandDate);
		if(yearsNum<1 || (yearsNum==1 && lnsbasicinfo.getBeginintdate().substring(4).equals(lnsbasicinfo.getContduedate().substring(4)))){
			if(expandYearNum>1 || (expandYearNum==1 && !lnsbasicinfo.getContduedate().substring(4).equals(afterExpandDate.substring(4)))){
				throw new FabException("LNS105");
			}
		}else if(yearsNum<5 || (yearsNum==5 && lnsbasicinfo.getBeginintdate().substring(4).equals(lnsbasicinfo.getContduedate().substring(4)))){
			if(CalendarUtil.actualDaysBetween(beforeExpandDate, afterExpandDate)>(CalendarUtil.actualDaysBetween(lnsbasicinfo.getBeginintdate(), lnsbasicinfo.getContduedate())/2)){
				throw new FabException("LNS105");
			}
		}else{
			if(expandYearNum==3 && !beforeExpandDate.substring(4).equals(afterExpandDate.substring(4))){
				throw new FabException("LNS105");
			}
		}
		
		//还款方式是5和6不符合要求
		if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_LSBQ
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_LZHK).split(",")).contains(lnsbasicinfo.getRepayway())){
			throw new FabException("LNS045");
		}
		
		//根据账号生成协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		//生成还款计划
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
		List<LnsBill> lnsbill = new ArrayList<LnsBill>();
		//历史期账单（账单表）
		lnsbill.addAll(lnsBillStatistics.getHisBillList());
		lnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前期和未来期
		lnsbill.addAll(lnsBillStatistics.getBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		
		//最后一期判断
		boolean isLastPeriod = false;
		boolean isLastNintPeriod = false;
		//最后一期本金账本
		LnsBill lastPrinBill = new LnsBill();
		
		for(LnsBill bill : lnsbill){
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(bill.getBillType())){
				if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
						|| 
						(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
					if(bill.getEndDate().equals(lnsbasicinfo.getContduedate())){
						isLastPeriod = true;
						lastPrinBill = bill;
					}
				}
			}
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
				if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
						|| 
						(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
					if(bill.getEndDate().equals(lnsbasicinfo.getContduedate())){
						isLastNintPeriod = true;
					}
				}
			}
		}
		
		if(isLastNintPeriod && isLastPeriod){
			isLastPeriod = true;
		}else{
			isLastPeriod = false;
		}
		
		//修改主文件合同到期日
		param.put("contduedate", afterExpandDate);
		param.put("flag1", lnsbasicinfo.getFlag1()==null?"":lnsbasicinfo.getFlag1()+"A");
		param.put("modifyDate", ctx.getTranDate());
		param.put("extnums", lnsbasicinfo.getCurprinterm());
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_606", param);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsbasicinfo");
		}

		param.put("key", "ZQ");
		param.put("acctno", acctNo);
		param.put("openbrc", tranctx.getBrc());

		//修改主文件拓展表
		Map<String,Object> exParam = new HashMap<String,Object>();
		exParam.put("acctno", acctNo);
		exParam.put("openbrc", ctx.getBrc());
		exParam.put("brc", ctx.getBrc());
		exParam.put("key", "ZQ");
		exParam.put("value2", lnsbasicinfo.getContractbal());
		//最后一期展期做特殊处理
		if(isLastPeriod
				&& !ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(lnsbasicinfo.getRepayway())){
			//value1字段存入格式 是最后一期的话（1，[原最后一期开始日]，[原最后一期结束日]）
			String value1 = "1"+","+lastPrinBill.getStartDate()+","+lastPrinBill.getEndDate();
			exParam.put("value1", value1);
			la.getBasicExtension().setLastPeriodStr(value1);
		}else{
			exParam.put("value1", "0");
			la.getBasicExtension().setLastPeriodStr("0");
		}
		TblLnsbasicinfoex lnsbasicinfoex = LoanDbUtil.queryForObject("Lnsbasicinfoex.selectByUk", param, TblLnsbasicinfoex.class);
		//支持 延期还款后展期
		if(lnsbasicinfoex==null)
			LoanDbUtil.insert("CUSTOMIZE.insert_lnsbasicinfoex", exParam);
		else
			LoanDbUtil.insert("Lnsbasicinfoex.update_lnsbasicinfoex", exParam);
		la.getBasicExtension().setBalBeforeEx(new FabAmount(lnsbasicinfo.getContractbal()));
		
		if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
			//查询等本等息未生成账本的期数
			//机构
			param.put("brc", ctx.getBrc());
			Integer dbdxMaxPeriod;
			try {
				//取主文件信息
				dbdxMaxPeriod = DbAccessUtil.queryForObject("CUSTOMIZE.query_period", param, Integer.class);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbill");
			}
			if(null == dbdxMaxPeriod){
				dbdxMaxPeriod = 0;
			}
			la.getBasicExtension().setExtnums(dbdxMaxPeriod+1);
		}else{
			la.getBasicExtension().setExtnums(lnsbasicinfo.getCurprinterm());
		}
		
		//判断是否有摊销计划
		LnsAmortizeplan lnsamortizeplan;
		
		try {
			lnsamortizeplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsamortizeplan", param, 
					LnsAmortizeplan.class);
		} catch(FabSqlException e) {
			throw new FabException(e, "SPS102", "CUSTOMIZE.query_lnsamortizeplan");
		}
		
		if (null != lnsamortizeplan) {
			Map<String,Object> amortizeplanParam = new HashMap<String,Object>();
			amortizeplanParam.put("brc", ctx.getBrc());
			amortizeplanParam.put("acctno", acctNo);
			amortizeplanParam.put("enddate", afterExpandDate);
			
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsamortizeplan_enddate", amortizeplanParam);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsamortizeplan");
			}
		}
		
		
		//主逻辑
		//如果是等本等息、等额本息、等额本金、季还本，月还息
		if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_DBDX
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_DEBX
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_DEBJ
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_JYHK+","+ConstantDeclare.REPAYWAY.REPAYWAY_WILLFUL).split(",")).contains(lnsbasicinfo.getRepayway())){
			la.getContract().setContractEndDate(afterExpandDate);
			la.getContract().setFlag1("A");
			//生成还款计划
			LnsBillStatistics afterBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
			List<LnsBill> afterlnsbill = new ArrayList<LnsBill>();
			//历史期账单（账单表）
			afterlnsbill.addAll(afterBillStatistics.getHisBillList());
			afterlnsbill.addAll(afterBillStatistics.getHisSetIntBillList());
			//当前期和未来期
			afterlnsbill.addAll(afterBillStatistics.getBillInfoList());
			afterlnsbill.addAll(afterBillStatistics.getFutureBillInfoList());
			afterlnsbill.addAll(afterBillStatistics.getFutureOverDuePrinIntBillList());
			//提前还款到利息,利息账本和并得到应还
			Map<Integer, Double> billTermRet = new HashMap<Integer, Double>();
			for(LnsBill lnsBill2 :afterlnsbill){
				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill2.getBillType())){
					if(billTermRet.containsKey(lnsBill2.getPeriod())){
						billTermRet.put(lnsBill2.getPeriod(),  billTermRet.get(lnsBill2.getPeriod())+lnsBill2.getBillAmt().getVal());
					}else{
						billTermRet.put(lnsBill2.getPeriod(),  lnsBill2.getBillAmt().getVal());
					}
				}
				
			}
			for(int i=0;i<afterlnsbill.size();i++){
				LnsBill bill = afterlnsbill.get(i);
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(bill.getBillType())){
					if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
							&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
							|| 
							(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
							&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
						param.clear();
						param.put("acctno", acctNo);
						param.put("openbrc", ctx.getBrc());
						param.put("brc", ctx.getBrc());
						param.put("repayterm", bill.getPeriod());
						param.put("termretprin", bill.getBillAmt().getVal());
						if(ConstantDeclare.REPAYWAY.REPAYWAY_JYHK.equals(lnsbasicinfo.getRepayway()) 
								&& isLastPeriod 
								&& !bill.getPeriod().equals(la.getBasicExtension().getExtnums())){
							param.put("repayterm", la.getBasicExtension().getExtnums());
							param.put("termretprin", 0.00);
						}
						FabAmount prinbalance = new FabAmount(lnsbasicinfo.getContractbal().doubleValue());
						if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
							prinbalance = bill.getPrinBal();
						}
						if(!ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway()) || (ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway()) && bill.getLastDate() == null)){
							prinbalance.selfSub(bill.getBillAmt());
						}
						if(ConstantDeclare.REPAYWAY.REPAYWAY_JYHK.equals(lnsbasicinfo.getRepayway()) 
								&& isLastPeriod 
								&& !bill.getPeriod().equals(la.getBasicExtension().getExtnums())){
							prinbalance = new FabAmount(lnsbasicinfo.getContractbal().doubleValue());
						}
						param.put("balance", prinbalance.getVal());
						if(isLastPeriod && CalendarUtil.actualDaysBetween(beforeExpandDate, afterExpandDate) <= la.getWithdrawAgreement().getPeriodMinDays()){
							param.put("repayownbdate", afterExpandDate);
							param.put("repayownedate", CalendarUtil.nDaysAfter(afterExpandDate, lnsbasicinfo.getGracedays()).toString("yyyy-MM-dd"));
							param.put("repayintedate", afterExpandDate);
							param.put("actrepaydate", afterExpandDate);
							param.put("balance", 0.00);
						}
						try {
							//还款计划
							DbAccessUtil.execute(
									"CUSTOMIZE.update_lnsrpyplan_605", param);
							
						} catch (FabSqlException e) {
							throw new FabException(e, "SPS103", "update_lnsrpyplan_605");
						}
						if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
							/*删除当前期之前的还款计划表*/
							param.put("repayterm", la.getBasicExtension().getExtnums()-1);
							try {
								DbAccessUtil.execute("CUSTOMIZE.delete_lnsrpyplan_606", param);
							} catch (FabSqlException e) {
								throw new FabException(e, "SPS101", "展期涉及");
							}
						}
					}
				}
				//bianli出当前期利息账单进行处理
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
					if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
							&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
							|| 
							(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
							&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
						if(bill.getBillAmt().equals(bill.getBillBal())){
							param.clear();
							param.put("acctno", acctNo);
							param.put("openbrc", ctx.getBrc());
							param.put("brc", ctx.getBrc());
							param.put("termretint", new BigDecimal(billTermRet.get(bill.getPeriod())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
							param.put("repayterm", bill.getPeriod());
							
							try {
								//还款计划
								DbAccessUtil.execute(
										"CUSTOMIZE.update_lnsrpyplan_605", param);
								
							} catch (FabSqlException e) {
								throw new FabException(e, "SPS103", "update_lnsrpyplan_605");
							}
						}
					}
				}
			}
		}
		//
		if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_HDE
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE
				+","+ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX).split(",")).contains(lnsbasicinfo.getRepayway())){
			String prinPerformula = getPrinPerformula(lnsbasicinfo.getBeginintdate(),afterExpandDate);
			param.put("prinperformula", prinPerformula);
			la.getWithdrawAgreement().setPeriodFormula(getPrinPerformula(la.getContract().getStartIntDate(),afterExpandDate));
			if(ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(lnsbasicinfo.getRepayway())){
				param.put("intperformula", prinPerformula);
				la.getInterestAgreement().setPeriodFormula(getPrinPerformula(la.getContract().getStartIntDate(),afterExpandDate));
				
			}
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_606", param);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfo");
			}
			
			for(int i=0;i<lnsbill.size();i++){
				LnsBill bill = lnsbill.get(i);
				if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
						|| 
						(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
					if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType()) || null != lnsamortizeplan){
						la.getContract().setContractEndDate(afterExpandDate);
						la.getContract().setFlag1("A");
						//生成还款计划
						LnsBillStatistics afterBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
						List<LnsBill> afterlnsbill = new ArrayList<LnsBill>();
						//历史期账单（账单表）
						afterlnsbill.addAll(afterBillStatistics.getHisBillList());
						afterlnsbill.addAll(afterBillStatistics.getHisSetIntBillList());
						//当前期和未来期
						afterlnsbill.addAll(afterBillStatistics.getBillInfoList());
						afterlnsbill.addAll(afterBillStatistics.getFutureBillInfoList());
						afterlnsbill.addAll(afterBillStatistics.getFutureOverDuePrinIntBillList());
						//提前还款到利息,利息账本和并得到应还
						Map<String, String> billTermRet = new HashMap<String, String>();
						for(LnsBill lnsBill2 :afterlnsbill){
							if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill2.getBillType())){
								if(billTermRet.containsKey(lnsBill2.getPeriod().toString())){
									billTermRet.put(lnsBill2.getPeriod().toString(),  String.valueOf(Double.valueOf(billTermRet.get(lnsBill2.getPeriod().toString()))+lnsBill2.getBillAmt().getVal()));
									if(CalendarUtil.before(lnsBill2.getStartDate(), billTermRet.get(lnsBill2.getPeriod()+"_1"))){
										billTermRet.put(lnsBill2.getPeriod()+"_1",  lnsBill2.getStartDate());
									}
								}else{
									billTermRet.put(lnsBill2.getPeriod().toString(),  lnsBill2.getBillAmt().getVal().toString());
									billTermRet.put(lnsBill2.getPeriod()+"_1",  lnsBill2.getStartDate());
								}
							}
							
						}
						LnsBill afterbill = afterlnsbill.get(i);
						param.clear();
						param.put("acctno", acctNo);
						param.put("openbrc", ctx.getBrc());
						param.put("brc", ctx.getBrc());
						param.put("repayterm", afterbill.getPeriod());
						if(Arrays.asList((ConstantDeclare.REPAYWAY.REPAYWAY_HDE
								+","+ConstantDeclare.REPAYWAY.REPAYWAY_BDQBDE).split(",")).contains(lnsbasicinfo.getRepayway())){
							if(!afterbill.getEndDate().equals(afterExpandDate)){
								param.put("termretprin", 0.00);
							}
						}
						FabAmount prinbalance = new FabAmount(lnsbasicinfo.getContractbal().doubleValue());
						param.put("balance", prinbalance.getVal());
						if((isLastPeriod && lnsBillStatistics.getIntTotalPeriod().equals(afterBillStatistics.getIntTotalPeriod()) ) || ( null != lnsamortizeplan)){
							param.put("repayownbdate", afterExpandDate);
							param.put("repayownedate", CalendarUtil.nDaysAfter(afterExpandDate, lnsbasicinfo.getGracedays()).toString("yyyy-MM-dd"));
							param.put("repayintbdate", billTermRet.get(bill.getPeriod()+"_1"));
							param.put("repayintedate", afterExpandDate);
							param.put("actrepaydate", afterExpandDate);
							param.put("balance", 0.00);
							//如果是最后一期并且有展期前已生成未还清利息账本  更新结束日期与状态开始日期
							param.put("curenddate", afterExpandDate);
							param.put("repayedate", afterExpandDate);
							param.put("statusbdate", afterExpandDate);
							param.put("period", afterbill.getPeriod());
							try {
								//更新账本
								DbAccessUtil.execute(
										"CUSTOMIZE.update_lnsbill_606", param);
								
							} catch (FabSqlException e) {
								throw new FabException(e, "SPS103", "update_lnsbill_606");
							}
						}
						param.put("termretint", new BigDecimal(billTermRet.get(bill.getPeriod().toString())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
						if( null != lnsamortizeplan){
							param.put("termretint", 0.00);
						}
						try {
							//还款计划
							DbAccessUtil.execute(
									"CUSTOMIZE.update_lnsrpyplan_605", param);
							
						} catch (FabSqlException e) {
							throw new FabException(e, "SPS103", "update_lnsrpyplan_605");
						}
					}
				}
			}
			
		}
		
		
		Map<String,Object> dyParam = new HashMap<String,Object>();
		dyParam.put("acctno", acctNo);
		dyParam.put("brc", ctx.getBrc());
		dyParam.put("trandate", ctx.getTranDate());
		//更新动态表修改时间
		try {
			DbAccessUtil.execute("CUSTOMIZE.update_dyninfo_603", dyParam);
		} catch (FabSqlException e) {
			throw new FabException(e, "SPS102", "lnsaccountdyninfo");
		}
	
		//幂等
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTranDate()); 
		lnsinterface.setSerialno(serialNo);
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode("470017");
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setAcctname(lnsbasicinfo.getName());
		lnsinterface.setUserno(lnsbasicinfo.getCustomid());
		lnsinterface.setAcctno(acctNo);
		lnsinterface.setTranamt(0.0);
		lnsinterface.setMagacct(acctNo);
		lnsinterface.setReserv3(beforeExpandDate);
		lnsinterface.setReserv4(afterExpandDate);
		try{
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
		} catch(FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e,TRAN.IDEMPOTENCY);
			}
			else
				throw new FabException(e, "SPS100", "lnsinterface");
		}
		
	}

	private static String getPrinPerformula(String begin,String end){
		DateTime b = new DateTime(begin);  
		DateTime e = new DateTime(end);  

		//计算区间天数  
		Period p = new Period(b, e, PeriodType.yearMonthDay());
		String result = "";
		if(p.getYears() != 0 && (p.getMonths() != 0 || p.getDays() != 0)){
			result += p.getYears()+"Y";
		}else if(p.getMonths() == 0 && p.getDays() == 0){
			result += p.getYears()+"YA";
		}
		if(p.getMonths() != 0 && p.getDays() != 0){
			result += p.getMonths()+"M";
		}else if(p.getMonths() != 0 && p.getDays() == 0){
			result += p.getMonths()+"MA";
		}
		if(p.getDays() != 0){
			result += p.getDays()+"DA";
		}
		return result;
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




	public String getSerialNo() {
		return serialNo;
	}


	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}


	public String getFlag() {
		return flag;
	}


	public void setFlag(String flag) {
		this.flag = flag;
	}
	

	/**
	 * @return the eventProvider
	 */
	public LoanEventOperateProvider getEventProvider() {
		return eventProvider;
	}


	/**
	 * @param eventProvider the eventProvider to set
	 */
	public void setEventProvider(LoanEventOperateProvider eventProvider) {
		this.eventProvider = eventProvider;
	}



	public String getBeforeExpandDate() {
		return beforeExpandDate;
	}



	public void setBeforeExpandDate(String beforeExpandDate) {
		this.beforeExpandDate = beforeExpandDate;
	}



	public String getAfterExpandDate() {
		return afterExpandDate;
	}



	public void setAfterExpandDate(String afterExpandDate) {
		this.afterExpandDate = afterExpandDate;
	}

}

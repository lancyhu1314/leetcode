package com.suning.fab.loan.utils;
/*
 * Copyright (C), 2002-2018, 苏宁易购电子商务有限公司
 * FileName: AcctStatistics.java
 * Author:
 * Date:     2019年04月18日 下午7:45:49
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */


import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.account.FaLoanPubDict;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.PreRepay;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.*;


/**
 *
 * @author
 * @date 2019年4月17日下午7:45:49
 * @since 1.0
 */
public abstract class ProvisionRepayQuery {

	static ExecutorService executor = Executors.newFixedThreadPool(4);

	/**
	 * key超时时间
	 */
//    protected static final int SECONDS = 1800;

	/**
	 * key值最大限制
	 */
	private static  int limit = 1;

	/**
	 * 线程执行最大时间
	 */
	protected static final int EXEC_LIMIT = 10;

	/**
	 *
	 * @return
	 * @throws FabException
	 * @since 1.0
	 */
	public static PreRepay dealQueryList(final String acctNo, final String brc,  final FabRate feeRate, final String tranDate, final Integer serseqNo) throws FabException {
		LoggerUtil.info("dealQueryList begin"+"["+acctNo+"]"+"["+brc+"]"+"["+feeRate.toString()+"]");
		long startTimestamp = System.currentTimeMillis();

		PreRepay preValue = new PreRepay();
		try {

			//RedisUtil.del(acctNo + brc);
			String redisValue = RedisUtil.get(acctNo + brc);
			LoggerUtil.debug("after redis get operation");
			JSONObject redisObj = JSONObject.parseObject(redisValue);
			if(null!=redisObj)
				LoggerUtil.info("redis取值："+redisObj.toString());
			DateFormat basicFormart = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSS");
			DateFormat redisFormart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

			//查询主文件信息
			Map<String,Object> param = new HashMap<String,Object>();
			param.put("acctno", acctNo);
			param.put("openbrc", brc);
			TblLnsbasicinfo lnsbasicinfo = null;
			String basictime = "";

			try {
				LoggerUtil.debug("before query basicinfo");
				lnsbasicinfo = DbAccessUtil.queryForObject("Lnsbasicinfo.selectByUk", param, TblLnsbasicinfo.class);
				basictime = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsbasicinfo_470020", param, String.class);
				LoggerUtil.debug("after query basicinfo"+basictime);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS103", "lnsbasicinfo");
			}
			if (null == lnsbasicinfo){
				throw new FabException("ACC108", acctNo);
			}

			//补充时间戳位数
			if( basictime.length() < 23 )
				basictime = basictime.concat("000");


			//不存在或失效的数据、跨日的数据、主文件时间戳变动的数据重新同步，其他的直接取
			if( null != redisValue &&
					CalendarUtil.afterAlsoEqual(redisObj.getString("timestamp").substring(0,10), tranDate) &&
					redisFormart.parse(redisObj.getString("timestamp").substring(0,23)).compareTo(basicFormart.parse(lnsbasicinfo.getModifytime().substring(0,23)))>0)
			{
				
				try {
					LoggerUtil.info("get=====redis"+"acctNo"+":"+lnsbasicinfo.getAcctno()+"==========");
					preValue.setAcctNo(redisObj.getString("acctNo"));
					preValue.setBrc(redisObj.getString("brc"));
					preValue.setCleanPrin(new FabAmount(redisObj.getDouble("cleanPrin")));
					preValue.setPrinAmt(new FabAmount(redisObj.getDouble("prinAmt")));
					preValue.setCleanInt(new FabAmount(redisObj.getDouble("cleanInt")));
					preValue.setIntAmt(new FabAmount(redisObj.getDouble("intAmt")));
					preValue.setCleanForfeit(new FabAmount(redisObj.getDouble("cleanForfeit")));
					preValue.setForfeitAmt(new FabAmount(redisObj.getDouble("forfeitAmt")));
					preValue.setOverPrin(new FabAmount(redisObj.getDouble("overPrin")));
					preValue.setOverInt(new FabAmount(redisObj.getDouble("overInt")));
					preValue.setOverDint(new FabAmount(redisObj.getDouble("overDint")));
					if("false".equals(GlobalScmConfUtil.getProperty("batchQuerySwitch","false"))){
						preValue.setCleanFee(new FabAmount(redisObj.getDouble("cleanFee")));
					}else{
						LoggerUtil.info("进入新批量还款查询");
						//未结本金判断  并且利率大于0
						if(redisObj.getDouble("totalPrin")!=null&&redisObj.getDouble("totalNint")!=null&&feeRate.isPositive()){
							Map<String,FabAmount> prinAndNint=new HashMap();
							prinAndNint.put("totalPrin",new FabAmount(redisObj.getDouble("totalPrin")));
							prinAndNint.put("totalNint",new FabAmount(redisObj.getDouble("totalNint")));
							preValue.setCleanFee(AdvanceFeeUtil.calAdvanceFeeByTotalPrin(feeRate,prinAndNint,redisObj.getString("advanceFeeType"),
									new FabAmount(redisObj.getDouble("dynamicCapAmt")),new FabAmount(redisObj.getDouble("dynamicCapDiff"))));
						}else{
							preValue.setCleanFee(new FabAmount(redisObj.getDouble("cleanFee")));
						}
					}
					//添加逾期利息和本金
					preValue.setExceedInt(new FabAmount(redisObj.getDouble("exceedInt")));
					preValue.setExceedPrin(new FabAmount(redisObj.getDouble("exceedPrin")));
					preValue.setTimestamp(redisObj.getString("timestamp"));
					//redisList.add(preValue);
				}catch (Exception e){
					LoggerUtil.error("redis读取信息赋值失败", e);
					preValue = getProvisionRepayInfo(acctNo, brc, feeRate, lnsbasicinfo, basictime, tranDate, serseqNo);
					RedisUtil.setWithExpire(acctNo + brc, JsonTransfer.ToJson(preValue), 86400); //86400
				}
			}
			else
			{
				LoggerUtil.info("set=====redis"+"acctNo"+":"+lnsbasicinfo.getAcctno()+"==========");
				preValue = getProvisionRepayInfo(acctNo, brc, feeRate, lnsbasicinfo, basictime, tranDate, serseqNo);
				RedisUtil.setWithExpire(acctNo + brc, JsonTransfer.ToJson(preValue), 86400); //86400
			}

		}  catch (FabSqlException e){
			LoggerUtil.error("FabSqlException:",e);
			throw new FabException("LNS157", "FabSqlException");
		}catch (FabException e) {
			LoggerUtil.error("FabException:",e);
			throw new FabException("LNS157", "FabException");
		} catch (Exception e) {
			LoggerUtil.error("Exception:",e);
			throw new FabException("LNS157", "Exception");
		}
		long  costtime = System.currentTimeMillis()-startTimestamp;
		LoggerUtil.info("dealQueryList function cost time:[{}]", costtime);

		return preValue;
	}



	/**
	 * 提交查询内容
	 * 功能描述: <br>
	 * @return
	 * @since 1.0
	 */
	public static Future<PreRepay> execute(final String acctNo, final String brc, final FabRate feeRate, final String tranDate, final Integer serseqNo) {
		return executor.submit(new Callable<PreRepay>() {
			@Override
			public PreRepay call() throws Exception {
				return dealQueryList(acctNo, brc, feeRate, tranDate, serseqNo);
			}
		});
	}

	/**
	 *
	 * 功能描述: <br>
	 * 处理预约查询金额
	 *
	 * @throws FabException
	 * @see [相关类/方法](可选)
	 * @since [产品/模块版本](可选)
	 */
	public static PreRepay getProvisionRepayInfo(String acctNo, String listBrc, FabRate cleanFeeRate, TblLnsbasicinfo lnsbasicinfo, String basictime, String tranDate, Integer serseqNo) throws FabException {
		LoggerUtil.info("getProvisionRepayInfo begin"+"["+acctNo+"]"+"["+listBrc+"]"+"["+cleanFeeRate.toString()+"]"+"["+basictime+"]");

		//初始化返回值和预约时间
		PreRepay preValue = new PreRepay();
		String endDate = tranDate;
		//金额赋值
		FabAmount cleanPrin = new FabAmount(0.00);
		FabAmount prinAmt = new FabAmount(0.00);
		FabAmount cleanInt = new FabAmount(0.00);
		FabAmount intAmt = new FabAmount(0.00);
		FabAmount cleanForfeit = new FabAmount(0.00);
		FabAmount forfeitAmt = new FabAmount(0.00);
		FabAmount overPrin = new FabAmount(0.00);
		FabAmount overInt = new FabAmount(0.00);
		FabAmount overDint = new FabAmount(0.00);
		FabAmount cleanFee = new FabAmount(0.00);


		FabAmount oPrin = new FabAmount(0.00);
		FabAmount oInt = new FabAmount(0.00);
        //逾期本金利息
		FabAmount exceedPrin=new FabAmount(0.00);
		FabAmount exceedInt=new FabAmount(0.00);

		//机构list
		if( null != listBrc && !VarChecker.isEmpty(listBrc) );
		//TODO:ctx.setBrc(listBrc);
		
/*		//读取借据主文件
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);	
		param.put("openbrc", ctx.getBrc());*/

		//结息日为空就取起息日
		if(VarChecker.isEmpty(lnsbasicinfo.getLastintdate())){
			lnsbasicinfo.setLastintdate(lnsbasicinfo.getBeginintdate());
		}
		TranCtx ctx = new TranCtx();
		//避免空指针异常
        ctx.mapRequest(new FaLoanPubDict());
		ctx.setBrc(listBrc);
		ctx.setTranDate(tranDate);
		ctx.setSerSeqNo(serseqNo);
		//根据账号生成协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
		if (la.getContract().getContractStartDate().equals(endDate) && 
				"3".equals(la.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(endDate, 1).toString("yyyy-MM-dd");
		}

		if(CalendarUtil.equalDate(lnsbasicinfo.getOpendate(), lnsbasicinfo.getBeginintdate())){
			//开户日当天还款还到最后一期本金，但是没还清
			if(!CalendarUtil.equalDate(lnsbasicinfo.getLastintdate(), lnsbasicinfo.getContduedate())
					&& CalendarUtil.equalDate(endDate, lnsbasicinfo.getOpendate())){
				if(new FabAmount(lnsbasicinfo.getContractamt())
						.sub(new FabAmount(lnsbasicinfo.getContractbal())).isZero()){
					preValue.setCleanPrin(new FabAmount(lnsbasicinfo.getContractbal()));
					preValue.setAcctNo(acctNo);
					preValue.setBrc(listBrc);
					preValue.setTimestamp(basictime);
					return preValue; //如果是倒起息开户 当天预约那就有利息不能直接返回 此段放子交易最后 重置本金
				}
			}
		}

		
		
		
		//生成还款计划
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, endDate, ctx);
		//历史期账单（账单表）
		List<LnsBill> hisLnsbill = new ArrayList<LnsBill>();
		List<LnsBill> lnsbill = new ArrayList<LnsBill>();
		hisLnsbill.addAll(lnsBillStatistics.getHisBillList());
		hisLnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		//当前期和未来期
		lnsbill.addAll(lnsBillStatistics.getBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
		lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());

//		//计算利息 author:chenchao
//		FabAmount totalNint=new FabAmount(0.00);
//		//费用收取方式
//		String advanceFeeType=la.getInterestAgreement().getAdvanceFeeType();

		String repayDate = endDate;
		//预约日期大于合同结束日期取合同结束日
		if(CalendarUtil.after(endDate, lnsbasicinfo.getContduedate())){
			endDate = lnsbasicinfo.getContduedate();
		}
		//用于记录提前还款还了部分账单的期数标识
		int period = 0;
		//遍历历史账单
		for(LnsBill bill:hisLnsbill){
			//过滤AMLT账单和已结清账单
			if("AMLT".equals(bill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE).contains(bill.getSettleFlag())){
				continue;
			}

			//计算已到期本金管理费以及利息 2019-04-22
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(),endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					oPrin.selfAdd(bill.getBillBal());

				}
				if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					oInt.selfAdd(bill.getBillBal());
				}
			}

			if(CalendarUtil.after(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
					FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
					if (famt != null)
					{
						cleanForfeit.selfAdd(famt);
						forfeitAmt.selfAdd(famt);
						overDint.selfAdd(famt);
					}
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){

					if( VarChecker.asList("11", "12", "13").contains(lnsbasicinfo.getRepayway()) && !VarChecker.isEmpty(bill.getHisBill()) )
					{
						cleanInt.selfAdd(bill.getRepayDateInt());
						intAmt.selfAdd(bill.getRepayDateInt());
					}
					else
					{
						cleanInt.selfAdd(bill.getBillBal());
						intAmt.selfAdd(bill.getBillBal());
						FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
						if (fint != null)
						{
							cleanForfeit.selfAdd(fint);
							forfeitAmt.selfAdd(fint);
							overDint.selfAdd(fint);
						}
					}
					if(period < bill.getPeriod()){
						period = bill.getPeriod();
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				}
				continue;
			}
			//历史期
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(), endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overPrin.selfAdd(bill.getBillBal());
					}

					//判断预期本金
					if(Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(bill.getBillStatus())){
						exceedPrin.selfAdd(bill.getBillBal());
					}
					//计算呆滞呆账罚息复利
					FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
					if (famt != null)
					{
						cleanForfeit.selfAdd(famt);
						forfeitAmt.selfAdd(famt);
						overDint.selfAdd(famt);
					}
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					cleanInt.selfAdd(bill.getBillBal());
					intAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overInt.selfAdd(bill.getBillBal());
					}

					//判断预期利息
					if(Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(bill.getBillStatus())){
						exceedInt.selfAdd(bill.getBillBal());
					}

					//计算呆滞呆账罚息复利
					FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
					if (fint != null)
					{
						cleanForfeit.selfAdd(fint);
						forfeitAmt.selfAdd(fint);
						overDint.selfAdd(fint);
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				}
			}
			//未来期
			else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					if( VarChecker.asList("11", "12", "13").contains(lnsbasicinfo.getRepayway()) && !VarChecker.isEmpty(bill.getHisBill()) )
					{
						cleanInt.selfAdd(bill.getRepayDateInt());
					}
					else
					{
						cleanInt.selfAdd(bill.getBillBal());
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				}
			}
		}
		String termStartDate = "";
		int termperiod = 0;
		//遍历当前期账单和未来期账单
		for(LnsBill bill:lnsbill){

			if ("".equals(termStartDate) || CalendarUtil.after(bill.getStartDate(), termStartDate)){
				termStartDate = bill.getStartDate();
			}

			//过滤AMLT账单和已结清账单
			if("AMLT".equals(bill.getBillType())
					|| VarChecker.asList(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE ).contains(bill.getSettleFlag())){
				continue;
			}

			//计算已到期本金管理费以及利息 2019-04-22
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(),endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					oPrin.selfAdd(bill.getBillBal());

				}
				if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					oInt.selfAdd(bill.getBillBal());
				}
			}

			//计算呆滞呆账罚息复利
			if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
				FabAmount famt = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getOverdueRate(), repayDate);
				if (famt != null)
				{
					cleanForfeit.selfAdd(famt);
					forfeitAmt.selfAdd(famt);
					overDint.selfAdd(famt);
				}
			}
			else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
				FabAmount fint = LoanBillSettleInterestSupporter.calculateBaddebtsInt(la,bill, la.getRateAgreement().getCompoundRate(), repayDate);
				if (fint != null)
				{
					cleanForfeit.selfAdd(fint);
					forfeitAmt.selfAdd(fint);
					overDint.selfAdd(fint);
				}
			}

			//当前期
			if((CalendarUtil.after(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())
					&& VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,
					ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())
					&& (period == 0 || period >= bill.getPeriod()))
					|| ("true".equals(la.getInterestAgreement().getIsCalTail())
					&& CalendarUtil.afterAlsoEqual(endDate, bill.getStartDate())
					&& CalendarUtil.beforeAlsoEqual(endDate, bill.getEndDate())
					&& VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN,
					ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())
					&& (period == 0 || period >= bill.getPeriod()))){//还款方式为12（先本后息）时，会先还利息，第一期的本金会在未来期出现

				if(termperiod == 0){
					termperiod = bill.getPeriod();
				}

				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					if(termperiod == bill.getPeriod()){
						prinAmt.selfAdd(bill.getBillBal());
					}
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					//等本等息
					if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
						intAmt.selfAdd(bill.getBillBal());
						cleanInt.selfAdd(bill.getBillBal());
					}
					//非等本等息
					else {
						intAmt.selfAdd(bill.getRepayDateInt());
						cleanInt.selfAdd(bill.getRepayDateInt());
					}
				}
				continue;
			}
			//试算出的历史期
			if(CalendarUtil.beforeAlsoEqual(bill.getEndDate(), endDate)){
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					prinAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overPrin.selfAdd(bill.getBillBal());
					}

					//判断预期本金
					if(Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(bill.getBillStatus())){
						exceedPrin.selfAdd(bill.getBillBal());
					}
				}
				else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())){
					cleanInt.selfAdd(bill.getBillBal());
					intAmt.selfAdd(bill.getBillBal());
					if(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS.equals(bill.getBillStatus())
							||ConstantDeclare.LOANSTATUS.LOANSTATUS_PARTOVR.equals(bill.getBillStatus())){
						overInt.selfAdd(bill.getBillBal());
					}

					//判断预期利息
					if(Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_OVERDU,ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(bill.getBillStatus())){
						exceedInt.selfAdd(bill.getBillBal());
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				}
			}
			//未来期
			else{
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())){
					cleanPrin.selfAdd(bill.getBillBal());
					//还款方式为8（随借随还）时，还多少本金插多少本金，当天之后的本金全部属于未来期
					if("8".equals(lnsbasicinfo.getRepayway())){
						prinAmt.selfAdd(bill.getBillBal());
					}
				}
				else if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
						ConstantDeclare.BILLTYPE.BILLTYPE_GINT).contains(bill.getBillType())){
					cleanForfeit.selfAdd(bill.getBillBal());
					forfeitAmt.selfAdd(bill.getBillBal());
					overDint.selfAdd(bill.getBillBal());
				}
			}

		}


		//结清手续费
//		if( null != la.getBasicExtension().getFeeRate() &&
//				!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
//				la.getBasicExtension().getFeeRate().isPositive())
//		{
//			FabAmount totalPrin = new FabAmount(0.00);
//			if( la.getContract().getContractStartDate().equals(endDate) ||
//					la.getContract().getContractEndDate().equals(endDate) ||
//					"8".equals(la.getWithdrawAgreement().getRepayWay()))
//			{
//				cleanFee.setVal(0.00);
//			}
//			else
//			{
//				List<LnsBill> lnsbillAll = new ArrayList<LnsBill>();
//
//				//取账本信息
//				lnsbillAll.addAll(lnsBillStatistics.getHisBillList());
//				lnsbillAll.addAll(lnsBillStatistics.getHisSetIntBillList());
//				lnsbillAll.addAll(lnsBillStatistics.getBillInfoList());
//				lnsbillAll.addAll(lnsBillStatistics.getFutureBillInfoList());
//				lnsbillAll.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
//				if( Arrays.asList("0","1","2","9","10").contains(la.getWithdrawAgreement().getRepayWay()))
//				{
//					for(LnsBill billNint:lnsbillAll){
//						//预约还款日期小于等于账本开始日期的属于未来期
//						if( CalendarUtil.beforeAlsoEqual(endDate, billNint.getStartDate())){
//							//累加未来期未还本金
//							if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
//								totalPrin.selfAdd(billNint.getBillBal());
//							}
//							//累加未来期利息金额
//							else if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(billNint.getBillType())){
//								totalNint.selfAdd(billNint.getBillBal());
//							}
//						}
//
//						//2019-10-12 现金贷提前还款违约金   开户传费率时按开户费率计算
//						if( null != la.getBasicExtension().getFeeRate() &&
//								!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
//								la.getBasicExtension().getFeeRate().isPositive() )
//						{
//							if( CalendarUtil.after(endDate, billNint.getStartDate()) &&
//									CalendarUtil.beforeAlsoEqual(endDate, billNint.getEndDate()) 	){
//								//累加当期本金
//								if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(billNint.getBillType())){
//									totalPrin.selfAdd(billNint.getBillBal());
//								}
//							}
//						}
//					}
//				}
//				else
//				{
////							throw new FabException("LNS051");
//					Map<Integer,LnsBill> map = new HashMap<Integer,LnsBill>();
//					for(LnsBill bill:lnsbill){
//						LnsBill billList = map.get(bill.getPeriod());
//						if( null == billList )
//						{
//							//存利息账本开始结束日
//							bill.setBillBal( new FabAmount(0.00) );
//							map.put(bill.getPeriod(), bill);
//						}
//						else
//						{
//							//取较小的作为开始日期
//							if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
//									CalendarUtil.before(bill.getStartDate(), billList.getStartDate() ) )
//								billList.setStartDate(bill.getStartDate());
//							//取较大的作为结束日期
//							if( ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(bill.getBillType())  &&
//									CalendarUtil.after(bill.getEndDate(), billList.getEndDate() ) )
//								billList.setEndDate(bill.getEndDate());
//							//取本金余额作为当期本金
//							if( ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(bill.getBillType())  &&
//									bill.getBillBal().sub(billList.getBillBal()).isPositive() )
//								billList.setBillBal(bill.getBillBal());
//							map.put(bill.getPeriod(), billList);
//						}
//					}
//
//					for (LnsBill value : map.values()) {
//						//预约还款日期小于等于账本开始日期的属于未来期
//						if( CalendarUtil.beforeAlsoEqual(endDate, value.getStartDate())){
//							//累加未来期未还本金
//							totalPrin.selfAdd(value.getBillBal());
//						}
//
//						if( null != la.getBasicExtension().getFeeRate() &&
//								!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
//								la.getBasicExtension().getFeeRate().isPositive() )
//						{
//							if( CalendarUtil.after(endDate, value.getStartDate()) &&
//									CalendarUtil.beforeAlsoEqual(endDate, value.getEndDate()) 	){
//								//累加当期本金
//								totalPrin.selfAdd(value.getBillBal());
//							}
//						}
//					}
//				}
//
//				//服务费=剩余本金*手续费率
//				cleanFee = new FabAmount(  BigDecimal.valueOf(totalPrin.getVal()).multiply(la.getBasicExtension().getFeeRate().getVal()).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue()    );
//				if( la.getBasicExtension().getDynamicCapAmt().isPositive() &&
//						la.getBasicExtension().getDynamicCapDiff().sub(cleanFee).isNegative())
//					cleanFee = la.getBasicExtension().getDynamicCapDiff();
//
//				//利息和费用大小比较
//				if(ConstantDeclare.ADVANCEFEETYPE.FIXED.equals(advanceFeeType)&&totalNint.sub(cleanFee).isNegative()){
//					//取利息和费用 最小的作为收取费用
//					cleanFee=totalNint;
//				}
//			}
//		}
//
//
//		//服务费=剩余本金*手续费率
//		if( null != cleanFeeRate && !VarChecker.isEmpty(cleanFeeRate) )
//		{
//			cleanFee = new FabAmount(  BigDecimal.valueOf(totalPrin.getVal()).multiply(la.getBasicExtension().getFeeRate().getVal()).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue()    );
//
//			if( la.getBasicExtension().getDynamicCapAmt().isPositive() &&
//					la.getBasicExtension().getDynamicCapDiff().sub(cleanFee).isNegative())
//				cleanFee = la.getBasicExtension().getDynamicCapDiff();
//		}
//		//利息和费用大小比较
//		if(ConstantDeclare.ADVANCEFEETYPE.FIXED.equals(advanceFeeType)&&totalNint.sub(cleanFee).isNegative()){
//			//取利息和费用 最小的作为收取费用
//			cleanFee=totalNint;
//		}
		if( null != la.getBasicExtension().getFeeRate() && 
	    	!VarChecker.isEmpty(la.getBasicExtension().getFeeRate()) &&
	    	la.getBasicExtension().getFeeRate().isPositive() )
	    {
	    	cleanFeeRate = la.getBasicExtension().getFeeRate();
	    }
		
    	//放款日当天还款，手续费不收  ,  随借随还手续费不收
		if(!"3".equals(la.getInterestAgreement().getAdvanceFeeType()) && la.getContract().getContractStartDate().equals(endDate) ||
			la.getContract().getContractEndDate().equals(endDate) ||
			"8".equals(la.getWithdrawAgreement().getRepayWay()) ||
			null == cleanFeeRate )
		{
			cleanFee.setVal(0.00);
		}
		else{
			//判断批量还款查询费用的开关
			if("false".equals(GlobalScmConfUtil.getProperty("batchQuerySwitch","false"))){
				cleanFee = AdvanceFeeUtil.calAdvanceFee( cleanFeeRate, endDate, la, lnsBillStatistics);
			}else{
				//实时计算需要存储剩余本金 罚息 封顶值
				//存放未来期本金和利息
				//费用收取方式
				String advanceFeeType=la.getInterestAgreement().getAdvanceFeeType();
				//动态收益封顶
				FabAmount dynamicCapAmt=la.getBasicExtension().getDynamicCapAmt();
				//动态收益封顶差值
				FabAmount dynamicCapDiff=la.getBasicExtension().getDynamicCapDiff();
				Map<String ,FabAmount> prinAndNint =AdvanceFeeUtil.calAdvanceTotalPrin(endDate,la,lnsBillStatistics);
				cleanFee=AdvanceFeeUtil.calAdvanceFeeByTotalPrin(cleanFeeRate,prinAndNint,advanceFeeType,dynamicCapAmt,dynamicCapDiff);
				//设置未来期本金利息
				preValue.setTotalPrin(prinAndNint.get("totalPrin"));
				preValue.setTotalNint(prinAndNint.get("totalNint"));
				//设置动态封顶相关信息
				preValue.setAdvanceFeeType(advanceFeeType);
				preValue.setDynamicCapAmt(dynamicCapAmt);
				preValue.setDynamicCapDiff(dynamicCapDiff);
			}
		}
		preValue.setAcctNo(acctNo);
		preValue.setBrc(listBrc);
		preValue.setCleanPrin(cleanPrin);
		preValue.setPrinAmt(prinAmt);
		preValue.setCleanInt(cleanInt);
		preValue.setIntAmt(intAmt);
		preValue.setCleanForfeit(cleanForfeit);
		preValue.setForfeitAmt(forfeitAmt);
		preValue.setOverPrin(oPrin);
		preValue.setOverInt(oInt);
		preValue.setOverDint(cleanForfeit);
		preValue.setTimestamp(basictime);
		preValue.setCleanFee(cleanFee);
		//设置逾期利率 利息
		preValue.setExceedPrin(exceedPrin);
		preValue.setExceedInt(exceedInt);
		LoggerUtil.info("getProvisionRepayInfo end"+"["+acctNo+"]"+"["+listBrc+"]"+"["+basictime+"]");
		return preValue;
	}


	/**
	 *
	 * 功能描述: <br>
	 *
	 *
	 * @param str
	 * @param e
	 * @see [相关类/方法](可选)
	 * @since [产品/模块版本](可选)
	 */
	public static void loggerMethod(String str, Exception e) {
//    	LoggerUtil.warn(str);
	}



	/**
	 * 获取limit
	 * @return limit
	 */
	public static int getLimit() {
		return limit;
	}


	/**
	 * 设置limit
	 * @param limit
	 */
	public static void setLimit(int limit) {
		ProvisionRepayQuery.limit = limit;
	}


	/**
	 * @return the executor
	 */
	public static ExecutorService getExecutor() {
		return executor;
	}


	/**
	 * @param executor the executor to set
	 */
	public static void setExecutor(ExecutorService executor) {
		ProvisionRepayQuery.executor = executor;
	}


	/**
	 * @return the execLimit
	 */
	public static int getExecLimit() {
		return EXEC_LIMIT;
	}



}

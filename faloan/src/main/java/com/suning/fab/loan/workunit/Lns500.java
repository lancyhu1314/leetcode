package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsprovision;
import com.suning.fab.loan.domain.TblLnsprovisiondtl;
import com.suning.fab.loan.entity.BillPeriodAmt;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;


/**
 * @author 	AY
 *
 * @version V1.0.1
 *
 * 			repayDate		计提日期
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns500 extends WorkUnit {

	
	String acctNo;						//借据号
	String repayDate;					//计提日期
	String transDate;					//为借新还旧接口报文repayDate映射字段重复定义

	int txnseq=0;
	
	/**最小差异*/  
    public static final double MINDIF=0.00000001;  

	LnsBillStatistics lnssatistics;		//试算信息
	TblLnsbasicinfo lnsbasicinfo;		//贷款基本信息

	@Autowired 
	LoanEventOperateProvider eventProvider;

	@Override
	public void run() throws Exception{
		
		TranCtx ctx = getTranctx();
		
		//日期不能为空!
		if (VarChecker.isEmpty(repayDate)){
			throw new FabException("LNS005");
		}

		//贷款基本信息在原交易479001中已经获取
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("acctno", acctNo);
		param.put("openbrc", ctx.getBrc());
		if (null == lnsbasicinfo){
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
		}
		
		//计提标志为close不计提
		//核销贷款计提标志为CLOSE,还款接口补计提在原交易中修改lnsbasicinfo.getProvisionflag()为正常	20190522|14050183
		if(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE.equals(lnsbasicinfo.getProvisionflag())){
			return;
		}


		//判断数据库中是否已存在计提
		//根据账号生成协议
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);

		TblLnsprovision lnsprovisionBasic;
		try {
			lnsprovisionBasic=LoanProvisionProvider.getLnsprovisionBasic(ctx.getBrc(),null,lnsbasicinfo.getAcctno(),ConstantDeclare.BILLTYPE.BILLTYPE_NINT,ConstantDeclare.INTERTYPE.PROVISION, la);
		}
		catch (FabSqlException e)
		{
			throw new FabException(e, "SPS103", "lnsprovision");
		}
		//上次计提日期晚于交易日期不计提,当天不可重复计提（为什么不是afterAlsoEqule？）
	 if (lnsprovisionBasic.isExist()&&CalendarUtil.after(lnsprovisionBasic.getLastenddate().toString(), repayDate)){
			return;
		}
		
		//结息日为空取起息日
		if (VarChecker.isEmpty(lnsbasicinfo.getLastintdate())){
			lnsbasicinfo.setLastintdate(lnsbasicinfo.getBeginintdate());
		}
		
		//计提总税金
		FabAmount totaltax = new FabAmount();
		//计提总利息
		FabAmount totalnint = new FabAmount();
		FabRate monrate = new FabRate(0.0);

		
		//贷款结清直接累加所有利息账单金额（提前结清，总计提金额不大于已还利息）
		/**
		 * 等本等息实际利率计提,所以不能直接找利息账单
		 * 20190522|14050183
		 * 非等本等息结清||等本等息放款日结清(原因未知)
		 */
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())
				&& (!ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())
						|| repayDate.equals(lnsbasicinfo.getBeginintdate()))){
			Double nintAmt;
			try {
				nintAmt = DbAccessUtil.queryForObject("CUSTOMIZE.query_nintbillsum", param, Double.class);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS103", "lnsbill");
			}

			if(null != nintAmt){
				totalnint.selfAdd(nintAmt);
				totaltax.selfAdd(TaxUtil.calcVAT(totalnint));
			}
		}
		else{
			//生成还款计划
			LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate, ctx);
			//封顶时，更新
			DynamicCapUtil.updateDTFDA(la,ctx);
			lnssatistics = lnsBillStatistics;

			/** 核销的贷款 不进行利息计提 2018-12-20 start */
			if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains( lnsbasicinfo.getLoanstat())){
				return;
			}
			/** end **/

			//历史期账单（账单表）
			List<LnsBill> lnsbill = new ArrayList<LnsBill>();
			lnsbill.addAll(lnsBillStatistics.getHisBillList());
			lnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
			//当前期和未来期
			lnsbill.addAll(lnsBillStatistics.getBillInfoList());
			lnsbill.addAll(lnsBillStatistics.getFutureBillInfoList());
			lnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
			//实际利率计算
			//当前时间点所处期数
			Integer nowPeriod = 0;
			Integer nowDayNum=0;
			if(ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
				for(LnsBill bill : lnsbill){
					if(isCurrentPeriod(la, bill)){
						//计算当前期实际占用天数
						nowPeriod = bill.getPeriod();
						nowDayNum=CalendarUtil.actualDaysBetween(bill.getStartDate(),bill.getEndDate());
					}
				}
			}
			Map<String, BillPeriodAmt> cashFlowMap = new LinkedHashMap<String, BillPeriodAmt>();
			if (ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
				//对账单按期数进行升序排序，避免提前结清乱序
				Collections.sort(lnsbill, new Comparator<LnsBill>() {
					@Override
					public int compare(LnsBill o1, LnsBill o2) {
						return o1.getPeriod()-o2.getPeriod();
					}
				});
				//存放现金流 公式需要
				cashFlowMap.put("0",new BillPeriodAmt(-lnsbasicinfo.getContractamt(),0));
				//区分退货账本的索引 退货期数：期数-k 这种形式
				int k=0;
				for(LnsBill bill : lnsbill){
					//索引递增
					k++;
					//本金或者利息参与现金流计算
					if(bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
							||bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)){
						//账单属性
						String billProperty=bill.getBillProperty();
						//账单期数
						String billPeriod=bill.getPeriod()+"";
						//判断账单属性是否为退货
						if("RETURN".equals(billProperty)){
							billPeriod=bill.getPeriod()+"-"+k;
						}
						//判断是否已经添加至集合 将本金、利息进行合并
						if(cashFlowMap.containsKey(billPeriod)){
							Double cash = cashFlowMap.get(billPeriod).getBillAmt();
							cash += bill.getBillAmt().getVal();
							cashFlowMap.get(billPeriod).setBillAmt(cash);
						}else{
							//结清并且是等本等息、并且遍历期数>当前期
							if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())
									&& !nowPeriod.equals(0)
									&& nowPeriod.compareTo(bill.getPeriod())<0){
								//主要是提前结清已经生成账本的合并之前的计提
								Double cash = cashFlowMap.get(nowPeriod+"").getBillAmt();
								cash += bill.getBillAmt().getVal();
								cashFlowMap.get(nowPeriod+"").setBillAmt(cash);
								cashFlowMap.get(nowPeriod+"").setCountDay(nowDayNum);
							}else{
								//提前结清irr利率计算时 所占天数从本期开始日开始 判断是不是提前结清 账单日在还款日之后
								if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())&&CalendarUtil.after(bill.getEndDate(),repayDate)){
									//提前结清 当前账本结束日在计提日 之后的 用计提日计算所占天数
									cashFlowMap.put(billPeriod,new BillPeriodAmt(bill.getBillAmt().getVal(),CalendarUtil.actualDaysBetween(bill.getStartDate(),repayDate)));
								}else{
									//退货的现金流从合同开始日算
									if("RETURN".equals(billProperty)){
										//退货的  并且不参与现金流的累加
										BillPeriodAmt billPeriodAmt=new BillPeriodAmt(bill.getBillAmt().getVal(),CalendarUtil.actualDaysBetween(la.getContract().getContractStartDate(),bill.getTranDate().toString()));
										billPeriodAmt.setBillProperty("RETURN");
										cashFlowMap.put(billPeriod,billPeriodAmt);
									}else{
										cashFlowMap.put(billPeriod,new BillPeriodAmt(bill.getBillAmt().getVal(),CalendarUtil.actualDaysBetween(bill.getStartDate(),bill.getEndDate())) );
									}
								}
							}
						}
					}
				}
		        //实际利率
		        monrate = new FabRate(getIrr(cashFlowMap));
			}
			List<BillPeriodAmt> cashFlows=changeCashFlow(cashFlowMap);
			
			//等本等息账单金额重新生成
			if (ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
				lnsbill = rebornDBDXBill(lnsbill,monrate,cashFlows,cashFlowMap);
			}
			//非标自定义不规则   非标自定义规则修改 按普通的流转
//			if (ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway())  )
//			{
//				//合同结束日取总利息轧差
//				if( CalendarUtil.afterAlsoEqual(repayDate, la.getContract().getContractEndDate()) )
//				{
//					totalnint.selfAdd(la.getBasicExtension().getTotalInt().getVal());
//					totaltax.selfAdd(TaxUtil.calcVAT(new FabAmount(la.getBasicExtension().getTotalInt().getVal())));
//				}
//				else
//				{
//					//日利息=总/总天数（四舍五入）
//					BigDecimal dayInt = new BigDecimal(la.getBasicExtension().getTotalInt().getVal()).divide(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(la.getContract().getContractStartDate(), la.getContract().getContractEndDate())),2,BigDecimal.ROUND_HALF_UP);
//					//当日利息=日利息*当期天数
//					BigDecimal bigInt = dayInt.multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(la.getContract().getContractStartDate(), repayDate))).setScale(2,BigDecimal.ROUND_HALF_UP);
//					//超过总利息取总利息
//					if( new FabAmount(bigInt.doubleValue()).sub(la.getBasicExtension().getTotalInt().getVal()).isPositive()  )
//						bigInt = BigDecimal.valueOf(la.getBasicExtension().getTotalInt().getVal());
//					//到当天总利息和总税金
//					totalnint.selfAdd(bigInt.doubleValue());
//					totaltax.selfAdd(TaxUtil.calcVAT(new FabAmount(bigInt.doubleValue())));
//				}
//
//			}
//			else
//			{
				//累加已还利息
				for (LnsBill bill:lnsbill){
					if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
						//历史期账本直接累加
						if (CalendarUtil.beforeAlsoEqual(bill.getEndDate(), repayDate)){
							totalnint.selfAdd(bill.getBillAmt());
							totaltax.selfAdd(TaxUtil.calcVAT(bill.getBillAmt()));
						}
						//当期账本计算（P2P当期算头算尾，与历史期有交叉？）
						else if (isCurrentPeriod(la, bill)){
							//等本等息利息计算（提前还到未来期怎么计算？）
							if (ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())
									&& !ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())){
								//按天计算利息
								BigDecimal dayNint = BigDecimal.valueOf(bill.getBillAmt().getVal()).divide(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(bill.getStartDate(), bill.getEndDate())),2,BigDecimal.ROUND_HALF_UP);
								//按天计算利息
								BigDecimal bigInt = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(bill.getStartDate(), repayDate))
												.multiply(dayNint)
												.setScale(2,BigDecimal.ROUND_HALF_UP);
								//按天计算的利息大于账单金额取账单金额
								if (new FabAmount(bigInt.doubleValue()).sub(bill.getBillAmt()).isPositive()){
									totalnint.selfAdd(bill.getBillAmt());
									totaltax.selfAdd(TaxUtil.calcVAT(bill.getBillAmt()));
								}
								else {
									totalnint.selfAdd(bigInt.doubleValue());
									totaltax.selfAdd(TaxUtil.calcVAT(new FabAmount(bigInt.doubleValue())));
								}
							}

							//其他
							else {
								//无追保理-账速融RepayDateInt是整期利息，计提利息特殊处理
								if( "3010014".equals(lnsbasicinfo.getPrdcode()))
								{
									//日利息=当期利息/当期天数（四舍五入）
									BigDecimal dayInt = new BigDecimal(bill.getRepayDateInt().getVal()).divide(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(bill.getStartDate(), bill.getEndDate())),2,BigDecimal.ROUND_HALF_UP);
									//当日利息=日利息*当期天数
									BigDecimal bigInt = dayInt.multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(bill.getStartDate(), repayDate))).setScale(2,BigDecimal.ROUND_HALF_UP);
									totalnint.selfAdd(bigInt.doubleValue());
									totaltax.selfAdd(TaxUtil.calcVAT(new FabAmount(bigInt.doubleValue())));
								}
								else
								{
									//累加当期开始日到当天利息
									totalnint.selfAdd(bill.getRepayDateInt());
									totaltax.selfAdd(TaxUtil.calcVAT(bill.getRepayDateInt()));
								}
							}
						}
					}

				}
//			}

			if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())
					&& ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
				totalnint = new FabAmount();
				totaltax = new FabAmount();
				for (int i = 1;i<cashFlows.size();i++){
					if(lnsbill.size()>=i){
						LnsBill bill = lnsbill.get(i-1);
						totalnint.selfAdd(bill.getBillAmt());
						totaltax.selfAdd(TaxUtil.calcVAT(bill.getBillAmt()));
					}
					
				}
				
			}
		}
		//计提利息（当日计提利息=试算到当天的利息-已计提利息）
		FabAmount pronint = new FabAmount(totalnint.sub(lnsprovisionBasic.getTotalinterest()).getVal());
		//计提税金
		FabAmount tax = new FabAmount(totaltax.sub(lnsprovisionBasic.getTotaltax()).getVal());
		//获取初始化明细登记数据
		TblLnsprovisiondtl lnsprovisiondtl=LoanProvisionProvider.getInitTblLnsprovisiondtl(ctx,lnsprovisionBasic ,totaltax ,totalnint,++txnseq,la.getContract().getCurrPrinPeriod());
		lnsprovisiondtl.setIrr(monrate.getRate().doubleValue());
		if (pronint.isPositive() || tax.isPositive()){
			//本次入账利息金额
			lnsprovisiondtl.setInterest(pronint.getVal());
			//本次入账税金金额
			lnsprovisiondtl.setTax(tax.getVal());
			//正反标志 取值：POSITIVE正向，NEGATIVE反向
			lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.POSITIVE);
			LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
			try {
				DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS100", "lnsprovisionreg");
			}
			LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency(lnsbasicinfo.getCcy()));
			LoanAgreement loanAgreement = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
			//写事件
			List<FabAmount> amtList = new ArrayList<FabAmount>() ;
			amtList.add(tax);
			eventProvider.createEvent(ConstantDeclare.EVENT.ACCRUEDINT, 
					pronint, acctinfo, null, loanAgreement.getFundInvest(), 
					ConstantDeclare.BRIEFCODE.LXJT, ctx, amtList, repayDate, ctx.getSerSeqNo(), 1, loanAgreement.getCustomer().getMerchantNo() , loanAgreement.getBasicExtension().getDebtCompany());
		//等本等息结清状态并且最后的计提金额为负的情况
		//计提金额为负的情况
		}else if(pronint.isNegative() && tax.isNegative() ){
			//本次计提金额
			lnsprovisiondtl.setInterest(0-pronint.getVal());	//本次利息计提金额为 修改前未结息已计提金额-修改后未结息应计提金额
			//本次计提税金
			lnsprovisiondtl.setTax(0-tax.getVal());	//本次计提税金为已计提税金总额，冲销掉原来的发生额
			//正向还是反向
			lnsprovisiondtl.setInterflag(ConstantDeclare.INTERFLAG.NEGATIVE);//反向
			LoanProvisionProvider.exchangeProvision(lnsprovisiondtl);
			try {
				DbAccessUtil.execute("Lnsprovisiondtl.insert", lnsprovisiondtl);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS100", "lnsprovisionreg");
			}
			//利息 冲销
			//冲销金额为prenint-afternint
			LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, 
					ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			//计算出冲销金额
			List<FabAmount> amtList = new ArrayList<FabAmount>();
			amtList.add(new FabAmount(0-tax.getVal()));
			//写利息计提冲销事件
			eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFINT, new FabAmount(0-pronint.getVal()), 
					acctinfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXCX, ctx, amtList, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
		}
		//利息计提 更新计提总表
		MapperUtil.map(lnsprovisiondtl, lnsprovisionBasic, "map500_01");
		try {
			if(lnsprovisionBasic.isSaveFlag()){
				DbAccessUtil.execute("Lnsprovision.updateByUk", lnsprovisionBasic);
			}else{
				if(!"".equals(lnsprovisiondtl.getInterflag())){
					DbAccessUtil.execute("Lnsprovision.insert", lnsprovisionBasic);
				}
			}
		}catch (FabSqlException e){
			LoggerUtil.info("插入计提明细总表异常：",e);
			throw new FabException(e, "SPS102", "Lnsprovision");
		}
		//提前结清修改主文件计提标志close
		if (ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())){

			Map<String,Object> basicinfo = new HashMap<String,Object>();
			basicinfo.put("acctno", lnsbasicinfo.getAcctno());
			basicinfo.put("brc", lnsbasicinfo.getOpenbrc());
			basicinfo.put("provisionflag", ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);

			int i = 0;
			try {
				i = DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_provisionflag", basicinfo);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS102", "lnsbasicinfo");
			}
			if(1 != i){
				throw new FabException("ACC108", lnsbasicinfo.getAcctno());
			}
		}
	}

	/**
	 * 判断是否是当前期
	 * @param la 协议
	 * @param bill 账单
	 * @return true是当前期
	 */
	private boolean isCurrentPeriod(LoanAgreement la, LnsBill bill) {
		//增加日期所在当前期判断 变更：增加beforeAlsoEqual 含尾判断 修改人：16090227
		if( "false".equals(la.getInterestAgreement().getIsCalTail())){
			if (CalendarUtil.after(repayDate, bill.getStartDate()) &&
					  CalendarUtil.beforeAlsoEqual(repayDate, bill.getEndDate()))
				return true;
		}else{
			if (CalendarUtil.afterAlsoEqual(repayDate, bill.getStartDate()) && 
				     CalendarUtil.beforeAlsoEqual(repayDate, bill.getEndDate()) )
				return true;			
		}
		 return false;
	}



	/**
	 * 讲金额map 转换金额集合
	 * @param cashFlowMap
	 * @return
	 */
	public static List<BillPeriodAmt> changeCashFlow(Map<String, BillPeriodAmt> cashFlowMap){
		Iterator<Map.Entry<String,BillPeriodAmt>> cashFlowIterators=cashFlowMap.entrySet().iterator();
		List<BillPeriodAmt> cashes=new ArrayList<BillPeriodAmt>();
		String periodKey=null;
		while(cashFlowIterators.hasNext()){
			Map.Entry<String,BillPeriodAmt> cashNext=cashFlowIterators.next();
			periodKey=cashNext.getKey();
			BillPeriodAmt billPeriodAmt=cashNext.getValue();
			if(periodKey!=null && periodKey.contains("-")){
				billPeriodAmt.setBillProperty("RETURN");
			}
			cashes.add(billPeriodAmt);
		}
		return cashes;
	}
	
	public static double getIrr(Map<String, BillPeriodAmt> cashFlowMap ){
    	/**迭代次数*/  
        int LOOPNUM=1000; 
        List<BillPeriodAmt> cashFlows=changeCashFlow(cashFlowMap);
        double flowOut=cashFlows.get(0).getBillAmt();
        double minValue=0d;  
        double maxValue=1d;  
        double testValue=0d;  
        while(LOOPNUM>0){  
            testValue=(minValue+maxValue)/2;  
            double npv=NPV(cashFlows,testValue);
            if(Math.abs(flowOut+npv)<MINDIF){  
                break;  
            }else if(Math.abs(flowOut)>npv){  
                maxValue=testValue;  
            }else{  
                minValue=testValue;  
            }  
            LOOPNUM--;  
        }  
        return testValue;  
    }
	
	public static double NPV(List<BillPeriodAmt> flowInArr,double rate){
        double npv=0;
        int sumDays=0;
        for(int i=1;i<flowInArr.size();i++){
        	if("RETURN".equals(flowInArr.get(i).getBillProperty())){
				npv+=flowInArr.get(i).getBillAmt()/Math.pow(1+rate, flowInArr.get(i).getCountDay());
			}else{
				sumDays+=flowInArr.get(i).getCountDay();
				npv+=flowInArr.get(i).getBillAmt()/Math.pow(1+rate, sumDays);
			}
		}
        return npv;  
    }
	
	private List<LnsBill> rebornDBDXBill(List<LnsBill> lnsbill, FabRate monrate, List<BillPeriodAmt> cashFlow,Map<String, BillPeriodAmt> cashFlowMap) {
		// TODO Auto-generated method stub
		List<LnsBill> nintBill = new ArrayList<LnsBill>();
		FabAmount totalContractAmt = new FabAmount(lnsbasicinfo.getContractamt());
		for (LnsBill bill:lnsbill){
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
				nintBill.add(bill);
			}
		}
		Collections.sort(nintBill, new Comparator<LnsBill>() {  
			  
            @Override  
            public int compare(LnsBill o1, LnsBill o2) {  
                int i = o1.getPeriod() - o2.getPeriod();  
                //小老弟说if可能没用	20190522|14050183
                if(i == 0){  
                    return o1.getPeriod() - o2.getPeriod();  
                }  
                return i;  
            }  
        });
		//是否跳过
		int k=0;
		for(int i = 1;i<cashFlow.size();i++){
			//利息
			BigDecimal nint=BigDecimal.valueOf(0);
			//因不是一一对应 需要取现金流的索引
			BillPeriodAmt billPeriodAmt=cashFlow.get(i);
			//判断是否是退货  退货的不参与利息的计算 但是需要把余额减掉
			if(billPeriodAmt.getBillProperty()==null || !"RETURN".equals(billPeriodAmt.getBillProperty())){
			    //不是退货 取现金流的日期 现金流和利息之前的索引相差(i-k)
				if((i-k)<=nintBill.size()){
					//获取对应期数的所占天数  期数所占天数
					BillPeriodAmt billPeriodAmt1=cashFlowMap.get(nintBill.get(i-1-k).getPeriod()+"");
					nint=BigDecimal.valueOf(totalContractAmt.getVal()).multiply(monrate.getRate()).multiply(BigDecimal.valueOf(billPeriodAmt1.getCountDay())).setScale(2,BigDecimal.ROUND_HALF_UP);
					nintBill.get(i-1-k).setBillAmt(new FabAmount(nint.doubleValue()));
				}
				//小老弟说有某种情况能跑进来(场景未知)	20190522|14050183
				else if((i-k)>nintBill.size() && ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())){
					LnsBill bill = new LnsBill();
					//提前结清的金额 计算
					nint=BigDecimal.valueOf(totalContractAmt.getVal()).multiply(monrate.getRate()).multiply(BigDecimal.valueOf(31)).setScale(2,BigDecimal.ROUND_HALF_UP);
					bill.setBillAmt(new FabAmount(nint.doubleValue()));
					nintBill.add(bill);
				}
			}else{
				k++;
			}
			totalContractAmt.selfSub(new FabAmount(BigDecimal.valueOf(cashFlow.get(i).getBillAmt()).subtract(nint).doubleValue()));
		}
		return nintBill;
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
	 * @Return com.suning.fab.loan.bo.LnsBillStatistics lnssatistics
	 */
	public LnsBillStatistics getLnssatistics() {
		return lnssatistics;
	}

	/**
	 * @param lnssatistics
	 */
	public void setLnssatistics(LnsBillStatistics lnssatistics) {
		this.lnssatistics = lnssatistics;
	}

	/**
	 * @Return com.suning.fab.loan.domain.TblLnsbasicinfo lnsbasicinfo
	 */
	public TblLnsbasicinfo getLnsbasicinfo() {
		return lnsbasicinfo;
	}

	/**
	 * @param lnsbasicinfo
	 */
	public void setLnsbasicinfo(TblLnsbasicinfo lnsbasicinfo) {
		this.lnsbasicinfo = lnsbasicinfo;
	}

	/**
	 * @return the transDate
	 */
	public String getTransDate() {
		return transDate;
	}

	/**
	 * @param transDate the transDate to set
	 */
	public void setTransDate(String transDate) {
		this.transDate = transDate;
	}

	public int getTxnseq() {
		return txnseq;
	}

	public void setTxnseq(int txnseq) {
		this.txnseq = txnseq;
	}
}

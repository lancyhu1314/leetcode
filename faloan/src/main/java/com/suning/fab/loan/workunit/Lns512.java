/*
 * Copyright (C), 2002-2019, 苏宁易购电子商务有限公司
 * FileName: Lns512.java
 * Author:   18049702
 * Date:     2018/5/24 18:00
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名    修改时间    版本号       描述
 */
package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.FundInvest;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉：罚息计提
 *
 * @author 18049702
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 * @version 1.0尚未加入历史勘误机制
 *
 */
@Scope("prototype")
@Repository
public class Lns512 extends WorkUnit {

	String acctNo;      //借据号
	String repayDate;   //计提日期
	String repayAcctNo;   //计提日期
	//封顶计息总计提金额---20190111
	FabAmount capTotalNint;
	LnsBillStatistics lnssatistics;	//试算信息
	TblLnsbasicinfo lnsbasicinfo;	//贷款基本信息
	LoanAgreement la;	//贷款基本信息
	Boolean capFlag = false;

	@Autowired
	LoanEventOperateProvider eventProvider;

	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	int txnseq = 0;
	@Override
	public void run() throws Exception{

		TranCtx ctx = getTranctx();

		//封顶判断list
		List<LnsBill> capFlagBills = new ArrayList<LnsBill>();

		//房抵贷特殊处理
		if( "473007".equals(ctx.getTranCode()) )
		{
			repayDate = ctx.getTranDate();
		}

		//读取账户基本信息
		la = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx,la);

		//历史计提总金额
		FabAmount totalCint = new FabAmount(0.00);
		FabAmount totalDint = new FabAmount(0.00);

		//试算信息结果
		FabAmount calCint = new FabAmount(0.00);
		FabAmount calDint = new FabAmount(0.00);
		//累计计提金额
		FabAmount billCint = new FabAmount(0.00);
		FabAmount billDint = new FabAmount(0.00);



		//取计提登记簿信息
		TblLnspenintprovreg penintprovisionDint = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("receiptno", acctNo);
		param.put("brc", ctx.getBrc());
		param.put("billtype", "DINT");
		try {
			penintprovisionDint = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
		}catch (FabSqlException e){
			throw new FabException(e, "SPS103", "罚息计提登记簿Lnspenintprovreg");
		}
		if (null == penintprovisionDint){

			penintprovisionDint = new TblLnspenintprovreg();
			penintprovisionDint.setBrc(ctx.getBrc());
			penintprovisionDint.setReceiptno(acctNo);
			penintprovisionDint.setCcy(la.getContract().getCcy().getCcy());
			penintprovisionDint.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")));
			penintprovisionDint.setTotalinterest(BigDecimal.valueOf(0.00));
			penintprovisionDint.setTotaltax(BigDecimal.valueOf(0.00));
			penintprovisionDint.setBegindate(java.sql.Date.valueOf(la.getContract().getContractStartDate()));
			penintprovisionDint.setTotallist(0);
			penintprovisionDint.setTimestamp(new Date().toString());
			penintprovisionDint.setBilltype("DINT");
			//直接插入表中后面直接更新
			try{
				DbAccessUtil.execute("Lnspenintprovreg.insert", penintprovisionDint);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS100", "Lnspenintprovreg");
			}
		}
		else
		{
			totalDint = new FabAmount(penintprovisionDint.getTotalinterest().doubleValue());
		}

		//如果当天已经计提过则直接返回
		if( "479001".equals(ctx.getTranCode()) )
		{
			String ls = penintprovisionDint.getEnddate().toString();
			if (ls.equals(ctx.getTranDate())){
				return;
			}
		}

		//如果试算信息为空，重新获取
		if (null == lnssatistics){
			lnssatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, repayDate, ctx);
		}

		//房抵贷罚息信息落表 在合同到期日之前
//		if("2412615".equals(lnsbasicinfo.getPrdcode())&&CalendarUtil.beforeAlsoEqual(ctx.getTranDate(),la.getContract().getContractEndDate())){
//			LoanTrailCalculationProvider.getCapAmt(lnssatistics);
//		}
		//封顶值判断
		boolean ifcdbillList = false;//如果selectedbills包含了呆滞呆账账本，则后面不落表
		//试算
		List<LnsBill> selectedbills = new ArrayList<LnsBill>();
		selectedbills.addAll(lnssatistics.getHisSetIntBillList());
		selectedbills.addAll(lnssatistics.getFutureOverDuePrinIntBillList());

		//获取呆滞呆账期间新罚息复利账单list
		//核销的不获取呆滞呆账期间的新罚息复利  在整笔呆滞之前 房抵贷都要落罚息
		if( !VarChecker.asList( "479001").contains(ctx.getTranCode()) ||
//			VarChecker.asList("2412631","2412632","2412633","2412632","2412633","2412623","2512617").contains(lnsbasicinfo.getPrdcode())) //融担开户的呆滞呆账也计提
			"true".equals(la.getInterestAgreement().getIsCompensatory()) || ("2412615".equals(lnsbasicinfo.getPrdcode())&&(!Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(la.getContract().getLoanStat()) || VarChecker.asList( "471007").contains(ctx.getTranCode())))) //融担开户的呆滞呆账也计提
		{
			List<LnsBill> cdbillList = lnssatistics.getCdbillList();
			selectedbills.addAll(cdbillList);
			ifcdbillList= true;
			if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate())){
				FabAmount sumlbcdint = new FabAmount(0.0);
				for(LnsBill lnsbill:cdbillList){
					sumlbcdint.selfAdd(lnsbill.getBillAmt());
				}
				if(sumlbcdint.isPositive()){
					updateDynSum(sumlbcdint.getVal(),"LBCDINT",ctx);
				}

			}
		}

		//封顶计息计提 呆滞呆账规则改变 （针对房抵贷新数据）
		if( VarChecker.asList( "479001").contains(ctx.getTranCode())
				&& !VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
				&& la.getContract().getFlag1().contains("C")){
			List<LnsBill> cdbillList =  lnssatistics.getCdbillList();
			selectedbills.addAll(cdbillList);
			ifcdbillList= true;

			FabAmount sumlbcdint = new FabAmount(0.0);
			for(LnsBill lnsbill:cdbillList){
				sumlbcdint.selfAdd(lnsbill.getBillAmt());
			}
			if(sumlbcdint.isPositive()){
				updateDynSum(sumlbcdint.getVal(),"LBCDINT",ctx);
			}

		}
		/**
		 * 房抵贷老数据呆滞呆账不在封顶计息范围内
		 * 20190516|14050183
		 */
		else if( VarChecker.asList( "479001").contains(ctx.getTranCode())
				&& !VarChecker.isEmpty(la.getInterestAgreement().getCapRate())
				&& !la.getContract().getFlag1().contains("C")){
			//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
			capFlagBills.addAll(lnssatistics.getFutureOverDuePrinIntBillList());
			//历史账单
			capFlagBills.addAll(lnssatistics.getHisBillList());
			//历史未结清账单结息账单
			capFlagBills.addAll(lnssatistics.getHisSetIntBillList());
			//当前账单：从上次结本日或者结息日到还款日之间的本金账单或者利息账单
			capFlagBills.addAll(lnssatistics.getBillInfoList());
			//未来期账单：从还款日到合同到期日之间的本金和利息账单
			capFlagBills.addAll(lnssatistics.getFutureBillInfoList());

			//当前以及未来期本金利息账单
			List<LnsBill> bills = new ArrayList<LnsBill>();
			bills.addAll(lnssatistics.getBillInfoList());
			bills.addAll(lnssatistics.getFutureBillInfoList());

			List<LnsBill> cdbillList =  lnssatistics.getCdbillList();
			capFlagBills.addAll(cdbillList);
			FabAmount totalComeinAmt = new FabAmount(0.0);
			for(LnsBill lnsBill : lnssatistics.getHisBillList()){
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
						.contains(lnsBill.getBillType())){
					totalComeinAmt.selfAdd(lnsBill.getBillAmt());
				}
			}
			for(LnsBill lnsBill : bills){
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
						.contains(lnsBill.getBillType())
						&& !VarChecker.isEmpty(lnsBill.getRepayDateInt())){
					totalComeinAmt.selfAdd(lnsBill.getRepayDateInt());
				}
			}
			for(LnsBill lnsBill : capFlagBills){
				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT).contains(lnsBill.getBillType()) ){
					totalComeinAmt.selfAdd(lnsBill.getBillAmt());
				}
			}
			if(totalComeinAmt.equals(la.getBasicExtension().getCapAmt())){
				this.setCapFlag(true);;
			}
		}

		//账单表
		List<LnsBill> billList = new ArrayList<LnsBill>();
		billList.addAll(lnssatistics.getHisBillList());

		//累加罚息试算金额
		for (LnsBill billDetail : selectedbills){
			if (billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)||
					billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)){
				calDint.selfAdd(billDetail.getBillAmt());

			}


		}
		//累加罚息账单金额  历史期
		for (LnsBill bill : billList){
			if (bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)||
					bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)){
				billDint.selfAdd(bill.getBillAmt());
			}
		}

		//封顶计息判断
//	    if(){
//	    	
//	    }

		/**
		 * 封顶计息只有房抵贷,房抵贷没有复利
		 * 20190516|14050183
		 */
		//取计提登记簿信息
		TblLnspenintprovreg penintprovisionCint = null;
		param.put("billtype", "CINT");
		try {
			penintprovisionCint = DbAccessUtil.queryForObject("Lnspenintprovreg.selectByUk", param, TblLnspenintprovreg.class);
		}catch (FabSqlException e){
			throw new FabException(e, "LNS103", "罚息计提登记簿Lnspenintprovreg");
		}
		if (null == penintprovisionCint){

			Map<String,Object> cIntParam = new HashMap<String,Object>();
			cIntParam.put("acctno", acctNo);
			cIntParam.put("brc", ctx.getBrc());
			cIntParam.put("billtype1", "CINT");
			//获取账单表信息
			Double cintAmt;
			try {
				cintAmt = DbAccessUtil.queryForObject("CUSTOMIZE.query_nintbillsum_512", cIntParam, Double.class);
				if( null != cintAmt)
					totalCint.selfAdd(cintAmt);
			}catch (FabSqlException e)
			{
				throw new FabException(e, "SPS103", "lnsbill");
			}
			if(null == cintAmt){
				cintAmt = 0.00;
			}

			penintprovisionCint = new TblLnspenintprovreg();
			penintprovisionCint.setBrc(ctx.getBrc());
			penintprovisionCint.setReceiptno(acctNo);
			penintprovisionCint.setCcy(la.getContract().getCcy().getCcy());
			penintprovisionCint.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")));
			penintprovisionCint.setTotalinterest(new BigDecimal(cintAmt));
			penintprovisionCint.setTotaltax(new BigDecimal(cintAmt));
			penintprovisionCint.setBegindate(java.sql.Date.valueOf(la.getContract().getContractStartDate()));
			penintprovisionCint.setTotallist(0);
			penintprovisionCint.setTimestamp(new Date().toString());
			penintprovisionCint.setBilltype("CINT");
			//直接插入表中后面直接更新
			try{
				DbAccessUtil.execute("Lnspenintprovreg.insert", penintprovisionCint);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS100", "Lnspenintprovreg");
			}
		}
		else
		{
			totalCint = new FabAmount(penintprovisionCint.getTotalinterest().doubleValue());
		}



		//登记计提明细登记簿
		TblLnspenintprovregdtl penintprovregdtl = new TblLnspenintprovregdtl();
		//累加复利试算金额
		for (LnsBill billDetail : selectedbills){
			if (billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)){
				calCint.selfAdd(billDetail.getBillAmt());

			}

		}
		//累加复利账单金额
		for (LnsBill bill : billList){
			if (bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)){
				billCint.selfAdd(bill.getBillAmt());
			}
		}

		/**
		 *
		 *  到期日之前 不管是否封顶 ，罚息都需要落表
		 *  到期日之后 不落（这之间不会改变封顶值 除非封顶 不会出现此消彼长的情况）
		 *  封顶的当天落
		 *  上线前注意：
		 */


		//明细落表  原正常贷款 罚息明细落表
		if( ((calDint.add(billDint).sub(totalDint).isPositive() || calCint.add(billCint).sub(totalCint).isPositive())&&
				!VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains( lnsbasicinfo.getLoanstat()))
				//封顶 到期日之前  (等本等息只需要封顶值为)
				||((!ConstantDeclare.REPAYWAY.isEqualInterest(la.getWithdrawAgreement().getRepayWay()))
				&&la.getBasicExtension().getDynamicCapAmt().isPositive()
				&&CalendarUtil.before(ctx.getTranDate(),la.getContract().getContractEndDate()))

				//封顶当天 罚息落表
				||(ctx.getTranDate().equals(la.getBasicExtension().getDynamicCapDate()))) {
			downToDtl( ctx, penintprovisionDint, ifcdbillList, selectedbills, penintprovisionCint, penintprovregdtl);
		}//动态封顶改造完  初始化的处理
        else{
        	//如果是动态封顶  老数据处理
			if(!VarChecker.isEmpty(la.getInterestAgreement().getCapRate()) ||
				"2412612".equals(lnsbasicinfo.getPrdcode())){
				//在限制日期之前还是需要落表的
				String limitDate = GlobalScmConfUtil.getProperty("LimitDate", "2020-05-07");
				LoggerUtil.info("limitDate："+limitDate);
				if(CalendarUtil.beforeAlsoEqual(ctx.getTranDate(),limitDate ))
					downToDtl( ctx, penintprovisionDint, ifcdbillList, selectedbills, penintprovisionCint, penintprovregdtl);

			}
		}

		/** 核销的贷款 不进行利息计提 2018-12-20 start */
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains( lnsbasicinfo.getLoanstat())){
			return;
		}
		/** end **/


		// 记罚息，补充税金
		List<FabAmount> amtDintList = new ArrayList<FabAmount>();
		amtDintList.add(TaxUtil.calcVAT(new FabAmount(calDint.add(billDint).sub(totalDint).getVal())));
		LnsAcctInfo lnsDintAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_DINT, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		lnsDintAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat()) ? "3" : "1");
		FundInvest openDintFundInvest = new FundInvest("", "", "", "", "");
		if( calDint.add(billDint).sub(totalDint).isPositive() ){

			//本次应计提的金额=罚息账单的账单金额（数据库表里面的）+试算出来的罚息账单金额-已计提出来的总金额
			FabAmount totalDintInterest = new FabAmount(0.00);
			totalDintInterest.selfAdd(calDint.add(billDint));
			penintprovisionDint.setTotalinterest( BigDecimal.valueOf(totalDintInterest.getVal()) );
			penintprovisionDint.setTotaltax( BigDecimal.valueOf(TaxUtil.calcVAT(totalDintInterest).getVal()) );
			penintprovisionDint.setEnddate(java.sql.Date.valueOf(repayDate));
			penintprovisionDint.setBilltype("DINT");
			try{
				DbAccessUtil.execute("Lnspenintprovreg.updateByUk", penintprovisionDint);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS102", "Lnspenintprovreg");
			}
			add.operate(lnsDintAcctInfo, null, calDint.add(billDint).sub(totalDint), la.getFundInvest(), ConstantDeclare.BRIEFCODE.FXJT, ctx);
			//罚息计提
			AccountingModeChange.saveProvisionTax(ctx, acctNo, calDint.add(billDint).sub(totalDint).getVal(), TaxUtil.calcVAT(new FabAmount(calDint.add(billDint).sub(totalDint).getVal())).getVal(),
					"JT",ConstantDeclare.INTERFLAG.POSITIVE, ConstantDeclare.BILLTYPE.BILLTYPE_DINT);

			if("51240001".equals(ctx.getBrc())){
				String reserv1 = null;
				if("1".equals(lnsbasicinfo.getCusttype())||"PERSON".equals(lnsbasicinfo.getCusttype())){
					reserv1 = "70215243";
				}else if("2".equals(lnsbasicinfo.getCusttype())||"COMPANY".equals(lnsbasicinfo.getCusttype())){
					reserv1 = repayAcctNo;
				}else{
					reserv1 = "";
				}
				lnsDintAcctInfo.setMerchantNo(reserv1);
				//判断是否是封顶计息的
				eventProvider.createEvent(ConstantDeclare.EVENT.DEFAULTINT, calDint.add(billDint).sub(totalDint), lnsDintAcctInfo, null, openDintFundInvest, ConstantDeclare.BRIEFCODE.FXJT, ctx, amtDintList);

				updateDynSum(calDint.add(billDint).sub(totalDint).getVal(),ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ctx);
			}else{
				eventProvider.createEvent(ConstantDeclare.EVENT.DEFAULTINT, calDint.add(billDint).sub(totalDint), lnsDintAcctInfo, null, openDintFundInvest, ConstantDeclare.BRIEFCODE.FXJT, ctx, amtDintList, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
				updateDynSum(calDint.add(billDint).sub(totalDint).getVal(),ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ctx);

			}
		}



		// 记复利，补充税金
		List<FabAmount> amtCintList = new ArrayList<FabAmount>();
		amtCintList.add(TaxUtil.calcVAT(new FabAmount(calCint.add(billCint).sub(totalCint).getVal())));
		LnsAcctInfo lnsCintAcctInfo = new LnsAcctInfo(acctNo, ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_DINT, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
		lnsCintAcctInfo.setCancelFlag(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat()) ? "3" : "1");
		FundInvest openCintFundInvest = new FundInvest("", "", "", "", "");
		if( calCint.add(billCint).sub(totalCint).isPositive() )
		{


			//本次应计提的金额=复利账单的账单金额（数据库表里面的）+试算出来的复利账单金额-已计提出来的总金额
			FabAmount totalCintInterest = new FabAmount(0.00);
			totalCintInterest.selfAdd(calCint.add(billCint));
			penintprovisionCint.setTotalinterest( BigDecimal.valueOf(totalCintInterest.getVal()) );
			penintprovisionCint.setTotaltax( BigDecimal.valueOf(TaxUtil.calcVAT(totalCintInterest).getVal()) );
			penintprovisionCint.setEnddate(java.sql.Date.valueOf(repayDate));
			penintprovisionCint.setBilltype("CINT");
			try{
				DbAccessUtil.execute("Lnspenintprovreg.updateByUk", penintprovisionCint);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS102", "Lnspenintprovreg");
			}


			//复利计提
			AccountingModeChange.saveProvisionTax(ctx, acctNo,  calCint.add(billCint).sub(totalCint).getVal(), TaxUtil.calcVAT(new FabAmount(calCint.add(billCint).sub(totalCint).getVal())).getVal(),
					"JT",ConstantDeclare.INTERFLAG.POSITIVE, ConstantDeclare.BILLTYPE.BILLTYPE_CINT);

			add.operate(lnsCintAcctInfo, null, calCint.add(billCint).sub(totalCint), la.getFundInvest(), ConstantDeclare.BRIEFCODE.FLJT, ctx);
			updateDynSum(calCint.add(billCint).sub(totalCint).getVal(),ConstantDeclare.BILLTYPE.BILLTYPE_CINT,ctx);
			eventProvider.createEvent(ConstantDeclare.EVENT.COMPONDINT, calCint.add(billCint).sub(totalCint), lnsCintAcctInfo, null, openCintFundInvest, ConstantDeclare.BRIEFCODE.FLJT, ctx, amtCintList, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
//			updateExHisComein(la, ctx, calCint.add(billCint).sub(totalCint).getVal());

		}

		//房抵贷罚息信息每日落表 在呆滞之前
		if("2412615".equals(lnsbasicinfo.getPrdcode())&&(!Arrays.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_LANGUISHMENT,ConstantDeclare.LOANSTATUS.LOANSTATUS_BADDEBTS).contains(la.getContract().getLoanStat()) || VarChecker.asList( "471007").contains(ctx.getTranCode()))){
			saveFddDint(la,ctx,lnssatistics);
		}
	}

	private void downToDtl(TranCtx ctx, TblLnspenintprovreg penintprovisionDint, boolean ifcdbillList, List<LnsBill> selectedbills, TblLnspenintprovreg penintprovisionCint, TblLnspenintprovregdtl penintprovregdtl) throws FabException {
		//判断是否封顶
		if(!VarChecker.isEmpty(la.getInterestAgreement().queryDynamicCapRate(ctx.getBrc()))) {
			String trandate;
			//定义查询map
			Map<String, Object> billparam = new HashMap<String, Object>();
			//按账号查询
			billparam.put("receiptno", acctNo);
			billparam.put("brc", ctx.getBrc());
			//防止重复落表  查一遍数据库
			try {
           /*
                  select max(TRANDATE)  from LNSPENINTPROVREGDTL where  brc = :brc and RECEIPTNO = :receiptno and billtype in ('CINT','DINT','GINT')

             )*/
				//取还款计划登记簿数据
				trandate = DbAccessUtil.queryForObject("Lnspenintprovregdtl.selectTrandate", billparam, String.class);

			} catch (FabSqlException e) {
				throw new FabException(e, "SPS103", "Lnspenintprovregdtl");
			}
			if (!VarChecker.isEmpty(trandate) && CalendarUtil.equalDate(trandate.trim(), ctx.getTranDate()))
				return;
			//动态封顶 呆滞呆账数据也落表
			//如果selectedbills包含了呆滞呆账账本，则呆滞呆账不落表
			if (!ifcdbillList) {
				//呆滞呆账 罚息登记明细
				for (LnsBill billDetail : lnssatistics.getCdbillList()) {
					if (billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT) ||
							billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)) {
						penintprovregdtl.setReserv1("C");
						txnseq = insertDtl(txnseq, ctx, penintprovisionDint, penintprovregdtl, billDetail);

					}
					if (billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)) {
						penintprovregdtl.setReserv1("C");
						//登记计提明细登记簿
						txnseq = insertDtl(txnseq, ctx, penintprovisionCint, penintprovregdtl, billDetail);
					}
				}
			}
		}
		//累加罚息试算金额  可能有了呆滞呆账的金额了
		for (LnsBill billDetail : selectedbills) {
			if (billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT) ||
					billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)) {
				penintprovregdtl.setReserv1("");
				txnseq = insertDtl(txnseq, ctx, penintprovisionDint, penintprovregdtl, billDetail);
			}
			if (billDetail.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)) {
				//登记计提明细登记簿
				penintprovregdtl.setReserv1("");
				txnseq = insertDtl(txnseq, ctx, penintprovisionCint, penintprovregdtl, billDetail);
			}
		}
	}

	private int insertDtl(int txnseq, TranCtx ctx, TblLnspenintprovreg penintprovisionDint, TblLnspenintprovregdtl penintprovregdtl, LnsBill billDetail) throws FabException {
		//登记计提明细登记簿
		penintprovregdtl.setReceiptno(acctNo);
		penintprovregdtl.setBrc(ctx.getBrc());
		penintprovregdtl.setCcy(la.getContract().getCcy().getCcy());
		//总表的条数
		penintprovisionDint.setTotallist(penintprovisionDint.getTotallist() + 1);
		//计提登记详细表
		penintprovregdtl.setTrandate(java.sql.Date.valueOf(ctx.getTranDate()));
		penintprovregdtl.setSerseqno(ctx.getSerSeqNo());
		penintprovregdtl.setTxnseq( ++txnseq );
		penintprovregdtl.setPeriod(billDetail.getPeriod());
		penintprovregdtl.setListno(penintprovisionDint.getTotallist());
		penintprovregdtl.setBilltype(billDetail.getBillType());
		penintprovregdtl.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
		penintprovregdtl.setBegindate(java.sql.Date.valueOf(billDetail.getStartDate()));
		penintprovregdtl.setEnddate(java.sql.Date.valueOf(billDetail.getEndDate()));
		penintprovregdtl.setTimestamp(new Date().toString());
		penintprovregdtl.setInterest(billDetail.getBillAmt().getVal());
		penintprovregdtl.setTax(TaxUtil.calcVAT(new FabAmount(penintprovregdtl.getInterest())).getVal());

		//登记详表
		if( new FabAmount(penintprovregdtl.getInterest()).isPositive() )
		{
			try {
				DbAccessUtil.execute("Lnspenintprovregdtl.insert", penintprovregdtl);
			}catch (FabSqlException e){
				throw new FabException(e, "SPS100", "Lnspenintprovregdtl");
			}
		}
		return txnseq;
	}


	public void updateDynSum(Double amount, String billtype, TranCtx ctx) throws FabException{
		//封顶计息特殊判断20190116


		Map<String, Object> param1 = new HashMap<String, Object>();
		param1.put("acctno", acctNo);
		param1.put("openbrc", ctx.getBrc());
		param1.put("trandate", ctx.getTranDate());
		if(ConstantDeclare.BILLTYPE.BILLTYPE_DINT.equals(billtype)){
			param1.put("sumdint", amount);
		}else if(ConstantDeclare.BILLTYPE.BILLTYPE_CINT.equals(billtype)){
			param1.put("sumcint", amount);
		}else if("LBCDINT".equals(billtype)){
			param1.put("sumlbcdint", amount);
		}


		try{
			DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfodyn_512",param1);
		}catch (FabException e){
			throw new FabException(e, "SPS100", "lnsbasicinfodyn");
		}
	}



	/**
	 * 房抵贷罚息信息落表
	 * @param la
	 * @param ctx
	 * @param lnsBillStatistics
	 * @throws FabException
	 */
	public void saveFddDint(LoanAgreement la,TranCtx ctx,LnsBillStatistics lnsBillStatistics)throws FabException{
		//定义各形态账单list（用于统计每期计划已还未还金额）
		List<LnsBill> billList = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		billList.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史未结清账单结息账单
		billList.addAll(lnsBillStatistics.getHisSetIntBillList());

		//获取呆滞呆账期间新罚息复利账单list
		//宽限期罚息落表的不获取呆滞呆账期间的新罚息复利
		billList.addAll(lnsBillStatistics.getCdbillList());
		try {
			for (LnsBill bill : billList) {
				//宽限期落表只落宽限期的
				if (bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)) {
					TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(la, bill, ctx);
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					tblLnsbill.setTrandate(new java.sql.Date(format.parse(ctx.getTranDate()).getTime()));
					tblLnsbill.setSerseqno(ctx.getSerSeqNo());
					bill.setTranDate(tblLnsbill.getTrandate());
					bill.setSerSeqno(ctx.getSerSeqNo());
					if (tblLnsbill.getTxseq() == 0) {
						//优化插入账本插入数据的问题 账本数量太多
						lnsBillStatistics.setBillNo(lnsBillStatistics.getBillNo() + 1);
						tblLnsbill.setTxseq(lnsBillStatistics.getBillNo());
					}

					if (bill.getBillAmt().isPositive()) {
						pepareDerCoding(bill, tblLnsbill);
						try {
							Map<String, Object> maxDint = DbAccessUtil.queryForMap("Lnsbill.queryMaxDint", tblLnsbill);
							//增加本金或利息的落表方式 若没落表 则不登记罚息
							if (tblLnsbill.getDerserseqno() == 0) {
								break;
							}
							if (maxDint == null) {
								DbAccessUtil.execute("Lnsbill.insert", tblLnsbill);
							} else {
								maxDint.put("curenddate", tblLnsbill.getCurenddate());
								maxDint.put("billamt", tblLnsbill.getBillamt());
								maxDint.put("billbal", tblLnsbill.getBillbal());
								maxDint.put("repayedate", tblLnsbill.getRepayedate());
								maxDint.put("intedate", tblLnsbill.getIntedate());
								maxDint.put("enddate", tblLnsbill.getEnddate());
								maxDint.put("dertrandate", tblLnsbill.getDertrandate());
								maxDint.put("derserseqno", tblLnsbill.getDerserseqno());
								maxDint.put("dertxseq", tblLnsbill.getDertxseq());
								DbAccessUtil.execute("Lnsbill.updateMaxDint", maxDint);
							}
						} catch (FabException e) {
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
		}catch (Exception e){
			//日期转换错误
			throw new FabException(e, "999999", "datetime");
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
	 * @return the lnssatistics
	 */
	public LnsBillStatistics getLnssatistics() {
		return lnssatistics;
	}

	/**
	 * @param lnssatistics the lnssatistics to set
	 */
	public void setLnssatistics(LnsBillStatistics lnssatistics) {
		this.lnssatistics = lnssatistics;
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



	/**
	 * @return the repayAcctNo
	 */
	public String getRepayAcctNo() {
		return repayAcctNo;
	}



	/**
	 * @param repayAcctNo the repayAcctNo to set
	 */
	public void setRepayAcctNo(String repayAcctNo) {
		this.repayAcctNo = repayAcctNo;
	}



	/**
	 * @return the capTotalNint
	 */
	public FabAmount getCapTotalNint() {
		return capTotalNint;
	}



	/**
	 * @param capTotalNint the capTotalNint to set
	 */
	public void setCapTotalNint(FabAmount capTotalNint) {
		this.capTotalNint = capTotalNint;
	}



	/**
	 * @return the la
	 */
	public LoanAgreement getLa() {
		return la;
	}



	/**
	 * @param la the la to set
	 */
	public void setLa(LoanAgreement la) {
		this.la = la;
	}



	/**
	 * @return the capFlag
	 */
	public Boolean getCapFlag() {
		return capFlag;
	}



	/**
	 * @param capFlag the capFlag to set
	 */
	public void setCapFlag(Boolean capFlag) {
		this.capFlag = capFlag;
	}



	/**
	 * @return the sub
	 */
	public AccountOperator getSub() {
		return sub;
	}



	/**
	 * @param sub the sub to set
	 */
	public void setSub(AccountOperator sub) {
		this.sub = sub;
	}



	/**
	 * @return the add
	 */
	public AccountOperator getAdd() {
		return add;
	}



	/**
	 * @param add the add to set
	 */
	public void setAdd(AccountOperator add) {
		this.add = add;
	}


	/**
	 * Gets the value of txnseq.
	 *
	 * @return the value of txnseq
	 */
	public int getTxnseq() {
		return txnseq;
	}

	/**
	 * Sets the txnseq.
	 *
	 * @param txnseq txnseq
	 */
	public void setTxnseq(int txnseq) {
		this.txnseq = txnseq;

	}
}

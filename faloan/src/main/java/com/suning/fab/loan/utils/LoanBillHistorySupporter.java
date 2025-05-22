package com.suning.fab.loan.utils;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.LnsGraceIntInfo;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.loan.domain.TblLnsfeeinfo;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.VarChecker;

public class LoanBillHistorySupporter {
	private LoanBillHistorySupporter() {
		// nothing to do
	}

	/**
	 * 查询历史账单
	 * 
	 * @param loanAgreement
	 * @param repayDate
	 * @return 账单明细
	 * @throws Exception
	 */
	public static void getLoanHisBillDetail(LoanAgreement loanAgreement,
			LnsBillStatistics billInfoAll, String repayDate, TranCtx ctx)
			throws FabException {
		//房抵贷封顶利率
		LoanTrailCalculationProvider.capRate(loanAgreement,ctx);
		String brc = ctx.getBrc();
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("acctno", loanAgreement.getContract().getReceiptNo());
		param.put("brc", brc);
		List<TblLnsbill> tblLnsBills = DbAccessUtil.queryForList(
				"CUSTOMIZE.query_hisbills", param, TblLnsbill.class);

		loanHistoryBill(loanAgreement, billInfoAll, repayDate, ctx, tblLnsBills);

	}

	public static void loanHistoryBill(LoanAgreement loanAgreement, LnsBillStatistics billInfoAll, String repayDate, TranCtx ctx, List<TblLnsbill> tblLnsBills) throws FabException {
		Map<String, LnsGraceIntInfo> graceIntList = new HashMap<String, LnsGraceIntInfo>();

		//房抵贷老数据需要剩余本金计算罚息
		if( null != loanAgreement.getContract().getFlag1() && !loanAgreement.getContract().getFlag1().contains("C") 
				&& VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate() ) && "2412615".equals(loanAgreement.getPrdId()))
		{
			 Map<String, Object> param =  new HashMap<>();
	        //查询总的未还本金
	        param.put("acctno", loanAgreement.getContract().getReceiptNo());
	        param.put("billtype", "PRIN");
	        param.put("brc", ctx.getBrc());
	        Double sumprin ;
	        try{
	            sumprin = DbAccessUtil.queryForObject("Lnsbill.query_billBalSum", param,Double.class);
	        }catch (FabSqlException e){
	            throw new FabException(e, "SPS103", "query_billBalSum");
	        }
	        if(sumprin!=null)
	            sumprin = loanAgreement.getContract().getBalance().add(sumprin).getVal();
	        else
	            sumprin = loanAgreement.getContract().getBalance().getVal();
	        
	        loanAgreement.getBasicExtension().setSumPrin(new FabAmount(sumprin));
		}
		
		
		String keyStr;
		//实际还款日操作,过滤掉在实际还款日之后结息的账本
		Map<String, String> removebills = filterBillRepayDate(loanAgreement, repayDate, ctx, tblLnsBills);
		for (TblLnsbill tbBill : tblLnsBills) {
			//更新上次结息日
			updateIntedate(removebills, tbBill);

			LnsBill lnsBill = BillTransformHelper.convertToLnsBill(tbBill);
			//为费用账本拉取费用信息
			matchFeeInfo(loanAgreement, lnsBill);
			// 添加到历史账单明细中
			addHisBill(billInfoAll, lnsBill);

			
			
			
			
			
			// 为补宽限期罚息做准备
			if (ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(lnsBill
					.getBillType())) {

				keyStr = genPrinBillKeyStr(lnsBill.getTranDate(),
						lnsBill.getSerSeqno(), lnsBill.getTxSeq());

				LnsGraceIntInfo lnsgraceintinfo = graceIntList.get(keyStr);

				if (lnsgraceintinfo == null) {
					lnsgraceintinfo = new LnsGraceIntInfo();
					lnsgraceintinfo.getPrinBill().add(lnsBill);
				} else {
					lnsgraceintinfo.getPrinBill().add(lnsBill);
				}

				graceIntList.put(keyStr, lnsgraceintinfo);
			} else if (ConstantDeclare.BILLTYPE.BILLTYPE_GINT.equals(lnsBill
					.getBillType())) {

				keyStr = genPrinBillKeyStr(lnsBill.getDerTranDate(),
						lnsBill.getDerSerseqno(), lnsBill.getDerTxSeq());

				LnsGraceIntInfo lnsgraceintinfo = graceIntList.get(keyStr);

				if (lnsgraceintinfo == null) {
					lnsgraceintinfo = new LnsGraceIntInfo();
					lnsgraceintinfo.getGraceIntBill().add(lnsBill);
				} else {
					lnsgraceintinfo.getGraceIntBill().add(lnsBill);
				}

				graceIntList.put(keyStr, lnsgraceintinfo);
			}else
			// 利息账单，计提金额等于利息账单金额
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
					.contains(lnsBill.getBillType())) {
				lnsBill.setRepayDateInt(lnsBill.getBillAmt());
			}

			//算违费用的约金
			else if(LoanFeeUtils.isFeeType(lnsBill.getBillType())){
				lnsBill.setRepayDateInt(lnsBill.getBillAmt());

				if( ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag())) {
					List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
							.hisFeeBillSettlePenalty(loanAgreement,
									lnsBill, repayDate);
					for (LnsBill cfIntBill : lnsBillList) {
						cfIntBill.setHisBill(lnsBill);
						addNoSetIntBill(billInfoAll, cfIntBill);
					}
				}
			}

//			VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())
//			//房抵贷的罚息 只算第一期-----20200121
//			&& !"2412615".equals(loanAgreement.getPrdId()) 
			//判断封顶计息标志 不是封顶计息正常走-----20190114
			//去除房抵贷数据
			if( null != loanAgreement.getContract().getFlag1() && !VarChecker.asList("2412615","4010002").contains(loanAgreement.getPrdId()) &&
					VarChecker.isEmpty(loanAgreement.getInterestAgreement().getCapRate())){
				// 本金结息
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
						.contains(lnsBill.getBillType())
						&& ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag())
	//					&& CalendarUtil.before(lnsBill.getIntendDate(), repayDate)     2018-02-25  改条件转移到宽限期和罚息Supporter中
						) {
					List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
							.hisPrinBillSettleInterest(loanAgreement, billInfoAll,
									lnsBill, repayDate);
					for (LnsBill cfIntBill : lnsBillList) {
						cfIntBill.setHisBill(lnsBill);

						addNoSetIntBill(billInfoAll, cfIntBill);
					}

				}else
				// 利息结息
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT)
						.contains(lnsBill.getBillType())
						&& ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING.equals(lnsBill.getSettleFlag())
						&& CalendarUtil.before(lnsBill.getIntendDate(), repayDate)) {
					List<LnsBill> lnsBillList = LoanBillSettleInterestSupporter
							.hisIntBillSettleInterest(loanAgreement, billInfoAll,
									lnsBill, repayDate);
					for (LnsBill cfIntBill : lnsBillList) {
						cfIntBill.setHisBill(lnsBill);

						addNoSetIntBill(billInfoAll, cfIntBill);
					}

				}
			}

		}

		// 补宽限期利息
		compensateGraceDint(loanAgreement, billInfoAll, graceIntList, repayDate);
	}

	private static void matchFeeInfo(LoanAgreement loanAgreement, LnsBill lnsBill) {
		if(LoanFeeUtils.isFeeType(lnsBill.getBillType()))
		{
			for(TblLnsfeeinfo lnsfeeinfo:loanAgreement.getFeeAgreement().getLnsfeeinfos()){
				if(lnsBill.getBillType().equals(lnsfeeinfo.getFeetype())
						&&lnsBill.getRepayWay().equals(lnsfeeinfo.getRepayway())) {
					lnsBill.setLnsfeeinfo(lnsfeeinfo.cloneFeeInfo());
					break;
				}
			}
		}
	}

	/**
	 * 	实际还款日操作,过滤掉在实际还款日之后结息的账本
	 */
	private static Map<String, String> filterBillRepayDate(LoanAgreement loanAgreement, String repayDate, TranCtx ctx, List<TblLnsbill> tblLnsBills) throws FabException {
		Map<String, String> removebills = new HashMap<>();
		//只有真实还款日期 才走这一步
		if(!CalendarUtil.before(repayDate,ctx.getTranDate() )) {
			return removebills;
		}
		String lastNintDate = loanAgreement.getContract().getRepayIntDate();
		String lastPrinDate = loanAgreement.getContract().getRepayPrinDate();
		int prinPeriodSub = 0;
		int intPeriodSub = 0;

		//不参与过滤的账本类型
		/*
		   兼容  费用和本息罚同时实际还款日还款
		 */
		List<String> ignoreTypes = new ArrayList<>();
		//实际还款日操作,过滤掉在实际还款日之后结息的账本
		for (int i= 0 ; i<tblLnsBills.size();i++) {
			//在实际还款日之后结息的账本过滤掉
			if (CalendarUtil.after(tblLnsBills.get(i).getEnddate(), repayDate)) {

				//跟费用没什么关系
				if(LoanFeeUtils.isFeeType(tblLnsBills.get(i).getBilltype())) {

					TblLnsfeeinfo lnsfeeinfo = LoanFeeUtils.matchFeeInfo(loanAgreement, tblLnsBills.get(i).getBilltype(),tblLnsBills.get(i).getRepayway() );
					//费用需要是按天计费的
					if(ConstantDeclare.CALCULATRULE.BYDAY.equals(lnsfeeinfo.getCalculatrule())){
						//费用期数
						lnsfeeinfo.setFeeperiod(lnsfeeinfo.getFeeperiod()-1);
						//上次结费日
						lnsfeeinfo.setLastfeedate(CalendarUtil.after(lnsfeeinfo.getLastfeedate(), tblLnsBills.get(i).getBegindate())
								?tblLnsBills.get(i).getBegindate():lnsfeeinfo.getLastfeedate());
					}else{
						continue;
					}

				}
				//忽略这些账本
				if(ignoreTypes.contains(tblLnsBills.get(i).getBilltype())){
					continue;
				}

				//在实际还款日后 落表了罚息账本（还款才落罚息账本，报错）
				if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,ConstantDeclare.BILLTYPE.BILLTYPE_CINT).contains(tblLnsBills.get(i).getBilltype())
						//
						||LoanFeeUtils.isPenalty(tblLnsBills.get(i).getBilltype())
						//账本金额不等于账本余额
						|| !new FabAmount(tblLnsBills.get(i).getBillamt()).sub(tblLnsBills.get(i).getBillbal()).isZero()){

					//实际还款日有费用
					if(ignoreTypes.isEmpty()
							&&!loanAgreement.getFeeAgreement().isEmpty()){
						if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
								ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
								ConstantDeclare.BILLTYPE.BILLTYPE_NINT,
								ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
								.contains(tblLnsBills.get(i).getBilltype()))
							ignoreTypes.addAll(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_DINT,
									ConstantDeclare.BILLTYPE.BILLTYPE_CINT,
									ConstantDeclare.BILLTYPE.BILLTYPE_NINT,
									ConstantDeclare.BILLTYPE.BILLTYPE_PRIN));
						else if(LoanFeeUtils.isPenalty(tblLnsBills.get(i).getBilltype())
								||LoanFeeUtils.isFeeType(tblLnsBills.get(i).getBilltype())) {
							//保费
							for (TblLnsfeeinfo lnsfeeinfo : loanAgreement.getFeeAgreement().getLnsfeeinfos()) {
								ignoreTypes.add(lnsfeeinfo.getFeetype());
							}
							//违约金
							ignoreTypes.add(ConstantDeclare.BILLTYPE.BILLTYPE_DBWY);
						}
						continue;
					}


					throw new FabException("LNS203");
				}
				//查找在实际还款日之前的上次结本结息日
				if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(tblLnsBills.get(i).getBilltype()))
				{
					//存储下删除的期数利息信息20200207
					loanAgreement.getBasicExtension().getDropNints().put(tblLnsBills.get(i).getPeriod(),tblLnsBills.get(i).getBillamt());
					intPeriodSub++;
					lastNintDate = CalendarUtil.after(lastNintDate, tblLnsBills.get(i).getBegindate())?tblLnsBills.get(i).getBegindate():lastNintDate;
				}
				if(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN.equals(tblLnsBills.get(i).getBilltype()))
				{
					prinPeriodSub++;
					//加上合同余额
					loanAgreement.getContract().getBalance().selfAdd(tblLnsBills.get(i).getBillamt());
					//查找在实际还款日之前的上次结本日
                    lastPrinDate = CalendarUtil.after(lastPrinDate, tblLnsBills.get(i).getBegindate())?tblLnsBills.get(i).getBegindate():lastPrinDate;
				}
				//账本需要修改一下对应账本的上次结息日
				removebills.put(tblLnsBills.get(i).getDertrandate().toString()
						+"+"+tblLnsBills.get(i).getDerserseqno().toString()
						+"+"+tblLnsBills.get(i).getDertxseq().toString(), tblLnsBills.get(i).getBegindate());
				//过滤此账本
				tblLnsBills.remove(i);
				i--;//对应减一
			}
		}
		//最多支持跨期一期
		if(prinPeriodSub>1||intPeriodSub>1)
			throw new FabException("LNS201");
		loanAgreement.getContract().setCurrPrinPeriod(loanAgreement.getContract().getCurrPrinPeriod()-prinPeriodSub);
		loanAgreement.getContract().setCurrIntPeriod(loanAgreement.getContract().getCurrIntPeriod()-intPeriodSub);
		//恢复在实际还款日之前的上次结本结息日
		loanAgreement.getContract().setRepayIntDate(lastNintDate);
		loanAgreement.getContract().setRepayPrinDate(lastPrinDate);

		return removebills;
	}

	/**
	 *  更新实际还款日
	 * @param removebills
	 * @param tbBill
	 */
	private static void updateIntedate(Map<String, String> removebills, TblLnsbill tbBill) {
		if(removebills.get(tbBill.getTrandate().toString()
				+"+"+tbBill.getSerseqno().toString()
				+"+"+tbBill.getTxseq().toString())!=null)
			tbBill.setIntedate(removebills.get(tbBill.getTrandate().toString()
					+"+"+tbBill.getSerseqno().toString()
					+"+"+tbBill.getTxseq().toString()));
	}

	/**
	 * 
	 * @param date
	 * @param serSeqno
	 * @param txseq
	 * @return
	 */
	private static String genPrinBillKeyStr(Date date, Integer serSeqno,
			Integer txseq) {
		return date.toString() + serSeqno.toString() + txseq.toString();
	}

	/**
	 * 补宽限期罚息
	 * 
	 * @param loanAgreement
	 * @param billInfoAll
	 * @param graceIntList
	 */
	private static void compensateGraceDint(LoanAgreement loanAgreement,
			LnsBillStatistics billInfoAll,
			Map<String, LnsGraceIntInfo> graceIntList, String repayDate) {

		for (LnsGraceIntInfo graceIntInfo : graceIntList.values()) {
			
			calGraceOverDueInterest(loanAgreement, billInfoAll, graceIntInfo,
					repayDate);
		
		}
	}

	/**
	 * 计算宽限期罚息
	 * 
	 * @param loanAgreement
	 * @return 历史未结清结息账单明细
	 */
	private static void calGraceOverDueInterest(LoanAgreement loanAgreement,
			LnsBillStatistics billInfoAll, LnsGraceIntInfo graceIntInfo,
			String repayDate) {

		//判断是否记罚息标识，如果为false,逾期、呆滞、呆账均不记罚息
		/**
		 * InterestAgreementBeanDefinitionParser类中赋值,如果PDxxxxxxx.xml中没有,默认赋值TRUE
		 * collectDefaultInterestFlag:是否记罚息标志,包含本金是否记罚息和利息是否记复利		14050183
		 */
		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestFlag())
		{
			return;
		}
		//本金是否记罚息		如果PDxxxxxxx.xml中没有,默认赋值TRUE	14050183
		if (!loanAgreement.getInterestAgreement().getCollectDefaultInterestPrin())
		{
			return;
		}
		
		for (LnsBill lnsBill : graceIntInfo.getPrinBill()) {
			
			// 如果本金逾期，补宽限期罚息：如果利息记至日期在宽限期内且还款日期超过应还款日期则补宽限期罚息，否则不补
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN)
					.contains(lnsBill.getBillType())
					&& ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING
					.equals(lnsBill.getSettleFlag())
					&& !CalendarUtil.after(lnsBill.getIntendDate(),
							lnsBill.getRepayendDate())
					&& CalendarUtil.after(repayDate, lnsBill.getRepayendDate())) {

				if("yes".equals(GlobalScmConfUtil.getProperty("newKXQ", "no"))) {
					//汇总宽限期内已落账本本金余额，宽限期利息总金额
					FabAmount totalGintPrinAmt = new FabAmount(0.00);
					FabAmount totalGintAmt = new FabAmount(0.00);
					FabAmount interest = new FabAmount();
					FabRate rt = new FabRate(0.00);
					LnsBill cfIntBill = new LnsBill();
					for (LnsBill hisBill : graceIntInfo.getGraceIntBill()) {
						totalGintPrinAmt.selfAdd(new FabAmount(BigDecimal.valueOf(hisBill.getPrinBal().getVal())
								.multiply(BigDecimal.valueOf(CalendarUtil.actualDaysBetween(hisBill.getStartDate(), hisBill.getEndDate()))).doubleValue()));
						totalGintAmt.selfAdd(hisBill.getBillAmt());

						BigDecimal rate;
						//从贷款主文件lnsbasicinfo取值,B--新的贷款利率修改标志	14050183
						if(loanAgreement.getContract().getFlag1().contains("B")){
							rate = hisBill.getBillRate().getYearRate();
						}else{
							rate = loanAgreement.getRateAgreement().getNormalRate().getYearRate();
						}
						BigDecimal dayIntRateDiff = loanAgreement
								.getRateAgreement()
								.getOverdueRate()
								.getYearRate()
								.subtract(rate).setScale(6, BigDecimal.ROUND_HALF_UP);

						rt = new FabRate(dayIntRateDiff.doubleValue(),
								loanAgreement.getRateAgreement().getOverdueRate()
										.getDaysPerYear(), loanAgreement
								.getRateAgreement().getOverdueRate()
								.getDaysPerMonth(), "Y");

						cfIntBill.setDerTranDate(hisBill.getTranDate());// 账务日期
						cfIntBill.setDerSerseqno(hisBill.getSerSeqno());// 流水号
						cfIntBill.setDerTxSeq(hisBill.getTxSeq());// 子序号
					}

					//罚息利率计算总罚息
					BigDecimal dIntDecx = new BigDecimal(totalGintPrinAmt.getVal())
							.multiply(loanAgreement.getRateAgreement().getOverdueRate().getDayRate()).setScale(2, BigDecimal.ROUND_HALF_UP);
					//补记罚息：总罚息-宽限期已计利息
					BigDecimal intDecx = dIntDecx.subtract(BigDecimal.valueOf(totalGintAmt.getVal()));
					interest.selfAdd(intDecx.doubleValue());
					cfIntBill.setPeriod(lnsBill.getPeriod());// 期数
					cfIntBill.setStartDate(lnsBill.getEndDate());
					cfIntBill.setEndDate(lnsBill.getRepayendDate());
					cfIntBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);// 账单状态N正常G宽限期O逾期L呆滞B呆账
					cfIntBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET);// 账单属性INTSET正常结息REPAY还款
					cfIntBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);// 利息入账标志NO未入YES已入
					cfIntBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);// 账单作废标志NORMAL正常CANCEL作废
					if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
							ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
						cfIntBill.setCancelFlag("3");
					}
					cfIntBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);// 结清标志RUNNING未结CLOSE已结
					cfIntBill.setRepayendDate(cfIntBill.getEndDate());
					cfIntBill.setIntendDate(cfIntBill.getEndDate());
					cfIntBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
					cfIntBill.setStartDate(lnsBill.getEndDate());
					cfIntBill.setEndDate(lnsBill.getRepayendDate());
					cfIntBill.setRepayendDate(lnsBill.getRepayendDate());
					cfIntBill.setIntendDate(lnsBill.getRepayendDate());
					cfIntBill.setBillRate(rt);// 账单执行利率
					cfIntBill.setCcy(interest.getCurrency().getCcy());
					cfIntBill.setBillAmt(interest);// 账单金额
					cfIntBill.setBillBal(interest);// 账单余额
					cfIntBill.setPrinBal(new FabAmount(totalGintPrinAmt
							.getVal()));// 账单对应本金余额
					cfIntBill.setCurendDate(cfIntBill.getEndDate());

					if (cfIntBill.getBillAmt().isPositive()) {
						cfIntBill.setHisBill(lnsBill);
						addNoSetIntBill(billInfoAll, cfIntBill);
					}
				}
				else{
					for (LnsBill hisBill : graceIntInfo.getGraceIntBill()) {
						LnsBill cfIntBill = new LnsBill();
						cfIntBill.setPeriod(hisBill.getPeriod());// 期数
						cfIntBill.setStartDate(hisBill.getStartDate());
						cfIntBill.setEndDate(hisBill.getEndDate());
						cfIntBill.setDerTranDate(hisBill.getTranDate());// 账务日期
						cfIntBill.setDerSerseqno(hisBill.getSerSeqno());// 流水号
						cfIntBill.setDerTxSeq(hisBill.getTxSeq());// 子序号
						cfIntBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);// 账单状态N正常G宽限期O逾期L呆滞B呆账
						cfIntBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_INTSET);// 账单属性INTSET正常结息REPAY还款
						cfIntBill.setIntrecordFlag(ConstantDeclare.INTRECORDFLAG.INTRECORDFLAG_YES);// 利息入账标志NO未入YES已入
						cfIntBill.setCancelFlag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);// 账单作废标志NORMAL正常CANCEL作废
						if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
								ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
							cfIntBill.setCancelFlag("3");
						}
						cfIntBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);// 结清标志RUNNING未结CLOSE已结
						cfIntBill.setRepayendDate(cfIntBill.getEndDate());
						cfIntBill.setIntendDate(cfIntBill.getEndDate());
						FabAmount interest = new FabAmount();

						// 宽限期利息，补宽限期罚息:用罚息利息减去正常利率计算罚息
						Integer intDays = CalendarUtil.actualDaysBetween(
								hisBill.getStartDate(), hisBill.getEndDate());


						BigDecimal rate;
						//从贷款主文件lnsbasicinfo取值,B--新的贷款利率修改标志	14050183
						if(loanAgreement.getContract().getFlag1().contains("B")){
							rate = hisBill.getBillRate().getYearRate();
						}else{
							rate = loanAgreement.getRateAgreement().getNormalRate().getYearRate();
						}

						BigDecimal dayIntRateDiff = loanAgreement
								.getRateAgreement()
								.getOverdueRate()
								.getYearRate()
								.subtract(rate).setScale(6, BigDecimal.ROUND_HALF_UP);

						FabRate rt = new FabRate(dayIntRateDiff.doubleValue(),
								loanAgreement.getRateAgreement().getOverdueRate()
										.getDaysPerYear(), loanAgreement
								.getRateAgreement().getOverdueRate()
								.getDaysPerMonth(), "Y");

						BigDecimal intDec = new BigDecimal(intDays);

						intDec = intDec.multiply(
								new BigDecimal(hisBill.getPrinBal().getVal()))
								.multiply(rt.getDayRate()).setScale(2, BigDecimal.ROUND_HALF_UP);

						interest.selfAdd(intDec.doubleValue());
						cfIntBill.setBillType(ConstantDeclare.BILLTYPE.BILLTYPE_DINT);
						cfIntBill.setStartDate(hisBill.getStartDate());
						cfIntBill.setEndDate(hisBill.getEndDate());
						cfIntBill.setRepayendDate(hisBill.getEndDate());
						cfIntBill.setIntendDate(hisBill.getEndDate());
						cfIntBill.setBillRate(rt);// 账单执行利率
						cfIntBill.setCcy(interest.getCurrency().getCcy());
						cfIntBill.setBillAmt(interest);// 账单金额
						cfIntBill.setBillBal(interest);// 账单余额
						cfIntBill.setPrinBal(new FabAmount(hisBill.getPrinBal()
								.getVal()));// 账单对应本金余额
						cfIntBill.setCurendDate(cfIntBill.getEndDate());

						if (cfIntBill.getBillAmt().isPositive()) {
							cfIntBill.setHisBill(hisBill);

							addNoSetIntBill(billInfoAll, cfIntBill);

						}
					}
				}
			}
		}
	}

	/**
	 * 登记历史账单
	 * 
	 * @param lnsBillInfoAll
	 * @param lnsBill
	 * 
	 */
	private static void addHisBill(LnsBillStatistics lnsBillInfoAll,
			LnsBill lnsBill) {

		LoanBillStatisticsSupporter.billStatistics(lnsBillInfoAll, lnsBill);
		lnsBillInfoAll.getHisBillList().add(lnsBill);
	}

	/**
	 * 登记未结清结息账单
	 * 
	 * @param lnsBillInfoAll
	 * @param lnsBill
	 * 
	 */
	private static void addNoSetIntBill(LnsBillStatistics lnsBillInfoAll,
			LnsBill lnsBill) {
		// 累加账单序号
		if (lnsBill.getBillAmt().isPositive()
				|| "AMLT".equals(lnsBill.getBillType())) {
			lnsBillInfoAll.setBillNo(lnsBillInfoAll.getBillNo() + 1);
			lnsBill.setTxSeq(lnsBillInfoAll.getBillNo());
			LoanBillStatisticsSupporter.billStatistics(lnsBillInfoAll, lnsBill);
			lnsBillInfoAll.getHisSetIntBillList().add(lnsBill);
		}
	}
}

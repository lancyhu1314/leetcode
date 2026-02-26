
package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author 	
 *
 * @version V1.0.1
 *
 * 			rateType		 利率类型
 * 			realRate		利率
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")
@Repository
public class Lns605 extends WorkUnit {

	String acctNo;  //账号
	String rateType; //利率类型
	FabRate realRate; //利率
	String serialNo; //流水号
	String flag;
	@Autowired LoanEventOperateProvider eventProvider;


	@Override
	public void run() throws Exception{
		
		TranCtx ctx = getTranctx();
		
		//参数为空判断
		if (VarChecker.isEmpty(rateType) ){
			throw new FabException("LNS055","利率类型");
		}
		if (VarChecker.isEmpty(realRate)){
			throw new FabException("LNS056 ","利率");
		}
		if (VarChecker.isEmpty(acctNo) ){
			throw new FabException("LNS055","借据号");
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
		if(ctx.getTranDate().equals(lnsbasicinfo.getOpendate())){
			throw new FabException("LNS129");
		}
		if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
				ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(lnsbasicinfo.getLoanstat())){
			throw new FabException("LNS130","已核销/非应计");
		}
		if(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE.equals(lnsbasicinfo.getLoanstat())){
			throw new FabException("LNS130","已结清");
		}
//		if(ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway()) ||
//		   ConstantDeclare.REPAYWAY.REPAYWAY_QQD.equals(lnsbasicinfo.getRepayway()) 	){
		//气球贷放开
		if(ConstantDeclare.REPAYWAY.REPAYWAY_ZDY.equals(lnsbasicinfo.getRepayway()) ){
			throw new FabException("LNS164");
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
		List<LnsBill> cdlnsbill = new ArrayList<LnsBill>();
		//未结清本金和利息账单结息，用于因批量漏跑，导致未生成账单逾期情况
		cdlnsbill.addAll(lnsBillStatistics.getFutureOverDuePrinIntBillList());
		//历史未结清账单结息账单
		cdlnsbill.addAll(lnsBillStatistics.getHisSetIntBillList());
		
		String flag2=null;
		
		//正常利率修改
		if("1".equals(rateType)){
			//当期计提税金
			FabAmount pretax = new FabAmount();
			//当期计提利息
			FabAmount prenint = new FabAmount();
			//累加已计利息
			List<FabAmount> nints = accumulatedInterest(lnsbill,prenint,pretax,la,ctx,lnsbasicinfo);
			prenint = nints.get(0);
			pretax = nints.get(1);
			
			//修改后计提当期税金
			FabAmount aftertax = new FabAmount(0.0);
			//修改后计提当期利息
			FabAmount afternint = new FabAmount(0.0);
			//修改后当期计提税金
//			FabAmount nowtax = new FabAmount();
			//修改后当期计提利息
//			FabAmount nownint = new FabAmount();
			la.getRateAgreement().setNormalRate(realRate);
			if(new FabRate(lnsbasicinfo.getNormalrate()).toString().equals(realRate.toString())){
				throw new FabException("LNS103",new FabRate(lnsbasicinfo.getNormalrate()).toString(),realRate.toString());
			}
			
			//房抵贷无宽限期，支持罚息小于利息
			if ( la.getRateAgreement().getOverdueRate().getYearRate().doubleValue()<la.getRateAgreement().getNormalRate().getYearRate().doubleValue()
				&& lnsbasicinfo.getGracedays() > 0  &&
				!VarChecker.asList("2412625","2521001").contains(lnsbasicinfo.getPrdcode())	){
				throw new FabException("LNS128");
			}
			//修改主文件利率
			param.put("rateType", rateType);
			//为保持精度不变（fabrate有问题）
			param.put("rate", la.getRateAgreement().getNormalRate().getYearRate().doubleValue());
			String flag1 = "B";
			flag2 = "3";
			if(la.getRateAgreement().getNormalRate().getYearRate().doubleValue()<lnsbasicinfo.getNormalrate()){
				if("1".equals(flag)){
					throw new FabException("LNS104");
				}

			}else if(la.getRateAgreement().getNormalRate().getYearRate().doubleValue()>lnsbasicinfo.getNormalrate()){
				if("0".equals(flag)){
					throw new FabException("LNS104");
				}
				flag2 = "6";
			}
			
			if(!VarChecker.isEmpty(lnsbasicinfo.getFlag1())){
				if(lnsbasicinfo.getFlag1().contains("B")){
					flag1 = lnsbasicinfo.getFlag1();
				}else{
					flag1 = lnsbasicinfo.getFlag1()+"B";
				}
			}
			param.put("flag1", flag1);
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_rate605", param);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfo");
			}
			
			//在宽限期内修改利率生成宽限期账本
			Integer num = 100;
			for (LnsBill bill : cdlnsbill){
		        if (bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)){
					TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(la,bill,ctx);
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					tblLnsbill.setTrandate(new java.sql.Date(format.parse(ctx.getTranDate()).getTime()));
					tblLnsbill.setSerseqno(ctx.getSerSeqNo());
					bill.setTranDate(tblLnsbill.getTrandate());
					bill.setSerSeqno(ctx.getSerSeqNo());
					if(tblLnsbill.getTxseq() == 0)
					{
						tblLnsbill.setTxseq(--num);
					}
					
					if(bill.getBillAmt().isPositive())
					{
						try{
							DbAccessUtil.execute("Lnsbill.insert",tblLnsbill);		
						}catch (FabException e){
							throw new FabException(e, "SPS100", "lnsbill");
						}
						
						param.clear();
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
		
			//查询还款计划辅助表判断当前期处于哪一期
			Map<String,Object> param2 = new HashMap<String,Object>();
			param2.put("acctno", acctNo);
			param2.put("brc", ctx.getBrc());
			List<TblLnsrpyplan> rpyplanlist = null;

			try {
				//取还款计划登记簿数据
				rpyplanlist = DbAccessUtil.queryForList("CUSTOMIZE.query_lnsrpyplan", param2, TblLnsrpyplan.class);
			}
			catch (FabSqlException e)
			{
				throw new FabException(e, "SPS103", "lnsrpyplan");
			}

			if (null == rpyplanlist){
				rpyplanlist = new ArrayList<TblLnsrpyplan>();
			}
            //处理还款计划辅助表
            for(TblLnsrpyplan lnsrpyplan:rpyplanlist){

                //去除未来其的 还款辅助表
                if(CalendarUtil.beforeAlsoEqual(tranctx.getTranDate(),lnsrpyplan.getRepayintbdate().trim())){
                    Map<String,Object> updateRpyPlan = new HashMap<>();
                    updateRpyPlan.put("acctno",lnsrpyplan.getAcctno());
                    updateRpyPlan.put("brc",lnsrpyplan.getBrc());
                    updateRpyPlan.put("repayterm",lnsrpyplan.getRepayterm());
                    LoanDbUtil.delete("Lnsrpyplan.deleteByUk",updateRpyPlan);
                }

            }
			Map<Integer, TblLnsrpyplan> rpyplanMap = new HashMap<Integer, TblLnsrpyplan>();
			for(TblLnsrpyplan rpyplan: rpyplanlist){
				rpyplanMap.put(rpyplan.getRepayterm(), rpyplan);
				
				//0利率判断修改还款计划表 2019-01-10
				if(realRate.getVal().compareTo(BigDecimal.ZERO) == 0){
					if((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), rpyplan.getRepayintbdate())
							&& CalendarUtil.before(ctx.getTranDate(), rpyplan.getRepayintedate()) 
							&& "false".equals(la.getInterestAgreement().getIsCalTail())) 
							|| 
							(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), rpyplan.getRepayintbdate())
							&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), rpyplan.getRepayintedate()) 
							&& "true".equals(la.getInterestAgreement().getIsCalTail()))){
						Map<String,Object> param3 = new HashMap<String,Object>();
						param3.put("acctno", acctNo);
						param3.put("brc", ctx.getBrc());
						param3.put("termretint", 0.00);
						param3.put("repayterm",rpyplan.getRepayterm());
						
						try {
							//还款计划
							DbAccessUtil.execute(
									"CUSTOMIZE.update_lnsrpyplan_605", param3);
							
						} catch (FabSqlException e) {
							throw new FabException(e, "SPS103", "update_lnsrpyplan_605");
						}
					}
				}
			}
			
			LnsBillStatistics nBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
			List<LnsBill> nbill = new ArrayList<LnsBill>();
			//历史期账单（账单表）
			nbill.addAll(nBillStatistics.getHisBillList());
			nbill.addAll(nBillStatistics.getHisSetIntBillList());
			//当前期和未来期
			nbill.addAll(nBillStatistics.getBillInfoList());
			nbill.addAll(nBillStatistics.getFutureBillInfoList());
			nbill.addAll(nBillStatistics.getFutureOverDuePrinIntBillList());
			Map<Integer, FabAmount> nintNum = new HashMap<Integer, FabAmount>();
			//修改利率后的同期利息相加
			for(LnsBill bill:nbill){
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
					if(nintNum.containsKey(bill.getPeriod())){
						nintNum.put(bill.getPeriod(), new FabAmount(nintNum.get(bill.getPeriod()).selfAdd(bill.getBillAmt()).getVal()));
					}else{
						nintNum.put(bill.getPeriod(), bill.getBillAmt());
					}
				}
			}
			for (LnsBill bill:nbill){
				if("1".equals(lnsbasicinfo.getRepayway()) ||
				   "9".equals(lnsbasicinfo.getRepayway())){
					if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(bill.getBillType())){
						if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
								&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
								|| 
								(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
								&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
							param.clear();
							param.put("acctno", acctNo);
							param.put("brc", ctx.getBrc());
							param.put("repayterm", bill.getPeriod());
							param.put("termretprin", bill.getBillAmt().getVal());
							FabAmount prinbalance = new FabAmount(lnsbasicinfo.getContractbal().doubleValue());
							prinbalance.selfSub(bill.getBillAmt());
							param.put("balance", prinbalance.getVal());
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
				//bianli出当前期利息账单进行处理
				if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
					if ((CalendarUtil.afterAlsoEqual(ctx.getTranDate(), rpyplanMap.get(bill.getPeriod())==null?bill.getStartDate():rpyplanMap.get(bill.getPeriod()).getRepayintbdate())
							&& CalendarUtil.before(ctx.getTranDate(), rpyplanMap.get(bill.getPeriod())==null?bill.getEndDate():rpyplanMap.get(bill.getPeriod()).getRepayintedate()) 
							&& "false".equals(la.getInterestAgreement().getIsCalTail())) 
							|| 
							(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), rpyplanMap.get(bill.getPeriod())==null?bill.getStartDate():rpyplanMap.get(bill.getPeriod()).getRepayintbdate())
							&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), rpyplanMap.get(bill.getPeriod())==null?bill.getEndDate():rpyplanMap.get(bill.getPeriod()).getRepayintedate()) 
							&& "true".equals(la.getInterestAgreement().getIsCalTail()))){
						//等本等息利息计算
						if (ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
							//按天计算利息
							BigDecimal bigInt = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(bill.getStartDate(), ctx.getTranDate())) 
											.multiply(BigDecimal.valueOf(lnsbasicinfo.getContractamt()) 
											.multiply(realRate.getDayRate()))
											.setScale(2,BigDecimal.ROUND_HALF_UP);
							//按天计算的利息大于账单金额取账单金额
							if (new FabAmount(bigInt.doubleValue()).sub(bill.getBillAmt()).isPositive()){
								afternint.selfAdd(bill.getBillAmt());
								aftertax.selfAdd(TaxUtil.calcVAT(bill.getBillAmt()));
							}
							else {
								afternint.selfAdd(bigInt.doubleValue());
								aftertax.selfAdd(TaxUtil.calcVAT(new FabAmount(bigInt.doubleValue())));
							}
						}
						//非等本等息
						else {
							if(!VarChecker.isEmpty(bill.getRepayDateInt()) 
									&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate())){
								afternint.selfAdd(bill.getRepayDateInt());
								aftertax.selfAdd(TaxUtil.calcVAT(bill.getRepayDateInt()));
							}
						}
						if(bill.getBillAmt().equals(bill.getBillBal()) || realRate.getVal().compareTo(BigDecimal.ZERO) == 0){
							param.clear();
							param.put("acctno", acctNo);
							param.put("brc", ctx.getBrc());
							param.put("termretint", bill.getBillBal().getVal());
							if(!nintNum.get(bill.getPeriod()).equals(bill.getBillBal()) 
									&& !ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
								param.put("termretint", nintNum.get(bill.getPeriod()).getVal());
							}
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
			
			//判断当期计提利息是否小于之前计提利息 write by huyi----20190111
//			if(afternint.getVal()<prenint.getVal()){
//				
//				//此处可以拿到借据号，难道借据号后查询利息计提摊销登记簿，看看有没有记录，有这说明利息计提过，没有则无需操作，直接return
//				TblLnsprovisionreg lnsprovisionreg; 
//				param.clear();
//				param.put("acctno", acctNo);
//				param.put("brc", ctx.getBrc());
//				try {
//					lnsprovisionreg = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsprovision_provision", param, 
//							TblLnsprovisionreg.class);
//				} catch (FabSqlException e) {
//					throw new FabException(e, "SPS103", "lnsprovision");
//				}
//				
//				if (null != lnsprovisionreg) {//没有利息计提数据，则不用处理该子交易，直接返回即可
//					//写利息计提摊销登记簿(lnsprovisionreg)
//					TblLnsprovisionreg lnsprovisionregOff = new TblLnsprovisionreg();
//					lnsprovisionregOff.setTrandate(java.sql.Date.valueOf(ctx.getTranDate()));
//					lnsprovisionregOff.setSerseqno(ctx.getSerSeqNo());
//					lnsprovisionregOff.setTxnseq(1);
//					lnsprovisionregOff.setBrc(ctx.getBrc());
//					lnsprovisionregOff.setTeller(ctx.getTeller());
//					lnsprovisionregOff.setReceiptno(lnsprovisionreg.getReceiptno());
//					lnsprovisionregOff.setPeriod(lnsprovisionreg.getPeriod() + 1);//期数
//					lnsprovisionregOff.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
//					lnsprovisionregOff.setCcy(lnsprovisionreg.getCcy());
//					lnsprovisionregOff.setTotalinterest(new FabAmount(lnsprovisionreg.getTotalinterest()).sub(prenint).add(afternint).getVal());
//					lnsprovisionregOff.setTotaltax(new FabAmount(lnsprovisionreg.getTotaltax()).sub(pretax).add(aftertax).getVal());
//					lnsprovisionregOff.setTaxrate(new BigDecimal(PropertyUtil.getPropertyOrDefault("tax.VATRATE", "0.06")).doubleValue());
//					lnsprovisionregOff.setInterest(prenint.sub(afternint).getVal());	//本次利息计提金额为 修改前未结息已计提金额-修改后未结息应计提金额
//					lnsprovisionregOff.setTax(pretax.sub(aftertax).getVal());	//本次计提税金为已计提税金总额，冲销掉原来的发生额
//					lnsprovisionregOff.setIntertype(ConstantDeclare.INTERTYPE.PROVISION);
//					lnsprovisionregOff.setBegindate(lnsprovisionreg.getEnddate());
//					lnsprovisionregOff.setEnddate(java.sql.Date.valueOf(ctx.getTranDate()));
//					lnsprovisionregOff.setInterflag(ConstantDeclare.INTERFLAG.NEGATIVE);//反向
//					lnsprovisionregOff.setSendflag(ConstantDeclare.SENDFLAG.PENDIND);
//					lnsprovisionregOff.setSendnum(0);
//					lnsprovisionregOff.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
//
//					try {
//						DbAccessUtil.execute("Lnsprovisionreg.insert", lnsprovisionregOff);
//					} catch (FabSqlException e) {
//						throw new FabException(e, "SPS100", "lnsprovisionreg");
//					}
//					
//					
//					//冲销金额为prenint-afternint
//					LnsAcctInfo acctinfo = new LnsAcctInfo(acctNo, ConstantDeclare.BILLTYPE.BILLTYPE_PRIN, 
//							ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
//					//计算出冲销金额
//					List<FabAmount> amtList = new ArrayList<FabAmount>();
//					amtList.add(new FabAmount(pretax.sub(aftertax).getVal()));
//					
//					//写利息计提冲销事件
//					eventProvider.createEvent(ConstantDeclare.EVENT.WRITOFFINT, new FabAmount(prenint.sub(afternint).getVal()), 
//							acctinfo, null, la.getFundInvest(), ConstantDeclare.BRIEFCODE.LXCX, ctx, amtList, la.getCustomer().getMerchantNo(), la.getBasicExtension().getDebtCompany());
//
//				}
//				
//				
//			}
			//修改还款计划当期应还利息
		}
		//逾期利率修改
		if("2".equals(rateType) || "3".equals(rateType)){
			//根据账号生成协议
			LoanAgreement laAfter = LoanAgreementProvider.genLoanAgreementFromDB(acctNo, ctx);
			String flag1 = "B";
			//修改利率
			if("2".equals(rateType)){
				flag2 = "4";
				laAfter.getRateAgreement().setOverdueRate(realRate);
				if(la.getRateAgreement().getOverdueRate().toString().equals(laAfter.getRateAgreement().getOverdueRate().toString())){
					throw new FabException("LNS103",la.getRateAgreement().getOverdueRate().toString(),laAfter.getRateAgreement().getOverdueRate().toString());
				}
				//房抵贷无宽限期，支持罚息小于利息
				if ( la.getRateAgreement().getOverdueRate().getYearRate().doubleValue()<la.getRateAgreement().getNormalRate().getYearRate().doubleValue()
					&& lnsbasicinfo.getGracedays() > 0 &&
					!VarChecker.asList("2412625","2521001").contains(lnsbasicinfo.getPrdcode() ) ){
					throw new FabException("LNS128");
				}
				if(laAfter.getRateAgreement().getOverdueRate().getYearRate().doubleValue()<lnsbasicinfo.getOverduerate()){
					if("1".equals(flag)){
						throw new FabException("LNS104");
					}

				}else if(laAfter.getRateAgreement().getOverdueRate().getYearRate().doubleValue()>lnsbasicinfo.getOverduerate()){
					if("0".equals(flag)){
						throw new FabException("LNS104");
					}
					flag2 = "7";
				}
				param.put("rate", laAfter.getRateAgreement().getOverdueRate().getYearRate().doubleValue());
				//罚息利率修改存主文件拓展表 存老数据
				//主文件拓展表
				Map<String,Object> updateMap = new HashMap<>();
				updateMap.put("key", ConstantDeclare.KEYNAME.OPIR);//登记老利率
				updateMap.put("acctno", acctNo);
				updateMap.put("openbrc", tranctx.getBrc());

				TblLnsbasicinfoex lnsbasicinfoex = LoanDbUtil.queryForObject("Lnsbasicinfoex.selectByUk", updateMap, TblLnsbasicinfoex.class);
				if(lnsbasicinfoex == null){
					lnsbasicinfoex = new TblLnsbasicinfoex();
					lnsbasicinfoex.setAcctno(acctNo);
					lnsbasicinfoex.setOpenbrc(tranctx.getBrc());
					lnsbasicinfoex.setKey(ConstantDeclare.KEYNAME.OPIR);
					Map<String,Object> tunneldata = new HashMap<>();
					Map<String,String> lastOverRate = new HashMap<>();
					lastOverRate.put(tranctx.getTranDate(), la.getRateAgreement().getOverdueRate().toString());
					tunneldata.put(ConstantDeclare.KEYNAME.OPIR,lastOverRate);
					lnsbasicinfoex.setTunneldata( JsonTransfer.ToJson(tunneldata));
					LoanDbUtil.insert("Lnsbasicinfoex.insert", lnsbasicinfoex);
				}else{
					JSONObject tunneldata = JSONObject.parseObject(lnsbasicinfoex.getTunneldata());
					//当天重复修改罚息利率 不更改
					if(!tunneldata.getJSONObject(ConstantDeclare.KEYNAME.OPIR).containsKey(tranctx.getTranDate())){
						tunneldata.getJSONObject(ConstantDeclare.KEYNAME.OPIR).put(tranctx.getTranDate(), la.getRateAgreement().getOverdueRate().toString());
						updateMap.put("tunneldata", tunneldata.toJSONString());
						LoanDbUtil.update("Lnsbasicinfoex.updateTunneldata", updateMap);
					}
				}



			}else{
				flag2 = "5";
				laAfter.getRateAgreement().setCompoundRate(realRate);
				if(la.getRateAgreement().getCompoundRate().toString().equals(laAfter.getRateAgreement().getCompoundRate().toString())){
					throw new FabException("LNS103",la.getRateAgreement().getCompoundRate().toString(),laAfter.getRateAgreement().getCompoundRate().toString());

				}
				if(laAfter.getRateAgreement().getCompoundRate().getYearRate().doubleValue()<lnsbasicinfo.getOverduerate1()){
					if("1".equals(flag)){
						throw new FabException("LNS104");
					}

				}else if(laAfter.getRateAgreement().getCompoundRate().getYearRate().doubleValue()>lnsbasicinfo.getOverduerate1()){
					if("0".equals(flag)){
						throw new FabException("LNS104");
					}
					flag2 = "9";
				}
				param.put("rate", laAfter.getRateAgreement().getCompoundRate().getYearRate().doubleValue());
			}
			
			if(!VarChecker.isEmpty(lnsbasicinfo.getFlag1())){
				if(lnsbasicinfo.getFlag1().contains("B")){
					flag1 = lnsbasicinfo.getFlag1();
				}else{
					flag1 = lnsbasicinfo.getFlag1()+"B";
				}
			}
			
			param.put("rateType", rateType);
			param.put("flag1", flag1);
			try {
				DbAccessUtil.execute("CUSTOMIZE.update_lnsbasicinfo_rate605", param);
			} catch (FabSqlException e) {
				throw new FabException(e, "SPS102", "lnsbasicinfo");
			}

			for (LnsBill bill : cdlnsbill){
		        if (bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_GINT)||
		            bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_CINT)||
		            bill.getBillType().equals(ConstantDeclare.BILLTYPE.BILLTYPE_DINT)){
					TblLnsbill tblLnsbill = BillTransformHelper.convertToTblLnsBill(la,bill,ctx);
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					tblLnsbill.setTrandate(new java.sql.Date(format.parse(ctx.getTranDate()).getTime()));
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

						param.clear();
						param.put("trandate", bill.getDerTranDate());
						param.put("serseqno", bill.getDerSerseqno());
						param.put("txseq", bill.getDerTxSeq());
						param.put("intedate1", bill.getEndDate());

						try {
							DbAccessUtil.execute("CUSTOMIZE.update_hisbill_513", param);
						} catch (FabException e) {
							throw new FabException(e, "SPS102", "lnsbills");
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
			throw new FabException(e, "SPS102", "lnsbasicinfo");
		}
	
		//幂等
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTranDate());
		lnsinterface.setSerialno(serialNo);
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode("470016");
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setAcctname(lnsbasicinfo.getName());
		lnsinterface.setUserno(lnsbasicinfo.getCustomid());
		lnsinterface.setAcctno(acctNo);
		lnsinterface.setTranamt(0.0);
		lnsinterface.setMagacct(acctNo);
		lnsinterface.setReserv1(flag2);
		if("1".equals(rateType)){
			lnsinterface.setReserv3(new FabRate(lnsbasicinfo.getNormalrate()).toString());
		}
		else if("2".equals(rateType)){
			lnsinterface.setReserv3(new FabRate(lnsbasicinfo.getOverduerate()).toString());
		}
		else if("3".equals(rateType)){
			lnsinterface.setReserv3(new FabRate(lnsbasicinfo.getOverduerate1()).toString());
		}
		lnsinterface.setReserv4(realRate.toString());
			
		
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

	public List<FabAmount> accumulatedInterest(List<LnsBill> lnsbill, FabAmount totalnint, FabAmount totaltax, LoanAgreement la, TranCtx ctx, TblLnsbasicinfo lnsbasicinfo){
//		Integer repayTerm = 0;
		for (LnsBill bill:lnsbill){
			if (VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(bill.getBillType())){
				if ((CalendarUtil.after(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()) && "false".equals(la.getInterestAgreement().getIsCalTail())) 
						|| 
						(CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate())
						&& CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) && "true".equals(la.getInterestAgreement().getIsCalTail()))){
					//等本等息利息计算
					if (ConstantDeclare.REPAYWAY.isEqualInterest(lnsbasicinfo.getRepayway())){
						//按天计算利息
						BigDecimal bigInt = BigDecimal.valueOf(CalendarUtil.actualDaysBetween(bill.getStartDate(), ctx.getTranDate())) 
										.multiply(BigDecimal.valueOf(lnsbasicinfo.getContractamt()) 
										.multiply(new FabRate(lnsbasicinfo.getNormalrate()).getDayRate()))
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
					//非等本等息
					else {
						totalnint.selfAdd(bill.getRepayDateInt());
						totaltax.selfAdd(TaxUtil.calcVAT(bill.getRepayDateInt()));
					}
//					repayTerm = bill.getPeriod();
				}
			}
		}
		List<FabAmount> nints = new ArrayList<FabAmount>();
		nints.add(totalnint);
		nints.add(totaltax);
//		nints.add(new FabAmount(Double.valueOf(repayTerm)));
		return nints;
	}
	

	private boolean isCurrentPeriod(LoanAgreement la, LnsBill bill,TranCtx ctx) {
		
		if( "false".equals(la.getInterestAgreement().getIsCalTail())){
			if (CalendarUtil.after(ctx.getTranDate(), bill.getStartDate()) && 
					  CalendarUtil.before(ctx.getTranDate(), bill.getEndDate()))
				return true;
		}else{
			if (CalendarUtil.afterAlsoEqual(ctx.getTranDate(), bill.getStartDate()) && 
				     CalendarUtil.beforeAlsoEqual(ctx.getTranDate(), bill.getEndDate()) )
				return true;			
		}
		 return false;
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
	 *
	 * @return rateType 利率类型
	 */
	public String getRateType() {
		return rateType;
	}

	/**
	 *
	 * @param rateType 利率类型
	 */
	public void setRateType(String rateType) {
		this.rateType = rateType;
	}


	/**
	 *
	 * @return serialNo 流水号
	 */
	public String getSerialNo() {
		return serialNo;
	}

	/**
	 *
	 * @param serialNo 流水号
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	/**
	 *
	 * @return realRate 利率
	 */
	public FabRate getRealRate() {
		return realRate;
	}

	/**
	 *
	 * @param realRate 利率
	 */
	public void setRealRate(FabRate realRate) {
		this.realRate = realRate;
	}

	/**
	 *
	 * @return flag fag
	 */
	public String getFlag() {
		return flag;
	}

	/**
	 *
	 * @param flag 标志
	 */
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

}

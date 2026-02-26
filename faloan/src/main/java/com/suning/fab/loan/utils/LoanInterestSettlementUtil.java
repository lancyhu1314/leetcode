package com.suning.fab.loan.utils;

import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.bo.LnsBasicInfo;
import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.bo.LnsBillStatistics;
import com.suning.fab.loan.bo.RepayAdvance;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.supporter.LoanSupporterUtil;
import com.suning.fab.loan.supporter.PrepayTermSupporter;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.GlobalScmConfUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.apache.commons.httpclient.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * 提前还款或利息减免未来期账单生成
 *
 * @author 	HY
 *
 * @version V1.1.1
 *
 * @see 	LoanInterestSettlementUtil
 *
 * @param
 *
 * @return
 *
 * @exception
 */

public class LoanInterestSettlementUtil {
	@Autowired
	@Qualifier("accountSuber")
	AccountOperator sub;
	@Autowired
	LoanEventOperateProvider eventProvider;


	/**
     * 变更还款计划查询辅助表
     * 提前还款还到本金时，调用该方法生成新一期本金利息账单，为还款计划查询提供日期和金额
     *
     * @param ctx  					公共信息
     * @param txseq     			子序号
     * @param la 			LoanAgreement
     * @param tblBill 				账单结构
     * @param lnsBillStatistics 	试算结构
     * @param billType 				账单类型
     * @return
     * @since  1.1.1
     */

	public static void interestBillPlan(TranCtx ctx,Integer txseq,LoanAgreement la,TblLnsbill tblBill,LnsBillStatistics lnsBillStatistics,String billType) throws FabException
	{
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();
		while(billListIterator.hasNext())
		{
			LnsBill lnsBill = billListIterator.next();
			//根据期数获取当期本金
			if(	new FabAmount(tblBill.getPrinbal()).isZero() ||
				(tblBill.getBegindate().equals(lnsBill.getStartDate()) &&
				lnsBill.getBillType().equals(billType)    &&
				la.getContract().getCurrIntPeriod().equals(lnsBill.getPeriod())) )
			{
				TblLnsbillplan lnsBillPlan = new TblLnsbillplan();
				lnsBillPlan.setTrandate(ctx.getTranDate());
				lnsBillPlan.setSerseqno(ctx.getSerSeqNo());
				lnsBillPlan.setTxseq(txseq);

				lnsBillPlan.setAcctno(tblBill.getAcctno());
				lnsBillPlan.setBrc(ctx.getBrc());
				lnsBillPlan.setBilltype(lnsBill.getBillType());
				lnsBillPlan.setPeriod(lnsBill.getPeriod());
				lnsBillPlan.setBillamt(lnsBill.getBillAmt().getVal());
				lnsBillPlan.setPrinbal(lnsBill.getPrinBal().getVal());
				lnsBillPlan.setBegindate(lnsBill.getStartDate());
				lnsBillPlan.setEnddate(lnsBill.getEndDate());
				lnsBillPlan.setRepayedate(lnsBill.getRepayendDate());

				try {
					DbAccessUtil.execute("Lnsbillplan.insert", lnsBillPlan);
				}
				catch (FabSqlException e)
				{
					if(!ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())){
						throw new FabException(e, "SPS100", "lnsbillplan");
					}
				}

				LoggerUtil.debug("账单类型："+ lnsBill.getBillType() + "，"
							+ "账单金额" + lnsBill.getBillAmt() + "，"
							+ "对应本金余额" + lnsBill.getPrinBal() + "，"
							+ "期数" + lnsBill.getPeriod() + "，"
							+ "账单起始日期" + lnsBill.getStartDate() +"，"
							+ "账单结束日期" + lnsBill.getEndDate());

				if( !new FabAmount(tblBill.getPrinbal()).isZero() )
					break;
			}
		}
	}

	/**
     * 功能：（P2P含服务费）非等本等息当前期或提前期（还款、利息减免）生成账单
     * 描述：利息账单生成到当前日期，剩余金额生成本金账单，如果提前还款还到本金登记还款计划表
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	txseq     				子序号
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	feeAdvance				服务费金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     * @param 	interestFlag 			利息标志（ONLYINT只还息、ONLYPRIN只还本、INTPRIN还本还息）
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
     *
     * @since  1.1.1
     */
	public static String feeRepaymentBill(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo,
			FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,List<RepayAdvance> feeAdvance,LnsBillStatistics lnsBillstatistics
			,String  interestFlag
			) throws FabException
	{

		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);


		Integer seqno = txseq;
		//本金标志（还到本金登记还款计划登记簿）
		Boolean prinFlag = false;
		//还完利息是否有钱还本金
		Boolean amtFlag = true;
		//结清标志 （1-未结清 3-结清）
		String settleFlag = "1";


		//提前还款还本金金额及流水
		RepayAdvance totalPrin = new RepayAdvance();
		//提前还款还利息金额及流水
		RepayAdvance totalInt = new RepayAdvance() ;
		//提前还款还服务费金额及流水
		RepayAdvance totalFee = new RepayAdvance() ;
		//利息账单结构
		TblLnsbill lnsIntBill = new TblLnsbill();
		//本金账单结构
		TblLnsbill lnsPrinBill = new TblLnsbill();
		//服务费账单结构
		TblLnsbill lnsFeeBill = new TblLnsbill();
		//提前还款后剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());



		//第一次还款取计息日作为账单开始日，非第一次还款取上次结息日作为账单开始日
		if( la.getContract().getRepayIntDate().isEmpty() )
		{
			lnsIntBill.setBegindate(la.getContract().getStartIntDate());

		}else{
			lnsIntBill.setBegindate(la.getContract().getRepayIntDate()); 			//账单开始日期
		}
		//计算本期服务费（通过试算取当期服务费账单的计提利息金额）
		FabAmount feeAmt = new FabAmount(0.00);
		//计算本期利息（通过试算取当期利息账单的计提利息金额）
		FabAmount intAmt = new FabAmount(0.00);

		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();
		LnsBill lnsBill = new LnsBill();


		while(billListIterator.hasNext())
		{
			lnsBill = billListIterator.next();
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
				!VarChecker.isEmpty(lnsBill.getRepayDateInt())	)
			{
				intAmt = lnsBill.getRepayDateInt();
				break;
			}
		}

		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//生成账单期数信息参数
		PrepayTermSupporter prepayTermSupporter = LoanSupporterUtil.getPrepayRepaywaySupporter(la.getWithdrawAgreement().getRepayWay());
		prepayTermSupporter.setLoanAgreement(la);
		prepayTermSupporter.genVal(prepayTermSupporter.genMoment(la.getContract().getContractStartDate(),la.getContract().getRepayIntDate() ,la.getContract().getRepayPrinDate(),ctx.getTranDate()),
								   prepayTermSupporter.genThing(intAmt, tranAmt));
		//根据配置设置期数
		Integer currentTerm = prepayTermSupporter.genUseTerm(la.getContract().getCurrIntPeriod());
		//期数是否累加
		boolean isAccumTerm = prepayTermSupporter.isAccumterm();
		//是否插本金账单计划表
		boolean isInstPrinBillPlan = prepayTermSupporter.isInsertPrinBillPlan();
		//是否插利息账单计划表
		boolean isInstIntBillPlan = prepayTermSupporter.isInsertIntBillPlan();





		//生成利息账单
		//非只还本标志的生成利息账单
		if( !ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN.equals(interestFlag) )
		{
			//利息金额>=还款金额
			if( intAmt.getVal() >= remainAmt.getVal() )
			{
				//利息余额=利息金额-还款金额
				lnsIntBill.setBillbal( intAmt.sub(remainAmt).getVal() );
				//不生成本金账单
				amtFlag = false;
				remainAmt = new FabAmount();
			}
			//利息金额<还款金额，
			else
			{
				//利息减免金额不能大于应还金额
				if(ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYINT.equals(interestFlag))
				{
					throw new FabException("LNS031");
				}
				//剩余金额=还款金额-利息金额
				remainAmt.setVal( remainAmt.sub(intAmt).getVal() );
				//账单结清日期赋值
				lnsIntBill.setSettledate(ctx.getTranDate());
				//账单余额0结清
				lnsIntBill.setBillbal( 0.00 );
			}

			//账单金额大于0 利息账单赋值
			if( new FabAmount(intAmt.getVal()).isPositive())
			{
				//账务日期
				lnsIntBill.setTrandate(Date.valueOf(ctx.getTranDate()));
				//流水号
				lnsIntBill.setSerseqno(ctx.getSerSeqNo());
				//子序号
				lnsIntBill.setTxseq(++seqno);
				//账号
				lnsIntBill.setAcctno(la.getContract().getReceiptNo());
				//机构
				lnsIntBill.setBrc(ctx.getBrc());
				//账单类型（利息）
				lnsIntBill.setBilltype(lnsBill.getBillType());
				//期数（利息期数）
				lnsIntBill.setPeriod(currentTerm);
				//账单金额
				lnsIntBill.setBillamt(intAmt.getVal());
				//账单余额
				lnsIntBill.setBillbal(lnsIntBill.getBillbal());
				//上日余额
				//lnsbill.setLastbal(0.00);
				//上笔日期
				lnsIntBill.setLastdate(ctx.getTranDate());
				//账单对应金额（当期本金金额）
				lnsIntBill.setPrinbal(lnsBill.getPrinBal().getVal());
				//账单利率（正常利率）
				lnsIntBill.setBillrate(la.getRateAgreement().getNormalRate().getYearRate().doubleValue());
				//账单结束日期
				lnsIntBill.setEnddate(ctx.getTranDate());
				//利息记止日期
				lnsIntBill.setIntedate(lnsBill.getIntendDate());
				//账单应还款日期
				lnsIntBill.setRepayedate(lnsBill.getRepayendDate());
				//当期到期日
				lnsIntBill.setCurenddate(lnsBill.getCurendDate());
				//还款方式
				lnsIntBill.setRepayway(lnsBill.getRepayWay());
				//币种
				lnsIntBill.setCcy(lnsBill.getCcy());
				//状态（正常）
				lnsIntBill.setBillstatus(lnsBill.getBillStatus());
				//账单状态开始日期
				lnsIntBill.setStatusbdate(ctx.getTranDate());
				//账单属性(还款)
				lnsIntBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//利息入账标志
				lnsIntBill.setIntrecordflag(lnsBill.getIntrecordFlag());
				//账单是否作废标志
				lnsIntBill.setCancelflag(lnsBill.getCancelFlag());
				if( new FabAmount(lnsIntBill.getBillbal()).isZero() )
				{
					//账单结清标志（结清）
					lnsIntBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				}
				else
				{
					//账单结清标志（未结清）
					lnsIntBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				}

				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsIntBill);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}

				//根据标志判断是否插辅助表
				if( isInstIntBillPlan )
				{
					interestBillPlan(ctx,seqno,la,lnsIntBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
				}



				//修改主文件上次结息日
				basicinfo.setLastIntDate(ctx.getTranDate());

				//已还利息（账单金额-账单余额）
				totalInt.setBillAmt(new FabAmount(new FabAmount(lnsIntBill.getBillamt()).sub(new FabAmount(lnsIntBill.getBillbal())).getVal()));
				totalInt.setBillTrandate(ctx.getTranDate());
				totalInt.setBillSerseqno(ctx.getSerSeqNo());
				totalInt.setBillTxseq(lnsIntBill.getTxseq());
				totalInt.setBillStatus(lnsBill.getBillStatus());
				nintAdvance.add(totalInt);


				//增值税
				LoanAcctChargeProvider.add(BillTransformHelper.convertToLnsBill(lnsIntBill),
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//登记事件
//				LoanAcctChargeProvider.event(BillTransformHelper.convertToLnsBill(lnsIntBill),
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(new FabAmount(lnsIntBill.getBillamt())));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), BillTransformHelper.convertToLnsBill(lnsIntBill));
			}

			//2017-12-04 剩余利息分摊到每一天时金额为0也更新上次结息日
			basicinfo.setLastIntDate(ctx.getTranDate());
		}
		if(CalendarUtil.afterAlsoEqual(ctx.getRequestDict("dealDate").toString() , lnsBill.getStartDate())){
			LnsBillStatistics dealDatelnsBills = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getRequestDict("dealDate").toString() , ctx);

			LnsBill feeBill = new LnsBill();
			while(dealDatelnsBills.hasFutureBill())
			{
				feeBill = dealDatelnsBills.getFutureBill();
				if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_AFEE).contains(feeBill.getBillType()) &&
						!VarChecker.isEmpty(feeBill.getRepayDateInt())	)
				{
					feeAmt = feeBill.getBillBal();
					break;
				}
			}

			//服务费金额>=还款金额
			if( feeAmt.getVal() >= remainAmt.getVal() )
			{
				//服务费余额=利息金额-还款金额
				lnsFeeBill.setBillbal( feeAmt.sub(remainAmt).getVal() );
				remainAmt = new FabAmount();
			}
			//利息金额<还款金额，
			else
			{

				//剩余金额=还款金额-利息金额
				remainAmt.setVal( remainAmt.sub(feeAmt).getVal() );
				//账单结清日期赋值
				lnsFeeBill.setSettledate(ctx.getTranDate());
				//账单余额0结清
				lnsFeeBill.setBillbal( 0.00 );
			}
			//账单金额大于0 服务费账单赋值
			if( new FabAmount(feeAmt.getVal()).isPositive())
			{
				//账务日期
				lnsFeeBill.setTrandate(Date.valueOf(ctx.getTranDate()));
				//流水号
				lnsFeeBill.setSerseqno(ctx.getSerSeqNo());
				//子序号
				lnsFeeBill.setTxseq(++seqno);
				//账号
				lnsFeeBill.setAcctno(la.getContract().getReceiptNo());
				//机构
				lnsFeeBill.setBrc(ctx.getBrc());
				//账单类型（利息）
				lnsFeeBill.setBilltype(feeBill.getBillType());
				//期数（利息期数）
				lnsFeeBill.setPeriod(currentTerm);
				//账单金额
				lnsFeeBill.setBillamt(feeAmt.getVal());
				//账单余额
				lnsFeeBill.setBillbal(lnsFeeBill.getBillbal());
				//上日余额
				//lnsbill.setLastbal(0.00);
				//上笔日期
				lnsFeeBill.setLastdate(ctx.getTranDate());
				//账单对应金额（当期本金金额）
				lnsFeeBill.setPrinbal(feeBill.getPrinBal().getVal());
				//账单利率（正常利率）
				lnsFeeBill.setBillrate(feeBill.getBillRate().getRate().doubleValue());
				//账单结束日期
				lnsFeeBill.setEnddate(ctx.getTranDate());
				//利息记止日期
				lnsFeeBill.setIntedate(feeBill.getIntendDate());
				//账单应还款日期
				lnsFeeBill.setRepayedate(feeBill.getRepayendDate());
				//当期到期日
				lnsFeeBill.setCurenddate(feeBill.getCurendDate());
				//还款方式
				lnsFeeBill.setRepayway(feeBill.getRepayWay());
				//币种
				lnsFeeBill.setCcy(feeBill.getCcy());
				//状态（正常）
				lnsFeeBill.setBillstatus(feeBill.getBillStatus());
				//账单状态开始日期
				lnsFeeBill.setStatusbdate(ctx.getTranDate());
				//账单属性(还款)
				lnsFeeBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//利息入账标志
				lnsFeeBill.setIntrecordflag(feeBill.getIntrecordFlag());
				//账单是否作废标志
				lnsFeeBill.setCancelflag(feeBill.getCancelFlag());
				if( new FabAmount(lnsFeeBill.getBillbal()).isZero() )
				{
					//账单结清标志（结清）
					lnsFeeBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				}
				else
				{
					//账单结清标志（未结清）
					lnsFeeBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				}

				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsFeeBill);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}
				//已还服务费（账单金额-账单余额）
				totalFee.setBillAmt(new FabAmount(new FabAmount(lnsFeeBill.getBillamt()).sub(new FabAmount(lnsFeeBill.getBillbal())).getVal()));
				totalFee.setBillTrandate(ctx.getTranDate());
				totalFee.setBillSerseqno(ctx.getSerSeqNo());
				totalFee.setBillTxseq(lnsFeeBill.getTxseq());
				feeAdvance.add(totalFee);

				//增值税
				LoanAcctChargeProvider.add(BillTransformHelper.convertToLnsBill(lnsFeeBill),
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//登记事件
//				LoanAcctChargeProvider.event(BillTransformHelper.convertToLnsBill(lnsFeeBill),
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(new FabAmount(lnsFeeBill.getBillamt())));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), BillTransformHelper.convertToLnsBill(lnsFeeBill));
			}
		}


		//生成本金账单
		//amtFlag标志不为false（还完利息还剩钱） ||  标志位只还本金
		if( (false != amtFlag && new FabAmount(lnsIntBill.getBillbal()).isZero())
			|| ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN.equals(interestFlag) )

		{


			//合同余额>还款余额
			if( la.getContract().getBalance().sub(remainAmt.getVal()).isPositive() )
			{
				//不允许提前还本
				if( "false".equals(la.getInterestAgreement().getIsAdvanceRepay()))

					throw new FabException("LNS064");

				//账单金额为还款余额
				lnsPrinBill.setBillamt( remainAmt.getVal() );
			}
			//合同余额<=还款余额，可以结清
			else
			{
				//账单金额为合同余额
				lnsPrinBill.setBillamt( la.getContract().getBalance().getVal() );
				//主文件状态结清
				//basicinfo.setLoanStat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
				//结清标志3-结清
				settleFlag = "3";

				//还款计划登记簿登记未来期所有计划
				LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"ADVANCE");
			}

			//本金账单赋值
			//账单余额0
			lnsPrinBill.setBillbal( 0.00 );
			//账务日期
			lnsPrinBill.setTrandate(Date.valueOf(ctx.getTranDate()));
			//流水号
			lnsPrinBill.setSerseqno(ctx.getSerSeqNo());
			//子序号
			lnsPrinBill.setTxseq(++seqno);
			//账号
			lnsPrinBill.setAcctno(la.getContract().getReceiptNo());
			//机构
			lnsPrinBill.setBrc(ctx.getBrc());
			//账单类型（本金）
			lnsPrinBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);
			//账单金额
			lnsPrinBill.setBillamt(lnsPrinBill.getBillamt());
			//账单余额
			lnsPrinBill.setBillbal(lnsPrinBill.getBillbal());
			//上日余额
			//lnsPrinBill.setLastbal(0.00);
			//上笔日期
			lnsPrinBill.setLastdate(ctx.getTranDate());
			//账单对应剩余本金金额（合同余额-账单金额）
			lnsPrinBill.setPrinbal(la.getContract().getBalance().sub(new FabAmount(lnsPrinBill.getBillamt())).getVal());
			//账单利率（正常利率）
			lnsPrinBill.setBillrate(0.00);
			//首次还款账单开始日为开户日
			if( la.getContract().getRepayPrinDate().isEmpty() )
			{
				lnsPrinBill.setBegindate(la.getContract().getContractStartDate());
			}
			//非首次账单开始日为上次结本日
			else
			{
				lnsPrinBill.setBegindate(la.getContract().getRepayPrinDate());
			}

			//账单结束日期
			lnsPrinBill.setEnddate(ctx.getTranDate());
			//期数（利息期数）
			lnsPrinBill.setPeriod(currentTerm);
			//账单应还款日期
			lnsPrinBill.setRepayedate(lnsBill.getRepayendDate());
			//账单结清日期
			lnsPrinBill.setSettledate(ctx.getTranDate());
			//利息记至日期
			lnsPrinBill.setIntedate(ctx.getTranDate());
			//当期到期日
			lnsPrinBill.setCurenddate(lnsBill.getCurendDate());
			//还款方式
			lnsPrinBill.setRepayway(la.getWithdrawAgreement().getRepayWay());
			//币种
			lnsPrinBill.setCcy(la.getContract().getCcy().getCcy());
			//账单状态（正常）
			lnsPrinBill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
			//账单状态开始日期
			lnsPrinBill.setStatusbdate(ctx.getTranDate());
			//账单属性(还款)
			lnsPrinBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);									//账单属性(还款)
			//利息入账标志
			lnsPrinBill.setIntrecordflag("YES");
			//账单是否作废标志
			lnsPrinBill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
			if( new FabAmount(lnsPrinBill.getBillbal()).isZero() )
			{
				//账单结清标志（结清）
				lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			}
			else
			{
				//账单结清标志（未结清）
				lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
			}

			//本金账单金额大于0
			if( lnsPrinBill.getBillamt() > 0.00 )
			{
				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsPrinBill);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}

				//根据标志判断是否插辅助表
				if( isInstPrinBillPlan || new FabAmount(lnsPrinBill.getPrinbal()).isZero() )
					interestBillPlan(ctx,seqno,la,lnsPrinBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);

				//根据标志判断是否更否更改主文件期数
				if( isAccumTerm )
				{
					basicinfo.setIntTerm(1);
					basicinfo.setPrinTerm(1);
				}


				//修改主文件上次结本日
				basicinfo.setLastPrinDate(ctx.getTranDate());
				//修改主文件合同余额
				basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));
			}

			//已还本金（账单金额-账单余额）
			totalPrin.setBillAmt(new FabAmount(new FabAmount(lnsPrinBill.getBillamt()).sub(new FabAmount(lnsPrinBill.getBillbal())).getVal()));
			totalPrin.setBillTrandate(ctx.getTranDate());
			totalPrin.setBillSerseqno(ctx.getSerSeqNo());
			totalPrin.setBillTxseq(lnsPrinBill.getTxseq());
			prinAdvance.add(totalPrin);

			//登记还款计划登记簿
			prinFlag = true;
		}


		//更新主文件结本结息日、期数
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);

		//登记还款计划登记簿，修改实际还款日
		//还到本金切未结清
		if(prinFlag && !"3".equals(settleFlag))
		{
			Map<String,Object> updmap = new HashMap<String, Object>();
			updmap.put("termretdate", ctx.getTranDate());
			updmap.put("acctno", la.getContract().getReceiptNo());
			updmap.put("brc", ctx.getBrc());

			//开户日还款或一天还多次本金的情况，删除当前一期还款计划
			if( ctx.getTranDate().equals(la.getContract().getContractStartDate()) ||
				lnsPrinBill.getBegindate().equals(ctx.getTranDate()) ||
				ctx.getTranDate().equals(basicinfo.getLastPrinDate()) )
			{
				try {
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsrpyplan", updmap);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS102", "lnsrpyplan");
				}

			}
			//生成下一期还款计划
			LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"REPAY");

			//还款方式为8定期时不更新实际还款日
			if(!lnsPrinBill.getBegindate().equals(ctx.getTranDate()) &&
			   !ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(la.getWithdrawAgreement().getRepayWay()))
			{
				int count = 0;
				try {
					count = DbAccessUtil.execute("CUSTOMIZE.update_lnsrpyplan", updmap);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS102", "lnsrpyplan");
				}
				if(1 < count){
					throw new FabException("SPS102", "lnsrpyplan");
				}
			}
		}
		//返回结清标志
		return settleFlag;

	}

	/**
     * 功能：非等本等息当前期或提前期（还款、利息减免）生成账单
     * 描述：利息账单生成到当前日期，剩余金额生成本金账单，如果提前还款还到本金登记还款计划表
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	txseq     				子序号
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     * @param 	interestFlag 			利息标志（ONLYINT只还息、ONLYPRIN只还本、INTPRIN还本还息）
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
     *
     * @since  1.1.1
     */

	//非等本等息当前期或提前期（还款、利息减免）生成账单
	public static String interestRepaymentBill(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag,String compensateFlag,String repaySettle) throws FabException
	{
		String endDate=ctx.getTranDate();
		if(loanAgreement.getContract().getContractStartDate().equals(ctx.getTranDate()) &&
				"3".equals(loanAgreement.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(ctx.getTranDate(), 1).toString("yyyy-MM-dd");
		}
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, endDate, ctx);


		Integer seqno = txseq;
		//本金标志（还到本金登记还款计划登记簿）
		Boolean prinFlag = false;
		//还完利息是否有钱还本金
		Boolean amtFlag = true;
		//结清标志 （1-未结清 3-结清）
		String settleFlag = "1";


		//提前还款还本金金额及流水
		RepayAdvance totalPrin = new RepayAdvance();
		//提前还款还利息金额及流水
		RepayAdvance totalInt = new RepayAdvance() ;

		//利息账单结构
		TblLnsbill lnsIntBill = new TblLnsbill();
		//本金账单结构
		TblLnsbill lnsPrinBill = new TblLnsbill();
		//提前还款后剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());



		//第一次还款取计息日作为账单开始日，非第一次还款取上次结息日作为账单开始日
		if( la.getContract().getRepayIntDate().isEmpty() )
		{
			lnsIntBill.setBegindate(la.getContract().getStartIntDate());

		}else{
			lnsIntBill.setBegindate(la.getContract().getRepayIntDate()); 			//账单开始日期
		}

		//计算本期利息（通过试算取当期利息账单的计提利息金额）
		FabAmount intAmt = new FabAmount(0.00);
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		//筛选账本  非费用账本
		listBill = LoanFeeUtils.filtFeeBill(listBill);
		ListIterator<LnsBill> billListIterator = listBill.listIterator();

		LnsBill lnsBill = new LnsBill();
		while(billListIterator.hasNext())
		{
			lnsBill = billListIterator.next();
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
				!VarChecker.isEmpty(lnsBill.getRepayDateInt())	)
			{
				intAmt = lnsBill.getRepayDateInt();
				break;
			}
		}

		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//生成账单期数信息参数
		PrepayTermSupporter prepayTermSupporter = LoanSupporterUtil.getPrepayRepaywaySupporter(la.getWithdrawAgreement().getRepayWay());
		prepayTermSupporter.setLoanAgreement(la);
		prepayTermSupporter.genVal(prepayTermSupporter.genMoment(la.getContract().getContractStartDate(),la.getContract().getRepayIntDate() ,la.getContract().getRepayPrinDate(),ctx.getTranDate()),
								   prepayTermSupporter.genThing(intAmt, tranAmt));
		//根据配置设置期数
		Integer currentTerm = prepayTermSupporter.genUseTerm(la.getContract().getCurrIntPeriod());
		//期数是否累加
		boolean isAccumTerm = prepayTermSupporter.isAccumterm();
		//是否插本金账单计划表
		boolean isInstPrinBillPlan = prepayTermSupporter.isInsertPrinBillPlan();
		//是否插利息账单计划表
		boolean isInstIntBillPlan = prepayTermSupporter.isInsertIntBillPlan();


		//生成利息账单
		//非只还本标志的生成利息账单
		if( !ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN.equals(interestFlag) )
		{
			//利息金额>=还款金额
			if( intAmt.getVal() >= tranAmt.getVal() )
			{
				//利息余额=利息金额-还款金额
				lnsIntBill.setBillbal( intAmt.sub(tranAmt).getVal() );
				//不生成本金账单
				amtFlag = false;
			}
			//利息金额<还款金额，
			else
			{
				//利息减免金额不能大于应还金额
				if(ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYINT.equals(interestFlag))
				{
					throw new FabException("LNS031");
				}
				//剩余金额=还款金额-利息金额
				remainAmt.setVal( tranAmt.sub(intAmt).getVal() );
				//账单结清日期赋值
				lnsIntBill.setSettledate(ctx.getTranDate());
				//账单余额0结清
				lnsIntBill.setBillbal( 0.00 );
			}

			//账单金额大于0 利息账单赋值
			if( new FabAmount(intAmt.getVal()).isPositive())
			{
				//账务日期
				lnsIntBill.setTrandate(Date.valueOf(ctx.getTranDate()));
				//流水号
				lnsIntBill.setSerseqno(ctx.getSerSeqNo());
				//子序号
				lnsIntBill.setTxseq(++seqno);
				//账号
				lnsIntBill.setAcctno(la.getContract().getReceiptNo());
				//机构
				lnsIntBill.setBrc(ctx.getBrc());
				//账单类型（利息）
				lnsIntBill.setBilltype(lnsBill.getBillType());
				//期数（利息期数）
				lnsIntBill.setPeriod(currentTerm);
				//账单金额
				lnsIntBill.setBillamt(intAmt.getVal());
				//账单余额
				lnsIntBill.setBillbal(lnsIntBill.getBillbal());
				//上日余额
				//lnsbill.setLastbal(0.00);
				//上笔日期
				lnsIntBill.setLastdate(ctx.getTranDate());
				//账单对应金额（当期本金金额）
				lnsIntBill.setPrinbal(lnsBill.getPrinBal().getVal());
				//账单利率（正常利率）
				lnsIntBill.setBillrate(la.getRateAgreement().getNormalRate().getYearRate().doubleValue());
				//账单结束日期
				lnsIntBill.setEnddate(ctx.getTranDate());
				//利息记止日期
				lnsIntBill.setIntedate(lnsBill.getIntendDate());
				//账单应还款日期
				lnsIntBill.setRepayedate(lnsBill.getRepayendDate());
				//当期到期日
				lnsIntBill.setCurenddate(lnsBill.getCurendDate());
				//还款方式
				lnsIntBill.setRepayway(lnsBill.getRepayWay());
				//币种
				lnsIntBill.setCcy(lnsBill.getCcy());
				//状态（正常）
				lnsIntBill.setBillstatus(lnsBill.getBillStatus());
				//账单状态开始日期
				lnsIntBill.setStatusbdate(ctx.getTranDate());
				//账单属性(还款)
				lnsIntBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//2019-11-01  代偿未来期改标志，借新还旧改标记【20200514】
				if( VarChecker.asList("2","3","4").contains(compensateFlag) )
					lnsIntBill.setBillproperty("2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ("3".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL));
				//利息入账标志
				lnsIntBill.setIntrecordflag(lnsBill.getIntrecordFlag());
				//账单是否作废标志
				lnsIntBill.setCancelflag(lnsBill.getCancelFlag());
				//核销优化登记cancelflag，“3”
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
					lnsIntBill.setCancelflag("3");
				}

				if( new FabAmount(lnsIntBill.getBillbal()).isZero() )
				{
					//账单结清标志（结清）
					lnsIntBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				}
				else
				{
					//账单结清标志（未结清）
					lnsIntBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				}

				//账速融接账务核心特殊处理 2019-02-14
				if( "3010014".equals(la.getPrdId()) )
					lnsIntBill.setEnddate(lnsBill.getCurendDate());

				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsIntBill);

				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}

				//根据标志判断是否插辅助表
				if( isInstIntBillPlan )
				{
					interestBillPlan(ctx,seqno,la,lnsIntBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
				}



				//修改主文件上次结息日
				basicinfo.setLastIntDate(ctx.getTranDate());

				//账速融接账务核心特殊处理 2019-02-14
				if( "3010014".equals(la.getPrdId()) )
					basicinfo.setLastIntDate(lnsIntBill.getCurenddate());

				//已还利息（账单金额-账单余额）
				totalInt.setBillAmt(new FabAmount(new FabAmount(lnsIntBill.getBillamt()).sub(new FabAmount(lnsIntBill.getBillbal())).getVal()));
				totalInt.setBillTrandate(ctx.getTranDate());
				totalInt.setBillSerseqno(ctx.getSerSeqNo());
				totalInt.setBillTxseq(lnsIntBill.getTxseq());
				totalInt.setRepayterm(lnsBill.getPeriod());
				totalInt.setBillStatus(lnsBill.getBillStatus());
				nintAdvance.add(totalInt);


				//增值税
				LoanAcctChargeProvider.add(BillTransformHelper.convertToLnsBill(lnsIntBill),
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//登记事件
//				LoanAcctChargeProvider.event(BillTransformHelper.convertToLnsBill(lnsIntBill),
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(new FabAmount(lnsIntBill.getBillamt())));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), BillTransformHelper.convertToLnsBill(lnsIntBill));
			}
			//免息有可能 利息为0 ， 还款金额大于0，继续还款本金，主文件动态表对应免息金额减少
            //intAmt>0 插账本了，也需要主文件动态表对应免息金额减少
            if( (new FabAmount(intAmt.getVal()).isPositive()||
                    tranAmt.isPositive())&&
                    !VarChecker.isEmpty(lnsBill.getTermFreeInterest())){
                //现金贷免息税金表生成记录
                AccountingModeChange.saveFreeInterestTax(ctx,la.getContract().getReceiptNo(),lnsBill);
                //主文件动态表对应免息金额减少
                AccountingModeChange.updateBasicDynMX(ctx,la.getBasicExtension().getFreeInterest().sub(lnsBill.getTermFreeInterest()).getVal(),la.getContract().getReceiptNo(),ctx.getTranDate(),lnsBill);
            }
			//2017-12-04 剩余利息分摊到每一天时金额为0也更新上次结息日
			if( !"3010014".equals(la.getPrdId()) )
				basicinfo.setLastIntDate(ctx.getTranDate());
		}

		//生成本金账单
		//amtFlag标志不为false（还完利息还剩钱） ||  标志位只还本金
		if( (false != amtFlag && new FabAmount(lnsIntBill.getBillbal()).isZero())
			|| ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN.equals(interestFlag) )

		{
			//2018-10-11 信速融账单合并
			if( "8".equals(la.getWithdrawAgreement().getRepayWay()))
			{






				//取本金账单
				List<LnsBill> prinBillList = lnsBillStatistics.getHisBillList();
				ListIterator<LnsBill> prinListIterator = prinBillList.listIterator();

				String prinDate = "1970-01-01";
				LnsBill prinLnsBill;
				//随借随还合并账单一天还多次本金取开始日期
				while(prinListIterator.hasNext())
				{
					prinLnsBill = prinListIterator.next();
					if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(prinLnsBill.getBillType()) &&
							prinLnsBill.getStartDate().equals(prinLnsBill.getEndDate())	)
					{
						if( CalendarUtil.after(prinLnsBill.getStartDate(), prinDate)  )
							prinDate = prinLnsBill.getStartDate();
					}
				}




				//当天有过提前还本，更新本金账单金额
				if( prinDate.equals(ctx.getTranDate()) )
				{
					//合同余额>还款余额
					if( la.getContract().getBalance().sub(remainAmt.getVal()).isPositive() )
					{
						//不允许提前还本
						if( "false".equals(la.getInterestAgreement().getIsAdvanceRepay()))
							throw new FabException("LNS064");

						//账单金额为还款余额
						lnsPrinBill.setBillamt( remainAmt.getVal() );
					}
					//合同余额<=还款余额，可以结清
					else
					{
						//账单金额为合同余额
						lnsPrinBill.setBillamt( la.getContract().getBalance().getVal() );
						//主文件状态结清
						//basicinfo.setLoanStat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
						//结清标志3-结清
						settleFlag = "3";
						remainAmt.selfSub(la.getContract().getBalance());
					}

					Map<String, Object> upbillmap = new HashMap<String, Object>();
					upbillmap.put("acctno", la.getContract().getReceiptNo());
					upbillmap.put("amt", lnsPrinBill.getBillamt());
					upbillmap.put("date", ctx.getTranDate());
					upbillmap.put("trandate", ctx.getTranDate());


					try {
						DbAccessUtil.execute("CUSTOMIZE.update_lnsbill_oneday", upbillmap);
					} catch (FabSqlException e) {
						throw new FabException(e, "SPS102", "lnsbasicinfo");
					}


					//修改主文件合同余额
					basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));
					//已还本金（账单金额-账单余额）
					totalPrin.setBillAmt(new FabAmount(lnsPrinBill.getBillamt()));
					totalPrin.setBillTrandate(ctx.getTranDate());
					totalPrin.setBillSerseqno(ctx.getSerSeqNo());
					totalPrin.setBillTxseq(lnsPrinBill.getTxseq());
//					totalPrin.setRepayterm(lnsPrinBill.getPeriod());
					totalPrin.setRepayterm(1);
					prinAdvance.add(totalPrin);

					//登记还款计划登记簿
					prinFlag = true;
				}
				//当天首次提前还本，插入本金账单
				else
				{
					//合同余额>还款余额
					if( la.getContract().getBalance().sub(remainAmt.getVal()).isPositive() )
					{
						//不允许提前还本
						if( "false".equals(la.getInterestAgreement().getIsAdvanceRepay()))
							throw new FabException("LNS064");

						//账单金额为还款余额
						lnsPrinBill.setBillamt( remainAmt.getVal() );
					}
					//合同余额<=还款余额，可以结清
					else
					{
						//账单金额为合同余额
						lnsPrinBill.setBillamt( la.getContract().getBalance().getVal() );
						//主文件状态结清
						//basicinfo.setLoanStat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
						//结清标志3-结清
						settleFlag = "3";
						remainAmt.selfSub(la.getContract().getBalance());
						//还款计划登记簿登记未来期所有计划
						LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"ADVANCE");
					}

					//本金账单赋值
					//账单余额0
					lnsPrinBill.setBillbal( 0.00 );
					//账务日期
					lnsPrinBill.setTrandate(Date.valueOf(ctx.getTranDate()));
					//流水号
					lnsPrinBill.setSerseqno(ctx.getSerSeqNo());
					//子序号
					lnsPrinBill.setTxseq(++seqno);
					//账号
					lnsPrinBill.setAcctno(la.getContract().getReceiptNo());
					//机构
					lnsPrinBill.setBrc(ctx.getBrc());
					//账单类型（本金）
					lnsPrinBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);
					//账单金额
					lnsPrinBill.setBillamt(lnsPrinBill.getBillamt());
					//账单余额
					lnsPrinBill.setBillbal(lnsPrinBill.getBillbal());
					//上日余额
					//lnsPrinBill.setLastbal(0.00);
					//上笔日期
					lnsPrinBill.setLastdate(ctx.getTranDate());
					//账单对应剩余本金金额（合同余额-账单金额）
					lnsPrinBill.setPrinbal(la.getContract().getBalance().sub(new FabAmount(lnsPrinBill.getBillamt())).getVal());
					//账单利率（正常利率）
					lnsPrinBill.setBillrate(0.00);
					//首次还款账单开始日为开户日
					if( la.getContract().getRepayPrinDate().isEmpty() )
					{
						lnsPrinBill.setBegindate(la.getContract().getContractStartDate());
					}
					//非首次账单开始日为上次结本日
					else
					{
						lnsPrinBill.setBegindate(la.getContract().getRepayPrinDate());
					}

					//账单结束日期
					lnsPrinBill.setEnddate(ctx.getTranDate());
					//期数（利息期数）
					lnsPrinBill.setPeriod(currentTerm);
					//账单应还款日期
					lnsPrinBill.setRepayedate(lnsBill.getRepayendDate());
					//账单结清日期
					lnsPrinBill.setSettledate(ctx.getTranDate());
					//利息记至日期
					lnsPrinBill.setIntedate(ctx.getTranDate());
					//当期到期日
					lnsPrinBill.setCurenddate(lnsBill.getCurendDate());
					//还款方式
					lnsPrinBill.setRepayway(la.getWithdrawAgreement().getRepayWay());
					//币种
					lnsPrinBill.setCcy(la.getContract().getCcy().getCcy());
					//账单状态（正常）
					lnsPrinBill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
					//账单状态开始日期
					lnsPrinBill.setStatusbdate(ctx.getTranDate());
					//账单属性(还款)
					lnsPrinBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
					//2019-11-01  代偿未来期改标志，借新还旧改标记【20200514】
					if( VarChecker.asList("2","3","4").contains(compensateFlag) )
						lnsPrinBill.setBillproperty("2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ("3".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL));
					//利息入账标志
					lnsPrinBill.setIntrecordflag("YES");
					//账单是否作废标志
					lnsPrinBill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
					if( new FabAmount(lnsPrinBill.getBillbal()).isZero() )
					{
						//账单结清标志（结清）
						lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					}
					else
					{
						//账单结清标志（未结清）
						lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
					}

					//本金账单金额大于0
					if( lnsPrinBill.getBillamt() > 0.00 )
					{
						//插入账单表
						try {
							DbAccessUtil.execute("Lnsbill.insert", lnsPrinBill);
						} catch (FabSqlException e) {
							throw new FabException(e, "SPS100", "lnsbill");
						}

						//根据标志判断是否插辅助表
						if( isInstPrinBillPlan || new FabAmount(lnsPrinBill.getPrinbal()).isZero() )
							interestBillPlan(ctx,seqno,la,lnsPrinBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);

						//根据标志判断是否更否更改主文件期数
						if( isAccumTerm )
						{
							basicinfo.setIntTerm(1);
							basicinfo.setPrinTerm(1);
						}


						//修改主文件上次结本日
						basicinfo.setLastPrinDate(ctx.getTranDate());
						//修改主文件合同余额
						basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));
					}

					//已还本金（账单金额-账单余额）
					totalPrin.setBillAmt(new FabAmount(new FabAmount(lnsPrinBill.getBillamt()).sub(new FabAmount(lnsPrinBill.getBillbal())).getVal()));
					totalPrin.setBillTrandate(ctx.getTranDate());
					totalPrin.setBillSerseqno(ctx.getSerSeqNo());
					totalPrin.setBillTxseq(lnsPrinBill.getTxseq());
//					totalPrin.setRepayterm(lnsPrinBill.getPeriod());
					totalPrin.setRepayterm(1);
					prinAdvance.add(totalPrin);

					//登记还款计划登记簿
					prinFlag = true;
				}
			}
			else
			{
				//合同余额>还款余额
				if( la.getContract().getBalance().sub(remainAmt.getVal()).isPositive() )
				{

					//不允许提前还本
					if( "false".equals(la.getInterestAgreement().getIsAdvanceRepay()))
					{
						if( "1".equals(repaySettle) )
							throw new FabException("LNS202");
						throw new FabException("LNS064");
					}

					//延期(结息日延期的情况)当期不允许提前部分还本 -- 结息日延期 修改了上次结本日到延期后当期到期日
					if(!VarChecker.isEmpty(la.getBasicExtension().getTermEndDate())
						&&CalendarUtil.before(ctx.getTranDate(),la.getContract().getRepayPrinDate()))
							throw new FabException("LNS213","延期当期");

//					if(!la.getFeeAgreement().isEmpty())
//                    {
//                        for(TblLnsfeeinfo lnsfeeinfo:la.getFeeAgreement().getLnsfeeinfos()){
//                            if(ConstantDeclare.CALCULATRULE.BYTERM.equals(lnsfeeinfo.getCalculatrule()))
//                                throw new FabException("LNS213","费用期缴");
//                        }
//                    }
					//账单金额为还款余额
					lnsPrinBill.setBillamt( remainAmt.getVal() );
				}
				//合同余额<=还款余额，可以结清
				else
				{
					//账单金额为合同余额
					lnsPrinBill.setBillamt( la.getContract().getBalance().getVal() );
					//主文件状态结清
					//basicinfo.setLoanStat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
					//结清标志3-结清
					settleFlag = "3";
					remainAmt.selfSub(la.getContract().getBalance());
					//还款计划登记簿登记未来期所有计划
					LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"ADVANCE");
				}

				//本金账单赋值
				//账单余额0
				lnsPrinBill.setBillbal( 0.00 );
				//账务日期
				lnsPrinBill.setTrandate(Date.valueOf(ctx.getTranDate()));
				//流水号
				lnsPrinBill.setSerseqno(ctx.getSerSeqNo());
				//子序号
				lnsPrinBill.setTxseq(++seqno);
				//账号
				lnsPrinBill.setAcctno(la.getContract().getReceiptNo());
				//机构
				lnsPrinBill.setBrc(ctx.getBrc());
				//账单类型（本金）
				lnsPrinBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);
				//账单金额
				lnsPrinBill.setBillamt(lnsPrinBill.getBillamt());
				//账单余额
				lnsPrinBill.setBillbal(lnsPrinBill.getBillbal());
				//上日余额
				//lnsPrinBill.setLastbal(0.00);
				//上笔日期
				lnsPrinBill.setLastdate(ctx.getTranDate());
				//账单对应剩余本金金额（合同余额-账单金额）
				lnsPrinBill.setPrinbal(la.getContract().getBalance().sub(new FabAmount(lnsPrinBill.getBillamt())).getVal());
				//账单利率（正常利率）
				lnsPrinBill.setBillrate(0.00);
				//首次还款账单开始日为开户日
				if( la.getContract().getRepayPrinDate().isEmpty() )
				{
					lnsPrinBill.setBegindate(la.getContract().getContractStartDate());
				}
				//非首次账单开始日为上次结本日
				else
				{
					lnsPrinBill.setBegindate(la.getContract().getRepayPrinDate());
				}

				//账单结束日期
				lnsPrinBill.setEnddate(ctx.getTranDate());
				//期数（利息期数）
				lnsPrinBill.setPeriod(currentTerm);
				//账单应还款日期
				lnsPrinBill.setRepayedate(lnsBill.getRepayendDate());
				//账单结清日期
				lnsPrinBill.setSettledate(ctx.getTranDate());
				//利息记至日期
				lnsPrinBill.setIntedate(ctx.getTranDate());
				//当期到期日
				lnsPrinBill.setCurenddate(ctx.getTranDate());
				//还款方式
				lnsPrinBill.setRepayway(la.getWithdrawAgreement().getRepayWay());
				//币种
				lnsPrinBill.setCcy(la.getContract().getCcy().getCcy());
				//账单状态（正常）
				lnsPrinBill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				//账单状态开始日期
				lnsPrinBill.setStatusbdate(ctx.getTranDate());
				//账单属性(还款)
				lnsPrinBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//2019-11-01  代偿未来期改标志，借新还旧改标记【20200514】
				if( VarChecker.asList("2","3","4").contains(compensateFlag) )
					lnsPrinBill.setBillproperty("2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ("3".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL));	//账单属性(还款)
				//利息入账标志
				lnsPrinBill.setIntrecordflag("YES");
				//账单是否作废标志
				lnsPrinBill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
				if( new FabAmount(lnsPrinBill.getBillbal()).isZero() )
				{
					//账单结清标志（结清）
					lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				}
				else
				{
					//账单结清标志（未结清）
					lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				}

				//本金账单金额大于0
				if( lnsPrinBill.getBillamt() > 0.00 )
				{
					//插入账单表
					try {
						DbAccessUtil.execute("Lnsbill.insert", lnsPrinBill);
					} catch (FabSqlException e) {
						throw new FabException(e, "SPS100", "lnsbill");
					}

					//根据标志判断是否插辅助表
					if( isInstPrinBillPlan || new FabAmount(lnsPrinBill.getPrinbal()).isZero() )
						interestBillPlan(ctx,seqno,la,lnsPrinBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);

					//根据标志判断是否更否更改主文件期数
					if( isAccumTerm )
					{
						basicinfo.setIntTerm(1);
						basicinfo.setPrinTerm(1);
					}


					//修改主文件上次结本日
					basicinfo.setLastPrinDate(ctx.getTranDate());
					//修改主文件合同余额
					basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));
				}

				//已还本金（账单金额-账单余额）
				totalPrin.setBillAmt(new FabAmount(new FabAmount(lnsPrinBill.getBillamt()).sub(new FabAmount(lnsPrinBill.getBillbal())).getVal()));
				totalPrin.setBillTrandate(ctx.getTranDate());
				totalPrin.setBillSerseqno(ctx.getSerSeqNo());
				totalPrin.setBillTxseq(lnsPrinBill.getTxseq());
				totalPrin.setRepayterm(lnsPrinBill.getPeriod());
				prinAdvance.add(totalPrin);

				//登记还款计划登记簿
				prinFlag = true;
			}

		}


		//更新主文件结本结息日、期数
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);

		//2019-11-29 传结清标志但未结清，报错
		if( "1".equals(repaySettle) &&
			(!"3".equals(settleFlag) || !remainAmt.isZero()) )
			throw new FabException("LNS202");

		//还到本金  费用结费到当天
		if(!la.getFeeAgreement().isEmpty())
          {
              for(TblLnsfeeinfo lnsfeeinfo:la.getFeeAgreement().getLnsfeeinfos()){
                  if(ConstantDeclare.CALCULATRULE.BYDAY.equals(lnsfeeinfo.getCalculatrule()))

                  {
                	  if(prinFlag )
            		  {
            		      LoanFeeUtils.earlyRepaySettle(lnsBillStatistics,ctx ,loanAgreement,seqno,lnsfeeinfo );
            		  }
                  }
              }
          }
//		if(prinFlag )
//		{
//			LoanFeeUtils.earlyRepaySettle(lnsBillStatistics,ctx ,loanAgreement );
//		}


		//登记还款计划登记簿，修改实际还款日
		//还到本金切未结清
		if(prinFlag && !"3".equals(settleFlag))
		{
			Map<String,Object> updmap = new HashMap<String, Object>();
			updmap.put("termretdate", ctx.getTranDate());
			updmap.put("acctno", la.getContract().getReceiptNo());
			updmap.put("brc", ctx.getBrc());

			//开户日还款或一天还多次本金的情况，删除当前一期还款计划
			if( ctx.getTranDate().equals(la.getContract().getContractStartDate()) ||
				lnsPrinBill.getBegindate().equals(ctx.getTranDate()) ||
				ctx.getTranDate().equals(basicinfo.getLastPrinDate()) )
			{
				try {
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsrpyplan", updmap);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS102", "lnsrpyplan");
				}

			}
			//生成下一期还款计划
			LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"REPAY");

			//还款方式为8定期时不更新实际还款日
			if(!lnsPrinBill.getBegindate().equals(ctx.getTranDate()) &&
			   !ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(la.getWithdrawAgreement().getRepayWay()))
			{
				int count = 0;
				try {
					count = DbAccessUtil.execute("CUSTOMIZE.update_lnsrpyplan", updmap);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS102", "lnsrpyplan");
				}
				if(1 < count){
					throw new FabException("SPS102", "lnsrpyplan");
				}
			}
		}

		//返回结清标志
		return settleFlag;
	}


	/**
     * 功能：等本等息当前期或提前期（还款）生成账单
     * 描述：等本等息还款方式如果提前结清则不收未来期利息，不能提前结清按每期利息、本金顺序扣款
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	txseq     				子序号
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     * @param 	interestFlag 			利息标志（ONLYINT只还息、ONLYPRIN只还本、INTPRIN还本还息）
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
	 * @throws InterruptedException
     *
     * @since  1.1.1
     */

	//等本等息当前期或提前期（还款）生成账单
	public static String specialRepaymentBill(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag,String compensateFlag,String repaySettle) throws FabException, InterruptedException
	{


		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);


		//用于记录生成账单的期数
		int terms = 0;
		//用于记录是否同一期账单
		Integer period = 0;
		//利息账单标志
		boolean intFlag = true;
		//本金账单标志
		boolean prinFlag = true;
		//能否提前结清
		boolean cleanFlag = false;
		//结清标志 1-未结清 3-结清
		String settleFlag = "1";

		//累计的本金金额
		FabAmount contAmt = new FabAmount(0.00);
		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//取未来期账单list
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		listBill = LoanFeeUtils.filtFeeBill(listBill);

		ListIterator<LnsBill> billListIterator = listBill.listIterator();
		//筛选账本  非费用账本
		//还款计划表结构
		TblLnsrpyplan lnsRpyPlan = new TblLnsrpyplan();

		//遍历未来期账单
		while(billListIterator.hasNext())
		{
			//提前还款还本金金额及流水
			RepayAdvance totalPrin = new RepayAdvance();
			//提前还款还利息金额及流水
			RepayAdvance totalInt = new RepayAdvance() ;
			//取下一条账单信息
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());

			//一整期本金、利息插一次还款计划表
			if( !period.equals(lnsBill.getPeriod()) && !period.equals(0) )
			{
				try {
					DbAccessUtil.execute("Lnsrpyplan.insert", lnsRpyPlan);
				}
				catch (FabSqlException e)
				{
					if (!ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
						throw new FabException(e, "SPS100", "lnsrpyplan");
					}
				}
			}

			/* 还款计划表准备数据 */
			//机构
			lnsRpyPlan.setBrc(ctx.getBrc());
			//账号
			lnsRpyPlan.setAcctno(la.getContract().getReceiptNo());

			//方式
			lnsRpyPlan.setRepayway("REPAY");
			//剩余金额
			lnsRpyPlan.setBalance(lnsBill.getBalance().getVal());

			Map<Integer,String>  intExist = new HashMap<Integer,String>();
			//list为利息账单且利息账单标志为真
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType())
				&& intFlag )
			{
				intExist.put(lnsBill.getPeriod(), "true");

				/* 还款计划表准备数据 */
				//期数
				lnsRpyPlan.setRepayterm(lnsBill.getPeriod());
				//本期开始日
				lnsRpyPlan.setRepayintbdate(lnsBill.getStartDate());
				//本期结束日
				lnsRpyPlan.setRepayintedate(lnsBill.getEndDate());
				//账单开始日
				lnsRpyPlan.setRepayownbdate(lnsBill.getEndDate());
				//账单结束日
				lnsRpyPlan.setRepayownedate(lnsBill.getRepayendDate());
				//实际还款日
				lnsRpyPlan.setActrepaydate(lnsBill.getEndDate());
				//还款日
				//lnsRepayPlan.setTermretdate(ctx.getTranDate());
				//应还利息金额
				lnsRpyPlan.setTermretint(lnsBill.getBillAmt().getVal());

				String endDate=ctx.getTranDate();

				if(la.getContract().getContractStartDate().equals(ctx.getTranDate()) &&
						"3".equals(la.getInterestAgreement().getAdvanceFeeType())){
					endDate = CalendarUtil.nDaysAfter(ctx.getTranDate(), 1).toString("yyyy-MM-dd");
				}

				//(当前期)当前期利息+合同余额够结清
				if( CalendarUtil.after(endDate, lnsBill.getStartDate()) &&
						!CalendarUtil.after(ctx.getTranDate(), lnsBill.getEndDate()) &&
						!remainAmt.sub( lnsBill.getBillBal().add(la.getContract().getBalance()) ).isNegative() )
				{
					//账单余额0
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清标志（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//结清日期（还款日）
					lnsBill.setSettleDate(ctx.getTranDate());

					//已还利息（利息账单金额）
					totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					totalInt.setRepayterm(lnsBill.getPeriod());
					nintAdvance.add(totalInt);

					//账单余额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());

					//不再生成利息
					intFlag = false;
					//结清
					cleanFlag = true;
				}
				//(未来期)合同余额够结清
				else if(!CalendarUtil.after("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")) ? endDate : ctx.getTranDate(), lnsBill.getStartDate()) &&
						!remainAmt.sub( la.getContract().getBalance()).isNegative() )
				{
					//不再生成利息
					intFlag = false;
					//结清
					cleanFlag = true;
					continue;
				}
				//不够结清
				else
				{
					//不够还息
					if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
					{
						//剩余金额不够还break
						if(remainAmt.isZero())
						{
							//取到当期本金对应的剩余金额
							lnsRpyPlan.setBalance(billListIterator.next().getBalance().getVal());
							break;
						}

						//账单余额
						lnsBill.setBillBal(new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

						//已还利息（剩余金额）
						totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
						totalInt.setBillTrandate(ctx.getTranDate());
						totalInt.setBillSerseqno(ctx.getSerSeqNo());
						totalInt.setBillTxseq(lnsBill.getTxSeq());
						totalInt.setRepayterm(lnsBill.getPeriod());
						nintAdvance.add(totalInt);

						//金额减完为0
						remainAmt.selfSub(remainAmt.getVal());

						//不再生成利息
						intFlag = false;
					}
					else
					{
						//账单状态（结清）
						lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
						//账单余额
						lnsBill.setBillBal(new FabAmount(0.00));
						//结清日期
						lnsBill.setSettleDate(ctx.getTranDate());

						//已还利息（账单金额）
						totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
						totalInt.setBillTrandate(ctx.getTranDate());
						totalInt.setBillSerseqno(ctx.getSerSeqNo());
						totalInt.setBillTxseq(lnsBill.getTxSeq());
						totalInt.setRepayterm(lnsBill.getPeriod());
						nintAdvance.add(totalInt);

						//剩余金额-账单金额
						remainAmt.selfSub(lnsBill.getBillAmt().getVal());
					}
				}

				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//2019-11-01  代偿未来期改标志
				if( VarChecker.asList("2","3","4").contains(compensateFlag) )
					lnsBill.setBillProperty("2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ("3".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL));

                //核销优化登记cancelflag，“3”
                if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
                    lnsBill.setCancelFlag("3");
                }
				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}
				 if(  !VarChecker.isEmpty(lnsBill.getTermFreeInterest())
						&& lnsBill.getTermFreeInterest().isPositive()){
					 //现金贷免息税金表生成记录
					 AccountingModeChange.saveFreeInterestTax(ctx,la.getContract().getReceiptNo(),lnsBill);
					 //主文件动态表对应免息金额减少
					 la.getBasicExtension().getFreeInterest().selfSub(lnsBill.getTermFreeInterest());
					 AccountingModeChange.updateBasicDynMX(ctx,la.getBasicExtension().getFreeInterest().getVal(),la.getContract().getReceiptNo(),ctx.getTranDate(),lnsBill);
				}

				//修改上次结息日为账单结束日
				basicinfo.setLastIntDate(lnsBill.getEndDate());

				//计税
				LoanAcctChargeProvider.add(lnsBill,
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//写事件
//				LoanAcctChargeProvider.event(lnsBill,
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(lnsBill.getBillAmt()));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
			}


			//list为本金账单且本金账单标志为真
			if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType())
					&& prinFlag )
			{
				FabAmount lastBill = new FabAmount(0.00);
				/* 还款计划表准备数据 */
				//应还本金
				lnsRpyPlan.setTermretprin(lnsBill.getBillAmt().getVal());
				//剩余金额
				lnsRpyPlan.setBalance(lnsBill.getBalance().getVal());


				if( 0 == la.getRateAgreement().getNormalRate().getRate().compareTo(BigDecimal.ZERO)  ||
				   la.getInterestAgreement().getIsCalInt().equals(ConstantDeclare.ISCALINT.ISCALINT_NO) ||
				   !intExist.containsKey(lnsBill.getPeriod()) )
				{
					/* 还款计划表准备数据 */
					//期数
					lnsRpyPlan.setRepayterm(lnsBill.getPeriod());
					//本期开始日
					lnsRpyPlan.setRepayintbdate(lnsBill.getStartDate());
					//本期结束日
					lnsRpyPlan.setRepayintedate(lnsBill.getEndDate());
					//账单开始日
					lnsRpyPlan.setRepayownbdate(lnsBill.getEndDate());
					//账单结束日
					lnsRpyPlan.setRepayownedate(lnsBill.getRepayendDate());
					//实际还款日
					lnsRpyPlan.setActrepaydate(lnsBill.getEndDate());
					//还款日
					//lnsRepayPlan.setTermretdate(ctx.getTranDate());
				}

				//剩余本金能结清未来期
				if(  (0 == la.getRateAgreement().getNormalRate().getRate().compareTo(BigDecimal.ZERO) || listBill.size() == 1 || !intExist.containsKey(lnsBill.getPeriod()))  &&
					!remainAmt.sub( la.getContract().getBalance()).isNegative() )
				{
					//结清
					cleanFlag = true;
				}

				//剩余金额不够还本
				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					//账单余额（账单金额-剩余金额）
					lnsBill.setBillBal( new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

					//已还本金（剩余金额）
					totalPrin.setBillAmt(new FabAmount(remainAmt.getVal()));
					totalPrin.setBillTrandate(ctx.getTranDate());
					totalPrin.setBillSerseqno(ctx.getSerSeqNo());
					totalPrin.setBillTxseq(lnsBill.getTxSeq());
					totalPrin.setRepayterm(lnsBill.getPeriod());
					if( totalPrin.getBillAmt().isPositive() )
						prinAdvance.add(totalPrin);
					remainAmt.selfSub(remainAmt.getVal());

					//不再生成利息
					intFlag = false;
					//不再生成本金
					prinFlag = false;
				}
				//剩余金额够还本
				else
				{
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额0
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());

					//能提前结清少不累计当期（要少累计一期）
					if(!cleanFlag)
					{
						//已还本金（账单金额）
						totalPrin.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
						totalPrin.setBillTrandate(ctx.getTranDate());
						totalPrin.setBillSerseqno(ctx.getSerSeqNo());
						totalPrin.setBillTxseq(lnsBill.getTxSeq());
						totalPrin.setRepayterm(lnsBill.getPeriod());
						if( totalPrin.getBillAmt().isPositive() )
							prinAdvance.add(totalPrin);
					}

					//剩余本金-账单金额
					lastBill.setVal(lnsBill.getBillAmt().getVal());
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());

					if("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no"))) {
                        if (!remainAmt.isPositive())
                            //不再生成本金
                            prinFlag = false;
                    }
				}
				//本金金额累加
				contAmt.selfAdd(lnsBill.getBillAmt());

				//提前结清生成一整期本金
				if( cleanFlag )
				{
					//转换成数据库结构
					TblLnsbill tblBill = BillTransformHelper.convertToTblLnsBill(la,lnsBill,ctx);
					tblBill.setPrinbal(0.00);
					//插辅助表
					interestBillPlan(ctx,txseq,la,tblBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);

					//插还款计划登记簿
					LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"ADVANCE");

					//账单金额（合同余额）
					lnsBill.setBillAmt(la.getContract().getBalance());
					//结束日期（合同到期日）
					lnsBill.setEndDate(lnsBill.getCurendDate());
					//应还款日（合同到期日）
					lnsBill.setRepayendDate(la.getContract().getContractEndDate());
					//上次结息日（合同到期日）
					lnsBill.setIntendDate(la.getContract().getContractEndDate());
					//状态日期
					lnsBill.setStatusbDate(ctx.getTranDate());
					//上次交易日
					lnsBill.setLastDate(ctx.getTranDate());
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额0
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());
					//账单对应剩余本金
					lnsBill.setPrinBal(new FabAmount(0.00));
					//账单属性(还款)
					lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
					//2019-11-01  代偿未来期改标志
					if( VarChecker.asList("2","3","4").contains(compensateFlag) )
						lnsBill.setBillProperty("2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ("3".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL));
					//结清状态3-结清
					settleFlag = "3";

					//已还本金金额（账单金额）
					totalPrin.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					totalPrin.setBillTrandate(ctx.getTranDate());
					totalPrin.setBillSerseqno(ctx.getSerSeqNo());
					totalPrin.setBillTxseq(lnsBill.getTxSeq());
					totalPrin.setRepayterm(lnsBill.getPeriod());
					if( totalPrin.getBillAmt().isPositive() )
						prinAdvance.add(totalPrin);

					//剩余金额-账单金额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());

					//2019-12-03 提前结清 多减的一期加回来
					if(remainAmt.isNegative())
						remainAmt.selfAdd(lastBill);

					//金额大于0写账单表
					if( lnsBill.getBillAmt().isPositive() )
					{
						lnsBillStatistics.writeBill(la,ctx,lnsBill);
					}

					//主文件状态（结清）
					//basicinfo.setLoanStat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
					//累计本金金额（合同金额）
					contAmt.setVal(la.getContract().getBalance().getVal());
					break;
				}
				else
				{
					//期数加1
					terms++;
					//上次交易日
					if( !lnsBill.getBillAmt().sub(lnsBill.getBillBal()).isZero())
						lnsBill.setLastDate(ctx.getTranDate());
					//账单属性(还款)
					lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
					//2019-11-01  代偿未来期改标志，借新环节改标记【20200515】
					if( VarChecker.asList("2","3","4").contains(compensateFlag) )
						lnsBill.setBillProperty("2".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_COMPEN : ("3".equals(compensateFlag) ? ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SWITCH : ConstantDeclare.BILLPROPERTY.BILLPROPERTY_SELL));
					//金额大于0写账单表
					if( lnsBill.getBillAmt().isPositive() )
					{
						lnsBillStatistics.writeBill(la,ctx,lnsBill);
					}
				}

				//主文件修改上次结本日
				basicinfo.setLastPrinDate(lnsBill.getEndDate());

			}

			//取当期期数
			period = lnsBill.getPeriod();

			//未来期钱不够提前退出
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				!CalendarUtil.after(ctx.getTranDate(), lnsBill.getStartDate()) &&
				!remainAmt.isPositive() )
				break;
		}


		//循环结束插最后一期还款计划
		try {
			DbAccessUtil.execute("Lnsrpyplan.insert", lnsRpyPlan);
		}
		catch (FabSqlException e)
		{
			if (!ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e, "SPS100", "lnsrpyplan");
			}
		}

		//2019-11-29 传结清标志但未结清，报错
		if( "1".equals(repaySettle) &&
			(!"3".equals(settleFlag) || !remainAmt.isZero()) )
			throw new FabException("LNS202");

		//更新主文件
		basicinfo.setIntTerm(terms);
		basicinfo.setPrinTerm(terms);
		basicinfo.setTranAmt(new FabAmount(contAmt.getVal()));
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);


		//返回结清标志
		return settleFlag;
	}


	/**
     * 功能：非标提前还款当前期或提前期（还款）生成账单
     * 描述：非标方式先息后本、先本后息、有本有息方式提前还款
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	txseq     				子序号
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics        试算结构
     * @param 	interestFlag 			利息标志（ONLYINT只还息、ONLYPRIN只还本、INTPRIN还本还息）
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
	 * @throws InterruptedException
     *
     * @since  1.2.13
     */
	//非标提前还款SUN  billInfoList当前期账单
	public static String nonStdRepaymentBill(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag, Integer totalTerms) throws FabException, InterruptedException
	{
		int	i = 0;
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);

		//用于记录生成账单的期数
		int terms = 0;
		//用于记录是否同一期账单
		Integer period = 0;
		//能否提前结清
		boolean cleanFlag = false;
		//结清标志 1-未结清 3-结清
		String settleFlag = "1";
		//提前还本标志 0-未还 1-已还
		String forPrinFlag = "0";

		//
		String endFlag = "0";

		//累计的本金金额
		FabAmount contAmt = new FabAmount(0.00);
		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();


		//取未来期账单list
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();

		List<LnsBill> hisBill = lnsBillStatistics.getHisBillList();
		ListIterator<LnsBill> hisListIterator = hisBill.listIterator();


		Map<String, Object> upbillmap = new HashMap<String, Object>();
		//遍历未来期账单
		while(billListIterator.hasNext())
		{
			//提前还款还本金金额及流水
			RepayAdvance totalPrin = new RepayAdvance();
			//提前还款还利息金额及流水
			RepayAdvance totalInt = new RepayAdvance() ;
			//取下一条账单信息
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());

			//先本后息本
			if(ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(lnsbasicinfo.getRepayway()) && lnsBill.getPeriod().equals(la.getUserDefineAgreement().size())
					&& "PRIN".equals(lnsBill.getBillType()))
			{
				while(hisListIterator.hasNext())
				{
					//提前还款还利息金额及流水
					RepayAdvance totalInt1 = new RepayAdvance() ;

					if( !remainAmt.isPositive() )
					{
						return settleFlag;
					}

					LnsBill lnsBillInt = hisListIterator.next();
					/*if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL).contains(lnsBillInt.getBillStatus())){
						LoanAcctChargeProvider.add(lnsBillInt, ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ, null);
						//结息税金
						LoanAcctChargeProvider.event(lnsBillInt, ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ, TaxUtil.calcVAT(lnsBill.getBillAmt()));
					}*/
					if(ConstantDeclare.BILLTYPE.BILLTYPE_NINT.equals(lnsBillInt.getBillType()))
					{

						upbillmap.put("actrandate", ctx.getTranDate());
						upbillmap.put("tranamt", remainAmt.getVal());

						upbillmap.put("trandate", lnsBillInt.getTranDate());
						upbillmap.put("serseqno", lnsBillInt.getSerSeqno());
						upbillmap.put("txseq", lnsBillInt.getTxSeq());

						Map<String, Object> repaymap;
						try {
							repaymap = DbAccessUtil.queryForMap("CUSTOMIZE.update_lnsbill_repay", upbillmap);
						} catch (FabSqlException e) {
							throw new FabException(e, "SPS103", "lnsbill");
						}
						if (repaymap == null) {
							throw new FabException("SPS104", "lnsbill");
						}

						Double minAmt = Double.parseDouble(repaymap.get("minamt").toString());
						LoggerUtil.debug("minAmt:" + minAmt);
						if (minAmt.equals(0.00)) {
							LoggerUtil.debug("该账单金额已经为零");
							continue;
						}
						FabAmount amount = new FabAmount(minAmt);
						remainAmt.selfSub(amount);
						totalInt1.setBillAmt(new FabAmount(amount.getVal()));
						totalInt1.setBillTrandate(lnsBillInt.getTranDate().toString());
						totalInt1.setBillSerseqno(lnsBillInt.getSerSeqno());
						totalInt1.setBillTxseq(lnsBillInt.getTxSeq());
						totalInt1.setBillStatus(lnsBillInt.getBillStatus());
						nintAdvance.add(totalInt1);

					}
				}
			}
			//lnsBill.getRepayDateInt();

			//list为利息账单且利息账单标志为真
			if( !VarChecker.isEmpty(lnsBill.getRepayDateInt()) &&   //add 2018-01-25
				VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType())
				&& (i == 0 && !lnsBill.getStartDate().equals(ctx.getTranDate())   ) ) //|| la.getContract().getStartIntDate().equals(lnsBill.getStartDate())
			{
				i++;
				lnsBill.setBillAmt(lnsBill.getRepayDateInt());

				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					//账单余额
					lnsBill.setBillBal(new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

					//已还利息（剩余金额）
					totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					nintAdvance.add(totalInt);

					//金额减完为0
					remainAmt.selfSub(remainAmt.getVal());
				}
				else
				{
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());

					//已还利息（账单金额）
					totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					nintAdvance.add(totalInt);

					//剩余金额-账单金额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());
				}


				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);

				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY); //账单属性(还款)
					lnsBill.setEndDate(ctx.getTranDate());
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//修改上次结息日为账单结束日
				basicinfo.setLastIntDate(ctx.getTranDate());

				//计税
				LoanAcctChargeProvider.add(lnsBill,
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//写事件
//				LoanAcctChargeProvider.event(lnsBill,
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(lnsBill.getBillAmt()));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);

			}
			//list为本金账单且本金账单标志为真
			if ( 	!ConstantDeclare.REPAYWAY.REPAYWAY_XXHB.equals(la.getWithdrawAgreement().getRepayWay()) &&
					VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()))
			{

				forPrinFlag = "1";

				//2018-12-14 非标自定义迁移不允许提前还款（本金）
				if( !remainAmt.sub( la.getContract().getBalance()).isNegative())
					endFlag = "1";


				//剩余本金能结清未来期
/*				if( 0 == la.getRateAgreement().getNormalRate().getRate().compareTo(BigDecimal.ZERO)  &&
					!remainAmt.sub( la.getContract().getBalance()).isNegative() )
				{
					//结清
					cleanFlag = true;
				}*/ //SUN先不考虑还清本金的情况 因为有每期从合同起息日计息的

				//剩余金额不够还本
				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					//账单余额（账单金额-剩余金额）
					lnsBill.setBillBal( new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));


					//已还本金（剩余金额）
					totalPrin.setBillAmt(new FabAmount(remainAmt.getVal()));
					totalPrin.setBillTrandate(ctx.getTranDate());
					totalPrin.setBillSerseqno(ctx.getSerSeqNo());
					totalPrin.setBillTxseq(lnsBill.getTxSeq());
					prinAdvance.add(totalPrin);
					remainAmt.selfSub(remainAmt.getVal());

				}
				//剩余金额够还本
				else
				{
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额0
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());

					//能提前结清少不累计当期（要少累计一期）
					if(!cleanFlag)
					{
						//已还本金（账单金额）
						totalPrin.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
						totalPrin.setBillTrandate(ctx.getTranDate());
						totalPrin.setBillSerseqno(ctx.getSerSeqNo());
						totalPrin.setBillTxseq(lnsBill.getTxSeq());
						prinAdvance.add(totalPrin);
					}

					//剩余本金-账单金额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());
				}
				//本金金额累加
				//contAmt.selfAdd(lnsBill.getBillAmt());

				{
					//期数加1
					terms++;
					//上次交易日
					lnsBill.setIntendDate(ctx.getTranDate());
					lnsBill.setLastDate(ctx.getTranDate());
					//账单属性(还款)
					lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
					//金额大于0写账单表
					if( lnsBill.getBillAmt().isPositive() )
					{
						lnsBillStatistics.writeBill(la,ctx,lnsBill);
					}
				}
				//主文件修改上次结本日
				basicinfo.setLastPrinDate(lnsBill.getEndDate());
				contAmt.selfAdd(lnsBill.getBillAmt());

			}

			basicinfo.setLastIntDate(ctx.getTranDate());
			//取当期期数
			period = lnsBill.getPeriod();



			if(period.equals(totalTerms) && lnsBill.getBillBal().isZero())
				settleFlag = "3";
			//未来期钱不够提前退出
			if( !remainAmt.isPositive() )
				break;
			//i++;
		}
		if(!VarChecker.isEmpty(lnsbasicinfo.getFlag1()) && lnsbasicinfo.getFlag1().contains("A"))
		//2018-12-14 非标自定义迁移不允许提前还款（利息）
		if(	la.getContract().getFlag1().contains("2") &&
			"0".equals(forPrinFlag) )
			throw new FabException("LNS100");

		//先息后本处理
		if( ConstantDeclare.REPAYWAY.REPAYWAY_XXHB.equals(la.getWithdrawAgreement().getRepayWay()) &&
			 remainAmt.isPositive())
		{
			//本金账单结构
			TblLnsbill lnsPrinBill = new TblLnsbill();
			RepayAdvance totalPrin = new RepayAdvance();
			Integer seqno = txseq;

			//合同余额>还款余额
			if( la.getContract().getBalance().sub(remainAmt.getVal()).isPositive() )
			{
				//账单金额为还款余额
				lnsPrinBill.setBillamt( remainAmt.getVal() );
			}
			//合同余额<=还款余额，可以结清
			else
			{
				//账单金额为合同余额
				lnsPrinBill.setBillamt( la.getContract().getBalance().getVal() );
				//结清标志3-结清
				settleFlag = "3";
			}

			//本金账单赋值
			//账单余额0
			lnsPrinBill.setBillbal( 0.00 );
			//账务日期
			lnsPrinBill.setTrandate(Date.valueOf(ctx.getTranDate()));
			//流水号
			lnsPrinBill.setSerseqno(ctx.getSerSeqNo());
			//子序号
			lnsPrinBill.setTxseq(++seqno);
			//账号
			lnsPrinBill.setAcctno(la.getContract().getReceiptNo());
			//机构
			lnsPrinBill.setBrc(ctx.getBrc());
			//账单类型（本金）
			lnsPrinBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);
			//账单金额
			lnsPrinBill.setBillamt(lnsPrinBill.getBillamt());
			//账单余额
			lnsPrinBill.setBillbal(lnsPrinBill.getBillbal());
			//上日余额
			//lnsPrinBill.setLastbal(0.00);
			//上笔日期
			lnsPrinBill.setLastdate(ctx.getTranDate());
			//账单对应剩余本金金额（合同余额-账单金额）
			lnsPrinBill.setPrinbal(la.getContract().getBalance().sub(new FabAmount(lnsPrinBill.getBillamt())).getVal());
			//账单利率（正常利率）
			lnsPrinBill.setBillrate(0.00);
			//首次还款账单开始日为开户日
			if( la.getContract().getRepayPrinDate().isEmpty() )
			{
				lnsPrinBill.setBegindate(la.getContract().getContractStartDate());
			}
			//非首次账单开始日为上次结本日
			else
			{
				lnsPrinBill.setBegindate(la.getContract().getRepayPrinDate());
			}

			//账单结束日期
			lnsPrinBill.setEnddate(ctx.getTranDate());
			//期数（利息期数）
			lnsPrinBill.setPeriod(period);
			//账单应还款日期
			lnsPrinBill.setRepayedate(la.getContract().getContractEndDate());
			//账单结清日期
			lnsPrinBill.setSettledate(la.getContract().getContractEndDate());
			//利息记至日期
			lnsPrinBill.setIntedate(la.getContract().getContractEndDate());
			//当期到期日
			lnsPrinBill.setCurenddate(la.getContract().getContractEndDate());
			//还款方式
			lnsPrinBill.setRepayway(la.getWithdrawAgreement().getRepayWay());
			//币种
			lnsPrinBill.setCcy(la.getContract().getCcy().getCcy());
			//账单状态（正常）
			lnsPrinBill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
			//账单状态开始日期
			lnsPrinBill.setStatusbdate(la.getContract().getContractEndDate());
			//账单属性(还款)
			lnsPrinBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);									//账单属性(还款)
			//利息入账标志
			lnsPrinBill.setIntrecordflag("YES");
			//账单是否作废标志
			lnsPrinBill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
			if( new FabAmount(lnsPrinBill.getBillbal()).isZero() )
			{
				//账单结清标志（结清）
				lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			}
			else
			{
				//账单结清标志（未结清）
				lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
			}

			//本金账单金额大于0
			if( lnsPrinBill.getBillamt() > 0.00 )
			{
				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsPrinBill);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}
				//修改主文件上次结本日
				basicinfo.setLastPrinDate(ctx.getTranDate());
				//修改主文件合同余额
				basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));

				Map<String, Object> lnsnonstdplanMap = new HashMap<String, Object>();
				//当期本金
				lnsnonstdplanMap.put("termprin", la.getContract().getBalance().sub(basicinfo.getTranAmt()).getVal());
				//借据号
				lnsnonstdplanMap.put("acctno", la.getContract().getReceiptNo());
				//机构
				lnsnonstdplanMap.put("openbrc", ctx.getBrc());
				//利息结束日期
				lnsnonstdplanMap.put("intedate", lnsPrinBill.getCurenddate());
				int ct = 0;
				try {
					ct = DbAccessUtil.execute("CUSTOMIZE.update_lnsnonstdplan_bill", lnsnonstdplanMap);
				} catch (FabException e) {
					throw new FabException(e, "SPS102", "lnsnonstdplan");
				}
				if (ct != 1) {
					throw new FabException("LNS041", la.getContract().getReceiptNo());
				}
			}




			//已还本金（账单金额-账单余额）
			totalPrin.setBillAmt(new FabAmount(new FabAmount(lnsPrinBill.getBillamt()).sub(new FabAmount(lnsPrinBill.getBillbal())).getVal()));
			totalPrin.setBillTrandate(ctx.getTranDate());
			totalPrin.setBillSerseqno(ctx.getSerSeqNo());
			totalPrin.setBillTxseq(lnsPrinBill.getTxseq());
			prinAdvance.add(totalPrin);



		}

		//更新主文件
		if(!ConstantDeclare.REPAYWAY.REPAYWAY_XXHB.equals(la.getWithdrawAgreement().getRepayWay()))
			basicinfo.setTranAmt(new FabAmount(contAmt.getVal()));

		//设置利息期数
		basicinfo.setIntTerm(terms);
		//设置本金期数
		basicinfo.setPrinTerm(terms);
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);

		//2018-12-14 非标自定义迁移不允许提前还款（本金）
		if( la.getContract().getFlag1().contains("2")  && !"1".equals(endFlag))
		{
			throw new FabException("LNS064");
		}


		//返回结清标志
		return settleFlag;
	}


	/**
     * 功能：非标自定义不规则提前还款当前期或提前期（还款）生成账单
     * 描述：非标自定义不规则方式先息后本、先本后息、有本有息方式提前还款
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics        试算结构
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
	 * @throws InterruptedException
     *
     * @since  1.2.13
     */

	//非标自定义不规则提前还款（还款）生成账单
	public static String nonStdIrregularRepaymentBill(LoanAgreement loanAgreement,TranCtx ctx, TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics) throws FabException, InterruptedException
	{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
		//上次节本结息日
		String lastDate = "";
		//用于记录生成账单的期数
		int terms = 0;
		//利息账单标志
		boolean intFlag = true;
		//本金账单标志
		boolean prinFlag = true;
		//能否提前结清
		boolean cleanFlag = false;
		//结清标志 1-未结清 3-结清
		String settleFlag = "1";
		Integer curPeriod=0;

		//累计的本金金额
		FabAmount contAmt = new FabAmount(0.00);
		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//取未来期账单list
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();

		//遍历未来期账单
		while(billListIterator.hasNext())
		{

			//提前还款还本金金额及流水
			RepayAdvance totalPrin = new RepayAdvance();
			//提前还款还利息金额及流水
			RepayAdvance totalInt = new RepayAdvance() ;
			//取下一条账单信息
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());
			//list为利息账单且利息账单标志为真
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
				intFlag &&
				(curPeriod.intValue() == lnsBill.getPeriod()  || remainAmt.isPositive()) )
			{
				//不够还息
				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					//剩余金额不够还break
					if(remainAmt.isZero())
					{
						break;
					}

					//账单余额
					lnsBill.setBillBal(new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

					//已还利息（剩余金额）
					totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					nintAdvance.add(totalInt);

					//金额减完为0
					remainAmt.selfSub(remainAmt.getVal());

					//不再生成利息
					intFlag = false;
				}
				else
				{
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());

					//已还利息（账单金额）
					totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					nintAdvance.add(totalInt);

					//剩余金额-账单金额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());
				}


				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);

				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//修改上次结息日为账单结束日
				lastDate = lnsBill.getEndDate();
//				basicinfo.setLastIntDate(lnsBill.getEndDate());

				//计税
				LoanAcctChargeProvider.add(lnsBill,
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//写事件
//				LoanAcctChargeProvider.event(lnsBill,
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(lnsBill.getBillAmt()));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
				curPeriod = lnsBill.getPeriod();
			}


			//list为本金账单且本金账单标志为真
			if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				 prinFlag &&
				 (curPeriod.intValue() == lnsBill.getPeriod()  || remainAmt.isPositive()) )
			{
				//剩余金额不够还本
				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					//账单余额（账单金额-剩余金额）
					lnsBill.setBillBal( new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

					//已还本金（剩余金额）
					totalPrin.setBillAmt(new FabAmount(remainAmt.getVal()));
					totalPrin.setBillTrandate(ctx.getTranDate());
					totalPrin.setBillSerseqno(ctx.getSerSeqNo());
					totalPrin.setBillTxseq(lnsBill.getTxSeq());
					prinAdvance.add(totalPrin);
					remainAmt.selfSub(remainAmt.getVal());

					//不再生成利息
					intFlag = false;
					//不再生成本金
					prinFlag = false;
				}
				//剩余金额够还本
				else
				{
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额0
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());

					//能提前结清少不累计当期（要少累计一期）
					if(!cleanFlag)
					{
						//已还本金（账单金额）
						totalPrin.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
						totalPrin.setBillTrandate(ctx.getTranDate());
						totalPrin.setBillSerseqno(ctx.getSerSeqNo());
						totalPrin.setBillTxseq(lnsBill.getTxSeq());
						prinAdvance.add(totalPrin);
					}

					//剩余本金-账单金额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());
				}
				//本金金额累加
				contAmt.selfAdd(lnsBill.getBillAmt());

				//期数加1
				terms++;
				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//主文件修改上次结本日
				lastDate = lnsBill.getEndDate();
//				basicinfo.setLastPrinDate(lnsBill.getEndDate());
				curPeriod = lnsBill.getPeriod();
			}

			//未来期钱不够提前退出
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				!CalendarUtil.after(ctx.getTranDate(), lnsBill.getStartDate()) &&
				!remainAmt.isPositive() )
				break;
		}

		//更新主文件
		basicinfo.setIntTerm(terms);
		basicinfo.setPrinTerm(terms);
		basicinfo.setTranAmt(new FabAmount(contAmt.getVal()));
		basicinfo.setLastPrinDate(lastDate);
		basicinfo.setLastIntDate(lastDate);
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);


		//返回结清标志
		return settleFlag;
	}



	/**
     * 功能：非标利息减免生成账单
     * 描述：非标方式先息后本、先本后息、有本有息方式非标利息减免
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	txseq     				子序号
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     * @param 	interestFlag 			利息标志（ONLYINT只还息、ONLYPRIN只还本、INTPRIN还本还息）
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
	 * @throws InterruptedException
     *
     * @since  1.2.13
     */
	//非标利息减免SUN  billInfoList当前期账单
	public static FabAmount nonStdIntDedu(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag ) throws FabException, InterruptedException
	{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);

		//用于记录生成账单的期数
		int terms = 0;

		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//取未来期账单list
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();

		//遍历未来期账单
		while(billListIterator.hasNext())
		{
			RepayAdvance totalInt = new RepayAdvance() ;
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
				!VarChecker.isEmpty(lnsBill.getRepayDateInt())	)
			{
				lnsBill.setBillAmt(lnsBill.getRepayDateInt());

				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					//账单余额
					lnsBill.setBillBal(new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

					//已还利息（剩余金额）
					totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					nintAdvance.add(totalInt);

					//金额减完为0
					remainAmt.selfSub(remainAmt.getVal());
				}
				else
				{
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());

					//已还利息（账单金额）
					totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					nintAdvance.add(totalInt);

					//剩余金额-账单金额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());
				}

				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);

				//先本后息期数取最后一期
				if(ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(lnsbasicinfo.getRepayway()))
				{
					//terms = lnsBillStatistics.getPrinTotalPeriod();
					//terms = lnsbasicinfo.getPrinterms();
					lnsBill.setPeriod(terms);
				}
				//else
					terms = lnsBill.getPeriod();

				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBill.setEndDate(ctx.getTranDate());
					lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//计税
				LoanAcctChargeProvider.add(lnsBill,
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//写事件
//				LoanAcctChargeProvider.event(lnsBill,
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(lnsBill.getBillAmt()));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
				break;
			}
		}

		//修改上次结息日为账单结束日
		basicinfo.setLastIntDate(ctx.getTranDate());
		basicinfo.setIntTerm(terms);
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);

		return remainAmt;
	}


	/**
     * 功能：非等本等息指定还款顺序
     * 描述：指定本金、利息、罚息的还款先后顺序进行还款
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	txseq     				子序号
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     * @param 	interestFlag 			利息标志（ONLYINT只还息、ONLYPRIN只还本、INTPRIN还本还息）
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
	 * @throws InterruptedException
     *
     * @since  1.1.9
     */
	public static String specialOrderInstBill(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag) throws FabException
	{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);


		Integer seqno = txseq;
		//本金标志（还到本金登记还款计划登记簿）
		Boolean prinFlag = false;
		//还完利息是否有钱还本金
		Boolean amtFlag = true;
		//结清标志 （1-未结清 3-结清）
		String settleFlag = "1";


		//提前还款还本金金额及流水
		RepayAdvance totalPrin = new RepayAdvance();
		//提前还款还利息金额及流水
		RepayAdvance totalInt = new RepayAdvance();

		//利息账单结构
		TblLnsbill lnsIntBill = new TblLnsbill();
		//本金账单结构
		TblLnsbill lnsPrinBill = new TblLnsbill();
		//提前还款后剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());



		//第一次还款取计息日作为账单开始日，非第一次还款取上次结息日作为账单开始日
		if( la.getContract().getRepayIntDate().isEmpty() )
		{
			lnsIntBill.setBegindate(la.getContract().getStartIntDate());

		}else{
			lnsIntBill.setBegindate(la.getContract().getRepayIntDate()); 			//账单开始日期
		}

		//计算本期利息（通过试算取当期利息账单的计提利息金额）
		FabAmount intAmt = new FabAmount(0.00);
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();
		LnsBill lnsBill = new LnsBill();
		while(billListIterator.hasNext())
		{
			lnsBill = billListIterator.next();
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
				!VarChecker.isEmpty(lnsBill.getRepayDateInt())	)
			{
				intAmt = lnsBill.getRepayDateInt();
				break;
			}
		}

		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//生成账单期数信息参数
		PrepayTermSupporter prepayTermSupporter = LoanSupporterUtil.getPrepayRepaywaySupporter(la.getWithdrawAgreement().getRepayWay());
		prepayTermSupporter.setLoanAgreement(la);
		prepayTermSupporter.genVal(prepayTermSupporter.genMoment(la.getContract().getContractStartDate(),la.getContract().getRepayIntDate() ,la.getContract().getRepayPrinDate(),ctx.getTranDate()),
								   prepayTermSupporter.genThing(intAmt, tranAmt));
		//根据配置设置期数
		Integer currentTerm = prepayTermSupporter.genUseTerm(la.getContract().getCurrIntPeriod());
		//期数是否累加  2019-10-12
		boolean isAccumTerm = false;
		if( "SPECPRIN".equals(interestFlag) )
		{
			if(intAmt.isPositive())
				isAccumTerm = true;
			else
			{
				if( la.getContract().getRepayPrinDate().equals(la.getContract().getRepayIntDate()) &&
					ctx.getTranDate().equals(la.getContract().getRepayPrinDate()))
					isAccumTerm = false;
				else
					isAccumTerm = true;
			}
		}
		else
		{
			isAccumTerm = prepayTermSupporter.isAccumterm();
		}
		//是否插本金账单计划表
		boolean isInstPrinBillPlan = prepayTermSupporter.isInsertPrinBillPlan();
		//是否插利息账单计划表
		boolean isInstIntBillPlan = prepayTermSupporter.isInsertIntBillPlan();


		//生成利息账单
		//指定还息生成利息账单
		if( intAmt.isPositive() )
		{
			//只还息处理
			if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYINT) )
			{
				//不生成本金账单
				amtFlag = false;

				//利息金额>=还款金额
				if( intAmt.getVal() >= tranAmt.getVal() )
				{
					//利息余额=利息金额-还款金额
					lnsIntBill.setBillbal( intAmt.sub(tranAmt).getVal() );

				}
				//利息金额<还款金额，
				else
				{
					//剩余金额=还款金额-利息金额
					//remainAmt.setVal( tranAmt.sub(intAmt).getVal() );
					//账单结清日期赋值
					lnsIntBill.setSettledate(ctx.getTranDate());
					//账单余额0结清
					lnsIntBill.setBillbal( 0.00 );
				}
			}
			//只还本处理
			else
			{
				remainAmt.setVal( tranAmt.getVal() );
				lnsIntBill.setBillbal( intAmt.getVal() );
			}


			//账单金额大于0 利息账单赋值
			if( intAmt.isPositive())
			{
				//账务日期
				lnsIntBill.setTrandate(Date.valueOf(ctx.getTranDate()));
				//流水号
				lnsIntBill.setSerseqno(ctx.getSerSeqNo());
				//子序号
				lnsIntBill.setTxseq(++seqno);
				//账号
				lnsIntBill.setAcctno(la.getContract().getReceiptNo());
				//机构
				lnsIntBill.setBrc(ctx.getBrc());
				//账单类型（利息）
				lnsIntBill.setBilltype(lnsBill.getBillType());
				//期数（利息期数）
				lnsIntBill.setPeriod(currentTerm);
				//账单金额
				lnsIntBill.setBillamt(intAmt.getVal());
				//账单余额
				lnsIntBill.setBillbal(lnsIntBill.getBillbal());
				//上日余额
				//lnsbill.setLastbal(0.00);
				//上笔日期
				lnsIntBill.setLastdate(ctx.getTranDate());
				//账单对应金额（当期本金金额）
				lnsIntBill.setPrinbal(lnsBill.getPrinBal().getVal());
				//账单利率（正常利率）
				lnsIntBill.setBillrate(la.getRateAgreement().getNormalRate().getYearRate().doubleValue());
				//账单结束日期
				lnsIntBill.setEnddate(ctx.getTranDate());
				//利息记止日期
				lnsIntBill.setIntedate(lnsBill.getIntendDate());
				//账单应还款日期
				lnsIntBill.setRepayedate(lnsBill.getRepayendDate());
				//当期到期日
				lnsIntBill.setCurenddate(lnsBill.getCurendDate());
				//还款方式
				lnsIntBill.setRepayway(lnsBill.getRepayWay());
				//币种
				lnsIntBill.setCcy(lnsBill.getCcy());
				//状态（正常）
				lnsIntBill.setBillstatus(lnsBill.getBillStatus());
				//账单状态开始日期
				lnsIntBill.setStatusbdate(ctx.getTranDate());
				//账单属性(还款)
				lnsIntBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//利息入账标志
				lnsIntBill.setIntrecordflag(lnsBill.getIntrecordFlag());
				//账单是否作废标志
				lnsIntBill.setCancelflag(lnsBill.getCancelFlag());
				//核销优化登记cancelflag，“3”
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
					lnsIntBill.setCancelflag("3");
				}
				if( new FabAmount(lnsIntBill.getBillbal()).isZero() )
				{
					//账单结清标志（结清）
					lnsIntBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				}
				else
				{
					//账单结清标志（未结清）
					lnsIntBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				}

				//账速融接账务核心特殊处理 2019-02-14
				if( "3010014".equals(la.getPrdId()) )
					lnsIntBill.setEnddate(lnsBill.getCurendDate());


				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsIntBill);
					if(!VarChecker.isEmpty(lnsBill.getTermFreeInterest())){
						//现金贷免息税金表生成记录
						AccountingModeChange.saveFreeInterestTax(ctx,la.getContract().getReceiptNo(),lnsBill);
						//主文件动态表对应免息金额减少
						AccountingModeChange.updateBasicDynMX(ctx,la.getBasicExtension().getFreeInterest().sub(lnsBill.getTermFreeInterest()).getVal(),la.getContract().getReceiptNo(),ctx.getTranDate(),lnsBill);
					}
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}

				//根据标志判断是否插辅助表
				if( isInstIntBillPlan )
				{
					interestBillPlan(ctx,seqno,la,lnsIntBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_NINT);
				}

				//修改主文件上次结息日
				basicinfo.setLastIntDate(ctx.getTranDate());

				//账速融接账务核心特殊处理 2019-02-14
				if( "3010014".equals(la.getPrdId()) )
					basicinfo.setLastIntDate(lnsIntBill.getCurenddate());


				//已还利息（账单金额-账单余额）
				totalInt.setBillAmt(new FabAmount(new FabAmount(lnsIntBill.getBillamt()).sub(new FabAmount(lnsIntBill.getBillbal())).getVal()));
				totalInt.setBillTrandate(ctx.getTranDate());
				totalInt.setBillSerseqno(ctx.getSerSeqNo());
				totalInt.setBillTxseq(lnsIntBill.getTxseq());
				totalInt.setBillStatus(lnsIntBill.getBillstatus());
				nintAdvance.add(totalInt);



				//增值税
				LoanAcctChargeProvider.add(BillTransformHelper.convertToLnsBill(lnsIntBill),
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//登记事件
//				LoanAcctChargeProvider.event(BillTransformHelper.convertToLnsBill(lnsIntBill),
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(new FabAmount(lnsIntBill.getBillamt())));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), BillTransformHelper.convertToLnsBill(lnsIntBill));
			}
		}

		//生成本金账单
		//amtFlag标志不为false &&  标志位只还本金
		if( false != amtFlag &&
			ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYPRIN.equals(interestFlag))
		{
			//合同余额>还款余额
			if( la.getContract().getBalance().sub(remainAmt).isPositive() )
			{
				//账单金额为还款余额
				lnsPrinBill.setBillamt( remainAmt.getVal() );
			}
			//合同余额<=还款余额，可以结清
			else
			{
				//账单金额为合同余额
				lnsPrinBill.setBillamt( la.getContract().getBalance().getVal() );
				if( intAmt.isZero())
				{
					//主文件状态结清
					//basicinfo.setLoanStat(ConstantDeclare.LOANSTATUS.LOANSTATUS_CLOSE);
					//结清标志3-结清
					settleFlag = "3";
				}

				//还款计划登记簿登记未来期所有计划
				LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"ADVANCE");
			}

			//本金账单赋值
			//账单余额0
			lnsPrinBill.setBillbal( 0.00 );
			//账务日期
			lnsPrinBill.setTrandate(Date.valueOf(ctx.getTranDate()));
			//流水号
			lnsPrinBill.setSerseqno(ctx.getSerSeqNo());
			//子序号
			lnsPrinBill.setTxseq(++seqno);
			//账号
			lnsPrinBill.setAcctno(la.getContract().getReceiptNo());
			//机构
			lnsPrinBill.setBrc(ctx.getBrc());
			//账单类型（本金）
			lnsPrinBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);
			//账单金额
			lnsPrinBill.setBillamt(lnsPrinBill.getBillamt());
			//账单余额
			lnsPrinBill.setBillbal(lnsPrinBill.getBillbal());
			//上日余额
			//lnsPrinBill.setLastbal(0.00);
			//上笔日期
			lnsPrinBill.setLastdate(ctx.getTranDate());
			//账单对应剩余本金金额（合同余额-账单金额）
			lnsPrinBill.setPrinbal(new FabAmount(la.getContract().getBalance().getVal()).sub(new FabAmount(lnsPrinBill.getBillamt())).getVal());
			//账单利率（正常利率）
			lnsPrinBill.setBillrate(0.00);
			//首次还款账单开始日为开户日
			if( la.getContract().getRepayPrinDate().isEmpty() )
			{
				lnsPrinBill.setBegindate(la.getContract().getContractStartDate());
			}
			//非首次账单开始日为上次结本日
			else
			{
				lnsPrinBill.setBegindate(la.getContract().getRepayPrinDate());
			}

			//账单结束日期
			lnsPrinBill.setEnddate(ctx.getTranDate());
			//期数（利息期数）
			lnsPrinBill.setPeriod(currentTerm);
			//账单应还款日期
			lnsPrinBill.setRepayedate(lnsBill.getRepayendDate());
			//账单结清日期
			lnsPrinBill.setSettledate(ctx.getTranDate());
			//利息记至日期
			lnsPrinBill.setIntedate(ctx.getTranDate());
			//当期到期日
			lnsPrinBill.setCurenddate(lnsBill.getCurendDate());
			//还款方式
			lnsPrinBill.setRepayway(la.getWithdrawAgreement().getRepayWay());
			//币种
			lnsPrinBill.setCcy(la.getContract().getCcy().getCcy());
			//账单状态（正常）
			lnsPrinBill.setBillstatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
			//账单状态开始日期
			lnsPrinBill.setStatusbdate(ctx.getTranDate());
			//账单属性(还款)
			lnsPrinBill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);									//账单属性(还款)
			//利息入账标志
			lnsPrinBill.setIntrecordflag("YES");
			//账单是否作废标志
			lnsPrinBill.setCancelflag(ConstantDeclare.CANCELFLAG.CANCELFLAG_NORMAL);
			if( new FabAmount(lnsPrinBill.getBillbal()).isZero() )
			{
				//账单结清标志（结清）
				lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
			}
			else
			{
				//账单结清标志（未结清）
				lnsPrinBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
			}

			//本金账单金额大于0
			if( lnsPrinBill.getBillamt() > 0.00 )
			{
				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsPrinBill);
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}

				//根据标志判断是否插辅助表
				if( isInstPrinBillPlan || new FabAmount(lnsPrinBill.getPrinbal()).isZero() )
					interestBillPlan(ctx,seqno,la,lnsPrinBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);

				//根据标志判断是否更否更改主文件期数
				//孙总说只还本一定会重新生成还款计划，然而出现了bug
				//if( isAccumTerm )

				if( isAccumTerm &&
					!ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(la.getWithdrawAgreement().getRepayWay()) )
				{
					basicinfo.setIntTerm(1);
					basicinfo.setPrinTerm(1);
				}
				//修改主文件上次结本日
				basicinfo.setLastPrinDate(ctx.getTranDate());
				//3010014账速融，提前还款收全部利息，上次结息日已记到当期到期日，不处理 2020-04-17
				if( CalendarUtil.before(basicinfo.getLastIntDate(), ctx.getTranDate()))
					basicinfo.setLastIntDate(ctx.getTranDate());
				//修改主文件合同余额
				basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));
			}

			//已还本金（账单金额-账单余额）
			totalPrin.setBillAmt(new FabAmount(new FabAmount(lnsPrinBill.getBillamt()).sub(new FabAmount(lnsPrinBill.getBillbal())).getVal()));
			totalPrin.setBillTrandate(ctx.getTranDate());
			totalPrin.setBillSerseqno(ctx.getSerSeqNo());
			totalPrin.setBillTxseq(lnsPrinBill.getTxseq());
			totalPrin.setRepayterm(lnsPrinBill.getPeriod());
			prinAdvance.add(totalPrin);

			//登记还款计划登记簿
			prinFlag = true;
		}


		//更新主文件
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);



		//登记还款计划登记簿，修改实际还款日
		//还到本金切未结清
		if(prinFlag && !"3".equals(settleFlag))
		{
			Map<String,Object> updmap = new HashMap<String, Object>();
			updmap.put("termretdate", ctx.getTranDate());
			updmap.put("acctno", la.getContract().getReceiptNo());
			updmap.put("brc", ctx.getBrc());

			//开户日还款或一天还多次本金的情况，删除当前一期还款计划
			if( ctx.getTranDate().equals(la.getContract().getContractStartDate()) ||
				(lnsPrinBill.getBegindate().equals(ctx.getTranDate()) ))
			{
				try {
					DbAccessUtil.execute("CUSTOMIZE.delete_lnsrpyplan", updmap);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS102", "lnsrpyplan");
				}

			}

			//生成下一期还款计划
			LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"REPAY");

			//还款方式为8定期时不更新实际还款日
			if(!lnsPrinBill.getBegindate().equals(ctx.getTranDate()) &&
			   !ConstantDeclare.REPAYWAY.REPAYWAY_DQHBHX.equals(la.getWithdrawAgreement().getRepayWay()))
			{
				int count = 0;
				try {
					count = DbAccessUtil.execute("CUSTOMIZE.update_lnsrpyplan", updmap);
				}
				catch (FabSqlException e)
				{
					throw new FabException(e, "SPS102", "lnsrpyplan");
				}
				if(1 < count){
					throw new FabException("SPS102", "lnsrpyplan");
				}
			}
		}

		//返回结清标志
		return settleFlag;
	}


	/**
     * 功能：等本等息指定还款顺序
     * 描述：指定本金、利息、罚息的还款先后顺序进行还款
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	txseq     				子序号
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     * @param 	interestFlag 			利息标志（ONLYINT只还息、ONLYPRIN只还本、INTPRIN还本还息）
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
	 * @throws InterruptedException
     *
     * @since  1.1.9
     */
	//指定还本，在生成本金账单时判断当期本金是否能结清（能结清都置成false，结不清不处理仍为true）
	//指定还息，在生成本金账单后判断当期利息是否能结清（能结清都置成false，结不清不处理仍为true）
	public static String specialOrderDbdxInstBill(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag) throws FabException
	{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);

		//用于记录生成账单的期数
		int terms = 0;
		//用于记录是否同一期账单
		Integer period = 0;
		//利息账单标志
		boolean intFlag = true;
		//本金账单标志
		boolean prinFlag = true;
		//能否提前结清
		boolean cleanFlag = false;
		//结清标志 1-未结清 3-结清
		String settleFlag = "1";

		//累计的本金金额
		FabAmount contAmt = new FabAmount(0.00);
		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//取未来期账单list
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();

		//还款计划表结构
		TblLnsrpyplan lnsRpyPlan = new TblLnsrpyplan();

		//遍历未来期账单
		while(billListIterator.hasNext())
		{
			//提前还款还本金金额及流水
			RepayAdvance totalPrin = new RepayAdvance();
			//提前还款还利息金额及流水
			RepayAdvance totalInt = new RepayAdvance() ;
			//取下一条账单信息
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());

			//一整期本金、利息插一次还款计划表
			if( !period.equals(lnsBill.getPeriod()) && !period.equals(0) )
			{
				try {
					DbAccessUtil.execute("Lnsrpyplan.insert", lnsRpyPlan);
				}
				catch (FabSqlException e)
				{
					if (!ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
						throw new FabException(e, "SPS100", "lnsrpyplan");
					}
				}
			}

			/* 还款计划表准备数据 */
			//机构
			lnsRpyPlan.setBrc(ctx.getBrc());
			//账号
			lnsRpyPlan.setAcctno(la.getContract().getReceiptNo());

			//方式
			lnsRpyPlan.setRepayway("REPAY");
			//剩余金额
			lnsRpyPlan.setBalance(lnsBill.getBalance().getVal());


			//list为利息账单且利息账单标志为真
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType())
				&& intFlag )
			{
				/* 还款计划表准备数据 */
				//期数
				lnsRpyPlan.setRepayterm(lnsBill.getPeriod());
				//本期开始日
				lnsRpyPlan.setRepayintbdate(lnsBill.getStartDate());
				//本期结束日
				lnsRpyPlan.setRepayintedate(lnsBill.getEndDate());
				//账单开始日
				lnsRpyPlan.setRepayownbdate(lnsBill.getEndDate());
				//账单结束日
				lnsRpyPlan.setRepayownedate(lnsBill.getRepayendDate());
				//实际还款日
				lnsRpyPlan.setActrepaydate(lnsBill.getEndDate());
				//还款日
				//lnsRepayPlan.setTermretdate(ctx.getTranDate());
				//应还利息金额
				lnsRpyPlan.setTermretint(lnsBill.getBillAmt().getVal());

				if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYINT) )
				{
					//不够还息
					if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
					{
						//剩余金额不够还break
						if(remainAmt.isZero())
						{
							//取到当期本金对应的剩余金额
							lnsRpyPlan.setBalance(billListIterator.next().getBalance().getVal());
							break;
						}

						//账单余额
						lnsBill.setBillBal(new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

						//已还利息（剩余金额）
						totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
						totalInt.setBillTrandate(ctx.getTranDate());
						totalInt.setBillSerseqno(ctx.getSerSeqNo());
						totalInt.setBillTxseq(lnsBill.getTxSeq());
						nintAdvance.add(totalInt);

						//金额减完为0
						remainAmt.selfSub(remainAmt.getVal());

						//不再生成利息
						intFlag = false;
					}
					else
					{
						//账单状态（结清）
						lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
						//账单余额
						lnsBill.setBillBal(new FabAmount(0.00));
						//结清日期
						lnsBill.setSettleDate(ctx.getTranDate());

						//已还利息（账单金额）
						totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
						totalInt.setBillTrandate(ctx.getTranDate());
						totalInt.setBillSerseqno(ctx.getSerSeqNo());
						totalInt.setBillTxseq(lnsBill.getTxSeq());
						nintAdvance.add(totalInt);

						//剩余金额-账单金额
						remainAmt.selfSub(lnsBill.getBillAmt().getVal());
					}
				}

				else if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYPRIN) )
				{
					//账单余额
					lnsBill.setBillBal(lnsBill.getBillAmt());
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);

				}

				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);

                //核销优化登记cancelflag，“3”
                if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
                    lnsBill.setCancelFlag("3");
                }

				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//修改上次结息日为账单结束日
				basicinfo.setLastIntDate(lnsBill.getEndDate());

				//计税
				LoanAcctChargeProvider.add(lnsBill,
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//写事件
//				LoanAcctChargeProvider.event(lnsBill,
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(lnsBill.getBillAmt()));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
			}


			//list为本金账单且本金账单标志为真
			if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType())
					&& prinFlag )
			{
				/* 还款计划表准备数据 */
				//应还本金
				lnsRpyPlan.setTermretprin(lnsBill.getBillAmt().getVal());
				//剩余金额
				lnsRpyPlan.setBalance(lnsBill.getBalance().getVal());

				if( 0 == la.getRateAgreement().getNormalRate().getRate().compareTo(BigDecimal.ZERO) ||
				   la.getInterestAgreement().getIsCalInt().equals(ConstantDeclare.ISCALINT.ISCALINT_NO))
				{
					/* 还款计划表准备数据 */
					//期数
					lnsRpyPlan.setRepayterm(lnsBill.getPeriod());
					//本期开始日
					lnsRpyPlan.setRepayintbdate(lnsBill.getStartDate());
					//本期结束日
					lnsRpyPlan.setRepayintedate(lnsBill.getEndDate());
					//账单开始日
					lnsRpyPlan.setRepayownbdate(lnsBill.getEndDate());
					//账单结束日
					lnsRpyPlan.setRepayownedate(lnsBill.getRepayendDate());
					//实际还款日
					lnsRpyPlan.setActrepaydate(lnsBill.getEndDate());
					//还款日
					//lnsRepayPlan.setTermretdate(ctx.getTranDate());
				}

				if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYPRIN) )
				{
					//剩余金额不够还本
					if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
					{
						//账单余额（账单金额-剩余金额）
						lnsBill.setBillBal( new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

						//已还本金（剩余金额）
						totalPrin.setBillAmt(new FabAmount(remainAmt.getVal()));
						totalPrin.setBillTrandate(ctx.getTranDate());
						totalPrin.setBillSerseqno(ctx.getSerSeqNo());
						totalPrin.setBillTxseq(lnsBill.getTxSeq());
						totalPrin.setRepayterm(lnsBill.getPeriod());
						prinAdvance.add(totalPrin);
						remainAmt.selfSub(remainAmt.getVal());

						//不再生成利息
						intFlag = false;
						//不再生成本金
						prinFlag = false;
					}
					//剩余金额够还本
					else
					{
						//账单状态（结清）
						lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
						//账单余额0
						lnsBill.setBillBal(new FabAmount(0.00));
						//结清日期
						lnsBill.setSettleDate(ctx.getTranDate());

						//能提前结清少不累计当期（要少累计一期）
						if(!cleanFlag)
						{
							//已还本金（账单金额）
							totalPrin.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
							totalPrin.setBillTrandate(ctx.getTranDate());
							totalPrin.setBillSerseqno(ctx.getSerSeqNo());
							totalPrin.setBillTxseq(lnsBill.getTxSeq());
							prinAdvance.add(totalPrin);
						}

						//剩余本金-账单金额
						remainAmt.selfSub(lnsBill.getBillAmt().getVal());
					}
					//本金金额累加
					contAmt.selfAdd(lnsBill.getBillAmt());
				}
				else if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYINT) )
				{
					//账单余额
					lnsBill.setBillBal(lnsBill.getBillAmt());
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
					contAmt.selfAdd(lnsBill.getBillAmt());
					//不再生成本金
					if(	intFlag == false )
						prinFlag = false;
				}



				//期数加1
				terms++;
				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//主文件修改上次结本日
				basicinfo.setLastPrinDate(lnsBill.getEndDate());

			}

			//取当期期数
			period = lnsBill.getPeriod();

			//未来期钱不够提前退出
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				!CalendarUtil.after(ctx.getTranDate(), lnsBill.getStartDate()) &&
				!remainAmt.isPositive() )
				break;
		}

		//循环结束插最后一期还款计划
		try {
			DbAccessUtil.execute("Lnsrpyplan.insert", lnsRpyPlan);
		}
		catch (FabSqlException e)
		{
			if (!ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e, "SPS100", "lnsrpyplan");
			}
		}


		//更新主文件
		basicinfo.setIntTerm(terms);
		basicinfo.setPrinTerm(terms);
		basicinfo.setTranAmt(new FabAmount(contAmt.getVal()));
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);


		//返回结清标志
		return settleFlag;
	}


	/**
     * 功能：非标自定义不规则指定还款顺序
     * 描述：指定本金、利息、罚息的还款先后顺序进行还款
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	txseq     				子序号
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     * @param 	interestFlag 			利息标志（ONLYINT只还息、ONLYPRIN只还本、INTPRIN还本还息）
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
	 * @throws InterruptedException
     *
     * @since  1.1.9
     */
	//指定还本，在生成本金账单时判断当期本金是否能结清（能结清都置成false，结不清不处理仍为true）
	//指定还息，在生成本金账单后判断当期利息是否能结清（能结清都置成false，结不清不处理仍为true）
	public static String specialOrderNonStd(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag) throws FabException
	{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);

		//用于记录生成账单的期数
		int terms = 0;
		//利息账单标志
		boolean intFlag = true;
		//本金账单标志
		boolean prinFlag = true;
		//能否提前结清
		boolean cleanFlag = false;
		//结清标志 1-未结清 3-结清
		String settleFlag = "1";
		Integer curPeriod=0;
		//上次节本结息日
		String lastDate = "";

		//累计的本金金额
		FabAmount contAmt = new FabAmount(0.00);
		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//取未来期账单list
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();

		//遍历未来期账单
		while(billListIterator.hasNext())
		{
			//指定还款还本金金额及流水
			RepayAdvance totalPrin = new RepayAdvance();
			//指定还款还利息金额及流水
			RepayAdvance totalInt = new RepayAdvance() ;
			//取下一条账单信息
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());
			//list为利息账单且利息账单标志为真
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
				intFlag &&
				(curPeriod.intValue() == lnsBill.getPeriod()  || remainAmt.isPositive()) )
			{
				if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYINT) )
				{
					//不够还息
					if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
					{
						//剩余金额不够还break
						if(remainAmt.isZero())
						{
							//取到当期本金对应的剩余金额
							break;
						}

						//账单余额
						lnsBill.setBillBal(new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

						//已还利息（剩余金额）
						totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
						totalInt.setBillTrandate(ctx.getTranDate());
						totalInt.setBillSerseqno(ctx.getSerSeqNo());
						totalInt.setBillTxseq(lnsBill.getTxSeq());
						nintAdvance.add(totalInt);

						//金额减完为0
						remainAmt.selfSub(remainAmt.getVal());

						//不再生成利息
						intFlag = false;
					}
					else
					{
						//账单状态（结清）
						lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
						//账单余额
						lnsBill.setBillBal(new FabAmount(0.00));
						//结清日期
						lnsBill.setSettleDate(ctx.getTranDate());

						//已还利息（账单金额）
						totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
						totalInt.setBillTrandate(ctx.getTranDate());
						totalInt.setBillSerseqno(ctx.getSerSeqNo());
						totalInt.setBillTxseq(lnsBill.getTxSeq());
						nintAdvance.add(totalInt);

						//剩余金额-账单金额
						remainAmt.selfSub(lnsBill.getBillAmt().getVal());
					}
				}

				else if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYPRIN) )
				{
					//账单余额
					lnsBill.setBillBal(lnsBill.getBillAmt());
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);

				}

				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);

				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//修改上次结息日为账单结束日
				lastDate = lnsBill.getEndDate();
				curPeriod = lnsBill.getPeriod();

				//计税
				LoanAcctChargeProvider.add(lnsBill,
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//写事件
//				LoanAcctChargeProvider.event(lnsBill,
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(lnsBill.getBillAmt()));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
			}


			//list为本金账单且本金账单标志为真
			if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				 prinFlag &&
				 (curPeriod.intValue() == lnsBill.getPeriod()  || remainAmt.isPositive()) )
			{
				if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYPRIN) )
				{
					//剩余金额不够还本
					if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
					{
						//账单余额（账单金额-剩余金额）
						lnsBill.setBillBal( new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

						//已还本金（剩余金额）
						totalPrin.setBillAmt(new FabAmount(remainAmt.getVal()));
						totalPrin.setBillTrandate(ctx.getTranDate());
						totalPrin.setBillSerseqno(ctx.getSerSeqNo());
						totalPrin.setBillTxseq(lnsBill.getTxSeq());
						prinAdvance.add(totalPrin);
						remainAmt.selfSub(remainAmt.getVal());

						//不再生成利息
						intFlag = false;
						//不再生成本金
						prinFlag = false;
					}
					//剩余金额够还本
					else
					{
						//账单状态（结清）
						lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
						//账单余额0
						lnsBill.setBillBal(new FabAmount(0.00));
						//结清日期
						lnsBill.setSettleDate(ctx.getTranDate());

						//能提前结清少不累计当期（要少累计一期）
						if(!cleanFlag)
						{
							//已还本金（账单金额）
							totalPrin.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
							totalPrin.setBillTrandate(ctx.getTranDate());
							totalPrin.setBillSerseqno(ctx.getSerSeqNo());
							totalPrin.setBillTxseq(lnsBill.getTxSeq());
							prinAdvance.add(totalPrin);
						}

						//剩余本金-账单金额
						remainAmt.selfSub(lnsBill.getBillAmt().getVal());
					}
					//本金金额累加
					contAmt.selfAdd(lnsBill.getBillAmt());
				}
				else if( interestFlag.equals(ConstantDeclare.PREPAYFLAG.PREPAYSPEC_ONLYINT) )
				{
					//账单余额
					lnsBill.setBillBal(lnsBill.getBillAmt());
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
					contAmt.selfAdd(lnsBill.getBillAmt());
					//不再生成本金
					if(	intFlag == false )
						prinFlag = false;
				}



				//期数加1
				terms++;
				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//主文件修改上次结本日
				lastDate = lnsBill.getEndDate();
				curPeriod = lnsBill.getPeriod();

			}

			//取当期期数
//			curPeriod = lnsBill.getPeriod();

			//未来期钱不够提前退出
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				!CalendarUtil.after(ctx.getTranDate(), lnsBill.getStartDate()) &&
				!remainAmt.isPositive() )
				break;
		}

		//更新主文件
		basicinfo.setIntTerm(terms);
		basicinfo.setPrinTerm(terms);
		basicinfo.setTranAmt(new FabAmount(contAmt.getVal()));
		basicinfo.setLastPrinDate(lastDate);
		basicinfo.setLastIntDate(lastDate);
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);


		//返回结清标志
		return settleFlag;
	}

	/**
     * 功能：等本等息利息减免
     * 描述：等本等息利息减免特殊处理，允许还未来期利息（生成一期利息账单同时生成一期本金账单）， 减免完所有利息还有余额报错
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
     *
     * @since  1.1.1
     */


	//等本等息利息减免
	public static String dbdxInterestDeduction(LoanAgreement loanAgreement,TranCtx ctx, TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics) throws FabException
	{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);


		//用于记录生成账单的期数
		int terms=0;
		//用于记录是否同一期账单
		Integer period = 0;
		//利息账单标志
		boolean intFlag = true;
		//本金账单标志
		boolean prinFlag = true;
		//结清标志 1-未结清 3-结清
		String settleFlag = "1";

		//累计的本金金额
		FabAmount contAmt = new FabAmount(0.00);
		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//取未来期账单list
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();

		//还款计划表结构
		TblLnsrpyplan lnsRpyPlan = new TblLnsrpyplan();

		//遍历未来期账单
		while(billListIterator.hasNext())
		{
			//提前还款还本金金额及流水
			RepayAdvance totalPrin = new RepayAdvance();
			//提前还款还利息金额及流水
			RepayAdvance totalInt = new RepayAdvance() ;
			//取下一条账单信息
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());
			//一整期本金、利息插一次还款计划表
			if( !period.equals(lnsBill.getPeriod()) && !period.equals(0) )
			{
				try {
					DbAccessUtil.execute("Lnsrpyplan.insert", lnsRpyPlan);
				}
				catch (FabSqlException e)
				{
					if (!ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
						throw new FabException(e, "SPS100", "lnsrpyplan");
					}
				}
			}

			/* 还款计划表准备数据 */
			//机构
			lnsRpyPlan.setBrc(ctx.getBrc());
			//账号
			lnsRpyPlan.setAcctno(lnsbasicinfo.getAcctno());

			//方式
			lnsRpyPlan.setRepayway("REPAY");
			//剩余金额
			lnsRpyPlan.setBalance(lnsBill.getBalance().getVal());

			//list为利息账单且利息账单标志为真
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType())
				&& intFlag )
			{
				/* 还款计划表准备数据 */
				//期数
				lnsRpyPlan.setRepayterm(lnsBill.getPeriod());
				//本期开始日
				lnsRpyPlan.setRepayintbdate(lnsBill.getStartDate());
				//本期结束日
				lnsRpyPlan.setRepayintedate(lnsBill.getEndDate());
				//账单开始日
				lnsRpyPlan.setRepayownbdate(lnsBill.getEndDate());
				//账单结束日
				lnsRpyPlan.setRepayownedate(lnsBill.getRepayendDate());
				//实际还款日
				lnsRpyPlan.setActrepaydate(lnsBill.getEndDate());
				//还款日
				//lnsRepayPlan.setTermretdate(ctx.getTranDate());
				//应还利息金额
				lnsRpyPlan.setTermretint(lnsBill.getBillAmt().getVal());


				//不够还息
				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					//剩余金额不够还break
					if(remainAmt.isZero())
					{
						//取到当期本金对应的剩余金额
						lnsRpyPlan.setBalance(billListIterator.next().getBalance().getVal());
						break;
					}

					//账单余额
					lnsBill.setBillBal(new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

					//已还利息（剩余金额）
					totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					totalInt.setRepayterm(lnsBill.getPeriod());
					nintAdvance.add(totalInt);

					//金额减完为0
					remainAmt.selfSub(remainAmt.getVal());
					//不再生成利息
					intFlag = false;
				}
				else
				{
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());

					//已还利息（账单金额）
					totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					totalInt.setRepayterm(lnsBill.getPeriod());
					nintAdvance.add(totalInt);

					//剩余金额-账单金额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());
				}

				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}
				//修改上次结息日为账单结束日
				basicinfo.setLastIntDate(lnsBill.getEndDate());

				//计税
				LoanAcctChargeProvider.add(lnsBill,
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//写事件
//				LoanAcctChargeProvider.event(lnsBill,
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(lnsBill.getBillAmt()));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
			}

			//list为本金账单且本金账单标志为真
			if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType())
					&& prinFlag )
			{
				/* 还款计划表准备数据 */
				//应还本金
				lnsRpyPlan.setTermretprin(lnsBill.getBillAmt().getVal());
				//剩余金额
				lnsRpyPlan.setBalance(lnsBill.getBalance().getVal());
				//账单余额（账单金额）
				lnsBill.setBillBal( new FabAmount(lnsBill.getBillAmt().getVal()) );

				//已还本金0
				totalPrin.setBillAmt(new FabAmount(0.00));
				totalPrin.setBillTrandate(ctx.getTranDate());
				totalPrin.setBillSerseqno(ctx.getSerSeqNo());
				totalPrin.setBillTxseq(lnsBill.getTxSeq());
				prinAdvance.add(totalPrin);

//				//剩余金额小于等于0不在生成账单
//				if( !remainAmt.isPositive() )
//				{
//					//不再生成利息
//					intFlag = false;
//					//不再生成本金
//					prinFlag = false;
//				}

				//本金金额累加
				contAmt.selfAdd(lnsBill.getBillAmt());

				//2020-09-08 减免未来期利息
				lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);
				//不再生成本金
				if(	intFlag == false )
					prinFlag = false;

				//期数加1
				terms++;
				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
                //核销优化登记cancelflag，“3”
                if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(loanAgreement.getContract().getLoanStat())){
                    lnsBill.setCancelFlag("3");
                }

				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//主文件修改上次结本日
				basicinfo.setLastPrinDate(lnsBill.getEndDate());
			}

			//取当期期数
			period = lnsBill.getPeriod();

			//未来期钱不够提前退出
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				!CalendarUtil.after(ctx.getTranDate(), lnsBill.getStartDate()) &&
				!remainAmt.isPositive() )
				break;
		}

		//2020-09-08 减免未来期利息
		try {
			DbAccessUtil.execute("Lnsrpyplan.insert", lnsRpyPlan);
		}
		catch (FabSqlException e)
		{
			if (!ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				throw new FabException(e, "SPS100", "lnsrpyplan");
			}
		}

		//剩余金额大于0
		if( remainAmt.isPositive() )
		{
			//利息减免金额不能大于应还金额
			throw new FabException("LNS031");
		}

		//更新主文件
		basicinfo.setIntTerm(terms);
		basicinfo.setPrinTerm(terms);
		basicinfo.setTranAmt(new FabAmount(contAmt.getVal()));
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);

		//返回结清标志
		return settleFlag;
	}

	/**
     * 功能：非标自定义不规则利息减免
     * 描述：非标自定义不规则利息减免特殊处理，允许还未来期利息（生成一期利息账单同时生成一期本金账单）， 减免完所有利息还有余额报错
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
     *
     * @since  1.1.1
     */


	//非标自定义不规则利息减免
	public static FabAmount nonStdIntDeduction(LoanAgreement loanAgreement,TranCtx ctx, TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics) throws FabException
	{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
		//上次节本结息日
		String lastDate = "";
		Integer curPeriod=0;

		//用于记录生成账单的期数
		int terms=0;
		//利息账单标志
		boolean intFlag = true;
		//本金账单标志
		boolean prinFlag = true;
		//结清标志 1-未结清 3-结清

		//累计的本金金额
		FabAmount contAmt = new FabAmount(0.00);
		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		LnsBasicInfo basicinfo = new LnsBasicInfo();

		//取未来期账单list
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();


		//遍历未来期账单
		while(billListIterator.hasNext())
		{
			//提前还款还本金金额及流水
			RepayAdvance totalPrin = new RepayAdvance();
			//提前还款还利息金额及流水
			RepayAdvance totalInt = new RepayAdvance() ;
			//取下一条账单信息
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());

			//list为利息账单且利息账单标志为真
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
				intFlag &&
				(curPeriod.intValue() == lnsBill.getPeriod()  || remainAmt.isPositive()) )
			{

				//不够还息
				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					//剩余金额不够还break
					if(remainAmt.isZero())
					{
						//取到当期本金对应的剩余金额
						break;
					}

					//账单余额
					lnsBill.setBillBal(new FabAmount(lnsBill.getBillAmt().sub(remainAmt).getVal()));

					//已还利息（剩余金额）
					totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					nintAdvance.add(totalInt);

					//金额减完为0
					remainAmt.selfSub(remainAmt.getVal());
					//不再生成利息
					intFlag = false;
				}
				else
				{
					//账单状态（结清）
					lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
					//账单余额
					lnsBill.setBillBal(new FabAmount(0.00));
					//结清日期
					lnsBill.setSettleDate(ctx.getTranDate());

					//已还利息（账单金额）
					totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					totalInt.setBillTrandate(ctx.getTranDate());
					totalInt.setBillSerseqno(ctx.getSerSeqNo());
					totalInt.setBillTxseq(lnsBill.getTxSeq());
					nintAdvance.add(totalInt);

					//剩余金额-账单金额
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());
				}

				//上次交易日
//				lnsBill.setLastDate(ctx.getTranDate());
				lastDate = lnsBill.getEndDate();
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}
				//修改上次结息日为账单结束日
				basicinfo.setLastIntDate(lnsBill.getEndDate());

				//计税
				LoanAcctChargeProvider.add(lnsBill,
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//写事件
//				LoanAcctChargeProvider.event(lnsBill,
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(lnsBill.getBillAmt()));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);
				curPeriod = lnsBill.getPeriod();
			}

			//list为本金账单且本金账单标志为真
			if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				 prinFlag &&
				 (curPeriod.intValue() == lnsBill.getPeriod()  || remainAmt.isPositive()) )
			{
				//账单余额（账单金额）
				lnsBill.setBillBal( new FabAmount(lnsBill.getBillAmt().getVal()) );

				//已还本金0
				totalPrin.setBillAmt(new FabAmount(0.00));
				totalPrin.setBillTrandate(ctx.getTranDate());
				totalPrin.setBillSerseqno(ctx.getSerSeqNo());
				totalPrin.setBillTxseq(lnsBill.getTxSeq());
				prinAdvance.add(totalPrin);

				//剩余金额小于等于0不在生成账单
				if( !remainAmt.isPositive() )
				{
					//不再生成利息
					intFlag = false;
					//不再生成本金
					prinFlag = false;
				}

				//本金金额累加
				contAmt.selfAdd(lnsBill.getBillAmt());

				//期数加1
				terms++;
				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单属性(还款)
				lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);
				//金额大于0写账单表
				if( lnsBill.getBillAmt().isPositive() )
				{
					lnsBillStatistics.writeBill(la,ctx,lnsBill);
				}

				//主文件修改上次结本日
				lastDate = lnsBill.getEndDate();
//				basicinfo.setLastPrinDate(lnsBill.getEndDate());



				curPeriod = lnsBill.getPeriod();
			}

			//未来期钱不够提前退出
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				!CalendarUtil.after(ctx.getTranDate(), lnsBill.getStartDate()) &&
				!remainAmt.isPositive() )
				break;
		}

		//剩余金额大于0
		if( remainAmt.isPositive() )
		{
			//利息减免金额不能大于应还金额
			throw new FabException("LNS031");
		}

		//更新主文件
		basicinfo.setIntTerm(terms);
		basicinfo.setPrinTerm(terms);
		basicinfo.setLastPrinDate(lastDate);
		basicinfo.setLastIntDate(lastDate);
		basicinfo.setTranAmt(new FabAmount(contAmt.getVal()));
		LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);

		return remainAmt;
	}

	/**
     * 功能：等本等息试算利息
     * 描述：等本等息试算利息
     *
     * @param 	ctx     				公共信息
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
     *
     * @since  1.1.1
     */
	public static String specialRepaymentBillPlan(TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag) throws FabException
	{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);

		boolean intFlag = true;
		boolean prinFlag = true;
		boolean cleanFlag = false;
		String settleFlag = "1"; 		//结清标志 1-未结清 3-结清

		FabAmount contAmt = new FabAmount(0.00);
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());

		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();

		String endDate=ctx.getTranDate();

		if(la.getContract().getContractStartDate().equals(ctx.getTranDate()) &&
				"3".equals(la.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(ctx.getTranDate(), 1).toString("yyyy-MM-dd");
		}
		if("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no"))) {
			//0利率，能提前结清
			if (la.getRateAgreement().getNormalRate().isZero() && !remainAmt.sub(la.getContract().getBalance()).isNegative())
				cleanFlag = true;
		}

		while(billListIterator.hasNext())
		{
			RepayAdvance totalPrin = new RepayAdvance();
			RepayAdvance totalInt = new RepayAdvance() ;
			LnsBill lnsBill = billListIterator.next();
			totalInt.setBillStatus(lnsBill.getBillStatus());
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType())
				&& intFlag )
			{
				//(当前期)当前期利息+合同余额够结清
				if( CalendarUtil.after(endDate, lnsBill.getStartDate()) &&
					!CalendarUtil.after(ctx.getTranDate(), lnsBill.getEndDate()) &&
					!remainAmt.sub( lnsBill.getBillBal().add(la.getContract().getBalance()) ).isNegative() )
				{
					totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					nintAdvance.add(totalInt);
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());

					intFlag = false;
					cleanFlag = true;
				}
				//(未来期)合同余额够结清
				else if(!CalendarUtil.after("yes".equals(GlobalScmConfUtil.getProperty("hdpz", "no")) ? endDate :ctx.getTranDate(), lnsBill.getStartDate()) &&
						!remainAmt.sub( la.getContract().getBalance()).isNegative() )
				{
					intFlag = false;
					cleanFlag = true;
					continue;
				}
				//不够结清
				else
				{
					//不够还息
					if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
					{
						if(remainAmt.isZero())
							break;

						totalInt.setBillAmt(new FabAmount(remainAmt.getVal()));
						nintAdvance.add(totalInt);

						remainAmt.selfSub(remainAmt.getVal());

						intFlag = false;
					}
					else
					{

						totalInt.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
						nintAdvance.add(totalInt);

						remainAmt.selfSub(lnsBill.getBillAmt().getVal());
					}
				}
			}

			if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType())
					&& prinFlag )
			{
				//不够还本
				if( remainAmt.sub(lnsBill.getBillAmt()).isNegative())
				{
					totalPrin.setBillAmt(new FabAmount(remainAmt.getVal()));
					prinAdvance.add(totalPrin);
					remainAmt.selfSub(remainAmt.getVal());

					intFlag = false;
					prinFlag = false;
				}
				else
				{
					//能提前结清少累计一期
					if(!cleanFlag)
					{
						totalPrin.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
						prinAdvance.add(totalPrin);
					}

					remainAmt.selfSub(lnsBill.getBillAmt().getVal());
				}
				//账单金额累加
				contAmt.selfAdd(lnsBill.getBillAmt());

				//提前结清生成一整期本金
				if( cleanFlag )
				{
					lnsBill.setBillAmt(la.getContract().getBalance());
					lnsBill.setBillProperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_REPAY);

					settleFlag = "3";
					totalPrin.setBillAmt(new FabAmount(lnsBill.getBillAmt().getVal()));
					prinAdvance.add(totalPrin);
					remainAmt.selfSub(lnsBill.getBillAmt().getVal());

					break;
				}

			}

			//未来期钱不够提前退出
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()) &&
				!CalendarUtil.after(ctx.getTranDate(), lnsBill.getStartDate()) &&
				!remainAmt.isPositive() )
				break;
		}

		return settleFlag;
	}


	/**
     * 功能：非等本等息试算利息
     * 描述：非等本等息试算利息
     *
     * @param 	loanAgreement  					贷款协议
     * @param 	ctx     				公共信息
     * @param 	lnsbasicinfo 			主文件结构
     * @param 	tranAmt 				提前还款发生额
     * @param 	prinAdvance				还款本金金额及相应信息
     * @param 	nintAdvance				还款利息金额及相应信息
     * @param 	lnsBillstatistics 		试算结构
     *
     * @return 	settleFlag 				结清标志（1-未结清 3-结清）
     *
     * @since  1.1.1
     */
	public static String interestRepaymentBillPlan(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag) throws FabException
	{
		String endDate=ctx.getTranDate();
		if(loanAgreement.getContract().getContractStartDate().equals(ctx.getTranDate()) &&
				"3".equals(loanAgreement.getInterestAgreement().getAdvanceFeeType())){
			endDate = CalendarUtil.nDaysAfter(ctx.getTranDate(), 1).toString("yyyy-MM-dd");
		}

		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, endDate, ctx);


		RepayAdvance totalPrin = new RepayAdvance();
		RepayAdvance totalInt = new RepayAdvance() ;

		TblLnsbill lnsIntBill = new TblLnsbill();
		TblLnsbill lnsPrinBill = new TblLnsbill();
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());

		String settleFlag = "1"; 		//结清标志 1-未结清 3-结清
		int amtFlag=1;

		PrepayTermSupporter prepayTermSupporter = LoanSupporterUtil.getPrepayRepaywaySupporter(la.getWithdrawAgreement().getRepayWay());
		prepayTermSupporter.setLoanAgreement(la);

		if( la.getContract().getRepayIntDate().isEmpty() )
		{
			lnsIntBill.setBegindate(la.getContract().getStartIntDate());

		}else{
			lnsIntBill.setBegindate(la.getContract().getRepayIntDate()); 			//账单开始日期
		}

		//计算本期利息
		//FabAmount intAmt = InterestBillProvider.calInterest(lnsIntBill.getBegindate(),ctx.getTranDate(),la.getInterestAgreement().getIntBase(),new FabAmount(lnsbasicinfo.getContractbal()),la.getRateAgreement().getNormalRate())
		FabAmount intAmt = new FabAmount(0.00);
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();
		LnsBill lnsBill;
		while(billListIterator.hasNext())
		{
			lnsBill = billListIterator.next();
			if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
				!VarChecker.isEmpty(lnsBill.getRepayDateInt())	)
			{
				intAmt = lnsBill.getRepayDateInt();

				break;
			}
		}

		//生成账单期数信息参数
		prepayTermSupporter.genVal(prepayTermSupporter.genMoment(la.getContract().getContractStartDate(),la.getContract().getRepayIntDate() ,la.getContract().getRepayPrinDate(),ctx.getTranDate()),
								   prepayTermSupporter.genThing(intAmt, tranAmt));

		//利息
		if( !ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN.equals(interestFlag) )
		{
			//如果利息金额>=还款金额，利息余额=利息金额-还款金额    不生成本金
			if( intAmt.getVal() >= tranAmt.getVal() )
			{
				lnsIntBill.setBillbal( intAmt.sub(tranAmt).getVal() );
				amtFlag = 0;
			}
			//如果利息金额<还款金额，还款余额=还款金额-利息金额
			else
			{
				remainAmt.setVal( tranAmt.sub(intAmt).getVal() );
				lnsIntBill.setSettledate(ctx.getTranDate());    	//账单结清日期
				lnsIntBill.setBillbal( 0.00 );
			}

			//账单金额大于0生成账单
			if( new FabAmount(intAmt.getVal()).isPositive())
			{
				lnsIntBill.setBillamt(intAmt.getVal()); 							//账单金额
				lnsIntBill.setBillbal(lnsIntBill.getBillbal());

				//totalInt.setBillAmt(new FabAmount(lnsIntBill.getBillamt() - lnsIntBill.getBillbal()))
				totalInt.setBillAmt(new FabAmount(new FabAmount(lnsIntBill.getBillamt()).sub(new FabAmount(lnsIntBill.getBillbal())).getVal()));
				totalInt.setBillStatus(lnsIntBill.getBillstatus());
				nintAdvance.add(totalInt);
			}
		}

		//本金
		if( (0 != amtFlag && new FabAmount(lnsIntBill.getBillbal()).isZero())
			|| ConstantDeclare.PREPAYFLAG.PREPAYFLAG_ONLYPRIN.equals(interestFlag) )

		{
			//如果合同余额>还款余额，账单金额为还款余额
			if( la.getContract().getBalance().sub(remainAmt).isPositive() )
			{
					//不允许提前还本
				if( "470018".equals(ctx.getTranCode())&&"false".equals(la.getInterestAgreement().getIsAdvanceRepay()))
					throw new FabException("LNS064");
				//账单金额
				lnsPrinBill.setBillamt( remainAmt.getVal() );
			}
			else
			{
				//账单金额
				lnsPrinBill.setBillamt( la.getContract().getBalance().getVal() );
				//结清标志
				settleFlag = "3";
			}
			//totalPrin.setBillAmt(new FabAmount(lnsPrinBill.getBillamt() - lnsPrinBill.getBillbal()))
			totalPrin.setBillAmt(new FabAmount(new FabAmount(lnsPrinBill.getBillamt()).sub(new FabAmount(lnsPrinBill.getBillbal())).getVal()));

			prinAdvance.add(totalPrin);

		}

		return settleFlag;
	}

	/**
	 * 功能：结息结到当前日期利息
	 * 描述：等本等息结整期利息，并在lnsbill中插入这期本金
	 *
	 * @param 	dealDate 				利息记录时间
	 * @param 	la  					贷款协议
	 * @param 	ctx     				公共信息
	 * @return result
	 *
	 * @since  1.1.1
	 */
		public static void inerestBillByDate(String dealDate ,LoanAgreement la,TranCtx ctx) throws FabException{

			LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, dealDate, ctx);


			//利息账单结构
			TblLnsbill lnsIntBill = new TblLnsbill();


			List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
			listBill.addAll(lnsBillStatistics.getBillInfoList());
			ListIterator<LnsBill> billListIterator = listBill.listIterator();
			LnsBill lnsBill = new LnsBill();
			// 找到需要结息的这一期
			while(billListIterator.hasNext())
			{
				lnsBill = billListIterator.next();
				if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsBill.getBillType()) &&
						!VarChecker.isEmpty(lnsBill.getRepayDateInt())	)
				{
					break;
				}
			}
			//校验是否需要结息
			if(!VarChecker.isEmpty(lnsBill.getRepayDateInt())&& lnsBill.getRepayDateInt().isPositive())
			{
				//第一次还款取计息日作为账单开始日，非第一次还款取上次结息日作为账单开始日
				if( la.getContract().getRepayIntDate().isEmpty() )
				{
					lnsIntBill.setBegindate(la.getContract().getStartIntDate());

				}else{
					lnsIntBill.setBegindate(la.getContract().getRepayIntDate()); 			//账单开始日期
				}
				//更新主文件信息Map结构
				LnsBasicInfo basicinfo = new LnsBasicInfo();

				//账务日期
				lnsIntBill.setTrandate(Date.valueOf(ctx.getTranDate()));
				//流水号
				lnsIntBill.setSerseqno(ctx.getSerSeqNo());
				//子序号
				lnsIntBill.setTxseq(lnsBill.getTxSeq());
				//账号
				lnsIntBill.setAcctno(la.getContract().getReceiptNo());
				//机构
				lnsIntBill.setBrc(ctx.getBrc());
				//账单类型（利息）
				lnsIntBill.setBilltype(lnsBill.getBillType());
				//期数（利息期数）
				lnsIntBill.setPeriod(lnsBill.getPeriod());
				//账单金额  等本等近插整期的利息
				//账单结束日期
				if(ConstantDeclare.REPAYWAY.isEqualInterest(la.getWithdrawAgreement().getRepayWay()))
				{
					lnsIntBill.setBillamt(lnsBill.getBillAmt().getVal());
					lnsIntBill.setEnddate(lnsBill.getEndDate());
				}
				else
				{
					lnsIntBill.setBillamt(lnsBill.getRepayDateInt().getVal());
					lnsIntBill.setEnddate(ctx.getTranDate());
				}


				//账单余额
				lnsIntBill.setBillbal(lnsIntBill.getBillamt());
				//上日余额
				//lnsbill.setLastbal(0.00);

				//账单对应金额（当期本金金额）
				lnsIntBill.setPrinbal(lnsBill.getPrinBal().getVal());
				//账单利率（正常利率）
				lnsIntBill.setBillrate(la.getRateAgreement().getNormalRate().getYearRate().doubleValue());


				//利息记止日期
				lnsIntBill.setIntedate(lnsBill.getIntendDate());
				//账单应还款日期
				lnsIntBill.setRepayedate(lnsBill.getRepayendDate());
				//当期到期日
				lnsIntBill.setCurenddate(lnsBill.getCurendDate());
				//还款方式
				lnsIntBill.setRepayway(lnsBill.getRepayWay());
				//币种
				lnsIntBill.setCcy(lnsBill.getCcy());
				//状态（正常）
				lnsIntBill.setBillstatus(lnsBill.getBillStatus());
				//账单状态开始日期
				lnsIntBill.setStatusbdate(ctx.getTranDate());
				//账单属性(还款)
				lnsIntBill.setBillproperty(lnsBill.getBillProperty());
				//利息入账标志
				lnsIntBill.setIntrecordflag(lnsBill.getIntrecordFlag());
				//账单是否作废标志
				lnsIntBill.setCancelflag(lnsBill.getCancelFlag());
				//核销优化登记cancelflag，“3”
				if(VarChecker.asList(ConstantDeclare.LOANSTATUS.LOANSTATUS_CERTIFICATION,
						ConstantDeclare.LOANSTATUS.LOANSTATUS_NONACCURAL).contains(la.getContract().getLoanStat())){
					lnsIntBill.setCancelflag("3");
				}

				//账单结清标志（未结清）
				lnsIntBill.setSettleflag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_RUNNING);


				//插入账单表
				try {
					DbAccessUtil.execute("Lnsbill.insert", lnsIntBill);
					if(!VarChecker.isEmpty(lnsBill.getTermFreeInterest())){
						//现金贷免息税金表生成记录
						AccountingModeChange.saveFreeInterestTax(ctx,la.getContract().getReceiptNo(),lnsBill);
						//主文件动态表对应免息金额减少
						AccountingModeChange.updateBasicDynMX(ctx,la.getBasicExtension().getFreeInterest().sub(lnsBill.getTermFreeInterest()).getVal(),la.getContract().getReceiptNo(),ctx.getTranDate(),lnsBill);
					}
				} catch (FabSqlException e) {
					throw new FabException(e, "SPS100", "lnsbill");
				}
                //interestBillPlan(ctx,seqno,la,lnsIntBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_NINT);


				//修改主文件上次结息日
				basicinfo.setLastIntDate(lnsIntBill.getEnddate());


				//增值税
				LoanAcctChargeProvider.add(BillTransformHelper.convertToLnsBill(lnsIntBill),
						ctx, la, ConstantDeclare.EVENT.INTSETLEMT, ConstantDeclare.BRIEFCODE.LXJZ);
				//登记事件
//				LoanAcctChargeProvider.event(BillTransformHelper.convertToLnsBill(lnsIntBill),
//						ctx, la, ConstantDeclare.EVENT.INTSETMTAX, ConstantDeclare.BRIEFCODE.JXSJ,
//						TaxUtil.calcVAT(new FabAmount(lnsIntBill.getBillamt())));
				//结息税金
				AccountingModeChange.saveIntSetmTax(ctx,  la.getContract().getReceiptNo(), lnsBill);



				//等本等息 还需要插本金账单
				if(ConstantDeclare.REPAYWAY.isEqualInterest(la.getWithdrawAgreement().getRepayWay())){

					while(billListIterator.hasNext())
					{
						lnsBill = billListIterator.next();
						if( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()))
						{
							break;
						}
					}
					//本金账单赋值
					TblLnsbill lnsPrinBill = lnsIntBill;
					//子序号
					lnsPrinBill.setTxseq(lnsBill.getTxSeq());
					//账单类型（本金）
					lnsPrinBill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);
					//账单金额
					lnsPrinBill.setBillamt(lnsBill.getBillAmt().getVal());
					//账单余额
					lnsPrinBill.setBillbal(lnsBill.getBillAmt().getVal());
					//账单对应本金余额余额
					lnsPrinBill.setPrinbal(lnsBill.getPrinBal().getVal());

					//账单利率（正常利率）
					lnsPrinBill.setBillrate(0.00);
					//首次还款账单开始日为开户日
					if( la.getContract().getRepayPrinDate().isEmpty() )
					{
						lnsPrinBill.setBegindate(la.getContract().getContractStartDate());
					}
					//非首次账单开始日为上次结本日
					else
					{
						lnsPrinBill.setBegindate(la.getContract().getRepayPrinDate());
					}
					//利息入账标志
					lnsPrinBill.setIntrecordflag("YES");


					//插入账单表
					try {
						DbAccessUtil.execute("Lnsbill.insert", lnsPrinBill);
					} catch (FabSqlException e) {
						throw new FabException(e, "SPS100", "lnsbill");
					}
                    //interestBillPlan(ctx,seqno,la,lnsPrinBill,lnsBillStatistics,ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);

                    //修改主文件上次结本日
					basicinfo.setLastPrinDate(lnsPrinBill.getEnddate());
                    basicinfo.setIntTerm(1);
                    basicinfo.setPrinTerm(1);
                    basicinfo.setTranAmt(new FabAmount(lnsPrinBill.getBillamt()));



				}
				//更新主文件结本结息日
				LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);

			}
		}

	/**
	 *
	 * author:chenchao
	 * @param loanAgreement
	 * @param ctx
	 * @param txseq
	 * @param lnsbasicinfo
	 * @param tranAmt
	 * @param prinAdvance
	 * @param nintAdvance
	 * @param lnsBillstatistics
	 * @param interestFlag
	 * @param compensateFlag
	 * @param repaySettle
	 */
		public static String salesReturn(LoanAgreement loanAgreement,TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag,String compensateFlag,String repaySettle)throws FabException, InterruptedException{
			LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
			LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
			//结清标志 1-未结清 3-结清
			String settleFlag = "1";
			//累计的本金金额
			//还款剩余金额
			FabAmount remainAmt = new FabAmount(tranAmt.getVal());
			//更新主文件信息Map结构
			LnsBasicInfo basicinfo = new LnsBasicInfo();
			//存放可退金额
			RepayAdvance totalPrin = new RepayAdvance();
			totalPrin.setBillTrandate(ctx.getTranDate());
			totalPrin.setBillSerseqno(ctx.getSerSeqNo());
			//判断剩余金额是否能够结清
			if(!remainAmt.sub( la.getContract().getBalance() ).isNegative()){
				//退货金额能够结清
				LoanRepayPlanProvider.interestRepayPlan( ctx,la.getContract().getReceiptNo(),"ADVANCE");
				settleFlag="3";
				//遍历未来期计划
				List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
				ListIterator<LnsBill> billListIterator = listBill.listIterator();
				while(billListIterator.hasNext()){
					LnsBill lnsBill = billListIterator.next();
					if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()))
					{
						//能够结清
						TblLnsbill tbLnsbill=getSalesReturnMap(la,ctx,la.getContract().getBalance(),lnsBill);
						//保存账单表
						DbAccessUtil.execute("Lnsbill.insert",tbLnsbill);
						//已退本金
						totalPrin.setBillAmt(new FabAmount(tbLnsbill.getBillamt()));
						totalPrin.setBillTxseq(lnsBill.getTxSeq());
						totalPrin.setRepayterm(lnsBill.getPeriod());
						//修改合同的余额
						basicinfo.setTranAmt(la.getContract().getBalance());
						LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);
						break;
					}
				}
			}else{
				//不够结清 扣减主合同金额
				basicinfo.setTranAmt(remainAmt);
				LoanBasicInfoProvider.basicInfoUpt(la, basicinfo, ctx);
				//重新加载合同
				LoanAgreement loanAgreement_new = LoanAgreementProvider.genLoanAgreementFromDB(ctx.getRequestDict("acctNo").toString(), ctx);
				//保存账户辅助表字段 并更新协议
				updateLnsbasicinfoexForSalesReturn(loanAgreement_new,ctx);
				//更新合同余额需重新生成还款计划
				LnsBillStatistics lnsBillStatistics_new = LoanTrailCalculationProvider.genLoanBillDetail(loanAgreement_new, ctx.getTranDate(), ctx);
				//保存账单表
				List<LnsBill> listBill = lnsBillStatistics_new.getFutureBillInfoList();
				TblLnsbill tbLnsbill=getSalesReturnMap(loanAgreement_new,ctx,remainAmt,listBill.get(0));
				//保存账单表
				DbAccessUtil.execute("Lnsbill.insert",tbLnsbill);
				//重新试算还款计划 并更改还款计划辅助表最近一期本金
				Map param=new HashMap();
				param.put("acctno", la.getContract().getReceiptNo());
				param.put("brc", ctx.getBrc());
				TblLnsrpyplan lnsrpyplan = DbAccessUtil.queryForObject("CUSTOMIZE.query_lnsrpyplan_603", param, TblLnsrpyplan.class);
				param.put("repayterm",lnsrpyplan.getRepayterm());
				for(LnsBill lnsbill:listBill){
					if(lnsbill.getStartDate().equals(lnsrpyplan.getRepayintbdate())&&lnsbill.getEndDate().equals(lnsrpyplan.getRepayintedate())){
						//账单的起始日期 和 最大还款期数 起始日期 核对   使用发生金额
						if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_NINT).contains(lnsbill.getBillType())){
							param.put("termretint",lnsbill.getBillAmt().getVal());
						}
						if(VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsbill.getBillType())){
							param.put("termretprin",lnsbill.getBillAmt().getVal());
							param.put("balance",lnsbill.getPrinBal().getVal());
							//保存已退本金
							totalPrin.setBillAmt(new FabAmount(remainAmt.getVal()));
							totalPrin.setBillTxseq(lnsbill.getTxSeq());
							totalPrin.setRepayterm(lnsbill.getPeriod());
						}
						//修改最新一期的还款计划
						DbAccessUtil.execute(
								"CUSTOMIZE.update_lnsrpyplan_605", param);
					}
				}
			}
			prinAdvance.add(totalPrin);
			return settleFlag;
		}

	/**
	 * 退货时构造退货相关map
	 * @param la 协议
	 * @param ctx 上下文
	 * @return
	 */
		public static TblLnsbill  getSalesReturnMap(LoanAgreement la,TranCtx ctx,FabAmount remainAmt,LnsBill lnsBillOld) throws FabException {
			Map param=new HashMap();
			param.put("acctno", la.getContract().getReceiptNo());
			param.put("brc", ctx.getBrc());
			//获取账本表最大期的本金行
			TblLnsbill tblLnsbill =DbAccessUtil.queryForObject("Lnsbill.query_max_period_txnseqno_lnsbill", param, TblLnsbill.class);
			if(tblLnsbill==null){
				LnsBill lnsBill=new LnsBill();
				lnsBill.setPeriod(1);
				//能够结清
				lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				lnsBill.setStartDate(la.getContract().getContractStartDate());
				//账单余额0
				lnsBill.setBillBal(new FabAmount(0.00));
				//结清日期
				lnsBill.setSettleDate(ctx.getTranDate());
				//结束日期（合同到期日）
				lnsBill.setEndDate(ctx.getTranDate());
				//应还款日（合同到期日）
				lnsBill.setRepayendDate(lnsBillOld.getRepayendDate());
				//上次结息日（合同到期日）
				lnsBill.setIntendDate(lnsBillOld.getCurendDate());
				//状态日期
				lnsBill.setStatusbDate(ctx.getTranDate());
				//上次交易日
				lnsBill.setLastDate(ctx.getTranDate());
				//账单状态（结清）
				lnsBill.setSettleFlag(ConstantDeclare.SETTLEFLAG.SETTLEFLAG_CLOSE);
				//结清日期
				lnsBill.setSettleDate(ctx.getTranDate());
				lnsBill.setBillAmt(remainAmt);
				lnsBill.setCurendDate(lnsBillOld.getCurendDate());
				lnsBill.setBillStatus(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);
				//金额大于0写账单表
				tblLnsbill = BillTransformHelper.convertToTblLnsBill(la,lnsBill,ctx);
			}else{
				tblLnsbill.setBillamt(remainAmt.getVal());
			}
			//设置账本表账务日期和流水号
			tblLnsbill.setTrandate(Date.valueOf(ctx.getTranDate()));
			tblLnsbill.setSerseqno(ctx.getSerSeqNo());
			//获取对应的序号最大值
			changeBillTxseqToMax(tblLnsbill);
			tblLnsbill.setLastdate(ctx.getTranDate());
			tblLnsbill.setBilltype(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN);
			//账单对应剩余本金
			tblLnsbill.setPrinbal(la.getContract().getBalance().getVal());
			//账单属性
			tblLnsbill.setBillproperty(ConstantDeclare.BILLPROPERTY.BILLPROPERTY_RETURN);
			return tblLnsbill;
		}

	/**
	 * 修改账本表的序号 使其插入在最后一条
	 * @param tblLnsbill
	 */
		public static void changeBillTxseqToMax(TblLnsbill tblLnsbill)throws FabException{
			//判断对象是否为空
			if(tblLnsbill!=null){
				//设置账本表最大序号
				Map param=new HashMap();
				param.put("trandate",tblLnsbill.getTrandate());
				param.put("serseqno",tblLnsbill.getSerseqno());
				Integer dbTxseq=DbAccessUtil.queryForObject("Lnsbill.query_lnsbill_txseq", param, Integer.class);
				//判断序号是否为空
				tblLnsbill.setTxseq(dbTxseq==null ?1:dbTxseq+1);
			}
		}

	/**
	 * 退货接口增加更新账户辅助表字段   并修改
	 * @param loanAgreement_new  协议
	 *
	 */
		public static void updateLnsbasicinfoexForSalesReturn(LoanAgreement loanAgreement_new,TranCtx ctx) throws FabException{
			//修改主文件拓展表登记合同的金额
			Map<String,Object> exParam = new HashMap<String,Object>();
			exParam.put("acctno", loanAgreement_new.getContract().getReceiptNo());
			exParam.put("openbrc", ctx.getBrc());
			exParam.put("brc", ctx.getBrc());
			exParam.put("key", "THJE");
			exParam.put("value1",loanAgreement_new.getContract().getCurrPrinPeriod());
			exParam.put("value2",loanAgreement_new.getContract().getBalance().getVal());
			exParam.put("value3",0.00);
			TblLnsbasicinfoex lnsbasicinfoex = LoanDbUtil.queryForObject("Lnsbasicinfoex.selectByUk", exParam, TblLnsbasicinfoex.class);
			//支持 退货接口 保存退货金额
			if(lnsbasicinfoex==null)
				LoanDbUtil.insert("CUSTOMIZE.insert_lnsbasicinfoex", exParam);
			else
				LoanDbUtil.update("Lnsbasicinfoex.update_lnsbasicinfoex", exParam);
			loanAgreement_new.getBasicExtension().setSalesReturnAmt(loanAgreement_new.getContract().getBalance());
			loanAgreement_new.getBasicExtension().setInitFuturePeriodNum(loanAgreement_new.getContract().getCurrPrinPeriod());
		}



	/**
	 * 退货试算接口
	 * author:chenchao
	 * @param ctx
	 * @param txseq
	 * @param lnsbasicinfo
	 * @param tranAmt
	 * @param prinAdvance
	 * @param nintAdvance
	 * @param lnsBillstatistics
	 * @param interestFlag
	 */
	public static String salesReturnPlan(TranCtx ctx, Integer txseq,TblLnsbasicinfo lnsbasicinfo, FabAmount tranAmt, List<RepayAdvance> prinAdvance, List<RepayAdvance> nintAdvance,LnsBillStatistics lnsBillstatistics,String  interestFlag)throws FabException, InterruptedException{
		LoanAgreement la = LoanAgreementProvider.genLoanAgreementFromDB(lnsbasicinfo.getAcctno(), ctx);
		LnsBillStatistics lnsBillStatistics = LoanTrailCalculationProvider.genLoanBillDetail(la, ctx.getTranDate(), ctx);
		//结清标志 1-未结清 3-结清
		String settleFlag = "1";
		//还款剩余金额
		FabAmount remainAmt = new FabAmount(tranAmt.getVal());
		//更新主文件信息Map结构
		RepayAdvance totalPrin = new RepayAdvance();
		//遍历未来期计划
		List<LnsBill> listBill = lnsBillStatistics.getFutureBillInfoList();
		ListIterator<LnsBill> billListIterator = listBill.listIterator();
		while(billListIterator.hasNext()){
			LnsBill lnsBill = billListIterator.next();
			if ( VarChecker.asList(ConstantDeclare.BILLTYPE.BILLTYPE_PRIN).contains(lnsBill.getBillType()))
			{
				//账单金额（合同余额）
				//判断剩余金额是否大于剩余本金
				if(!remainAmt.sub( la.getContract().getBalance() ).isNegative()){
					//退货金额能够结清
					settleFlag="3";
					totalPrin.setBillAmt(la.getContract().getBalance() );
				}else{
					//不能结清
					totalPrin.setBillAmt(remainAmt);
				}
				totalPrin.setBillTrandate(ctx.getTranDate());
				totalPrin.setBillSerseqno(ctx.getSerSeqNo());
				totalPrin.setBillTxseq(lnsBill.getTxSeq());
				prinAdvance.add(totalPrin);
				break;
			}
		}
		return settleFlag;
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
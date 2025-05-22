package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.LnsNonstdPlanCalculate;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.JsonTransfer;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;

/**
 * 〈一句话功能简述〉<br> 
 * 〈功能详细描述〉：非标准开户前还款计划试算
 *
 * @author 16071579
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */

@Scope("prototype")
@Repository
public class Lns414 extends WorkUnit {

	FabAmount contractAmt;	//本金
	String repayWay;		//还款方式
	FabRate normalRate;		//利率
	String loanType;		//贷款种类
	String calcIntFlag1;
	String calcIntFlag2;
	String endDate;
	
	Integer totalLine;
	String pkgList2;
	FabAmount prinAmt;	//应还本金合计
	FabAmount intAmt;	//应还利息合计
	int		allTerms = 0;
	int 	i = 0;
	
	@Override
	public void run() throws Exception {
		TranCtx ctx = getTranctx();
		ListMap 	pkgListInMap = ctx.getRequestDict("pkgList1");
		List<LnsNonstdPlanCalculate> lnsNonstdPlanOut = new ArrayList<LnsNonstdPlanCalculate>();
		
		if(pkgListInMap == null || pkgListInMap.size() == 0)
			throw new FabException("LNS046");
		
		List<Map> sortList =  pkgListInMap.ToList();
		
		Collections.sort(sortList,
				new Comparator<Map>() {
					@Override
					public int compare(Map o1, Map o2) {
						Integer p1 =  Integer.parseInt(o1.get("repayTerm").toString());
						Integer p2 =  Integer.parseInt(o2.get("repayTerm").toString());
						return p1.compareTo(p2);
					}
		});
		
		//对接口list按期数从小到大做排序DOTO
		String	termeDateTmp = "";
		FabAmount amtTmp = new FabAmount(0.00);
		
		FabAmount allInt = new FabAmount(0.00);
		FabAmount allPrin = new FabAmount(contractAmt.getVal());

		allTerms = sortList.size();
		for (Map sortListTmp:sortList) {
			i++;
			LnsNonstdPlanCalculate lnsNonstdPlanOutTmp = new LnsNonstdPlanCalculate();
			
			//当期期数
			Integer repayTerm = Integer.parseInt(sortListTmp.get("repayTerm").toString());
			LoggerUtil.debug("SUNTERM"+repayTerm);
			lnsNonstdPlanOutTmp.setRepayterm(repayTerm);
			
			//本期起日 第一期为合同开始日 其他期为上期止日 因为前端不送每期的起始日 说界面只有一个止日字段 
			if(repayTerm.equals(1))
				lnsNonstdPlanOutTmp.setRepayintbdate(ctx.getTranDate());
			else
				lnsNonstdPlanOutTmp.setRepayintbdate(termeDateTmp);
			
			//本期止日 前端必输字段
			String	termeDate = sortListTmp.get("termeDate").toString();
			lnsNonstdPlanOutTmp.setRepayintedate(termeDate);
			//试算的返回 还款起日和止日都是本期止日 因为试算不考虑宽限期
			lnsNonstdPlanOutTmp.setRepayownbdate(termeDate);
			lnsNonstdPlanOutTmp.setRepayownedate(termeDate);
			//赋值临时变量 作为下一期起始日期
			termeDateTmp = termeDate;
			
			//起息日期标志
			String calcIntFlag2 = null;
			if(sortListTmp.containsKey("calcIntFlag2"))
				calcIntFlag2 = sortListTmp.get("calcIntFlag2").toString();
			if(VarChecker.isEmpty(calcIntFlag2)) //如果上面循环中每期的标志没赋值 取主报文的值
				calcIntFlag2 = getCalcIntFlag2();
			
			//如果明细赋值了起息日期 那什么条件都不多考虑 直接用
			String	intbDate = null;
			if(sortListTmp.containsKey("intbDate"))
				intbDate = sortListTmp.get("intbDate").toString();
			
			if( ("2").equals(calcIntFlag2 )) 
			{//每期都是从开户日起息	但是非标准的计划我们以明细里给的值为准
				if(VarChecker.isEmpty(intbDate))
					intbDate = ctx.getTranDate();	
			}
			else
			{//每期都是正常从当期起息 特殊的是如果主起息日期不为空第一期赋值主起息日期
				if(VarChecker.isEmpty(intbDate))
					intbDate = lnsNonstdPlanOutTmp.getRepayintbdate();
			}	
			
			String inteDate = null;
			if(sortListTmp.containsKey("inteDate"))
				inteDate = sortListTmp.get("inteDate").toString();
			
			if(VarChecker.isEmpty(inteDate))
				inteDate = lnsNonstdPlanOutTmp.getRepayintedate();
			
			//下面的一些金额要插表,是否需要考虑精度问题 DOTO
			FabAmount  termPrin = new FabAmount(Double.parseDouble(sortListTmp.get("termPrin").toString()));
			lnsNonstdPlanOutTmp.setNoretamt(termPrin);
			//如果无利息需要计算利息还是到当期结束再计算 DOTO 如果给了利息是否需要加个数据库字段标志
			
			FabRate normalRate = null;
			if(sortListTmp.containsKey("normalRate"))
				normalRate = new FabRate(Double.parseDouble(sortListTmp.get("normalRate").toString()));
			else
				normalRate = new FabRate(getNormalRate().getYearRate().doubleValue());
			
			String calcIntFlag1 = null;
			if(sortListTmp.containsKey("calcIntFlag1"))
				calcIntFlag1 = sortListTmp.get("calcIntFlag1").toString();
			
			if(VarChecker.isEmpty(calcIntFlag1))
				calcIntFlag1 = getCalcIntFlag1();
			LoggerUtil.debug("calcIntFlag1"+calcIntFlag1+"calcIntFlag2"+calcIntFlag2);
			LoggerUtil.debug(intbDate+inteDate);
			//明细可能赋值就是零 那就需要特殊处理了 判断字段是否存在 DOTO  VarChecker.isEmpty(repayChannel)
			//1-剩余 2-当期 3-全部 calcIntFlag1本金标志
			FabAmount termInt = null;
			FabAmount termAll = new FabAmount(0.00);
			//目前先还本后还息的 因为其利息计算方式特殊单独写了一个模块  只有一种方式 所以没做细分
			//如果以后有细分 拷贝之后else里面的方式
			if(ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(repayWay)) 
			{
				contractAmt.selfSub(amtTmp); //合同金额减去已过期本金
				int nDays = CalendarUtil.actualDaysBetween(intbDate, inteDate);
				LoggerUtil.debug("contractAmt"+contractAmt+"nDays"+nDays);
				BigDecimal intDec = new BigDecimal(nDays);
				intDec = (intDec.multiply(normalRate.getDayRate())
						.multiply(new BigDecimal(contractAmt.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
				termInt = new FabAmount(intDec.doubleValue());
				
				allInt.selfAdd(termInt); //累积每期利息
				
				if(i == allTerms)
				{
					termAll.setVal(0.00);
					termAll.selfAdd(termPrin);
					termAll.selfAdd(allInt);
					lnsNonstdPlanOutTmp.setNoretint(allInt);
					lnsNonstdPlanOutTmp.setSumamt(termAll);
				}	
				else
				{
					lnsNonstdPlanOutTmp.setNoretint(new FabAmount(0.00));
					lnsNonstdPlanOutTmp.setSumamt(termPrin);
				}	

				LoggerUtil.debug("termAll"+termAll+"termPrin"+termPrin+"termInt"+termInt);
				
				lnsNonstdPlanOut.add(lnsNonstdPlanOutTmp);
				
				amtTmp = termPrin; //当期本金赋值临时变量 辅助计算下期剩余本金
			}
			else //repayway为1的有一种方式 为3的有两种方式 加上上面的repayway为2的目前一共四种
			{	
				if("1".equals(calcIntFlag1))
				{	
					contractAmt.selfSub(amtTmp);
					int nDays = CalendarUtil.actualDaysBetween(intbDate, inteDate);
					LoggerUtil.debug("contractAmt"+contractAmt+"nDays"+nDays);
					BigDecimal intDec = new BigDecimal(nDays);
					intDec = (intDec.multiply(normalRate.getDayRate())
							.multiply(new BigDecimal(contractAmt.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
					termInt = new FabAmount(intDec.doubleValue());
				}	
				else if("2".equals(calcIntFlag1))
				{
					int nDays = CalendarUtil.actualDaysBetween(intbDate, inteDate);
					BigDecimal intDec = new BigDecimal(nDays);
					intDec = (intDec.multiply(normalRate.getDayRate())
							.multiply(new BigDecimal(termPrin.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
					termInt = new FabAmount(intDec.doubleValue());
				}	
				else
				{
					int nDays = CalendarUtil.actualDaysBetween(intbDate, inteDate);
					BigDecimal intDec = new BigDecimal(nDays);
					intDec = (intDec.multiply(normalRate.getDayRate())
							.multiply(new BigDecimal(contractAmt.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
					termInt = new FabAmount(intDec.doubleValue());
				}	
			
				lnsNonstdPlanOutTmp.setNoretint(termInt);
				
				termAll.setVal(0.00);
				termAll.selfAdd(termPrin);
				termAll.selfAdd(termInt);
				lnsNonstdPlanOutTmp.setSumamt(termAll);
				LoggerUtil.debug("termAll"+termAll+"termPrin"+termPrin+"termInt"+termInt);
				lnsNonstdPlanOut.add(lnsNonstdPlanOutTmp);
				
				amtTmp = termPrin; //当期本金赋值临时变量 辅助计算下期剩余本金
	
				allInt.selfAdd(termInt); //累积每期利息
			}
		}	
		LoggerUtil.debug("allInt"+allInt);
		setPrinAmt(allPrin);
		setIntAmt(allInt);
		//转换之前需要
		setPkgList2(JsonTransfer.ToJson(lnsNonstdPlanOut));
	}	
	/**
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}

	public Integer getTotalLine() {
		return totalLine;
	}
	public void setTotalLine(Integer totalLine) {
		this.totalLine = totalLine;
	}
	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}

	public String getPkgList2() {
		return pkgList2;
	}
	public void setPkgList2(String pkgList2) {
		this.pkgList2 = pkgList2;
	}
	/**
	 * @return the repayWay
	 */
	public String getRepayWay() {
		return repayWay;
	}

	/**
	 * @param repayWay the repayWay to set
	 */
	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
	}

	public String getCalcIntFlag1() {
		return calcIntFlag1;
	}
	public void setCalcIntFlag1(String calcIntFlag1) {
		this.calcIntFlag1 = calcIntFlag1;
	}
	public String getCalcIntFlag2() {
		return calcIntFlag2;
	}
	public void setCalcIntFlag2(String calcIntFlag2) {
		this.calcIntFlag2 = calcIntFlag2;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public FabRate getNormalRate() {
		return normalRate;
	}
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}
	/**
	 * @return the loanType
	 */
	public String getLoanType() {
		return loanType;
	}

	/**
	 * @param loanType the loanType to set
	 */
	public void setLoanType(String loanType) {
		this.loanType = loanType;
	}

	/**
	 * @return the prinAmt
	 */
	public FabAmount getPrinAmt() {
		return prinAmt;
	}

	/**
	 * @param prinAmt the prinAmt to set
	 */
	public void setPrinAmt(FabAmount prinAmt) {
		this.prinAmt = prinAmt;
	}

	/**
	 * @return the intAmt
	 */
	public FabAmount getIntAmt() {
		return intAmt;
	}

	/**
	 * @param intAmt the intAmt to set
	 */
	public void setIntAmt(FabAmount intAmt) {
		this.intAmt = intAmt;
	}

}

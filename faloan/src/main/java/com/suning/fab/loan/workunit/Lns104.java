package com.suning.fab.loan.workunit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.suning.fab.loan.account.FaLoanPubDict;
import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsRentPlanCalculate;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.TblLnsaccountdyninfo;
import com.suning.fab.loan.domain.TblLnsbasicinfo;
import com.suning.fab.loan.domain.TblLnsnonstdplan;
import com.suning.fab.loan.domain.TblLnsrpyplan;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.la.LoanAgreement;
import com.suning.fab.loan.utils.CalendarUtil;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanAgreementProvider;
import com.suning.fab.loan.utils.LoanEventOperateProvider;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.PubDict;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.MapperUtil;
import com.suning.fab.tup4j.utils.VarChecker;



/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 开户-非标贷款本金账户
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns104 extends WorkUnit { 

	FabRate		compoundRate;		//复利利率
	String		loanType;			//贷款类型 1-普通 2-按揭
	FabRate		normalRate;			//正常利率
	String		normalRateType;		//正常利率类型
	FabRate		overdueRate;		//逾期利率
	String		repayWay;			//还款方式
	String		serialNo;			//幂等流水号
	String		termDate;			//交易日期
	String		tranCode;			//交易代码
	String		merchantNo;			//商户号
	String		customName;			//户名
	String		customType;			//客户类型
	String		oldrepayacct;		//历史预收账号
	FabAmount	contractAmt;		//合同金额
	String		receiptNo;			//借据号
	String		startIntDate;		//起息日期
	String		endDate;			//合同到期日
	Integer		graceDays;			//宽限期
	String		contractNo;			//合同编号
	String		fundChannel;		//科目
	String		channelType;		//渠道编码
	String		outSerialNo;		//外部流水号
	String		productCode;		//产品编码
	String		calcIntFlag1;		
	String		calcIntFlag2;
	String		acctNoNon;
	
	List<LnsRentPlanCalculate> rentPlanpkgList = new ArrayList<LnsRentPlanCalculate>();


	@Autowired
	@Qualifier("accountAdder")
	AccountOperator add;
	@Autowired LoanEventOperateProvider eventProvider;
	@Override
	public void run() throws Exception {
		//2020-07-15 涉敏字段
		customName="";
		TranCtx ctx = getTranctx();
		
				
		ListMap pkgListInMap = new ListMap();
		ListMap 	listMap = ctx.getRequestDict("pkgList1");
		if(rentPlanpkgList.isEmpty())
		{
			pkgListInMap=listMap;
		}
		else
		{
			ArrayList<PubDict> list = new ArrayList<PubDict>();
			for(LnsRentPlanCalculate plan:rentPlanpkgList){
				list.add(MapperUtil.map(plan, FaLoanPubDict.class));
			}
			pkgListInMap.setLoopmsg(list);
		}
		
		
		
		int			allTerms = 0;
		int			i = 0;
		FabAmount	contractAmtTmp = new FabAmount(getContractAmt().getVal());
		
		//非标准还款计划插表lnsnonstdplan
		if(pkgListInMap == null || pkgListInMap.size() == 0)
			throw new FabException("LNS046");
		
		List<Map> sortList =  pkgListInMap.ToList();
		//按期数从小到大顺序对list拍讯
		Collections.sort(sortList,
				new Comparator<Map>() {
					@Override
					public int compare(Map o1, Map o2) {
						Integer p1 =  Integer.parseInt(o1.get("repayTerm").toString());
						Integer p2 =  Integer.parseInt(o2.get("repayTerm").toString());
						return p1.compareTo(p2);
					}
		});
		FabAmount sumAmt = new FabAmount();
		TblLnsnonstdplan lnsnonstdplan =  new TblLnsnonstdplan();
		//TblLnsrpyplan rpyPlan = new TblLnsrpyplan();
		TblLnsrpyplan rpyPlan = new TblLnsrpyplan();
		//对接口list按期数从小到大做排序DOTO
		String	termeDateTmp = "";
		FabAmount allInt = new FabAmount(0.00);
		FabAmount termPrin = new FabAmount(0.00);
		allTerms = sortList.size();
		for (Map sortListTmp:sortList) {
			i++;
			if("2".equals(getCalcIntFlag1())) //第四种方式 当期本金余额计息
			{
				//写多个静态表
				TblLnsbasicinfo lnsbasicinfo = new TblLnsbasicinfo();
				Integer repayTerm = Integer.parseInt(sortListTmp.get("repayTerm").toString());
				if(!repayTerm.equals(i))
				{
					LoggerUtil.error("明细金额之和与合同总金额不符");
					throw new FabException("LNS028");
				}	
				lnsbasicinfo.setOpenbrc(ctx.getBrc());	//开户机构代码
				lnsbasicinfo.setAcctno(getReceiptNo().trim()+repayTerm.toString());	//账号
				setAcctNoNon(lnsbasicinfo.getAcctno());
				lnsbasicinfo.setAcctype(ConstantDeclare.ACCTYPE.DEFAULT);	//帐别
				lnsbasicinfo.setCcy(new FabCurrency().getCcy());
				if( "2".equals(getCustomType()) )
					lnsbasicinfo.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
				else
					lnsbasicinfo.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
				
				lnsbasicinfo.setCustomid(getMerchantNo());
				lnsbasicinfo.setName("");	//户名
				lnsbasicinfo.setPrdcode(getProductCode());	//产品代码
				lnsbasicinfo.setFlag1("");	//静态标志1 目前第一位标志扣息放款
				lnsbasicinfo.setPrinmindays(0);
				lnsbasicinfo.setIntmindays(0);
		
				lnsbasicinfo.setIscalint("YES");
				lnsbasicinfo.setRepayway("8");	//担保方式    被用为还款方式 SUN固定的赋值为8 随借随还
				lnsbasicinfo.setLoantype1(getLoanType());	//贷款种类1   2-按揭 、1-普通
				lnsbasicinfo.setLoanstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);	//贷款状态
				lnsbasicinfo.setProfitbrc(ctx.getBrc());	//核算机构代码
				lnsbasicinfo.setAcctno1(getReceiptNo());	//借据号
				//lnsbasicinfo.setFirstprindate(billInfoAll.getFirstRepayPrinDate());  //首次还本日期  char(10)
				//lnsbasicinfo.setFirstintdate(billInfoAll.getFirstRepayIntDate());   //首次付息日期  char(10)
				lnsbasicinfo.setFirstintflag("1");   //首次结息日标志    char(1)
		
				//lnsbasicinfo.setLastprindate("");   //下次结本日    char(10)
				//lnsbasicinfo.setLastintdate("");    //下次结息日    char(10)
				if(VarChecker.isEmpty(getStartIntDate()))
				{	
					lnsbasicinfo.setLastprindate(ctx.getTranDate()); 
					lnsbasicinfo.setLastintdate(ctx.getTranDate()); 
				}
				else
				{	
					lnsbasicinfo.setLastprindate(getStartIntDate()); 
					lnsbasicinfo.setLastintdate(getStartIntDate());	//起息日期
				}
		
				lnsbasicinfo.setPrinterms(1);   //还本总期数    integer
				lnsbasicinfo.setIntterms(1);    //还息总期数    integer
				lnsbasicinfo.setCurprinterm(1); //还本当期期数  integer
				lnsbasicinfo.setCurintterm(1);  //还息当期期数  integer
				lnsbasicinfo.setOldprinterm(repayTerm); //本金已还期数  integer
				lnsbasicinfo.setOldintterm(repayTerm);  //利息已还期数  integer
				termPrin = new FabAmount(Double.parseDouble(sortListTmp.get("termPrin").toString()));
				lnsbasicinfo.setContractbal(termPrin.getVal());  //合同余额  decimal
		
				if(VarChecker.isEmpty(getStartIntDate()))
					lnsbasicinfo.setBeginintdate(ctx.getTranDate());	//起息日期
				else
					lnsbasicinfo.setBeginintdate(getStartIntDate());	//起息日期
					
				lnsbasicinfo.setOpendate(ctx.getTranDate());
				lnsbasicinfo.setGracedays(getGraceDays());	//贷款宽限期
				lnsbasicinfo.setContduedate(sortListTmp.get("termeDate").toString().trim());	//合同到期日
				if(termeDateTmp.compareTo(lnsbasicinfo.getContduedate()) >= 0)
				{
					LoggerUtil.error("每期结束日期必须大于上期结束日期:"+"termeDateTmp["+termeDateTmp
					+"]"+"termenddate["+lnsbasicinfo.getContduedate());
					throw new FabException("LNS036");
				}
				termeDateTmp = sortListTmp.get("termeDate").toString().trim();
				//termeDate必须大于工作日期
				if(ctx.getTranDate().compareTo(lnsbasicinfo.getContduedate()) >= 0)
				{
					LoggerUtil.error("每期结束日期必须大于起始日期:"+"startdate["+ctx.getTranDate()
					+"]"+"termenddate["+lnsbasicinfo.getContduedate());
					throw new FabException("LNS036");
				}
				
				LoanAgreement la = LoanAgreementProvider.genLoanAgreement(getProductCode());
				LoanAgreementProvider.genNonLoanAgreementFromRepayWay(la, ctx, 
						lnsbasicinfo.getContduedate().substring(8,10), 
						lnsbasicinfo.getBeginintdate(), lnsbasicinfo.getContduedate(), 
						lnsbasicinfo.getBeginintdate(), lnsbasicinfo.getContduedate());
				
				lnsbasicinfo.setPrinperformula(la.getWithdrawAgreement().getPeriodFormula());
				lnsbasicinfo.setPrinformula("2"); 
				lnsbasicinfo.setIntperformula(la.getInterestAgreement().getPeriodFormula());
				lnsbasicinfo.setIntformula("2");
				
				lnsbasicinfo.setExtnums(0);	//展期次数
				lnsbasicinfo.setIntrateplan("");	//利率计划
				lnsbasicinfo.setBjtxbl(0.00);	//本金贴息比例
				lnsbasicinfo.setTxacctno("");	//贴息帐号
				lnsbasicinfo.setDiacct(oldrepayacct);	//扣款帐号
				lnsbasicinfo.setChanneltype(getChannelType());    //资金来源 放款渠道 1-银行 2-易付宝char
				lnsbasicinfo.setFundchannel(getFundChannel());    //资金来源帐号 资金通道 sap银行科目编号/易付宝总
				lnsbasicinfo.setOutserialno(getOutSerialNo());    //外部流水单号：银行资金流水号/易付宝交易单号
				//2019-09-26 合同号超过20位存拓展表
				if( !VarChecker.isEmpty(getContractNo()) )
				{
					if( getContractNo().length() <= 20 )
						lnsbasicinfo.setContrasctno(getContractNo());	//合同编号
					else
					{
						//修改主文件拓展表
						Map<String,Object> exParam = new HashMap<>();
						exParam.put("acctno", lnsbasicinfo.getAcctno());
						exParam.put("brc", ctx.getBrc());
						exParam.put("key", "HTBM");
						exParam.put("value1", getContractNo());
						exParam.put("value2", 0.00);
						exParam.put("value3", 0.00);
						exParam.put("tunneldata", "");
						
						try {
							DbAccessUtil.execute("CUSTOMIZE.insert_lnsbasicinfoex_202", exParam);
						} catch (FabSqlException e) {
							throw new FabException(e, "SPS102", "lnsbasicinfoex");
						}
					}
				}
				lnsbasicinfo.setContractamt(termPrin.getVal());	//合同金额
				lnsbasicinfo.setProvisionflag("RUNNING");
				lnsbasicinfo.setMemo("");	//备注
				lnsbasicinfo.setNormalrate(getNormalRate().getYearRate().doubleValue());	//正常利率
				lnsbasicinfo.setOverduerate(getOverdueRate().getYearRate().doubleValue());	//逾期利率
				lnsbasicinfo.setOverduerate1(getCompoundRate().getYearRate().doubleValue());	//复利利率
				lnsbasicinfo.setModifydate(ctx.getTranDate());	//交易日期
				lnsbasicinfo.setTrandate(ctx.getTranDate());
				lnsbasicinfo.setModifytime("");	//交易时间
		
				try {
					DbAccessUtil.execute("Lnsbasicinfo.insert",lnsbasicinfo );
				} catch(FabSqlException e) {
					LoggerUtil.error("Lnsbasicinfo.insert.ERROR");
					throw new FabException(e, "SPS100", "lnsbasicinfo");
				}
	
				//写老辅助表
				rpyPlan.setAcctno(getReceiptNo().trim()+repayTerm.toString());
				rpyPlan.setBrc(ctx.getBrc());
				rpyPlan.setRepayterm(1);
				rpyPlan.setAcctflag("");
				rpyPlan.setRepayway("");
				rpyPlan.setRepayintedate(lnsbasicinfo.getContduedate());
				rpyPlan.setRepayownbdate(lnsbasicinfo.getContduedate());
				DateTime repayownedatetmp = CalendarUtil.nDaysAfter(lnsbasicinfo.getContduedate(), getGraceDays().intValue());
				rpyPlan.setRepayownedate( repayownedatetmp.toString("yyyy-MM-dd") );
				rpyPlan.setRepayintbdate(lnsbasicinfo.getBeginintdate());
				rpyPlan.setTermretprin(termPrin.getVal());
				
				int nDays = CalendarUtil.actualDaysBetween(lnsbasicinfo.getBeginintdate(), lnsbasicinfo.getContduedate());
				BigDecimal intDec = new BigDecimal(nDays);
				normalRate = new FabRate(getNormalRate());
				intDec = (intDec.multiply(normalRate.getDayRate())
						.multiply(new BigDecimal(termPrin.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
				FabAmount termInt = new FabAmount(intDec.doubleValue());
				rpyPlan.setTermretint(termInt.getVal());
				
				rpyPlan.setBalance(0.00);
				rpyPlan.setActrepaydate(lnsbasicinfo.getContduedate());
				DbAccessUtil.execute("Lnsrpyplan.insert",rpyPlan);
				
				//写动态表
				TblLnsaccountdyninfo lnsaccountdyninfo =  new TblLnsaccountdyninfo();
				lnsaccountdyninfo.setAcctno(getReceiptNo().trim()+repayTerm.toString());//贷款账号
				lnsaccountdyninfo.setAcctstat(ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN+"."+ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);//帐户形态
				lnsaccountdyninfo.setStatus(ConstantDeclare.STATUS.NORMAL);//状态
				lnsaccountdyninfo.setInacctno("");//贷款内部账号
				
				lnsaccountdyninfo.setProfitbrc(ctx.getBrc());//核算机构代码
				lnsaccountdyninfo.setOpenacctbrc(ctx.getBrc());//开户机构代码
				lnsaccountdyninfo.setAcctype(ConstantDeclare.ACCTYPE.DEFAULT);//帐别
				lnsaccountdyninfo.setSubctrlcode("");//科目控制字
				lnsaccountdyninfo.setCcy(new FabCurrency().getCcy());
				lnsaccountdyninfo.setFlag("");//静态控制标志
				lnsaccountdyninfo.setFlag1("");//动态控制标志
				lnsaccountdyninfo.setRepayorder("");//贷款还款顺序
				lnsaccountdyninfo.setRatetype(getNormalRate().getRateUnit());//利率类型
				lnsaccountdyninfo.setBald(ConstantDeclare.BALCTRLDIR.DEBIT);//余额方向
				lnsaccountdyninfo.setIntrate(getNormalRate().getYearRate().doubleValue());//执行利率
				lnsaccountdyninfo.setLastbal(0.00);//昨日余额
				lnsaccountdyninfo.setCurrbal(0.00);//当前余额
				lnsaccountdyninfo.setCtrlbal(0.00);//控制余额
				lnsaccountdyninfo.setAccum(0.00);//积数
				lnsaccountdyninfo.setBegindate("");//起息日期
				lnsaccountdyninfo.setPrechgratedate("");//上次利率调整日
				lnsaccountdyninfo.setPrecalcintdate("");//上次结息日
				lnsaccountdyninfo.setNextcalcdate("");//下次结息日
				lnsaccountdyninfo.setPretrandate(ctx.getTranDate());//上笔交易日
				lnsaccountdyninfo.setDac("");//校验位
				lnsaccountdyninfo.setBegincacudate("");//累计积数起日
				lnsaccountdyninfo.setEndcacudate("");//累计积数止日
				lnsaccountdyninfo.setPrecacudays(1);//应计积数天数
				lnsaccountdyninfo.setCacudays(1);//已计积数天数
				lnsaccountdyninfo.setYearstartbal(0.00);//年初余额
				lnsaccountdyninfo.setFlagres11("");//标志备用
				lnsaccountdyninfo.setDateres01("");//日期备用
				lnsaccountdyninfo.setDateres02("");//日期备用
				lnsaccountdyninfo.setAmountres1(0.00);//金额备用
				lnsaccountdyninfo.setAmountres2(0.00);//金额备用
				lnsaccountdyninfo.setAccumres(0.00);//积数备用
				lnsaccountdyninfo.setAcctnores01("");//账号备用
				lnsaccountdyninfo.setAcctnores02("");//账号备用
				lnsaccountdyninfo.setOrdnu(1);//明细帐序号
				lnsaccountdyninfo.setModifydate(ctx.getTranDate());//交易日期
				lnsaccountdyninfo.setPrdcode(getProductCode());
				try{
					DbAccessUtil.execute("Lnsaccountdyninfo.insert", lnsaccountdyninfo);
				}catch (FabSqlException e){
					LoggerUtil.error("Lnsbasicinfo.insert.ERROR");
					throw new FabException(e, "SPS100", "lnsaccountdyninfo");
				}
				
				FabAmount	amount = new FabAmount(0.00);
				LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(lnsaccountdyninfo.getAcctno(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
				lnsAcctInfo.setMerchantNo(getMerchantNo());
				lnsAcctInfo.setCustType(getCustomType());
				lnsAcctInfo.setCustName("");
				lnsAcctInfo.setPrdCode(getProductCode());
				
				FundInvest openFundInvest = new FundInvest("", "", getChannelType(), getFundChannel(), getOutSerialNo());	
				eventProvider.createEvent(ConstantDeclare.EVENT.LOANMEBRGT, amount, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.BJKH, ctx);
				//非标自定义开户事件在lns202
				add.operate(lnsAcctInfo, null, termPrin, openFundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);
				eventProvider.createEvent(ConstantDeclare.EVENT.LOANGRANTA, termPrin, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);
				
				//sumAmt.selfAdd(termPrin);	
			}
			else //前三种情况 按合同剩余金额计息
			{
				//写多个非标辅助表
				lnsnonstdplan.setAcctno(getReceiptNo());//贷款账号
				lnsnonstdplan.setOpenbrc(ctx.getBrc());
				Integer repayTerm = Integer.parseInt(sortListTmp.get("repayTerm").toString());
				lnsnonstdplan.setRepayterm(repayTerm);
				if(!repayTerm.equals(i))
				{
					LoggerUtil.error("明细金额之和与合同总金额不符");
					throw new FabException("LNS028");
				}
				
				rpyPlan.setAcctno(getReceiptNo());
				rpyPlan.setBrc(ctx.getBrc());
				rpyPlan.setRepayterm(repayTerm);
				rpyPlan.setAcctflag("");
				rpyPlan.setRepayway("");
				lnsnonstdplan.setRepayway(getRepayWay());
				
				if(repayTerm.equals(1))
					lnsnonstdplan.setTermbdate(ctx.getTranDate());
				else
					lnsnonstdplan.setTermbdate(termeDateTmp);
				
				if(termeDateTmp.compareTo(sortListTmp.get("termeDate").toString().trim() ) >= 0)
				{
					LoggerUtil.error("每期结束日期必须大于上期结束日期:"+"termenddatetmp["+termeDateTmp
					+"]"+"termenddate["+sortListTmp.get("termeDate").toString().trim());
					throw new FabException("LNS036");
				}
				
				String	termeDate = sortListTmp.get("termeDate").toString().trim();
				//termeDate必须大于工作日期 DOTO 加上倒起息条件校验 日期是否合法
/*				if(ctx.getTranDate().compareTo(termeDate) >= 0)
				{
					LoggerUtil.error("合同结束日期必须大于起始日期:"+"startdate["+ctx.getTranDate()
					+"]"+"termenddate["+termeDate);
					throw new FabException("LNS036");
				}	*/
				
				lnsnonstdplan.setTermedate(termeDate);
				termeDateTmp = termeDate;
				
				rpyPlan.setRepayintedate(lnsnonstdplan.getTermedate());
				
				//如果无宽限期 需要取主文件中的宽限期
				int days = -1;
				if(sortListTmp.containsKey("days"))
					days = Integer.parseInt( sortListTmp.get("days").toString());
				if(days != -1)
					lnsnonstdplan.setDays(Integer.valueOf(days));
				else
					lnsnonstdplan.setDays(getGraceDays());
				
				rpyPlan.setRepayownbdate(lnsnonstdplan.getTermedate());
				DateTime repayownedatetmp = CalendarUtil.nDaysAfter(lnsnonstdplan.getTermedate(),lnsnonstdplan.getDays().intValue());
				rpyPlan.setRepayownedate( repayownedatetmp.toString("yyyy-MM-dd") );
				
				//如果明细赋值了起息日期 那什么条件都不多考虑 直接用
				String calcIntFlag2 = null;
				if(sortListTmp.containsKey("calcIntFlag2"))
					calcIntFlag2 = sortListTmp.get("calcIntFlag2").toString();
				if(VarChecker.isEmpty(calcIntFlag2)) //如果上面循环中每期的标志没赋值 取主报文的值
					calcIntFlag2 = getCalcIntFlag2();

				lnsnonstdplan.setCalcintflag2(calcIntFlag2);
				
				String	intbDate = null;
				if(sortListTmp.containsKey("intbDate"))
					intbDate = sortListTmp.get("intbDate").toString();
				
				if((!VarChecker.isEmpty(calcIntFlag2) && ("2").equals(calcIntFlag2)) || 
						(VarChecker.isEmpty(calcIntFlag2) && ("2").equals(getCalcIntFlag2()) ))
				{//每期都是从开户日起息	
					if(!VarChecker.isEmpty(intbDate))
						lnsnonstdplan.setIntbdate(intbDate);
					else if(!VarChecker.isEmpty(getStartIntDate()))
						lnsnonstdplan.setIntbdate(getStartIntDate());
					else
						lnsnonstdplan.setIntbdate(ctx.getTranDate());
				}
				else
				{//每期都是正常从当期起息 特殊的是如果主起息日期不为空第一期赋值主起息日期
					if(!VarChecker.isEmpty(intbDate))
						lnsnonstdplan.setIntbdate(intbDate);
					else if(!VarChecker.isEmpty(getStartIntDate()) && lnsnonstdplan.getRepayterm().equals(1))
						lnsnonstdplan.setIntbdate(getStartIntDate());
					else
						lnsnonstdplan.setIntbdate(lnsnonstdplan.getTermbdate());
				}	
				
				rpyPlan.setRepayintbdate(lnsnonstdplan.getIntbdate());
				
				String inteDate = null;
				if(sortListTmp.containsKey("inteDate"))
					inteDate = sortListTmp.get("inteDate").toString();
				if(VarChecker.isEmpty(inteDate))
					lnsnonstdplan.setIntedate(lnsnonstdplan.getTermedate());
				else
					lnsnonstdplan.setIntedate(inteDate);
				
				//下面的一些金额要插表,是否需要考虑精度问题 DOTO
				termPrin = new FabAmount(Double.parseDouble(sortListTmp.get("termPrin").toString()));
				lnsnonstdplan.setTermprin(termPrin.getVal());
				rpyPlan.setTermretprin(termPrin.getVal());
				
				String calcIntFlag1 = null;
				if(sortListTmp.containsKey("calcIntFlag1"))
					calcIntFlag1 = sortListTmp.get("calcIntFlag1").toString();
				if(VarChecker.isEmpty(calcIntFlag1)) //如果上面循环中每期的标志没赋值 取主报文的值
					calcIntFlag1 = getCalcIntFlag1();

				lnsnonstdplan.setCalcintflag1(calcIntFlag1);
				
				FabRate normalRate = null;
				if(sortListTmp.containsKey("normalRate"))
					normalRate = new FabRate(Double.parseDouble(sortListTmp.get("normalRate").toString())/100.00);
				else
					normalRate = new FabRate(getNormalRate());
				
				lnsnonstdplan.setNormalrate(normalRate.getYearRate().doubleValue());
				
				LoggerUtil.debug(calcIntFlag1+"FUCK3"+calcIntFlag2);
				
				//如果无利息需要计算利息还是到当期结束再计算 DOTO 如果给了利息是否需要加个数据库字段标志
				FabAmount  termInt = null;
				if(sortListTmp.containsKey("termInt"))
					 termInt = new FabAmount(Double.parseDouble(sortListTmp.get("termInt").toString()));
				if(!VarChecker.isEmpty(termInt) && !"2412614".equals(ctx.getProductCode()) && !"2412617".equals(ctx.getProductCode())) //现在前端都不送利息 如果以后送了利息为零要考虑下
				{
					LoggerUtil.debug( "FUCK1"+rpyPlan.getRepayownedate());

					lnsnonstdplan.setTermint(termInt.getVal());
					rpyPlan.setTermretint(termInt.getVal());
					contractAmtTmp.selfSub(termPrin);//余额递减
				}
				else
				{
					LoggerUtil.debug( "FUCK2"+rpyPlan.getRepayownedate());
					//目前先还本后还息的 因为其利息计算方式特殊单独写了一个模块  只有一种方式 所以没做细分
					//如果以后有细分 拷贝之后else里面的方式
					if(ConstantDeclare.REPAYWAY.REPAYWAY_XBHX.equals(repayWay)) 
					{
						int nDays = CalendarUtil.actualDaysBetween(lnsnonstdplan.getIntbdate(), lnsnonstdplan.getIntedate());
						LoggerUtil.debug("contractAmt"+contractAmt+"nDays"+nDays);
						BigDecimal intDec = new BigDecimal(nDays);
						intDec = (intDec.multiply(normalRate.getDayRate())
								.multiply(new BigDecimal(contractAmtTmp.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
						termInt = new FabAmount(intDec.doubleValue());
						
						allInt.selfAdd(termInt); //累积每期利息
						
						if(i == allTerms)
						{
							rpyPlan.setTermretint(allInt.getVal());
							lnsnonstdplan.setTermint(allInt.getVal());
						}	
						else
						{
							rpyPlan.setTermretint(0.00);
							lnsnonstdplan.setTermint(0.00);
						}	
						contractAmtTmp.selfSub(termPrin);
					}
					else //repayway为1的有一种方式 为3的有两种方式 加上上面的repayway为2的目前一共四种
					{	
						if("1".equals(calcIntFlag1))
						{	
							
							int nDays = CalendarUtil.actualDaysBetween(lnsnonstdplan.getIntbdate(), lnsnonstdplan.getIntedate());
							LoggerUtil.debug("contractAmt"+contractAmt+"nDays"+nDays);
							BigDecimal intDec = new BigDecimal(nDays);
							intDec = (intDec.multiply(normalRate.getDayRate())
									.multiply(new BigDecimal(contractAmtTmp.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
							termInt = new FabAmount(intDec.doubleValue());
							contractAmtTmp.selfSub(termPrin);
						}	
						else if("2".equals(calcIntFlag1))
						{
							int nDays = CalendarUtil.actualDaysBetween(lnsnonstdplan.getIntbdate(), lnsnonstdplan.getIntedate());
							BigDecimal intDec = new BigDecimal(nDays);
							intDec = (intDec.multiply(normalRate.getDayRate())
									.multiply(new BigDecimal(termPrin.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
							termInt = new FabAmount(intDec.doubleValue());
						}	
						else
						{
							int nDays = CalendarUtil.actualDaysBetween(lnsnonstdplan.getIntbdate(), lnsnonstdplan.getIntedate());
							BigDecimal intDec = new BigDecimal(nDays);
							intDec = (intDec.multiply(normalRate.getDayRate())
									.multiply(new BigDecimal(contractAmt.getVal()))).setScale(2,BigDecimal.ROUND_HALF_UP);
							termInt = new FabAmount(intDec.doubleValue());
						}	
					
						rpyPlan.setTermretint(termInt.getVal());
						lnsnonstdplan.setTermint(termInt.getVal());
			
					}
				}
				
				//剩余本金需要先处理循环然后计算出来DOTO
				if("2".equals(lnsnonstdplan.getCalcintflag1())) //当期本金
					lnsnonstdplan.setBalance(lnsnonstdplan.getTermprin());
				else if("3".equals(lnsnonstdplan.getCalcintflag1())) //全部本金
					lnsnonstdplan.setBalance(contractAmt.getVal());
				else //剩余本金
					lnsnonstdplan.setBalance(contractAmtTmp.getVal());
				
				rpyPlan.setBalance(contractAmtTmp.getVal());
				
				FabRate overdueRate = null;
				if(sortListTmp.containsKey("overdueRate"))
					overdueRate = new FabRate(Double.parseDouble(sortListTmp.get("overdueRate").toString())/100.00);
				if(!VarChecker.isEmpty(overdueRate))
					lnsnonstdplan.setOverduerate(overdueRate.getYearRate().doubleValue());
				else
					lnsnonstdplan.setOverduerate(getOverdueRate().getYearRate().doubleValue());
				
				FabRate overdueRate1 = null;
				if(sortListTmp.containsKey("compoundRate"))
					overdueRate1 = new FabRate(Double.parseDouble(sortListTmp.get("compoundRate").toString())/100.00);
				if(!VarChecker.isEmpty(overdueRate1))	
					lnsnonstdplan.setOverduerate1(overdueRate1.getYearRate().doubleValue());
				else
					lnsnonstdplan.setOverduerate1(getCompoundRate().getYearRate().doubleValue());
				
				String normalRateType = null;
				if(sortListTmp.containsKey("normalRateType"))
					normalRateType = sortListTmp.get("normalRateType").toString();
				if(!VarChecker.isEmpty(normalRateType))
					lnsnonstdplan.setRatetype(normalRateType);
				else
					lnsnonstdplan.setRatetype(getNormalRateType());
				
				lnsnonstdplan.setModifydate(ctx.getTranDate());
				//Modifytime时间戳是默认就有的
				
				LoanAgreement la = LoanAgreementProvider.genLoanAgreement(getProductCode());
				LoanAgreementProvider.genNonLoanAgreementFromRepayWay(la, ctx, 
						lnsnonstdplan.getTermedate().substring(8,10), 
						lnsnonstdplan.getIntbdate(), lnsnonstdplan.getIntedate(), 
						lnsnonstdplan.getTermbdate(), lnsnonstdplan.getTermedate());
				
				lnsnonstdplan.setPrinperformula(la.getWithdrawAgreement().getPeriodFormula());
				//lnsnonstdplan.setPrinformula(la.getWithdrawAgreement().getRepayAmtFormula());  
				
				lnsnonstdplan.setIntperformula(la.getInterestAgreement().getPeriodFormula());
				//lnsnonstdplan.setIntformula(la.getInterestAgreement().getIntFormula());
				
				lnsnonstdplan.setLastprindate(lnsnonstdplan.getTermbdate()); 
				lnsnonstdplan.setLastintdate(lnsnonstdplan.getIntbdate()); 
				
				if( "1".equals(lnsnonstdplan.getCalcintflag1()) )
				{	
						lnsnonstdplan.setIntformula("3");
						lnsnonstdplan.setPrinformula("3");
				}
				if( "2".equals(lnsnonstdplan.getCalcintflag1()) )
				{	
					lnsnonstdplan.setIntformula("2");
					lnsnonstdplan.setPrinformula("2");
				}
				if( "3".equals(lnsnonstdplan.getCalcintflag1()) )
				{	
					lnsnonstdplan.setIntformula("4");
					lnsnonstdplan.setPrinformula("4");
				}
				
				rpyPlan.setActrepaydate(lnsnonstdplan.getTermedate());
				
				try{
					DbAccessUtil.execute("Lnsnonstdplan.insert",lnsnonstdplan);		
				}catch (FabException e){
					LoggerUtil.error("SQLCODE"+e.getErrCode());
					throw new FabException(e, "SPS100", "lnsnonstdplan");
				}
				
				try{
					DbAccessUtil.execute("Lnsrpyplan.insert",rpyPlan);		
				}catch (FabException e){
					throw new FabException(e, "SPS100", "lnsrpyPlan");
				}
							
				
			}//多个辅助表结束
			sumAmt.selfAdd(termPrin);	
			if(!ConstantDeclare.REPAYWAY.REPAYWAY_XXHB.equals(getRepayWay()) && termPrin.isZero())
			{
				LoggerUtil.error("明细金额不可为零");
				throw new FabException("LNS084");
			}	
		}//整体for循环结束
		
		if(termeDateTmp.compareTo(getEndDate()) != 0)
		{
			LoggerUtil.error("明细最后结束日期必须等于合同结束日期:"+"termeDateTmp["+termeDateTmp
			+"]"+"termenddate["+getEndDate());
			throw new FabException("LNS036");
		}	
		sumAmt.selfSub(contractAmt);
		if (!sumAmt.isZero()) {
			LoggerUtil.error("明细金额之和与合同总金额不符");
			throw new FabException("LNS028");
		}
		if(ConstantDeclare.REPAYWAY.REPAYWAY_XXHB.equals(getRepayWay()) && !termPrin.equals(getContractAmt()))
		{
			LoggerUtil.error("repayway11最后一期金额非法"+termPrin+"|"+getContractAmt());
			throw new FabException("LNS028");
		}	
	}
	/**
	 * @return the compoundRate
	 */
	public FabRate getCompoundRate() {
		return compoundRate;
	}
	/**
	 * @param compoundRate the compoundRate to set
	 */
	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
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
	 * @return the normalRate
	 */
	public FabRate getNormalRate() {
		return normalRate;
	}
	/**
	 * @param normalRate the normalRate to set
	 */
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}
	/**
	 * @return the normalRateType
	 */
	public String getNormalRateType() {
		return normalRateType;
	}
	/**
	 * @param normalRateType the normalRateType to set
	 */
	public void setNormalRateType(String normalRateType) {
		this.normalRateType = normalRateType;
	}
	/**
	 * @return the overdueRate
	 */
	public FabRate getOverdueRate() {
		return overdueRate;
	}
	/**
	 * @param overdueRate the overdueRate to set
	 */
	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
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
	/**
	 * @return the serialNo
	 */
	public String getSerialNo() {
		return serialNo;
	}
	/**
	 * @param serialNo the serialNo to set
	 */
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	/**
	 * @return the termDate
	 */
	public String getTermDate() {
		return termDate;
	}
	/**
	 * @param termDate the termDate to set
	 */
	public void setTermDate(String termDate) {
		this.termDate = termDate;
	}
	/**
	 * @return the tranCode
	 */
	public String getTranCode() {
		return tranCode;
	}
	/**
	 * @param tranCode the tranCode to set
	 */
	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
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
	public String getMerchantNo() {
		return merchantNo;
	}
	public void setMerchantNo(String merchantNo) {
		this.merchantNo = merchantNo;
	}
	public String getCustomName() {
		return customName;
	}
	public void setCustomName(String customName) {
		this.customName = customName;
	}
	public String getCustomType() {
		return customType;
	}
	public void setCustomType(String customType) {
		this.customType = customType;
	}
	public String getOldrepayacct() {
		return oldrepayacct;
	}
	public void setOldrepayacct(String oldrepayacct) {
		this.oldrepayacct = oldrepayacct;
	}
	public FabAmount getContractAmt() {
		return contractAmt;
	}
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
	}
	public String getReceiptNo() {
		return receiptNo;
	}
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}
	public String getStartIntDate() {
		return startIntDate;
	}
	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public String getContractNo() {
		return contractNo;
	}
	public void setContractNo(String contractNo) {
		this.contractNo = contractNo;
	}
	public String getFundChannel() {
		return fundChannel;
	}
	public Integer getGraceDays() {
		return graceDays;
	}
	public void setGraceDays(Integer graceDays) {
		this.graceDays = graceDays;
	}
	public void setFundChannel(String fundChannel) {
		this.fundChannel = fundChannel;
	}
	public String getChannelType() {
		return channelType;
	}
	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}
	public String getOutSerialNo() {
		return outSerialNo;
	}
	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}
	public String getProductCode() {
		return productCode;
	}
	public void setProductCode(String productCode) {
		this.productCode = productCode;
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
	public String getAcctNoNon() {
		return acctNoNon;
	}
	public void setAcctNoNon(String acctNoNon) {
		this.acctNoNon = acctNoNon;
	}
	/**
	 * @return the rentPlanpkgList
	 */
	public List<LnsRentPlanCalculate> getRentPlanpkgList() {
		return rentPlanpkgList;
	}
	/**
	 * @param rentPlanpkgList the rentPlanpkgList to set
	 */
	public void setRentPlanpkgList(List<LnsRentPlanCalculate> rentPlanpkgList) {
		this.rentPlanpkgList = rentPlanpkgList;
	}

}

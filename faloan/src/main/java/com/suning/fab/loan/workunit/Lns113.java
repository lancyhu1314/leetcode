package com.suning.fab.loan.workunit;

import com.suning.fab.loan.account.LnsAcctInfo;
import com.suning.fab.loan.account.LnsRentPlanCalculate;
import com.suning.fab.loan.ao.AccountOperator;
import com.suning.fab.loan.domain.*;
import com.suning.fab.loan.la.FundInvest;
import com.suning.fab.loan.utils.*;
import com.suning.fab.loan.utils.ConstantDeclare.RSPCODE.TRAN;
import com.suning.fab.tup4j.amount.FabAmount;
import com.suning.fab.tup4j.amount.FabRate;
import com.suning.fab.tup4j.base.FabException;
import com.suning.fab.tup4j.base.FabSqlException;
import com.suning.fab.tup4j.base.TranCtx;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.currency.FabCurrency;
import com.suning.fab.tup4j.loopmsg.ListMap;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import com.suning.fab.tup4j.utils.LoggerUtil;
import com.suning.fab.tup4j.utils.VarChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author LH
 *
 * @version V1.0.1
 *
 * @see 开户--非标辅助表预收户幂等三项处理
 *
 * @param
 *
 * @return
 *
 * @exception
 */
@Scope("prototype")

@Repository
public class Lns113 extends WorkUnit { 

	String		openBrc;
	String		receiptNo;
	String		merchantNo;
	FabAmount	contractAmt;
	FabAmount	tailAmt;
	String		loanType;
	Integer		graceDays;
	String		calcIntFlag1;
	String		calcIntFlag2;
	String		startIntDate;
	FabRate		normalRate;
	FabRate		overdueRate;
	FabRate		compoundRate;
	String		normalRateType;
	String		customType;
	String		customName;
	String		productCode;
	String		repayWay;
	String		oldrepayacct;
	String		serialNo;
	String		tranCode;
	String		outSerialNo;
	String		channelType;
	String		fundChannel;
	String		contractNo;
	String		endDate;
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
		//预约日期大于交易日
		if(CalendarUtil.before(endDate, ctx.getTranDate())){
			throw new FabException("LNS071");
		}
		ListMap 	pkgListInMap = ctx.getRequestDict("pkgList1");
		int Count = 0;
		if(rentPlanpkgList.isEmpty())
			Count=pkgListInMap.size();
		else
			Count=rentPlanpkgList.size();


		//预收登记簿
//		TblLnsprefundaccount lnsprefundaccount = new TblLnsprefundaccount();
//		lnsprefundaccount.setBrc(ctx.getBrc());
//		lnsprefundaccount.setCustomid(getMerchantNo());
//		lnsprefundaccount.setAcctno(getMerchantNo());
//		lnsprefundaccount.setName(getCustomName());
//		lnsprefundaccount.setAccsrccode("N");
//		if( "2".equals(getCustomType()) )
//			lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
//		else
//			lnsprefundaccount.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
//
//		lnsprefundaccount.setOpendate(ctx.getTranDate());
//		lnsprefundaccount.setClosedate("");
//		lnsprefundaccount.setBalance(0.00);
//		lnsprefundaccount.setStatus(ConstantDeclare.STATUS.NORMAL);
//		lnsprefundaccount.setReverse1("");
//		lnsprefundaccount.setReverse2("");
//
//		try{
//			DbAccessUtil.execute("Lnsprefundaccount.insert", lnsprefundaccount);
//		}
//		catch (FabSqlException e)
//		{
//			LoggerUtil.info("Lnsprefundaccount.insert.SQLCODE"+e.getSqlCode());
//
//			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
//				LoggerUtil.info("该商户已开户"+getMerchantNo());
//			}
//			else
//				throw new FabException(e, "SPS100", "lnsprefundaccount");
//		}
//
//		Map<String, Object> param = new HashMap<String, Object>();
//		param.put("brc", ctx.getBrc());
//		param.put("accsrccode", "N");
//		param.put("customid", getMerchantNo());
//
//		Map<String, Object> oldrepayacctno;
//
//		oldrepayacctno = DbAccessUtil.queryForMap("CUSTOMIZE.query_oldrepayacctno", param );
//		if (null == oldrepayacctno) {
//			LoggerUtil.info("lns102|query_oldrepayacctno:" + getMerchantNo());
//			throw new FabException("SPS103", "Lnsprefundaccount");
//		}
//		oldrepayacct  = oldrepayacctno.get("acctno").toString();


		//预收户开户:有可能本商户已经开过户了
		LoanAssistDynInfoUtil.addPreaccountinfo(ctx,
			ctx.getBrc(),
			getMerchantNo(),
			ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,
			getMerchantNo(),
			"2".equals(getCustomType())?ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY:ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);

		//查询brc,feetype,custmid为条件的acctno
		TblLnsassistdyninfo preaccountInfo = LoanAssistDynInfoUtil.queryPreaccountInfo(ctx.getBrc(), "", getMerchantNo(), ConstantDeclare.ASSISTACCOUNT.PREFUNDACCT,ctx);

		if (null == preaccountInfo) {
			throw new FabException("SPS104", "lnsassistdyninfo/lnsprefundaccount");
		}
		oldrepayacct  = preaccountInfo.getAcctno();
		
		
		
		//幂等登记薄 幂等的话返回数据要从表里面取  现在是刚好借用了请求包数据
		//如果以后不直接用借据生成账号 幂等插表还是要放最后
		TblLnsinterface	lnsinterface = new TblLnsinterface();
		lnsinterface.setTrandate(ctx.getTermDate());
		lnsinterface.setSerialno(getSerialNo());
		lnsinterface.setAccdate(ctx.getTranDate());
		lnsinterface.setSerseqno(ctx.getSerSeqNo());
		lnsinterface.setTrancode(ctx.getTranCode());
		lnsinterface.setBrc(ctx.getBrc());
		lnsinterface.setAcctname(getCustomName());
		lnsinterface.setUserno(oldrepayacct);
		lnsinterface.setAcctno(getReceiptNo());
		lnsinterface.setTranamt(getContractAmt().getVal());
		lnsinterface.setMagacct(getReceiptNo());
		lnsinterface.setBankno(getOutSerialNo());
		if(getChannelType() != null)
			lnsinterface.setReserv5(getChannelType());
		//科目号存在billno
		if(!VarChecker.isEmpty(getFundChannel()))
			lnsinterface.setBillno(getFundChannel());
		try{
			DbAccessUtil.execute("Lnsinterface.insert", lnsinterface );
		} catch(FabSqlException e) {
			if (ConstantDeclare.SQLCODE.DUPLICATIONKEY.equals(e.getSqlCode())) {
				LoggerUtil.info("交易幂等ACCTNO["+getReceiptNo()+"]");
				throw new FabException(e,TRAN.IDEMPOTENCY);
			}
			else
				throw new FabException(e, "SPS100", "lnsinterface");
		}
		//禁止一笔借据多机构开户
		LoanBasicInfoProvider.checkReceiptNo(getReceiptNo());
		//为了7*24 下面的比较 改opendate为trandate 2017-11-29 DOTO加上倒起息条件校验 日期是否合法
/*		if(!Date.valueOf(getEndDate()).after(Date.valueOf(ctx.getTranDate())))
		{
			LoggerUtil.error("合同结束日期必须大于起始日期:"+"startdate["+ctx.getTranDate()
			+"]"+"enddate["+getEndDate());
			throw new FabException("LNS036");
		}*/
		
		//为了7*24 下面的比较 改opendate为trandate 2017-11-29
		if(!VarChecker.isEmpty(getStartIntDate()) && Date.valueOf(getStartIntDate()).after(Date.valueOf(ctx.getTranDate())))
		{
			LoggerUtil.error("起息日期不能大于合同开户日期:"+"STARTINTDATE["+getStartIntDate()
			+"]"+"OPENDATE["+ctx.getTranDate()+"]");
			throw new FabException("LNS025");
		}
		
		//开本金户抛会计系统事件准备
		if(!"2".equals(calcIntFlag1))
		{	
			FabAmount	amount = new FabAmount(0.00);
			LnsAcctInfo lnsAcctInfo = new LnsAcctInfo(getReceiptNo(), ConstantDeclare.ACCOUNTTYPE.ACCOUNTTYPE_PRIN, ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL, new FabCurrency());
			String reserv1 = null;
			if("1".equals(getCustomType())){
				reserv1 = "70215243";
			}else if("2".equals(getCustomType())){
				reserv1 = ctx.getMerchantNo();
			}else{
				reserv1 = "";
			}
			lnsAcctInfo.setMerchantNo(reserv1);
			lnsAcctInfo.setCustType(getCustomType());
			lnsAcctInfo.setCustName(getCustomName());
			lnsAcctInfo.setPrdCode(getProductCode());

			List<FabAmount> amtList = new ArrayList<FabAmount>();
			if(!VarChecker.isEmpty(tailAmt)){
				amtList.add(tailAmt);
			}

			FundInvest openFundInvest = new FundInvest("", "", getChannelType(), getFundChannel(), getOutSerialNo());	
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANMEBRGT, amount, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.BJKH, ctx);
			add.operate(lnsAcctInfo, null, contractAmt, openFundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx);
			eventProvider.createEvent(ConstantDeclare.EVENT.LOANGRANTA, contractAmt, lnsAcctInfo, null, openFundInvest, ConstantDeclare.BRIEFCODE.BJFK, ctx,amtList,"","","","",getCustomName());
		}
		//单条主文件插表或者单条非标辅助表插表
		if("2".equals(calcIntFlag1)) //插单条非标辅助表 第四种按当期本金余额计息方式
		{
			//每期独立计息的插非标准辅助表
			TblLnsnonstdplan lnsnonstdplan =  new TblLnsnonstdplan();
			lnsnonstdplan.setOpenbrc(ctx.getBrc());	//开户机构代码
			lnsnonstdplan.setAcctno(getReceiptNo());	//账号
			lnsnonstdplan.setRepayterm(0); //期数插零 与其他方式区分
			lnsnonstdplan.setAcctype(ConstantDeclare.ACCTYPE.DEFAULT);	//帐别
			lnsnonstdplan.setCcy(new FabCurrency().getCcy());
			if( "2".equals(getCustomType()) )
				lnsnonstdplan.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
			else
				lnsnonstdplan.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
			
			lnsnonstdplan.setCustomid(getMerchantNo());
			lnsnonstdplan.setName(getCustomName());	//户名
			lnsnonstdplan.setPrdcode(getProductCode());	//产品代码
			lnsnonstdplan.setFlag1("");	//静态标志1 目前第一位标志扣息放款
			lnsnonstdplan.setPrinmindays(0);
			lnsnonstdplan.setIntmindays(0);

			lnsnonstdplan.setIscalint("YES");
			lnsnonstdplan.setRepayway("8");	//对应许磊磊的需求
			//lnsnonstdplan.setRepayway(getRepayWay());	//担保方式    被用为还款方式
			lnsnonstdplan.setLoantype1(getLoanType());	//贷款种类1   2-按揭 、1-普通  3-非标准
			lnsnonstdplan.setLoanstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);	//贷款状态
			lnsnonstdplan.setProfitbrc(ctx.getBrc());	//核算机构代码
			lnsnonstdplan.setAcctno1(getReceiptNo());	//借据号
			//lnsnonstdplan.setFirstprindate(billInfoAll.getFirstRepayPrinDate());  //首次还本日期  char(10)
			//lnsnonstdplan.setFirstintdate(billInfoAll.getFirstRepayIntDate());   //首次付息日期  char(10)
			lnsnonstdplan.setFirstintflag("1");   //首次结息日标志    char(1)
			lnsnonstdplan.setPrinperformula("");
			lnsnonstdplan.setPrinformula("2");  

			//lnsnonstdplan.setLastprindate("");   //下次结本日    char(10)
			//lnsnonstdplan.setLastintdate("");    //下次结息日    char(10)
			if(VarChecker.isEmpty(getStartIntDate()))
			{	
				lnsnonstdplan.setLastprindate(ctx.getTranDate()); 
				lnsnonstdplan.setLastintdate(ctx.getTranDate()); 
			}
			else
			{	
				lnsnonstdplan.setLastprindate(getStartIntDate()); 
				lnsnonstdplan.setLastintdate(getStartIntDate());	//起息日期
			}
			
			lnsnonstdplan.setIntperformula("");
			lnsnonstdplan.setIntformula("2");

			lnsnonstdplan.setPrinterms(Count);   //还本总期数    integer
			//lnsnonstdplan.setIntterms(billInfoAll.getIntTotalPeriod());    //还息总期数    integer
			lnsnonstdplan.setCurprinterm(1); //还本当期期数  integer
			lnsnonstdplan.setCurintterm(1);  //还息当期期数  integer
			lnsnonstdplan.setOldprinterm(0); //本金已还期数  integer
			lnsnonstdplan.setOldintterm(0);  //利息已还期数  integer
			lnsnonstdplan.setContractbal(getContractAmt().getVal());  //合同余额  decimal

			if(VarChecker.isEmpty(getStartIntDate()))
				lnsnonstdplan.setBeginintdate(ctx.getTranDate());	//起息日期
			else
				lnsnonstdplan.setBeginintdate(getStartIntDate());	//起息日期
				
			lnsnonstdplan.setOpendate(ctx.getTranDate());
			lnsnonstdplan.setGracedays(getGraceDays());	//贷款宽限期
			lnsnonstdplan.setContduedate(getEndDate());	//合同到期日
			lnsnonstdplan.setExtnums(0);	//展期次数
			lnsnonstdplan.setIntrateplan("");	//利率计划
			lnsnonstdplan.setBjtxbl(0.00);	//本金贴息比例
			lnsnonstdplan.setTxacctno("");	//贴息帐号
			lnsnonstdplan.setDiacct(oldrepayacct);	//扣款帐号
			lnsnonstdplan.setChanneltype(getChannelType());    //资金来源 放款渠道 1-银行 2-易付宝char
			lnsnonstdplan.setFundchannel(getFundChannel());    //资金来源帐号 资金通道 sap银行科目编号/易付宝总
			lnsnonstdplan.setOutserialno(getOutSerialNo());    //外部流水单号：银行资金流水号/易付宝交易单号
			//2019-09-26 合同号超过20位存拓展表
			if( !VarChecker.isEmpty(getContractNo()) )
			{
				if( getContractNo().length() <= 20 )
					lnsnonstdplan.setContrasctno(getContractNo());	//合同编号
				else
				{
					//修改主文件拓展表
					Map<String,Object> exParam = new HashMap<>();
					exParam.put("acctno", lnsnonstdplan.getAcctno());
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
			lnsnonstdplan.setContractamt(getContractAmt().getVal());	//合同金额
			lnsnonstdplan.setProvisionflag("RUNNING");
			lnsnonstdplan.setMemo("");	//备注
			lnsnonstdplan.setNormalrate(getNormalRate().getYearRate().doubleValue());	//正常利率
			lnsnonstdplan.setOverduerate(getOverdueRate().getYearRate().doubleValue());	//逾期利率
			lnsnonstdplan.setOverduerate1(getCompoundRate().getYearRate().doubleValue());	//复利利率
			lnsnonstdplan.setModifydate(ctx.getTranDate());	//交易日期
			lnsnonstdplan.setTrandate(ctx.getTranDate());
			lnsnonstdplan.setModifytime("");	//交易时间

			try {
				DbAccessUtil.execute("Lnsnonstdplan.insert",lnsnonstdplan );
			} catch(FabSqlException e) {
				LoggerUtil.error("lnsnonstdplan.insert.ERROR"+e.getSqlCode());
				throw new FabException(e, "SPS100", "lnsnonstdplan");
			}
		
		}
		else//插单条主文件 前三种按合同余额计息方式
		{
			//写静态表
			TblLnsbasicinfo lnsbasicinfo = new TblLnsbasicinfo();
			lnsbasicinfo.setOpenbrc(ctx.getBrc());	//开户机构代码
			lnsbasicinfo.setAcctno(getReceiptNo());	//账号
			lnsbasicinfo.setAcctype(ConstantDeclare.ACCTYPE.DEFAULT);	//帐别
			lnsbasicinfo.setCcy(new FabCurrency().getCcy());
			if( "2".equals(getCustomType()) )
				lnsbasicinfo.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.COMPANY);
			else
				lnsbasicinfo.setCusttype(ConstantDeclare.ACCOUNT.CUSTOMERTYPE.PERSON);
			
			lnsbasicinfo.setCustomid(getMerchantNo());
			lnsbasicinfo.setName(getCustomName());	//户名
			lnsbasicinfo.setPrdcode(getProductCode());	//产品代码
			lnsbasicinfo.setFlag1("");	//静态标志1 目前第一位标志扣息放款
			lnsbasicinfo.setPrinmindays(0);
			lnsbasicinfo.setIntmindays(0);

			lnsbasicinfo.setIscalint("YES");
			lnsbasicinfo.setRepayway(getRepayWay());	//担保方式    被用为还款方式
			lnsbasicinfo.setLoantype1(getLoanType());	//贷款种类1   2-按揭 、1-普通
			lnsbasicinfo.setLoanstat(ConstantDeclare.LOANSTATUS.LOANSTATUS_NORMAL);	//贷款状态
			lnsbasicinfo.setProfitbrc(ctx.getBrc());	//核算机构代码
			lnsbasicinfo.setAcctno1(getReceiptNo());	//借据号
			//lnsbasicinfo.setFirstprindate(billInfoAll.getFirstRepayPrinDate());  //首次还本日期  char(10)
			//lnsbasicinfo.setFirstintdate(billInfoAll.getFirstRepayIntDate());   //首次付息日期  char(10)
			lnsbasicinfo.setFirstintflag("1");   //首次结息日标志    char(1)
			lnsbasicinfo.setPrinperformula("");
			lnsbasicinfo.setPrinformula("3");  

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
			
			lnsbasicinfo.setIntperformula("");
			lnsbasicinfo.setIntformula("3");

			lnsbasicinfo.setPrinterms(Count);   //还本总期数    integer
			//lnsbasicinfo.setIntterms(billInfoAll.getIntTotalPeriod());    //还息总期数    integer
			lnsbasicinfo.setCurprinterm(1); //还本当期期数  integer
			lnsbasicinfo.setCurintterm(1);  //还息当期期数  integer
			lnsbasicinfo.setOldprinterm(0); //本金已还期数  integer
			lnsbasicinfo.setOldintterm(0);  //利息已还期数  integer
			lnsbasicinfo.setContractbal(getContractAmt().getVal());  //合同余额  decimal

			if(VarChecker.isEmpty(getStartIntDate()))
				lnsbasicinfo.setBeginintdate(ctx.getTranDate());	//起息日期
			else
				lnsbasicinfo.setBeginintdate(getStartIntDate());	//起息日期
				
			lnsbasicinfo.setOpendate(ctx.getTranDate());
			lnsbasicinfo.setGracedays(getGraceDays());	//贷款宽限期
			lnsbasicinfo.setContduedate(getEndDate());	//合同到期日
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
			lnsbasicinfo.setContractamt(getContractAmt().getVal());	//合同金额
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
			
			//写动态表
			TblLnsaccountdyninfo lnsaccountdyninfo =  new TblLnsaccountdyninfo();
			lnsaccountdyninfo.setAcctno(getReceiptNo());//贷款账号
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
			
		}	
	}
	
	public String getRepayWay() {
		return repayWay;
	}
	public void setRepayWay(String repayWay) {
		this.repayWay = repayWay;
	}
	public FabRate getCompoundRate() {
		return compoundRate;
	}
	public void setCompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
	}
	public String getCustomType() {
		return customType;
	}
	public void setCustomType(String customType) {
		this.customType = customType;
	}
	public String getCustomName() {
		return customName;
	}
	public void setCustomName(String customName) {
		this.customName = customName;
	}
	public String getProductCode() {
		return productCode;
	}
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}
	/**
	 * @return the openBrc
	 */
	public String getOpenBrc() {
		return openBrc;
	}
	/**
	 * @param openBrc the openBrc to set
	 */
	public void setOpenBrc(String openBrc) {
		this.openBrc = openBrc;
	}
	/**
	 * @return the receiptNo
	 */
	public String getReceiptNo() {
		return receiptNo;
	}
	/**
	 * @param receiptNo the receiptNo to set
	 */
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}
	/**
	 * @return the merchantNo
	 */
	public String getMerchantNo() {
		return merchantNo;
	}
	/**
	 * @param merchantNo the merchantNo to set
	 */
	public void setMerchantNo(String merchantNo) {
		this.merchantNo = merchantNo;
	}
	/**
	 * @return the contractAmt
	 */
	public FabAmount getContractAmt() {
		return contractAmt;
	}
	/**
	 * @param contractAmt the contractAmt to set
	 */
	public void setContractAmt(FabAmount contractAmt) {
		this.contractAmt = contractAmt;
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
	public String getLoanType() {
		return loanType;
	}
	public void setLoanType(String loanType) {
		this.loanType = loanType;
	}
	public Integer getGraceDays() {
		return graceDays;
	}
	public void setGraceDays(Integer graceDays) {
		this.graceDays = graceDays;
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
	public String getStartIntDate() {
		return startIntDate;
	}
	public void setStartIntDate(String startIntDate) {
		this.startIntDate = startIntDate;
	}
	public FabRate getNormalRate() {
		return normalRate;
	}
	public void setNormalRate(FabRate normalRate) {
		this.normalRate = normalRate;
	}
	public FabRate getOverdueRate() {
		return overdueRate;
	}
	public void setOverdueRate(FabRate overdueRate) {
		this.overdueRate = overdueRate;
	}
	public FabRate getcompoundRate() {
		return compoundRate;
	}
	public void setcompoundRate(FabRate compoundRate) {
		this.compoundRate = compoundRate;
	}
	public String getNormalRateType() {
		return normalRateType;
	}
	public void setNormalRateType(String normalRateType) {
		this.normalRateType = normalRateType;
	}

	public String getOldrepayacct() {
		return oldrepayacct;
	}

	public void setOldrepayacct(String oldrepayacct) {
		this.oldrepayacct = oldrepayacct;
	}

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	public String getTranCode() {
		return tranCode;
	}

	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}

	public String getOutSerialNo() {
		return outSerialNo;
	}

	public void setOutSerialNo(String outSerialNo) {
		this.outSerialNo = outSerialNo;
	}

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public String getFundChannel() {
		return fundChannel;
	}

	public void setFundChannel(String fundChannel) {
		this.fundChannel = fundChannel;
	}

	public String getContractNo() {
		return contractNo;
	}

	public void setContractNo(String contractNo) {
		this.contractNo = contractNo;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
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

	public FabAmount getTailAmt() {
		return tailAmt;
	}

	public void setTailAmt(FabAmount tailAmt) {
		this.tailAmt = tailAmt;
	}




	
}
